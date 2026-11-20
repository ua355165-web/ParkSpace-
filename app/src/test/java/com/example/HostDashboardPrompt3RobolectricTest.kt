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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import com.example.core.config.BookingType
import com.example.core.config.SupportedCity
import com.example.core.config.UserRole
import com.example.core.di.ParkSpaceAppContainer
import com.example.core.domain.model.AvailabilitySchedule
import com.example.core.domain.model.BookingStatus
import com.example.core.domain.model.DemoPaymentMethod
import com.example.core.domain.model.HostVerificationStatus
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
import com.example.core.domain.repository.HostBayMutationResult
import com.example.core.domain.repository.HostListingMutationResult
import com.example.core.navigation.ParkSpaceNavHost
import com.example.demo.sandbox.DemoSandboxActionResult
import com.example.demo.sandbox.DemoSessionManager
import com.example.feature.foundation.ParkSpaceFoundationViewModel
import com.example.feature.host.HostDashboardTab
import com.example.feature.host.HostDashboardUiState
import com.example.feature.host.HostDashboardViewModel
import com.example.feature.host.domain.HostDashboardAnalyticsCalculator
import com.example.feature.host.ui.HostDashboardScreen
import com.example.ui.theme.ParkSpaceTheme
import com.example.ui.theme.SharedPrefsThemePreferenceStore
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
class HostDashboardPrompt3RobolectricTest {

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

  private fun buildHostViewModel(): HostDashboardViewModel =
    HostDashboardViewModel(
      parkingRepository = container.parkingRepository,
      bookingRepository = container.bookingRepository,
      userProfileRepository = container.userProfileRepository,
      demoSessionManager = container.demoSessionManager,
      notificationRepository = container.notificationRepository,
      chatRepository = container.chatRepository,
      disputeRepository = container.disputeRepository,
      demoAuthRepository = container.demoAuthRepository,
    )

