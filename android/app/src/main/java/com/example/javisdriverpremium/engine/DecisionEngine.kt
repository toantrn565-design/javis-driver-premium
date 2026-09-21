package com.example.javisdriverpremium.engine

import com.example.javisdriverpremium.model.*
import kotlin.math.roundToInt

class DecisionEngine(
    private val roadMatcher: RoadMatcher = RoadMatcher(),
    private val maxAlertDistanceMeters: Float = 600f,
    private val minAlertDistanceMeters: Float = 40f,
    private val alertCooldownMs: Long = 120_000L // 2 minutes cooldown per alert ID
) {
    private val alertTriggerHistory = mutableMapOf<String, Long>()
    private var lastAnnouncedSpeedLimit: Int? = null
    private var lastMatchedSegmentId: String? = null
    private var lastMatchedSegment: RoadSegment? = null

    fun resetSession() {
        alertTriggerHistory.clear()
        lastAnnouncedSpeedLimit = null
        lastMatchedSegmentId = null
        lastMatchedSegment = null
    }

    /**
     * Evaluates the current GPS position against road and alert datasets.
     * Pure deterministic execution. Returns DecisionResult.
     */
    fun evaluate(
        gps: GpsSample,
        segments: List<RoadSegment>,
        alertPoints: List<AlertPoint>,
        currentTimeMs: Long = System.currentTimeMillis()
    ): DecisionResult {
        // 1. Match current road segment
        val matchedSegment = roadMatcher.matchSegment(gps, segments, lastMatchedSegment)
        val isNewSegment = matchedSegment != null && matchedSegment.id != lastMatchedSegmentId
        lastMatchedSegment = matchedSegment
        if (matchedSegment != null) {
            lastMatchedSegmentId = matchedSegment.id
        }

        val currentRoadName = matchedSegment?.name
        // Rule: Only verified speed limits are returned. Unknown returns null (UI displays "--")
        val currentSpeedLimit = if (matchedSegment != null &&
            matchedSegment.verificationStatus == VerificationStatus.VERIFIED
        ) {
            matchedSegment.speedLimit
        } else {
            null
        }

        // 2. Filter & find candidate alerts ahead
        val isMoving = gps.speedKmh >= 5.0f
        val candidateAlerts = mutableListOf<AlertCandidate>()
        var suppressedCandidate: AlertCandidate? = null
        var suppressionReason: String? = null

        for (alert in alertPoints) {
            val dist = GeoUtils.distanceMeters(gps.lat, gps.lng, alert.lat, alert.lng)

            // Check distance window
            if (dist < minAlertDistanceMeters || dist > maxAlertDistanceMeters) {
                continue
            }

            val headingDiff = if (alert.direction != null && isMoving) {
                GeoUtils.angleDifference(gps.bearing, alert.direction)
            } else {
                0f
            }

            val isAhead = if (isMoving) {
                GeoUtils.isPointAhead(gps.lat, gps.lng, gps.bearing, alert.lat, alert.lng)
            } else {
                true
            }

            val candidate = AlertCandidate(
                alertPoint = alert,
                distanceMeters = dist,
                headingDifferenceDegrees = headingDiff,
                isAhead = isAhead
            )

            // HARD RULE 1: Alert behind vehicle => MUST NOT TRIGGER
            if (isMoving && !isAhead) {
                suppressedCandidate = candidate
                suppressionReason = "ALERT_BEHIND_VEHICLE"
                continue
            }

            // HARD RULE 2: Opposite direction alert => MUST NOT TRIGGER
            if (alert.direction != null && isMoving) {
                if (!GeoUtils.isDirectionCompatible(gps.bearing, alert.direction, 60f)) {
                    suppressedCandidate = candidate
                    suppressionReason = "OPPOSITE_DIRECTION"
                    continue
                }
            }

            // HARD RULE 3: Unverified alert => NEVER ALERT AS VERIFIED
            if (alert.verificationStatus != VerificationStatus.VERIFIED) {
                suppressedCandidate = candidate
                suppressionReason = "UNVERIFIED_ALERT_DATA"
                continue
            }

            candidateAlerts.add(candidate)
        }

        // Sort candidates by closest distance first
        candidateAlerts.sortBy { it.distanceMeters }
        val nearestAlert = candidateAlerts.firstOrNull()

        // 3. Determine if voice alert should trigger (Deduplication + Cooldown + Priority)
        var ttsMessage: String? = null
        var ttsPriority = 0

        if (nearestAlert != null && isMoving) {
            val alertId = nearestAlert.alertPoint.id
            val lastTriggerTime = alertTriggerHistory[alertId] ?: 0L

            if (currentTimeMs - lastTriggerTime > alertCooldownMs) {
                val distRounded = (nearestAlert.distanceMeters / 50).roundToInt() * 50
                ttsMessage = when (nearestAlert.alertPoint.type) {
                    AlertType.SPEED_CAMERA -> {
                        if (nearestAlert.alertPoint.speedLimitKm != null) {
                            "Chú ý, camera bắn tốc độ ${nearestAlert.alertPoint.speedLimitKm} ki-lô-mét một giờ phía trước, cách $distRounded mét."
                        } else {
                            "Chú ý, camera phía trước cách $distRounded mét."
                        }
                    }
                    AlertType.RED_LIGHT_CAMERA -> {
                        "Chú ý, camera phạt nguội phía trước cách $distRounded mét."
                    }
                    AlertType.HAZARD -> {
                        "Chú ý, đoạn đường nguy hiểm phía trước."
                    }
                    AlertType.SPEED_LIMIT_CHANGE -> {
                        val limit = nearestAlert.alertPoint.speedLimitKm
                        if (limit != null) {
                            "Giới hạn tốc độ phía trước là $limit ki-lô-mét một giờ."
                        } else {
                            "Chú ý, giới hạn tốc độ đã thay đổi."
                        }
                    }
                    AlertType.OTHER -> "Chú ý phía trước."
                }
                ttsPriority = 100 // Highest priority
                alertTriggerHistory[alertId] = currentTimeMs
            } else {
                suppressedCandidate = nearestAlert
                suppressionReason = "COOLDOWN_ACTIVE"
            }
        }

        // 4. Announce road speed limit change ONLY on entering a new segment with different limit
        if (ttsMessage == null && isMoving && isNewSegment && currentSpeedLimit != null && currentSpeedLimit != lastAnnouncedSpeedLimit) {
            ttsMessage = "Giới hạn tốc độ đoạn này là $currentSpeedLimit ki-lô-mét một giờ."
            ttsPriority = 50
            lastAnnouncedSpeedLimit = currentSpeedLimit
        } else if (currentSpeedLimit != null && lastAnnouncedSpeedLimit == null) {
            // Silently sync initial speed limit so it doesn't interrupt later
            lastAnnouncedSpeedLimit = currentSpeedLimit
        }

        return DecisionResult(
            currentRoadName = currentRoadName,
            currentSpeedLimit = currentSpeedLimit,
            nextAlert = nearestAlert,
            ttsToSpeak = ttsMessage,
            ttsPriority = ttsPriority,
            suppressedAlert = suppressedCandidate,
            suppressionReason = suppressionReason
        )
    }
}
