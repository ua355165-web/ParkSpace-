package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
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
import com.example.core.domain.model.HostVerificationStatus
import com.example.core.domain.model.VehicleType
import com.example.core.domain.repository.BookingCreateRequest
import com.example.core.domain.repository.BookingMutationResult
import com.example.core.domain.repository.DemoAuthResult
import com.example.core.domain.repository.DemoRegisterRequest
import com.example.core.domain.repository.DemoSignInRequest
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
    parkingRepo = DemoParkingRepositoryImpl(database.parkingDao())
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

      // Customer A adds own vehicle & books seeded space (M3 flow remains intact)
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
            spaceId = "demo-lhr-01",
            customerId = custA.id,
            vehicleId = addedVehicle.id,
            bookingType = BookingType.HOURLY,
            units = 2,
            startEpochMs = futureStart,
            confirmDemoPaymentImmediately = true,
          )
        )
      assertTrue(booked is BookingMutationResult.Success)

      // Customer B must still see ZERO vehicles and ZERO bookings
      assertEquals(1, userRepo.observeVehiclesForUser(custA.id).first().size)
      assertEquals(1, bookingRepo.observeBookingsForCustomer(custA.id).first().size)
      assertTrue(userRepo.observeVehiclesForUser(custB.id).first().isEmpty())
      assertTrue(bookingRepo.observeBookingsForCustomer(custB.id).first().isEmpty())
    }

  @Test
  fun `welcome entry hides preset users and shows customer and host sign in or register options`() {
    runBlocking(Dispatchers.IO) { seedInitializer.seedIfNeeded() }
    val spaces = runBlocking(Dispatchers.IO) {
      parkingRepo.observeApprovedSpacesByCity(SupportedCity.LAHORE).first()
    }

    composeRule.setContent {
      ParkSpaceTheme {
        WelcomeEntryScreen(
          uiState =
            UiState.Success(
              FoundationDashboardData(
                session = DemoSessionState(),
                approvedSpacesInSelectedCity = spaces,
                allSpacesCount = 12,
                approvedSpacesCount = 11,
                pendingSpacesCount = 1,
                activeCustomerProfile = null,
                activeUserVehicles = emptyList(),
                activeHostProfile = null,
                activeHostSpaces = emptyList(),
                activeUserBookings = emptyList(),
              )
            ),
          sandboxFeedback = null,
          isDarkTheme = false,
          onToggleTheme = {},
          onSelectCity = {},
          onOpenAuth = { _, _ -> },
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
    composeRule.onNodeWithTag("customer_sign_in_button").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("customer_register_button").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("host_sign_in_button").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("host_register_button").performScrollTo().assertIsDisplayed()
    composeRule.onNodeWithTag("private_admin_gate_button").performScrollTo().assertIsDisplayed()
    assertTrue(composeRule.onAllNodesWithText("Ayesha", substring = true).fetchSemanticsNodes().isEmpty())
    assertTrue(composeRule.onAllNodesWithText("Usman", substring = true).fetchSemanticsNodes().isEmpty())
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
