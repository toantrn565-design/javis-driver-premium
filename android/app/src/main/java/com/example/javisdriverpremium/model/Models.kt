package com.example.javisdriverpremium.model

import kotlinx.serialization.Serializable

@Serializable
enum class VerificationStatus {
    VERIFIED,
    UNVERIFIED,
    UNKNOWN
}

@Serializable
enum class AlertType {
    SPEED_CAMERA,
    RED_LIGHT_CAMERA,
    HAZARD,
    SPEED_LIMIT_CHANGE,
    OTHER
}

@Serializable
enum class ReportType {
    CAMERA,
    SPEED_WRONG,
    HAZARD,
    OTHER
}

@Serializable
enum class GpsQuality {
    EXCELLENT,   // accuracy <= 10m
    ACCEPTABLE,  // 10m < accuracy <= 25m
    WEAK,        // accuracy > 25m
    LOST,        // no GPS
    PERMISSION_DENIED
}

@Serializable
data class GeoPoint(
    val lat: Double,
    val lng: Double
)

@Serializable
data class RoadSegment(
    val id: String,
    val name: String,
    val points: List<GeoPoint>,
    val direction: Float? = null, // Compass bearing in degrees (0..359), null if bidirectional
    val speedLimit: Int? = null,  // In km/h. null if unknown
    val verificationStatus: VerificationStatus = VerificationStatus.UNKNOWN,
    val source: String = "JAVIS_CORRIDOR_V0.1",
    val lastVerified: Long = 0L,
    val isTestFixture: Boolean = false
)

@Serializable
data class AlertPoint(
    val id: String,
    val lat: Double,
    val lng: Double,
    val type: AlertType,
    val direction: Float? = null, // Required heading direction to trigger (e.g. 90 deg). Null = omnidirectional
    val segmentId: String? = null,
    val speedLimitKm: Int? = null,
    val verificationStatus: VerificationStatus = VerificationStatus.UNKNOWN,
    val source: String = "JAVIS_CORRIDOR_V0.1",
    val lastVerified: Long = 0L,
    val isTestFixture: Boolean = false
)

@Serializable
data class GpsSample(
    val lat: Double,
    val lng: Double,
    val speedKmh: Float,
    val bearing: Float,
    val accuracy: Float,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class Report(
    val id: String,
    val lat: Double,
    val lng: Double,
    val heading: Float,
    val speed: Float,
    val accuracy: Float,
    val type: ReportType,
    val note: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
enum class TripEventType {
    TRIP_START,
    TRIP_END,
    GPS_SAMPLE,
    ALERT_TRIGGERED,
    ALERT_SUPPRESSED,
    REPORT_SUBMITTED,
    GPS_ACCURACY_DEGRADED,
    GPS_LOST,
    GPS_RECOVERED,
    ERROR
}

@Serializable
data class TripEvent(
    val tripId: String,
    val eventType: TripEventType,
    val timestamp: Long = System.currentTimeMillis(),
    val lat: Double? = null,
    val lng: Double? = null,
    val speedKmh: Float? = null,
    val bearing: Float? = null,
    val accuracy: Float? = null,
    val message: String? = null,
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
data class TripSummary(
    val tripId: String,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val totalSamples: Int,
    val maxSpeedKmh: Float,
    val averageSpeedKmh: Float,
    val alertsTriggeredCount: Int,
    val alertsSuppressedCount: Int,
    val reportsSubmittedCount: Int,
    val gpsWeakCount: Int,
    val events: List<TripEvent> = emptyList(),
    val reports: List<Report> = emptyList()
)

@Serializable
data class AlertCandidate(
    val alertPoint: AlertPoint,
    val distanceMeters: Float,
    val headingDifferenceDegrees: Float,
    val isAhead: Boolean
)

@Serializable
data class DecisionResult(
    val currentRoadName: String?,
    val currentSpeedLimit: Int?, // null means unknown -> displays "--"
    val nextAlert: AlertCandidate?,
    val ttsToSpeak: String?,
    val ttsPriority: Int = 0,    // higher = more critical
    val suppressedAlert: AlertCandidate? = null,
    val suppressionReason: String? = null
)
