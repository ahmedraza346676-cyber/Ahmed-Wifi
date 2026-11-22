package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BillingFormatters
import com.example.data.model.Subscriber
import com.example.data.model.SubscriberStatus

private val PACKAGE_PRESETS = listOf(
    "15 Mbps Fiber",
    "25 Mbps Fiber",
    "30 Mbps Fiber",
    "50 Mbps Fiber",
    "100 Mbps Fiber",
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditSubscriberScreen(
    existingSubscriber: Subscriber?,
    suggestedNextCode: String,
    existingAreas: List<String>,
    isBusy: Boolean,
    onBack: () -> Unit,
    onSave: (Subscriber) -> Unit,
) {
    BackHandler { onBack() }

    val isEdit = existingSubscriber != null

    var subscriberCode by remember(existingSubscriber) {
        mutableStateOf(existingSubscriber?.subscriberCode ?: suggestedNextCode)
    }
    var fullName by remember(existingSubscriber) {
        mutableStateOf(existingSubscriber?.fullName ?: "")
    }
    var phoneNumber by remember(existingSubscriber) {
        mutableStateOf(existingSubscriber?.phoneNumber ?: "")
    }
    var cnic by remember(existingSubscriber) {
        mutableStateOf(existingSubscriber?.cnic ?: "")
    }
    var areaZone by remember(existingSubscriber) {
        mutableStateOf(existingSubscriber?.areaZone ?: "")
    }
    var location by remember(existingSubscriber) {
        mutableStateOf(existingSubscriber?.location ?: "")
    }
    var packageName by remember(existingSubscriber) {
        mutableStateOf(existingSubscriber?.packageName ?: "30 Mbps Fiber")
    }
    var status by remember(existingSubscriber) {
        mutableStateOf(existingSubscriber?.parsedStatus ?: SubscriberStatus.NEW)
    }
    var monthlyBillText by remember(existingSubscriber) {
        mutableStateOf(existingSubscriber?.monthlyBill?.toString() ?: "2500")
    }
    var pendingBillText by remember(existingSubscriber) {
        mutableStateOf(existingSubscriber?.pendingBill?.toString() ?: "2500")
    }
    var pendingEditedManually by remember(existingSubscriber) {
        mutableStateOf(isEdit)
    }
    var totalPaidText by remember(existingSubscriber) {
        mutableStateOf(existingSubscriber?.totalPaid?.toString() ?: "0")
    }
    var notes by remember(existingSubscriber) {
        mutableStateOf(existingSubscriber?.notes ?: "")
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEdit) "Edit Subscriber" else "Add New Internet User") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("form_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Section 1: Identity & Contact
            FormSectionCard(title = "Account & Identity") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = subscriberCode,
                        onValueChange = { subscriberCode = it.take(40) },
                        label = { Text("User ID *") },
                        placeholder = { Text("NET-1001") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(0.9f)
                            .testTag("input_user_id")
                    )
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it.take(80) },
                        label = { Text("Full Name *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier
                            .weight(1.4f)
                            .testTag("input_full_name")
                    )
                }

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it.take(25) },
                    label = { Text("Mobile Number *") },
                    placeholder = { Text("0300-1234567") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_phone")
                )

                OutlinedTextField(
                    value = cnic,
                    onValueChange = { raw ->
                        cnic = BillingFormatters.formatCnicInput(raw)
                    },
                    label = { Text("CNIC Number *") },
                    placeholder = { Text("35202-1234567-1") },
                    supportingText = { Text("13 digits auto-formatted as #####-#######-#") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_cnic")
                )
            }

            // Section 2: Location & Internet Package
            FormSectionCard(title = "Location & Connection") {
                OutlinedTextField(
                    value = areaZone,
                    onValueChange = { areaZone = it.take(60) },
                    label = { Text("Area / Zone") },
                    placeholder = { Text("e.g., Model Town, Sector F-11") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_area")
                )

                if (existingAreas.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        existingAreas.take(6).forEach { zone ->
                            SuggestionChip(
                                onClick = { areaZone = zone },
                                label = { Text(zone) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it.take(160) },
                    label = { Text("Street / Home Location *") },
                    placeholder = { Text("House #, Street #, Landmark") },
                    maxLines = 2,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_location")
                )

                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it.take(60) },
                    label = { Text("Internet Package") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_package")
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    PACKAGE_PRESETS.forEach { preset ->
                        FilterChip(
                            selected = packageName == preset,
                            onClick = { packageName = preset },
                            label = { Text(preset) }
                        )
                    }
                }

                Text(
                    text = "Connection Status",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SubscriberStatus.entries.forEach { item ->
                        FilterChip(
                            selected = status == item,
                            onClick = { status = item },
                            label = { Text(item.label) },
                            modifier = Modifier.testTag("status_chip_${item.name}")
                        )
                    }
                }
            }

            // Section 3: Billing
            FormSectionCard(title = "Billing & Balances (PKR)") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = monthlyBillText,
                        onValueChange = { input ->
                            val clean = input.filter { it.isDigit() }.take(8)
                            monthlyBillText = clean
                            if (!pendingEditedManually) {
                                pendingBillText = clean
                            }
                        },
                        label = { Text("Monthly Bill *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_monthly_bill")
                    )

                    OutlinedTextField(
                        value = pendingBillText,
                        onValueChange = { input ->
                            pendingEditedManually = true
                            pendingBillText = input.filter { it.isDigit() }.take(9)
                        },
                        label = { Text("Pending Bill") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_pending_bill")
                    )
                }

                OutlinedTextField(
                    value = totalPaidText,
                    onValueChange = { totalPaidText = it.filter { ch -> ch.isDigit() }.take(9) },
                    label = { Text("Paid Bill Total") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_total_paid")
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it.take(300) },
                    label = { Text("Notes (Router / Pole / Installation)") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_notes")
                )
            }

            if (validationError != null) {
                Text(
                    text = validationError!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Button(
                onClick = {
                    val code = subscriberCode.trim()
                    val name = fullName.trim()
                    val phone = phoneNumber.trim()
                    val cleanCnic = cnic.trim()
                    val loc = location.trim()
                    val monthly = monthlyBillText.toLongOrNull() ?: -1L
                    val pending = pendingBillText.toLongOrNull() ?: 0L
                    val paid = totalPaidText.toLongOrNull() ?: 0L

                    when {
                        code.isEmpty() -> validationError = "User ID is required"
                        name.isEmpty() -> validationError = "Full Name is required"
                        phone.length < 5 -> validationError = "Enter a valid mobile number (at least 5 characters)"
                        cleanCnic.length < 5 -> validationError = "Enter a valid CNIC number"
                        loc.isEmpty() -> validationError = "Location / Address is required"
                        monthly < 0L -> validationError = "Enter a valid monthly bill amount"
                        else -> {
                            validationError = null
                            val result = (existingSubscriber ?: Subscriber()).copy(
                                subscriberCode = code,
                                fullName = name,
                                phoneNumber = phone,
                                cnic = cleanCnic,
                                areaZone = areaZone.trim(),
                                location = loc,
                                packageName = packageName.trim(),
                                status = status.name,
                                monthlyBill = monthly,
                                pendingBill = pending.coerceAtLeast(0L),
                                totalPaid = paid.coerceAtLeast(0L),
                                lastBilledMonth = existingSubscriber?.lastBilledMonth
                                    ?: if (pending > 0L) BillingFormatters.currentYearMonth() else "",
                                notes = notes.trim(),
                            )
                            onSave(result)
                        }
                    }
                },
                enabled = !isBusy,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_subscriber_button")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Filled.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEdit) "Save Changes" else "Save Subscriber",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun FormSectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}
