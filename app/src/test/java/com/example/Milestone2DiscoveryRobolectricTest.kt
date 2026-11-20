package com.example

import android.content.Context
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.common.UiState
import com.example.core.config.SupportedCity
import com.example.core.config.UserRole
import com.example.core.data.demo.DemoAdminRepositoryImpl
import com.example.core.data.demo.DemoAuthRepositoryImpl
import com.example.core.data.demo.DemoParkingRepositoryImpl
import com.example.core.data.demo.DemoUserProfileRepositoryImpl
import com.example.core.database.ParkSpaceDemoDatabase
import com.example.core.domain.model.AvailabilitySchedule
import com.example.core.domain.model.ParkingAmenity
import com.example.core.domain.model.ParkingDayOfWeek
import com.example.core.domain.model.ParkingLocation
import com.example.core.domain.model.ParkingSpace
import com.example.core.domain.model.ParkingType
import com.example.core.domain.model.VehicleType
import com.example.core.domain.repository.AdminMutationResult
import com.example.core.domain.repository.DemoAuthResult
import com.example.core.domain.repository.DemoRegisterRequest
import com.example.core.domain.repository.HostListingMutationResult
import com.example.core.navigation.NavGuardResult
import com.example.core.navigation.ParkSpaceNavGuard
import com.example.core.navigation.ParkSpaceRoute
import com.example.demo.sandbox.DemoSandboxActionResult
import com.example.demo.sandbox.DemoSessionManager
import com.example.demo.seed.DemoSeedInitializer
import com.example.feature.discovery.DiscoveryScreenData
import com.example.feature.discovery.DiscoveryViewModel
import com.example.feature.discovery.domain.model.MapRenderStatus
import com.example.feature.discovery.ui.ParkingDetailPreviewScreen
import com.example.feature.discovery.ui.ParkingDiscoveryScreen
import com.example.ui.theme.ParkSpaceTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
class Milestone2DiscoveryRobolectricTest {

  @get:Rule val composeRule = createComposeRule()

