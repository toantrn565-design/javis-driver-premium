package com.example.javisdriverpremium

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.javisdriverpremium.model.GpsQuality
import com.example.javisdriverpremium.model.ReportType
import com.example.javisdriverpremium.service.DrivingForegroundService
import com.example.javisdriverpremium.service.DrivingState
import com.example.javisdriverpremium.theme.JavisDriverPremiumTheme
import com.example.javisdriverpremium.ui.JavisDriverApp
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private var drivingService: DrivingForegroundService? = null
    private var isBound = false
    private val fallbackState = MutableStateFlow(DrivingState())

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as DrivingForegroundService.LocalBinder
            drivingService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            drivingService = null
            isBound = false
        }
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (!fineGranted && !coarseGranted) {
            Toast.makeText(
                this,
                "Cần quyền định vị để JAVIS Driver đo tốc độ và cảnh báo.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on while driving
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Request required permissions
        checkAndRequestPermissions()

        // Bind to background driving service
        val intent = Intent(this, DrivingForegroundService::class.java)
        startService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        setContent {
            JavisDriverPremiumTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val state by (drivingService?.drivingState ?: fallbackState).collectAsState()

                    JavisDriverApp(
                        drivingState = state,
                        onStartDrive = { useTestFixture ->
                            if (hasLocationPermission()) {
                                drivingService?.startDriving(useTestFixture)
                            } else {
                                checkAndRequestPermissions()
                            }
                        },
                        onEndDrive = {
                            drivingService?.stopDriving()
                        },
                        onReportSubmit = { reportType ->
                            drivingService?.submitReport(reportType)
                        },
                        onSimulateGps = { sample ->
                            drivingService?.processSimulatedGpsSample(sample)
                        }
                    )
                }
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        requestPermissionsLauncher.launch(permissions.toTypedArray())
    }

    override fun onDestroy() {
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        super.onDestroy()
    }
}
