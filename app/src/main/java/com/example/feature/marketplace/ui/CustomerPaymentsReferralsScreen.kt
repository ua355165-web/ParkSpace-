package com.example.feature.marketplace.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.core.config.AppEnvironment
import com.example.core.designsystem.components.ParkSpaceModeBanner
import com.example.core.designsystem.components.ParkSpacePrimaryButton
import com.example.core.designsystem.components.ParkSpaceStatusPill
import com.example.core.designsystem.components.StatusTone
import com.example.core.domain.model.ReferralRewardStatus
import com.example.feature.booking.ui.formatDateTimePkt
import com.example.feature.booking.ui.formatPkr
import com.example.feature.marketplace.CustomerPaymentsReferralsUiState
import com.example.ui.theme.ParkSpaceMonoCodeStyle
import com.example.ui.theme.ParkSpaceMonoPriceStyle
import com.example.ui.theme.ParkSpaceSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPaymentsReferralsScreen(
  uiState: CustomerPaymentsReferralsUiState,
  feedback: String?,
  onBack: () -> Unit,
  onRedeemReferralCode: (String) -> Unit,
  onDismissFeedback: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var referralInput by rememberSaveable { mutableStateOf("") }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      TopAppBar(
        title = { Text("Demo Payments & Referrals", style = MaterialTheme.typography.titleLarge) },
        navigationIcon = {
          IconButton(
            onClick = onBack,
            modifier =
              Modifier.size(ParkSpaceSpacing.minTouchTarget).testTag("payments_referrals_back_button"),
          ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        modifier = Modifier.widthIn(max = ParkSpaceSpacing.maxReadableWidth).fillMaxSize(),
        contentPadding = PaddingValues(ParkSpaceSpacing.screenHorizontal, ParkSpaceSpacing.md),
        verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.md),
      ) {
        item { ParkSpaceModeBanner(mode = AppEnvironment.currentMode) }

        if (feedback != null) {
          item {
            Surface(
              modifier = Modifier.fillMaxWidth().testTag("payments_referrals_feedback_banner"),
              shape = MaterialTheme.shapes.small,
              color = MaterialTheme.colorScheme.primaryContainer,
            ) {
              Row(
                modifier = Modifier.padding(ParkSpaceSpacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text(
                  text = feedback,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismissFeedback) { Text("OK") }
              }
            }
          }
        }

        // Demo Credit Balance & My Referral Code Card
        item {
          Surface(
            modifier = Modifier.fillMaxWidth().testTag("demo_credits_referral_card"),
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
                    Icons.Outlined.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                  )
                  Text("ParkSpace Demo Credits", style = MaterialTheme.typography.titleMedium)
                }
                ParkSpaceStatusPill(label = "DEMO ONLY", tone = StatusTone.INFO)
              }

              Text(
                text = formatPkr(uiState.availableDemoCreditPkr),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("demo_credit_balance_text"),
              )
              Text(
                text =
                  "Earned strictly through verified local Demo referral completions. Zero real monetary value.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )

              Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(ParkSpaceSpacing.sm),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Column {
                    Text("Your Referral Code", style = MaterialTheme.typography.labelSmall)
                    Text(
                      text = uiState.myReferralCode.ifBlank { "—" },
                      style = ParkSpaceMonoCodeStyle,
                      color = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.testTag("my_referral_code_text"),
                    )
                  }
                  Icon(Icons.Outlined.CardGiftcard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
              }

              if (uiState.appliedReferral == null) {
                OutlinedTextField(
                  value = referralInput,
                  onValueChange = { referralInput = it },
                  label = { Text("Enter a friend's referral code") },
                  singleLine = true,
                  modifier = Modifier.fillMaxWidth().testTag("redeem_referral_code_input"),
                )
                ParkSpacePrimaryButton(
                  text = "Apply Referral Code (Demo)",
                  onClick = {
                    onRedeemReferralCode(referralInput)
                    referralInput = ""
                  },
                  modifier = Modifier.fillMaxWidth().testTag("redeem_referral_code_button"),
                )
              } else {
                val applied = uiState.appliedReferral
                Surface(
                  shape = MaterialTheme.shapes.small,
                  color = MaterialTheme.colorScheme.surfaceVariant,
                  modifier = Modifier.fillMaxWidth().testTag("applied_referral_status_card"),
                ) {
                  Column(modifier = Modifier.padding(ParkSpaceSpacing.sm)) {
                    Text(
                      text = "Linked Referral: ${applied.referralCodeUsed}",
                      style = MaterialTheme.typography.labelLarge,
                    )
                    Text(
                      text = "${applied.status.displayName} • Rs. ${applied.rewardAmountPkr} Demo Credit",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
              }

              if (uiState.referralsSent.isNotEmpty()) {
                Text("Friends Invited (${uiState.referralsSent.size})", style = MaterialTheme.typography.titleSmall)
                uiState.referralsSent.forEach { rec ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                  ) {
                    Text(
                      text = "Invited User (${rec.referralCodeUsed})",
                      style = MaterialTheme.typography.bodySmall,
                    )
                    ParkSpaceStatusPill(
                      label =
                        if (rec.status == ReferralRewardStatus.CREDITED_DEMO) "+Rs. ${rec.rewardAmountPkr} Credited"
                        else "Pending Booking",
                      tone =
                        if (rec.status == ReferralRewardStatus.CREDITED_DEMO) StatusTone.AVAILABLE
                        else StatusTone.PENDING,
                    )
                  }
                }
              }
            }
          }
        }

        // Demo Payment Transactions History
        item {
          Text(
            text = "Demo Payment Transactions (${uiState.transactions.size})",
            style = MaterialTheme.typography.titleMedium,
          )
        }

        if (uiState.transactions.isEmpty()) {
          item {
            Surface(
              modifier = Modifier.fillMaxWidth().testTag("payments_empty_state"),
              shape = MaterialTheme.shapes.medium,
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
              Column(
                modifier = Modifier.padding(ParkSpaceSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
              ) {
                Icon(
                  Icons.Outlined.ReceiptLong,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(32.dp),
                )
                Text(stringResource(R.string.empty_payments), style = MaterialTheme.typography.titleMedium)
                Text(
                  text = stringResource(R.string.demo_payment_disclaimer),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        } else {
          items(uiState.transactions, key = { it.id }) { tx ->
            Surface(
              modifier = Modifier.fillMaxWidth().testTag("customer_tx_card_${tx.id}"),
              shape = MaterialTheme.shapes.small,
              color = MaterialTheme.colorScheme.surface,
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
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
                  ParkSpaceStatusPill(label = tx.transactionType.displayName, tone = StatusTone.INFO)
                  Text(
                    text = formatPkr(tx.amountPkr),
                    style = ParkSpaceMonoPriceStyle,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
                Text(
                  text = "${tx.referenceCode} • ${tx.method.displayName}",
                  style = ParkSpaceMonoCodeStyle,
                )
                Text(
                  text = tx.summary,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                  text = formatDateTimePkt(tx.createdAtEpochMs),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }
        }
      }
    }
  }
}
