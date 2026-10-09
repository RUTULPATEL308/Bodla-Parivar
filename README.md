# બોદલા પરિવાર — Bodla Parivar

> **A trusted digital home for the people of બોદલા (Bodla), Mehsana, Gujarat, India.**
> 
> *ગુજરાતી સંસ્કૃતિ + ગામની ઓળખ + સમુદાય + પરિવાર + આધુનિક ટેકનોલોજી*

---

## 🏛️ Project Overview

**બોદલા પરિવાર** is a production-grade, multiplatform digital community application designed for the village **બોદલા (Bodla), Mehsana, Gujarat**. Built with an offline-first architecture, it serves as the central hub connecting village residents, administration, local businesses, and community initiatives.

---

## 🎨 Brand Identity & Design System

- **App Name**: **બોદલા પરિવાર** (English: **Bodla Parivar**)
- **Village Spelling**: **બોદલા**
- **Typography**:
  - Gujarati: **Anek Gujarati** (first-class Unicode, 16sp+ for body text)
  - English & Numerals: **DM Sans**
- **Color Palette**:
  - Primary Saffron: `#D97706`
  - Secondary Leaf Green: `#4F7D4A`
  - Accent Golden: `#E9A23B`
  - Background Cream: `#FFF8E7`
  - Surface Warm White: `#FFFCF5`
  - Primary Text: `#292524`
  - Secondary Text: `#78716C`
  - Border: `#E7E5E4`
  - Icon Container: `#FEF3C7` (48dp × 48dp, 14dp radius)
- **Icons**: Material Symbols Rounded outline style in `#D97706`.

---

## 📱 Technology Stack

### Mobile Client (Android & iOS)
- **Framework**: Kotlin Multiplatform (KMP) + Compose Multiplatform (CMP)
- **Language**: Kotlin 1.9.23+
- **Architecture**: Clean Architecture (Presentation, Domain, Data)
- **Local Persistence**: SQLDelight 2.0.2 (Offline-First SQLite Cache)
- **Networking**: Ktor Client 2.3+ with `kotlinx.serialization`
- **Dependency Injection**: Koin 3.5+
- **Target OS**: Android (Min API 26+) & iOS (15+)

### Web Admin Console
- **Framework**: Next.js 14+ (App Router)
- **Language**: TypeScript 5+
- **Styling**: Tailwind CSS
- **Icons**: Lucide React

### Backend & Cloud Infrastructure
- **Platform**: Supabase
- **Database**: PostgreSQL 15+ with Row Level Security (RLS)
- **Authentication**: Supabase Auth (Email/Password in V1, Phone OTP ready)
- **Storage**: Supabase Storage with strict MIME and file size validation
- **Edge Functions**: Deno TypeScript serverless functions for push notifications and audit trails

---

## 🌟 Core Features & Modules

1. **Home Dashboard**: Top bar with village subtitle, personalized bilingual greeting, search bar, important notice card, 9 quick service tiles, upcoming events carousel, and latest notices.
2. **Notices (સૂચનાઓ)**: Categorized, pinned, scheduled, and archived announcements from Gram Panchayat.
3. **Events (કાર્યક્રમો)**: Cultural celebrations, religious events, Gram Sabhas, and health camps.
4. **Directory (ડિરેક્ટરી)**: Verified village businesses, job openings, and historical places with direct Call, WhatsApp, and Map intents.
5. **Offerings / ચઢાવો (Strictly Non-Auction)**: Community devotional and social voluntary contributions (Temple Puja, Dhwajarohan, Gaushala fodder, festival seva).
6. **Complaints (ફરિયાદ નિવારણ)**: Citizen grievance submission with photo and location tracking through a 7-stage resolution flow.
7. **Emergency Contacts (તાત્કાલિક સહાય)**: High-contrast, verified helplines (108 Ambulance, 100/112 Police, 101 Fire, 181 Abhayam).
8. **Offline-First Synchronization**: Full SQLite local cache with background sync engine and conflict resolution.
9. **Web Admin Console**: Complete management of users, RBAC roles, content moderation, reports, and immutable audit logs.

---

## 📂 Repository Structure

