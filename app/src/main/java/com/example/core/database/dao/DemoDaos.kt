package com.example.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.core.config.SupportedCity
import com.example.core.config.UserRole
import com.example.core.database.entity.DemoBookingEntity
import com.example.core.database.entity.DemoCustomerProfileEntity
import com.example.core.database.entity.DemoHostProfileEntity
import com.example.core.database.entity.DemoParkingBayEntity
import com.example.core.database.entity.DemoParkingSpaceEntity
import com.example.core.database.entity.DemoSeedMetadataEntity
import com.example.core.database.entity.DemoUserEntity
import com.example.core.database.entity.DemoVehicleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DemoSeedMetadataDao {
  @Query("SELECT * FROM demo_seed_metadata WHERE `key` = :key LIMIT 1")
  suspend fun getSeedMetadata(key: String): DemoSeedMetadataEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSeedMetadata(meta: DemoSeedMetadataEntity)
}

@Dao
interface DemoUserDao {
  @Query("SELECT * FROM demo_users ORDER BY role ASC, fullName ASC")
  fun observeAllUsers(): Flow<List<DemoUserEntity>>

  @Query("SELECT * FROM demo_users WHERE role = :role ORDER BY fullName ASC")
  fun observeUsersByRole(role: UserRole): Flow<List<DemoUserEntity>>

  @Query("SELECT * FROM demo_users WHERE id = :userId LIMIT 1")
  suspend fun getUserById(userId: String): DemoUserEntity?

  @Query("SELECT * FROM demo_users WHERE emailNormalized = :emailNormalized AND role = :role LIMIT 1")
  suspend fun findByEmailAndRole(emailNormalized: String, role: UserRole): DemoUserEntity?

  @Query("SELECT * FROM demo_customer_profiles WHERE userId = :userId LIMIT 1")
  fun observeCustomerProfile(userId: String): Flow<DemoCustomerProfileEntity?>

  @Query("SELECT * FROM demo_customer_profiles WHERE userId = :userId LIMIT 1")
  suspend fun getCustomerProfileOnce(userId: String): DemoCustomerProfileEntity?

  @Query("SELECT * FROM demo_customer_profiles WHERE UPPER(referralCode) = UPPER(:referralCode) LIMIT 1")
  suspend fun getCustomerProfileByReferralCode(referralCode: String): DemoCustomerProfileEntity?

  @Query("SELECT * FROM demo_host_profiles WHERE userId = :userId LIMIT 1")
  fun observeHostProfile(userId: String): Flow<DemoHostProfileEntity?>

  @Query("SELECT * FROM demo_host_profiles WHERE userId = :userId LIMIT 1")
  suspend fun getHostProfileOnce(userId: String): DemoHostProfileEntity?

  @Query("SELECT * FROM demo_host_profiles ORDER BY joinedEpochMs DESC")
  fun observeAllHostProfiles(): Flow<List<DemoHostProfileEntity>>

  @Query("SELECT * FROM demo_vehicles WHERE ownerUserId = :userId ORDER BY isDefault DESC, label ASC")
  fun observeVehiclesForUser(userId: String): Flow<List<DemoVehicleEntity>>

  @Query("SELECT * FROM demo_vehicles WHERE id = :vehicleId LIMIT 1")
  suspend fun getVehicleById(vehicleId: String): DemoVehicleEntity?

