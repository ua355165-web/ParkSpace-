package com.example.core.navigation

import com.example.core.config.UserRole
import com.example.demo.sandbox.DemoSessionState

sealed interface NavGuardResult {
  data object Allowed : NavGuardResult

  data class Denied(val reason: String) : NavGuardResult
}

/**
 * Centralized role-based navigation guard.
 * Prevents unauthorized or unauthenticated access to role-scoped destinations.
 */
object ParkSpaceNavGuard {

  fun evaluateAccess(
    destination: ParkSpaceRoute,
    session: DemoSessionState,
  ): NavGuardResult =
    when (destination) {
      is ParkSpaceRoute.WelcomeEntry,
      is ParkSpaceRoute.AccessDenied, -> NavGuardResult.Allowed

      is ParkSpaceRoute.DemoAuth -> {
        if (destination.role.allowsPublicRegistration && destination.role != UserRole.SUPER_ADMIN) {
          NavGuardResult.Allowed
        } else {
          NavGuardResult.Denied(
            "Private Super Admin accounts have no public registration or login screen."
          )
        }
      }

      is ParkSpaceRoute.SharedProfileSettings,
      is ParkSpaceRoute.NotificationsInbox -> {
        if (session.isSignedIn) {
          NavGuardResult.Allowed
        } else {
          NavGuardResult.Denied("Sign in to a Demo account before accessing notifications or settings.")
        }
      }

      is ParkSpaceRoute.ChatInbox,
      is ParkSpaceRoute.BookingChat,
      is ParkSpaceRoute.DisputesCenter -> {
        if (session.activeRole == UserRole.CUSTOMER || session.activeRole == UserRole.HOST) {
          NavGuardResult.Allowed
        } else {
          NavGuardResult.Denied("Sign in as a Customer or Host to access booking messages and complaints.")
        }
      }

      is ParkSpaceRoute.CustomerHome,
      is ParkSpaceRoute.CustomerDiscovery,
      is ParkSpaceRoute.ParkingDetailPreview,
      is ParkSpaceRoute.BookingCheckout,
      is ParkSpaceRoute.BookingConfirmation,
      is ParkSpaceRoute.CustomerBookings,
      is ParkSpaceRoute.CustomerPaymentsAndReferrals -> {
        if (session.activeRole == UserRole.CUSTOMER) {
          NavGuardResult.Allowed
        } else {
          NavGuardResult.Denied("Customer workspace requires an active Customer session.")
        }
      }

      is ParkSpaceRoute.HostHome,
      is ParkSpaceRoute.HostReviewsCenter -> {
        if (session.activeRole == UserRole.HOST) {
          NavGuardResult.Allowed
        } else {
          NavGuardResult.Denied("Host workspace requires an active Host session.")
        }
      }

      is ParkSpaceRoute.PrivateAdminHome -> {
        if (session.activeRole == UserRole.SUPER_ADMIN && session.isAdminSandboxUnlocked) {
          NavGuardResult.Allowed
        } else {
          NavGuardResult.Denied(
            "Private Super Admin console is restricted and cannot be accessed publicly."
          )
        }
      }
    }

  fun homeRouteForRole(role: UserRole?): ParkSpaceRoute =
    when (role) {
      UserRole.CUSTOMER -> ParkSpaceRoute.CustomerHome
      UserRole.HOST -> ParkSpaceRoute.HostHome
      UserRole.SUPER_ADMIN -> ParkSpaceRoute.PrivateAdminHome
      null -> ParkSpaceRoute.WelcomeEntry
    }
}
