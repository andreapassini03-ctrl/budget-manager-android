// File: BudgetRepository.kt
package com.budgetapp.budgetapp.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.budgetapp.budgetapp.data.Entry
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Calendar // Import Calendar
import java.util.Date

/**
 * Repository for budget data and authentication management.
 * Interacts with Firebase Authentication and Firestore.
 */
class BudgetRepository(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {

    private val TAG = "UserRepository"

    // --- Authentication related properties and methods ---
    private val _currentUser = MutableStateFlow(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()
    private val _authReady = MutableStateFlow(false)
    val authReady: StateFlow<Boolean> = _authReady.asStateFlow()

    init {
        // Listener for Firebase authentication state changes.
        // This ensures _currentUser is always updated.
        auth.addAuthStateListener { auth ->
            val user = auth.currentUser
            _currentUser.value = user
            if (!_authReady.value) _authReady.value = true
            Log.d(TAG, "AuthState changed. Current user: ${user?.email ?: "null"}")
        }
    }

    /**
     * Attempts to sign in to Firebase with a Google credential.
     * This function is called after FirebaseUI obtains a Google credential.
     *
     * @param credential The authentication credential obtained from Google.
     * @return true if sign-in is successful, false otherwise.
     */
    suspend fun signInWithGoogleCredential(credential: AuthCredential): Boolean {
        return try {
            Log.d(TAG, "Attempting to sign in with Google credential.")
            auth.signInWithCredential(credential).await()
            // Ensure user document exists after successful sign-in
            ensureUserDocumentExists()
            Log.d(TAG, "Sign in with Google credential successful.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Sign in with Google credential failed: ${e.message}", e)
            false
        }
    }

    /**
     * Attempts to sign in anonymously to Firebase.
     * Useful for "guest" mode.
     *
     * @return true if sign-in is successful, false otherwise.
     */
    suspend fun signInAnonymously(): Boolean {
        return try {
            Log.d(TAG, "Attempting anonymous sign in.")
            auth.signInAnonymously().await()
            // Ensure user document exists after successful sign-in
            ensureUserDocumentExists()
            Log.d(TAG, "Anonymous sign in successful.")
            _currentUser.value = auth.currentUser // Update the flow
            true
        } catch (e: Exception) {
            Log.e(TAG, "Anonymous sign in failed: ${e.message}", e)
            false
        }
    }

    /**
     * Returns the UID of the currently logged-in Firebase user.
     *
     * @return The UID of the current user, or null if no user is logged in.
     */
    fun getUserId(): String? {
        return auth.currentUser?.uid
    }

    /**
     * Signs out the current user from Firebase.
     */
    fun signOut() {
        Log.d(TAG, "Attempting to sign out.")
        auth.signOut()
        Log.d(TAG, "Sign out successful. Current user is now null.")
        // The authStateChanged listener will automatically update _currentUser.value to null
    }

    /**
     *  NEW METHOD FOR ACCOUNT DELETION
     */
    suspend fun deleteUserAccount(): Boolean {
        return try {
            val user = auth.currentUser
            val userId = user?.uid ?: return false

            // Step 1: Delete user data from Firestore
            // Delete entries data
            firestore.collection("users").document(userId).collection("entries").get().await()
                .documents.forEach { it.reference.delete().await() }

            // Delete Account data (profile doc)
            firestore.collection("users").document(userId).delete().await()

            // Step 2: Delete user from Firebase Authentication
            user.delete().await()

            // Lo stato verrà aggiornato automaticamente dal AuthStateListener
            Log.d(TAG, "User account and data deleted successfully.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting user account: ${e.message}", e)
            false
        }
    }

    // Ensures the parent user document exists, helpful if rules expect it
    private suspend fun ensureUserDocumentExists() {
        val uid = auth.currentUser?.uid ?: return
        try {
            val docRef = firestore.collection("users").document(uid)
            // Merge to avoid overwriting if it already exists
            docRef.set(
                mapOf(
                    "uid" to uid,
                    "provider" to (auth.currentUser?.providerData?.firstOrNull()?.providerId ?: "unknown"),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "exists" to true
                ),
                SetOptions.merge()
            ).await()
        } catch (e: Exception) {
            Log.w(TAG, "ensureUserDocumentExists failed: ${e.message}", e)
        }
    }

    /**
     * Adds a new entry (transaction/recharge) to the database.
     * @param entry The Entry object to add.
     */
    suspend fun addEntry(entry: Entry) {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
        // Ensure user profile doc exists before writing entries (in case of strict rules)
        ensureUserDocumentExists()
        val docRef = firestore.collection("users").document(userId)
            .collection("entries").add(entry).await()
        // Salva anche l'id nel documento per coerenza
        firestore.collection("users").document(userId)
            .collection("entries").document(docRef.id)
            .update("id", docRef.id)
            .await()
        Log.d(TAG, "Entry added with ID: ${docRef.id}")
    }

    /**
     * Updates an existing entry in the database.
     * @param entry The updated Entry object.
     */
    suspend fun updateEntry(entry: Entry) {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
        firestore.collection("users").document(userId)
            .collection("entries").document(entry.id).set(entry).await()
        Log.d(TAG, "Entry updated with ID: ${entry.id}")
    }

    /**
     * Deletes an entry from the database.
     * @param entryId The ID of the entry to delete.
     */
    suspend fun deleteEntry(entryId: String) {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
        firestore.collection("users").document(userId)
            .collection("entries").document(entryId).delete().await()
        Log.d(TAG, "Entry deleted with ID: $entryId")
    }

    /**
     * Retrieves all entries for a given day.
     * @param date The day for which to retrieve entries.
     * @return A list of Entry for the specified day.
     */
    suspend fun getEntriesPerDay(date: Date): List<Entry> {
        val userId = auth.currentUser?.uid ?: return emptyList()
        val startOfDay = getStartOfDay(date)
        val endOfDay = getEndOfDay(date)

        // Query Firestore for entries within the specified day
        return firestore.collection("users").document(userId)
            .collection("entries")
            .whereGreaterThanOrEqualTo("date", startOfDay)
            .whereLessThanOrEqualTo("date", endOfDay)
            .get()
            .await()
            .documents
            .mapNotNull { doc ->
                doc.toObject(Entry::class.java)?.copy(id = doc.id)
            }
    }

    /**
     * Retrieves all user entries.
     * @return A Flow of lists of Entry that updates in real time.
     */
    fun getAllEntries(): Flow<List<Entry>> {
        val userId = auth.currentUser?.uid ?: return MutableStateFlow(emptyList())
        val entriesCollection = firestore.collection("users").document(userId).collection("entries")
        // Use callbackFlow to properly register and remove the listener when collected/cancelled
        return callbackFlow {
            val registration: ListenerRegistration = entriesCollection.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Error listening for entries: ${e.message}", e)
                    trySend(emptyList()).isSuccess
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val entries = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Entry::class.java)?.copy(id = doc.id)
                    }
                    Log.d(TAG, "Entries snapshot size: ${entries.size}")
                    trySend(entries).isSuccess
                }
            }
            awaitClose { registration.remove() }
        }
    }

    /**
     * Retrieves all entries for a given month and year.
     * @param month The month for which to retrieve entries (0-11).
     * @param year The year for which to retrieve entries.
     * @return A Flow of lists of Entry for the specified month and year.
     */
    fun getMonthlyEntries(month: Int, year: Int): Flow<List<Entry>> {
        val userId = auth.currentUser?.uid ?: return MutableStateFlow(emptyList())
        // Riutilizza il flusso realtime e filtra per mese/anno
        return getAllEntries().map { allEntries ->
            allEntries.filter { entry ->
                val cal = Calendar.getInstance().apply { time = entry.date }
                cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month
            }
        }
    }

    /**
     * Performs statistical calculations on entries based on filters.
     * @param filters Map of filters for statistics (e.g., "amount", "paymentMethod", "category", "minAmount", "maxAmount", "transactionType").
     * @return A Flow<Map<String, Double>> containing aggregated statistics (e.g., total by category).
     */
    fun getStatistics(filters: Map<String, Any>): Flow<Map<String, Double>> {
        val userId = auth.currentUser?.uid ?: return MutableStateFlow(emptyMap())
        val entriesCollection = firestore.collection("users").document(userId).collection("entries")

        // Use callbackFlow for proper subscription lifecycle
        return callbackFlow {
            val registration: ListenerRegistration = entriesCollection.addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Error listening for statistics: ${e.message}", e)
                    trySend(emptyMap()).isSuccess
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val allEntries = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Entry::class.java)?.copy(id = doc.id)
                    }

                    val filteredEntries = allEntries.filter { entry ->
                        var matches = true

                        // Filter by transaction type (income/expense)
                        val transactionTypeFilter = filters["transactionType"] as? String
                        if (!transactionTypeFilter.isNullOrBlank() && transactionTypeFilter != "all") {
                            matches = when (transactionTypeFilter) {
                                "income" -> matches && entry.amount >= 0
                                "expense" -> matches && entry.amount < 0
                                else -> matches
                            }
                        }

                        // Filter by payment method (keys: cash/card)
                        val paymentMethodFilter = filters["paymentMethod"] as? String
                        if (!paymentMethodFilter.isNullOrBlank() && paymentMethodFilter != "all") {
                            matches = matches && entry.paymentMethod == paymentMethodFilter
                        }

                        // Filter by category (keys like food/transport/...)
                        val categoryFilter = filters["category"] as? String
                        if (!categoryFilter.isNullOrBlank() && categoryFilter != "all") {
                            matches = matches && entry.category == categoryFilter
                        }

                        // Filter by min amount
                        val minAmountFilter = (filters["minAmount"] as? Double)
                        if (minAmountFilter != null) {
                            matches = matches && entry.amount >= minAmountFilter
                        }

                        // Filter by max amount
                        val maxAmountFilter = (filters["maxAmount"] as? Double)
                        if (maxAmountFilter != null) {
                            matches = matches && entry.amount <= maxAmountFilter
                        }

                        matches
                    }

                    // Aggregate totals by category
                    val totalByCategory = filteredEntries.groupBy { it.category }
                        .mapValues { (_, entries) -> entries.sumOf { it.amount } }

                    trySend(totalByCategory).isSuccess
                }
            }
            awaitClose { registration.remove() }
        }
    }

    // Helpers
    private fun getStartOfDay(date: Date): Date {
        val cal = Calendar.getInstance().apply { time = date }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.time
    }

    private fun getEndOfDay(date: Date): Date {
        val cal = Calendar.getInstance().apply { time = date }
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.time
    }
}

