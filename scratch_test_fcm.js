const { initializeApp, cert } = require('firebase-admin/app');
const { getMessaging } = require('firebase-admin/messaging');
const fs = require('fs');

const sa = JSON.parse(fs.readFileSync('D:/Project-Work/BODLA-PARIVVAR/composeApp/bodla-fcm-sa.json', 'utf8'));

const app = initializeApp({
  credential: cert(sa)
});

async function run() {
  console.log('Sending test message to all_users topic...');
  // DATA-ONLY — no "notification" key so onMessageReceived() fires even when app is background/killed
  const msg = {
    topic: 'all_users',
    data: {
      title_gu: 'બોદલા ગામના તમામ સ્નેહીજનોને સપ્રેમ નમસ્તે.',
      body_gu: 'આ એક ચકાસણી (ટેસ્ટ-૩) સંદેશ છે, જેથી આપણી ગામની માહિતી સેવા યોગ્ય રીતે કાર્ય કરી છે કે નહીં તેની ખાતરી કરી શકાય.',
      type: 'NOTICE',
      reference_id: '',
      image_url: '',
      sender_profile_id: '',
      notification_id: '',
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
    console.log('FCM Success messageId:', res);
  } catch (e) {
    console.error('FCM Error:', e);
  }
}

run();
