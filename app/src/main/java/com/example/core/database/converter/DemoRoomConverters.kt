package com.example.core.database.converter

import androidx.room.TypeConverter
import com.example.core.config.BookingType
import com.example.core.config.SupportedCity
import com.example.core.config.UserRole
import com.example.core.domain.model.AdminAuditAction
import com.example.core.domain.model.AdminAuditTargetType
import com.example.core.domain.model.BookingStatus
import com.example.core.domain.model.DemoPaymentMethod
import com.example.core.domain.model.DemoPaymentStatus
import com.example.core.domain.model.DemoPaymentTransactionType
import com.example.core.domain.model.DisputeCategory
import com.example.core.domain.model.DisputeStatus
import com.example.core.domain.model.HostVerificationStatus
import com.example.core.domain.model.ParkSpaceNotificationType
import com.example.core.domain.model.ParkingAmenity
import com.example.core.domain.model.ParkingDayOfWeek
import com.example.core.domain.model.ParkingType
import com.example.core.domain.model.ReferralRewardStatus
import com.example.core.domain.model.VehicleType

class DemoRoomConverters {

  @TypeConverter fun fromUserRole(value: UserRole): String = value.name

  @TypeConverter fun toUserRole(raw: String): UserRole = UserRole.valueOf(raw)

  @TypeConverter fun fromSupportedCity(value: SupportedCity): String = value.name

  @TypeConverter fun toSupportedCity(raw: String): SupportedCity = SupportedCity.valueOf(raw)

  @TypeConverter fun fromBookingType(value: BookingType): String = value.name

  @TypeConverter fun toBookingType(raw: String): BookingType = BookingType.valueOf(raw)

  @TypeConverter fun fromBookingStatus(value: BookingStatus): String = value.name

  @TypeConverter fun toBookingStatus(raw: String): BookingStatus = BookingStatus.valueOf(raw)

  @TypeConverter fun fromDemoPaymentStatus(value: DemoPaymentStatus): String = value.name

  @TypeConverter
  fun toDemoPaymentStatus(raw: String): DemoPaymentStatus = DemoPaymentStatus.valueOf(raw)

  @TypeConverter
  fun fromDemoPaymentMethod(value: DemoPaymentMethod?): String? = value?.name

  @TypeConverter
  fun toDemoPaymentMethod(raw: String?): DemoPaymentMethod? =
    raw?.takeIf { it.isNotBlank() }?.let { DemoPaymentMethod.valueOf(it) }

  @TypeConverter
  fun fromDemoPaymentTransactionType(value: DemoPaymentTransactionType): String = value.name

  @TypeConverter
  fun toDemoPaymentTransactionType(raw: String): DemoPaymentTransactionType =
    DemoPaymentTransactionType.valueOf(raw)

  @TypeConverter fun fromDisputeCategory(value: DisputeCategory): String = value.name

  @TypeConverter fun toDisputeCategory(raw: String): DisputeCategory = DisputeCategory.valueOf(raw)

  @TypeConverter fun fromDisputeStatus(value: DisputeStatus): String = value.name

  @TypeConverter fun toDisputeStatus(raw: String): DisputeStatus = DisputeStatus.valueOf(raw)

  @TypeConverter
  fun fromReferralRewardStatus(value: ReferralRewardStatus): String = value.name

  @TypeConverter
  fun toReferralRewardStatus(raw: String): ReferralRewardStatus = ReferralRewardStatus.valueOf(raw)

  @TypeConverter
  fun fromNotificationType(value: ParkSpaceNotificationType): String = value.name

  @TypeConverter
  fun toNotificationType(raw: String): ParkSpaceNotificationType =
    ParkSpaceNotificationType.valueOf(raw)

  @TypeConverter fun fromAdminAuditAction(value: AdminAuditAction): String = value.name

  @TypeConverter fun toAdminAuditAction(raw: String): AdminAuditAction = AdminAuditAction.valueOf(raw)

  @TypeConverter
  fun fromAdminAuditTargetType(value: AdminAuditTargetType): String = value.name

  @TypeConverter
  fun toAdminAuditTargetType(raw: String): AdminAuditTargetType = AdminAuditTargetType.valueOf(raw)

  @TypeConverter fun fromParkingType(value: ParkingType): String = value.name

  @TypeConverter fun toParkingType(raw: String): ParkingType = ParkingType.valueOf(raw)

  @TypeConverter fun fromVehicleType(value: VehicleType): String = value.name

  @TypeConverter fun toVehicleType(raw: String): VehicleType = VehicleType.valueOf(raw)

  @TypeConverter
  fun fromHostVerificationStatus(value: HostVerificationStatus): String = value.name

  @TypeConverter
  fun toHostVerificationStatus(raw: String): HostVerificationStatus =
    when (raw) {
      "VERIFIED" -> HostVerificationStatus.APPROVED
      "SUSPENDED" -> HostVerificationStatus.REJECTED
      else -> HostVerificationStatus.valueOf(raw)
    }

  @TypeConverter
  fun fromVehicleTypeSet(set: Set<VehicleType>): String =
    set.joinToString(DELIMITER) { it.name }

  @TypeConverter
  fun toVehicleTypeSet(raw: String): Set<VehicleType> =
    if (raw.isBlank()) emptySet()
    else raw.split(DELIMITER).map { VehicleType.valueOf(it) }.toSet()

  @TypeConverter
  fun fromAmenitySet(set: Set<ParkingAmenity>): String =
    set.joinToString(DELIMITER) { it.name }

  @TypeConverter
  fun toAmenitySet(raw: String): Set<ParkingAmenity> =
    if (raw.isBlank()) emptySet()
    else raw.split(DELIMITER).map { ParkingAmenity.valueOf(it) }.toSet()

  @TypeConverter
  fun fromDayOfWeekSet(set: Set<ParkingDayOfWeek>): String =
    set.joinToString(DELIMITER) { it.name }

  @TypeConverter
  fun toDayOfWeekSet(raw: String): Set<ParkingDayOfWeek> =
    if (raw.isBlank()) emptySet()
    else raw.split(DELIMITER).map { ParkingDayOfWeek.valueOf(it) }.toSet()

  @TypeConverter
  fun fromSupportedCitySet(set: Set<SupportedCity>): String =
    set.joinToString(DELIMITER) { it.name }

  @TypeConverter
  fun toSupportedCitySet(raw: String): Set<SupportedCity> =
    if (raw.isBlank()) emptySet()
    else raw.split(DELIMITER).map { SupportedCity.valueOf(it) }.toSet()

  @TypeConverter
  fun fromStringSet(set: Set<String>): String = set.joinToString(DELIMITER)

  @TypeConverter
  fun toStringSet(raw: String): Set<String> =
    if (raw.isBlank()) emptySet() else raw.split(DELIMITER).toSet()

  @TypeConverter
  fun fromStringList(list: List<String>): String = list.joinToString(DELIMITER)

  @TypeConverter
  fun toStringList(raw: String): List<String> =
    if (raw.isBlank()) emptyList() else raw.split(DELIMITER)

  private companion object {
    const val DELIMITER = "|"
  }
}
