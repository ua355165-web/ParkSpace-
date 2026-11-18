package com.example.core.database.mapper

import com.example.core.database.entity.DemoBookingEntity
import com.example.core.database.entity.DemoCustomerProfileEntity
import com.example.core.database.entity.DemoHostProfileEntity
import com.example.core.database.entity.DemoParkingBayEntity
import com.example.core.database.entity.DemoParkingSpaceEntity
import com.example.core.database.entity.DemoUserEntity
import com.example.core.database.entity.DemoVehicleEntity
import com.example.core.domain.model.AvailabilitySchedule
import com.example.core.domain.model.Booking
import com.example.core.domain.model.CustomerProfile
import com.example.core.domain.model.HostProfile
import com.example.core.domain.model.ParkingBay
import com.example.core.domain.model.ParkingLocation
import com.example.core.domain.model.ParkingSpace
import com.example.core.domain.model.UserProfile
import com.example.core.domain.model.VehicleProfile

fun DemoUserEntity.toDomain(): UserProfile =
  UserProfile(
    id = id,
    fullName = fullName,
    phoneMasked = phoneMasked,
    emailMasked = emailMasked,
    role = role,
    city = city,
    isDemoAccount = isDemoAccount,
    createdAtEpochMs = createdAtEpochMs,
    isSelfRegisteredDemoAccount = isSelfRegisteredDemoAccount,
  )

fun UserProfile.toEntity(): DemoUserEntity =
  DemoUserEntity(
    id = id,
    fullName = fullName,
    phoneMasked = phoneMasked,
    emailMasked = emailMasked,
    role = role,
    city = city,
    isDemoAccount = isDemoAccount,
    createdAtEpochMs = createdAtEpochMs,
    isSelfRegisteredDemoAccount = isSelfRegisteredDemoAccount,
  )

fun DemoCustomerProfileEntity.toDomain(): CustomerProfile =
  CustomerProfile(
    userId = userId,
    preferredCity = preferredCity,
    defaultVehicleId = defaultVehicleId,
    referralCode = referralCode,
  )

fun CustomerProfile.toEntity(): DemoCustomerProfileEntity =
  DemoCustomerProfileEntity(
    userId = userId,
    preferredCity = preferredCity,
    defaultVehicleId = defaultVehicleId,
    referralCode = referralCode,
  )

fun DemoHostProfileEntity.toDomain(): HostProfile =
  HostProfile(
    userId = userId,
    displayBusinessAlias = displayBusinessAlias,
    verificationStatus = verificationStatus,
    operatingCities = operatingCities,
    totalSpacesCount = totalSpacesCount,
    joinedEpochMs = joinedEpochMs,
    verificationNote = verificationNote,
    reviewedAtEpochMs = reviewedAtEpochMs,
  )

fun HostProfile.toEntity(): DemoHostProfileEntity =
  DemoHostProfileEntity(
    userId = userId,
    displayBusinessAlias = displayBusinessAlias,
    verificationStatus = verificationStatus,
    operatingCities = operatingCities,
    totalSpacesCount = totalSpacesCount,
    joinedEpochMs = joinedEpochMs,
    verificationNote = verificationNote,
    reviewedAtEpochMs = reviewedAtEpochMs,
  )

fun DemoVehicleEntity.toDomain(): VehicleProfile =
  VehicleProfile(
    id = id,
    ownerUserId = ownerUserId,
    vehicleType = vehicleType,
    label = label,
    plateNumberMasked = plateNumberMasked,
    isDefault = isDefault,
  )

fun VehicleProfile.toEntity(): DemoVehicleEntity =
  DemoVehicleEntity(
    id = id,
    ownerUserId = ownerUserId,
    vehicleType = vehicleType,
    label = label,
    plateNumberMasked = plateNumberMasked,
    isDefault = isDefault,
  )

fun DemoParkingSpaceEntity.toDomain(): ParkingSpace =
  ParkingSpace(
    id = id,
    hostId = hostId,
    title = title,
    description = description,
    parkingType = parkingType,
    location =
      ParkingLocation(
        city = city,
        areaLabel = areaLabel,
        publicMaskedLatitude = publicMaskedLatitude,
        publicMaskedLongitude = publicMaskedLongitude,
        maskRadiusMeters = maskRadiusMeters,
        privateGateSummary = privateGateSummary,
        privateExactLatitude = privateExactLatitude,
        privateExactLongitude = privateExactLongitude,
      ),
    hourlyRatePkr = hourlyRatePkr,
    dailyRatePkr = dailyRatePkr,
    monthlyRatePkr = monthlyRatePkr,
    monthlyEnabled = monthlyEnabled,
    totalBays = totalBays,
    availableBays = availableBays,
    supportedVehicles = supportedVehicles,
    amenities = amenities,
    schedule =
      AvailabilitySchedule(
        openDays = openDays,
        openHour = openHour,
        closeHour = closeHour,
        is24Hours = is24Hours,
        blackoutDatesIso = blackoutDatesIso,
      ),
    isApproved = isApproved,
    isDemoListing = isDemoListing,
    photoTags = photoTags,
    photoUris = photoUris,
    moderationNote = moderationNote,
    moderatedAtEpochMs = moderatedAtEpochMs,
    isRejectedByAdmin = isRejectedByAdmin,
  )

