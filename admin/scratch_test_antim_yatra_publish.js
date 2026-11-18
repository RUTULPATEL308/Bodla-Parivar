const { initializeApp, cert, getApps } = require('firebase-admin/app');
const { getMessaging } = require('firebase-admin/messaging');
const https = require('https');
const fs = require('fs');

const sa = JSON.parse(fs.readFileSync('D:/Project-Work/BODLA-PARIVVAR/composeApp/bodla-fcm-sa.json', 'utf8'));

const app = getApps().length === 0 ? initializeApp({
  credential: cert(sa)
}) : getApps()[0];

const SUPABASE_URL = 'https://kjoykvxrnyykdnzektoj.supabase.co';
const ANON_KEY = 'sb_publishable_PNb4_Epkaqv-J1KdC0x7dQ_-vxdAt-d';

async function run() {
  console.log('1. Inserting test Antim Yatra / Besanu notice into Supabase...');

  const noticeData = {
    title_gu: '🕊️ અંજલિ / બેસણું - સ્વ. મંગળભાઈ બોદલા',
    title_en: 'Besanu Notice - Mangalbhai Bodla',
    description_gu: 'આથી બોદલા ગામના તમામ સ્નેહીજનોને જણાવવામાં આવે છે કે આપણા વ્હાલા સ્વ. મંગળભાઈ પુંજાભાઈ બોદલાનું દુઃખદ અવસાન થયેલ છે. તેમનું બેસણું તેમના નિવાસસ્થાને રાખવામાં આવેલ છે. સૌ સગા-સંબંધીઓએ નોંધ લેવી.',
    description_en: 'Condolence and Besanu notice for late Mangalbhai Bodla.',
    status: 'PUBLISHED',
    is_pinned: true,
    publish_at: new Date().toISOString(),
    image_url: 'https://images.unsplash.com/photo-1544717305-2782549b5136?w=600'
  };

  const url = new URL(`${SUPABASE_URL}/rest/v1/notices`);
  const req = https.request(url, {
    method: 'POST',
    headers: {
      'apikey': ANON_KEY,
      'Authorization': `Bearer ${ANON_KEY}`,
      'Content-Type': 'application/json',
      'Prefer': 'return=representation'
    }
  }, (res) => {
    let body = '';
    res.on('data', chunk => body += chunk);
    res.on('end', async () => {
      console.log('Notice insert response:', res.statusCode, body);
      try {
        const json = JSON.parse(body);
        const notice = Array.isArray(json) ? json[0] : json;
        const noticeId = notice?.id;

        console.log('2. Sending FCM push notification with poster image for notice ID:', noticeId);

        const msg = {
          topic: 'all_users',
          data: {
            title_gu: noticeData.title_gu,
            title_en: noticeData.title_en,
            body_gu: noticeData.description_gu,
            body_en: noticeData.description_en,
            type: 'NOTICE',
            reference_id: noticeId || 'test_antim_yatra_123',
            image_url: noticeData.image_url,
            sender_profile_id: '',
            notification_id: 'test_antim_yatra_notif_' + Date.now(),
          },
          android: {
            priority: 'high'
          },
          apns: {
            payload: { aps: { 'content-available': 1, sound: 'default' } },
            headers: { 'apns-priority': '10' },
          },
        };

        const fcmRes = await getMessaging(app).send(msg);
        console.log('Antim Yatra FCM Push Success messageId:', fcmRes);
      } catch (e) {
        console.error('Error in publish test:', e);
      }
    });
  });

  req.on('error', e => console.error('Supabase insert error:', e));
  req.write(JSON.stringify(noticeData));
  req.end();
}

run();
