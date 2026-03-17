// File: StatisticheScreen.kt
package com.budgetapp.budgetapp.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.budgetapp.budgetapp.repository.BudgetRepository
import com.budgetapp.budgetapp.ui.components.AppDrawer
import com.budgetapp.budgetapp.ui.components.PrivacyPolicyDialog
import com.budgetapp.budgetapp.ui.components.SettingsMenu
import com.budgetapp.budgetapp.ui.theme.MyWalletTheme
import com.budgetapp.budgetapp.utils.AppString
import com.budgetapp.budgetapp.viewmodel.BudgetViewModel
import com.budgetapp.budgetapp.viewmodel.BudgetViewModelFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * Schermata Statistiche.
 * Mostra statistiche sulle transazioni con filtri per importo, metodo di pagamento e tipologia.
 * Include grafici a torta e istogramma per visualizzare i dati per categoria.
 *
 * @param onBack Callback per tornare alla schermata precedente.
 * @param budgetViewModel ViewModel per la gestione dei dati del budget.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticheScreen(
    onBack: () -> Unit,
    budgetViewModel: BudgetViewModel = viewModel(
        factory = BudgetViewModelFactory(
            repository = BudgetRepository(
                auth = FirebaseAuth.getInstance(),
                firestore = FirebaseFirestore.getInstance()
            )
        )
    ),
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

    val currentUser by budgetViewModel.currentUser.collectAsState()
    val statistics by budgetViewModel.statistics.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val drawerWidth = remember(configuration) { configuration.screenWidthDp.dp * 0.75f }

    var settingsMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var showSignOutDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmationDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }

    // Filtri: usa chiavi stabili e mostra label localizzate
    var selectedTransactionTypeKey by remember { mutableStateOf("all") }
    var selectedPaymentMethodKey by remember { mutableStateOf("all") }
    var selectedCategoryKey by remember { mutableStateOf("all") }
    var minAmount by remember { mutableStateOf("") }
    var maxAmount by remember { mutableStateOf("") }

    val transactionTypeKeys = listOf("all") + AppString.transactionTypeKeys.filter { it != "all" }
    val paymentMethodKeys = listOf("all") + AppString.paymentMethodKeys

    // Rende reattiva la lista delle categorie includendo quelle custom
    val allCategories = AppString.allCategoryKeys(context)
    val categoryKeys = remember(allCategories) { listOf("all") + allCategories }

    // Carica le statistiche all'avvio e quando i filtri cambiano
    LaunchedEffect(selectedTransactionTypeKey, selectedPaymentMethodKey, selectedCategoryKey, minAmount, maxAmount) {
        val filters = mutableMapOf<String, Any>()
        filters["transactionType"] = selectedTransactionTypeKey
        if (selectedPaymentMethodKey != "all") filters["paymentMethod"] = selectedPaymentMethodKey
        if (selectedCategoryKey != "all") filters["category"] = selectedCategoryKey

        // Gestisci min/max amount in base al tipo selezionato
        if (selectedTransactionTypeKey != "all") {
            minAmount.toDoubleOrNull()?.let {
                filters["minAmount"] = if (selectedTransactionTypeKey == "expense") -it else it
            }
            maxAmount.toDoubleOrNull()?.let {
                filters["maxAmount"] = if (selectedTransactionTypeKey == "expense") -it else it
            }
        }
        budgetViewModel.loadStatistics(filters)
    }

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
                    title = { Text(strings.statistics, color = MaterialTheme.colorScheme.onBackground) },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Filled.Menu, contentDescription = strings.menu, tint = MaterialTheme.colorScheme.onBackground)
                        }
                    },
                    actions = {
                        androidx.compose.foundation.layout.Box {
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
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Sezione Filtri
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = strings.filters,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Filtro Tipo di Transazione
                        var transactionTypeExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = transactionTypeExpanded,
                            onExpandedChange = { transactionTypeExpanded = !transactionTypeExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            TextField(
                                value = AppString.transactionTypeLabel(selectedTransactionTypeKey),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(strings.transactionType) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = transactionTypeExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                colors = ExposedDropdownMenuDefaults.textFieldColors(
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = transactionTypeExpanded,
                                onDismissRequest = { transactionTypeExpanded = false }
                            ) {
                                transactionTypeKeys.forEach { key ->
                                    DropdownMenuItem(
                                        text = { Text(AppString.transactionTypeLabel(key)) },
                                        onClick = {
                                            selectedTransactionTypeKey = key
                                            transactionTypeExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Filtro Metodo di Pagamento
                        var paymentMethodExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = paymentMethodExpanded,
                            onExpandedChange = { paymentMethodExpanded = !paymentMethodExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val paymentMethodLabel = if (selectedPaymentMethodKey == "all") strings.allOption else AppString.paymentMethodLabel(selectedPaymentMethodKey)
                            TextField(
                                value = paymentMethodLabel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(strings.paymentMethod) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentMethodExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                colors = ExposedDropdownMenuDefaults.textFieldColors(
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = paymentMethodExpanded,
                                onDismissRequest = { paymentMethodExpanded = false }
                            ) {
                                paymentMethodKeys.forEach { key ->
                                    val label = if (key == "all") strings.allOption else AppString.paymentMethodLabel(key)
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            selectedPaymentMethodKey = key
                                            paymentMethodExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Filtro Categoria
                        var categoryExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = categoryExpanded,
                            onExpandedChange = { categoryExpanded = !categoryExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val categoryLabel = if (selectedCategoryKey == "all") strings.allOption else AppString.categoryLabel(selectedCategoryKey)
                            TextField(
                                value = categoryLabel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(strings.category) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                colors = ExposedDropdownMenuDefaults.textFieldColors(
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false }
                            ) {
                                categoryKeys.forEach { key ->
                                    val label = if (key == "all") strings.allOption else AppString.categoryLabel(key)
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            selectedCategoryKey = key
                                            categoryExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sezione Risultati Statistiche
                Text(
                    text = strings.results,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth()
                    ) {
                        if (statistics == null) {
                            Text(
                                text = strings.noStatisticsAvailable,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        } else {
                            if (statistics is Map<*, *>) {
                                val statsMap = statistics as Map<String, Double>
                                if (statsMap.isEmpty()) {
                                    Text(
                                        text = strings.noDataForSelectedFilters,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                } else {
                                    statsMap.forEach { (category, total) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = AppString.categoryLabel(category),
                                                fontSize = 16.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = formatCurrency(total),
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (total >= 0) Color.Green.copy(alpha = 0.7f) else Color.Red.copy(alpha = 0.7f)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }
                            } else {
                                Text(
                                    text = strings.unexpectedStatisticsFormat,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Sezione Grafici
                if (statistics is Map<*, *> && selectedCategoryKey == "all") {
                    val statsMap = statistics as Map<String, Double>
                    if (statsMap.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(24.dp))

                        // Grafico a Torta - SPESE
                        val expensesData = statsMap.filter { it.value < 0 }
                        if (expensesData.isNotEmpty()) {
                            Text(
                                text = "Distribuzione Spese per Categoria",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                PieChartComposable(
                                    data = expensesData,
                                    colorOffset = 0, // Inizia dal primo colore
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp)
                                        .padding(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        // Grafico a Torta - ENTRATE
                        val incomeData = statsMap.filter { it.value > 0 }
                        if (incomeData.isNotEmpty()) {
                            Text(
                                text = "Distribuzione Entrate per Categoria",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                PieChartComposable(
                                    data = incomeData,
                                    colorOffset = expensesData.size, // Inizia dopo i colori delle spese
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(300.dp)
                                        .padding(16.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                        }

                        // Istogramma
                        Text(
                            text = "Confronto Importi per Categoria",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Calcola l'altezza dinamicamente in base al numero di categorie
                        val categoryCount = statsMap.size

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            BarChartComposable(
                                data = statsMap,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(450.dp)
                                    .padding(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    // --- Dialoghi settings ---
    if (showSignOutDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showSignOutDialog = false },
            title = { Text(strings.confirmSignOut) },
            text = { Text(strings.areYouSureYouWantToSignOut) },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = { showSignOutDialog = false; budgetViewModel.signOut() },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(strings.signOut) }
            },
            dismissButton = { TextButton(onClick = { showSignOutDialog = false }) { Text(strings.cancel) } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showDeleteConfirmationDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirmationDialog = false },
            title = { Text(strings.deleteAccountTitle) },
            text = { Text(strings.deleteAccountMessage) },
            confirmButton = {
                androidx.compose.material3.Button(
                    onClick = { budgetViewModel.deleteAccount { }; showDeleteConfirmationDialog = false },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text(strings.yes) }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmationDialog = false }) { Text(strings.no) } },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showLanguageDialog) {
        androidx.compose.material3.AlertDialog(
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

/**
 * Genera una lista di colori diversi per le categorie con alto contrasto
 */
@Composable
fun generateCategoryColors(count: Int, offset: Int = 0): List<Color> {
    // Palette base di colori vivaci e con alto contrasto
    val baseColors = listOf(
        Color(0xFFE91E63), // Rosa acceso
        Color(0xFF9C27B0), // Viola
        Color(0xFF673AB7), // Deep Purple
        Color(0xFF2196F3), // Blue
        Color(0xFF3F51B5), // Indigo
        Color(0xFF00BCD4), // Cyan
        Color(0xFF009688), // Teal
        Color(0xFF4CAF50), // Green
        Color(0xFF8BC34A), // Light Green
        Color(0xFFCDDC39), // Lime
        Color(0xFFFFEB3B), // Yellow
        Color(0xFFFFC107), // Amber
        Color(0xFFFF9800), // Orange
        Color(0xFFFF5722), // Deep Orange
        Color(0xFFF44336), // Red
        Color(0xFF795548), // Brown
        Color(0xFF607D8B), // Blue Grey
        Color(0xFFFF4081), // Pink A200
        Color(0xFFE040FB), // Purple A200
        Color(0xFF7C4DFF), // Deep Purple A200
        Color(0xFF536DFE), // Indigo A200
        Color(0xFF448AFF), // Blue A200
        Color(0xFF40C4FF), // Light Blue A200
        Color(0xFF18FFFF), // Cyan A200
        Color(0xFF64FFDA), // Teal A200
        Color(0xFF69F0AE), // Green A200
        Color(0xFFB2FF59), // Light Green A200
        Color(0xFFEEFF41), // Lime A200
        Color(0xFFFFFF00), // Yellow A200
        Color(0xFFFFD740), // Amber A200
        Color(0xFFFFAB40), // Orange A200
        Color(0xFFFF6E40), // Deep Orange A200
    )

    // Applica l'offset e limita il numero di colori alla dimensione della palette base
    return if (count <= baseColors.size) {
        baseColors.drop(offset).take(count)
    } else {
        // Se servono più colori, generiamo variazioni con alto contrasto
        val result = mutableListOf<Color>()
        result.addAll(baseColors.drop(offset))

        var index = 0
        while (result.size < count) {
            val baseColor = baseColors[(index + offset) % baseColors.size]
            val variation = (result.size / baseColors.size) * 0.2f

            // Alterna tra schiarire e scurire per massimo contrasto
            val factor = if ((result.size / baseColors.size) % 2 == 0) 1f else -1f

            result.add(
                baseColor.copy(
                    alpha = 1f,
                    red = (baseColor.red + variation * factor).coerceIn(0f, 1f),
                    green = (baseColor.green + variation * factor * 0.8f).coerceIn(0f, 1f),
                    blue = (baseColor.blue + variation * factor * 0.6f).coerceIn(0f, 1f)
                )
            )
            index++
        }
        result.take(count)
    }
}

/**
 * Grafico a Torta personalizzato con Canvas
 */
@Composable
fun PieChartComposable(
    data: Map<String, Double>,
    colorOffset: Int = 0,
    modifier: Modifier = Modifier
) {
    val categories = data.keys.toList()
    val colors = generateCategoryColors(categories.size, colorOffset)
    val total = data.values.sumOf { abs(it) }

    if (total == 0.0) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Nessun dato da visualizzare", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
        return
    }

    Column(modifier = modifier) {
        // Canvas per il grafico a torta
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val canvasSize = size.minDimension
            val radius = canvasSize / 2.5f
            val centerX = size.width / 2
            val centerY = size.height / 2

            var startAngle = -90f

            categories.forEachIndexed { index, category ->
                val value = abs(data[category] ?: 0.0)
                val sweepAngle = (value / total * 360).toFloat()

                drawArc(
                    color = colors[index],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(
                        centerX - radius,
                        centerY - radius
                    ),
                    size = Size(radius * 2, radius * 2)
                )

                startAngle += sweepAngle
            }
        }

        // Legenda
        Spacer(modifier = Modifier.height(16.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            categories.forEachIndexed { index, category ->
                val value = abs(data[category] ?: 0.0)
                val percentage = (value / total * 100)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(colors[index], shape = RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${AppString.categoryLabel(category)}: ${formatCurrency(value)} (${String.format("%.1f", percentage)}%)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Istogramma personalizzato con Canvas
 */
@Composable
fun BarChartComposable(
    data: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    val categories = data.keys.toList()
    val colors = generateCategoryColors(categories.size)
    val originalValues = categories.map { data[it] ?: 0.0 }

    if (originalValues.all { it == 0.0 }) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("Nessun dato da visualizzare", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
        return
    }

    Column(modifier = modifier) {
        // Calcola il valore massimo assoluto per scalare correttamente
        val maxAbsValue = originalValues.maxOfOrNull { abs(it) } ?: 1.0
        val barWidth = 32.dp
        val spacing = 8.dp

        // Canvas per l'istogramma
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerY = canvasHeight / 2

            // Disegna la linea dello zero al centro
            drawLine(
                color = Color.Gray.copy(alpha = 0.5f),
                start = Offset(0f, centerY),
                end = Offset(canvasWidth, centerY),
                strokeWidth = 2.dp.toPx()
            )

            // Disegna ogni barra
            originalValues.forEachIndexed { index, value ->
                val absValue = abs(value)
                val barHeight = (absValue / maxAbsValue * (canvasHeight / 2) * 0.9f).toFloat()
                val left = index * (barWidth.toPx() + spacing.toPx())
                val right = left + barWidth.toPx()

                // Se positivo, barra verso l'alto; se negativo, verso il basso
                val top: Float
                val bottom: Float

                if (value >= 0) {
                    // Valore positivo: barra sopra la linea centrale
                    top = centerY - barHeight
                    bottom = centerY
                } else {
                    // Valore negativo: barra sotto la linea centrale
                    top = centerY
                    bottom = centerY + barHeight
                }

                drawRoundRect(
                    color = colors[index],
                    topLeft = Offset(left, top),
                    size = Size(right - left, bottom - top),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
            }
        }

        // Legenda per l'istogramma
        Spacer(modifier = Modifier.height(8.dp))
        Column(modifier = Modifier.fillMaxWidth()) {
            categories.forEachIndexed { index, category ->
                val value = data[category] ?: 0.0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .background(colors[index], shape = RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${AppString.categoryLabel(category)}: ${formatCurrency(value)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewStatisticheScreen() {
    MyWalletTheme(darkTheme = true) {
        StatisticheScreen(onBack = {})
    }
}
