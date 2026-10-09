const { Client } = require('pg');

async function main() {
  const connectionString = 'postgresql://postgres:Rutul%409824470026@db.kjoykvxrnyykdnzektoj.supabase.co:5432/postgres';
  const client = new Client({ connectionString });
  
  try {
    await client.connect();
    console.log('Connected to Supabase DB successfully.');
    
    // Check if offerings table exists
    const res = await client.query(`
      SELECT EXISTS (
        SELECT FROM information_schema.tables 
        WHERE table_schema = 'public' 
        AND table_name = 'offerings'
      );
    `);
    
    if (res.rows[0].exists) {
      // Insert a testing offering
      const insertQuery = `
        INSERT INTO offerings (id, title_gu, title_en, description_gu, description_en, amount, quantity, status, location_gu, location_en) 
        VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
        ON CONFLICT (id) DO UPDATE SET title_en = EXCLUDED.title_en
      `;
      await client.query(insertQuery, [
        'test-label-' + Date.now(),
        'ટેસ્ટિંગ લેબલ - ડેટાબેઝ જોડાયેલ છે',
        'TESTING LABEL - DB Connected!',
        'આ માત્ર ડેટાબેઝ કનેક્શન તપાસવા માટે છે.',
        'This is just to verify the database connection from both Web and Mobile apps.',
        101,
        1,
        'ACTIVE',
        'ટેસ્ટ લોકેશન',
        'Test Location'
      ]);
      console.log('Testing label inserted into offerings table successfully!');
    } else {
      console.log('Table "offerings" does not exist yet. Please run the SQL schema migrations.');
    }
    
  } catch (err) {
    console.error('Error connecting to DB or inserting:', err.message);
  } finally {
    await client.end();
  }
}

main();
