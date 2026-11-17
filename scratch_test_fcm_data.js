const { initializeApp, cert } = require('firebase-admin/app');
const { getMessaging } = require('firebase-admin/messaging');
const fs = require('fs');

const sa = JSON.parse(fs.readFileSync('D:/Project-Work/BODLA-PARIVVAR/composeApp/bodla-fcm-sa.json', 'utf8'));

const app = initializeApp({
  credential: cert(sa)
});

async function run() {
  console.log('Sending DATA-ONLY message to all_users topic...');
  const msg = {
    topic: 'all_users',
    data: {
      title_gu: 'બોદલા ગામના તમામ સ્નેહીજનોને સપ્રેમ નમસ્તે.',
      body_gu: 'આ માત્ર ડેટા મેસેજ છે જેથી એન્ડ્રોઇડ સિસ્ટમ વચ્ચે ન આવે અને આપણી એપ સીધી એક્ઝિક્યુટ થાય.',
      type: 'NOTICE'
    },
    android: {
      priority: 'high'
    }
  };

  try {
    const res = await getMessaging(app).send(msg);
    console.log('Data-only FCM Success messageId:', res);
  } catch (e) {
    console.error('FCM Error:', e);
  }
}

run();
