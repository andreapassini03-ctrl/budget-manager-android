// File: MainActivity.kt
package com.budgetapp.budgetapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.budgetapp.budgetapp.repository.BudgetRepository
import com.budgetapp.budgetapp.screen.CalendarioScreen
import com.budgetapp.budgetapp.screen.HomePage
import com.budgetapp.budgetapp.screen.InfoDialog
import com.budgetapp.budgetapp.screen.LogScreen
import com.budgetapp.budgetapp.screen.LoginScreen
import com.budgetapp.budgetapp.screen.OnboardingScreen
import com.budgetapp.budgetapp.screen.ResocontoMensileScreen
import com.budgetapp.budgetapp.screen.StatisticheScreen
import com.budgetapp.budgetapp.ui.theme.MyWalletTheme
import com.budgetapp.budgetapp.utils.OnboardingPreferences
import com.budgetapp.budgetapp.viewmodel.BudgetViewModel
import com.budgetapp.budgetapp.viewmodel.BudgetViewModelFactory
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent { MyWalletApp() }
    }
}

@Composable
fun MyWalletApp() {
    val context = LocalContext.current

    // Stato del tema dark/light (salvato in memoria durante la sessione)
    var isDarkTheme by rememberSaveable { mutableStateOf(true) }

    // Verifica se l'onboarding è stato completato
    val onboardingCompleted = remember { OnboardingPreferences.isOnboardingCompleted(context) }

    MyWalletTheme(darkTheme = isDarkTheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val navController = rememberNavController()
            val budgetRepository = remember { BudgetRepository(FirebaseAuth.getInstance(), FirebaseFirestore.getInstance()) }
            val budgetViewModel: BudgetViewModel = viewModel(factory = BudgetViewModelFactory(repository = budgetRepository))

            val isAuthenticated by budgetViewModel.isAuthenticated.collectAsState()
            val initialUser = remember { FirebaseAuth.getInstance().currentUser }

            // Stato per il popup Info
            var showInfoDialog by remember { mutableStateOf(false) }
            val currentUser by budgetViewModel.currentUser.collectAsState()

            // Determina la destinazione iniziale: onboarding -> login/home
            val startDestination = remember {
                when {
                    !onboardingCompleted -> "onboarding"
                    initialUser != null -> "home"
                    else -> "login"
                }
            }

            // Effetto: gestisce SOLO il logout
            LaunchedEffect(isAuthenticated) {
                val current = navController.currentDestination?.route
                if (!isAuthenticated && current != "login" && current != "onboarding") {
                    Log.d("NavAuth", "Logout rilevato: navigo a login")
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }

            // Popup Info mostrato sopra qualsiasi schermata
            if (showInfoDialog) {
                InfoDialog(
                    show = true,
                    onDismiss = { showInfoDialog = false },
                    appVersion = "1.0",
                    user = currentUser
                )
            }

            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                // Onboarding screen
                composable("onboarding") {
                    OnboardingScreen(
                        onFinish = {
                            OnboardingPreferences.setOnboardingCompleted(context)
                            // Dopo onboarding, vai al login
                            navController.navigate("login") {
                                popUpTo("onboarding") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }

                composable("login") {
                    LoginScreen(
                        onAuthSuccess = {
                            if (navController.currentDestination?.route == "login") {
                                navController.navigate("home") {
                                    popUpTo("login") { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        },
                        onSignInAsGuest = { budgetViewModel.signInAnonymously() },
                        viewModel = budgetViewModel
                    )
                }

                composable("home") {
                    HomePage(
                        onNavigateToInfo = { showInfoDialog = true },
                        onNavigateToCalendar = { navController.navigate("calendar") },
                        onNavigateToLog = { navController.navigate("log") },
                        onNavigateToStatistics = { navController.navigate("statistics") },
                        onNavigateToMonthlySummary = { month, year ->
                            navController.navigate("monthly_summary/$month/$year")
                        },
                        budgetViewModel = budgetViewModel,
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = { isDarkTheme = !isDarkTheme }
                    )
                }

                composable("calendar") {
                    CalendarioScreen(
                        onNavigateToLog = { date ->
                            navController.currentBackStackEntry?.savedStateHandle?.set("selectedDate", date.time)
                            navController.navigate("log_by_date")
                        },
                        onNavigateToMonthlySummary = { month, year -> navController.navigate("monthly_summary/$month/$year") },
                        onBack = { navController.popBackStack() },
                        budgetViewModel = budgetViewModel,
                        onNavigateToInfo = { showInfoDialog = true },
                        onNavigateToCalendar = { /* già qui */ },
                        onNavigateToHome = { navController.navigate("home") { launchSingleTop = true } },
                        onNavigateToStatistics = { navController.navigate("statistics") },
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = { isDarkTheme = !isDarkTheme }
                    )
                }

                composable("log") {
                    LogScreen(
                        onBack = { navController.popBackStack() },
                        budgetViewModel = budgetViewModel,
                        onNavigateToInfo = { showInfoDialog = true },
                        onNavigateToCalendar = { navController.navigate("calendar") },
                        onNavigateToLog = { /* già qui */ },
                        onNavigateToHome = { navController.navigate("home") { launchSingleTop = true } },
                        onNavigateToStatistics = { navController.navigate("statistics") },
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = { isDarkTheme = !isDarkTheme }
                    )
                }

                composable("log_by_date") { backStackEntry ->
                    val selectedDateMillis = backStackEntry.savedStateHandle.get<Long>("selectedDate")
                    val selectedDate = selectedDateMillis?.let { Date(it) }
                    LogScreen(
                        selectedDate = selectedDate,
                        onBack = { navController.popBackStack() },
                        budgetViewModel = budgetViewModel,
                        onNavigateToInfo = { showInfoDialog = true },
                        onNavigateToCalendar = { navController.navigate("calendar") },
                        onNavigateToLog = { navController.navigate("log") },
                        onNavigateToHome = { navController.navigate("home") { launchSingleTop = true } },
                        onNavigateToStatistics = { navController.navigate("statistics") },
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = { isDarkTheme = !isDarkTheme }
                    )
                }

                composable("monthly_summary/{month}/{year}") { backStackEntry ->
                    val month = backStackEntry.arguments?.getString("month")?.toIntOrNull()
                    val year = backStackEntry.arguments?.getString("year")?.toIntOrNull()
                    if (month != null && year != null) {
                        ResocontoMensileScreen(
                            month = month,
                            year = year,
                            onBack = { navController.popBackStack() },
                            budgetViewModel = budgetViewModel,
                            onNavigateToInfo = { showInfoDialog = true },
                            onNavigateToCalendar = { navController.navigate("calendar") },
                            onNavigateToLog = { navController.navigate("log") },
                            onNavigateToHome = { navController.navigate("home") { launchSingleTop = true } },
                            onNavigateToStatistics = { navController.navigate("statistics") },
                            isDarkTheme = isDarkTheme,
                            onThemeToggle = { isDarkTheme = !isDarkTheme }
                        )
                    } else {
                        Text("Error: Invalid month or year for monthly summary.")
                    }
                }

                composable("statistics") {
                    StatisticheScreen(
                        onBack = { navController.popBackStack() },
                        budgetViewModel = budgetViewModel,
                        onNavigateToInfo = { showInfoDialog = true },
                        onNavigateToCalendar = { navController.navigate("calendar") },
                        onNavigateToLog = { navController.navigate("log") },
                        onNavigateToHome = { navController.navigate("home") { launchSingleTop = true } },
                        onNavigateToStatistics = { /* già qui */ },
                        isDarkTheme = isDarkTheme,
                        onThemeToggle = { isDarkTheme = !isDarkTheme }
                    )
                }
            }
        }
    }
}
