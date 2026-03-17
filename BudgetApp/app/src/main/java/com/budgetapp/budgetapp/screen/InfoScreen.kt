package com.budgetapp.budgetapp.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetapp.budgetapp.ui.theme.MyWalletTheme
import com.budgetapp.budgetapp.utils.AppString
import com.google.firebase.auth.FirebaseUser

/**
 * InfoScreen ora è un popup (AlertDialog) invece di una schermata separata.
 * Mostra un dialogo con le informazioni dell'app, il benvenuto e la versione.
 */
@Composable
fun InfoScreen(onBack: () -> Unit, appVersion: String, user: FirebaseUser? = null) {
    InfoDialog(show = true, onDismiss = onBack, appVersion = appVersion, user = user)
}

@Composable
private fun InfoContent(appVersion: String, userDisplayName: String, featuresString: String) {
    val strings = AppString.get()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = strings.welcome + " " + userDisplayName,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 12.dp),
            textAlign = TextAlign.Center
        )
        Text(
            text = strings.appDescription,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Text(
            text = strings.keyFeatures,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = featuresString,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
        Text(
            text = "${strings.appVersionLabel}: $appVersion",
            fontSize = 13.sp,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun InfoDialog(show: Boolean, onDismiss: () -> Unit, appVersion: String, user: FirebaseUser?) {
    if (!show) return
    val strings = AppString.get()
    val userDisplayName = remember(user) { user?.displayName ?: "user" }
    val featuresString = remember(strings) {
        listOf(
            strings.easyEntryTracking,
            strings.monthlyAndYearlyOverviews,
            strings.interactiveCalendar,
            strings.detailedLog,
            strings.powerfulStatistics,
            strings.secureLogin
        ).joinToString("\n")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = strings.myWallet,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                InfoContent(
                    appVersion = appVersion,
                    userDisplayName = userDisplayName,
                    featuresString = featuresString
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(strings.ok)
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.primary,
        textContentColor = MaterialTheme.colorScheme.onSurface
    )
}

@Preview(showBackground = true)
@Composable
fun PreviewInfoScreen() {
    MyWalletTheme(darkTheme = true) {
        InfoScreen(onBack = {}, appVersion = "1.0.0")
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewInfoDialog() {
    MyWalletTheme(darkTheme = true) {
        InfoDialog(show = true, onDismiss = {}, appVersion = "1.0.0", user = null)
    }
}
