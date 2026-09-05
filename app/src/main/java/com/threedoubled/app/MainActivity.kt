package com.threedoubled.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.threedoubled.app.ui.screens.AnimationScreen
import com.threedoubled.app.ui.screens.BuilderScreen
import com.threedoubled.app.ui.screens.MaterialEditorScreen
import com.threedoubled.app.ui.screens.ModelLibraryScreen
import com.threedoubled.app.ui.theme.ThreeDoubleDTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ThreeDoubleDTheme {
                MainNav()
            }
        }
    }
}

sealed class Dest(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Builder : Dest("builder", "Studio", Icons.Filled.GridView)
    data object Library : Dest("library", "Library", Icons.Filled.PhotoLibrary)
    data object Materials : Dest("materials", "Materials", Icons.Filled.Palette)
    data object Animate : Dest("animate", "Animate", Icons.Filled.Movie)
}

private val destinations = listOf(Dest.Builder, Dest.Library, Dest.Materials, Dest.Animate)

@Composable
fun MainNav() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                destinations.forEach { dest ->
                    val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Dest.Builder.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Dest.Builder.route) { BuilderScreen() }
            composable(Dest.Library.route) { ModelLibraryScreen(models = emptyList(), selectedId = null, onSelect = {}) }
            composable(Dest.Materials.route) { MaterialEditorScreen(modelName = "Selected") }
            composable(Dest.Animate.route) { AnimationScreen(onExport = {}, exporting = false, lastPath = null) }
        }
    }
}
