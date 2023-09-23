package com.grpc.chatroom;


import com.google.protobuf.Empty;
import grpc.chatroom.server.*;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class ChatRoomServiceImpl extends ChatServiceGrpc.ChatServiceImplBase {

  private final List<User> users = new ArrayList<>();
  private static final LinkedHashSet<StreamObserver<ChatMessageFromServer>> observers = new LinkedHashSet<>();

  @Override
  public void join(User user, StreamObserver<JoinResponse> responseObserver) {
    System.out.println("Request user: " + user);

    users.add(user);

    JoinResponse joinResponse = JoinResponse.newBuilder()
            .setMessage("Add user successfully!")
            .build();

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
    observers.add(responseObserver);
    return new StreamObserver<ChatMessage>() {
      @Override
      public void onNext(ChatMessage chatMessage) {
        // Receive the message from the client
        System.out.println(chatMessage);

        ChatMessageFromServer messageFromServer = ChatMessageFromServer.newBuilder()
                .setMessage(chatMessage)
                .build();

        // Broadcast the message
        observers.forEach(o -> {
          o.onNext(messageFromServer);
        });
      }

      @Override
      public void onError(Throwable t) {
        observers.remove(responseObserver);
      }

      @Override
      public void onCompleted() {
        observers.remove(responseObserver);
      }
    };
  }
}
