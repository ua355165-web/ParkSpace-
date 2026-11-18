# ParkSpace — Milestone 2 MapLibre Discovery & Privacy Architecture

## 1. MapLibre Native Android Configuration
- **Artifact:** `org.maplibre.gl:android-sdk:11.8.0` (configured in `gradle/libs.versions.toml` and `app/build.gradle.kts`).
- **Style URL:** `https://tiles.openfreemap.org/styles/liberty` (`AppEnvironment.MAP_STYLE_URL`).
- **Permissions:** Normal install-time `INTERNET` and `ACCESS_NETWORK_STATE` in `AndroidManifest.xml` (no Google Maps API key, no paid map SDKs, no storage permissions).
- **Lifecycle & State:** `ParkSpaceMapLibreView` initializes `MapLibre.getInstance(context.applicationContext)`, forwards `ON_START/ON_RESUME/ON_PAUSE/ON_STOP/ON_DESTROY` via `DisposableEffect`, checks `NetworkCapabilities` before style fetch, listens to `OnDidFailLoadingMapListener`, and exposes accessible `48.dp` Zoom In, Zoom Out, and City Re-center controls alongside a fallback **List View** switch.

## 2. Privacy-Safe Public Marker Projection
- `FilterParkingDiscoveryUseCase` projects every approved, positive-capacity `ParkingSpace` through `ParkingPublicListing.fromDomainSpace(space)`, which invokes `space.location.toPublicMaskedLocation()`.
- `ParkingPublicListing` and `PublicParkingMarker` physically omit `privateExactLatitude`, `privateExactLongitude`, `privateGateSummary`, and private host identifiers.
- `buildMarkerFeatureCollection(...)` serializes only the ~200m masked coordinates into MapLibre's `GeoJsonSource` (`MASK_ZONE_LAYER_ID` halo + `MARKER_PIN_LAYER_ID` pins).

## 3. Search & 10-Dimension Filter Architecture
- Implemented in pure Kotlin (`FilterParkingDiscoveryUseCase.execute`) completely separate from Compose UI:
  1. City (`Lahore`, `Islamabad`, `Faisalabad`, `Karachi`)
  2. Locality (`areaLabel`)
  3. Vehicle compatibility (`BIKE`, `CAR`, `SUV`)
  4. Parking type (`COVERED_GARAGE`, `COMMERCIAL_PLAZA`, `OPEN_LOT`, `GATED_COMPOUND`, `PRIVATE_DRIVEWAY`)
  5. Shade & enclosure (`All`, `Covered`, `Open Air`)
  6. Security & features (`CCTV`, `Security Guard`, `EV Charging`, `Night Lighting`)
  7. Booking plan availability (`Hourly`, `Daily`, `Monthly`)
  8. Min & Max price in PKR
  9. Available capacity (`1+`, `5+`, `10+`, `15+` bays; unavailable `0`-bay or unapproved spaces excluded)
  10. Reset All Filters (`resetPreservingCity()`)
