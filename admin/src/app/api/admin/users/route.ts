import { createClient, type SupabaseClient } from '@supabase/supabase-js';
import { NextRequest, NextResponse } from 'next/server';

const roleNames = ['SUPER_ADMIN', 'ADMIN', 'MODERATOR', 'CONTENT_MANAGER', 'BUSINESS_OWNER', 'USER'] as const;
type RoleName = (typeof roleNames)[number];
const featureKeys = [
  'members', 'notices', 'offerings', 'navratri', 'complaints',
  'emergency_contacts', 'village_history', 'events', 'directory',
  'gallery', 'notifications',
] as const;
type FeatureKey = (typeof featureKeys)[number];

type AdminContext = {
  supabase: SupabaseClient;
  authUserId: string;
  profileId: string;
  roles: RoleName[];
};

type ApiResult = { context: AdminContext } | { response: NextResponse };

function errorResponse(message: string, status: number) {
  return NextResponse.json({ error: message }, { status });
}

async function requireAdmin(request: NextRequest, allowMemberFeature: boolean = false): Promise<ApiResult> {
  const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL;
  const anonKey = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY;
  const serviceRoleKey = process.env.SUPABASE_SERVICE_ROLE_KEY;
  if (!supabaseUrl || !anonKey || !serviceRoleKey) {
    return { response: errorResponse('User administration is not configured on the server.', 503) };
  }

  const accessToken = request.headers.get('authorization')?.replace(/^Bearer\s+/i, '');
  if (!accessToken) return { response: errorResponse('Sign in again to continue.', 401) };

  const authClient = createClient(supabaseUrl, anonKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data: { user }, error: authError } = await authClient.auth.getUser(accessToken);
  if (authError || !user) return { response: errorResponse('Your session could not be verified. Sign in again.', 401) };

  const supabase = createClient(supabaseUrl, serviceRoleKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
  const { data: roleProbe, error: roleProbeError } = await supabase
    .from('roles')
    .select('id')
    .limit(1);
  if (roleProbeError || !roleProbe?.length) {
    return {
      response: errorResponse(
        'SUPABASE_SERVICE_ROLE_KEY must contain a secret key beginning with sb_secret_ from this project. The current key has no elevated server access.',
        503
      ),
    };
  }

  const { data: profile, error: profileError } = await supabase
    .from('profiles')
    .select('id')
    .eq('auth_user_id', user.id)
    .eq('status', 'ACTIVE')
    .maybeSingle();
  if (profileError) return { response: errorResponse(profileError.message, 500) };
  if (!profile) return { response: errorResponse('Administrator profile was not found.', 403) };

  const { data: assignments, error: assignmentsError } = await supabase
    .from('user_roles')
    .select('role_id')
    .eq('user_id', profile.id);
  if (assignmentsError) return { response: errorResponse(assignmentsError.message, 500) };

  const roleIds = (assignments ?? []).map((assignment) => assignment.role_id);
  const { data: roleRows, error: rolesError } = roleIds.length
    ? await supabase.from('roles').select('id,name').in('id', roleIds)
    : { data: [], error: null };
  if (rolesError) return { response: errorResponse(rolesError.message, 500) };

  const roles = (roleRows ?? []).map((role) => role.name as RoleName);
  const isAdmin = roles.some((role) => role === 'ADMIN' || role === 'SUPER_ADMIN');
  if (!isAdmin && allowMemberFeature) {
    const { data: grants, error: grantsError } = await supabase
      .from('profile_feature_permissions')
      .select('feature_key')
      .eq('profile_id', profile.id);
    if (grantsError) return { response: errorResponse(grantsError.message, 500) };
    if (grants?.some(({ feature_key }) => feature_key === 'members')) {
      return { context: { supabase, authUserId: user.id, profileId: profile.id, roles } };
    }
  }
  if (!isAdmin) {
    return { response: errorResponse('Only administrators can manage member accounts.', 403) };
  }

  return { context: { supabase, authUserId: user.id, profileId: profile.id, roles } };
}

function validRoleList(value: unknown): value is RoleName[] {
  return Array.isArray(value) && value.length > 0 && value.every(
    (role): role is RoleName => typeof role === 'string' && roleNames.includes(role as RoleName)
  );
}

function validFeatureList(value: unknown): value is FeatureKey[] {
  return Array.isArray(value) && value.every(
    (feature): feature is FeatureKey => typeof feature === 'string' && featureKeys.includes(feature as FeatureKey)
  );
}

async function getFeaturePermissions(supabase: SupabaseClient, profileIds: string[]) {
  const byProfile = new Map<string, FeatureKey[]>();
  if (!profileIds.length) return byProfile;
  const { data, error } = await supabase
    .from('profile_feature_permissions')
    .select('profile_id,feature_key')
    .in('profile_id', profileIds);
  if (error) throw new Error(error.message);
  for (const grant of data ?? []) {
    const key = grant.feature_key as FeatureKey;
    byProfile.set(grant.profile_id, [...(byProfile.get(grant.profile_id) ?? []), key]);
  }
  return byProfile;
}

async function replaceFeaturePermissions(
  context: AdminContext,
  profileId: string,
  desired: FeatureKey[]
) {
  const { error: deleteError } = await context.supabase
    .from('profile_feature_permissions')
    .delete()
    .eq('profile_id', profileId);
  if (deleteError) throw new Error(deleteError.message);
  if (!desired.length) return;
  const { error } = await context.supabase.from('profile_feature_permissions').insert(
    desired.map((feature_key) => ({ profile_id: profileId, feature_key, granted_by: context.profileId }))
  );
  if (error) throw new Error(error.message);
}

async function loadRoleIds(supabase: SupabaseClient) {
  const { data, error } = await supabase.from('roles').select('id,name');
  if (error) throw new Error(error.message);
  const ids = new Map((data ?? []).map((role) => [role.name as RoleName, role.id]));
  if (roleNames.some((role) => !ids.has(role))) throw new Error('Role definitions are incomplete in the database.');
  return ids;
}

async function getAssignedRoles(supabase: SupabaseClient, profileId: string): Promise<RoleName[]> {
  const { data: assignments, error } = await supabase
    .from('user_roles')
    .select('role_id')
    .eq('user_id', profileId);
  if (error) throw new Error(error.message);

  const roleIds = (assignments ?? []).map((assignment) => assignment.role_id);
  if (!roleIds.length) return [];

  const { data: rows, error: rolesError } = await supabase
    .from('roles')
    .select('id,name')
    .in('id', roleIds);
  if (rolesError) throw new Error(rolesError.message);
  return (rows ?? []).map((role) => role.name as RoleName);
}

async function protectRoleChanges(
  context: AdminContext,
  profileId: string,
  desiredRoles: RoleName[]
) {
  const currentRoles = await getAssignedRoles(context.supabase, profileId);
  const isSuperAdmin = context.roles.includes('SUPER_ADMIN');
  if (!isSuperAdmin && (currentRoles.includes('SUPER_ADMIN') || desiredRoles.includes('SUPER_ADMIN'))) {
    throw new Error('Only a SUPER_ADMIN can change a SUPER_ADMIN assignment.');
  }
  if (profileId === context.profileId && currentRoles.some((role) => !desiredRoles.includes(role))) {
    throw new Error('You cannot remove your own assigned roles.');
  }

  if (currentRoles.includes('SUPER_ADMIN') && !desiredRoles.includes('SUPER_ADMIN')) {
    const roleIds = await loadRoleIds(context.supabase);
    const superAdminRoleId = roleIds.get('SUPER_ADMIN');
    if (!superAdminRoleId) throw new Error('SUPER_ADMIN role is missing.');
    const { data: assignments, error } = await context.supabase
      .from('user_roles')
      .select('user_id')
      .eq('role_id', superAdminRoleId);
    if (error) throw new Error(error.message);
    const profileIds = (assignments ?? []).map((assignment) => assignment.user_id);
    const { data: activeProfiles, error: activeError } = await context.supabase
      .from('profiles')
      .select('id')
      .in('id', profileIds)
      .eq('status', 'ACTIVE');
    if (activeError) throw new Error(activeError.message);
    if ((activeProfiles ?? []).length <= 1) throw new Error('The last active SUPER_ADMIN cannot be removed.');
  }

  return currentRoles;
}

async function replaceRoles(
  context: AdminContext,
  profileId: string,
  desiredRoles: RoleName[],
  currentRoles: RoleName[]
) {
  const roleIds = await loadRoleIds(context.supabase);
  const additions = desiredRoles
    .filter((role) => !currentRoles.includes(role))
    .map((role) => ({ user_id: profileId, role_id: roleIds.get(role) }));

  if (additions.length) {
    const { error } = await context.supabase.from('user_roles').insert(additions);
    if (error) throw new Error(error.message);
  }

  const removedIds = currentRoles
    .filter((role) => !desiredRoles.includes(role))
    .map((role) => roleIds.get(role))
    .filter((id): id is string => Boolean(id));
  if (removedIds.length) {
    const { error } = await context.supabase
      .from('user_roles')
      .delete()
      .eq('user_id', profileId)
      .in('role_id', removedIds);
    if (error) throw new Error(error.message);
  }
}

export async function GET(request: NextRequest) {
  const result = await requireAdmin(request, true);
  if ('response' in result) return result.response;
  const { supabase } = result.context;
  const isAdministrator = result.context.roles.some((role) => role === 'ADMIN' || role === 'SUPER_ADMIN');

  const [{ data: users, error: usersError }, { data: assignments, error: assignmentsError }, { data: roles, error: rolesError }] = await Promise.all([
    supabase.from('profiles').select('id,auth_user_id,full_name,email,phone,status,created_at').order('created_at', { ascending: false }),
    supabase.from('user_roles').select('user_id,role_id'),
    supabase.from('roles').select('id,name,description').order('name'),
  ]);
  const error = usersError ?? assignmentsError ?? rolesError;
  if (error) return errorResponse(error.message, 500);

  const authUserIds = new Set<string>();
  if (isAdministrator) {
    for (let page = 1; ; page += 1) {
      const { data: authPage, error: authListError } = await supabase.auth.admin.listUsers({ page, perPage: 1000 });
      if (authListError) return errorResponse(authListError.message, 500);
      for (const authUser of authPage.users) authUserIds.add(authUser.id);
      if (authPage.users.length < 1000) break;
    }
  }

  const roleById = new Map((roles ?? []).map((role) => [role.id, role.name as RoleName]));
  const roleByUser = new Map<string, RoleName[]>();
  for (const assignment of assignments ?? []) {
    const role = roleById.get(assignment.role_id);
    if (!role) continue;
    roleByUser.set(assignment.user_id, [...(roleByUser.get(assignment.user_id) ?? []), role]);
  }
  let featuresByUser: Map<string, FeatureKey[]>;
  try {
    featuresByUser = await getFeaturePermissions(supabase, (users ?? []).map((user) => user.id));
  } catch (permissionError) {
    return errorResponse(permissionError instanceof Error ? permissionError.message : 'Could not load feature access.', 500);
  }

  const allUsers = (users ?? []).map((user) => ({
      ...user,
      auth_exists: isAdministrator ? authUserIds.has(user.auth_user_id) : true,
      roles: roleByUser.get(user.id) ?? [],
      feature_permissions: featuresByUser.get(user.id) ?? [],
    }));
  return NextResponse.json(isAdministrator
    ? { users: allUsers, roles: roles ?? [], canManageUsers: true }
    : { users: allUsers.map(({ auth_user_id: _authUserId, ...user }) => ({ ...user, auth_exists: false })), roles: [], canManageUsers: false });
}

export async function POST(request: NextRequest) {
  const result = await requireAdmin(request);
  if ('response' in result) return result.response;
  const { context } = result;

  try {
    const body = await request.json();
    const fullName = typeof body.full_name === 'string' ? body.full_name.trim() : '';
    const email = typeof body.email === 'string' ? body.email.trim().toLowerCase() : '';
    const phone = typeof body.phone === 'string' ? body.phone.trim() : '';
    const roles = body.roles ?? ['USER'];
    const featurePermissions = body.feature_permissions ?? [];
    if (!fullName || !email || !validRoleList(roles) || !validFeatureList(featurePermissions)) {
      return errorResponse('Name, valid email, and at least one valid role are required.', 400);
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return errorResponse('Enter a valid email address.', 400);
    if (!context.roles.includes('SUPER_ADMIN') && roles.includes('SUPER_ADMIN')) {
      return errorResponse('Only a SUPER_ADMIN can grant the SUPER_ADMIN role.', 403);
    }

    const { data: invite, error: inviteError } = await context.supabase.auth.admin.inviteUserByEmail(email, {
      data: { full_name: fullName, phone },
    });
    if (inviteError) return errorResponse(inviteError.message, 400);
    if (!invite.user) return errorResponse('The invitation did not create an Auth user.', 500);

    const { data: profile, error: profileError } = await context.supabase
      .from('profiles')
      .upsert({
        auth_user_id: invite.user.id,
        full_name: fullName,
        email,
        phone: phone || null,
        status: 'ACTIVE',
      }, { onConflict: 'auth_user_id' })
      .select('id,auth_user_id,full_name,email,phone,status,created_at')
      .single();
    if (profileError) return errorResponse(`Account invited, but profile creation failed: ${profileError.message}`, 500);

    const currentRoles = await getAssignedRoles(context.supabase, profile.id);
    await replaceRoles(context, profile.id, roles, currentRoles);
    await replaceFeaturePermissions(context, profile.id, featurePermissions);
    return NextResponse.json({ user: { ...profile, roles, feature_permissions: featurePermissions } }, { status: 201 });
  } catch (error) {
    return errorResponse(error instanceof Error ? error.message : 'Could not create the member account.', 500);
  }
}

export async function PATCH(request: NextRequest) {
  const result = await requireAdmin(request);
  if ('response' in result) return result.response;
  const { context } = result;

  try {
    const body = await request.json();
    const profileId = typeof body.id === 'string' ? body.id : '';
    const fullName = typeof body.full_name === 'string' ? body.full_name.trim() : '';
    const email = typeof body.email === 'string' ? body.email.trim().toLowerCase() : '';
    const phone = typeof body.phone === 'string' ? body.phone.trim() : '';
    const status = body.status;
    const roles = body.roles;
    const featurePermissions = body.feature_permissions;
    if (!profileId || !fullName || !email || !validRoleList(roles) || !validFeatureList(featurePermissions) || !['ACTIVE', 'SUSPENDED', 'DELETED'].includes(status)) {
      return errorResponse('Valid member details, status, and at least one role are required.', 400);
    }
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return errorResponse('Enter a valid email address.', 400);

    const { data: profile, error: profileError } = await context.supabase
      .from('profiles')
      .select('id,auth_user_id,full_name,email,phone,status,created_at')
      .eq('id', profileId)
      .maybeSingle();
    if (profileError) return errorResponse(profileError.message, 500);
    if (!profile) return errorResponse('Member was not found.', 404);

    const currentRoles = await protectRoleChanges(context, profile.id, roles);
    if (profile.auth_user_id === context.authUserId && currentRoles.some((role) => !roles.includes(role))) {
      return errorResponse('You cannot remove your own assigned roles.', 400);
    }

    const { data: authUser, error: authUserError } = await context.supabase.auth.admin.getUserById(profile.auth_user_id);
    if (authUserError) return errorResponse(authUserError.message, 500);
    if (authUser.user) {
      const { error: authUpdateError } = await context.supabase.auth.admin.updateUserById(profile.auth_user_id, {
        email,
        email_confirm: true,
        user_metadata: { full_name: fullName, phone },
        ban_duration: status === 'ACTIVE' ? 'none' : '876000h',
      });
      if (authUpdateError) return errorResponse(authUpdateError.message, 400);
    }

    const { data: updatedProfile, error: updateError } = await context.supabase
      .from('profiles')
      .update({ full_name: fullName, email, phone: phone || null, status })
      .eq('id', profile.id)
      .select('id,auth_user_id,full_name,email,phone,status,created_at')
      .single();
    if (updateError) return errorResponse(`Auth account updated, but profile update failed: ${updateError.message}`, 500);

    await replaceRoles(context, profile.id, roles, currentRoles);
    await replaceFeaturePermissions(context, profile.id, featurePermissions);
    return NextResponse.json({ user: { ...updatedProfile, roles, feature_permissions: featurePermissions } });
  } catch (error) {
    return errorResponse(error instanceof Error ? error.message : 'Could not update the member account.', 500);
  }
}

export async function DELETE(request: NextRequest) {
  const result = await requireAdmin(request);
  if ('response' in result) return result.response;
  const { context } = result;

  try {
    const body = await request.json();
    const profileId = typeof body.id === 'string' ? body.id : '';
    if (!profileId) return errorResponse('Member ID is required.', 400);
    if (profileId === context.profileId) return errorResponse('You cannot delete your own account here.', 400);

    const { data: profile, error: profileError } = await context.supabase
      .from('profiles')
      .select('id,auth_user_id')
      .eq('id', profileId)
      .maybeSingle();
    if (profileError) return errorResponse(profileError.message, 500);
    if (!profile) return errorResponse('Member was not found.', 404);

    const currentRoles = await getAssignedRoles(context.supabase, profile.id);
    await protectRoleChanges(context, profile.id, []);

    const { data: authUser, error: authUserError } = await context.supabase.auth.admin.getUserById(profile.auth_user_id);
    if (authUserError) return errorResponse(authUserError.message, 500);
    if (authUser.user) {
      const { error: deleteAuthError } = await context.supabase.auth.admin.deleteUser(profile.auth_user_id);
      if (deleteAuthError) return errorResponse(deleteAuthError.message, 400);
    }

    const { error: deleteProfileError } = await context.supabase.from('profiles').delete().eq('id', profile.id);
    if (deleteProfileError) {
      return errorResponse(`Auth account deleted, but profile cleanup failed: ${deleteProfileError.message}`, 500);
    }
    return NextResponse.json({ deleted: true, roles: currentRoles });
  } catch (error) {
    return errorResponse(error instanceof Error ? error.message : 'Could not delete the member account.', 500);
  }
}
