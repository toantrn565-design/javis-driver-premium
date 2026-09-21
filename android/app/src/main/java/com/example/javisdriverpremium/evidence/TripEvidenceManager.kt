package com.example.javisdriverpremium.evidence

import android.content.Context
import android.util.Log
import com.example.javisdriverpremium.model.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class TripEvidenceManager(private val context: Context) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private var currentTripId: String? = null
    private var tripStartTime: Long = 0L
    private val currentEvents = mutableListOf<TripEvent>()
    private val currentReports = mutableListOf<Report>()
    private val speedSamples = mutableListOf<Float>()
    private var weakGpsCount = 0
    private var triggeredAlertsCount = 0
    private var suppressedAlertsCount = 0

    private val tripsDir: File
        get() = File(context.filesDir, "trips").apply { if (!exists()) mkdirs() }

    fun isTripActive(): Boolean = currentTripId != null

    fun getCurrentTripId(): String? = currentTripId

    /**
     * Starts a new trip session.
     */
    fun startTrip(): String {
        val tripId = "TRIP_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + "_" + UUID.randomUUID().toString().take(6)
        currentTripId = tripId
        tripStartTime = System.currentTimeMillis()
        currentEvents.clear()
        currentReports.clear()
        speedSamples.clear()
        weakGpsCount = 0
        triggeredAlertsCount = 0
        suppressedAlertsCount = 0

        val startEvent = TripEvent(
            tripId = tripId,
            eventType = TripEventType.TRIP_START,
            timestamp = tripStartTime,
            message = "Trip started by driver"
        )
        currentEvents.add(startEvent)
        Log.i("JAVIS_EVIDENCE", "Trip started: $tripId")
        return tripId
    }

    /**
     * Logs GPS sample.
     */
    fun logGpsSample(gps: GpsSample) {
        val tripId = currentTripId ?: return
        speedSamples.add(gps.speedKmh)

        if (gps.accuracy > 25.0f) {
            weakGpsCount++
            currentEvents.add(
                TripEvent(
                    tripId = tripId,
                    eventType = TripEventType.GPS_ACCURACY_DEGRADED,
                    lat = gps.lat,
                    lng = gps.lng,
                    speedKmh = gps.speedKmh,
                    bearing = gps.bearing,
                    accuracy = gps.accuracy,
                    message = "GPS accuracy degraded to ${gps.accuracy}m"
                )
            )
        }
    }

    /**
     * Logs triggered alert event.
     */
    fun logAlertTriggered(alert: AlertCandidate, ttsMessage: String) {
        val tripId = currentTripId ?: return
        triggeredAlertsCount++
        currentEvents.add(
            TripEvent(
                tripId = tripId,
                eventType = TripEventType.ALERT_TRIGGERED,
                lat = alert.alertPoint.lat,
                lng = alert.alertPoint.lng,
                message = ttsMessage,
                metadata = mapOf(
                    "alertId" to alert.alertPoint.id,
                    "type" to alert.alertPoint.type.name,
                    "distanceMeters" to alert.distanceMeters.toString(),
                    "speedLimitKm" to (alert.alertPoint.speedLimitKm?.toString() ?: "--")
                )
            )
        )
    }

    /**
     * Logs suppressed alert event with reason (critical for evidence & QA).
     */
    fun logAlertSuppressed(alert: AlertCandidate, reason: String) {
        val tripId = currentTripId ?: return
        suppressedAlertsCount++
        currentEvents.add(
            TripEvent(
                tripId = tripId,
                eventType = TripEventType.ALERT_SUPPRESSED,
                lat = alert.alertPoint.lat,
                lng = alert.alertPoint.lng,
                message = "Alert suppressed: $reason",
                metadata = mapOf(
                    "alertId" to alert.alertPoint.id,
                    "type" to alert.alertPoint.type.name,
                    "reason" to reason,
                    "headingDifference" to alert.headingDifferenceDegrees.toString(),
                    "isAhead" to alert.isAhead.toString()
                )
            )
        )
    }

    /**
     * Records driver report (Camera, Speed wrong, Hazard, Other).
     */
    fun logReport(report: Report) {
        val tripId = currentTripId ?: return
        currentReports.add(report)
        currentEvents.add(
            TripEvent(
                tripId = tripId,
                eventType = TripEventType.REPORT_SUBMITTED,
                lat = report.lat,
                lng = report.lng,
                speedKmh = report.speed,
                bearing = report.heading,
                accuracy = report.accuracy,
                message = "Driver report submitted: ${report.type}",
                metadata = mapOf(
                    "reportId" to report.id,
                    "reportType" to report.type.name,
                    "note" to report.note
                )
            )
        )
    }

    /**
     * Ends current trip and saves evidence JSON file locally.
     */
    fun endTrip(): TripSummary? {
        val tripId = currentTripId ?: return null
        val endTime = System.currentTimeMillis()
        val durationSec = (endTime - tripStartTime) / 1000

        val maxSpeed = if (speedSamples.isNotEmpty()) speedSamples.maxOrNull() ?: 0f else 0f
        val avgSpeed = if (speedSamples.isNotEmpty()) speedSamples.average().toFloat() else 0f

        val endEvent = TripEvent(
            tripId = tripId,
            eventType = TripEventType.TRIP_END,
            timestamp = endTime,
            message = "Trip ended by driver. Duration: ${durationSec}s, Avg Speed: ${avgSpeed.toInt()} km/h"
        )
        currentEvents.add(endEvent)

        val summary = TripSummary(
            tripId = tripId,
            startTime = tripStartTime,
            endTime = endTime,
            durationSeconds = durationSec,
            totalSamples = speedSamples.size,
            maxSpeedKmh = maxSpeed,
            averageSpeedKmh = avgSpeed,
            alertsTriggeredCount = triggeredAlertsCount,
            alertsSuppressedCount = suppressedAlertsCount,
            reportsSubmittedCount = currentReports.size,
            gpsWeakCount = weakGpsCount,
            events = currentEvents.toList(),
            reports = currentReports.toList()
        )

        // Save evidence to file
        try {
            val file = File(tripsDir, "${tripId}.json")
            val jsonString = json.encodeToString(summary)
            file.writeText(jsonString)
            Log.i("JAVIS_EVIDENCE", "Trip evidence persisted: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("JAVIS_EVIDENCE", "Failed to persist trip evidence", e)
        }

        currentTripId = null
        return summary
    }

    /**
     * Lists all saved trip evidence files.
     */
    fun getAllTripFiles(): List<File> {
        return tripsDir.listFiles { file -> file.extension == "json" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
}
