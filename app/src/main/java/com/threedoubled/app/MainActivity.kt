package com.threedoubled.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.threedoubled.app.data.ModelRepository
import com.threedoubled.app.export.ExportManager
import com.threedoubled.app.ui.screens.BuilderScreen
import com.threedoubled.app.ui.screens.MaterialEditorScreen
import com.threedoubled.app.ui.screens.ModelLibraryScreen
import com.threedoubled.app.ui.theme.ThreeDoubleDTheme
import com.threedoubled.app.ui.viewmodel.ExportViewModel
import com.threedoubled.app.ui.viewmodel.StudioViewModel

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

private data class Dest(val route: String, val label: String, val icon: ImageVector)

private val destinations = listOf(
    Dest("studio", "Studio", Icons.Filled.GridView),
    Dest("library", "Library", Icons.Filled.PhotoLibrary),
    Dest("materials", "Materials", Icons.Filled.Palette)
)

@Composable
fun MainNav() {
    val context = LocalContext.current
    val repository = remember { ModelRepository(context.applicationContext) }
    val studioViewModel: StudioViewModel = viewModel(
        factory = StudioViewModel.factory(repository)
    )
    val exportViewModel: ExportViewModel = viewModel(
        factory = ExportViewModel.factory(ExportManager(context.applicationContext))
    )

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
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
            startDestination = "studio",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable("studio") {
                BuilderScreen(
                    viewModel = studioViewModel,
                    exportViewModel = exportViewModel,
                    repository = repository
                )
            }
            composable("library") {
                ModelLibraryScreen(viewModel = studioViewModel)
            }
            composable("materials") {
                MaterialEditorScreen(viewModel = studioViewModel)
            }
        }
    }
}
