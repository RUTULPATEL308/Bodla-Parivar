const https = require('https');

const SUPABASE_URL = 'https://kjoykvxrnyykdnzektoj.supabase.co';
const ANON_KEY = 'sb_publishable_PNb4_Epkaqv-J1KdC0x7dQ_-vxdAt-d';

function request(method, path, data = null) {
  return new RegExp('^https?://').test(path) ? null : new Promise((resolve, reject) => {
    // handled below
  });
}

async function run() {
  console.log('1. Fetching pending notices from Supabase...');

  const url = new URL(`${SUPABASE_URL}/rest/v1/notices?status=eq.PENDING`);
  const options = {
    method: 'GET',
    headers: {
      'apikey': ANON_KEY,
      'Authorization': `Bearer ${ANON_KEY}`,
      'Content-Type': 'application/json'
    }
  };

  const req = https.request(url, options, (res) => {
    let body = '';
    res.on('data', chunk => body += chunk);
    res.on('end', async () => {
      try {
        const notices = JSON.parse(body);
        console.log(`Found ${notices.length} pending notices:`, notices);
        if (notices.length > 0) {
          const notice = notices[0];
          console.log(`Approving notice ID: ${notice.id} (${notice.title_gu})`);

          // Approve notice
          const patchUrl = new URL(`${SUPABASE_URL}/rest/v1/notices?id=eq.${notice.id}`);
          const patchReq = https.request(patchUrl, {
            method: 'PATCH',
            headers: {
              'apikey': ANON_KEY,
              'Authorization': `Bearer ${ANON_KEY}`,
              'Content-Type': 'application/json',
              'Prefer': 'return=representation'
            }
          }, (patchRes) => {
            let patchBody = '';
            patchRes.on('data', chunk => patchBody += chunk);
            patchRes.on('end', async () => {
              console.log('Notice approval response:', patchRes.statusCode, patchBody);

              // Now trigger send-notification function
              console.log('Sending push notification for approved notice...');
              const notifyUrl = new URL(`${SUPABASE_URL}/functions/v1/send-notification`);
              const notifyReq = https.request(notifyUrl, {
                method: 'POST',
                headers: {
                  'apikey': ANON_KEY,
                  'Authorization': `Bearer ${ANON_KEY}`,
                  'Content-Type': 'application/json'
                }
              }, (notifyRes) => {
                let notifyBody = '';
                notifyRes.on('data', chunk => notifyBody += chunk);
                notifyRes.on('end', () => {
                  console.log('Send notification response:', notifyRes.statusCode, notifyBody);
                });
              });
              notifyReq.on('error', e => console.error('Notify error:', e));
              notifyReq.write(JSON.stringify({
                title_gu: notice.title_gu,
                title_en: notice.title_en,
                body_gu: notice.description_gu,
                body_en: notice.description_en,
                type: 'NOTICE',
                reference_id: notice.id,
                image_url: notice.image_url || undefined,
                attachment_url: notice.attachment_url || undefined
              }));
              notifyReq.end();
            });
          });
          patchReq.on('error', e => console.error('Patch error:', e));
          patchReq.write(JSON.stringify({
            status: 'PUBLISHED',
            publish_at: new Date().toISOString()
          }));
          patchReq.end();
        } else {
          console.log('No pending notices found.');
        }
      } catch (e) {
        console.error('Parse error:', e, body);
      }
    });
  });
  req.on('error', e => console.error('Request error:', e));
  req.end();
}

run();
