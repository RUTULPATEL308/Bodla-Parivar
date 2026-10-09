const secretKey = 'sb_secret_z3S-VZzBlrTs_NLNAwjdPw_U1tb57oE';
const baseUrl = 'https://kjoykvxrnyykdnzektoj.supabase.co';

const statements = [
  // Realtime
  `ALTER PUBLICATION supabase_realtime ADD TABLE IF NOT EXISTS offerings`,
  `ALTER PUBLICATION supabase_realtime ADD TABLE IF NOT EXISTS notices`,
  `ALTER PUBLICATION supabase_realtime ADD TABLE IF NOT EXISTS events`,
  `ALTER PUBLICATION supabase_realtime ADD TABLE IF NOT EXISTS businesses`,

  // OFFERINGS: open read/insert/update for anon + authenticated
  `DROP POLICY IF EXISTS "Active offerings are viewable by all" ON offerings`,
  `DROP POLICY IF EXISTS "Users can view own offerings" ON offerings`,
  `DROP POLICY IF EXISTS "Users can submit offerings" ON offerings`,
  `DROP POLICY IF EXISTS "Staff can manage offerings" ON offerings`,
  `DROP POLICY IF EXISTS "Allow portal select offerings" ON offerings`,
  `CREATE POLICY "Allow portal select offerings" ON offerings FOR SELECT TO anon, authenticated USING (true)`,
  `DROP POLICY IF EXISTS "Allow portal insert offerings" ON offerings`,
  `CREATE POLICY "Allow portal insert offerings" ON offerings FOR INSERT TO anon, authenticated WITH CHECK (true)`,
  `DROP POLICY IF EXISTS "Allow portal update offerings" ON offerings`,
  `CREATE POLICY "Allow portal update offerings" ON offerings FOR UPDATE TO anon, authenticated USING (true) WITH CHECK (true)`,

  // NOTICES: open read/insert
  `DROP POLICY IF EXISTS "Published notices are viewable by all" ON notices`,
  `DROP POLICY IF EXISTS "Staff can manage notices" ON notices`,
  `DROP POLICY IF EXISTS "Allow portal select notices" ON notices`,
  `CREATE POLICY "Allow portal select notices" ON notices FOR SELECT TO anon, authenticated USING (true)`,
  `DROP POLICY IF EXISTS "Allow portal insert notices" ON notices`,
  `CREATE POLICY "Allow portal insert notices" ON notices FOR INSERT TO anon, authenticated WITH CHECK (true)`,

  // EVENTS: open read/insert
  `DROP POLICY IF EXISTS "Allow portal select events" ON events`,
  `CREATE POLICY "Allow portal select events" ON events FOR SELECT TO anon, authenticated USING (true)`,
  `DROP POLICY IF EXISTS "Allow portal insert events" ON events`,
  `CREATE POLICY "Allow portal insert events" ON events FOR INSERT TO anon, authenticated WITH CHECK (true)`,

  // EMERGENCY CONTACTS: open read
  `DROP POLICY IF EXISTS "Emergency contacts are publicly viewable" ON emergency_contacts`,
  `DROP POLICY IF EXISTS "Allow portal select emergency_contacts" ON emergency_contacts`,
  `CREATE POLICY "Allow portal select emergency_contacts" ON emergency_contacts FOR SELECT TO anon, authenticated USING (true)`,

  // VILLAGE SETTINGS: open read
  `DROP POLICY IF EXISTS "Village settings are publicly readable" ON village_settings`,
  `DROP POLICY IF EXISTS "Allow portal select village_settings" ON village_settings`,
  `CREATE POLICY "Allow portal select village_settings" ON village_settings FOR SELECT TO anon, authenticated USING (true)`,

  // OFFERING CATEGORIES: open read
  `DROP POLICY IF EXISTS "Offering categories are publicly readable" ON offering_categories`,
  `DROP POLICY IF EXISTS "Allow portal select offering_categories" ON offering_categories`,
  `CREATE POLICY "Allow portal select offering_categories" ON offering_categories FOR SELECT TO anon, authenticated USING (true)`,

  // NOTICE CATEGORIES: open read
  `DROP POLICY IF EXISTS "Notice categories are publicly readable" ON notice_categories`,
  `DROP POLICY IF EXISTS "Allow portal select notice_categories" ON notice_categories`,
  `CREATE POLICY "Allow portal select notice_categories" ON notice_categories FOR SELECT TO anon, authenticated USING (true)`,

  // EVENT CATEGORIES: open read
  `DROP POLICY IF EXISTS "Allow portal select event_categories" ON event_categories`,
  `CREATE POLICY "Allow portal select event_categories" ON event_categories FOR SELECT TO anon, authenticated USING (true)`,

  // BUSINESSES: open read
  `DROP POLICY IF EXISTS "Approved businesses are viewable by all" ON businesses`,
  `DROP POLICY IF EXISTS "Allow portal select businesses" ON businesses`,
  `CREATE POLICY "Allow portal select businesses" ON businesses FOR SELECT TO anon, authenticated USING (true)`,

  // BUSINESS CATEGORIES: open read
  `DROP POLICY IF EXISTS "Business categories are publicly readable" ON business_categories`,
  `DROP POLICY IF EXISTS "Allow portal select business_categories" ON business_categories`,
  `CREATE POLICY "Allow portal select business_categories" ON business_categories FOR SELECT TO anon, authenticated USING (true)`,
];

async function runAll() {
  let success = 0;
  let failed = 0;
  for (const stmt of statements) {
    try {
      const resp = await fetch(baseUrl + '/rest/v1/rpc/exec_sql', {
        method: 'POST',
        headers: {
          'apikey': secretKey,
          'Authorization': 'Bearer ' + secretKey,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ query: stmt })
      });
      if (resp.status === 200) {
        success++;
      } else {
        const body = await resp.text();
        console.log('FAILED (' + resp.status + '): ' + stmt.substring(0, 70));
        console.log('  -> ' + body.substring(0, 150));
        failed++;
      }
    } catch(e) {
      console.log('ERROR: ' + e.message);
      failed++;
    }
  }
  console.log('\nDone! Success: ' + success + ', Failed: ' + failed);

  // Now verify
  console.log('\n--- Verification ---');
  const anonKey = 'sb_publishable_PNb4_Epkaqv-J1KdC0x7dQ_-vxdAt-d';

  const readResp = await fetch(baseUrl + '/rest/v1/offerings?select=id,title_en,status', {
    headers: { 'apikey': anonKey, 'Authorization': 'Bearer ' + anonKey }
  });
  const readData = await readResp.json();
  console.log('Anon READ offerings:', readData.length, 'rows', JSON.stringify(readData));

  const insertResp = await fetch(baseUrl + '/rest/v1/offerings', {
    method: 'POST',
    headers: {
      'apikey': anonKey,
      'Authorization': 'Bearer ' + anonKey,
      'Content-Type': 'application/json',
      'Prefer': 'return=representation'
    },
    body: JSON.stringify({
      title_gu: 'વેબ પોર્ટલ ટેસ્ટ ચઢાવો',
      title_en: 'Web Portal Test Offering',
      status: 'ACTIVE',
      amount: 251
    })
  });
  console.log('Anon INSERT:', insertResp.status, await insertResp.text());
}

runAll();
