package com.budgetapp.budgetapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.budgetapp.budgetapp.utils.AppString
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

/**
 * Componente riutilizzabile per il menu drawer laterale dell'app.
 *
 * @param drawerState Stato del drawer per gestire apertura/chiusura
 * @param drawerWidth Larghezza del drawer
 * @param currentUser Utente corrente per mostrare il nome
 * @param onNavigateToInfo Callback per navigare alla schermata Info
 * @param onNavigateToHome Callback per navigare alla Home Page
 * @param onNavigateToCalendar Callback per navigare al Calendario
 * @param onNavigateToLog Callback per navigare al Log transazioni
 * @param onNavigateToStatistics Callback per navigare alle Statistiche
 */
@Composable
fun AppDrawer(
    drawerState: DrawerState,
    drawerWidth: Dp,
    currentUser: FirebaseUser?,
    onNavigateToInfo: () -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToCalendar: () -> Unit,
    onNavigateToLog: () -> Unit,
    onNavigateToStatistics: () -> Unit
) {
    val strings = AppString.get()
    val scope = rememberCoroutineScope()

    ModalDrawerSheet(modifier = Modifier.width(drawerWidth)) {
        // Header professionale con avatar e info utente
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar circolare
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    tonalElevation = 4.dp
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "User Avatar",
                        modifier = Modifier
                            .padding(8.dp)
                            .size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                // Nome utente
                val userDisplayName = when {
                    currentUser?.isAnonymous == true -> strings.guestUser
                    currentUser?.displayName?.isNotBlank() == true -> currentUser.displayName
                    currentUser?.email?.isNotBlank() == true -> currentUser.email?.substringBefore('@')
                    else -> strings.guestUser
                } ?: strings.guestUser

                Text(
                    text = userDisplayName ?: strings.guestUser,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Email (se disponibile)
                if (currentUser?.email != null && currentUser.isAnonymous == false) {
                    Text(
                        text = currentUser.email ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Menu Items
        NavigationDrawerItem(
            label = { Text(strings.myAccount) },
            selected = false,
            icon = { Icon(Icons.Default.Person, contentDescription = null) },
            onClick = {
                scope.launch { drawerState.close() }
                onNavigateToInfo()
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )

        NavigationDrawerItem(
            label = { Text(strings.homePage) },
            selected = false,
            icon = { Icon(Icons.Default.Home, contentDescription = null) },
            onClick = {
                scope.launch { drawerState.close() }
                onNavigateToHome()
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )

        NavigationDrawerItem(
            label = { Text(strings.currentMonth) },
            selected = false,
            icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
            onClick = {
                scope.launch { drawerState.close() }
                onNavigateToCalendar()
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )

        NavigationDrawerItem(
            label = { Text(strings.recentTransactions) },
            selected = false,
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
            onClick = {
                scope.launch { drawerState.close() }
                onNavigateToLog()
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )

        NavigationDrawerItem(
            label = { Text(strings.statistics) },
            selected = false,
            icon = { Icon(Icons.Default.Star, contentDescription = null) },
            onClick = {
                scope.launch { drawerState.close() }
                onNavigateToStatistics()
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}
