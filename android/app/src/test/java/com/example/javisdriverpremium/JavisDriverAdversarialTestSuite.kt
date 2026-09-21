package com.example.javisdriverpremium

import com.example.javisdriverpremium.data.TestCorridorRepository
import com.example.javisdriverpremium.engine.DecisionEngine
import com.example.javisdriverpremium.engine.GeoUtils
import com.example.javisdriverpremium.engine.RoadMatcher
import com.example.javisdriverpremium.model.*
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class JavisDriverAdversarialTestSuite {

    private lateinit var decisionEngine: DecisionEngine
    private lateinit var roadMatcher: RoadMatcher
    private lateinit var segments: List<RoadSegment>
    private lateinit var alertPoints: List<AlertPoint>

    @Before
    fun setUp() {
        roadMatcher = RoadMatcher(maxSnapDistanceMeters = 35f, maxHeadingDifferenceDegrees = 50f)
        decisionEngine = DecisionEngine(roadMatcher)
        val fixture = TestCorridorRepository.getTestCorridorFixture()
        segments = fixture.first
        alertPoints = fixture.second
    }

    // ADVERSARIAL 1: Rapid 180-degree U-turn bearing flip
    @Test
    fun testAdversarial_RapidUTurnFlip() {
        // Vehicle moving North at 70 km/h
        val gpsNorth = GpsSample(lat = 21.070000, lng = 105.808000, speedKmh = 70f, bearing = 15f, accuracy = 5f)
        val resNorth = decisionEngine.evaluate(gpsNorth, segments, alertPoints, currentTimeMs = 1000L)
        assertEquals("Võ Chí Công (Hướng Cầu Nhật Tân)", resNorth.currentRoadName)

        // Instant U-turn to Southbound (195 deg)
        val gpsSouth = GpsSample(lat = 21.070000, lng = 105.808500, speedKmh = 60f, bearing = 195f, accuracy = 5f)
        val resSouth = decisionEngine.evaluate(gpsSouth, segments, alertPoints, currentTimeMs = 2000L)
        assertEquals("Võ Chí Công (Hướng Bưởi / Hoàng Quốc Việt)", resSouth.currentRoadName)
    }

    // ADVERSARIAL 2: Extreme High Speed (180 km/h) & Extreme Low Speed (0.1 km/h)
    @Test
    fun testAdversarial_SpeedExtremes() {
        // Highway sprint
        val gpsFast = GpsSample(lat = 21.072000, lng = 105.808500, speedKmh = 180f, bearing = 15f, accuracy = 3f)
        val resFast = decisionEngine.evaluate(gpsFast, segments, alertPoints, currentTimeMs = 1000L)
        assertNotNull(resFast.ttsToSpeak)

        // Crawling / Traffic jam
        decisionEngine.resetSession()
        val gpsCrawl = GpsSample(lat = 21.072000, lng = 105.808500, speedKmh = 1.0f, bearing = 15f, accuracy = 3f)
        val resCrawl = decisionEngine.evaluate(gpsCrawl, segments, alertPoints, currentTimeMs = 1000L)
        assertNull("Sub-5km/h crawl must not trigger dynamic camera alerts", resCrawl.ttsToSpeak)
    }

    // ADVERSARIAL 3: Degraded GPS Accuracy (> 50m) in tunnel
    @Test
    fun testAdversarial_TunnelGpsDegradation() {
        val gpsDegraded = GpsSample(lat = 21.072000, lng = 105.808500, speedKmh = 60f, bearing = 15f, accuracy = 85f)
        val res = decisionEngine.evaluate(gpsDegraded, segments, alertPoints, currentTimeMs = 1000L)
        // No crash occurs and road match still handles boundaries
        assertNotNull(res)
    }

    // ADVERSARIAL 4: High-frequency GPS updates (50 consecutive calls in quick succession)
    @Test
    fun testAdversarial_HighFrequencyStress() {
        for (i in 0 until 50) {
            val offset = i * 0.0001
            val gps = GpsSample(
                lat = 21.060000 + offset,
                lng = 105.805000 + (offset * 0.3),
                speedKmh = 60f + (i % 5),
                bearing = 15f,
                accuracy = 4f,
                timestamp = 1000L + (i * 100)
            )
            val result = decisionEngine.evaluate(gps, segments, alertPoints, currentTimeMs = gps.timestamp)
            assertNotNull(result)
        }
    }

    // ADVERSARIAL 5: Session Reset and Restart
    @Test
    fun testAdversarial_SessionResetAndRestart() {
        val gps = GpsSample(lat = 21.072000, lng = 105.808500, speedKmh = 75f, bearing = 15f, accuracy = 5f)
        
        // 1. First trigger
        val res1 = decisionEngine.evaluate(gps, segments, alertPoints, currentTimeMs = 1000L)
        assertNotNull(res1.ttsToSpeak)

        // 2. Cooldown suppresses second immediate hit
        val res2 = decisionEngine.evaluate(gps, segments, alertPoints, currentTimeMs = 2000L)
        assertNull(res2.ttsToSpeak)

        // 3. Reset session (e.g. End drive & Start new drive) -> Cooldown is cleared
        decisionEngine.resetSession()
        val res3 = decisionEngine.evaluate(gps, segments, alertPoints, currentTimeMs = 2050L)
        assertNotNull("After session reset, alert triggers fresh on new trip", res3.ttsToSpeak)
    }
}
