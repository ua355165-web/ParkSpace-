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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.core.config.AppEnvironment
import com.example.core.config.BookingType
import com.example.core.config.SupportedCity
import com.example.core.config.UserRole
import com.example.core.data.demo.DemoAuthRepositoryImpl
import com.example.core.data.demo.DemoBookingRepositoryImpl
import com.example.core.data.demo.DemoParkingRepositoryImpl
import com.example.core.data.demo.DemoUserProfileRepositoryImpl
import com.example.core.database.ParkSpaceDemoDatabase
import com.example.core.database.mapper.toEntity
import com.example.core.domain.model.AvailabilitySchedule
import com.example.core.domain.model.Booking
import com.example.core.domain.model.BookingStatus
import com.example.core.domain.model.DemoPaymentStatus
import com.example.core.domain.model.HostProfile
import com.example.core.domain.model.HostVerificationStatus
import com.example.core.domain.model.ParkingAmenity
import com.example.core.domain.model.ParkingDayOfWeek
import com.example.core.domain.model.ParkingLocation
import com.example.core.domain.model.ParkingSpace
import com.example.core.domain.model.ParkingType
import com.example.core.domain.model.UserProfile
import com.example.core.domain.model.VehicleType
import com.example.core.domain.repository.BookingCreateRequest
import com.example.core.domain.repository.BookingMutationResult
import com.example.core.domain.repository.DemoAuthResult
import com.example.core.domain.repository.DemoRegisterRequest
import com.example.core.domain.repository.HostBayMutationResult
import com.example.core.domain.repository.HostListingMutationResult
import com.example.core.domain.repository.HostScheduleMutationResult
import com.example.demo.sandbox.DemoSessionManager
import com.example.demo.sandbox.SharedPrefsDemoSessionPersistence
import com.example.demo.seed.DemoSeedInitializer
import com.example.feature.booking.domain.BookingAvailabilityValidator
import com.example.feature.host.HostDashboardViewModel
import com.example.feature.host.domain.HostLedgerCalculator
import com.example.feature.host.ui.HostBaysManagerDialog
import com.example.feature.host.ui.HostDashboardScreen
import com.example.ui.theme.ParkSpaceTheme
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
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
class Milestone4HostRobolectricTest {

  @get:Rule val composeRule = createComposeRule()

