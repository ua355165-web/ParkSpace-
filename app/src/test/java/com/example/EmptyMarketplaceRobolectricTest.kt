package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.common.UiState
import com.example.core.config.BookingType
import com.example.core.config.SupportedCity
import com.example.core.config.UserRole
import com.example.core.data.demo.DemoAuthRepositoryImpl
import com.example.core.data.demo.DemoBookingRepositoryImpl
import com.example.core.data.demo.DemoParkingRepositoryImpl
import com.example.core.data.demo.DemoUserProfileRepositoryImpl
import com.example.core.database.ParkSpaceDemoDatabase
import com.example.core.database.entity.DemoBookingEntity
import com.example.core.database.entity.DemoCustomerProfileEntity
import com.example.core.database.entity.DemoHostProfileEntity
import com.example.core.database.entity.DemoParkingBayEntity
import com.example.core.database.entity.DemoParkingSpaceEntity
import com.example.core.database.entity.DemoSeedMetadataEntity
import com.example.core.database.entity.DemoUserEntity
import com.example.core.database.entity.DemoVehicleEntity
import com.example.core.database.mapper.toEntity
import com.example.core.domain.model.AvailabilitySchedule
import com.example.core.domain.model.BookingStatus
import com.example.core.domain.model.HostVerificationStatus
import com.example.core.domain.model.ParkingAmenity
import com.example.core.domain.model.ParkingDayOfWeek
import com.example.core.domain.model.ParkingLocation
import com.example.core.domain.model.ParkingSpace
import com.example.core.domain.model.ParkingType
import com.example.core.domain.model.VehicleType
import com.example.core.domain.repository.BookingCreateRequest
import com.example.core.domain.repository.BookingMutationResult
import com.example.core.domain.repository.DemoAuthResult
import com.example.core.domain.repository.DemoRegisterRequest
import com.example.core.domain.repository.HostListingMutationResult
import com.example.demo.sandbox.DemoSessionManager
import com.example.demo.sandbox.DemoSessionState
import com.example.demo.seed.DemoSeedDataset
import com.example.demo.seed.DemoSeedInitializer
import com.example.feature.booking.domain.BookingAvailabilityValidator
import com.example.feature.discovery.DiscoveryScreenData
import com.example.feature.discovery.DiscoveryViewModel
import com.example.feature.discovery.domain.model.DiscoveryFilterResult
import com.example.feature.discovery.domain.model.DiscoveryViewMode
import com.example.feature.discovery.domain.model.MapRenderStatus
import com.example.feature.discovery.domain.model.ParkingDiscoveryFilter
import com.example.feature.discovery.ui.ParkingDiscoveryScreen
import com.example.feature.foundation.FoundationDashboardData
import com.example.feature.foundation.ui.WelcomeEntryScreen
import com.example.ui.theme.ParkSpaceTheme
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
class EmptyMarketplaceRobolectricTest {

