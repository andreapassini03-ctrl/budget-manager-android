package com.budgetapp.budgetapp.utils

import android.content.Context
import android.content.SharedPreferences

object OnboardingPreferences {
    private const val PREFS_NAME = "onboarding_prefs"
    private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isOnboardingCompleted(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(context: Context) {
        getPreferences(context).edit().putBoolean(KEY_ONBOARDING_COMPLETED, true).apply()
    }

    // Per testing: resetta onboarding
    fun resetOnboarding(context: Context) {
        getPreferences(context).edit().putBoolean(KEY_ONBOARDING_COMPLETED, false).apply()
    }
}

