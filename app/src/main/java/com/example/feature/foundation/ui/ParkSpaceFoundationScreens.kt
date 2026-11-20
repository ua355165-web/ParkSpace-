package com.example.feature.foundation.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.common.UiState
import com.example.core.config.SupportedCity
import com.example.core.config.UserRole
import com.example.core.designsystem.components.ParkSpaceAdminSandboxGateDialog
import com.example.core.designsystem.components.ParkSpaceAtmosphericBackground
import com.example.core.designsystem.components.ParkSpaceCompactDemoPill
import com.example.core.designsystem.components.ParkSpaceGlassIconButton
import com.example.core.designsystem.components.ParkSpaceGlassSurface
import com.example.core.designsystem.components.ParkSpaceGlobalHeader
import com.example.core.designsystem.components.ParkSpaceGuestSettingsDialog
import com.example.core.designsystem.components.ParkSpaceLiquidGlassButton
import com.example.core.designsystem.components.ParkSpaceLiquidGlassRoleCard
import com.example.core.designsystem.components.ParkSpaceLiquidGlassSecondaryButton
import com.example.core.designsystem.components.ParkSpaceStateContainer
import com.example.core.designsystem.components.ParkSpaceStatusPill
import com.example.core.designsystem.components.StatusTone
import com.example.core.domain.model.HostVerificationStatus
import com.example.core.domain.model.VehicleType
import com.example.core.navigation.ParkSpaceNavGuard
import com.example.core.navigation.ParkSpaceRoute
import com.example.feature.auth.domain.DemoAuthValidator
import com.example.feature.foundation.FoundationDashboardData
import com.example.ui.theme.Brass400
import com.example.ui.theme.Brass600
import com.example.ui.theme.Emerald900
import com.example.ui.theme.Emerald950
import com.example.ui.theme.Jade400
import com.example.ui.theme.ParkSpaceMonoCodeStyle
import com.example.ui.theme.ParkSpaceSpacing
import com.example.ui.theme.ParkSpaceThemeTokens

