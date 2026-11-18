package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.core.config.BookingType
import com.example.core.config.SupportedCity
import com.example.core.config.UserRole
import com.example.core.data.demo.DemoAdminRepositoryImpl
import com.example.core.data.demo.DemoAuthRepositoryImpl
import com.example.core.data.demo.DemoBookingRepositoryImpl
import com.example.core.data.demo.DemoDisputeRepositoryImpl
import com.example.core.data.demo.DemoNotificationRepositoryImpl
import com.example.core.data.demo.DemoParkingRepositoryImpl
import com.example.core.data.demo.DemoUserProfileRepositoryImpl
import com.example.core.database.ParkSpaceDemoDatabase
import com.example.core.domain.model.AdminAuditAction
import com.example.core.domain.model.AdminListingModerationState
import com.example.core.domain.model.AvailabilitySchedule
import com.example.core.domain.model.DemoCommissionRule
import com.example.core.domain.model.DemoPaymentMethod
import com.example.core.domain.model.DisputeCategory
import com.example.core.domain.model.DisputeStatus
import com.example.core.domain.model.HostVerificationStatus
import com.example.core.domain.model.ParkSpaceNotificationType
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
import com.example.core.domain.repository.DemoSignInRequest
import com.example.core.domain.repository.DisputeMutationResult
import com.example.core.domain.repository.HostListingMutationResult
import com.example.core.navigation.NavGuardResult
import com.example.core.navigation.ParkSpaceNavGuard
import com.example.core.navigation.ParkSpaceRoute
import com.example.demo.sandbox.DemoSandboxActionResult
import com.example.demo.sandbox.DemoSessionManager
import com.example.demo.sandbox.SharedPrefsDemoSessionPersistence
import com.example.demo.seed.DemoSeedInitializer
import com.example.feature.admin.AdminConsoleTab
import com.example.feature.admin.AdminDashboardViewModel
import com.example.feature.admin.ui.PrivateAdminConsoleScreen
import com.example.feature.booking.domain.BookingPricingEngine
import com.example.ui.theme.ParkSpaceTheme
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
class Milestone6AdminRobolectricTest {

