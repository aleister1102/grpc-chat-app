# Chat Application Using gRPC

## Thông tin chung

- Họ và tên: Lê Minh Quân
- MSSV: 20120356

## Thiết kế chương trình

Server bao gồm hai class chính là:

- `Server.java`: chạy server ở một địa chỉ và port nhất định.
- `Service.java`: cài đặt các thao tác chính của server.

Client bao gồm hai class chính là:

- `Client.java`: bao gồm các phương thức tương tác trực tiếp đến server thông qua các stub.
- `Menu.java`: hiển thị menu và xử lý logic chính.

Các service mà server cung cấp:
- Đăng ký tài khoản: `register(User) returns (RegisterResponse)`
- Lấy danh sách các user: `getUsers(google.protobuf.Empty) returns (UserList)`
- Nhắn tin: `chat(stream ChatMessage) returns (stream ChatMessageFromServer)`
- Lấy danh sách các tin nhắn: `getMessages(google.protobuf.Empty) returns (ChatMessageList)`
- Like tin nhắn: `like(LikeMessage) returns (ChatMessageFromServer)`;
- Lấy tin nhắn trước đó của một user: `getPreviousMessage(User) returns (ChatMessageFromServer)`;

## Cách chạy chương trình

- Thực thi file `dist/server.exe` để chạy server.
- Thực thi file `dist/client.exe` để chạy client.

## Demo

## Tham khảo

- https://grpc.io/docs/what-is-grpc/introduction/
- https://www.youtube.com/watch?v=gnchfOojMk4
- https://www.youtube.com/watch?v=DU-q5kOf2Rc
- https://retroryan8080.gitlab.io/grpc-java-workshop/




