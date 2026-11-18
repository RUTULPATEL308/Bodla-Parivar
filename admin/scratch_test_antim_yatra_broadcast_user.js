const { createClient } = require('@supabase/supabase-js');
const fs = require('fs');

const SUPABASE_URL = 'https://kjoykvxrnyykdnzektoj.supabase.co';
const ANON_KEY = 'sb_publishable_PNb4_Epkaqv-J1KdC0x7dQ_-vxdAt-d';

async function run() {
  console.log('Testing Antim Yatra / Besanu notice publishing from user perspective...');

  // Create Supabase client
  const supabase = createClient(SUPABASE_URL, ANON_KEY);

  // Sign in as user
  const { data: authData, error: authError } = await supabase.auth.signInWithPassword({
    email: 'user1@bodla.com', // or test user
    password: 'password123'
  });

  const token = authData?.session?.access_token;
  console.log('User signed in, token present:', Boolean(token));

  if (!token) {
    console.log('Fallback: Testing POST payload directly to endpoint validation...');
  }

  // Payload sent by AntimYatraBroadcast.kt or BesanuBroadcast.kt
  const payload = {
    title_gu: '🕊️ અંજલિ / બેસણું - સ્વ. રામજીભાઈ',
    title_en: 'Besanu Notice - Ramjibhai',
    notification_title_gu: '🕊️ અંજલિ / બેસણું - સ્વ. રામજીભાઈ',
    notification_title_en: 'Besanu Notice - Ramjibhai',
    body_gu: 'આપણા વ્હાલા સ્વ. રામજીભાઈનું બેસણું તેમના નિવાસસ્થાને રાખવામાં આવેલ છે. સૌ સ્નેહીજનોએ નોંધ લેવી.',
    body_en: 'Besanu notice for late Ramjibhai.',
    description_gu: 'આપણા વ્હાલા સ્વ. રામજીભાઈનું બેસણું તેમના નિવાસસ્થાને રાખવામાં આવેલ છે. સૌ સ્નેહીજનોએ નોંધ લેવી.',
    description_en: 'Besanu notice for late Ramjibhai.',
    image_url: 'https://kjoykvxrnyykdnzektoj.supabase.co/storage/v1/object/public/notices/sample_poster.png',
    type: 'NOTICE',
    is_besanu: true
  };

  console.log('Payload:', payload);
}

run();