@Suppress("UNUSED_PARAMETER")
@Composable
fun WelcomeEntryScreen(
  uiState: UiState<FoundationDashboardData>,
  sandboxFeedback: String?,
  isDarkTheme: Boolean,
  onToggleTheme: () -> Unit,
  onSelectCity: (SupportedCity) -> Unit,
  onOpenAuth: (UserRole, Boolean) -> Unit,
  onSignOut: () -> Unit,
  onUnlockPrivateAdmin: () -> Unit,
  onNavigateToRoleHome: (ParkSpaceRoute) -> Unit,
  onNavigateToSettings: () -> Unit,
  onRetrySeed: () -> Unit,
  onDismissFeedback: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showAdminGateDialog by rememberSaveable { mutableStateOf(false) }
  var gateAcknowledged by rememberSaveable { mutableStateOf(false) }
  var showGuestSettingsDialog by rememberSaveable { mutableStateOf(false) }

  val successData = (uiState as? UiState.Success)?.data
  val activeUser = successData?.session?.activeUser
  val semantic = ParkSpaceThemeTokens.semantic

  ParkSpaceAtmosphericBackground(isDarkTheme = isDarkTheme, modifier = modifier) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = Color.Transparent,
      contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
      Box(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentAlignment = Alignment.TopCenter,
      ) {
        Column(
          modifier =
            Modifier
              .widthIn(max = ParkSpaceSpacing.maxReadableWidth)
              .fillMaxWidth()
              .verticalScroll(rememberScrollState())
              .padding(
                horizontal = ParkSpaceSpacing.screenHorizontal,
                vertical = ParkSpaceSpacing.md,
              ),
          verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.lg),
        ) {
          // Compact Global Header: Theme Toggle (left) • ParkSpace Branding (center) • Menu (right)
          ParkSpaceGlobalHeader(
            isDarkTheme = isDarkTheme,
            onToggleTheme = onToggleTheme,
            isSignedIn = activeUser != null,
            activeRole = activeUser?.role,
            onOpenSettings = {
              if (activeUser != null) {
                onNavigateToSettings()
              } else {
                showGuestSettingsDialog = true
              }
            },
            onOpenAdminGate = {
              gateAcknowledged = false
              showAdminGateDialog = true
            },
            onSignOut = if (activeUser != null) onSignOut else null,
          )

          // Clean Hero Section
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
          ) {
            ParkSpaceCompactDemoPill(modifier = Modifier.testTag("mode_banner"))

            Text(
              text = stringResource(R.string.welcome_hero_title),
              style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onBackground,
            )

            Text(
              text = stringResource(R.string.welcome_hero_subtitle),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          if (sandboxFeedback != null) {
            Surface(
              modifier = Modifier.fillMaxWidth().testTag("sandbox_feedback_banner"),
              color = MaterialTheme.colorScheme.errorContainer,
              contentColor = MaterialTheme.colorScheme.onErrorContainer,
              shape = MaterialTheme.shapes.medium,
            ) {
              Row(
                modifier = Modifier.padding(ParkSpaceSpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = sandboxFeedback,
                  style = MaterialTheme.typography.bodySmall,
                  modifier = Modifier.weight(1f),
                )
                TextButton(
                  onClick = onDismissFeedback,
                  modifier = Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget),
                ) {
                  Text("Dismiss")
                }
              }
            }
          }

          ParkSpaceStateContainer(state = uiState, onRetry = onRetrySeed) { data ->
            Column(verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.md)) {
              // Active Signed-In Session Card (concise, shown only when authenticated)
              val sessionUser = data.session.activeUser
              if (sessionUser != null) {
                val activeAccent =
                  if (sessionUser.role == UserRole.HOST) {
                    if (isDarkTheme) Brass400 else Brass600
                  } else {
                    if (isDarkTheme) Jade400 else Emerald900
                  }
                ParkSpaceGlassSurface(
                  modifier = Modifier.testTag("active_session_card"),
                  accentGlow =
                    if (sessionUser.role == UserRole.HOST) semantic.hostGlow
                    else semantic.customerGlow,
                  borderColor = activeAccent.copy(alpha = 0.55f),
                  contentPaddingDp = 18,
                ) {
                  ParkSpaceStatusPill(
                    label = "ACTIVE ${sessionUser.role.displayName.uppercase()} SESSION",
                    tone = StatusTone.AVAILABLE,
                    icon = Icons.Filled.CheckCircle,
                  )
                  Text(
                    text = sessionUser.fullName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                  )
                  Text(
                    text = "${sessionUser.emailMasked} • ${sessionUser.city.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Spacer(modifier = Modifier.height(ParkSpaceSpacing.xxs))
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    val targetRoute = ParkSpaceNavGuard.homeRouteForRole(sessionUser.role)
                    ParkSpaceLiquidGlassButton(
                      text = "Open ${sessionUser.role.displayName}",
                      onClick = { onNavigateToRoleHome(targetRoute) },
                      leadingIcon =
                        when (sessionUser.role) {
                          UserRole.CUSTOMER -> Icons.Outlined.DirectionsCar
                          UserRole.HOST -> Icons.Outlined.Storefront
                          UserRole.SUPER_ADMIN -> Icons.Outlined.AdminPanelSettings
                        },
                      accentColor = activeAccent,
                      contentColor = if (isDarkTheme) Emerald950 else Color.White,
                      modifier = Modifier.weight(1f).testTag("open_active_role_button"),
                    )
                    TextButton(
                      onClick = onSignOut,
                      modifier =
                        Modifier
                          .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                          .testTag("welcome_sign_out_button"),
                    ) {
                      Text("Sign Out")
                    }
                  }
                }
              }

              // Two Prominent Liquid Glass Role Cards: Customer & Host
              ParkSpaceLiquidGlassRoleCard(
                title = stringResource(R.string.role_customer),
                subtitle = stringResource(R.string.customer_entry_subtitle),
                pillLabel =
                  if (sessionUser?.role == UserRole.HOST) "Switch / Activate" else "Drivers",
                icon = Icons.Outlined.DirectionsCar,
                isDarkTheme = isDarkTheme,
                isPrimaryAccent = true,
                onClick = { onOpenAuth(UserRole.CUSTOMER, false) },
                modifier = Modifier.testTag("customer_entry_card"),
              )

              ParkSpaceLiquidGlassRoleCard(
                title = stringResource(R.string.role_host),
                subtitle = stringResource(R.string.host_entry_subtitle),
                pillLabel =
                  if (sessionUser?.role == UserRole.CUSTOMER) "Switch / Activate" else "Space Owners",
                icon = Icons.Outlined.Storefront,
                isDarkTheme = isDarkTheme,
                isPrimaryAccent = false,
                onClick = { onOpenAuth(UserRole.HOST, false) },
                modifier = Modifier.testTag("host_entry_card"),
              )

              Text(
                text = "One email supports both Customer & Host roles on this device",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = ParkSpaceSpacing.xs),
              )
            }
          }
        }
      }
    }
  }

  if (showGuestSettingsDialog) {
    ParkSpaceGuestSettingsDialog(
      isDarkTheme = isDarkTheme,
      onToggleTheme = onToggleTheme,
      onDismiss = { showGuestSettingsDialog = false },
    )
  }

  if (showAdminGateDialog) {
    ParkSpaceAdminSandboxGateDialog(
      acknowledged = gateAcknowledged,
      onToggleAcknowledge = { gateAcknowledged = it },
      onConfirmUnlock = {
        gateAcknowledged = false
        showAdminGateDialog = false
        onUnlockPrivateAdmin()
      },
      onDismiss = {
        gateAcknowledged = false
        showAdminGateDialog = false
      },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleShellScaffold(
  title: String,
  onBack: () -> Unit,
  onOpenSettings: () -> Unit,
  isDarkTheme: Boolean = false,
  showSettingsAction: Boolean = true,
  bottomBar: @Composable () -> Unit = {},
  content: @Composable () -> Unit,
) {
  ParkSpaceAtmosphericBackground(isDarkTheme = isDarkTheme) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = Color.Transparent,
      contentWindowInsets = WindowInsets.safeDrawing,
      bottomBar = bottomBar,
      topBar = {
        TopAppBar(
          title = {
            Text(
              text = title,
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
          },
          navigationIcon = {
            ParkSpaceGlassIconButton(
              onClick = onBack,
              icon = Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = "Back",
              testTag = "shell_back_button",
              modifier = Modifier.padding(start = ParkSpaceSpacing.xs),
            )
          },
          actions = {
            ParkSpaceCompactDemoPill(
              modifier = Modifier.padding(end = ParkSpaceSpacing.xs).testTag("mode_banner")
            )
            if (showSettingsAction) {
              ParkSpaceGlassIconButton(
                onClick = onOpenSettings,
                icon = Icons.Outlined.Settings,
                contentDescription = "Profile & Settings",
                testTag = "shell_settings_button",
                modifier = Modifier.padding(end = ParkSpaceSpacing.xs),
              )
            }
          },
          colors =
            TopAppBarDefaults.topAppBarColors(
              containerColor = Color.Transparent,
            ),
        )
      },
    ) { innerPadding ->
      Box(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentAlignment = Alignment.TopCenter,
      ) {
        Column(
          modifier =
            Modifier.widthIn(max = ParkSpaceSpacing.maxReadableWidth)
              .fillMaxWidth()
              .verticalScroll(rememberScrollState())
              .padding(
                horizontal = ParkSpaceSpacing.screenHorizontal,
                vertical = ParkSpaceSpacing.md,
              ),
          verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.md),
        ) {
          content()
        }
      }
    }
  }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun CustomerHomeShellScreen(
  data: FoundationDashboardData,
  onBack: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenDiscovery: () -> Unit = {},
  onOpenBookings: () -> Unit = {},
  onAddVehicle: (VehicleType, String, String) -> Unit = { _, _, _ -> },
  onUpdatePreferredCity: (SupportedCity) -> Unit = {},
  onOpenPaymentsAndReferrals: () -> Unit = {},
  onOpenMessages: () -> Unit = {},
  onOpenDisputes: () -> Unit = {},
  onOpenNotifications: () -> Unit = {},
  isDarkTheme: Boolean = false,
  onToggleTheme: () -> Unit = {},
  sandboxFeedback: String? = null,
  onDismissFeedback: () -> Unit = {},
  onOpenBookingDetail: (String) -> Unit = { onOpenBookings() },
  onOpenHostDashboardOrActivate: () -> Unit = {},
  onSignOut: () -> Unit = onBack,
  bottomBar: @Composable () -> Unit = {},
) {
  CustomerDashboardScreen(
    data = data,
    isDarkTheme = isDarkTheme,
    onToggleTheme = onToggleTheme,
    onOpenDiscovery = onOpenDiscovery,
    onOpenBookings = onOpenBookings,
    onOpenBookingDetail = onOpenBookingDetail,
    onOpenProfile = onOpenSettings,
    onAddVehicle = onAddVehicle,
    onUpdatePreferredCity = onUpdatePreferredCity,
    onOpenPaymentsAndReferrals = onOpenPaymentsAndReferrals,
    onOpenDisputes = onOpenDisputes,
    onOpenNotifications = onOpenNotifications,
    onOpenHostDashboardOrActivate = onOpenHostDashboardOrActivate,
    onSignOut = onSignOut,
    sandboxFeedback = sandboxFeedback,
    onDismissFeedback = onDismissFeedback,
    bottomBar = bottomBar,
  )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HostHomeShellScreen(
  data: FoundationDashboardData,
  onBack: () -> Unit,
  onOpenSettings: () -> Unit,
  onUpdateHostOnboarding: (String, Set<SupportedCity>) -> Unit = { _, _ -> },
) {
  val host = data.activeHostProfile
  val user = data.session.activeUser
  var showOnboardingDialog by rememberSaveable { mutableStateOf(false) }

  RoleShellScaffold(
    title = "Host Workspace",
    onBack = onBack,
    onOpenSettings = onOpenSettings,
  ) {
    val verificationStatus = host?.verificationStatus ?: HostVerificationStatus.PENDING_REVIEW
    val statusTone =
      when (verificationStatus) {
        HostVerificationStatus.PENDING_REVIEW -> StatusTone.PENDING
        HostVerificationStatus.APPROVED -> StatusTone.AVAILABLE
        HostVerificationStatus.REJECTED -> StatusTone.ERROR
      }

    ParkSpaceGlassSurface(
      modifier = Modifier.testTag("host_shell_card"),
      accentGlow = ParkSpaceThemeTokens.semantic.hostGlow,
      contentPaddingDp = 18,
    ) {
      ParkSpaceStatusPill(
        label = "Verification: ${verificationStatus.displayName}",
        tone = statusTone,
        modifier = Modifier.testTag("host_verification_status_pill"),
      )
      Text(
        text = host?.displayBusinessAlias ?: user?.fullName.orEmpty(),
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
      )
      Text(
        text =
          "Owner: ${user?.fullName.orEmpty()} (${user?.emailMasked.orEmpty()})\nOperating Cities: ${host?.operatingCities?.joinToString { it.displayName } ?: data.session.selectedCity.displayName}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text =
          "${data.activeHostSpaces.size} Owned Properties • ${data.activeUserBookings.size} Host Bookings",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.testTag("host_owned_counts_text"),
      )

      if (!verificationStatus.canPublishListings || !host?.verificationNote.isNullOrBlank()) {
        Surface(
          modifier = Modifier.fillMaxWidth().testTag("host_approval_restriction_banner"),
          shape = MaterialTheme.shapes.small,
          color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
        ) {
          Column(
            modifier = Modifier.padding(ParkSpaceSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xxs),
          ) {
            if (!verificationStatus.canPublishListings) {
              Text(
                text =
                  if (verificationStatus == HostVerificationStatus.PENDING_REVIEW) {
                    "Your Host account is PENDING_REVIEW. Public listing publication remains locked until Super Admin approval, while you can complete your Host onboarding details."
                  } else {
                    "Host verification is REJECTED. Public listing publication is blocked until moderation approval."
                  },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
            }
            val reviewNote = host?.verificationNote
            if (!reviewNote.isNullOrBlank()) {
              Text(
                text = "Admin review note: $reviewNote",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
              )
            }
          }
        }
      }

      if (data.activeHostSpaces.isEmpty()) {
        Text(
          text = "No parking spaces belong to this Host account yet.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.testTag("host_empty_spaces_text"),
        )
      } else {
        data.activeHostSpaces.forEach { space ->
          val statusLabel = if (space.isApproved) "Approved" else "Pending Review"
          Text(
            text = "• ${space.title} ($statusLabel • ${space.totalBays} bays)",
            style = MaterialTheme.typography.bodySmall,
          )
        }
      }

      Spacer(modifier = Modifier.height(ParkSpaceSpacing.xs))
      ParkSpaceLiquidGlassSecondaryButton(
        text = "Edit Host Onboarding Profile",
        leadingIcon = Icons.Outlined.Edit,
        onClick = { showOnboardingDialog = true },
        modifier = Modifier.fillMaxWidth().testTag("host_edit_onboarding_button"),
      )
    }

    ParkSpaceGlassSurface(
      accentGlow = ParkSpaceThemeTokens.semantic.hostGlow,
      contentPaddingDp = 18,
    ) {
      Text(
        text = "Host Operations",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
      )
      Text(
        text =
          "Manage property listings, bays & schedules, incoming booking approvals, and simulated PKR demo ledger payouts.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }

  if (showOnboardingDialog && host != null) {
    var aliasInput by rememberSaveable(host.userId) { mutableStateOf(host.displayBusinessAlias) }
    var selectedCities by rememberSaveable(host.userId) { mutableStateOf(host.operatingCities.toList()) }
    var errorMsg by rememberSaveable { mutableStateOf<String?>(null) }

    AlertDialog(
      onDismissRequest = { showOnboardingDialog = false },
      title = { Text("Host Onboarding Profile") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm)) {
          OutlinedTextField(
            value = aliasInput,
            onValueChange = {
              aliasInput = it
              errorMsg = null
            },
            label = { Text("Parking Business Alias") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("host_onboarding_alias_input"),
          )
          Text("Operating Cities", style = MaterialTheme.typography.labelLarge)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
            verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
          ) {
            SupportedCity.entries.forEach { city ->
              val isSelected = city in selectedCities
              FilterChip(
                selected = isSelected,
                onClick = {
                  selectedCities =
                    if (isSelected && selectedCities.size > 1) selectedCities - city
                    else if (!isSelected) selectedCities + city
                    else selectedCities
                },
                label = { Text(city.displayName) },
              )
            }
          }
          if (errorMsg != null) {
            Text(
              text = errorMsg.orEmpty(),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.error,
            )
          }
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            if (aliasInput.trim().length < 2) {
              errorMsg = "Business alias must be at least 2 characters."
            } else {
              onUpdateHostOnboarding(aliasInput.trim(), selectedCities.toSet())
              showOnboardingDialog = false
            }
          },
          modifier = Modifier.testTag("save_host_onboarding_button"),
        ) {
          Text("Save")
        }
      },
      dismissButton = {
        TextButton(onClick = { showOnboardingDialog = false }) { Text("Cancel") }
      },
    )
  }
}

@Composable
fun PrivateAdminHomeShellScreen(
  data: FoundationDashboardData,
  onBack: () -> Unit,
  onOpenSettings: () -> Unit,
) {
  RoleShellScaffold(
    title = "Private Admin Shell",
    onBack = onBack,
    onOpenSettings = onOpenSettings,
  ) {
    ParkSpaceGlassSurface(
      modifier = Modifier.testTag("admin_shell_card"),
      accentGlow = ParkSpaceThemeTokens.semantic.hostGlow,
      borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
      contentPaddingDp = 18,
    ) {
      ParkSpaceStatusPill(
        label = "PRIVATE ADMIN SANDBOX • NO PUBLIC SIGNUP",
        tone = StatusTone.ERROR,
      )
      Text(
        text = "Platform Moderation Boundary",
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
      )
      Text(
        text =
          "Total Spaces: ${data.allSpacesCount} (${data.approvedSpacesCount} approved, ${data.pendingSpacesCount} pending verification) • Demo Bookings: ${data.activeUserBookings.size}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Text(
        text =
          "Super Admin governs host KYC queue, space moderation, dispute resolution, and simulated commission rules with local audit logs.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun SharedProfileSettingsScreen(
  data: FoundationDashboardData,
  onBack: () -> Unit,
  onSignOut: () -> Unit,
  isDarkTheme: Boolean = false,
  onToggleTheme: (() -> Unit)? = null,
  bottomBar: @Composable () -> Unit = {},
) {
  val semantic = ParkSpaceThemeTokens.semantic
  RoleShellScaffold(
    title = "Profile & Account",
    onBack = onBack,
    onOpenSettings = {},
    isDarkTheme = isDarkTheme,
    showSettingsAction = false,
    bottomBar = bottomBar,
  ) {
    val user = data.session.activeUser
    ParkSpaceGlassSurface(
      modifier = Modifier.testTag("shared_settings_card"),
      accentGlow =
        if (user?.role == UserRole.HOST) semantic.hostGlow else semantic.customerGlow,
      contentPaddingDp = 18,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
      ) {
        Icon(
          imageVector = Icons.Outlined.Person,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = user?.fullName ?: "No Active Account",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        )
      }
      if (user != null) {
        Surface(
          shape = MaterialTheme.shapes.small,
          color = semantic.glassSurface,
          border = BorderStroke(1.dp, semantic.glassBorder),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(
            modifier = Modifier.padding(ParkSpaceSpacing.md),
            verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xxs),
          ) {
            Text(
              text = "Role: ${user.role.displayName} • City: ${user.city.displayName}",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            )
            Text(
              text = "Email: ${user.emailMasked}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "Account Scope: ${user.phoneMasked}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Text(
          text = "Appearance follows the header Light/Dark mode toggle across all screens.",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(ParkSpaceSpacing.xxs))
        ParkSpaceLiquidGlassSecondaryButton(
          text = "Sign Out Local Demo Account",
          leadingIcon = Icons.AutoMirrored.Outlined.Logout,
          onClick = onSignOut,
          accentColor = MaterialTheme.colorScheme.error,
          modifier = Modifier.fillMaxWidth().testTag("sign_out_sandbox_button"),
        )
      }
    }
  }
}

@Composable
fun AccessDeniedScreen(
  attemptedRouteName: String,
  reason: String,
  onReturnToEntry: () -> Unit,
  modifier: Modifier = Modifier,
  isDarkTheme: Boolean = false,
) {
  ParkSpaceAtmosphericBackground(isDarkTheme = isDarkTheme, modifier = modifier) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = Color.Transparent,
      contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
      Box(
        modifier = Modifier.fillMaxSize().padding(innerPadding).padding(ParkSpaceSpacing.lg),
        contentAlignment = Alignment.Center,
      ) {
        ParkSpaceGlassSurface(
          modifier = Modifier.widthIn(max = 480.dp).testTag("access_denied_card"),
          accentGlow = MaterialTheme.colorScheme.error.copy(alpha = 0.25f),
          borderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.55f),
          contentPaddingDp = 22,
        ) {
          Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            Icon(
              imageVector = Icons.Outlined.Block,
              contentDescription = "Access Denied",
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(40.dp),
            )
            Text(
              text = "Navigation Guard Blocked",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            )
            Text(
              text = "Destination: $attemptedRouteName",
              style = ParkSpaceMonoCodeStyle,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(text = reason, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(ParkSpaceSpacing.xxs))
            ParkSpaceLiquidGlassButton(
              text = "Return to Welcome Entry",
              onClick = onReturnToEntry,
              modifier = Modifier.fillMaxWidth().testTag("access_denied_return_button"),
            )
          }
        }
      }
    }
  }
}
