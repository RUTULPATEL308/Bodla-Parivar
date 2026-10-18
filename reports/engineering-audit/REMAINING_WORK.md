# Remaining work — 2026-10-18

1. Create or identify an isolated Supabase staging project with synthetic data and an approved project reference.
2. Apply and validate all database migrations, especially 031–033; run the SQL policy tests and inspect effective policies/grants.
3. Run the staged k6 progression against that staging target while collecting API/Postgres logs and Database reports.
4. Investigate the notices plan and resource correlation before introducing a database index or RLS change.
5. Track the remaining Expo/Metro/React Native audit findings for a compatible upstream remediation; Expo is now on 58.0.14 and no safe compatible audit fix is currently reported. Then run Android and macOS iOS builds.
6. Resolve the Kotlin/Android Gradle Plugin compatibility warning and Gradle 9 deprecations in a tested upgrade.
7. Review hosted Git secret-scanning alerts and complete historical key-exposure review with a working Git diff configuration.
