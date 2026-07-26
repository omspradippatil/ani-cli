package com.anicli.tv.ui.mobile

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Composable
fun MobileApp() {
    val navController = rememberNavController()

    val navItems = listOf(
        Triple("home", "Home", Icons.Filled.Home),
        Triple("search", "Search", Icons.Filled.Search),
        Triple("settings", "Settings", Icons.Filled.Settings)
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            // Only show bottom bar on top level destinations
            val showBottomBar = currentDestination?.hierarchy?.any { 
                it.route in listOf("home", "search", "settings") 
            } == true
            
            if (showBottomBar) {
                NavigationBar {
                    navItems.forEach { (route, label, icon) ->
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                            selected = currentDestination?.hierarchy?.any { it.route == route } == true,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                MobileHomeScreen(
                    onMediaClick = { mediaId ->
                        navController.navigate("details/$mediaId")
                    }
                )
            }
            composable("search") {
                MobileSearchScreen(
                    onMediaClick = { mediaId ->
                        navController.navigate("details/$mediaId")
                    }
                )
            }
            composable("settings") {
                MobileSettingsScreen()
            }
            composable("details/{mediaId}") { backStackEntry ->
                val mediaId = backStackEntry.arguments?.getString("mediaId") ?: return@composable
                MobileDetailsScreen(
                    mediaId = mediaId,
                    onPlayClick = { episodeId ->
                        navController.navigate("player/$episodeId")
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
            composable("player/{episodeId}") { backStackEntry ->
                val episodeId = backStackEntry.arguments?.getString("episodeId") ?: return@composable
                MobilePlayerScreen(
                    episodeId = episodeId,
                    onNextEpisode = { nextEpId ->
                        navController.popBackStack()
                        navController.navigate("player/$nextEpId")
                    },
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
