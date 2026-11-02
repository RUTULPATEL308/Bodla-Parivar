import { NextRequest, NextResponse } from 'next/server';
import { createClient } from '@supabase/supabase-js';

const MAX_FILE_SIZE = 25 * 1024 * 1024;

export async function POST(request: NextRequest) {
  try {
    const supabaseUrl = process.env.NEXT_PUBLIC_SUPABASE_URL;
    const anonKey = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY;
    const serviceRoleKey = process.env.SUPABASE_SERVICE_ROLE_KEY;
    if (!supabaseUrl || !anonKey || !serviceRoleKey) {
      return NextResponse.json({ error: 'Server environment configuration missing for Supabase.' }, { status: 500 });
    }

    const accessToken = request.headers.get('authorization')?.replace(/^Bearer\s+/i, '');
    if (!accessToken) {
      return NextResponse.json({ error: 'Sign in to upload a notice attachment.' }, { status: 401 });
    }

    const authClient = createClient(supabaseUrl, anonKey, {
      auth: { persistSession: false, autoRefreshToken: false },
    });
    const { data: { user }, error: authError } = await authClient.auth.getUser(accessToken);
    if (authError || !user) {
      return NextResponse.json({ error: 'Your session is invalid. Sign in again.' }, { status: 401 });
    }

    const formData = await request.formData();
    const file = formData.get('file');
    if (!(file instanceof File)) {
      return NextResponse.json({ error: 'No file was provided.' }, { status: 400 });
    }
    if (file.size > MAX_FILE_SIZE) {
      return NextResponse.json({ error: 'The file must be 25 MB or smaller.' }, { status: 413 });
    }
    if (!file.type.startsWith('image/') && file.type !== 'application/pdf') {
      return NextResponse.json({ error: 'Only images or PDF files can be uploaded.' }, { status: 400 });
    }

    const extension = file.name.split('.').pop()?.replace(/[^a-zA-Z0-9]/g, '') || 'file';
    const cleanExtension = extension.slice(0, 10).toLowerCase();
    const filePath = `${user.id}/broadcasts/${Date.now()}-${crypto.randomUUID()}.${cleanExtension}`;
    const buffer = Buffer.from(await file.arrayBuffer());

    const serviceClient = createClient(supabaseUrl, serviceRoleKey, {
      auth: { persistSession: false, autoRefreshToken: false },
    });
    const { error: uploadError } = await serviceClient.storage.from('notices').upload(filePath, buffer, {
      contentType: file.type,
      upsert: false,
    });
    if (uploadError) {
      console.error('Supabase notice attachment upload failed:', uploadError);
      return NextResponse.json({ error: 'The notice attachment could not be uploaded.' }, { status: 500 });
    }

    const { data } = serviceClient.storage.from('notices').getPublicUrl(filePath);
    return NextResponse.json({
      publicUrl: data.publicUrl,
      isImage: file.type.startsWith('image/'),
      fileName: file.name,
    });
  } catch (error: unknown) {
    console.error('Notice attachment upload failed:', error);
    return NextResponse.json({ error: 'The notice attachment could not be uploaded.' }, { status: 500 });
  }
}
