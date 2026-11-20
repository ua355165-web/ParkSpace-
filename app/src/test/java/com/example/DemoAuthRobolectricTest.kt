package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.common.UiState
import com.example.core.config.BookingType
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
import com.example.core.domain.repository.BookingCreateRequest
import com.example.core.domain.repository.BookingMutationResult
import com.example.core.domain.repository.DemoAuthResult
import com.example.core.domain.repository.DemoRegisterRequest
import com.example.core.domain.repository.DemoSignInRequest
import com.example.core.domain.repository.HostListingMutationResult
import com.example.core.navigation.NavGuardResult
import com.example.core.navigation.ParkSpaceNavGuard
import com.example.core.navigation.ParkSpaceRoute
import com.example.demo.sandbox.DemoSessionManager
import com.example.demo.sandbox.DemoSessionState
import com.example.demo.sandbox.SharedPrefsDemoSessionPersistence
import com.example.demo.seed.DemoSeedInitializer
import com.example.feature.auth.DemoAuthViewModel
import com.example.feature.auth.ui.DemoAuthScreen
import com.example.feature.booking.domain.BookingAvailabilityValidator
import com.example.feature.foundation.FoundationDashboardData
import com.example.feature.foundation.ui.WelcomeEntryScreen
import com.example.ui.theme.ParkSpaceTheme
import java.time.LocalTime
import java.time.ZonedDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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
class DemoAuthRobolectricTest {

  @get:Rule val composeRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var database: ParkSpaceDemoDatabase
  private lateinit var seedInitializer: DemoSeedInitializer
  private lateinit var userRepo: DemoUserProfileRepositoryImpl
  private lateinit var authRepo: DemoAuthRepositoryImpl
  private lateinit var parkingRepo: DemoParkingRepositoryImpl
  private lateinit var bookingRepo: DemoBookingRepositoryImpl
  private lateinit var persistence: SharedPrefsDemoSessionPersistence

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    context.getSharedPreferences("parkspace_demo_session", Context.MODE_PRIVATE).edit().clear().commit()
    database =
      Room.inMemoryDatabaseBuilder(context, ParkSpaceDemoDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    seedInitializer = DemoSeedInitializer(database)
    userRepo = DemoUserProfileRepositoryImpl(database.userDao(), database)
    authRepo = DemoAuthRepositoryImpl(database)
    parkingRepo = DemoParkingRepositoryImpl(database)
    bookingRepo = DemoBookingRepositoryImpl(database)
    persistence = SharedPrefsDemoSessionPersistence(context)
  }

  @After
  fun tearDown() {
    database.close()
  }

