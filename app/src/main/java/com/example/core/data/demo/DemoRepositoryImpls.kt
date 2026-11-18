package com.example.core.data.demo

import androidx.room.withTransaction
import com.example.core.config.SupportedCity
import com.example.core.config.UserRole
import com.example.core.database.ParkSpaceDemoDatabase
import com.example.core.database.dao.DemoBookingDao
import com.example.core.database.dao.DemoParkingDao
import com.example.core.database.dao.DemoUserDao
import com.example.core.database.entity.DemoHostProfileEntity
import com.example.core.database.entity.DemoVehicleEntity
import com.example.core.database.mapper.toDomain
import com.example.core.database.mapper.toEntity
import com.example.core.domain.model.Booking
import com.example.core.domain.model.BookingStatus
import com.example.core.domain.model.CustomerProfile
import com.example.core.domain.model.DemoCommissionRule
import com.example.core.domain.model.DemoPaymentMethod
import com.example.core.domain.model.DemoPaymentStatus
import com.example.core.domain.model.HostProfile
import com.example.core.domain.model.HostVerificationStatus
import com.example.core.domain.model.ParkingBay
import com.example.core.domain.model.ParkingSpace
import com.example.core.database.entity.DemoParkingBayEntity
import com.example.core.domain.model.AvailabilitySchedule
import com.example.core.domain.model.ParkingDayOfWeek
import com.example.core.domain.model.ParkingType
import com.example.core.domain.model.UserProfile
import com.example.core.domain.model.VehicleProfile
import com.example.core.domain.model.VehicleType
import com.example.core.domain.repository.BookingCreateRequest
import com.example.core.domain.repository.BookingMutationResult
import com.example.core.domain.repository.BookingRepository
import com.example.core.domain.repository.HostBayMutationResult
import com.example.core.domain.repository.HostListingMutationResult
import com.example.core.domain.repository.HostScheduleMutationResult
import com.example.core.domain.repository.ParkingRepository
import com.example.core.domain.repository.UserProfileRepository
import com.example.feature.booking.domain.BookingAvailabilityValidator
import com.example.feature.booking.domain.BookingCancellationCalculator
import com.example.feature.booking.domain.BookingPricingEngine
import java.util.UUID
import kotlin.math.abs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DemoUserProfileRepositoryImpl(
  private val userDao: DemoUserDao,
  private val database: ParkSpaceDemoDatabase? = null,
) : UserProfileRepository {

  override fun observeAllUsers(): Flow<List<UserProfile>> =
    userDao.observeAllUsers().map { list -> list.map { it.toDomain() } }

  override fun observeUsersByRole(role: UserRole): Flow<List<UserProfile>> =
    userDao.observeUsersByRole(role).map { list -> list.map { it.toDomain() } }

  override suspend fun getUserById(userId: String): UserProfile? =
    userDao.getUserById(userId)?.toDomain()

  override fun observeCustomerProfile(userId: String): Flow<CustomerProfile?> =
    userDao.observeCustomerProfile(userId).map { it?.toDomain() }

  override fun observeHostProfile(userId: String): Flow<HostProfile?> =
    userDao.observeHostProfile(userId).map { it?.toDomain() }

  override fun observeVehiclesForUser(userId: String): Flow<List<VehicleProfile>> =
    userDao.observeVehiclesForUser(userId).map { list -> list.map { it.toDomain() } }

  override suspend fun addVehicleForUser(
    ownerUserId: String,
    vehicleType: VehicleType,
    label: String,
    plateNumber: String,
    makeDefault: Boolean,
  ): Result<VehicleProfile> {
    val cleanLabel = label.trim()
    val cleanPlate = plateNumber.trim().uppercase()
    if (cleanLabel.length < 2) {
      return Result.failure(IllegalArgumentException("Enter a vehicle name (at least 2 characters)."))
    }
    if (cleanPlate.length < 3) {
      return Result.failure(IllegalArgumentException("Enter a valid plate number (e.g. LEA-1234)."))
    }
    val user = userDao.getUserById(ownerUserId)
      ?: return Result.failure(IllegalStateException("Account not found."))
    if (user.role != UserRole.CUSTOMER) {
      return Result.failure(IllegalStateException("Only Customer accounts can register vehicles."))
    }

    val maskedPlate =
      if (cleanPlate.length <= 4) cleanPlate
      else "${cleanPlate.take(3)}•${cleanPlate.takeLast(3)}"

    val vehicle =
      VehicleProfile(
        id = "demo-veh-${UUID.randomUUID().toString().take(8)}",
        ownerUserId = ownerUserId,
        vehicleType = vehicleType,
        label = cleanLabel,
        plateNumberMasked = maskedPlate,
        isDefault = makeDefault,
      )

    val runBlock: suspend () -> Unit = {
      if (makeDefault) {
        userDao.clearDefaultVehicleFlagForUser(ownerUserId)
      }
      userDao.upsertVehicle(vehicle.toEntity())
      val custProfile = userDao.getCustomerProfileOnce(ownerUserId)
      if (custProfile != null && (makeDefault || custProfile.defaultVehicleId == null)) {
        userDao.upsertCustomerProfile(custProfile.copy(defaultVehicleId = vehicle.id))
      }
    }
    if (database != null) {
      database.withTransaction { runBlock() }
    } else {
      runBlock()
    }
    return Result.success(vehicle)
  }

  override suspend fun updateCustomerPreferredCity(userId: String, city: SupportedCity) {
    val runBlock: suspend () -> Unit = {
      userDao.updateUserCity(userId, city)
      val existing = userDao.getCustomerProfileOnce(userId)
      if (existing != null) {
        userDao.upsertCustomerProfile(existing.copy(preferredCity = city))
      }
    }
    if (database != null) {
      database.withTransaction { runBlock() }
    } else {
      runBlock()
    }
  }

  override suspend fun updateHostOnboardingProfile(
    userId: String,
    businessAlias: String,
    operatingCities: Set<SupportedCity>,
  ): Result<HostProfile> {
    val cleanAlias = businessAlias.trim()
    if (cleanAlias.length < 2) {
      return Result.failure(IllegalArgumentException("Business alias must be at least 2 characters."))
    }
    if (operatingCities.isEmpty()) {
      return Result.failure(IllegalArgumentException("Select at least one operating city."))
    }
    val existing = userDao.getHostProfileOnce(userId)
    val updated =
      if (existing != null) {
        existing.toDomain().copy(
          displayBusinessAlias = cleanAlias,
          operatingCities = operatingCities,
        )
      } else {
        HostProfile(
          userId = userId,
          displayBusinessAlias = cleanAlias,
          verificationStatus = HostVerificationStatus.PENDING_REVIEW,
          operatingCities = operatingCities,
          totalSpacesCount = 0,
          joinedEpochMs = System.currentTimeMillis(),
        )
      }
    userDao.upsertHostProfile(updated.toEntity())
    return Result.success(updated)
  }
}

