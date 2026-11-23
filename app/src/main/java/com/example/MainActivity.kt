package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.NetLedgerRepository
import com.example.data.model.Subscriber
import com.example.ui.NetLedgerViewModel
import com.example.ui.SubscriberFilter
import com.example.ui.UiState
import com.example.ui.components.RecordPaymentSheet
import com.example.ui.screens.AddEditSubscriberScreen
import com.example.ui.screens.BillingTab
import com.example.ui.screens.DashboardTab
import com.example.ui.screens.SubscriberDetailScreen
import com.example.ui.screens.SubscribersTab
import com.example.ui.theme.NetLedgerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NetLedgerTheme {
                NetLedgerWorkspace()
            }
        }
    }
}

private enum class MainTab(
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    DASHBOARD(R.string.nav_dashboard, Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    SUBSCRIBERS(R.string.nav_subscribers, Icons.Filled.Group, Icons.Outlined.Group),
    BILLING(R.string.nav_billing, Icons.AutoMirrored.Filled.ReceiptLong, Icons.AutoMirrored.Outlined.ReceiptLong),
}

private sealed interface ActiveScreen {
    data object Tabs : ActiveScreen
    data class Detail(val subscriberId: String) : ActiveScreen
    data class Form(val subscriberId: String?) : ActiveScreen
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetLedgerWorkspace() {
    val viewModel: NetLedgerViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                NetLedgerViewModel(NetLedgerRepository(app))
            }
        }
    )

    val subscribersState by viewModel.subscribersState.collectAsStateWithLifecycle()
    val paymentsState by viewModel.paymentsState.collectAsStateWithLifecycle()
    val summary by viewModel.dashboardSummary.collectAsStateWithLifecycle()
    val filteredSubscribers by viewModel.filteredSubscribers.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val selectedSort by viewModel.selectedSort.collectAsStateWithLifecycle()
    val selectedArea by viewModel.selectedArea.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    var currentTab by remember { mutableStateOf(MainTab.DASHBOARD) }
    var activeScreen by remember { mutableStateOf<ActiveScreen>(ActiveScreen.Tabs) }
    var paymentTarget by remember { mutableStateOf<Subscriber?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearSnackbar()
    }

    val allSubscribers = (subscribersState as? UiState.Success)?.data ?: emptyList()
    val allPayments = (paymentsState as? UiState.Success)?.data ?: emptyList()

    when (val screen = activeScreen) {
        is ActiveScreen.Detail -> {
            val subscriber = allSubscribers.firstOrNull { it.id == screen.subscriberId }
            if (subscriber == null) {
                activeScreen = ActiveScreen.Tabs
            } else {
                SubscriberDetailScreen(
                    subscriber = subscriber,
                    payments = allPayments,
                    isBusy = isBusy,
                    onBack = { activeScreen = ActiveScreen.Tabs },
                    onEdit = { activeScreen = ActiveScreen.Form(subscriber.id) },
                    onDelete = {
                        viewModel.deleteSubscriber(subscriber) {
                            activeScreen = ActiveScreen.Tabs
                        }
                    },
                    onRecordPayment = { paymentTarget = subscriber },
                    onCopiedMessage = { viewModel.showMessage(it) }
                )
            }
        }

        is ActiveScreen.Form -> {
            val editing = screen.subscriberId?.let { id ->
                allSubscribers.firstOrNull { it.id == id }
            }
            val nextCode = remember(allSubscribers.size) {
                "NET-${1001 + allSubscribers.size}"
            }
            val knownAreas = remember(allSubscribers) {
                allSubscribers.mapNotNull { it.areaZone.trim().takeIf(String::isNotEmpty) }.distinct()
            }
            AddEditSubscriberScreen(
                existingSubscriber = editing,
                suggestedNextCode = nextCode,
                existingAreas = knownAreas,
                isBusy = isBusy,
                onBack = {
                    activeScreen = if (editing != null) {
                        ActiveScreen.Detail(editing.id)
                    } else {
                        ActiveScreen.Tabs
                    }
                },
                onSave = { savedModel ->
                    viewModel.saveSubscriber(
                        subscriber = savedModel,
                        isEdit = editing != null,
                        onSaved = { savedId ->
                            activeScreen = ActiveScreen.Detail(savedId)
                        }
                    )
                }
            )
        }

        ActiveScreen.Tabs -> {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavigationRail = maxWidth >= 600.dp

                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            title = {
                                Text(
                                    text = when (currentTab) {
                                        MainTab.DASHBOARD -> stringResource(R.string.app_name)
                                        MainTab.SUBSCRIBERS -> stringResource(R.string.nav_subscribers)
                                        MainTab.BILLING -> stringResource(R.string.nav_billing)
                                    }
                                )
                            }
                        )
                    },
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = { activeScreen = ActiveScreen.Form(null) },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.testTag("fab_add_subscriber")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.add_subscriber)
                            )
                        }
                    },
                    bottomBar = {
                        if (!useNavigationRail) {
                            NavigationBar {
                                MainTab.entries.forEach { tab ->
                                    val selected = currentTab == tab
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { currentTab = tab },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = stringResource(tab.labelRes)
                                            )
                                        },
                                        label = { Text(stringResource(tab.labelRes)) },
                                        modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (useNavigationRail) {
                            NavigationRail(modifier = Modifier.fillMaxHeight()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                MainTab.entries.forEach { tab ->
                                    val selected = currentTab == tab
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { currentTab = tab },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = stringResource(tab.labelRes)
                                            )
                                        },
                                        label = { Text(stringResource(tab.labelRes)) },
                                        modifier = Modifier.testTag("rail_${tab.name.lowercase()}")
                                    )
                                }
                            }
                        }

                        Box(
                            contentAlignment = Alignment.TopCenter,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = 860.dp)
                                    .fillMaxSize()
                            ) {
                                when (val state = subscribersState) {
                                    UiState.Loading -> {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(40.dp))
                                            Spacer(modifier = Modifier.height(12.dp))
                                            Text(
                                                text = "Loading NetLedger…",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    is UiState.Error -> {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(24.dp)
                                        ) {
                                            Text(
                                                text = "Could not load subscribers",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = state.message,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    is UiState.Success -> {
                                        when (currentTab) {
                                            MainTab.DASHBOARD -> DashboardTab(
                                                operatorName = "ISP Manager",
                                                summary = summary,
                                                subscribers = state.data,
                                                isBusy = isBusy,
                                                onAddSubscriber = { activeScreen = ActiveScreen.Form(null) },
                                                onOpenSubscribers = {
                                                    viewModel.setFilter(SubscriberFilter.ALL)
                                                    viewModel.setAreaFilter(null)
                                                    currentTab = MainTab.SUBSCRIBERS
                                                },
                                                onOpenPendingFilter = {
                                                    viewModel.setFilter(SubscriberFilter.PENDING)
                                                    viewModel.setAreaFilter(null)
                                                    currentTab = MainTab.SUBSCRIBERS
                                                },
                                                onSelectArea = { areaName ->
                                                    viewModel.setAreaFilter(areaName)
                                                    viewModel.setFilter(SubscriberFilter.ALL)
                                                    currentTab = MainTab.SUBSCRIBERS
                                                },
                                                onSubscriberClick = { sub ->
                                                    activeScreen = ActiveScreen.Detail(sub.id)
                                                },
                                                onCollectClick = { sub -> paymentTarget = sub },
                                                onGenerateMonthlyBills = { viewModel.generateMonthlyBills() }
                                            )

                                            MainTab.SUBSCRIBERS -> SubscribersTab(
                                                subscribers = filteredSubscribers,
                                                totalCount = state.data.size,
                                                searchQuery = searchQuery,
                                                selectedFilter = selectedFilter,
                                                selectedSort = selectedSort,
                                                selectedArea = selectedArea,
                                                onSearchQueryChange = viewModel::setSearchQuery,
                                                onFilterChange = viewModel::setFilter,
                                                onSortChange = viewModel::setSort,
                                                onClearArea = { viewModel.setAreaFilter(null) },
                                                onAddSubscriber = { activeScreen = ActiveScreen.Form(null) },
                                                onSubscriberClick = { sub ->
                                                    activeScreen = ActiveScreen.Detail(sub.id)
                                                },
                                                onCollectClick = { sub -> paymentTarget = sub }
                                            )

                                            MainTab.BILLING -> BillingTab(
                                                summary = summary,
                                                subscribers = state.data,
                                                payments = allPayments,
                                                isBusy = isBusy,
                                                onSubscriberClick = { sub ->
                                                    activeScreen = ActiveScreen.Detail(sub.id)
                                                },
                                                onCollectClick = { sub -> paymentTarget = sub },
                                                onGenerateMonthlyBills = { viewModel.generateMonthlyBills() }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    paymentTarget?.let { target ->
        RecordPaymentSheet(
            subscriber = target,
            isBusy = isBusy,
            onDismiss = { paymentTarget = null },
            onConfirmPayment = { amount, month, method, note ->
                viewModel.recordPayment(
                    subscriber = target,
                    amount = amount,
                    billingMonth = month,
                    method = method,
                    note = note,
                    onComplete = { paymentTarget = null }
                )
            }
        )
    }
}
