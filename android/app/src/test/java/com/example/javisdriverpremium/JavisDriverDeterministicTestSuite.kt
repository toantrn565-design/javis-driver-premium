package com.example.javisdriverpremium

import com.example.javisdriverpremium.data.TestCorridorRepository
import com.example.javisdriverpremium.engine.DecisionEngine
import com.example.javisdriverpremium.engine.GeoUtils
import com.example.javisdriverpremium.engine.RoadMatcher
import com.example.javisdriverpremium.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class JavisDriverDeterministicTestSuite {

    private lateinit var decisionEngine: DecisionEngine
    private lateinit var roadMatcher: RoadMatcher
    private lateinit var fixtureSegments: List<RoadSegment>
    private lateinit var fixtureAlerts: List<AlertPoint>

    @Before
    fun setUp() {
        roadMatcher = RoadMatcher(maxSnapDistanceMeters = 35f, maxHeadingDifferenceDegrees = 50f)
        decisionEngine = DecisionEngine(roadMatcher)
        val fixture = TestCorridorRepository.getTestCorridorFixture()
        fixtureSegments = fixture.first
        fixtureAlerts = fixture.second
    }

    // 1. BEARING AND ANGLE DIFFERENCE TESTS
    @Test
    fun testAngleDifference_exactAndWrapAround() {
        assertEquals(0f, GeoUtils.angleDifference(0f, 0f), 0.001f)
        assertEquals(90f, GeoUtils.angleDifference(0f, 90f), 0.001f)
        assertEquals(180f, GeoUtils.angleDifference(0f, 180f), 0.001f)
        assertEquals(90f, GeoUtils.angleDifference(10f, 280f), 0.001f)
        assertEquals(20f, GeoUtils.angleDifference(350f, 10f), 0.001f)
    }

    // 2. DIRECTION COMPATIBILITY TEST
    @Test
    fun testDirectionCompatibility() {
        // Heading 15 deg vs segment 15 deg -> Compatible
        assertTrue(GeoUtils.isDirectionCompatible(15f, 15f, 50f))

        // Heading 30 deg vs segment 15 deg -> Compatible (diff 15 deg <= 50 deg)
        assertTrue(GeoUtils.isDirectionCompatible(30f, 15f, 50f))

        // Heading 195 deg vs segment 15 deg -> INCOMPATIBLE (Opposite direction, diff 180 deg)
        assertFalse(GeoUtils.isDirectionCompatible(195f, 15f, 50f))

        // Heading 105 deg vs segment 15 deg -> INCOMPATIBLE (Perpendicular, diff 90 deg)
        assertFalse(GeoUtils.isDirectionCompatible(105f, 15f, 50f))
    }

    // 3. IS POINT AHEAD VS BEHIND TEST
    @Test
    fun testIsPointAhead_AheadAndBehind() {
        val userLat = 21.070000
        val userLng = 105.808000
        val userBearing = 15f // Heading North-Northeast

        // Target point further North along bearing
        val targetAheadLat = 21.075000
        val targetAheadLng = 105.809500
        assertTrue(GeoUtils.isPointAhead(userLat, userLng, userBearing, targetAheadLat, targetAheadLng))

        // Target point to the South (Behind vehicle)
        val targetBehindLat = 21.065000
        val targetBehindLng = 105.806500
        assertFalse(GeoUtils.isPointAhead(userLat, userLng, userBearing, targetBehindLat, targetBehindLng))
    }

    // 4. ROAD MATCHING - SAME ROAD & DIRECTION
    @Test
    fun testRoadMatching_CorrectDirection() {
        val gpsNorthbound = GpsSample(
            lat = 21.070000,
            lng = 105.808000,
            speedKmh = 65f,
            bearing = 15f,
            accuracy = 5f
        )
        val matched = roadMatcher.matchSegment(gpsNorthbound, fixtureSegments)
        assertNotNull(matched)
        assertEquals("TEST_SEG_VO_CHI_CONG_NB", matched?.id)
        assertEquals(80, matched?.speedLimit)
    }

    // 5. ROAD MATCHING - OPPOSITE DIRECTION MATCHES OPPOSITE SEGMENT
    @Test
    fun testRoadMatching_OppositeDirection() {
        // Line on SB segment passes through (21.070000, 105.808500)
        val gpsSouthbound = GpsSample(
            lat = 21.070000,
            lng = 105.808500,
            speedKmh = 60f,
            bearing = 195f, // Heading Southbound
            accuracy = 5f
        )
        val matched = roadMatcher.matchSegment(gpsSouthbound, fixtureSegments)
        assertNotNull(matched)
        assertEquals("TEST_SEG_VO_CHI_CONG_SB", matched?.id)
    }

    // 6. HARD RULE: OPPOSITE DIRECTION CAMERA => NO ALERT
    @Test
    fun testAlertEngine_OppositeDirectionCamera_MustNotTrigger() {
        // Vehicle driving Northbound (15 deg) approaching Southbound camera (195 deg at 21.065000, 105.807000)
        val oppositeCamera = AlertPoint(
            id = "TEST_CAM_VCC_SB_80",
            lat = 21.065000,
            lng = 105.807000,
            type = AlertType.SPEED_CAMERA,
            direction = 195f,
            verificationStatus = VerificationStatus.VERIFIED
        )
        val gps = GpsSample(
            lat = 21.063000,
            lng = 105.806000,
            speedKmh = 60f,
            bearing = 15f,
            accuracy = 5f
        )
        val result = decisionEngine.evaluate(gps, emptyList(), listOf(oppositeCamera))
        
        // Southbound camera should NOT trigger voice alert or become candidate for Northbound vehicle
        assertNull("Opposite direction camera must not trigger voice alert", result.ttsToSpeak)
        assertNull("Opposite camera must not be active nextAlert", result.nextAlert)
    }

    // 7. HARD RULE: ALERT BEHIND VEHICLE => NO ALERT
    @Test
    fun testAlertEngine_AlertBehindVehicle_MustNotTrigger() {
        // Vehicle at 21.078000 has already passed NB camera at 21.075000 and is heading North (15 deg)
        val passedCamera = AlertPoint(
            id = "TEST_CAM_VCC_NB_80",
            lat = 21.075000,
            lng = 105.809500,
            type = AlertType.SPEED_CAMERA,
            direction = 15f,
            verificationStatus = VerificationStatus.VERIFIED
        )
        val gpsPastCamera = GpsSample(
            lat = 21.078000,
            lng = 105.810500,
            speedKmh = 70f,
            bearing = 15f,
            accuracy = 5f
        )
        val result = decisionEngine.evaluate(gpsPastCamera, emptyList(), listOf(passedCamera))
        assertNull("Camera behind vehicle must never trigger voice alert", result.ttsToSpeak)
        assertNull("Passed camera must not be nextAlert", result.nextAlert)
    }

    // 8. HARD RULE: UNKNOWN SPEED LIMIT MUST DISPLAY NULL (UI SHOWS "--") - NEVER GUESS
    @Test
    fun testAlertEngine_UnknownSpeedLimit_ReturnsNull() {
        // Line on TEST_SEG_UNKNOWN_SPEED passes through (21.012000, 105.754000) heading 60 deg
        val gpsOnUnknownRoad = GpsSample(
            lat = 21.012000,
            lng = 105.754000,
            speedKmh = 40f,
            bearing = 60f,
            accuracy = 5f
        )
        val result = decisionEngine.evaluate(gpsOnUnknownRoad, fixtureSegments, fixtureAlerts)
        assertEquals("Đường Gom Đại Lộ", result.currentRoadName)
        assertNull("Unknown speed limit must return null (UI renders '--')", result.currentSpeedLimit)
    }

    // 9. HARD RULE: UNVERIFIED ALERT DATA => SUPPRESSED
    @Test
    fun testAlertEngine_UnverifiedAlert_MustBeSuppressed() {
        val unverifiedAlert = AlertPoint(
            id = "TEST_CAM_UNVERIFIED_RUMOR",
            lat = 21.072000,
            lng = 105.808600,
            type = AlertType.SPEED_CAMERA,
            direction = 15f,
            speedLimitKm = 60,
            verificationStatus = VerificationStatus.UNVERIFIED
        )
        val gps = GpsSample(
            lat = 21.070000,
            lng = 105.808000,
            speedKmh = 50f,
            bearing = 15f,
            accuracy = 5f
        )
        val result = decisionEngine.evaluate(gps, emptyList(), listOf(unverifiedAlert))
        
        // Unverified camera should never trigger voice alert as verified
        assertNull("Unverified camera must never trigger voice alert", result.ttsToSpeak)
        assertNull("Unverified camera must not be active nextAlert", result.nextAlert)
        assertEquals("UNVERIFIED_ALERT_DATA", result.suppressionReason)
    }

    // 10. CORRECT ALERT AHEAD => TRIGGERS VOICE ALERT WITH DISTANCE
    @Test
    fun testAlertEngine_CorrectAlertAhead_TriggersVoiceAlert() {
        // Vehicle approaching NB camera (at 21.075000, 105.809500) ~350m ahead
        val gpsApproachingCamera = GpsSample(
            lat = 21.072000,
            lng = 105.808500,
            speedKmh = 75f,
            bearing = 15f,
            accuracy = 5f
        )
        val result = decisionEngine.evaluate(gpsApproachingCamera, fixtureSegments, fixtureAlerts)
        assertNotNull("Approaching verified camera ahead must trigger alert", result.ttsToSpeak)
        assertTrue(result.ttsToSpeak!!.contains("camera"))
        assertEquals(80, result.currentSpeedLimit)
        assertNotNull(result.nextAlert)
    }

    // 11. HARD RULE: ALERT COOLDOWN & DUPLICATE SUPPRESSION
    @Test
    fun testAlertEngine_AlertCooldown_SilencesDuplicate() {
        val gps = GpsSample(
            lat = 21.072000,
            lng = 105.808500,
            speedKmh = 75f,
            bearing = 15f,
            accuracy = 5f,
            timestamp = 1000000L
        )
        // First evaluation triggers alert
        val firstResult = decisionEngine.evaluate(gps, fixtureSegments, fixtureAlerts, currentTimeMs = 1000000L)
        assertNotNull(firstResult.ttsToSpeak)

        // Second evaluation 5 seconds later at nearby location -> Must be SILENT (cooldown active)
        val secondResult = decisionEngine.evaluate(gps, fixtureSegments, fixtureAlerts, currentTimeMs = 1005000L)
        assertNull("Alert must not repeat continuously within cooldown", secondResult.ttsToSpeak)
        assertEquals("COOLDOWN_ACTIVE", secondResult.suppressionReason)
    }

    // 12. STATIONARY VEHICLE TEST
    @Test
    fun testStationaryVehicle_NoSpeedSpam() {
        val gpsStationary = GpsSample(
            lat = 21.072000,
            lng = 105.808500,
            speedKmh = 0.5f, // Stopped at light
            bearing = 15f,
            accuracy = 5f
        )
        val result = decisionEngine.evaluate(gpsStationary, fixtureSegments, fixtureAlerts)
        assertNull("Stationary vehicle must not trigger dynamic camera alerts", result.ttsToSpeak)
    }

    // 13. PARALLEL / UNRELATED ROAD REJECTION
    @Test
    fun testAlertEngine_FarUnrelatedRoad_NoAlert() {
        // Vehicle 5km away in city center
        val gpsFar = GpsSample(
            lat = 21.028511,
            lng = 105.854444,
            speedKmh = 40f,
            bearing = 90f,
            accuracy = 5f
        )
        val result = decisionEngine.evaluate(gpsFar, fixtureSegments, fixtureAlerts)
        assertNull(result.currentRoadName)
        assertNull(result.currentSpeedLimit)
        assertNull(result.ttsToSpeak)
        assertNull(result.nextAlert)
    }
}
