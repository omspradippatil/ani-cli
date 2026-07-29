package com.anicli.tv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.*
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import com.google.firebase.auth.FirebaseAuth
import com.anicli.tv.domain.provider.ProviderSelector

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
    val auth = FirebaseAuth.getInstance()

    val preferredLanguage by viewModel.preferredLanguage.collectAsState()
    val activeProvider by viewModel.activeProvider.collectAsState()
    val gogoDomain by viewModel.gogoDomain.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalArrangement = Arrangement.spacedBy(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Left Column: App Settings ────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                "Settings",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(40.dp))

            // ── Audio Language ──────────────────────────────────────────────
            Text(
                "Default Audio Language",
                color = Color.LightGray,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { viewModel.setPreferredLanguage("dub") },
                    colors = ButtonDefaults.colors(
                        containerColor = if (preferredLanguage == "dub")
                            MaterialTheme.colorScheme.primary else Color.DarkGray
                    )
                ) {
                    Text("English (Dub)", modifier = Modifier.padding(8.dp))
                }
                Button(
                    onClick = { viewModel.setPreferredLanguage("sub") },
                    colors = ButtonDefaults.colors(
                        containerColor = if (preferredLanguage == "sub")
                            MaterialTheme.colorScheme.primary else Color.DarkGray
                    )
                ) {
                    Text("Japanese (Sub)", modifier = Modifier.padding(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // ── Provider Selection ──────────────────────────────────────────
            Text(
                "Anime Source",
                color = Color.LightGray,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Switch between AllAnime (GraphQL) and GoGoAnime (HTML scraper)",
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = { viewModel.setActiveProvider(ProviderSelector.PROVIDER_ALLANIME) },
                    colors = ButtonDefaults.colors(
                        containerColor = if (activeProvider == ProviderSelector.PROVIDER_ALLANIME)
                            MaterialTheme.colorScheme.primary else Color.DarkGray
                    )
                ) {
                    Text("AllAnime", modifier = Modifier.padding(8.dp))
                }
                Button(
                    onClick = { viewModel.setActiveProvider(ProviderSelector.PROVIDER_GOGOANIME) },
                    colors = ButtonDefaults.colors(
                        containerColor = if (activeProvider == ProviderSelector.PROVIDER_GOGOANIME)
                            Color(0xFF1E8C45) else Color.DarkGray
                    )
                ) {
                    Text("GoGoAnime", modifier = Modifier.padding(8.dp))
                }
            }

            // ── GoGoAnime Mirror Domain (only shown when GoGoAnime is active) ──
            if (activeProvider == ProviderSelector.PROVIDER_GOGOANIME) {
                Spacer(modifier = Modifier.height(28.dp))
                Text(
                    "GoGoAnime Mirror Domain",
                    color = Color.LightGray,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "If the default domain is blocked by your ISP, enter a working mirror here.",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))

                var domainInput by remember(gogoDomain) { mutableStateOf(gogoDomain) }

                OutlinedTextField(
                    value = domainInput,
                    onValueChange = { domainInput = it },
                    label = { androidx.compose.material3.Text("Domain (e.g. gogoanime.by)", color = Color.LightGray) },
                    placeholder = { androidx.compose.material3.Text("gogoanime.by", color = Color.Gray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.White.copy(alpha = 0.1f),
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.setGogoDomain(domainInput) },
                    colors = ButtonDefaults.colors(containerColor = Color(0xFF1E8C45))
                ) {
                    Text("Save Domain", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Known working mirrors: gogoanime.by • gogoanime3.co • anitaku.pe",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // ── Right Column: Account & Auth ────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color.DarkGray.copy(alpha = 0.3f), shape = RoundedCornerShape(16.dp))
                .padding(32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Account",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (userEmail != null) {
                Text("Logged in as:", color = Color.LightGray, style = MaterialTheme.typography.titleMedium)
                Text(userEmail!!, color = Color.White, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = { viewModel.signOut() }) {
                    Text("Sign Out", modifier = Modifier.padding(8.dp))
                }
            } else {
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { androidx.compose.material3.Text("Email", color = Color.LightGray) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.White.copy(alpha = 0.1f),
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { androidx.compose.material3.Text("Password", color = Color.LightGray) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.White.copy(alpha = 0.1f),
                        unfocusedContainerColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                if (loginError != null) {
                    Text(loginError!!, color = Color(0xFFFF5252))
                    Spacer(modifier = Modifier.height(16.dp))
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
                        enabled = !isLoggingIn && emailInput.isNotBlank() && passwordInput.isNotBlank(),
                        colors = ButtonDefaults.colors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Login", modifier = Modifier.padding(8.dp))
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
                        enabled = !isLoggingIn && emailInput.isNotBlank() && passwordInput.isNotBlank(),
                        colors = ButtonDefaults.colors(containerColor = Color.DarkGray)
                    ) {
                        Text("Register", modifier = Modifier.padding(8.dp))
                    }
                }
            }
        }
    }
}
