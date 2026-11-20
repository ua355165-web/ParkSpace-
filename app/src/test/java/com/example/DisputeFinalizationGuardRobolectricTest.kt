package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.config.BookingType
import com.example.core.config.SupportedCity
import com.example.core.config.UserRole
import com.example.core.data.demo.DemoAdminRepositoryImpl
import com.example.core.data.demo.DemoAuthRepositoryImpl
import com.example.core.data.demo.DemoBookingRepositoryImpl
import com.example.core.data.demo.DemoDisputeRepositoryImpl
import com.example.core.data.demo.DemoParkingRepositoryImpl
import com.example.core.data.demo.DemoUserProfileRepositoryImpl
import com.example.core.database.ParkSpaceDemoDatabase
import com.example.core.domain.model.AvailabilitySchedule
import com.example.core.domain.model.DemoPaymentMethod
import com.example.core.domain.model.DisputeCategory
import com.example.core.domain.model.DisputeStatus
import com.example.core.domain.model.ParkingAmenity
import com.example.core.domain.model.ParkingDayOfWeek
import com.example.core.domain.model.ParkingLocation
import com.example.core.domain.model.ParkingSpace
import com.example.core.domain.model.ParkingType
import com.example.core.domain.model.VehicleType
import com.example.core.domain.repository.AdminMutationResult
import com.example.core.domain.repository.BookingCreateRequest
import com.example.core.domain.repository.BookingMutationResult
import com.example.core.domain.repository.DemoAuthResult
import com.example.core.domain.repository.DemoRegisterRequest
import com.example.core.domain.repository.DisputeMutationResult
import com.example.core.domain.repository.HostListingMutationResult
import com.example.demo.sandbox.DemoSandboxActionResult
import com.example.demo.sandbox.DemoSessionManager
import com.example.demo.sandbox.SharedPrefsDemoSessionPersistence
import com.example.feature.booking.domain.BookingAvailabilityValidator
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DisputeFinalizationGuardRobolectricTest {

  private lateinit var context: Context
  private lateinit var database: ParkSpaceDemoDatabase
  private lateinit var userRepo: DemoUserProfileRepositoryImpl
  private lateinit var authRepo: DemoAuthRepositoryImpl
  private lateinit var parkingRepo: DemoParkingRepositoryImpl
  private lateinit var bookingRepo: DemoBookingRepositoryImpl
  private lateinit var disputeRepo: DemoDisputeRepositoryImpl
  private lateinit var sessionManager: DemoSessionManager
  private lateinit var adminRepo: DemoAdminRepositoryImpl

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    context
      .getSharedPreferences("parkspace_demo_session", Context.MODE_PRIVATE)
      .edit()
      .clear()
      .commit()
    database =
      Room.inMemoryDatabaseBuilder(context, ParkSpaceDemoDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    userRepo = DemoUserProfileRepositoryImpl(database.userDao(), database)
    authRepo = DemoAuthRepositoryImpl(database)
    parkingRepo = DemoParkingRepositoryImpl(database)
    bookingRepo = DemoBookingRepositoryImpl(database)
    disputeRepo = DemoDisputeRepositoryImpl(database)
    val persistence = SharedPrefsDemoSessionPersistence(context)
    sessionManager = DemoSessionManager(userRepo, persistence)
    adminRepo = DemoAdminRepositoryImpl(database) { sessionManager.session.value }
  }

  @After
  fun tearDown() {
    if (::database.isInitialized && database.isOpen) {
      database.close()
    }
  }

  private suspend fun seedBookingAndDispute(suffix: String): Triple<String, String, String> {
    val customer =
      (authRepo.registerLocalAccount(
        DemoRegisterRequest(
          fullName = "Ayesha Driver $suffix",
          email = "ayesha.$suffix@parkspace.demo",
          password = "Password123!",
          confirmPassword = "Password123!",
          role = UserRole.CUSTOMER,
          preferredCity = SupportedCity.LAHORE,
        )
      ) as DemoAuthResult.Success).user
    val vehicle =
      userRepo.addVehicleForUser(customer.id, VehicleType.CAR, "Civic", "LEA-10$suffix", true).getOrThrow()

    val host =
      (authRepo.registerLocalAccount(
        DemoRegisterRequest(
          fullName = "Bilal Host $suffix",
          email = "bilal.$suffix@parkspace.demo",
          password = "Password123!",
          confirmPassword = "Password123!",
          role = UserRole.HOST,
          preferredCity = SupportedCity.LAHORE,
          businessAlias = "Gulberg Lot $suffix",
        )
      ) as DemoAuthResult.Success).user

    assertTrue(
      sessionManager.unlockPrivateDemoAdminSandbox(acknowledgedLocalTestingOnly = true)
        is DemoSandboxActionResult.Success
    )
    val adminId = DemoSessionManager.PRIVATE_DEMO_ADMIN_ID
    assertTrue(adminRepo.approveHostKyc(adminId, host.id, "CNIC verified", 1_000L) is AdminMutationResult.Success)

    val space =
      ParkingSpace(
        id = "space-guard-$suffix",
        hostId = host.id,
        title = "Gulberg Covered Deck $suffix",
        description = "Private guard-monitored bays.",
        parkingType = ParkingType.COVERED_GARAGE,
        location = ParkingLocation(SupportedCity.LAHORE, "Gulberg III", 31.512, 74.348),
        hourlyRatePkr = 150,
        dailyRatePkr = 1200,
        monthlyRatePkr = null,
        monthlyEnabled = false,
        totalBays = 2,
        availableBays = 2,
        supportedVehicles = setOf(VehicleType.CAR),
        amenities = setOf(ParkingAmenity.CCTV),
        schedule = AvailabilitySchedule(ParkingDayOfWeek.entries.toSet(), 0, 23, true),
        isApproved = false,
        isDemoListing = true,
      )
    assertTrue(
      parkingRepo.createOrUpdateListing(host.id, space, publishImmediately = true)
        is HostListingMutationResult.Success
    )
    assertTrue(adminRepo.approveListing(adminId, space.id, "Listing approved", 2_000L) is AdminMutationResult.Success)

    val futureStart =
      LocalDate.now(BookingAvailabilityValidator.PAKISTAN_ZONE)
        .plusDays(2)
        .atTime(11, 0)
        .atZone(BookingAvailabilityValidator.PAKISTAN_ZONE)
        .toInstant()
        .toEpochMilli()

    val bookingRes =
      bookingRepo.createBookingWithAvailabilityCheck(
        BookingCreateRequest(
          spaceId = space.id,
          customerId = customer.id,
          vehicleId = vehicle.id,
          bookingType = BookingType.HOURLY,
          units = 1,
          startEpochMs = futureStart,
          confirmDemoPaymentImmediately = true,
          paymentMethod = DemoPaymentMethod.JAZZCASH_DEMO,
        )
      )
    assertTrue(bookingRes is BookingMutationResult.Success)
    val booking = (bookingRes as BookingMutationResult.Success).booking

    val disputeRes =
      disputeRepo.fileDispute(
        bookingId = booking.id,
        reporterUserId = customer.id,
        category = DisputeCategory.GATE_ACCESS_ISSUE,
        description = "Gate barrier delayed opening on arrival.",
        nowEpochMs = 3_000L,
      )
    assertTrue(disputeRes is DisputeMutationResult.Success)
    val dispute = (disputeRes as DisputeMutationResult.Success).dispute
    return Triple(dispute.id, customer.id, host.id)
  }

  @Test
  fun `admin resolved dispute cannot be modified reopened or overwritten by customer or host`() {
    runBlocking(Dispatchers.IO) {
      val (disputeId, customerId, hostId) = seedBookingAndDispute("res")
      val adminId = DemoSessionManager.PRIVATE_DEMO_ADMIN_ID

      val adminResolve =
        adminRepo.updateDisputeStatusAsAdmin(
          actorAdminId = adminId,
          disputeId = disputeId,
          targetStatus = DisputeStatus.RESOLVED,
          note = "Demo refund approved by Admin.",
          nowEpochMs = 4_000L,
        )
      assertTrue(adminResolve is AdminMutationResult.Success)

      // Customer and Host attempts to reopen, dismiss, overwrite resolution, or post host response must fail
      assertTrue(
        disputeRepo.resolveDisputeAsCustomerOrHost(
          disputeId = disputeId,
          actorUserId = customerId,
          resolutionNote = "Trying to reopen",
          targetStatus = DisputeStatus.OPEN,
          nowEpochMs = 5_000L,
        ) is DisputeMutationResult.Rejected
      )
      assertTrue(
        disputeRepo.resolveDisputeAsCustomerOrHost(
          disputeId = disputeId,
          actorUserId = hostId,
          resolutionNote = "Overwriting to dismissed",
          targetStatus = DisputeStatus.DISMISSED,
          nowEpochMs = 5_100L,
        ) is DisputeMutationResult.Rejected
      )
      assertTrue(
        disputeRepo.resolveDisputeAsCustomerOrHost(
          disputeId = disputeId,
          actorUserId = customerId,
          resolutionNote = "Overwriting admin note",
          targetStatus = DisputeStatus.RESOLVED,
          nowEpochMs = 5_200L,
        ) is DisputeMutationResult.Rejected
      )
      assertTrue(
        disputeRepo.resolveDisputeAsCustomerOrHost(
          disputeId = disputeId,
          actorUserId = hostId,
          resolutionNote = "Moving back to review",
          targetStatus = DisputeStatus.UNDER_REVIEW,
          nowEpochMs = 5_300L,
        ) is DisputeMutationResult.Rejected
      )
      assertTrue(
        disputeRepo.respondToDisputeAsHost(
          disputeId = disputeId,
          hostId = hostId,
          responseNote = "Late host note after resolution",
          nowEpochMs = 5_400L,
        ) is DisputeMutationResult.Rejected
      )

      val stored = disputeRepo.observeDisputesForCustomer(customerId).first().first { it.id == disputeId }
      assertEquals(DisputeStatus.RESOLVED, stored.status)
      assertEquals("Demo refund approved by Admin.", stored.resolutionNote)
      assertNull(stored.hostResponseNote)
      assertEquals(4_000L, stored.updatedAtEpochMs)
    }
  }

  @Test
  fun `open to under review workflow stays valid while admin dismissed dispute blocks customer and host edits`() {
    runBlocking(Dispatchers.IO) {
      val (disputeId, customerId, hostId) = seedBookingAndDispute("dis")
      val adminId = DemoSessionManager.PRIVATE_DEMO_ADMIN_ID

      // Valid OPEN -> UNDER_REVIEW transition by Host response
      val hostResponseRes =
        disputeRepo.respondToDisputeAsHost(
          disputeId = disputeId,
          hostId = hostId,
          responseNote = "Gate sensor checked and operational.",
          nowEpochMs = 4_000L,
        )
      assertTrue(hostResponseRes is DisputeMutationResult.Success)
      assertEquals(DisputeStatus.UNDER_REVIEW, (hostResponseRes as DisputeMutationResult.Success).dispute.status)

      // Cannot revert UNDER_REVIEW back to OPEN
      assertTrue(
        disputeRepo.resolveDisputeAsCustomerOrHost(
          disputeId = disputeId,
          actorUserId = customerId,
          resolutionNote = "Reverting back to open",
          targetStatus = DisputeStatus.OPEN,
          nowEpochMs = 4_500L,
        ) is DisputeMutationResult.Rejected
      )

      // Admin finalizes as DISMISSED
      val adminDismiss =
        adminRepo.updateDisputeStatusAsAdmin(
          actorAdminId = adminId,
          disputeId = disputeId,
          targetStatus = DisputeStatus.DISMISSED,
          note = "Evidence confirms gate opened on time.",
          nowEpochMs = 5_000L,
        )
      assertTrue(adminDismiss is AdminMutationResult.Success)

      // Post-dismissal Customer/Host modifications must be rejected
      assertTrue(
        disputeRepo.resolveDisputeAsCustomerOrHost(
          disputeId = disputeId,
          actorUserId = customerId,
          resolutionNote = "Forcing resolved after dismiss",
          targetStatus = DisputeStatus.RESOLVED,
          nowEpochMs = 6_000L,
        ) is DisputeMutationResult.Rejected
      )
      assertTrue(
        disputeRepo.respondToDisputeAsHost(
          disputeId = disputeId,
          hostId = hostId,
          responseNote = "Altering dismissed host note",
          nowEpochMs = 6_100L,
        ) is DisputeMutationResult.Rejected
      )

      val stored = disputeRepo.observeDisputesForHost(hostId).first().first { it.id == disputeId }
      assertEquals(DisputeStatus.DISMISSED, stored.status)
      assertEquals("Evidence confirms gate opened on time.", stored.resolutionNote)
      assertEquals("Gate sensor checked and operational.", stored.hostResponseNote)
      assertEquals(5_000L, stored.updatedAtEpochMs)
    }
  }
}
