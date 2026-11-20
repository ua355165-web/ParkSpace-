package com.example.core.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.core.common.UiState
import com.example.core.config.UserRole
import com.example.core.designsystem.components.CustomerBottomTab
import com.example.core.designsystem.components.HostBottomTab
import com.example.core.designsystem.components.ParkSpaceCustomerBottomBar
import com.example.core.designsystem.components.ParkSpaceHostBottomBar
import com.example.core.designsystem.components.ParkSpaceStateContainer
import com.example.core.di.ParkSpaceAppContainer
import com.example.ui.theme.ParkSpaceSpacing
import com.example.feature.auth.DemoAuthViewModel
import com.example.feature.auth.ui.DemoAuthScreen
import com.example.feature.booking.BookingCheckoutViewModel
import com.example.feature.booking.BookingHistoryViewModel
import com.example.feature.booking.ui.BookingCheckoutScreen
import com.example.feature.booking.ui.BookingConfirmationScreen
import com.example.feature.booking.ui.CustomerBookingsScreen
import com.example.feature.discovery.DiscoveryViewModel
import com.example.feature.discovery.ui.ParkingDetailPreviewScreen
import com.example.feature.discovery.ui.ParkingDiscoveryScreen
import com.example.feature.foundation.FoundationDashboardData
import com.example.feature.foundation.ParkSpaceFoundationViewModel
import com.example.feature.foundation.ui.AccessDeniedScreen
import com.example.feature.foundation.ui.CustomerHomeShellScreen
import com.example.feature.admin.AdminDashboardViewModel
import com.example.feature.admin.ui.PrivateAdminConsoleScreen
import com.example.feature.foundation.ui.HostHomeShellScreen
import com.example.feature.foundation.ui.SharedProfileSettingsScreen
import com.example.feature.foundation.ui.WelcomeEntryScreen
import com.example.feature.host.HostDashboardViewModel
import com.example.feature.host.ui.HostDashboardScreen
import com.example.feature.marketplace.ChatViewModel
import com.example.feature.marketplace.CustomerPaymentsReferralsViewModel
import com.example.feature.marketplace.DisputesViewModel
import com.example.feature.marketplace.NotificationsViewModel
import com.example.feature.marketplace.ReviewsViewModel
import com.example.feature.marketplace.ui.BookingChatScreen
import com.example.feature.marketplace.ui.ChatInboxScreen
import com.example.feature.marketplace.ui.CustomerPaymentsReferralsScreen
import com.example.feature.marketplace.ui.DisputesCenterScreen
import com.example.feature.marketplace.ui.HostReviewsScreen
import com.example.feature.marketplace.ui.NotificationsScreen

