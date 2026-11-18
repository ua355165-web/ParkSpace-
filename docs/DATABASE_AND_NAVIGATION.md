# ParkSpace — Milestone 1 Database, Seed & Navigation Architecture

## 1. Local Demo Room Database (`ParkSpaceDemoDatabase`)
- **Database File:** `parkspace_demo_v1.db` (strictly isolated for Demo Mode; never mixed with future Milestone 8 Firebase Firestore collections).
- **Schema Version:** `CURRENT_SCHEMA_VERSION = 2`
- **Versioning Strategy:**
  - Incremental Room `Migration` instances are registered in `ParkSpaceDemoDatabase.MIGRATIONS` (`MIGRATION_1_2` adds Milestone 3 booking payment, simulated gate PIN, check-in/out, and cancellation refund columns non-destructively).
  - Destructive fallback is avoided so user test state is preserved across upgrades.
- **Tables (`DemoEntities.kt`):**
  - `demo_seed_metadata` — tracks `seedKey` and `seedVersion`
  - `demo_users` — Demo Customer, Demo Host, and Private Demo Admin profiles
  - `demo_customer_profiles` — preferred city, default vehicle, demo referral code
  - `demo_host_profiles` — business alias, `HostVerificationStatus`, operating cities
  - `demo_vehicles` — Bike, Car, and SUV demo vehicles
  - `demo_parking_spaces` — fictional listings with ~200m `maskRadiusMeters`, pricing, schedule, amenities
  - `demo_parking_bays` — per-space bays (`A1`, `A2`)
  - `demo_bookings` — Demo bookings enforcing duration, `gatePinReleased`, Demo payment status, simulated check-in/out timestamps, and cancellation refund metadata

## 2. Idempotent Demo Seeding (`DemoSeedInitializer`)
- Guarded by a coroutine `Mutex`, a Room `withTransaction` block, `demo_seed_metadata` version check, and `OnConflictStrategy.IGNORE`.
- Guarantees **12 fictional parking spaces** (3 each in **Lahore, Islamabad, Faisalabad, Karachi**), **24 bays**, **7 demo users**, **4 demo vehicles**, and **2 sample bookings** with zero duplication across repeated app launches.

## 3. Type-Safe Navigation & Centralized Guard (`ParkSpaceNavHost` + `ParkSpaceNavGuard`)
- **Routes (`@Serializable` `ParkSpaceRoute`):**
  - `WelcomeEntry`
  - `CustomerHome`
  - `CustomerDiscovery`
  - `ParkingDetailPreview(spaceId)`
  - `BookingCheckout(spaceId)`
  - `BookingConfirmation(bookingId)`
  - `CustomerBookings`
  - `HostHome`
  - `PrivateAdminHome`
  - `SharedProfileSettings`
  - `AccessDenied(attemptedRouteName, reason)`
- **Role Guards (`ParkSpaceNavGuard.evaluateAccess`):**
  - Every role-scoped navigation transition and destination checks the active `DemoSessionState`.
  - Unauthorized transitions route safely to `AccessDenied` rather than exposing protected shells.

## 4. Demo Sandbox Test Accounts (Local Testing Only)
> **Security Notice:** These accounts exist strictly in the local Demo Room database (`isDemoAccount = true`) for UI/flow verification. They do **not** provide production authentication or server authorization.

| Role | Demo User ID | Display Name | City |
|---|---|---|---|
| Customer | `demo-cust-lhr` | Ayesha Tariq (Demo) | Lahore |
| Customer | `demo-cust-khi` | Bilal Siddiqui (Demo) | Karachi |
| Host | `demo-host-lhr` | Hamza Malik (Demo Host) | Lahore |
| Host | `demo-host-isb` | Sana Javed (Demo Host) | Islamabad |
| Host | `demo-host-fsd` | Usman Raza (Demo Host) | Faisalabad |
| Host | `demo-host-khi` | Zainab Noor (Demo Host) | Karachi |
| Private Super Admin | `demo-admin-private` | Super Admin (Demo Sandbox) | Islamabad (Requires explicit Private Gate acknowledgement; no public signup) |