  private lateinit var database: ParkSpaceDemoDatabase
  private lateinit var seedInitializer: DemoSeedInitializer
  private lateinit var userRepo: DemoUserProfileRepositoryImpl
  private lateinit var authRepo: DemoAuthRepositoryImpl
  private lateinit var parkingRepo: DemoParkingRepositoryImpl
  private lateinit var sessionManager: DemoSessionManager
  private lateinit var adminRepo: DemoAdminRepositoryImpl
  private lateinit var viewModelStore: ViewModelStore
  private lateinit var discoveryViewModel: DiscoveryViewModel

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
    sessionManager = DemoSessionManager(userRepo)
    adminRepo = DemoAdminRepositoryImpl(database) { sessionManager.session.value }
    viewModelStore = ViewModelStore()
    discoveryViewModel =
      ViewModelProvider(
        viewModelStore,
        object : ViewModelProvider.Factory {
          @Suppress("UNCHECKED_CAST")
          override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            DiscoveryViewModel(parkingRepo, sessionManager) as T
        },
      )[DiscoveryViewModel::class.java]
  }

  @After
  fun tearDown() {
    viewModelStore.clear()
    database.close()
  }

  private fun seedHostCreatedDiscoveryInventory() {
    runBlocking(Dispatchers.IO) {
      seedInitializer.seedIfNeeded()
      val host =
        (authRepo.registerLocalAccount(
          DemoRegisterRequest(
            role = UserRole.HOST,
            fullName = "Raza Host",
            email = "raza.m2@parkspace.demo",
            password = "password123",
            confirmPassword = "password123",
            preferredCity = SupportedCity.LAHORE,
            businessAlias = "M2 Verified Garages",
          )
        ) as DemoAuthResult.Success).user

      assertTrue(sessionManager.unlockPrivateDemoAdminSandbox(true) is DemoSandboxActionResult.Success)
      val adminId = DemoSessionManager.PRIVATE_DEMO_ADMIN_ID
      assertTrue(adminRepo.approveHostKyc(adminId, host.id, "KYC verified", 1_000L) is AdminMutationResult.Success)

      val spaces =
        listOf(
          sampleDiscoverySpace("demo-lhr-03", host.id, SupportedCity.LAHORE, "Anarkali Yard", "Anarkali", 90),
          sampleDiscoverySpace("demo-lhr-02", host.id, SupportedCity.LAHORE, "DHA Phase 5 Deck", "DHA Phase 5", 150),
          sampleDiscoverySpace("demo-lhr-01", host.id, SupportedCity.LAHORE, "Gulberg Prime Garage", "Gulberg III", 200),
          sampleDiscoverySpace("demo-khi-01", host.id, SupportedCity.KARACHI, "Clifton Deck", "Clifton", 140),
          sampleDiscoverySpace("demo-khi-02", host.id, SupportedCity.KARACHI, "PECHS Compound", "PECHS", 180),
          sampleDiscoverySpace("demo-khi-03", host.id, SupportedCity.KARACHI, "Saddar Lot", "Saddar", 80),
        )
      spaces.forEach { space ->
        assertTrue(
          parkingRepo.createOrUpdateListing(host.id, space, publishImmediately = true)
            is HostListingMutationResult.Success
        )
        assertTrue(
          adminRepo.approveListing(adminId, space.id, "Approved for discovery", 2_000L)
            is AdminMutationResult.Success
        )
      }
      sessionManager.signOutDemoSandbox()
    }
  }

  private fun sampleDiscoverySpace(
    id: String,
    hostId: String,
    city: SupportedCity,
    title: String,
    areaLabel: String,
    hourlyRatePkr: Int,
  ): ParkingSpace =
    ParkingSpace(
      id = id,
      hostId = hostId,
      title = title,
      description = "Host-created verified parking in $areaLabel, ${city.displayName}.",
      parkingType = ParkingType.COVERED_GARAGE,
      location =
        ParkingLocation(
          city = city,
          areaLabel = areaLabel,
          publicMaskedLatitude = city.centerLatitude,
          publicMaskedLongitude = city.centerLongitude,
          maskRadiusMeters = 200,
        ),
      hourlyRatePkr = hourlyRatePkr,
      dailyRatePkr = hourlyRatePkr * 8,
      monthlyRatePkr = null,
      monthlyEnabled = false,
      totalBays = 4,
      availableBays = 3,
      supportedVehicles = setOf(VehicleType.CAR, VehicleType.BIKE),
      amenities = setOf(ParkingAmenity.COVERED, ParkingAmenity.CCTV),
      schedule = AvailabilitySchedule(ParkingDayOfWeek.entries.toSet(), 0, 23, true),
      isApproved = false,
      isDemoListing = true,
    )

  private fun startUiStateSubscription(): Job =
    CoroutineScope(Dispatchers.Main.immediate).launch {
      discoveryViewModel.uiState.collect {}
    }

  private fun awaitDiscoveryState(
    predicate: (DiscoveryScreenData) -> Boolean = { true },
  ): DiscoveryScreenData {
    var matched: DiscoveryScreenData? = null
    composeRule.waitUntil(timeoutMillis = 10_000) {
      ShadowLooper.idleMainLooper()
      val current = (discoveryViewModel.uiState.value as? UiState.Success)?.data
      if (current != null && predicate(current)) {
        matched = current
        true
      } else {
        false
      }
    }
    return checkNotNull(matched)
  }

  @Test
  fun `discovery viewmodel integrates room seed data, map states, and marker selection`() {
    seedHostCreatedDiscoveryInventory()
    val subscription = startUiStateSubscription()
    try {
      val initial = awaitDiscoveryState { it.result.listings.size == 3 }
      assertEquals(SupportedCity.LAHORE, initial.filter.city)
      assertEquals(3, initial.result.listings.size)
      assertEquals(3, initial.result.markers.size)

      // Map loading -> offline/error -> retry -> ready transitions
      assertTrue(initial.mapRenderStatus is MapRenderStatus.LoadingStyle)
      discoveryViewModel.onMapLoadFailed("Simulated offline")
      val errored = awaitDiscoveryState { it.mapRenderStatus is MapRenderStatus.OfflineOrError }
      assertTrue(errored.mapRenderStatus is MapRenderStatus.OfflineOrError)

      discoveryViewModel.onRetryMapLoad()
      val retrying = awaitDiscoveryState { it.mapRenderStatus is MapRenderStatus.LoadingStyle }
      assertTrue(retrying.mapRenderStatus is MapRenderStatus.LoadingStyle)

      discoveryViewModel.onMapStyleLoaded()
      val ready = awaitDiscoveryState { it.mapRenderStatus is MapRenderStatus.Ready }
      assertTrue(ready.mapRenderStatus is MapRenderStatus.Ready)

      // Marker selection & city switch clears selection and updates listings
      discoveryViewModel.onSelectSpace("demo-lhr-01")
      val selected = awaitDiscoveryState { it.selectedListing?.id == "demo-lhr-01" }
      assertNotNull(selected.selectedListing)

      discoveryViewModel.onSelectCity(SupportedCity.KARACHI)
      val karachi = awaitDiscoveryState { it.filter.city == SupportedCity.KARACHI && it.result.listings.size == 3 }
      assertNull(karachi.selectedListing)
      assertEquals(3, karachi.result.listings.size)
    } finally {
      subscription.cancel()
    }
  }

  @Test
  fun `navigation guard restricts discovery and detail preview strictly to customer role`() =
    runBlocking(Dispatchers.IO) {
      seedInitializer.seedIfNeeded()

      val guest = sessionManager.session.value
      assertTrue(
        ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.CustomerDiscovery, guest)
          is NavGuardResult.Denied
      )
      assertTrue(
        ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.ParkingDetailPreview("demo-lhr-01"), guest)
          is NavGuardResult.Denied
      )

      val hostUser =
        (authRepo.registerLocalAccount(
          DemoRegisterRequest(
            role = UserRole.HOST,
            fullName = "M2 Guard Host",
            email = "guard.host.m2@parkspace.demo",
            password = "password123",
            confirmPassword = "password123",
            preferredCity = SupportedCity.LAHORE,
            businessAlias = "Guard Garage",
          )
        ) as DemoAuthResult.Success).user
      assertTrue(sessionManager.activateAuthenticatedUser(hostUser) is DemoSandboxActionResult.Success)
      val host = sessionManager.session.value
      assertTrue(
        ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.CustomerDiscovery, host)
          is NavGuardResult.Denied
      )

      val customerUser =
        (authRepo.registerLocalAccount(
          DemoRegisterRequest(
            role = UserRole.CUSTOMER,
            fullName = "M2 Guard Customer",
            email = "guard.cust.m2@parkspace.demo",
            password = "password123",
            confirmPassword = "password123",
            preferredCity = SupportedCity.LAHORE,
          )
        ) as DemoAuthResult.Success).user
      assertTrue(sessionManager.activateAuthenticatedUser(customerUser) is DemoSandboxActionResult.Success)
      val customer = sessionManager.session.value
      assertTrue(
        ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.CustomerDiscovery, customer)
          is NavGuardResult.Allowed
      )
      assertTrue(
        ParkSpaceNavGuard.evaluateAccess(
          ParkSpaceRoute.ParkingDetailPreview("demo-lhr-01"),
          customer,
        ) is NavGuardResult.Allowed
      )
    }

  @Test
  fun `discovery compose screen supports search, empty reset, view toggle, and preview card`() {
    seedHostCreatedDiscoveryInventory()
    var navigatedDetailId: String? = null

    composeRule.setContent {
      val state = discoveryViewModel.uiState.collectAsStateWithLifecycle().value
      ParkSpaceTheme {
        ParkingDiscoveryScreen(
          uiState = state,
          viewModel = discoveryViewModel,
          onBack = {},
          onViewDetails = { navigatedDetailId = it },
          mapContentOverride = { data ->
            Text(
              text = "MapStub:${data.result.markers.size}",
              modifier = Modifier.testTag("map_stub"),
            )
          },
        )
      }
    }

    composeRule.waitUntil(timeoutMillis = 10_000) {
      composeRule.onAllNodesWithTag("map_stub").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("map_stub").assertIsDisplayed()

    // Switch to List View; demo-lhr-03 is sorted first (lowest hourly rate), and demo-lhr-01 is reachable via scroll
    composeRule.onNodeWithTag("toggle_list_button").performClick()
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodesWithTag("discovery_list_view").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("discovery_list_view").assertIsDisplayed()
    composeRule.onNodeWithTag("parking_card_demo-lhr-03").assertIsDisplayed()
    composeRule
      .onNodeWithTag("discovery_list_view")
      .performScrollToNode(hasTestTag("parking_card_demo-lhr-01"))
    composeRule.onNodeWithTag("parking_card_demo-lhr-01").assertIsDisplayed()

    // Search non-existent locality -> Empty state -> Reset button restores listings
    composeRule.onNodeWithTag("discovery_search_input").performTextInput("NowhereSector99")
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodesWithTag("discovery_empty_state").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("discovery_empty_state").assertIsDisplayed()

    composeRule.onNodeWithTag("empty_reset_filters_button").performClick()
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodesWithTag("parking_card_demo-lhr-03").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("parking_card_demo-lhr-03").assertIsDisplayed()

    // Tap marker selection -> Preview card renders in Map Mode -> View Details triggers route id
    composeRule.onNodeWithTag("toggle_map_button").performClick()
    discoveryViewModel.onSelectSpace("demo-lhr-01")
    composeRule.waitUntil(timeoutMillis = 5_000) {
      composeRule.onAllNodesWithTag("map_selected_preview_card").fetchSemanticsNodes().isNotEmpty()
    }
    composeRule.onNodeWithTag("map_selected_preview_card").assertIsDisplayed()
    composeRule.onNodeWithTag("view_details_demo-lhr-01").performClick()
    composeRule.waitForIdle()
    assertEquals("demo-lhr-01", navigatedDetailId)
  }

  @Test
  fun `detail preview screen displays privacy notice and milestone 3 boundary`() {
    seedHostCreatedDiscoveryInventory()
    val subscription = startUiStateSubscription()
    val listing =
      try {
        awaitDiscoveryState { it.result.listings.isNotEmpty() }.result.listings.first()
      } finally {
        subscription.cancel()
      }

    composeRule.setContent {
      ParkSpaceTheme {
        ParkingDetailPreviewScreen(listing = listing, onBack = {})
      }
    }

    composeRule.waitForIdle()
    composeRule.onNodeWithTag("detail_overview_card").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("detail_privacy_card").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("detail_booking_milestone_notice").performScrollTo().assertIsDisplayed()
  }
}
