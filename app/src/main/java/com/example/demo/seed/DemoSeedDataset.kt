package com.example.demo.seed

import com.example.core.config.SupportedCity
import com.example.core.config.UserRole
import com.example.core.domain.model.Booking
import com.example.core.domain.model.CustomerProfile
import com.example.core.domain.model.HostProfile
import com.example.core.domain.model.ParkingBay
import com.example.core.domain.model.ParkingSpace
import com.example.core.domain.model.UserProfile
import com.example.core.domain.model.VehicleProfile

/**
 * Marketplace configuration and legacy seed identifiers for ParkSpace Demo Mode.
 *
 * Fresh installations start with a completely empty marketplace (zero fictional users,
 * listings, bays, vehicles, or bookings). Every record must come from genuine user actions.
 */
object DemoSeedDataset {
  const val SEED_KEY = "parkspace_demo_seed"
  const val SEED_VERSION = 2

  private const val BASE_ADMIN_CREATED_MS = 1_760_000_000_000L

  // Exact primary keys of legacy Milestone 1 fictional seed rows so existing local DBs can purge
  // ONLY those predefined sample records without ever touching user-created data.
  val LEGACY_SEED_USER_IDS: Set<String> =
    setOf(
      "demo-cust-lhr",
      "demo-cust-khi",
      "demo-host-lhr",
      "demo-host-isb",
      "demo-host-fsd",
      "demo-host-khi",
      "demo-admin-private",
    )

  val LEGACY_SEED_CUSTOMER_PROFILE_IDS: Set<String> =
    setOf("demo-cust-lhr", "demo-cust-khi")

  val LEGACY_SEED_HOST_PROFILE_IDS: Set<String> =
    setOf("demo-host-lhr", "demo-host-isb", "demo-host-fsd", "demo-host-khi")

  val LEGACY_SEED_VEHICLE_IDS: Set<String> =
    setOf("demo-veh-1", "demo-veh-2", "demo-veh-3", "demo-veh-4")

  val LEGACY_SEED_SPACE_IDS: Set<String> =
    setOf(
      "demo-lhr-01",
      "demo-lhr-02",
      "demo-lhr-03",
      "demo-isb-01",
      "demo-isb-02",
      "demo-isb-03",
      "demo-fsd-01",
      "demo-fsd-02",
      "demo-fsd-03",
      "demo-khi-01",
      "demo-khi-02",
      "demo-khi-03",
    )

  val LEGACY_SEED_BAY_IDS: Set<String> =
    LEGACY_SEED_SPACE_IDS.flatMap { spaceId -> listOf("$spaceId-bay-a1", "$spaceId-bay-a2") }.toSet()

  val LEGACY_SEED_BOOKING_IDS: Set<String> =
    setOf("demo-bkg-01", "demo-bkg-02")

  /**
   * Transient local profile used ONLY when an operator explicitly unlocks the
   * Private Super Admin Sandbox Gate (never preloaded into demo_users on startup).
   */
  val privateDemoAdminUser: UserProfile =
    UserProfile(
      id = "demo-admin-private",
      fullName = "Super Admin (Demo Sandbox)",
      phoneMasked = "+92 300 ••••000",
      emailMasked = "admin.sandbox@parkspace.local",
      role = UserRole.SUPER_ADMIN,
      city = SupportedCity.ISLAMABAD,
      isDemoAccount = true,
      createdAtEpochMs = BASE_ADMIN_CREATED_MS,
    )

  // All preloaded marketplace collections are strictly empty on fresh launch.
  val users: List<UserProfile> = emptyList()
  val customerProfiles: List<CustomerProfile> = emptyList()
  val hostProfiles: List<HostProfile> = emptyList()
  val vehicles: List<VehicleProfile> = emptyList()
  val parkingSpaces: List<ParkingSpace> = emptyList()
  val parkingBays: List<ParkingBay> = emptyList()
  val sampleBookings: List<Booking> = emptyList()
}
