package com.example.javisdriverpremium.data

import com.example.javisdriverpremium.model.*

object TestCorridorRepository {

    /**
     * Standard production corridor dataset.
     * Starts empty or strictly verified. When unknown -> returns empty list -> UI shows "--".
     */
    fun getProductionCorridor(): List<RoadSegment> {
        return emptyList()
    }

    fun getProductionAlertPoints(): List<AlertPoint> {
        return emptyList()
    }

    /**
     * TEST ONLY: A simulated Hanoi / HCMC commute corridor fixture used ONLY for unit tests and local simulation.
     * All items are explicitly tagged with `isTestFixture = true`.
     */
    fun getTestCorridorFixture(): Pair<List<RoadSegment>, List<AlertPoint>> {
        val segments = listOf(
            RoadSegment(
                id = "TEST_SEG_VO_CHI_CONG_NB",
                name = "Võ Chí Công (Hướng Cầu Nhật Tân)",
                points = listOf(
                    GeoPoint(21.060000, 105.805000),
                    GeoPoint(21.070000, 105.808000),
                    GeoPoint(21.080000, 105.811000)
                ),
                direction = 15f, // Heading North-Northeast (~15 deg)
                speedLimit = 80,
                verificationStatus = VerificationStatus.VERIFIED,
                source = "TEST_ONLY_FIXTURE",
                isTestFixture = true
            ),
            RoadSegment(
                id = "TEST_SEG_VO_CHI_CONG_SB",
                name = "Võ Chí Công (Hướng Bưởi / Hoàng Quốc Việt)",
                points = listOf(
                    GeoPoint(21.080000, 105.811500),
                    GeoPoint(21.070000, 105.808500),
                    GeoPoint(21.060000, 105.805500)
                ),
                direction = 195f, // Heading South-Southwest (~195 deg)
                speedLimit = 80,
                verificationStatus = VerificationStatus.VERIFIED,
                source = "TEST_ONLY_FIXTURE",
                isTestFixture = true
            ),
            RoadSegment(
                id = "TEST_SEG_NGUYEN_VAN_HUYEN",
                name = "Nguyễn Văn Huyên",
                points = listOf(
                    GeoPoint(21.035000, 105.795000),
                    GeoPoint(21.045000, 105.797000)
                ),
                direction = 10f,
                speedLimit = 50,
                verificationStatus = VerificationStatus.VERIFIED,
                source = "TEST_ONLY_FIXTURE",
                isTestFixture = true
            ),
            RoadSegment(
                id = "TEST_SEG_UNKNOWN_SPEED",
                name = "Đường Gom Đại Lộ",
                points = listOf(
                    GeoPoint(21.010000, 105.750000),
                    GeoPoint(21.015000, 105.760000)
                ),
                direction = 60f,
                speedLimit = null, // Unknown speed limit
                verificationStatus = VerificationStatus.UNKNOWN,
                source = "TEST_ONLY_FIXTURE",
                isTestFixture = true
            )
        )

        val alertPoints = listOf(
            // Speed camera on Vo Chi Cong Northbound (direction 15 deg)
            AlertPoint(
                id = "TEST_CAM_VCC_NB_80",
                lat = 21.075000,
                lng = 105.809500,
                type = AlertType.SPEED_CAMERA,
                direction = 15f,
                segmentId = "TEST_SEG_VO_CHI_CONG_NB",
                speedLimitKm = 80,
                verificationStatus = VerificationStatus.VERIFIED,
                source = "TEST_ONLY_FIXTURE",
                isTestFixture = true
            ),
            // Speed camera on Vo Chi Cong Southbound (direction 195 deg - opposite)
            AlertPoint(
                id = "TEST_CAM_VCC_SB_80",
                lat = 21.065000,
                lng = 105.807000,
                type = AlertType.SPEED_CAMERA,
                direction = 195f,
                segmentId = "TEST_SEG_VO_CHI_CONG_SB",
                speedLimitKm = 80,
                verificationStatus = VerificationStatus.VERIFIED,
                source = "TEST_ONLY_FIXTURE",
                isTestFixture = true
            ),
            // Red light camera on Nguyen Van Huyen
            AlertPoint(
                id = "TEST_CAM_NVH_REDLIGHT",
                lat = 21.040000,
                lng = 105.796000,
                type = AlertType.RED_LIGHT_CAMERA,
                direction = 10f,
                segmentId = "TEST_SEG_NGUYEN_VAN_HUYEN",
                verificationStatus = VerificationStatus.VERIFIED,
                source = "TEST_ONLY_FIXTURE",
                isTestFixture = true
            ),
            // Unverified community alert (Must be suppressed)
            AlertPoint(
                id = "TEST_CAM_UNVERIFIED_RUMOR",
                lat = 21.072000,
                lng = 105.808600,
                type = AlertType.SPEED_CAMERA,
                direction = 15f,
                speedLimitKm = 60,
                verificationStatus = VerificationStatus.UNVERIFIED,
                source = "UNVERIFIED_COMMUNITY_REPORT",
                isTestFixture = true
            )
        )

        return Pair(segments, alertPoints)
    }
}