class DemoParkingRepositoryImpl(
  private val parkingDao: DemoParkingDao,
  private val database: ParkSpaceDemoDatabase? = null,
  private val bookingDao: DemoBookingDao? = null,
  private val userDao: DemoUserDao? = null,
) : ParkingRepository {

  constructor(database: ParkSpaceDemoDatabase) :
    this(
      parkingDao = database.parkingDao(),
      database = database,
      bookingDao = database.bookingDao(),
      userDao = database.userDao(),
    )

  override fun observeAllApprovedSpaces(): Flow<List<ParkingSpace>> =
    parkingDao.observeAllApprovedSpaces().map { list -> list.map { it.toDomain() } }

  override fun observeApprovedSpacesByCity(city: SupportedCity): Flow<List<ParkingSpace>> =
    parkingDao.observeApprovedSpacesByCity(city).map { list -> list.map { it.toDomain() } }

  override fun observeAllSpacesIncludingPending(): Flow<List<ParkingSpace>> =
    parkingDao.observeAllSpacesIncludingPending().map { list -> list.map { it.toDomain() } }

  override fun observeSpacesByHost(hostId: String): Flow<List<ParkingSpace>> =
    parkingDao.observeSpacesByHost(hostId).map { list -> list.map { it.toDomain() } }

  override fun observeBaysForSpace(spaceId: String): Flow<List<ParkingBay>> =
    parkingDao.observeBaysForSpace(spaceId).map { list -> list.map { it.toDomain() } }

  override suspend fun getSpaceById(spaceId: String): ParkingSpace? =
    parkingDao.getSpaceById(spaceId)?.toDomain()

  override suspend fun upsertSpace(space: ParkingSpace) {
    parkingDao.upsertSpace(space.toEntity())
  }

  override suspend fun createOrUpdateListing(
    hostId: String,
    space: ParkingSpace,
    publishImmediately: Boolean,
  ): HostListingMutationResult {
    if (space.hostId != hostId) {
      return HostListingMutationResult.Rejected("You do not have permission to manage this listing.")
    }

    val hostProfile = userDao?.getHostProfileOnce(hostId)?.toDomain()
    val isApprovedHost = hostProfile?.verificationStatus == HostVerificationStatus.APPROVED

    if (publishImmediately && !isApprovedHost) {
      val statusName = hostProfile?.verificationStatus?.displayName ?: "PENDING_REVIEW"
      return HostListingMutationResult.Rejected(
        "Cannot publish listing: Host verification status is $statusName. Only APPROVED Hosts can publish live listings. Save as Draft instead."
      )
    }

    val targetApproved = if (publishImmediately && isApprovedHost) true else false

    val runBlock: suspend () -> HostListingMutationResult = runBlockLabel@{
      val existing = parkingDao.getSpaceById(space.id)
      if (existing != null && existing.hostId != hostId) {
        return@runBlockLabel HostListingMutationResult.Rejected(
          "You do not have permission to overwrite another Host's listing."
        )
      }
      val isNew = (existing == null)
      val spaceToSave =
        if (existing != null) {
          space.copy(
            hostId = hostId,
            isApproved = targetApproved,
            isRejectedByAdmin = false,
            moderationNote = if (targetApproved) null else existing.moderationNote.takeIf { !existing.isRejectedByAdmin },
            moderatedAtEpochMs = if (targetApproved) null else existing.moderatedAtEpochMs.takeIf { !existing.isRejectedByAdmin },
            totalBays = existing.totalBays,
            availableBays = existing.availableBays,
          )
        } else {
          space.copy(
            hostId = hostId,
            isApproved = targetApproved,
            isRejectedByAdmin = false,
          )
        }

      parkingDao.upsertSpace(spaceToSave.toEntity())

      if (isNew) {
        val initialBay = DemoParkingBayEntity(
          id = "${space.id}-bay-1",
          spaceId = space.id,
          bayCode = "Bay A-1",
          supportedVehicles = space.supportedVehicles,
          isCovered = (space.parkingType == ParkingType.COVERED_GARAGE),
          isActiveInDemo = true,
        )
        parkingDao.upsertBay(initialBay)

        if (hostProfile != null && userDao != null) {
          userDao.upsertHostProfile(
            hostProfile.copy(totalSpacesCount = hostProfile.totalSpacesCount + 1).toEntity()
          )
        }
      }

      val actionMsg = if (targetApproved) {
        "Listing '${spaceToSave.title}' published successfully! It is now live in Customer Discovery."
      } else {
        "Listing '${spaceToSave.title}' saved as Draft / Pending Review."
      }
      HostListingMutationResult.Success(spaceToSave, actionMsg)
    }

    return if (database != null) {
      database.withTransaction { runBlock() }
    } else {
      runBlock()
    }
  }

  override suspend fun addBayToSpace(
    hostId: String,
    spaceId: String,
    bayCode: String,
    supportedVehicles: Set<VehicleType>,
    isCovered: Boolean,
  ): HostBayMutationResult {
    val cleanCode = bayCode.trim()
    if (cleanCode.length < 2) {
      return HostBayMutationResult.Rejected("Bay code must be at least 2 characters (e.g. B-02).")
    }
    if (supportedVehicles.isEmpty()) {
      return HostBayMutationResult.Rejected("Select at least one supported vehicle type for the bay.")
    }

    val spaceEntity = parkingDao.getSpaceById(spaceId)
      ?: return HostBayMutationResult.Rejected("Parking space not found.")

    if (spaceEntity.hostId != hostId) {
      return HostBayMutationResult.Rejected("You do not have permission to modify bays for this space.")
    }

    val existingBays = parkingDao.getBaysForSpaceOnce(spaceId)
    if (existingBays.any { it.bayCode.equals(cleanCode, ignoreCase = true) }) {
      return HostBayMutationResult.Rejected("Bay code '$cleanCode' already exists in this space.")
    }

    val bayId = "bay-${UUID.randomUUID().toString().take(8)}"
    val newBay = ParkingBay(
      id = bayId,
      spaceId = spaceId,
      bayCode = cleanCode,
      supportedVehicles = supportedVehicles,
      isCovered = isCovered,
      isActiveInDemo = true,
    )

    val runBlock: suspend () -> HostBayMutationResult = {
      parkingDao.upsertBay(newBay.toEntity())
      val allBays = parkingDao.getBaysForSpaceOnce(spaceId)
      val totalCount = allBays.size
      val activeCount = allBays.count { it.isActiveInDemo }

      val updatedSpace = spaceEntity.copy(
        totalBays = totalCount.coerceAtLeast(1),
        availableBays = activeCount.coerceIn(0, totalCount),
      )
      parkingDao.upsertSpace(updatedSpace)
      HostBayMutationResult.Success(newBay, "Parking bay '$cleanCode' added successfully.")
    }

    return if (database != null) {
      database.withTransaction { runBlock() }
    } else {
      runBlock()
    }
  }

  override suspend fun updateBay(
    hostId: String,
    spaceId: String,
    bay: ParkingBay,
  ): HostBayMutationResult {
    if (bay.spaceId != spaceId) {
      return HostBayMutationResult.Rejected("Bay spaceId does not match the requested space.")
    }

    val spaceEntity = parkingDao.getSpaceById(spaceId)
      ?: return HostBayMutationResult.Rejected("Parking space not found.")
    if (spaceEntity.hostId != hostId) {
      return HostBayMutationResult.Rejected("You do not have permission to modify this bay.")
    }

    val existing = parkingDao.getBayById(bay.id)
      ?: return HostBayMutationResult.Rejected("Bay not found.")
    if (existing.spaceId != spaceId) {
      return HostBayMutationResult.Rejected("Bay does not belong to this space.")
    }

    val cleanCode = bay.bayCode.trim()
    if (cleanCode.length < 2) {
      return HostBayMutationResult.Rejected("Bay code must be at least 2 characters.")
    }
    if (bay.supportedVehicles.isEmpty()) {
      return HostBayMutationResult.Rejected("Select at least one supported vehicle type for the bay.")
    }

    val existingBays = parkingDao.getBaysForSpaceOnce(spaceId)
    if (existingBays.any { it.id != bay.id && it.bayCode.equals(cleanCode, ignoreCase = true) }) {
      return HostBayMutationResult.Rejected("Bay code '$cleanCode' already exists in this space.")
    }

    val nowEpochMs = System.currentTimeMillis()
    val bDao = bookingDao
    if (bDao != null) {
      val spaceBookings = bDao.getBookingsForSpaceOnce(spaceId)
      val activeConflicts = spaceBookings.filter { b ->
        b.bayId == bay.id &&
          b.status in Booking.CAPACITY_CONSUMING_STATUSES &&
          b.endEpochMs > nowEpochMs
      }
      if (!bay.isActiveInDemo && activeConflicts.isNotEmpty()) {
        val first = activeConflicts.first()
        return HostBayMutationResult.Rejected(
          "Cannot deactivate bay '${existing.bayCode}': it has ${activeConflicts.size} active or upcoming booking(s) (e.g. ${first.bookingCode})."
        )
      }
      if (userDao != null && activeConflicts.isNotEmpty()) {
        for (conflict in activeConflicts) {
          val vehicle = userDao.getVehicleById(conflict.vehicleId)
          if (vehicle != null && vehicle.vehicleType !in bay.supportedVehicles) {
            return HostBayMutationResult.Rejected(
              "Cannot remove ${vehicle.vehicleType.displayName} compatibility: bay '${existing.bayCode}' has active booking ${conflict.bookingCode}."
            )
          }
        }
      }
    }

    val updatedBay = bay.copy(spaceId = spaceId, bayCode = cleanCode)
    val runBlock: suspend () -> HostBayMutationResult = {
      parkingDao.upsertBay(updatedBay.toEntity())
      val allBays = parkingDao.getBaysForSpaceOnce(spaceId)
      val totalCount = allBays.size
      val activeCount = allBays.count { it.isActiveInDemo }
      val updatedSpace = spaceEntity.copy(
        totalBays = totalCount.coerceAtLeast(1),
        availableBays = activeCount.coerceIn(0, totalCount),
      )
      parkingDao.upsertSpace(updatedSpace)
      HostBayMutationResult.Success(updatedBay, "Bay '$cleanCode' updated.")
    }

    return if (database != null) {
      database.withTransaction { runBlock() }
    } else {
      runBlock()
    }
  }

  override suspend fun toggleBayActive(
    hostId: String,
    spaceId: String,
    bayId: String,
    isActive: Boolean,
    nowEpochMs: Long,
  ): HostBayMutationResult {
    val spaceEntity = parkingDao.getSpaceById(spaceId)
      ?: return HostBayMutationResult.Rejected("Parking space not found.")
    if (spaceEntity.hostId != hostId) {
      return HostBayMutationResult.Rejected("You do not have permission to modify this bay.")
    }

    val bayEntity = parkingDao.getBayById(bayId)
      ?: return HostBayMutationResult.Rejected("Bay not found.")
    if (bayEntity.spaceId != spaceId) {
      return HostBayMutationResult.Rejected("Bay does not belong to this parking space.")
    }

    if (!isActive) {
      val bDao = bookingDao
      if (bDao != null) {
        val spaceBookings = bDao.getBookingsForSpaceOnce(spaceId)
        val activeConflicts = spaceBookings.filter { b ->
          b.bayId == bayId &&
            b.status in Booking.CAPACITY_CONSUMING_STATUSES &&
            b.endEpochMs > nowEpochMs
        }
        if (activeConflicts.isNotEmpty()) {
          val first = activeConflicts.first()
          return HostBayMutationResult.Rejected(
            "Cannot deactivate bay '${bayEntity.bayCode}': it has ${activeConflicts.size} active or upcoming booking(s) (e.g. ${first.bookingCode})."
          )
        }
      }
    }

    val runBlock: suspend () -> HostBayMutationResult = {
      val updatedBay = bayEntity.copy(isActiveInDemo = isActive)
      parkingDao.upsertBay(updatedBay)

      val allBays = parkingDao.getBaysForSpaceOnce(spaceId)
      val totalCount = allBays.size
      val activeCount = allBays.count { it.isActiveInDemo }

      val updatedSpace = spaceEntity.copy(
        availableBays = activeCount.coerceIn(0, totalCount),
      )
      parkingDao.upsertSpace(updatedSpace)

      val msg = if (isActive) {
        "Bay '${bayEntity.bayCode}' is now Active in Demo."
      } else {
        "Bay '${bayEntity.bayCode}' deactivated."
      }
      HostBayMutationResult.Success(updatedBay.toDomain(), msg)
    }

    return if (database != null) {
      database.withTransaction { runBlock() }
    } else {
      runBlock()
    }
  }

  override suspend fun updateListingScheduleAndBlackouts(
    hostId: String,
    spaceId: String,
    schedule: AvailabilitySchedule,
    nowEpochMs: Long,
  ): HostScheduleMutationResult {
    val spaceEntity = parkingDao.getSpaceById(spaceId)
      ?: return HostScheduleMutationResult.Rejected("Parking space not found.")
    if (spaceEntity.hostId != hostId) {
      return HostScheduleMutationResult.Rejected("You do not have permission to modify this space.")
    }

    if (schedule.openDays.isEmpty()) {
      return HostScheduleMutationResult.Rejected("Schedule must contain at least one open day.")
    }
    if (!schedule.is24Hours && schedule.openHour >= schedule.closeHour) {
      return HostScheduleMutationResult.Rejected("Opening hour must be before closing hour.")
    }

    val bDao = bookingDao
    if (bDao != null) {
      val spaceBookings = bDao.getBookingsForSpaceOnce(spaceId)
      val activeBookings = spaceBookings.filter { b ->
        b.status in Booking.CAPACITY_CONSUMING_STATUSES && b.endEpochMs > nowEpochMs
      }

      for (blackoutDate in schedule.blackoutDatesIso) {
        val conflicting = activeBookings.find { b ->
          val startZoned = java.time.Instant.ofEpochMilli(b.startEpochMs)
            .atZone(BookingAvailabilityValidator.PAKISTAN_ZONE).toLocalDate()
          val endZoned = java.time.Instant.ofEpochMilli((b.endEpochMs - 1L).coerceAtLeast(b.startEpochMs))
            .atZone(BookingAvailabilityValidator.PAKISTAN_ZONE).toLocalDate()
          val bDate = java.time.LocalDate.parse(blackoutDate)
          !bDate.isBefore(startZoned) && !bDate.isAfter(endZoned)
        }
        if (conflicting != null) {
          return HostScheduleMutationResult.Rejected(
            "Cannot add blackout date $blackoutDate: Booking ${conflicting.bookingCode} is already confirmed for this date."
          )
        }
      }

      for (b in activeBookings) {
        val startZoned = java.time.Instant.ofEpochMilli(b.startEpochMs)
          .atZone(BookingAvailabilityValidator.PAKISTAN_ZONE)
        val endZoned = java.time.Instant.ofEpochMilli((b.endEpochMs - 1L).coerceAtLeast(b.startEpochMs))
          .atZone(BookingAvailabilityValidator.PAKISTAN_ZONE)

        val day = when (startZoned.dayOfWeek) {
          java.time.DayOfWeek.MONDAY -> ParkingDayOfWeek.MON
          java.time.DayOfWeek.TUESDAY -> ParkingDayOfWeek.TUE
          java.time.DayOfWeek.WEDNESDAY -> ParkingDayOfWeek.WED
          java.time.DayOfWeek.THURSDAY -> ParkingDayOfWeek.THU
          java.time.DayOfWeek.FRIDAY -> ParkingDayOfWeek.FRI
          java.time.DayOfWeek.SATURDAY -> ParkingDayOfWeek.SAT
          java.time.DayOfWeek.SUNDAY -> ParkingDayOfWeek.SUN
        }
        if (day !in schedule.openDays) {
          return HostScheduleMutationResult.Rejected(
            "Cannot close on ${day.shortLabel}: Booking ${b.bookingCode} is already scheduled on this day."
          )
        }

        if (!schedule.is24Hours) {
          if (startZoned.hour < schedule.openHour || endZoned.hour > schedule.closeHour) {
            return HostScheduleMutationResult.Rejected(
              "Cannot adjust hours: Booking ${b.bookingCode} is scheduled during the hours you are closing."
            )
          }
        }
      }
    }

    val updatedSpace = spaceEntity.copy(
      openDays = schedule.openDays,
      openHour = schedule.openHour,
      closeHour = schedule.closeHour,
      is24Hours = schedule.is24Hours,
      blackoutDatesIso = schedule.blackoutDatesIso,
    )
    parkingDao.upsertSpace(updatedSpace)
    return HostScheduleMutationResult.Success(schedule, "Operating schedule and blackouts updated.")
  }
}