  @Query("SELECT COUNT(*) FROM demo_users")
  suspend fun countUsers(): Int

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insertUser(user: DemoUserEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertCustomerProfile(profile: DemoCustomerProfileEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertHostProfile(profile: DemoHostProfileEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertVehicle(vehicle: DemoVehicleEntity)

  @Query("UPDATE demo_vehicles SET isDefault = 0 WHERE ownerUserId = :ownerUserId")
  suspend fun clearDefaultVehicleFlagForUser(ownerUserId: String)

  @Query("UPDATE demo_users SET city = :city WHERE id = :userId")
  suspend fun updateUserCity(userId: String, city: SupportedCity)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertUsersIfMissing(users: List<DemoUserEntity>)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertCustomerProfilesIfMissing(profiles: List<DemoCustomerProfileEntity>)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertHostProfilesIfMissing(profiles: List<DemoHostProfileEntity>)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertVehiclesIfMissing(vehicles: List<DemoVehicleEntity>)

  @Query("DELETE FROM demo_users WHERE id IN (:ids) AND isSelfRegisteredDemoAccount = 0")
  suspend fun deleteLegacySeedUsersByIds(ids: Set<String>)

  @Query("DELETE FROM demo_customer_profiles WHERE userId IN (:userIds)")
  suspend fun deleteLegacySeedCustomerProfilesByIds(userIds: Set<String>)

  @Query("DELETE FROM demo_host_profiles WHERE userId IN (:userIds)")
  suspend fun deleteLegacySeedHostProfilesByIds(userIds: Set<String>)

  @Query("DELETE FROM demo_vehicles WHERE id IN (:vehicleIds)")
  suspend fun deleteLegacySeedVehiclesByIds(vehicleIds: Set<String>)
}

@Dao
interface DemoParkingDao {
  @Query("SELECT * FROM demo_parking_spaces WHERE isApproved = 1 ORDER BY city ASC, title ASC")
  fun observeAllApprovedSpaces(): Flow<List<DemoParkingSpaceEntity>>

  @Query(
    "SELECT * FROM demo_parking_spaces WHERE isApproved = 1 AND city = :city ORDER BY hourlyRatePkr ASC"
  )
  fun observeApprovedSpacesByCity(city: SupportedCity): Flow<List<DemoParkingSpaceEntity>>

  @Query("SELECT * FROM demo_parking_spaces ORDER BY city ASC, isApproved DESC, title ASC")
  fun observeAllSpacesIncludingPending(): Flow<List<DemoParkingSpaceEntity>>

  @Query("SELECT * FROM demo_parking_spaces WHERE hostId = :hostId ORDER BY title ASC")
  fun observeSpacesByHost(hostId: String): Flow<List<DemoParkingSpaceEntity>>

  @Query("SELECT * FROM demo_parking_spaces WHERE id = :spaceId LIMIT 1")
  suspend fun getSpaceById(spaceId: String): DemoParkingSpaceEntity?

  @Query("SELECT * FROM demo_parking_bays WHERE spaceId = :spaceId ORDER BY bayCode ASC")
  fun observeBaysForSpace(spaceId: String): Flow<List<DemoParkingBayEntity>>

  @Query("SELECT * FROM demo_parking_bays")
  fun observeAllBays(): Flow<List<DemoParkingBayEntity>>

  @Query("SELECT * FROM demo_parking_bays WHERE spaceId = :spaceId ORDER BY bayCode ASC")
  suspend fun getBaysForSpaceOnce(spaceId: String): List<DemoParkingBayEntity>

  @Query("SELECT * FROM demo_parking_bays WHERE id = :bayId LIMIT 1")
  suspend fun getBayById(bayId: String): DemoParkingBayEntity?

  @Query("SELECT * FROM demo_parking_bays WHERE spaceId = :spaceId AND bayCode = :bayCode LIMIT 1")
  suspend fun getBayBySpaceAndCode(spaceId: String, bayCode: String): DemoParkingBayEntity?

  @Query("SELECT COUNT(*) FROM demo_parking_spaces")
  suspend fun countSpaces(): Int

  @Query("SELECT COUNT(*) FROM demo_parking_bays")
  suspend fun countBays(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSpace(space: DemoParkingSpaceEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertBay(bay: DemoParkingBayEntity)

  @Update
  suspend fun updateBay(bay: DemoParkingBayEntity)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertSpacesIfMissing(spaces: List<DemoParkingSpaceEntity>)

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertBaysIfMissing(bays: List<DemoParkingBayEntity>)

  @Query("DELETE FROM demo_parking_spaces WHERE id IN (:spaceIds)")
  suspend fun deleteLegacySeedSpacesByIds(spaceIds: Set<String>)

  @Query("DELETE FROM demo_parking_bays WHERE id IN (:bayIds)")
  suspend fun deleteLegacySeedBaysByIds(bayIds: Set<String>)
}

@Dao
interface DemoBookingDao {
  @Query("SELECT * FROM demo_bookings WHERE customerId = :customerId ORDER BY startEpochMs DESC")
  fun observeBookingsForCustomer(customerId: String): Flow<List<DemoBookingEntity>>

  @Query("SELECT * FROM demo_bookings WHERE hostId = :hostId ORDER BY startEpochMs DESC")
  fun observeBookingsForHost(hostId: String): Flow<List<DemoBookingEntity>>

  @Query("SELECT * FROM demo_bookings ORDER BY startEpochMs DESC")
  fun observeAllBookings(): Flow<List<DemoBookingEntity>>

  @Query("SELECT * FROM demo_bookings WHERE spaceId = :spaceId ORDER BY startEpochMs ASC")
  fun observeBookingsForSpace(spaceId: String): Flow<List<DemoBookingEntity>>

  @Query("SELECT * FROM demo_bookings WHERE spaceId = :spaceId ORDER BY startEpochMs ASC")
  suspend fun getBookingsForSpaceOnce(spaceId: String): List<DemoBookingEntity>

  @Query("SELECT * FROM demo_bookings WHERE id = :bookingId LIMIT 1")
  fun observeBookingById(bookingId: String): Flow<DemoBookingEntity?>

  @Query("SELECT * FROM demo_bookings WHERE id = :bookingId LIMIT 1")
  suspend fun getBookingById(bookingId: String): DemoBookingEntity?

  @Query("SELECT * FROM demo_bookings WHERE status IN ('PENDING_PAYMENT', 'CONFIRMED') AND endEpochMs < :nowEpochMs")
  suspend fun getStaleExpirableBookingsOnce(nowEpochMs: Long): List<DemoBookingEntity>

  @Query("SELECT COUNT(*) FROM demo_bookings")
  suspend fun countBookings(): Int

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertBookingsIfMissing(bookings: List<DemoBookingEntity>)

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insertBooking(booking: DemoBookingEntity)

  @Update
  suspend fun updateBooking(booking: DemoBookingEntity)

  @Query("DELETE FROM demo_bookings WHERE id IN (:bookingIds)")
  suspend fun deleteLegacySeedBookingsByIds(bookingIds: Set<String>)
}
