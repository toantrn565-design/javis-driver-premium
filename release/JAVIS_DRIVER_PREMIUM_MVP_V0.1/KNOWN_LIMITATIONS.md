# JAVIS DRIVER PREMIUM — CÁC GIỚI HẠN ĐÃ BIẾT (KNOWN LIMITATIONS)

Phiên bản **MVP V0.1** được khóa phạm vi tối giản và tập trung cao độ vào độ tin cậy của chu trình Daily Drive thực tế.

---

## 1. Hành lang dữ liệu đường (Corridor Data)
- **Hiện trạng:** Khi chưa có bản đồ hành lang được nạp chính thức từ cung đường đi làm thực tế của Owner, ứng dụng sẽ hiển thị `--` ở ô giới hạn tốc độ và tên đường là `Đường không xác định` (theo đúng nguyên tắc không bịa đặt dữ liệu).
- **Chế độ kiểm thử:** Có sẵn công tắc *"Chế độ Fixture giả lập"* trên màn hình Home để thử nghiệm các ca camera/giới hạn tốc độ trên cung đường mẫu Võ Chí Công - Nguyễn Văn Huyên.

## 2. Giọng nói Tiếng Việt (TTS)
- Ứng dụng sử dụng engine TextToSpeech của thiết bị Android (`vi-VN`).
- Nếu trên một số dòng máy chưa tải gói dữ liệu giọng nói tiếng Việt từ Google TTS, Android sẽ phát bằng giọng mặc định hoặc cần kích hoạt tải gói tiếng Việt trong Cài đặt Android -> Quản lý chung -> Chuyển văn bản thành giọng nói.

## 3. Không hỗ trợ các tính năng ngoài phạm vi MVP:
- Chưa có bản đồ điều hướng A -> B (Google Maps style).
- Chưa có dự báo tắc đường AI hay chat bot LLM.
- Chưa hỗ trợ Android Auto / CarPlay.
- Chưa có tài khoản đăng nhập hay thanh toán đám mây (tuân thủ nguyên tắc Privacy & Local-first).
