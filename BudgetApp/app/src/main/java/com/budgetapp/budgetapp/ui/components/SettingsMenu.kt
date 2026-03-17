package com.budgetapp.budgetapp.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.budgetapp.budgetapp.utils.AppString

/**
 * Componente riutilizzabile per il menu Settings dell'app.
 * Include: Language, Theme Toggle, Privacy Policy, Logout, Delete Account
 *
 * @param expanded Stato di apertura/chiusura del menu
 * @param isDarkTheme Stato corrente del tema (dark/light)
 * @param onDismiss Callback per chiudere il menu
 * @param onLanguageClick Callback per aprire il dialogo lingua
 * @param onThemeToggle Callback per cambiare tema
 * @param onPrivacyPolicyClick Callback per aprire privacy policy
 * @param onSignOutClick Callback per logout
 * @param onDeleteAccountClick Callback per eliminare account
 */
@Composable
fun SettingsMenu(
    expanded: Boolean,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onLanguageClick: () -> Unit,
    onThemeToggle: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onDeleteAccountClick: () -> Unit
) {
    val strings = AppString.get()

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        // Language selector
        DropdownMenuItem(
            text = { Text(strings.setUpLanguage, color = MaterialTheme.colorScheme.onBackground) },
            onClick = {
                onLanguageClick()
                onDismiss()
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Language,
                    contentDescription = strings.setUpLanguage
                )
            }
        )

        // Theme Toggle
        DropdownMenuItem(
            text = {
                Text(
                    if (isDarkTheme) strings.lightMode else strings.darkMode,
                    color = MaterialTheme.colorScheme.onBackground
                )
            },
            onClick = {
                onThemeToggle()
                onDismiss()
            },
            leadingIcon = {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = strings.theme
                )
            }
        )

        // Privacy Policy
        DropdownMenuItem(
            text = { Text(strings.privacyPolicy, color = MaterialTheme.colorScheme.onBackground) },
            onClick = {
                onPrivacyPolicyClick()
                onDismiss()
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Policy,
                    contentDescription = strings.privacyPolicy
                )
            }
        )

        // Logout
        DropdownMenuItem(
            text = { Text(strings.logout, color = MaterialTheme.colorScheme.onBackground) },
            onClick = {
                onSignOutClick()
                onDismiss()
            },
            leadingIcon = {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = strings.logout
                )
            }
        )

        // Delete Account
        DropdownMenuItem(
            text = { Text(strings.deleteAccount, color = MaterialTheme.colorScheme.error) },
            onClick = {
                onDeleteAccountClick()
                onDismiss()
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = strings.deleteAccount,
                    tint = MaterialTheme.colorScheme.error
                )
            }
        )
    }
}

