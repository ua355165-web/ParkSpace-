package com.example.feature.marketplace.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.core.designsystem.components.ParkSpacePrimaryButton
import com.example.core.domain.model.DemoPaymentMethod
import com.example.feature.booking.ui.formatPkr
import com.example.ui.theme.ParkSpaceMonoPriceStyle
import com.example.ui.theme.ParkSpaceSpacing

@Composable
fun DemoPaymentMethodSelector(
  selectedMethod: DemoPaymentMethod,
  availableDemoCreditPkr: Int,
  requiredTotalPkr: Int?,
  onSelectMethod: (DemoPaymentMethod) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
  ) {
    Text(
      text = "Select Simulated Demo Payment Method",
      style = MaterialTheme.typography.labelLarge,
    )
    DemoPaymentMethod.entries.forEach { method ->
      val isWallet = method == DemoPaymentMethod.DEMO_CREDIT_WALLET
      val walletEnough =
        !isWallet || (requiredTotalPkr != null && availableDemoCreditPkr >= requiredTotalPkr)
      val selected = selectedMethod == method
      Surface(
        modifier =
          Modifier.fillMaxWidth()
            .defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget)
            .clickable { onSelectMethod(method) }
            .testTag("payment_method_${method.name.lowercase()}"),
        shape = MaterialTheme.shapes.small,
        color =
          if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
          else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border =
          BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color =
              if (selected) MaterialTheme.colorScheme.primary
              else MaterialTheme.colorScheme.outlineVariant,
          ),
      ) {
        Row(
          modifier = Modifier.padding(horizontal = ParkSpaceSpacing.sm, vertical = ParkSpaceSpacing.xs),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
        ) {
          RadioButton(
            selected = selected,
            onClick = { onSelectMethod(method) },
          )
          Icon(
            imageVector =
              when (method) {
                DemoPaymentMethod.JAZZCASH_DEMO,
                DemoPaymentMethod.EASYPAISA_DEMO -> Icons.Outlined.PhoneAndroid
                DemoPaymentMethod.CARD_DEMO -> Icons.Outlined.CreditCard
                DemoPaymentMethod.BANK_IBFT_DEMO -> Icons.Outlined.AccountBalance
                DemoPaymentMethod.DEMO_CREDIT_WALLET -> Icons.Outlined.AccountBalanceWallet
              },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = method.displayName,
              style = MaterialTheme.typography.labelLarge,
            )
            Text(
              text =
                if (isWallet) {
                  "Available Demo Credit: ${formatPkr(availableDemoCreditPkr)}" +
                    if (!walletEnough && requiredTotalPkr != null) " (Insufficient for ${formatPkr(requiredTotalPkr)})"
                    else ""
                } else {
                  method.subtitle
                },
              style = MaterialTheme.typography.bodySmall,
              color =
                if (isWallet && !walletEnough) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

@Composable
fun DemoPaymentMethodDialog(
  bookingCode: String,
  amountPkr: Int,
  availableDemoCreditPkr: Int,
  onDismiss: () -> Unit,
  onConfirmPayment: (DemoPaymentMethod) -> Unit,
) {
  var selectedMethod by rememberSaveable { mutableStateOf(DemoPaymentMethod.JAZZCASH_DEMO) }
  val isWalletInsufficient =
    selectedMethod == DemoPaymentMethod.DEMO_CREDIT_WALLET && availableDemoCreditPkr < amountPkr

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Complete Demo Payment ($bookingCode)") },
    text = {
      Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.sm),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text("Simulated Demo Total", style = MaterialTheme.typography.bodyMedium)
          Text(
            text = formatPkr(amountPkr),
            style = ParkSpaceMonoPriceStyle,
            color = MaterialTheme.colorScheme.primary,
          )
        }

        DemoPaymentMethodSelector(
          selectedMethod = selectedMethod,
          availableDemoCreditPkr = availableDemoCreditPkr,
          requiredTotalPkr = amountPkr,
          onSelectMethod = { selectedMethod = it },
        )

        Surface(
          shape = MaterialTheme.shapes.small,
          color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
          Row(
            modifier = Modifier.padding(ParkSpaceSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(ParkSpaceSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Icon(
              imageVector = Icons.Outlined.Info,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
              tint = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = stringResource(R.string.demo_payment_disclaimer),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    },
    confirmButton = {
      ParkSpacePrimaryButton(
        text = "Pay ${formatPkr(amountPkr)} (Demo)",
        enabled = !isWalletInsufficient,
        leadingIcon = Icons.Outlined.CheckCircle,
        onClick = { onConfirmPayment(selectedMethod) },
        modifier = Modifier.testTag("dialog_confirm_demo_payment_button"),
      )
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.defaultMinSize(minHeight = ParkSpaceSpacing.minTouchTarget),
      ) {
        Text("Cancel")
      }
    },
  )
}
