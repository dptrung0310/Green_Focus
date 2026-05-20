# Kế hoạch liên kết cây đã sở hữu và Cửa hàng (Store)

Tài liệu này mô tả kế hoạch kỹ thuật để liên kết danh sách các cây đã sở hữu (`unlockedTreeIds`) của người dùng với danh sách các hạt giống có thể lựa chọn trên màn hình tập trung Pomodoro (Home).

## User Review Required

> [!IMPORTANT]
> - Sau khi liên kết, người dùng đăng ký tài khoản mới sẽ bắt đầu với **0 xu** và chỉ có duy nhất cây Sồi mặc định (`default_oak`) để lựa chọn. Các loại cây khác (như Thông, Anh Đào, Phong, v.v.) sẽ cần được mua từ tab **Store** trước khi có thể xuất hiện và chọn lựa tại tab **Home**.
> - Danh sách cây được lấy tập trung từ repository thông qua tệp cấu hình [trees.json](file:///d:/Android_Dev/GreenFocus/app/src/main/assets/trees.json). Danh sách tĩnh `DataSource.plants` cũ sẽ bị loại bỏ để đảm bảo tính nhất quán của ID tài nguyên.

## Proposed Changes

### Tầng Dữ liệu & DI (Data & DI Layer)

#### [MODIFY] [Tree.kt](file:///d:/Android_Dev/GreenFocus/app/src/main/java/com/example/greenfocus/data/model/Tree.kt)
- Định nghĩa thêm đối tượng tĩnh `DEFAULT_TREE` trong `TreeType.companion` tương ứng với cây Sồi mặc định (`default_oak`) để làm giá trị khởi tạo mặc định cho UI và Service khi chưa tải xong dữ liệu.

#### [MODIFY] [AppContainer.kt](file:///d:/Android_Dev/GreenFocus/app/src/main/java/com/example/greenfocus/data/AppContainer.kt)
- Khai báo thêm giao diện `forestRepository` vào interface `AppContainer`.
- Triển khai khởi tạo lười biếng (`lazy`) cho `forestRepository` trong `DefaultAppContainer` sử dụng `ProdForestRepository(context)`.

#### [MODIFY] [TimerManager.kt](file:///d:/Android_Dev/GreenFocus/app/src/main/java/com/example/greenfocus/util/TimerManager.kt)
- Thay đổi giá trị khởi tạo của thuộc tính `currentTree` trong `TimerState` từ `DataSource.plants[0]` thành `TreeType.DEFAULT_TREE`.

#### [MODIFY] [DataSource.kt](file:///d:/Android_Dev/GreenFocus/app/src/main/java/com/example/greenfocus/data/DataSource.kt)
- Loại bỏ hoàn toàn danh sách tĩnh `plants` để tránh dư thừa dữ liệu. Giữ lại danh sách nhạc chuông hoàn thành `winRingtone` phục vụ cho cài đặt hệ thống.

---

### Tầng UI & ViewModel (UI & ViewModel Layer)

#### [MODIFY] [PomodoroUiState.kt](file:///d:/Android_Dev/GreenFocus/app/src/main/java/com/example/greenfocus/ui/screen/pomodoro/PomodoroUiState.kt)
- Thay đổi giá trị khởi tạo của `selectedTree` thành `TreeType.DEFAULT_TREE`.
- Thêm thuộc tính `unlockedTrees: List<TreeType>` đại diện cho danh sách các cây người dùng đã sở hữu (mặc định khởi tạo chỉ có `TreeType.DEFAULT_TREE`).

#### [MODIFY] [PomodoroViewModel.kt](file:///d:/Android_Dev/GreenFocus/app/src/main/java/com/example/greenfocus/ui/screen/pomodoro/PomodoroViewModel.kt)
- Cập nhật hàm tạo để nhận thêm `forestRepository: ForestRepository`.
- Cập nhật `PomodoroViewModel.Factory` để cung cấp `forestRepository` từ `AppContainer`.
- Thêm cơ chế tải danh sách toàn bộ loài cây từ `forestRepository.getAllTrees()` khi khởi tạo ViewModel.
- Lắng nghe sự thay đổi của người dùng từ `userRepository.getCurrentUserProfileFlow()`, lọc danh sách cây toàn phần dựa theo `unlockedTreeIds` của người dùng và cập nhật vào `PomodoroUiState.unlockedTrees`.
- Khi cập nhật cây được chọn thông qua `updateSelectedTree(value: TreeType)`, gọi thêm `timerManager.setTree(value)` (thay thế cho lời gọi hàm cũ `timerManager.setTreeId(...)` không tồn tại).

#### [MODIFY] [PomodoroScreen.kt](file:///d:/Android_Dev/GreenFocus/app/src/main/java/com/example/greenfocus/ui/screen/pomodoro/PomodoroScreen.kt)
- Cập nhật `TreeSelectionRow` để truyền `pomodoroUiState.unlockedTrees` vào tham số `items()` của `LazyRow` thay vì sử dụng danh sách tĩnh `DataSource.plants` cũ.

---

## Verification Plan

### Manual Verification
1. Lập một tài khoản mới tinh (tên tùy chọn, ví dụ `test_user_link@example.com`).
2. Xác nhận rằng khi vào tab **Home**, bạn chỉ nhìn thấy duy nhất cây Sồi (`default_oak`) trong thanh chọn cây và số coin hiển thị là `0 xu`.
3. Hoàn thành thử một phiên tập trung Pomodoro ngắn (ví dụ: đặt thời gian 1 phút) để nhận xu, hoặc chỉnh sửa trực tiếp số coin trên bảng Firestore `users` của user tương ứng lên `300 xu`.
4. Chuyển sang tab **Store**, xác nhận cây Thông (`pine`) hiển thị giá `300 xu` ở trạng thái **BUYABLE**.
5. Nhấn mua cây Thông và xác nhận thành công. Số xu bị trừ tương ứng và hiển thị của cây Thông chuyển sang **OWNED**.
6. Quay lại tab **Home**, kiểm tra xem cây Thông đã xuất hiện bên cạnh cây Sồi trong danh sách chọn chưa. Chọn cây Thông mới mua và chạy đếm giờ để xác nhận cây mọc thành công là cây Thông.
