package com.hyun.robot.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class DeviceDetailActivity : BaseActivity() {
    private lateinit var context: Context
    var deviceAddress = ""
    var deviceName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this
        
        // Fix: Replace deprecated systemUiVisibility with WindowInsetsControllerCompat
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        
        actionBar?.hide()
        deviceAddress = intent.getStringExtra("DEVICE_ADDRESS") ?: ""
        deviceName = intent.getStringExtra("DEVICE_NAME") ?: ""

        // Fix: Replace deprecated onBackPressed() with OnBackPressedDispatcher
        onBackPressedDispatcher.addCallback(this) {
            val intent = Intent(this@DeviceDetailActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                var hasPermissions by remember { mutableStateOf(false) }
                PermissionHandler(
                    onPermissionsGranted = { hasPermissions = true },
                    onPermissionsDenied = { }
                )
                ShowDetailScreen(this)
            }
        }
    }
}