  @Test
  fun `customer and host registration support temp emails, salted password hashes, duplicate rejection, and host pending status`() =
    runBlocking(Dispatchers.IO) {
      seedInitializer.seedIfNeeded()

      // 1. Customer registers using a temporary email format
      val custReg =
        authRepo.registerLocalAccount(
          DemoRegisterRequest(
            role = UserRole.CUSTOMER,
            fullName = "Sadia Khan",
            email = "Sadia.Temp+01@10minutemail.com",
            password = "secret123",
            confirmPassword = "secret123",
            preferredCity = SupportedCity.ISLAMABAD,
          )
        )
      assertTrue(custReg is DemoAuthResult.Success)
      val custUser = (custReg as DemoAuthResult.Success).user
      assertTrue(custUser.isSelfRegisteredDemoAccount)
      assertEquals(UserRole.CUSTOMER, custUser.role)

      // Stored entity must have salted hash, NEVER plaintext password
      val storedCust = database.userDao().getUserById(custUser.id)!!
      assertNotNull(storedCust.passwordSalt)
      assertNotNull(storedCust.passwordHash)
      assertNotEquals("secret123", storedCust.passwordHash)
      assertFalse(storedCust.passwordHash!!.contains("secret123"))

      // 2. Case-insensitive duplicate email in same role is rejected
      val dupCust =
        authRepo.registerLocalAccount(
          DemoRegisterRequest(
            role = UserRole.CUSTOMER,
            fullName = "Duplicate Sadia",
            email = "sadia.temp+01@10MINUTEMAIL.COM",
            password = "secret123",
            confirmPassword = "secret123",
          )
        )
      assertTrue(dupCust is DemoAuthResult.Rejected)

      // 3. Invalid password rejected; valid sign in succeeds
      val badSignIn =
        authRepo.signInLocalAccount(
          DemoSignInRequest(UserRole.CUSTOMER, "sadia.temp+01@10minutemail.com", "wrongpass")
        )
      assertTrue(badSignIn is DemoAuthResult.Rejected)

      val goodSignIn =
        authRepo.signInLocalAccount(
          DemoSignInRequest(UserRole.CUSTOMER, "sadia.temp+01@10minutemail.com", "secret123")
        )
      assertTrue(goodSignIn is DemoAuthResult.Success)
      assertEquals(custUser.id, (goodSignIn as DemoAuthResult.Success).user.id)

      // 4. Seeded preset user email cannot be signed into via public Demo auth
      val presetSignIn =
        authRepo.signInLocalAccount(
          DemoSignInRequest(UserRole.CUSTOMER, "ayesha.demo@parkspace.local", "anything")
        )
      assertTrue(presetSignIn is DemoAuthResult.Rejected)

      // 5. Host registers with initial PENDING_REVIEW verification status and approval lock
      val hostReg =
        authRepo.registerLocalAccount(
          DemoRegisterRequest(
            role = UserRole.HOST,
            fullName = "Kamran Ahmed",
            email = "sadia.temp+01@10minutemail.com",
            password = "hostpass99",
            confirmPassword = "hostpass99",
            preferredCity = SupportedCity.LAHORE,
            businessAlias = "Kamran Plaza Bays",
          )
        )
      assertTrue(hostReg is DemoAuthResult.Success)
      val hostUser = (hostReg as DemoAuthResult.Success).user
      val hostProfile = userRepo.observeHostProfile(hostUser.id).first()!!
      assertEquals(HostVerificationStatus.PENDING_REVIEW, hostProfile.verificationStatus)
      assertFalse(hostProfile.verificationStatus.canPublishListings)
      assertEquals("Kamran Plaza Bays", hostProfile.displayBusinessAlias)

      // 6. SUPER_ADMIN cannot be registered or logged in publicly
      val adminReg =
        authRepo.registerLocalAccount(
          DemoRegisterRequest(
            role = UserRole.SUPER_ADMIN,
            fullName = "Admin Try",
            email = "admin@parkspace.local",
            password = "password1",
            confirmPassword = "password1",
          )
        )
      assertTrue(adminReg is DemoAuthResult.Rejected)
      assertTrue(
        ParkSpaceNavGuard.evaluateAccess(
          ParkSpaceRoute.DemoAuth(UserRole.SUPER_ADMIN),
          DemoSessionState(),
        ) is NavGuardResult.Denied
      )
    }

  @Test
  fun `session persists across restarts without silent preset login and clears on logout`() =
    runBlocking(Dispatchers.IO) {
      seedInitializer.seedIfNeeded()

      // Fresh launch must remain unauthenticated (no Ayesha/Usman auto-login)
      val launchSessionManager = DemoSessionManager(userRepo, persistence)
      launchSessionManager.restorePersistedSession()
      assertNull(launchSessionManager.session.value.activeUser)

      // Register and activate Customer
      val reg =
        authRepo.registerLocalAccount(
          DemoRegisterRequest(
            role = UserRole.CUSTOMER,
            fullName = "Nadia Farooq",
            email = "nadia@mailinator.com",
            password = "password123",
            confirmPassword = "password123",
            preferredCity = SupportedCity.KARACHI,
          )
        ) as DemoAuthResult.Success
      launchSessionManager.activateAuthenticatedUser(reg.user)
      assertEquals(reg.user.id, launchSessionManager.session.value.activeUser?.id)

      // Simulate cold app restart with new DemoSessionManager reading SharedPreferences
      val restartedManager = DemoSessionManager(userRepo, persistence)
      restartedManager.restorePersistedSession()
      assertNotNull(restartedManager.session.value.activeUser)
      assertEquals(reg.user.id, restartedManager.session.value.activeUser?.id)
      assertEquals(UserRole.CUSTOMER, restartedManager.session.value.activeRole)
      assertEquals(SupportedCity.KARACHI, restartedManager.session.value.selectedCity)

      // Logout clears active session across subsequent restart
      restartedManager.signOutDemoSandbox()
      assertNull(restartedManager.session.value.activeUser)

      val afterLogoutManager = DemoSessionManager(userRepo, persistence)
      afterLogoutManager.restorePersistedSession()
      assertNull(afterLogoutManager.session.value.activeUser)
    }

