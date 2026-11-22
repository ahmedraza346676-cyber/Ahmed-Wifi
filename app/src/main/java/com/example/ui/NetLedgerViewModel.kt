package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.NetLedgerRepository
import com.example.data.model.BillingFormatters
import com.example.data.model.Payment
import com.example.data.model.PaymentMethod
import com.example.data.model.Subscriber
import com.example.data.model.SubscriberStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class SubscriberFilter(val label: String) {
    ALL("All"),
    PENDING("Pending Dues"),
    PAID("Clear / Paid"),
    NEW_USERS("New Users"),
    SUSPENDED("Suspended")
}

enum class SubscriberSort(val label: String) {
    PENDING_DESC("Highest Pending"),
    RECENT("Newest First"),
    NAME_ASC("Name (A–Z)")
}

data class AreaStats(
    val areaName: String,
    val count: Int,
    val pendingTotal: Long,
)

data class DashboardSummary(
    val totalSubscribers: Int = 0,
    val activeSubscribers: Int = 0,
    val newSubscribers: Int = 0,
    val suspendedSubscribers: Int = 0,
    val monthlyRecurringRevenue: Long = 0L,
    val totalPending: Long = 0L,
    val totalPaid: Long = 0L,
    val pendingAccountsCount: Int = 0,
    val unbilledThisMonthCount: Int = 0,
    val currentMonthKey: String = BillingFormatters.currentYearMonth(),
    val areaBreakdown: List<AreaStats> = emptyList(),
)

