package com.example.feature.auth.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.core.config.AppEnvironment
import com.example.core.config.SupportedCity
import com.example.core.config.UserRole
import com.example.core.designsystem.components.ParkSpaceModeBanner
import com.example.core.designsystem.components.ParkSpacePrimaryButton
import com.example.core.designsystem.components.ParkSpaceStatusPill
import com.example.core.designsystem.components.StatusTone
import com.example.feature.auth.AuthFormMode
import com.example.feature.auth.DemoAuthUiState
import com.example.feature.auth.domain.DemoAuthValidator
import com.example.ui.theme.ParkSpaceSpacing

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
) {
  var passwordVisible by rememberSaveable { mutableStateOf(false) }
  var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

  val isCustomer = uiState.role == UserRole.CUSTOMER
  val roleTitle = if (isCustomer) "Customer Account" else "Host Account"

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        title = { Text(text = roleTitle, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier =
              Modifier.size(ParkSpaceSpacing.minTouchTarget).testTag("auth_back_button"),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = "Back",
            )
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

        // Transparency & Role Header Card
        Surface(
          modifier = Modifier.fillMaxWidth().testTag("demo_auth_transparency_card"),
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(
            modifier = Modifier.padding(ParkSpaceSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
            ) {
              Icon(
                imageVector =
                  if (isCustomer) Icons.Outlined.DirectionsCar else Icons.Outlined.Storefront,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
              )
              ParkSpaceStatusPill(
                label = DemoAuthValidator.DEMO_TRANSPARENCY_NOTICE,
                tone = StatusTone.INFO,
              )
            }
            Text(
              text =
                if (uiState.mode == AuthFormMode.SIGN_IN) {
                  "Sign in to your local ${uiState.role.displayName} account"
                } else {
                  "Create your own local ${uiState.role.displayName} account"
                },
              style = MaterialTheme.typography.titleMedium,
            )
            Text(
              text =
                "Accounts are stored strictly on this device in the Demo database with salted password hashing. Any valid email format is accepted without OTP or email verification.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        // Mode Switcher: Sign In vs Create Account
        SingleChoiceSegmentedButtonRow(
          modifier = Modifier.fillMaxWidth().testTag("auth_mode_selector"),
        ) {
          val modes = listOf(AuthFormMode.SIGN_IN to "Sign In", AuthFormMode.REGISTER to "Create Account")
          modes.forEachIndexed { index, (mode, label) ->
            SegmentedButton(
              selected = uiState.mode == mode,
              onClick = { onSelectMode(mode) },
              shape = SegmentedButtonDefaults.itemShape(index, modes.size),
              modifier =
                Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
                  .testTag("auth_mode_${mode.name.lowercase()}"),
            ) {
              Text(label)
            }
          }
        }

        // Credentials Form Card
        Surface(
          modifier = Modifier.fillMaxWidth().testTag("auth_form_card"),
          shape = MaterialTheme.shapes.medium,
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(
            modifier = Modifier.padding(ParkSpaceSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
          ) {
            if (uiState.mode == AuthFormMode.REGISTER) {
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
                    Modifier.size(ParkSpaceSpacing.minTouchTarget)
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
                      Modifier.size(ParkSpaceSpacing.minTouchTarget)
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
                  modifier = Modifier.fillMaxWidth().testTag("auth_business_alias_input"),
                )
              }

              Text("Preferred City", style = MaterialTheme.typography.labelLarge)
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
              ) {
                SupportedCity.entries.forEach { city ->
                  FilterChip(
                    selected = uiState.selectedCity == city,
                    onClick = { onSelectCity(city) },
                    label = { Text(city.displayName) },
                    modifier = Modifier.testTag("auth_city_${city.name.lowercase()}"),
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
                        "New Host accounts start in PENDING_REVIEW status. You can access your Host dashboard & onboarding immediately; listing publication remains restricted until Admin approval.",
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
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.errorContainer,
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

            Spacer(modifier = Modifier.height(ParkSpaceSpacing.xxs))

            ParkSpacePrimaryButton(
              text =
                if (uiState.isSubmitting) {
                  "Please wait..."
                } else if (uiState.mode == AuthFormMode.SIGN_IN) {
                  "Sign In as ${uiState.role.displayName}"
                } else {
                  "Create ${uiState.role.displayName} Demo Account"
                },
              onClick = onSubmit,
              enabled = !uiState.isSubmitting,
              modifier = Modifier.fillMaxWidth().testTag("auth_submit_button"),
            )
          }
        }
      }
    }
  }
}
