# CHANGELOG — JAVIS DRIVER PREMIUM

## [0.1.0] - 2026-09-21 (Overnight Execution)

### Added
- **MVP-01 / TOA-127:** Khởi tạo Android Shell với giao diện Jetpack Compose chuyên dụng ban đêm (OLED dark theme, tương phản cao, số lớn dễ đọc).
- **MVP-02 / TOA-128:** Hệ thống đọc GPS thời gian thực (lat, lng, GPS speed km/h, bearing, accuracy) và bộ chỉ thị chất lượng GPS.
- **MVP-03 / TOA-129:** Mô hình dữ liệu hành lang kiểm thử `TestCorridorRepository`, tuân thủ nguyên tắc không bịa đặt dữ liệu (hiển thị `--` khi không rõ giới hạn tốc độ).
- **MVP-04 / TOA-130:** Bộ lọc hướng và khớp đoạn đường `RoadMatcher` + `GeoUtils` loại bỏ hoàn toàn các cảnh báo ngược chiều, sau lưng và khác làn.
- **MVP-05 / TOA-131:** `DecisionEngine` phân cấp cảnh báo camera, tốc độ, nguy hiểm với cơ chế cooldown 120s và khử lặp lại.
- **MVP-06 / TOA-132:** Quản lý âm thanh giọng nói tiếng Việt `VietnameseTtsManager` phát cảnh báo tự nhiên không làm phiền tài xế.
- **MVP-07 / TOA-133:** Nút báo cáo 1 chạm `One-Tap Report` hỗ trợ 4 danh mục lớn, tự động đính kèm tọa độ, hướng xe và tốc độ.
- **MVP-08 / TOA-134:** Quản lý bằng chứng hành trình `TripEvidenceManager` lưu file JSON hoàn chỉnh vào bộ nhớ cục bộ để phục vụ hậu kiểm.
- **MVP-09 / TOA-135:** Bộ kiểm thử tự động 13 ca test quy tắc vàng đạt 100% PASS và xuất file cài đặt APK `JAVIS_DRIVER_PREMIUM_MVP_V0.1.apk`.
