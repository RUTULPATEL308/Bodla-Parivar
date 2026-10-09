# Contributing Guidelines

Thank you for contributing to **બોદલા પરિવાર (Bodla Parivar)**. To maintain high code quality, security, and cultural integrity, all contributors must follow these guidelines.

---

## 1. Non-Negotiable Project Rules

1. **App Name**: Always use exact name **બોદલા પરિવાર** (English: **Bodla Parivar**).
2. **Gujarati Spelling**: Always spell **બોદલા** (Bodla). Never use phonetic butchering or transliteration in place of real Unicode Gujarati.
3. **Offerings / ચઢાવો**: Never refer to, model, or label this feature as an **Auction** or use bidding terminology.
4. **Zero Secrets in Git**: Never commit API keys, `.env` files, or service role credentials.
5. **No Invented Real Data**: Never invent fake people, fake businesses, or fake emergency contacts. Use clear `DEMO DATA` markers for development placeholders.

---

## 2. Git Branching Model

- `main`: Production release branch. Protected.
- `develop`: Integration branch for tested features.
- `feature/<feature-name>`: Dedicated branches for new features (e.g. `feature/fcm-push`).
- `bugfix/<issue-name>`: Targeted bug fixes.
- `release/<version>`: Release staging and testing.

---

## 3. Conventional Commit Messages

Follow the Conventional Commits specification:
```text
feat: add bilingual localization support
feat: add notices module
feat: add events calendar
feat: add business directory
feat: add offerings module
feat: add complaints tracking
feat: add offline sync engine
feat: add admin dashboard

fix: prevent unauthorized profile access
fix: improve offline SQLite sync retry backoff
fix: validate offering permissions in RLS
```

---

## 4. Coding Standards

- **Kotlin**: Follow official Kotlin coding conventions. Use Compose Multiplatform declarative UI principles without nested recomposition leaks.
- **TypeScript**: Strict type checking enabled (`strict: true`). No `any` types in production models.
- **Database**: All schema changes must be accompanied by numbered SQL migration files under `database/migrations/`.
