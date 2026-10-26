package com.anicli.tv.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.anicli.tv.core.theme.KuroNamiColors
import com.anicli.tv.domain.provider.ProviderSelector

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    BackHandler { onBack() }

    val currentUser by viewModel.currentUser.collectAsState()
    val userStats by viewModel.userStats.collectAsState()
    val isAuthLoading by viewModel.isAuthLoading.collectAsState()
    val authError by viewModel.authError.collectAsState()

    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    val preferredLanguage by viewModel.preferredLanguage.collectAsState()
    val activeProvider by viewModel.activeProvider.collectAsState()
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
            Spacer(modifier = Modifier.height(24.dp))

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

            Spacer(modifier = Modifier.height(24.dp))

            // ── Provider Selection ──────────────────────────────────────────
            Text(
                "Active Source Engine",
                color = KuroNamiColors.OnSurface,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Web Cloud connects directly to the SQLite backend and ultra-fast CDN delivery",
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

        // ── Right Column: Account & SQLite Sync ──────────────────────────────
        Column(
            modifier = Modifier
                .weight(1.0f)
                .fillMaxHeight()
                .background(KuroNamiColors.SurfaceLow, shape = RoundedCornerShape(16.dp))
                .border(1.dp, KuroNamiColors.Outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(28.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Account & SQLite Sync",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Unified sync with Website watch history and bookmarks",
                style = MaterialTheme.typography.bodySmall,
                color = KuroNamiColors.OnSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (currentUser != null) {
                // ── Signed In User Profile Card ──
                val user = currentUser!!
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (!user.avatar.isNullOrBlank()) {
                        AsyncImage(
                            model = user.avatar,
                            contentDescription = user.username,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .border(2.dp, KuroNamiColors.Cyan, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(KuroNamiColors.Cyan.copy(alpha = 0.2f), CircleShape)
                                .border(2.dp, KuroNamiColors.Cyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = user.username.take(1).uppercase(),
                                color = KuroNamiColors.Cyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                        }
                    }

                    Column {
                        Text(
                            text = user.username,
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${user.tier ?: "Community Otaku"} • Lvl ${user.level}",
                            color = KuroNamiColors.Cyan,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Cloud Status
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFF10B981), CircleShape)
                    )
                    Text(
                        text = "Connected to SQLite / D1 Backend",
                        color = Color(0xFF10B981),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats Cards
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val watchedCount = userStats?.totalEpisodesWatched
                        ?: userStats?.episodesWatched
                        ?: userStats?.totalWatched
                        ?: 0
                    val watchlistCount = userStats?.watchlistCount ?: 0

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(KuroNamiColors.Surface, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$watchedCount",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Episodes",
                                color = KuroNamiColors.OnSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(KuroNamiColors.Surface, RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$watchlistCount",
                                color = KuroNamiColors.Cyan,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Watchlist",
                                color = KuroNamiColors.OnSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                // ── 1-Click TV Remote Demo Login ──
                Button(
                    onClick = { viewModel.demoLogin() },
                    colors = ButtonDefaults.colors(
                        containerColor = KuroNamiColors.Crimson,
                        focusedContainerColor = KuroNamiColors.CrimsonContainer
                    ),
                    shape = ButtonDefaults.shape(RoundedCornerShape(10.dp)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        if (isAuthLoading) "Connecting to SQLite..." else "⚡ 1-Click Demo Login (Alex Mercer)",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(KuroNamiColors.Outline.copy(alpha = 0.3f)))
                    Text(
                        "  OR SIGN IN  ",
                        color = KuroNamiColors.OnSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(KuroNamiColors.Outline.copy(alpha = 0.3f)))
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    label = { androidx.compose.material3.Text("Username or Email", color = KuroNamiColors.OnSurfaceVariant) },
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
                Spacer(modifier = Modifier.height(10.dp))
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

                if (authError != null) {
                    Text(
                        text = authError ?: "",
                        color = KuroNamiColors.Crimson,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { viewModel.login(usernameInput, passwordInput) },
                        colors = ButtonDefaults.colors(
                            containerColor = KuroNamiColors.Crimson,
                            focusedContainerColor = KuroNamiColors.CrimsonContainer
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(8.dp))
                    ) {
                        Text(if (isAuthLoading) "Signing in..." else "Sign In", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.register(usernameInput, null, passwordInput) },
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
