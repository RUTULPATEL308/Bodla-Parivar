const net = require('net');

const client = new net.Socket();
client.connect(5432, 'db.kjoykvxrnyykdnzektoj.supabase.co', () => {
  console.log('Connected to Supabase DB port 5432!');
  client.destroy();
});

client.on('error', (err) => {
  console.log('Port 5432 error:', err.message);
  // Try transaction pooler port 6543
  const pooler = new net.Socket();
  pooler.connect(6543, 'db.kjoykvxrnyykdnzektoj.supabase.co', () => {
    console.log('Connected to Supabase DB pooler port 6543!');
    pooler.destroy();
  });
  pooler.on('error', (e) => console.log('Port 6543 error:', e.message));
});
