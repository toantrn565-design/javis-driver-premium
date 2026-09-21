# JAVIS DRIVER PREMIUM — MVP V0.1 STATUS

## Overall Status
**READY FOR MORNING ROAD TEST (22/09/2026)**

---

## Deliverables & Work Packages

| ID | Linear Issue | Work Package | Status | Evidence / DoD |
| :--- | :--- | :--- | :--- | :--- |
| **MVP-01** | TOA-127 | Android Shell | **PASS** | Hoàn thành Compose UI Dark Theme, foreground service lifecycle, nút START/END DRIVE, cấp quyền vị trí. |
| **MVP-02** | TOA-128 | Live GPS & Speed | **PASS** | Ghi nhận lat/lng, speed (km/h), bearing, accuracy, trạng thái GPS hiển thị trực tiếp. |
| **MVP-03** | TOA-129 | Test Corridor Data | **READY (Isolated)** | Tách bạch 100% dữ liệu thật vs fixture giả lập `TEST_ONLY`. Đường chưa xác thực hiển thị `--`, không đoán mò. |
| **MVP-04** | TOA-130 | Road & Direction Matching | **PASS** | Thuật toán `RoadMatcher` và `GeoUtils`: lọc chính xác theo góc lệch hướng di chuyển và đoạn đường. |
| **MVP-05** | TOA-131 | Decision / Alert Engine Lite | **PASS** | Thuật toán xác định camera phía trước, lọc ngược chiều/phía sau, khử lặp lại và quản lý cooldown 120s. |
| **MVP-06** | TOA-132 | Vietnamese TTS | **PASS** | Quản lý phát giọng nói tiếng Việt tự nhiên, tự động khử trùng lặp và không lặp âm thanh liên tục. |
| **MVP-07** | TOA-133 | One-Tap Report | **PASS** | Menu 4 nút lớn 1 chạm (Camera, Biển sai, Nguy hiểm, Khác), tự động đóng gói GPS + tốc độ + hướng + thời gian. |
| **MVP-08** | TOA-134 | Trip Evidence | **PASS** | Tự động lưu log JSON chi tiết (`trip_<id>.json`) trong bộ nhớ máy: sự kiện bắt đầu/kết thúc, cảnh báo phát/hủy, báo cáo. |
| **MVP-09** | TOA-135 | Morning Road Test | **READY FOR ROAD TEST** | Sẵn sàng cho Owner chạy xe thực tế sáng 22/09/2026. Sẽ đóng PASS sau khi có log chuyến đi thật. |

---

## BTO Release Gate Verification Checklist

- [x] APK builds successfully (`assembleDebug`)
- [x] APK is installable & signed
- [x] App launches with dark automotive UI
- [x] Runtime location permission handled
- [x] START DRIVE / END DRIVE session working
- [x] Live GPS and prominent speed display (km/h)
- [x] GPS status visible (Tốt / Yếu / Chờ tín hiệu)
- [x] Unknown speed limit displays `--` (never guessed)
- [x] Deterministic alert engine passes all tests
- [x] Opposite direction camera => NO ALERT (PASS)
- [x] Behind vehicle camera => NO ALERT (PASS)
- [x] Alert cooldown => SILENCE (PASS)
- [x] Vietnamese TTS engine integrated
- [x] Driver One-Tap Report working
- [x] Trip evidence persists to JSON locally
- [x] Zero release-blocking crashes
- [x] 13/13 automated unit tests passed
- [x] No API keys or secrets hardcoded in APK