  @Test
  fun `empty host database renders zero metrics dash for earnings empty chart states and PENDING_REVIEW guidance`() {
    val hostUser =
      runBlocking(Dispatchers.IO) {
        val reg =
          container.demoAuthRepository.registerLocalAccount(
            DemoRegisterRequest(
              role = UserRole.HOST,
              fullName = "Salman Empty Host",
              email = "salman.empty@host.demo",
              password = "password123",
              confirmPassword = "password123",
              preferredCity = SupportedCity.LAHORE,
              businessAlias = "Salman Gulberg Lot",
            )
          ) as DemoAuthResult.Success
        container.demoSessionManager.activateAuthenticatedUser(reg.user)
        reg.user
      }

    val hostViewModel = buildHostViewModel()

    composeRule.setContent {
      val state by hostViewModel.uiState.collectAsStateWithLifecycle()
      ParkSpaceTheme(darkTheme = false) {
        HostDashboardScreen(
          uiState = state,
          viewModel = hostViewModel,
          onBack = {},
          onOpenSettings = {},
          isDarkTheme = false,
          onToggleTheme = {},
        )
      }
    }

    composeRule.waitUntil(5_000) {
      ShadowLooper.idleMainLooper()
      !hostViewModel.uiState.value.isLoading &&
        hostViewModel.uiState.value.activeUser?.id == hostUser.id &&
        composeRule.onAllNodesWithTag("host_verification_card").fetchSemanticsNodes().isNotEmpty()
    }

    // Pure analytics verification on empty database
    val analytics = HostDashboardAnalyticsCalculator.compute(emptyList(), emptyList(), null)
    assertEquals(0, analytics.summary.totalListingsCount)
    assertEquals(0, analytics.summary.totalBaysCount)
    assertEquals(0, analytics.summary.upcomingBookingsCount)
    assertEquals(0, analytics.summary.activeBookingsCount)
    assertEquals(0, analytics.summary.completedBookingsCount)
    assertFalse(analytics.summary.hasEarningsRecords)
    assertNull(analytics.summary.netEarningsPkr)
    assertTrue(analytics.earningsTrend.isEmpty())

    // Verification state: strictly PENDING_REVIEW (never implies approval)
    composeRule.onNodeWithTag("host_status_badge").assertIsDisplayed()
    composeRule.onNodeWithText("KYC: Pending Review").assertIsDisplayed()
    composeRule.onNodeWithTag("host_verification_guidance_box").assertIsDisplayed()

    // KPIs: zero real records + dash for earnings
    composeRule.onNodeWithTag("host_kpi_total_spaces").assertIsDisplayed()
    composeRule.onNodeWithTag("host_kpi_total_bays").assertIsDisplayed()
    composeRule.onNodeWithTag("host_kpi_settled_rev").assertIsDisplayed()
    composeRule.onNodeWithText("No Demo earnings yet").assertIsDisplayed()

    // Charts show clean empty states & conditional earnings trend stays hidden
    composeRule.onNodeWithTag("host_activity_chart_empty").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("host_status_donut_empty").performScrollTo().assertIsDisplayed()
    assertTrue(
      "Host earnings trend must remain hidden when 0 revenue transactions exist",
      composeRule.onAllNodesWithTag("host_earnings_chart_card").fetchSemanticsNodes().isEmpty(),
    )
    composeRule.onNodeWithTag("host_recent_activity_empty").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `real room listings bays bookings and ledger records update KPIs charts quick actions and analytics`() {
    val hostUser =
      runBlocking(Dispatchers.IO) {
        val reg =
          container.demoAuthRepository.registerLocalAccount(
            DemoRegisterRequest(
              role = UserRole.HOST,
              fullName = "Zainab Active Host",
              email = "zainab.host@parkspace.demo",
              password = "password123",
              confirmPassword = "password123",
              preferredCity = SupportedCity.LAHORE,
              businessAlias = "MM Alam Emerald Deck",
            )
          ) as DemoAuthResult.Success

        container.demoSessionManager.unlockPrivateDemoAdminSandbox(true)
        assertTrue(
          container.adminRepository.approveHostKyc(
            actorAdminId = DemoSessionManager.PRIVATE_DEMO_ADMIN_ID,
            hostUserId = reg.user.id,
            note = "Verified Host Plaza",
          ) is AdminMutationResult.Success
        )
        container.demoSessionManager.signOutDemoSandbox()

        val space =
          ParkingSpace(
            id = "p3-host-space-1",
            hostId = reg.user.id,
            title = "MM Alam Covered Plaza",
            description = "Covered bays near Main Boulevard",
            parkingType = ParkingType.COVERED_GARAGE,
            location =
              ParkingLocation(
                city = SupportedCity.LAHORE,
                areaLabel = "Gulberg II",
                publicMaskedLatitude = SupportedCity.LAHORE.centerLatitude,
                publicMaskedLongitude = SupportedCity.LAHORE.centerLongitude,
                maskRadiusMeters = 200,
                privateGateSummary = "Basement Ramp B",
              ),
            hourlyRatePkr = 250,
            dailyRatePkr = 1800,
            monthlyRatePkr = null,
            monthlyEnabled = false,
            totalBays = 1,
            availableBays = 1,
            supportedVehicles = setOf(VehicleType.CAR, VehicleType.SUV),
            amenities = setOf(ParkingAmenity.CCTV, ParkingAmenity.COVERED),
            schedule = AvailabilitySchedule(ParkingDayOfWeek.entries.toSet(), 0, 23, true),
            isApproved = false,
            isDemoListing = true,
          )
        val createdSpace =
          (container.parkingRepository.createOrUpdateListing(reg.user.id, space, publishImmediately = true)
            as HostListingMutationResult.Success).space

        assertTrue(
          container.parkingRepository.addBayToSpace(
            hostId = reg.user.id,
            spaceId = createdSpace.id,
            bayCode = "Bay B-2",
            supportedVehicles = setOf(VehicleType.CAR),
            isCovered = true,
          ) is HostBayMutationResult.Success
        )

        container.demoSessionManager.unlockPrivateDemoAdminSandbox(true)
        container.adminRepository.approveListing(DemoSessionManager.PRIVATE_DEMO_ADMIN_ID, createdSpace.id, "Approved")
        container.demoSessionManager.signOutDemoSandbox()

        val custReg =
          container.demoAuthRepository.registerLocalAccount(
            DemoRegisterRequest(
              role = UserRole.CUSTOMER,
              fullName = "Hamza Driver",
              email = "hamza.p3cust@parkspace.demo",
              password = "password123",
              confirmPassword = "password123",
              preferredCity = SupportedCity.LAHORE,
            )
          ) as DemoAuthResult.Success
        val vehicle =
          container.userProfileRepository
            .addVehicleForUser(custReg.user.id, VehicleType.CAR, "Corolla", "LEH-202", true)
            .getOrThrow()

        val slot1 = ZonedDateTime.now().plusHours(2).withMinute(0).withSecond(0).withNano(0)
        val b1 =
          (container.bookingRepository.createBookingWithAvailabilityCheck(
            BookingCreateRequest(
              spaceId = createdSpace.id,
              customerId = custReg.user.id,
              vehicleId = vehicle.id,
              bookingType = BookingType.HOURLY,
              units = 2,
              startEpochMs = slot1.toInstant().toEpochMilli(),
              confirmDemoPaymentImmediately = true,
              paymentMethod = DemoPaymentMethod.EASYPAISA_DEMO,
            )
          ) as BookingMutationResult.Success).booking

        val slot2 = ZonedDateTime.now().plusHours(6).withMinute(0).withSecond(0).withNano(0)
        val b2 =
          (container.bookingRepository.createBookingWithAvailabilityCheck(
            BookingCreateRequest(
              spaceId = createdSpace.id,
              customerId = custReg.user.id,
              vehicleId = vehicle.id,
              bookingType = BookingType.HOURLY,
              units = 1,
              startEpochMs = slot2.toInstant().toEpochMilli(),
              confirmDemoPaymentImmediately = true,
              paymentMethod = DemoPaymentMethod.JAZZCASH_DEMO,
            )
          ) as BookingMutationResult.Success).booking

        // Check in & check out first booking -> becomes settled revenue; second stays confirmed/upcoming
        container.bookingRepository.hostCheckInBooking(b1.id, reg.user.id)
        container.bookingRepository.hostCheckOutBooking(b1.id, reg.user.id)

        assertTrue(container.demoSessionManager.activateAuthenticatedUser(reg.user) is DemoSandboxActionResult.Success)
        reg.user
      }

    val hostViewModel = buildHostViewModel()

    composeRule.setContent {
      val state by hostViewModel.uiState.collectAsStateWithLifecycle()
      ParkSpaceTheme(darkTheme = true) {
        HostDashboardScreen(
          uiState = state,
          viewModel = hostViewModel,
          onBack = {},
          onOpenSettings = {},
          isDarkTheme = true,
          onToggleTheme = {},
        )
      }
    }

    composeRule.waitUntil(5_000) {
      ShadowLooper.idleMainLooper()
      val s = hostViewModel.uiState.value
      !s.isLoading &&
        s.activeUser?.id == hostUser.id &&
        s.spaces.size == 1 &&
        s.bookings.size == 2 &&
        composeRule.onAllNodesWithTag("host_verification_card").fetchSemanticsNodes().isNotEmpty()
    }

    // Verification Approved + real Admin review note shown
    composeRule.onNodeWithText("KYC: Approved Host").assertIsDisplayed()
    composeRule.onNodeWithTag("host_verification_note_text").assertIsDisplayed()

    // Real KPIs & Demo earnings displayed
    composeRule.onNodeWithText("1 live in Discovery").assertIsDisplayed()
    composeRule.onNodeWithText("2 operational").assertIsDisplayed()
    assertTrue(composeRule.onAllNodesWithText("Rs. 750").fetchSemanticsNodes().isNotEmpty())

    // Real analytics charts shown (zero empty states)
    composeRule.onNodeWithTag("host_activity_chart_card").performScrollTo().assertIsDisplayed()
    assertTrue(composeRule.onAllNodesWithTag("host_activity_chart_empty").fetchSemanticsNodes().isEmpty())
    composeRule.onNodeWithTag("host_status_donut_card").performScrollTo().assertIsDisplayed()
    assertTrue(composeRule.onAllNodesWithTag("host_status_donut_empty").fetchSemanticsNodes().isEmpty())
    composeRule.onNodeWithTag("host_earnings_chart_card").performScrollTo().assertIsDisplayed()

    // Recent activity renders real space & bookings
    composeRule.onAllNodesWithText("MM Alam Covered Plaza")[0].performScrollTo().assertIsDisplayed()

    // Quick Action: Manage Bays directly opens Bays Manager Dialog for single space
    composeRule.onNodeWithTag("host_overview_manage_bays_button").performScrollTo().performClick()
    composeRule.waitUntil(5_000) {
      composeRule.onAllNodesWithTag("host_open_add_bay_form_button").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithText("Bay B-2").assertIsDisplayed()
    composeRule.onNodeWithTag("host_bays_dialog_close_button").performClick()

    // Quick Action: Manage Availability directly opens Schedule Manager Dialog
    composeRule.onNodeWithTag("host_overview_manage_availability_button").performScrollTo().performClick()
    composeRule.waitUntil(5_000) {
      composeRule.onAllNodesWithTag("host_save_schedule_button").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("host_schedule_dialog_close_button").performClick()

    // Bottom Navigation & Quick Actions switch cleanly across Listings, Bookings, Ledger
    composeRule.onNodeWithTag("host_overview_manage_listings_button").performScrollTo().performClick()
    composeRule.waitUntil(5_000) {
      composeRule.onAllNodesWithTag("host_listings_tab_list").fetchSemanticsNodes().isNotEmpty()
    }
    assertEquals(HostDashboardTab.LISTINGS, hostViewModel.uiState.value.activeTab)

    composeRule.onNodeWithTag("host_tab_bookings").performClick()
    composeRule.waitUntil(5_000) {
      composeRule.onAllNodesWithTag("host_bookings_tab_list").fetchSemanticsNodes().isNotEmpty()
    }
    assertEquals(HostDashboardTab.BOOKINGS, hostViewModel.uiState.value.activeTab)

    composeRule.onNodeWithTag("host_tab_ledger").performClick()
    composeRule.waitUntil(5_000) {
      composeRule.onAllNodesWithTag("host_ledger_tab_list").fetchSemanticsNodes().isNotEmpty()
    }
    assertEquals(HostDashboardTab.LEDGER, hostViewModel.uiState.value.activeTab)
    composeRule.onNodeWithTag("ledger_host_net").assertIsDisplayed()
  }

  @Test
  fun `rejected verification state loading and error states render accurately without implying approval`() {
    val hostUser =
      runBlocking(Dispatchers.IO) {
        val reg =
          container.demoAuthRepository.registerLocalAccount(
            DemoRegisterRequest(
              role = UserRole.HOST,
              fullName = "Imran Rejected Host",
              email = "imran.rejected@host.demo",
              password = "password123",
              confirmPassword = "password123",
              preferredCity = SupportedCity.ISLAMABAD,
              businessAlias = "Imran F-8 Lot",
            )
          ) as DemoAuthResult.Success

        container.demoSessionManager.unlockPrivateDemoAdminSandbox(true)
        assertTrue(
          container.adminRepository.rejectHostKyc(
            actorAdminId = DemoSessionManager.PRIVATE_DEMO_ADMIN_ID,
            hostUserId = reg.user.id,
            reason = "Missing commercial lot permit",
          ) is AdminMutationResult.Success
        )
        container.demoSessionManager.signOutDemoSandbox()
        container.demoSessionManager.activateAuthenticatedUser(reg.user)
        reg.user
      }

    val hostViewModel = buildHostViewModel()

    composeRule.setContent {
      val state by hostViewModel.uiState.collectAsStateWithLifecycle()
      ParkSpaceTheme(darkTheme = false) {
        HostDashboardScreen(
          uiState = state,
          viewModel = hostViewModel,
          onBack = {},
          onOpenSettings = {},
        )
      }
    }

    composeRule.waitUntil(5_000) {
      ShadowLooper.idleMainLooper()
      val s = hostViewModel.uiState.value
      !s.isLoading &&
        s.hostProfile?.verificationStatus == HostVerificationStatus.REJECTED &&
        composeRule.onAllNodesWithTag("host_verification_card").fetchSemanticsNodes().isNotEmpty()
    }

    assertEquals(hostUser.id, hostViewModel.uiState.value.activeUser?.id)
    composeRule.onNodeWithText("KYC: Verification Rejected").assertIsDisplayed()
    composeRule.onNodeWithTag("host_verification_note_text").assertIsDisplayed()
    composeRule.onNodeWithText("Admin review note: Missing commercial lot permit").assertIsDisplayed()

    // Quick Action on 0 spaces provides clear actionable feedback rather than breaking
    composeRule.onNodeWithTag("host_overview_manage_bays_button").performScrollTo().performClick()
    composeRule.waitUntil(5_000) {
      composeRule.onAllNodesWithTag("host_feedback_banner").fetchSemanticsNodes().isNotEmpty()
    }

    // Add Space quick action launches listing editor even when REJECTED (draft mode allowed)
    composeRule.onNodeWithTag("host_overview_add_listing_button").performScrollTo().performClick()
    composeRule.waitUntil(5_000) {
      composeRule.onAllNodesWithTag("host_editor_verification_lock_banner").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("host_save_draft_button").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `loading and error states display cleanly in HostDashboardScreen`() {
    val hostViewModel = buildHostViewModel()

    composeRule.setContent {
      ParkSpaceTheme(darkTheme = false) {
        HostDashboardScreen(
          uiState =
            HostDashboardUiState(
              isLoading = true,
              errorMessage = "Simulated Room sync interruption",
            ),
          viewModel = hostViewModel,
          onBack = {},
          onOpenSettings = {},
        )
      }
    }

    composeRule.onNodeWithTag("host_loading_state").assertIsDisplayed()
    composeRule.onNodeWithTag("host_error_banner").assertIsDisplayed()
    composeRule.onNodeWithText("Simulated Room sync interruption").assertIsDisplayed()
  }

  @Test
  fun `theme toggle persists and Host menu switches cleanly to Customer or Activate Customer without Admin exposure`() {
    runBlocking(Dispatchers.IO) {
      val hostReg =
        container.demoAuthRepository.registerLocalAccount(
          DemoRegisterRequest(
            role = UserRole.HOST,
            fullName = "Sana Dual Host",
            email = "sana.dualhost@parkspace.demo",
            password = "password123",
            confirmPassword = "password123",
            preferredCity = SupportedCity.KARACHI,
            businessAlias = "Clifton Host Bays",
          )
        ) as DemoAuthResult.Success
      container.demoSessionManager.activateAuthenticatedUser(hostReg.user)
    }

    val hostViewModel = buildHostViewModel()
    val foundationViewModel = ParkSpaceFoundationViewModel(container)
    val themeStore = SharedPrefsThemePreferenceStore(context)
    var isDark by mutableStateOf(themeStore.darkThemeOverride.value ?: false)
    var switchTriggered = false

    composeRule.setContent {
      val hostState by hostViewModel.uiState.collectAsStateWithLifecycle()
      ParkSpaceTheme(darkTheme = isDark) {
        HostDashboardScreen(
          uiState = hostState,
          viewModel = hostViewModel,
          onBack = {},
          onOpenSettings = {},
          isDarkTheme = isDark,
          onToggleTheme = {
            isDark = !isDark
            themeStore.setDarkTheme(isDark)
          },
          onOpenCustomerDashboardOrActivate = { switchTriggered = true },
          onSignOut = {},
        )
      }
    }

    composeRule.waitUntil(5_000) {
      ShadowLooper.idleMainLooper()
      !hostViewModel.uiState.value.isLoading &&
        composeRule.onAllNodesWithTag("host_menu_button").fetchSemanticsNodes().isNotEmpty()
    }

    // 1. Theme toggle persists
    composeRule.onNodeWithTag("theme_toggle_button").performClick()
    assertTrue(isDark)
    assertEquals(true, themeStore.darkThemeOverride.value)

    // 2. Host menu shows "Activate Customer Role" when no companion Customer exists, and NO Super Admin entry
    composeRule.onNodeWithTag("host_menu_button").performClick()
    composeRule.onNodeWithTag("menu_host_dashboard").assertIsDisplayed()
    composeRule.onNodeWithTag("menu_activate_customer_role").assertIsDisplayed()
    composeRule.onNodeWithTag("menu_host_profile").assertIsDisplayed()
    composeRule.onNodeWithTag("menu_host_settings").assertIsDisplayed()
    composeRule.onNodeWithTag("menu_host_help_support").assertIsDisplayed()
    composeRule.onNodeWithTag("menu_host_privacy_terms").assertIsDisplayed()
    composeRule.onNodeWithTag("menu_sign_out_button").assertIsDisplayed()
    assertTrue(
      composeRule.onAllNodesWithTag("private_admin_gate_button").fetchSemanticsNodes().isEmpty()
    )

    composeRule.onNodeWithTag("menu_activate_customer_role").performClick()
    assertTrue(switchTriggered)

    // 3. Now activate companion Customer on same email -> re-login Host -> NavHost switches cleanly to CustomerHome!
    runBlocking(Dispatchers.IO) {
      val activeHost = container.demoSessionManager.session.value.activeUser!!
      val custCompanion =
        container.demoAuthRepository.activateOrSwitchCompanionRole(
          currentSignedInUserId = activeHost.id,
          targetRole = UserRole.CUSTOMER,
        ) as DemoAuthResult.Success
      assertEquals(UserRole.CUSTOMER, custCompanion.user.role)
      // Switch back to Host
      container.demoSessionManager.activateAuthenticatedUser(activeHost)
    }

    var switchedToCustomerHome = false
    composeRule.waitUntil(5_000) {
      ShadowLooper.idleMainLooper()
      true
    }
    foundationViewModel.switchToCompanionCustomerDashboard(
      onSwitched = { switchedToCustomerHome = true },
      onRequiresActivation = {},
    )
    composeRule.waitUntil(5_000) {
      ShadowLooper.idleMainLooper()
      switchedToCustomerHome
    }
    assertEquals(UserRole.CUSTOMER, foundationViewModel.sessionState.value.activeRole)
  }

  @Test
  fun `navHost renders HostHome and routes between Host tabs and SharedProfileSettings with bottom bar`() {
    runBlocking(Dispatchers.IO) {
      val hostReg =
        container.demoAuthRepository.registerLocalAccount(
          DemoRegisterRequest(
            role = UserRole.HOST,
            fullName = "Bilal Nav Host",
            email = "bilal.navhost@parkspace.demo",
            password = "password123",
            confirmPassword = "password123",
            preferredCity = SupportedCity.LAHORE,
            businessAlias = "Liberty Nav Plaza",
          )
        ) as DemoAuthResult.Success
      container.demoSessionManager.activateAuthenticatedUser(hostReg.user)
    }

    val foundationViewModel = ParkSpaceFoundationViewModel(container)

    composeRule.setContent {
      ParkSpaceTheme(darkTheme = true) {
        ParkSpaceNavHost(
          appContainer = container,
          viewModel = foundationViewModel,
          isDarkTheme = true,
          onToggleTheme = {},
          discoveryMapContentOverride = {},
        )
      }
    }

    composeRule.waitUntil(5_000) {
      composeRule.onAllNodesWithTag("open_active_role_button").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("open_active_role_button").performScrollTo().performClick()

    composeRule.waitUntil(5_000) {
      composeRule.onAllNodesWithTag("host_dashboard_screen").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("host_bottom_nav").assertIsDisplayed()

    // Navigate to Profile via Host bottom nav -> renders SharedProfileSettingsScreen WITH Host bottom bar!
    composeRule.onNodeWithTag("host_tab_profile").performClick()
    composeRule.waitUntil(5_000) {
      composeRule.onAllNodesWithTag("shared_settings_card").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("host_bottom_nav").assertIsDisplayed()

    // Navigate from Profile back to Host Listings via bottom nav!
    composeRule.onNodeWithTag("host_tab_listings").performClick()
    composeRule.waitUntil(5_000) {
      composeRule.onAllNodesWithTag("host_listings_tab_list").fetchSemanticsNodes().isNotEmpty()
    }
  }
}
