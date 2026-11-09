package com.shiftpay.tracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.shiftpay.tracker.data.AppDatabase
import com.shiftpay.tracker.data.Repository
import com.shiftpay.tracker.ui.screens.AddShiftScreen
import com.shiftpay.tracker.ui.screens.HistoryScreen
import com.shiftpay.tracker.ui.screens.HomeScreen
import com.shiftpay.tracker.ui.screens.JobsScreen
import com.shiftpay.tracker.ui.screens.PaySummaryScreen
import com.shiftpay.tracker.ui.screens.SettingsScreen
import com.shiftpay.tracker.ui.screens.TemplatesScreen
import com.shiftpay.tracker.ui.theme.ShiftPayTrackerTheme
import com.shiftpay.tracker.ui.viewmodel.TrackerViewModel
import com.shiftpay.tracker.util.AdMobHelper
import com.shiftpay.tracker.util.AppOpenAdManager
import com.shiftpay.tracker.util.ConsentManager
import com.shiftpay.tracker.widget.WidgetUpdateWorker
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val appOpenAdManager = AppOpenAdManager()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // UMP consent first; ads initialise only after the consent flow
        // finishes (or fails — e.g. offline — in which case ads still start).
        ConsentManager.requestConsent(this) {
            AdMobHelper.initialize(this)
            appOpenAdManager.load(this)
        }
        // Daily widget refresh via WorkManager — no polling, no location.
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "widget-update",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WidgetUpdateWorker>(24, TimeUnit.HOURS).build()
        )
        setContent {
            ShiftPayTrackerTheme {
                val repo = Repository(AppDatabase.get(applicationContext), applicationContext)
                val vm: TrackerViewModel = viewModel(factory = TrackerViewModel.Factory(repo))
                AppNav(vm)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // App Open ad: optional, at most once per session, only after the
        // startup flow is ready — never blocking first paint.
        appOpenAdManager.showIfAvailable(this)
    }
}

private data class Tab(val route: String, val label: String)

private val tabs = listOf(
    Tab("home", "Home"),
    Tab("jobs", "Jobs"),
    Tab("history", "History"),
    Tab("pay", "Pay"),
    Tab("settings", "Settings")
)

@Composable
fun AppNav(vm: TrackerViewModel) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: "home"
    Scaffold(
        bottomBar = {
            NavigationBar {
                for (tab in tabs) {
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {},
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(padding)
        ) {
            composable("home") { HomeScreen(vm, onAddShift = { navController.navigate("addShift") }, onTemplates = { navController.navigate("templates") }) }
            composable("jobs") { JobsScreen(vm) }
            composable("history") { HistoryScreen(vm, onEdit = { id -> navController.navigate("addShift/$id") }) }
            composable("templates") { TemplatesScreen(vm) }
            composable("pay") { PaySummaryScreen(vm) }
            composable("settings") { SettingsScreen(vm) }
            composable("addShift") { AddShiftScreen(vm, shiftId = null, onDone = { navController.popBackStack() }) }
            composable(
                "addShift/{shiftId}",
                arguments = listOf(navArgument("shiftId") { type = NavType.LongType })
            ) { entry ->
                AddShiftScreen(vm, shiftId = entry.arguments?.getLong("shiftId"), onDone = { navController.popBackStack() })
            }
        }
    }
}
