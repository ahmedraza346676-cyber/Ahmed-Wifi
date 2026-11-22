package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.BillingFormatters
import com.example.data.model.Payment
import com.example.data.model.Subscriber
import com.example.ui.DashboardSummary
import com.example.ui.components.SubscriberCard
import com.example.ui.components.dialSubscriberPhone
import com.example.ui.components.shareBillReminder
import com.example.ui.theme.CoralPending
import com.example.ui.theme.EmeraldPaid
import com.example.ui.theme.JetBrainsMono

@Composable
fun BillingTab(
    summary: DashboardSummary,
    subscribers: List<Subscriber>,
    payments: List<Payment>,
    isBusy: Boolean,
    onSubscriberClick: (Subscriber) -> Unit,
    onCollectClick: (Subscriber) -> Unit,
    onGenerateMonthlyBills: () -> Unit,
) {
    val context = LocalContext.current
    var selectedSection by remember { mutableIntStateOf(0) } // 0 = Pending Bills, 1 = Paid History
    var showConfirmBilling by remember { mutableStateOf(false) }

    val pendingSubscribers = remember(subscribers) {
        subscribers.filter { it.pendingBill > 0L }.sortedByDescending { it.pendingBill }
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Monthly Cycle Generator Card
        item {
            val monthLabel = BillingFormatters.formatMonthLabel(summary.currentMonthKey)
            Card(
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$monthLabel Billing Cycle",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = if (summary.unbilledThisMonthCount > 0)
                                "${summary.unbilledThisMonthCount} active user(s) pending monthly charge"
                            else
                                "All active users billed for $monthLabel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = { showConfirmBilling = true },
                        enabled = !isBusy && summary.unbilledThisMonthCount > 0,
                        modifier = Modifier.testTag("billing_generate_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesomeMotion,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Run Cycle")
                    }
                }
            }
        }

        // Totals Summary Strip
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    color = CoralPending.copy(alpha = 0.10f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Total Pending",
                            style = MaterialTheme.typography.labelMedium,
                            color = CoralPending
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = BillingFormatters.formatPkr(summary.totalPending),
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMono),
                            color = CoralPending,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Surface(
                    color = EmeraldPaid.copy(alpha = 0.10f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Total Paid",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldPaid
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = BillingFormatters.formatPkr(summary.totalPaid),
                            style = MaterialTheme.typography.titleLarge.copy(fontFamily = JetBrainsMono),
                            color = EmeraldPaid,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Segmented Toggle: Pending Bills vs Paid Bills History
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedSection == 0,
                    onClick = { selectedSection = 0 },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("tab_pending_bills")
                ) {
                    Text("Pending (${pendingSubscribers.size})")
                }
                SegmentedButton(
                    selected = selectedSection == 1,
                    onClick = { selectedSection = 1 },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("tab_paid_history")
                ) {
                    Text("Paid History (${payments.size})")
                }
            }
        }

        if (selectedSection == 0) {
            if (pendingSubscribers.isEmpty()) {
                item {
                    EmptyBillingNotice(
                        icon = Icons.Filled.CheckCircle,
                        title = "No Pending Bills",
                        subtitle = "Every subscriber has cleared their dues."
                    )
                }
            } else {
                items(pendingSubscribers, key = { it.id }) { sub ->
                    SubscriberCard(
                        subscriber = sub,
                        onClick = { onSubscriberClick(sub) },
                        onCollectClick = { onCollectClick(sub) },
                        onCallClick = { dialSubscriberPhone(context, sub.phoneNumber) },
                        onShareClick = { shareBillReminder(context, sub) }
                    )
                }
            }
        } else {
            if (payments.isEmpty()) {
                item {
                    EmptyBillingNotice(
                        icon = Icons.Filled.History,
                        title = "No Paid Receipts Yet",
                        subtitle = "Collected bill payments will appear here with method and month details."
                    )
                }
            } else {
                items(payments, key = { it.id }) { payment ->
                    PaymentReceiptCard(payment = payment)
                }
            }
        }
    }

    if (showConfirmBilling) {
        val monthLabel = BillingFormatters.formatMonthLabel(summary.currentMonthKey)
        AlertDialog(
            onDismissRequest = { showConfirmBilling = false },
            title = { Text("Run $monthLabel Monthly Billing?") },
            text = {
                Text("Add monthly fee to pending balance for ${summary.unbilledThisMonthCount} active subscriber(s)?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmBilling = false
                        onGenerateMonthlyBills()
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmBilling = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PaymentReceiptCard(payment: Payment) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Surface(
                color = EmeraldPaid.copy(alpha = 0.14f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Receipt,
                    contentDescription = null,
                    tint = EmeraldPaid,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = payment.subscriberName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${payment.subscriberCode} • ${BillingFormatters.formatMonthLabel(payment.billingMonth)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (payment.note.isNotBlank()) {
                    Text(
                        text = payment.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = BillingFormatters.formatTimestamp(payment.paidAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "+${BillingFormatters.formatPkr(payment.amount)}",
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
                    color = EmeraldPaid,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = payment.parsedMethod.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyBillingNotice(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EmeraldPaid,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
