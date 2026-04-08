package com.example.greenfocus

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.greenfocus.ui.theme.GreenFocusTheme
import com.example.greenfocus.util.PermissionManager
import android.provider.Settings
import androidx.compose.runtime.mutableStateMapOf
import androidx.core.net.toUri
import com.example.greenfocus.ui.screens.PermissionTestScreen
import android.os.Build

class MainActivity : ComponentActivity() {

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) toast("Camera OK") else toast("Camera bị từ chối")
    }

    private val notiLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) toast("Thông báo OK") else toast("Thông báo bị từ chối")
    }

    private val permissionStatuses = mutableStateMapOf<String, Boolean>()


    fun testPermissions() {
        val context = this

        // Test Camera
        if (!PermissionManager.hasCameraPermission(context)) {
            cameraLauncher.launch(Manifest.permission.CAMERA)
        }

        // Test Special: Overlay
        if (!PermissionManager.hasOverlayPermission(context)) {
            val intent =
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri())
            startActivity(intent)
        }

        // Test Special: Usage Stats
        if (!PermissionManager.hasUsageStatsPermission(context)) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GreenFocusTheme {
                Scaffold { innerPadding ->
                    // GỌI MÀN HÌNH CỦA ÔNG Ở ĐÂY
                    PermissionTestScreen(
                        modifier = Modifier.padding(innerPadding),
                        statuses = permissionStatuses,
                        onCheckAll = { updatePermissionStatuses() },
                        onRequestCamera = { cameraLauncher.launch(Manifest.permission.CAMERA) },
                        onRequestOverlay = { requestOverlay() },
                        onRequestUsage = { requestUsage() },
                        onRequestNotification = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notiLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                toast("Android phiên bản cũ không cần xin quyền này")
                            }
                        }
                    )
                }
            }
        }
    }

    private fun updatePermissionStatuses() {
        permissionStatuses["Camera"] = PermissionManager.hasCameraPermission(this)
        permissionStatuses["Notification"] = PermissionManager.hasNotificationPermission(this)
        permissionStatuses["Overlay"] = PermissionManager.hasOverlayPermission(this)
        permissionStatuses["Usage Stats"] = PermissionManager.hasUsageStatsPermission(this)
    }

    override fun onResume() {
        super.onResume()
        updatePermissionStatuses()
    }

    private fun requestOverlay() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:$packageName".toUri())
        startActivity(intent)
    }

    private fun requestUsage() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    GreenFocusTheme {
        Greeting("Android")
    }
}

