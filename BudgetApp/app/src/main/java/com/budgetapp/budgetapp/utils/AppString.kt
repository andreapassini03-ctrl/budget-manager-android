package com.budgetapp.budgetapp.utils

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.Locale

/**
 * Classe per gestire le traduzioni e, ora, anche le categorie personalizzate persistenti.
 */
object AppString {
    val currentLanguage = mutableStateOf("it")

    // Persistenza categorie personalizzate (SharedPreferences) scoped per utente
    private const val PREFS_NAME = "budgetapp_prefs"
    private const val KEY_CUSTOM_CATEGORIES = "custom_categories_v1"
    private var lastLoadedUid: String? = null
    private var isLoaded = false
    val customCategories = mutableStateOf<List<String>>(emptyList())

    private fun prefsKeyForCurrentUser(): String {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
        return "${KEY_CUSTOM_CATEGORIES}_$uid"
    }

    private fun loadCustomCategories(context: Context) {
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (isLoaded && lastLoadedUid == currentUid) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = prefsKeyForCurrentUser()
        val raw = prefs.getString(key, "") ?: ""
        val list = if (raw.isBlank()) emptyList() else raw.split("\n").filter { it.isNotBlank() }
        customCategories.value = list
        lastLoadedUid = currentUid
        isLoaded = true

        // Prova a sincronizzare da Firestore (sovrascrive locale se presente)
        syncFromFirestore(context)
    }

    private fun saveCustomCategories(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = prefsKeyForCurrentUser()
        prefs.edit().putString(key, customCategories.value.joinToString("\n")).apply()
    }

    fun ensureCategoriesLoaded(context: Context) {
        loadCustomCategories(context)
    }

    private fun syncFromFirestore(context: Context) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(uid).collection("meta").document("settings").get()
            .addOnSuccessListener { snap ->
                val list = (snap.get("customCategories") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                if (list.isNotEmpty()) {
                    customCategories.value = list
                    saveCustomCategories(context)
                }
            }
            .addOnFailureListener { /* ignora: resterà la cache locale */ }
    }

