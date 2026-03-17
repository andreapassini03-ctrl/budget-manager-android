package com.budgetapp.budgetapp.screen

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetapp.budgetapp.data.Entry
import com.budgetapp.budgetapp.repository.BudgetRepository
import com.budgetapp.budgetapp.ui.components.AddEditEntryDialog
import com.budgetapp.budgetapp.ui.components.AppDrawer
import com.budgetapp.budgetapp.ui.components.PrivacyPolicyDialog
import com.budgetapp.budgetapp.ui.components.SettingsMenu
import com.budgetapp.budgetapp.ui.theme.MyWalletTheme
import com.budgetapp.budgetapp.utils.AppString
import com.budgetapp.budgetapp.utils.LocalizedStrings
import com.budgetapp.budgetapp.viewmodel.BudgetViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Calendar

// --- NUOVO ENUM PER PERIODI RIEPILOGO ---
private enum class SummaryPeriod { TODAY, YESTERDAY, CURRENT_MONTH, LAST_MONTH, CURRENT_YEAR, LAST_YEAR }

// Helper per etichetta localizzata
private fun SummaryPeriod.label(strings: LocalizedStrings): String = when (this) {
    SummaryPeriod.TODAY -> strings.today
    SummaryPeriod.YESTERDAY -> strings.yesterday
    SummaryPeriod.CURRENT_MONTH -> strings.currentMonth
    SummaryPeriod.LAST_MONTH -> strings.lastMonth
    SummaryPeriod.CURRENT_YEAR -> strings.currentYear
    SummaryPeriod.LAST_YEAR -> strings.lastYear
}

