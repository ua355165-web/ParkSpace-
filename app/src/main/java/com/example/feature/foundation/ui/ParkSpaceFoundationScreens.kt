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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.common.UiState
import com.example.core.config.AppEnvironment
import com.example.core.config.SupportedCity
import com.example.core.config.UserRole
import com.example.core.designsystem.components.AddDemoVehicleDialog
import com.example.core.designsystem.components.ParkSpaceModeBanner
import com.example.core.designsystem.components.ParkSpacePrimaryButton
import com.example.core.designsystem.components.ParkSpaceStateContainer
import com.example.core.designsystem.components.ParkSpaceStatusPill
import com.example.core.designsystem.components.StatusTone
import com.example.core.domain.model.HostVerificationStatus
import com.example.core.domain.model.ParkingSpace
import com.example.core.domain.model.VehicleType
import com.example.core.navigation.ParkSpaceNavGuard
import com.example.core.navigation.ParkSpaceRoute
import com.example.feature.auth.domain.DemoAuthValidator
import com.example.feature.foundation.FoundationDashboardData
import com.example.ui.theme.ParkSpaceMonoCodeStyle
import com.example.ui.theme.ParkSpaceMonoPriceStyle
import com.example.ui.theme.ParkSpaceSpacing

@OptIn(ExperimentalLayoutApi::class)
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

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
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
        // Brand Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
          ) {
            Surface(
              color = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              shape = MaterialTheme.shapes.small,
              modifier = Modifier.size(44.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Filled.LocalParking,
                  contentDescription = stringResource(R.string.app_name),
                  modifier = Modifier.size(26.dp),
                )
              }
            }
            Column {
              Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
              )
              Text(
                text = stringResource(R.string.brand_tagline),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          Row(horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xxs)) {
            IconButton(
              onClick = onNavigateToSettings,
              modifier =
                Modifier.size(ParkSpaceSpacing.minTouchTarget).testTag("open_settings_button"),
            ) {
              Icon(
                imageVector = Icons.Outlined.Settings,
                contentDescription = stringResource(R.string.settings_icon_cd),
              )
            }
            IconButton(
              onClick = onToggleTheme,
              modifier =
                Modifier.size(ParkSpaceSpacing.minTouchTarget).testTag("theme_toggle_button"),
            ) {
              Icon(
                imageVector =
                  if (isDarkTheme) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                contentDescription = stringResource(R.string.theme_toggle_cd),
              )
            }
          }
        }

        ParkSpaceModeBanner(mode = AppEnvironment.currentMode)

        if (sandboxFeedback != null) {
          Surface(
            modifier = Modifier.fillMaxWidth().testTag("sandbox_feedback_banner"),
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            shape = MaterialTheme.shapes.small,
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
              TextButton(onClick = onDismissFeedback) { Text("Dismiss") }
            }
          }
        }

        ParkSpaceStateContainer(state = uiState, onRetry = onRetrySeed) { data ->
          Column(verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.md)) {
            // Active Signed-In Local Demo Session Banner (when user already signed in/persisted)
            val activeUser = data.session.activeUser
            if (activeUser != null) {
              Surface(
                modifier = Modifier.fillMaxWidth().testTag("active_session_card"),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
              ) {
                Column(
                  modifier = Modifier.padding(ParkSpaceSpacing.cardPadding),
                  verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
                ) {
                  ParkSpaceStatusPill(
                    label = DemoAuthValidator.DEMO_TRANSPARENCY_NOTICE,
                    tone = StatusTone.AVAILABLE,
                    icon = Icons.Filled.CheckCircle,
                  )
                  Text(
                    text = "Signed in as ${activeUser.fullName}",
                    style = MaterialTheme.typography.titleMedium,
                  )
                  Text(
                    text = "${activeUser.role.displayName} • ${activeUser.emailMasked} • ${activeUser.city.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
                  ) {
                    val targetRoute = ParkSpaceNavGuard.homeRouteForRole(activeUser.role)
                    ParkSpacePrimaryButton(
                      text = "Open ${activeUser.role.displayName} Workspace",
                      onClick = { onNavigateToRoleHome(targetRoute) },
                      leadingIcon =
                        when (activeUser.role) {
                          UserRole.CUSTOMER -> Icons.Outlined.DirectionsCar
                          UserRole.HOST -> Icons.Outlined.Storefront
                          UserRole.SUPER_ADMIN -> Icons.Outlined.AdminPanelSettings
                        },
                      modifier = Modifier.weight(1f).testTag("open_active_role_button"),
                    )
                    OutlinedButton(
                      onClick = onSignOut,
                      modifier =
                        Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                          .testTag("welcome_sign_out_button"),
                      shape = MaterialTheme.shapes.small,
                    ) {
                      Text("Sign Out")
                    }
                  }
                }
              }
            }

            // Local Demo Account Entry — Customer & Host (Sign In / Create Account)
            Surface(
              modifier = Modifier.fillMaxWidth().testTag("demo_auth_entry_card"),
              shape = MaterialTheme.shapes.medium,
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
              Column(
                modifier = Modifier.padding(ParkSpaceSpacing.cardPadding),
                verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Shield,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(20.dp),
                    )
                    Text(
                      text = stringResource(R.string.demo_sandbox_title),
                      style = MaterialTheme.typography.titleMedium,
                    )
                  }
                  ParkSpaceStatusPill(label = "LOCAL AUTH", tone = StatusTone.INFO)
                }

                Text(
                  text = DemoAuthValidator.DEMO_TRANSPARENCY_NOTICE,
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text =
                    "Create or sign in with your own local Demo email and password to manage your private vehicles, bookings, or host parking properties.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                // Customer Role Entry
                RoleAuthActionCard(
                  title = "Customer",
                  subtitle =
                    "Discover parking in 4 cities, manage your vehicles, and track bookings & entry passes.",
                  icon = Icons.Outlined.DirectionsCar,
                  onSignIn = { onOpenAuth(UserRole.CUSTOMER, false) },
                  onRegister = { onOpenAuth(UserRole.CUSTOMER, true) },
                  cardTag = "customer_entry_card",
                  signInTag = "customer_sign_in_button",
                  registerTag = "customer_register_button",
                )

                // Host Role Entry
                RoleAuthActionCard(
                  title = "Host",
                  subtitle =
                    "Register a local Host account (starts Pending Review) to manage onboarding, lots, bays, and reservations.",
                  icon = Icons.Outlined.Storefront,
                  onSignIn = { onOpenAuth(UserRole.HOST, false) },
                  onRegister = { onOpenAuth(UserRole.HOST, true) },
                  cardTag = "host_entry_card",
                  signInTag = "host_sign_in_button",
                  registerTag = "host_register_button",
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Private Admin Sandbox Gate — intentionally separated from public accounts
                OutlinedButton(
                  onClick = {
                    gateAcknowledged = false
                    showAdminGateDialog = true
                  },
                  modifier =
                    Modifier.fillMaxWidth()
                      .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                      .testTag("private_admin_gate_button"),
                  shape = MaterialTheme.shapes.small,
                ) {
                  Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.size(ParkSpaceSpacing.xs))
                  Text("Private Super Admin Sandbox Gate (No Public Signup)")
                }
              }
            }

            // City Selector + Live Marketplace Availability (4 Supported Cities)
            Surface(
              modifier = Modifier.fillMaxWidth().testTag("city_inventory_preview_card"),
              shape = MaterialTheme.shapes.medium,
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
              Column(
                modifier = Modifier.padding(ParkSpaceSpacing.cardPadding),
                verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
                ) {
                  Icon(
                    imageVector = Icons.Outlined.LocationCity,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                  )
                  Text(
                    text = stringResource(R.string.supported_cities_label),
                    style = MaterialTheme.typography.titleMedium,
                  )
                }

                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
                  verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
                ) {
                  SupportedCity.entries.forEach { city ->
                    FilterChip(
                      selected = city == data.session.selectedCity,
                      onClick = { onSelectCity(city) },
                      label = { Text("${city.displayName} (${city.provinceLabel})") },
                      modifier = Modifier.testTag("city_chip_${city.name.lowercase()}"),
                      colors =
                        FilterChipDefaults.filterChipColors(
                          selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                          selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    )
                  }
                }

                if (data.approvedSpacesInSelectedCity.isEmpty()) {
                  Text(
                    text = stringResource(R.string.empty_customer_marketplace),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.testTag("welcome_city_empty_state"),
                  )
                  Text(
                    text =
                      "Hosts can register and list parking spaces in ${data.session.selectedCity.displayName}. Exact locations remain masked (~${AppEnvironment.PRE_BOOKING_LOCATION_MASK_RADIUS_METERS}m) prior to confirmed booking.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                } else {
                  Text(
                    text =
                      "${data.approvedSpacesInSelectedCity.size} approved parking spaces in ${data.session.selectedCity.displayName} (~${AppEnvironment.PRE_BOOKING_LOCATION_MASK_RADIUS_METERS}m public mask active):",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )

                  data.approvedSpacesInSelectedCity.forEach { space ->
                    val masked = data.maskedLocationFor(space)
                    DemoSpacePreviewRow(space = space, areaLabel = masked.areaLabel)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  if (showAdminGateDialog) {
    AlertDialog(
      onDismissRequest = {
        gateAcknowledged = false
        showAdminGateDialog = false
      },
      icon = {
        Icon(
          imageVector = Icons.Outlined.AdminPanelSettings,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.error,
        )
      },
      title = { Text("Private Super Admin Demo Gate") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm)) {
          Text(
            "Super Admin has NO public registration, NO Customer/Host access, and NO pre-created database account. Unlocking this console enters a private local Demo Governance Sandbox; production admin access requires server-verified Firebase Custom Claims."
          )
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                .clickable { gateAcknowledged = !gateAcknowledged },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
          ) {
            Checkbox(
              checked = gateAcknowledged,
              onCheckedChange = { gateAcknowledged = it },
              modifier = Modifier.testTag("admin_gate_acknowledge_checkbox"),
            )
            Text(
              text = "I acknowledge this is a private local Demo Admin Sandbox.",
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            gateAcknowledged = false
            showAdminGateDialog = false
            onUnlockPrivateAdmin()
          },
          enabled = gateAcknowledged,
          modifier = Modifier.testTag("confirm_admin_sandbox_button"),
        ) {
          Text("Acknowledge & Open Sandbox")
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            gateAcknowledged = false
            showAdminGateDialog = false
          }
        ) {
          Text("Cancel")
        }
      },
    )
  }
}

