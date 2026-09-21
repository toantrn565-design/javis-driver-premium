package com.example.javisdriverpremium.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.javisdriverpremium.model.*
import com.example.javisdriverpremium.service.DrivingState

// Premium Automotive Dark Theme Colors
val JavisDarkBg = Color(0xFF0D1117)
val JavisSurfaceBg = Color(0xFF161B22)
val JavisCardBg = Color(0xFF21262D)
val JavisAccentBlue = Color(0xFF58A6FF)
val JavisAccentGreen = Color(0xFF3FB950)
val JavisAccentRed = Color(0xFFF85149)
val JavisAccentYellow = Color(0xFFD29922)
val JavisTextPrimary = Color(0xFFF0F6FC)
val JavisTextSecondary = Color(0xFF8B949E)

@Composable
fun JavisDriverApp(
    drivingState: DrivingState,
    onStartDrive: (useTestFixture: Boolean) -> Unit,
    onEndDrive: () -> Unit,
    onReportSubmit: (ReportType) -> Unit,
    onSimulateGps: (GpsSample) -> Unit
) {
    var showReportDialog by remember { mutableStateOf(false) }
    var showSummaryDialog by remember { mutableStateOf(false) }
    var useTestFixtureMode by remember { mutableStateOf(false) }

    LaunchedEffect(drivingState.lastSummary) {
        if (drivingState.lastSummary != null && !drivingState.isDriving) {
            showSummaryDialog = true
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = JavisDarkBg
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!drivingState.isDriving) {
                HomeScreen(
                    gpsQuality = drivingState.gpsStatus,
                    useTestFixture = useTestFixtureMode,
                    onToggleTestFixture = { useTestFixtureMode = it },
                    onStartDrive = { onStartDrive(useTestFixtureMode) }
                )
            } else {
                DriveScreen(
                    state = drivingState,
                    onOpenReport = { showReportDialog = true },
                    onEndDrive = onEndDrive,
                    onSimulateGps = onSimulateGps
                )
            }

            if (showReportDialog) {
                ReportBottomSheet(
                    onSelect = { type ->
                        onReportSubmit(type)
                        showReportDialog = false
                    },
                    onDismiss = { showReportDialog = false }
                )
            }

            if (showSummaryDialog && drivingState.lastSummary != null) {
                TripSummaryModal(
                    summary = drivingState.lastSummary,
                    onDismiss = { showSummaryDialog = false }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    gpsQuality: GpsQuality,
    useTestFixture: Boolean,
    onToggleTestFixture: (Boolean) -> Unit,
    onStartDrive: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Top Header
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 32.dp)
        ) {
            Text(
                text = "JAVIS DRIVER",
                fontSize = 32.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = JavisAccentBlue
            )
            Text(
                text = "PREMIUM V0.1",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = JavisTextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Ready to drive",
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = JavisTextPrimary
            )
        }

        // Center Status Indicators
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = JavisSurfaceBg),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Trạng thái GPS", color = JavisTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val (color, text) = when (gpsQuality) {
                            GpsQuality.EXCELLENT -> JavisAccentGreen to "Sẵn sàng (Tốt)"
                            GpsQuality.ACCEPTABLE -> JavisAccentGreen to "Sẵn sàng"
                            GpsQuality.WEAK -> JavisAccentYellow to "GPS Yếu"
                            GpsQuality.LOST -> JavisAccentRed to "Chờ tín hiệu"
                            GpsQuality.PERMISSION_DENIED -> JavisAccentRed to "Chưa cấp quyền"
                        }
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text, color = color, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Dữ liệu đường", color = JavisTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val color = if (useTestFixture) JavisAccentYellow else JavisAccentGreen
                        val label = if (useTestFixture) "Fixture kiểm thử" else "Trực tiếp"
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, color = color, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Chế độ Fixture giả lập", color = JavisTextSecondary, fontSize = 14.sp)
                    Switch(
                        checked = useTestFixture,
                        onCheckedChange = onToggleTestFixture
                    )
                }
            }
        }

        // Bottom Action Button
        Button(
            onClick = onStartDrive,
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = JavisAccentBlue)
        ) {
            Text(
                text = "START DRIVE",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = Color.Black
            )
        }
    }
}

