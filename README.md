# JAVIS DRIVER PREMIUM — MVP V0.1

> **"JAVIS knows the roads you drive every day — and tells you what changed."**

Ứng dụng trợ lý lái xe thông minh thời gian thực (Android Native — Jetpack Compose).  
Tối ưu hóa cho trải nghiệm lái xe hàng ngày (Daily Drive) với tốc độ số lớn, cảnh báo camera phạt nguội/tốc độ phía trước, lọc chuẩn hướng di chuyển và cảnh báo giọng nói tiếng Việt tự nhiên.

---

## 🚀 Tính năng nổi bật MVP V0.1

1. **Giao diện lái xe chuyên dụng (Automotive Dark UI):**
   - Chế độ nền tối OLED độ tương phản cao, giảm chói mắt khi lái xe ban đêm.
   - Hiển thị tốc độ GPS thời gian thực với kích thước số siêu lớn (110sp).
   - Ô hiển thị giới hạn tốc độ và cảnh báo kế tiếp nổi bật, dễ quan sát trong nháy mắt.

2. **Định vị GPS chính xác cao & Quản lý trạng thái:**
   - Thu thập lat/lng, tốc độ GPS (km/h), góc hướng (bearing), sai số (accuracy).
   - Tự động cảnh báo khi GPS yếu hoặc mất tín hiệu.

3. **Thuật toán Khớp làn & Lọc hướng thông minh (`RoadMatcher` & `GeoUtils`):**
   - **Quy tắc vàng 1:** Camera chiều ngược lại TUYỆT ĐỐI KHÔNG cảnh báo.
   - **Quy tắc vàng 2:** Camera đã đi qua / sau lưng xe TUYỆT ĐỐI KHÔNG cảnh báo.
   - **Quy tắc vàng 3:** Không bịa đặt dữ liệu — đoạn đường chưa rõ giới hạn tốc độ hiển thị `--`.

4. **Bộ máy Quyết định Cảnh báo (`DecisionEngine`):**
   - Tính toán khoảng cách và góc quét hình nón phía trước đầu xe.
   - Khử lặp lại và thiết lập thời gian chờ (cooldown 120s) giữa các lần cảnh báo cùng một điểm.

5. **Giọng nói Cảnh báo Tiếng Việt (`VietnameseTtsManager`):**
   - Đọc khoảng cách và loại camera rõ ràng, tự nhiên (Ví dụ: *"Chú ý, camera bắn tốc độ 80 ki-lô-mét một giờ phía trước, cách 350 mét"*).

6. **Báo cáo Tài xế 1 Chạm (`One-Tap Report`):**
   - 4 nút lớn an toàn cho tài xế: 📷 Camera | ⚠️ Biển tốc độ sai | 🚧 Nguy hiểm | ❓ Khác.
   - Tự động gắn kèm tọa độ, tốc độ, góc hướng tại thời điểm bấm.

7. **Lưu trữ Bằng chứng Hành trình (`TripEvidenceManager`):**
   - Tự động lưu toàn bộ log chuyến đi (sự kiện, cảnh báo, báo cáo) thành file JSON trong bộ nhớ máy để phục vụ hậu kiểm.

---

## 📦 Tải & Cài đặt APK

- **File APK Release:** [`release/JAVIS_DRIVER_PREMIUM_MVP_V0.1/JAVIS_DRIVER_PREMIUM_MVP_V0.1.apk`](file:///g:/05.%20GEM%20GEMINI%202026/19.%20Javis%20Driver%20Premium/release/JAVIS_DRIVER_PREMIUM_MVP_V0.1/JAVIS_DRIVER_PREMIUM_MVP_V0.1.apk)
- **Hướng dẫn cài đặt chi tiết:** [README_INSTALL.md](release/JAVIS_DRIVER_PREMIUM_MVP_V0.1/README_INSTALL.md)
- **Checklist Road Test sáng mai:** [ROAD_TEST_CHECKLIST.md](release/JAVIS_DRIVER_PREMIUM_MVP_V0.1/ROAD_TEST_CHECKLIST.md)

---

## 🧪 Kết quả Kiểm thử Tự Động (18/18 PASS — 100%)

- **13 ca Deterministic Unit Tests:** Đạt 100% PASS (Lọc ngược chiều, sau lưng xe, khử trùng lặp, tốc độ giới hạn, xe dừng đỗ).
- **5 ca Adversarial Tests:** Đạt 100% PASS (Quay đầu 180°, chạy 180 km/h, suy giảm GPS trong hầm, dồn dập 50 mẫu GPS, khởi động lại phiên).
- Chi tiết: [TEST_REPORT.md](release/JAVIS_DRIVER_PREMIUM_MVP_V0.1/TEST_REPORT.md)

---

## 🛠 Kiến trúc Công nghệ

- **Ngôn ngữ:** Kotlin 2.3.20 (JVM Toolchain 17)
- **UI Framework:** Jetpack Compose + Material3
- **Kiến trúc:** Clean Architecture (Model - Engine - Service - UI - Evidence)
- **Xử lý nền:** Android Foreground Service with Persistent Notification
- **Quản lý trạng thái:** Kotlin Coroutines & StateFlow
