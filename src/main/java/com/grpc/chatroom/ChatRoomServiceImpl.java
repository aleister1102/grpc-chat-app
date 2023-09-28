package com.grpc.chatroom;


import com.google.protobuf.Empty;
import grpc.chatroom.server.*;
import io.grpc.stub.StreamObserver;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatRoomServiceImpl extends ChatServiceGrpc.ChatServiceImplBase {
  private final List<User> users = new ArrayList<>();
  private static final Map<String, StreamObserver<ChatMessageFromServer>> observers = new ConcurrentHashMap<>();

  private boolean isUsernameExisted(String username) {
    for (User user : users) {
      if (user.getName().equals(username)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void join(User user, StreamObserver<JoinResponse> responseObserver) {
    String username = user.getName();
    System.out.println("A new user is joining: " + username);

    JoinResponse.Builder joinResponseBuilder = JoinResponse.newBuilder();
    JoinResponse joinResponse;

    // Check if the username is already existed
    if (isUsernameExisted(username)) {
      String failedJoinMessage = "Join the chat room failed because the username is already existed!";
      joinResponse = joinResponseBuilder.setResponseCode(JoinResponseCode.NAME_TAKEN).setMessage(failedJoinMessage).build();
    } else {
      String succeedJoinMessage = "Join the chat room successfully!";
      joinResponse = joinResponseBuilder.setResponseCode(JoinResponseCode.OK).setMessage(succeedJoinMessage).build();
      users.add(user);
    }

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
    return new StreamObserver<ChatMessage>() {
      private String username;

      @Override
      public void onNext(ChatMessage chatMessage) {
        observers.putIfAbsent(chatMessage.getSender().getName(), responseObserver);

        System.out.println("Current observers: ");
        for (var observer : observers.entrySet()) {
          System.out.println(observer.getKey());
        }

        ChatRoomUtil.logChatMessage(chatMessage);
        ChatMessageFromServer messageFromServer = ChatMessageFromServer.newBuilder()
                .setMessageFromServer(chatMessage)
                .build();

        // Broadcast the message
        for (var observer : observers.entrySet()) {
          observer.getValue().onNext(messageFromServer);
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
