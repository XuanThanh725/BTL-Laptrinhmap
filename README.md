<h2 align="center">
    <a href="https://github.com/XuanThanh725">
    🎓 Project Network Programming (BTL - Laptrinhmang)
    </a>
</h2>

<h2 align="center">
    CHAT ROOM SỬ DỤNG UDP MULTICAST
</h2>

<div align="center">
    <p align="center">
        <img alt="Logo 1" width="170" src="https://github.com/user-attachments/assets/711a2cd8-7eb4-4dae-9d90-12c0a0a208a2" />
        <img alt="Logo 2" width="180" src="https://github.com/user-attachments/assets/dc2ef2b8-9a70-4cfa-9b4b-f6c2f25f1660" />
        <img alt="Logo 3" width="200" src="https://github.com/user-attachments/assets/77fe0fd1-2e55-4032-be3c-b1a705a1b574" />
    </p>

[![GitHub](https://img.shields.io/badge/GitHub-Project-black?style=for-the-badge)](https://github.com/XuanThanh725/BTL-Laptrinhmap)
[![Java](https://img.shields.io/badge/Java-17-red?style=for-the-badge)](https://www.oracle.com/java/technologies/downloads/)
[![UDP Multicast](https://img.shields.io/badge/UDP-Multicast-blue?style=for-the-badge)](#)

</div>

---

## 📖 1. Giới thiệu
Chat Room là ứng dụng hỗ trợ **trao đổi tin nhắn nhóm theo thời gian thực** trong cùng một mạng LAN.  
Hệ thống sử dụng giao thức **UDP Multicast** để các client có thể gửi/nhận tin nhắn mà **không cần server trung tâm**.  
Ứng dụng gồm hai thành phần:

- **MulticastChatMain** → Quản lý xử lý mạng (gửi/nhận gói tin UDP).  
- **MulticastChatApp** → Giao diện (Java Swing), hiển thị tin nhắn và lưu log.  

Với thiết kế này, Chat Room vừa **đơn giản, nhẹ gọn** vừa dễ mở rộng (thêm admin, kick user, password phòng, v.v.).

---

## 🔧 2. Công nghệ sử dụng
- **Ngôn ngữ**: Java (JDK 8+, khuyến nghị JDK 17).  
- **Giao diện**: Java Swing (`JFrame`, `JTextArea`, `JTextField`, `JButton`).  
- **Mạng**: `MulticastSocket`, `DatagramPacket`, địa chỉ nhóm `224.0.0.69:3000`.  
- **Đa luồng**: ExecutorService để nhận/gửi song song mà không làm treo giao diện.  
- **File I/O**: Ghi log chat vào `chat_log.txt`.  
- **Timestamp**: `LocalDateTime` để hiển thị giờ gửi/nhận.  

---

## 🚀 3. Hình ảnh minh họa

<p align="center">
  <img src="[picture/Screenshot 2025-09-16 141353.png](https://media.discordapp.net/attachments/729165164808634522/1418400171355803688/image.png?ex=68cdfb99&is=68ccaa19&hm=f132a6f8cd314fb4b4e4b84e6963585c36eb1ee666524464888257f4412a463e&=&format=webp&quality=lossless&width=1083&height=680)" alt="Ảnh 1" width="800"/>
</p>
<p align="center"><em>Hình 1: Đăng nhập</em></p>

<p align="center">
  <img src="picture/Screenshot 2025-09-16 134828.png" alt="Ảnh 2" width="800"/>
</p>
<p align="center"><em>Hình 2: Giao diện chat</em></p>

<p align="center">
  <img src="picture/Screenshot 2025-09-16 140817.png" alt="Ảnh 3" width="800"/>
</p>
<p align="center"><em>Hình 3: Tin nhắn giữa server và client</em></p>

<p align="center">
  <img src="picture/Screenshot 2025-09-16 140702.png" alt="Ảnh 4" width="800"/>
</p>
<p align="center"><em>Hình 4: Thông báo khi user thoát</em></p>

<p align="center">
  <img src="picture/Screenshot 2025-09-16 140708.png" alt="Ảnh 5" width="800"/>
</p>
<p align="center"><em>Hình 5: Lưu lịch sử chat</em></p>

<p align="center">
  <img src="picture/Screenshot 2025-09-16 140754.png" alt="Ảnh 6" width="800"/>
</p>
<p align="center"><em>Hình 6: Quyền quản trị của admin</em></p>

<p align="center">
  <img src="picture/Screenshot 2025-09-16 141219.png" alt="Ảnh 7" width="800"/>
</p>
<p align="center"><em>Hình 7: Kick client</em></p>

---

## 📝 4. Cách cài đặt & chạy

**Bước 1: Cài môi trường**  
- Cài **JDK 8+** (khuyên dùng OpenJDK 17).  
- Kiểm tra:
  ```bash
  java -version
  javac -version
