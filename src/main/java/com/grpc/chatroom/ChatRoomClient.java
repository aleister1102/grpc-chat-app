package com.grpc.chatroom;

import com.google.protobuf.Empty;
import grpc.chatroom.server.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ChatRoomClient {

  private static List<String> messages = new ArrayList<>();

  public static void main(String[] args) {
    ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090)
            .usePlaintext()
            .build();

    ChatServiceGrpc.ChatServiceBlockingStub stub = ChatServiceGrpc.newBlockingStub(channel);
    ChatServiceGrpc.ChatServiceStub asyncStub = ChatServiceGrpc.newStub(channel);

    System.out.println("Joining chat room: ");
    User user = User.newBuilder()
            .setId("ID")
            .setName("Quan")
            .setStatus(UserStatus.ONLINE).build();

    JoinResponse response = stub.join(user);
    System.out.println("Server response: " + response);

    System.out.println("Get user list");
    Empty empty = Empty.newBuilder().build();

    UserList currentUserList = stub.getUsers(empty);
    System.out.println("Current users: " + currentUserList);

    System.out.println("Sending chat message...");
    ChatMessage chatMessage = ChatMessage.newBuilder()
            .setFrom("Process " + ProcessHandle.current().pid())
            .setMessage("Hello everyone!")
            .setLikeCount(0)
            .build();


    StreamObserver<ChatMessage> toServer = asyncStub.chat(new StreamObserver<ChatMessageFromServer>() {
      @Override
      public void onNext(ChatMessageFromServer chatMessageFromServer) {
        String message = String.format("%s: %s",
                chatMessageFromServer.getMessage().getFrom(),
                chatMessageFromServer.getMessage().getMessage());
        messages.add(message);
        System.out.println(message);
      }

      @Override
      public void onError(Throwable t) {
        t.printStackTrace();
      }

      @Override
      public void onCompleted() {
        // do nothing
      }
    });

    toServer.onNext(chatMessage);

    Scanner scanner = new Scanner(System.in);
    while (scanner.hasNextLine()) {
      int number = scanner.nextInt();
      System.out.println(number);
    }
  }
}