```text
bodla-parivar/
├── composeApp/                     # Kotlin Multiplatform client (Android & iOS)
│   ├── src/
│   │   ├── commonMain/kotlin/com/bodla/parivar/
│   │   │   ├── core/               # Theme, localization, network, security, sync
│   │   │   ├── data/               # Repositories, mappers, local data sources
│   │   │   ├── domain/             # Models, repository contracts, use cases
│   │   │   └── features/           # Home, notices, events, directory, offerings, complaints, etc.
│   │   ├── commonMain/sqldelight/  # SQLDelight AppDatabase.sq schema
│   │   ├── androidMain/            # Android activity and platform drivers
│   │   └── iosMain/                # iOS ViewController and platform drivers
├── iosApp/                         # Xcode project and SwiftUI host
├── admin/                          # Next.js 14 Web Admin Portal
│   ├── src/app/                    # Dashboard, notices, offerings, complaints, audit logs
│   ├── src/components/             # Sidebar, Header, StatCard, StatusBadge
│   └── src/lib/                    # TypeScript models and Supabase client
├── database/                       # PostgreSQL migrations, RLS policies, and seed data
│   ├── migrations/                 # Schema, indexes, functions, triggers, storage
│   ├── policies/                   # Row Level Security SQL policies
│   └── seeds/                      # Default categories and marked DEMO DATA
├── server/supabase/                # Supabase local config and Edge Functions
├── docs/                           # Comprehensive technical documentation suite
├── .github/workflows/              # CI/CD pipelines for mobile and admin
├── .env.example                    # Environment variable template
└── README.md                       # Project master guide
```

---

## 🚀 Getting Started

### 1. Database Setup
1. Create a Supabase project at [supabase.com](https://supabase.com).
2. Execute the migrations sequentially in SQL Editor:
   - `database/migrations/001_initial_schema.sql`
   - `database/migrations/002_indexes_and_performance.sql`
   - `database/migrations/003_functions_and_triggers.sql`
   - `database/policies/001_row_level_security.sql`
   - `database/migrations/004_storage_policies.sql`
   - `database/seeds/001_initial_seed.sql`

### 2. Running Web Admin Console
```bash
cd admin
npm install
npm run dev
```
Access at `http://localhost:3000`.

### 3. Running Mobile Client
- **Android**: Open project in Android Studio Iguana+, select `composeApp`, and run on emulator or physical device.
- **iOS**: Open `iosApp/iosApp.xcodeproj` in Xcode on macOS and run on iOS Simulator.

---

## 📖 Documentation Suite

- [Architecture & Overview](file:///d:/Project-Work/BODLA-PARIVVAR/docs/ARCHITECTURE.md)
- [Database Schema & Relationships](file:///d:/Project-Work/BODLA-PARIVVAR/docs/DATABASE.md)
- [API Specification (/api/v1)](file:///d:/Project-Work/BODLA-PARIVVAR/docs/API.md)
- [Authentication & Identity](file:///d:/Project-Work/BODLA-PARIVVAR/docs/AUTH.md)
- [Security & Data Protection](file:///d:/Project-Work/BODLA-PARIVVAR/docs/SECURITY.md)
- [Offline-First Sync Engine](file:///d:/Project-Work/BODLA-PARIVVAR/docs/OFFLINE_SYNC.md)
- [Offerings / ચઢાવો Specification](file:///d:/Project-Work/BODLA-PARIVVAR/docs/OFFERINGS.md)
- [Admin Console Guide](file:///d:/Project-Work/BODLA-PARIVVAR/docs/ADMIN_PANEL.md)
- [Deployment & Release](file:///d:/Project-Work/BODLA-PARIVVAR/docs/DEPLOYMENT.md)
- [API Keys & Secrets](file:///d:/Project-Work/BODLA-PARIVVAR/docs/API_KEYS.md)
- [Contributing Guidelines](file:///d:/Project-Work/BODLA-PARIVVAR/docs/CONTRIBUTING.md)

---

## 📜 License

Distributed under the MIT License. See [LICENSE](file:///d:/Project-Work/BODLA-PARIVVAR/LICENSE) for more information.
