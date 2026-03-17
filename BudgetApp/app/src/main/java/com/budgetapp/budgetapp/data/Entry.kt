// File: data/Entry.kt
package com.budgetapp.budgetapp.data

import com.google.firebase.firestore.IgnoreExtraProperties
import java.util.Date

/**
 * Modello di dati per una singola transazione o ricarica (Entry).
 * Rappresenta un movimento di denaro nel budget personale.
 *
 * @param id ID univoco dell'entry (generato da Firebase o locale).
 * @param description Breve descrizione della transazione/ricarica.
 * @param amount Importo della transazione. Positivo per ricariche, negativo per spese.
 * @param paymentMethod Metodo di pagamento (es. "carta", "contanti").
 * @param category Categoria della transazione (es. "food", "transport", "salario").
 * @param date Data e ora della transazione.
 */
@IgnoreExtraProperties
data class Entry(
    var id: String = "", // ID univoco per Firebase
    var description: String = "",
    var amount: Double = 0.0,
    var paymentMethod: String = "", // "carta" o "contanti"
    var category: String = "", // "food", "transport", "salario", etc.
    var date: Date = Date()
) {
    // Costruttore no-arg esplicito richiesto da Firestore per la deserializzazione affidabile
    constructor() : this(
        id = "",
        description = "",
        amount = 0.0,
        paymentMethod = "",
        category = "",
        date = Date()
    )
}
