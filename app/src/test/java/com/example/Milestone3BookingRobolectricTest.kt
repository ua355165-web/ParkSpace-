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
import com.example.core.data.demo.DemoBookingRepositoryImpl
import com.example.core.data.demo.DemoParkingRepositoryImpl
import com.example.core.data.demo.DemoUserProfileRepositoryImpl
import com.example.core.database.ParkSpaceDemoDatabase
import com.example.core.domain.model.BookingStatus
import com.example.core.domain.repository.BookingCreateRequest
import com.example.core.domain.repository.BookingMutationResult
import com.example.core.navigation.NavGuardResult
import com.example.core.navigation.ParkSpaceNavGuard
import com.example.core.navigation.ParkSpaceRoute
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
  private lateinit var parkingRepo: DemoParkingRepositoryImpl
  private lateinit var bookingRepo: DemoBookingRepositoryImpl
  private lateinit var sessionManager: DemoSessionManager
  private lateinit var viewModelStore: ViewModelStore

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
    bookingRepo = DemoBookingRepositoryImpl(database)
    sessionManager = DemoSessionManager(userRepo)
    viewModelStore = ViewModelStore()
  }

  @After
  fun tearDown() {
    viewModelStore.clear()
    database.close()
  }

  @Test
  fun `room transactional booking creation saves confirmed and pending states, enforces capacity, and protects gate pin`() =
    runBlocking(Dispatchers.IO) {
      seedInitializer.seedIfNeeded()

      val futureStart =
        ZonedDateTime.now(BookingAvailabilityValidator.PAKISTAN_ZONE)
          .toLocalDate()
          .plusDays(2)
          .let { date ->
            val safeDate =
              if (date.monthValue == 12 && date.dayOfMonth == 25) date.plusDays(1) else date
            ZonedDateTime.of(safeDate, LocalTime.of(10, 0), BookingAvailabilityValidator.PAKISTAN_ZONE)
          }
          .toInstant()
          .toEpochMilli()

      // 1. Pending payment booking keeps gatePinReleased = false and demoEntryPin = null
      val pendingRes =
        bookingRepo.createBookingWithAvailabilityCheck(
          BookingCreateRequest(
            spaceId = "demo-lhr-01",
            customerId = "demo-cust-lhr",
            vehicleId = "demo-veh-1",
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
            spaceId = "demo-lhr-01",
            customerId = "demo-cust-lhr",
            vehicleId = "demo-veh-1",
            bookingType = BookingType.HOURLY,
            units = 2,
            startEpochMs = futureStart,
            confirmDemoPaymentImmediately = true,
          )
        )
      assertTrue(dupRes is BookingMutationResult.Rejected)

      // 3. Completing Demo payment unlocks PIN & transitions CONFIRMED -> CHECKED_IN -> CHECKED_OUT
      val paid =
        bookingRepo.confirmPendingBookingPayment(pending.id, "demo-cust-lhr")
          as BookingMutationResult.Success
      assertEquals(BookingStatus.CONFIRMED, paid.booking.status)
      assertTrue(paid.booking.gatePinReleased)
      assertNotNull(paid.booking.demoEntryPin)

      val checkedIn =
        bookingRepo.checkInBooking(pending.id, "demo-cust-lhr") as BookingMutationResult.Success
      assertEquals(BookingStatus.CHECKED_IN, checkedIn.booking.status)

      val checkedOut =
        bookingRepo.checkOutBooking(pending.id, "demo-cust-lhr") as BookingMutationResult.Success
      assertEquals(BookingStatus.CHECKED_OUT, checkedOut.booking.status)

      // 4. Tight-capacity space (availableBays = 1) rejects overlapping second booking
      val smallSpace = parkingRepo.getSpaceById("demo-lhr-02")!!.copy(availableBays = 1, totalBays = 1)
      parkingRepo.upsertSpace(smallSpace)
      val firstBook =
        bookingRepo.createBookingWithAvailabilityCheck(
          BookingCreateRequest(
            spaceId = "demo-lhr-02",
            customerId = "demo-cust-lhr",
            vehicleId = "demo-veh-1",
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
            spaceId = "demo-lhr-02",
            customerId = "demo-cust-khi",
            vehicleId = "demo-veh-3",
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
        bookingRepo.cancelBooking(firstId, "demo-cust-lhr") as BookingMutationResult.Success
      assertEquals(BookingStatus.CANCELLED, cancelled.booking.status)
      assertFalse(cancelled.booking.gatePinReleased)
      assertNull(cancelled.booking.demoEntryPin)
      assertEquals(firstBook.booking.totalAmountPkr, cancelled.booking.refundAmountPkr)
    }

  @Test
  fun `navigation guard restricts booking checkout confirmation and history to customer role`() =
    runBlocking(Dispatchers.IO) {
      seedInitializer.seedIfNeeded()
      val guest = sessionManager.session.value
      assertTrue(
        ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.BookingCheckout("demo-lhr-01"), guest)
          is NavGuardResult.Denied
      )

      sessionManager.switchToDemoUser("demo-host-lhr")
      assertTrue(
        ParkSpaceNavGuard.evaluateAccess(
          ParkSpaceRoute.CustomerBookings,
          sessionManager.session.value,
        ) is NavGuardResult.Denied
      )

      sessionManager.switchToDemoUser("demo-cust-lhr")
      val customer = sessionManager.session.value
      assertTrue(
        ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.BookingCheckout("demo-lhr-01"), customer)
          is NavGuardResult.Allowed
      )
      assertTrue(
        ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.BookingConfirmation("demo-bkg-01"), customer)
          is NavGuardResult.Allowed
      )
      assertTrue(
        ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.CustomerBookings, customer)
          is NavGuardResult.Allowed
      )
    }

  @Test
  fun `checkout compose screen creates booking and transitions to confirmation and history`() {
    runBlocking(Dispatchers.IO) {
      seedInitializer.seedIfNeeded()
      sessionManager.switchToDemoUser("demo-cust-lhr")
    }

    val checkoutVm =
      ViewModelProvider(
        viewModelStore,
        object : ViewModelProvider.Factory {
          @Suppress("UNCHECKED_CAST")
          override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            BookingCheckoutViewModel("demo-lhr-01", parkingRepo, userRepo, bookingRepo, sessionManager) as T
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
    runBlocking(Dispatchers.IO) {
      seedInitializer.seedIfNeeded()
      sessionManager.switchToDemoUser("demo-cust-lhr")
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

    val detailFlow = historyVm.observeBookingDetail("demo-bkg-01")
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
      composeRule.onAllNodesWithTag("booking_history_card_demo-bkg-01").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("booking_history_card_demo-bkg-01").assertIsDisplayed()
    composeRule.onNodeWithTag("bookings_tab_completed").performClick()
    composeRule.waitUntil(timeoutMillis = 5_000) {
      ShadowLooper.idleMainLooper()
      composeRule.onAllNodesWithTag("booking_history_card_demo-bkg-02").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("booking_history_card_demo-bkg-02").assertIsDisplayed()
  }
}
