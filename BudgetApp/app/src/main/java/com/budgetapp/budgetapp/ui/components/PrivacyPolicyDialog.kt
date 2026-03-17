package com.budgetapp.budgetapp.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.budgetapp.budgetapp.utils.AppString

/**
 * Dialog che mostra la Privacy Policy.
 * Offre la possibilità di visualizzare la policy e aprire il link completo online.
 *
 * @param onDismiss Callback chiamato quando il dialog viene chiuso.
 */
@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    val strings = AppString.get()
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = strings.privacyPolicy,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = strings.privacyPolicyContent,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = {
                        // Apri il link alla privacy policy completa
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.giocaitalia.it/apps/budgetapp/privacy_budgetapp.html"))
                        context.startActivity(intent)
                    }
                ) {
                    Text("${strings.termsAndConditions} (Online)")
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(strings.ok)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface
    )
}
