package com.example

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.core.app.ApplicationProvider
import com.example.core.config.BookingType
import com.example.core.config.SupportedCity
import com.example.core.config.UserRole
import com.example.core.di.ParkSpaceAppContainer
import com.example.core.domain.model.AdminListingModerationState
import com.example.core.domain.model.AvailabilitySchedule
import com.example.core.domain.model.DemoPaymentMethod
import com.example.core.domain.model.DisputeCategory
import com.example.core.domain.model.DisputeStatus
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
import com.example.core.domain.repository.DisputeMutationResult
import com.example.core.domain.repository.HostBayMutationResult
import com.example.core.domain.repository.HostListingMutationResult
import com.example.core.navigation.NavGuardResult
import com.example.core.navigation.ParkSpaceNavGuard
import com.example.core.navigation.ParkSpaceNavHost
import com.example.core.navigation.ParkSpaceRoute
import com.example.demo.sandbox.DemoSandboxActionResult
import com.example.demo.sandbox.DemoSessionManager
import com.example.feature.admin.AdminConsoleTab
import com.example.feature.admin.AdminConsoleUiState
import com.example.feature.admin.AdminDashboardViewModel
import com.example.feature.admin.ui.PrivateAdminConsoleScreen
import com.example.feature.foundation.ParkSpaceFoundationViewModel
import com.example.ui.theme.ParkSpaceTheme
import com.example.ui.theme.SharedPrefsThemePreferenceStore
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
class AdminConsolePrompt4RobolectricTest {

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

  private fun sampleSpace(id: String, hostId: String, city: SupportedCity): ParkingSpace =
    ParkingSpace(
      id = id,
      hostId = hostId,
      title = "Blue Area Covered Deck",
      description = "Private guard-monitored underground parking bays.",
      parkingType = ParkingType.COVERED_GARAGE,
      location =
        ParkingLocation(
          city = city,
          areaLabel = "Jinnah Avenue",
          publicMaskedLatitude = city.centerLatitude,
          publicMaskedLongitude = city.centerLongitude,
          maskRadiusMeters = 200,
          privateGateSummary = "Basement Gate 2",
          privateExactLatitude = city.centerLatitude + 0.001,
          privateExactLongitude = city.centerLongitude + 0.001,
        ),
      hourlyRatePkr = 250,
      dailyRatePkr = 1600,
      monthlyRatePkr = 24000,
      monthlyEnabled = true,
      totalBays = 2,
      availableBays = 2,
      supportedVehicles = setOf(VehicleType.CAR, VehicleType.SUV),
      amenities = setOf(ParkingAmenity.COVERED, ParkingAmenity.CCTV),
      schedule =
        AvailabilitySchedule(
          openDays = ParkingDayOfWeek.entries.toSet(),
          openHour = 0,
          closeHour = 23,
          is24Hours = true,
        ),
      isApproved = false,
      isDemoListing = true,
    )

  @Test
  fun `empty admin console renders clean empty states zero metrics and five mobile bottom tabs`() {
    runBlocking(Dispatchers.IO) {
      assertTrue(
        container.demoSessionManager.unlockPrivateDemoAdminSandbox(true) is DemoSandboxActionResult.Success
      )
    }

    val adminViewModel = AdminDashboardViewModel(container.adminRepository, container.demoSessionManager)

    composeRule.setContent {
      val state by adminViewModel.uiState.collectAsStateWithLifecycle()
      ParkSpaceTheme(darkTheme = true) {
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
          isDarkTheme = true,
          onToggleTheme = {},
        )
      }
    }

    composeRule.waitUntil(5_000L) {
      ShadowLooper.idleMainLooper()
      adminViewModel.uiState.value.isAuthorizedAdmin &&
        composeRule.onAllNodesWithTag("admin_kyc_empty_state").fetchSemanticsNodes().isNotEmpty()
    }

    composeRule.onNodeWithTag("admin_bottom_nav").assertIsDisplayed()
    AdminConsoleTab.entries.forEach { tab ->
      composeRule.onNodeWithTag("admin_tab_${tab.name.lowercase()}").assertIsDisplayed()
    }
    composeRule.onNodeWithTag("admin_kyc_empty_state").assertIsDisplayed()