  @get:Rule val composeRule = createComposeRule()

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    context.getSharedPreferences("parkspace_demo_session", Context.MODE_PRIVATE).edit().clear().commit()
  }

  @Test
  fun `fresh install starts completely empty across all cities and shows empty marketplace copy`() {
    val db =
      Room.inMemoryDatabaseBuilder(context, ParkSpaceDemoDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    try {
      runBlocking(Dispatchers.IO) {
        DemoSeedInitializer(db).seedIfNeeded()
        assertEquals(0, db.userDao().countUsers())
        assertEquals(0, db.parkingDao().countSpaces())
        assertEquals(0, db.parkingDao().countBays())
        assertEquals(0, db.bookingDao().countBookings())
      }

      // Verify all 4 cities remain selectable configuration options
      assertEquals(4, SupportedCity.entries.size)

      composeRule.setContent {
        ParkSpaceTheme {
          WelcomeEntryScreen(
            uiState =
              UiState.Success(
                FoundationDashboardData(
                  session = DemoSessionState(selectedCity = SupportedCity.LAHORE),
                  approvedSpacesInSelectedCity = emptyList(),
                  allSpacesCount = 0,
                  approvedSpacesCount = 0,
                  pendingSpacesCount = 0,
                  activeCustomerProfile = null,
                  activeUserVehicles = emptyList(),
                  activeHostProfile = null,
                  activeHostSpaces = emptyList(),
                  activeUserBookings = emptyList(),
                )
              ),
            sandboxFeedback = null,
            isDarkTheme = false,
            onToggleTheme = {},
            onSelectCity = {},
            onOpenAuth = { _, _ -> },
            onSignOut = {},
            onUnlockPrivateAdmin = {},
            onNavigateToRoleHome = {},
            onNavigateToSettings = {},
            onRetrySeed = {},
            onDismissFeedback = {},
          )
        }
      }

      composeRule.waitForIdle()
      composeRule.onNodeWithTag("customer_entry_card").assertIsDisplayed()
      composeRule.onNodeWithTag("host_entry_card").assertIsDisplayed()
      composeRule.onNodeWithText("Lahore").assertDoesNotExist()
      composeRule.onNodeWithText("Gulberg").assertDoesNotExist()
    } finally {
      db.close()
    }
  }

  @Test
  fun `legacy seed cleanup removes only fictional seed rows and preserves user accounts listings and bookings across restart`() {
    val dbName = "parkspace_empty_cleanup_restart.db"
    context.deleteDatabase(dbName)

    var db =
      Room.databaseBuilder(context, ParkSpaceDemoDatabase::class.java, dbName)
        .addMigrations(*ParkSpaceDemoDatabase.MIGRATIONS)
        .allowMainThreadQueries()
        .build()

    try {
      val hostId: String
      val custId: String
      val userSpaceId = "space-real-user-01"
      val userBookingId: String

      runBlocking(Dispatchers.IO) {
        // Simulate a pre-existing v1 local DB containing both old seed rows AND genuine user-created data
        db.seedMetadataDao().upsertSeedMetadata(
          DemoSeedMetadataEntity(key = DemoSeedDataset.SEED_KEY, seedVersion = 1, seededAtEpochMs = 1_000L)
        )
        db.userDao().insertUsersIfMissing(
          listOf(
            DemoUserEntity("demo-cust-lhr", "Ayesha (Old Seed)", "0300", "a@b.c", UserRole.CUSTOMER, SupportedCity.LAHORE, true, 1L, isSelfRegisteredDemoAccount = false),
            DemoUserEntity("demo-host-lhr", "Hamza (Old Seed)", "0333", "h@b.c", UserRole.HOST, SupportedCity.LAHORE, true, 2L, isSelfRegisteredDemoAccount = false),
          )
        )
        db.userDao().insertCustomerProfilesIfMissing(
          listOf(DemoCustomerProfileEntity("demo-cust-lhr", SupportedCity.LAHORE, "demo-veh-1", "PKDEMO100"))
        )
        db.userDao().insertHostProfilesIfMissing(
          listOf(DemoHostProfileEntity("demo-host-lhr", "Crescent Seed", HostVerificationStatus.APPROVED, setOf(SupportedCity.LAHORE), 1, 1L))
        )
        db.userDao().insertVehiclesIfMissing(
          listOf(DemoVehicleEntity("demo-veh-1", "demo-cust-lhr", VehicleType.CAR, "Seed Car", "LE-1", true))
        )
        db.parkingDao().insertSpacesIfMissing(
          listOf(
            DemoParkingSpaceEntity(
              id = "demo-lhr-01",
              hostId = "demo-host-lhr",
              title = "[Demo] Seeded Lahore Garage",
              description = "Old seed",
              parkingType = ParkingType.COVERED_GARAGE,
              city = SupportedCity.LAHORE,
              areaLabel = "Gulberg",
              publicMaskedLatitude = 31.51,
              publicMaskedLongitude = 74.34,
              maskRadiusMeters = 200,
              privateGateSummary = null,
              privateExactLatitude = null,
              privateExactLongitude = null,
              hourlyRatePkr = 160,
              dailyRatePkr = 1100,
              monthlyRatePkr = null,
              monthlyEnabled = false,
              totalBays = 2,
              availableBays = 2,
              supportedVehicles = setOf(VehicleType.CAR),
              amenities = setOf(ParkingAmenity.CCTV),
              openDays = ParkingDayOfWeek.entries.toSet(),
              openHour = 0,
              closeHour = 23,
              is24Hours = true,
              blackoutDatesIso = emptySet(),
              isApproved = true,
              isDemoListing = true,
            )
          )
        )
        db.parkingDao().insertBaysIfMissing(
          listOf(DemoParkingBayEntity("demo-lhr-01-bay-a1", "demo-lhr-01", "A1", setOf(VehicleType.CAR), true, true))
        )
        db.bookingDao().insertBookingsIfMissing(
          listOf(
            DemoBookingEntity(
              id = "demo-bkg-01",
              bookingCode = "PS-DEMO-1042",
              spaceId = "demo-lhr-01",
              bayId = "demo-lhr-01-bay-a1",
              customerId = "demo-cust-lhr",
              hostId = "demo-host-lhr",
              vehicleId = "demo-veh-1",
              city = SupportedCity.LAHORE,
              bookingType = BookingType.HOURLY,
              status = BookingStatus.CONFIRMED,
              startEpochMs = 1_000L,
              endEpochMs = 4_600_000L,
              baseAmountPkr = 160,
              serviceFeePkr = 13,
              totalAmountPkr = 173,
              gatePinReleased = true,
              isDemoRecord = true,
            )
          )
        )

        // Now add genuine user-created Host, Customer, Vehicle, Listing, and Booking
        val authRepo = DemoAuthRepositoryImpl(db)
        val userRepo = DemoUserProfileRepositoryImpl(db.userDao(), db)
        val parkingRepo = DemoParkingRepositoryImpl(db)
        val bookingRepo = DemoBookingRepositoryImpl(db)

        val hostUser =
          (authRepo.registerLocalAccount(
            DemoRegisterRequest(
              role = UserRole.HOST,
              fullName = "Zeeshan Real Host",
              email = "zeeshan.host@gmail.com",
              password = "password123",
              confirmPassword = "password123",
              preferredCity = SupportedCity.LAHORE,
              businessAlias = "Zeeshan DHA Parking",
            )
          ) as DemoAuthResult.Success).user
        hostId = hostUser.id

        // Approve real host and publish their listing
        val hp = userRepo.observeHostProfile(hostId).first()!!
        db.userDao().upsertHostProfile(hp.copy(verificationStatus = HostVerificationStatus.APPROVED).toEntity())

        val createdSpace =
          ParkingSpace(
            id = userSpaceId,
            hostId = hostId,
            title = "Zeeshan Real DHA Lot",
            description = "User created listing",
            parkingType = ParkingType.COVERED_GARAGE,
            location = ParkingLocation(SupportedCity.LAHORE, "DHA Phase 6", 31.47, 74.45),
            hourlyRatePkr = 200,
            dailyRatePkr = 1500,
            monthlyRatePkr = null,
            monthlyEnabled = false,
            totalBays = 1,
            availableBays = 1,
            supportedVehicles = setOf(VehicleType.CAR),
            amenities = setOf(ParkingAmenity.CCTV),
            schedule = AvailabilitySchedule(ParkingDayOfWeek.entries.toSet(), 0, 0, true),
            isApproved = false,
            isDemoListing = true,
          )
        assertTrue(parkingRepo.createOrUpdateListing(hostId, createdSpace, publishImmediately = true) is HostListingMutationResult.Success)

        val custUser =
          (authRepo.registerLocalAccount(
            DemoRegisterRequest(
              role = UserRole.CUSTOMER,
              fullName = "Hira Real Customer",
              email = "hira.cust@gmail.com",
              password = "password123",
              confirmPassword = "password123",
              preferredCity = SupportedCity.LAHORE,
            )
          ) as DemoAuthResult.Success).user
        custId = custUser.id

        val realVehicle =
          userRepo.addVehicleForUser(custId, VehicleType.CAR, "Hira Alto", "LEA-4421", true).getOrThrow()

        val futureStart =
          LocalDate.now(BookingAvailabilityValidator.PAKISTAN_ZONE)
            .plusDays(3)
            .atTime(10, 0)
            .atZone(BookingAvailabilityValidator.PAKISTAN_ZONE)
            .toInstant()
            .toEpochMilli()

        val bookingRes =
          bookingRepo.createBookingWithAvailabilityCheck(
            BookingCreateRequest(
              spaceId = userSpaceId,
              customerId = custId,
              vehicleId = realVehicle.id,
              bookingType = BookingType.HOURLY,
              units = 2,
              startEpochMs = futureStart,
              confirmDemoPaymentImmediately = true,
            )
          )
        assertTrue(bookingRes is BookingMutationResult.Success)
        userBookingId = (bookingRes as BookingMutationResult.Success).booking.id

        // Execute safe cleanup (SEED_VERSION 1 -> 2)
        DemoSeedInitializer(db).seedIfNeeded()

        // Verify legacy seed records were purged
        assertNull("Seeded customer must be deleted", db.userDao().getUserById("demo-cust-lhr"))
        assertNull("Seeded host must be deleted", db.userDao().getUserById("demo-host-lhr"))
        assertNull("Seeded space must be deleted", db.parkingDao().getSpaceById("demo-lhr-01"))
        assertNull("Seeded bay must be deleted", db.parkingDao().getBayById("demo-lhr-01-bay-a1"))
        assertNull("Seeded booking must be deleted", db.bookingDao().getBookingById("demo-bkg-01"))
        assertNull("Seeded vehicle must be deleted", db.userDao().getVehicleById("demo-veh-1"))
      }

      // Simulate app restart: close DB, reopen file, run seedIfNeeded again
      db.close()
      db =
        Room.databaseBuilder(context, ParkSpaceDemoDatabase::class.java, dbName)
          .addMigrations(*ParkSpaceDemoDatabase.MIGRATIONS)
          .allowMainThreadQueries()
          .build()

      runBlocking(Dispatchers.IO) {
        DemoSeedInitializer(db).seedIfNeeded()

        // Genuine user data MUST still exist and fictional seed MUST NOT reappear
        assertNull(db.parkingDao().getSpaceById("demo-lhr-01"))
        assertNotNull("Host-created listing must persist across restart", db.parkingDao().getSpaceById(userSpaceId))
        assertNotNull("User host account must remain intact", db.userDao().getUserById(hostId))
        assertNotNull("User customer account must remain intact", db.userDao().getUserById(custId))
        assertNotNull("User booking must remain intact", db.bookingDao().getBookingById(userBookingId))

        // Customer discovers ONLY the Host-created approved listing
        val approvedInLahore = DemoParkingRepositoryImpl(db).observeApprovedSpacesByCity(SupportedCity.LAHORE).first()
        assertEquals(listOf(userSpaceId), approvedInLahore.map { it.id })
        assertFalse(approvedInLahore.any { it.id.startsWith("demo-lhr-") })
      }
    } finally {
      db.close()
      context.deleteDatabase(dbName)
    }
  }

  @Test
  fun `discovery screen shows No parking spaces available yet when city has zero approved listings`() {
    val db =
      Room.inMemoryDatabaseBuilder(context, ParkSpaceDemoDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    try {
      runBlocking(Dispatchers.IO) { DemoSeedInitializer(db).seedIfNeeded() }
      val sessionManager = DemoSessionManager(DemoUserProfileRepositoryImpl(db.userDao(), db))
      val vm = DiscoveryViewModel(DemoParkingRepositoryImpl(db), sessionManager)
      val emptyData =
        DiscoveryScreenData(
          filter = ParkingDiscoveryFilter(city = SupportedCity.LAHORE),
          viewMode = DiscoveryViewMode.LIST,
          mapRenderStatus = MapRenderStatus.Ready,
          result =
            DiscoveryFilterResult(
              listings = emptyList(),
              markers = emptyList(),
              availableLocalities = emptyList(),
              totalApprovedInCity = 0,
            ),
          selectedListing = null,
          cameraTriggerVersion = 0,
        )

      composeRule.setContent {
        ParkSpaceTheme {
          ParkingDiscoveryScreen(
            uiState = UiState.Success(emptyData),
            viewModel = vm,
            onBack = {},
            onViewDetails = {},
            mapContentOverride = {},
          )
        }
      }

      composeRule.waitForIdle()
      composeRule.onNodeWithTag("discovery_empty_state").assertIsDisplayed()
      composeRule.onNodeWithText("No parking spaces available yet.").assertIsDisplayed()
      composeRule.onNodeWithTag("empty_explore_cities_button").assertIsDisplayed()
    } finally {
      db.close()
    }
  }
}
