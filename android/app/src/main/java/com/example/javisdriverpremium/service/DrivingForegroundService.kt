package com.example.javisdriverpremium.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.javisdriverpremium.MainActivity
import com.example.javisdriverpremium.data.TestCorridorRepository
import com.example.javisdriverpremium.engine.DecisionEngine
import com.example.javisdriverpremium.evidence.TripEvidenceManager
import com.example.javisdriverpremium.model.*
import com.example.javisdriverpremium.tts.VietnameseTtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class DrivingForegroundService : Service(), LocationListener {

    private val binder = LocalBinder()
    private lateinit var locationManager: LocationManager
    private lateinit var decisionEngine: DecisionEngine
    private lateinit var ttsManager: VietnameseTtsManager
    private lateinit var evidenceManager: TripEvidenceManager

    private val _drivingState = MutableStateFlow(DrivingState())
    val drivingState: StateFlow<DrivingState> = _drivingState.asStateFlow()

    private var activeCorridorSegments: List<RoadSegment> = emptyList()
    private var activeAlertPoints: List<AlertPoint> = emptyList()
    private var isSimulatedMode = false

    inner class LocalBinder : Binder() {
        fun getService(): DrivingForegroundService = this@DrivingForegroundService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        decisionEngine = DecisionEngine()
        ttsManager = VietnameseTtsManager(applicationContext)
        evidenceManager = TripEvidenceManager(applicationContext)

        // Load production corridor data (empty/unknown by default unless corridor configured)
        activeCorridorSegments = TestCorridorRepository.getProductionCorridor()
        activeAlertPoints = TestCorridorRepository.getProductionAlertPoints()

        createNotificationChannel()
    }

    fun startDriving(useTestFixture: Boolean = false): String {
        isSimulatedMode = useTestFixture
        if (useTestFixture) {
            val fixture = TestCorridorRepository.getTestCorridorFixture()
            activeCorridorSegments = fixture.first
            activeAlertPoints = fixture.second
        } else {
            activeCorridorSegments = TestCorridorRepository.getProductionCorridor()
            activeAlertPoints = TestCorridorRepository.getProductionAlertPoints()
        }

        decisionEngine.resetSession()
        val tripId = evidenceManager.startTrip()

        startForegroundServiceWithNotification()
        startLocationUpdates()

        _drivingState.value = _drivingState.value.copy(
            isDriving = true,
            tripId = tripId,
            gpsStatus = GpsQuality.ACCEPTABLE,
            isTestFixtureLoaded = useTestFixture
        )

        ttsManager.speak("JAVIS Driver đã sẵn sàng. Chúc bạn một chuyến đi an toàn.", force = true)
        return tripId
    }

    fun stopDriving(): TripSummary? {
        stopLocationUpdates()
        val summary = evidenceManager.endTrip()

        _drivingState.value = _drivingState.value.copy(
            isDriving = false,
            currentSpeedKmh = 0f,
            nextAlert = null,
            lastSummary = summary
        )

        ttsManager.speak("Đã kết thúc chuyến đi. Cảm ơn bạn.", force = true)
        stopForeground(STOP_FOREGROUND_REMOVE)
        return summary
    }

    fun submitReport(type: ReportType, note: String = ""): Report {
        val currentState = _drivingState.value
        val report = Report(
            id = "REP_" + UUID.randomUUID().toString().take(8),
            lat = currentState.currentLat,
            lng = currentState.currentLng,
            heading = currentState.bearing,
            speed = currentState.currentSpeedKmh,
            accuracy = currentState.accuracy,
            type = type,
            note = note,
            timestamp = System.currentTimeMillis()
        )

        evidenceManager.logReport(report)
        ttsManager.speak("Đã ghi nhận báo cáo ${getReportTypeName(type)}.", force = true)
        return report
    }

    private fun getReportTypeName(type: ReportType): String = when (type) {
        ReportType.CAMERA -> "camera"
        ReportType.SPEED_WRONG -> "tốc độ sai"
        ReportType.HAZARD -> "nguy hiểm"
        ReportType.OTHER -> "sự cố khác"
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        try {
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (isGpsEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000L, // 1 second
                    0f,
                    this
                )
            } else if (isNetworkEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    1000L,
                    0f,
                    this
                )
            } else {
                _drivingState.value = _drivingState.value.copy(gpsStatus = GpsQuality.LOST)
            }
        } catch (e: Exception) {
            Log.e("JAVIS_SERVICE", "Error requesting location updates", e)
            _drivingState.value = _drivingState.value.copy(gpsStatus = GpsQuality.PERMISSION_DENIED)
        }
    }

    private fun stopLocationUpdates() {
        try {
            locationManager.removeUpdates(this)
        } catch (e: Exception) {
            Log.e("JAVIS_SERVICE", "Error removing location updates", e)
        }
    }

    override fun onLocationChanged(location: Location) {
        val speedKmh = if (location.hasSpeed()) (location.speed * 3.6f) else 0f
        val bearing = if (location.hasBearing()) location.bearing else 0f
        val accuracy = location.accuracy

        val gpsQuality = when {
            accuracy <= 10f -> GpsQuality.EXCELLENT
            accuracy <= 25f -> GpsQuality.ACCEPTABLE
            else -> GpsQuality.WEAK
        }

        val sample = GpsSample(
            lat = location.latitude,
            lng = location.longitude,
            speedKmh = speedKmh,
            bearing = bearing,
            accuracy = accuracy,
            timestamp = location.time
        )

        evidenceManager.logGpsSample(sample)

        // Evaluate driving decision
        val decision = decisionEngine.evaluate(sample, activeCorridorSegments, activeAlertPoints)

        // Handle voice alert
        if (decision.ttsToSpeak != null) {
            ttsManager.speak(decision.ttsToSpeak)
            decision.nextAlert?.let {
                evidenceManager.logAlertTriggered(it, decision.ttsToSpeak)
            }
        }

        // Handle suppressed alert logging
        if (decision.suppressedAlert != null && decision.suppressionReason != null) {
            evidenceManager.logAlertSuppressed(decision.suppressedAlert, decision.suppressionReason)
        }

        // Update state flow for Compose UI
        _drivingState.value = _drivingState.value.copy(
            currentLat = location.latitude,
            currentLng = location.longitude,
            currentSpeedKmh = speedKmh,
            bearing = bearing,
            accuracy = accuracy,
            gpsStatus = gpsQuality,
            currentRoad = decision.currentRoadName,
            speedLimit = decision.currentSpeedLimit,
            nextAlert = decision.nextAlert,
            lastDecision = decision
        )

        updateNotification(speedKmh, decision.currentSpeedLimit, decision.currentRoadName)
    }

    /**
     * Manually feeds a simulated GPS sample (used for automated tests & simulation).
     */
    fun processSimulatedGpsSample(sample: GpsSample) {
        evidenceManager.logGpsSample(sample)
        val decision = decisionEngine.evaluate(sample, activeCorridorSegments, activeAlertPoints, sample.timestamp)

        if (decision.ttsToSpeak != null) {
            ttsManager.speak(decision.ttsToSpeak)
            decision.nextAlert?.let {
                evidenceManager.logAlertTriggered(it, decision.ttsToSpeak)
            }
        }

        if (decision.suppressedAlert != null && decision.suppressionReason != null) {
            evidenceManager.logAlertSuppressed(decision.suppressedAlert, decision.suppressionReason)
        }

        _drivingState.value = _drivingState.value.copy(
            currentLat = sample.lat,
            currentLng = sample.lng,
            currentSpeedKmh = sample.speedKmh,
            bearing = sample.bearing,
            accuracy = sample.accuracy,
            gpsStatus = if (sample.accuracy <= 10f) GpsQuality.EXCELLENT else GpsQuality.ACCEPTABLE,
            currentRoad = decision.currentRoadName,
            speedLimit = decision.currentSpeedLimit,
            nextAlert = decision.nextAlert,
            lastDecision = decision
        )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "JAVIS Driver Session",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Theo dõi tốc độ và cảnh báo giao thông trực tiếp"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceWithNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JAVIS Driver — Đang chạy")
            .setContentText("Theo dõi tốc độ và cảnh báo thời gian thực")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(speed: Float, speedLimit: Int?, roadName: String?) {
        val speedLimitText = speedLimit?.let { "$it km/h" } ?: "--"
        val roadText = roadName ?: "Đường không xác định"
        val content = "Tốc độ: ${speed.toInt()} km/h | Giới hạn: $speedLimitText | $roadText"

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JAVIS Driver — Đang lái xe")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        stopLocationUpdates()
        ttsManager.shutdown()
        super.onDestroy()
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {
        _drivingState.value = _drivingState.value.copy(gpsStatus = GpsQuality.LOST)
    }
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    companion object {
        const val CHANNEL_ID = "javis_driver_session_channel"
        const val NOTIFICATION_ID = 2026
    }
}

data class DrivingState(
    val isDriving: Boolean = false,
    val tripId: String? = null,
    val currentLat: Double = 0.0,
    val currentLng: Double = 0.0,
    val currentSpeedKmh: Float = 0f,
    val bearing: Float = 0f,
    val accuracy: Float = 0f,
    val gpsStatus: GpsQuality = GpsQuality.LOST,
    val currentRoad: String? = null,
    val speedLimit: Int? = null,
    val nextAlert: AlertCandidate? = null,
    val lastDecision: DecisionResult? = null,
    val lastSummary: TripSummary? = null,
    val isTestFixtureLoaded: Boolean = false
)
