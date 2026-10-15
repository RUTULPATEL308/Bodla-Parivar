import http from 'k6/http';
import { check, sleep } from 'k6';

const target = (__ENV.SUPABASE_URL || '').replace(/\/$/, '');
const publicKey = __ENV.SUPABASE_PUBLISHABLE_KEY || __ENV.SUPABASE_ANON_KEY || '';
const environment = (__ENV.TARGET_ENV || '').toLowerCase();

if (!['local', 'staging'].includes(environment)) {
  throw new Error('Set TARGET_ENV=local or staging. Production load tests are blocked by this script.');
}
if (!target || !publicKey) {
  throw new Error('Set SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY for the local or staging project.');
}

const targetUrl = new URL(target);
const localHosts = ['localhost', '127.0.0.1', '::1'];
if (environment === 'local' && !localHosts.includes(targetUrl.hostname)) {
  throw new Error('TARGET_ENV=local requires a loopback SUPABASE_URL.');
}
if (targetUrl.hostname === 'kjoykvxrnyykdnzektoj.supabase.co') {
  throw new Error('This is the shared hosted project. Create a staging Supabase project before load testing.');
}
if (environment === 'staging' && targetUrl.protocol !== 'https:') {
  throw new Error('TARGET_ENV=staging requires an HTTPS SUPABASE_URL.');
}

export const options = {
  discardResponseBodies: true,
  scenarios: {
    village_residents: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 100 },
        { duration: '3m', target: 1000 },
        { duration: '4m', target: 3000 },
        { duration: '10m', target: 3000 },
        { duration: '2m', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<2000', 'p(99)<5000'],
  },
};

const headers = {
  apikey: publicKey,
  Accept: 'application/json',
};

export default function publicReadScenario() {
  const results = http.batch([
    ['GET', `${target}/rest/v1/notices?select=id&limit=5`, null, { headers, tags: { resource: 'notices' } }],
    ['GET', `${target}/rest/v1/events?select=id&limit=5`, null, { headers, tags: { resource: 'events' } }],
    ['GET', `${target}/rest/v1/emergency_contacts?select=id&limit=5`, null, { headers, tags: { resource: 'emergency_contacts' } }],
  ]);

  check(results[0], { 'notices read succeeds': (response) => response.status === 200 });
  check(results[1], { 'events read succeeds': (response) => response.status === 200 });
  check(results[2], { 'emergency contacts read succeeds': (response) => response.status === 200 });
  // Model users who load their dashboard, then spend time reading it.
  // Three public reads per user every 30 seconds is about 300 requests/second
  // at the 3,000-VU plateau, instead of overwhelming the service with a tight loop.
  sleep(30);
}