  @Test
  fun `account specific data isolation keeps vehicles bookings and host properties separate while supporting m3 booking`() =
    runBlocking(Dispatchers.IO) {
      seedInitializer.seedIfNeeded()

      val custA =
        (authRepo.registerLocalAccount(
          DemoRegisterRequest(
            role = UserRole.CUSTOMER,
            fullName = "Customer Alpha",
            email = "alpha@temp-mail.org",
            password = "password123",
            confirmPassword = "password123",
          )
        ) as DemoAuthResult.Success).user

      val custB =
        (authRepo.registerLocalAccount(
          DemoRegisterRequest(
            role = UserRole.CUSTOMER,
            fullName = "Customer Beta",
            email = "beta@temp-mail.org",
            password = "password123",
            confirmPassword = "password123",
          )
        ) as DemoAuthResult.Success).user

      val hostH =
        (authRepo.registerLocalAccount(
          DemoRegisterRequest(
            role = UserRole.HOST,
            fullName = "Host Gamma",
            email = "gamma@temp-mail.org",
            password = "password123",
            confirmPassword = "password123",
          )
        ) as DemoAuthResult.Success).user

      // Newly registered accounts start clean (zero seeded vehicles/bookings/spaces leaked)
      assertTrue(userRepo.observeVehiclesForUser(custA.id).first().isEmpty())
      assertTrue(bookingRepo.observeBookingsForCustomer(custA.id).first().isEmpty())
      assertTrue(parkingRepo.observeSpacesByHost(hostH.id).first().isEmpty())
      assertTrue(bookingRepo.observeBookingsForHost(hostH.id).first().isEmpty())

      // Approve Host H and publish a real Host-created listing (empty-marketplace: no pre-seeded listings)
      val hostEntity = database.userDao().getHostProfileOnce(hostH.id)!!
      database.userDao().upsertHostProfile(
        hostEntity.copy(verificationStatus = HostVerificationStatus.APPROVED)
      )
      val hostSpace =
        ParkingSpace(
          id = "host-gamma-space-01",
          hostId = hostH.id,
          title = "Gamma Gulberg Covered Garage",
          description = "Private covered parking created by Host Gamma.",
          parkingType = ParkingType.COVERED_GARAGE,
          location = ParkingLocation(SupportedCity.LAHORE, "Gulberg III", 31.512, 74.348),
          hourlyRatePkr = 150,
          dailyRatePkr = 1200,
          monthlyRatePkr = null,
          monthlyEnabled = false,
          totalBays = 2,
          availableBays = 2,
          supportedVehicles = setOf(VehicleType.CAR, VehicleType.BIKE),
          amenities = setOf(ParkingAmenity.CCTV, ParkingAmenity.COVERED),
          schedule = AvailabilitySchedule(ParkingDayOfWeek.entries.toSet(), 0, 23, true),
          isApproved = false,
          isDemoListing = true,
        )
      val listingRes = parkingRepo.createOrUpdateListing(hostH.id, hostSpace, publishImmediately = true)
      assertTrue(listingRes is HostListingMutationResult.Success)
      val createdSpaceId = (listingRes as HostListingMutationResult.Success).space.id

      // Customer A adds own vehicle & books Host H's real space (M3 flow remains intact)
      val addedVehicle =
        userRepo
          .addVehicleForUser(
            ownerUserId = custA.id,
            vehicleType = VehicleType.CAR,
            label = "Alpha Civic",
            plateNumber = "ICT-7788",
            makeDefault = true,
          )
          .getOrThrow()

      val futureStart =
        ZonedDateTime.now(BookingAvailabilityValidator.PAKISTAN_ZONE)
          .toLocalDate()
          .plusDays(2)
          .atTime(LocalTime.of(10, 0))
          .atZone(BookingAvailabilityValidator.PAKISTAN_ZONE)
          .toInstant()
          .toEpochMilli()

      val booked =
        bookingRepo.createBookingWithAvailabilityCheck(
          BookingCreateRequest(
            spaceId = createdSpaceId,
            customerId = custA.id,
            vehicleId = addedVehicle.id,
            bookingType = BookingType.HOURLY,
            units = 2,
            startEpochMs = futureStart,
            confirmDemoPaymentImmediately = true,
          )
        )
      assertTrue(booked is BookingMutationResult.Success)

      // Customer B must still see ZERO vehicles and ZERO bookings; Host H sees its own listing & booking
      assertEquals(1, userRepo.observeVehiclesForUser(custA.id).first().size)
      assertEquals(1, bookingRepo.observeBookingsForCustomer(custA.id).first().size)
      assertEquals(1, parkingRepo.observeSpacesByHost(hostH.id).first().size)
      assertEquals(1, bookingRepo.observeBookingsForHost(hostH.id).first().size)
      assertTrue(userRepo.observeVehiclesForUser(custB.id).first().isEmpty())
      assertTrue(bookingRepo.observeBookingsForCustomer(custB.id).first().isEmpty())
    }

