import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const target = (__ENV.SUPABASE_URL || '').replace(/\/$/, '');
const publicKey = __ENV.SUPABASE_PUBLISHABLE_KEY || __ENV.SUPABASE_ANON_KEY || '';
const environment = (__ENV.TARGET_ENV || '').toLowerCase();
const profile = (__ENV.TEST_PROFILE || 'load').toLowerCase();
const maxVUs = Number(__ENV.MAX_VUS || 1000);
const hold = __ENV.HOLD_DURATION || '2m';

if (!['local', 'staging'].includes(environment)) {
  throw new Error("PowerShell: set $env:TARGET_ENV='local' for a loopback Supabase instance, or 'staging' with a dedicated staging project. Hosted production/shared targets are blocked.");
}
if (!target || !publicKey) {
  throw new Error('Set SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY for the local or staging project.');
}
if (!['smoke', 'stage', 'load', 'sustained', 'spike', 'stress'].includes(profile)) {
  throw new Error('TEST_PROFILE must be smoke, stage, load, sustained, spike, or stress.');
}
if (!Number.isInteger(maxVUs) || maxVUs < 1 || maxVUs > 1000) {
  throw new Error('MAX_VUS must be a whole number from 1 through 1000.');
}

const parsedTarget = target.match(/^(https?):\/\/([^/:?#]+)(?::\d+)?(?:[/?#]|$)/i);
if (!parsedTarget) throw new Error('SUPABASE_URL must be an HTTP or HTTPS project URL.');
const protocol = parsedTarget[1].toLowerCase();
const host = parsedTarget[2].toLowerCase();
if (environment === 'local' && !['localhost', '127.0.0.1', '::1'].includes(host)) {
  throw new Error('TARGET_ENV=local requires a loopback SUPABASE_URL.');
}
if (host === 'kjoykvxrnyykdnzektoj.supabase.co') {
  throw new Error('This is the shared Bodla project. Use only shared-smoke.k6.js for a limited smoke check.');
}
if (environment === 'staging' && protocol !== 'https') {
  throw new Error('TARGET_ENV=staging requires HTTPS.');
}
if (environment === 'staging') {
  const approvedProjectRef = (__ENV.STAGING_SUPABASE_PROJECT_REF || '').toLowerCase();
  if (!/^[a-z0-9]{20}$/.test(approvedProjectRef) || host !== `${approvedProjectRef}.supabase.co`) {
    throw new Error('TARGET_ENV=staging requires STAGING_SUPABASE_PROJECT_REF to match the approved staging host.');
  }
}

const stagesByProfile = {
  smoke: [
    { duration: '10s', target: 1 },
    { duration: '15s', target: Math.min(5, maxVUs) },
    { duration: '30s', target: Math.min(5, maxVUs) },
    { duration: '15s', target: 0 },
  ],
  stage: [
    { duration: '1m', target: maxVUs },
    { duration: hold, target: maxVUs },
    { duration: '1m', target: 0 },
  ],
  load: [
    { duration: '1m', target: 1 },
    { duration: '1m', target: Math.min(10, maxVUs) },
    { duration: '1m', target: Math.min(25, maxVUs) },
    { duration: '1m', target: Math.min(50, maxVUs) },
    { duration: '1m', target: Math.min(100, maxVUs) },
    { duration: '2m', target: Math.min(250, maxVUs) },
    { duration: '2m', target: Math.min(500, maxVUs) },
    { duration: '2m', target: Math.min(750, maxVUs) },
    { duration: '2m', target: maxVUs },
    { duration: hold, target: maxVUs },
    { duration: '1m', target: 0 },
  ],
  sustained: [
    { duration: '2m', target: maxVUs },
    { duration: hold, target: maxVUs },
    { duration: '2m', target: 0 },
  ],
  spike: [
    { duration: '1m', target: Math.min(25, maxVUs) },
    { duration: '10s', target: maxVUs },
    { duration: '1m', target: maxVUs },
    { duration: '10s', target: Math.min(25, maxVUs) },
    { duration: '1m', target: Math.min(25, maxVUs) },
    { duration: '1m', target: 0 },
  ],
  stress: [
    { duration: '2m', target: Math.min(10, maxVUs) },
    { duration: '2m', target: Math.min(25, maxVUs) },
    { duration: '2m', target: Math.min(50, maxVUs) },
    { duration: '2m', target: Math.min(100, maxVUs) },
    { duration: '3m', target: Math.min(250, maxVUs) },
    { duration: '3m', target: Math.min(500, maxVUs) },
    { duration: '3m', target: Math.min(750, maxVUs) },
    { duration: '5m', target: maxVUs },
    { duration: '2m', target: 0 },
  ],
};

export const options = {
  discardResponseBodies: true,
  scenarios: {
    residents: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: stagesByProfile[profile],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<2000', 'p(99)<5000'],
    'http_req_duration{endpoint:notices}': ['p(95)<2000', 'p(99)<5000'],
    'http_req_duration{endpoint:events}': ['p(95)<2000', 'p(99)<5000'],
    'http_req_duration{endpoint:emergency_contacts}': ['p(95)<2000', 'p(99)<5000'],
  },
};

const headers = { apikey: publicKey, Accept: 'application/json' };
const httpStatusCodes = new Counter('http_status_codes');
const httpErrorCodes = new Counter('http_error_codes');
const endpoints = [
  ['notices', `${target}/rest/v1/notices?select=id&limit=5`],
  ['events', `${target}/rest/v1/events?select=id&limit=5`],
  ['emergency_contacts', `${target}/rest/v1/emergency_contacts?select=id&limit=5`],
];

export default function publicReadScenario() {
  for (const [name, url] of endpoints) {
    const response = http.get(url, {
      headers,
      tags: { endpoint: name },
      timeout: '10s',
    });
    check(response, {
      [`${name} returns HTTP 200`]: (result) => result.status === 200,
    });
    httpStatusCodes.add(1, { endpoint: name, status: String(response.status || 0) });
    httpErrorCodes.add(1, { endpoint: name, error_code: String(response.error_code || 0) });
  }
  // Model a resident loading the portal and then reading it for about half a minute.
  sleep(30);
}
