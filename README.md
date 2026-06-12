# GreenFocus - Ứng dụng Tập trung & Trồng cây 🌳

**GreenFocus** là một ứng dụng di động hỗ trợ tập trung và quản lý thời gian theo phương pháp Pomodoro, được xây dựng trên nền tảng **Android Native (Kotlin + Jetpack Compose)** kết hợp hệ thống máy chủ **Firebase**. Lấy cảm hứng từ ứng dụng Forest nổi tiếng, GreenFocus kết hợp cơ chế trò chơi hóa (gamification) để biến thời gian tập trung thành các cây ảo sinh động, giúp người dùng tăng hiệu suất làm việc và học tập.

---

## 🌟 Tính năng chính

### 1. Đồng hồ Pomodoro & Chế độ Tập trung Sâu
- **Đồng hồ tùy chỉnh**: Lựa chọn thời gian tập trung với giao diện vòng tròn đếm ngược trực quan, mượt mà.
- **Lựa chọn hạt giống**: Chọn loại cây muốn trồng trước khi bắt đầu phiên.
- **Tập trung sâu (Deep Focus)**: Khi kích hoạt, ứng dụng sẽ chặn các ứng dụng gây xao nhãng nằm trong danh sách đen. Nếu người dùng thoát khỏi ứng dụng trong thời gian tập trung, cây sẽ bị héo úa.

### 2. Chế độ Tập trung Nhóm (Survival Room)
- **Tạo/Tham gia phòng**: Người dùng có thể tạo phòng tập trung chung hoặc tham gia phòng của bạn bè bằng Mã phòng độ dài 6 ký tự.
- **Nguyên lý sinh tồn**: Cả nhóm sẽ cùng tập trung trong một khoảng thời gian. Nếu **bất kỳ ai** trong phòng vi phạm chế độ tập trung sâu, phòng sẽ thất bại ngay lập tức và cây của tất cả thành viên đều bị héo úa.
- **Đồng bộ thời gian thực**: Trạng thái thành viên, đếm ngược được đồng bộ tức thì thông qua Firebase Realtime Database.

### 3. Khu rừng ảo (Virtual Forest - 3D & AR)
- **Khu rừng 2D**: Hiển thị lưới các cây mà người dùng đã trồng thành công theo lịch sử phiên tập trung.
- **Trải nghiệm Thực tế ảo Tăng cường (AR - Augmented Reality)**:
  - Sử dụng **Google ARCore** và thư viện **Sceneview** để chiếu khu rừng ảo của bạn lên bề mặt thực tế (sàn nhà, bàn làm việc) qua camera điện thoại.
  - Sử dụng mô hình 3D chất lượng cao định dạng `.glb` cho các loại cây.
  - Tích hợp **Thuật toán Căn ô ảo (Grid Snapping)** với khoảng cách `0.5m` giúp các cây tự động xếp thẳng hàng thẳng lối đẹp mắt khi đặt xuống không gian thực.
  - Có thể di chuyển cây trồng trong không gian AR.

### 4. Cửa hàng cây (Store) & Đồng xu (Coins)
- Tích lũy đồng xu từ các phiên tập trung thành công, số xu tương ứng với thời gian tập trung.
- Sử dụng đồng xu để mở khóa các giống cây mới trong Cửa hàng như: *Cây Thông (Pine), Cây Phong (Maple), Cây Dừa (Palm), Xương Rồng (Cactus), Tre (Bamboo), Hoa Anh Đào (Sakura)*.

### 5. Bảng xếp hạng (Leaderboard) & Mạng xã hội
- Tìm kiếm và gửi lời mời kết bạn.
- Bảng xếp hạng cập nhật thời gian thực so sánh số lượng cây đã trồng giữa người dùng với bạn bè của họ.
- Nhận thông báo đẩy thời gian thực về Lời mời kết bạn hoặc Lời mời vào phòng tập trung nhóm.

### 6. Thống kê & Phân tích chi tiết (Analytics)
- Theo dõi chỉ số tổng quan: Tổng số phút tập trung, Số phiên hoàn thành, Tỉ lệ hoàn thành mục tiêu, Chuỗi ngày tập trung liên tục (Streak).
- Biểu đồ trực quan theo Tuần, Tháng, Năm để đánh giá hiệu suất.
- Biểu đồ phân bổ tỷ lệ thời gian tập trung theo từng loại cây.

---

## 🛠️ Công nghệ sử dụng

