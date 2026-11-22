package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Notes
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.BillingFormatters
import com.example.data.model.Payment
import com.example.data.model.Subscriber
import com.example.ui.components.StatusPill
import com.example.ui.components.copyToClipboard
import com.example.ui.components.dialSubscriberPhone
import com.example.ui.components.shareBillReminder
import com.example.ui.theme.CoralPending
import com.example.ui.theme.EmeraldPaid
import com.example.ui.theme.JetBrainsMono

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriberDetailScreen(
    subscriber: Subscriber,
    payments: List<Payment>,
    isBusy: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onRecordPayment: () -> Unit,
    onCopiedMessage: (String) -> Unit,
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    val subscriberPayments = remember(payments, subscriber.id) {
        payments.filter { it.subscriberId == subscriber.id }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subscriber Details") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onEdit,
                        enabled = !isBusy,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("detail_edit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit subscriber"
                        )
                    }
                    IconButton(
                        onClick = { showDeleteDialog = true },
                        enabled = !isBusy,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("detail_delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete subscriber",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Identity Header Card
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        text = "ID: ${subscriber.subscriberCode}",
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMono),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = subscriber.fullName,
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            }
                            StatusPill(status = subscriber.parsedStatus)
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(12.dp))

                        DetailRow(
                            icon = Icons.Filled.Phone,
                            label = "Mobile Number",
                            value = subscriber.phoneNumber,
                            trailingActionIcon = Icons.Filled.ContentCopy,
                            onTrailingClick = {
                                copyToClipboard(context, "Phone", subscriber.phoneNumber)
                                onCopiedMessage("Phone copied")
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        DetailRow(
                            icon = Icons.Outlined.Badge,
                            label = "CNIC Number",
                            value = subscriber.cnic,
                            isMono = true,
                            trailingActionIcon = Icons.Filled.ContentCopy,
                            onTrailingClick = {
                                copyToClipboard(context, "CNIC", subscriber.cnic)
                                onCopiedMessage("CNIC copied")
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val fullLoc = if (subscriber.areaZone.isNotBlank()) {
                            "${subscriber.areaZone} — ${subscriber.location}"
                        } else {
                            subscriber.location
                        }
                        DetailRow(
                            icon = Icons.Filled.LocationOn,
                            label = "Location & Area",
                            value = fullLoc
                        )
                        if (subscriber.packageName.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            DetailRow(
                                icon = Icons.Filled.Router,
                                label = "Internet Plan",
                                value = subscriber.packageName
                            )
                        }
                        if (subscriber.lastBilledMonth.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            DetailRow(
                                icon = Icons.Outlined.CalendarMonth,
                                label = "Last Billed Month",
                                value = BillingFormatters.formatMonthLabel(subscriber.lastBilledMonth)
                            )
                        }
                        if (subscriber.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            DetailRow(
                                icon = Icons.AutoMirrored.Outlined.Notes,
                                label = "Notes",
                                value = subscriber.notes
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilledTonalButton(
                                onClick = { dialSubscriberPhone(context, subscriber.phoneNumber) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Call")
                            }
                            FilledTonalButton(
                                onClick = { shareBillReminder(context, subscriber) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Bill Notice")
                            }
                        }
                    }
                }
            }

            // Billing Summary Card
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = "Billing Overview",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Text(
                                    text = "Monthly Bill",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = BillingFormatters.formatPkr(subscriber.monthlyBill),
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Pending Bill",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = BillingFormatters.formatPkr(subscriber.pendingBill),
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                                    color = if (subscriber.pendingBill > 0L) CoralPending else EmeraldPaid,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Total Paid",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = BillingFormatters.formatPkr(subscriber.totalPaid),
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                                    color = EmeraldPaid,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onRecordPayment,
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("detail_record_payment_button")
                        ) {
                            Icon(Icons.Filled.Payments, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (subscriber.pendingBill > 0L) "Collect / Record Payment" else "Add Advance Payment",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Paid Bills History for This Subscriber
            item {
                Text(
                    text = "Paid Bill Receipts (${subscriberPayments.size})",
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (subscriberPayments.isEmpty()) {
                item {
                    Card(
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Text(
                            text = "No payments logged yet for this subscriber.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(subscriberPayments, key = { it.id }) { payment ->
                    PaymentReceiptCard(payment = payment)
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Subscriber?") },
            text = {
                Text("Remove ${subscriber.fullName} (${subscriber.subscriberCode}) from your subscriber directory? This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    isMono: Boolean = false,
    trailingActionIcon: ImageVector? = null,
    onTrailingClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = if (isMono) {
                    MaterialTheme.typography.bodyLarge.copy(fontFamily = JetBrainsMono)
                } else {
                    MaterialTheme.typography.bodyLarge
                }
            )
        }
        if (trailingActionIcon != null && onTrailingClick != null) {
            IconButton(
                onClick = onTrailingClick,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = trailingActionIcon,
                    contentDescription = "Copy $label",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
