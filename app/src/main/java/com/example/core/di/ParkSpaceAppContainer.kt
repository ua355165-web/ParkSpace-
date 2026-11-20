package com.example.core.di

import android.content.Context
import com.example.core.data.demo.DemoAdminRepositoryImpl
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
import com.example.core.domain.repository.AdminRepository
import com.example.core.domain.repository.BookingRepository
import com.example.core.domain.repository.ChatRepository
import com.example.core.domain.repository.DemoAuthRepository
import com.example.core.domain.repository.DisputeRepository
import com.example.core.domain.repository.NotificationRepository
import com.example.core.domain.repository.ParkingRepository
import com.example.core.domain.repository.PaymentTransactionRepository
import com.example.core.domain.repository.ReferralRepository
import com.example.core.domain.repository.ReviewRepository
import com.example.core.domain.repository.UserProfileRepository
import com.example.demo.sandbox.DemoSessionManager
import com.example.demo.sandbox.DemoSessionPersistence
import com.example.demo.sandbox.SharedPrefsDemoSessionPersistence
import com.example.demo.seed.DemoSeedInitializer

/**
 * Simple Constructor-Injection application container.
 * Keeps Demo Room persistence cleanly encapsulated behind repository interfaces.
 */
class ParkSpaceAppContainer(context: Context) {
  val appContext: Context = context.applicationContext

  val demoDatabase: ParkSpaceDemoDatabase = ParkSpaceDemoDatabase.getInstance(context)

  val demoSeedInitializer: DemoSeedInitializer = DemoSeedInitializer(demoDatabase)

  val userProfileRepository: UserProfileRepository =
    DemoUserProfileRepositoryImpl(demoDatabase.userDao(), demoDatabase)

  val demoAuthRepository: DemoAuthRepository =
    DemoAuthRepositoryImpl(demoDatabase)

  val parkingRepository: ParkingRepository =
    DemoParkingRepositoryImpl(demoDatabase)

  val bookingRepository: BookingRepository =
    DemoBookingRepositoryImpl(demoDatabase)

  val paymentTransactionRepository: PaymentTransactionRepository =
    DemoPaymentTransactionRepositoryImpl(demoDatabase)

  val chatRepository: ChatRepository =
    DemoChatRepositoryImpl(demoDatabase)

  val reviewRepository: ReviewRepository =
    DemoReviewRepositoryImpl(demoDatabase)

  val disputeRepository: DisputeRepository =
    DemoDisputeRepositoryImpl(demoDatabase)

  val referralRepository: ReferralRepository =
    DemoReferralRepositoryImpl(demoDatabase)

  val notificationRepository: NotificationRepository =
    DemoNotificationRepositoryImpl(demoDatabase)

  val sessionPersistence: DemoSessionPersistence =
    SharedPrefsDemoSessionPersistence(context)

  val themePreferences: com.example.ui.theme.ThemePreferenceStore =
    com.example.ui.theme.SharedPrefsThemePreferenceStore(context)

  val demoSessionManager: DemoSessionManager =
    DemoSessionManager(userProfileRepository, sessionPersistence)

  val adminRepository: AdminRepository =
    DemoAdminRepositoryImpl(demoDatabase) { demoSessionManager.session.value }
}
