package com.grpc.chatroom;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import grpc.chatroom.server.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.util.AbstractMap;
import java.util.Scanner;

public class ChatRoomClient {
  private static final ManagedChannel channel = ManagedChannelBuilder
          .forAddress("localhost", ChatRoomServer.PORT)
          .usePlaintext()
          .build();
  private static final ChatServiceGrpc.ChatServiceBlockingStub blockingStub = ChatServiceGrpc.newBlockingStub(channel);
  private static final ChatServiceGrpc.ChatServiceStub asyncStub = ChatServiceGrpc.newStub(channel);

  public static JoinResponse joinChatRoom(ChatServiceGrpc.ChatServiceBlockingStub blockingStub, User user) {
    return blockingStub.join(user);
  }

  public static UserList getUserList(ChatServiceGrpc.ChatServiceBlockingStub blockingStub) {
    Empty empty = Empty.newBuilder().build();
    return blockingStub.getUsers(empty);
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
    String userName = "QUAN";
    User user = User.newBuilder().setName(userName).build();

    System.out.println("Joining chat room...");
    JoinResponse joinResponse = joinChatRoom(blockingStub, user);
    JoinResponseCode joinResponseCode = joinResponse.getResponseCode();
    String responseMessage = joinResponse.getMessage();
    System.out.println("Joining result: " + responseMessage);

    if (joinResponseCode.equals(JoinResponseCode.NAME_TAKEN))
      return;

    System.out.println("Get user list...");
    UserList currentUserList = getUserList(blockingStub);
    System.out.println("Current users: " + currentUserList);

    System.out.println("Sending chat message...");
    StreamObserver<ChatMessage> streamToServer = sendMessage(asyncStub);

    Scanner scanner = new Scanner(System.in);
    do {
      System.out.println("Enter some message or enter 'q' to exit: ");
      String input = scanner.nextLine();
      System.out.println("You enter: " + input);
      if (input.equals("q")) {
        streamToServer.onCompleted();
        break;
      } else {
        Timestamp timestamp = Timestamp.newBuilder().setSeconds(System.currentTimeMillis()).build();
        ChatMessage chatMessage = ChatMessage.newBuilder()
                .setSender(user)
                .setMessage(input)
                .setLikeCount(0)
                .setTimestamp(timestamp)
                .build();

        streamToServer.onNext(chatMessage);
      }
    } while (scanner.hasNextLine());
  }
}
