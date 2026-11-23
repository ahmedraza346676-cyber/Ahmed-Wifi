package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.Payment
import com.example.data.model.Subscriber
import kotlinx.coroutines.flow.Flow

@Dao
interface NetLedgerDao {
    @Query("SELECT * FROM subscribers ORDER BY createdAtMillis DESC")
    fun observeSubscribers(): Flow<List<Subscriber>>

    @Query("SELECT * FROM payments ORDER BY paidAtMillis DESC")
    fun observePayments(): Flow<List<Payment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscriber(subscriber: Subscriber)

    @Update
    suspend fun updateSubscriber(subscriber: Subscriber)

    @Update
    suspend fun updateSubscribers(subscribers: List<Subscriber>)

    @Query("DELETE FROM subscribers WHERE id = :subscriberId")
    suspend fun deleteSubscriberById(subscriberId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: Payment)

    @Transaction
    suspend fun insertSubscriberWithOptionalPayment(subscriber: Subscriber, payment: Payment?) {
        insertSubscriber(subscriber)
        if (payment != null) {
            insertPayment(payment)
        }
    }

    @Transaction
    suspend fun recordPaymentTransaction(payment: Payment, updatedSubscriber: Subscriber) {
        insertPayment(payment)
        updateSubscriber(updatedSubscriber)
    }
}

@Database(
    entities = [Subscriber::class, Payment::class],
    version = 1,
    exportSchema = false,
)
abstract class NetLedgerDatabase : RoomDatabase() {
    abstract fun netLedgerDao(): NetLedgerDao

    companion object {
        @Volatile
        private var INSTANCE: NetLedgerDatabase? = null

        fun getInstance(context: Context): NetLedgerDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NetLedgerDatabase::class.java,
                    "netledger_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
