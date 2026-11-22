package com.example.data

import android.content.Context
import com.example.R
import com.example.data.model.Payment
import com.example.data.model.PaymentMethod
import com.example.data.model.Subscriber
import com.example.data.model.SubscriberStatus
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class NetLedgerRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Please sign in with Google before accessing NetLedger.")

    fun observeSubscribers(userId: String): Flow<List<Subscriber>> = flow {
        val uid = userId.takeIf { it.isNotBlank() } ?: requireUserId()
        val path = "users/$uid/subscribers"
        emitAll(
            db.collection("users").document(uid).collection("subscribers")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Subscriber::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observePayments(userId: String): Flow<List<Payment>> = flow {
        val uid = userId.takeIf { it.isNotBlank() } ?: requireUserId()
        val path = "users/$uid/payments"
        emitAll(
            db.collection("users").document(uid).collection("payments")
                .orderBy("paidAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Payment::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addSubscriber(subscriber: Subscriber): Result<String> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/subscribers"
        return try {
            val uid = requireUserId()
            validateSubscriberInput(subscriber)

            val subscribersRef = db.collection("users").document(uid).collection("subscribers")
            val docRef = subscribersRef.document()
            docPath = docRef.path

            val batch = db.batch()
            batch.set(docRef, subscriber.toCreateMap(uid))

            if (subscriber.totalPaid > 0L) {
                val paymentRef = db.collection("users").document(uid).collection("payments").document()
                val initialPaymentPayload = mapOf(
                    "userId" to uid,
                    "subscriberId" to docRef.id,
                    "subscriberName" to subscriber.fullName.trim(),
                    "subscriberCode" to subscriber.subscriberCode.trim(),
                    "amount" to subscriber.totalPaid.coerceIn(1L, 100_000_000L),
                    "billingMonth" to subscriber.lastBilledMonth.ifBlank { "Initial" }.take(20),
                    "paymentMethod" to PaymentMethod.CASH.name,
                    "note" to "Opening paid balance",
                    "paidAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                )
                batch.set(paymentRef, initialPaymentPayload)
            }

            batch.commit().await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateSubscriber(subscriber: Subscriber): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/subscribers/${subscriber.id}"
        return try {
            val uid = requireUserId()
            require(subscriber.id.isNotBlank()) { "Subscriber ID cannot be blank" }
            validateSubscriberInput(subscriber)

            val docRef = db.collection("users").document(uid)
                .collection("subscribers").document(subscriber.id)
            docPath = docRef.path
            docRef.update(subscriber.toProfileUpdateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteSubscriber(subscriberId: String): Result<Unit> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/subscribers/$subscriberId"
        return try {
            val uid = requireUserId()
            require(subscriberId.isNotBlank()) { "Subscriber ID cannot be blank" }
            val docRef = db.collection("users").document(uid)
                .collection("subscribers").document(subscriberId)
            docPath = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
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
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/payments"
        return try {
            val uid = requireUserId()
            require(subscriber.id.isNotBlank()) { "Subscriber ID cannot be blank" }
            require(amount in 1L..100_000_000L) { "Payment amount must be greater than zero" }
            val cleanMonth = billingMonth.trim().take(20)
            require(cleanMonth.isNotEmpty()) { "Billing month is required" }
            val cleanNote = note.trim().take(200)

            val subRef = db.collection("users").document(uid)
                .collection("subscribers").document(subscriber.id)
            val payRef = db.collection("users").document(uid)
                .collection("payments").document()
            docPath = payRef.path

            val newPending = (subscriber.pendingBill - amount).coerceAtLeast(0L)
            val newTotalPaid = (subscriber.totalPaid + amount).coerceAtMost(1_000_000_000L)
            val nextStatus = if (subscriber.parsedStatus == SubscriberStatus.NEW) {
                SubscriberStatus.ACTIVE.name
            } else {
                subscriber.parsedStatus.name
            }

            val batch = db.batch()
            batch.set(
                payRef,
                mapOf(
                    "userId" to uid,
                    "subscriberId" to subscriber.id,
                    "subscriberName" to subscriber.fullName.trim().take(80),
                    "subscriberCode" to subscriber.subscriberCode.trim().take(40),
                    "amount" to amount,
                    "billingMonth" to cleanMonth,
                    "paymentMethod" to method.name,
                    "note" to cleanNote,
                    "paidAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.update(
                subRef,
                mapOf(
                    "pendingBill" to newPending,
                    "totalPaid" to newTotalPaid,
                    "lastBilledMonth" to cleanMonth,
                    "status" to nextStatus,
                    "updatedAt" to FieldValue.serverTimestamp(),
                )
            )
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun generateMonthlyBills(
        subscribers: List<Subscriber>,
        monthKey: String,
    ): Result<Int> {
        var docPath = "users/${auth.currentUser?.uid ?: "unauthenticated"}/subscribers"
        return try {
            val uid = requireUserId()
            val cleanMonth = monthKey.trim().take(20)
            require(cleanMonth.isNotEmpty()) { "Billing month is required" }

            val eligible = subscribers.filter { sub ->
                sub.id.isNotBlank() &&
                    sub.parsedStatus != SubscriberStatus.SUSPENDED &&
                    sub.monthlyBill > 0L &&
                    sub.lastBilledMonth != cleanMonth
            }
            if (eligible.isEmpty()) return Result.success(0)

            // Firestore batch limit is 500 writes
            eligible.chunked(450).forEach { chunk ->
                val batch = db.batch()
                chunk.forEach { sub ->
                    val subRef = db.collection("users").document(uid)
                        .collection("subscribers").document(sub.id)
                    docPath = subRef.path
                    val updatedPending = (sub.pendingBill + sub.monthlyBill).coerceAtMost(100_000_000L)
                    batch.update(
                        subRef,
                        mapOf(
                            "pendingBill" to updatedPending,
                            "lastBilledMonth" to cleanMonth,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        )
                    )
                }
                batch.commit().await()
            }
            Result.success(eligible.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
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
