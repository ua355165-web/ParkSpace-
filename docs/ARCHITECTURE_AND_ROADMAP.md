# ParkSpace — Architecture & Implementation Plan (Milestone 0)

- **Application ID:** `pk.parkspace.marketplace`
- **Scope:** Dedicated Parking Marketplace for Pakistan (**Lahore, Islamabad, Faisalabad, Karachi**)
- **User Roles:** Customer, Host, Private Super Admin (no public admin registration)
- **Current Phase:** Milestone 3 — Customer Booking System (`AppMode.DEMO`)

## 1. Environment & Toolchain Alignment
- **Verified Container Toolchain:** Gradle `9.3.1`, AGP `9.1.1`, Kotlin `2.2.10`, KSP `2.3.5`, Compose BOM `2024.09.00`, `compileSdk 36`, `targetSdk 36`, `minSdk 24`.
- **Target Specification Note:** User specification lists Gradle `8.9`, AGP `8.7.3`, Kotlin `2.0.21`. The container's pre-tested toolchain is preserved for verified local builds; no incompatible downgrade was forced.

## 2. Demo Mode vs Production Mode Boundaries
| Capability | Demo Mode (Milestones 0–7) | Production Mode (Milestone 8+) |
|---|---|---|
| Auth & Session | Explicit local Demo Sandbox Role/Profile switcher with visible `DEMO SANDBOX` banner; Admin access locked behind dedicated private demo gate (no public Admin signup). | Firebase Authentication (Phone OTP / Google Sign-In via Credential Manager) + Custom Claims (`role: customer | host | super_admin`). |
| Parking Listings | Seeded in-memory/Room Pakistan parking spaces across Lahore, Islamabad, Faisalabad, Karachi with ~200m masked approximate coordinates before booking (see `BASELINE_LOCK.md`). | Firestore `parking_spaces` collection; exact entry coordinates & gate PINs released via Cloud Functions v2 only after verified booking. |
| Map & Discovery | MapLibre Native Android `11.8.0` + OpenFreeMap Liberty style (`https://tiles.openfreemap.org/styles/liberty`) + 10-dimension pure Kotlin filter engine. | Same MapLibre + OpenFreeMap Liberty stack, backed by GeoHash/Firestore queries. |
| Booking & Payments | Local deterministic state machine + PKR fee breakdown + simulated Demo Wallet/Escrow ledger clearly marked as Demo. | Server-authoritative Cloud Functions v2 transactions, webhook-verified payment gateways (JazzCash / Easypaisa / Card), and server escrow release. |
| Host Approval & Admin | Local Admin moderation queue approving/rejecting demo host KYC & parking spaces. | Cloud Functions v2 admin-only callable functions verified against `super_admin` custom claim & App Check. |
| Chat, Notifications, Reviews, Disputes, Referrals | Local reactive repositories + local notification inbox. | Firestore real-time listeners, FCM push notifications, and server-validated review/referral/dispute rules. |

## 3. Milestone Roadmap
- **Milestone 0 (Completed):** Project identity, custom adaptive icon, bundled typography (`Plus Jakarta Sans`, `Inter`, `JetBrains Mono`), Material 3 light/dark Design System, `AppEnvironment` / `UiState` foundation, verified build & Robolectric test.
- **Milestone 1 (Completed):** Core Domain Models, isolated Room Demo Database (`parkspace_demo_v1.db`), idempotent Demo Seed Dataset for 4 cities, Demo Session Sandbox, and Type-Safe Navigation Compose + Role Guard.
- **Milestone 2 (Completed):** MapLibre Native Android `11.8.0` + OpenFreeMap Liberty discovery screen, Map/List toggle, 10-dimension search & filter sheet, ~200m masked marker projection, parking preview card, and privacy-safe detail preview.
- **Milestone 3 (Completed):** Customer Booking Lifecycle (hourly/daily/monthly 30-day cycle pricing, Room transactional half-open overlap availability & capacity checks, unique Demo reference, post-confirmation Demo PIN/QR pass & exact gate reveal, simulated check-in/out & overstay/expiry, `BASELINE_LOCK.md` cancellation refund rules, and 4-tab Booking History).
- **Milestone 4:** Host Console (space onboarding wizard, Bay/Slot management, availability calendar, booking approvals, local PKR earnings & payout ledger).
- **Milestone 5:** Payments Ledger (Demo Escrow), In-App Chat (masked contact), Local Notifications, Reviews, Complaints/Disputes, and Referral Codes.
- **Milestone 6:** Private Super Admin Console (host KYC approval queue, space verification, dispute resolution, city analytics, fee configuration, audit log).
- **Milestone 7:** Comprehensive Testing (Unit + Robolectric CUJ tests across Customer/Host/Admin), code review, accessibility & responsive verification, and ZIP/GitHub source backup.
- **Milestone 8 (Final Production Phase):** Firebase Auth + Firestore + Security Rules + App Check + Cloud Functions v2 integration (after explicit approval and Firebase provisioning).
