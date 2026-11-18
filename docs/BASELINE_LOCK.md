# ParkSpace — Milestone 0 Baseline Lock & Architecture Decisions

This document locks all architectural, build, privacy, booking, and Demo Mode security decisions prior to Milestone 1.

---

## 1. Version Alignment & Target GitHub CI Migration Strategy

### Current Verified Container Toolchain (Preserved)
- **Gradle Wrapper:** `9.3.1` (`gradle/wrapper/gradle-wrapper.properties`)
- **Android Gradle Plugin (AGP):** `9.1.1` (uses AGP built-in Kotlin support; `org.jetbrains.kotlin.android` plugin is not applied separately)
- **Kotlin:** `2.2.10`
- **KSP:** `2.3.5`
- **Compose BOM:** `2024.09.00`
- **SDK Levels:** `compileSdk = 36` (minorApiLevel `1`), `targetSdk = 36`, `minSdk = 24`, Java `11`

### Compatibility Rationale
This execution container's Gradle cache, AGP 9.1.1 built-in Kotlin plugin, KSP `2.3.5`, `androidx.core:core-ktx:1.18.0`, `androidx.activity:activity-compose:1.10.1`, and `org.robolectric:robolectric:4.16.1` are pre-aligned and verified together. Downgrading blindly to Gradle `8.9` / AGP `8.7.3` / Kotlin `2.0.21` inside this container would break AGP 9 built-in Kotlin assumptions, KSP/Compose compiler alignment, and cached artifacts.

### Target GitHub Pipeline Migration Checklist (Unverified Until Run on Target CI)
When exporting to a dedicated GitHub Actions / Android Studio pipeline targeting **Gradle 8.9 + AGP 8.7.3 + Kotlin 2.0.21**:
1. Update `gradle/wrapper/gradle-wrapper.properties` `distributionUrl` to `gradle-8.9-bin.zip`.
2. In `gradle/libs.versions.toml`, set `agp = "8.7.3"`, `kotlin = "2.0.21"`, `googleDevtoolsKsp = "2.0.21-1.0.27"`.
3. Add `alias(libs.plugins.kotlin.android)` to root `build.gradle.kts` (`apply false`) and `app/build.gradle.kts` (`plugins { ... }`), because AGP `8.7.3` does not embed Kotlin automatically.
4. Set `compileSdk = 35` / `targetSdk = 35` and downgrade `core-ktx` (`1.15.0`) / `activity-compose` (`1.9.3`) if AGP `8.7.3` warns on AAR metadata requirements.
5. Run `./gradlew clean :app:assembleDebug :app:testDebugUnitTest` on the target runner to verify compatibility before releasing. *(Not yet verified on Gradle 8.9/AGP 8.7.3).*

---

## 2. Package & Namespace Consistency Decision

- **Application ID (Locked):** `pk.parkspace.marketplace` (in `app/build.gradle.kts`).
- **Namespace (Locked for AI Studio Environment):** `com.example` (in `app/build.gradle.kts`).
- **Technical Assessment:** AGP cleanly decouples `applicationId` (device/Play Store identity) from `namespace` (compile-time `R` & `BuildConfig` package). Keeping `namespace = "com.example"` is not a strict AGP limitation on an external project, **however** inside the AI Studio container keeping `namespace = "com.example"` and `com/example/` directories is required by platform tooling and preserves verified resource (`com.example.R`) and test (`ExampleRobolectricTest`, `ExampleUnitTest`, `ExampleInstrumentedTest`) paths without risky churn.
- **Clean Package Strategy:**
  - All ParkSpace modules inside the current project live cleanly under `com.example.core.*`, `com.example.demo.*`, and `com.example.feature.*`.
  - If an external repository later requires matching `namespace = "pk.parkspace.marketplace"`, execute it as a single atomic post-export refactor updating `namespace`, `AndroidManifest.xml` relative class names, `import com.example.R`, and source/test directory moves (`com/example` -> `pk/parkspace/marketplace`), followed by full compilation and unit test verification.

---

## 3. Pre-Booking Location Privacy Standard

