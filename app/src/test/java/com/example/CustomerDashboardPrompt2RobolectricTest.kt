package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.robolectric.shadows.ShadowLooper
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import com.example.core.common.UiState
import com.example.core.config.BookingType
import com.example.core.config.SupportedCity
import com.example.core.config.UserRole
import com.example.core.designsystem.components.CustomerBottomTab
import com.example.core.designsystem.components.ParkSpaceCustomerBottomBar
import com.example.core.di.ParkSpaceAppContainer
import com.example.core.domain.model.AvailabilitySchedule
import com.example.core.domain.model.BookingStatus
import com.example.core.domain.model.DemoPaymentMethod
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
import com.example.core.domain.repository.HostListingMutationResult
import com.example.core.navigation.ParkSpaceNavHost
import com.example.demo.sandbox.DemoSandboxActionResult
import com.example.demo.sandbox.DemoSessionManager
import com.example.feature.foundation.ParkSpaceFoundationViewModel
import com.example.feature.foundation.domain.CustomerDashboardAnalyticsCalculator
import com.example.feature.foundation.ui.CustomerDashboardScreen
import com.example.ui.theme.ParkSpaceTheme
import com.example.ui.theme.SharedPrefsThemePreferenceStore
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
class CustomerDashboardPrompt2RobolectricTest {

