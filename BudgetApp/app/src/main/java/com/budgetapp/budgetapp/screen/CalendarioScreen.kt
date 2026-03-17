package com.budgetapp.budgetapp.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetapp.budgetapp.ui.theme.MyWalletTheme
import com.budgetapp.budgetapp.viewmodel.BudgetViewModel
import com.budgetapp.budgetapp.repository.BudgetRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Date
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import java.time.YearMonth
import java.time.LocalDate
import com.budgetapp.budgetapp.ui.components.AddEditEntryDialog
import com.budgetapp.budgetapp.ui.components.AppDrawer
import com.budgetapp.budgetapp.ui.components.PrivacyPolicyDialog
import com.budgetapp.budgetapp.ui.components.SettingsMenu
import com.budgetapp.budgetapp.utils.AppString
import java.time.format.DateTimeFormatter
import java.text.NumberFormat
import kotlinx.coroutines.launch

/**
 * Calendar Screen.
 * Displays a calendar with the sum of transactions for each day.
 * Allows navigation to the entry log for a specific day or to the monthly summary.
 *
 * @param onNavigateToLog Callback to navigate to the Log screen with a selected day.
 * @param onNavigateToMonthlySummary Callback to navigate to the Monthly Summary screen.
 * @param onBack Callback to return to the previous screen.
 * @param budgetViewModel ViewModel for budget data management.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CalendarioScreen(
    onNavigateToLog: (Date) -> Unit,
    onNavigateToMonthlySummary: (Int, Int) -> Unit,
    onBack: () -> Unit,
    budgetViewModel: BudgetViewModel,
    onNavigateToInfo: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToStatistics: () -> Unit = {},

    isDarkTheme: Boolean = true,
    onThemeToggle: () -> Unit = {}
) {
    val allEntries by budgetViewModel.allEntries.collectAsState()
    val strings = AppString.get()
    val currentUser by budgetViewModel.currentUser.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val drawerWidth = remember(configuration) { configuration.screenWidthDp.dp * 0.75f }

    var settingsMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

    // Stati per i dialogs
    var showAddEntryTypeDialog by remember { mutableStateOf(false) }
    var showAddEntryDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf<Date?>(null) }
    var isNewEntryIncome by remember { mutableStateOf<Boolean?>(null) }

    // Locale corrente in base alla lingua selezionata
    val currentLocale = AppString.currentLocale()

    // State for the month and year displayed in the calendar
    // Inizializza il pagerState per centrare sul mese corrente
    val initialPage = 12 * 50 // Un punto centrale per permettere lo scroll in entrambe le direzioni
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { Int.MAX_VALUE })

    // Utilizza un LaunchedEffect per aggiornare il mese visualizzato quando la pagina cambia
    var displayedMonth by remember { mutableStateOf(YearMonth.now()) }

    LaunchedEffect(pagerState.currentPage) {
        val monthOffset = pagerState.currentPage - initialPage
        val newMonth = YearMonth.now().plusMonths(monthOffset.toLong())
        if (newMonth != displayedMonth) {
            displayedMonth = newMonth
        }
    }

    // Calculate daily sums for the current month
    val dailySums = remember(allEntries, displayedMonth) {
        val sums = mutableMapOf<Int, Double>() // Day of the month to sum
        val currentMonth = displayedMonth.monthValue - 1 // Calendar.MONTH è 0-based
        val currentYear = displayedMonth.year

        allEntries.forEach { entry ->
            val entryCal = Calendar.getInstance().apply { time = entry.date }
            if (entryCal.get(Calendar.MONTH) == currentMonth && entryCal.get(Calendar.YEAR) == currentYear) {
                val dayOfMonth = entryCal.get(Calendar.DAY_OF_MONTH)
                sums[dayOfMonth] = (sums[dayOfMonth] ?: 0.0) + entry.amount
            }
        }
        sums
    }

    // Formatter localizzato per il titolo del mese (usa forma standalone del mese)
    val monthFormatter = remember(currentLocale) {
        DateTimeFormatter.ofPattern("LLLL yyyy", currentLocale)
    }

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
                onNavigateToLog = { onNavigateToLog(Date()) },
                onNavigateToStatistics = onNavigateToStatistics
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(strings.interactiveCalendar, color = MaterialTheme.colorScheme.onBackground) },
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
                    .verticalScroll(rememberScrollState()) // Make the content scrollable
            ) {
                // Month header (senza frecce)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center // Centra il testo del mese
                ) {
                    ElevatedCard(
                        modifier = Modifier
                            .clickable {
                                // On month click, navigate to monthly summary
                                onNavigateToMonthlySummary(
                                    displayedMonth.monthValue - 1, // Calendar.MONTH è 0-based
                                    displayedMonth.year
                                )
                            }
                            .padding(horizontal = 24.dp, vertical = 16.dp), // Padding interno
                        shape = RoundedCornerShape(50.dp), // Angoli arrotondati
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primary) // Colore primary pieno
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = displayedMonth.format(monthFormatter).replaceFirstChar { it.uppercaseChar() },
                                fontSize = 28.sp, // Titolo più grande
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = strings.tapForSummary, // Sottotitolo
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Weekday names
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        ),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val weekdays = listOf(
                        strings.sun,
                        strings.mon,
                        strings.tue,
                        strings.wed,
                        strings.thu,
                        strings.fri,
                        strings.sat
                    )
                    weekdays.forEach { day ->
                        Text(
                            text = day,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 8.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Griglia del calendario con scorrimento orizzontale
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                ) { page ->
                    val monthOffset = page - initialPage
                    val currentMonthForPager = YearMonth.now().plusMonths(monthOffset.toLong())

                    // Calcola i giorni del mese per la griglia
                    val firstDayOfMonth = currentMonthForPager.atDay(1)
                    val startDayOffset = firstDayOfMonth.dayOfWeek.value % 7

                    val daysInMonth = currentMonthForPager.lengthOfMonth()

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(7),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 450.dp)
                            .padding(horizontal = 8.dp),
                        contentPadding = PaddingValues(4.dp),
                        userScrollEnabled = false
                    ) {
                        // Empty days before the start of the month
                        items(startDayOffset) {
                            Spacer(modifier = Modifier.aspectRatio(1f))
                        }

                        // Days of the month
                        items(daysInMonth) { index ->
                            val day = index + 1
                            val date = currentMonthForPager.atDay(day)

                            val totalAmount = dailySums[day] ?: 0.0
                            val isToday = date == LocalDate.now()

                            DayCell(
                                day = day,
                                totalAmount = totalAmount,
                                isToday = isToday,
                                currencyFormatter = currencyFormatter,
                                onClick = {
                                    // Converti LocalDate in Date impostando l'ora a 12:00
                                    val calendar = Calendar.getInstance().apply {
                                        set(date.year, date.monthValue - 1, date.dayOfMonth, 12, 0, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                    selectedDate = calendar.time
                                    showAddEntryTypeDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog per scegliere il tipo di entry (income/expense)
    if (showAddEntryTypeDialog) {
        AlertDialog(
            onDismissRequest = { showAddEntryTypeDialog = false },
            title = { Text(strings.addEntry) },
            text = { Text(strings.whatKindOfEntry) },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Button(
                        onClick = {
                            isNewEntryIncome = false
                            showAddEntryTypeDialog = false
                            showAddEntryDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.7f))
                    ) { Text(strings.expense) }
                    Button(
                        onClick = {
                            isNewEntryIncome = true
                            showAddEntryTypeDialog = false
                            showAddEntryDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Green.copy(alpha = 0.7f))
                    ) { Text(strings.income) }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddEntryTypeDialog = false }) {
                    Text(strings.cancel)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Mostra il dialogo di aggiunta entry quando richiesto
    if (showAddEntryDialog && selectedDate != null) {
        AddEditEntryDialog(
            entry = null,
            onDismiss = {
                showAddEntryDialog = false
                isNewEntryIncome = null
            },
            onConfirm = { entry ->
                budgetViewModel.addEntry(entry)
                showAddEntryDialog = false
                isNewEntryIncome = null
            },
            selectedDateTime = selectedDate!!,
            isIncome = isNewEntryIncome
        )
    }

    // --- Dialoghi settings ---
    if (showSignOutDialog) {
        AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(strings.confirmSignOut) },
            text = { Text(strings.areYouSureYouWantToSignOut) },
            confirmButton = {
                Button(onClick = { showSignOutDialog = false; budgetViewModel.signOut() }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(strings.signOut) }
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
                Button(onClick = { budgetViewModel.deleteAccount { }; showDeleteConfirmationDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(strings.yes) }
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
                            TextButton(onClick = { AppString.currentLanguage.value = langCode; showLanguageDialog = false }) { Text(langName) }
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
fun DayCell(day: Int, totalAmount: Double, isToday: Boolean, currencyFormatter: NumberFormat, onClick: () -> Unit) {
    val backgroundColor = if (isToday) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    else MaterialTheme.colorScheme.surface

    Column(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = day.toString(),
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (totalAmount != 0.0) {
            Text(
                text = currencyFormatter.format(totalAmount),
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (totalAmount >= 0) Color.Green.copy(alpha = 0.7f) else Color.Red.copy(alpha = 0.7f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Text(text = "", fontSize = 8.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCalendarioScreen() {
    val repo = remember { BudgetRepository(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance()) }
    val vm = remember { BudgetViewModel(repo) }
    MyWalletTheme(darkTheme = true) {
        CalendarioScreen(
            onNavigateToLog = {},
            onNavigateToMonthlySummary = { _, _ -> },
            onBack = {},
            budgetViewModel = vm
        )
    }
}
