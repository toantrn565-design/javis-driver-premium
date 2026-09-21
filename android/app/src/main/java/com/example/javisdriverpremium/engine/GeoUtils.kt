package com.example.javisdriverpremium.engine

import com.example.javisdriverpremium.model.GeoPoint
import kotlin.math.*

object GeoUtils {
    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Computes great-circle distance between two points in meters using Haversine formula.
     */
    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLng / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (EARTH_RADIUS_METERS * c).toFloat()
    }

    fun distanceMeters(p1: GeoPoint, p2: GeoPoint): Float {
        return distanceMeters(p1.lat, p1.lng, p2.lat, p2.lng)
    }

    /**
     * Calculates initial compass bearing (0..359.99 degrees) from point 1 to point 2.
     */
    fun initialBearing(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lng2 - lng1)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        val theta = atan2(y, x)
        val bearing = (Math.toDegrees(theta) + 360.0) % 360.0
        return bearing.toFloat()
    }

    /**
     * Computes the smallest absolute angle difference between two bearings in degrees (0..180).
     */
    fun angleDifference(b1: Float, b2: Float): Float {
        val diff = abs(b1 - b2) % 360f
        return if (diff > 180f) 360f - diff else diff
    }

    /**
     * Checks if target point is in front of the vehicle heading.
     * Uses dot product between heading vector and vector towards target.
     * @param headingDifferenceMaxDegrees Maximum angle offset from forward vector (default 75 degrees)
     */
    fun isPointAhead(
        userLat: Double,
        userLng: Double,
        userBearing: Float,
        targetLat: Double,
        targetLng: Double,
        headingDifferenceMaxDegrees: Float = 75f
    ): Boolean {
        val bearingToTarget = initialBearing(userLat, userLng, targetLat, targetLng)
        val diff = angleDifference(userBearing, bearingToTarget)
        return diff <= headingDifferenceMaxDegrees
    }

    /**
     * Checks if vehicle heading is directionally compatible with a road/alert direction.
     * Threshold is typically 45 to 60 degrees.
     */
    fun isDirectionCompatible(
        vehicleBearing: Float,
        targetDirection: Float,
        thresholdDegrees: Float = 50f
    ): Boolean {
        val diff = angleDifference(vehicleBearing, targetDirection)
        return diff <= thresholdDegrees
    }

    /**
     * Calculates minimum distance from point P to line segment AB in meters.
     */
    fun distanceToSegmentMeters(
        pLat: Double, pLng: Double,
        aLat: Double, aLng: Double,
        bLat: Double, bLng: Double
    ): Float {
        // Convert to local Cartesian coordinates centered at A in meters
        val latMidRad = Math.toRadians((aLat + bLat) / 2.0)
        val metersPerDegreeLat = 111132.92
        val metersPerDegreeLng = 111412.84 * cos(latMidRad)

        val ax = 0.0
        val ay = 0.0
        val bx = (bLng - aLng) * metersPerDegreeLng
        val by = (bLat - aLat) * metersPerDegreeLat
        val px = (pLng - aLng) * metersPerDegreeLng
        val py = (pLat - aLat) * metersPerDegreeLat

        val segLenSq = bx * bx + by * by
        if (segLenSq == 0.0) {
            return sqrt(px * px + py * py).toFloat()
        }

        // Project point P onto segment AB
        val t = max(0.0, min(1.0, (px * bx + py * by) / segLenSq))
        val projX = ax + t * bx
        val projY = ay + t * by
        val dx = px - projX
        val dy = py - projY
        return sqrt(dx * dx + dy * dy).toFloat()
    }
}