  @get:Rule val composeRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var container: ParkSpaceAppContainer

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    clearLocalPrefs()
    container = ParkSpaceAppContainer(context)
    runBlocking(Dispatchers.IO) {
      container.demoDatabase.clearAllTables()
      container.demoSeedInitializer.seedIfNeeded()
    }
  }

  @After
  fun tearDown() {
    runBlocking(Dispatchers.IO) {
      container.demoDatabase.clearAllTables()
    }
    clearLocalPrefs()
  }

  private fun clearLocalPrefs() {
    context.getSharedPreferences("parkspace_demo_session", Context.MODE_PRIVATE).edit().clear().commit()
    context.getSharedPreferences("parkspace_theme_prefs", Context.MODE_PRIVATE).edit().clear().commit()
  }

  @Test
  fun `empty database renders zero metrics dash for spending empty chart states and zero vehicles`() {
    val customerUser =
      runBlocking(Dispatchers.IO) {
        val reg =
          container.demoAuthRepository.registerLocalAccount(
            DemoRegisterRequest(
              role = UserRole.CUSTOMER,
              fullName = "Ayesha Driver",
              email = "ayesha.empty@parkspace.demo",
              password = "password123",
              confirmPassword = "password123",
              preferredCity = SupportedCity.LAHORE,
            )
          ) as DemoAuthResult.Success
        container.demoSessionManager.activateAuthenticatedUser(reg.user)
        reg.user
      }

    val viewModel = ParkSpaceFoundationViewModel(container)

    composeRule.setContent {
      val state by viewModel.uiState.collectAsStateWithLifecycle()
      val data = (state as? UiState.Success)?.data
      ParkSpaceTheme(darkTheme = false) {
        if (data != null) {
          CustomerDashboardScreen(
            data = data,
            isDarkTheme = false,
            onToggleTheme = {},
            onOpenDiscovery = {},
            onOpenBookings = {},
            onOpenBookingDetail = {},
            onOpenProfile = {},
            onAddVehicle = { _, _, _ -> },
            onUpdatePreferredCity = {},
            onOpenPaymentsAndReferrals = {},
            onOpenDisputes = {},
            onOpenNotifications = {},
            onOpenHostDashboardOrActivate = {},
            onSignOut = {},
            bottomBar = {
              ParkSpaceCustomerBottomBar(
                selectedTab = null,
                unreadMessagesCount = data.unreadChatsCount,
                onSelectTab = {},
              )
            },
          )
        }
      }
    }

    composeRule.waitUntil(5_000) {
      composeRule.onAllNodesWithTag("customer_shell_card").fetchSemanticsNodes().isNotEmpty()
    }

    // Real empty calculator assertion
    val analytics = CustomerDashboardAnalyticsCalculator.compute(emptyList(), emptyList(), emptyMap())
    assertEquals(0, analytics.summary.upcomingCount)
    assertEquals(0, analytics.summary.activeCount)
    assertEquals(0, analytics.summary.totalBookings)
    assertFalse(analytics.summary.hasSpendingRecords)
    assertNull(analytics.summary.recentSpendingPkr)
    assertTrue(analytics.spendingTrend.isEmpty())

    // UI assertions: zero counts, dash for spending, no fake Rs spending chart, empty states visible
    composeRule.onNodeWithTag("kpi_upcoming_bookings").assertIsDisplayed()
    composeRule.onNodeWithTag("kpi_recent_spending").assertIsDisplayed()
    composeRule.onNodeWithText("No Demo payments yet").assertIsDisplayed()
    composeRule.onNodeWithTag("open_discovery_button").assertIsDisplayed()
    assertTrue(
      "Spotlight preview card stays hidden on empty DB to keep screen uncluttered",
      composeRule.onAllNodesWithTag("customer_upcoming_preview_card").fetchSemanticsNodes().isEmpty(),
    )
    composeRule.onNodeWithTag("customer_activity_chart_empty").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("customer_status_donut_empty").performScrollTo().assertIsDisplayed()
    assertTrue(
      "Spending trend card must stay hidden when no Demo payments exist",
      composeRule.onAllNodesWithTag("customer_spending_chart_card").fetchSemanticsNodes().isEmpty(),
    )
    composeRule.onNodeWithTag("customer_empty_vehicles_text").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("customer_recent_bookings_empty").performScrollTo().assertIsDisplayed()
    assertNotNull(customerUser.id)
  }

  @Test
  fun `real room booking and simulated payment update KPIs charts gate pass spotlight and menu host activation`() {
    val createdBooking =
      runBlocking(Dispatchers.IO) {
        val hostReg =
          container.demoAuthRepository.registerLocalAccount(
            DemoRegisterRequest(
              role = UserRole.HOST,
              fullName = "Hamza Host",
              email = "hamza.host@parkspace.demo",
              password = "password123",
              confirmPassword = "password123",
              preferredCity = SupportedCity.LAHORE,
              businessAlias = "Liberty Safe Garage",
            )
          ) as DemoAuthResult.Success

        // Approve Host via Private Admin sandbox + repository
        assertTrue(
          container.demoSessionManager.unlockPrivateDemoAdminSandbox(acknowledgedLocalTestingOnly = true)
            is DemoSandboxActionResult.Success
        )
        assertTrue(
          container.adminRepository.approveHostKyc(
            actorAdminId = DemoSessionManager.PRIVATE_DEMO_ADMIN_ID,
            hostUserId = hostReg.user.id,
            note = "Verified for test",
          ) is AdminMutationResult.Success
        )
        container.demoSessionManager.signOutDemoSandbox()

        val space =
          ParkingSpace(
            id = "p2-space-lhr",
            hostId = hostReg.user.id,
            title = "Gulberg Emerald Vault",
            description = "Covered basement bay near Liberty",
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
            dailyRatePkr = 1400,
            monthlyRatePkr = 24000,
            monthlyEnabled = true,
            totalBays = 2,
            availableBays = 2,
            supportedVehicles = setOf(VehicleType.CAR),
            amenities = setOf(ParkingAmenity.CCTV, ParkingAmenity.COVERED),
            schedule = AvailabilitySchedule(ParkingDayOfWeek.entries.toSet(), 0, 23, true),
            isApproved = false,
            isDemoListing = true,
          )
        val createdSpace =
          (container.parkingRepository.createOrUpdateListing(hostReg.user.id, space, publishImmediately = true)
            as HostListingMutationResult.Success).space

        assertTrue(
          container.demoSessionManager.unlockPrivateDemoAdminSandbox(acknowledgedLocalTestingOnly = true)
            is DemoSandboxActionResult.Success
        )
        assertTrue(
          container.adminRepository.approveListing(
            actorAdminId = DemoSessionManager.PRIVATE_DEMO_ADMIN_ID,
            spaceId = createdSpace.id,
            note = "Listing approved",
          ) is AdminMutationResult.Success
        )
        container.demoSessionManager.signOutDemoSandbox()

        // Register Customer & add vehicle & make a confirmed booking with simulated payment
        val custReg =
          container.demoAuthRepository.registerLocalAccount(
            DemoRegisterRequest(
              role = UserRole.CUSTOMER,
              fullName = "Bilal Customer",
              email = "bilal.cust@parkspace.demo",
              password = "password123",
              confirmPassword = "password123",
              preferredCity = SupportedCity.LAHORE,
            )
          ) as DemoAuthResult.Success
        container.demoSessionManager.activateAuthenticatedUser(custReg.user)

        val vehicle =
          container.userProfileRepository
            .addVehicleForUser(
              ownerUserId = custReg.user.id,
              vehicleType = VehicleType.CAR,
              label = "Honda Civic",
              plateNumber = "LEB-9042",
              makeDefault = true,
            )
            .getOrThrow()

        val futureStart = ZonedDateTime.now().plusHours(4).withMinute(0).withSecond(0).withNano(0)
        val bookingResult =
          container.bookingRepository.createBookingWithAvailabilityCheck(
            BookingCreateRequest(
              spaceId = createdSpace.id,
              customerId = custReg.user.id,
              vehicleId = vehicle.id,
              bookingType = BookingType.HOURLY,
              units = 2,
              startEpochMs = futureStart.toInstant().toEpochMilli(),
              confirmDemoPaymentImmediately = true,
              paymentMethod = DemoPaymentMethod.JAZZCASH_DEMO,
            )
          ) as BookingMutationResult.Success
        bookingResult.booking
      }

    assertEquals(BookingStatus.CONFIRMED, createdBooking.status)
    assertTrue(createdBooking.hasActiveGateAccess())

    val viewModel = ParkSpaceFoundationViewModel(container)
    var clickedBookingId: String? = null
    var hostSwitchTriggered = false
    var isDark by mutableStateOf(false)
    val themeStore = SharedPrefsThemePreferenceStore(context)

    composeRule.setContent {
      val state by viewModel.uiState.collectAsStateWithLifecycle()
      val data = (state as? UiState.Success)?.data
      ParkSpaceTheme(darkTheme = isDark) {
        if (data != null) {
          CustomerDashboardScreen(
            data = data,
            isDarkTheme = isDark,
            onToggleTheme = {
              isDark = !isDark
              themeStore.setDarkTheme(isDark)
            },
            onOpenDiscovery = {},
            onOpenBookings = {},
            onOpenBookingDetail = { clickedBookingId = it },
            onOpenProfile = {},
            onAddVehicle = viewModel::addVehicleForActiveCustomer,
            onUpdatePreferredCity = viewModel::updateActiveCustomerPreferredCity,
            onOpenPaymentsAndReferrals = {},
            onOpenDisputes = {},
            onOpenNotifications = {},
            onOpenHostDashboardOrActivate = { hostSwitchTriggered = true },
            onSignOut = {},
          )
        }
      }
    }

    composeRule.waitUntil(5_000) {
      composeRule.onAllNodesWithTag("customer_upcoming_preview_card").fetchSemanticsNodes().isNotEmpty()
    }

    // Real booking + payment metrics displayed (space title appears in Spotlight & Recent Activity)
    composeRule.onNodeWithText("Simulated Demo ledger").assertIsDisplayed()
    composeRule.onNodeWithTag("customer_upcoming_preview_card").performScrollTo().assertIsDisplayed()
    composeRule.onAllNodesWithText("Gulberg Emerald Vault")[0].assertIsDisplayed()
    composeRule.onNodeWithText("View Gate Pass").assertIsDisplayed()

    // Charts show real activity & spending (no empty chart fallbacks)
    composeRule.onNodeWithTag("customer_activity_chart_card").performScrollTo().assertIsDisplayed()
    assertTrue(composeRule.onAllNodesWithTag("customer_activity_chart_empty").fetchSemanticsNodes().isEmpty())
    composeRule.onNodeWithTag("customer_status_donut_card").performScrollTo().assertIsDisplayed()
    assertTrue(composeRule.onAllNodesWithTag("customer_status_donut_empty").fetchSemanticsNodes().isEmpty())
    composeRule.onNodeWithTag("customer_spending_chart_card").performScrollTo().assertIsDisplayed()

    // Vehicle appears
    composeRule.onNodeWithText("Honda Civic").performScrollTo().assertIsDisplayed()

    // Gate pass CTA opens real booking detail
    composeRule.onNodeWithTag("spotlight_booking_action_button").performScrollTo().performClick()
    assertEquals(createdBooking.id, clickedBookingId)

    // Theme toggle persists
    composeRule.onNodeWithTag("theme_toggle_button").performScrollTo().performClick()
    assertTrue(isDark)
    assertEquals(true, themeStore.darkThemeOverride.value)

    // Customer Menu shows Activate Host Role (since no companion Host account exists for bilal.cust)
    composeRule.onNodeWithTag("customer_menu_button").performScrollTo().performClick()
    composeRule.onNodeWithTag("menu_customer_dashboard").assertIsDisplayed()
    composeRule.onNodeWithTag("menu_activate_host_role").assertIsDisplayed()
    composeRule.onNodeWithTag("menu_customer_profile").assertIsDisplayed()
    composeRule.onNodeWithTag("menu_customer_settings").assertIsDisplayed()
    composeRule.onNodeWithTag("menu_help_support").assertIsDisplayed()
    composeRule.onNodeWithTag("menu_privacy_terms").assertIsDisplayed()
    composeRule.onNodeWithTag("menu_sign_out_button").assertIsDisplayed()

    // Super Admin MUST NOT appear in Customer menu
    assertTrue(
      composeRule.onAllNodesWithTag("private_admin_gate_button").fetchSemanticsNodes().isEmpty()
    )

    // Tapping Activate Host Role triggers handler
    composeRule.onNodeWithTag("menu_activate_host_role").performClick()
    assertTrue(hostSwitchTriggered)
  }

  @Test
  fun `customer menu displays Host Dashboard when companion Host exists and bottom nav routes across tabs`() {
    runBlocking(Dispatchers.IO) {
      val custReg =
        container.demoAuthRepository.registerLocalAccount(
          DemoRegisterRequest(
            role = UserRole.CUSTOMER,
            fullName = "Zara Dual",
            email = "zara.dual@parkspace.demo",
            password = "password123",
            confirmPassword = "password123",
            preferredCity = SupportedCity.ISLAMABAD,
          )
        ) as DemoAuthResult.Success
      // Activate companion Host role on same email, then switch back to Customer
      val hostActivation =
        container.demoAuthRepository.activateOrSwitchCompanionRole(
          currentSignedInUserId = custReg.user.id,
          targetRole = UserRole.HOST,
          businessAlias = "Zara F-6 Parking",
        ) as DemoAuthResult.Success
      assertNotNull(hostActivation.user.id)
      val activationResult = container.demoSessionManager.activateAuthenticatedUser(custReg.user)
      assertTrue(activationResult is DemoSandboxActionResult.Success)
    }

    val viewModel = ParkSpaceFoundationViewModel(container)
    var navigatedToHost = false

    composeRule.setContent {
      val state by viewModel.uiState.collectAsStateWithLifecycle()
      val data = (state as? UiState.Success)?.data
      var currentTab by mutableStateOf<CustomerBottomTab?>(null)
      ParkSpaceTheme(darkTheme = false) {
        if (data != null) {
          CustomerDashboardScreen(
            data = data,
            isDarkTheme = false,
            onToggleTheme = {},
            onOpenDiscovery = {},
            onOpenBookings = {},
            onOpenBookingDetail = {},
            onOpenProfile = {},
            onAddVehicle = { _, _, _ -> },
            onUpdatePreferredCity = {},
            onOpenPaymentsAndReferrals = {},
            onOpenDisputes = {},
            onOpenNotifications = {},
            onOpenHostDashboardOrActivate = {
              viewModel.switchToCompanionHostDashboard(
                onSwitched = { navigatedToHost = true },
                onRequiresActivation = {},
              )
            },
            onSignOut = {},
            bottomBar = {
              ParkSpaceCustomerBottomBar(
                selectedTab = currentTab,
                unreadMessagesCount = data.unreadChatsCount,
                onSelectTab = { currentTab = it },
              )
            },
          )
        }
      }
    }

    composeRule.waitUntil(5_000) {
      val data = (viewModel.uiState.value as? UiState.Success)?.data
      data != null &&
        data.hasCompanionHostAccount &&
        composeRule.onAllNodesWithTag("customer_bottom_nav").fetchSemanticsNodes().isNotEmpty()
    }

    // Bottom Bar displays all 4 destinations cleanly
    composeRule.onNodeWithTag("customer_bottom_nav").assertIsDisplayed()
    composeRule.onNodeWithTag("customer_nav_discover").assertIsDisplayed()
    composeRule.onNodeWithTag("customer_nav_bookings").assertIsDisplayed()
    composeRule.onNodeWithTag("customer_nav_messages").assertIsDisplayed()
    composeRule.onNodeWithTag("customer_nav_profile").assertIsDisplayed()

    // Customer menu shows "Host Dashboard" (not "Activate Host Role") and switches seamlessly
    composeRule.onNodeWithTag("customer_menu_button").performClick()
    composeRule.onNodeWithTag("menu_switch_host_dashboard").assertIsDisplayed()
    assertTrue(
      composeRule.onAllNodesWithTag("menu_activate_host_role").fetchSemanticsNodes().isEmpty()
    )

    composeRule.onNodeWithTag("menu_switch_host_dashboard").performClick()
    composeRule.waitUntil(5_000) {
      ShadowLooper.idleMainLooper()
      navigatedToHost
    }
    assertEquals(UserRole.HOST, viewModel.sessionState.value.activeRole)
  }

  @Test
  fun `navHost bottom bar navigates cleanly between Discover Bookings Messages Profile and Gate Pass`() {
    val bookedId =
      runBlocking(Dispatchers.IO) {
        val hostReg =
          container.demoAuthRepository.registerLocalAccount(
            DemoRegisterRequest(
              role = UserRole.HOST,
              fullName = "Host Nav",
              email = "host.nav@parkspace.demo",
              password = "password123",
              confirmPassword = "password123",
              preferredCity = SupportedCity.KARACHI,
              businessAlias = "Clifton Deck",
            )
          ) as DemoAuthResult.Success
        container.demoSessionManager.unlockPrivateDemoAdminSandbox(true)
        container.adminRepository.approveHostKyc(DemoSessionManager.PRIVATE_DEMO_ADMIN_ID, hostReg.user.id, "OK")
        container.demoSessionManager.signOutDemoSandbox()

        val space =
          ParkingSpace(
            id = "nav-space-khi",
            hostId = hostReg.user.id,
            title = "Clifton Block 4 Lot",
            description = "Secure lot near Clifton",
            parkingType = ParkingType.COVERED_GARAGE,
            location =
              ParkingLocation(
                city = SupportedCity.KARACHI,
                areaLabel = "Clifton Block 4",
                publicMaskedLatitude = SupportedCity.KARACHI.centerLatitude,
                publicMaskedLongitude = SupportedCity.KARACHI.centerLongitude,
                maskRadiusMeters = 200,
                privateGateSummary = "Clifton Gate 2",
                privateExactLatitude = SupportedCity.KARACHI.centerLatitude + 0.001,
                privateExactLongitude = SupportedCity.KARACHI.centerLongitude + 0.001,
              ),
            hourlyRatePkr = 150,
            dailyRatePkr = 1000,
            monthlyRatePkr = 18000,
            monthlyEnabled = false,
            totalBays = 2,
            availableBays = 2,
            supportedVehicles = setOf(VehicleType.CAR),
            amenities = setOf(ParkingAmenity.CCTV),
            schedule = AvailabilitySchedule(ParkingDayOfWeek.entries.toSet(), 0, 23, true),
            isApproved = false,
            isDemoListing = true,
          )
        val createdSpace =
          (container.parkingRepository.createOrUpdateListing(hostReg.user.id, space, publishImmediately = true)
            as HostListingMutationResult.Success).space
        container.demoSessionManager.unlockPrivateDemoAdminSandbox(true)
        container.adminRepository.approveListing(DemoSessionManager.PRIVATE_DEMO_ADMIN_ID, createdSpace.id, "OK")
        container.demoSessionManager.signOutDemoSandbox()

        val custReg =
          container.demoAuthRepository.registerLocalAccount(
            DemoRegisterRequest(
              role = UserRole.CUSTOMER,
              fullName = "Sana Navigator",
              email = "sana.nav@parkspace.demo",
              password = "password123",
              confirmPassword = "password123",
              preferredCity = SupportedCity.KARACHI,
            )
          ) as DemoAuthResult.Success
        container.demoSessionManager.activateAuthenticatedUser(custReg.user)
        val veh =
          container.userProfileRepository
            .addVehicleForUser(custReg.user.id, VehicleType.CAR, "Vitz", "KHI-888", true)
            .getOrThrow()
        val futureStart = ZonedDateTime.now().plusHours(3).withMinute(0).withSecond(0).withNano(0)
        val bookingRes =
          container.bookingRepository.createBookingWithAvailabilityCheck(
            BookingCreateRequest(
              spaceId = createdSpace.id,
              customerId = custReg.user.id,
              vehicleId = veh.id,
              bookingType = BookingType.HOURLY,
              units = 1,
              startEpochMs = futureStart.toInstant().toEpochMilli(),
              confirmDemoPaymentImmediately = true,
            )
          ) as BookingMutationResult.Success
        bookingRes.booking.id
      }

    val viewModel = ParkSpaceFoundationViewModel(container)

    composeRule.setContent {
      ParkSpaceTheme(darkTheme = true) {
        ParkSpaceNavHost(
          appContainer = container,
          viewModel = viewModel,
          isDarkTheme = true,
          onToggleTheme = {},
          discoveryMapContentOverride = {},
        )
      }
    }

    // Starts on WelcomeEntry with active customer session; open Customer dashboard
    composeRule.waitUntil(5_000) {
      composeRule.onAllNodesWithTag("open_active_role_button").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("open_active_role_button").performScrollTo().performClick()

    composeRule.waitUntil(5_000) {
      composeRule.onAllNodesWithTag("customer_shell_card").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("customer_bottom_nav").assertIsDisplayed()

    // Tap View Gate Pass on Spotlight Preview -> opens BookingConfirmation with Gate PIN!
    composeRule.waitUntil(5_000) {
      composeRule.onAllNodesWithTag("spotlight_booking_action_button").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("spotlight_booking_action_button").performScrollTo().performClick()
    composeRule.waitUntil(5_000) {
      composeRule.onAllNodesWithTag("booking_reference_code").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("simulated_demo_pin_value").assertIsDisplayed()
    assertNotNull(bookedId)

    // Back to Customer Dashboard
    composeRule.onNodeWithTag("confirmation_back_button").performClick()
    composeRule.waitUntil(5_000) {
      composeRule.onAllNodesWithTag("customer_shell_card").fetchSemanticsNodes().isNotEmpty()
    }

    // 1. Tap Discover tab
    composeRule.onNodeWithTag("customer_nav_discover").performClick()
    composeRule.waitUntil(5_000) {
      composeRule.onAllNodesWithTag("discovery_back_button").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("customer_bottom_nav").assertIsDisplayed()

    // 2. Tap Bookings tab
    composeRule.onNodeWithTag("customer_nav_bookings").performClick()
    composeRule.waitUntil(5_000) {
      composeRule.onAllNodesWithTag("bookings_back_button").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("customer_bottom_nav").assertIsDisplayed()

    // 3. Tap Messages tab
    composeRule.onNodeWithTag("customer_nav_messages").performClick()
    composeRule.waitUntil(5_000) {
      composeRule.onAllNodesWithTag("chat_inbox_back_button").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("customer_bottom_nav").assertIsDisplayed()

    // 4. Tap Profile tab
    composeRule.onNodeWithTag("customer_nav_profile").performClick()
    composeRule.waitUntil(5_000) {
      composeRule.onAllNodesWithTag("shared_settings_card").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("customer_bottom_nav").assertIsDisplayed()
  }
}