  @Test
  fun `welcome entry hides preset users, exposes customer host glass cards, and keeps settings and admin gate inside menu`() {
    runBlocking(Dispatchers.IO) { seedInitializer.seedIfNeeded() }
    val openedRoles = mutableListOf<UserRole>()
    var darkThemeState = false

    composeRule.setContent {
      ParkSpaceTheme(darkTheme = darkThemeState) {
        WelcomeEntryScreen(
          uiState =
            UiState.Success(
              FoundationDashboardData(
                session = DemoSessionState(),
                approvedSpacesInSelectedCity = emptyList(),
                allSpacesCount = 0,
                approvedSpacesCount = 0,
                pendingSpacesCount = 0,
                activeCustomerProfile = null,
                activeUserVehicles = emptyList(),
                activeHostProfile = null,
                activeHostSpaces = emptyList(),
                activeUserBookings = emptyList(),
              )
            ),
          sandboxFeedback = null,
          isDarkTheme = darkThemeState,
          onToggleTheme = { darkThemeState = !darkThemeState },
          onSelectCity = {},
          onOpenAuth = { role, _ -> openedRoles += role },
          onSignOut = {},
          onUnlockPrivateAdmin = {},
          onNavigateToRoleHome = {},
          onNavigateToSettings = {},
          onRetrySeed = {},
          onDismissFeedback = {},
        )
      }
    }

    composeRule.waitForIdle()
    composeRule.onNodeWithTag("theme_toggle_button").assertIsDisplayed()
    composeRule.onNodeWithTag("global_menu_button").assertIsDisplayed()
    composeRule.onNodeWithTag("customer_entry_card").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("host_entry_card").performScrollTo().assertIsDisplayed()

    // Single header theme toggle flips global theme state directly
    composeRule.onNodeWithTag("theme_toggle_button").performClick()
    assertTrue(darkThemeState)

    // Tapping Customer & Host cards routes directly to respective auth flows
    composeRule.onNodeWithTag("customer_entry_card").performClick()
    composeRule.onNodeWithTag("host_entry_card").performClick()
    assertEquals(listOf(UserRole.CUSTOMER, UserRole.HOST), openedRoles)

    // Settings and Private Admin Gate live inside the Menu (without duplicate Dark Appearance item)
    composeRule.onNodeWithTag("global_menu_button").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("open_settings_button").assertIsDisplayed()
    composeRule.onNodeWithTag("private_admin_gate_button").assertIsDisplayed()
    assertTrue(composeRule.onAllNodesWithTag("menu_theme_toggle_item").fetchSemanticsNodes().isEmpty())

    // Clicking Settings in Menu while signed out opens Guest Preferences Dialog without duplicate dark switch
    composeRule.onNodeWithTag("open_settings_button").performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag("guest_settings_dialog").assertIsDisplayed()
    assertTrue(composeRule.onAllNodesWithTag("guest_settings_dark_switch").fetchSemanticsNodes().isEmpty())
    composeRule.onNodeWithTag("guest_settings_close_button").performClick()

    assertTrue(composeRule.onAllNodesWithText("Ayesha", substring = true).fetchSemanticsNodes().isEmpty())
    assertTrue(composeRule.onAllNodesWithText("Usman", substring = true).fetchSemanticsNodes().isEmpty())
  }

  @Test
  fun `theme preference persists dark and light selection across store restarts`() {
    context.getSharedPreferences(
      com.example.ui.theme.SharedPrefsThemePreferenceStore.PREFS_NAME,
      Context.MODE_PRIVATE,
    ).edit().clear().commit()

    val firstStore = com.example.ui.theme.SharedPrefsThemePreferenceStore(context)
    assertNull(firstStore.darkThemeOverride.value)

    firstStore.setDarkTheme(true)
    assertEquals(true, firstStore.darkThemeOverride.value)

    val restartedStore = com.example.ui.theme.SharedPrefsThemePreferenceStore(context)
    assertEquals(true, restartedStore.darkThemeOverride.value)

    restartedStore.toggleTheme(currentEffectiveDark = true)
    val thirdStore = com.example.ui.theme.SharedPrefsThemePreferenceStore(context)
    assertEquals(false, thirdStore.darkThemeOverride.value)
  }

