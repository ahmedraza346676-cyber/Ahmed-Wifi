package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesomeMotion
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.BillingFormatters
import com.example.data.model.Subscriber
import com.example.ui.DashboardSummary
import com.example.ui.components.SubscriberCard
import com.example.ui.components.dialSubscriberPhone
import com.example.ui.components.shareBillReminder
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralPending
import com.example.ui.theme.CyanPulse
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.EmeraldPaid
import com.example.ui.theme.JetBrainsMono

@Composable
fun DashboardTab(
    operatorName: String,
    summary: DashboardSummary,
    subscribers: List<Subscriber>,
    isBusy: Boolean,
    onAddSubscriber: () -> Unit,
    onOpenSubscribers: () -> Unit,
    onOpenPendingFilter: () -> Unit,
    onSelectArea: (String) -> Unit,
    onSubscriberClick: (Subscriber) -> Unit,
    onCollectClick: (Subscriber) -> Unit,
    onGenerateMonthlyBills: () -> Unit,
) {
    val context = LocalContext.current
    var showBillingDialog by remember { mutableStateOf(false) }

    val overdueList = remember(subscribers) {
        subscribers.filter { it.pendingBill > 0L }
            .sortedByDescending { it.pendingBill }
            .take(5)
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // 1. Secure Operator Command Banner
        item {
            Card(
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(168.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_isp_hero_1795368176118),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        DeepNavy.copy(alpha = 0.94f),
                                        DeepNavy.copy(alpha = 0.72f)
                                    )
                                )
                            )
                    )
                    Column(
                        verticalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp)
                    ) {
                        Column {
                            Text(
                                text = "SECURE ISP LEDGER • ${BillingFormatters.formatMonthLabel(summary.currentMonthKey).uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanPulse
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Welcome, $operatorName",
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${summary.totalSubscribers} subscribers • ${BillingFormatters.formatPkr(summary.monthlyRecurringRevenue)}/mo potential",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.82f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onAddSubscriber,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanPulse,
                                    contentColor = DeepNavy
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("dashboard_add_user_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("New User", fontWeight = FontWeight.Bold)
                            }

                            FilledTonalButton(
                                onClick = { showBillingDialog = true },
                                enabled = !isBusy && summary.unbilledThisMonthCount > 0,
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.16f),
                                    contentColor = Color.White,
                                    disabledContainerColor = Color.White.copy(alpha = 0.08f),
                                    disabledContentColor = Color.White.copy(alpha = 0.5f)
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("run_monthly_billing_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesomeMotion,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (summary.unbilledThisMonthCount > 0)
                                        "Bill Month (${summary.unbilledThisMonthCount})"
                                    else
                                        "Month Billed"
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Billing Collection Health Ring + Financial Metrics
        item {
            BillingHealthCard(
                summary = summary,
                onPendingClick = onOpenPendingFilter
            )
        }

        // 3. 2x2 Operational KPI Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    KpiMiniCard(
                        title = "Total Users",
                        value = summary.totalSubscribers.toString(),
                        subtitle = "${summary.activeSubscribers} active",
                        icon = Icons.Filled.Group,
                        accent = MaterialTheme.colorScheme.primary,
                        onClick = onOpenSubscribers,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMiniCard(
                        title = "Pending Accounts",
                        value = summary.pendingAccountsCount.toString(),
                        subtitle = BillingFormatters.formatPkr(summary.totalPending),
                        icon = Icons.Filled.WarningAmber,
                        accent = CoralPending,
                        onClick = onOpenPendingFilter,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    KpiMiniCard(
                        title = "New Users",
                        value = summary.newSubscribers.toString(),
                        subtitle = "Awaiting activation",
                        icon = Icons.Filled.PersonAdd,
                        accent = AmberWarning,
                        onClick = onOpenSubscribers,
                        modifier = Modifier.weight(1f)
                    )
                    KpiMiniCard(
                        title = "Suspended",
                        value = summary.suspendedSubscribers.toString(),
                        subtitle = "Paused lines",
                        icon = Icons.Filled.PauseCircle,
                        accent = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = onOpenSubscribers,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. Area / Zone Breakdown
        if (summary.areaBreakdown.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Zones & Areas",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${summary.areaBreakdown.size} locations",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(summary.areaBreakdown, key = { it.areaName }) { area ->
                            Card(
                                onClick = { onSelectArea(area.areaName) },
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.width(172.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.LocationCity,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = area.areaName,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "${area.count} subscriber(s)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (area.pendingTotal > 0L)
                                            "Due: ${BillingFormatters.formatPkr(area.pendingTotal)}"
                                        else
                                            "All Clear",
                                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMono),
                                        color = if (area.pendingTotal > 0L) CoralPending else EmeraldPaid,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Overdue Spotlight or Onboarding Empty State
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = CoralPending,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Pending Bills Spotlight",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                if (overdueList.isNotEmpty()) {
                    TextButton(onClick = onOpenPendingFilter) {
                        Text("See all")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (subscribers.isEmpty()) {
            item {
                Card(
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                            modifier = Modifier.size(58.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.PersonAdd,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Internet Subscribers Yet",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Add your first internet subscriber with User ID, Phone, CNIC, Location, Monthly Bill, and Pending/Paid balance.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onAddSubscriber,
                            modifier = Modifier.testTag("empty_add_subscriber_btn")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add First Subscriber")
                        }
                    }
                }
            }
        } else if (overdueList.isEmpty()) {
            item {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(
                        containerColor = EmeraldPaid.copy(alpha = 0.10f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalanceWallet,
                            contentDescription = null,
                            tint = EmeraldPaid,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "All Accounts Paid Up!",
                                style = MaterialTheme.typography.titleSmall,
                                color = EmeraldPaid,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Zero pending dues across ${summary.totalSubscribers} subscribers.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        } else {
            items(overdueList, key = { it.id }) { sub ->
                SubscriberCard(
                    subscriber = sub,
                    onClick = { onSubscriberClick(sub) },
                    onCollectClick = { onCollectClick(sub) },
                    onCallClick = { dialSubscriberPhone(context, sub.phoneNumber) },
                    onShareClick = { shareBillReminder(context, sub) }
                )
            }
        }
    }

    if (showBillingDialog) {
        val monthLabel = BillingFormatters.formatMonthLabel(summary.currentMonthKey)
        AlertDialog(
            onDismissRequest = { showBillingDialog = false },
            title = { Text("Generate $monthLabel Bills?") },
            text = {
                Text(
                    "This will add the monthly fee to pending dues for ${summary.unbilledThisMonthCount} active subscriber(s) who have not yet been billed for $monthLabel."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showBillingDialog = false
                        onGenerateMonthlyBills()
                    }
                ) {
                    Text("Generate Bills")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBillingDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun BillingHealthCard(
    summary: DashboardSummary,
    onPendingClick: () -> Unit,
) {
    val totalVolume = (summary.totalPaid + summary.totalPending).coerceAtLeast(1L)
    val paidRatio = (summary.totalPaid.toFloat() / totalVolume.toFloat()).coerceIn(0f, 1f)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(92.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 10.dp.toPx()
                    drawArc(
                        color = if (summary.totalPending > 0L) CoralPending.copy(alpha = 0.25f) else trackColor,
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = EmeraldPaid,
                        startAngle = -90f,
                        sweepAngle = 360f * paidRatio,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${(paidRatio * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMono),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Collected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(18.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onPendingClick() }
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = "Pending Bills",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = BillingFormatters.formatPkr(summary.totalPending),
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
                        color = if (summary.totalPending > 0L) CoralPending else EmeraldPaid,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Monthly Target",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = BillingFormatters.formatPkr(summary.monthlyRecurringRevenue),
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Total Paid",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = BillingFormatters.formatPkr(summary.totalPaid),
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMono),
                        color = EmeraldPaid,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun KpiMiniCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.14f))
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = JetBrainsMono),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