// Helper per importo da HomePageSummary
private fun SummaryPeriod.amount(summary: BudgetViewModel.HomePageSummary): Double = when (this) {
    SummaryPeriod.TODAY -> summary.totalToday
    SummaryPeriod.YESTERDAY -> summary.totalYesterday
    SummaryPeriod.CURRENT_MONTH -> summary.totalCurrentMonth
    SummaryPeriod.LAST_MONTH -> summary.totalLastMonth
    SummaryPeriod.CURRENT_YEAR -> summary.totalCurrentYear
    SummaryPeriod.LAST_YEAR -> summary.totalLastYear
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePage(
    onNavigateToInfo: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToLog: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToMonthlySummary: (Int, Int) -> Unit,
    budgetViewModel: BudgetViewModel, // rimosso default factory
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    val strings = AppString.get()
    val homePageSummary by budgetViewModel.homePageSummary.collectAsState()
    val currentUser by budgetViewModel.currentUser.collectAsState()
    val sortedEntries by budgetViewModel.sortedEntries.collectAsState() // usa lista già ordinata

    var selectedSummaryPeriodName by rememberSaveable { mutableStateOf(SummaryPeriod.CURRENT_MONTH.name) }
    val selectedSummaryPeriod = remember(selectedSummaryPeriodName) { SummaryPeriod.valueOf(selectedSummaryPeriodName) }
    var showAddEntryTypeDialog by remember { mutableStateOf(false) }
    var showEntryDialog by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<Entry?>(null) }
    var isNewEntryIncome by remember { mutableStateOf<Boolean?>(null) }
    var settingsMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val context = androidx.compose.ui.platform.LocalContext.current
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(com.budgetapp.budgetapp.R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }
    val configuration = LocalConfiguration.current
    val drawerWidth = remember(configuration) { configuration.screenWidthDp.dp * 0.75f }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                drawerState = drawerState,
                drawerWidth = drawerWidth,
                currentUser = currentUser,
                onNavigateToInfo = onNavigateToInfo,
                onNavigateToHome = { /* già in home */ },
                onNavigateToCalendar = onNavigateToCalendar,
                onNavigateToLog = onNavigateToLog,
                onNavigateToStatistics = onNavigateToStatistics
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(strings.myWallet, color = MaterialTheme.colorScheme.onBackground) },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch { drawerState.open() }
                        }) {
                            Icon(Icons.Filled.Menu, contentDescription = strings.menu, tint = MaterialTheme.colorScheme.onBackground)
                        }
                    },
                    actions = {
                        // Box come anchor del menu Settings
                        Box {
                            IconButton(onClick = { settingsMenuExpanded = true }) {
                                Icon(Icons.Filled.Settings, contentDescription = strings.myAccount, tint = MaterialTheme.colorScheme.onBackground)
                            }
                            SettingsMenu(
                                expanded = settingsMenuExpanded,
                                isDarkTheme = isDarkTheme,
                                onDismiss = { settingsMenuExpanded = false },
                                onLanguageClick = { showLanguageDialog = true },
                                onThemeToggle = onThemeToggle,
                                onPrivacyPolicyClick = { showPrivacyPolicyDialog = true },
                                onSignOutClick = { showSignOutDialog = true },
                                onDeleteAccountClick = { showDeleteConfirmationDialog = true }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { showAddEntryTypeDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) { Icon(Icons.Default.Add, contentDescription = strings.addEntry) }
            },
            floatingActionButtonPosition = FabPosition.Center
        ) { paddingValues ->
            // Contenuto precedente invariato (rimosse definizioni DropdownMenu principali)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = strings.overview,
                            fontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        var summaryExpanded by rememberSaveable { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = summaryExpanded,
                            onExpandedChange = { summaryExpanded = !summaryExpanded },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            TextField(
                                value = selectedSummaryPeriod.label(strings),
                                onValueChange = {},
                                readOnly = true,
                                singleLine = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = summaryExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth(),
                            )
                            ExposedDropdownMenu(
                                expanded = summaryExpanded,
                                onDismissRequest = { summaryExpanded = false }
                            ) {
                                SummaryPeriod.entries.forEach { period ->
                                    DropdownMenuItem(
                                        text = { Text(period.label(strings)) },
                                        onClick = {
                                            selectedSummaryPeriodName = period.name
                                            summaryExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val displayAmount = selectedSummaryPeriod.amount(homePageSummary)

                        Text(
                            text = formatCurrency(displayAmount),
                            fontSize = 32.sp,
                            color = if (displayAmount >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                        )

                        // Card per navigare al resoconto mensile solo per i periodi mese corrente / mese scorso
                        if (selectedSummaryPeriod == SummaryPeriod.CURRENT_MONTH || selectedSummaryPeriod == SummaryPeriod.LAST_MONTH) {
                            HomeSummaryCard(onCardClick = {
                                val calendar = Calendar.getInstance()
                                when (selectedSummaryPeriod) {
                                    SummaryPeriod.CURRENT_MONTH -> onNavigateToMonthlySummary(calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR))
                                    SummaryPeriod.LAST_MONTH -> {
                                        calendar.add(Calendar.MONTH, -1)
                                        onNavigateToMonthlySummary(calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR))
                                    }
                                    else -> Unit
                                }
                            })
                        }
                    }
                }

                Text(
                    text = strings.recentTransactions,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(sortedEntries, key = { it.id }) { entry ->
                        if (entry.description.isNotEmpty()) EntryItem(entry) { onNavigateToLog() }
                    }
                }
            }
        }
    }

    // Dialogs -------------------------------------------------------------
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(strings.confirmSignOut) },
            text = { Text(strings.areYouSureYouWantToSignOut) },
            confirmButton = {
                Button(
                    onClick = {
                        showSignOutDialog = false
                        googleSignInClient.signOut().addOnCompleteListener { budgetViewModel.signOut() }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(strings.signOut) }
            },
            dismissButton = { TextButton(onClick = { showSignOutDialog = false }) { Text(strings.cancel) } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showDeleteConfirmationDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = { Text(strings.deleteAccountTitle) },
            text = { Text(strings.deleteAccountMessage) },
            confirmButton = {
                Button(
                    onClick = {
                        budgetViewModel.deleteAccount { success ->
                            if (success) {
                                googleSignInClient.revokeAccess().addOnCompleteListener { budgetViewModel.signOut() }
                            }
                            showDeleteConfirmationDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(strings.yes) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmationDialog = false }) { Text(strings.no) } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(strings.setUpLanguage) },
            text = {
                Column {
                    val languages = mapOf(
                        "it" to "Italiano",
                        "en" to "English",
                        "es" to "Español",
                        "fr" to "Français",
                        "pt" to "Português"
                    )
                    languages.forEach { (langCode, langName) ->
                        TextButton(onClick = { AppString.currentLanguage.value = langCode; showLanguageDialog = false }) { Text(text = langName) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLanguageDialog = false }) { Text(strings.cancel) } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showAddEntryTypeDialog) {
        AlertDialog(
            onDismissRequest = { showAddEntryTypeDialog = false },
            title = { Text(strings.addEntry) },
            text = { Text(strings.whatKindOfEntry) },
            confirmButton = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Button(
                        onClick = {
                            entryToEdit = null
                            isNewEntryIncome = false
                            showAddEntryTypeDialog = false
                            showEntryDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f))
                    ) { Text(strings.expense) }
                    Button(
                        onClick = {
                            entryToEdit = null
                            isNewEntryIncome = true
                            showAddEntryTypeDialog = false
                            showEntryDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green.copy(alpha = 0.7f))
                    ) { Text(strings.income) }
                }
            },
            dismissButton = { TextButton(onClick = { showAddEntryTypeDialog = false }) { Text(strings.cancel) } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showEntryDialog) {
        AddEditEntryDialog(
            entry = entryToEdit,
            onDismiss = {
                showEntryDialog = false
                entryToEdit = null
                isNewEntryIncome = null
            },
            onConfirm = { entry: Entry ->
                if (entry.id.isNotEmpty()) budgetViewModel.updateEntry(entry) else budgetViewModel.addEntry(entry)
                showEntryDialog = false
                entryToEdit = null
                isNewEntryIncome = null
            },
            isIncome = isNewEntryIncome
        )
    }

    if (showPrivacyPolicyDialog) {
        PrivacyPolicyDialog(
            onDismiss = { showPrivacyPolicyDialog = false }
        )
    }
}

@Composable
fun EntryItem(entry: Entry, onNavigateToLog: (String) -> Unit = {}) {
    val currentLocale = AppString.currentLocale()
    val currencyFormatter = remember(AppString.currentLanguage.value) { NumberFormat.getCurrencyInstance(currentLocale) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onNavigateToLog(entry.id) },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)

        //Aggingi click a logScreen

    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = entry.description, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = "${AppString.categoryLabel(entry.category)} - ${AppString.paymentMethodLabel(entry.paymentMethod)} ",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = currencyFormatter.format(entry.amount),
                fontSize = 18.sp,
                color = if (entry.amount >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
}

@Composable
fun HomeSummaryCard(onCardClick: () -> Unit) {
    val strings = AppString.get()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable(onClick = onCardClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = strings.monthInformation, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewHomePage() {
    // Preview: crea un ViewModel standalone (solo per anteprima)
    val repo = remember { BudgetRepository(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance()) }
    val vm = remember { BudgetViewModel(repo) }
    MyWalletTheme(darkTheme = true) {
        HomePage(
            onNavigateToInfo = {},
            onNavigateToCalendar = {},
            onNavigateToLog = {},
            onNavigateToStatistics = {},
            onNavigateToMonthlySummary = { _, _ -> },
            budgetViewModel = vm,
            isDarkTheme = true,
            onThemeToggle = {}
        )
    }
}
