package com.example.javisdriverpremium.engine

import com.example.javisdriverpremium.model.GpsSample
import com.example.javisdriverpremium.model.RoadSegment
import com.example.javisdriverpremium.model.VerificationStatus

class RoadMatcher(
    private val maxSnapDistanceMeters: Float = 35f,
    private val maxHeadingDifferenceDegrees: Float = 50f
) {
    /**
     * Matches a GPS sample to the most relevant RoadSegment in the corridor.
     */
    fun matchSegment(
        gps: GpsSample,
        segments: List<RoadSegment>,
        previousSegment: RoadSegment? = null
    ): RoadSegment? {
        if (segments.isEmpty()) return null

        val isMoving = gps.speedKmh >= 3.0f
        var bestSegment: RoadSegment? = null
        var bestDistance = Float.MAX_VALUE

        for (segment in segments) {
            if (segment.points.size < 2) continue

            // 1. Check direction compatibility if segment is unidirectional and vehicle is moving
            if (segment.direction != null && isMoving) {
                if (!GeoUtils.isDirectionCompatible(gps.bearing, segment.direction, maxHeadingDifferenceDegrees)) {
                    continue // Reject opposite or perpendicular direction
                }
            }

            // 2. Find minimum distance from user point to any line segment in the road polyline
            var minDistanceToPolyline = Float.MAX_VALUE
            for (i in 0 until segment.points.size - 1) {
                val p1 = segment.points[i]
                val p2 = segment.points[i + 1]
                val dist = GeoUtils.distanceToSegmentMeters(
                    gps.lat, gps.lng,
                    p1.lat, p1.lng,
                    p2.lat, p2.lng
                )
                if (dist < minDistanceToPolyline) {
                    minDistanceToPolyline = dist
                }
            }

            // 3. Candidate evaluation
            if (minDistanceToPolyline <= maxSnapDistanceMeters) {
                // Bias slightly towards maintaining previous segment to prevent rapid flapping
                val adjustedDistance = if (previousSegment?.id == segment.id) {
                    minDistanceToPolyline * 0.8f
                } else {
                    minDistanceToPolyline
                }

                if (adjustedDistance < bestDistance) {
                    bestDistance = adjustedDistance
                    bestSegment = segment
                }
            }
        }

        return bestSegment
    }
}