@Composable
fun DriveScreen(
    state: DrivingState,
    onOpenReport: () -> Unit,
    onEndDrive: () -> Unit,
    onSimulateGps: (GpsSample) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // TOP: Road Name & GPS Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ROAD",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = JavisTextSecondary
                )
                Text(
                    text = state.currentRoad ?: "Đường không xác định",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = JavisTextPrimary
                )
            }

            // GPS Quality Pill
            val (gpsColor, gpsLabel) = when (state.gpsStatus) {
                GpsQuality.EXCELLENT -> JavisAccentGreen to "GPS ±${state.accuracy.toInt()}m"
                GpsQuality.ACCEPTABLE -> JavisAccentGreen to "GPS ±${state.accuracy.toInt()}m"
                GpsQuality.WEAK -> JavisAccentYellow to "GPS Yếu ±${state.accuracy.toInt()}m"
                GpsQuality.LOST -> JavisAccentRed to "Mất GPS"
                GpsQuality.PERMISSION_DENIED -> JavisAccentRed to "Chưa cấp quyền"
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(JavisCardBg)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(gpsColor))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(gpsLabel, color = gpsColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // CENTER 1: HUGE LIVE SPEED DISPLAY
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 12.dp)
        ) {
            Text(
                text = "${state.currentSpeedKmh.toInt()}",
                fontSize = 110.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                color = JavisTextPrimary,
                lineHeight = 110.sp
            )
            Text(
                text = "km/h",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = JavisTextSecondary
            )
        }

        // CENTER 2: SPEED LIMIT & NEXT ALERT
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Speed Limit Box
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp),
                colors = CardDefaults.cardColors(containerColor = JavisSurfaceBg),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "SPEED LIMIT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = JavisTextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(4.dp, Color.Red, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.speedLimit?.toString() ?: "--",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Black
                        )
                    }
                }
            }

            // Next Alert Box
            Card(
                modifier = Modifier
                    .weight(1.4f)
                    .height(130.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (state.nextAlert != null) Color(0xFF2E1A1A) else JavisSurfaceBg
                ),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder()
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "NEXT ALERT",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (state.nextAlert != null) JavisAccentRed else JavisTextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (state.nextAlert != null) {
                        val alert = state.nextAlert.alertPoint
                        val title = when (alert.type) {
                            AlertType.SPEED_CAMERA -> "📷 Camera tốc độ ${alert.speedLimitKm ?: ""} km/h"
                            AlertType.RED_LIGHT_CAMERA -> "📷 Phạt nguội vượt đèn"
                            AlertType.HAZARD -> "⚠️ Điểm nguy hiểm"
                            AlertType.SPEED_LIMIT_CHANGE -> "🛑 Đổi giới hạn tốc độ"
                            AlertType.OTHER -> "Chú ý"
                        }
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = JavisAccentRed
                        )
                        Text(
                            text = "Cách ${state.nextAlert.distanceMeters.toInt()}m phía trước",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = JavisTextPrimary
                        )
                    } else {
                        Text(
                            text = "Đường an toàn",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = JavisTextSecondary
                        )
                        Text(
                            text = "Không có cảnh báo",
                            fontSize = 12.sp,
                            color = JavisTextSecondary
                        )
                    }
                }
            }
        }

        // BOTTOM ACTIONS: REPORT + END DRIVE
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Big Driver-Safe Report Button
            Button(
                onClick = onOpenReport,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = JavisAccentYellow)
            ) {
                Text(
                    text = "⚠️ BÁO CÁO NHANH (REPORT)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }

            // Big End Drive Button
            OutlinedButton(
                onClick = onEndDrive,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, JavisAccentRed),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = JavisAccentRed)
            ) {
                Text(
                    text = "KẾT THÚC CHUYẾN ĐI (END DRIVE)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = JavisAccentRed
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportBottomSheet(
    onSelect: (ReportType) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = JavisSurfaceBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "BÁO CÁO TÀI XẾ (1 CHẠM)",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = JavisTextPrimary
            )
            Text(
                text = "Tự động ghi nhận tọa độ GPS, tốc độ và hướng xe hiện tại",
                fontSize = 12.sp,
                color = JavisTextSecondary
            )
            Spacer(modifier = Modifier.height(20.dp))

            val items = listOf(
                Triple(ReportType.CAMERA, "📷 CÓ CAMERA", JavisAccentRed),
                Triple(ReportType.SPEED_WRONG, "⚠️ BIỂN TỐC ĐỘ SAI", JavisAccentYellow),
                Triple(ReportType.HAZARD, "🚧 ĐOẠN NGUY HIỂM", Color(0xFFE36209)),
                Triple(ReportType.OTHER, "❓ KHÁC", JavisTextSecondary)
            )

            for ((type, label, color) in items) {
                Button(
                    onClick = { onSelect(type) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = JavisCardBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, color)
                ) {
                    Text(
                        text = label,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = JavisTextPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun TripSummaryModal(
    summary: TripSummary,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = JavisSurfaceBg,
        title = {
            Text(
                text = "TỔNG KẾT CHUYẾN ĐI",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = JavisAccentGreen
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mã chuyến đi: ${summary.tripId}", fontSize = 12.sp, color = JavisTextSecondary)
                HorizontalDivider(color = JavisCardBg)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Thời gian chạy:", color = JavisTextSecondary)
                    Text("${summary.durationSeconds} giây", fontWeight = FontWeight.Bold, color = JavisTextPrimary)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tốc độ tối đa:", color = JavisTextSecondary)
                    Text("${summary.maxSpeedKmh.toInt()} km/h", fontWeight = FontWeight.Bold, color = JavisTextPrimary)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tốc độ trung bình:", color = JavisTextSecondary)
                    Text("${summary.averageSpeedKmh.toInt()} km/h", fontWeight = FontWeight.Bold, color = JavisTextPrimary)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Cảnh báo đã phát:", color = JavisTextSecondary)
                    Text("${summary.alertsTriggeredCount}", fontWeight = FontWeight.Bold, color = JavisAccentGreen)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Cảnh báo lọc bỏ:", color = JavisTextSecondary)
                    Text("${summary.alertsSuppressedCount}", fontWeight = FontWeight.Bold, color = JavisAccentYellow)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Báo cáo tài xế:", color = JavisTextSecondary)
                    Text("${summary.reportsSubmittedCount}", fontWeight = FontWeight.Bold, color = JavisAccentBlue)
                }
                HorizontalDivider(color = JavisCardBg)
                Text(
                    text = "✓ Bằng chứng hành trình đã lưu an toàn vào bộ nhớ máy (JSON).",
                    fontSize = 11.sp,
                    color = JavisAccentGreen
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = JavisAccentBlue)
            ) {
                Text("ĐÓNG", fontWeight = FontWeight.Bold, color = Color.Black)
            }
        }
    )
}
