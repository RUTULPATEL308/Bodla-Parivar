import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const target = (__ENV.SUPABASE_URL || '').replace(/\/$/, '');
const publicKey = __ENV.SUPABASE_PUBLISHABLE_KEY || __ENV.SUPABASE_ANON_KEY || '';
const environment = (__ENV.TARGET_ENV || '').toLowerCase();

if (environment !== 'shared-smoke') {
  throw new Error('Set TARGET_ENV=shared-smoke to run this rate-limited, read-only smoke check.');
}
if (!target || !publicKey) {
  throw new Error('Set SUPABASE_URL and SUPABASE_PUBLISHABLE_KEY (or SUPABASE_ANON_KEY).');
}

const parsedTarget = target.match(/^https:\/\/([^/:?#]+)(?::\d+)?(?:[/?#]|$)/i);
if (!parsedTarget || parsedTarget[1].toLowerCase() !== 'kjoykvxrnyykdnzektoj.supabase.co') {
  throw new Error('This smoke check is pinned to the configured Bodla Parivar project over HTTPS.');
}

export const options = {
  discardResponseBodies: true,
  scenarios: {
    limited_smoke: {
      executor: 'per-vu-iterations',
      vus: 200,
      iterations: 2,
      maxDuration: '3m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{endpoint:notices}': ['p(95)<2000'],
    'http_req_duration{endpoint:events}': ['p(95)<2000'],
    'http_req_duration{endpoint:emergency_contacts}': ['p(95)<2000'],
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

export default function sharedReadSmoke() {
  for (const [name, url] of endpoints) {
    const response = http.get(url, {
      headers,
      tags: { endpoint: name },
      timeout: '10s',
    });
    httpStatusCodes.add(1, { endpoint: name, status: String(response.status || 0) });
    httpErrorCodes.add(1, {
      endpoint: name,
      error_code: String(response.error_code || 0),
    });
    const passed = check(response, {
      [`${name} returns HTTP 200`]: (result) => result.status === 200,
    });
    if (!passed) {
      console.error(`${name} returned HTTP ${response.status || 'no response'}`);
    }
    sleep(2);
  }
}
