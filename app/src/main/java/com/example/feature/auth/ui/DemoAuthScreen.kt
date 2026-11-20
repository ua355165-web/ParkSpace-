package com.example.feature.auth.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.core.config.SupportedCity
import com.example.core.config.UserRole
import com.example.core.designsystem.components.ParkSpaceAtmosphericBackground
import com.example.core.designsystem.components.ParkSpaceCompactDemoPill
import com.example.core.designsystem.components.ParkSpaceGlassIconButton
import com.example.core.designsystem.components.ParkSpaceGlassSurface
import com.example.core.designsystem.components.ParkSpaceLiquidGlassButton
import com.example.core.designsystem.components.ParkSpaceStatusPill
import com.example.core.designsystem.components.StatusTone
import com.example.feature.auth.AuthFormMode
import com.example.feature.auth.DemoAuthUiState
import com.example.feature.auth.domain.DemoAuthValidator
import com.example.ui.theme.Brass400
import com.example.ui.theme.Brass600
import com.example.ui.theme.Emerald900
import com.example.ui.theme.Emerald950
import com.example.ui.theme.Jade400
import com.example.ui.theme.ParkSpaceSpacing
import com.example.ui.theme.ParkSpaceThemeTokens

@Suppress("UNUSED_PARAMETER")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DemoAuthScreen(
  uiState: DemoAuthUiState,
  onBack: () -> Unit,
  onSelectMode: (AuthFormMode) -> Unit,
  onFullNameChange: (String) -> Unit,
  onEmailChange: (String) -> Unit,
  onPasswordChange: (String) -> Unit,
  onConfirmPasswordChange: (String) -> Unit,
  onBusinessAliasChange: (String) -> Unit,
  onSelectCity: (SupportedCity) -> Unit,
  onSubmit: () -> Unit,
  modifier: Modifier = Modifier,
  isDarkTheme: Boolean = false,
  onToggleTheme: (() -> Unit)? = null,
  onDualRoleBusinessAliasChange: (String) -> Unit = {},
  onActivateOrSwitchCompanionRole: () -> Unit = {},
) {
  var passwordVisible by rememberSaveable { mutableStateOf(false) }
  var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

  val semantic = ParkSpaceThemeTokens.semantic
  val isCustomer = uiState.role == UserRole.CUSTOMER
  val roleTitle = if (isCustomer) "Customer Account" else "Host Account"
  val roleAccent =
    if (isCustomer) {
      if (isDarkTheme) Jade400 else Emerald900
    } else {
      if (isDarkTheme) Brass400 else Brass600
    }
  val roleGlow = if (isCustomer) semantic.customerGlow else semantic.hostGlow
  val buttonContentColor =
    if (isDarkTheme) Emerald950 else Color.White

  ParkSpaceAtmosphericBackground(isDarkTheme = isDarkTheme, modifier = modifier) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = Color.Transparent,
      contentWindowInsets = WindowInsets.safeDrawing,
      topBar = {
        TopAppBar(
          title = {
            Column {
              Text(
                text = roleTitle,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              )
              Text(
                text = if (isCustomer) "Find & book secure parking" else "Manage lots, bays & payouts",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          },
          navigationIcon = {
            ParkSpaceGlassIconButton(
              onClick = onBack,
              icon = Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = "Back",
              testTag = "auth_back_button",
              modifier = Modifier.padding(start = ParkSpaceSpacing.xs),
            )
          },
          actions = {
            ParkSpaceCompactDemoPill(
              modifier = Modifier.padding(end = ParkSpaceSpacing.xs),
            )
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
            Modifier
              .widthIn(max = ParkSpaceSpacing.maxReadableWidth)
              .fillMaxWidth()
              .verticalScroll(rememberScrollState())
              .padding(
                horizontal = ParkSpaceSpacing.screenHorizontal,
                vertical = ParkSpaceSpacing.md,
              ),
          verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.md),
        ) {
          // Role Hero Glass Card
          ParkSpaceGlassSurface(
            modifier = Modifier.testTag("demo_auth_transparency_card"),
            accentGlow = roleGlow,
            borderColor = roleAccent.copy(alpha = 0.35f),
            contentPaddingDp = 18,
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
            ) {
              Box(
                modifier =
                  Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(roleAccent.copy(alpha = if (isDarkTheme) 0.20f else 0.12f))
                    .border(
                      1.dp,
                      roleAccent.copy(alpha = 0.40f),
                      RoundedCornerShape(14.dp),
                    ),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector =
                    if (isCustomer) Icons.Outlined.DirectionsCar else Icons.Outlined.Storefront,
                  contentDescription = null,
                  tint = roleAccent,
                  modifier = Modifier.size(24.dp),
                )
              }

              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                ParkSpaceStatusPill(
                  label = "Local Demo",
                  tone = if (isCustomer) StatusTone.AVAILABLE else StatusTone.PENDING,
                )
                Text(
                  text =
                    if (uiState.mode == AuthFormMode.SIGN_IN) {
                      "Sign in as ${uiState.role.displayName}"
                    } else {
                      "Create ${uiState.role.displayName} account"
                    },
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                )
              }
            }

            Text(
              text =
                "Stored securely on-device with salted password hashing. No OTP or email check required.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          // Dual-Role Companion Flow Card (when signed in under the other public role)
          val activeCompanionUser = uiState.signedInUserForDualRole
          if (activeCompanionUser != null) {
            ParkSpaceGlassSurface(
              modifier = Modifier.testTag("dual_role_companion_card"),
              accentGlow = roleGlow,
              borderColor = roleAccent.copy(alpha = 0.50f),
              contentPaddingDp = 16,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
              ) {
                Icon(
                  imageVector = Icons.Outlined.SwapHoriz,
                  contentDescription = null,
                  tint = roleAccent,
                  modifier = Modifier.size(20.dp),
                )
                ParkSpaceStatusPill(
                  label = "DUAL-ROLE ACCOUNT",
                  tone = StatusTone.INFO,
                )
              }

              Text(
                text =
                  if (uiState.companionRoleAlreadyExists) {
                    "Your ${uiState.role.displayName} role is ready on ${activeCompanionUser.emailMasked}"
                  } else {
                    "Activate ${uiState.role.displayName} role on ${activeCompanionUser.emailMasked}"
                  },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
              )

              Text(
                text =
                  if (uiState.companionRoleAlreadyExists) {
                    "Currently signed in as ${activeCompanionUser.role.displayName}. Switch workspaces in one tap without re-entering credentials."
                  } else if (isCustomer) {
                    "Uses your signed-in identity (${activeCompanionUser.fullName}) so you can book parking seamlessly alongside hosting."
                  } else {
                    "Links a separate Host profile to ${activeCompanionUser.emailMasked}. New Host profiles start in PENDING_REVIEW until Admin approval."
                  },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )

              if (!uiState.companionRoleAlreadyExists && !isCustomer) {
                OutlinedTextField(
                  value = uiState.dualRoleBusinessAlias,
                  onValueChange = onDualRoleBusinessAliasChange,
                  label = { Text("Parking Business Alias (Optional)") },
                  placeholder = { Text("e.g. ${activeCompanionUser.fullName} Parking") },
                  leadingIcon = { Icon(Icons.Outlined.Badge, contentDescription = null) },
                  singleLine = true,
                  keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                  modifier = Modifier.fillMaxWidth().testTag("dual_role_business_alias_input"),
                )
              }

              ParkSpaceLiquidGlassButton(
                text =
                  if (uiState.isSubmitting) {
                    "Switching Role..."
                  } else if (uiState.companionRoleAlreadyExists) {
                    "Switch to ${uiState.role.displayName}"
                  } else {
                    "Activate ${uiState.role.displayName} Role"
                  },
                onClick = onActivateOrSwitchCompanionRole,
                enabled = !uiState.isSubmitting,
                isLoading = uiState.isSubmitting,
                leadingIcon = Icons.Outlined.SwapHoriz,
                accentColor = roleAccent,
                contentColor = buttonContentColor,
                modifier =
                  Modifier
                    .fillMaxWidth()
                    .testTag(
                      if (uiState.companionRoleAlreadyExists) "dual_role_switch_button"
                      else "dual_role_activate_button"
                    ),
              )
            }
          }

          // Liquid Glass Mode Switcher: Sign In vs Create Account
          LiquidModeSwitcher(
            selectedMode = uiState.mode,
            accentColor = roleAccent,
            isDarkTheme = isDarkTheme,
            onSelectMode = onSelectMode,
          )

          // Credentials Form Glass Card
          ParkSpaceGlassSurface(
            modifier = Modifier.testTag("auth_form_card"),
            accentGlow = roleGlow,
            contentPaddingDp = 18,
          ) {
            AnimatedVisibility(visible = uiState.mode == AuthFormMode.REGISTER) {
              Column(verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm)) {
                OutlinedTextField(
                  value = uiState.fullName,
                  onValueChange = onFullNameChange,
                  label = { Text("Full Name") },
                  placeholder = { Text("e.g. Tariq Mahmood") },
                  leadingIcon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                  singleLine = true,
                  keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                  modifier = Modifier.fillMaxWidth().testTag("auth_full_name_input"),
                )
              }
            }

            OutlinedTextField(
              value = uiState.email,
              onValueChange = onEmailChange,
              label = { Text("Email Address") },
              placeholder = { Text("name@example.com") },
              leadingIcon = { Icon(Icons.Outlined.Email, contentDescription = null) },
              singleLine = true,
              keyboardOptions =
                KeyboardOptions(
                  keyboardType = KeyboardType.Email,
                  imeAction = ImeAction.Next,
                ),
              modifier = Modifier.fillMaxWidth().testTag("auth_email_input"),
            )

            OutlinedTextField(
              value = uiState.password,
              onValueChange = onPasswordChange,
              label = { Text("Password") },
              placeholder = {
                Text(
                  if (uiState.mode == AuthFormMode.REGISTER) {
                    "Minimum ${DemoAuthValidator.MIN_PASSWORD_LENGTH} characters"
                  } else {
                    "Enter your password"
                  }
                )
              },
              leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
              trailingIcon = {
                IconButton(
                  onClick = { passwordVisible = !passwordVisible },
                  modifier =
                    Modifier
                      .size(ParkSpaceSpacing.minTouchTarget)
                      .testTag("auth_toggle_password_visibility"),
                ) {
                  Icon(
                    imageVector =
                      if (passwordVisible) Icons.Outlined.VisibilityOff
                      else Icons.Outlined.Visibility,
                    contentDescription =
                      if (passwordVisible) "Hide password" else "Show password",
                  )
                }
              },
              visualTransformation =
                if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
              singleLine = true,
              keyboardOptions =
                KeyboardOptions(
                  keyboardType = KeyboardType.Password,
                  imeAction =
                    if (uiState.mode == AuthFormMode.REGISTER) ImeAction.Next else ImeAction.Done,
                ),
              keyboardActions =
                KeyboardActions(
                  onDone = {
                    if (uiState.mode == AuthFormMode.SIGN_IN && !uiState.isSubmitting) {
                      onSubmit()
                    }
                  }
                ),
              modifier = Modifier.fillMaxWidth().testTag("auth_password_input"),
            )

            if (uiState.mode == AuthFormMode.REGISTER) {
              OutlinedTextField(
                value = uiState.confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = { Text("Confirm Password") },
                placeholder = { Text("Re-enter password") },
                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null) },
                trailingIcon = {
                  IconButton(
                    onClick = { confirmPasswordVisible = !confirmPasswordVisible },
                    modifier =
                      Modifier
                        .size(ParkSpaceSpacing.minTouchTarget)
                        .testTag("auth_toggle_confirm_visibility"),
                  ) {
                    Icon(
                      imageVector =
                        if (confirmPasswordVisible) Icons.Outlined.VisibilityOff
                        else Icons.Outlined.Visibility,
                      contentDescription =
                        if (confirmPasswordVisible) "Hide confirm password"
                        else "Show confirm password",
                    )
                  }
                },
                visualTransformation =
                  if (confirmPasswordVisible) VisualTransformation.None
                  else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions =
                  KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = if (isCustomer) ImeAction.Done else ImeAction.Next,
                  ),
                keyboardActions =
                  KeyboardActions(
                    onDone = {
                      if (isCustomer && !uiState.isSubmitting) onSubmit()
                    }
                  ),
                modifier = Modifier.fillMaxWidth().testTag("auth_confirm_password_input"),
              )

              if (!isCustomer) {
                OutlinedTextField(
                  value = uiState.businessAlias,
                  onValueChange = onBusinessAliasChange,
                  label = { Text("Parking Business Alias (Optional)") },
                  placeholder = { Text("e.g. Liberty Commercial Bays") },
                  leadingIcon = { Icon(Icons.Outlined.Badge, contentDescription = null) },
                  singleLine = true,
                  keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                  keyboardActions =
                    KeyboardActions(
                      onDone = {
                        if (!uiState.isSubmitting) onSubmit()
                      }
                    ),
                  modifier = Modifier.fillMaxWidth().testTag("auth_business_alias_input"),
                )
              }

              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Preferred City",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
              )
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
              ) {
                SupportedCity.entries.forEach { city ->
                  FilterChip(
                    selected = uiState.selectedCity == city,
                    onClick = { onSelectCity(city) },
                    label = { Text(city.displayName) },
                    shape = CircleShape,
                    colors =
                      FilterChipDefaults.filterChipColors(
                        selectedContainerColor = roleAccent.copy(alpha = if (isDarkTheme) 0.24f else 0.14f),
                        selectedLabelColor = roleAccent,
                      ),
                    modifier =
                      Modifier
                        .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                        .testTag("auth_city_${city.name.lowercase()}"),
                  )
                }
              }

              if (!isCustomer) {
                Surface(
                  modifier = Modifier.fillMaxWidth().testTag("host_pending_review_notice"),
                  shape = MaterialTheme.shapes.small,
                  color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                ) {
                  Row(
                    modifier = Modifier.padding(ParkSpaceSpacing.sm),
                    horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Icon(
                      imageVector = Icons.Outlined.Info,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.secondary,
                      modifier = Modifier.size(18.dp),
                    )
                    Text(
                      text =
                        "New Host accounts start in PENDING_REVIEW status. Dashboard access opens immediately; listing publication unlocks upon Admin approval.",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                  }
                }
              }
            }

            if (uiState.errorMessage != null) {
              Surface(
                modifier = Modifier.fillMaxWidth().testTag("auth_error_banner"),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
              ) {
                Row(
                  modifier = Modifier.padding(ParkSpaceSpacing.sm),
                  horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.size(18.dp),
                  )
                  Text(
                    text = uiState.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                  )
                }
              }
            }

            if (uiState.successMessage != null) {
              Surface(
                modifier = Modifier.fillMaxWidth().testTag("auth_success_banner"),
                shape = RoundedCornerShape(14.dp),
                color = semantic.availableContainer,
                border = BorderStroke(1.dp, semantic.available.copy(alpha = 0.4f)),
              ) {
                Row(
                  modifier = Modifier.padding(ParkSpaceSpacing.sm),
                  horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = semantic.available,
                    modifier = Modifier.size(18.dp),
                  )
                  Text(
                    text = uiState.successMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = semantic.available,
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(ParkSpaceSpacing.xxs))

            ParkSpaceLiquidGlassButton(
              text =
                if (uiState.isSubmitting) {
                  "Authenticating..."
                } else if (uiState.mode == AuthFormMode.SIGN_IN) {
                  "Sign In as ${uiState.role.displayName}"
                } else {
                  "Create ${uiState.role.displayName} Account"
                },
              onClick = onSubmit,
              enabled = !uiState.isSubmitting,
              isLoading = uiState.isSubmitting,
              accentColor = roleAccent,
              contentColor = buttonContentColor,
              modifier = Modifier.fillMaxWidth().testTag("auth_submit_button"),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun LiquidModeSwitcher(
  selectedMode: AuthFormMode,
  accentColor: Color,
  isDarkTheme: Boolean,
  onSelectMode: (AuthFormMode) -> Unit,
) {
  val semantic = ParkSpaceThemeTokens.semantic
  val modes = listOf(AuthFormMode.SIGN_IN to "Sign In", AuthFormMode.REGISTER to "Create Account")

  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(CircleShape)
        .background(semantic.glassSurface)
        .border(1.dp, semantic.glassBorder, CircleShape)
        .padding(4.dp)
        .testTag("auth_mode_selector"),
    horizontalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    modes.forEach { (mode, label) ->
      val isSelected = selectedMode == mode
      val pillColor by animateColorAsState(
        targetValue =
          if (isSelected) accentColor
          else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "mode_pill_bg",
      )
      val textColor =
        if (isSelected) {
          if (isDarkTheme) Emerald950 else Color.White
        } else {
          MaterialTheme.colorScheme.onSurfaceVariant
        }

      Box(
        modifier =
          Modifier
            .weight(1f)
            .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
            .clip(CircleShape)
            .background(
              if (isSelected) {
                Brush.verticalGradient(
                  colors = listOf(pillColor, pillColor.copy(alpha = 0.85f))
                )
              } else {
                Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Transparent))
              }
            )
            .clickable(role = Role.Tab) { onSelectMode(mode) }
            .testTag("auth_mode_${mode.name.lowercase()}"),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = label,
          style = MaterialTheme.typography.labelLarge.copy(
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
          ),
          color = textColor,
        )
      }
    }
  }
}
