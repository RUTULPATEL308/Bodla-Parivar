'use client';

import { Shield, Users } from 'lucide-react';
import { useAdminLanguage } from '@/components/AdminLanguageContext';

const roles = [
  {
    name: 'SUPER_ADMIN',
    nameGu: 'સુપર એડમિન',
    purpose: 'Complete system administration and settings control.',
    purposeGu: 'સંપૂર્ણ સિસ્ટમ સંચાલન અને સેટિંગ્સ પર નિયંત્રણ.',
  },
  {
    name: 'ADMIN',
    nameGu: 'એડમિન',
    purpose: 'General administration of village content and registered users.',
    purposeGu: 'ગામની સામગ્રી અને નોંધાયેલા સભ્યોનું સામાન્ય સંચાલન.',
  },
  {
    name: 'MODERATOR',
    nameGu: 'મોડરેટર',
    purpose: 'Community moderation, complaint resolution and member verification.',
    purposeGu: 'સમુદાયનું સંચાલન, ફરિયાદોનો ઉકેલ અને સભ્યોની ચકાસણી.',
  },
  {
    name: 'CONTENT_MANAGER',
    nameGu: 'કન્ટેન્ટ મેનેજર',
    purpose: 'Editorial management of notices, events and gallery content.',
    purposeGu: 'સૂચનાઓ, કાર્યક્રમો અને ગેલેરીની સામગ્રીનું સંપાદકીય સંચાલન.',
  },
  {
    name: 'BUSINESS_OWNER',
    nameGu: 'વ્યવસાય માલિક',
    purpose: 'Manages a verified village business or job listing.',
    purposeGu: 'ચકાસાયેલ ગામના વ્યવસાય અથવા નોકરીની સૂચિનું સંચાલન.',
  },
  {
    name: 'USER',
    nameGu: 'સભ્ય',
    purpose: 'Registered village community member.',
    purposeGu: 'નોંધાયેલ ગામનો સમુદાય સભ્ય.',
  },
];

export default function RolesPage() {
  const { lang } = useAdminLanguage();
  const isGujarati = lang === 'gu';

  return (
    <div className="mx-auto max-w-5xl space-y-6">
      <header className="border-b border-brand-border pb-5">
        <div className="flex items-center gap-2 text-saffron">
          <Shield className="h-5 w-5" />
          <span className="text-xs font-semibold uppercase">
            {isGujarati ? 'પ્રવેશ વ્યવસ્થા' : 'Access directory'}
          </span>
        </div>
        <h1 className="mt-2 text-2xl font-bold text-brand-text font-gujarati">
          {isGujarati ? 'ભૂમિકાઓ અને જવાબદારીઓ' : 'Roles & Permissions'}
        </h1>
        <p className="mt-1 max-w-2xl text-sm leading-6 text-brand-muted">
          {isGujarati
            ? 'આ પૃષ્ઠ પ્રોજેક્ટમાં વ્યાખ્યાયિત સભ્ય અને સ્ટાફ ભૂમિકાઓનો પરિચય આપે છે. વાસ્તવિક પ્રવેશ અધિકારો ડેટાબેઝની સુરક્ષા નીતિઓ દ્વારા નક્કી થાય છે.'
            : 'An overview of member and staff roles defined by the project. Actual access is enforced by the database security policies.'}
        </p>
      </header>

      <div className="flex items-center gap-2 text-sm text-brand-muted">
        <Users className="h-4 w-4" />
        <span>
          {isGujarati
            ? `${roles.length} વ્યાખ્યાયિત ભૂમિકાઓ`
            : `${roles.length} defined roles`}
        </span>
      </div>

      <div className="divide-y divide-brand-border border-y border-brand-border">
        {roles.map((role) => (
          <article key={role.name} className="grid gap-2 py-4 sm:grid-cols-[minmax(12rem,0.8fr)_minmax(0,1.2fr)] sm:gap-6">
            <div>
              <h2 className="font-mono text-sm font-semibold text-brand-text">{role.name}</h2>
              <p className="mt-1 text-sm text-brand-muted">{role.nameGu}</p>
            </div>
            <p className="text-sm leading-6 text-brand-muted">
              {isGujarati ? role.purposeGu : role.purpose}
            </p>
          </article>
        ))}
      </div>
    </div>
  );
}