@Composable
private fun RoleAuthActionCard(
  title: String,
  subtitle: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  onSignIn: () -> Unit,
  onRegister: () -> Unit,
  cardTag: String,
  signInTag: String,
  registerTag: String,
) {
  Surface(
    modifier = Modifier.fillMaxWidth().testTag(cardTag),
    shape = MaterialTheme.shapes.small,
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Column(
      modifier = Modifier.padding(ParkSpaceSpacing.sm),
      verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
        Text(text = title, style = MaterialTheme.typography.titleSmall)
      }
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
      ) {
        OutlinedButton(
          onClick = onSignIn,
          modifier =
            Modifier.weight(1f)
              .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
              .testTag(signInTag),
          shape = MaterialTheme.shapes.small,
        ) {
          Text("Sign In")
        }
        ParkSpacePrimaryButton(
          text = "Create Account",
          onClick = onRegister,
          modifier = Modifier.weight(1f).testTag(registerTag),
        )
      }
    }
  }
}

@Composable
private fun DemoSpacePreviewRow(
  space: ParkingSpace,
  areaLabel: String,
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    color = MaterialTheme.colorScheme.surfaceVariant,
    shape = MaterialTheme.shapes.small,
  ) {
    Column(
      modifier = Modifier.padding(ParkSpaceSpacing.sm),
      verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xxs),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = space.title,
          style = MaterialTheme.typography.titleSmall,
          modifier = Modifier.weight(1f),
        )
        Text(
          text = "${AppEnvironment.CURRENCY_SYMBOL} ${space.hourlyRatePkr}/hr",
          style = ParkSpaceMonoPriceStyle,
          color = MaterialTheme.colorScheme.primary,
        )
      }
      Text(
        text =
          "$areaLabel • ${space.parkingType.displayName} • ${space.availableBays}/${space.totalBays} bays • ${space.schedule.formattedHours()}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoleShellScaffold(
  title: String,
  onBack: () -> Unit,
  onOpenSettings: () -> Unit,
  content: @Composable () -> Unit,
) {
  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        title = { Text(title, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier =
              Modifier.size(ParkSpaceSpacing.minTouchTarget).testTag("shell_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = "Back",
            )
          }
        },
        actions = {
          IconButton(
            onClick = onOpenSettings,
            modifier =
              Modifier.size(ParkSpaceSpacing.minTouchTarget).testTag("shell_settings_button"),
          ) {
            Icon(imageVector = Icons.Outlined.Settings, contentDescription = "Profile & Settings")
          }
        },
        colors =
          TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background,
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
        ParkSpaceModeBanner(mode = AppEnvironment.currentMode)
        content()
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
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
) {
  var showAddVehicleDialog by rememberSaveable { mutableStateOf(false) }

  RoleShellScaffold(
    title = "Customer Workspace",
    onBack = onBack,
    onOpenSettings = onOpenSettings,
  ) {
    val user = data.session.activeUser
    val preferredCity = data.activeCustomerProfile?.preferredCity ?: data.session.selectedCity

    Surface(
      modifier = Modifier.fillMaxWidth().testTag("customer_shell_card"),
      shape = MaterialTheme.shapes.medium,
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
      Column(
        modifier = Modifier.padding(ParkSpaceSpacing.cardPadding),
        verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
      ) {
        ParkSpaceStatusPill(
          label = DemoAuthValidator.DEMO_TRANSPARENCY_NOTICE,
          tone = StatusTone.INFO,
        )
        Text(text = user?.fullName ?: "Customer", style = MaterialTheme.typography.titleLarge)
        Text(
          text =
            "${user?.emailMasked.orEmpty()} • Preferred City: ${preferredCity.displayName}\n${data.activeUserVehicles.size} Owned Vehicles • ${data.activeUserBookings.size} Account Bookings • Demo Credits: Rs. ${data.availableDemoCreditPkr}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(ParkSpaceSpacing.xxs))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("My Vehicles", style = MaterialTheme.typography.titleSmall)
          OutlinedButton(
            onClick = { showAddVehicleDialog = true },
            modifier =
              Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                .testTag("customer_add_vehicle_button"),
            shape = MaterialTheme.shapes.small,
          ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.size(4.dp))
            Text("Add Vehicle")
          }
        }

        if (data.activeUserVehicles.isEmpty()) {
          Text(
            text = "No vehicles saved to your account yet. Add a vehicle here or during booking checkout.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.testTag("customer_empty_vehicles_text"),
          )
        } else {
          data.activeUserVehicles.forEach { vehicle ->
            Text(
              text = "• ${vehicle.label} (${vehicle.vehicleType.displayName}) — ${vehicle.plateNumberMasked}",
              style = ParkSpaceMonoCodeStyle,
              modifier = Modifier.testTag("customer_vehicle_row_${vehicle.id}"),
            )
          }
        }

        Spacer(modifier = Modifier.height(ParkSpaceSpacing.xs))
        Text("Preferred City Preference", style = MaterialTheme.typography.labelLarge)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
          verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        ) {
          SupportedCity.entries.forEach { city ->
            FilterChip(
              selected = city == preferredCity,
              onClick = { onUpdatePreferredCity(city) },
              label = { Text(city.displayName) },
              modifier = Modifier.testTag("customer_pref_city_${city.name.lowercase()}"),
            )
          }
        }

        Spacer(modifier = Modifier.height(ParkSpaceSpacing.xs))
        ParkSpacePrimaryButton(
          text = "Discover Parking (Map & Filters)",
          onClick = onOpenDiscovery,
          leadingIcon = Icons.Outlined.DirectionsCar,
          modifier = Modifier.fillMaxWidth().testTag("open_discovery_button"),
        )
        OutlinedButton(
          onClick = onOpenBookings,
          modifier =
            Modifier.fillMaxWidth()
              .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
              .testTag("open_customer_bookings_button"),
          shape = MaterialTheme.shapes.small,
        ) {
          Text("My Bookings & Demo Entry Passes")
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        ) {
          OutlinedButton(
            onClick = onOpenPaymentsAndReferrals,
            modifier =
              Modifier.weight(1f)
                .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                .testTag("open_customer_payments_referrals_button"),
            shape = MaterialTheme.shapes.small,
          ) {
            Text("Payments & Referrals")
          }
          OutlinedButton(
            onClick = onOpenMessages,
            modifier =
              Modifier.weight(1f)
                .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                .testTag("open_customer_messages_button"),
            shape = MaterialTheme.shapes.small,
          ) {
            Text(
              if (data.unreadChatsCount > 0) "Messages (${data.unreadChatsCount})"
              else "Messages"
            )
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        ) {
          OutlinedButton(
            onClick = onOpenNotifications,
            modifier =
              Modifier.weight(1f)
                .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                .testTag("open_customer_notifications_button"),
            shape = MaterialTheme.shapes.small,
          ) {
            Text(
              if (data.unreadNotificationsCount > 0) "Alerts (${data.unreadNotificationsCount})"
              else "Notifications"
            )
          }
          OutlinedButton(
            onClick = onOpenDisputes,
            modifier =
              Modifier.weight(1f)
                .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                .testTag("open_customer_disputes_button"),
            shape = MaterialTheme.shapes.small,
          ) {
            Text("Complaints")
          }
        }
      }
    }
  }

  if (showAddVehicleDialog) {
    AddDemoVehicleDialog(
      onDismiss = { showAddVehicleDialog = false },
      onSaveVehicle = { type, label, plate ->
        onAddVehicle(type, label, plate)
        showAddVehicleDialog = false
      },
    )
  }
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

    Surface(
      modifier = Modifier.fillMaxWidth().testTag("host_shell_card"),
      shape = MaterialTheme.shapes.medium,
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
      Column(
        modifier = Modifier.padding(ParkSpaceSpacing.cardPadding),
        verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
      ) {
        ParkSpaceStatusPill(
          label = DemoAuthValidator.DEMO_TRANSPARENCY_NOTICE,
          tone = StatusTone.INFO,
        )
        ParkSpaceStatusPill(
          label = "Verification: ${verificationStatus.displayName}",
          tone = statusTone,
          modifier = Modifier.testTag("host_verification_status_pill"),
        )
        Text(
          text = host?.displayBusinessAlias ?: user?.fullName.orEmpty(),
          style = MaterialTheme.typography.titleLarge,
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
        OutlinedButton(
          onClick = { showOnboardingDialog = true },
          modifier =
            Modifier.fillMaxWidth()
              .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
              .testTag("host_edit_onboarding_button"),
          shape = MaterialTheme.shapes.small,
        ) {
          Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.size(ParkSpaceSpacing.xs))
          Text("Edit Host Onboarding Profile")
        }
      }
    }

    Surface(
      modifier = Modifier.fillMaxWidth(),
      shape = MaterialTheme.shapes.medium,
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
      Column(
        modifier = Modifier.padding(ParkSpaceSpacing.cardPadding),
        verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
      ) {
        Text(text = "Upcoming Host Milestone", style = MaterialTheme.typography.titleMedium)
        Text(
          text =
            "Milestone 4 adds property listing wizard, bay & schedule editor, incoming booking approvals, and PKR payout ledger.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
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
    Surface(
      modifier = Modifier.fillMaxWidth().testTag("admin_shell_card"),
      shape = MaterialTheme.shapes.medium,
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
      Column(
        modifier = Modifier.padding(ParkSpaceSpacing.cardPadding),
        verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
      ) {
        ParkSpaceStatusPill(
          label = "PRIVATE ADMIN SANDBOX • NO PUBLIC SIGNUP",
          tone = StatusTone.ERROR,
        )
        Text(
          text = "Platform Moderation Boundary",
          style = MaterialTheme.typography.titleLarge,
        )
        Text(
          text =
            "Total Spaces: ${data.allSpacesCount} (${data.approvedSpacesCount} approved, ${data.pendingSpacesCount} pending verification) • Demo Bookings: ${data.activeUserBookings.size}",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text =
            "Milestone 6 will wire host verification approval queue, space approval moderation, dispute mediation, and audit logs. Milestone 8 enforces server-verified Custom Claims.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
fun SharedProfileSettingsScreen(
  data: FoundationDashboardData,
  onBack: () -> Unit,
  onSignOut: () -> Unit,
) {
  RoleShellScaffold(
    title = "Demo Profile & Settings",
    onBack = onBack,
    onOpenSettings = {},
  ) {
    val user = data.session.activeUser
    Surface(
      modifier = Modifier.fillMaxWidth().testTag("shared_settings_card"),
      shape = MaterialTheme.shapes.medium,
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
      Column(
        modifier = Modifier.padding(ParkSpaceSpacing.cardPadding),
        verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
      ) {
        ParkSpaceStatusPill(
          label = DemoAuthValidator.DEMO_TRANSPARENCY_NOTICE,
          tone = StatusTone.INFO,
        )
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
            text = user?.fullName ?: "No Active Demo Account",
            style = MaterialTheme.typography.titleLarge,
          )
        }
        if (user != null) {
          Text(
            text =
              "Role: ${user.role.displayName} • City: ${user.city.displayName}\nAccount Email: ${user.emailMasked}\nAccount Type: ${user.phoneMasked}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          OutlinedButton(
            onClick = onSignOut,
            modifier =
              Modifier.fillMaxWidth()
                .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                .testTag("sign_out_sandbox_button"),
            shape = MaterialTheme.shapes.small,
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.Logout,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(ParkSpaceSpacing.xs))
            Text("Sign Out Local Demo Account")
          }
        }
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
) {
  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding).padding(ParkSpaceSpacing.lg),
      contentAlignment = Alignment.Center,
    ) {
      Surface(
        modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth().testTag("access_denied_card"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
      ) {
        Column(
          modifier = Modifier.padding(ParkSpaceSpacing.lg),
          verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Icon(
            imageVector = Icons.Outlined.Block,
            contentDescription = "Access Denied",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(40.dp),
          )
          Text(text = "Navigation Guard Blocked", style = MaterialTheme.typography.titleLarge)
          Text(
            text = "Destination: $attemptedRouteName",
            style = ParkSpaceMonoCodeStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(text = reason, style = MaterialTheme.typography.bodyMedium)
          ParkSpacePrimaryButton(
            text = "Return to Welcome Entry",
            onClick = onReturnToEntry,
            modifier = Modifier.fillMaxWidth().testTag("access_denied_return_button"),
          )
        }
      }
    }
  }
}
