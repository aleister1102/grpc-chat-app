package com.grpc.chatroom;


import com.google.protobuf.Empty;
import com.grpc.chatroom.constants.ErrorMessage;
import grpc.chatroom.server.*;
import io.grpc.stub.StreamObserver;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Service extends ChatServiceGrpc.ChatServiceImplBase {
  private final List<User> users = new ArrayList<>();
  private long userCounter = 1;
  private final Map<String, StreamObserver<ChatMessageFromServer>> observers = new ConcurrentHashMap<>();
  private final List<ChatMessage> messages = new LinkedList<>();
  private long messageCounter = 1;


  private boolean isUsernameExisted(String username) {
    return users.stream().anyMatch(user -> user.getName().equals(username));
  }

  @Override
  public void register(User user, StreamObserver<RegisterResponse> responseObserver) {
    String username = user.getName();
    Timestamp now = new Timestamp(System.currentTimeMillis());
    String log = String.format("[%s] User '%s' is registering...\n", now, username);
    System.out.printf(log);
    Util.writeLog(log);

    RegisterResponse.Builder joinResponseBuilder = RegisterResponse.newBuilder();
    RegisterResponse joinResponse;

    // Check if the username is already existed
    if (isUsernameExisted(username)) {
      String failedJoinMessage = String.format("[%s] User '%s' registered failed because the username '%s' is already existed!\n", now, username, username);
      Util.writeLog(failedJoinMessage);
      joinResponse = joinResponseBuilder.setResponseCode(RegisterResponseCode.NAME_TAKEN).setMessage(String.format(failedJoinMessage, username)).build();
    } else {
      User userWithId = user.toBuilder().setId(userCounter++).build();
      users.add(userWithId);

      String succeedJoinMessage = String.format("[%s] User '%s' registered successfully!\n", now, username);
      Util.writeLog(succeedJoinMessage);
      joinResponse = joinResponseBuilder
              .setResponseCode(RegisterResponseCode.OK)
              .setMessage(succeedJoinMessage)
              .setUser(userWithId)
              .build();
    }

    System.out.println("Current user list: ");
    users.forEach(u -> System.out.println(u.getName()));

    responseObserver.onNext(joinResponse);
    responseObserver.onCompleted();
  }

  @Override
  public void getUsers(Empty request, StreamObserver<UserList> responseObserver) {
    UserList userList = UserList.newBuilder()
            .addAllUsers(users)
            .build();

    responseObserver.onNext(userList);
    responseObserver.onCompleted();
  }


  @Override
  public void getMessages(Empty request, StreamObserver<ChatMessageList> responseObserver) {
    ChatMessageList chatMessageList = ChatMessageList.newBuilder()
            .addAllMessages(messages)
            .build();

    responseObserver.onNext(chatMessageList);
    responseObserver.onCompleted();
  }

  @Override
  public StreamObserver<ChatMessage> chat(StreamObserver<ChatMessageFromServer> responseObserver) {
    return new StreamObserver<>() {
      private String username;

      @Override
      public void onNext(ChatMessage chatMessage) {
        MessageType messageType = chatMessage.getMessageType();
        if (messageType.equals(MessageType.JOIN)) {
          username = chatMessage.getSender().getName();
          observers.putIfAbsent(username, responseObserver);
        }

        ChatMessage chatMessageWithId = chatMessage.toBuilder().setId(messageCounter++).build();
        messages.add(chatMessageWithId);

        String log = Util.logChatMessage(chatMessageWithId);
        Util.writeLog(log);
        ChatMessageFromServer messageFromServer = ChatMessageFromServer.newBuilder()
                .setMessageFromServer(chatMessageWithId)
                .build();

        User receiver = chatMessageWithId.getReceiver();
        for (var observer : observers.entrySet()) {
          String observerName = observer.getKey();
          StreamObserver<ChatMessageFromServer> streamObserver = observer.getValue();

          // Broadcast the message
          if (!chatMessageWithId.hasReceiver()) {
            streamObserver.onNext(messageFromServer);
          } else {
            // Send the message to the receiver
            if (receiver.getName().equals(observerName) || username.equals(observerName)) {
              streamObserver.onNext(messageFromServer);
            }
          }
        }
      }

      @Override
      public void onError(Throwable t) {
        if (username != null) {
          User user = users.stream().filter(u -> u.getName().equals(username)).findFirst().orElse(null);
          users.remove(user);
          observers.remove(username);

        }
      }

      @Override
      public void onCompleted() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        String log = String.format("[%s] User: '%s' is leaving...", now, username);
        System.out.println(log);
        Util.writeLog(log);
        if (username != null) {
          User user = users.stream().filter(u -> u.getName().equals(username)).findFirst().orElse(null);
          users.remove(user);
          observers.remove(username);
        }
      }
    };
  }

  @Override
  public void like(LikeMessage likeMessage, StreamObserver<ChatMessageFromServer> responseObserver) {
    long messageId = likeMessage.getMessageId();
    ChatMessage chatMessage = messages.stream()
            .filter(message -> message.getId() == messageId)
            .findFirst()
            .orElse(null);
    ChatMessageFromServer.Builder messageFromServerBuilder = ChatMessageFromServer.newBuilder();

    Timestamp now = new Timestamp(System.currentTimeMillis());
    if (chatMessage == null) {
      String messageNotFound = "[%s] Message with id '%d' is not found!";
      String errorMessage = String.format(messageNotFound, now, messageId);
      ChatMessageFromServer messageFromServer = messageFromServerBuilder.setMessageFromServer(ChatMessage.newBuilder().setMessage(errorMessage).build()).build();
      responseObserver.onNext(messageFromServer);
    } else {
      // Check whether the user has liked the message
      boolean isUserLiked = chatMessage.getLikeUsersList().stream()
              .anyMatch(user -> user.getName().equals(likeMessage.getSender().getName()));

      if (isUserLiked) {
        String messageAlreadyLiked = "[%s] Message with id '%d' is already liked by user '%s'!";
        String errorMessage = String.format(messageAlreadyLiked, now, messageId, likeMessage.getSender().getName());
        ChatMessageFromServer messageFromServer = messageFromServerBuilder.setMessageFromServer(ChatMessage.newBuilder().setMessage(errorMessage).build()).build();
        responseObserver.onNext(messageFromServer);
      } else {
        ChatMessage likedMessage = chatMessage.toBuilder()
                .addLikeUsers(likeMessage.getSender())
                .build();
        messages.set((int) messageId - 1, likedMessage);
        ChatMessageFromServer messageFromServer = messageFromServerBuilder
                .setMessageFromServer(likedMessage)
                .build();

        // Broadcast the like message
        for (var observer : observers.entrySet()) {
          StreamObserver<ChatMessageFromServer> streamObserver = observer.getValue();
          streamObserver.onNext(messageFromServer);
        }
        responseObserver.onNext(messageFromServer);
      }
    }
    responseObserver.onCompleted();
  }

  @Override
  public void getPreviousMessage(User request, StreamObserver<ChatMessageFromServer> responseObserver) {
    String username = request.getName();
    ChatMessage previousMessage = messages.stream()
            .filter(message -> message.getSender().getName().equals(username))
            .reduce((first, second) -> second)
            .orElse(null);

    ChatMessage previousMessageNotFound = ChatMessage.newBuilder().setMessage(ErrorMessage.PREVIOUS_MESSAGE_NOT_FOUND).build();

    ChatMessageFromServer messageFromServer = ChatMessageFromServer.newBuilder()
            .setMessageFromServer(previousMessage != null ? previousMessage : previousMessageNotFound)
            .build();

    responseObserver.onNext(messageFromServer);
    responseObserver.onCompleted();
  }
}
