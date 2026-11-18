const { initializeApp, cert, getApps } = require('firebase-admin/app');
const { getMessaging } = require('firebase-admin/messaging');
const fs = require('fs');

const sa = JSON.parse(fs.readFileSync('D:/Project-Work/BODLA-PARIVVAR/composeApp/bodla-fcm-sa.json', 'utf8'));

const app = getApps().length === 0 ? initializeApp({
  credential: cert(sa)
}) : getApps()[0];

async function run() {
  console.log('Sending test Antim Yatra / Besanu push notification with image to all_users topic...');
  const msg = {
    topic: 'all_users',
    data: {
      title_gu: '🕊️ બેસણું / અંજલિ - શ્રીમતી કાશીબેન',
      title_en: 'Besanu Notice - Kashiben',
      body_gu: 'આપણા સ્વર્ગસ્થ કાશીબેનનું બેસણું રાખવામાં આવેલ છે. સૌ સ્નેહીજનોએ નોંધ લેવી.',
      body_en: 'Besanu notice for late Kashiben.',
      type: 'NOTICE',
      reference_id: 'test_besanu_123',
      image_url: 'https://images.unsplash.com/photo-1544717305-2782549b5136?w=600',
      sender_profile_id: '',
      notification_id: 'test_besanu_notif_1',
    },
    android: {
      priority: 'high'
    },
    apns: {
      payload: { aps: { 'content-available': 1, sound: 'default' } },
      headers: { 'apns-priority': '10' },
    },
  };

  try {
    const res = await getMessaging(app).send(msg);
    console.log('Antim Yatra / Besanu FCM Push Success messageId:', res);
  } catch (e) {
    console.error('Antim Yatra / Besanu FCM Push Error:', e);
  }
}

run();
