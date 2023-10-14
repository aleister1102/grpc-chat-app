package com.grpc.chatroom;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import com.grpc.chatroom.constants.ErrorMessage;
import com.grpc.chatroom.menu.MainMenu;
import com.grpc.chatroom.utils.Converter;
import com.grpc.chatroom.utils.Logger;
import grpc.chatroom.server.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.List;

public class Client {
  private static final ManagedChannel channel = ManagedChannelBuilder
          .forAddress("localhost", Server.PORT)
          .usePlaintext()
          .build();
  public static final ChatServiceGrpc.ChatServiceBlockingStub blockingStub = ChatServiceGrpc.newBlockingStub(channel);
  public static final ChatServiceGrpc.ChatServiceStub asyncStub = ChatServiceGrpc.newStub(channel);
  public static final StreamObserver<ChatMessage> streamToServer = sendMessage();

  public static RegisterResponse register(String userName) {
    User user = User.newBuilder().setName(userName).build();
    return blockingStub.register(user);
  }

  public static UserList getUserList() {
    Empty empty = Empty.newBuilder().build();
    return blockingStub.getUsers(empty);
  }

  public static StreamObserver<ChatMessage> sendMessage() {
    return asyncStub.chat(new StreamObserver<>() {
      @Override
      public void onNext(ChatMessageFromServer chatMessageFromServer) {
        String chatMessage = Converter.convertChatMessageToString(chatMessageFromServer.getMessageFromServer());
        Logger logger = new Logger(chatMessage);
        logger.logWithCurrentTimeStamp();
      }

      @Override
      public void onError(Throwable t) {
        Logger logger = new Logger("An error have been occurred: " + t.getMessage());
        logger.logWithCurrentTimeStamp();
      }

      @Override
      public void onCompleted() {
        Logger logger = new Logger("Server is shutting down...");
        logger.logWithCurrentTimeStamp();
      }
    });
  }

  public static void joinChatRoom(User user) {
    Timestamp timestamp = Timestamp.newBuilder().setSeconds(System.currentTimeMillis()).build();

    ChatMessage joinMessage = ChatMessage.newBuilder()
            .setSender(user)
            .setMessage("Joining chat room...")
            .setMessageType(MessageType.JOIN)
            .setTimestamp(timestamp)
            .build();

    Client.streamToServer.onNext(joinMessage);
  }

  public static void broadcastMessage(User sender, String message) {
    if (!canUserSendBroadcastMessage(sender.getName())) {
      Logger logger = new Logger(ErrorMessage.BROADCAST_NOT_ALLOWED);
      logger.log();
      return;
    }

    Timestamp timestamp = Timestamp.newBuilder().setSeconds(System.currentTimeMillis()).build();
    ChatMessage chatMessage = ChatMessage.newBuilder()
            .setSender(sender)
            .setMessage(message)
            .setTimestamp(timestamp)
            .setMessageType(MessageType.BROADCAST)
            .build();
    Client.streamToServer.onNext(chatMessage);
  }

  private static boolean canUserSendBroadcastMessage(String userName) {
    ChatMessage previousMessage = getPreviousMessage(userName);
    if (previousMessage == null)
      return true;

    List<User> likeUsers = previousMessage.getLikeUsersList();
    int likeCount = calculateLikeCount(likeUsers, userName);

    return likeCount >= 2 || isNotBroadcastMessage(previousMessage);
  }

  public static ChatMessage getPreviousMessage(String userName) {
    User user = User.newBuilder().setName(userName).build();
    ChatMessageFromServer previousMessageFromServer = blockingStub.getPreviousMessage(user);
    ChatMessage previousMessage = previousMessageFromServer.getMessageFromServer();

    if (previousMessage.getMessage().equals(ErrorMessage.PREVIOUS_MESSAGE_NOT_FOUND)) {
      Logger logger = new Logger(ErrorMessage.PREVIOUS_MESSAGE_NOT_FOUND);
      logger.logWithCurrentTimeStamp();
      return null;
    } else {

      Logger logger = new Logger(String.format("Previous message of '%s':", userName));
      logger.logWithCurrentTimeStamp();
      return previousMessage;
    }
  }

  private static int calculateLikeCount(List<User> likeUsers, String userName) {
    int likeCount = 0;
    for (User likeUser : likeUsers) {
      likeCount += likeUser.getName().equals(userName) ? 1 : 0;
    }
    return likeCount;
  }

  private static boolean isNotBroadcastMessage(ChatMessage chatMessage) {
    return !chatMessage.getMessageType().equals(MessageType.BROADCAST);
  }

  public static void sendMessageDirectly(User sender, String receiverName, String message) {
    if (!isReceiverExist(receiverName)) {
      Logger logger = new Logger(ErrorMessage.RECEIVER_NOT_FOUND);
      logger.log();
      return;
    }

    User receiver = User.newBuilder().setName(receiverName).build();
    Timestamp timestamp = Timestamp.newBuilder().setSeconds(System.currentTimeMillis()).build();
    ChatMessage chatMessage = ChatMessage.newBuilder()
            .setSender(sender)
            .setReceiver(receiver)
            .setMessage(message)
            .setTimestamp(timestamp)
            .setMessageType(MessageType.DIRECT)
            .build();
    Client.streamToServer.onNext(chatMessage);
  }

  public static boolean isReceiverExist(String receiverName) {
    UserList userList = getUserList();
    List<User> users = userList.getUsersList();

    return users.stream().anyMatch(user -> user.getName().equals(receiverName));
  }

  public static ChatMessageList getMessageList() {
    Empty empty = Empty.newBuilder().build();
    return blockingStub.getMessages(empty);
  }

  public static ChatMessageFromServer likeMessage(User sender, long messageId) {
    LikeMessage likeMessage = LikeMessage.newBuilder()
            .setMessageId(messageId)
            .setSender(sender)
            .build();
    return blockingStub.like(likeMessage);
  }

  public static void main(String[] args) {
    MainMenu mainMenu = new MainMenu();
    mainMenu.execute();
  }
}
