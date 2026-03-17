package com.budgetapp.budgetapp.utils

import android.content.Context
import com.google.firebase.FirebaseApp

object AuthPrefs {
    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_FIRST_LAUNCH_DONE = "first_launch_done"
    private const val KEY_LAST_LOGOUT = "last_logout"
    private const val KEY_DARK_MODE = "dark_mode"

    private fun prefs(): android.content.SharedPreferences {
        val appContext = FirebaseApp.getInstance().applicationContext
        return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun isFirstLaunchDone(): Boolean = prefs().getBoolean(KEY_FIRST_LAUNCH_DONE, false)
    fun markFirstLaunchDone() { prefs().edit().putBoolean(KEY_FIRST_LAUNCH_DONE, true).apply() }

    fun wasLoggedOut(): Boolean = prefs().getBoolean(KEY_LAST_LOGOUT, false)
    fun markLoggedOut() { prefs().edit().putBoolean(KEY_LAST_LOGOUT, true).apply() }
    fun clearLoggedOut() { prefs().edit().putBoolean(KEY_LAST_LOGOUT, false).apply() }

    // Gestione tema
    fun isDarkMode(): Boolean = prefs().getBoolean(KEY_DARK_MODE, true) // Default: dark mode
    fun setDarkMode(enabled: Boolean) { prefs().edit().putBoolean(KEY_DARK_MODE, enabled).apply() }
}
