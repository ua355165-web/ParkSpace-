package com.example.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.core.config.BookingType
import com.example.core.config.SupportedCity
import com.example.core.config.UserRole
import com.example.core.domain.model.BookingStatus
import com.example.core.domain.model.DemoPaymentMethod
import com.example.core.domain.model.DemoPaymentStatus
import com.example.core.domain.model.HostVerificationStatus
import com.example.core.domain.model.ParkingAmenity
import com.example.core.domain.model.ParkingDayOfWeek
import com.example.core.domain.model.ParkingType
import com.example.core.domain.model.VehicleType

@Entity(tableName = "demo_seed_metadata")
data class DemoSeedMetadataEntity(
  @PrimaryKey val key: String,
  val seedVersion: Int,
  val seededAtEpochMs: Long,
)

@Entity(
  tableName = "demo_users",
  indices = [Index(value = ["emailNormalized", "role"], unique = true)],
)
data class DemoUserEntity(
  @PrimaryKey val id: String,
  val fullName: String,
  val phoneMasked: String,
  val emailMasked: String,
  val role: UserRole,
  val city: SupportedCity,
  val isDemoAccount: Boolean,
  val createdAtEpochMs: Long,
  val emailNormalized: String? = null,
  val passwordHash: String? = null,
  val passwordSalt: String? = null,
  val isSelfRegisteredDemoAccount: Boolean = false,
)

@Entity(tableName = "demo_customer_profiles")
data class DemoCustomerProfileEntity(
  @PrimaryKey val userId: String,
  val preferredCity: SupportedCity,
  val defaultVehicleId: String?,
  val referralCode: String,
)

@Entity(tableName = "demo_host_profiles")
data class DemoHostProfileEntity(
  @PrimaryKey val userId: String,
  val displayBusinessAlias: String,
  val verificationStatus: HostVerificationStatus,
  val operatingCities: Set<SupportedCity>,
  val totalSpacesCount: Int,
  val joinedEpochMs: Long,
  val verificationNote: String? = null,
  val reviewedAtEpochMs: Long? = null,
)

@Entity(tableName = "demo_vehicles")
data class DemoVehicleEntity(
  @PrimaryKey val id: String,
  val ownerUserId: String,
  val vehicleType: VehicleType,
  val label: String,
  val plateNumberMasked: String,
  val isDefault: Boolean,
)

@Entity(tableName = "demo_parking_spaces")
data class DemoParkingSpaceEntity(
  @PrimaryKey val id: String,
  val hostId: String,
  val title: String,
  val description: String,
  val parkingType: ParkingType,
  val city: SupportedCity,
  val areaLabel: String,
  val publicMaskedLatitude: Double,
  val publicMaskedLongitude: Double,
  val maskRadiusMeters: Int,
  val privateGateSummary: String?,
  val privateExactLatitude: Double?,
  val privateExactLongitude: Double?,
  val hourlyRatePkr: Int,
  val dailyRatePkr: Int,
  val monthlyRatePkr: Int?,
  val monthlyEnabled: Boolean,
  val totalBays: Int,
  val availableBays: Int,
  val supportedVehicles: Set<VehicleType>,
  val amenities: Set<ParkingAmenity>,
  val openDays: Set<ParkingDayOfWeek>,
  val openHour: Int,
  val closeHour: Int,
  val is24Hours: Boolean,
  val blackoutDatesIso: Set<String>,
  val isApproved: Boolean,
  val isDemoListing: Boolean,
  val photoTags: List<String> = listOf("Entrance Gate", "Covered Bay"),
  val photoUris: List<String> = emptyList(),
  val moderationNote: String? = null,
  val moderatedAtEpochMs: Long? = null,
  val isRejectedByAdmin: Boolean = false,
)

@Entity(tableName = "demo_parking_bays")
data class DemoParkingBayEntity(
  @PrimaryKey val id: String,
  val spaceId: String,
  val bayCode: String,
  val supportedVehicles: Set<VehicleType>,
  val isCovered: Boolean,
  val isActiveInDemo: Boolean,
)

@Entity(tableName = "demo_bookings")
data class DemoBookingEntity(
  @PrimaryKey val id: String,
  val bookingCode: String,
  val spaceId: String,
  val bayId: String,
  val customerId: String,
  val hostId: String,
  val vehicleId: String,
  val city: SupportedCity,
  val bookingType: BookingType,
  val status: BookingStatus,
  val startEpochMs: Long,
  val endEpochMs: Long,
  val baseAmountPkr: Int,
  val serviceFeePkr: Int,
  val totalAmountPkr: Int,
  val gatePinReleased: Boolean,
  val isDemoRecord: Boolean,
  val demoPaymentStatus: DemoPaymentStatus = DemoPaymentStatus.DEMO_PENDING_HOLD,
  val demoEntryPin: String? = null,
  val checkedInAtEpochMs: Long? = null,
  val checkedOutAtEpochMs: Long? = null,
  val cancelledAtEpochMs: Long? = null,
  val refundAmountPkr: Int? = null,
  val cancellationPolicySummary: String? = null,
  val createdAtEpochMs: Long = 0L,
  val demoPaymentMethod: DemoPaymentMethod? = null,
)