class NetLedgerViewModel(
    private val repository: NetLedgerRepository,
    private val currentUserId: String,
) : ViewModel() {

    val subscribersState: StateFlow<UiState<List<Subscriber>>> =
        repository.observeSubscribers(currentUserId)
            .map<List<Subscriber>, UiState<List<Subscriber>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing subscribers", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load subscribers"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val paymentsState: StateFlow<UiState<List<Payment>>> =
        repository.observePayments(currentUserId)
            .map<List<Payment>, UiState<List<Payment>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing payments", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "Failed to load payment history"))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(SubscriberFilter.ALL)
    val selectedFilter: StateFlow<SubscriberFilter> = _selectedFilter.asStateFlow()

    private val _selectedSort = MutableStateFlow(SubscriberSort.PENDING_DESC)
    val selectedSort: StateFlow<SubscriberSort> = _selectedSort.asStateFlow()

    private val _selectedArea = MutableStateFlow<String?>(null)
    val selectedArea: StateFlow<String?> = _selectedArea.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    val dashboardSummary: StateFlow<DashboardSummary> = subscribersState
        .map { state ->
            val list = (state as? UiState.Success)?.data ?: emptyList()
            val monthKey = BillingFormatters.currentYearMonth()
            val activeCount = list.count { it.parsedStatus == SubscriberStatus.ACTIVE }
            val newCount = list.count { it.parsedStatus == SubscriberStatus.NEW }
            val suspendedCount = list.count { it.parsedStatus == SubscriberStatus.SUSPENDED }
            val mrr = list.filter { it.parsedStatus != SubscriberStatus.SUSPENDED }
                .sumOf { it.monthlyBill }
            val pending = list.sumOf { it.pendingBill }
            val paid = list.sumOf { it.totalPaid }
            val pendingAccounts = list.count { it.pendingBill > 0L }
            val unbilled = list.count {
                it.parsedStatus != SubscriberStatus.SUSPENDED &&
                    it.monthlyBill > 0L &&
                    it.lastBilledMonth != monthKey
            }
            val areas = list
                .groupBy { it.areaZone.trim().ifBlank { "General" } }
                .map { (area, members) ->
                    AreaStats(
                        areaName = area,
                        count = members.size,
                        pendingTotal = members.sumOf { it.pendingBill }
                    )
                }
                .sortedByDescending { it.pendingTotal }

            DashboardSummary(
                totalSubscribers = list.size,
                activeSubscribers = activeCount,
                newSubscribers = newCount,
                suspendedSubscribers = suspendedCount,
                monthlyRecurringRevenue = mrr,
                totalPending = pending,
                totalPaid = paid,
                pendingAccountsCount = pendingAccounts,
                unbilledThisMonthCount = unbilled,
                currentMonthKey = monthKey,
                areaBreakdown = areas
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = DashboardSummary()
        )

    val filteredSubscribers: StateFlow<List<Subscriber>> = combine(
        subscribersState,
        _searchQuery,
        _selectedFilter,
        _selectedSort,
        _selectedArea,
    ) { state, query, filter, sort, area ->
        val raw = (state as? UiState.Success)?.data ?: emptyList()
        val q = query.trim().lowercase()

        val filtered = raw.filter { sub ->
            val matchesQuery = q.isEmpty() ||
                sub.fullName.lowercase().contains(q) ||
                sub.subscriberCode.lowercase().contains(q) ||
                sub.phoneNumber.lowercase().contains(q) ||
                sub.cnic.lowercase().contains(q) ||
                sub.location.lowercase().contains(q) ||
                sub.areaZone.lowercase().contains(q) ||
                sub.packageName.lowercase().contains(q)

            val matchesFilter = when (filter) {
                SubscriberFilter.ALL -> true
                SubscriberFilter.PENDING -> sub.pendingBill > 0L
                SubscriberFilter.PAID -> sub.pendingBill == 0L
                SubscriberFilter.NEW_USERS -> sub.parsedStatus == SubscriberStatus.NEW
                SubscriberFilter.SUSPENDED -> sub.parsedStatus == SubscriberStatus.SUSPENDED
            }

            val matchesArea = area == null ||
                sub.areaZone.trim().ifBlank { "General" }.equals(area, ignoreCase = true)

            matchesQuery && matchesFilter && matchesArea
        }

        when (sort) {
            SubscriberSort.PENDING_DESC -> filtered.sortedByDescending { it.pendingBill }
            SubscriberSort.NAME_ASC -> filtered.sortedBy { it.fullName.lowercase() }
            SubscriberSort.RECENT -> filtered.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = emptyList()
    )

    fun setSearchQuery(value: String) {
        _searchQuery.value = value
    }

    fun setFilter(filter: SubscriberFilter) {
        _selectedFilter.value = filter
    }

    fun setSort(sort: SubscriberSort) {
        _selectedSort.value = sort
    }

    fun setAreaFilter(area: String?) {
        _selectedArea.value = area
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun saveSubscriber(
        subscriber: Subscriber,
        isEdit: Boolean,
        onSaved: (String) -> Unit,
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = if (isEdit) {
                repository.updateSubscriber(subscriber).map { subscriber.id }
            } else {
                repository.addSubscriber(subscriber)
            }
            _isBusy.value = false
            result.onSuccess { id ->
                _snackbarMessage.value = if (isEdit) {
                    "${subscriber.fullName} updated"
                } else {
                    "${subscriber.fullName} added to NetLedger"
                }
                onSaved(id)
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Unable to save subscriber"
            }
        }
    }

    fun deleteSubscriber(subscriber: Subscriber, onDeleted: () -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteSubscriber(subscriber.id)
            _isBusy.value = false
            result.onSuccess {
                _snackbarMessage.value = "${subscriber.fullName} removed"
                onDeleted()
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to delete subscriber"
            }
        }
    }

    fun recordPayment(
        subscriber: Subscriber,
        amount: Long,
        billingMonth: String,
        method: PaymentMethod,
        note: String,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.recordPayment(
                subscriber = subscriber,
                amount = amount,
                billingMonth = billingMonth,
                method = method,
                note = note
            )
            _isBusy.value = false
            result.onSuccess {
                _snackbarMessage.value =
                    "Collected ${BillingFormatters.formatPkr(amount)} from ${subscriber.fullName}"
                onComplete()
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to record payment"
            }
        }
    }

    fun generateMonthlyBills() {
        val currentList = (subscribersState.value as? UiState.Success)?.data ?: return
        val monthKey = BillingFormatters.currentYearMonth()
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.generateMonthlyBills(currentList, monthKey)
            _isBusy.value = false
            result.onSuccess { billedCount ->
                _snackbarMessage.value = if (billedCount > 0) {
                    "Generated ${BillingFormatters.formatMonthLabel(monthKey)} bills for $billedCount subscriber(s)"
                } else {
                    "All active subscribers are already billed for ${BillingFormatters.formatMonthLabel(monthKey)}"
                }
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to generate monthly bills"
            }
        }
    }

    private companion object {
        const val TAG = "NetLedgerVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
