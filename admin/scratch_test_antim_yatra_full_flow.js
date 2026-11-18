const { initializeApp, cert, getApps } = require('firebase-admin/app');
const { getMessaging } = require('firebase-admin/messaging');
const fs = require('fs');

const sa = JSON.parse(fs.readFileSync('D:/Project-Work/BODLA-PARIVVAR/composeApp/bodla-fcm-sa.json', 'utf8'));

const app = getApps().length === 0 ? initializeApp({
  credential: cert(sa)
}) : getApps()[0];

async function run() {
  console.log('Testing full Antim Yatra poster broadcast with image...');

  // Sample Antim Yatra poster image URL uploaded during creation
  const samplePosterUrl = 'https://images.unsplash.com/photo-1544717305-2782549b5136?w=600';

  const titleGu = '🕯️ અંતિમ યાત્રા - સ્વ. રામજીભાઈ મોહનભાઈ બોદલા';
  const bodyGu = 'અત્યંત દુઃખ સાથે જણાવવાનું કે આપણા પંચના વડીલ સ્વ. રામજીભાઈ મોહનભાઈ બોદલાનું સ્વર્ગવાસ થયેલ છે. તેમની અંતિમ યાત્રા આજે સાંજે ૫:૦૦ કલાકે તેમના નિવાસસ્થાનેથી નીકળશે. સૌ ગ્રામજનોને ઉપસ્થિત રહેવા નમ્ર વિનંતી.';

  const msg = {
    topic: 'all_users',
    data: {
      title_gu: titleGu,
      title_en: 'Antim Yatra Notice - Ramjibhai Bodla',
      body_gu: bodyGu,
      body_en: 'Antim Yatra notice for late Ramjibhai Mohanbhai Bodla.',
      type: 'NOTICE',
      reference_id: 'antim_yatra_sample_101',
      image_url: samplePosterUrl,
      sender_profile_id: '',
      notification_id: 'antim_yatra_notif_' + Date.now(),
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
    console.log('Antim Yatra Poster Push Notification SUCCESS:', res);
  } catch (e) {
    console.error('Antim Yatra Push Error:', e);
  }
}

run();
