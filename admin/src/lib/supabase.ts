import { createClient } from '@supabase/supabase-js';
import type { SupabaseClient } from '@supabase/supabase-js';

const supabaseUrl =
  process.env.NEXT_PUBLIC_SUPABASE_URL ||
  'https://demo-placeholder.supabase.co';

const supabaseAnonKey =
  process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY ||
  'demo-anon-key';

export const isSupabaseConfigured = Boolean(
  process.env.NEXT_PUBLIC_SUPABASE_URL &&
  process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY
);

const globalForSupabase = globalThis as typeof globalThis & {
  bodlaSupabaseClient?: SupabaseClient;
};

export const supabase = globalForSupabase.bodlaSupabaseClient ?? createClient(supabaseUrl, supabaseAnonKey, {
  auth: {
    autoRefreshToken: true,
    persistSession: true,
    detectSessionInUrl: true,
    flowType: 'implicit',
  },
});

if (process.env.NODE_ENV !== 'production') {
  globalForSupabase.bodlaSupabaseClient = supabase;
}
