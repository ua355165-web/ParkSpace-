package com.example.core.domain.model

import com.example.core.config.SupportedCity
import com.example.core.config.UserRole

enum class VehicleType(val displayName: String) {
  BIKE("Motorbike"),
  CAR("Car / Sedan"),
  SUV("SUV / 4x4"),
}

enum class HostVerificationStatus(
  val displayName: String,
  val canPublishListings: Boolean,
) {
  PENDING_REVIEW("Pending Review", canPublishListings = false),
  APPROVED("Approved Host", canPublishListings = true),
  REJECTED("Verification Rejected", canPublishListings = false),
}

data class UserProfile(
  val id: String,
  val fullName: String,
  val phoneMasked: String,
  val emailMasked: String,
  val role: UserRole,
  val city: SupportedCity,
  val isDemoAccount: Boolean,
  val createdAtEpochMs: Long,
  val isSelfRegisteredDemoAccount: Boolean = false,
) {
  init {
    require(id.isNotBlank()) { "UserProfile.id must not be blank" }
    require(fullName.trim().length >= 2) { "UserProfile.fullName must be at least 2 characters" }
    require(phoneMasked.isNotBlank()) { "UserProfile.phoneMasked must not be blank" }
    require(emailMasked.contains("@")) { "UserProfile.emailMasked must contain @" }
    require(createdAtEpochMs > 0L) { "UserProfile.createdAtEpochMs must be positive" }
  }
}

data class CustomerProfile(
  val userId: String,
  val preferredCity: SupportedCity,
  val defaultVehicleId: String?,
  val referralCode: String,
) {
  init {
    require(userId.isNotBlank()) { "CustomerProfile.userId must not be blank" }
    require(referralCode.trim().length >= 4) { "CustomerProfile.referralCode must be at least 4 chars" }
  }
}

data class HostProfile(
  val userId: String,
  val displayBusinessAlias: String,
  val verificationStatus: HostVerificationStatus,
  val operatingCities: Set<SupportedCity>,
  val totalSpacesCount: Int,
  val joinedEpochMs: Long,
  val verificationNote: String? = null,
  val reviewedAtEpochMs: Long? = null,
) {
  init {
    require(userId.isNotBlank()) { "HostProfile.userId must not be blank" }
    require(displayBusinessAlias.isNotBlank()) { "HostProfile.displayBusinessAlias must not be blank" }
    require(operatingCities.isNotEmpty()) { "HostProfile must operate in at least 1 city" }
    require(totalSpacesCount >= 0) { "HostProfile.totalSpacesCount cannot be negative" }
    require(joinedEpochMs > 0L) { "HostProfile.joinedEpochMs must be positive" }
  }
}

data class VehicleProfile(
  val id: String,
  val ownerUserId: String,
  val vehicleType: VehicleType,
  val label: String,
  val plateNumberMasked: String,
  val isDefault: Boolean,
) {
  init {
    require(id.isNotBlank()) { "VehicleProfile.id must not be blank" }
    require(ownerUserId.isNotBlank()) { "VehicleProfile.ownerUserId must not be blank" }
    require(label.isNotBlank()) { "VehicleProfile.label must not be blank" }
    require(plateNumberMasked.isNotBlank()) { "VehicleProfile.plateNumberMasked must not be blank" }
  }
}
