package com.grpc.greeting;

import grpc.chatroom.server.GreetingServiceGrpc;
import grpc.chatroom.server.HelloRequest;
import grpc.chatroom.server.HelloResponse;
import grpc.chatroom.server.User;
import io.grpc.stub.StreamObserver;

import java.util.ArrayList;
import java.util.List;

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
