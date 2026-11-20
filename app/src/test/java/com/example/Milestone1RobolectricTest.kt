package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.config.SupportedCity
import com.example.core.config.UserRole
import com.example.core.data.demo.DemoAuthRepositoryImpl
import com.example.core.data.demo.DemoBookingRepositoryImpl
import com.example.core.data.demo.DemoParkingRepositoryImpl
import com.example.core.data.demo.DemoUserProfileRepositoryImpl
import com.example.core.database.ParkSpaceDemoDatabase
import com.example.core.domain.model.AvailabilitySchedule
import com.example.core.domain.model.HostVerificationStatus
import com.example.core.domain.model.ParkingAmenity
import com.example.core.domain.model.ParkingDayOfWeek
import com.example.core.domain.model.ParkingLocation
import com.example.core.domain.model.ParkingSpace
import com.example.core.domain.model.ParkingType
import com.example.core.domain.model.VehicleType
import com.example.core.domain.repository.DemoAuthResult
import com.example.core.domain.repository.DemoRegisterRequest
import com.example.core.domain.repository.HostListingMutationResult
import com.example.core.navigation.NavGuardResult
import com.example.core.navigation.ParkSpaceNavGuard
import com.example.core.navigation.ParkSpaceRoute
import com.example.demo.sandbox.DemoSandboxActionResult
import com.example.demo.sandbox.DemoSessionManager
import com.example.demo.seed.DemoSeedDataset
import com.example.demo.seed.DemoSeedInitializer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class Milestone1RobolectricTest {

  private lateinit var database: ParkSpaceDemoDatabase
  private lateinit var seedInitializer: DemoSeedInitializer
  private lateinit var userRepo: DemoUserProfileRepositoryImpl
  private lateinit var authRepo: DemoAuthRepositoryImpl
  private lateinit var parkingRepo: DemoParkingRepositoryImpl
  private lateinit var bookingRepo: DemoBookingRepositoryImpl
  private lateinit var sessionManager: DemoSessionManager

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database =
      Room.inMemoryDatabaseBuilder(context, ParkSpaceDemoDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    seedInitializer = DemoSeedInitializer(database)
    userRepo = DemoUserProfileRepositoryImpl(database.userDao(), database)
    authRepo = DemoAuthRepositoryImpl(database)
    parkingRepo = DemoParkingRepositoryImpl(database)
    bookingRepo = DemoBookingRepositoryImpl(database)
    sessionManager = DemoSessionManager(userRepo)
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun `database initializes and seed prevents duplicates across repeated runs`() = runTest {
    seedInitializer.seedIfNeeded()
    seedInitializer.seedIfNeeded()
    seedInitializer.seedIfNeeded()

    assertEquals(DemoSeedDataset.users.size, database.userDao().countUsers())
    assertEquals(DemoSeedDataset.parkingSpaces.size, database.parkingDao().countSpaces())
    assertEquals(DemoSeedDataset.parkingBays.size, database.parkingDao().countBays())
    assertEquals(DemoSeedDataset.sampleBookings.size, database.bookingDao().countBookings())
  }

  @Test
  fun `seed data covers all four Pakistani cities and varied attributes`() = runTest {
    seedInitializer.seedIfNeeded()

    // Empty-marketplace requirement: fresh installation starts with 0 spaces across all 4 cities
    val initialSpaces = parkingRepo.observeAllSpacesIncludingPending().first()
    assertTrue("Expected fresh marketplace to start empty", initialSpaces.isEmpty())

    // Register & approve an actual Host creating listings across all four Pakistani cities
    val hostUser =
      (authRepo.registerLocalAccount(
        DemoRegisterRequest(
          role = UserRole.HOST,
          fullName = "Zainab Host",
          email = "zainab.m1@parkspace.demo",
          password = "password123",
          confirmPassword = "password123",
          preferredCity = SupportedCity.LAHORE,
          businessAlias = "Nationwide Host",
        )
      ) as DemoAuthResult.Success).user
    val hostEntity = database.userDao().getHostProfileOnce(hostUser.id)!!
    database.userDao().upsertHostProfile(hostEntity.copy(verificationStatus = HostVerificationStatus.APPROVED))

    val cityListings =
      listOf(
        ParkingSpace(
          id = "m1-lhr-1",
          hostId = hostUser.id,
          title = "Lahore Covered Garage",
          description = "Covered garage with CCTV",
          parkingType = ParkingType.COVERED_GARAGE,
          location = ParkingLocation(SupportedCity.LAHORE, "Gulberg III", 31.512, 74.348),
          hourlyRatePkr = 150,
          dailyRatePkr = 1200,
          monthlyRatePkr = 20_000,
          monthlyEnabled = true,
          totalBays = 2,
          availableBays = 2,
          supportedVehicles = setOf(VehicleType.CAR, VehicleType.SUV),
          amenities = setOf(ParkingAmenity.COVERED, ParkingAmenity.CCTV),
          schedule = AvailabilitySchedule(ParkingDayOfWeek.entries.toSet(), 0, 23, true),
          isApproved = false,
          isDemoListing = true,
        ),
        ParkingSpace(
          id = "m1-isb-1",
          hostId = hostUser.id,
          title = "Islamabad Open Lot",
          description = "Open surface lot",
          parkingType = ParkingType.OPEN_LOT,
          location = ParkingLocation(SupportedCity.ISLAMABAD, "F-7 Markaz", 33.721, 73.056),
          hourlyRatePkr = 100,
          dailyRatePkr = 800,
          monthlyRatePkr = null,
          monthlyEnabled = false,
          totalBays = 2,
          availableBays = 2,
          supportedVehicles = setOf(VehicleType.BIKE, VehicleType.CAR),
          amenities = setOf(ParkingAmenity.CCTV),
          schedule = AvailabilitySchedule(ParkingDayOfWeek.entries.toSet(), 0, 23, true),
          isApproved = false,
          isDemoListing = true,
        ),
        ParkingSpace(
          id = "m1-fsd-1",
          hostId = hostUser.id,
          title = "Faisalabad Plaza",
          description = "D-Ground bays",
          parkingType = ParkingType.COVERED_GARAGE,
          location = ParkingLocation(SupportedCity.FAISALABAD, "D-Ground", 31.412, 73.112),
          hourlyRatePkr = 120,
          dailyRatePkr = 900,
          monthlyRatePkr = null,
          monthlyEnabled = false,
          totalBays = 2,
          availableBays = 2,
          supportedVehicles = setOf(VehicleType.CAR),
          amenities = setOf(ParkingAmenity.COVERED),
          schedule = AvailabilitySchedule(ParkingDayOfWeek.entries.toSet(), 0, 23, true),
          isApproved = false,
          isDemoListing = true,
        ),
        ParkingSpace(
          id = "m1-khi-1",
          hostId = hostUser.id,
          title = "Karachi Clifton Yard",
          description = "Secure coastal bays",
          parkingType = ParkingType.OPEN_LOT,
          location = ParkingLocation(SupportedCity.KARACHI, "Clifton", 24.813, 67.029),
          hourlyRatePkr = 180,
          dailyRatePkr = 1400,
          monthlyRatePkr = 24_000,
          monthlyEnabled = true,
          totalBays = 2,
          availableBays = 2,
          supportedVehicles = setOf(VehicleType.SUV, VehicleType.BIKE),
          amenities = setOf(ParkingAmenity.CCTV),
          schedule = AvailabilitySchedule(ParkingDayOfWeek.entries.toSet(), 0, 23, true),
          isApproved = false,
          isDemoListing = true,
        ),
      )

    cityListings.forEach { space ->
      assertTrue(
        parkingRepo.createOrUpdateListing(hostUser.id, space, publishImmediately = true)
          is HostListingMutationResult.Success
      )
    }

    val allSpaces = parkingRepo.observeAllSpacesIncludingPending().first()
    SupportedCity.entries.forEach { city ->
      assertTrue(
        "Expected Host-created space in ${city.displayName}",
        allSpaces.any { it.location.city == city },
      )
    }

    val vehiclesCovered = allSpaces.flatMap { it.supportedVehicles }.toSet()
    assertTrue(VehicleType.BIKE in vehiclesCovered)
    assertTrue(VehicleType.CAR in vehiclesCovered)
    assertTrue(VehicleType.SUV in vehiclesCovered)

    val amenitiesCovered = allSpaces.flatMap { it.amenities }.toSet()
    assertTrue(ParkingAmenity.COVERED in amenitiesCovered)
    assertTrue(ParkingAmenity.CCTV in amenitiesCovered)
    assertTrue(allSpaces.any { ParkingAmenity.COVERED !in it.amenities })
    assertTrue(allSpaces.any { it.monthlyEnabled })
    assertTrue(allSpaces.any { !it.monthlyEnabled })
    assertTrue(allSpaces.any { it.parkingType == ParkingType.OPEN_LOT })
    assertTrue(allSpaces.any { it.parkingType == ParkingType.COVERED_GARAGE })
    assertTrue(allSpaces.all { it.isDemoListing })
  }

  @Test
  fun `demo session enforces private admin restrictions and no public admin registration`() =
    runTest {
      seedInitializer.seedIfNeeded()

      // Public registration of SUPER_ADMIN is forbidden
      val regAdminResult = sessionManager.validatePublicRegistrationRole(UserRole.SUPER_ADMIN)
      assertTrue(regAdminResult is DemoSandboxActionResult.Rejected)

      val regCustomerResult = sessionManager.validatePublicRegistrationRole(UserRole.CUSTOMER)
      assertTrue(regCustomerResult is DemoSandboxActionResult.Success)

      // Direct switch or activation to private admin without sandbox gate acknowledgement is rejected
      val directAdminSwitch = sessionManager.switchToDemoUser(DemoSessionManager.PRIVATE_DEMO_ADMIN_ID)
      assertTrue(directAdminSwitch is DemoSandboxActionResult.Rejected)
      val directAdminActivation = sessionManager.activateAuthenticatedUser(DemoSeedDataset.privateDemoAdminUser)
      assertTrue(directAdminActivation is DemoSandboxActionResult.Rejected)

      // Unlock without acknowledgement is rejected
      val unacknowledgedAdmin = sessionManager.unlockPrivateDemoAdminSandbox(false)
      assertTrue(unacknowledgedAdmin is DemoSandboxActionResult.Rejected)

      // Valid registered customer activation works
      val custUser =
        (authRepo.registerLocalAccount(
          DemoRegisterRequest(
            role = UserRole.CUSTOMER,
            fullName = "Hamza Customer",
            email = "hamza.m1@parkspace.demo",
            password = "password123",
            confirmPassword = "password123",
            preferredCity = SupportedCity.LAHORE,
          )
        ) as DemoAuthResult.Success).user
      val custSwitch = sessionManager.activateAuthenticatedUser(custUser)
      assertTrue(custSwitch is DemoSandboxActionResult.Success)
      assertEquals(UserRole.CUSTOMER, sessionManager.session.value.activeRole)
      assertFalse(sessionManager.session.value.isAdminSandboxUnlocked)

      // Valid private admin sandbox unlock works only with acknowledgement
      val ackAdmin = sessionManager.unlockPrivateDemoAdminSandbox(true)
      assertTrue(ackAdmin is DemoSandboxActionResult.Success)
      assertEquals(UserRole.SUPER_ADMIN, sessionManager.session.value.activeRole)
      assertTrue(sessionManager.session.value.isAdminSandboxUnlocked)
    }

  @Test
  fun `navigation guard restricts routes strictly by active role`() = runTest {
    seedInitializer.seedIfNeeded()

    // Unauthenticated session
    val guestSession = sessionManager.session.value
    assertTrue(
      ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.WelcomeEntry, guestSession)
        is NavGuardResult.Allowed
    )
    assertTrue(
      ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.CustomerHome, guestSession)
        is NavGuardResult.Denied
    )
    assertTrue(
      ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.HostHome, guestSession)
        is NavGuardResult.Denied
    )
    assertTrue(
      ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.PrivateAdminHome, guestSession)
        is NavGuardResult.Denied
    )
    assertTrue(
      ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.SharedProfileSettings, guestSession)
        is NavGuardResult.Denied
    )

    val custUser =
      (authRepo.registerLocalAccount(
        DemoRegisterRequest(
          role = UserRole.CUSTOMER,
          fullName = "Noor Customer",
          email = "noor.m1@parkspace.demo",
          password = "password123",
          confirmPassword = "password123",
          preferredCity = SupportedCity.LAHORE,
        )
      ) as DemoAuthResult.Success).user
    val hostUser =
      (authRepo.registerLocalAccount(
        DemoRegisterRequest(
          role = UserRole.HOST,
          fullName = "Faisal Host",
          email = "faisal.m1@parkspace.demo",
          password = "password123",
          confirmPassword = "password123",
          preferredCity = SupportedCity.KARACHI,
          businessAlias = "Clifton Bays",
        )
      ) as DemoAuthResult.Success).user

    // Customer session cannot access Host or Admin
    assertTrue(sessionManager.activateAuthenticatedUser(custUser) is DemoSandboxActionResult.Success)
    val custSession = sessionManager.session.value
    assertTrue(
      ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.CustomerHome, custSession)
        is NavGuardResult.Allowed
    )
    assertTrue(
      ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.HostHome, custSession)
        is NavGuardResult.Denied
    )
    assertTrue(
      ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.PrivateAdminHome, custSession)
        is NavGuardResult.Denied
    )

    // Host session cannot access Customer or Admin
    assertTrue(sessionManager.activateAuthenticatedUser(hostUser) is DemoSandboxActionResult.Success)
    val hostSession = sessionManager.session.value
    assertTrue(
      ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.HostHome, hostSession)
        is NavGuardResult.Allowed
    )
    assertTrue(
      ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.CustomerHome, hostSession)
        is NavGuardResult.Denied
    )
    assertTrue(
      ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.PrivateAdminHome, hostSession)
        is NavGuardResult.Denied
    )

    // Private Demo Admin sandbox session allows PrivateAdminHome
    sessionManager.unlockPrivateDemoAdminSandbox(true)
    val adminSession = sessionManager.session.value
    assertTrue(
      ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.PrivateAdminHome, adminSession)
        is NavGuardResult.Allowed
    )
  }
}
