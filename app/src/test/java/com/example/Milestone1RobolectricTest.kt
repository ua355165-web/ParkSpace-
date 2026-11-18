package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.config.SupportedCity
import com.example.core.config.UserRole
import com.example.core.data.demo.DemoBookingRepositoryImpl
import com.example.core.data.demo.DemoParkingRepositoryImpl
import com.example.core.data.demo.DemoUserProfileRepositoryImpl
import com.example.core.database.ParkSpaceDemoDatabase
import com.example.core.domain.model.ParkingAmenity
import com.example.core.domain.model.ParkingType
import com.example.core.domain.model.VehicleType
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
    userRepo = DemoUserProfileRepositoryImpl(database.userDao())
    parkingRepo = DemoParkingRepositoryImpl(database.parkingDao())
    bookingRepo = DemoBookingRepositoryImpl(database.bookingDao())
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

    val allSpaces = parkingRepo.observeAllSpacesIncludingPending().first()
    SupportedCity.entries.forEach { city ->
      val citySpaces = allSpaces.filter { it.location.city == city }
      assertTrue("Expected >= 3 spaces in ${city.displayName}", citySpaces.size >= 3)
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

      // Direct switch to admin without sandbox gate acknowledgement is rejected
      val directAdminSwitch =
        sessionManager.switchToDemoUser(DemoSeedDataset.users.first { it.role == UserRole.SUPER_ADMIN }.id)
      assertTrue(directAdminSwitch is DemoSandboxActionResult.Rejected)

      // Unlock without acknowledgement is rejected
      val unacknowledgedAdmin = sessionManager.unlockPrivateDemoAdminSandbox(false)
      assertTrue(unacknowledgedAdmin is DemoSandboxActionResult.Rejected)

      // Valid customer switch works
      val custSwitch = sessionManager.switchToDemoUser("demo-cust-lhr")
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

    // Customer session cannot access Host or Admin
    sessionManager.switchToDemoUser("demo-cust-lhr")
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
    sessionManager.switchToDemoUser("demo-host-khi")
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
