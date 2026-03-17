package com.budgetapp.budgetapp.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import com.budgetapp.budgetapp.viewmodel.BudgetViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

/**
 * Schermata Log delle Entry.
 * Visualizza le transazioni per un giorno specifico o tutte le transazioni.
 * Permette di modificare ed eliminare le entry.
 *
 * @param selectedDate Data opzionale per filtrare le entry. Se null, mostra tutte le entry.
 * @param onBack Callback per tornare alla schermata precedente.
 * @param budgetViewModel ViewModel per la gestione dei dati del budget.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    selectedDate: Date? = null,
    onBack: () -> Unit,
    budgetViewModel: BudgetViewModel,
    onNavigateToInfo: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToLog: () -> Unit = {},
    onNavigateToStatistics: () -> Unit = {},
    isDarkTheme: Boolean = true,
    onThemeToggle: () -> Unit = {}
) {
    val strings = AppString.get()
    val context = LocalContext.current
    LaunchedEffect(Unit) { AppString.ensureCategoriesLoaded(context) }

    val currentLocale = AppString.currentLocale()
    val currentUser by budgetViewModel.currentUser.collectAsState()

    // Drawer
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val drawerWidth = remember(configuration) { configuration.screenWidthDp.dp * 0.75f }

    // Settings menu state
    var settingsMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

    // Imposta / reset data selezionata nel VM
    LaunchedEffect(selectedDate) { budgetViewModel.setSelectedDateForLog(selectedDate) }

    val baseEntriesFlow = if (selectedDate != null) budgetViewModel.filteredEntriesForSelectedDay else budgetViewModel.allEntries
    val baseEntries by baseEntriesFlow.collectAsState()

    var selectedCategoryFilter by remember { mutableStateOf("all") }
    val allCategories = AppString.allCategoryKeys(context)
    var catFilterExpanded by remember { mutableStateOf(false) }

    val visibleEntries = remember(baseEntries, selectedCategoryFilter) {
        if (selectedCategoryFilter == "all") baseEntries else baseEntries.filter { it.category.equals(selectedCategoryFilter, true) }
    }
    val sortedEntries = remember(visibleEntries) { visibleEntries.sortedByDescending { it.date } }

    var showEntryDialog by remember { mutableStateOf(false) }
    var entryToEdit by remember { mutableStateOf<Entry?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var entryToDeleteId by remember { mutableStateOf<String?>(null) }

    val currencyFormatter = remember(AppString.currentLanguage.value) { NumberFormat.getCurrencyInstance(currentLocale) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            AppDrawer(
                drawerState = drawerState,
                drawerWidth = drawerWidth,
                currentUser = currentUser,
                onNavigateToInfo = onNavigateToInfo,
                onNavigateToHome = onNavigateToHome,
                onNavigateToCalendar = onNavigateToCalendar,
                onNavigateToLog = onNavigateToLog,
                onNavigateToStatistics = onNavigateToStatistics
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (selectedDate != null) "${strings.transactions} - ${SimpleDateFormat("dd/MM/yyyy", currentLocale).format(selectedDate)}" else strings.transactions,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = strings.menu, tint = MaterialTheme.colorScheme.onBackground)
                        }
                    },
                    actions = {
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
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                ExposedDropdownMenuBox(
                    expanded = catFilterExpanded,
                    onExpandedChange = { catFilterExpanded = !catFilterExpanded }
                ) {
                    TextField(
                        value = if (selectedCategoryFilter == "all") strings.allOption else AppString.categoryLabel(selectedCategoryFilter),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(strings.category) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catFilterExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = catFilterExpanded,
                        onDismissRequest = { catFilterExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(strings.allOption) },
                            onClick = { selectedCategoryFilter = "all"; catFilterExpanded = false }
                        )
                        allCategories.forEach { catKey ->
                            DropdownMenuItem(
                                text = { Text(AppString.categoryLabel(catKey)) },
                                onClick = { selectedCategoryFilter = catKey; catFilterExpanded = false }
                            )
                        }
                    }
                }

                if (sortedEntries.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(strings.noEntriesFound, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f), fontSize = 18.sp)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        items(sortedEntries, key = { it.id }) { entry ->
                            if (entry.description.isNotEmpty())
                                LogEntryItem(
                                    entry = entry,
                                    currencyFormatter = currencyFormatter,
                                    onEdit = { entryToEdit = it; showEntryDialog = true },
                                    onDelete = { entryToDeleteId = it.id; showDeleteConfirmDialog = true }
                                )
                        }
                    }
                }
            }

            if (showEntryDialog) {
                AddEditEntryDialog(
                    entry = entryToEdit,
                    onDismiss = { showEntryDialog = false },
                    onConfirm = { entry: Entry ->
                        if (entry.id.isNotEmpty()) budgetViewModel.updateEntry(entry) else budgetViewModel.addEntry(entry)
                        showEntryDialog = false
                        entryToEdit = null
                    }
                )
            }

            if (showDeleteConfirmDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirmDialog = false },
                    title = { Text(strings.delete) },
                    text = { Text(strings.deleteAccountMessage) },
                    confirmButton = {
                        Button(
                            onClick = {
                                entryToDeleteId?.let { budgetViewModel.deleteEntry(it) }
                                showDeleteConfirmDialog = false
                                entryToDeleteId = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) { Text(strings.delete) }
                    },
                    dismissButton = {
                        OutlinedButton(onClick = { showDeleteConfirmDialog = false }) { Text(strings.cancel) }
                    },
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }
        }
    }

    // --- Dialoghi settings ---
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(strings.confirmSignOut) },
            text = { Text(strings.areYouSureYouWantToSignOut) },
            confirmButton = {
                Button(
                    onClick = { showSignOutDialog = false; budgetViewModel.signOut() },
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
                        budgetViewModel.deleteAccount { }
                        showDeleteConfirmationDialog = false
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
                    mapOf("it" to "Italiano", "en" to "English", "es" to "Español", "fr" to "Français", "pt" to "Português")
                        .forEach { (langCode, langName) ->
                            TextButton(onClick = { AppString.currentLanguage.value = langCode; showLanguageDialog = false }) {
                                Text(text = langName)
                            }
                        }
                }
            },
            confirmButton = { TextButton(onClick = { showLanguageDialog = false }) { Text(strings.cancel) } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showPrivacyPolicyDialog) {
        PrivacyPolicyDialog(onDismiss = { showPrivacyPolicyDialog = false })
    }
}

@Composable
fun LogEntryItem(
    entry: Entry,
    currencyFormatter: NumberFormat,
    onEdit: (Entry) -> Unit,
    onDelete: (Entry) -> Unit
) {
    val strings = AppString.get()
    val currentLocale = AppString.currentLocale()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.description, fontWeight = FontWeight.Medium, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = "${AppString.categoryLabel(entry.category)} - ${AppString.paymentMethodLabel(entry.paymentMethod)} - ${SimpleDateFormat("dd/MM/yyyy HH:mm", currentLocale).format(entry.date)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = currencyFormatter.format(entry.amount),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = if (entry.amount >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
            Spacer(Modifier.width(8.dp))
            Row {
                IconButton(onClick = { onEdit(entry) }) {
                    Icon(Icons.Default.Edit, contentDescription = strings.edit, tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { onDelete(entry) }) {
                    Icon(Icons.Default.Delete, contentDescription = strings.delete, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLogScreen() {
    val repo = remember { BudgetRepository(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance()) }
    val vm = remember { BudgetViewModel(repo) }
    MyWalletTheme(darkTheme = true) { LogScreen(onBack = {}, budgetViewModel = vm) }
}
