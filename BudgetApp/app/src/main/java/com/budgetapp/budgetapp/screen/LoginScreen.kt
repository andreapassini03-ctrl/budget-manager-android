package com.budgetapp.budgetapp.screen

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewModelScope
import com.budgetapp.budgetapp.R
import com.budgetapp.budgetapp.utils.AppString
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.budgetapp.budgetapp.viewmodel.BudgetViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onAuthSuccess: () -> Unit,
    onSignInAsGuest: () -> Unit,
    viewModel: BudgetViewModel
) {
    val TAG = "LoginScreenLog"

    val context = LocalContext.current
    val strings = AppString.get()

    // Observe authentication states from TravelViewModel
    val currentUser by viewModel.currentUser.collectAsState(initial = null)
    val isLoading by viewModel.isAuthLoading.collectAsState()
    val errorMessage by viewModel.authErrorMessage.collectAsState()

    // Initialize GoogleSignInClient
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    // Google Sign-In Launcher
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    Log.d(TAG, "Google sign-in successful, attempting Firebase auth.")
                    viewModel.viewModelScope.launch {
                        val success = viewModel.signInWithGoogle(idToken)
                        if (success) {
                            Log.d(TAG, "Firebase Google auth successful.")
                            // Navigation will be handled by LaunchedEffect(currentUser) in MainActivity
                        } else {
                            Log.e(TAG, "Firebase Google auth failed.")
                            Toast.makeText(context, strings.authenticating, Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.e(TAG, "Google ID Token is null.")
                    Toast.makeText(context, "Errore: ID Token Google nullo.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Log.e(TAG, "Google sign-in failed: ${e.statusCode} - ${e.message}", e)
                Toast.makeText(context, "Accesso Google fallito: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Error during Google sign-in process: ${e.message}", e)
                Toast.makeText(context, "Errore accesso Google: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.d(TAG, "Google sign-in cancelled or failed with result code: ${result.resultCode}")
            Toast.makeText(context, "Accesso Google annullato.", Toast.LENGTH_SHORT).show()
        }
    }

    // Effect to navigate on successful authentication
    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            Log.d(TAG, "LaunchedEffect(currentUser): User is now authenticated, navigating to success.")
            onAuthSuccess()
        }
    }

    // Effect to show error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            //travelViewModel.authErrorMessage.value = null // Consume the error
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = strings.myWallet,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Image(
            painter = painterResource(id = R.drawable.wallet_logo),
            contentDescription = "budgetapp App Logo",
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = strings.appDescription,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(48.dp))

        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(48.dp))
            Text(strings.authenticating, modifier = Modifier.padding(top = 16.dp))
        } else {
            Button(
                onClick = {
                    Log.d(TAG, "Google Sign-In button clicked.")
                    // Avvia direttamente il flusso di sign-in senza fare signOut,
                    // così il silent sign-in potrà funzionare ai successivi avvii
                    val signInIntent = googleSignInClient.signInIntent
                    signInLauncher.launch(signInIntent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(strings.signInWithGoogle, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    Log.d(TAG, "Sign In as Guest button clicked.")
                    onSignInAsGuest()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text(strings.signInAsAnonymous, fontSize = 18.sp, color = Color.White)
            }
        }
    }
}
