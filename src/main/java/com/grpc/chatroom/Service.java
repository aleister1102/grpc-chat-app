package com.grpc.chatroom;


import com.google.protobuf.Empty;
import grpc.chatroom.server.*;
import io.grpc.stub.StreamObserver;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Service extends ChatServiceGrpc.ChatServiceImplBase {
  private final List<User> users = new ArrayList<>();
  private long userCounter = 1;
  private final Map<String, StreamObserver<ChatMessageFromServer>> observers = new ConcurrentHashMap<>();
  private final List<ChatMessage> messages = new ArrayList<>();
  private long messageCounter = 1;


  private boolean isUsernameExisted(String username) {
    return users.stream().anyMatch(user -> user.getName().equals(username));
  }

  @Override
  public void register(User user, StreamObserver<RegisterResponse> responseObserver) {
    String username = user.getName();
    System.out.println("A new user is joining: " + username);

    RegisterResponse.Builder joinResponseBuilder = RegisterResponse.newBuilder();
    RegisterResponse joinResponse;

    // Check if the username is already existed
    if (isUsernameExisted(username)) {
      String failedJoinMessage = "Join the chat room failed because the username '%s' is already existed!";
      joinResponse = joinResponseBuilder.setResponseCode(RegisterResponseCode.NAME_TAKEN).setMessage(String.format(failedJoinMessage, username)).build();
    } else {
      User userWithId = user.toBuilder().setId(userCounter++).build();
      users.add(userWithId);

      String succeedJoinMessage = "Join the chat room successfully!";
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
  public StreamObserver<ChatMessage> chat(StreamObserver<ChatMessageFromServer> responseObserver) {
    return new StreamObserver<>() {
      private String username;

      @Override
      public void onNext(ChatMessage chatMessage) {
        ChatMessage chatMessageWithId = chatMessage.toBuilder().setId(messageCounter++).build();
        messages.add(chatMessageWithId);

        username = chatMessageWithId.getSender().getName();
        observers.putIfAbsent(username, responseObserver);

        Util.logChatMessage(chatMessageWithId);
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
        observers.remove(username);
      }

      @Override
      public void onCompleted() {
        System.out.println("Username: " + username);
        observers.remove(username);
      }
    };
  }

}
