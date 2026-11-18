package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.core.database.converter.DemoRoomConverters
import com.example.core.database.dao.DemoAdminDao
import com.example.core.database.dao.DemoBookingDao
import com.example.core.database.dao.DemoChatDao
import com.example.core.database.dao.DemoDisputeDao
import com.example.core.database.dao.DemoNotificationDao
import com.example.core.database.dao.DemoParkingDao
import com.example.core.database.dao.DemoPaymentTransactionDao
import com.example.core.database.dao.DemoReferralDao
import com.example.core.database.dao.DemoReviewDao
import com.example.core.database.dao.DemoSeedMetadataDao
import com.example.core.database.dao.DemoUserDao
import com.example.core.database.entity.DemoAdminAuditLogEntity
import com.example.core.database.entity.DemoBookingEntity
import com.example.core.database.entity.DemoChatMessageEntity
import com.example.core.database.entity.DemoCommissionConfigEntity
import com.example.core.database.entity.DemoCustomerProfileEntity
import com.example.core.database.entity.DemoDisputeEntity
import com.example.core.database.entity.DemoHostProfileEntity
import com.example.core.database.entity.DemoNotificationEntity
import com.example.core.database.entity.DemoParkingBayEntity
import com.example.core.database.entity.DemoParkingSpaceEntity
import com.example.core.database.entity.DemoPaymentTransactionEntity
import com.example.core.database.entity.DemoReferralEntity
import com.example.core.database.entity.DemoReviewEntity
import com.example.core.database.entity.DemoSeedMetadataEntity
import com.example.core.database.entity.DemoUserEntity
import com.example.core.database.entity.DemoVehicleEntity

/**
 * Isolated local Room database strictly for Demo Mode persistence.
 * Production cloud data (Milestone 8) will live in separate Firebase data sources.
 */
@Database(
  entities =
    [
      DemoSeedMetadataEntity::class,
      DemoUserEntity::class,
      DemoCustomerProfileEntity::class,
      DemoHostProfileEntity::class,
      DemoVehicleEntity::class,
      DemoParkingSpaceEntity::class,
      DemoParkingBayEntity::class,
      DemoBookingEntity::class,
      DemoPaymentTransactionEntity::class,
      DemoChatMessageEntity::class,
      DemoReviewEntity::class,
      DemoDisputeEntity::class,
      DemoReferralEntity::class,
      DemoNotificationEntity::class,
      DemoCommissionConfigEntity::class,
      DemoAdminAuditLogEntity::class,
    ],
  version = ParkSpaceDemoDatabase.CURRENT_SCHEMA_VERSION,
  exportSchema = false,
)
@TypeConverters(DemoRoomConverters::class)
abstract class ParkSpaceDemoDatabase : RoomDatabase() {
  abstract fun seedMetadataDao(): DemoSeedMetadataDao

  abstract fun userDao(): DemoUserDao

  abstract fun parkingDao(): DemoParkingDao

  abstract fun bookingDao(): DemoBookingDao

  abstract fun paymentTransactionDao(): DemoPaymentTransactionDao

  abstract fun chatDao(): DemoChatDao

  abstract fun reviewDao(): DemoReviewDao

  abstract fun disputeDao(): DemoDisputeDao

  abstract fun referralDao(): DemoReferralDao

  abstract fun notificationDao(): DemoNotificationDao

  abstract fun adminDao(): DemoAdminDao

