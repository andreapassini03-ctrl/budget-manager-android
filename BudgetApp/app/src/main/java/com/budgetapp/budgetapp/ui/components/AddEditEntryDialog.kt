package com.budgetapp.budgetapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TimeInput
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.budgetapp.budgetapp.data.Entry
import com.budgetapp.budgetapp.utils.AppString
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete

/**
 * Dialogo per aggiungere o modificare una Entry.
 * Include campi per descrizione, importo, metodo di pagamento, categoria e data/ora.
 * I selettori di data e ora sono integrati direttamente nel dialogo.
 *
 * @param entry L'Entry da modificare (null se si sta aggiungendo una nuova entry).
 * @param onDismiss Callback chiamato quando il dialogo viene chiuso.
 * @param onConfirm Callback chiamato quando l'entry viene confermata.
 * @param isIncome Booleano opzionale che indica se si sta aggiungendo una ricarica (true) o una spesa (false).
 * Null se si sta modificando un'entry esistente o se non è stato specificato.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditEntryDialog(
    entry: Entry?,
    onDismiss: () -> Unit,
    onConfirm: (Entry) -> Unit,
    isIncome: Boolean? = null,
    selectedDateTime: Date? = null // Nuovo parametro opzionale
) {
    // Context per persistenza categorie custom
    val context = LocalContext.current
    LaunchedEffect(Unit) { AppString.ensureCategoriesLoaded(context) }

    var description by remember { mutableStateOf(entry?.description ?: "") }
    // Inizializza l'importo: se è una nuova entry e isIncome è specificato, preimposta a ""
    // altrimenti usa l'importo esistente o ""
    var amount by remember {
        mutableStateOf(
            entry?.amount?.toString() ?: if (isIncome != null) "" else ""
        )
    }
    var paymentMethod by remember { mutableStateOf(entry?.paymentMethod ?: "cash") }
    var category by remember { mutableStateOf(entry?.category ?: "food") }
    // Inizializza la data con selectedDateTime se fornita, altrimenti usa entry?.date o la data corrente
    var selectedDate by remember { mutableStateOf(selectedDateTime ?: entry?.date ?: Date()) }

    // Stati per controllare la visibilit�� dei selettori di data e ora come dialoghi
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }

    // Stati per la validazione dei campi
    var descriptionError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }


    val initialDateMillis = Calendar.getInstance().apply { time = selectedDate }.timeInMillis
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDateMillis)

    val initialHour = Calendar.getInstance().apply { time = selectedDate }.get(Calendar.HOUR_OF_DAY)
    val initialMinute = Calendar.getInstance().apply { time = selectedDate }.get(Calendar.MINUTE)
    val timePickerState = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)

    // Sorgente label localizzate
    val strings = AppString.get()
    val currentLocale = AppString.currentLocale()

    // Liste chiave per i menu (manteniamo le chiavi interne)
    val paymentMethods = AppString.paymentMethodKeys
    val categories = AppString.categoryKeys

    // Dialog stato per nuova categoria
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var newCategoryError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    entry != null -> strings.editEntryTitle
                    isIncome == true -> strings.addIncomeTitle
                    isIncome == false -> strings.addExpenseTitle
                    else -> strings.addEntryTitle
                },
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = {
                        description = it
                        descriptionError = false
                        errorMessage = ""
                    },
                    label = { Text(strings.description) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = descriptionError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        errorBorderColor = MaterialTheme.colorScheme.error,
                        errorLabelColor = MaterialTheme.colorScheme.error,
                        errorTextColor = MaterialTheme.colorScheme.error
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { newValue ->
                        // Permetti numeri con punto o virgola come separatore decimale e un segno meno all'inizio
                        if (newValue.matches(Regex("^-?\\d*[\\.,]?\\d*$"))) {
                            amount = newValue
                            amountError = false
                            errorMessage = ""
                        }
                    },
                    label = {
                        Text(
                            when {
                                isIncome == true -> strings.amountHintIncome
                                isIncome == false -> strings.amountHintExpense
                                else -> strings.amountHintGeneric
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = amountError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        errorBorderColor = MaterialTheme.colorScheme.error,
                        errorLabelColor = MaterialTheme.colorScheme.error,
                        errorTextColor = MaterialTheme.colorScheme.error
                    )
                )
                // Messaggio di errore generale
                if (errorMessage.isNotBlank()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Display selected date and time
                val dateFormatter = SimpleDateFormat("dd/MM/yyyy", currentLocale)
                val timeFormatter = SimpleDateFormat("HH:mm", currentLocale)
                Text(
                    text = "${strings.selectedDate}: ${dateFormatter.format(selectedDate)}",
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${strings.selectedTime}: ${timeFormatter.format(selectedDate)}",
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Buttons to open DatePicker and TimeInput dialogs
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    if(selectedDateTime == null) {
                        SegmentedButton(
                            selected = false,
                            onClick = {
                                showDatePickerDialog = true
                            }, // Triggers the DatePickerDialog
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = MaterialTheme.colorScheme.primary,
                                activeContentColor = MaterialTheme.colorScheme.onPrimary,
                                inactiveContainerColor = MaterialTheme.colorScheme.surface,
                                inactiveContentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            shape = SegmentedButtonDefaults.baseShape
                        ) {
                            Text(strings.selectDate)
                        }
                    }
                    SegmentedButton(
                        selected = false,
                        onClick = { showTimePickerDialog = true }, // Triggers our custom TimePickerDialog2
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primary,
                            activeContentColor = MaterialTheme.colorScheme.onPrimary,
                            inactiveContainerColor = MaterialTheme.colorScheme.surface,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        shape = SegmentedButtonDefaults.baseShape
                    ) {
                        Text(strings.selectTime)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Dropdown per Metodo di Pagamento (label localizzata)
                var paymentMethodExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = paymentMethodExpanded,
                    onExpandedChange = { paymentMethodExpanded = !paymentMethodExpanded }
                ) {
                    TextField(
                        value = AppString.paymentMethodLabel(paymentMethod),
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
                            focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
                            unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
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
                        AppString.paymentMethodKeys.forEach { methodKey ->
                            DropdownMenuItem(
                                text = { Text(AppString.paymentMethodLabel(methodKey)) },
                                onClick = {
                                    paymentMethod = methodKey
                                    paymentMethodExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Dropdown per Categoria (supporta categorie personalizzate)
                var categoryExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    TextField(
                        value = AppString.categoryLabel(category),
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
                            focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
                            unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
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
                        // Azione: Nuova categoria
                        DropdownMenuItem(
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                            text = { Text(AppString.get().newCategory) },
                            onClick = {
                                // Chiudi il menu prima di mostrare il dialog
                                categoryExpanded = false
                                newCategoryName = ""
                                newCategoryError = false
                                showNewCategoryDialog = true
                            }
                        )
                        // Elenco categorie predefinite + personalizzate
                        val allCats = AppString.allCategoryKeys(context)
                        allCats.forEach { catKey ->
                            val isCustom = AppString.isCustomCategory(context, catKey)
                            DropdownMenuItem(
                                text = { Text(AppString.categoryLabel(catKey)) },
                                onClick = {
                                    category = catKey
                                    categoryExpanded = false
                                },
                                trailingIcon = if (isCustom) {
                                    {
                                        IconButton(onClick = {
                                            // Chiudi il menu prima di modificare la lista per evitare crash
                                            categoryExpanded = false
                                            AppString.removeCustomCategory(context, catKey)
                                        }) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                } else null
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedAmount = amount.replace(',', '.').toDoubleOrNull()
                    // Validazione dei campi
                    descriptionError = description.isBlank()
                    amountError = parsedAmount == null

                    if (descriptionError) {
                        errorMessage = strings.errorDescriptionEmpty
                    } else if (amountError) {
                        errorMessage = strings.errorAmountInvalid
                    } else {
                        // Applica il segno all'importo in base a isIncome, se specificato
                        val finalAmount = when {
                            isIncome == true && parsedAmount!! < 0 -> parsedAmount * -1 // Ricarica negativa -> positiva
                            isIncome == false && parsedAmount!! > 0 -> parsedAmount * -1 // Spesa positiva -> negativa
                            else -> parsedAmount!! // Altrimenti usa il valore così com'è (per modifica o input diretto)
                        }

                        onConfirm(
                            entry?.copy(
                                description = description,
                                amount = finalAmount,
                                paymentMethod = paymentMethod,
                                category = category,
                                date = selectedDate
                            ) ?: Entry(
                                description = description,
                                amount = finalAmount,
                                paymentMethod = paymentMethod,
                                category = category,
                                date = selectedDate
                            )
                        )
                        errorMessage = "" // Resetta il messaggio di errore
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(strings.confirm)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { onDismiss() }) {
                Text(strings.cancel)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface
    )

    // DatePickerDialog - rimane come dialogo pop-up separato
    if (showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                Button(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val newDate = Date(millis)
                        val currentCalendar = Calendar.getInstance().apply { time = selectedDate }
                        val newCalendar = Calendar.getInstance().apply { time = newDate }
                        currentCalendar.set(Calendar.YEAR, newCalendar.get(Calendar.YEAR))
                        currentCalendar.set(Calendar.MONTH, newCalendar.get(Calendar.MONTH))
                        currentCalendar.set(Calendar.DAY_OF_MONTH, newCalendar.get(Calendar.DAY_OF_MONTH))
                        selectedDate = currentCalendar.time
                    }
                    showDatePickerDialog = false
                }) {
                    Text(strings.ok)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDatePickerDialog = false }) {
                    Text(strings.cancel)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Il nostro TimePickerDialog2 personalizzato
    if (showTimePickerDialog) {
        TimePickerDialog2(
            onDismissRequest = { showTimePickerDialog = false },
            onConfirm = { hour, minute ->
                val currentCalendar = Calendar.getInstance().apply { time = selectedDate }
                currentCalendar.set(Calendar.HOUR_OF_DAY, hour)
                currentCalendar.set(Calendar.MINUTE, minute)
                currentCalendar.set(Calendar.SECOND, 0)
                currentCalendar.set(Calendar.MILLISECOND, 0)
                selectedDate = currentCalendar.time
                showTimePickerDialog = false
            },
            initialHour = timePickerState.hour,
            initialMinute = timePickerState.minute
        )
    }

    // Dialog: Nuova categoria
    if (showNewCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showNewCategoryDialog = false },
            title = { Text(AppString.get().newCategory) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = {
                            newCategoryName = it
                            newCategoryError = false
                        },
                        label = { Text(AppString.get().categoryName) },
                        isError = newCategoryError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (newCategoryError) {
                        val msg = when (AppString.currentLanguage.value) {
                            "it" -> "Categoria già esistente o nome non valido"
                            "es" -> "Categoría ya existente o nombre no válido"
                            "fr" -> "Catégorie déjà existante ou nom non valide"
                            else -> "Category already exists or invalid name"
                        }
                        Text(text = msg, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val ok = AppString.addCustomCategory(context, newCategoryName)
                    if (ok) {
                        category = newCategoryName.trim()
                        showNewCategoryDialog = false
                    } else {
                        newCategoryError = true
                    }
                }) { Text(AppString.get().confirm) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showNewCategoryDialog = false }) { Text(AppString.get().cancel) }
            }
        )
    }
}

/**
 * Il nostro componente TimePickerDialog2 personalizzato.
 * Questo avvolge un AlertDialog generico e utilizza il TimeInput di Material3 per la selezione.
 *
 * @param onDismissRequest Callback invocato quando il dialogo viene chiuso.
 * @param onConfirm Callback invocato quando l'ora viene confermata, fornendo ora e minuto.
 * @param initialHour L'ora iniziale da visualizzare nel selettore dell'ora.
 * @param initialMinute Il minuto iniziale da visualizzare nel selettore dell'ora.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog2(
    onDismissRequest: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    initialHour: Int,
    initialMinute: Int
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = true
    )
    val strings = AppString.get()

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(strings.selectTime, color = MaterialTheme.colorScheme.onSurface) },
        text = {
            TimeInput(
                state = timePickerState,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(timePickerState.hour, timePickerState.minute)
            }) {
                Text(strings.ok)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismissRequest) {
                Text(strings.cancel)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface
    )
}
