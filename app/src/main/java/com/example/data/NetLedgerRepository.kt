package com.example.data

import android.content.Context
import com.example.data.local.NetLedgerDao
import com.example.data.local.NetLedgerDatabase
import com.example.data.model.Payment
import com.example.data.model.PaymentMethod
import com.example.data.model.Subscriber
import com.example.data.model.SubscriberStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class NetLedgerRepository(
    private val dao: NetLedgerDao,
) {
    constructor(context: Context) : this(
        NetLedgerDatabase.getInstance(context).netLedgerDao()
    )

    fun observeSubscribers(): Flow<List<Subscriber>> = dao.observeSubscribers()

    fun observePayments(): Flow<List<Payment>> = dao.observePayments()

    suspend fun addSubscriber(subscriber: Subscriber): Result<String> {
        return try {
            validateSubscriberInput(subscriber)
            val now = System.currentTimeMillis()
            val newId = subscriber.id.ifBlank { UUID.randomUUID().toString() }
            val cleanSubscriber = subscriber.copy(
                id = newId,
                subscriberCode = subscriber.subscriberCode.trim(),
                fullName = subscriber.fullName.trim(),
                phoneNumber = subscriber.phoneNumber.trim(),
                cnic = subscriber.cnic.trim(),
                location = subscriber.location.trim(),
                areaZone = subscriber.areaZone.trim(),
                packageName = subscriber.packageName.trim(),
                status = SubscriberStatus.fromRaw(subscriber.status).name,
                monthlyBill = subscriber.monthlyBill.coerceIn(0L, 10_000_000L),
                pendingBill = subscriber.pendingBill.coerceIn(0L, 100_000_000L),
                totalPaid = subscriber.totalPaid.coerceIn(0L, 1_000_000_000L),
                lastBilledMonth = subscriber.lastBilledMonth.trim(),
                notes = subscriber.notes.trim(),
                createdAtMillis = now,
                updatedAtMillis = now,
            )

            val initialPayment = if (cleanSubscriber.totalPaid > 0L) {
                Payment(
                    subscriberId = newId,
                    subscriberName = cleanSubscriber.fullName,
                    subscriberCode = cleanSubscriber.subscriberCode,
                    amount = cleanSubscriber.totalPaid,
                    billingMonth = cleanSubscriber.lastBilledMonth.ifBlank { "Initial" }.take(20),
                    paymentMethod = PaymentMethod.CASH.name,
                    note = "Opening paid balance",
                    paidAtMillis = now,
                    createdAtMillis = now,
                )
            } else {
                null
            }

            dao.insertSubscriberWithOptionalPayment(cleanSubscriber, initialPayment)
            Result.success(newId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSubscriber(subscriber: Subscriber): Result<Unit> {
        return try {
            require(subscriber.id.isNotBlank()) { "Subscriber ID cannot be blank" }
            validateSubscriberInput(subscriber)
            val updated = subscriber.copy(
                subscriberCode = subscriber.subscriberCode.trim(),
                fullName = subscriber.fullName.trim(),
                phoneNumber = subscriber.phoneNumber.trim(),
                cnic = subscriber.cnic.trim(),
                location = subscriber.location.trim(),
                areaZone = subscriber.areaZone.trim(),
                packageName = subscriber.packageName.trim(),
                status = SubscriberStatus.fromRaw(subscriber.status).name,
                monthlyBill = subscriber.monthlyBill.coerceIn(0L, 10_000_000L),
                pendingBill = subscriber.pendingBill.coerceIn(0L, 100_000_000L),
                totalPaid = subscriber.totalPaid.coerceIn(0L, 1_000_000_000L),
                lastBilledMonth = subscriber.lastBilledMonth.trim(),
                notes = subscriber.notes.trim(),
                updatedAtMillis = System.currentTimeMillis(),
            )
            dao.updateSubscriber(updated)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSubscriber(subscriberId: String): Result<Unit> {
        return try {
            require(subscriberId.isNotBlank()) { "Subscriber ID cannot be blank" }
            dao.deleteSubscriberById(subscriberId)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recordPayment(
        subscriber: Subscriber,
        amount: Long,
        billingMonth: String,
        method: PaymentMethod,
        note: String,
    ): Result<Unit> {
        return try {
            require(subscriber.id.isNotBlank()) { "Subscriber ID cannot be blank" }
            require(amount in 1L..100_000_000L) { "Payment amount must be greater than zero" }
            val cleanMonth = billingMonth.trim().take(20)
            require(cleanMonth.isNotEmpty()) { "Billing month is required" }

            val now = System.currentTimeMillis()
            val newPending = (subscriber.pendingBill - amount).coerceAtLeast(0L)
            val newTotalPaid = (subscriber.totalPaid + amount).coerceAtMost(1_000_000_000L)
            val nextStatus = if (subscriber.parsedStatus == SubscriberStatus.NEW) {
                SubscriberStatus.ACTIVE.name
            } else {
                subscriber.parsedStatus.name
            }

            val payment = Payment(
                subscriberId = subscriber.id,
                subscriberName = subscriber.fullName.trim().take(80),
                subscriberCode = subscriber.subscriberCode.trim().take(40),
                amount = amount,
                billingMonth = cleanMonth,
                paymentMethod = method.name,
                note = note.trim().take(200),
                paidAtMillis = now,
                createdAtMillis = now,
            )

            val updatedSubscriber = subscriber.copy(
                pendingBill = newPending,
                totalPaid = newTotalPaid,
                lastBilledMonth = cleanMonth,
                status = nextStatus,
                updatedAtMillis = now,
            )

            dao.recordPaymentTransaction(payment, updatedSubscriber)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateMonthlyBills(
        subscribers: List<Subscriber>,
        monthKey: String,
    ): Result<Int> {
        return try {
            val cleanMonth = monthKey.trim().take(20)
            require(cleanMonth.isNotEmpty()) { "Billing month is required" }

            val now = System.currentTimeMillis()
            val eligible = subscribers.filter { sub ->
                sub.id.isNotBlank() &&
                    sub.parsedStatus != SubscriberStatus.SUSPENDED &&
                    sub.monthlyBill > 0L &&
                    sub.lastBilledMonth != cleanMonth
            }
            if (eligible.isEmpty()) return Result.success(0)

            val updates = eligible.map { sub ->
                sub.copy(
                    pendingBill = (sub.pendingBill + sub.monthlyBill).coerceAtMost(100_000_000L),
                    lastBilledMonth = cleanMonth,
                    updatedAtMillis = now,
                )
            }
            dao.updateSubscribers(updates)
            Result.success(updates.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun validateSubscriberInput(subscriber: Subscriber) {
        require(subscriber.subscriberCode.trim().length in 1..40) {
            "User ID must be between 1 and 40 characters."
        }
        require(subscriber.fullName.trim().length in 1..80) {
            "Full name must be between 1 and 80 characters."
        }
        require(subscriber.phoneNumber.trim().length in 5..25) {
            "Phone number must be between 5 and 25 characters."
        }
        require(subscriber.cnic.trim().length in 5..20) {
            "CNIC number must be between 5 and 20 characters."
        }
        require(subscriber.location.trim().length in 1..160) {
            "Location must be between 1 and 160 characters."
        }
        require(subscriber.areaZone.trim().length <= 60) {
            "Area/Zone cannot exceed 60 characters."
        }
        require(subscriber.packageName.trim().length <= 60) {
            "Package name cannot exceed 60 characters."
        }
        require(subscriber.monthlyBill in 0L..10_000_000L) {
            "Monthly bill must be non-negative."
        }
        require(subscriber.pendingBill in 0L..100_000_000L) {
            "Pending bill must be non-negative."
        }
        require(subscriber.totalPaid in 0L..1_000_000_000L) {
            "Total paid must be non-negative."
        }
        require(subscriber.notes.trim().length <= 300) {
            "Notes cannot exceed 300 characters."
        }
    }
}
