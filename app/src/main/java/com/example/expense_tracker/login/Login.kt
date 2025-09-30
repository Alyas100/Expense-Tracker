package com.example.expense_tracker.login

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class Login : ComponentActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            Log.e("Login", "Firebase initialization failed", e)
        }

        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)

        setContent {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = "login_screen") {
                composable("login_screen") {
                    LoginScreen(
                        auth = auth,
                        credentialManager = credentialManager,
                        navController = navController
                    )
                }
                composable("main_screen") {
                    MainScreen()
                }
            }
        }
    }

    @Composable
    fun LoginScreen(
        auth: FirebaseAuth,
        credentialManager: CredentialManager,
        navController: NavHostController
    ) {
        val context = LocalContext.current
        val activity = context as ComponentActivity
        val coroutineScope = rememberCoroutineScope()
        var isLoading by remember { mutableStateOf(false) }

        // UI
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF0F0F0)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Login Screen",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Button(onClick = {
                        isLoading = true

                        // Get client ID from resources
                        val clientId = try {
                            context.getString(R.string.default_web_client_id)
                        } catch (e: Exception) {
                            Log.e("Login", "Failed to get client ID from R.string", e)
                            isLoading = false
                            Toast.makeText(
                                context,
                                "Configuration error: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            return@Button
                        }

                        if (clientId.isEmpty()) {
                            isLoading = false
                            Toast.makeText(
                                context,
                                "Client ID is empty in strings.xml",
                                Toast.LENGTH_LONG
                            ).show()
                            return@Button
                        }

                        Log.d("Login", "Using client ID: $clientId")

                        // Create Google ID option using Credential Manager (latest API)
                        val googleIdOption = GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false) // Show all accounts
                            .setServerClientId(clientId)
                            .build()

                        // Create credential request
                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build()

                        // Launch coroutine to get credentials
                        coroutineScope.launch {
                            try {
                                val result = credentialManager.getCredential(
                                    request = request,
                                    context = activity
                                )

                                // Handle the credential result
                                val credential = result.credential

                                if (credential is CustomCredential &&
                                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {

                                    val googleIdTokenCredential = GoogleIdTokenCredential
                                        .createFrom(credential.data)

                                    val idToken = googleIdTokenCredential.idToken
                                    Log.d("Login", "Got ID token, authenticating with Firebase")

                                    // Authenticate with Firebase
                                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                                    auth.signInWithCredential(firebaseCredential)
                                        .addOnCompleteListener(activity) { task ->
                                            isLoading = false
                                            if (task.isSuccessful) {
                                                Log.d("Login", "Firebase auth successful")
                                                navController.navigate("main_screen") {
                                                    popUpTo("login_screen") { inclusive = true }
                                                }
                                            } else {
                                                Log.e("Login", "Firebase auth failed", task.exception)
                                                Toast.makeText(
                                                    context,
                                                    "Authentication failed: ${task.exception?.message}",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                } else {
                                    isLoading = false
                                    Log.w("Login", "Unexpected credential type: ${credential.type}")
                                    Toast.makeText(
                                        context,
                                        "Unexpected credential type",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }

                            } catch (e: GetCredentialException) {
                                isLoading = false
                                Log.e("Login", "Get credential failed", e)
                                Toast.makeText(
                                    context,
                                    "Sign in failed: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            } catch (e: Exception) {
                                isLoading = false
                                Log.e("Login", "Unexpected error", e)
                                Toast.makeText(
                                    context,
                                    "Error: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }) {
                        Text("Sign in with Google")
                    }
                }
            }
        }
    }

    @Composable
    fun MainScreen() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE0F7FA)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Welcome to the Main Screen!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF006064)
            )
        }
    }
}