    // Analytics charts show explicit empty states with zero fabricated activity
    composeRule.onNodeWithTag("admin_tab_${AdminConsoleTab.ANALYTICS.name.lowercase()}").performClick()
    composeRule.waitUntil(5_000L) {
      composeRule.onAllNodesWithTag("admin_analytics_summary_card").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("admin_moderation_chart_empty").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("admin_city_chart_empty").performScrollTo().assertIsDisplayed()

    // Rules & Logs empty audit state
    composeRule.onNodeWithTag("admin_tab_${AdminConsoleTab.CONFIG_AND_LOGS.name.lowercase()}").performClick()
    composeRule.waitUntil(5_000L) {
      composeRule.onAllNodesWithTag("admin_commission_config_card").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("admin_audit_logs_empty_state").performScrollTo().assertIsDisplayed()
  }

  @Test
  fun `all five admin destinations approve kyc moderate listings resolve disputes plot real charts and persist commission rules`() {
    val (hostId, spaceId, disputeId) =
      runBlocking(Dispatchers.IO) {
        val pendingHostReg =
          container.demoAuthRepository.registerLocalAccount(
            DemoRegisterRequest(
              role = UserRole.HOST,
              fullName = "Bilal Owner",
              email = "bilal.host@parkspace.demo",
              password = "password123",
              confirmPassword = "password123",
              preferredCity = SupportedCity.ISLAMABAD,
              businessAlias = "Blue Area Deck",
            )
          ) as DemoAuthResult.Success
        val liveHostReg =
          container.demoAuthRepository.registerLocalAccount(
            DemoRegisterRequest(
              role = UserRole.HOST,
              fullName = "Nadia Verified Host",
              email = "nadia.host@parkspace.demo",
              password = "password123",
              confirmPassword = "password123",
              preferredCity = SupportedCity.ISLAMABAD,
              businessAlias = "F-7 Executive Bays",
            )
          ) as DemoAuthResult.Success
        val custReg =
          container.demoAuthRepository.registerLocalAccount(
            DemoRegisterRequest(
              role = UserRole.CUSTOMER,
              fullName = "Ayesha Driver",
              email = "ayesha.cust@parkspace.demo",
              password = "password123",
              confirmPassword = "password123",
              preferredCity = SupportedCity.ISLAMABAD,
            )
          ) as DemoAuthResult.Success

        // Draft listing from pending Host awaiting Admin moderation
        val draftRes =
          container.parkingRepository.createOrUpdateListing(
            hostId = pendingHostReg.user.id,
            space = sampleSpace("sp-admin-p4", pendingHostReg.user.id, SupportedCity.ISLAMABAD),
            publishImmediately = false,
          ) as HostListingMutationResult.Success

        val vehicle =
          container.userProfileRepository
            .addVehicleForUser(custReg.user.id, VehicleType.CAR, "Civic", "ISB-778")
            .getOrThrow()

        // Unlock Admin and verify second Host + live space + bay so Customer booking & dispute fixtures are genuine
        container.demoSessionManager.unlockPrivateDemoAdminSandbox(true)
        assertTrue(
          container.adminRepository.approveHostKyc(
            actorAdminId = DemoSessionManager.PRIVATE_DEMO_ADMIN_ID,
            hostUserId = liveHostReg.user.id,
            note = "Verified live fixture host",
          ) is AdminMutationResult.Success
        )

        val liveSpace =
          (container.parkingRepository.createOrUpdateListing(
            hostId = liveHostReg.user.id,
            space = sampleSpace("sp-admin-disp", liveHostReg.user.id, SupportedCity.ISLAMABAD),
            publishImmediately = true,
          ) as HostListingMutationResult.Success).space
        assertTrue(
          container.parkingRepository.addBayToSpace(
            hostId = liveHostReg.user.id,
            spaceId = liveSpace.id,
            bayCode = "Bay B-9",
            supportedVehicles = setOf(VehicleType.CAR, VehicleType.SUV),
            isCovered = true,
          ) is HostBayMutationResult.Success
        )

        val slot = ZonedDateTime.now().plusHours(4).withMinute(0).withSecond(0).withNano(0)
        val booking =
          (container.bookingRepository.createBookingWithAvailabilityCheck(
            BookingCreateRequest(
              spaceId = liveSpace.id,
              customerId = custReg.user.id,
              vehicleId = vehicle.id,
              bookingType = BookingType.HOURLY,
              units = 2,
              startEpochMs = slot.toInstant().toEpochMilli(),
              confirmDemoPaymentImmediately = true,
              paymentMethod = DemoPaymentMethod.EASYPAISA_DEMO,
            )
          ) as BookingMutationResult.Success).booking

        val dispute =
          (container.disputeRepository.fileDispute(
            bookingId = booking.id,
            reporterUserId = custReg.user.id,
            category = DisputeCategory.GATE_ACCESS_ISSUE,
            description = "Barrier delayed opening for 10 minutes.",
          ) as DisputeMutationResult.Success).dispute

        Triple(pendingHostReg.user.id, draftRes.space.id, dispute.id)
      }

    val adminViewModel = AdminDashboardViewModel(container.adminRepository, container.demoSessionManager)

    composeRule.setContent {
      val state by adminViewModel.uiState.collectAsStateWithLifecycle()
      ParkSpaceTheme(darkTheme = false) {
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
          isDarkTheme = false,
          onToggleTheme = {},
        )
      }
    }

    composeRule.waitUntil(5_000L) {
      ShadowLooper.idleMainLooper()
      adminViewModel.uiState.value.hostKycQueue.isNotEmpty() &&
        composeRule.onAllNodesWithTag("admin_kyc_card_$hostId").fetchSemanticsNodes().isNotEmpty()
    }

    // 1. Host KYC tab approves Host
    composeRule.onNodeWithTag("admin_approve_host_$hostId").performScrollTo().performClick()
    composeRule.onNodeWithTag("admin_confirm_moderation_button").performClick()
    composeRule.waitUntil(5_000L) {
      ShadowLooper.idleMainLooper()
      adminViewModel.uiState.value.hostKycQueue.firstOrNull { it.user.id == hostId }?.hostProfile?.verificationStatus ==
        HostVerificationStatus.APPROVED
    }

    // 2. Listings tab approves pending listing
    composeRule.onNodeWithTag("admin_tab_${AdminConsoleTab.LISTINGS.name.lowercase()}").performClick()
    composeRule.waitUntil(5_000L) {
      composeRule.onAllNodesWithTag("admin_listing_card_$spaceId").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("admin_approve_listing_$spaceId").performScrollTo().performClick()
    composeRule.onNodeWithTag("admin_confirm_moderation_button").performClick()
    composeRule.waitUntil(5_000L) {
      ShadowLooper.idleMainLooper()
      adminViewModel.uiState.value.listingQueue.firstOrNull { it.space.id == spaceId }?.moderationState ==
        AdminListingModerationState.APPROVED
    }

    // 3. Disputes tab resolves active dispute
    composeRule.onNodeWithTag("admin_tab_${AdminConsoleTab.DISPUTES.name.lowercase()}").performClick()
    composeRule.waitUntil(5_000L) {
      composeRule.onAllNodesWithTag("admin_dispute_card_$disputeId").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("admin_resolve_dispute_$disputeId").performScrollTo().performClick()
    composeRule.onNodeWithTag("admin_moderation_note_input").performTextInput("Confirmed gate log resolved")
    composeRule.onNodeWithTag("admin_confirm_moderation_button").performClick()
    composeRule.waitUntil(5_000L) {
      ShadowLooper.idleMainLooper()
      adminViewModel.uiState.value.disputesQueue.firstOrNull { it.dispute.id == disputeId }?.dispute?.status ==
        DisputeStatus.RESOLVED
    }

    // 4. Analytics tab renders real Canvas charts (no empty states) + 4 supported cities
    composeRule.onNodeWithTag("admin_tab_${AdminConsoleTab.ANALYTICS.name.lowercase()}").performClick()
    composeRule.waitUntil(5_000L) {
      composeRule.onAllNodesWithTag("admin_moderation_chart_card").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("admin_moderation_chart_card").performScrollTo().assertIsDisplayed()
    assertTrue(composeRule.onAllNodesWithTag("admin_moderation_chart_empty").fetchSemanticsNodes().isEmpty())
    composeRule.onNodeWithTag("admin_city_chart_card").performScrollTo().assertIsDisplayed()
    assertTrue(composeRule.onAllNodesWithTag("admin_city_chart_empty").fetchSemanticsNodes().isEmpty())
    composeRule.onNodeWithTag("admin_city_analytics_islamabad").performScrollTo().assertIsDisplayed()

    // 5. Rules & Logs updates commission rule & shows audit logs
    composeRule.onNodeWithTag("admin_tab_${AdminConsoleTab.CONFIG_AND_LOGS.name.lowercase()}").performClick()
    composeRule.waitUntil(5_000L) {
      composeRule.onAllNodesWithTag("admin_commission_percent_input").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("admin_commission_percent_input").performScrollTo().performTextReplacement("11")
    composeRule.onNodeWithTag("admin_save_commission_button").performScrollTo().performClick()
    composeRule.waitUntil(5_000L) {
      ShadowLooper.idleMainLooper()
      adminViewModel.uiState.value.commissionRule.commissionPercent == 11
    }
    composeRule.onNodeWithText("Demo Commission Rule Updated").assertIsDisplayed()
  }

  @Test
  fun `unauthorized customer host callers are blocked from Admin UI and theme toggle persists`() {
    // 1. NavGuard denies Customer & Host from PrivateAdminHome
    val customerSession =
      runBlocking(Dispatchers.IO) {
        val cust =
          container.demoAuthRepository.registerLocalAccount(
            DemoRegisterRequest(
              role = UserRole.CUSTOMER,
              fullName = "Zara Driver",
              email = "zara@parkspace.demo",
              password = "password123",
              confirmPassword = "password123",
              preferredCity = SupportedCity.LAHORE,
            )
          ) as DemoAuthResult.Success
        container.demoSessionManager.activateAuthenticatedUser(cust.user)
        container.demoSessionManager.session.value
      }
    assertTrue(
      ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.PrivateAdminHome, customerSession)
        is NavGuardResult.Denied
    )
    assertTrue(
      ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.DemoAuth(UserRole.SUPER_ADMIN, false), customerSession)
        is NavGuardResult.Denied
    )

    // 2. Unauthorized UI state blocks admin_shell_card and bottom nav cleanly
    val themeStore = SharedPrefsThemePreferenceStore(context)
    var isDark by mutableStateOf(themeStore.darkThemeOverride.value ?: false)
    var menuSignedOut = false

    composeRule.setContent {
      ParkSpaceTheme(darkTheme = isDark) {
        PrivateAdminConsoleScreen(
          uiState = AdminConsoleUiState(isAuthorizedAdmin = false),
          onBack = {},
          onOpenSettings = {},
          onSelectTab = {},
          onApproveHostKyc = { _, _ -> },
          onRejectHostKyc = { _, _ -> },
          onApproveListing = { _, _ -> },
          onRejectListing = { _, _ -> },
          onUpdateDisputeStatus = { _, _, _ -> },
          onUpdateCommissionRule = { _, _, _, _ -> },
          onDismissFeedback = {},
          isDarkTheme = isDark,
          onToggleTheme = {
            isDark = !isDark
            themeStore.setDarkTheme(isDark)
          },
          onSignOut = { menuSignedOut = true },
        )
      }
    }

    composeRule.onNodeWithTag("admin_unauthorized_card").assertIsDisplayed()
    assertTrue(composeRule.onAllNodesWithTag("admin_bottom_nav").fetchSemanticsNodes().isEmpty())
    assertTrue(composeRule.onAllNodesWithTag("admin_shell_card").fetchSemanticsNodes().isEmpty())

    // 3. Theme toggle persists
    composeRule.onNodeWithTag("theme_toggle_button").performClick()
    assertTrue(isDark)
    assertEquals(true, themeStore.darkThemeOverride.value)

    // 4. Admin menu exposes Admin tabs + Sign Out with zero Customer/Host role switcher shortcuts
    composeRule.onNodeWithTag("admin_menu_button").performClick()
    composeRule.onNodeWithTag("admin_menu_sign_out_button").performClick()
    assertTrue(menuSignedOut)
  }

  @Test
  fun `welcome entry keeps Admin hidden from main role cards while gated PrivateAdminHome renders cleanly`() {
    val foundationViewModel = ParkSpaceFoundationViewModel(container)

    composeRule.setContent {
      ParkSpaceTheme(darkTheme = false) {
        ParkSpaceNavHost(
          appContainer = container,
          viewModel = foundationViewModel,
          isDarkTheme = false,
          onToggleTheme = {},
          discoveryMapContentOverride = {},
        )
      }
    }

    composeRule.waitUntil(5_000L) {
      composeRule.onAllNodesWithTag("customer_entry_card").fetchSemanticsNodes().isNotEmpty()
    }
    // Welcome surface only shows Customer & Host role cards publicly (no Admin card)
    composeRule.onNodeWithTag("customer_entry_card").assertIsDisplayed()
    composeRule.onNodeWithTag("host_entry_card").assertIsDisplayed()
    assertTrue(
      composeRule.onAllNodesWithTag("private_admin_console_screen").fetchSemanticsNodes().isEmpty()
    )

    // Unlock via Private Admin Gate in secondary global menu
    composeRule.onNodeWithTag("global_menu_button").performClick()
    composeRule.onNodeWithTag("private_admin_gate_button").performClick()
    composeRule.waitUntil(5_000L) {
      composeRule.onAllNodesWithTag("admin_gate_dialog").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("admin_gate_acknowledge_checkbox").performClick()
    composeRule.onNodeWithTag("confirm_admin_sandbox_button").performClick()

    composeRule.waitUntil(5_000L) {
      composeRule.onAllNodesWithTag("private_admin_console_screen").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("admin_bottom_nav").assertIsDisplayed()
  }
}
