package com.example.core.domain.repository

import com.example.core.config.BookingType
import com.example.core.config.SupportedCity
import com.example.core.config.UserRole
import com.example.core.domain.model.AvailabilitySchedule
import com.example.core.domain.model.Booking
import com.example.core.domain.model.CustomerProfile
import com.example.core.domain.model.DemoPaymentMethod
import com.example.core.domain.model.HostProfile
import com.example.core.domain.model.ParkingBay
import com.example.core.domain.model.ParkingSpace
import com.example.core.domain.model.UserProfile
import com.example.core.domain.model.VehicleProfile
import com.example.core.domain.model.VehicleType
import kotlinx.coroutines.flow.Flow

data class DemoRegisterRequest(
  val role: UserRole,
  val fullName: String,
  val email: String,
  val password: String,
  val confirmPassword: String,
  val preferredCity: SupportedCity = SupportedCity.LAHORE,
  val businessAlias: String = "",
)

data class DemoSignInRequest(
  val role: UserRole,
  val email: String,
  val password: String,
)

sealed interface DemoAuthResult {
  data class Success(val user: UserProfile, val message: String) : DemoAuthResult

  data class Rejected(val reason: String) : DemoAuthResult
}

/**
 * Local Demo authentication contract designed to swap cleanly with Firebase Auth in Milestone 8.
 */
interface DemoAuthRepository {
  suspend fun registerLocalAccount(request: DemoRegisterRequest): DemoAuthResult

  suspend fun signInLocalAccount(request: DemoSignInRequest): DemoAuthResult
}

data class BookingCreateRequest(
  val spaceId: String,
  val customerId: String,
  val vehicleId: String,
  val bookingType: BookingType,
  val units: Int,
  val startEpochMs: Long,
  val confirmDemoPaymentImmediately: Boolean = true,
  val paymentMethod: DemoPaymentMethod = DemoPaymentMethod.JAZZCASH_DEMO,
  val nowEpochMs: Long = System.currentTimeMillis(),
)

sealed interface BookingMutationResult {
  data class Success(val booking: Booking, val message: String) : BookingMutationResult

  data class Rejected(val reason: String) : BookingMutationResult
}

interface UserProfileRepository {
  fun observeAllUsers(): Flow<List<UserProfile>>
  fun observeUsersByRole(role: UserRole): Flow<List<UserProfile>>
  suspend fun getUserById(userId: String): UserProfile?
  fun observeCustomerProfile(userId: String): Flow<CustomerProfile?>
  fun observeHostProfile(userId: String): Flow<HostProfile?>
  fun observeVehiclesForUser(userId: String): Flow<List<VehicleProfile>>

  suspend fun addVehicleForUser(
    ownerUserId: String,
    vehicleType: VehicleType,
    label: String,
    plateNumber: String,
    makeDefault: Boolean = true,
  ): Result<VehicleProfile>

  suspend fun updateCustomerPreferredCity(userId: String, city: SupportedCity)

  suspend fun updateHostOnboardingProfile(
    userId: String,
    businessAlias: String,
    operatingCities: Set<SupportedCity>,
  ): Result<HostProfile>
}

sealed interface HostListingMutationResult {
  data class Success(val space: ParkingSpace, val message: String) : HostListingMutationResult

  data class Rejected(val reason: String) : HostListingMutationResult
}

sealed interface HostBayMutationResult {
  data class Success(val bay: ParkingBay, val message: String) : HostBayMutationResult

  data class Rejected(val reason: String) : HostBayMutationResult
}

sealed interface HostScheduleMutationResult {
  data class Success(val schedule: AvailabilitySchedule, val message: String) : HostScheduleMutationResult

  data class Rejected(val reason: String) : HostScheduleMutationResult
}

interface ParkingRepository {
  fun observeAllApprovedSpaces(): Flow<List<ParkingSpace>>
  fun observeApprovedSpacesByCity(city: SupportedCity): Flow<List<ParkingSpace>>
  fun observeAllSpacesIncludingPending(): Flow<List<ParkingSpace>>
  fun observeSpacesByHost(hostId: String): Flow<List<ParkingSpace>>
  fun observeBaysForSpace(spaceId: String): Flow<List<ParkingBay>>
  suspend fun getSpaceById(spaceId: String): ParkingSpace?
  suspend fun upsertSpace(space: ParkingSpace)

  suspend fun createOrUpdateListing(
    hostId: String,
    space: ParkingSpace,
    publishImmediately: Boolean,
  ): HostListingMutationResult

  suspend fun addBayToSpace(
    hostId: String,
    spaceId: String,
    bayCode: String,
    supportedVehicles: Set<VehicleType>,
    isCovered: Boolean,
  ): HostBayMutationResult

  suspend fun updateBay(
    hostId: String,
    spaceId: String,
    bay: ParkingBay,
  ): HostBayMutationResult

  suspend fun toggleBayActive(
    hostId: String,
    spaceId: String,
    bayId: String,
    isActive: Boolean,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): HostBayMutationResult

  suspend fun updateListingScheduleAndBlackouts(
    hostId: String,
    spaceId: String,
    schedule: AvailabilitySchedule,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): HostScheduleMutationResult
}

interface BookingRepository {
  fun observeBookingsForCustomer(customerId: String): Flow<List<Booking>>
  fun observeBookingsForHost(hostId: String): Flow<List<Booking>>
  fun observeAllBookings(): Flow<List<Booking>>
  fun observeBookingsForSpace(spaceId: String): Flow<List<Booking>>
  fun observeBookingById(bookingId: String): Flow<Booking?>
  suspend fun getBookingById(bookingId: String): Booking?

  suspend fun createBookingWithAvailabilityCheck(request: BookingCreateRequest): BookingMutationResult

  suspend fun confirmPendingBookingPayment(
    bookingId: String,
    customerId: String,
    paymentMethod: DemoPaymentMethod = DemoPaymentMethod.JAZZCASH_DEMO,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): BookingMutationResult

  suspend fun checkInBooking(
    bookingId: String,
    customerId: String,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): BookingMutationResult

  suspend fun checkOutBooking(
    bookingId: String,
    customerId: String,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): BookingMutationResult

  suspend fun cancelBooking(
    bookingId: String,
    customerId: String,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): BookingMutationResult

  suspend fun hostCheckInBooking(
    bookingId: String,
    hostId: String,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): BookingMutationResult

  suspend fun hostCheckOutBooking(
    bookingId: String,
    hostId: String,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): BookingMutationResult

  suspend fun hostCancelBooking(
    bookingId: String,
    hostId: String,
    reason: String,
    nowEpochMs: Long = System.currentTimeMillis(),
  ): BookingMutationResult

  suspend fun expireStaleBookings(nowEpochMs: Long = System.currentTimeMillis()): Int
}
