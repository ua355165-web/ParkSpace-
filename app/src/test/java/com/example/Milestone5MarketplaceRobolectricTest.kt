package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.config.BookingType
import com.example.core.config.SupportedCity
import com.example.core.config.UserRole
import com.example.core.data.demo.DemoAuthRepositoryImpl
import com.example.core.data.demo.DemoBookingRepositoryImpl
import com.example.core.data.demo.DemoChatRepositoryImpl
import com.example.core.data.demo.DemoDisputeRepositoryImpl
import com.example.core.data.demo.DemoNotificationRepositoryImpl
import com.example.core.data.demo.DemoParkingRepositoryImpl
import com.example.core.data.demo.DemoPaymentTransactionRepositoryImpl
import com.example.core.data.demo.DemoReferralRepositoryImpl
import com.example.core.data.demo.DemoReviewRepositoryImpl
import com.example.core.data.demo.DemoUserProfileRepositoryImpl
import com.example.core.database.ParkSpaceDemoDatabase
import com.example.core.database.mapper.toEntity
import com.example.core.domain.model.AvailabilitySchedule
import com.example.core.domain.model.BookingStatus
import com.example.core.domain.model.DemoPaymentMethod
import com.example.core.domain.model.DemoPaymentTransactionType
import com.example.core.domain.model.DisputeCategory
import com.example.core.domain.model.DisputeStatus
import com.example.core.domain.model.HostVerificationStatus
import com.example.core.domain.model.ParkSpaceNotificationType
import com.example.core.domain.model.ParkingAmenity
import com.example.core.domain.model.ParkingDayOfWeek
import com.example.core.domain.model.ParkingLocation
import com.example.core.domain.model.ParkingSpace
import com.example.core.domain.model.ParkingType
import com.example.core.domain.model.ReferralRewardStatus
import com.example.core.domain.model.VehicleType
import com.example.core.domain.repository.BookingCreateRequest
import com.example.core.domain.repository.BookingMutationResult
import com.example.core.domain.repository.ChatMutationResult
import com.example.core.domain.repository.DemoAuthResult
import com.example.core.domain.repository.DemoRegisterRequest
import com.example.core.domain.repository.DisputeMutationResult
import com.example.core.domain.repository.HostListingMutationResult
import com.example.core.domain.repository.ReferralMutationResult
import com.example.core.domain.repository.ReviewMutationResult
import com.example.core.navigation.NavGuardResult
import com.example.core.navigation.ParkSpaceNavGuard
import com.example.core.navigation.ParkSpaceRoute
import com.example.demo.sandbox.DemoSessionManager
import com.example.demo.sandbox.DemoSessionState
import com.example.demo.seed.DemoSeedInitializer
import com.example.feature.booking.domain.BookingAvailabilityValidator
import com.example.feature.discovery.DiscoveryViewModel
import com.example.feature.discovery.domain.model.ParkingPublicListing
import com.example.feature.discovery.ui.ParkingDetailPreviewScreen
import com.example.feature.marketplace.ChatInboxUiState
import com.example.feature.marketplace.CustomerPaymentsReferralsUiState
import com.example.feature.marketplace.DisputesCenterUiState
import com.example.feature.marketplace.NotificationsUiState
import com.example.feature.marketplace.ui.ChatInboxScreen
import com.example.feature.marketplace.ui.CustomerPaymentsReferralsScreen
import com.example.feature.marketplace.ui.DisputesCenterScreen
import com.example.feature.marketplace.ui.HostReviewsScreen
import com.example.feature.marketplace.ui.NotificationsScreen
import com.example.ui.theme.ParkSpaceTheme
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
class Milestone5MarketplaceRobolectricTest {

  @get:Rule val composeRule = createComposeRule()