fun ParkingSpace.toEntity(): DemoParkingSpaceEntity =
  DemoParkingSpaceEntity(
    id = id,
    hostId = hostId,
    title = title,
    description = description,
    parkingType = parkingType,
    city = location.city,
    areaLabel = location.areaLabel,
    publicMaskedLatitude = location.publicMaskedLatitude,
    publicMaskedLongitude = location.publicMaskedLongitude,
    maskRadiusMeters = location.maskRadiusMeters,
    privateGateSummary = location.privateGateSummary,
    privateExactLatitude = location.privateExactLatitude,
    privateExactLongitude = location.privateExactLongitude,
    hourlyRatePkr = hourlyRatePkr,
    dailyRatePkr = dailyRatePkr,
    monthlyRatePkr = monthlyRatePkr,
    monthlyEnabled = monthlyEnabled,
    totalBays = totalBays,
    availableBays = availableBays,
    supportedVehicles = supportedVehicles,
    amenities = amenities,
    openDays = schedule.openDays,
    openHour = schedule.openHour,
    closeHour = schedule.closeHour,
    is24Hours = schedule.is24Hours,
    blackoutDatesIso = schedule.blackoutDatesIso,
    isApproved = isApproved,
    isDemoListing = isDemoListing,
    photoTags = photoTags,
    photoUris = photoUris,
    moderationNote = moderationNote,
    moderatedAtEpochMs = moderatedAtEpochMs,
    isRejectedByAdmin = isRejectedByAdmin,
  )

fun DemoParkingBayEntity.toDomain(): ParkingBay =
  ParkingBay(
    id = id,
    spaceId = spaceId,
    bayCode = bayCode,
    supportedVehicles = supportedVehicles,
    isCovered = isCovered,
    isActiveInDemo = isActiveInDemo,
  )

fun ParkingBay.toEntity(): DemoParkingBayEntity =
  DemoParkingBayEntity(
    id = id,
    spaceId = spaceId,
    bayCode = bayCode,
    supportedVehicles = supportedVehicles,
    isCovered = isCovered,
    isActiveInDemo = isActiveInDemo,
  )

fun DemoBookingEntity.toDomain(): Booking =
  Booking(
    id = id,
    bookingCode = bookingCode,
    spaceId = spaceId,
    bayId = bayId,
    customerId = customerId,
    hostId = hostId,
    vehicleId = vehicleId,
    city = city,
    bookingType = bookingType,
    status = status,
    startEpochMs = startEpochMs,
    endEpochMs = endEpochMs,
    baseAmountPkr = baseAmountPkr,
    serviceFeePkr = serviceFeePkr,
    totalAmountPkr = totalAmountPkr,
    gatePinReleased = gatePinReleased,
    isDemoRecord = isDemoRecord,
    demoPaymentStatus = demoPaymentStatus,
    demoEntryPin = demoEntryPin,
    checkedInAtEpochMs = checkedInAtEpochMs,
    checkedOutAtEpochMs = checkedOutAtEpochMs,
    cancelledAtEpochMs = cancelledAtEpochMs,
    refundAmountPkr = refundAmountPkr,
    cancellationPolicySummary = cancellationPolicySummary,
    createdAtEpochMs = if (createdAtEpochMs > 0L) createdAtEpochMs else startEpochMs,
    demoPaymentMethod = demoPaymentMethod,
  )

fun Booking.toEntity(): DemoBookingEntity =
  DemoBookingEntity(
    id = id,
    bookingCode = bookingCode,
    spaceId = spaceId,
    bayId = bayId,
    customerId = customerId,
    hostId = hostId,
    vehicleId = vehicleId,
    city = city,
    bookingType = bookingType,
    status = status,
    startEpochMs = startEpochMs,
    endEpochMs = endEpochMs,
    baseAmountPkr = baseAmountPkr,
    serviceFeePkr = serviceFeePkr,
    totalAmountPkr = totalAmountPkr,
    gatePinReleased = gatePinReleased,
    isDemoRecord = isDemoRecord,
    demoPaymentStatus = demoPaymentStatus,
    demoEntryPin = demoEntryPin,
    checkedInAtEpochMs = checkedInAtEpochMs,
    checkedOutAtEpochMs = checkedOutAtEpochMs,
    cancelledAtEpochMs = cancelledAtEpochMs,
    refundAmountPkr = refundAmountPkr,
    cancellationPolicySummary = cancellationPolicySummary,
    createdAtEpochMs = createdAtEpochMs,
    demoPaymentMethod = demoPaymentMethod,
  )
