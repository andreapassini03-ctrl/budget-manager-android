package com.budgetapp.budgetapp.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetapp.budgetapp.data.Entry
import com.budgetapp.budgetapp.repository.BudgetRepository
import com.budgetapp.budgetapp.utils.AuthPrefs
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

/**
 * ViewModel for managing the business logic of the MyWallet application.
 * Interacts with BudgetRepository for data access and authentication.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BudgetViewModel(
    private val repository: BudgetRepository
) : ViewModel() {

    private val TAG = "viewModelLog"

    // --- Authentication related StateFlows ---
    val currentUser: StateFlow<FirebaseUser?> = repository.currentUser
    val authReady: StateFlow<Boolean> = repository.authReady

    private val _isAuthLoading = MutableStateFlow(false)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _authErrorMessage = MutableStateFlow<String?>(null)
    val authErrorMessage: StateFlow<String?> = _authErrorMessage.asStateFlow()
    // --- End Authentication related StateFlows ---


    // All user entries (real-time updated)
    private val _allEntries = MutableStateFlow<List<Entry>>(emptyList())
    val allEntries: StateFlow<List<Entry>> = _allEntries.asStateFlow()

    // State for the currently selected date in LogScreen for filtering
    private val _selectedDateForLog = MutableStateFlow<Date?>(null)
    val selectedDateForLog: StateFlow<Date?> = _selectedDateForLog.asStateFlow()

    // Entries filtered for the selected day (for LogScreen) - now derived from _allEntries
    val filteredEntriesForSelectedDay: StateFlow<List<Entry>> = combine(
        _allEntries,
        _selectedDateForLog
    ) { allEntries, selectedDate ->
        if (selectedDate == null) {
            emptyList() // Return empty list if no date is selected
        } else {
            val calendarSelectedDate = Calendar.getInstance().apply { time = selectedDate }
            val workCal = Calendar.getInstance()
            allEntries.filter { entry ->
                workCal.time = entry.date
                workCal.get(Calendar.YEAR) == calendarSelectedDate.get(Calendar.YEAR) &&
                        workCal.get(Calendar.DAY_OF_YEAR) == calendarSelectedDate.get(Calendar.DAY_OF_YEAR)
            }
        }
    }.stateIn(
        scope = viewModelScope, // Usa lo scope del ViewModel
        started = SharingStarted.WhileSubscribed(5000), // Inizia a collezionare quando ci sono sottoscrittori, mantiene attivo per 5s dopo l'ultimo
        initialValue = emptyList() // Valore iniziale prima che i dati vengano emessi
    )


    // State for homepage summary (today, current month, current year)
    private val _homePageSummary = MutableStateFlow(HomePageSummary())
    val homePageSummary: StateFlow<HomePageSummary> = _homePageSummary.asStateFlow()

    // State for monthly summary (for MonthlySummaryScreen)
    private val _monthlySummary = MutableStateFlow(MonthlySummary())
    val monthlySummary: StateFlow<MonthlySummary> = _monthlySummary.asStateFlow()

    // State for statistics (for StatisticheScreen)
    private val _statistics = MutableStateFlow<Map<String, Double>?>(null) // Changed to Map<String, Double>
    val statistics: StateFlow<Map<String, Double>?> = _statistics.asStateFlow()

    // State for error or success messages
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // State for sorted entries
    private val _sortedEntries = MutableStateFlow<List<Entry>>(emptyList())
    val sortedEntries: StateFlow<List<Entry>> = _sortedEntries.asStateFlow()

    // Stato autenticazione semplificato per navigazione
    private val _isAuthenticated = MutableStateFlow(repository.currentUser.value != null)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private var computeJob: Job? = null

    init {
        // Aggiorna flag autenticazione
        viewModelScope.launch { currentUser.collect { user -> _isAuthenticated.value = user != null } }
        // Usa flatMapLatest così ogni cambio utente cancella i vecchi listener Firestore
        viewModelScope.launch {
            currentUser
                .flatMapLatest { user -> if (user != null) repository.getAllEntries() else flowOf(emptyList()) }
                .collect { entries ->
                    _allEntries.value = entries
                    computeJob?.cancel()
                    computeJob = viewModelScope.launch(Dispatchers.Default) {
                        val sorted = entries.sortedByDescending { it.date }
                        // Aggiorna summary e sortedEntries
                        updateHomePageSummaryInternal(entries)
                        _sortedEntries.value = sorted
                    }
                }
        }
    }

    suspend fun signInWithGoogle(idToken: String): Boolean {
        _isAuthLoading.value = true
        _authErrorMessage.value = null
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val success = repository.signInWithGoogleCredential(credential)
            if (success) {
                Log.d(TAG, "Google sign-in successful.")
            } else {
                Log.e(TAG, "Google sign-in failed.")
                _authErrorMessage.value = "Google sign-in failed."
            }
            success
        } catch (e: Exception) {
            Log.e(TAG, "Error during Google sign-in: ${e.message}", e)
            _authErrorMessage.value = "Error during Google sign-in: ${e.localizedMessage}"
            false
        } finally {
            _isAuthLoading.value = false
        }
    }

    fun signInAnonymously() {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            try {
                val success = repository.signInAnonymously()
                if (success) {
                    Log.d(TAG, "Anonymous sign-in successful.")
                } else {
                    Log.e(TAG, "Anonymous sign-in failed.")
                    _authErrorMessage.value = "Anonymous sign-in failed."
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during anonymous sign-in: ${e.message}", e)
                _authErrorMessage.value = "Error during anonymous sign-in: ${e.localizedMessage}"
            } finally {
                _isAuthLoading.value = false
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            _isAuthLoading.value = true
            _authErrorMessage.value = null
            try {
                AuthPrefs.markLoggedOut()
                repository.signOut()
                _allEntries.value = emptyList()
                _homePageSummary.value = HomePageSummary()
                _selectedDateForLog.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Error during sign out: ${e.message}", e)
                _authErrorMessage.value = "Error during sign out: ${e.localizedMessage}"
            } finally {
                _isAuthLoading.value = false
            }
        }
    }

    fun deleteAccount(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                _isAuthLoading.value = true
                val success = repository.deleteUserAccount()
                onComplete(success)
                // Pulizia stato locale indipendentemente dall'esito
                _allEntries.value = emptyList()
                _homePageSummary.value = HomePageSummary()
                _selectedDateForLog.value = null
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting user account", e)
                _authErrorMessage.value = "Errore durante l'eliminazione dell'account: ${e.localizedMessage}"
                onComplete(false)
            } finally {
                _isAuthLoading.value = false
            }
        }
    }

    /**
     * Adds a new entry.
     * @param entry The Entry object to add.
     */
    fun addEntry(entry: Entry) {
        viewModelScope.launch {
            try {
                repository.addEntry(entry)
                _message.value = "Entry added successfully!"
            } catch (e: Exception) {
                _message.value = "Error adding entry: ${e.message}"
            }
        }
    }

    /**
     * Updates an existing entry.
     * @param entry The updated Entry object.
     */
    fun updateEntry(entry: Entry) {
        viewModelScope.launch {
            try {
                repository.updateEntry(entry)
                _message.value = "Entry updated successfully!"
            } catch (e: Exception) {
                _message.value = "Error updating entry: ${e.message}"
            }
        }
    }

    /**
     * Deletes an entry.
     * @param entryId The ID of the entry to delete.
     */
    fun deleteEntry(entryId: String) {
        viewModelScope.launch {
            try {
                repository.deleteEntry(entryId)
                _message.value = "Entry deleted successfully!"
            } catch (e: Exception) {
                _message.value = "Error deleting entry: ${e.message}"
            }
        }
    }

    /**
     * Sets the selected date for filtering entries in LogScreen.
     * @param date The date to filter by. Set to null to show all entries.
     */
    fun setSelectedDateForLog(date: Date?) {
        _selectedDateForLog.value = date
    }

    // State for monthly entries
    private val _monthlyEntries = MutableStateFlow<List<Entry>>(emptyList())
    val monthlyEntries: StateFlow<List<Entry>> = _monthlyEntries.asStateFlow()

    /**
     * Loads entries for a specific month and year and calculates the monthly summary.
     * @param month The month (0-11) for which to load entries.
     * @param year The year for which to load entries.
     */
    fun loadMonthlyEntries(month: Int, year: Int) {
        viewModelScope.launch {
            repository.getMonthlyEntries(month, year).collect { entries ->
                _monthlyEntries.value = entries
                calculateMonthlySummary(entries)
            }
        }
    }

    /**
     * Updates the homepage summary (today, current month, current year, yesterday, last month, last year).
     */
    private fun updateHomePageSummary() { /* non più usato esternamente, mantenuto per compatibilità */
        viewModelScope.launch(Dispatchers.Default) { updateHomePageSummaryInternal(_allEntries.value) }
    }

    private fun updateHomePageSummaryInternal(entries: List<Entry>) {
        val todayCal = Calendar.getInstance()
        val yesterdayCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val currentMonthCal = Calendar.getInstance()
        val lastMonthCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        val currentYearCal = Calendar.getInstance()
        val lastYearCal = Calendar.getInstance().apply { add(Calendar.YEAR, -1) }
        val workCal = Calendar.getInstance()

        var totalToday = 0.0
        var totalYesterday = 0.0
        var totalCurrentMonth = 0.0
        var totalLastMonth = 0.0
        var totalCurrentYear = 0.0
        var totalLastYear = 0.0

        for (entry in entries) {
            workCal.time = entry.date
            val year = workCal.get(Calendar.YEAR)
            val dayOfYear = workCal.get(Calendar.DAY_OF_YEAR)
            val month = workCal.get(Calendar.MONTH)

            if (year == todayCal.get(Calendar.YEAR) && dayOfYear == todayCal.get(Calendar.DAY_OF_YEAR)) totalToday += entry.amount
            if (year == yesterdayCal.get(Calendar.YEAR) && dayOfYear == yesterdayCal.get(Calendar.DAY_OF_YEAR)) totalYesterday += entry.amount
            if (year == currentMonthCal.get(Calendar.YEAR) && month == currentMonthCal.get(Calendar.MONTH)) totalCurrentMonth += entry.amount
            if (year == lastMonthCal.get(Calendar.YEAR) && month == lastMonthCal.get(Calendar.MONTH)) totalLastMonth += entry.amount
            if (year == currentYearCal.get(Calendar.YEAR)) totalCurrentYear += entry.amount
            if (year == lastYearCal.get(Calendar.YEAR)) totalLastYear += entry.amount
        }
        _homePageSummary.value = HomePageSummary(
            totalToday = totalToday,
            totalYesterday = totalYesterday,
            totalCurrentMonth = totalCurrentMonth,
            totalLastMonth = totalLastMonth,
            totalCurrentYear = totalCurrentYear,
            totalLastYear = totalLastYear
        )
    }

    private fun calculateMonthlySummary(entries: List<Entry>) {
        viewModelScope.launch(Dispatchers.Default) {
            var income = 0.0
            var expense = 0.0
            for (e in entries) {
                if (e.amount > 0) income += e.amount else if (e.amount < 0) expense += e.amount
            }
            _monthlySummary.value = MonthlySummary(totalIncome = income, totalExpenses = expense)
        }
    }

    /**
     * Loads statistics based on filters.
     * @param filters Map of filters for statistics.
     */
    fun loadStatistics(filters: Map<String, Any>) {
        viewModelScope.launch {
            try {
                // Collect from the Flow returned by repository.getStatistics
                repository.getStatistics(filters).collect { stats ->
                    _statistics.value = stats
                    _message.value = null // Clear any previous messages on successful load
                }
            } catch (e: Exception) {
                _message.value = "Error loading statistics: ${e.message}"
                _statistics.value = null
            }
        }
    }

    /**
     * Resets the status message.
     */
    fun clearMessage() {
        _message.value = null
    }

    /**
     * Data class for homepage summary.
     */
    data class HomePageSummary(
        val totalToday: Double = 0.0,
        val totalYesterday: Double = 0.0,
        val totalCurrentMonth: Double = 0.0,
        val totalLastMonth: Double = 0.0,
        val totalCurrentYear: Double = 0.0,
        val totalLastYear: Double = 0.0
    )

    /**
     * Data class for monthly summary.
     */
    data class MonthlySummary(
        val totalIncome: Double = 0.0,
        val totalExpenses: Double = 0.0
    )
}

// Factory for the ViewModel (necessary to pass the repository)
class BudgetViewModelFactory(private val repository: BudgetRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BudgetViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BudgetViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