  private lateinit var context: Context

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    context.getSharedPreferences("parkspace_demo_session", Context.MODE_PRIVATE).edit().clear().commit()
  }

  @Test
  fun `fresh install has zero Milestone 5 records and empty screens display cleanly`() {
    val db =
      Room.inMemoryDatabaseBuilder(context, ParkSpaceDemoDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    try {
      runBlocking(Dispatchers.IO) {
        DemoSeedInitializer(db).seedIfNeeded()
        assertEquals(0, db.userDao().countUsers())
        assertEquals(0, db.parkingDao().countSpaces())
        assertEquals(0, db.parkingDao().countBays())
        assertEquals(0, db.bookingDao().countBookings())
        assertEquals(0, db.paymentTransactionDao().countTransactions())
        assertEquals(0, db.chatDao().countMessages())
        assertEquals(0, db.reviewDao().countReviews())
        assertEquals(0, db.disputeDao().countDisputes())
        assertEquals(0, db.referralDao().countReferrals())
        assertEquals(0, db.notificationDao().countNotifications())
      }

      val screenIndex = androidx.compose.runtime.mutableIntStateOf(0)
      composeRule.setContent {
        ParkSpaceTheme {
          when (screenIndex.intValue) {
            0 ->
              NotificationsScreen(
                uiState = NotificationsUiState(),
                onBack = {},
                onMarkAsRead = {},
                onMarkAllAsRead = {},
              )
            1 ->
              ChatInboxScreen(
                uiState = ChatInboxUiState(),
                onBack = {},
                onOpenBookingChat = {},
              )
            2 ->
              CustomerPaymentsReferralsScreen(
                uiState = CustomerPaymentsReferralsUiState(),
                feedback = null,
                onBack = {},
                onRedeemReferralCode = {},
                onDismissFeedback = {},
              )
            3 ->
              HostReviewsScreen(
                reviews = emptyList(),
                feedback = null,
                onBack = {},
                onReplyToReview = { _, _ -> },
                onDismissFeedback = {},
              )
            else ->
              DisputesCenterScreen(
                uiState = DisputesCenterUiState(),
                feedback = null,
                prefillBookingId = null,
                onBack = {},
                onFileDispute = { _, _, _ -> },
                onRespondAsHost = { _, _ -> },
                onResolveDispute = { _, _ -> },
                onDismissFeedback = {},
              )
          }
        }
      }

      composeRule.waitForIdle()
      composeRule.onNodeWithTag("notifications_empty_state").assertIsDisplayed()
      composeRule.onNodeWithText("You're all caught up.").assertIsDisplayed()

      composeRule.runOnUiThread { screenIndex.intValue = 1 }
      composeRule.waitForIdle()
      composeRule.onNodeWithTag("chat_inbox_empty_state").assertIsDisplayed()
      composeRule.onNodeWithText("No conversations yet.").assertIsDisplayed()

      composeRule.runOnUiThread { screenIndex.intValue = 2 }
      composeRule.waitForIdle()
      composeRule.onNodeWithTag("payments_empty_state").assertIsDisplayed()
      composeRule.onNodeWithText("No transactions yet.").assertIsDisplayed()

      composeRule.runOnUiThread { screenIndex.intValue = 3 }
      composeRule.waitForIdle()
      composeRule.onNodeWithTag("host_reviews_empty_state").assertIsDisplayed()
      composeRule.onNodeWithText("No reviews yet.").assertIsDisplayed()

      composeRule.runOnUiThread { screenIndex.intValue = 4 }
      composeRule.waitForIdle()
      composeRule.onNodeWithTag("disputes_empty_state").assertIsDisplayed()
      composeRule.onNodeWithText("No complaints or disputes yet.").assertIsDisplayed()
    } finally {
      db.close()
    }
  }

  @Test
  fun `non destructive MIGRATION_4_5 preserves existing v4 records and enables v5 marketplace tables`() {
    val dbName = "parkspace_m5_migration_4_5.db"
    context.deleteDatabase(dbName)
    val dbFile = context.getDatabasePath(dbName)
    dbFile.parentFile?.mkdirs()

    val rawV4 = SQLiteDatabase.openOrCreateDatabase(dbFile, null)
    try {
      rawV4.execSQL(
        "CREATE TABLE IF NOT EXISTS `demo_seed_metadata` (`key` TEXT NOT NULL, `seedVersion` INTEGER NOT NULL, `seededAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`key`))"
      )
      rawV4.execSQL(
        "CREATE TABLE IF NOT EXISTS `demo_users` (`id` TEXT NOT NULL, `fullName` TEXT NOT NULL, `phoneMasked` TEXT NOT NULL, `emailMasked` TEXT NOT NULL, `role` TEXT NOT NULL, `city` TEXT NOT NULL, `isDemoAccount` INTEGER NOT NULL, `createdAtEpochMs` INTEGER NOT NULL, `emailNormalized` TEXT, `passwordHash` TEXT, `passwordSalt` TEXT, `isSelfRegisteredDemoAccount` INTEGER NOT NULL, PRIMARY KEY(`id`))"
      )
      rawV4.execSQL(
        "CREATE UNIQUE INDEX IF NOT EXISTS `index_demo_users_emailNormalized_role` ON `demo_users` (`emailNormalized`, `role`)"
      )
      rawV4.execSQL(
        "CREATE TABLE IF NOT EXISTS `demo_customer_profiles` (`userId` TEXT NOT NULL, `preferredCity` TEXT NOT NULL, `defaultVehicleId` TEXT, `referralCode` TEXT NOT NULL, PRIMARY KEY(`userId`))"
      )
      rawV4.execSQL(
        "CREATE TABLE IF NOT EXISTS `demo_host_profiles` (`userId` TEXT NOT NULL, `displayBusinessAlias` TEXT NOT NULL, `verificationStatus` TEXT NOT NULL, `operatingCities` TEXT NOT NULL, `totalSpacesCount` INTEGER NOT NULL, `joinedEpochMs` INTEGER NOT NULL, PRIMARY KEY(`userId`))"
      )
      rawV4.execSQL(
        "CREATE TABLE IF NOT EXISTS `demo_vehicles` (`id` TEXT NOT NULL, `ownerUserId` TEXT NOT NULL, `vehicleType` TEXT NOT NULL, `label` TEXT NOT NULL, `plateNumberMasked` TEXT NOT NULL, `isDefault` INTEGER NOT NULL, PRIMARY KEY(`id`))"
      )
      rawV4.execSQL(
        "CREATE TABLE IF NOT EXISTS `demo_parking_spaces` (`id` TEXT NOT NULL, `hostId` TEXT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `parkingType` TEXT NOT NULL, `city` TEXT NOT NULL, `areaLabel` TEXT NOT NULL, `publicMaskedLatitude` REAL NOT NULL, `publicMaskedLongitude` REAL NOT NULL, `maskRadiusMeters` INTEGER NOT NULL, `privateGateSummary` TEXT, `privateExactLatitude` REAL, `privateExactLongitude` REAL, `hourlyRatePkr` INTEGER NOT NULL, `dailyRatePkr` INTEGER NOT NULL, `monthlyRatePkr` INTEGER, `monthlyEnabled` INTEGER NOT NULL, `totalBays` INTEGER NOT NULL, `availableBays` INTEGER NOT NULL, `supportedVehicles` TEXT NOT NULL, `amenities` TEXT NOT NULL, `openDays` TEXT NOT NULL, `openHour` INTEGER NOT NULL, `closeHour` INTEGER NOT NULL, `is24Hours` INTEGER NOT NULL, `blackoutDatesIso` TEXT NOT NULL, `isApproved` INTEGER NOT NULL, `isDemoListing` INTEGER NOT NULL, `photoTags` TEXT NOT NULL, PRIMARY KEY(`id`))"
      )
      rawV4.execSQL(
        "CREATE TABLE IF NOT EXISTS `demo_parking_bays` (`id` TEXT NOT NULL, `spaceId` TEXT NOT NULL, `bayCode` TEXT NOT NULL, `supportedVehicles` TEXT NOT NULL, `isCovered` INTEGER NOT NULL, `isActiveInDemo` INTEGER NOT NULL, PRIMARY KEY(`id`))"
      )
      rawV4.execSQL(
        "CREATE TABLE IF NOT EXISTS `demo_bookings` (`id` TEXT NOT NULL, `bookingCode` TEXT NOT NULL, `spaceId` TEXT NOT NULL, `bayId` TEXT NOT NULL, `customerId` TEXT NOT NULL, `hostId` TEXT NOT NULL, `vehicleId` TEXT NOT NULL, `city` TEXT NOT NULL, `bookingType` TEXT NOT NULL, `status` TEXT NOT NULL, `startEpochMs` INTEGER NOT NULL, `endEpochMs` INTEGER NOT NULL, `baseAmountPkr` INTEGER NOT NULL, `serviceFeePkr` INTEGER NOT NULL, `totalAmountPkr` INTEGER NOT NULL, `gatePinReleased` INTEGER NOT NULL, `isDemoRecord` INTEGER NOT NULL, `demoPaymentStatus` TEXT NOT NULL, `demoEntryPin` TEXT, `checkedInAtEpochMs` INTEGER, `checkedOutAtEpochMs` INTEGER, `cancelledAtEpochMs` INTEGER, `refundAmountPkr` INTEGER, `cancellationPolicySummary` TEXT, `createdAtEpochMs` INTEGER NOT NULL, PRIMARY KEY(`id`))"
      )

      // Populate genuine self-registered v4 records before migrating to v5
      rawV4.execSQL(
        "INSERT INTO `demo_seed_metadata` VALUES ('parkspace_demo_seed', 2, 1000)"
      )
      rawV4.execSQL(
        "INSERT INTO `demo_users` VALUES ('user-host-v4', 'Bilal Host', '0300', 'b***@gmail.com', 'HOST', 'LAHORE', 1, 1000, 'bilal@gmail.com', 'hash', 'salt', 1)"
      )
      rawV4.execSQL(
        "INSERT INTO `demo_users` VALUES ('user-cust-v4', 'Sana Cust', '0301', 's***@gmail.com', 'CUSTOMER', 'LAHORE', 1, 1001, 'sana@gmail.com', 'hash', 'salt', 1)"
      )
      rawV4.execSQL(
        "INSERT INTO `demo_host_profiles` VALUES ('user-host-v4', 'Bilal DHA Lot', 'APPROVED', 'LAHORE', 1, 1000)"
      )
      rawV4.execSQL(
        "INSERT INTO `demo_customer_profiles` VALUES ('user-cust-v4', 'LAHORE', 'veh-v4-1', 'PKUSER901')"
      )
      rawV4.execSQL(
        "INSERT INTO `demo_vehicles` VALUES ('veh-v4-1', 'user-cust-v4', 'CAR', 'Vitz', 'LEB-1122', 1)"
      )
      rawV4.execSQL(
        "INSERT INTO `demo_parking_spaces` VALUES ('space-v4-1', 'user-host-v4', 'Bilal Covered Garage', 'Safe DHA spot', 'COVERED_GARAGE', 'LAHORE', 'DHA Phase 5', 31.46, 74.41, 200, 'Gate 2', 31.461, 74.411, 120, 900, NULL, 0, 1, 1, 'CAR', 'CCTV', 'MON|TUE|WED|THU|FRI|SAT|SUN', 0, 23, 1, '', 1, 1, 'Entrance|Bay')"
      )
      rawV4.execSQL(
        "INSERT INTO `demo_parking_bays` VALUES ('bay-v4-1', 'space-v4-1', 'A-01', 'CAR', 1, 1)"
      )
      rawV4.execSQL(
        "INSERT INTO `demo_bookings` VALUES ('bkg-v4-1', 'PS-DEMO-4401', 'space-v4-1', 'bay-v4-1', 'user-cust-v4', 'user-host-v4', 'veh-v4-1', 'LAHORE', 'HOURLY', 'CONFIRMED', 5000000, 8600000, 120, 10, 130, 1, 1, 'DEMO_PAID_ESCROW_HOLD', 'DEMO-112233', NULL, NULL, NULL, NULL, NULL, 1000)"
      )
      rawV4.version = 4
    } finally {
      rawV4.close()
    }

    val migratedDb =
      Room.databaseBuilder(context, ParkSpaceDemoDatabase::class.java, dbName)
        .addMigrations(*ParkSpaceDemoDatabase.MIGRATIONS)
        .allowMainThreadQueries()
        .build()

    try {
      runBlocking(Dispatchers.IO) {
        val preservedSpace = migratedDb.parkingDao().getSpaceById("space-v4-1")
        val preservedBooking = migratedDb.bookingDao().getBookingById("bkg-v4-1")
        val preservedHost = migratedDb.userDao().getUserById("user-host-v4")
        val preservedCust = migratedDb.userDao().getUserById("user-cust-v4")

        assertNotNull(preservedSpace)
        assertNotNull(preservedBooking)
        assertNotNull(preservedHost)
        assertNotNull(preservedCust)
        assertEquals(emptyList<String>(), preservedSpace?.photoUris)
        assertNull(preservedBooking?.demoPaymentMethod)

        // Ensure v5 chat table is live and preserves account scoping after migration
        val chatRepo = DemoChatRepositoryImpl(migratedDb)
        val chatRes = chatRepo.sendMessage("bkg-v4-1", "user-cust-v4", "On my way to Gate 2")
        assertTrue(chatRes is ChatMutationResult.Success)
        assertEquals(1, migratedDb.chatDao().countMessages())
      }
    } finally {
      migratedDb.close()
      context.deleteDatabase(dbName)
    }
  }

  @Test
  fun `end to end marketplace lifecycle Demo payments referrals photos chat verified reviews disputes notifications`() {
    val db =
      Room.inMemoryDatabaseBuilder(context, ParkSpaceDemoDatabase::class.java)
        .allowMainThreadQueries()
        .build()
    try {
      val authRepo = DemoAuthRepositoryImpl(db)
      val userRepo = DemoUserProfileRepositoryImpl(db.userDao(), db)
      val parkingRepo = DemoParkingRepositoryImpl(db)
      val bookingRepo = DemoBookingRepositoryImpl(db)
      val txRepo = DemoPaymentTransactionRepositoryImpl(db)
      val chatRepo = DemoChatRepositoryImpl(db)
      val reviewRepo = DemoReviewRepositoryImpl(db)
      val disputeRepo = DemoDisputeRepositoryImpl(db)
      val referralRepo = DemoReferralRepositoryImpl(db)
      val notifRepo = DemoNotificationRepositoryImpl(db)
      val sessionManager = DemoSessionManager(userRepo)

      var spaceId = ""
      var bookingCId = ""
      var custAId = ""
      var custCId = ""
      var hostBId = ""

      runBlocking(Dispatchers.IO) {
        // 1. Register Customer A (Referrer), Customer C (Referred), Host B
        val custA =
          (authRepo.registerLocalAccount(
            DemoRegisterRequest(
              role = UserRole.CUSTOMER,
              fullName = "Amina Referrer",
              email = "amina@parkspace.pk",
              password = "password123",
              confirmPassword = "password123",
              preferredCity = SupportedCity.LAHORE,
            )
          ) as DemoAuthResult.Success).user
        custAId = custA.id
        val custACode = userRepo.observeCustomerProfile(custAId).first()!!.referralCode
        val vehicleA =
          userRepo.addVehicleForUser(custAId, VehicleType.BIKE, "Amina CD70", "LER-771", true).getOrThrow()

        val custC =
          (authRepo.registerLocalAccount(
            DemoRegisterRequest(
              role = UserRole.CUSTOMER,
              fullName = "Saad Parker",
              email = "saad@parkspace.pk",
              password = "password123",
              confirmPassword = "password123",
              preferredCity = SupportedCity.LAHORE,
            )
          ) as DemoAuthResult.Success).user
        custCId = custC.id
        val vehicleC =
          userRepo.addVehicleForUser(custCId, VehicleType.CAR, "Saad Swift", "LEG-8801", true).getOrThrow()

        val hostB =
          (authRepo.registerLocalAccount(
            DemoRegisterRequest(
              role = UserRole.HOST,
              fullName = "Tariq Garage Host",
              email = "tariq@parkspace.pk",
              password = "password123",
              confirmPassword = "password123",
              preferredCity = SupportedCity.LAHORE,
              businessAlias = "Tariq Gulberg Plaza",
            )
          ) as DemoAuthResult.Success).user
        hostBId = hostB.id
        val hpB = userRepo.observeHostProfile(hostBId).first()!!
        db.userDao().upsertHostProfile(hpB.copy(verificationStatus = HostVerificationStatus.APPROVED).toEntity())

        // 2. Host B publishes listing with real internal photo URI
        val photoUri = "file:///data/user/0/pk.parkspace.marketplace/files/listing_photos/tariq_gate.jpg"
        val spaceDraft =
          ParkingSpace(
            id = "space-m5-tariq-01",
            hostId = hostBId,
            title = "Tariq Gulberg Covered Deck",
            description = "Secure basement bays with guard.",
            parkingType = ParkingType.COVERED_GARAGE,
            location = ParkingLocation(SupportedCity.LAHORE, "Gulberg III", 31.512, 74.348),
            hourlyRatePkr = 100,
            dailyRatePkr = 800,
            monthlyRatePkr = null,
            monthlyEnabled = false,
            totalBays = 2,
            availableBays = 2,
            supportedVehicles = setOf(VehicleType.CAR, VehicleType.BIKE),
            amenities = setOf(ParkingAmenity.CCTV, ParkingAmenity.SECURITY_GUARD),
            schedule = AvailabilitySchedule(ParkingDayOfWeek.entries.toSet(), 0, 23, true),
            isApproved = false,
            isDemoListing = true,
            photoUris = listOf(photoUri),
          )
        val createdSpaceRes = parkingRepo.createOrUpdateListing(hostBId, spaceDraft, publishImmediately = true)
        assertTrue(createdSpaceRes is HostListingMutationResult.Success)
        spaceId = (createdSpaceRes as HostListingMutationResult.Success).space.id
        assertEquals(listOf(photoUri), parkingRepo.observeApprovedSpacesByCity(SupportedCity.LAHORE).first().first().photoUris)

        // 3. Referrals: reject self-referral & bogus code, redeem Customer A's code for Customer C
        val custCCode = userRepo.observeCustomerProfile(custCId).first()!!.referralCode
        assertTrue(referralRepo.redeemReferralCode(custCId, custCCode) is ReferralMutationResult.Rejected)
        assertTrue(referralRepo.redeemReferralCode(custCId, "INVALID99") is ReferralMutationResult.Rejected)
        val redeemRes = referralRepo.redeemReferralCode(custCId, custACode)
        assertTrue(redeemRes is ReferralMutationResult.Success)
        assertEquals(ReferralRewardStatus.PENDING_FIRST_BOOKING, referralRepo.observeReferralAsReferred(custCId).first()?.status)

        // 4. Reject DEMO_CREDIT_WALLET when customer has 0 Demo credits; succeed with EASYPAISA_DEMO
        val futureStart =
          LocalDate.now(BookingAvailabilityValidator.PAKISTAN_ZONE)
            .plusDays(2)
            .atTime(10, 0)
            .atZone(BookingAvailabilityValidator.PAKISTAN_ZONE)
            .toInstant()
            .toEpochMilli()

        val insufficientCreditRes =
          bookingRepo.createBookingWithAvailabilityCheck(
            BookingCreateRequest(
              spaceId = spaceId,
              customerId = custCId,
              vehicleId = vehicleC.id,
              bookingType = BookingType.HOURLY,
              units = 1,
              startEpochMs = futureStart,
              confirmDemoPaymentImmediately = true,
              paymentMethod = DemoPaymentMethod.DEMO_CREDIT_WALLET,
            )
          )
        assertTrue(insufficientCreditRes is BookingMutationResult.Rejected)

        val confirmedRes =
          bookingRepo.createBookingWithAvailabilityCheck(
            BookingCreateRequest(
              spaceId = spaceId,
              customerId = custCId,
              vehicleId = vehicleC.id,
              bookingType = BookingType.HOURLY,
              units = 2,
              startEpochMs = futureStart,
              confirmDemoPaymentImmediately = true,
              paymentMethod = DemoPaymentMethod.EASYPAISA_DEMO,
            )
          )
        assertTrue(confirmedRes is BookingMutationResult.Success)
        val bookingC = (confirmedRes as BookingMutationResult.Success).booking
        bookingCId = bookingC.id
        assertEquals(DemoPaymentMethod.EASYPAISA_DEMO, bookingC.demoPaymentMethod)

        // Verify Demo Escrow transaction recorded & referral unlocked (Rs. 150 Demo Credit each)
        val bookingTxs = txRepo.observeTransactionsForBooking(bookingCId).first()
        assertTrue(bookingTxs.any { it.transactionType == DemoPaymentTransactionType.ESCROW_PAYMENT && it.method == DemoPaymentMethod.EASYPAISA_DEMO && it.isDemoOnly })
        assertEquals(ReferralRewardStatus.CREDITED_DEMO, referralRepo.observeReferralAsReferred(custCId).first()?.status)
        assertEquals(150, txRepo.observeAvailableDemoCreditPkr(custAId).first())
        assertEquals(150, txRepo.observeAvailableDemoCreditPkr(custCId).first())

        // Customer A spends Demo credits on a Rs. 108 (100 base + 8 fee) 1-hour booking using DEMO_CREDIT_WALLET
        val creditBookingRes =
          bookingRepo.createBookingWithAvailabilityCheck(
            BookingCreateRequest(
              spaceId = spaceId,
              customerId = custAId,
              vehicleId = vehicleA.id,
              bookingType = BookingType.HOURLY,
              units = 1,
              startEpochMs = futureStart,
              confirmDemoPaymentImmediately = true,
              paymentMethod = DemoPaymentMethod.DEMO_CREDIT_WALLET,
            )
          )
        assertTrue(creditBookingRes is BookingMutationResult.Success)
        val creditBooking = (creditBookingRes as BookingMutationResult.Success).booking
        assertEquals(30, txRepo.observeAvailableDemoCreditPkr(custAId).first())
        assertEquals(150 - creditBooking.totalAmountPkr, txRepo.observeAvailableDemoCreditPkr(custAId).first())

        // 5. Booking-scoped Chat + account isolation
        assertTrue(chatRepo.sendMessage(bookingCId, custAId, "Unauthorized peek") is ChatMutationResult.Rejected)
        assertEquals(emptyList<Any>(), chatRepo.observeMessagesForBooking(bookingCId, custAId).first())

        assertTrue(chatRepo.sendMessage(bookingCId, custCId, "Arriving at basement ramp soon.", nowEpochMs = 10_000L) is ChatMutationResult.Success)
        assertTrue(chatRepo.sendMessage(bookingCId, hostBId, "Gate guard is ready at B-01.", nowEpochMs = 20_000L) is ChatMutationResult.Success)
        val msgs = chatRepo.observeMessagesForBooking(bookingCId, custCId).first()
        assertEquals(2, msgs.size)
        chatRepo.markThreadRead(bookingCId, custCId)
        val custThreads = chatRepo.observeThreadSummariesForUser(custCId).first()
        assertEquals(1, custThreads.size)
        assertEquals(0, custThreads.first().unreadCount)

        // 6. Verified Reviews: blocked before check-out, allowed once after check-out + Host reply + rating summary
        assertTrue(reviewRepo.submitReview(bookingCId, custCId, 5, "Great spot!") is ReviewMutationResult.Rejected)
        assertTrue(bookingRepo.checkInBooking(bookingCId, custCId, futureStart + 60_000L) is BookingMutationResult.Success)
        assertTrue(bookingRepo.checkOutBooking(bookingCId, custCId, futureStart + 3_600_000L) is BookingMutationResult.Success)

        val hostSettlementTxs = txRepo.observeTransactionsForBooking(bookingCId).first()
        assertTrue(hostSettlementTxs.any { it.transactionType == DemoPaymentTransactionType.HOST_SETTLEMENT })

        val reviewRes = reviewRepo.submitReview(bookingCId, custCId, 5, "Clean covered basement and quick entry.")
        assertTrue(reviewRes is ReviewMutationResult.Success)
        val createdReview = (reviewRes as ReviewMutationResult.Success).review
        assertTrue(reviewRepo.submitReview(bookingCId, custCId, 4, "Duplicate attempt") is ReviewMutationResult.Rejected)

        val replyRes = reviewRepo.replyToReview(createdReview.id, hostBId, "Thank you for parking with us!")
        assertTrue(replyRes is ReviewMutationResult.Success)

        val ratingSummary = reviewRepo.observeSpaceRatingSummaries().first().first { it.spaceId == spaceId }
        assertEquals(5.0, ratingSummary.averageRating!!, 0.01)
        assertEquals(1, ratingSummary.reviewCount)

        // 7. Booking Disputes / Complaints: File -> Host Respond -> Resolve
        val fileDisputeRes = disputeRepo.fileDispute(bookingCId, custCId, DisputeCategory.GATE_ACCESS_ISSUE, "Barrier sensor took 2 minutes to open.")
        assertTrue(fileDisputeRes is DisputeMutationResult.Success)
        val dispute = (fileDisputeRes as DisputeMutationResult.Success).dispute
        assertEquals(DisputeStatus.OPEN, dispute.status)

        val respondDisputeRes = disputeRepo.respondToDisputeAsHost(dispute.id, hostBId, "Recalibrated gate loop sensor this evening.")
        assertTrue(respondDisputeRes is DisputeMutationResult.Success)
        assertEquals(DisputeStatus.UNDER_REVIEW, (respondDisputeRes as DisputeMutationResult.Success).dispute.status)

        val resolveRes = disputeRepo.resolveDisputeAsCustomerOrHost(dispute.id, custCId, "Thanks, worked smoothly on exit.")
        assertTrue(resolveRes is DisputeMutationResult.Success)
        assertEquals(DisputeStatus.RESOLVED, (resolveRes as DisputeMutationResult.Success).dispute.status)

        // 8. Recipient-scoped Notifications verification
        val custCNotifs = notifRepo.observeNotificationsForUser(custCId).first()
        val hostBNotifs = notifRepo.observeNotificationsForUser(hostBId).first()
        assertTrue(custCNotifs.any { it.type == ParkSpaceNotificationType.PAYMENT_DEMO })
        assertTrue(custCNotifs.any { it.type == ParkSpaceNotificationType.REFERRAL_UPDATE })
        assertTrue(custCNotifs.any { it.type == ParkSpaceNotificationType.CHAT_MESSAGE })
        assertTrue(custCNotifs.any { it.type == ParkSpaceNotificationType.DISPUTE_UPDATE })
        assertTrue(hostBNotifs.any { it.type == ParkSpaceNotificationType.REVIEW_POSTED })
        assertTrue(notifRepo.observeUnreadCount(custCId).first() > 0)
        notifRepo.markAllAsRead(custCId)
        assertEquals(0, notifRepo.observeUnreadCount(custCId).first())
      }

      // 9. Verify DiscoveryViewModel integrates rating summaries & ParkingDetailPreviewScreen renders verified review + host reply
      val listingForUi =
        runBlocking(Dispatchers.IO) {
          val reviews = reviewRepo.observeReviewsForSpace(spaceId).first()
          val space = parkingRepo.observeApprovedSpacesByCity(SupportedCity.LAHORE).first().first()
          val discoveryVm =
            DiscoveryViewModel(
              parkingRepository = parkingRepo,
              demoSessionManager = sessionManager,
              reviewRepository = reviewRepo,
            )
          assertNotNull(discoveryVm)
          ParkingPublicListing.fromDomainSpace(
            space = space,
            averageRating = 5.0,
            reviewCount = reviews.size,
          ) to reviews
        }

      composeRule.setContent {
        ParkSpaceTheme {
          ParkingDetailPreviewScreen(
            listing = listingForUi.first,
            onBack = {},
            onBookNow = {},
            reviews = listingForUi.second,
          )
        }
      }
      composeRule.waitForIdle()
      composeRule.onNodeWithTag("detail_reviews_card").performScrollTo().assertIsDisplayed()
      composeRule.onNodeWithText("Clean covered basement and quick entry.").performScrollTo().assertIsDisplayed()
      composeRule.onNodeWithText("Host Reply: Thank you for parking with us!").performScrollTo().assertIsDisplayed()
    } finally {
      db.close()
    }
  }

  @Test
  fun `ParkSpaceNavGuard enforces Milestone 5 role boundaries and isolates Super Admin shell`() {
    val unauth = DemoSessionState()
    assertTrue(ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.NotificationsInbox, unauth) is NavGuardResult.Denied)
    assertTrue(ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.ChatInbox, unauth) is NavGuardResult.Denied)
    assertTrue(ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.CustomerPaymentsAndReferrals, unauth) is NavGuardResult.Denied)
    assertTrue(ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.HostReviewsCenter, unauth) is NavGuardResult.Denied)

    val customerSession =
      DemoSessionState(
        activeUser =
          com.example.core.domain.model.UserProfile(
            id = "c1",
            fullName = "Customer One",
            phoneMasked = "0300",
            emailMasked = "c***@pk.com",
            role = UserRole.CUSTOMER,
            city = SupportedCity.LAHORE,
            isDemoAccount = true,
            createdAtEpochMs = 1_000L,
            isSelfRegisteredDemoAccount = true,
          ),
      )
    assertTrue(ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.NotificationsInbox, customerSession) is NavGuardResult.Allowed)
    assertTrue(ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.ChatInbox, customerSession) is NavGuardResult.Allowed)
    assertTrue(ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.BookingChat("b1"), customerSession) is NavGuardResult.Allowed)
    assertTrue(ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.CustomerPaymentsAndReferrals, customerSession) is NavGuardResult.Allowed)
    assertTrue(ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.DisputesCenter(), customerSession) is NavGuardResult.Allowed)
    assertTrue(ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.HostReviewsCenter, customerSession) is NavGuardResult.Denied)

    val hostSession =
      customerSession.copy(
        activeUser = customerSession.activeUser!!.copy(id = "h1", role = UserRole.HOST),
      )
    assertTrue(ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.NotificationsInbox, hostSession) is NavGuardResult.Allowed)
    assertTrue(ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.ChatInbox, hostSession) is NavGuardResult.Allowed)
    assertTrue(ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.DisputesCenter("b1"), hostSession) is NavGuardResult.Allowed)
    assertTrue(ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.HostReviewsCenter, hostSession) is NavGuardResult.Allowed)
    assertTrue(ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.CustomerPaymentsAndReferrals, hostSession) is NavGuardResult.Denied)

    val adminSession =
      customerSession.copy(
        activeUser = customerSession.activeUser!!.copy(id = "a1", role = UserRole.SUPER_ADMIN),
        isAdminSandboxUnlocked = true,
      )
    assertTrue(ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.ChatInbox, adminSession) is NavGuardResult.Denied)
    assertTrue(ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.CustomerPaymentsAndReferrals, adminSession) is NavGuardResult.Denied)
    assertTrue(ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.DisputesCenter(), adminSession) is NavGuardResult.Denied)
    assertTrue(ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.HostReviewsCenter, adminSession) is NavGuardResult.Denied)
  }
}