- **Masking Radius:** `AppEnvironment.PRE_BOOKING_LOCATION_MASK_RADIUS_METERS = 200` (approx. 200 metres).
- **Pre-Booking Public View:** Discovery map pins, neighborhood circles, and public listing cards display only the **deterministically offset (~200m radius) centroid** plus area/sector label (e.g., *Gulberg III, Lahore*, *F-7 Markaz, Islamabad*).
- **Restricted Exact Details:** Exact street/house/plaza gate coordinates, entrance instructions, gate/guard phone contact, and entry PIN/QR pass remain withheld until the booking reaches **`BookingStatus.CONFIRMED`** (verified payment/escrow hold in Demo Mode; server-enforced Cloud Function / restricted Firestore subcollection in Production Mode).

---

## 4. Booking Types & Validation Rules (Hourly, Daily, Monthly)

All three booking types (`BookingType.HOURLY`, `BookingType.DAILY`, `BookingType.MONTHLY`) are supported with strict domain rules:

| Rule Dimension | Hourly | Daily | Monthly |
|---|---|---|---|
| **Duration Unit & Bounds** | Min `1 hour`, max `23 hours` (15-min or 1-hour boundary alignment; starts on top/half hour). | `1 to 29 consecutive days` (standardized check-in/out window, e.g., 08:00–08:00 or 24h blocks). | `30-day rolling cycle` (`1 to 12 billing cycles`), auto-renewal disabled by default unless opted in. |
| **Pricing Formula (PKR)** | `hourlyRatePkr * hours` + platform service fee. | `dailyRatePkr * days` (capped/discounted vs `24 * hourlyRatePkr`) + platform service fee. | `monthlyRatePkr * months` + platform service fee (prorated extensions handled as separate top-up booking). |
| **Availability & Capacity** | Deducts `1` bay from `totalBays` for `[startEpochMs, endEpochMs)`. | Deducts `1` bay across all overlapping `[startDay, endDay)` intervals; respects host blackout dates. | Reserves `1` dedicated bay for the full 30-day block (`[startEpochMs, endEpochMs)`), rejecting spaces with blackout dates inside the cycle or `monthlyEnabled == false`. |
| **Conflict Validation** | Overlap check: `existing.start < req.end && existing.end > req.start` for active statuses (`PENDING_PAYMENT`, `CONFIRMED`, `CHECKED_IN`); `activeCountAtAnyMinute < space.availableBays`. | Same half-open interval overlap check across all overlapping hourly, daily, and monthly bookings on that space. | Unified interval overlap check ensures hourly/daily bookings cannot oversell a bay committed to an active monthly reservation (and vice-versa). |
| **Cancellation Policy** | `>= 2 hrs` before start: `100%` refund; `< 2 hrs`: `50%` refund; after start / no-show: `0%` (dispute eligible). | `>= 24 hrs` before start: `100%` refund; `< 24 hrs`: `1 day` fee retained; mid-stay: unused full days minus 1-day penalty. | `>= 72 hrs` before cycle start: `100%` refund; active cycle: current 30-day cycle non-refundable except verified host fault/dispute; future pre-paid cycles `100%` refundable. |

---

## 5. Demo Mode Security & Sandbox Guardrails

1. **Explicit Local Testing Sandbox:** Any role switching in Milestones 1–7 is explicitly labeled **`Demo Sandbox • Local Testing Only (Not Production Auth)`** and uses an in-app Demo Sandbox Switcher banner so it can never be mistaken for production login or authorization.
2. **No Fake Production Auth:** Demo Mode never mints fake JWTs, fake Firebase tokens, or fake server responses.
3. **Private Super Admin Isolation:** `UserRole.SUPER_ADMIN.allowsPublicRegistration = false`. There is **no public Admin sign-up/registration screen**. In Demo Mode, Super Admin views sit behind an explicit Demo Sandbox Admin Gate; in Production Mode (Milestone 8), Admin access requires a pre-provisioned account with server-assigned `super_admin` Custom Claims verified by Cloud Functions v2 and Firestore Security Rules.