  @Test
  fun `dual role activation creates companion host role in pending review and switches seamlessly`() =
    runBlocking(Dispatchers.IO) {
      seedInitializer.seedIfNeeded()
      val sessionManager = DemoSessionManager(userRepo, persistence)

      val cust =
        (authRepo.registerLocalAccount(
          DemoRegisterRequest(
            role = UserRole.CUSTOMER,
            fullName = "Bilal Dual",
            email = "bilal.dual@mailinator.com",
            password = "password123",
            confirmPassword = "password123",
            preferredCity = SupportedCity.ISLAMABAD,
          )
        ) as DemoAuthResult.Success).user
      sessionManager.activateAuthenticatedUser(cust)

      // No companion Host exists yet for same email
      assertFalse(authRepo.hasAccountForEmailAndRole("bilal.dual@mailinator.com", UserRole.HOST))

      // Activate Host companion role from signed-in Customer
      val hostActivation =
        authRepo.activateOrSwitchCompanionRole(
          currentSignedInUserId = cust.id,
          targetRole = UserRole.HOST,
          businessAlias = "Bilal F-7 Bays",
        )
      assertTrue(hostActivation is DemoAuthResult.Success)
      val hostUser = (hostActivation as DemoAuthResult.Success).user
      assertEquals(UserRole.HOST, hostUser.role)
      assertNotEquals(cust.id, hostUser.id)
      assertEquals("bilal.dual@mailinator.com", hostUser.emailMasked)

      val hostProfile = userRepo.observeHostProfile(hostUser.id).first()!!
      assertEquals(HostVerificationStatus.PENDING_REVIEW, hostProfile.verificationStatus)
      assertFalse(hostProfile.verificationStatus.canPublishListings)
      assertEquals("Bilal F-7 Bays", hostProfile.displayBusinessAlias)

      // Activate session as Host and switch cleanly back to existing Customer
      sessionManager.activateAuthenticatedUser(hostUser)
      assertTrue(authRepo.hasAccountForEmailAndRole("bilal.dual@mailinator.com", UserRole.CUSTOMER))
      val backToCust =
        authRepo.activateOrSwitchCompanionRole(
          currentSignedInUserId = hostUser.id,
          targetRole = UserRole.CUSTOMER,
        )
      assertTrue(backToCust is DemoAuthResult.Success)
      assertEquals(cust.id, (backToCust as DemoAuthResult.Success).user.id)

      // Security check: Dual-Role must always reject SUPER_ADMIN
      val blockedAdmin =
        authRepo.activateOrSwitchCompanionRole(
          currentSignedInUserId = cust.id,
          targetRole = UserRole.SUPER_ADMIN,
        )
      assertTrue(blockedAdmin is DemoAuthResult.Rejected)
    }

  @Test
  fun `demo auth screen registers new customer end to end`() {
    runBlocking(Dispatchers.IO) { seedInitializer.seedIfNeeded() }

    val sessionManager = DemoSessionManager(userRepo, persistence)
    val authVm = DemoAuthViewModel(UserRole.CUSTOMER, true, authRepo, sessionManager)
    var authenticatedRole: UserRole? = null

    composeRule.setContent {
      val state = authVm.uiState.collectAsStateWithLifecycle().value
      ParkSpaceTheme {
        DemoAuthScreen(
          uiState = state,
          onBack = {},
          onSelectMode = authVm::setMode,
          onFullNameChange = authVm::onFullNameChange,
          onEmailChange = authVm::onEmailChange,
          onPasswordChange = authVm::onPasswordChange,
          onConfirmPasswordChange = authVm::onConfirmPasswordChange,
          onBusinessAliasChange = authVm::onBusinessAliasChange,
          onSelectCity = authVm::onSelectCity,
          onSubmit = { authVm.submit { authenticatedRole = it } },
        )
      }
    }

    composeRule.onNodeWithTag("auth_full_name_input").performScrollTo().performTextInput("Tariq Mahmood")
    composeRule.onNodeWithTag("auth_email_input").performScrollTo().performTextInput("tariq@mailinator.com")
    composeRule.onNodeWithTag("auth_password_input").performScrollTo().performTextInput("demo9876")
    composeRule.onNodeWithTag("auth_confirm_password_input").performScrollTo().performTextInput("demo9876")
    composeRule.onNodeWithTag("auth_submit_button").performScrollTo().performClick()

    composeRule.waitUntil(timeoutMillis = 10_000) {
      ShadowLooper.idleMainLooper()
      authenticatedRole != null
    }
    assertEquals(UserRole.CUSTOMER, authenticatedRole)
    assertEquals("tariq@mailinator.com", sessionManager.session.value.activeUser?.emailMasked)
  }
}