    fun addCustomCategory(context: Context, name: String): Boolean {
        ensureCategoriesLoaded(context)
        val trimmed = name.trim()
        if (trimmed.isBlank()) return false
        // Evita duplicati rispetto a predefinite + personalizzate (case-insensitive)
        val exists = (categoryKeys + customCategories.value).any { it.equals(trimmed, ignoreCase = true) }
        if (exists) return false
        val updated = customCategories.value.toMutableList().apply { add(trimmed) }
        customCategories.value = updated
        saveCustomCategories(context)

        // Persisti su Firestore nell'utente corrente
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(uid).collection("meta").document("settings")
                .set(mapOf("customCategories" to FieldValue.arrayUnion(trimmed)), SetOptions.merge())
        }
        return true
    }

    fun removeCustomCategory(context: Context, name: String) {
        ensureCategoriesLoaded(context)
        val updated = customCategories.value.filterNot { it.equals(name, ignoreCase = true) }
        customCategories.value = updated
        saveCustomCategories(context)

        // Rimuovi da Firestore per l'utente corrente
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            val db = FirebaseFirestore.getInstance()
            db.collection("users").document(uid).collection("meta").document("settings")
                .set(mapOf("customCategories" to FieldValue.arrayRemove(name)), SetOptions.merge())
        }
    }

    fun allCategoryKeys(context: Context): List<String> {
        ensureCategoriesLoaded(context)
        return categoryKeys + customCategories.value
    }

    fun isCustomCategory(context: Context, key: String): Boolean {
        ensureCategoriesLoaded(context)
        return customCategories.value.any { it.equals(key, ignoreCase = true) }
    }

    // Helper per ottenere le stringhe correnti
    fun get(): LocalizedStrings {
        return strings[currentLanguage.value] ?: strings["en"]!!
    }

    // Chiavi interne stabili per logica/dati (predefinite)
    val transactionTypeKeys = listOf("all", "income", "expense")
    val paymentMethodKeys = listOf("cash", "card")
    val categoryKeys = listOf("food", "transport", "salary", "bills", "entertainment", "other")

    // Fornisce la Locale corrente in base alla lingua selezionata
    fun currentLocale(): Locale = when (currentLanguage.value) {
        "it" -> Locale.ITALY
        "en" -> Locale.US
        "es" -> Locale("es", "ES")
        "fr" -> Locale.FRANCE
        "pt" -> Locale("pt", "PT")
        else -> Locale.getDefault()
    }

    // Mappature etichette localizzate
    fun transactionTypeLabel(key: String): String = when (key) {
        "all" -> get().allOption
        "income" -> get().income
        "expense" -> get().expense
        else -> key
    }
    fun paymentMethodLabel(key: String): String = when (key) {
        "cash" -> get().paymentCash
        "card" -> get().paymentCard
        else -> key
    }
    fun categoryLabel(key: String): String = when (key) {
        "food" -> get().categoryFood
        "transport" -> get().categoryTransport
        "salary" -> get().categorySalary
        "bills" -> get().categoryBills
        "entertainment" -> get().categoryEntertainment
        "other" -> get().categoryOther
        else -> key // Per categorie personalizzate, usa il testo inserito dall'utente
    }

    val strings: Map<String, LocalizedStrings> = mapOf(
        "en" to LocalizedStrings(
            myWallet = "MyWallet",
            menu = "Menu",
            myAccount = "My Account",
            setUpLanguage = "Set up language",
            deleteAccount = "Delete Account",
            logout = "Logout",
            confirmSignOut = "Confirm Sign Out",
            areYouSureYouWantToSignOut = "Are you sure you want to sign out?",
            signOut = "Sign Out",
            cancel = "Cancel",
            overview = "Overview",
            today = "Today",
            yesterday = "Yesterday",
            currentMonth = "Current Month",
            lastMonth = "Last Month",
            currentYear = "Current Year",
            lastYear = "Last Year",
            monthInformation = "Month information",
            recentTransactions = "Recent Transactions",
            welcome = "Welcome,",
            addEntry = "Add New Entry",
            whatKindOfEntry = "What kind of entry do you want to add?",
            expense = "Expense",
            income = "Income",
            deleteAccountTitle = "Delete Account",
            deleteAccountMessage = "Are you sure you want to delete your account? This action is irreversible.",
            yes = "Yes",
            no = "No",
            appDescription = "MyWallet is your personal budget management application, designed to help you keep track of your finances with ease. Manage your income and expenses, view monthly summaries, analyze statistics, and stay in control of your money.",
            keyFeatures = "Key Features:",
            easyEntryTracking = "Easy Entry Tracking (Income/Expense)",
            monthlyAndYearlyOverviews = "Monthly and Yearly Overviews",
            interactiveCalendar = "Interactive Calendar",
            detailedLog = "Detailed Log of all your entries",
            powerfulStatistics = "Powerful Statistics with filtering options",
            secureLogin = "Secure Login",
            signInWithGoogle = "Sign in with Google",
            signInAsAnonymous = "Sign in as Anonymous",
            authenticating = "Authenticating...",
            transactions = "Transactions",
            edit = "Edit",
            delete = "Delete",
            totalBalance = "Total Balance",
            totalIncome = "Total Income",
            totalExpense = "Total Expense",
            noEntriesFound = "No entries found for this period.",
            selectFilterOptions = "Select filter options",
            amount = "Amount",
            paymentMethod = "Payment Method",
            type = "Type",
            category = "Category",
            statistics = "Statistics",
            monthlySummary = "Monthly Summary",
            back = "Back",
            sun = "Sun",
            mon = "Mon",
            tue = "Tue",
            wed = "Wed",
            thu = "Thu",
            fri = "Fri",
            sat = "Sat",
            filters = "Filters",
            transactionType = "Transaction Type",
            results = "Results",
            noStatisticsAvailable = "No statistics available.",
            noDataForSelectedFilters = "No data found for the selected filters.",
            unexpectedStatisticsFormat = "Unexpected statistics format.",
            allOption = "All",
            paymentCash = "Cash",
            paymentCard = "Card",
            categoryFood = "Food",
            categoryTransport = "Transport",
            categorySalary = "Salary",
            categoryBills = "Bills",
            categoryEntertainment = "Entertainment",
            categoryOther = "Other",
            // New localized labels
            description = "Description",
            selectDate = "Select Date",
            selectTime = "Select Time",
            selectedDate = "Selected Date",
            selectedTime = "Selected Time",
            confirm = "Confirm",
            ok = "OK",
            editEntryTitle = "Edit Entry",
            addIncomeTitle = "Add New Income",
            addExpenseTitle = "Add New Expense",
            addEntryTitle = "Add New Entry",
            amountHintIncome = "Amount (e.g., 50.00)",
            amountHintExpense = "Amount (e.g., 10.50)",
            amountHintGeneric = "Amount (e.g., -10.50 for expense, 50.00 for income)",
            errorDescriptionEmpty = "Description cannot be empty.",
            errorAmountInvalid = "Amount must be a valid number.",
            appVersionLabel = "App Version",
            // New for custom categories UI
            newCategory = "New category",
            categoryName = "Category name",
            // New for Privacy Policy
            privacyPolicy = "Privacy Policy",
            privacyPolicyContent = "We value your privacy. This app does not collect or share any personal information. Your data is stored locally on your device and is not accessible to us or any third parties.",
            termsAndConditions = "Terms and Conditions",
            accept = "Accept",
            decline = "Decline",
            lightMode = "Light Mode",
            darkMode = "Dark Mode",
            theme = "Theme",
            guestUser = "Guest User",
            // Onboarding
            skip = "Skip",
            next = "Next",
            getStarted = "Get Started",
            onboardingWelcomeTitle = "Welcome to MyWallet",
            onboardingWelcomeDesc = "Manage your finances effectively with MyWallet. Let's get started!",
            onboardingTrackTitle = "Track your expenses and income",
            onboardingTrackDesc = "Easily log and categorize your financial transactions.",
            onboardingAnalyticsTitle = "Analyze your financial data",
            onboardingAnalyticsDesc = "Gain insights into your spending habits and income sources.",
            onboardingSecureTitle = "Secure and private",
            onboardingSecureDesc = "Your data is stored securely on your device. We value your privacy.",
            homePage = "Home Page",
            tapForSummary = "Tap for summary"
        ),
        "it" to LocalizedStrings(
            myWallet = "Il mio portafoglio",
            menu = "Menu",
            myAccount = "Il mio account",
            setUpLanguage = "Imposta lingua",
            deleteAccount = "Elimina Account",
            logout = "Disconnetti",
            confirmSignOut = "Conferma disconnessione",
            areYouSureYouWantToSignOut = "Sei sicuro di voler uscire?",
            signOut = "Esci",
            cancel = "Annulla",
            overview = "Panoramica",
            today = "Oggi",
            yesterday = "Ieri",
            currentMonth = "Mese Corrente",
            lastMonth = "Mese scorso",
            currentYear = "Anno corrente",
            lastYear = "Anno scorso",
            monthInformation = "Informazioni del mese",
            recentTransactions = "Transazioni recenti",
            welcome = "Benvenuto,",
            addEntry = "Aggiungi nuova voce",
            whatKindOfEntry = "Che tipo di voce vuoi aggiungere?",
            expense = "Spesa",
            income = "Entrata",
            deleteAccountTitle = "Elimina Account",
            deleteAccountMessage = "Sei sicuro di voler eliminare il tuo account? Questa azione è irreversibile.",
            yes = "Sì",
            no = "No",
            appDescription = "MyWallet è la tua applicazione di gestione del budget personale, progettata per aiutarti a tenere traccia delle tue finanze con facilità. Gestisci le tue entrate e uscite, visualizza i riepiloghi mensili, analizza le statistiche e mantieni il controllo del tuo denaro.",
            keyFeatures = "Funzionalità principali:",
            easyEntryTracking = "Tracciamento Facile (Entrate/Uscite)",
            monthlyAndYearlyOverviews = "Riepiloghi Mensili e Annuali",
            interactiveCalendar = "Calendario Interattivo",
            detailedLog = "Log Dettagliato di tutte le tue voci",
            powerfulStatistics = "Statistiche Avanzate con opzioni di filtro",
            secureLogin = "Accesso Sicuro",
            signInWithGoogle = "Accedi con Google",
            signInAsAnonymous = "Accedi come Anonimo",
            authenticating = "Autenticazione in corso...",
            transactions = "Transazioni",
            edit = "Modifica",
            delete = "Elimina",
            totalBalance = "Saldo Totale",
            totalIncome = "Entrate Totali",
            totalExpense = "Uscite Totali",
            noEntriesFound = "Nessuna voce trovata per questo periodo.",
            selectFilterOptions = "Seleziona opzioni di filtro",
            amount = "Importo",
            paymentMethod = "Metodo di pagamento",
            type = "Tipologia",
            category = "Categoria",
            statistics = "Statistiche",
            monthlySummary = "Riepilogo Mensile",
            back = "Indietro",
            sun = "Dom",
            mon = "Lun",
            tue = "Mar",
            wed = "Mer",
            thu = "Gio",
            fri = "Ven",
            sat = "Sab",
            filters = "Filtri",
            transactionType = "Tipo di Transazione",
            results = "Risultati",
            noStatisticsAvailable = "Nessuna statistica disponibile.",
            noDataForSelectedFilters = "Nessun dato trovato per i filtri selezionati.",
            unexpectedStatisticsFormat = "Formato statistiche inatteso.",
            allOption = "Tutte",
            paymentCash = "Contanti",
            paymentCard = "Carta",
            categoryFood = "Cibo",
            categoryTransport = "Trasporti",
            categorySalary = "Stipendio",
            categoryBills = "Bollette",
            categoryEntertainment = "Intrattenimento",
            categoryOther = "Altro",
            description = "Descrizione",
            selectDate = "Seleziona data",
            selectTime = "Seleziona ora",
            selectedDate = "Data selezionata",
            selectedTime = "Ora selezionata",
            confirm = "Conferma",
            ok = "OK",
            editEntryTitle = "Modifica voce",
            addIncomeTitle = "Aggiungi entrata",
            addExpenseTitle = "Aggiungi spesa",
            addEntryTitle = "Aggiungi voce",
            amountHintIncome = "Importo (es. 50,00)",
            amountHintExpense = "Importo (es. 10,50)",
            amountHintGeneric = "Importo (es. -10,50 per spesa, 50,00 per entrata)",
            errorDescriptionEmpty = "La descrizione non può essere vuota.",
            errorAmountInvalid = "L'importo deve essere un numero valido.",
            appVersionLabel = "Versione app",
            newCategory = "Nuova categoria",
            categoryName = "Nome categoria",
            privacyPolicy = "Informativa sulla privacy",
            privacyPolicyContent = "Valorizziamo la tua privacy. Questa app non raccoglie né condivide alcuna informazione personale. I tuoi dati sono memorizzati localmente sul tuo dispositivo e non sono accessibili a noi o a terzi.",
            termsAndConditions = "Termini e Condizioni",
            accept = "Accetta",
            decline = "Declina",
            lightMode = "Modalità Chiara",
            darkMode = "Modalità Scura",
            theme = "Tema",
            guestUser = "Utente Ospite",
            skip = "Salta",
            next = "Avanti",
            getStarted = "Inizia",
            onboardingWelcomeTitle = "Benvenuto in MyWallet",
            onboardingWelcomeDesc = "Gestisci le tue finanze in modo efficace con MyWallet. Iniziamo!",
            onboardingTrackTitle = "Monitora le tue spese e entrate",
            onboardingTrackDesc = "Registra e classifica facilmente le tue transazioni finanziarie.",
            onboardingAnalyticsTitle = "Analizza i tuoi dati finanziari",
            onboardingAnalyticsDesc = "Ottieni informazioni sulle tue abitudini di spesa e fonti di reddito.",
            onboardingSecureTitle = "Sicuro e privato",
            onboardingSecureDesc = "I tuoi dati sono memorizzati in modo sicuro sul tuo dispositivo. Valorizziamo la tua privacy.",
            homePage = "Home Page",
            tapForSummary = "Tocca per riepilogo"
        ),
        "es" to LocalizedStrings(
            myWallet = "Mi Billetera",
            menu = "Menú",
            myAccount = "Mi Cuenta",
            setUpLanguage = "Configurar idioma",
            deleteAccount = "Eliminar Cuenta",
            logout = "Cerrar sesión",
            confirmSignOut = "Confirmar cierre de sesión",
            areYouSureYouWantToSignOut = "¿Estás seguro de que quieres cerrar sesión?",
            signOut = "Cerrar sesión",
            cancel = "Cancelar",
            overview = "Resumen",
            today = "Hoy",
            yesterday = "Ayer",
            currentMonth = "Mes actual",
            lastMonth = "Mes pasado",
            currentYear = "Año actual",
            lastYear = "Año pasado",
            monthInformation = "Información del mes",
            recentTransactions = "Transacciones recientes",
            welcome = "Bienvenido,",
            addEntry = "Añadir nueva entrada",
            whatKindOfEntry = "¿Qué tipo de entrada quieres añadir?",
            expense = "Gasto",
            income = "Ingreso",
            deleteAccountTitle = "Eliminar Cuenta",
            deleteAccountMessage = "¿Estás seguro de que quieres eliminar tu cuenta? Esta acción es irreversible.",
            yes = "Sí",
            no = "No",
            appDescription = "MyWallet es su aplicación personal de gestión de presupuesto, diseñada para ayudarle a realizar un seguimiento de sus finanzas con facilidad. Gestione sus ingresos y gastos, vea resúmenes mensuales, analice estadísticas y mantenga el control de su dinero.",
            keyFeatures = "Características clave:",
            easyEntryTracking = "Seguimiento de Entradas Fácil (Ingresos/Gastos)",
            monthlyAndYearlyOverviews = "Resúmenes Mensuales y Anuales",
            interactiveCalendar = "Calendario Interactivo",
            detailedLog = "Registro Detallado de todas sus entradas",
            powerfulStatistics = "Estadísticas Potentes con opciones de filtro",
            secureLogin = "Inicio de Sesión Seguro",
            signInWithGoogle = "Iniciar sesión con Google",
            signInAsAnonymous = "Iniciar sesión como anónimo",
            authenticating = "Autenticando...",
            transactions = "Transacciones",
            edit = "Editar",
            delete = "Eliminar",
            totalBalance = "Saldo Total",
            totalIncome = "Ingresos Totales",
            totalExpense = "Gastos Totales",
            noEntriesFound = "No se encontraron entradas para este período.",
            selectFilterOptions = "Seleccionar opciones de filtro",
            amount = "Importe",
            paymentMethod = "Método de pago",
            type = "Tipo",
            category = "Categoría",
            statistics = "Estadísticas",
            monthlySummary = "Resumen Mensual",
            back = "Atrás",
            sun = "Dom",
            mon = "Lun",
            tue = "Mar",
            wed = "Mié",
            thu = "Jue",
            fri = "Vie",
            sat = "Sáb",
            filters = "Filtros",
            transactionType = "Tipo de Transacción",
            results = "Resultados",
            noStatisticsAvailable = "No hay estadísticas disponibles.",
            noDataForSelectedFilters = "No se encontraron datos para los filtros seleccionados.",
            unexpectedStatisticsFormat = "Formato de estadísticas inesperado.",
            allOption = "Todas",
            paymentCash = "Efectivo",
            paymentCard = "Tarjeta",
            categoryFood = "Comida",
            categoryTransport = "Transporte",
            categorySalary = "Salario",
            categoryBills = "Facturas",
            categoryEntertainment = "Entretenimiento",
            categoryOther = "Otro",
            description = "Descripción",
            selectDate = "Seleccionar fecha",
            selectTime = "Seleccionar hora",
            selectedDate = "Fecha seleccionada",
            selectedTime = "Hora seleccionada",
            confirm = "Confirmar",
            ok = "OK",
            editEntryTitle = "Editar entrada",
            addIncomeTitle = "Añadir ingreso",
            addExpenseTitle = "Añadir gasto",
            addEntryTitle = "Añadir entrada",
            amountHintIncome = "Importe (p. ej., 50,00)",
            amountHintExpense = "Importe (p. ej., 10,50)",
            amountHintGeneric = "Importe (p. ej., -10,50 gasto, 50,00 ingreso)",
            errorDescriptionEmpty = "La descripción no puede estar vacía.",
            errorAmountInvalid = "El importe debe ser un número válido.",
            appVersionLabel = "Versión de la app",
            newCategory = "Nueva categoría",
            categoryName = "Nombre de la categoría",
            privacyPolicy = "Política de privacidad",
            privacyPolicyContent = "Valoramos su privacidad. Esta aplicación no recopila ni comparte ninguna información personal. Sus datos se almacenan localmente en su dispositivo y no son accesibles para nosotros ni para terceros.",
            termsAndConditions = "Términos y Condiciones",
            accept = "Aceptar",
            decline = "Rechazar",
            lightMode = "Modo Claro",
            darkMode = "Modo Oscuro",
            theme = "Tema",
            guestUser = "Usuario Invitado",
            skip = "Saltar",
            next = "Siguiente",
            getStarted = "Empezar",
            onboardingWelcomeTitle = "Bienvenido a MyWallet",
            onboardingWelcomeDesc = "Administra tus finanzas de manera efectiva con MyWallet. ¡Comencemos!",
            onboardingTrackTitle = "Rastrea tus gastos e ingresos",
            onboardingTrackDesc = "Registra y clasifica fácilmente tus transacciones financieras.",
            onboardingAnalyticsTitle = "Analiza tus datos financieros",
            onboardingAnalyticsDesc = "Obtén información sobre tus hábitos de gasto y fuentes de ingresos.",
            onboardingSecureTitle = "Seguro y privado",
            onboardingSecureDesc = "Tus datos se almacenan de forma segura en tu dispositivo. Valoramos tu privacidad.",
            homePage = "Página Principal",
            tapForSummary = "Toca para resumen"
        ),
        "fr" to LocalizedStrings(
            myWallet = "Mon Portefeuille",
            menu = "Menu",
            myAccount = "Mon Compte",
            setUpLanguage = "Configurer la langue",
            deleteAccount = "Supprimer le compte",
            logout = "Se déconnecter",
            confirmSignOut = "Confirmer la déconnexion",
            areYouSureYouWantToSignOut = "Êtes-vous sûr de vouloir vous déconnecter?",
            signOut = "Se déconnecter",
            cancel = "Annuler",
            overview = "Aperçu",
            today = "Aujourd'hui",
            yesterday = "Hier",
            currentMonth = "Mois en cours",
            lastMonth = "Mois dernier",
            currentYear = "Année en cours",
            lastYear = "Année dernière",
            monthInformation = "Informations du mois",
            recentTransactions = "Transactions récentes",
            welcome = "Bienvenue,",
            addEntry = "Ajouter une nouvelle entrée",
            whatKindOfEntry = "Quel type d'entrée souhaitez-vous ajouter?",
            expense = "Dépense",
            income = "Revenu",
            deleteAccountTitle = "Supprimer le compte",
            deleteAccountMessage = "Êtes-vous sûr de vouloir supprimer votre compte? Cette action est irréversible.",
            yes = "Oui",
            no = "Non",
            appDescription = "MyWallet est votre application personnelle de gestion de budget, conçue pour vous aider à suivre facilement vos finances. Gérez vos revenus et dépenses, visualisez des résumés mensuels, analysez des statistiques et gardez le contrôle de votre argent.",
            keyFeatures = "Fonctionnalités clés:",
            easyEntryTracking = "Suivi Facile des Entrées (Revenu/Dépense)",
            monthlyAndYearlyOverviews = "Aperçus Mensuels et Annuels",
            interactiveCalendar = "Calendrier Interactif",
            detailedLog = "Journal Détaillé de toutes vos entrées",
            powerfulStatistics = "Statistiques Puissantes avec options de filtrage",
            secureLogin = "Connexion Sécurisée",
            signInWithGoogle = "Se connecter avec Google",
            signInAsAnonymous = "Se connecter de manière anonyme",
            authenticating = "Authentification en cours...",
            transactions = "Transactions",
            edit = "Modifier",
            delete = "Supprimer",
            totalBalance = "Solde total",
            totalIncome = "Revenu total",
            totalExpense = "Dépense totale",
            noEntriesFound = "Aucune entrée trouvée pour cette période.",
            selectFilterOptions = "Sélectionner les options de filtre",
            amount = "Montant",
            paymentMethod = "Méthode de paiement",
            type = "Type",
            category = "Catégorie",
            statistics = "Statistiques",
            monthlySummary = "Résumé mensuel",
            back = "Retour",
            sun = "Dim",
            mon = "Lun",
            tue = "Mar",
            wed = "Mer",
            thu = "Jeu",
            fri = "Ven",
            sat = "Sam",
            filters = "Filtres",
            transactionType = "Type de Transazione",
            results = "Résultats",
            noStatisticsAvailable = "Aucune statistique disponible.",
            noDataForSelectedFilters = "Aucune donnée trouvée pour les filtres sélectionnés.",
            unexpectedStatisticsFormat = "Format de statistiques inattendu.",
            allOption = "Toutes",
            paymentCash = "Espèces",
            paymentCard = "Carte",
            categoryFood = "Alimentation",
            categoryTransport = "Transport",
            categorySalary = "Salaire",
            categoryBills = "Factures",
            categoryEntertainment = "Divertissement",
            categoryOther = "Autre",
            description = "Description",
            selectDate = "Sélectionner la date",
            selectTime = "Sélectionner l'heure",
            selectedDate = "Date sélectionnée",
            selectedTime = "Heure sélectionnée",
            confirm = "Confirmer",
            ok = "OK",
            editEntryTitle = "Modifier l'entrée",
            addIncomeTitle = "Ajouter un revenu",
            addExpenseTitle = "Ajouter une dépense",
            addEntryTitle = "Ajouter une entrée",
            amountHintIncome = "Montant (ex. 50,00)",
            amountHintExpense = "Montant (ex. 10,50)",
            amountHintGeneric = "Montant (ex. -10,50 dépense, 50,00 revenu)",
            errorDescriptionEmpty = "La description ne peut pas être vide.",
            errorAmountInvalid = "Le montant doit être un nombre valide.",
            appVersionLabel = "Version de l'app",
            newCategory = "Nouvelle catégorie",
            categoryName = "Nom de la catégorie",
            privacyPolicy = "Politique de confidentialité",
            privacyPolicyContent = "Nous attachons une grande importance à votre vie privée. Cette application ne collecte ni ne partage aucune information personnelle. Vos données sont stockées localement sur votre appareil et ne sont pas accessibles par nos soins ni par des tiers.",
            termsAndConditions = "Conditions générales",
            accept = "Accepter",
            decline = "Décliner",
            lightMode = "Mode Clair",
            darkMode = "Mode Sombre",
            theme = "Thème",
            guestUser = "Utilisateur Invité",
            skip = "Passer",
            next = "Suivant",
            getStarted = "Commencer",
            onboardingWelcomeTitle = "Bienvenue dans MyWallet",
            onboardingWelcomeDesc = "Gérez vos finances efficacement avec MyWallet. Commençons!",
            onboardingTrackTitle = "Suivez vos dépenses et revenus",
            onboardingTrackDesc = "Enregistrez et classifiez facilement vos transactions financières.",
            onboardingAnalyticsTitle = "Analysez vos données financières",
            onboardingAnalyticsDesc = "Obtenez des informations sur vos habitudes de dépenses et vos sources de revenus.",
            onboardingSecureTitle = "Sécurisé et privé",
            onboardingSecureDesc = "Vos données sont stockées en toute sécurité sur votre appareil. Nous attachons une grande importance à votre vie privée.",
            homePage = "Page d'accueil",
            tapForSummary = "Appuyez pour le résumé"
        ),
        "pt" to LocalizedStrings(
            myWallet = "Minha Carteira",
            menu = "Menu",
            myAccount = "Minha Conta",
            setUpLanguage = "Definir idioma",
            deleteAccount = "Eliminar Conta",
            logout = "Terminar sessão",
            confirmSignOut = "Confirmar término de sessão",
            areYouSureYouWantToSignOut = "Tem a certeza de que quer terminar sessão?",
            signOut = "Terminar sessão",
            cancel = "Cancelar",
            overview = "Visão geral",
            today = "Hoje",
            yesterday = "Ontem",
            currentMonth = "Mês atual",
            lastMonth = "Mês passado",
            currentYear = "Ano atual",
            lastYear = "Ano passado",
            monthInformation = "Informações do mês",
            recentTransactions = "Transações recentes",
            welcome = "Bem-vindo,",
            addEntry = "Adicionar nova entrada",
            whatKindOfEntry = "Que tipo de entrada pretende adicionar?",
            expense = "Despesa",
            income = "Receita",
            deleteAccountTitle = "Eliminar Conta",
            deleteAccountMessage = "Tem a certeza de que quer eliminar a sua conta? Esta ação é irreversível.",
            yes = "Sim",
            no = "Não",
            appDescription = "O MyWallet é a sua aplicação de gestão de orçamento pessoal, concebida para o ajudar a controlar as suas finanças com facilidade. Gere receitas e despesas, vê resumos mensais, analisa estatísticas e mantém o controlo do seu dinheiro.",
            keyFeatures = "Funcionalidades principais:",
            easyEntryTracking = "Registo fácil de entradas (Receita/Despesa)",
            monthlyAndYearlyOverviews = "Resumos mensais e anuais",
            interactiveCalendar = "Calendário interativo",
            detailedLog = "Registo detalhado de todas as suas entradas",
            powerfulStatistics = "Estatísticas avançadas com opções de filtro",
            secureLogin = "Início de sessão seguro",
            signInWithGoogle = "Entrar com o Google",
            signInAsAnonymous = "Entrar como Anónimo",
            authenticating = "A autenticar...",
            transactions = "Transações",
            edit = "Editar",
            delete = "Eliminar",
            totalBalance = "Saldo total",
            totalIncome = "Receitas totais",
            totalExpense = "Despesas totais",
            noEntriesFound = "Nenhuma entrada encontrada para este período.",
            selectFilterOptions = "Selecionar opções de filtro",
            amount = "Montante",
            paymentMethod = "Método de pagamento",
            type = "Tipo",
            category = "Categoria",
            statistics = "Estatísticas",
            monthlySummary = "Resumo mensal",
            back = "Voltar",
            sun = "Dom",
            mon = "Seg",
            tue = "Ter",
            wed = "Qua",
            thu = "Qui",
            fri = "Sex",
            sat = "Sáb",
            filters = "Filtros",
            transactionType = "Tipo de transação",
            results = "Resultados",
            noStatisticsAvailable = "Sem estatísticas disponíveis.",
            noDataForSelectedFilters = "Não foram encontrados dados para os filtros selecionados.",
            unexpectedStatisticsFormat = "Formato de estatísticas inesperado.",
            allOption = "Todas",
            paymentCash = "Dinheiro",
            paymentCard = "Cartão",
            categoryFood = "Alimentação",
            categoryTransport = "Transportes",
            categorySalary = "Salário",
            categoryBills = "Faturas",
            categoryEntertainment = "Entretenimento",
            categoryOther = "Outro",
            description = "Descrição",
            selectDate = "Selecionar data",
            selectTime = "Selecionar hora",
            selectedDate = "Data selecionada",
            selectedTime = "Hora selecionada",
            confirm = "Confirmar",
            ok = "OK",
            editEntryTitle = "Editar entrada",
            addIncomeTitle = "Adicionar receita",
            addExpenseTitle = "Adicionar despesa",
            addEntryTitle = "Adicionar entrada",
            amountHintIncome = "Montante (ex.: 50,00)",
            amountHintExpense = "Montante (ex.: 10,50)",
            amountHintGeneric = "Montante (ex.: -10,50 despesa, 50,00 receita)",
            errorDescriptionEmpty = "A descrição não pode estar vazia.",
            errorAmountInvalid = "O montante deve ser um número válido.",
            appVersionLabel = "Versão da app",
            newCategory = "Nova categoria",
            categoryName = "Nome da categoria",
            privacyPolicy = "Política de privacidade",
            privacyPolicyContent = "Valorizamos a sua privacidade. Esta aplicação não recolhe nem partilha qualquer informação pessoal. Os seus dados são armazenados localmente no seu dispositivo e não são acessíveis por nós ou por terceiros.",
            termsAndConditions = "Termos e Condições",
            accept = "Aceitar",
            decline = "Recusar",
            lightMode = "Modo Claro",
            darkMode = "Modo Escuro",
            theme = "Tema",
            guestUser = "Usuário Convidado",
            skip = "Ignorar",
            next = "Próximo",
            getStarted = "Começar",
            onboardingWelcomeTitle = "Bem-vindo ao MyWallet",
            onboardingWelcomeDesc = "Gerencie suas finanças de forma eficaz com o MyWallet. Vamos começar!",
            onboardingTrackTitle = "Acompanhe suas despesas e receitas",
            onboardingTrackDesc = "Registre e classifique facilmente suas transações financeiras.",
            onboardingAnalyticsTitle = "Analise seus dados financeiros",
            onboardingAnalyticsDesc = "Obtenha insights sobre seus hábitos de consumo e fontes de renda.",
            onboardingSecureTitle = "Seguro e privado",
            onboardingSecureDesc = "Seus dados são armazenados com segurança em seu dispositivo. Valorizamos sua privacidade.",
            homePage = "Página Inicial",
            tapForSummary = "Toque para resumo"
        )
    )
}

