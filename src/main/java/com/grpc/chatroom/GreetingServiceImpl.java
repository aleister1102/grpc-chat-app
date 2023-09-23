package com.grpc.chatroom;

import grpc.chatroom.server.GreetingServiceGrpc;
import grpc.chatroom.server.HelloRequest;
import grpc.chatroom.server.HelloResponse;
import io.grpc.stub.StreamObserver;

public class GreetingServiceImpl extends GreetingServiceGrpc.GreetingServiceImplBase {
  @Override
  public void greet(HelloRequest request, StreamObserver<HelloResponse> responseObserver) {
    System.out.println(request);
    String greeting = "Hello " +
            request.getFirstName() +
            " " +
            request.getLastName() +
            "!";

    HelloResponse helloResponse = HelloResponse.newBuilder()
            .setGreeting(greeting)
            .build();

    responseObserver.onNext(helloResponse);
    responseObserver.onCompleted();
  }
}
