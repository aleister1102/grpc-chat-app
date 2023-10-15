# Chat Application Using gRPC

## Thông tin chung
- Họ và tên: Lê Minh Quân
- MSSV: 20120356

## Thiết kế chương trình

### Proto
Các kiểu dữ liệu:
- `User`: người dùng.
- `UserList`: danh sách người dùng.
- `RegisterResponse`: phản hồi khi đăng ký người dùng.
- `ChatMessage`: thông điệp chat. Có các thông tin cơ bản chẳng hạn như người gửi, người nhận, loại message và danh sách người dùng đã like message.
- `ChatMessageFromServer`: bản chất là ChatMessage nhưng được gửi từ server.
- `ChatMessageList`: danh sách các message.
- `LikeMessage`: thông điệp dùng khi like một message nào đó.

Các service mà server cung cấp:
- Đăng ký tài khoản: `register(User) returns (RegisterResponse)`
- Lấy danh sách các user: `getUsers(google.protobuf.Empty) returns (UserList)`
- Nhắn tin: `chat(stream ChatMessage) returns (stream ChatMessageFromServer)`
- Lấy danh sách các tin nhắn: `getMessages(google.protobuf.Empty) returns (ChatMessageList)`
- Like tin nhắn: `like(LikeMessage) returns (ChatMessageFromServer)`
- Lấy tin nhắn trước đó của một user: `getPreviousMessage(User) returns (ChatMessageFromServer)`

### Các lớp đối tượng
Chương trình bao gồm hai class chính là:
- Service: cài đặt các dịch vụ chính của server.
- Client.java: cài đặt các hàm tương tác với server.

Các class hỗ trợ khác:
- Converter: giúp chuyển đổi kiểu dữ liệu.
- Logger: giúp ghi log ra console và file.
- Menu và các subclass của nó: giúp hiển thị và xử lý input nhập vào của người dùng.

### Log
- Tập tin log trong quá trình thực thi sẽ có tên là `chatroom.log`.
- Tất cả các sự kiện liên quan đến server đều sẽ được ghi vào tập tin log.
- Mỗi dòng log đều sẽ có timestamp cho biết thời điểm xảy ra sự kiện.

## Demo
- https://www.youtube.com/watch?v=O-qVLasEjEs

## Tham khảo
- https://grpc.io/docs/what-is-grpc/introduction/
- https://www.youtube.com/watch?v=gnchfOojMk4
- https://www.youtube.com/watch?v=DU-q5kOf2Rc
- https://retroryan8080.gitlab.io/grpc-java-workshop/