// Aggiornata la data class con nuove etichette localizzate

data class LocalizedStrings(
    val myWallet: String,
    val menu: String,
    val myAccount: String,
    val setUpLanguage: String,
    val deleteAccount: String,
    val logout: String,
    val confirmSignOut: String,
    val areYouSureYouWantToSignOut: String,
    val signOut: String,
    val cancel: String,
    val overview: String,
    val today: String,
    val yesterday: String,
    val currentMonth: String,
    val lastMonth: String,
    val currentYear: String,
    val lastYear: String,
    val monthInformation: String,
    val recentTransactions: String,
    val welcome: String,
    val addEntry: String,
    val whatKindOfEntry: String,
    val expense: String,
    val income: String,
    val deleteAccountTitle: String,
    val deleteAccountMessage: String,
    val yes: String,
    val no: String,
    val appDescription: String,
    val keyFeatures: String,
    val easyEntryTracking: String,
    val monthlyAndYearlyOverviews: String,
    val interactiveCalendar: String,
    val detailedLog: String,
    val powerfulStatistics: String,
    val secureLogin: String,
    val signInWithGoogle: String,
    val signInAsAnonymous: String,
    val authenticating: String,
    val transactions: String,
    val edit: String,
    val delete: String,
    val totalBalance: String,
    val totalIncome: String,
    val totalExpense: String,
    val noEntriesFound: String,
    val selectFilterOptions: String,
    val amount: String,
    val paymentMethod: String,
    val type: String,
    val category: String,
    val statistics: String,
    val monthlySummary: String,
    val back: String,
    val sun: String,
    val mon: String,
    val tue: String,
    val wed: String,
    val thu: String,
    val fri: String,
    val sat: String,
    val filters: String,
    val transactionType: String,
    val results: String,
    val noStatisticsAvailable: String,
    val noDataForSelectedFilters: String,
    val unexpectedStatisticsFormat: String,
    // Nuove etichette localizzate
    val allOption: String,
    val paymentCash: String,
    val paymentCard: String,
    val categoryFood: String,
    val categoryTransport: String,
    val categorySalary: String,
    val categoryBills: String,
    val categoryEntertainment: String,
    val categoryOther: String,
    // Nuove etichette
    val description: String,
    val selectDate: String,
    val selectTime: String,
    val selectedDate: String,
    val selectedTime: String,
    val confirm: String,
    val ok: String,
    val editEntryTitle: String,
    val addIncomeTitle: String,
    val addExpenseTitle: String,
    val addEntryTitle: String,
    val amountHintIncome: String,
    val amountHintExpense: String,
    val amountHintGeneric: String,
    val errorDescriptionEmpty: String,
    val errorAmountInvalid: String,
    val appVersionLabel: String,
    // Nuovi campi per UI categorie personalizzate
    val newCategory: String,
    val categoryName: String,
    // Nuovi campi per Privacy Policy
    val privacyPolicy: String,
    val privacyPolicyContent: String,
    val termsAndConditions: String,
    val accept: String,
    val decline: String,
    val lightMode: String,
    val darkMode: String,
    val theme: String,
    val guestUser: String, // Nuova stringa per utente guest
    // Onboarding
    val skip: String,
    val next: String,
    val getStarted: String,
    val onboardingWelcomeTitle: String,
    val onboardingWelcomeDesc: String,
    val onboardingTrackTitle: String,
    val onboardingTrackDesc: String,
    val onboardingAnalyticsTitle: String,
    val onboardingAnalyticsDesc: String,
    val onboardingSecureTitle: String,
    val onboardingSecureDesc: String,
    val homePage: String,
    val tapForSummary: String
)
