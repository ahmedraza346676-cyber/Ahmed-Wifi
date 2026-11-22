package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BillingFormatters
import com.example.data.model.PaymentMethod
import com.example.data.model.Subscriber
import com.example.ui.theme.CoralPending
import com.example.ui.theme.JetBrainsMono

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecordPaymentSheet(
    subscriber: Subscriber,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onConfirmPayment: (amount: Long, billingMonth: String, method: PaymentMethod, note: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val defaultAmount = if (subscriber.pendingBill > 0L) subscriber.pendingBill else subscriber.monthlyBill
    var amountText by remember(subscriber.id) {
        mutableStateOf(if (defaultAmount > 0L) defaultAmount.toString() else "")
    }
    var billingMonth by remember(subscriber.id) {
        mutableStateOf(BillingFormatters.currentYearMonth())
    }
    var selectedMethod by remember(subscriber.id) { mutableStateOf(PaymentMethod.CASH) }
    var note by remember(subscriber.id) { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Record Bill Payment",
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${subscriber.fullName} (${subscriber.subscriberCode})",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "Current Pending",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = BillingFormatters.formatPkr(subscriber.pendingBill),
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                            color = if (subscriber.pendingBill > 0L) CoralPending else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Monthly Plan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = BillingFormatters.formatPkr(subscriber.monthlyBill),
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick amount preset chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (subscriber.pendingBill > 0L) {
                    FilterChip(
                        selected = amountText == subscriber.pendingBill.toString(),
                        onClick = {
                            amountText = subscriber.pendingBill.toString()
                            validationError = null
                        },
                        label = { Text("Full Pending (${BillingFormatters.formatPkr(subscriber.pendingBill)})") }
                    )
                }
                if (subscriber.monthlyBill > 0L && subscriber.monthlyBill != subscriber.pendingBill) {
                    FilterChip(
                        selected = amountText == subscriber.monthlyBill.toString(),
                        onClick = {
                            amountText = subscriber.monthlyBill.toString()
                            validationError = null
                        },
                        label = { Text("Monthly (${BillingFormatters.formatPkr(subscriber.monthlyBill)})") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { input ->
                        amountText = input.filter { it.isDigit() }.take(9)
                        validationError = null
                    },
                    label = { Text("Amount (Rs.)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("payment_amount_input")
                )

                OutlinedTextField(
                    value = billingMonth,
                    onValueChange = { billingMonth = it.take(20) },
                    label = { Text("Billing Month") },
                    singleLine = true,
                    placeholder = { Text("2026-11") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("payment_month_input")
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Payment Method",
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.height(6.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PaymentMethod.entries.forEach { method ->
                    FilterChip(
                        selected = selectedMethod == method,
                        onClick = { selectedMethod = method },
                        label = { Text(method.label) },
                        modifier = Modifier.testTag("payment_method_${method.name}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it.take(200) },
                label = { Text("Reference / Note (Optional)") },
                placeholder = { Text("e.g., TID, collector name, partial note") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payment_note_input")
            )

            if (validationError != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = validationError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isBusy,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val parsedAmount = amountText.toLongOrNull() ?: 0L
                        when {
                            parsedAmount <= 0L -> validationError = "Enter an amount greater than 0"
                            billingMonth.isBlank() -> validationError = "Enter the billing month (e.g. 2026-11)"
                            else -> onConfirmPayment(parsedAmount, billingMonth.trim(), selectedMethod, note.trim())
                        }
                    },
                    enabled = !isBusy,
                    modifier = Modifier
                        .weight(1.4f)
                        .testTag("confirm_payment_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Payments,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Confirm Paid", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
