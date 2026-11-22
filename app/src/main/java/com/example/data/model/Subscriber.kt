package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SubscriberStatus(val label: String) {
    ACTIVE("Active"),
    NEW("New User"),
    SUSPENDED("Suspended");

    companion object {
        fun fromRaw(raw: String?): SubscriberStatus =
            entries.firstOrNull { it.name == raw } ?: ACTIVE
    }
}

enum class PaymentMethod(val label: String) {
    CASH("Cash"),
    JAZZCASH("JazzCash"),
    EASYPAISA("Easypaisa"),
    BANK("Bank Transfer");

    companion object {
        fun fromRaw(raw: String?): PaymentMethod =
            entries.firstOrNull { it.name == raw } ?: CASH
    }
}

@Keep
@IgnoreExtraProperties
data class Subscriber(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subscriberCode: String = "",
    val fullName: String = "",
    val phoneNumber: String = "",
    val cnic: String = "",
    val location: String = "",
    val areaZone: String = "",
    val packageName: String = "",
    val status: String = SubscriberStatus.ACTIVE.name,
    val monthlyBill: Long = 0L,
    val pendingBill: Long = 0L,
    val totalPaid: Long = 0L,
    val lastBilledMonth: String = "",
    val notes: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val parsedStatus: SubscriberStatus
        get() = SubscriberStatus.fromRaw(status)

    val hasPendingDues: Boolean
        get() = pendingBill > 0L

    fun toCreateMap(ownerUid: String): Map<String, Any> = mapOf(
        "userId" to ownerUid,
        "subscriberCode" to subscriberCode.trim(),
        "fullName" to fullName.trim(),
        "phoneNumber" to phoneNumber.trim(),
        "cnic" to cnic.trim(),
        "location" to location.trim(),
        "areaZone" to areaZone.trim(),
        "packageName" to packageName.trim(),
        "status" to SubscriberStatus.fromRaw(status).name,
        "monthlyBill" to monthlyBill.coerceIn(0L, 10_000_000L),
        "pendingBill" to pendingBill.coerceIn(0L, 100_000_000L),
        "totalPaid" to totalPaid.coerceIn(0L, 1_000_000_000L),
        "lastBilledMonth" to lastBilledMonth.trim(),
        "notes" to notes.trim(),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )

    fun toProfileUpdateMap(): Map<String, Any> = mapOf(
        "subscriberCode" to subscriberCode.trim(),
        "fullName" to fullName.trim(),
        "phoneNumber" to phoneNumber.trim(),
        "cnic" to cnic.trim(),
        "location" to location.trim(),
        "areaZone" to areaZone.trim(),
        "packageName" to packageName.trim(),
        "status" to SubscriberStatus.fromRaw(status).name,
        "monthlyBill" to monthlyBill.coerceIn(0L, 10_000_000L),
        "pendingBill" to pendingBill.coerceIn(0L, 100_000_000L),
        "totalPaid" to totalPaid.coerceIn(0L, 1_000_000_000L),
        "lastBilledMonth" to lastBilledMonth.trim(),
        "notes" to notes.trim(),
        "updatedAt" to FieldValue.serverTimestamp(),
    )
}

@Keep
@IgnoreExtraProperties
data class Payment(
    @DocumentId val id: String = "",
    val userId: String = "",
    val subscriberId: String = "",
    val subscriberName: String = "",
    val subscriberCode: String = "",
    val amount: Long = 0L,
    val billingMonth: String = "",
    val paymentMethod: String = PaymentMethod.CASH.name,
    val note: String = "",
    @ServerTimestamp val paidAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
) {
    val parsedMethod: PaymentMethod
        get() = PaymentMethod.fromRaw(paymentMethod)
}

object BillingFormatters {
    private val numberFormat = NumberFormat.getNumberInstance(Locale.US)

    fun formatPkr(amount: Long): String = "Rs. ${numberFormat.format(amount)}"

    fun currentYearMonth(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        return sdf.format(Date())
    }

    fun formatMonthLabel(yearMonth: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM", Locale.US)
            val formatter = SimpleDateFormat("MMM yyyy", Locale.US)
            val parsed = parser.parse(yearMonth)
            if (parsed != null) formatter.format(parsed) else yearMonth
        } catch (_: Exception) {
            yearMonth
        }
    }

    fun formatTimestamp(ts: Timestamp?): String {
        val date = ts?.toDate() ?: return "Just now"
        return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(date)
    }

    fun formatShortDate(ts: Timestamp?): String {
        val date = ts?.toDate() ?: return "Today"
        return SimpleDateFormat("dd MMM yyyy", Locale.US).format(date)
    }

    /** Formats digits into 12345-1234567-1 pattern while typing CNIC */
    fun formatCnicInput(raw: String): String {
        val digits = raw.filter { it.isDigit() }.take(13)
        return buildString {
            digits.forEachIndexed { index, ch ->
                if (index == 5 || index == 12) append('-')
                append(ch)
            }
        }
    }
}
