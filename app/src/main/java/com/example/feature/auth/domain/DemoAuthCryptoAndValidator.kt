package com.example.feature.auth.domain

import com.example.core.config.UserRole
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import java.util.Base64
import java.util.Locale
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class SaltedPasswordHash(
  val saltBase64: String,
  val hashEncoded: String,
)

interface DemoPasswordHasher {
  fun hash(password: String): SaltedPasswordHash

  fun verify(password: String, saltBase64: String, expectedHashEncoded: String): Boolean
}

/**
 * Salted PBKDF2 password hasher for local Demo Mode account storage.
 * Uses PBKDF2WithHmacSHA256 where available with automatic PBKDF2WithHmacSHA1 fallback for API 24-25.
 */
class Pbkdf2DemoPasswordHasher(
  private val iterations: Int = DEFAULT_ITERATIONS,
  private val keyLengthBits: Int = DEFAULT_KEY_BITS,
  private val secureRandom: SecureRandom = SecureRandom(),
) : DemoPasswordHasher {

  override fun hash(password: String): SaltedPasswordHash {
    val salt = ByteArray(SALT_BYTES)
    secureRandom.nextBytes(salt)
    val saltBase64 = Base64.getEncoder().encodeToString(salt)
    val algorithm = resolvePreferredAlgorithm()
    val derived = deriveKey(password, salt, algorithm)
    val prefix = if (algorithm == ALG_SHA256) PREFIX_SHA256 else PREFIX_SHA1
    return SaltedPasswordHash(
      saltBase64 = saltBase64,
      hashEncoded = prefix + Base64.getEncoder().encodeToString(derived),
    )
  }

  override fun verify(
    password: String,
    saltBase64: String,
    expectedHashEncoded: String,
  ): Boolean {
    if (saltBase64.isBlank() || expectedHashEncoded.isBlank()) return false
    val (algorithm, expectedB64) =
      when {
        expectedHashEncoded.startsWith(PREFIX_SHA256) ->
          ALG_SHA256 to expectedHashEncoded.removePrefix(PREFIX_SHA256)
        expectedHashEncoded.startsWith(PREFIX_SHA1) ->
          ALG_SHA1 to expectedHashEncoded.removePrefix(PREFIX_SHA1)
        else -> return false
      }
    return runCatching {
      val salt = Base64.getDecoder().decode(saltBase64)
      val expectedBytes = Base64.getDecoder().decode(expectedB64)
      val computedBytes = deriveKey(password, salt, algorithm)
      MessageDigest.isEqual(computedBytes, expectedBytes)
    }.getOrDefault(false)
  }

  private fun deriveKey(password: String, salt: ByteArray, algorithm: String): ByteArray {
    val spec = PBEKeySpec(password.toCharArray(), salt, iterations, keyLengthBits)
    return try {
      SecretKeyFactory.getInstance(algorithm).generateSecret(spec).encoded
    } finally {
      spec.clearPassword()
    }
  }

  private fun resolvePreferredAlgorithm(): String =
    try {
      SecretKeyFactory.getInstance(ALG_SHA256)
      ALG_SHA256
    } catch (_: NoSuchAlgorithmException) {
      ALG_SHA1
    }

  companion object {
    private const val SALT_BYTES = 16
    private const val DEFAULT_ITERATIONS = 12_000
    private const val DEFAULT_KEY_BITS = 256
    private const val ALG_SHA256 = "PBKDF2WithHmacSHA256"
    private const val ALG_SHA1 = "PBKDF2WithHmacSHA1"
    private const val PREFIX_SHA256 = "pbkdf2_sha256$"
    private const val PREFIX_SHA1 = "pbkdf2_sha1$"
  }
}

object DemoAuthValidator {
  private val EMAIL_REGEX =
    Regex("^[A-Za-z0-9+._%-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$")

  const val MIN_PASSWORD_LENGTH = 6
  const val DEMO_TRANSPARENCY_NOTICE =
    "Demo Account • Local Device Only • No Real Email Verification"

  fun normalizeEmail(raw: String): String = raw.trim().lowercase(Locale.US)

  fun isValidEmail(raw: String): Boolean {
    val trimmed = raw.trim()
    if (trimmed.length !in 5..254) return false
    if (".." in trimmed) return false
    val atIndex = trimmed.indexOf('@')
    if (atIndex <= 0 || atIndex != trimmed.lastIndexOf('@')) return false
    return EMAIL_REGEX.matches(trimmed)
  }

  fun validateRegistration(
    role: UserRole,
    fullName: String,
    email: String,
    password: String,
    confirmPassword: String,
  ): String? {
    if (!role.allowsPublicRegistration || role == UserRole.SUPER_ADMIN) {
      return "Public registration is only supported for Customer and Host accounts."
    }
    if (fullName.trim().length < 2) {
      return "Enter your full name (at least 2 characters)."
    }
    if (!isValidEmail(email)) {
      return "Enter a valid email address (any valid format is accepted)."
    }
    if (password.length < MIN_PASSWORD_LENGTH) {
      return "Password must be at least $MIN_PASSWORD_LENGTH characters."
    }
    if (password != confirmPassword) {
      return "Password and Confirm Password do not match."
    }
    return null
  }

  fun validateSignIn(
    role: UserRole,
    email: String,
    password: String,
  ): String? {
    if (role == UserRole.SUPER_ADMIN) {
      return "Private Super Admin uses the dedicated Sandbox Gate, not email sign-in."
    }
    if (!isValidEmail(email)) {
      return "Enter a valid email address."
    }
    if (password.isEmpty()) {
      return "Enter your password."
    }
    return null
  }
}
