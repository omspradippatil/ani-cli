package com.anicli.tv.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.anicli.tv.ui.details.DetailsScreen
import com.anicli.tv.ui.home.HomeScreen
import com.anicli.tv.ui.player.PlayerScreen
import com.anicli.tv.ui.search.SearchScreen
import com.anicli.tv.ui.settings.SettingsScreen
import com.anicli.tv.ui.splash.SplashScreen

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvApp() {
    Surface(modifier = Modifier.fillMaxSize()) {
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = "splash") {
            composable("splash") {
                SplashScreen(
                    onSplashFinished = {
                        navController.navigate("home") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                )
            }
            composable("home") {
                HomeScreen(
                    onMediaClick = { mediaId ->
                        navController.navigate("details/$mediaId")
                    },
                    onSearchClick = {
                        navController.navigate("search")
                    },
                    onSettingsClick = {
                        navController.navigate("settings")
                    }
                )
            }
            composable("settings") {
                SettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable("search") {
                SearchScreen(
                    onMediaClick = { mediaId ->
                        navController.navigate("details/$mediaId")
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("details/{mediaId}") { backStackEntry ->
                val mediaId = backStackEntry.arguments?.getString("mediaId") ?: return@composable
                DetailsScreen(
                    mediaId = mediaId,
                    onPlayClick = { episodeId ->
                        navController.navigate("player/$episodeId")
                    },
                    onBack = { navController.popBackStack() }
                )
            }
            composable("player/{episodeId}") { backStackEntry ->
                val episodeId = backStackEntry.arguments?.getString("episodeId") ?: return@composable
                PlayerScreen(
                    episodeId = episodeId,
                    onNextEpisode = { nextEpId ->
                        navController.popBackStack()
                        navController.navigate("player/$nextEpId")
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
