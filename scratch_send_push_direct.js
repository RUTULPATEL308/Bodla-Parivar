const { initializeApp, cert, getApps } = require('firebase-admin/app');
const { getMessaging } = require('firebase-admin/messaging');
const fs = require('fs');

const sa = JSON.parse(fs.readFileSync('D:/Project-Work/BODLA-PARIVVAR/composeApp/bodla-fcm-sa.json', 'utf8'));

const app = getApps().length === 0 ? initializeApp({
  credential: cert(sa)
}) : getApps()[0];

async function run() {
  console.log('Sending live alert push notification to all_users topic...');
  const msg = {
    topic: 'all_users',
    data: {
      title_gu: '📢 બોદલા પરિવાર - મહત્વની સૂચના',
      title_en: 'Important Village Notice',
      body_gu: 'આથી બોદલા ગામના તમામ ગ્રામજનોને જણાવવામાં આવે છે કે નવી સૂચના મંજૂર કરવામાં આવી છે. આપ સૌએ યોગ્ય નોંધ લેવી.',
      body_en: 'Notice has been approved and published for all members.',
      type: 'NOTICE',
      reference_id: 'd11b6477-3999-41ac-98c3-00f76a11f922',
      image_url: '',
      sender_profile_id: '',
      notification_id: 'test_notif_1',
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
    console.log('FCM Push Success messageId:', res);
  } catch (e) {
    console.error('FCM Push Error:', e);
  }
}

run();
