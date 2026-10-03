package com.example.revgfw

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.ViewModelProvider
import com.example.revgfw.data.AppRepository
import com.example.revgfw.ui.MainViewModel
import com.example.revgfw.ui.screens.AppDetailScreen
import com.example.revgfw.ui.screens.MonitorScreen
import com.example.revgfw.ui.screens.ReportScreen
import com.example.revgfw.ui.screens.ScanScreen
import com.example.revgfw.ui.screens.SettingsScreen
import com.example.revgfw.ui.theme.RevGfwTheme

class MainActivity : ComponentActivity() {

    private val vm: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(AppRepository(applicationContext)) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RevGfwTheme {
                AppRoot(vm)
            }
        }
    }
}

/** 底部导航目的地定义 */
private data class Dest(val route: String, val label: String, val icon: ImageVector)

private val DESTINATIONS = listOf(
    Dest("scan", "扫描", Icons.Filled.Security),
    Dest("monitor", "监控", Icons.Filled.MonitorHeart),
    Dest("report", "报告", Icons.Filled.Assessment),
    Dest("settings", "设置", Icons.Filled.Settings),
)

@Composable
fun AppRoot(vm: MainViewModel) {
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current

    val message by vm.message.collectAsState()
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                DESTINATIONS.forEach { d ->
                    val selected = currentRoute?.hierarchy?.any { it.route == d.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            nav.navigate(d.route) {
                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(d.icon, contentDescription = d.label) },
                        label = { Text(d.label) },
                    )
                }
            }
        },
    ) { inner ->
        NavHost(
            navController = nav,
            startDestination = "scan",
            modifier = Modifier.padding(inner),
        ) {
            composable("scan") { ScanScreen(vm, onOpenDetail = { nav.navigate("detail") }) }
            composable("detail") { AppDetailScreen(vm, onBack = { nav.popBackStack() }) }
            composable("monitor") { MonitorScreen(vm) }
            composable("report") { ReportScreen(vm) }
            composable("settings") { SettingsScreen(vm) }
        }
    }
}