class DemoBookingRepositoryImpl(
  private val database: ParkSpaceDemoDatabase?,
  private val bookingDao: DemoBookingDao,
  private val parkingDao: DemoParkingDao?,
  private val userDao: DemoUserDao?,
) : BookingRepository {

  constructor(database: ParkSpaceDemoDatabase) :
    this(
      database = database,
      bookingDao = database.bookingDao(),
      parkingDao = database.parkingDao(),
      userDao = database.userDao(),
    )

  constructor(bookingDao: DemoBookingDao) :
    this(
      database = null,
      bookingDao = bookingDao,
      parkingDao = null,
      userDao = null,
    )

  private suspend fun <T> withOptionalTransaction(block: suspend () -> T): T {
    val db = database
    return if (db != null) db.withTransaction { block() } else block()
  }

  override fun observeBookingsForCustomer(customerId: String): Flow<List<Booking>> =
    bookingDao.observeBookingsForCustomer(customerId).map { list -> list.map { it.toDomain() } }

  override fun observeBookingsForHost(hostId: String): Flow<List<Booking>> =
    bookingDao.observeBookingsForHost(hostId).map { list -> list.map { it.toDomain() } }

  override fun observeAllBookings(): Flow<List<Booking>> =
    bookingDao.observeAllBookings().map { list -> list.map { it.toDomain() } }

  override fun observeBookingsForSpace(spaceId: String): Flow<List<Booking>> =
    bookingDao.observeBookingsForSpace(spaceId).map { list -> list.map { it.toDomain() } }

  override fun observeBookingById(bookingId: String): Flow<Booking?> =
    bookingDao.observeBookingById(bookingId).map { it?.toDomain() }

  override suspend fun getBookingById(bookingId: String): Booking? =
    bookingDao.getBookingById(bookingId)?.toDomain()

  override suspend fun createBookingWithAvailabilityCheck(
    request: BookingCreateRequest,
  ): BookingMutationResult =
    withOptionalTransaction {
      val pDao = parkingDao
        ?: return@withOptionalTransaction BookingMutationResult.Rejected("Database not configured.")
      val uDao = userDao
        ?: return@withOptionalTransaction BookingMutationResult.Rejected("Database not configured.")
      val space = pDao.getSpaceById(request.spaceId)?.toDomain()
        ?: return@withOptionalTransaction BookingMutationResult.Rejected("Parking space not found.")

      val vehicle = uDao.getVehicleById(request.vehicleId)?.toDomain()
        ?: return@withOptionalTransaction BookingMutationResult.Rejected("Selected vehicle not found.")

      if (vehicle.ownerUserId != request.customerId) {
        return@withOptionalTransaction BookingMutationResult.Rejected(
          "Selected vehicle does not belong to the active Customer profile."
        )
      }

      val commissionRule =
        database?.adminDao()?.getCommissionConfigOnce()?.toDomain() ?: DemoCommissionRule.DEFAULT
      val pricing =
        BookingPricingEngine.calculate(space, request.bookingType, request.units, commissionRule)
          .getOrElse { err ->
            return@withOptionalTransaction BookingMutationResult.Rejected(
              err.message ?: "Invalid booking duration or pricing configuration."
            )
          }

      val endEpochMs = request.startEpochMs + pricing.durationMillis
      val bays = pDao.getBaysForSpaceOnce(space.id).map { it.toDomain() }
      val existingForSpace = bookingDao.getBookingsForSpaceOnce(space.id).map { it.toDomain() }

      val availability =
        BookingAvailabilityValidator.validate(
          space = space,
          bays = bays,
          existingBookingsForSpace = existingForSpace,
          vehicle = vehicle,
          bookingType = request.bookingType,
          startEpochMs = request.startEpochMs,
          endEpochMs = endEpochMs,
          nowEpochMs = request.nowEpochMs,
          customerId = request.customerId,
        )

      if (!availability.isAvailable || availability.assignedBayId == null) {
        return@withOptionalTransaction BookingMutationResult.Rejected(
          availability.blockingReason ?: "Selected slot is unavailable."
        )
      }

      val confirmed = request.confirmDemoPaymentImmediately
      if (confirmed && request.paymentMethod == DemoPaymentMethod.DEMO_CREDIT_WALLET && database != null) {
        val availableCredit =
          DemoMarketplaceEvents.getAvailableDemoCreditPkr(database, request.customerId)
        if (availableCredit < pricing.totalAmountPkr) {
          return@withOptionalTransaction BookingMutationResult.Rejected(
            "Insufficient Demo Credits (Rs. $availableCredit available, Rs. ${pricing.totalAmountPkr} required). Select another simulated payment method."
          )
        }
      }

      val bookingId = "demo-bkg-${UUID.randomUUID().toString().take(8)}"
      val referenceSuffix =
        abs((bookingId + request.nowEpochMs.toString()).hashCode()) % 9000 + 1000
      val bookingCode = "PS-DEMO-$referenceSuffix"

      val status = if (confirmed) BookingStatus.CONFIRMED else BookingStatus.PENDING_PAYMENT
      val demoPin = if (confirmed) generateSimulatedDemoPin(bookingCode, space.id) else null

      val booking =
        Booking(
          id = bookingId,
          bookingCode = bookingCode,
          spaceId = space.id,
          bayId = availability.assignedBayId,
          customerId = request.customerId,
          hostId = space.hostId,
          vehicleId = vehicle.id,
          city = space.location.city,
          bookingType = request.bookingType,
          status = status,
          startEpochMs = request.startEpochMs,
          endEpochMs = endEpochMs,
          baseAmountPkr = pricing.baseAmountPkr,
          serviceFeePkr = pricing.serviceFeePkr,
          totalAmountPkr = pricing.totalAmountPkr,
          gatePinReleased = confirmed,
          isDemoRecord = true,
          demoPaymentStatus =
            if (confirmed) DemoPaymentStatus.DEMO_PAID_ESCROW_HOLD
            else DemoPaymentStatus.DEMO_PENDING_HOLD,
          demoEntryPin = demoPin,
          createdAtEpochMs = request.nowEpochMs,
          demoPaymentMethod = if (confirmed) request.paymentMethod else null,
        )

      bookingDao.insertBooking(booking.toEntity())
      DemoMarketplaceEvents.onBookingCreated(database, booking, space.title, request.nowEpochMs)
      BookingMutationResult.Success(
        booking = booking,
        message =
          if (confirmed) {
            "Demo booking confirmed ($bookingCode) via ${request.paymentMethod.displayName}. Exact gate details & simulated Demo PIN unlocked."
          } else {
            "Demo booking saved as Pending Payment ($bookingCode). Gate PIN remains locked until confirmed."
          },
      )
    }

  override suspend fun confirmPendingBookingPayment(
    bookingId: String,
    customerId: String,
    paymentMethod: DemoPaymentMethod,
    nowEpochMs: Long,
  ): BookingMutationResult =
    withOptionalTransaction {
      val current = bookingDao.getBookingById(bookingId)?.toDomain()
        ?: return@withOptionalTransaction BookingMutationResult.Rejected("Booking not found.")

      if (current.customerId != customerId) {
        return@withOptionalTransaction BookingMutationResult.Rejected(
          "You do not have permission to update this booking."
        )
      }
      if (current.status != BookingStatus.PENDING_PAYMENT) {
        return@withOptionalTransaction BookingMutationResult.Rejected(
          "Only Pending Payment bookings can complete Demo payment."
        )
      }
      if (nowEpochMs > current.endEpochMs) {
        val expired = current.copy(status = BookingStatus.EXPIRED, gatePinReleased = false, demoEntryPin = null)
        bookingDao.updateBooking(expired.toEntity())
        return@withOptionalTransaction BookingMutationResult.Rejected(
          "Booking window has already passed; marked as Expired."
        )
      }
      if (paymentMethod == DemoPaymentMethod.DEMO_CREDIT_WALLET && database != null) {
        val availableCredit =
          DemoMarketplaceEvents.getAvailableDemoCreditPkr(database, customerId)
        if (availableCredit < current.totalAmountPkr) {
          return@withOptionalTransaction BookingMutationResult.Rejected(
            "Insufficient Demo Credits (Rs. $availableCredit available, Rs. ${current.totalAmountPkr} required). Select another simulated payment method."
          )
        }
      }

      val confirmed =
        current.copy(
          status = BookingStatus.CONFIRMED,
          demoPaymentStatus = DemoPaymentStatus.DEMO_PAID_ESCROW_HOLD,
          gatePinReleased = true,
          demoEntryPin = current.demoEntryPin ?: generateSimulatedDemoPin(current.bookingCode, current.spaceId),
          demoPaymentMethod = paymentMethod,
        )
      bookingDao.updateBooking(confirmed.toEntity())
      val spaceTitle = parkingDao?.getSpaceById(current.spaceId)?.title ?: "Parking Space"
      DemoMarketplaceEvents.onBookingPaymentConfirmed(database, confirmed, spaceTitle, nowEpochMs)
      BookingMutationResult.Success(
        booking = confirmed,
        message = "Simulated Demo payment completed via ${paymentMethod.displayName}. Gate PIN unlocked (Demo-only).",
      )
    }

  override suspend fun checkInBooking(
    bookingId: String,
    customerId: String,
    nowEpochMs: Long,
  ): BookingMutationResult =
    withOptionalTransaction {
      val current = bookingDao.getBookingById(bookingId)?.toDomain()
        ?: return@withOptionalTransaction BookingMutationResult.Rejected("Booking not found.")

      if (current.customerId != customerId) {
        return@withOptionalTransaction BookingMutationResult.Rejected(
          "You do not have permission to check in this booking."
        )
      }
      if (current.status != BookingStatus.CONFIRMED) {
        return@withOptionalTransaction BookingMutationResult.Rejected(
          "Check-in requires a Confirmed booking (current: ${current.status.displayName})."
        )
      }
      if (nowEpochMs > current.endEpochMs) {
        val expired =
          current.copy(
            status = BookingStatus.EXPIRED,
            gatePinReleased = false,
            demoEntryPin = null,
          )
        bookingDao.updateBooking(expired.toEntity())
        return@withOptionalTransaction BookingMutationResult.Rejected(
          "Reservation window has expired without check-in; status updated to Expired."
        )
      }

      val updated =
        current.copy(
          status = BookingStatus.CHECKED_IN,
          checkedInAtEpochMs = nowEpochMs,
          gatePinReleased = true,
          demoEntryPin = current.demoEntryPin ?: generateSimulatedDemoPin(current.bookingCode, current.spaceId),
        )
      bookingDao.updateBooking(updated.toEntity())
      DemoMarketplaceEvents.onBookingCheckedIn(database, updated, byHost = false, nowEpochMs = nowEpochMs)
      BookingMutationResult.Success(
        booking = updated,
        message = "Simulated Demo check-in recorded.",
      )
    }

  override suspend fun checkOutBooking(
    bookingId: String,
    customerId: String,
    nowEpochMs: Long,
  ): BookingMutationResult =
    withOptionalTransaction {
      val current = bookingDao.getBookingById(bookingId)?.toDomain()
        ?: return@withOptionalTransaction BookingMutationResult.Rejected("Booking not found.")

      if (current.customerId != customerId) {
        return@withOptionalTransaction BookingMutationResult.Rejected(
          "You do not have permission to check out this booking."
        )
      }
      if (current.status != BookingStatus.CHECKED_IN) {
        return@withOptionalTransaction BookingMutationResult.Rejected(
          "Check-out is only available after Demo check-in."
        )
      }

      val overstayMinutes = ((nowEpochMs - current.endEpochMs) / 60_000L).coerceAtLeast(0L)
      val updated =
        current.copy(
          status = BookingStatus.CHECKED_OUT,
          checkedOutAtEpochMs = nowEpochMs,
          demoPaymentStatus = DemoPaymentStatus.DEMO_SETTLED,
        )
      bookingDao.updateBooking(updated.toEntity())
      DemoMarketplaceEvents.onBookingCheckedOut(database, updated, nowEpochMs)

      val msg =
        if (overstayMinutes > 0L) {
          "Simulated Demo check-out recorded with ${overstayMinutes}m overstay flagged for review."
        } else {
          "Simulated Demo check-out complete. Demo payment marked settled."
        }
      BookingMutationResult.Success(booking = updated, message = msg)
    }

  override suspend fun cancelBooking(
    bookingId: String,
    customerId: String,
    nowEpochMs: Long,
  ): BookingMutationResult =
    withOptionalTransaction {
      val current = bookingDao.getBookingById(bookingId)?.toDomain()
        ?: return@withOptionalTransaction BookingMutationResult.Rejected("Booking not found.")

      if (current.customerId != customerId) {
        return@withOptionalTransaction BookingMutationResult.Rejected(
          "You do not have permission to cancel this booking."
        )
      }

      val space = parkingDao?.getSpaceById(current.spaceId)?.toDomain()
      val quote = BookingCancellationCalculator.calculate(current, space, nowEpochMs)
      if (!quote.isEligibleToCancel) {
        return@withOptionalTransaction BookingMutationResult.Rejected(quote.policyRuleExplanation)
      }

      val updated =
        current.copy(
          status = BookingStatus.CANCELLED,
          gatePinReleased = false,
          demoEntryPin = null,
          demoPaymentStatus = quote.resultingPaymentStatus,
          cancelledAtEpochMs = nowEpochMs,
          refundAmountPkr = quote.refundAmountPkr,
          cancellationPolicySummary = quote.policyRuleExplanation,
        )
      bookingDao.updateBooking(updated.toEntity())
      DemoMarketplaceEvents.onBookingCancelled(database, updated, byHost = false, nowEpochMs = nowEpochMs)
      BookingMutationResult.Success(
        booking = updated,
        message =
          "Booking cancelled (${quote.refundPercentageLabel} Demo refund: Rs. ${quote.refundAmountPkr}).",
      )
    }

  override suspend fun hostCheckInBooking(
    bookingId: String,
    hostId: String,
    nowEpochMs: Long,
  ): BookingMutationResult =
    withOptionalTransaction {
      val current = bookingDao.getBookingById(bookingId)?.toDomain()
        ?: return@withOptionalTransaction BookingMutationResult.Rejected("Booking not found.")

      if (current.hostId != hostId) {
        return@withOptionalTransaction BookingMutationResult.Rejected(
          "You do not have permission to check in bookings for another Host's property."
        )
      }
      if (current.status != BookingStatus.CONFIRMED) {
        return@withOptionalTransaction BookingMutationResult.Rejected(
          "Check-in requires a Confirmed booking (current status: ${current.status.displayName})."
        )
      }

      val updated = current.copy(
        status = BookingStatus.CHECKED_IN,
        checkedInAtEpochMs = nowEpochMs,
        gatePinReleased = true,
      )
      bookingDao.updateBooking(updated.toEntity())
      DemoMarketplaceEvents.onBookingCheckedIn(database, updated, byHost = true, nowEpochMs = nowEpochMs)
      BookingMutationResult.Success(
        booking = updated,
        message = "Customer check-in recorded for booking ${current.bookingCode}.",
      )
    }

  override suspend fun hostCheckOutBooking(
    bookingId: String,
    hostId: String,
    nowEpochMs: Long,
  ): BookingMutationResult =
    withOptionalTransaction {
      val current = bookingDao.getBookingById(bookingId)?.toDomain()
        ?: return@withOptionalTransaction BookingMutationResult.Rejected("Booking not found.")

      if (current.hostId != hostId) {
        return@withOptionalTransaction BookingMutationResult.Rejected(
          "You do not have permission to check out bookings for another Host's property."
        )
      }
      if (current.status != BookingStatus.CHECKED_IN) {
        return@withOptionalTransaction BookingMutationResult.Rejected(
          "Check-out is only available for Checked In bookings (current status: ${current.status.displayName})."
        )
      }

      val updated = current.copy(
        status = BookingStatus.CHECKED_OUT,
        checkedOutAtEpochMs = nowEpochMs,
        demoPaymentStatus = DemoPaymentStatus.DEMO_SETTLED,
      )
      bookingDao.updateBooking(updated.toEntity())
      DemoMarketplaceEvents.onBookingCheckedOut(database, updated, nowEpochMs)
      BookingMutationResult.Success(
        booking = updated,
        message = "Customer checked out (${current.bookingCode}). Demo payment marked Settled.",
      )
    }

  override suspend fun hostCancelBooking(
    bookingId: String,
    hostId: String,
    reason: String,
    nowEpochMs: Long,
  ): BookingMutationResult =
    withOptionalTransaction {
      val current = bookingDao.getBookingById(bookingId)?.toDomain()
        ?: return@withOptionalTransaction BookingMutationResult.Rejected("Booking not found.")

      if (current.hostId != hostId) {
        return@withOptionalTransaction BookingMutationResult.Rejected(
          "You do not have permission to cancel bookings for another Host."
        )
      }
      if (current.status !in setOf(BookingStatus.CONFIRMED, BookingStatus.PENDING_PAYMENT, BookingStatus.CHECKED_IN)) {
        return@withOptionalTransaction BookingMutationResult.Rejected(
          "Cannot cancel booking with status: ${current.status.displayName}."
        )
      }

      val cleanReason = reason.trim().ifEmpty { "Host property maintenance/unavailable" }
      val updated = current.copy(
        status = BookingStatus.CANCELLED,
        cancelledAtEpochMs = nowEpochMs,
        gatePinReleased = false,
        demoEntryPin = null,
        demoPaymentStatus = DemoPaymentStatus.DEMO_FULL_REFUND,
        refundAmountPkr = current.totalAmountPkr,
        cancellationPolicySummary = "Host cancelled: $cleanReason (100% Demo refund Rs. ${current.totalAmountPkr})",
      )
      bookingDao.updateBooking(updated.toEntity())
      DemoMarketplaceEvents.onBookingCancelled(database, updated, byHost = true, nowEpochMs = nowEpochMs)
      BookingMutationResult.Success(
        booking = updated,
        message = "Booking cancelled by Host. Full Demo refund (Rs. ${current.totalAmountPkr}) issued to customer.",
      )
    }

  override suspend fun expireStaleBookings(nowEpochMs: Long): Int =
    withOptionalTransaction {
      val candidates = bookingDao.getStaleExpirableBookingsOnce(nowEpochMs)
      var expiredCount = 0
      for (entity in candidates) {
        val updated =
          entity.toDomain().copy(
            status = BookingStatus.EXPIRED,
            gatePinReleased = false,
            demoEntryPin = null,
          )
        bookingDao.updateBooking(updated.toEntity())
        expiredCount++
      }
      expiredCount
    }

  private fun generateSimulatedDemoPin(bookingCode: String, spaceId: String): String {
    val numeric = (abs((bookingCode + spaceId).hashCode()) % 900_000) + 100_000
    return "DEMO-$numeric"
  }
}
