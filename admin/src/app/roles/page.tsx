'use client';

import { Shield, Users, CheckCircle2, Lock, Key } from 'lucide-react';
import { useAdminLanguage } from '@/components/AdminLanguageContext';
import StatusBadge from '@/components/StatusBadge';

const roles = [
  {
    name: 'SUPER_ADMIN',
    nameGu: 'સુપર એડમિન',
    badge: 'Executive',
    purpose: 'Complete system administration, database management, and overall security governance.',
    purposeGu: 'સંપૂર્ણ સિસ્ટમ સંચાલન, ડેટાબેઝ વ્યવસ્થા અને સુરક્ષા પર નિયંત્રણ.',
    capabilities: [
      'Grant/revoke any staff role',
      'System-wide settings & credentials configuration',
      'Direct audit log surveillance',
      'Emergency database and user management',
    ],
  },
  {
    name: 'ADMIN',
    nameGu: 'એડમિન',
    badge: 'Operations',
    purpose: 'General administration of village digital services, citizen registrations, and approvals.',
    purposeGu: 'ગામની સેવાઓ, નોંધાયેલા સભ્યો અને મંજૂરીઓનું સામાન્ય સંચાલન.',
    capabilities: [
      'Create and update resident profiles',
      'Assign feature-level permissions',
      'Publish village notices and announcements',
      'Review and finalize devotional offerings (ચઢાવો)',
    ],
  },
  {
    name: 'MODERATOR',
    nameGu: 'મોડરેટર',
    badge: 'Civic',
    purpose: 'Community moderation, complaint resolution, dispute handling, and member verification.',
    purposeGu: 'સમુદાયનું સંચાલન, ફરિયાદોનો ઉકેલ અને સભ્યોની ચકાસણી.',
    capabilities: [
      'Investigate and resolve citizen grievances',
      'Add official resolution notes',
      'Verify incoming business and resident registrations',
      'Flag inappropriate content',
    ],
  },
  {
    name: 'CONTENT_MANAGER',
    nameGu: 'કન્ટેન્ટ મેનેજર',
    badge: 'Editorial',
    purpose: 'Editorial management of circulars, cultural event archives, gallery media, and history.',
    purposeGu: 'સૂચનાઓ, કાર્યક્રમો અને ગેલેરીની સામગ્રીનું સંપાદકીય સંચાલન.',
    capabilities: [
      'Draft and publish community notices',
      'Create festival & Navratri schedule entries',
      'Update village historical archives',
      'Send push broadcasts to village app',
    ],
  },
  {
    name: 'BUSINESS_OWNER',
    nameGu: 'વ્યવસાય માલિક',
    badge: 'Commercial',
    purpose: 'Manages a verified village business or local employment opportunity listing.',
    purposeGu: 'ચકાસાયેલ ગામના વ્યવસાય અથવા નોકરીની સૂચિનું સંચાલન.',
    capabilities: [
      'Update business contact and address',
      'Post local jobs and trade opportunities',
      'Respond to commercial inquiries',
    ],
  },
  {
    name: 'USER',
    nameGu: 'સભ્ય',
    badge: 'Resident',
    purpose: 'Registered village community resident with personal access portal.',
    purposeGu: 'નોંધાયેલ ગામનો સમુદાય સભ્ય.',
    capabilities: [
      'Access personalized resident dashboard',
      'Manage profile and contact preferences',
      'Submit citizen grievances & track status',
      'Read official village announcements and event details',
    ],
  },
];

export default function RolesPage() {
  const { lang } = useAdminLanguage();
  const isGujarati = lang === 'gu';

  return (
    <div className="mx-auto max-w-6xl space-y-8">
      {/* Page Header */}
      <header className="border-b border-slate-200 pb-5 dark:border-slate-800">
        <div className="flex items-center gap-2 text-sky-600 dark:text-sky-400">
          <Shield className="h-5 w-5" />
          <span className="text-xs font-semibold uppercase tracking-wider">
            {isGujarati ? 'પ્રવેશ વ્યવસ્થા અને સત્તાઓ' : 'Access Control Matrix'}
          </span>
        </div>
        <h1 className="mt-2 text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
          {isGujarati ? 'ભૂમિકાઓ અને અધિકારો (Roles & Permissions)' : 'Role-Based Access Control (RBAC)'}
        </h1>
        <p className="mt-1 text-xs text-slate-500 dark:text-slate-400">
          Security architecture and privilege hierarchy enforced by Supabase Row-Level Security (RLS).
        </p>
      </header>

      {/* Role Cards Grid */}
      <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
        {roles.map((role) => (
          <div
            key={role.name}
            className="flex flex-col justify-between rounded-3xl border border-slate-200/80 bg-white p-6 shadow-sm transition hover:shadow-md dark:border-slate-800 dark:bg-slate-900"
          >
            <div>
              <div className="flex items-center justify-between">
                <StatusBadge status={role.name} />
                <span className="rounded-md bg-slate-100 px-2 py-0.5 text-[10px] font-bold text-slate-600 dark:bg-slate-800 dark:text-slate-400">
                  {role.badge}
                </span>
              </div>

              <h3 className="mt-4 text-base font-bold text-slate-900 dark:text-white">
                {isGujarati ? role.nameGu : role.name.replace(/_/g, ' ')}
              </h3>
              <p className="mt-2 text-xs text-slate-600 dark:text-slate-400 leading-relaxed">
                {isGujarati ? role.purposeGu : role.purpose}
              </p>

              <div className="mt-5 border-t border-slate-100 pt-4 dark:border-slate-800">
                <h4 className="text-[11px] font-bold uppercase tracking-wider text-slate-400">
                  {isGujarati ? 'પરવાનગીઓ' : 'Key Capabilities'}
                </h4>
                <ul className="mt-2 space-y-2">
                  {role.capabilities.map((cap, i) => (
                    <li key={i} className="flex items-start gap-2 text-xs text-slate-700 dark:text-slate-300">
                      <CheckCircle2 className="h-3.5 w-3.5 shrink-0 text-emerald-500 mt-0.5" />
                      <span>{cap}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </div>

            <div className="mt-6 flex items-center gap-1.5 text-[11px] font-semibold text-slate-400 border-t border-slate-100 pt-3 dark:border-slate-800">
              <Lock className="h-3 w-3" />
              <span>Enforced at API & DB Layer</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}