  private lateinit var context: Context
  private lateinit var database: ParkSpaceDemoDatabase
  private lateinit var seedInitializer: DemoSeedInitializer
  private lateinit var userRepo: DemoUserProfileRepositoryImpl
  private lateinit var authRepo: DemoAuthRepositoryImpl
  private lateinit var parkingRepo: DemoParkingRepositoryImpl
  private lateinit var bookingRepo: DemoBookingRepositoryImpl
  private lateinit var persistence: SharedPrefsDemoSessionPersistence
  private lateinit var sessionManager: DemoSessionManager
  private lateinit var viewModelStore: ViewModelStore

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
    sessionManager = DemoSessionManager(userRepo, persistence)
    viewModelStore = ViewModelStore()
  }

  private fun createHostDashboardViewModel(): HostDashboardViewModel {
    val factory =
      object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
          HostDashboardViewModel(
            parkingRepository = parkingRepo,
            bookingRepository = bookingRepo,
            userProfileRepository = userRepo,
            demoSessionManager = sessionManager,
          ) as T
      }
    return ViewModelProvider(viewModelStore, factory)[HostDashboardViewModel::class.java]
  }

  @After
  fun tearDown() {
    viewModelStore.clear()
    ShadowLooper.idleMainLooper()
    runCatching { database.close() }
  }

  private suspend fun createApprovedHostWithSpace(
    email: String,
    fullName: String,
    city: SupportedCity,
    spaceId: String,
    title: String,
    areaLabel: String,
  ): Pair<UserProfile, ParkingSpace> {
    val hostUser =
      (authRepo.registerLocalAccount(
        DemoRegisterRequest(
          role = UserRole.HOST,
          fullName = fullName,
          email = email,
          password = "password123",
          confirmPassword = "password123",
          preferredCity = city,
          businessAlias = "$fullName Plaza",
        )
      ) as DemoAuthResult.Success).user
    val hp = database.userDao().getHostProfileOnce(hostUser.id)!!
    database.userDao().upsertHostProfile(hp.copy(verificationStatus = HostVerificationStatus.APPROVED))
    val draft =
      ParkingSpace(
        id = spaceId,
        hostId = hostUser.id,
        title = title,
        description = "Host-created parking facility in $areaLabel.",
        parkingType = ParkingType.COVERED_GARAGE,
        location =
          ParkingLocation(
            city = city,
            areaLabel = areaLabel,
            publicMaskedLatitude = city.centerLatitude,
            publicMaskedLongitude = city.centerLongitude,
            maskRadiusMeters = AppEnvironment.PRE_BOOKING_LOCATION_MASK_RADIUS_METERS,
            privateGateSummary = "Guard Gate A",
          ),
        hourlyRatePkr = 150,
        dailyRatePkr = 1200,
        monthlyRatePkr = null,
        monthlyEnabled = false,
        totalBays = 1,
        availableBays = 1,
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
    val res = parkingRepo.createOrUpdateListing(hostUser.id, draft, publishImmediately = true)
    assertTrue("Approved host must create and publish listing", res is HostListingMutationResult.Success)
    return hostUser to (res as HostListingMutationResult.Success).space
  }

  @Test
  fun `scenario 1 - host can create and edit their own listing`() =
    runBlocking(Dispatchers.IO) {
      seedInitializer.seedIfNeeded()

      // Register new local host
      val hostReg =
        authRepo.registerLocalAccount(
          DemoRegisterRequest(
            role = UserRole.HOST,
            fullName = "Tariq Malik",
            email = "tariq.malik@host.pk",
            password = "password123",
            confirmPassword = "password123",
            preferredCity = SupportedCity.LAHORE,
            businessAlias = "Tariq Parking Hub",
          )
        )
      val hostUser = (hostReg as DemoAuthResult.Success).user

      val newSpace =
        ParkingSpace(
          id = "space-tariq-01",
          hostId = hostUser.id,
          title = "Tariq Executive Parking",
          description = "Covered basement slots with guard on Main Boulevard Gulberg.",
          parkingType = ParkingType.COVERED_GARAGE,
          location =
            ParkingLocation(
              city = SupportedCity.LAHORE,
              areaLabel = "Gulberg III",
              publicMaskedLatitude = 31.5204,
              publicMaskedLongitude = 74.3587,
              maskRadiusMeters = AppEnvironment.PRE_BOOKING_LOCATION_MASK_RADIUS_METERS,
              privateGateSummary = "Gate 2 next to Mall entry. PIN required.",
            ),
          hourlyRatePkr = 200,
          dailyRatePkr = 1600,
          monthlyRatePkr = 22000,
          monthlyEnabled = true,
          totalBays = 1,
          availableBays = 1,
          supportedVehicles = setOf(VehicleType.CAR, VehicleType.SUV),
          amenities = setOf(ParkingAmenity.COVERED, ParkingAmenity.CCTV, ParkingAmenity.SECURITY_GUARD),
          schedule =
            AvailabilitySchedule(
              openDays = ParkingDayOfWeek.entries.toSet(),
              openHour = 0,
              closeHour = 0,
              is24Hours = true,
            ),
          isApproved = false,
          isDemoListing = true,
          photoTags = listOf("Entrance Gate", "Covered Bay"),
        )

      // Create listing as Draft
      val createRes = parkingRepo.createOrUpdateListing(hostUser.id, newSpace, publishImmediately = false)
      assertTrue("Create listing should succeed as draft", createRes is HostListingMutationResult.Success)

      val savedSpace = parkingRepo.getSpaceById("space-tariq-01")
      assertNotNull(savedSpace)
      assertEquals("Tariq Executive Parking", savedSpace?.title)
      assertFalse("Draft space should not be approved", savedSpace?.isApproved ?: true)

      // Verify initial bay auto-created
      val bays = parkingRepo.observeBaysForSpace("space-tariq-01").first()
      assertEquals("Initial bay must be auto-created", 1, bays.size)
      assertEquals("Bay A-1", bays.first().bayCode)
      assertTrue(bays.first().isActiveInDemo)

      // Edit listing
      val updatedSpace = savedSpace!!.copy(
        title = "Tariq Executive Parking (Updated)",
        dailyRatePkr = 1800,
      )
      val editRes = parkingRepo.createOrUpdateListing(hostUser.id, updatedSpace, publishImmediately = false)
      assertTrue("Edit listing should succeed", editRes is HostListingMutationResult.Success)

      val reloaded = parkingRepo.getSpaceById("space-tariq-01")
      assertEquals("Tariq Executive Parking (Updated)", reloaded?.title)
      assertEquals(1800, reloaded?.dailyRatePkr)
    }

  @Test
  fun `scenario 2 - unapproved host cannot publish a listing but approved host can`() =
    runBlocking(Dispatchers.IO) {
      seedInitializer.seedIfNeeded()

      // Self-registered host starts as PENDING_REVIEW
      val hostReg =
        authRepo.registerLocalAccount(
          DemoRegisterRequest(
            role = UserRole.HOST,
            fullName = "Kashif Javed",
            email = "kashif@pendinghost.pk",
            password = "password123",
            confirmPassword = "password123",
            preferredCity = SupportedCity.ISLAMABAD,
            businessAlias = "Kashif AutoPark",
          )
        )
      val hostUser = (hostReg as DemoAuthResult.Success).user

      val hostProfile = userRepo.observeHostProfile(hostUser.id).first()
      assertEquals(HostVerificationStatus.PENDING_REVIEW, hostProfile?.verificationStatus)
      assertFalse(hostProfile?.verificationStatus?.canPublishListings ?: true)

      val space =
        ParkingSpace(
          id = "space-kashif-01",
          hostId = hostUser.id,
          title = "Kashif F-7 Secure Bay",
          description = "Paved driveway near Jinnah Super.",
          parkingType = ParkingType.PRIVATE_DRIVEWAY,
          location =
            ParkingLocation(
              city = SupportedCity.ISLAMABAD,
              areaLabel = "F-7 Markaz",
              publicMaskedLatitude = 33.7200,
              publicMaskedLongitude = 73.0600,
            ),
          hourlyRatePkr = 150,
          dailyRatePkr = 1200,
          monthlyRatePkr = null,
          monthlyEnabled = false,
          totalBays = 1,
          availableBays = 1,
          supportedVehicles = setOf(VehicleType.CAR),
          amenities = setOf(ParkingAmenity.CCTV),
          schedule =
            AvailabilitySchedule(
              openDays = ParkingDayOfWeek.entries.toSet(),
              openHour = 0,
              closeHour = 0,
              is24Hours = true,
            ),
          isApproved = false,
          isDemoListing = true,
        )

      // Attempt to publish immediately while PENDING_REVIEW -> must reject!
      val publishAttempt = parkingRepo.createOrUpdateListing(hostUser.id, space, publishImmediately = true)
      assertTrue("Publishing must be rejected for PENDING_REVIEW host", publishAttempt is HostListingMutationResult.Rejected)
      val reason = (publishAttempt as HostListingMutationResult.Rejected).reason
      assertTrue("Rejection reason must mention APPROVED", reason.contains("APPROVED"))

      // Now simulate Super Admin approval of this Host
      database.userDao().upsertHostProfile(hostProfile!!.copy(verificationStatus = HostVerificationStatus.APPROVED).toEntity())

      // Now attempt publishing -> must succeed!
      val approvedPublish = parkingRepo.createOrUpdateListing(hostUser.id, space, publishImmediately = true)
      assertTrue("Publishing must succeed for APPROVED host", approvedPublish is HostListingMutationResult.Success)

      val liveSpace = parkingRepo.getSpaceById("space-kashif-01")
      assertTrue("Space must now be approved for discovery", liveSpace?.isApproved ?: false)
    }

  @Test
  fun `scenario 3 - host can manage bays, capacity, schedules, and blackout dates with conflict prevention`() =
    runBlocking(Dispatchers.IO) {
      seedInitializer.seedIfNeeded()

      val (hostUser, initialSpace) =
        createApprovedHostWithSpace(
          email = "usman.s3@host.pk",
          fullName = "Usman M4 Host",
          city = SupportedCity.LAHORE,
          spaceId = "space-s3-lhr",
          title = "Usman Gulberg Deck",
          areaLabel = "Gulberg III",
        )
      val hostId = hostUser.id
      val spaceId = initialSpace.id

      // 1. Add a second bay
      val addBayRes =
        parkingRepo.addBayToSpace(
          hostId = hostId,
          spaceId = spaceId,
          bayCode = "Bay LHR-VIP",
          supportedVehicles = setOf(VehicleType.CAR, VehicleType.SUV),
          isCovered = true,
        )
      assertTrue("Add bay must succeed", addBayRes is HostBayMutationResult.Success)

      val baysAfterAdd = parkingRepo.observeBaysForSpace(spaceId).first()
      val vipBay = baysAfterAdd.find { it.bayCode == "Bay LHR-VIP" }
      assertNotNull(vipBay)

      // 2. Create an active booking on Bay LHR-VIP for a registered Customer
      val custUser =
        (authRepo.registerLocalAccount(
          DemoRegisterRequest(
            role = UserRole.CUSTOMER,
            fullName = "Ali S3 Driver",
            email = "ali.s3@parkspace.pk",
            password = "password123",
            confirmPassword = "password123",
            preferredCity = SupportedCity.LAHORE,
          )
        ) as DemoAuthResult.Success).user
      val vehicle = userRepo.addVehicleForUser(custUser.id, VehicleType.CAR, "Civic", "LEA-303", true).getOrThrow()

      val now = System.currentTimeMillis()
      val futureStart =
        LocalDate.now(BookingAvailabilityValidator.PAKISTAN_ZONE)
          .plusDays(4)
          .atTime(11, 0)
          .atZone(BookingAvailabilityValidator.PAKISTAN_ZONE)
          .toInstant()
          .toEpochMilli()
      val endEpochMs = futureStart + 2 * Booking.MILLIS_PER_HOUR

      val activeBooking =
        Booking(
          id = "bkg-vip-active",
          bookingCode = "PS-DEMO-VIP",
          spaceId = spaceId,
          bayId = vipBay!!.id,
          customerId = custUser.id,
          hostId = hostId,
          vehicleId = vehicle.id,
          city = SupportedCity.LAHORE,
          bookingType = BookingType.HOURLY,
          status = BookingStatus.CONFIRMED,
          startEpochMs = futureStart,
          endEpochMs = endEpochMs,
          baseAmountPkr = 300,
          serviceFeePkr = 24,
          totalAmountPkr = 324,
          gatePinReleased = true,
          isDemoRecord = true,
          createdAtEpochMs = now,
        )
      database.bookingDao().insertBooking(activeBooking.toEntity())

      // 3. Attempt to deactivate Bay LHR-VIP -> must be rejected due to active booking!
      val deactAttempt = parkingRepo.toggleBayActive(hostId, spaceId, vipBay.id, isActive = false, nowEpochMs = now)
      assertTrue("Deactivating bay with active booking must be rejected", deactAttempt is HostBayMutationResult.Rejected)
      val deactReason = (deactAttempt as HostBayMutationResult.Rejected).reason
      assertTrue("Reason must mention active booking", deactReason.contains("active") || deactReason.contains("PS-DEMO-VIP"))

      // 4. Attempt to add a blackout date on the booked date -> must be rejected!
      val bookedDateIso =
        Instant.ofEpochMilli(futureStart)
          .atZone(BookingAvailabilityValidator.PAKISTAN_ZONE)
          .toLocalDate()
          .format(DateTimeFormatter.ISO_LOCAL_DATE)

      val conflictingSchedule =
        initialSpace.schedule.copy(
          blackoutDatesIso = setOf(bookedDateIso),
        )
      val scheduleAttempt = parkingRepo.updateListingScheduleAndBlackouts(hostId, spaceId, conflictingSchedule, nowEpochMs = now)
      assertTrue("Adding blackout date over active booking must be rejected", scheduleAttempt is HostScheduleMutationResult.Rejected)
      val schedReason = (scheduleAttempt as HostScheduleMutationResult.Rejected).reason
      assertTrue("Reason must mention confirmed booking", schedReason.contains("PS-DEMO-VIP") || schedReason.contains(bookedDateIso))

      // 5. Add a blackout on a non-conflicting future date -> must succeed!
      val nonConflictingDate =
        LocalDate.now(BookingAvailabilityValidator.PAKISTAN_ZONE)
          .plusDays(15)
          .format(DateTimeFormatter.ISO_LOCAL_DATE)
      val validSchedule =
        initialSpace.schedule.copy(
          blackoutDatesIso = setOf(nonConflictingDate),
        )
      val validSchedRes = parkingRepo.updateListingScheduleAndBlackouts(hostId, spaceId, validSchedule, nowEpochMs = now)
      assertTrue("Valid blackout schedule must succeed", validSchedRes is HostScheduleMutationResult.Success)

      val reloadedSpace = parkingRepo.getSpaceById(spaceId)
      assertTrue(reloadedSpace!!.schedule.blackoutDatesIso.contains(nonConflictingDate))
    }

  @Test
  fun `scenario 4 - host sees only bookings belonging to their own listings and enforces host actions`() =
    runBlocking(Dispatchers.IO) {
      seedInitializer.seedIfNeeded()

      val (host1, space1) =
        createApprovedHostWithSpace(
          email = "host1.s4@host.pk",
          fullName = "Host One Lahore",
          city = SupportedCity.LAHORE,
          spaceId = "space-s4-h1",
          title = "Host 1 Garage",
          areaLabel = "Model Town",
        )
      val (host2, space2) =
        createApprovedHostWithSpace(
          email = "host2.s4@host.pk",
          fullName = "Host Two Islamabad",
          city = SupportedCity.ISLAMABAD,
          spaceId = "space-s4-h2",
          title = "Host 2 Garage",
          areaLabel = "F-7 Markaz",
        )
      val host1Id = host1.id
      val host2Id = host2.id

      val custUser =
        (authRepo.registerLocalAccount(
          DemoRegisterRequest(
            role = UserRole.CUSTOMER,
            fullName = "Bilal S4 Driver",
            email = "bilal.s4@parkspace.pk",
            password = "password123",
            confirmPassword = "password123",
            preferredCity = SupportedCity.LAHORE,
          )
        ) as DemoAuthResult.Success).user
      val vehicle = userRepo.addVehicleForUser(custUser.id, VehicleType.CAR, "Swift", "LEG-404", true).getOrThrow()

      val now = System.currentTimeMillis()
      val startEpoch =
        LocalDate.now(BookingAvailabilityValidator.PAKISTAN_ZONE)
          .plusDays(3)
          .atTime(10, 0)
          .atZone(BookingAvailabilityValidator.PAKISTAN_ZONE)
          .toInstant()
          .toEpochMilli()

      val host1BookingRes =
        bookingRepo.createBookingWithAvailabilityCheck(
          BookingCreateRequest(
            spaceId = space1.id,
            customerId = custUser.id,
            vehicleId = vehicle.id,
            bookingType = BookingType.HOURLY,
            units = 2,
            startEpochMs = startEpoch,
            confirmDemoPaymentImmediately = true,
          )
        )
      assertTrue("Host 1 booking creation must succeed", host1BookingRes is BookingMutationResult.Success)

      val host2BookingRes =
        bookingRepo.createBookingWithAvailabilityCheck(
          BookingCreateRequest(
            spaceId = space2.id,
            customerId = custUser.id,
            vehicleId = vehicle.id,
            bookingType = BookingType.HOURLY,
            units = 2,
            startEpochMs = startEpoch,
            confirmDemoPaymentImmediately = true,
          )
        )
      assertTrue("Host 2 booking creation must succeed", host2BookingRes is BookingMutationResult.Success)
      val host2Booking = (host2BookingRes as BookingMutationResult.Success).booking

      val host1Bookings = bookingRepo.observeBookingsForHost(host1Id).first()
      assertEquals("Host 1 must see exactly their own booking", 1, host1Bookings.size)
      assertTrue("All bookings must belong to Host 1", host1Bookings.all { it.hostId == host1Id })
      assertTrue("No Host 2 bookings in Host 1 query", host1Bookings.none { it.hostId == host2Id })

      // Host 1 attempts to check in Host 2's booking -> Rejected!
      val checkInAttempt = bookingRepo.hostCheckInBooking(host2Booking.id, host1Id, nowEpochMs = now)
      assertTrue("Host 1 cannot check in Host 2 booking", checkInAttempt is BookingMutationResult.Rejected)

      // Host 2 checks in their own booking -> Success!
      val h2CheckIn = bookingRepo.hostCheckInBooking(host2Booking.id, host2Id, nowEpochMs = now)
      assertTrue("Host 2 check in must succeed", h2CheckIn is BookingMutationResult.Success)

      val reloaded = bookingRepo.getBookingById(host2Booking.id)
      assertEquals(BookingStatus.CHECKED_IN, reloaded?.status)

      // Host 2 records check-out
      val h2CheckOut = bookingRepo.hostCheckOutBooking(host2Booking.id, host2Id, nowEpochMs = now + 1000)
      assertTrue("Host 2 check out must succeed", h2CheckOut is BookingMutationResult.Success)

      val checkedOut = bookingRepo.getBookingById(host2Booking.id)
      assertEquals(BookingStatus.CHECKED_OUT, checkedOut?.status)
      assertEquals(DemoPaymentStatus.DEMO_SETTLED, checkedOut?.demoPaymentStatus)
    }

  @Test
  fun `scenario 5 - demo ledger values are correctly calculated from local booking records`() =
    runBlocking(Dispatchers.IO) {
      val hostId = "host-ledger-test"
      val space1 =
        ParkingSpace(
          id = "space-led-1",
          hostId = hostId,
          title = "Ledger Test Space",
          description = "Desc",
          parkingType = ParkingType.COVERED_GARAGE,
          location = ParkingLocation(SupportedCity.LAHORE, "Gulberg", 31.52, 74.35),
          hourlyRatePkr = 100,
          dailyRatePkr = 800,
          monthlyRatePkr = null,
          monthlyEnabled = false,
          totalBays = 2,
          availableBays = 2,
          supportedVehicles = setOf(VehicleType.CAR),
          amenities = setOf(ParkingAmenity.COVERED),
          schedule = AvailabilitySchedule(ParkingDayOfWeek.entries.toSet(), 0, 0, true),
          isApproved = true,
          isDemoListing = true,
        )

      val now = 1731750000000L
      // Booking A: Confirmed (Escrow held) - exactly 1 hour
      val bkgA =
        Booking(
          id = "bkg-a",
          bookingCode = "PS-A",
          spaceId = "space-led-1",
          bayId = "bay-1",
          customerId = "cust-1",
          hostId = hostId,
          vehicleId = "veh-1",
          city = SupportedCity.LAHORE,
          bookingType = BookingType.HOURLY,
          status = BookingStatus.CONFIRMED,
          startEpochMs = now,
          endEpochMs = now + Booking.MILLIS_PER_HOUR,
          baseAmountPkr = 1000,
          serviceFeePkr = 80,
          totalAmountPkr = 1080,
          gatePinReleased = true,
          isDemoRecord = true,
          createdAtEpochMs = now,
        )

      // Booking B: Checked Out (Settled) - exactly 1 full 24-hr day
      val bkgB =
        Booking(
          id = "bkg-b",
          bookingCode = "PS-B",
          spaceId = "space-led-1",
          bayId = "bay-2",
          customerId = "cust-2",
          hostId = hostId,
          vehicleId = "veh-2",
          city = SupportedCity.LAHORE,
          bookingType = BookingType.DAILY,
          status = BookingStatus.CHECKED_OUT,
          startEpochMs = now - Booking.MILLIS_PER_DAY,
          endEpochMs = now,
          baseAmountPkr = 2000,
          serviceFeePkr = 160,
          totalAmountPkr = 2160,
          gatePinReleased = true,
          isDemoRecord = true,
          createdAtEpochMs = now - Booking.MILLIS_PER_DAY,
        )

      val ledger = HostLedgerCalculator.compute(listOf(bkgA, bkgB), listOf(space1))

      assertEquals("Gross customer volume must be 1080 + 2160 = 3240", 3240, ledger.totalLifetimeGrossPkr)
      assertEquals("Platform fee (8%) must be 80 + 160 = 240", 240, ledger.totalPlatformCommissionPkr)
      assertEquals("Host net parking share must be 1000 + 2000 = 3000", 3000, ledger.totalHostNetEarningsPkr)
      assertEquals("Escrow held must be 1000 (Booking A)", 1000, ledger.escrowHeldPkr)
      assertEquals("Settled must be 2000 (Booking B)", 2000, ledger.settledPkr)
      assertEquals("Total bookings count must be 2", 2, ledger.totalBookingsCount)
      assertEquals("Transactions list size must be 2", 2, ledger.transactions.size)
    }

  @Test
  fun `scenario 6 - account switching and HostDashboardScreen do not expose another host data`() {
    runBlocking(Dispatchers.IO) {
      seedInitializer.seedIfNeeded()

      // Register Host Alpha
      val regA =
        authRepo.registerLocalAccount(
          DemoRegisterRequest(
            role = UserRole.HOST,
            fullName = "Alpha Host",
            email = "alpha@host.pk",
            password = "password123",
            confirmPassword = "password123",
            preferredCity = SupportedCity.LAHORE,
            businessAlias = "Alpha Parking Group",
          )
        )
      val userA = (regA as DemoAuthResult.Success).user

      // Register Host Beta
      val regB =
        authRepo.registerLocalAccount(
          DemoRegisterRequest(
            role = UserRole.HOST,
            fullName = "Beta Host",
            email = "beta@host.pk",
            password = "password123",
            confirmPassword = "password123",
            preferredCity = SupportedCity.KARACHI,
            businessAlias = "Beta Parking Solutions",
          )
        )
      val userB = (regB as DemoAuthResult.Success).user

      // Create space and booking for Host Alpha
      val spaceA =
        ParkingSpace(
          id = "space-alpha-01",
          hostId = userA.id,
          title = "Alpha Private Plaza",
          description = "Alpha plaza bay",
          parkingType = ParkingType.COMMERCIAL_PLAZA,
          location = ParkingLocation(SupportedCity.LAHORE, "Gulberg", 31.52, 74.35),
          hourlyRatePkr = 100,
          dailyRatePkr = 800,
          monthlyRatePkr = null,
          monthlyEnabled = false,
          totalBays = 1,
          availableBays = 1,
          supportedVehicles = setOf(VehicleType.CAR),
          amenities = setOf(ParkingAmenity.CCTV),
          schedule = AvailabilitySchedule(ParkingDayOfWeek.entries.toSet(), 0, 0, true),
          isApproved = false,
          isDemoListing = true,
        )
      parkingRepo.createOrUpdateListing(userA.id, spaceA, publishImmediately = false)

      val now = System.currentTimeMillis()
      val alphaBooking =
        Booking(
          id = "bkg-alpha-01",
          bookingCode = "PS-ALPHA-01",
          spaceId = spaceA.id,
          bayId = "${spaceA.id}-bay-1",
          customerId = "demo-cust-01",
          hostId = userA.id,
          vehicleId = "demo-veh-01",
          city = SupportedCity.LAHORE,
          bookingType = BookingType.HOURLY,
          status = BookingStatus.CONFIRMED,
          startEpochMs = now + Booking.MILLIS_PER_HOUR,
          endEpochMs = now + 2 * Booking.MILLIS_PER_HOUR,
          baseAmountPkr = 100,
          serviceFeePkr = 8,
          totalAmountPkr = 108,
          gatePinReleased = true,
          isDemoRecord = true,
          createdAtEpochMs = now,
        )
      database.bookingDao().insertBooking(alphaBooking.toEntity())

      // Sign in as Host Alpha
      sessionManager.activateAuthenticatedUser(userA)
    }

    val viewModel = createHostDashboardViewModel()

    composeRule.setContent {
      ParkSpaceTheme(darkTheme = false) {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        HostDashboardScreen(
          uiState = state,
          viewModel = viewModel,
          onBack = {},
          onOpenSettings = {},
        )
      }
    }

    composeRule.waitUntil(5_000L) {
      ShadowLooper.idleMainLooper()
      viewModel.uiState.value.activeUser?.fullName == "Alpha Host" &&
        viewModel.uiState.value.spaces.size == 1 &&
        viewModel.uiState.value.bookings.size == 1
    }

    // Verify Host Alpha data displayed
    composeRule.onNodeWithTag("host_dashboard_screen").assertIsDisplayed()
    composeRule.onNodeWithTag("host_verification_card").assertIsDisplayed()
    composeRule.onNodeWithTag("host_kpi_total_spaces").assertIsDisplayed()
    assertEquals("Alpha Host must see 1 space", "space-alpha-01", viewModel.uiState.value.spaces.single().id)
    assertEquals("Alpha Host must see 1 booking", "bkg-alpha-01", viewModel.uiState.value.bookings.single().id)

    // Switch session to Host Beta
    runBlocking(Dispatchers.IO) {
      val userB = userRepo.observeUsersByRole(UserRole.HOST).first().find { it.fullName == "Beta Host" }!!
      sessionManager.activateAuthenticatedUser(userB)
    }

    composeRule.waitUntil(5_000L) {
      ShadowLooper.idleMainLooper()
      viewModel.uiState.value.activeUser?.fullName == "Beta Host"
    }
    composeRule.waitForIdle()

    // Host Beta has 0 spaces and 0 bookings -> Alpha's spaces and bookings must not be exposed!
    assertEquals("Beta Host must have 0 spaces", 0, viewModel.uiState.value.spaces.size)
    assertNull("Beta Host must not see Alpha space", viewModel.uiState.value.spaces.find { it.id == "space-alpha-01" })
    assertEquals("Beta Host must have 0 bookings", 0, viewModel.uiState.value.bookings.size)
    assertNull("Beta Host must not see Alpha booking", viewModel.uiState.value.bookings.find { it.id == "bkg-alpha-01" })
  }

  @Test
  fun `scenario 7 - existing host listing cannot be overwritten by another host using same listing id`() =
    runBlocking(Dispatchers.IO) {
      seedInitializer.seedIfNeeded()

      val regA =
        authRepo.registerLocalAccount(
          DemoRegisterRequest(
            role = UserRole.HOST,
            fullName = "Owner Host",
            email = "owner@host.pk",
            password = "password123",
            confirmPassword = "password123",
            preferredCity = SupportedCity.LAHORE,
            businessAlias = "Owner Parking",
          )
        )
      val hostA = (regA as DemoAuthResult.Success).user

      val regB =
        authRepo.registerLocalAccount(
          DemoRegisterRequest(
            role = UserRole.HOST,
            fullName = "Attacker Host",
            email = "attacker@host.pk",
            password = "password123",
            confirmPassword = "password123",
            preferredCity = SupportedCity.LAHORE,
            businessAlias = "Attacker Parking",
          )
        )
      val hostB = (regB as DemoAuthResult.Success).user

      val originalSpace =
        ParkingSpace(
          id = "space-protected-01",
          hostId = hostA.id,
          title = "Host A Protected Plaza",
          description = "Owned exclusively by Host A.",
          parkingType = ParkingType.COVERED_GARAGE,
          location = ParkingLocation(SupportedCity.LAHORE, "DHA Phase 5", 31.46, 74.40),
          hourlyRatePkr = 180,
          dailyRatePkr = 1400,
          monthlyRatePkr = null,
          monthlyEnabled = false,
          totalBays = 1,
          availableBays = 1,
          supportedVehicles = setOf(VehicleType.CAR),
          amenities = setOf(ParkingAmenity.CCTV),
          schedule = AvailabilitySchedule(ParkingDayOfWeek.entries.toSet(), 0, 0, true),
          isApproved = false,
          isDemoListing = true,
        )
      val createRes = parkingRepo.createOrUpdateListing(hostA.id, originalSpace, publishImmediately = false)
      assertTrue(createRes is HostListingMutationResult.Success)

      // Host B tries to overwrite Host A's listing ID while claiming hostId = hostB.id
      val forgedOverwrite =
        originalSpace.copy(
          hostId = hostB.id,
          title = "Hijacked By Host B",
          hourlyRatePkr = 50,
        )
      val hijackRes = parkingRepo.createOrUpdateListing(hostB.id, forgedOverwrite, publishImmediately = false)
      assertTrue("Overwriting another host's existing listing ID must be rejected", hijackRes is HostListingMutationResult.Rejected)

      val reloaded = parkingRepo.getSpaceById("space-protected-01")
      assertNotNull(reloaded)
      assertEquals("Listing owner must remain Host A", hostA.id, reloaded?.hostId)
      assertEquals("Listing title must remain unchanged", "Host A Protected Plaza", reloaded?.title)
      assertEquals("Listing rate must remain unchanged", 180, reloaded?.hourlyRatePkr)
    }

  @Test
  fun `scenario 8 - foreign or mismatched parking bay ownership is rejected across bay mutations`() =
    runBlocking(Dispatchers.IO) {
      seedInitializer.seedIfNeeded()

      val (hostA, spaceA) =
        createApprovedHostWithSpace(
          email = "hosta.s8@host.pk",
          fullName = "Host A Bay Owner",
          city = SupportedCity.LAHORE,
          spaceId = "space-s8-a",
          title = "Host A Garage",
          areaLabel = "Gulberg III",
        )
      val (hostB, spaceB) =
        createApprovedHostWithSpace(
          email = "hostb.s8@host.pk",
          fullName = "Host B Foreign",
          city = SupportedCity.ISLAMABAD,
          spaceId = "space-s8-b",
          title = "Host B Garage",
          areaLabel = "Blue Area",
        )
      val hostAId = hostA.id
      val spaceAId = spaceA.id
      val hostBId = hostB.id
      val spaceBId = spaceB.id

      val bayA = parkingRepo.observeBaysForSpace(spaceAId).first().first()
      val bayB = parkingRepo.observeBaysForSpace(spaceBId).first().first()

      // 1. Foreign host cannot toggle Host A's bay
      val foreignToggle = parkingRepo.toggleBayActive(hostBId, spaceAId, bayA.id, isActive = false)
      assertTrue("Foreign host toggleBayActive must be rejected", foreignToggle is HostBayMutationResult.Rejected)

      // 2. Host A cannot toggle a bay belonging to Space B under Space A's spaceId
      val mismatchedToggle = parkingRepo.toggleBayActive(hostAId, spaceAId, bayB.id, isActive = false)
      assertTrue("Mismatched bay spaceId in toggleBayActive must be rejected", mismatchedToggle is HostBayMutationResult.Rejected)
      assertTrue("Host B's bay must remain active", parkingRepo.observeBaysForSpace(spaceBId).first().first { it.id == bayB.id }.isActiveInDemo)

      // 3. Foreign host cannot update Host A's bay
      val foreignUpdate = parkingRepo.updateBay(hostBId, spaceAId, bayA.copy(bayCode = "HACKED"))
      assertTrue("Foreign host updateBay must be rejected", foreignUpdate is HostBayMutationResult.Rejected)

      // 4. Host A cannot pass mismatched bay.spaceId or foreign bay.id into updateBay
      val mismatchedPayloadUpdate = parkingRepo.updateBay(hostAId, spaceAId, bayA.copy(spaceId = spaceBId, bayCode = "X1"))
      assertTrue("Mismatched bay.spaceId in updateBay must be rejected", mismatchedPayloadUpdate is HostBayMutationResult.Rejected)

      val crossSpaceBayUpdate = parkingRepo.updateBay(hostAId, spaceAId, bayB.copy(spaceId = spaceAId, bayCode = "X2"))
      assertTrue("Foreign bay.id in updateBay must be rejected", crossSpaceBayUpdate is HostBayMutationResult.Rejected)
    }

  @Test
  fun `scenario 9 - host can add, edit, and deactivate own bays via HostBaysManagerDialog`() {
    val (spaceId, initialSpace) =
      runBlocking(Dispatchers.IO) {
        seedInitializer.seedIfNeeded()
        val (hostUser, space) =
          createApprovedHostWithSpace(
            email = "host.s9@host.pk",
            fullName = "Sanaullah S9 Host",
            city = SupportedCity.LAHORE,
            spaceId = "space-s9-lhr",
            title = "Sanaullah Bay Plaza",
            areaLabel = "Johar Town",
          )
        sessionManager.activateAuthenticatedUser(hostUser)
        space.id to space
      }

    val viewModel = createHostDashboardViewModel()
    viewModel.openBaysManager(initialSpace)

    composeRule.setContent {
      ParkSpaceTheme(darkTheme = false) {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val selectedSpace = state.selectedSpaceForBays
        if (selectedSpace != null) {
          HostBaysManagerDialog(
            space = selectedSpace,
            bays = state.baysForSelectedSpace,
            onDismiss = viewModel::dismissBaysManager,
            onAddBay = viewModel::addBay,
            onUpdateBay = viewModel::updateBay,
            onToggleBayActive = viewModel::toggleBayActive,
            errorMessage = state.errorMessage,
          )
        }
      }
    }

    composeRule.waitUntil(5_000L) {
      ShadowLooper.idleMainLooper()
      viewModel.uiState.value.baysForSelectedSpace.isNotEmpty()
    }

    // 1. Add a new bay via UI
    composeRule.onNodeWithTag("host_open_add_bay_form_button").performScrollTo().performClick()
    composeRule.onNodeWithTag("host_new_bay_code_input").performScrollTo().performTextInput("Bay C-01")
    composeRule.onNodeWithTag("host_submit_add_bay_button").performScrollTo().performClick()

    composeRule.waitUntil(5_000L) {
      ShadowLooper.idleMainLooper()
      viewModel.uiState.value.baysForSelectedSpace.any { it.bayCode == "Bay C-01" }
    }

    val addedBay = viewModel.uiState.value.baysForSelectedSpace.first { it.bayCode == "Bay C-01" }

    // 2. Edit the newly added bay via UI (rename and enable Bike support)
    composeRule.onNodeWithTag("host_bay_edit_button_${addedBay.id}").performScrollTo().performClick()
    composeRule.onNodeWithTag("host_edit_bay_form").assertIsDisplayed()
    composeRule.onNodeWithTag("host_edit_bay_code_input").performScrollTo().performTextReplacement("Bay C-VIP")
    composeRule.onNodeWithTag("host_edit_bay_vehicle_chip_BIKE").performScrollTo().performClick()
    composeRule.onNodeWithTag("host_submit_edit_bay_button").performScrollTo().performClick()

    composeRule.waitUntil(5_000L) {
      ShadowLooper.idleMainLooper()
      val updated = viewModel.uiState.value.baysForSelectedSpace.find { it.id == addedBay.id }
      updated?.bayCode == "Bay C-VIP" && VehicleType.BIKE in (updated.supportedVehicles)
    }

    composeRule.onNodeWithText("Bay C-VIP").assertIsDisplayed()

    // 3. Deactivate the bay via UI toggle
    composeRule.onNodeWithTag("host_bay_active_toggle_${addedBay.id}").performScrollTo().performClick()

    composeRule.waitUntil(5_000L) {
      ShadowLooper.idleMainLooper()
      viewModel.uiState.value.baysForSelectedSpace.find { it.id == addedBay.id }?.isActiveInDemo == false
    }

    val persistedBay = runBlocking(Dispatchers.IO) {
      parkingRepo.observeBaysForSpace(spaceId).first().first { it.id == addedBay.id }
    }
    assertEquals("Bay C-VIP", persistedBay.bayCode)
    assertTrue(VehicleType.BIKE in persistedBay.supportedVehicles)
    assertFalse("Bay should be deactivated after toggle", persistedBay.isActiveInDemo)
  }

  @Test
  fun `scenario 10 - room MIGRATION_3_4 migrates v3 database preserving records and photoTags default`() {
    val dbName = "parkspace_migration_3_4_test.db"
    context.deleteDatabase(dbName)

    val v3Callback =
      object : SupportSQLiteOpenHelper.Callback(3) {
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
            """
            CREATE TABLE IF NOT EXISTS `demo_parking_spaces` (
              `id` TEXT NOT NULL,
              `hostId` TEXT NOT NULL,
              `title` TEXT NOT NULL,
              `description` TEXT NOT NULL,
              `parkingType` TEXT NOT NULL,
              `city` TEXT NOT NULL,
              `areaLabel` TEXT NOT NULL,
              `publicMaskedLatitude` REAL NOT NULL,
              `publicMaskedLongitude` REAL NOT NULL,
              `maskRadiusMeters` INTEGER NOT NULL,
              `privateGateSummary` TEXT,
              `privateExactLatitude` REAL,
              `privateExactLongitude` REAL,
              `hourlyRatePkr` INTEGER NOT NULL,
              `dailyRatePkr` INTEGER NOT NULL,
              `monthlyRatePkr` INTEGER,
              `monthlyEnabled` INTEGER NOT NULL,
              `totalBays` INTEGER NOT NULL,
              `availableBays` INTEGER NOT NULL,
              `supportedVehicles` TEXT NOT NULL,
              `amenities` TEXT NOT NULL,
              `openDays` TEXT NOT NULL,
              `openHour` INTEGER NOT NULL,
              `closeHour` INTEGER NOT NULL,
              `is24Hours` INTEGER NOT NULL,
              `blackoutDatesIso` TEXT NOT NULL,
              `isApproved` INTEGER NOT NULL,
              `isDemoListing` INTEGER NOT NULL,
              PRIMARY KEY(`id`)
            )
            """.trimIndent()
          )
          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `demo_parking_bays` (`id` TEXT NOT NULL, `spaceId` TEXT NOT NULL, `bayCode` TEXT NOT NULL, `supportedVehicles` TEXT NOT NULL, `isCovered` INTEGER NOT NULL, `isActiveInDemo` INTEGER NOT NULL, PRIMARY KEY(`id`))"
          )
          db.execSQL(
            "CREATE TABLE IF NOT EXISTS `demo_bookings` (`id` TEXT NOT NULL, `bookingCode` TEXT NOT NULL, `spaceId` TEXT NOT NULL, `bayId` TEXT NOT NULL, `customerId` TEXT NOT NULL, `hostId` TEXT NOT NULL, `vehicleId` TEXT NOT NULL, `city` TEXT NOT NULL, `bookingType` TEXT NOT NULL, `status` TEXT NOT NULL, `startEpochMs` INTEGER NOT NULL, `endEpochMs` INTEGER NOT NULL, `baseAmountPkr` INTEGER NOT NULL, `serviceFeePkr` INTEGER NOT NULL, `totalAmountPkr` INTEGER NOT NULL, `gatePinReleased` INTEGER NOT NULL, `isDemoRecord` INTEGER NOT NULL, `demoPaymentStatus` TEXT NOT NULL, `demoEntryPin` TEXT, `checkedInAtEpochMs` INTEGER, `checkedOutAtEpochMs` INTEGER, `cancelledAtEpochMs` INTEGER, `refundAmountPkr` INTEGER, `cancellationPolicySummary` TEXT, `createdAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`id`))"
          )
          db.execSQL(
            """
            INSERT INTO demo_parking_spaces (
              id, hostId, title, description, parkingType, city, areaLabel,
              publicMaskedLatitude, publicMaskedLongitude, maskRadiusMeters,
              privateGateSummary, privateExactLatitude, privateExactLongitude,
              hourlyRatePkr, dailyRatePkr, monthlyRatePkr, monthlyEnabled,
              totalBays, availableBays, supportedVehicles, amenities, openDays,
              openHour, closeHour, is24Hours, blackoutDatesIso, isApproved, isDemoListing
            ) VALUES (
              'v3-space-1', 'demo-host-lhr', 'Legacy V3 Gulberg Garage', 'Preserved across migration',
              'COVERED_GARAGE', 'LAHORE', 'Gulberg II',
              31.5204, 74.3587, 200,
              'Guard Gate A', 31.5208, 74.3591,
              150, 1200, 18000, 1,
              2, 2, 'CAR|SUV', 'CCTV|COVERED', 'MON|TUE|WED|THU|FRI|SAT|SUN',
              0, 0, 1, '', 1, 1
            )
            """.trimIndent()
          )
        }

        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
      }

    val v3Helper =
      FrameworkSQLiteOpenHelperFactory().create(
        SupportSQLiteOpenHelper.Configuration.builder(context)
          .name(dbName)
          .callback(v3Callback)
          .build()
      )
    v3Helper.writableDatabase.close()
    v3Helper.close()

    val migratedDb =
      Room.databaseBuilder(context, ParkSpaceDemoDatabase::class.java, dbName)
        .addMigrations(*ParkSpaceDemoDatabase.MIGRATIONS)
        .allowMainThreadQueries()
        .build()

    try {
      val migratedSpace = runBlocking { migratedDb.parkingDao().getSpaceById("v3-space-1") }
      assertNotNull("Existing v3 space must be preserved after v3->v4 migration", migratedSpace)
      assertEquals("Legacy V3 Gulberg Garage", migratedSpace?.title)
      assertEquals("demo-host-lhr", migratedSpace?.hostId)
      assertEquals(150, migratedSpace?.hourlyRatePkr)
      assertEquals(
        "Default photoTags must be populated by MIGRATION_3_4",
        listOf("Entrance Gate", "Covered Bay"),
        migratedSpace?.photoTags,
      )
    } finally {
      migratedDb.close()
      context.deleteDatabase(dbName)
    }
  }
}
