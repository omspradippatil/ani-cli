package com.anicli.tv.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val userEmail by viewModel.userEmail.collectAsState()
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isLoggingIn by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val auth = FirebaseAuth.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Settings", style = androidx.tv.material3.MaterialTheme.typography.displayMedium, color = Color.White)
        Spacer(modifier = Modifier.height(32.dp))

        val preferredLanguage by viewModel.preferredLanguage.collectAsState()
        Text("Default Audio Language", color = Color.White, style = androidx.tv.material3.MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = { viewModel.setPreferredLanguage("dub") },
                colors = androidx.tv.material3.ButtonDefaults.colors(
                    containerColor = if (preferredLanguage == "dub") androidx.tv.material3.MaterialTheme.colorScheme.primary else Color.DarkGray
                )
            ) {
                Text("English (Dub)")
            }
            Button(
                onClick = { viewModel.setPreferredLanguage("sub") },
                colors = androidx.tv.material3.ButtonDefaults.colors(
                    containerColor = if (preferredLanguage == "sub") androidx.tv.material3.MaterialTheme.colorScheme.primary else Color.DarkGray
                )
            ) {
                Text("Japanese (Sub)")
            }
        }
        
        Spacer(modifier = Modifier.height(48.dp))

        if (userEmail != null) {
            Text("Logged in as: $userEmail", color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { viewModel.signOut() }) {
                Text("Sign Out")
            }
        } else {
            OutlinedTextField(
                value = emailInput,
                onValueChange = { emailInput = it },
                label = { androidx.compose.material3.Text("Email", color = Color.White) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = { androidx.compose.material3.Text("Password", color = Color.White) },
                colors = TextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            if (loginError != null) {
                Text(loginError!!, color = Color.Red)
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
                
                Button(
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
