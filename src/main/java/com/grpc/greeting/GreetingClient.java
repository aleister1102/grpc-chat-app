package com.grpc.greeting;

import grpc.chatroom.server.GreetingServiceGrpc;
import grpc.chatroom.server.HelloRequest;
import grpc.chatroom.server.HelloResponse;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class GreetingClient {
  public static void main(String[] args) {
    ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090)
            .usePlaintext()
            .build();

    GreetingServiceGrpc.GreetingServiceBlockingStub stub = GreetingServiceGrpc.newBlockingStub(channel);

    HelloRequest request = HelloRequest.newBuilder()
            .setFirstName("Quan")
            .setLastName("Le")
            .setAge(21)
            .addHobbies("coding")
            .addHobbies("sleeping")
            .putBagOfTricks("sleep to remember", "remember to sleep")
            .build();

    HelloResponse response = stub.greet(request);

    System.out.println(response);
  }
}