@Composable
fun ParkSpaceNavHost(
  appContainer: ParkSpaceAppContainer,
  viewModel: ParkSpaceFoundationViewModel,
  isDarkTheme: Boolean,
  onToggleTheme: () -> Unit,
  navController: NavHostController = rememberNavController(),
  discoveryMapContentOverride: (@Composable (com.example.feature.discovery.DiscoveryScreenData) -> Unit)? = null,
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
  val sandboxFeedback by viewModel.sandboxFeedback.collectAsStateWithLifecycle()

  val discoveryViewModel: DiscoveryViewModel =
    viewModel(factory = DiscoveryViewModel.Factory(appContainer))
  val discoveryUiState by discoveryViewModel.uiState.collectAsStateWithLifecycle()

  val bookingHistoryViewModel: BookingHistoryViewModel =
    viewModel(factory = BookingHistoryViewModel.Factory(appContainer))

  val reviewsViewModel: ReviewsViewModel =
    viewModel(factory = ReviewsViewModel.Factory(appContainer))

  val disputesViewModel: DisputesViewModel =
    viewModel(factory = DisputesViewModel.Factory(appContainer))

  val hostViewModel: HostDashboardViewModel =
    viewModel(factory = HostDashboardViewModel.Factory(appContainer))

  fun navigateGuarded(destination: ParkSpaceRoute) {
    when (val decision = ParkSpaceNavGuard.evaluateAccess(destination, sessionState)) {
      is NavGuardResult.Allowed -> navController.navigate(destination)
      is NavGuardResult.Denied ->
        navController.navigate(
          ParkSpaceRoute.AccessDenied(
            attemptedRouteName = destination::class.simpleName ?: "Unknown",
            reason = decision.reason,
          )
        )
    }
  }

  val unreadChatsCount = (uiState as? UiState.Success)?.data?.unreadChatsCount ?: 0

  @Composable
  fun CustomerBottomNavBar(selectedTab: CustomerBottomTab?) {
    ParkSpaceCustomerBottomBar(
      selectedTab = selectedTab,
      unreadMessagesCount = unreadChatsCount,
      onSelectTab = { tab ->
        if (tab == selectedTab) return@ParkSpaceCustomerBottomBar
        val destination =
          when (tab) {
            CustomerBottomTab.DISCOVER -> {
              val city =
                (uiState as? UiState.Success)?.data?.session?.selectedCity
                  ?: sessionState.selectedCity
              discoveryViewModel.onSelectCity(city)
              ParkSpaceRoute.CustomerDiscovery
            }
            CustomerBottomTab.BOOKINGS -> {
              bookingHistoryViewModel.refreshExpiredBookings()
              ParkSpaceRoute.CustomerBookings
            }
            CustomerBottomTab.MESSAGES -> ParkSpaceRoute.ChatInbox
            CustomerBottomTab.PROFILE -> ParkSpaceRoute.SharedProfileSettings
          }
        when (val decision = ParkSpaceNavGuard.evaluateAccess(destination, sessionState)) {
          is NavGuardResult.Allowed ->
            navController.navigate(destination) {
              popUpTo(ParkSpaceRoute.CustomerHome) { inclusive = false }
              launchSingleTop = true
            }
          is NavGuardResult.Denied ->
            navController.navigate(
              ParkSpaceRoute.AccessDenied(
                attemptedRouteName = destination::class.simpleName ?: "Unknown",
                reason = decision.reason,
              )
            )
        }
      },
    )
  }

  @Composable
  fun HostBottomNavBar(selectedTab: HostBottomTab) {
    val hostState by hostViewModel.uiState.collectAsStateWithLifecycle()
    val pendingBadge =
      hostState.bookings.count {
        it.status == com.example.core.domain.model.BookingStatus.PENDING_PAYMENT ||
          it.status == com.example.core.domain.model.BookingStatus.CONFIRMED ||
          it.status == com.example.core.domain.model.BookingStatus.CHECKED_IN
      }
    ParkSpaceHostBottomBar(
      selectedTab = selectedTab,
      isDarkTheme = isDarkTheme,
      pendingBookingsBadge = pendingBadge,
      onSelectTab = { tab ->
        if (tab == selectedTab) return@ParkSpaceHostBottomBar
        val targetHostTab = tab.hostTab
        if (targetHostTab != null) {
          hostViewModel.selectTab(targetHostTab)
          when (val decision = ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.HostHome, sessionState)) {
            is NavGuardResult.Allowed ->
              navController.navigate(ParkSpaceRoute.HostHome) {
                popUpTo(ParkSpaceRoute.HostHome) { inclusive = false }
                launchSingleTop = true
              }
            is NavGuardResult.Denied ->
              navController.navigate(
                ParkSpaceRoute.AccessDenied(
                  attemptedRouteName = "HostHome",
                  reason = decision.reason,
                )
              )
          }
        } else {
          navigateGuarded(ParkSpaceRoute.SharedProfileSettings)
        }
      },
    )
  }

  NavHost(
    navController = navController,
    startDestination = ParkSpaceRoute.WelcomeEntry,
  ) {
    composable<ParkSpaceRoute.WelcomeEntry> {
      WelcomeEntryScreen(
        uiState = uiState,
        sandboxFeedback = sandboxFeedback,
        isDarkTheme = isDarkTheme,
        onToggleTheme = onToggleTheme,
        onSelectCity = { city ->
          viewModel.selectCity(city)
          discoveryViewModel.onSelectCity(city)
        },
        onOpenAuth = { role, startWithRegister ->
          navigateGuarded(ParkSpaceRoute.DemoAuth(role = role, startWithRegister = startWithRegister))
        },
        onSignOut = viewModel::signOutDemoSandbox,
        onUnlockPrivateAdmin = {
          viewModel.unlockPrivateDemoAdmin {
            navController.navigate(ParkSpaceRoute.PrivateAdminHome)
          }
        },
        onNavigateToRoleHome = ::navigateGuarded,
        onNavigateToSettings = { navigateGuarded(ParkSpaceRoute.SharedProfileSettings) },
        onRetrySeed = viewModel::bootstrapDemoSeed,
        onDismissFeedback = viewModel::dismissFeedback,
      )
    }

    composable<ParkSpaceRoute.DemoAuth> { backStackEntry ->
      val args = backStackEntry.toRoute<ParkSpaceRoute.DemoAuth>()
      when (val guard = ParkSpaceNavGuard.evaluateAccess(args, sessionState)) {
        is NavGuardResult.Denied ->
          AccessDeniedScreen(
            attemptedRouteName = "DemoAuth",
            reason = guard.reason,
            onReturnToEntry = {
              navController.popBackStack(ParkSpaceRoute.WelcomeEntry, inclusive = false)
            },
            isDarkTheme = isDarkTheme,
          )
        is NavGuardResult.Allowed -> {
          val authViewModel: DemoAuthViewModel =
            viewModel(
              key = "auth_${args.role.name}_${args.startWithRegister}",
              factory =
                DemoAuthViewModel.Factory(
                  role = args.role,
                  startWithRegister = args.startWithRegister,
                  container = appContainer,
                ),
            )
          val authState by authViewModel.uiState.collectAsStateWithLifecycle()
          DemoAuthScreen(
            uiState = authState,
            onBack = { navController.popBackStack() },
            onSelectMode = authViewModel::setMode,
            onFullNameChange = authViewModel::onFullNameChange,
            onEmailChange = authViewModel::onEmailChange,
            onPasswordChange = authViewModel::onPasswordChange,
            onConfirmPasswordChange = authViewModel::onConfirmPasswordChange,
            onBusinessAliasChange = authViewModel::onBusinessAliasChange,
            onSelectCity = authViewModel::onSelectCity,
            onSubmit = {
              authViewModel.submit { authenticatedRole ->
                val targetHome =
                  when (authenticatedRole) {
                    UserRole.CUSTOMER -> ParkSpaceRoute.CustomerHome
                    UserRole.HOST -> ParkSpaceRoute.HostHome
                    UserRole.SUPER_ADMIN -> ParkSpaceRoute.WelcomeEntry
                  }
                navController.navigate(targetHome) {
                  popUpTo(ParkSpaceRoute.WelcomeEntry) { inclusive = false }
                }
              }
            },
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme,
            onDualRoleBusinessAliasChange = authViewModel::onDualRoleBusinessAliasChange,
            onActivateOrSwitchCompanionRole = {
              authViewModel.activateOrSwitchCompanionRole { authenticatedRole ->
                val targetHome =
                  when (authenticatedRole) {
                    UserRole.CUSTOMER -> ParkSpaceRoute.CustomerHome
                    UserRole.HOST -> ParkSpaceRoute.HostHome
                    UserRole.SUPER_ADMIN -> ParkSpaceRoute.WelcomeEntry
                  }
                navController.navigate(targetHome) {
                  popUpTo(ParkSpaceRoute.WelcomeEntry) { inclusive = false }
                }
              }
            },
          )
        }
      }
    }

    composable<ParkSpaceRoute.CustomerHome> {
      GuardedDashboardDestination(
        route = ParkSpaceRoute.CustomerHome,
        uiState = uiState,
        session = sessionState,
        onRetrySeed = viewModel::bootstrapDemoSeed,
        onUnauthorized = { reason ->
          AccessDeniedScreen(
            attemptedRouteName = "CustomerHome",
            reason = reason,
            onReturnToEntry = {
              navController.popBackStack(ParkSpaceRoute.WelcomeEntry, inclusive = false)
            },
            isDarkTheme = isDarkTheme,
          )
        },
      ) { data ->
        CustomerHomeShellScreen(
          data = data,
          onBack = { navController.popBackStack() },
          onOpenSettings = { navigateGuarded(ParkSpaceRoute.SharedProfileSettings) },
          onOpenDiscovery = {
            discoveryViewModel.onSelectCity(data.session.selectedCity)
            navigateGuarded(ParkSpaceRoute.CustomerDiscovery)
          },
          onOpenBookings = {
            bookingHistoryViewModel.refreshExpiredBookings()
            navigateGuarded(ParkSpaceRoute.CustomerBookings)
          },
          onAddVehicle = viewModel::addVehicleForActiveCustomer,
          onUpdatePreferredCity = { city ->
            viewModel.updateActiveCustomerPreferredCity(city)
            discoveryViewModel.onSelectCity(city)
          },
          onOpenPaymentsAndReferrals = {
            navigateGuarded(ParkSpaceRoute.CustomerPaymentsAndReferrals)
          },
          onOpenMessages = { navigateGuarded(ParkSpaceRoute.ChatInbox) },
          onOpenDisputes = { navigateGuarded(ParkSpaceRoute.DisputesCenter()) },
          onOpenNotifications = { navigateGuarded(ParkSpaceRoute.NotificationsInbox) },
          isDarkTheme = isDarkTheme,
          onToggleTheme = onToggleTheme,
          sandboxFeedback = sandboxFeedback,
          onDismissFeedback = viewModel::dismissFeedback,
          onOpenBookingDetail = { bId ->
            navigateGuarded(ParkSpaceRoute.BookingConfirmation(bId))
          },
          onOpenHostDashboardOrActivate = {
            viewModel.switchToCompanionHostDashboard(
              onSwitched = {
                navController.navigate(ParkSpaceRoute.HostHome) {
                  popUpTo(ParkSpaceRoute.WelcomeEntry) { inclusive = false }
                }
              },
              onRequiresActivation = {
                navigateGuarded(
                  ParkSpaceRoute.DemoAuth(role = UserRole.HOST, startWithRegister = true)
                )
              },
            )
          },
          onSignOut = {
            viewModel.signOutDemoSandbox()
            navController.popBackStack(ParkSpaceRoute.WelcomeEntry, inclusive = false)
          },
          bottomBar = { CustomerBottomNavBar(selectedTab = null) },
        )
      }
    }

    composable<ParkSpaceRoute.CustomerDiscovery> {
      when (
        val guard = ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.CustomerDiscovery, sessionState)
      ) {
        is NavGuardResult.Denied ->
          AccessDeniedScreen(
            attemptedRouteName = "CustomerDiscovery",
            reason = guard.reason,
            onReturnToEntry = {
              navController.popBackStack(ParkSpaceRoute.WelcomeEntry, inclusive = false)
            },
            isDarkTheme = isDarkTheme,
          )
        is NavGuardResult.Allowed ->
          ParkingDiscoveryScreen(
            uiState = discoveryUiState,
            viewModel = discoveryViewModel,
            onBack = { navController.popBackStack() },
            onViewDetails = { spaceId ->
              navigateGuarded(ParkSpaceRoute.ParkingDetailPreview(spaceId))
            },
            mapContentOverride = discoveryMapContentOverride,
            bottomBar = { CustomerBottomNavBar(selectedTab = CustomerBottomTab.DISCOVER) },
            isDarkTheme = isDarkTheme,
          )
      }
    }

    composable<ParkSpaceRoute.ParkingDetailPreview> { backStackEntry ->
      val args = backStackEntry.toRoute<ParkSpaceRoute.ParkingDetailPreview>()
      when (
        val guard =
          ParkSpaceNavGuard.evaluateAccess(
            ParkSpaceRoute.ParkingDetailPreview(args.spaceId),
            sessionState,
          )
      ) {
        is NavGuardResult.Denied ->
          AccessDeniedScreen(
            attemptedRouteName = "ParkingDetailPreview",
            reason = guard.reason,
            onReturnToEntry = {
              navController.popBackStack(ParkSpaceRoute.WelcomeEntry, inclusive = false)
            },
            isDarkTheme = isDarkTheme,
          )
        is NavGuardResult.Allowed -> {
          val listing =
            (discoveryUiState as? UiState.Success)?.data?.result?.listings?.firstOrNull {
              it.id == args.spaceId
            }
          val reviewsFlow =
            remember(args.spaceId) { reviewsViewModel.observeReviewsForSpace(args.spaceId) }
          val reviews by reviewsFlow.collectAsStateWithLifecycle()
          ParkingDetailPreviewScreen(
            listing = listing,
            onBack = { navController.popBackStack() },
            onBookNow = { spaceId ->
              navigateGuarded(ParkSpaceRoute.BookingCheckout(spaceId))
            },
            reviews = reviews,
            isDarkTheme = isDarkTheme,
          )
        }
      }
    }

    composable<ParkSpaceRoute.BookingCheckout> { backStackEntry ->
      val args = backStackEntry.toRoute<ParkSpaceRoute.BookingCheckout>()
      when (
        val guard =
          ParkSpaceNavGuard.evaluateAccess(
            ParkSpaceRoute.BookingCheckout(args.spaceId),
            sessionState,
          )
      ) {
        is NavGuardResult.Denied ->
          AccessDeniedScreen(
            attemptedRouteName = "BookingCheckout",
            reason = guard.reason,
            onReturnToEntry = {
              navController.popBackStack(ParkSpaceRoute.WelcomeEntry, inclusive = false)
            },
            isDarkTheme = isDarkTheme,
          )
        is NavGuardResult.Allowed -> {
          val checkoutViewModel: BookingCheckoutViewModel =
            viewModel(
              key = "checkout_${args.spaceId}",
              factory = BookingCheckoutViewModel.Factory(args.spaceId, appContainer),
            )
          val checkoutState by checkoutViewModel.uiState.collectAsStateWithLifecycle()
          BookingCheckoutScreen(
            uiState = checkoutState,
            onBack = { navController.popBackStack() },
            onSelectVehicle = checkoutViewModel::onSelectVehicle,
            onSelectBookingType = checkoutViewModel::onSelectBookingType,
            onSelectDayOffset = checkoutViewModel::onSelectDayOffset,
            onAdjustStartHour = checkoutViewModel::onAdjustStartHour,
            onToggleStartMinute = checkoutViewModel::onToggleStartMinute,
            onAdjustUnits = checkoutViewModel::onAdjustUnits,
            onSetUnits = checkoutViewModel::onSetUnits,
            onToggleImmediateDemoPayment = checkoutViewModel::onToggleImmediateDemoPayment,
            onConfirmBooking = {
              checkoutViewModel.confirmBooking { createdBookingId ->
                navController.navigate(ParkSpaceRoute.BookingConfirmation(createdBookingId)) {
                  popUpTo(ParkSpaceRoute.BookingCheckout(args.spaceId)) { inclusive = true }
                }
              }
            },
            onAddVehicle = { type, label, plate ->
              checkoutViewModel.addVehicle(type, label, plate)
            },
            onSelectPaymentMethod = checkoutViewModel::onSelectPaymentMethod,
            isDarkTheme = isDarkTheme,
          )
        }
      }
    }

    composable<ParkSpaceRoute.BookingConfirmation> { backStackEntry ->
      val args = backStackEntry.toRoute<ParkSpaceRoute.BookingConfirmation>()
      when (
        val guard =
          ParkSpaceNavGuard.evaluateAccess(
            ParkSpaceRoute.BookingConfirmation(args.bookingId),
            sessionState,
          )
      ) {
        is NavGuardResult.Denied ->
          AccessDeniedScreen(
            attemptedRouteName = "BookingConfirmation",
            reason = guard.reason,
            onReturnToEntry = {
              navController.popBackStack(ParkSpaceRoute.WelcomeEntry, inclusive = false)
            },
            isDarkTheme = isDarkTheme,
          )
        is NavGuardResult.Allowed -> {
          val detailFlow =
            remember(args.bookingId) { bookingHistoryViewModel.observeBookingDetail(args.bookingId) }
          val detailState by detailFlow.collectAsStateWithLifecycle()
          val actionFeedback by bookingHistoryViewModel.actionFeedback.collectAsStateWithLifecycle()
          val reviewFeedback by reviewsViewModel.feedback.collectAsStateWithLifecycle()
          val disputeFeedback by disputesViewModel.feedback.collectAsStateWithLifecycle()
          val availableDemoCreditPkr by
            bookingHistoryViewModel.availableDemoCreditPkr.collectAsStateWithLifecycle()
          BookingConfirmationScreen(
            uiState = detailState,
            actionFeedback = actionFeedback ?: reviewFeedback ?: disputeFeedback,
            onBack = { navController.popBackStack() },
            onOpenBookingsHistory = {
              navigateGuarded(ParkSpaceRoute.CustomerBookings)
            },
            onConfirmPendingPayment = bookingHistoryViewModel::confirmPendingPayment,
            onCheckIn = bookingHistoryViewModel::checkIn,
            onCheckOut = bookingHistoryViewModel::checkOut,
            onCancelBooking = bookingHistoryViewModel::cancelBooking,
            onDismissFeedback = {
              bookingHistoryViewModel.dismissFeedback()
              reviewsViewModel.dismissFeedback()
              disputesViewModel.dismissFeedback()
            },
            availableDemoCreditPkr = availableDemoCreditPkr,
            onConfirmPendingPaymentWithMethod = bookingHistoryViewModel::confirmPendingPayment,
            onOpenChat = { bId -> navigateGuarded(ParkSpaceRoute.BookingChat(bId)) },
            onSubmitReview = { bId, rating, comment ->
              reviewsViewModel.submitReview(bId, rating, comment)
            },
            onFileDispute = { bId, category, description ->
              disputesViewModel.fileDispute(bId, category, description)
            },
            isDarkTheme = isDarkTheme,
          )
        }
      }
    }

    composable<ParkSpaceRoute.CustomerBookings> {
      when (
        val guard =
          ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.CustomerBookings, sessionState)
      ) {
        is NavGuardResult.Denied ->
          AccessDeniedScreen(
            attemptedRouteName = "CustomerBookings",
            reason = guard.reason,
            onReturnToEntry = {
              navController.popBackStack(ParkSpaceRoute.WelcomeEntry, inclusive = false)
            },
            isDarkTheme = isDarkTheme,
          )
        is NavGuardResult.Allowed -> {
          val historyState by bookingHistoryViewModel.historyUiState.collectAsStateWithLifecycle()
          val actionFeedback by bookingHistoryViewModel.actionFeedback.collectAsStateWithLifecycle()
          val reviewFeedback by reviewsViewModel.feedback.collectAsStateWithLifecycle()
          val disputeFeedback by disputesViewModel.feedback.collectAsStateWithLifecycle()
          CustomerBookingsScreen(
            uiState = historyState,
            actionFeedback = actionFeedback ?: reviewFeedback ?: disputeFeedback,
            onBack = { navController.popBackStack() },
            onSelectTab = bookingHistoryViewModel::onSelectTab,
            onOpenBookingDetail = { bookingId ->
              navigateGuarded(ParkSpaceRoute.BookingConfirmation(bookingId))
            },
            onConfirmPendingPayment = bookingHistoryViewModel::confirmPendingPayment,
            onCheckIn = bookingHistoryViewModel::checkIn,
            onCheckOut = bookingHistoryViewModel::checkOut,
            onCancelBooking = bookingHistoryViewModel::cancelBooking,
            onOpenDiscovery = {
              navigateGuarded(ParkSpaceRoute.CustomerDiscovery)
            },
            onDismissFeedback = {
              bookingHistoryViewModel.dismissFeedback()
              reviewsViewModel.dismissFeedback()
              disputesViewModel.dismissFeedback()
            },
            onConfirmPendingPaymentWithMethod = bookingHistoryViewModel::confirmPendingPayment,
            onOpenChat = { bId -> navigateGuarded(ParkSpaceRoute.BookingChat(bId)) },
            onSubmitReview = { bId, rating, comment ->
              reviewsViewModel.submitReview(bId, rating, comment)
            },
            onFileDispute = { bId, category, description ->
              disputesViewModel.fileDispute(bId, category, description)
            },
            bottomBar = { CustomerBottomNavBar(selectedTab = CustomerBottomTab.BOOKINGS) },
            isDarkTheme = isDarkTheme,
          )
        }
      }
    }

    composable<ParkSpaceRoute.HostHome> {
      when (val guard = ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.HostHome, sessionState)) {
        is NavGuardResult.Denied ->
          AccessDeniedScreen(
            attemptedRouteName = "HostHome",
            reason = guard.reason,
            onReturnToEntry = {
              navController.popBackStack(ParkSpaceRoute.WelcomeEntry, inclusive = false)
            },
            isDarkTheme = isDarkTheme,
          )
        is NavGuardResult.Allowed -> {
          val hostState by hostViewModel.uiState.collectAsStateWithLifecycle()
          HostDashboardScreen(
            uiState = hostState,
            viewModel = hostViewModel,
            onBack = { navController.popBackStack() },
            onOpenSettings = { navigateGuarded(ParkSpaceRoute.SharedProfileSettings) },
            onOpenNotifications = { navigateGuarded(ParkSpaceRoute.NotificationsInbox) },
            onOpenMessages = { navigateGuarded(ParkSpaceRoute.ChatInbox) },
            onOpenReviews = { navigateGuarded(ParkSpaceRoute.HostReviewsCenter) },
            onOpenDisputes = { navigateGuarded(ParkSpaceRoute.DisputesCenter()) },
            onOpenBookingChat = { bId -> navigateGuarded(ParkSpaceRoute.BookingChat(bId)) },
            onOpenBookingDispute = { bId ->
              navigateGuarded(ParkSpaceRoute.DisputesCenter(prefillBookingId = bId))
            },
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme,
            onOpenCustomerDashboardOrActivate = {
              viewModel.switchToCompanionCustomerDashboard(
                onSwitched = {
                  navController.navigate(ParkSpaceRoute.CustomerHome) {
                    popUpTo(ParkSpaceRoute.WelcomeEntry) { inclusive = false }
                  }
                },
                onRequiresActivation = {
                  navigateGuarded(
                    ParkSpaceRoute.DemoAuth(role = UserRole.CUSTOMER, startWithRegister = true)
                  )
                },
              )
            },
            onSignOut = {
              viewModel.signOutDemoSandbox()
              navController.popBackStack(ParkSpaceRoute.WelcomeEntry, inclusive = false)
            },
          )
        }
      }
    }

    composable<ParkSpaceRoute.PrivateAdminHome> {
      when (
        val guard =
          ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.PrivateAdminHome, sessionState)
      ) {
        is NavGuardResult.Denied ->
          AccessDeniedScreen(
            attemptedRouteName = "PrivateAdminHome",
            reason = guard.reason,
            onReturnToEntry = {
              navController.popBackStack(ParkSpaceRoute.WelcomeEntry, inclusive = false)
            },
            isDarkTheme = isDarkTheme,
          )
        is NavGuardResult.Allowed -> {
          val adminViewModel: AdminDashboardViewModel =
            viewModel(factory = AdminDashboardViewModel.Factory(appContainer))
          val adminState by adminViewModel.uiState.collectAsStateWithLifecycle()
          PrivateAdminConsoleScreen(
            uiState = adminState,
            onBack = { navController.popBackStack() },
            onOpenSettings = { navigateGuarded(ParkSpaceRoute.SharedProfileSettings) },
            onSelectTab = adminViewModel::selectTab,
            onApproveHostKyc = adminViewModel::approveHostKyc,
            onRejectHostKyc = adminViewModel::rejectHostKyc,
            onApproveListing = adminViewModel::approveListing,
            onRejectListing = adminViewModel::rejectListing,
            onUpdateDisputeStatus = adminViewModel::updateDisputeStatus,
            onUpdateCommissionRule = adminViewModel::updateCommissionRule,
            onDismissFeedback = adminViewModel::dismissFeedback,
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme,
            onSignOut = {
              viewModel.signOutDemoSandbox()
              navController.popBackStack(ParkSpaceRoute.WelcomeEntry, inclusive = false)
            },
          )
        }
      }
    }

    composable<ParkSpaceRoute.SharedProfileSettings> {
      GuardedDashboardDestination(
        route = ParkSpaceRoute.SharedProfileSettings,
        uiState = uiState,
        session = sessionState,
        onRetrySeed = viewModel::bootstrapDemoSeed,
        onUnauthorized = { reason ->
          AccessDeniedScreen(
            attemptedRouteName = "SharedProfileSettings",
            reason = reason,
            onReturnToEntry = {
              navController.popBackStack(ParkSpaceRoute.WelcomeEntry, inclusive = false)
            },
            isDarkTheme = isDarkTheme,
          )
        },
      ) { data ->
        SharedProfileSettingsScreen(
          data = data,
          onBack = { navController.popBackStack() },
          onSignOut = {
            viewModel.signOutDemoSandbox()
            navController.popBackStack(ParkSpaceRoute.WelcomeEntry, inclusive = false)
          },
          isDarkTheme = isDarkTheme,
          onToggleTheme = onToggleTheme,
          bottomBar = {
            when (sessionState.activeRole) {
              UserRole.CUSTOMER -> CustomerBottomNavBar(selectedTab = CustomerBottomTab.PROFILE)
              UserRole.HOST -> HostBottomNavBar(selectedTab = HostBottomTab.PROFILE)
              else -> Unit
            }
          },
        )
      }
    }

    composable<ParkSpaceRoute.NotificationsInbox> {
      when (
        val guard =
          ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.NotificationsInbox, sessionState)
      ) {
        is NavGuardResult.Denied ->
          AccessDeniedScreen(
            attemptedRouteName = "NotificationsInbox",
            reason = guard.reason,
            onReturnToEntry = {
              navController.popBackStack(ParkSpaceRoute.WelcomeEntry, inclusive = false)
            },
            isDarkTheme = isDarkTheme,
          )
        is NavGuardResult.Allowed -> {
          val notifViewModel: NotificationsViewModel =
            viewModel(factory = NotificationsViewModel.Factory(appContainer))
          val notifState by notifViewModel.uiState.collectAsStateWithLifecycle()
          NotificationsScreen(
            uiState = notifState,
            onBack = { navController.popBackStack() },
            onMarkAsRead = notifViewModel::markAsRead,
            onMarkAllAsRead = notifViewModel::markAllAsRead,
            onOpenRelatedBooking = { bId ->
              when (sessionState.activeRole) {
                UserRole.CUSTOMER -> navigateGuarded(ParkSpaceRoute.BookingConfirmation(bId))
                UserRole.HOST -> navigateGuarded(ParkSpaceRoute.BookingChat(bId))
                else -> Unit
              }
            },
            isDarkTheme = isDarkTheme,
          )
        }
      }
    }

    composable<ParkSpaceRoute.ChatInbox> {
      when (val guard = ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.ChatInbox, sessionState)) {
        is NavGuardResult.Denied ->
          AccessDeniedScreen(
            attemptedRouteName = "ChatInbox",
            reason = guard.reason,
            onReturnToEntry = {
              navController.popBackStack(ParkSpaceRoute.WelcomeEntry, inclusive = false)
            },
            isDarkTheme = isDarkTheme,
          )
        is NavGuardResult.Allowed -> {
          val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory(appContainer))
          val inboxState by chatViewModel.inboxUiState.collectAsStateWithLifecycle()
          ChatInboxScreen(
            uiState = inboxState,
            onBack = { navController.popBackStack() },
            onOpenBookingChat = { bId -> navigateGuarded(ParkSpaceRoute.BookingChat(bId)) },
            bottomBar = {
              if (sessionState.activeRole == UserRole.CUSTOMER) {
                CustomerBottomNavBar(selectedTab = CustomerBottomTab.MESSAGES)
              }
            },
            isDarkTheme = isDarkTheme,
          )
        }
      }
    }

    composable<ParkSpaceRoute.BookingChat> { backStackEntry ->
      val args = backStackEntry.toRoute<ParkSpaceRoute.BookingChat>()
      when (
        val guard =
          ParkSpaceNavGuard.evaluateAccess(
            ParkSpaceRoute.BookingChat(args.bookingId),
            sessionState,
          )
      ) {
        is NavGuardResult.Denied ->
          AccessDeniedScreen(
            attemptedRouteName = "BookingChat",
            reason = guard.reason,
            onReturnToEntry = {
              navController.popBackStack(ParkSpaceRoute.WelcomeEntry, inclusive = false)
            },
            isDarkTheme = isDarkTheme,
          )
        is NavGuardResult.Allowed -> {
          val chatViewModel: ChatViewModel = viewModel(factory = ChatViewModel.Factory(appContainer))
          val threadFlow =
            remember(args.bookingId) { chatViewModel.observeBookingThread(args.bookingId) }
          val threadState by threadFlow.collectAsStateWithLifecycle()
          BookingChatScreen(
            bookingId = args.bookingId,
            uiState = threadState,
            onBack = { navController.popBackStack() },
            onMarkThreadRead = chatViewModel::markThreadRead,
            onSendMessage = { bId, body -> chatViewModel.sendMessage(bId, body) },
            isDarkTheme = isDarkTheme,
          )
        }
      }
    }

    composable<ParkSpaceRoute.CustomerPaymentsAndReferrals> {
      when (
        val guard =
          ParkSpaceNavGuard.evaluateAccess(
            ParkSpaceRoute.CustomerPaymentsAndReferrals,
            sessionState,
          )
      ) {
        is NavGuardResult.Denied ->
          AccessDeniedScreen(
            attemptedRouteName = "CustomerPaymentsAndReferrals",
            reason = guard.reason,
            onReturnToEntry = {
              navController.popBackStack(ParkSpaceRoute.WelcomeEntry, inclusive = false)
            },
            isDarkTheme = isDarkTheme,
          )
        is NavGuardResult.Allowed -> {
          val prViewModel: CustomerPaymentsReferralsViewModel =
            viewModel(factory = CustomerPaymentsReferralsViewModel.Factory(appContainer))
          val prState by prViewModel.uiState.collectAsStateWithLifecycle()
          val prFeedback by prViewModel.feedback.collectAsStateWithLifecycle()
          CustomerPaymentsReferralsScreen(
            uiState = prState,
            feedback = prFeedback,
            onBack = { navController.popBackStack() },
            onRedeemReferralCode = prViewModel::redeemReferralCode,
            onDismissFeedback = prViewModel::dismissFeedback,
            isDarkTheme = isDarkTheme,
          )
        }
      }
    }

    composable<ParkSpaceRoute.DisputesCenter> { backStackEntry ->
      val args = backStackEntry.toRoute<ParkSpaceRoute.DisputesCenter>()
      when (
        val guard =
          ParkSpaceNavGuard.evaluateAccess(
            ParkSpaceRoute.DisputesCenter(args.prefillBookingId),
            sessionState,
          )
      ) {
        is NavGuardResult.Denied ->
          AccessDeniedScreen(
            attemptedRouteName = "DisputesCenter",
            reason = guard.reason,
            onReturnToEntry = {
              navController.popBackStack(ParkSpaceRoute.WelcomeEntry, inclusive = false)
            },
            isDarkTheme = isDarkTheme,
          )
        is NavGuardResult.Allowed -> {
          val disputesState by disputesViewModel.uiState.collectAsStateWithLifecycle()
          val disputeFeedback by disputesViewModel.feedback.collectAsStateWithLifecycle()
          DisputesCenterScreen(
            uiState = disputesState,
            feedback = disputeFeedback,
            prefillBookingId = args.prefillBookingId,
            onBack = { navController.popBackStack() },
            onFileDispute = { bId, cat, desc -> disputesViewModel.fileDispute(bId, cat, desc) },
            onRespondAsHost = { dId, note -> disputesViewModel.respondAsHost(dId, note) },
            onResolveDispute = { dId, note -> disputesViewModel.resolveDispute(dId, note) },
            onDismissFeedback = disputesViewModel::dismissFeedback,
            isDarkTheme = isDarkTheme,
          )
        }
      }
    }

    composable<ParkSpaceRoute.HostReviewsCenter> {
      when (
        val guard =
          ParkSpaceNavGuard.evaluateAccess(ParkSpaceRoute.HostReviewsCenter, sessionState)
      ) {
        is NavGuardResult.Denied ->
          AccessDeniedScreen(
            attemptedRouteName = "HostReviewsCenter",
            reason = guard.reason,
            onReturnToEntry = {
              navController.popBackStack(ParkSpaceRoute.WelcomeEntry, inclusive = false)
            },
            isDarkTheme = isDarkTheme,
          )
        is NavGuardResult.Allowed -> {
          val hostReviews by reviewsViewModel.hostReviewsUiState.collectAsStateWithLifecycle()
          val reviewFeedback by reviewsViewModel.feedback.collectAsStateWithLifecycle()
          HostReviewsScreen(
            reviews = hostReviews,
            feedback = reviewFeedback,
            onBack = { navController.popBackStack() },
            onReplyToReview = { rId, reply -> reviewsViewModel.replyToReview(rId, reply) },
            onDismissFeedback = reviewsViewModel::dismissFeedback,
            isDarkTheme = isDarkTheme,
          )
        }
      }
    }

    composable<ParkSpaceRoute.AccessDenied> { backStackEntry ->
      val args = backStackEntry.toRoute<ParkSpaceRoute.AccessDenied>()
      AccessDeniedScreen(
        attemptedRouteName = args.attemptedRouteName,
        reason = args.reason,
        onReturnToEntry = {
          navController.popBackStack(ParkSpaceRoute.WelcomeEntry, inclusive = false)
        },
        isDarkTheme = isDarkTheme,
      )
    }
  }
}

@Composable
private fun GuardedDashboardDestination(
  route: ParkSpaceRoute,
  uiState: UiState<FoundationDashboardData>,
  session: com.example.demo.sandbox.DemoSessionState,
  onRetrySeed: () -> Unit,
  onUnauthorized: @Composable (String) -> Unit,
  content: @Composable (FoundationDashboardData) -> Unit,
) {
  when (val guard = ParkSpaceNavGuard.evaluateAccess(route, session)) {
    is NavGuardResult.Denied -> onUnauthorized(guard.reason)
    is NavGuardResult.Allowed -> {
      when (uiState) {
        is UiState.Success -> content(uiState.data)
        else ->
          Surface(modifier = Modifier.fillMaxSize()) {
            ParkSpaceStateContainer(
              state = uiState,
              onRetry = onRetrySeed,
              modifier = Modifier.padding(ParkSpaceSpacing.screenHorizontal),
            ) {}
          }
      }
    }
  }
}