  @get:Rule val composeRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var database: ParkSpaceDemoDatabase
  private lateinit var seedInitializer: DemoSeedInitializer
  private lateinit var userRepo: DemoUserProfileRepositoryImpl
  private lateinit var authRepo: DemoAuthRepositoryImpl
  private lateinit var parkingRepo: DemoParkingRepositoryImpl
  private lateinit var bookingRepo: DemoBookingRepositoryImpl
  private lateinit var disputeRepo: DemoDisputeRepositoryImpl
  private lateinit var notifRepo: DemoNotificationRepositoryImpl
  private lateinit var persistence: SharedPrefsDemoSessionPersistence
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
    seedInitializer = DemoSeedInitializer(database)
    userRepo = DemoUserProfileRepositoryImpl(database.userDao(), database)
    authRepo = DemoAuthRepositoryImpl(database)
    parkingRepo = DemoParkingRepositoryImpl(database)
    bookingRepo = DemoBookingRepositoryImpl(database)
    disputeRepo = DemoDisputeRepositoryImpl(database)
    notifRepo = DemoNotificationRepositoryImpl(database)
    persistence = SharedPrefsDemoSessionPersistence(context)
    sessionManager = DemoSessionManager(userRepo, persistence)
    adminRepo = DemoAdminRepositoryImpl(database) { sessionManager.session.value }
  }

  @After
  fun tearDown() {
    if (::database.isInitialized && database.isOpen) {
      database.close()
    }
  }

  private suspend fun registerCustomer(
    email: String = "ali.customer@parkspace.demo",
    city: SupportedCity = SupportedCity.LAHORE,
  ): String {
    val result =
      authRepo.registerLocalAccount(
        DemoRegisterRequest(
          fullName = "Ali Customer",
          email = email,
          password = "Password123!",
          confirmPassword = "Password123!",
          role = UserRole.CUSTOMER,
          preferredCity = city,
        )
      ) as DemoAuthResult.Success
    return result.user.id
  }

  private suspend fun registerHost(
    email: String = "sana.host@parkspace.demo",
    city: SupportedCity = SupportedCity.LAHORE,
    alias: String = "Gulberg Secure Park",
  ): String {
    val result =
      authRepo.registerLocalAccount(
        DemoRegisterRequest(
          fullName = "Sana Host",
          email = email,
          password = "Password123!",
          confirmPassword = "Password123!",
          role = UserRole.HOST,
          preferredCity = city,
          businessAlias = alias,
        )
      ) as DemoAuthResult.Success
    return result.user.id
  }

  private fun sampleSpace(
    id: String,
    hostId: String,
    city: SupportedCity,
    title: String = "Gulberg Prime Garage",
  ): ParkingSpace =
    ParkingSpace(
      id = id,
      hostId = hostId,
      title = title,
      description = "Verified private covered lot with CCTV.",
      parkingType = ParkingType.COVERED_GARAGE,
      location =
        ParkingLocation(
          city = city,
          areaLabel = "Main Boulevard",
          publicMaskedLatitude = city.centerLatitude,
          publicMaskedLongitude = city.centerLongitude,
          maskRadiusMeters = 200,
          privateGateSummary = "Guard Gate B",
          privateExactLatitude = city.centerLatitude + 0.001,
          privateExactLongitude = city.centerLongitude + 0.001,
        ),
      hourlyRatePkr = 200,
      dailyRatePkr = 1500,
      monthlyRatePkr = 22000,
      monthlyEnabled = true,
      totalBays = 2,
      availableBays = 2,
      supportedVehicles = setOf(VehicleType.CAR, VehicleType.SUV),
      amenities = setOf(ParkingAmenity.COVERED, ParkingAmenity.CCTV),
      schedule =
        AvailabilitySchedule(
          openDays = ParkingDayOfWeek.entries.toSet(),
          openHour = 0,
          closeHour = 24,
          is24Hours = true,
        ),
      isApproved = false,
      isDemoListing = true,
      photoTags = listOf("Entrance Gate", "CCTV"),
    )

  @Test
  fun `scenario 1 - private super admin access isolation and zero pre-created admin db accounts`() {
    runBlocking(Dispatchers.IO) {
      seedInitializer.seedIfNeeded()

      // No pre-created Super Admin in demo_users table
      val adminDbUsers = database.userDao().observeUsersByRole(UserRole.SUPER_ADMIN).first()
      assertTrue("demo_users must contain zero SUPER_ADMIN accounts", adminDbUsers.isEmpty())

      // Public signup & login reject SUPER_ADMIN
      val regAdmin =
        authRepo.registerLocalAccount(
          DemoRegisterRequest(
            fullName = "Fake Admin",
            email = "admin@parkspace.demo",
            password = "Password123!",
            confirmPassword = "Password123!",
            role = UserRole.SUPER_ADMIN,
            preferredCity = SupportedCity.LAHORE,
          )
        )
      assertTrue(regAdmin is DemoAuthResult.Rejected)

      val signInAdmin =
        authRepo.signInLocalAccount(
          DemoSignInRequest(
            role = UserRole.SUPER_ADMIN,
            email = "admin@parkspace.demo",
            password = "Password123!",
          )
        )
      assertTrue(signInAdmin is DemoAuthResult.Rejected)
      assertTrue(
        ParkSpaceNavGuard.evaluateAccess(
          ParkSpaceRoute.DemoAuth(UserRole.SUPER_ADMIN, true),
          sessionManager.session.value,
        ) is NavGuardResult.Denied
      )

      // Unauthenticated and Customer/Host callers cannot unlock Admin route or invoke Admin repo
      assertTrue(
        ParkSpaceNavGuard.evaluateAccess(
          ParkSpaceRoute.PrivateAdminHome,
          sessionManager.session.value,
        ) is NavGuardResult.Denied
      )

      val customerId = registerCustomer()
      val hostId = registerHost()

      val customerAttempt =
        adminRepo.approveHostKyc(customerId, hostId, "Unauthorized bypass", 1_000L)
      assertTrue(customerAttempt is AdminMutationResult.Rejected)

      assertTrue(sessionManager.switchToDemoUser(hostId) is DemoSandboxActionResult.Success)
      val hostNavAttempt =
        ParkSpaceNavGuard.evaluateAccess(
          ParkSpaceRoute.PrivateAdminHome,
          sessionManager.session.value,
        )
      assertTrue(hostNavAttempt is NavGuardResult.Denied)

      val hostAttempt =
        adminRepo.approveHostKyc(DemoSessionManager.PRIVATE_DEMO_ADMIN_ID, hostId, "Session mismatch", 1_000L)
      assertTrue(
        "Admin action must be rejected when active session is HOST even if actorAdminId string is forged",
        hostAttempt is AdminMutationResult.Rejected,
      )

      // Without explicit acknowledgment, unlockPrivateDemoAdminSandbox stays locked
      assertTrue(
        sessionManager.unlockPrivateDemoAdminSandbox(acknowledgedLocalTestingOnly = false)
          is DemoSandboxActionResult.Rejected
      )
      assertFalse(sessionManager.session.value.isAdminSandboxUnlocked)

      // With explicit acknowledgment, Private Demo Admin Sandbox unlocks safely
      assertTrue(
        sessionManager.unlockPrivateDemoAdminSandbox(acknowledgedLocalTestingOnly = true)
          is DemoSandboxActionResult.Success
      )
      assertTrue(sessionManager.session.value.isAdminSandboxUnlocked)
      assertEquals(UserRole.SUPER_ADMIN, sessionManager.session.value.activeRole)
      assertTrue(
        ParkSpaceNavGuard.evaluateAccess(
          ParkSpaceRoute.PrivateAdminHome,
          sessionManager.session.value,
        ) is NavGuardResult.Allowed
      )
    }
  }

  @Test
  fun `scenario 2 - host kyc verification queue review reject with reason and approve unlocks publishing`() {
    runBlocking(Dispatchers.IO) {
      val hostId = registerHost()

      // Pending Host cannot publish immediately
      val pendingPublishAttempt =
        parkingRepo.createOrUpdateListing(
          hostId = hostId,
          space = sampleSpace("sp-kyc-1", hostId, SupportedCity.LAHORE),
          publishImmediately = true,
        )
      assertTrue(pendingPublishAttempt is HostListingMutationResult.Rejected)

      // Unlock Private Super Admin Sandbox
      sessionManager.unlockPrivateDemoAdminSandbox(acknowledgedLocalTestingOnly = true)
      val adminId = DemoSessionManager.PRIVATE_DEMO_ADMIN_ID

      val initialQueue = adminRepo.observeHostKycQueue().first()
      assertEquals(1, initialQueue.size)
      assertEquals(HostVerificationStatus.PENDING_REVIEW, initialQueue.first().hostProfile.verificationStatus)

      // Rejecting with too short / blank reason fails
      val blankReject = adminRepo.rejectHostKyc(adminId, hostId, "no", 2_000L)
      assertTrue(blankReject is AdminMutationResult.Rejected)

      // Reject with clear reason
      val rejectResult = adminRepo.rejectHostKyc(adminId, hostId, "Missing CNIC front photo", 3_000L)
      assertTrue(rejectResult is AdminMutationResult.Success)

      val rejectedProfile = userRepo.observeHostProfile(hostId).first()!!
      assertEquals(HostVerificationStatus.REJECTED, rejectedProfile.verificationStatus)
      assertEquals("Missing CNIC front photo", rejectedProfile.verificationNote)

      // Rejected Host still cannot publish
      val rejectedPublishAttempt =
        parkingRepo.createOrUpdateListing(
          hostId = hostId,
          space = sampleSpace("sp-kyc-1", hostId, SupportedCity.LAHORE),
          publishImmediately = true,
        )
      assertTrue(rejectedPublishAttempt is HostListingMutationResult.Rejected)

      // Admin approves Host KYC
      val approveResult = adminRepo.approveHostKyc(adminId, hostId, "Documents verified", 4_000L)
      assertTrue(approveResult is AdminMutationResult.Success)

      val approvedProfile = userRepo.observeHostProfile(hostId).first()!!
      assertEquals(HostVerificationStatus.APPROVED, approvedProfile.verificationStatus)
      assertEquals("Documents verified", approvedProfile.verificationNote)

      // Host notifications received moderation updates
      val hostNotifs = notifRepo.observeNotificationsForUser(hostId).first()
      assertEquals(2, hostNotifs.count { it.type == ParkSpaceNotificationType.ADMIN_MODERATION })

      // Approved Host can now create & publish
      val publishAfterApprove =
        parkingRepo.createOrUpdateListing(
          hostId = hostId,
          space = sampleSpace("sp-kyc-1", hostId, SupportedCity.LAHORE),
          publishImmediately = true,
        )
      assertTrue(publishAfterApprove is HostListingMutationResult.Success)
    }
  }

  @Test
  fun `scenario 3 - listing approval queue controls discovery visibility and records rejection reasons`() {
    runBlocking(Dispatchers.IO) {
      val hostId = registerHost(city = SupportedCity.ISLAMABAD, alias = "F-7 Parkers")
      sessionManager.unlockPrivateDemoAdminSandbox(true)
      val adminId = DemoSessionManager.PRIVATE_DEMO_ADMIN_ID

      // Approve Host KYC first so Host can create a draft listing
      assertTrue(adminRepo.approveHostKyc(adminId, hostId, "Verified", 1_000L) is AdminMutationResult.Success)

      // Host creates draft listing (isApproved = false)
      val draftSave =
        parkingRepo.createOrUpdateListing(
          hostId = hostId,
          space = sampleSpace("sp-isb-1", hostId, SupportedCity.ISLAMABAD, "F-7 Markaz Basement"),
          publishImmediately = false,
        )
      assertTrue(draftSave is HostListingMutationResult.Success)

      // Not visible in Customer Discovery yet
      assertTrue(
        parkingRepo.observeApprovedSpacesByCity(SupportedCity.ISLAMABAD).first().isEmpty()
      )

      val queuePending = adminRepo.observeListingModerationQueue().first()
      assertEquals(1, queuePending.size)
      assertEquals(AdminListingModerationState.PENDING_REVIEW, queuePending.first().moderationState)

      // Admin approves listing -> appears in Customer Discovery
      assertTrue(
        adminRepo.approveListing(adminId, "sp-isb-1", "Safety & pricing verified", 2_000L)
          is AdminMutationResult.Success
      )
      val discoveryAfterApprove =
        parkingRepo.observeApprovedSpacesByCity(SupportedCity.ISLAMABAD).first()
      assertEquals(1, discoveryAfterApprove.size)
      assertEquals("sp-isb-1", discoveryAfterApprove.first().id)

      // Admin rejects listing -> hidden from Customer Discovery with reason
      assertTrue(
        adminRepo.rejectListing(adminId, "sp-isb-1", "Unclear entrance signage photo", 3_000L)
          is AdminMutationResult.Success
      )
      assertTrue(
        parkingRepo.observeApprovedSpacesByCity(SupportedCity.ISLAMABAD).first().isEmpty()
      )
      val moderatedSpace = parkingRepo.getSpaceById("sp-isb-1")!!
      assertFalse(moderatedSpace.isApproved)
      assertTrue(moderatedSpace.isRejectedByAdmin)
      assertEquals("Unclear entrance signage photo", moderatedSpace.moderationNote)
    }
  }

  @Test
  fun `scenario 4 - disputes management workflow enforces transitions and notifies both parties`() {
    runBlocking(Dispatchers.IO) {
      val customerId = registerCustomer()
      val hostId = registerHost()
      sessionManager.unlockPrivateDemoAdminSandbox(true)
      val adminId = DemoSessionManager.PRIVATE_DEMO_ADMIN_ID

      adminRepo.approveHostKyc(adminId, hostId, "OK", 1_000L)
      parkingRepo.createOrUpdateListing(
        hostId = hostId,
        space = sampleSpace("sp-disp-1", hostId, SupportedCity.LAHORE),
        publishImmediately = true,
      )
      val vehicle =
        userRepo.addVehicleForUser(customerId, VehicleType.CAR, "Civic", "LEA-9911").getOrThrow()

      val bookingResult =
        bookingRepo.createBookingWithAvailabilityCheck(
          BookingCreateRequest(
            spaceId = "sp-disp-1",
            customerId = customerId,
            vehicleId = vehicle.id,
            bookingType = BookingType.HOURLY,
            units = 2,
            startEpochMs = Instant.now().plusSeconds(3600).toEpochMilli(),
            confirmDemoPaymentImmediately = true,
            paymentMethod = DemoPaymentMethod.JAZZCASH_DEMO,
          )
        ) as BookingMutationResult.Success

      val filedDispute =
        disputeRepo.fileDispute(
          bookingId = bookingResult.booking.id,
          reporterUserId = customerId,
          category = DisputeCategory.GATE_ACCESS_ISSUE,
          description = "Guard initially asked for extra fee at gate.",
          nowEpochMs = 2_000L,
        ) as DisputeMutationResult.Success

      // Invalid transition back to OPEN rejected
      assertTrue(
        adminRepo.updateDisputeStatusAsAdmin(
          adminId,
          filedDispute.dispute.id,
          DisputeStatus.OPEN,
          "Revert",
          3_000L,
        ) is AdminMutationResult.Rejected
      )

      // OPEN -> UNDER_REVIEW
      assertTrue(
        adminRepo.updateDisputeStatusAsAdmin(
          adminId,
          filedDispute.dispute.id,
          DisputeStatus.UNDER_REVIEW,
          "Checking entry logs with Host",
          4_000L,
        ) is AdminMutationResult.Success
      )

      // UNDER_REVIEW -> RESOLVED
      assertTrue(
        adminRepo.updateDisputeStatusAsAdmin(
          adminId,
          filedDispute.dispute.id,
          DisputeStatus.RESOLVED,
          "Confirmed free entry; issued formal warning to Host",
          5_000L,
        ) is AdminMutationResult.Success
      )

      val resolvedQueueItem =
        adminRepo.observeAllDisputesQueue().first().first { it.dispute.id == filedDispute.dispute.id }
      assertEquals(DisputeStatus.RESOLVED, resolvedQueueItem.dispute.status)
      assertEquals(
        "Confirmed free entry; issued formal warning to Host",
        resolvedQueueItem.dispute.resolutionNote,
      )

      // Finalized dispute cannot be re-dismissed
      assertTrue(
        adminRepo.updateDisputeStatusAsAdmin(
          adminId,
          filedDispute.dispute.id,
          DisputeStatus.DISMISSED,
          "Try overwrite",
          6_000L,
        ) is AdminMutationResult.Rejected
      )

      val customerNotifs = notifRepo.observeNotificationsForUser(customerId).first()
      val hostNotifs = notifRepo.observeNotificationsForUser(hostId).first()
      assertTrue(customerNotifs.any { it.type == ParkSpaceNotificationType.DISPUTE_UPDATE })
      assertTrue(hostNotifs.any { it.type == ParkSpaceNotificationType.DISPUTE_UPDATE })
    }
  }

  @Test
  fun `scenario 5 - city analytics are zero on empty marketplace and update strictly from real user actions`() {
    runBlocking(Dispatchers.IO) {
      val emptyAnalytics = adminRepo.observePlatformAnalytics().first()
      assertEquals(0, emptyAnalytics.totalCustomersCount)
      assertEquals(0, emptyAnalytics.totalHostsCount)
      assertEquals(0, emptyAnalytics.totalListingsCount)
      assertEquals(0, emptyAnalytics.totalBookingsCount)
      assertEquals(0, emptyAnalytics.totalDemoGrossVolumePkr)
      assertEquals(SupportedCity.entries.size, emptyAnalytics.cityMetrics.size)
      assertTrue(emptyAnalytics.cityMetrics.all { it.totalListingsCount == 0 && it.demoGrossVolumePkr == 0 })

      // Register Host + Customer in Karachi, create listing + booking
      val hostKhr = registerHost(email = "khr@host.demo", city = SupportedCity.KARACHI, alias = "Clifton Lot")
      val custKhr = registerCustomer(email = "khr@cust.demo", city = SupportedCity.KARACHI)
      sessionManager.unlockPrivateDemoAdminSandbox(true)
      val adminId = DemoSessionManager.PRIVATE_DEMO_ADMIN_ID

      adminRepo.approveHostKyc(adminId, hostKhr, "Verified KHR", 1_000L)
      parkingRepo.createOrUpdateListing(
        hostId = hostKhr,
        space = sampleSpace("sp-khr-1", hostKhr, SupportedCity.KARACHI, "Clifton Block 4 Lot"),
        publishImmediately = true,
      )
      val vehicle = userRepo.addVehicleForUser(custKhr, VehicleType.CAR, "Alto", "KHI-100").getOrThrow()
      val bookingRes =
        bookingRepo.createBookingWithAvailabilityCheck(
          BookingCreateRequest(
            spaceId = "sp-khr-1",
            customerId = custKhr,
            vehicleId = vehicle.id,
            bookingType = BookingType.HOURLY,
            units = 3,
            startEpochMs = Instant.now().plusSeconds(3600).toEpochMilli(),
            confirmDemoPaymentImmediately = true,
            paymentMethod = DemoPaymentMethod.EASYPAISA_DEMO,
          )
        ) as BookingMutationResult.Success

      val updatedAnalytics = adminRepo.observePlatformAnalytics().first()
      assertEquals(1, updatedAnalytics.totalCustomersCount)
      assertEquals(1, updatedAnalytics.totalHostsCount)
      assertEquals(1, updatedAnalytics.approvedHostsCount)
      assertEquals(1, updatedAnalytics.totalListingsCount)
      assertEquals(1, updatedAnalytics.totalBookingsCount)
      assertEquals(bookingRes.booking.totalAmountPkr, updatedAnalytics.totalDemoGrossVolumePkr)

      val karachiMetric = updatedAnalytics.cityMetrics.first { it.city == SupportedCity.KARACHI }
      val lahoreMetric = updatedAnalytics.cityMetrics.first { it.city == SupportedCity.LAHORE }
      assertEquals(1, karachiMetric.totalListingsCount)
      assertEquals(2, karachiMetric.totalBaysCount)
      assertEquals(1, karachiMetric.totalBookingsCount)
      assertEquals(bookingRes.booking.totalAmountPkr, karachiMetric.demoGrossVolumePkr)
      assertEquals(0, lahoreMetric.totalListingsCount)
      assertEquals(0, lahoreMetric.demoGrossVolumePkr)
    }
  }

  @Test
  fun `scenario 6 - demo commission rules persist and drive new booking pricing plus audit logs start empty`() {
    runBlocking(Dispatchers.IO) {
      // Audit logs initially empty; commission rule falls back to default 8%
      assertTrue(adminRepo.observeAuditLogs().first().isEmpty())
      assertEquals(DemoCommissionRule.DEFAULT, adminRepo.observeActiveCommissionRule().first())

      sessionManager.unlockPrivateDemoAdminSandbox(true)
      val adminId = DemoSessionManager.PRIVATE_DEMO_ADMIN_ID

      // Out-of-range commission rejected
      assertTrue(
        adminRepo.updateCommissionRule(adminId, 50, 20, 1500, "Too high", 1_000L)
          is AdminMutationResult.Rejected
      )

      // Update Demo commission rule to 12%, min Rs 50, max Rs 2000
      val savedRuleRes =
        adminRepo.updateCommissionRule(
          actorAdminId = adminId,
          commissionPercent = 12,
          minFeePkr = 50,
          maxFeePkr = 2000,
          note = "Ramzan promo Demo adjustment",
          nowEpochMs = 2_000L,
        )
      assertTrue(savedRuleRes is AdminMutationResult.Success)

      val activeRule = adminRepo.observeActiveCommissionRule().first()
      assertEquals(12, activeRule.commissionPercent)
      assertEquals(50, activeRule.minFeePkr)
      assertEquals(2000, activeRule.maxFeePkr)

      // Verify BookingPricingEngine & DemoBookingRepositoryImpl apply the persisted 12% rule
      val hostId = registerHost(city = SupportedCity.FAISALABAD)
      val customerId = registerCustomer(city = SupportedCity.FAISALABAD)
      adminRepo.approveHostKyc(adminId, hostId, "OK", 3_000L)
      val space = sampleSpace("sp-fsd-1", hostId, SupportedCity.FAISALABAD, "D-Ground Safe Park")
      parkingRepo.createOrUpdateListing(hostId, space, publishImmediately = true)
      val vehicle =
        userRepo.addVehicleForUser(customerId, VehicleType.CAR, "Corolla", "FSD-404").getOrThrow()

      val bookingRes =
        bookingRepo.createBookingWithAvailabilityCheck(
          BookingCreateRequest(
            spaceId = space.id,
            customerId = customerId,
            vehicleId = vehicle.id,
            bookingType = BookingType.HOURLY,
            units = 5, // base = 5 * 200 = 1000 PKR; 12% = 120 PKR; total = 1120 PKR
            startEpochMs = Instant.now().plusSeconds(3600).toEpochMilli(),
            confirmDemoPaymentImmediately = true,
          )
        ) as BookingMutationResult.Success

      val expectedQuote =
        BookingPricingEngine.calculate(space, BookingType.HOURLY, 5, activeRule).getOrThrow()
      assertEquals(1000, expectedQuote.baseAmountPkr)
      assertEquals(120, expectedQuote.serviceFeePkr)
      assertEquals(120, bookingRes.booking.serviceFeePkr)
      assertEquals(1120, bookingRes.booking.totalAmountPkr)

      // Audit logs captured genuine admin actions
      val logs = adminRepo.observeAuditLogs().first()
      assertEquals(2, logs.size)
      assertTrue(logs.any { it.actionType == AdminAuditAction.COMMISSION_RULE_UPDATED })
      assertTrue(logs.any { it.actionType == AdminAuditAction.HOST_KYC_APPROVED })
    }
  }

  @Test
  fun `scenario 7 - private admin console ui renders tabs kyc approval analytics and commission form`() {
    val hostId = runBlocking(Dispatchers.IO) {
      val hId = registerHost(city = SupportedCity.LAHORE, alias = "Liberty Secure Garage")
      sessionManager.unlockPrivateDemoAdminSandbox(true)
      hId
    }

    val adminViewModel = AdminDashboardViewModel(adminRepo, sessionManager)

    composeRule.setContent {
      ParkSpaceTheme {
        val state by adminViewModel.uiState.collectAsStateWithLifecycle()
        PrivateAdminConsoleScreen(
          uiState = state,
          onBack = {},
          onOpenSettings = {},
          onSelectTab = adminViewModel::selectTab,
          onApproveHostKyc = adminViewModel::approveHostKyc,
          onRejectHostKyc = adminViewModel::rejectHostKyc,
          onApproveListing = adminViewModel::approveListing,
          onRejectListing = adminViewModel::rejectListing,
          onUpdateDisputeStatus = adminViewModel::updateDisputeStatus,
          onUpdateCommissionRule = adminViewModel::updateCommissionRule,
          onDismissFeedback = adminViewModel::dismissFeedback,
        )
      }
    }

    composeRule.waitUntil(5_000L) {
      ShadowLooper.idleMainLooper()
      adminViewModel.uiState.value.hostKycQueue.isNotEmpty()
    }

    // 1. Host KYC tab shows pending Host and approves via modal dialog
    composeRule.onNodeWithTag("admin_shell_card").assertIsDisplayed()
    composeRule.onNodeWithTag("admin_kyc_card_$hostId").assertIsDisplayed()
    composeRule.onNodeWithTag("admin_approve_host_$hostId").performScrollTo().performClick()
    composeRule.onNodeWithTag("admin_moderation_note_input").performTextInput("CNIC checked")
    composeRule.onNodeWithTag("admin_confirm_moderation_button").performClick()

    composeRule.waitUntil(5_000L) {
      ShadowLooper.idleMainLooper()
      adminViewModel.uiState.value.hostKycQueue.firstOrNull()?.hostProfile?.verificationStatus ==
        HostVerificationStatus.APPROVED
    }

    // 2. Analytics tab displays all 4 cities
    composeRule.onNodeWithTag("admin_tab_${AdminConsoleTab.ANALYTICS.name.lowercase()}").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("admin_analytics_summary_card").assertIsDisplayed()
    composeRule.onNodeWithTag("admin_city_analytics_lahore").assertIsDisplayed()

    // 3. Rules & Logs tab saves Demo commission and lists Audit Log
    composeRule.onNodeWithTag("admin_tab_${AdminConsoleTab.CONFIG_AND_LOGS.name.lowercase()}").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("admin_commission_percent_input").performScrollTo().performTextReplacement("10")
    composeRule.onNodeWithTag("admin_save_commission_button").performScrollTo().performClick()

    composeRule.waitUntil(5_000L) {
      ShadowLooper.idleMainLooper()
      adminViewModel.uiState.value.commissionRule.commissionPercent == 10 &&
        adminViewModel.uiState.value.auditLogs.any {
          it.actionType == AdminAuditAction.COMMISSION_RULE_UPDATED
        }
    }
    composeRule.onNodeWithText("Demo Commission Rule Updated").assertIsDisplayed()
  }

  @Test
  fun `scenario 8 - room MIGRATION_5_6 preserves existing records and creates admin tables`() {
    val dbName = "parkspace_migration_5_6_test.db"
    context.deleteDatabase(dbName)

    val v5Callback =
      object : SupportSQLiteOpenHelper.Callback(5) {
        override fun onCreate(db: SupportSQLiteDatabase) {
          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `demo_seed_metadata` (`key` TEXT NOT NULL, `seedVersion` INTEGER NOT NULL, `seededAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`key`))"
          )
          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `demo_users` (`id` TEXT NOT NULL, `fullName` TEXT NOT NULL, `phoneMasked` TEXT NOT NULL, `emailMasked` TEXT NOT NULL, `role` TEXT NOT NULL, `city` TEXT NOT NULL, `isDemoAccount` INTEGER NOT NULL, `createdAtEpochMs` INTEGER NOT NULL, `emailNormalized` TEXT, `passwordHash` TEXT, `passwordSalt` TEXT, `isSelfRegisteredDemoAccount` INTEGER NOT NULL, PRIMARY KEY(`id`))"
          )
          db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_demo_users_emailNormalized_role` ON `demo_users` (`emailNormalized`, `role`)"
          )
          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `demo_customer_profiles` (`userId` TEXT NOT NULL, `preferredCity` TEXT NOT NULL, `defaultVehicleId` TEXT, `referralCode` TEXT NOT NULL, PRIMARY KEY(`userId`))"
          )
          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `demo_host_profiles` (`userId` TEXT NOT NULL, `displayBusinessAlias` TEXT NOT NULL, `verificationStatus` TEXT NOT NULL, `operatingCities` TEXT NOT NULL, `totalSpacesCount` INTEGER NOT NULL, `joinedEpochMs` INTEGER NOT NULL, PRIMARY KEY(`userId`))"
          )
          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `demo_vehicles` (`id` TEXT NOT NULL, `ownerUserId` TEXT NOT NULL, `vehicleType` TEXT NOT NULL, `label` TEXT NOT NULL, `plateNumberMasked` TEXT NOT NULL, `isDefault` INTEGER NOT NULL, PRIMARY KEY(`id`))"
          )
          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `demo_parking_spaces` (`id` TEXT NOT NULL, `hostId` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `parkingType` TEXT NOT NULL, `city` TEXT NOT NULL, `areaLabel` TEXT NOT NULL, `publicMaskedLatitude` REAL NOT NULL, `publicMaskedLongitude` REAL NOT NULL, `maskRadiusMeters` INTEGER NOT NULL, `privateGateSummary` TEXT, `privateExactLatitude` REAL, `privateExactLongitude` REAL, `hourlyRatePkr` INTEGER NOT NULL, `dailyRatePkr` INTEGER NOT NULL, `monthlyRatePkr` INTEGER, `monthlyEnabled` INTEGER NOT NULL, `totalBays` INTEGER NOT NULL, `availableBays` INTEGER NOT NULL, `supportedVehicles` TEXT NOT NULL, `amenities` TEXT NOT NULL, `openDays` TEXT NOT NULL, `openHour` INTEGER NOT NULL, `closeHour` INTEGER NOT NULL, `is24Hours` INTEGER NOT NULL, `blackoutDatesIso` TEXT NOT NULL, `isApproved` INTEGER NOT NULL, `isDemoListing` INTEGER NOT NULL, `photoTags` TEXT NOT NULL, `photoUris` TEXT NOT NULL, PRIMARY KEY(`id`))"
          )
          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `demo_parking_bays` (`id` TEXT NOT NULL, `spaceId` TEXT NOT NULL, `bayCode` TEXT NOT NULL, `supportedVehicles` TEXT NOT NULL, `isCovered` INTEGER NOT NULL, `isActiveInDemo` INTEGER NOT NULL, PRIMARY KEY(`id`))"
          )
          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `demo_bookings` (`id` TEXT NOT NULL, `bookingCode` TEXT NOT NULL, `spaceId` TEXT NOT NULL, `bayId` TEXT NOT NULL, `customerId` TEXT NOT NULL, `hostId` TEXT NOT NULL, `vehicleId` TEXT NOT NULL, `city` TEXT NOT NULL, `bookingType` TEXT NOT NULL, `status` TEXT NOT NULL, `startEpochMs` INTEGER NOT NULL, `endEpochMs` INTEGER NOT NULL, `baseAmountPkr` INTEGER NOT NULL, `serviceFeePkr` INTEGER NOT NULL, `totalAmountPkr` INTEGER NOT NULL, `gatePinReleased` INTEGER NOT NULL, `isDemoRecord` INTEGER NOT NULL, `demoPaymentStatus` TEXT NOT NULL, `demoPaymentMethod` TEXT, `demoEntryPin` TEXT, `checkedInAtEpochMs` INTEGER, `checkedOutAtEpochMs` INTEGER, `cancelledAtEpochMs` INTEGER, `refundAmountPkr` INTEGER, `cancellationPolicySummary` TEXT, `createdAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`id`))"
          )
          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `demo_payment_transactions` (`id` TEXT NOT NULL, `bookingId` TEXT, `bookingCode` TEXT, `customerId` TEXT NOT NULL, `hostId` TEXT, `spaceId` TEXT, `method` TEXT NOT NULL, `transactionType` TEXT NOT NULL, `amountPkr` INTEGER NOT NULL, `referenceCode` TEXT NOT NULL, `summary` TEXT NOT NULL, `isDemoOnly` INTEGER NOT NULL, `createdAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`id`))"
          )
          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `demo_chat_messages` (`id` TEXT NOT NULL, `bookingId` TEXT NOT NULL, `spaceId` TEXT NOT NULL, `customerId` TEXT NOT NULL, `hostId` TEXT NOT NULL, `senderUserId` TEXT NOT NULL, `senderRole` TEXT NOT NULL, `senderDisplayName` TEXT NOT NULL, `body` TEXT NOT NULL, `sentAtEpochMs` INTEGER NOT NULL, `isReadByCustomer` INTEGER NOT NULL, `isReadByHost` INTEGER NOT NULL, PRIMARY KEY(`id`))"
          )
          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `demo_reviews` (`id` TEXT NOT NULL, `bookingId` TEXT NOT NULL, `spaceId` TEXT NOT NULL, `customerId` TEXT NOT NULL, `customerDisplayName` TEXT NOT NULL, `hostId` TEXT NOT NULL, `rating` INTEGER NOT NULL, `comment` TEXT NOT NULL, `hostReply` TEXT, `hostRepliedAtEpochMs` INTEGER, `createdAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`id`))"
          )
          db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_demo_reviews_bookingId` ON `demo_reviews` (`bookingId`)"
          )
          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `demo_disputes` (`id` TEXT NOT NULL, `bookingId` TEXT NOT NULL, `bookingCode` TEXT NOT NULL, `spaceId` TEXT NOT NULL, `customerId` TEXT NOT NULL, `hostId` TEXT NOT NULL, `reporterUserId` TEXT NOT NULL, `reporterRole` TEXT NOT NULL, `category` TEXT NOT NULL, `description` TEXT NOT NULL, `status` TEXT NOT NULL, `hostResponseNote` TEXT, `resolutionNote` TEXT, `createdAtEpochMs` INTEGER NOT NULL, `updatedAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`id`))"
          )
          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `demo_referrals` (`id` TEXT NOT NULL, `referrerUserId` TEXT NOT NULL, `referredUserId` TEXT NOT NULL, `referralCodeUsed` TEXT NOT NULL, `rewardAmountPkr` INTEGER NOT NULL, `status` TEXT NOT NULL, `isDemoOnly` INTEGER NOT NULL, `createdAtEpochMs` INTEGER NOT NULL, `creditedAtEpochMs` INTEGER, PRIMARY KEY(`id`))"
          )
          db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_demo_referrals_referredUserId` ON `demo_referrals` (`referredUserId`)"
          )
          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `demo_notifications` (`id` TEXT NOT NULL, `recipientUserId` TEXT NOT NULL, `type` TEXT NOT NULL, `title` TEXT NOT NULL, `message` TEXT NOT NULL, `relatedBookingId` TEXT, `relatedSpaceId` TEXT, `isRead` INTEGER NOT NULL, `createdAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`id`))"
          )
          db.execSQL(
            "INSERT INTO demo_host_profiles (userId, displayBusinessAlias, verificationStatus, operatingCities, totalSpacesCount, joinedEpochMs) VALUES ('v5-host-1', 'V5 Johar Lot', 'PENDING_REVIEW', 'LAHORE', 1, 1000)"
          )
          db.execSQL(
            """
            INSERT INTO demo_parking_spaces (
              id, hostId, title, description, parkingType, city, areaLabel,
              publicMaskedLatitude, publicMaskedLongitude, maskRadiusMeters,
              privateGateSummary, privateExactLatitude, privateExactLongitude,
              hourlyRatePkr, dailyRatePkr, monthlyRatePkr, monthlyEnabled,
              totalBays, availableBays, supportedVehicles, amenities, openDays,
              openHour, closeHour, is24Hours, blackoutDatesIso, isApproved, isDemoListing, photoTags, photoUris
            ) VALUES (
              'v5-space-1', 'v5-host-1', 'Johar Town Gate 1', 'Migrated v5 listing',
              'COVERED_GARAGE', 'LAHORE', 'Johar Town',
              31.4697, 74.2728, 200,
              'Gate 1', 31.4701, 74.2731,
              180, 1400, 20000, 1,
              2, 2, 'CAR', 'CCTV|COVERED', 'MON|TUE|WED|THU|FRI|SAT|SUN',
              0, 24, 1, '', 0, 1, 'Entrance Gate', ''
            )
            """.trimIndent()
          )
        }

        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
      }

    val v5Helper =
      FrameworkSQLiteOpenHelperFactory().create(
        SupportSQLiteOpenHelper.Configuration.builder(context)
          .name(dbName)
          .callback(v5Callback)
          .build()
      )
    v5Helper.writableDatabase.close()
    v5Helper.close()

    val migratedDb =
      Room.databaseBuilder(context, ParkSpaceDemoDatabase::class.java, dbName)
        .addMigrations(*ParkSpaceDemoDatabase.MIGRATIONS)
        .allowMainThreadQueries()
        .build()

    try {
      runBlocking {
        val host = migratedDb.userDao().getHostProfileOnce("v5-host-1")
        assertNotNull(host)
        assertEquals("V5 Johar Lot", host?.displayBusinessAlias)
        assertEquals(null, host?.verificationNote)

        val space = migratedDb.parkingDao().getSpaceById("v5-space-1")
        assertNotNull(space)
        assertEquals("Johar Town Gate 1", space?.title)
        assertFalse(space?.isRejectedByAdmin ?: true)

        assertEquals(0, migratedDb.adminDao().observeAllAuditLogs().first().size)
      }
    } finally {
      migratedDb.close()
      context.deleteDatabase(dbName)
    }
  }
}
