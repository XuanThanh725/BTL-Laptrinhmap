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
Chat Room là một ứng dụng giao tiếp nhóm được thiết kế để hỗ trợ trao đổi tin nhắn theo thời gian thực giữa nhiều người dùng trong cùng một mạng nội bộ (LAN) thông qua giao thức UDP Multicast. Hệ thống cho phép người dùng gửi và nhận tin nhắn mà không cần một máy chủ trung tâm, tận dụng đặc điểm truyền phát dữ liệu đa điểm của multicast để tối ưu hóa hiệu suất mạng. Ứng dụng cung cấp giao diện thân thiện với người dùng, hỗ trợ lưu trữ lịch sử trò chuyện vào file log, và được xây dựng với mục tiêu đơn giản hóa quy trình giao tiếp nhóm trong các môi trường như văn phòng, lớp học, hoặc các sự kiện nội bộ.
Hệ thống bao gồm hai thành phần chính:

MulticastChatMain: Chịu trách nhiệm xử lý các tác vụ mạng, bao gồm gửi và nhận tin nhắn qua UDP Multicast.
MulticastChatApp: Quản lý giao diện người dùng, hiển thị tin nhắn, và lưu trữ dữ liệu chat.

Với thiết kế này, Chat Room không chỉ mang lại trải nghiệm giao tiếp hiệu quả mà còn dễ dàng mở rộng để tích hợp thêm các tính năng nâng cao trong tương lai.
## 🔧 2. Công nghệ sử dụng
Java: Sử dụng Java làm ngôn ngữ chính nhờ tính đa nền tảng, hỗ trợ tốt các thư viện mạng và giao diện người dùng. Phiên bản JDK (ví dụ: OpenJDK 17) được sử dụng để đảm bảo tương thích với các môi trường phát triển hiện đại.

#### Java Core và Multithreading: 
Sử dụng ExecutorService (thuộc gói java.util.concurrent) để quản lý một pool các luồng (thread) trên server, cho phép xử lý đồng thời nhiều kết nối client mà không cần tạo thủ công từng Thread. Điều này giúp cải thiện hiệu suất và quản lý tài nguyên hiệu quả hơn so với sử dụng Thread trực tiếp. Ví dụ: Executors.newFixedThreadPool() được dùng để giới hạn số luồng tối đa, mỗi luồng xử lý một client.

#### Thư Viện và Framework:
Xây dựng giao diện đồ họa (GUI) cho client sử dụng các thành phần của gói javax.swing.*:

        Java Swing: Được sử dụng để xây dựng giao diện đồ họa (GUI) với các thành phần như JFrame, JTextArea, JTextField, và JButton. Swing cung cấp khả năng tùy chỉnh cao và dễ tích hợp với các ứng dụng Java.
        
        Java Networking API: Sử dụng các lớp như MulticastSocket, DatagramPacket, để triển khai giao thức UDP Multicast, cho phép truyền dữ liệu đa điểm hiệu quả.

Swing cung cấp giao diện thân thiện, dễ tùy chỉnh mà không cần thư viện bên ngoài.

#### Công nghệ mạng:
        UDP Multicast: Sử dụng giao thức UDP Multicast để truyền tin nhắn đến nhiều người nhận trong cùng một nhóm (ví dụ: địa chỉ multicast 224.0.0.69, cổng 3000). Phương pháp này giảm tải băng thông so với gửi riêng lẻ đến từng client.
        
        Socket: Được client sử dụng để kết nối đến server thông qua địa chỉ IP và port.
        
        Threading: Sử dụng luồng (thread) trong Java để xử lý nhận tin nhắn đồng thời với giao diện, đảm bảo ứng dụng không bị treo khi chờ dữ liệu mạng.

#### File I/O:

        File I/O: Sử dụng lớp BufferedWriter và FileWriter để ghi lại lịch sử chat vào file chat_log.txt, hỗ trợ lưu trữ dài hạn và dễ dàng truy xuất.

#### Hỗ trợ:

    java.util.Date hoặc java.time.LocalDateTime: Tạo timestamp cho mỗi tin nhắn để ghi vào file và hiển thị trên giao diện, giúp người dùng theo dõi thời gian gửi.
    ArrayList: Quản lý danh sách các client đang kết nối trên server (lưu trữ PrintWriter hoặc DataOutputStream của từng client) để broadcast tin nhắn. Có thể mở rộng để lưu danh sách tên người dùng và trạng thái online/offline.
Không sử dụng thư viện bên ngoài, đảm bảo ứng dụng nhẹ và dễ triển khai trên mọi môi trường Java.
## 🚀 3. Hình ảnh các chức năng

<p align="center">
  <img src="picture/Screenshot 2025-09-16 141353.png" alt="Ảnh 1" width="800"/>
</p>

<p align="center">
  <em>Hình 1:  Chức năng đăng nhập</em>
</p>

<p align="center">
  <img src="picture/Screenshot 2025-09-16 134828.png" alt="Ảnh 2" width="800"/>
</p>
<p align="center">
  <em> Hình 2: Giao diện của server và client khi chat </em>
</p>


<p align="center">
  <img src="picture/Screenshot 2025-09-16 140817.png" alt="Ảnh 5" width="800"/>
</p>
<p align="center">
  <em> Hình 3: Hình ảnh chat của server và client  - </em>
</p>

<p align="center">
  <img src="picture/Screenshot 2025-09-16 140702.png" alt="Ảnh 5" width="800"/>
</p>
<p align="center">
  <em> Hình 4: Thông báo khi có người thoát ra - </em>
</p>

<p align="center">
  <img src="picture/Screenshot 2025-09-16 140708.png" alt="Ảnh 5" width="800"/>
</p>
<p align="center">
  <em> Hình 5: Lịch sử chat của server hoặc client - </em>
</p>

<p align="center">
  <img src="picture/Screenshot 2025-09-16 140754.png" alt="Ảnh 5" width="800"/>
</p>
<p align="center">
  <em> Hình 6: quyền quản trị của riêng admin - </em>
</p>

<p align="center">
  <img src="picture/Screenshot 2025-09-16 141219.png" alt="Ảnh 5" width="800"/>
</p>
<p align="center">
  <em> Hình 7: Khi 1 client bị kick bởi admin - </em>
</p>

## 📝 4. Các bước cài đặt
**Bước 1**: Chuẩn bị môi trường

Cài đặt Java Development Kit (JDK):

Tải JDK phiên bản 8 hoặc cao hơn (khuyến nghị OpenJDK 17) từ adoptium.net hoặc oracle.com.

Cài đặt JDK theo hướng dẫn của nhà cung cấp.

Sau cài đặt, kiểm tra bằng cách mở terminal và chạy:

        java -version
        
Nếu thấy phiên bản (ví dụ: "openjdk 17.0.1"), JDK đã sẵn sàng.

**Bước 2**: Tải 2 file MulticastChatApp.java và MulticastChatMain.java

        Đảm bảo 2 file ở chung 1 package

**Bước 3**: Mở file MulticastChatMain.java

        run java và sau đó Server sẽ khởi động và hiển thị giao diện, chờ kết nối từ client. 

        Mở một terminal mới (hoặc nhiều terminal để chạy nhiều client).


**Mọi Thắc Mắc Vui Lòng Liên Hệ Theo Thông Tin Bên Dưới Dưới**

**SDT**: 0356484203
**EMAIL**: dohuydung24@gmail.com
    
© 2025 AIoTLab, Faculty of Information Technology, DaiNam University. All rights reserved.

---
