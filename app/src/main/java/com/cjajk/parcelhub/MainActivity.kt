package com.cjajk.parcelhub

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cjajk.parcelhub.ui.theme.ParcelHubTheme
import com.cjajk.parcelhub.ui.screen.MainScreen
import com.cjajk.parcelhub.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val perms = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val need = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (need.isNotEmpty()) permLauncher.launch(need.toTypedArray())

        setContent {
            ParcelHubTheme {
                val vm: MainViewModel = viewModel()
                MainScreen(vm)
            }
        }
    }
}
