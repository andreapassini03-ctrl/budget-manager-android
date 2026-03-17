package com.budgetapp.budgetapp.screen

import com.budgetapp.budgetapp.utils.AppString
import java.text.NumberFormat

fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(AppString.currentLocale())
    return formatter.format(amount)
}

