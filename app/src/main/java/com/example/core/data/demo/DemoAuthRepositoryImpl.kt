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
}