  companion object {
    const val CURRENT_SCHEMA_VERSION = 6
    const val DATABASE_NAME = "parkspace_demo_v1.db"

    val MIGRATION_1_2: Migration =
      object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            "ALTER TABLE demo_bookings ADD COLUMN demoPaymentStatus TEXT NOT NULL DEFAULT 'DEMO_PENDING_HOLD'"
          )
          db.execSQL("ALTER TABLE demo_bookings ADD COLUMN demoEntryPin TEXT")
          db.execSQL("ALTER TABLE demo_bookings ADD COLUMN checkedInAtEpochMs INTEGER")
          db.execSQL("ALTER TABLE demo_bookings ADD COLUMN checkedOutAtEpochMs INTEGER")
          db.execSQL("ALTER TABLE demo_bookings ADD COLUMN cancelledAtEpochMs INTEGER")
          db.execSQL("ALTER TABLE demo_bookings ADD COLUMN refundAmountPkr INTEGER")
          db.execSQL("ALTER TABLE demo_bookings ADD COLUMN cancellationPolicySummary TEXT")
          db.execSQL(
            "ALTER TABLE demo_bookings ADD COLUMN createdAtEpochMs INTEGER NOT NULL DEFAULT 0"
          )
        }
      }

    val MIGRATION_2_3: Migration =
      object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE demo_users ADD COLUMN emailNormalized TEXT")
          db.execSQL("ALTER TABLE demo_users ADD COLUMN passwordHash TEXT")
          db.execSQL("ALTER TABLE demo_users ADD COLUMN passwordSalt TEXT")
          db.execSQL(
            "ALTER TABLE demo_users ADD COLUMN isSelfRegisteredDemoAccount INTEGER NOT NULL DEFAULT 0"
          )
          db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_demo_users_emailNormalized_role` ON `demo_users` (`emailNormalized`, `role`)"
          )
          db.execSQL(
            "UPDATE demo_host_profiles SET verificationStatus = 'APPROVED' WHERE verificationStatus = 'VERIFIED'"
          )
          db.execSQL(
            "UPDATE demo_host_profiles SET verificationStatus = 'REJECTED' WHERE verificationStatus = 'SUSPENDED'"
          )
        }
      }

    val MIGRATION_3_4: Migration =
      object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            "ALTER TABLE demo_parking_spaces ADD COLUMN photoTags TEXT NOT NULL DEFAULT 'Entrance Gate|Covered Bay'"
          )
        }
      }

    val MIGRATION_4_5: Migration =
      object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL(
            "ALTER TABLE demo_parking_spaces ADD COLUMN photoUris TEXT NOT NULL DEFAULT ''"
          )
          db.execSQL("ALTER TABLE demo_bookings ADD COLUMN demoPaymentMethod TEXT")
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `demo_payment_transactions` (
              `id` TEXT NOT NULL,
              `bookingId` TEXT,
              `bookingCode` TEXT,
              `customerId` TEXT NOT NULL,
              `hostId` TEXT,
              `spaceId` TEXT,
              `method` TEXT NOT NULL,
              `transactionType` TEXT NOT NULL,
              `amountPkr` INTEGER NOT NULL,
              `referenceCode` TEXT NOT NULL,
              `summary` TEXT NOT NULL,
              `isDemoOnly` INTEGER NOT NULL,
              `createdAtEpochMs` INTEGER NOT NULL,
              PRIMARY KEY(`id`)
            )
            """.trimIndent()
          )
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `demo_chat_messages` (
              `id` TEXT NOT NULL,
              `bookingId` TEXT NOT NULL,
              `spaceId` TEXT NOT NULL,
              `customerId` TEXT NOT NULL,
              `hostId` TEXT NOT NULL,
              `senderUserId` TEXT NOT NULL,
              `senderRole` TEXT NOT NULL,
              `senderDisplayName` TEXT NOT NULL,
              `body` TEXT NOT NULL,
              `sentAtEpochMs` INTEGER NOT NULL,
              `isReadByCustomer` INTEGER NOT NULL,
              `isReadByHost` INTEGER NOT NULL,
              PRIMARY KEY(`id`)
            )
            """.trimIndent()
          )
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `demo_reviews` (
              `id` TEXT NOT NULL,
              `bookingId` TEXT NOT NULL,
              `spaceId` TEXT NOT NULL,
              `customerId` TEXT NOT NULL,
              `customerDisplayName` TEXT NOT NULL,
              `hostId` TEXT NOT NULL,
              `rating` INTEGER NOT NULL,
              `comment` TEXT NOT NULL,
              `hostReply` TEXT,
              `hostRepliedAtEpochMs` INTEGER,
              `createdAtEpochMs` INTEGER NOT NULL,
              PRIMARY KEY(`id`)
            )
            """.trimIndent()
          )
          db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_demo_reviews_bookingId` ON `demo_reviews` (`bookingId`)"
          )
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `demo_disputes` (
              `id` TEXT NOT NULL,
              `bookingId` TEXT NOT NULL,
              `bookingCode` TEXT NOT NULL,
              `spaceId` TEXT NOT NULL,
              `customerId` TEXT NOT NULL,
              `hostId` TEXT NOT NULL,
              `reporterUserId` TEXT NOT NULL,
              `reporterRole` TEXT NOT NULL,
              `category` TEXT NOT NULL,
              `description` TEXT NOT NULL,
              `status` TEXT NOT NULL,
              `hostResponseNote` TEXT,
              `resolutionNote` TEXT,
              `createdAtEpochMs` INTEGER NOT NULL,
              `updatedAtEpochMs` INTEGER NOT NULL,
              PRIMARY KEY(`id`)
            )
            """.trimIndent()
          )
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `demo_referrals` (
              `id` TEXT NOT NULL,
              `referrerUserId` TEXT NOT NULL,
              `referredUserId` TEXT NOT NULL,
              `referralCodeUsed` TEXT NOT NULL,
              `rewardAmountPkr` INTEGER NOT NULL,
              `status` TEXT NOT NULL,
              `isDemoOnly` INTEGER NOT NULL,
              `createdAtEpochMs` INTEGER NOT NULL,
              `creditedAtEpochMs` INTEGER,
              PRIMARY KEY(`id`)
            )
            """.trimIndent()
          )
          db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_demo_referrals_referredUserId` ON `demo_referrals` (`referredUserId`)"
          )
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `demo_notifications` (
              `id` TEXT NOT NULL,
              `recipientUserId` TEXT NOT NULL,
              `type` TEXT NOT NULL,
              `title` TEXT NOT NULL,
              `message` TEXT NOT NULL,
              `relatedBookingId` TEXT,
              `relatedSpaceId` TEXT,
              `isRead` INTEGER NOT NULL,
              `createdAtEpochMs` INTEGER NOT NULL,
              PRIMARY KEY(`id`)
            )
            """.trimIndent()
          )
        }
      }

    val MIGRATION_5_6: Migration =
      object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
          db.execSQL("ALTER TABLE demo_host_profiles ADD COLUMN verificationNote TEXT")
          db.execSQL("ALTER TABLE demo_host_profiles ADD COLUMN reviewedAtEpochMs INTEGER")
          db.execSQL("ALTER TABLE demo_parking_spaces ADD COLUMN moderationNote TEXT")
          db.execSQL("ALTER TABLE demo_parking_spaces ADD COLUMN moderatedAtEpochMs INTEGER")
          db.execSQL(
            "ALTER TABLE demo_parking_spaces ADD COLUMN isRejectedByAdmin INTEGER NOT NULL DEFAULT 0"
          )
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `demo_commission_config` (
              `id` TEXT NOT NULL,
              `commissionPercent` INTEGER NOT NULL,
              `minFeePkr` INTEGER NOT NULL,
              `maxFeePkr` INTEGER NOT NULL,
              `note` TEXT NOT NULL,
              `updatedByAdminId` TEXT,
              `updatedAtEpochMs` INTEGER NOT NULL,
              PRIMARY KEY(`id`)
            )
            """.trimIndent()
          )
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `demo_admin_audit_logs` (
              `id` TEXT NOT NULL,
              `actorAdminId` TEXT NOT NULL,
              `actorLabel` TEXT NOT NULL,
              `actionType` TEXT NOT NULL,
              `targetType` TEXT NOT NULL,
              `targetId` TEXT NOT NULL,
              `targetLabel` TEXT NOT NULL,
              `outcomeSummary` TEXT NOT NULL,
              `reasonOrNote` TEXT,
              `createdAtEpochMs` INTEGER NOT NULL,
              PRIMARY KEY(`id`)
            )
            """.trimIndent()
          )
        }
      }

    // Explicit incremental migrations registered cleanly for schema v2..v6.
    val MIGRATIONS: Array<Migration> =
      arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)

    @Volatile private var instance: ParkSpaceDemoDatabase? = null

    fun getInstance(context: Context): ParkSpaceDemoDatabase =
      instance
        ?: synchronized(this) {
          instance
            ?: Room.databaseBuilder(
                context.applicationContext,
                ParkSpaceDemoDatabase::class.java,
                DATABASE_NAME,
              )
              .addMigrations(*MIGRATIONS)
              .build()
              .also { instance = it }
        }
  }
}
