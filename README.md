# ChessMaster - Online Chess Website

## Các tính năng chính

* Chơi ở 2 chế độ:

  * Khách (không cần đăng nhập)
  * Người chơi (cần đăng nhập)
* Thi đấu với máy thông qua tích hợp AI.
* Thi đấu PvP thời gian thực giữa:

  * Guest/User
  * User/User
* Xem lịch sử các trận đấu của bản thân.
* Theo dõi tỉ lệ thắng/thua.
* Trang quản trị Admin để:

  * Kiểm soát tài khoản
  * Thống kê người dùng
  * Quản lý trận đấu

---

##  Hướng dẫn cài đặt nhanh (Quick Start)

Dự án đã được container hóa hoàn toàn. Bạn không cần cài đặt Java hay MySQL thủ công trên máy.

### 1. Yêu cầu hệ thống

* Đã cài đặt Docker và Docker Compose.
* Máy tính có tối thiểu 4GB RAM trống.

### 2. Triển khai

Mở Terminal và thực hiện các lệnh sau:

```bash
# Clone source code hoặc tải repo dưới dạng .zip và giải nén 
git clone https://github.com/trungduc81/ChessWeb.git 
cd ChessWeb

# Khởi chạy toàn bộ hệ thống (App + Database)
docker compose up -d --build
```

Hệ thống sẽ tự động build mã nguồn Java, thiết lập Database và chạy tệp `database_setup.sql` để khởi tạo dữ liệu ban đầu.

## 🔗 Đường dẫn truy cập

Sau khi hệ thống khởi chạy thành công, bạn có thể truy cập qua các đường dẫn sau:

* Trang chủ: `http://localhost:8080/`
* Đăng nhập: `http://localhost:8080/Login.html`
* Đăng ký: `http://localhost:8080/Register.html`
* Quản trị viên: `http://localhost:8080/Dashboard.html`

##  Tài khoản mặc định

| Vai trò | Username | Password |
| ------- | -------- | -------- |
| Admin   | admin    | admin123 |
| User    | player1  | 123  |

##  Cấu trúc chức năng chính

* Guest Mode

  * Chơi nhanh không cần đăng nhập
  * Tham gia PvP với người dùng khác

* User Mode

  * Đăng ký / đăng nhập tài khoản
  * Theo dõi lịch sử trận đấu
  * Xem thống kê cá nhân
  * Tạo phòng hơi với AI hoặc PvP

* Admin Mode

  * Quản lý tài khoản người dùng
  * Theo dõi thống kê hệ thống
  * Kiểm soát trạng thái tài khoản