### 1. Client App (Android Native)
- **Ngôn ngữ**: Kotlin
- **UI Framework**: Jetpack Compose
- **Dependency Injection**: Manual DI thông qua `AppContainer` để khởi tạo các repository tập trung.
- **Xử lý nền & Dịch vụ**: Android Foreground Service (`TimerForegroundService`) đảm bảo đếm ngược chính xác ngay cả khi tắt màn hình, tích hợp thông báo trạng thái.
- **AR & 3D Engine**: 
  - **Google ARCore SDK** cho khả năng nhận diện chuyển động và phát hiện mặt phẳng.
  - **Sceneview / ARSceneview** để tải và kết xuất mô hình 3D `.glb` mượt mà trong Jetpack Compose.
- **Asynchronous & Reactive**: Kotlin Coroutines & Flow để xử lý luồng dữ liệu thời gian thực không đồng bộ.

### 2. Backend & Cloud Infrastructure
- **Firebase Authentication**: Xác thực người dùng (Đăng ký/Đăng nhập bằng Email & Password).
- **Firebase Cloud Firestore**: Cơ sở dữ liệu tài liệu lưu trữ thông tin Người dùng (`users`), Phiên tập trung (`sessions`), Lời mời kết bạn (`friend_requests`), và Cấu hình.
- **Firebase Realtime Database**: Đồng bộ hóa siêu tốc trạng thái phòng tập trung (`rooms`), thành viên, và tin nhắn văn bản tức thời.
- **Firebase Cloud Messaging (FCM)**: Gửi thông báo đẩy đến các thiết bị di động.
- **Firebase Cloud Functions**: Viết bằng TypeScript, thực hiện xử lý ở phía server như:
  - Tự động kích hoạt thông báo đẩy FCM khi có yêu cầu kết bạn hoặc lời mời vào phòng.
  - Lắng nghe cập nhật phòng tập trung để ghi nhận hàng loạt phiên tập trung thành công (`ALIVE`) hoặc thất bại (`DEAD`) của tất cả các thành viên một cách tự động và toàn vẹn.

---

## 📂 Cấu trúc thư mục dự án

```text
GreenFocus/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/greenfocus/
│   │   │   │   ├── data/                 # Tầng dữ liệu (Models, Repositories, AppContainer)
│   │   │   │   │   ├── model/            # User, Tree, FocusSession, SurvivalRoom, Member, Message, FriendRequest
│   │   │   │   │   └── repository/       # AuthRepository, UserRepository, DataRepository, ForestRepository, UserSettingRepository
│   │   │   │   ├── di/                   # Cấu hình phụ thuộc Firebase
│   │   │   │   ├── fcm/                  # Quản lý FCM token và Service nhận thông báo đẩy
│   │   │   │   ├── ui/                   # Giao diện người dùng (Compose screens & components)
│   │   │   │   │   ├── components/       # Các widget tái sử dụng (BottomNavBar, CoinContainer, TreeCard, v.v.)
│   │   │   │   │   ├── navigation/       # Quản lý định tuyến màn hình (NavGraph, Screen)
│   │   │   │   │   ├── theme/            # Màu sắc, Font chữ, Theme ứng dụng
│   │   │   │   │   └── screen/           # Các màn hình chức năng chính (auth, pomodoro, forest, store, stats, social, profile)
│   │   │   │   └── util/                 # Các tiện ích (TimerManager, TimerForegroundService, SoundManager, PermissionManager)
│   │   │   ├── assets/                   # Chứa trees.json (danh mục cây) và models/ (mô hình 3D .glb)
│   │   │   └── res/                      # Tài nguyên giao diện (Drawables, Mipmaps, Raw sounds, XMLs)
│   │   └── test/                         # Unit tests cho logic nghiệp vụ
│   └── build.gradle.kts                  # Cấu hình build của module app
├── functions/                            # Mã nguồn Firebase Cloud Functions (TypeScript)
│   ├── src/
│   │   └── index.ts                      # Triggers cho FCM và tự động lưu phiên nhóm
│   └── package.json
├── firestore.rules                       # Luật bảo mật Cloud Firestore
├── firestore.indexes.json                # Định nghĩa các chỉ mục kép Firestore
├── settings.gradle.kts                   # Định nghĩa các module và Version Catalog của Gradle
└── gradle/
    └── libs.versions.toml                # Quản lý phiên bản dependencies tập trung
```

---
