package com.grpc.chatroom;

import com.google.protobuf.Empty;
import grpc.chatroom.server.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class ChatRoomClient {
  private static final ManagedChannel channel = ManagedChannelBuilder
          .forAddress("localhost", ChatRoomServer.PORT)
          .usePlaintext()
          .build();
  public static final ChatServiceGrpc.ChatServiceBlockingStub blockingStub = ChatServiceGrpc.newBlockingStub(channel);
  public static final ChatServiceGrpc.ChatServiceStub asyncStub = ChatServiceGrpc.newStub(channel);

  public static boolean joinChatRoom(ChatServiceGrpc.ChatServiceBlockingStub blockingStub, User user) {
    System.out.println("Joining chat room...");
    JoinResponse joinResponse = blockingStub.join(user);
    JoinResponseCode joinResponseCode = joinResponse.getResponseCode();
    String responseMessage = joinResponse.getMessage();
    System.out.println("Joining result: " + responseMessage);

    return !joinResponseCode.equals(JoinResponseCode.NAME_TAKEN);
  }

  public static void getUserList(ChatServiceGrpc.ChatServiceBlockingStub blockingStub) {
    System.out.println("Getting current user list...");
    Empty empty = Empty.newBuilder().build();
    UserList currentUserList = blockingStub.getUsers(empty);
    System.out.println("Current users: " + currentUserList);
  }

  public static StreamObserver<ChatMessage> sendMessage(final ChatServiceGrpc.ChatServiceStub asyncStub) {
    return asyncStub.chat(new StreamObserver<ChatMessageFromServer>() {
      @Override
      public void onNext(ChatMessageFromServer chatMessageFromServer) {
        ChatRoomUtil.logChatMessage(chatMessageFromServer.getMessageFromServer());
      }

      @Override
      public void onError(Throwable t) {
        t.printStackTrace();
      }

      @Override
      public void onCompleted() {
      }
    });
  }

  public static void main(String[] args) {
    ChatRoomMenu.displayMainMenu();
    ChatRoomMenu.processMainMenu();
  }
}
