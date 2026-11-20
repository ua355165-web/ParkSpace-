package com.example.core.data.demo

import androidx.room.withTransaction
import com.example.core.config.UserRole
import com.example.core.database.ParkSpaceDemoDatabase
import com.example.core.database.entity.DemoCustomerProfileEntity
import com.example.core.database.entity.DemoHostProfileEntity
import com.example.core.database.entity.DemoUserEntity
import com.example.core.database.mapper.toDomain
import com.example.core.domain.model.HostVerificationStatus
import com.example.core.domain.repository.DemoAuthRepository
import com.example.core.domain.repository.DemoAuthResult
import com.example.core.domain.repository.DemoRegisterRequest
import com.example.core.domain.repository.DemoSignInRequest
import com.example.feature.auth.domain.DemoAuthValidator
import com.example.feature.auth.domain.DemoPasswordHasher
import com.example.feature.auth.domain.Pbkdf2DemoPasswordHasher
import java.util.UUID

class DemoAuthRepositoryImpl(
  private val database: ParkSpaceDemoDatabase,
  private val passwordHasher: DemoPasswordHasher = Pbkdf2DemoPasswordHasher(),
  private val nowProvider: () -> Long = { System.currentTimeMillis() },
) : DemoAuthRepository {

  private val userDao = database.userDao()

  override suspend fun registerLocalAccount(request: DemoRegisterRequest): DemoAuthResult {
    val validationError =
      DemoAuthValidator.validateRegistration(
        role = request.role,
        fullName = request.fullName,
        email = request.email,
        password = request.password,
        confirmPassword = request.confirmPassword,
      )
    if (validationError != null) {
      return DemoAuthResult.Rejected(validationError)
    }

    val cleanName = request.fullName.trim()
    val cleanEmail = request.email.trim()
    val normalizedEmail = DemoAuthValidator.normalizeEmail(cleanEmail)

    return database.withTransaction {
      val duplicate = userDao.findByEmailAndRole(normalizedEmail, request.role)
      if (duplicate != null) {
        return@withTransaction DemoAuthResult.Rejected(
          "A ${request.role.displayName} Demo account with this email already exists. Please sign in."
        )
      }

      val saltedHash = passwordHasher.hash(request.password)
      val userId =
        "demo-acct-${request.role.name.lowercase()}-${UUID.randomUUID().toString().take(8)}"
      val nowMs = nowProvider()

      val userEntity =
        DemoUserEntity(
          id = userId,
          fullName = cleanName,
          phoneMasked = "Local Demo Account",
          emailMasked = cleanEmail,
          role = request.role,
          city = request.preferredCity,
          isDemoAccount = true,
          createdAtEpochMs = nowMs,
          emailNormalized = normalizedEmail,
          passwordHash = saltedHash.hashEncoded,
          passwordSalt = saltedHash.saltBase64,
          isSelfRegisteredDemoAccount = true,
        )
      userDao.insertUser(userEntity)

      when (request.role) {
        UserRole.CUSTOMER -> {
          val referral = "PK${userId.takeLast(6).uppercase()}"
          userDao.upsertCustomerProfile(
            DemoCustomerProfileEntity(
              userId = userId,
              preferredCity = request.preferredCity,
              defaultVehicleId = null,
              referralCode = referral,
            )
          )
        }
        UserRole.HOST -> {
          val alias = request.businessAlias.trim().ifBlank { "$cleanName Parking" }
          userDao.upsertHostProfile(
            DemoHostProfileEntity(
              userId = userId,
              displayBusinessAlias = alias,
              verificationStatus = HostVerificationStatus.PENDING_REVIEW,
              operatingCities = setOf(request.preferredCity),
              totalSpacesCount = 0,
              joinedEpochMs = nowMs,
            )
          )
        }
        UserRole.SUPER_ADMIN -> {
          return@withTransaction DemoAuthResult.Rejected(
            "Private Super Admin accounts cannot be created via public registration."
          )
        }
      }

      DemoAuthResult.Success(
        user = userEntity.toDomain(),
        message =
          if (request.role == UserRole.HOST) {
            "Local Demo Host account created (Verification: Pending Review)."
          } else {
            "Local Demo Customer account created."
          },
      )
    }
  }

  override suspend fun signInLocalAccount(request: DemoSignInRequest): DemoAuthResult {
    val validationError =
      DemoAuthValidator.validateSignIn(
        role = request.role,
        email = request.email,
        password = request.password,
      )
    if (validationError != null) {
      return DemoAuthResult.Rejected(validationError)
    }

    val normalizedEmail = DemoAuthValidator.normalizeEmail(request.email)
    val userEntity = userDao.findByEmailAndRole(normalizedEmail, request.role)

    if (
      userEntity == null ||
        !userEntity.isSelfRegisteredDemoAccount ||
        userEntity.passwordHash.isNullOrBlank() ||
        userEntity.passwordSalt.isNullOrBlank()
    ) {
      return DemoAuthResult.Rejected(
        "No registered local ${request.role.displayName} account matched this email and password."
      )
    }

    val passwordMatches =
      passwordHasher.verify(
        password = request.password,
        saltBase64 = userEntity.passwordSalt,
        expectedHashEncoded = userEntity.passwordHash,
      )

    if (!passwordMatches) {
      return DemoAuthResult.Rejected("Incorrect password. Please check your credentials and retry.")
    }

    return DemoAuthResult.Success(
      user = userEntity.toDomain(),
      message = "Signed in as ${userEntity.fullName}.",
    )
  }

  override suspend fun hasAccountForEmailAndRole(email: String, role: UserRole): Boolean {
    if (!role.allowsPublicRegistration || role == UserRole.SUPER_ADMIN) return false
    val normalized = DemoAuthValidator.normalizeEmail(email)
    if (normalized.isBlank()) return false
    val entity = userDao.findByEmailAndRole(normalized, role)
    return entity != null && entity.isDemoAccount && entity.isSelfRegisteredDemoAccount
  }

  override suspend fun activateOrSwitchCompanionRole(
    currentSignedInUserId: String,
    targetRole: UserRole,
    businessAlias: String,
  ): DemoAuthResult {
    if (!targetRole.allowsPublicRegistration || targetRole == UserRole.SUPER_ADMIN) {
      return DemoAuthResult.Rejected(
        "Private Super Admin accounts cannot be activated via public Dual-Role access."
      )
    }

    val currentUser = userDao.getUserById(currentSignedInUserId)
    val normalizedEmail = currentUser?.emailNormalized
    val currentHash = currentUser?.passwordHash
    val currentSalt = currentUser?.passwordSalt
    if (
      currentUser == null ||
        !currentUser.isDemoAccount ||
        !currentUser.isSelfRegisteredDemoAccount ||
        currentUser.role == UserRole.SUPER_ADMIN ||
        normalizedEmail.isNullOrBlank() ||
        currentHash.isNullOrBlank() ||
        currentSalt.isNullOrBlank()
    ) {
      return DemoAuthResult.Rejected(
        "Sign in with a valid Customer or Host Demo account first to switch or activate a companion role."
      )
    }

    if (currentUser.role == targetRole) {
      return DemoAuthResult.Success(
        user = currentUser.toDomain(),
        message = "Already active as ${targetRole.displayName}.",
      )
    }

    return database.withTransaction {
      val existingCompanion = userDao.findByEmailAndRole(normalizedEmail, targetRole)
      if (existingCompanion != null) {
        if (
          !existingCompanion.isDemoAccount ||
            !existingCompanion.isSelfRegisteredDemoAccount ||
            existingCompanion.passwordHash.isNullOrBlank() ||
            existingCompanion.passwordSalt.isNullOrBlank()
        ) {
          return@withTransaction DemoAuthResult.Rejected(
            "Existing ${targetRole.displayName} record is incompatible with local Dual-Role switching."
          )
        }
        return@withTransaction DemoAuthResult.Success(
          user = existingCompanion.toDomain(),
          message = "Switched to your ${targetRole.displayName} role (${existingCompanion.emailMasked}).",
        )
      }

      val newUserId =
        "demo-acct-${targetRole.name.lowercase()}-${UUID.randomUUID().toString().take(8)}"
      val nowMs = nowProvider()
      val newEntity =
        DemoUserEntity(
          id = newUserId,
          fullName = currentUser.fullName,
          phoneMasked = "Local Demo Account",
          emailMasked = currentUser.emailMasked,
          role = targetRole,
          city = currentUser.city,
          isDemoAccount = true,
          createdAtEpochMs = nowMs,
          emailNormalized = normalizedEmail,
          passwordHash = currentHash,
          passwordSalt = currentSalt,
          isSelfRegisteredDemoAccount = true,
        )
      userDao.insertUser(newEntity)

      when (targetRole) {
        UserRole.CUSTOMER -> {
          val referral = "PK${newUserId.takeLast(6).uppercase()}"
          userDao.upsertCustomerProfile(
            DemoCustomerProfileEntity(
              userId = newUserId,
              preferredCity = currentUser.city,
              defaultVehicleId = null,
              referralCode = referral,
            )
          )
        }
        UserRole.HOST -> {
          val alias = businessAlias.trim().ifBlank { "${currentUser.fullName} Parking" }
          userDao.upsertHostProfile(
            DemoHostProfileEntity(
              userId = newUserId,
              displayBusinessAlias = alias,
              verificationStatus = HostVerificationStatus.PENDING_REVIEW,
              operatingCities = setOf(currentUser.city),
              totalSpacesCount = 0,
              joinedEpochMs = nowMs,
            )
          )
        }
        UserRole.SUPER_ADMIN -> {
          return@withTransaction DemoAuthResult.Rejected(
            "Private Super Admin accounts cannot be activated via public Dual-Role access."
          )
        }
      }

      DemoAuthResult.Success(
        user = newEntity.toDomain(),
        message =
          if (targetRole == UserRole.HOST) {
            "Host role activated for ${currentUser.emailMasked} (Verification: Pending Review)."
          } else {
            "Customer role activated for ${currentUser.emailMasked}."
          },
      )
    }
  }
}
