package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.common.UiState
import com.example.core.config.BookingType
import com.example.core.config.SupportedCity
import com.example.core.config.UserRole
import com.example.core.data.demo.DemoAdminRepositoryImpl
import com.example.core.data.demo.DemoAuthRepositoryImpl
import com.example.core.data.demo.DemoBookingRepositoryImpl
import com.example.core.data.demo.DemoParkingRepositoryImpl
import com.example.core.data.demo.DemoUserProfileRepositoryImpl
import com.example.core.database.ParkSpaceDemoDatabase
import com.example.core.domain.model.AvailabilitySchedule
import com.example.core.domain.model.BookingStatus
import com.example.core.domain.model.ParkingAmenity
import com.example.core.domain.model.ParkingDayOfWeek
import com.example.core.domain.model.ParkingLocation
import com.example.core.domain.model.ParkingSpace
import com.example.core.domain.model.ParkingType
import com.example.core.domain.model.UserProfile
import com.example.core.domain.model.VehicleType
import com.example.core.domain.repository.AdminMutationResult
import com.example.core.domain.repository.BookingCreateRequest
import com.example.core.domain.repository.BookingMutationResult
import com.example.core.domain.repository.DemoAuthResult
import com.example.core.domain.repository.DemoRegisterRequest
import com.example.core.domain.repository.HostListingMutationResult
import com.example.core.navigation.NavGuardResult
import com.example.core.navigation.ParkSpaceNavGuard
import com.example.core.navigation.ParkSpaceRoute
import com.example.demo.sandbox.DemoSandboxActionResult
import com.example.demo.sandbox.DemoSessionManager
import com.example.demo.seed.DemoSeedInitializer
import com.example.feature.booking.BookingCheckoutViewModel
import com.example.feature.booking.BookingHistoryViewModel
import com.example.feature.booking.domain.BookingAvailabilityValidator
import com.example.feature.booking.ui.BookingCheckoutScreen
import com.example.feature.booking.ui.BookingConfirmationScreen
import com.example.feature.booking.ui.CustomerBookingsScreen
import com.example.ui.theme.ParkSpaceTheme
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
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
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
class Milestone3BookingRobolectricTest {

  @get:Rule val composeRule = createComposeRule()

  private lateinit var database: ParkSpaceDemoDatabase
  private lateinit var seedInitializer: DemoSeedInitializer
  private lateinit var userRepo: DemoUserProfileRepositoryImpl
  private lateinit var authRepo: DemoAuthRepositoryImpl
  private lateinit var parkingRepo: DemoParkingRepositoryImpl
  private lateinit var bookingRepo: DemoBookingRepositoryImpl
  private lateinit var sessionManager: DemoSessionManager
  private lateinit var adminRepo: DemoAdminRepositoryImpl
  private lateinit var viewModelStore: ViewModelStore

