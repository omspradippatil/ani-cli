package com.anicli.tv.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.*
import com.anicli.tv.core.theme.KuroNamiColors
import com.anicli.tv.domain.provider.ProviderSelector
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    BackHandler { onBack() }
    val userEmail by viewModel.userEmail.collectAsState()
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var isLoggingIn by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    val auth = FirebaseAuth.getInstance()

    val preferredLanguage by viewModel.preferredLanguage.collectAsState()
    val activeProvider by viewModel.activeProvider.collectAsState()
    val gogoDomain by viewModel.gogoDomain.collectAsState()
    val backendUrl by viewModel.backendUrl.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(KuroNamiColors.Obsidian)
            .padding(48.dp),
        horizontalArrangement = Arrangement.spacedBy(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // ── Left Column: App Settings ────────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                "Preferences",
                style = MaterialTheme.typography.displayMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(28.dp))

            // ── Audio Language ──────────────────────────────────────────────
            Text(
                "Default Audio Stream",
                color = KuroNamiColors.OnSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { viewModel.setPreferredLanguage("sub") },
                    colors = ButtonDefaults.colors(
                        containerColor = if (preferredLanguage == "sub")
                            KuroNamiColors.Crimson else KuroNamiColors.SurfaceLow,
                        focusedContainerColor = KuroNamiColors.CrimsonContainer
                    ),
                    shape = ButtonDefaults.shape(RoundedCornerShape(8.dp))
                ) {
                    Text(
                        "Japanese (Sub)",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Button(
                    onClick = { viewModel.setPreferredLanguage("dub") },
                    colors = ButtonDefaults.colors(
                        containerColor = if (preferredLanguage == "dub")
                            KuroNamiColors.Crimson else KuroNamiColors.SurfaceLow,
                        focusedContainerColor = KuroNamiColors.CrimsonContainer
                    ),
                    shape = ButtonDefaults.shape(RoundedCornerShape(8.dp))
                ) {
                    Text(
                        "English (Dub)",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Provider Selection ──────────────────────────────────────────
            Text(
                "Active Source Engine",
                color = KuroNamiColors.OnSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Web Cloud is recommended for direct ultra-fast CDN delivery",
                color = KuroNamiColors.OnSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { viewModel.setActiveProvider(ProviderSelector.PROVIDER_WEB_API) },
                    colors = ButtonDefaults.colors(
                        containerColor = if (activeProvider == ProviderSelector.PROVIDER_WEB_API)
                            KuroNamiColors.Crimson else KuroNamiColors.SurfaceLow,
                        focusedContainerColor = KuroNamiColors.CrimsonContainer
                    ),
                    shape = ButtonDefaults.shape(RoundedCornerShape(8.dp))
                ) {
                    Text(
                        "Web Cloud (Recommended)",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Button(
                    onClick = { viewModel.setActiveProvider(ProviderSelector.PROVIDER_ALLANIME) },
                    colors = ButtonDefaults.colors(
                        containerColor = if (activeProvider == ProviderSelector.PROVIDER_ALLANIME)
                            KuroNamiColors.Crimson else KuroNamiColors.SurfaceLow,
                        focusedContainerColor = KuroNamiColors.CrimsonContainer
                    ),
                    shape = ButtonDefaults.shape(RoundedCornerShape(8.dp))
                ) {
                    Text(
                        "AllAnime",
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
                Button(
                    onClick = { viewModel.setActiveProvider(ProviderSelector.PROVIDER_GOGOANIME) },
                    colors = ButtonDefaults.colors(
                        containerColor = if (activeProvider == ProviderSelector.PROVIDER_GOGOANIME)
                            KuroNamiColors.Cyan else KuroNamiColors.SurfaceLow,
                        focusedContainerColor = KuroNamiColors.CyanSoft
                    ),
                    shape = ButtonDefaults.shape(RoundedCornerShape(8.dp))
                ) {
                    Text(
                        "GoGoAnime",
                        fontWeight = FontWeight.Medium,
                        color = if (activeProvider == ProviderSelector.PROVIDER_GOGOANIME) Color.Black else Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }

            // ── Web Cloud Backend URL ──
            if (activeProvider == ProviderSelector.PROVIDER_WEB_API) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "Connected Backend Server",
                    color = KuroNamiColors.OnSurface,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                var backendInput by remember(backendUrl) { mutableStateOf(backendUrl) }

                OutlinedTextField(
                    value = backendInput,
                    onValueChange = { backendInput = it },
                    label = { androidx.compose.material3.Text("Server URL", color = KuroNamiColors.OnSurfaceVariant) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = KuroNamiColors.OnSurface,
                        focusedContainerColor = KuroNamiColors.SurfaceLow,
                        unfocusedContainerColor = KuroNamiColors.SurfaceLow,
                        focusedIndicatorColor = KuroNamiColors.Crimson,
                        unfocusedIndicatorColor = KuroNamiColors.Outline.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth(0.9f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.setBackendUrl(backendInput) },
                    colors = ButtonDefaults.colors(
                        containerColor = KuroNamiColors.Crimson,
                        focusedContainerColor = KuroNamiColors.CrimsonContainer
                    ),
                    shape = ButtonDefaults.shape(RoundedCornerShape(8.dp))
                ) {
                    Text(
                        "Save Server URL",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // ── Right Column: Account & Sync ────────────────────────────────────
        Column(
            modifier = Modifier
                .weight(0.9f)
                .fillMaxHeight()
                .background(KuroNamiColors.SurfaceLow, shape = RoundedCornerShape(16.dp))
                .border(1.dp, KuroNamiColors.Outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Account & Sync",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "Sync watch history across devices",
                style = MaterialTheme.typography.bodySmall,
                color = KuroNamiColors.OnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (userEmail != null) {
                Text(
                    text = "Signed in as",
                    color = KuroNamiColors.OnSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = userEmail ?: "",
                    color = KuroNamiColors.Cyan,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )
                Button(
                    onClick = { viewModel.signOut() },
                    colors = ButtonDefaults.colors(
                        containerColor = KuroNamiColors.SurfaceHigh,
                        focusedContainerColor = KuroNamiColors.Crimson
                    ),
                    shape = ButtonDefaults.shape(RoundedCornerShape(8.dp))
                ) {
                    Text("Sign Out", fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedTextField(
                    value = emailInput,
                    onValueChange = { emailInput = it },
                    label = { androidx.compose.material3.Text("Email", color = KuroNamiColors.OnSurfaceVariant) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = KuroNamiColors.Surface,
                        unfocusedContainerColor = KuroNamiColors.Surface,
                        focusedIndicatorColor = KuroNamiColors.Crimson
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { androidx.compose.material3.Text("Password", color = KuroNamiColors.OnSurfaceVariant) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = KuroNamiColors.Surface,
                        unfocusedContainerColor = KuroNamiColors.Surface,
                        focusedIndicatorColor = KuroNamiColors.Crimson
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (loginError != null) {
                    Text(
                        text = loginError ?: "",
                        color = KuroNamiColors.Crimson,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            if (emailInput.isNotBlank() && passwordInput.isNotBlank()) {
                                isLoggingIn = true
                                loginError = null
                                auth.signInWithEmailAndPassword(emailInput.trim(), passwordInput)
                                    .addOnCompleteListener { task ->
                                        isLoggingIn = false
                                        if (!task.isSuccessful) {
                                            loginError = task.exception?.localizedMessage ?: "Login failed"
                                        }
                                    }
                            }
                        },
                        colors = ButtonDefaults.colors(
                            containerColor = KuroNamiColors.Crimson,
                            focusedContainerColor = KuroNamiColors.CrimsonContainer
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(8.dp))
                    ) {
                        Text(if (isLoggingIn) "Logging in..." else "Sign In", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (emailInput.isNotBlank() && passwordInput.isNotBlank()) {
                                isLoggingIn = true
                                loginError = null
                                auth.createUserWithEmailAndPassword(emailInput.trim(), passwordInput)
                                    .addOnCompleteListener { task ->
                                        isLoggingIn = false
                                        if (!task.isSuccessful) {
                                            loginError = task.exception?.localizedMessage ?: "Registration failed"
                                        }
                                    }
                            }
                        },
                        colors = ButtonDefaults.colors(
                            containerColor = KuroNamiColors.SurfaceHigh,
                            focusedContainerColor = KuroNamiColors.SurfaceHighest
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(8.dp))
                    ) {
                        Text("Register", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
