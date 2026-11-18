package com.example.demo.seed

import androidx.room.withTransaction
import com.example.core.database.ParkSpaceDemoDatabase
import com.example.core.database.entity.DemoSeedMetadataEntity
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Ensures ParkSpace starts with a completely empty marketplace and safely purges any
 * identifiable legacy v1 fictional seed records from pre-existing local databases.
 * Never inserts fictional users, listings, bays, vehicles, or bookings, and never deletes
 * user-created records.
 */
class DemoSeedInitializer(
  private val database: ParkSpaceDemoDatabase,
) {
  private val mutex = Mutex()

  suspend fun seedIfNeeded() {
    mutex.withLock {
      val seedDao = database.seedMetadataDao()
      val existingMeta = seedDao.getSeedMetadata(DemoSeedDataset.SEED_KEY)
      if (existingMeta != null && existingMeta.seedVersion >= DemoSeedDataset.SEED_VERSION) {
        return
      }

      val userDao = database.userDao()
      val parkingDao = database.parkingDao()
      val bookingDao = database.bookingDao()

      database.withTransaction {
        // Purge only exact-ID legacy fictional seed fixtures; user-created records are untouched.
        bookingDao.deleteLegacySeedBookingsByIds(DemoSeedDataset.LEGACY_SEED_BOOKING_IDS)
        parkingDao.deleteLegacySeedBaysByIds(DemoSeedDataset.LEGACY_SEED_BAY_IDS)
        parkingDao.deleteLegacySeedSpacesByIds(DemoSeedDataset.LEGACY_SEED_SPACE_IDS)
        userDao.deleteLegacySeedVehiclesByIds(DemoSeedDataset.LEGACY_SEED_VEHICLE_IDS)
        userDao.deleteLegacySeedHostProfilesByIds(DemoSeedDataset.LEGACY_SEED_HOST_PROFILE_IDS)
        userDao.deleteLegacySeedCustomerProfilesByIds(DemoSeedDataset.LEGACY_SEED_CUSTOMER_PROFILE_IDS)
        userDao.deleteLegacySeedUsersByIds(DemoSeedDataset.LEGACY_SEED_USER_IDS)

        seedDao.upsertSeedMetadata(
          DemoSeedMetadataEntity(
            key = DemoSeedDataset.SEED_KEY,
            seedVersion = DemoSeedDataset.SEED_VERSION,
            seededAtEpochMs = System.currentTimeMillis(),
          )
        )
      }
    }
  }
}