  private data class M3TestFixtures(
    val hostUser: UserProfile,
    val cust1User: UserProfile,
    val cust1VehicleId: String,
    val cust2User: UserProfile,
    val cust2VehicleId: String,
    val space1Id: String,
    val space2Id: String,
  )

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
    adminRepo = DemoAdminRepositoryImpl(database) { sessionManager.session.value }
    viewModelStore = ViewModelStore()
  }

  @After
  fun tearDown() {
    viewModelStore.clear()
    database.close()
  }

  private suspend fun seedM3AccountsAndListings(): M3TestFixtures {
    seedInitializer.seedIfNeeded()
    val hostUser =
      (authRepo.registerLocalAccount(
        DemoRegisterRequest(
          role = UserRole.HOST,
          fullName = "Hamid M3 Host",
          email = "hamid.m3@parkspace.demo",
          password = "password123",
          confirmPassword = "password123",
          preferredCity = SupportedCity.LAHORE,
          businessAlias = "Gulberg Deck",
        )
      ) as DemoAuthResult.Success).user

    val cust1User =
      (authRepo.registerLocalAccount(
        DemoRegisterRequest(
          role = UserRole.CUSTOMER,
          fullName = "Sana Driver",
          email = "sana.m3@parkspace.demo",
          password = "password123",
          confirmPassword = "password123",
          preferredCity = SupportedCity.LAHORE,
        )
      ) as DemoAuthResult.Success).user
    val veh1 = userRepo.addVehicleForUser(cust1User.id, VehicleType.CAR, "Corolla", "LEA-101", true).getOrThrow()

    val cust2User =
      (authRepo.registerLocalAccount(
        DemoRegisterRequest(
          role = UserRole.CUSTOMER,
          fullName = "Umar Driver",
          email = "umar.m3@parkspace.demo",
          password = "password123",
          confirmPassword = "password123",
          preferredCity = SupportedCity.KARACHI,
        )
      ) as DemoAuthResult.Success).user
    val veh2 = userRepo.addVehicleForUser(cust2User.id, VehicleType.CAR, "Alto", "KHI-202", true).getOrThrow()

    assertTrue(sessionManager.unlockPrivateDemoAdminSandbox(true) is DemoSandboxActionResult.Success)
    val adminId = DemoSessionManager.PRIVATE_DEMO_ADMIN_ID
    assertTrue(adminRepo.approveHostKyc(adminId, hostUser.id, "KYC verified", 1_000L) is AdminMutationResult.Success)

    val space1 =
      ParkingSpace(
        id = "m3-space-gulberg",
        hostId = hostUser.id,
        title = "Gulberg Prime Garage",
        description = "Covered basement bays in Gulberg III.",
        parkingType = ParkingType.COVERED_GARAGE,
        location =
          ParkingLocation(
            city = SupportedCity.LAHORE,
            areaLabel = "Gulberg III",
            publicMaskedLatitude = SupportedCity.LAHORE.centerLatitude,
            publicMaskedLongitude = SupportedCity.LAHORE.centerLongitude,
            maskRadiusMeters = 200,
            privateGateSummary = "Gate B Guard Desk",
            privateExactLatitude = SupportedCity.LAHORE.centerLatitude + 0.001,
            privateExactLongitude = SupportedCity.LAHORE.centerLongitude + 0.001,
          ),
        hourlyRatePkr = 200,
        dailyRatePkr = 1500,
        monthlyRatePkr = 22_000,
        monthlyEnabled = true,
        totalBays = 2,
        availableBays = 2,
        supportedVehicles = setOf(VehicleType.CAR, VehicleType.SUV),
        amenities = setOf(ParkingAmenity.COVERED, ParkingAmenity.CCTV),
        schedule = AvailabilitySchedule(ParkingDayOfWeek.entries.toSet(), 0, 23, true),
        isApproved = false,
        isDemoListing = true,
      )
    val space2 =
      space1.copy(
        id = "m3-space-dha",
        title = "DHA Phase 5 Single Bay",
        location = space1.location.copy(areaLabel = "DHA Phase 5"),
        totalBays = 1,
        availableBays = 1,
        supportedVehicles = setOf(VehicleType.CAR),
      )

    listOf(space1, space2).forEach { sp ->
      assertTrue(parkingRepo.createOrUpdateListing(hostUser.id, sp, publishImmediately = true) is HostListingMutationResult.Success)
      assertTrue(adminRepo.approveListing(adminId, sp.id, "Approved for booking", 2_000L) is AdminMutationResult.Success)
    }
    sessionManager.signOutDemoSandbox()

    return M3TestFixtures(
      hostUser = hostUser,
      cust1User = cust1User,
      cust1VehicleId = veh1.id,
      cust2User = cust2User,
      cust2VehicleId = veh2.id,
      space1Id = space1.id,
      space2Id = space2.id,
    )
  }

  private fun nextSafeMorningEpochMs(daysAhead: Long = 2): Long =
    ZonedDateTime.now(BookingAvailabilityValidator.PAKISTAN_ZONE)
      .toLocalDate()
      .plusDays(daysAhead)
      .let { date ->
        val safeDate = if (date.monthValue == 12 && date.dayOfMonth == 25) date.plusDays(1) else date
        ZonedDateTime.of(safeDate, LocalTime.of(10, 0), BookingAvailabilityValidator.PAKISTAN_ZONE)
      }
      .toInstant()
      .toEpochMilli()

  @Test
  fun `room transactional booking creation saves confirmed and pending states, enforces capacity, and protects gate pin`() =
    runBlocking(Dispatchers.IO) {
      val fixtures = seedM3AccountsAndListings()
      val futureStart = nextSafeMorningEpochMs(2)

      // 1. Pending payment booking keeps gatePinReleased = false and demoEntryPin = null
      val pendingRes =
        bookingRepo.createBookingWithAvailabilityCheck(
          BookingCreateRequest(
            spaceId = fixtures.space1Id,
            customerId = fixtures.cust1User.id,
            vehicleId = fixtures.cust1VehicleId,
            bookingType = BookingType.HOURLY,
            units = 2,
            startEpochMs = futureStart,
            confirmDemoPaymentImmediately = false,
          )
        )
      assertTrue(pendingRes is BookingMutationResult.Success)
      val pending = (pendingRes as BookingMutationResult.Success).booking
      assertEquals(BookingStatus.PENDING_PAYMENT, pending.status)
      assertFalse(pending.gatePinReleased)
      assertNull(pending.demoEntryPin)

      // 2. Duplicate exact submission is rejected
      val dupRes =
        bookingRepo.createBookingWithAvailabilityCheck(
          BookingCreateRequest(
            spaceId = fixtures.space1Id,
            customerId = fixtures.cust1User.id,
            vehicleId = fixtures.cust1VehicleId,
            bookingType = BookingType.HOURLY,
            units = 2,
            startEpochMs = futureStart,
            confirmDemoPaymentImmediately = true,
          )
        )
      assertTrue(dupRes is BookingMutationResult.Rejected)

      // 3. Completing Demo payment unlocks PIN & transitions CONFIRMED -> CHECKED_IN -> CHECKED_OUT
      val paid =
        bookingRepo.confirmPendingBookingPayment(pending.id, fixtures.cust1User.id)
          as BookingMutationResult.Success
      assertEquals(BookingStatus.CONFIRMED, paid.booking.status)
      assertTrue(paid.booking.gatePinReleased)
      assertNotNull(paid.booking.demoEntryPin)

      val checkedIn =
        bookingRepo.checkInBooking(pending.id, fixtures.cust1User.id) as BookingMutationResult.Success
      assertEquals(BookingStatus.CHECKED_IN, checkedIn.booking.status)

      val checkedOut =
        bookingRepo.checkOutBooking(pending.id, fixtures.cust1User.id) as BookingMutationResult.Success
      assertEquals(BookingStatus.CHECKED_OUT, checkedOut.booking.status)

      // 4. Tight-capacity space (availableBays = 1) rejects overlapping second booking
      val firstBook =
        bookingRepo.createBookingWithAvailabilityCheck(
          BookingCreateRequest(
            spaceId = fixtures.space2Id,
            customerId = fixtures.cust1User.id,
            vehicleId = fixtures.cust1VehicleId,
            bookingType = BookingType.HOURLY,
            units = 2,
            startEpochMs = futureStart,
            confirmDemoPaymentImmediately = true,
          )
        )
      assertTrue(firstBook is BookingMutationResult.Success)

      val secondOverlapping =
        bookingRepo.createBookingWithAvailabilityCheck(
          BookingCreateRequest(
            spaceId = fixtures.space2Id,
            customerId = fixtures.cust2User.id,
            vehicleId = fixtures.cust2VehicleId,
            bookingType = BookingType.HOURLY,
            units = 1,
            startEpochMs = futureStart + 3_600_000L,
            confirmDemoPaymentImmediately = true,
          )
        )
      assertTrue(secondOverlapping is BookingMutationResult.Rejected)

      // 5. Cancellation persists Demo refund and revokes gate access
      val firstId = (firstBook as BookingMutationResult.Success).booking.id
      val cancelled =
        bookingRepo.cancelBooking(firstId, fixtures.cust1User.id) as BookingMutationResult.Success
      assertEquals(BookingStatus.CANCELLED, cancelled.booking.status)
      assertFalse(cancelled.booking.gatePinReleased)
      assertNull(cancelled.booking.demoEntryPin)
      assertEquals(firstBook.booking.totalAmountPkr, cancelled.booking.refundAmountPkr)
    }

  @Test
  fun `navigation guard restricts booking checkout confirmation and history to customer role`() =
    runBlocking(Dispatchers.IO) {
      val fixtures = seedM3AccountsAndListings()
      val guest = sessionManager.session.value
      assertTrue(
        ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.BookingCheckout(fixtures.space1Id), guest)
          is NavGuardResult.Denied
      )

      assertTrue(sessionManager.activateAuthenticatedUser(fixtures.hostUser) is DemoSandboxActionResult.Success)
      assertTrue(
        ParkSpaceNavGuard.evaluateAccess(
          ParkSpaceRoute.CustomerBookings,
          sessionManager.session.value,
        ) is NavGuardResult.Denied
      )

      assertTrue(sessionManager.activateAuthenticatedUser(fixtures.cust1User) is DemoSandboxActionResult.Success)
      val customer = sessionManager.session.value
      assertTrue(
        ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.BookingCheckout(fixtures.space1Id), customer)
          is NavGuardResult.Allowed
      )
      assertTrue(
        ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.BookingConfirmation("any-booking-id"), customer)
          is NavGuardResult.Allowed
      )
      assertTrue(
        ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.CustomerBookings, customer)
          is NavGuardResult.Allowed
      )
    }

  @Test
  fun `checkout compose screen creates booking and transitions to confirmation and history`() {
    val fixtures =
      runBlocking(Dispatchers.IO) {
        seedM3AccountsAndListings().also {
          assertTrue(sessionManager.activateAuthenticatedUser(it.cust1User) is DemoSandboxActionResult.Success)
        }
      }

    val checkoutVm =
      ViewModelProvider(
        viewModelStore,
        object : ViewModelProvider.Factory {
          @Suppress("UNCHECKED_CAST")
          override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            BookingCheckoutViewModel(fixtures.space1Id, parkingRepo, userRepo, bookingRepo, sessionManager) as T
        },
      )[BookingCheckoutViewModel::class.java]

    var createdBookingId: String? = null
    composeRule.setContent {
      val state = checkoutVm.uiState.collectAsStateWithLifecycle().value
      ParkSpaceTheme {
        BookingCheckoutScreen(
          uiState = state,
          onBack = {},
          onSelectVehicle = checkoutVm::onSelectVehicle,
          onSelectBookingType = checkoutVm::onSelectBookingType,
          onSelectDayOffset = checkoutVm::onSelectDayOffset,
          onAdjustStartHour = checkoutVm::onAdjustStartHour,
          onToggleStartMinute = checkoutVm::onToggleStartMinute,
          onAdjustUnits = checkoutVm::onAdjustUnits,
          onSetUnits = checkoutVm::onSetUnits,
          onToggleImmediateDemoPayment = checkoutVm::onToggleImmediateDemoPayment,
          onConfirmBooking = { checkoutVm.confirmBooking { createdBookingId = it } },
        )
      }
    }

    composeRule.waitUntil(timeoutMillis = 10_000) {
      ShadowLooper.idleMainLooper()
      composeRule.onAllNodesWithTag("checkout_space_summary_card").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("day_offset_1").performScrollTo().performClick()
    composeRule.onNodeWithTag("booking_type_daily").performScrollTo().performClick()
    composeRule.onNodeWithTag("confirm_booking_button").performScrollTo().performClick()

    composeRule.waitUntil(timeoutMillis = 10_000) {
      ShadowLooper.idleMainLooper()
      createdBookingId != null
    }
    assertNotNull(createdBookingId)
  }

  @Test
  fun `confirmation and history compose screens render demo pin, qr pass, and seeded bookings`() {
    val (upcomingBookingId, completedBookingId) =
      runBlocking(Dispatchers.IO) {
        val fixtures = seedM3AccountsAndListings()
        assertTrue(sessionManager.activateAuthenticatedUser(fixtures.cust1User) is DemoSandboxActionResult.Success)
        val upcomingRes =
          bookingRepo.createBookingWithAvailabilityCheck(
            BookingCreateRequest(
              spaceId = fixtures.space1Id,
              customerId = fixtures.cust1User.id,
              vehicleId = fixtures.cust1VehicleId,
              bookingType = BookingType.HOURLY,
              units = 2,
              startEpochMs = nextSafeMorningEpochMs(2),
              confirmDemoPaymentImmediately = true,
            )
          ) as BookingMutationResult.Success

        val completedStart = nextSafeMorningEpochMs(3)
        val completedRes =
          bookingRepo.createBookingWithAvailabilityCheck(
            BookingCreateRequest(
              spaceId = fixtures.space1Id,
              customerId = fixtures.cust1User.id,
              vehicleId = fixtures.cust1VehicleId,
              bookingType = BookingType.HOURLY,
              units = 1,
              startEpochMs = completedStart,
              confirmDemoPaymentImmediately = true,
            )
          ) as BookingMutationResult.Success
        assertTrue(bookingRepo.checkInBooking(completedRes.booking.id, fixtures.cust1User.id) is BookingMutationResult.Success)
        assertTrue(bookingRepo.checkOutBooking(completedRes.booking.id, fixtures.cust1User.id) is BookingMutationResult.Success)

        upcomingRes.booking.id to completedRes.booking.id
      }

    val historyVm =
      ViewModelProvider(
        viewModelStore,
        object : ViewModelProvider.Factory {
          @Suppress("UNCHECKED_CAST")
          override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            BookingHistoryViewModel(parkingRepo, userRepo, bookingRepo, sessionManager) as T
        },
      )[BookingHistoryViewModel::class.java]

    val detailFlow = historyVm.observeBookingDetail(upcomingBookingId)
    var showHistory by androidx.compose.runtime.mutableStateOf(false)
    composeRule.setContent {
      val detailState = detailFlow.collectAsStateWithLifecycle().value
      val historyState = historyVm.historyUiState.collectAsStateWithLifecycle().value
      ParkSpaceTheme {
        if (!showHistory) {
          BookingConfirmationScreen(
            uiState = detailState,
            actionFeedback = null,
            onBack = {},
            onOpenBookingsHistory = { showHistory = true },
            onConfirmPendingPayment = {},
            onCheckIn = historyVm::checkIn,
            onCheckOut = historyVm::checkOut,
            onCancelBooking = historyVm::cancelBooking,
            onDismissFeedback = {},
          )
        } else {
          CustomerBookingsScreen(
            uiState = historyState,
            actionFeedback = null,
            onBack = {},
            onSelectTab = historyVm::onSelectTab,
            onOpenBookingDetail = {},
            onConfirmPendingPayment = {},
            onCheckIn = {},
            onCheckOut = {},
            onCancelBooking = {},
            onOpenDiscovery = {},
            onDismissFeedback = {},
          )
        }
      }
    }

    composeRule.waitUntil(timeoutMillis = 10_000) {
      ShadowLooper.idleMainLooper()
      composeRule.onAllNodesWithTag("booking_confirmation_hero_card").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("booking_reference_code").assertIsDisplayed()
    composeRule.onNodeWithTag("simulated_demo_pin_value").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("simulated_demo_qr_pass").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("unlocked_gate_summary").performScrollTo().assertIsDisplayed()

    // Transition to CustomerBookingsScreen and verify Upcoming + Completed tabs
    composeRule.onNodeWithTag("view_my_bookings_button").performScrollTo().performClick()
    composeRule.waitUntil(timeoutMillis = 5_000) {
      ShadowLooper.idleMainLooper()
      composeRule.onAllNodesWithTag("booking_history_card_$upcomingBookingId").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("booking_history_card_$upcomingBookingId").assertIsDisplayed()
    composeRule.onNodeWithTag("bookings_tab_completed").performClick()
    composeRule.waitUntil(timeoutMillis = 5_000) {
      ShadowLooper.idleMainLooper()
      composeRule.onAllNodesWithTag("booking_history_card_$completedBookingId").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("booking_history_card_$completedBookingId").assertIsDisplayed()
  }
}
