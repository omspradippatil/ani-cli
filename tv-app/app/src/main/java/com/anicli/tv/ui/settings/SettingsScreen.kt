package com.anicli.tv.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.*
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
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

    val tvPairCode by viewModel.tvPairCode.collectAsState()
    val isTvCodeLoading by viewModel.isTvCodeLoading.collectAsState()
    val tvPairStatus by viewModel.tvPairStatus.collectAsState()
    val availableAccounts by viewModel.availableAccounts.collectAsState()

    var usernameInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }

    val usernameFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val signInBtnFocusRequester = remember { FocusRequester() }
    val backendInputFocusRequester = remember { FocusRequester() }

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
                    keyboardActions = KeyboardActions(onDone = { viewModel.setBackendUrl(backendInput) }),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = KuroNamiColors.OnSurface,
                        focusedContainerColor = KuroNamiColors.SurfaceLow,
                        unfocusedContainerColor = KuroNamiColors.SurfaceLow,
                        focusedIndicatorColor = KuroNamiColors.Crimson,
                        unfocusedIndicatorColor = KuroNamiColors.Outline.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .focusRequester(backendInputFocusRequester)
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
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(user.avatar)
                                .size(112, 112)
                                .precision(coil.size.Precision.INEXACT)
                                .crossfade(false)
                                .allowRgb565(true)
                                .build(),
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
                // ── 1. TV Link Code (Fastest & Zero Remote Typing) ──
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(KuroNamiColors.Surface, RoundedCornerShape(12.dp))
                        .border(1.5.dp, KuroNamiColors.CyanSoft.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.Tv,
                                contentDescription = null,
                                tint = KuroNamiColors.Cyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Link via Phone / Browser",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "Open ani-cli on Web & click 'Link Android TV'",
                            color = KuroNamiColors.OnSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                        )

                        // 6-Character Code Display
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(KuroNamiColors.SurfaceLow, RoundedCornerShape(8.dp))
                                .border(1.dp, KuroNamiColors.Outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(vertical = 8.dp, horizontal = 12.dp)
                        ) {
                            Text(
                                text = if (isTvCodeLoading) "GENERATING..." else (tvPairCode?.chunked(3)?.joinToString(" - ") ?: "------"),
                                color = KuroNamiColors.Cyan,
                                fontSize = 24.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = { viewModel.generateTvPairCode() },
                                colors = ButtonDefaults.colors(
                                    containerColor = KuroNamiColors.SurfaceHigh,
                                    focusedContainerColor = KuroNamiColors.Crimson
                                ),
                                shape = ButtonDefaults.shape(RoundedCornerShape(6.dp)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.Sync,
                                        contentDescription = "New Code",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("New Code", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        if (!tvPairStatus.isNullOrBlank()) {
                            Text(
                                text = tvPairStatus ?: "",
                                color = if (tvPairStatus!!.contains("Success", ignoreCase = true)) Color(0xFF10B981) else KuroNamiColors.Crimson,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── 2. Discovered Accounts Quick Switcher ──
                if (availableAccounts.isNotEmpty()) {
                    Text(
                        text = "Or Select Web Account:",
                        color = KuroNamiColors.OnSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableAccounts.take(3).forEach { acc ->
                            Button(
                                onClick = {
                                    usernameInput = acc.username
                                    try { passwordFocusRequester.requestFocus() } catch (_: Exception) {}
                                },
                                colors = ButtonDefaults.colors(
                                    containerColor = if (usernameInput == acc.username) KuroNamiColors.Crimson else KuroNamiColors.SurfaceHigh,
                                    focusedContainerColor = KuroNamiColors.CrimsonContainer
                                ),
                                shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = acc.username,
                                    maxLines = 1,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // ── 3. TV Remote Navigable Input Fields ──
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { usernameInput = it },
                    label = { androidx.compose.material3.Text("Username or Email", color = KuroNamiColors.OnSurfaceVariant) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = {
                        try { passwordFocusRequester.requestFocus() } catch (_: Exception) {}
                    }),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = KuroNamiColors.Surface,
                        unfocusedContainerColor = KuroNamiColors.Surface,
                        focusedIndicatorColor = KuroNamiColors.Crimson,
                        unfocusedIndicatorColor = KuroNamiColors.Outline.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(usernameFocusRequester)
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                if (keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN) {
                                    try { passwordFocusRequester.requestFocus() } catch (_: Exception) {}
                                    return@onKeyEvent true
                                }
                            }
                            false
                        }
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    label = { androidx.compose.material3.Text("Password", color = KuroNamiColors.OnSurfaceVariant) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        viewModel.login(usernameInput, passwordInput)
                    }),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = KuroNamiColors.Surface,
                        unfocusedContainerColor = KuroNamiColors.Surface,
                        focusedIndicatorColor = KuroNamiColors.Crimson,
                        unfocusedIndicatorColor = KuroNamiColors.Outline.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(passwordFocusRequester)
                        .onKeyEvent { keyEvent ->
                            if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                when (keyEvent.nativeKeyEvent.keyCode) {
                                    android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                                        try { usernameFocusRequester.requestFocus() } catch (_: Exception) {}
                                        return@onKeyEvent true
                                    }
                                    android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                                        try { signInBtnFocusRequester.requestFocus() } catch (_: Exception) {}
                                        return@onKeyEvent true
                                    }
                                }
                            }
                            false
                        }
                )

                if (authError != null) {
                    Text(
                        text = authError ?: "",
                        color = KuroNamiColors.Crimson,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { viewModel.login(usernameInput, passwordInput) },
                        colors = ButtonDefaults.colors(
                            containerColor = KuroNamiColors.Crimson,
                            focusedContainerColor = KuroNamiColors.CrimsonContainer
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(signInBtnFocusRequester)
                            .onKeyEvent { keyEvent ->
                                if (keyEvent.nativeKeyEvent.action == android.view.KeyEvent.ACTION_DOWN) {
                                    if (keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP) {
                                        try { passwordFocusRequester.requestFocus() } catch (_: Exception) {}
                                        return@onKeyEvent true
                                    }
                                }
                                false
                            }
                    ) {
                        Text(if (isAuthLoading) "Signing in..." else "Sign In", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.register(usernameInput, null, passwordInput) },
                        colors = ButtonDefaults.colors(
                            containerColor = KuroNamiColors.SurfaceHigh,
                            focusedContainerColor = KuroNamiColors.SurfaceHighest
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(8.dp)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Register", fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = { viewModel.demoLogin() },
                        colors = ButtonDefaults.colors(
                            containerColor = KuroNamiColors.SurfaceHigh,
                            focusedContainerColor = KuroNamiColors.Crimson
                        ),
                        shape = ButtonDefaults.shape(RoundedCornerShape(8.dp))
                    ) {
                        Text("Demo", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}
