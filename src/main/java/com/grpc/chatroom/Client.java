package com.grpc.chatroom;

import com.google.protobuf.Empty;
import grpc.chatroom.server.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

public class Client {
  private static final ManagedChannel channel = ManagedChannelBuilder
          .forAddress("localhost", Server.PORT)
          .usePlaintext()
          .build();
  public static final ChatServiceGrpc.ChatServiceBlockingStub blockingStub = ChatServiceGrpc.newBlockingStub(channel);
  public static final ChatServiceGrpc.ChatServiceStub asyncStub = ChatServiceGrpc.newStub(channel);
  public static final StreamObserver<ChatMessage> streamToServer = Client.sendMessage(asyncStub);

  public static RegisterResponse register(ChatServiceGrpc.ChatServiceBlockingStub blockingStub, User user) {
    return blockingStub.register(user);
  }

  public static UserList getUserList(ChatServiceGrpc.ChatServiceBlockingStub blockingStub) {
    Empty empty = Empty.newBuilder().build();
    return blockingStub.getUsers(empty);
  }

  public static StreamObserver<ChatMessage> sendMessage(final ChatServiceGrpc.ChatServiceStub asyncStub) {
    return asyncStub.chat(new StreamObserver<ChatMessageFromServer>() {
      @Override
      public void onNext(ChatMessageFromServer chatMessageFromServer) {
        Util.logChatMessage(chatMessageFromServer.getMessageFromServer());
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
    Menu.mainMenu();
  }
}
