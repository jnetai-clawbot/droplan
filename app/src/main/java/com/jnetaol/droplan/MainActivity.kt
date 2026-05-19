package com.jnetaol.droplan

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jnetaol.droplan.ui.screens.AppViewModel
import com.jnetaol.droplan.ui.screens.clipboard.ClipboardScreen
import com.jnetaol.droplan.ui.screens.home.HomeScreen
import com.jnetaol.droplan.ui.screens.pair.PairScreen
import com.jnetaol.droplan.ui.screens.settings.SettingsScreen
import com.jnetaol.droplan.ui.screens.transfer.TransferScreen
import com.jnetaol.droplan.ui.theme.DropLANTheme
import com.jnetaol.droplan.ui.theme.DLBackground

class MainActivity : ComponentActivity() {

    private val permissions = buildList {
        add(Manifest.permission.ACCESS_NETWORK_STATE)
        add(Manifest.permission.ACCESS_WIFI_STATE)
        add(Manifest.permission.CHANGE_WIFI_MULTICAST_STATE)
        if (Build.VERSION.SDK_INT <= 32) add(Manifest.permission.READ_EXTERNAL_STORAGE)
        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestPermissions()
        setContent {
            DropLANTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = DLBackground) {
                    val navController = rememberNavController()
                    val viewModel: AppViewModel = viewModel()
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") { HomeScreen(viewModel, { navController.navigate("transfer") }, { navController.navigate("clipboard") }, { navController.navigate("pair") }, { navController.navigate("settings") }) }
                        composable("transfer") { TransferScreen(viewModel, { navController.popBackStack() }) }
                        composable("clipboard") { ClipboardScreen(viewModel, { navController.popBackStack() }) }
                        composable("pair") { PairScreen(viewModel, { navController.popBackStack() }) }
                        composable("settings") { SettingsScreen(viewModel, { navController.popBackStack() }) }
                    }
                }
            }
        }
    }

    private fun requestPermissions() {
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isNotEmpty()) {
            requestPermissionLauncher.launch(notGranted.toTypedArray())
        }
    }
}
