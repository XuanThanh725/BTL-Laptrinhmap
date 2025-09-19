<h2 align="center">
    <a href="https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin">
    🎓 Faculty of Information Technology (DaiNam University)
    </a>
</h2>
<h2 align="center">
    CHAT ROOM DÙNG UDP MULTICAST
</h2>
<div align="center">
    <p align="center">
        <img alt="AIoTLab Logo" width="170" src="https://github.com/user-attachments/assets/711a2cd8-7eb4-4dae-9d90-12c0a0a208a2" />
        <img alt="AIoTLab Logo" width="180" src="https://github.com/user-attachments/assets/dc2ef2b8-9a70-4cfa-9b4b-f6c2f25f1660" />
        <img alt="DaiNam University Logo" width="200" src="https://github.com/user-attachments/assets/77fe0fd1-2e55-4032-be3c-b1a705a1b574" />
    </p>

[![AIoTLab](https://img.shields.io/badge/AIoTLab-green?style=for-the-badge)](https://www.facebook.com/DNUAIoTLab)
[![Faculty of Information Technology](https://img.shields.io/badge/Faculty%20of%20Information%20Technology-blue?style=for-the-badge)](https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin)
[![DaiNam University](https://img.shields.io/badge/DaiNam%20University-orange?style=for-the-badge)](https://dainam.edu.vn)

</div>

---

## 📖 1. Giới thiệu hệ thống
Chat Room là một ứng dụng giao tiếp nhóm theo thời gian thực trong LAN, sử dụng giao thức **UDP Multicast**.  
Người dùng có thể gửi/nhận tin nhắn đồng thời mà không cần server trung tâm. Hệ thống còn hỗ trợ:

- **Đăng nhập** bằng tài khoản trong DB (SQLite).
- **Phân quyền**: admin có thể kick user, gửi thông báo.
- **Lưu lịch sử chat** trong DB để truy xuất sau.
- **Giao diện Swing** trực quan, dễ sử dụng.

---

## 🔧 2. Công nghệ sử dụng

### 🖥 Java + Swing
- Ngôn ngữ chính: **Java (JDK 17/21)**.  
- Giao diện xây dựng bằng **Java Swing**: `JFrame`, `JTextArea`, `JTable`, `JDialog`, `JMenu`.  

### 🌐 UDP Multicast
- Sử dụng `MulticastSocket` + `DatagramPacket`.  
- Multicast group: `239.1.1.1`, cổng `5000`.  
- Gói điều khiển (login, history, kick…) qua UDP unicast cổng `6000`.

### 🧵 Đa luồng
- Server: `ControlServer` + `MulticastLogger` chạy song song.  
- Client: mỗi client có luồng riêng để lắng nghe multicast.

### 💾 CSDL SQLite
- **users**: lưu username, mật khẩu hash, role (user/admin).  
- **messages**: lưu lịch sử chat (room, user, text, timestamp).  
- Dùng thư viện `sqlite-jdbc-x.x.x.jar` (import vào Eclipse).  

### 🔑 Bảo mật & Phân quyền
- Mật khẩu hash SHA-256 + salt.  
- Role = `admin` → menu Admin bật: Kick user, gửi thông báo.  

---

## 🚀 3. Hình ảnh chức năng

<p align="center">
  <img src="![Uploading image.png…]()
" alt="Ảnh 1" width="800"/>
</p>
<p align="center"><em>Hình 1: Đăng nhập</em></p>

<p align="center">
  <img src="picture/Screenshot 2025-09-16 134828.png" alt="Ảnh 2" width="800"/>
</p>
<p align="center"><em>Hình 2: Giao diện chat client & server</em></p>

<p align="center">
  <img src="picture/Screenshot 2025-09-16 140817.png" alt="Ảnh 3" width="800"/>
</p>
<p align="center"><em>Hình 3: Cửa sổ chat</em></p>

<p align="center">
  <img src="picture/Screenshot 2025-09-16 140702.png" alt="Ảnh 4" width="800"/>
</p>
<p align="center"><em>Hình 4: Thông báo khi user thoát</em></p>

<p align="center">
  <img src="picture/Screenshot 2025-09-16 140708.png" alt="Ảnh 5" width="800"/>
</p>
<p align="center"><em>Hình 5: Lịch sử chat</em></p>

<p align="center">
  <img src="picture/Screenshot 2025-09-16 140754.png" alt="Ảnh 6" width="800"/>
</p>
<p align="center"><em>Hình 6: Quyền quản trị của admin</em></p>

<p align="center">
  <img src="picture/Screenshot 2025-09-16 141219.png" alt="Ảnh 7" width="800"/>
</p>
<p align="center"><em>Hình 7: Khi 1 client bị kick</em></p>

---

## 📝 4. Cài đặt & chạy

### Bước 1. Cài môi trường
- Cài **JDK 17+** ([Adoptium Temurin](https://adoptium.net) hoặc Oracle JDK).  
- Tải **SQLite JDBC Driver** (sqlite-jdbc-x.x.x.jar) → Add vào project (Build Path).  
- Cài [DB Browser for SQLite](https://sqlitebrowser.org/) để xem DB.

### Bước 2. Tạo project
- Trong Eclipse, tạo project `MulticastChat`.  
- Thư mục chính:
  - `client/`: `ChatClientMain.java`, `LoginDialog.java`, `AdminDialog.java`.  
  - `server/`: `ServerMain.java`, `ControlServer.java`, `MulticastLogger.java`, `DbUserStore.java`, `DbMessageStore.java`, `MessageStore.java`, `ChatMessage.java`.

### Bước 3. Tạo database
- Thư mục `data/chat.db`.  
- Tạo bảng:

```sql
CREATE TABLE users(
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  username TEXT UNIQUE,
  pass_hash TEXT,
  salt TEXT,
  role TEXT,
  created_at INTEGER,
  last_seen INTEGER
);

CREATE TABLE messages(
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  room TEXT,
  user TEXT,
  text TEXT,
  ts INTEGER
);
