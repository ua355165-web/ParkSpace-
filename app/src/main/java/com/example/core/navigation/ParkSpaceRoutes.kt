package com.example.core.navigation

import com.example.core.config.UserRole
import kotlinx.serialization.Serializable

/**
 * Type-safe Navigation Compose routes for ParkSpace.
 * Each role boundary and shared destination has an explicit route contract.
 */
sealed interface ParkSpaceRoute {
  @Serializable data object WelcomeEntry : ParkSpaceRoute

  @Serializable
  data class DemoAuth(
    val role: UserRole,
    val startWithRegister: Boolean = false,
  ) : ParkSpaceRoute

  @Serializable data object CustomerHome : ParkSpaceRoute

  @Serializable data object CustomerDiscovery : ParkSpaceRoute

  @Serializable data class ParkingDetailPreview(val spaceId: String) : ParkSpaceRoute

  @Serializable data class BookingCheckout(val spaceId: String) : ParkSpaceRoute

  @Serializable data class BookingConfirmation(val bookingId: String) : ParkSpaceRoute

  @Serializable data object CustomerBookings : ParkSpaceRoute

  @Serializable data object HostHome : ParkSpaceRoute

  @Serializable data object PrivateAdminHome : ParkSpaceRoute

  @Serializable data object SharedProfileSettings : ParkSpaceRoute

  @Serializable data object NotificationsInbox : ParkSpaceRoute

  @Serializable data object ChatInbox : ParkSpaceRoute

  @Serializable data class BookingChat(val bookingId: String) : ParkSpaceRoute

  @Serializable data object CustomerPaymentsAndReferrals : ParkSpaceRoute

  @Serializable data class DisputesCenter(val prefillBookingId: String? = null) : ParkSpaceRoute

  @Serializable data object HostReviewsCenter : ParkSpaceRoute

  @Serializable
  data class AccessDenied(
    val attemptedRouteName: String,
    val reason: String,
  ) : ParkSpaceRoute
}
