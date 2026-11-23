package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

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

@Entity(tableName = "subscribers")
data class Subscriber(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
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
    val createdAtMillis: Long = System.currentTimeMillis(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
) {
    val parsedStatus: SubscriberStatus
        get() = SubscriberStatus.fromRaw(status)

    val hasPendingDues: Boolean
        get() = pendingBill > 0L
}

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val subscriberId: String = "",
    val subscriberName: String = "",
    val subscriberCode: String = "",
    val amount: Long = 0L,
    val billingMonth: String = "",
    val paymentMethod: String = PaymentMethod.CASH.name,
    val note: String = "",
    val paidAtMillis: Long = System.currentTimeMillis(),
    val createdAtMillis: Long = System.currentTimeMillis(),
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

    fun formatTimestamp(millis: Long): String {
        if (millis <= 0L) return "Just now"
        return SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(millis))
    }

    fun formatShortDate(millis: Long): String {
        if (millis <= 0L) return "Today"
        return SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(millis))
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
