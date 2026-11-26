package com.aibuilt.ludokingcoinsfixed.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aibuilt.ludokingcoinsfixed.ui.screens.FileDetailScreen
import com.aibuilt.ludokingcoinsfixed.ui.screens.FilesScreen
import com.aibuilt.ludokingcoinsfixed.ui.screens.GuideScreen
import com.aibuilt.ludokingcoinsfixed.ui.screens.HistoryScreen
import com.aibuilt.ludokingcoinsfixed.ui.screens.PatchScreen
import com.aibuilt.ludokingcoinsfixed.ui.screens.TrackerScreen
import com.aibuilt.ludokingcoinsfixed.viewmodel.MainViewModel

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab("patch", "Patch", Icons.Filled.Build),
    Tab("files", "Files", Icons.Filled.Description),
    Tab("tracker", "Tracker", Icons.Filled.AccountBalanceWallet),
    Tab("guide", "Guide", Icons.Filled.MenuBook)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppRoot(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route.orEmpty()
    val onDetail = route.startsWith("file/") || route == "history"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titleFor(route)) },
                navigationIcon = {
                    if (onDetail) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = route == tab.route,
                        onClick = {
                            if (route != tab.route) {
                                navController.navigate(tab.route) {
                                    popUpTo("patch") { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "patch",
            modifier = Modifier.padding(padding)
        ) {
            composable("patch") { PatchScreen(viewModel) }
            composable("files") { FilesScreen(viewModel) { id -> navController.navigate("file/$id") } }
            composable("file/{id}") { entry ->
                FileDetailScreen(viewModel, entry.arguments?.getString("id").orEmpty())
            }
            composable("tracker") { TrackerScreen(viewModel) { navController.navigate("history") } }
            composable("history") { HistoryScreen(viewModel) }
            composable("guide") { GuideScreen() }
        }
    }
}

private fun titleFor(route: String): String = when {
    route.startsWith("file/") -> "Helper file"
    route == "files" -> "Build files"
    route == "tracker" -> "Coin tracker"
    route == "history" -> "History"
    route == "guide" -> "Build & install"
    else -> "Coins Fixed"
}
