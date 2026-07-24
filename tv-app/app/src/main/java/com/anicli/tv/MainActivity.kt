package com.anicli.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.anicli.tv.core.theme.AniCliTvTheme
import com.anicli.tv.ui.details.DetailsScreen
import com.anicli.tv.ui.home.HomeScreen
import com.anicli.tv.ui.search.SearchScreen
import com.anicli.tv.ui.player.PlayerScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AniCliTvTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                onMediaClick = { mediaId ->
                                    navController.navigate("details/$mediaId")
                                },
                                onSearchClick = {
                                    navController.navigate("search")
                                }
                            )
                        }
                        composable("search") {
                            SearchScreen(
                                onMediaClick = { mediaId ->
                                    navController.navigate("details/$mediaId")
                                }
                            )
                        }
                        composable("details/{mediaId}") { backStackEntry ->
                            val mediaId = backStackEntry.arguments?.getString("mediaId") ?: return@composable
                            DetailsScreen(
                                mediaId = mediaId,
                                onPlayClick = { episodeId ->
                                    navController.navigate("player/$episodeId")
                                }
                            )
                        }
                        composable("player/{episodeId}") { backStackEntry ->
                            val episodeId = backStackEntry.arguments?.getString("episodeId") ?: return@composable
                            PlayerScreen(episodeId = episodeId)
                        }
                    }
                }
            }
        }
    }
}
