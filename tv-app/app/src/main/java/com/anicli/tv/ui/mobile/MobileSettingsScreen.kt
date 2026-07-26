package com.anicli.tv.ui.mobile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.anicli.tv.ui.settings.SettingsViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MobileSettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val userEmail by viewModel.userEmail.collectAsState()
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isLoggingIn by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    val auth = FirebaseAuth.getInstance()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Settings") })
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (userEmail != null) {
                Text("Logged in as: $userEmail", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { viewModel.signOut() }) {
                    Text("Sign Out")
                }
            } else {
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                if (loginError != null) {
                    Text(loginError!!, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Button(
                        onClick = {
                            isLoggingIn = true
                            loginError = null
                            auth.signInWithEmailAndPassword(emailInput, passwordInput)
                                .addOnSuccessListener { isLoggingIn = false }
                                .addOnFailureListener { 
                                    loginError = it.message 
                                    isLoggingIn = false
                                }
                        },
                        enabled = !isLoggingIn && emailInput.isNotBlank() && passwordInput.isNotBlank()
                    ) {
                        Text("Login")
                    }
                    OutlinedButton(
                        onClick = {
                            isLoggingIn = true
                            loginError = null
                            auth.createUserWithEmailAndPassword(emailInput, passwordInput)
                                .addOnSuccessListener { isLoggingIn = false }
                                .addOnFailureListener { 
                                    loginError = it.message 
                                    isLoggingIn = false
                                }
                        },
                        enabled = !isLoggingIn && emailInput.isNotBlank() && passwordInput.isNotBlank()
                    ) {
                        Text("Register")
                    }
                }
            }
        }
    }
}
