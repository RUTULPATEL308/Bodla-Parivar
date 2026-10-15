# Public read load test

This k6 scenario exercises anonymous REST reads for notices, events, and emergency contacts. It ramps up to 3,000 virtual users, holds for 10 minutes, and then ramps down. Each virtual user loads those three resources once and waits 30 seconds before the next read cycle. At the 3,000-user plateau, that models about 300 requests per second when responses are fast. It does not test sign-in, admin writes, or rendering in the web/mobile clients.

## Prepare a safe target

1. Create a separate Supabase project for staging. Do not point this test at the production project.
2. Apply the SQL files in `database/migrations` to staging in order and seed only non-sensitive test data.
3. In the Supabase Dashboard, get the Project URL and publishable key from the project's Connect dialog or **Settings → API Keys**.
4. Confirm staging has capacity for the planned run and that all three public read endpoints respond successfully.

## Run from PowerShell

Install k6, then from the repository root set the staging values in the current shell and run:

```powershell
$env:TARGET_ENV = 'staging'
$env:SUPABASE_URL = 'https://YOUR-STAGING-PROJECT.supabase.co'
$env:SUPABASE_PUBLISHABLE_KEY = 'YOUR-STAGING-PUBLISHABLE-KEY'
k6 run .\admin\load-tests\public-read.k6.js
```

The script rejects the configured shared project, requires a loopback host for `TARGET_ENV=local`, and requires HTTPS for staging. Never place a secret/service-role key in this test.
