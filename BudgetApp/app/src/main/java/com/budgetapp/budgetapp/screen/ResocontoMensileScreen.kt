package com.budgetapp.budgetapp.screen
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetapp.budgetapp.data.Entry
import com.budgetapp.budgetapp.repository.BudgetRepository
import com.budgetapp.budgetapp.ui.components.AppDrawer
import com.budgetapp.budgetapp.ui.components.PrivacyPolicyDialog
import com.budgetapp.budgetapp.ui.components.SettingsMenu
import com.budgetapp.budgetapp.ui.theme.MyWalletTheme
import com.budgetapp.budgetapp.utils.AppString
import com.budgetapp.budgetapp.viewmodel.BudgetViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ResocontoMensileScreen(
    month: Int,
    year: Int,
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
    val monthlySummary by budgetViewModel.monthlySummary.collectAsState()
    val monthlyEntries by budgetViewModel.monthlyEntries.collectAsState()
    val currentUser by budgetViewModel.currentUser.collectAsState()

    // Pager centrato: initialPage rappresenta il mese/anno passato come parametro
    val initialPage = 12 * 50 // centro virtuale
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { Int.MAX_VALUE })
    val scope = rememberCoroutineScope()

    // Calcola il mese/anno corrente in base alla pagina del pager
    val baseCalendar = remember(month, year) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    // Mese visualizzato corrente (aggiornato al cambio pagina)
    val currentPageOffset = pagerState.currentPage - initialPage
    val displayedCalendar = remember(currentPageOffset, month, year) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, currentPageOffset)
        }
    }
    val displayedMonth = displayedCalendar.get(Calendar.MONTH)
    val displayedYear = displayedCalendar.get(Calendar.YEAR)

    // Carica le entry ogni volta che cambia il mese visualizzato
    LaunchedEffect(displayedMonth, displayedYear) {
        budgetViewModel.loadMonthlyEntries(displayedMonth, displayedYear)
    }

    val currentLocale = AppString.currentLocale()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val configuration = LocalConfiguration.current
    val drawerWidth = remember(configuration) { configuration.screenWidthDp.dp * 0.75f }

    var settingsMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

    val monthTitle = remember(displayedMonth, displayedYear, currentLocale) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        SimpleDateFormat("LLLL yyyy", currentLocale).format(cal.time)
            .replaceFirstChar { it.uppercaseChar() }
    }

    val incomeEntries by remember(monthlyEntries) { mutableStateOf(monthlyEntries.filter { it.amount > 0 }) }
    val expenseEntries by remember(monthlyEntries) { mutableStateOf(monthlyEntries.filter { it.amount < 0 }) }

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
                            text = "${strings.overview} - $monthTitle",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
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
                // --- Navigazione mese con frecce ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.back,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = monthTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Avanti",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // --- Pager orizzontale per il contenuto del mese ---
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Il contenuto è lo stesso per ogni pagina: i dati vengono aggiornati
                    // tramite LaunchedEffect al cambio di displayedMonth/displayedYear
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            SummaryCard(
                                title = strings.totalIncome.uppercase(),
                                amount = monthlySummary.totalIncome,
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.weight(1f)
                            )
                            SummaryCard(
                                title = strings.totalExpense.uppercase(),
                                amount = monthlySummary.totalExpenses,
                                color = Color(0xFFF44336),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = strings.totalBalance,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val netBalance = monthlySummary.totalIncome + monthlySummary.totalExpenses
                                Text(
                                    text = formatCurrency(netBalance),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (netBalance >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                                )
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Text(
                                text = strings.income.uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = strings.expense.uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(incomeEntries, key = { it.id }) { entry ->
                                    HalfEntryItem(entry)
                                }
                            }
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(expenseEntries, key = { it.id }) { entry ->
                                    HalfEntryItem(entry)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Dialoghi ---
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
                    onClick = { budgetViewModel.deleteAccount { }; showDeleteConfirmationDialog = false },
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
                                Text(langName)
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
fun HalfEntryItem(entry: Entry) {
    val currentLocale = AppString.currentLocale()
    val currencyFormatter = remember(AppString.currentLanguage.value) { NumberFormat.getCurrencyInstance(currentLocale) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = entry.description,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${AppString.categoryLabel(entry.category)} - ${AppString.paymentMethodLabel(entry.paymentMethod)} - ${SimpleDateFormat("dd/MM/yyyy HH:mm", currentLocale).format(entry.date)}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = currencyFormatter.format(entry.amount),
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = if (entry.amount >= 0) Color.Green.copy(alpha = 0.7f) else Color.Red.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun SummaryCard(title: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatCurrency(amount),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = Color.White
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewResocontoMensileScreen() {
    val repo = remember { BudgetRepository(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance()) }
    val vm = remember { BudgetViewModel(repo) }
    MyWalletTheme(darkTheme = true) {
        ResocontoMensileScreen(month = Calendar.JULY, year = 2025, onBack = {}, budgetViewModel = vm)
    }
}