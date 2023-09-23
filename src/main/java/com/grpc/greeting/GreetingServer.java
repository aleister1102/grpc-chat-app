package com.grpc.greeting;

import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.io.IOException;

public class GreetingServer {
  public static void main(String[] args) throws IOException, InterruptedException {
    Server server = ServerBuilder.forPort(9090)
            .addService(new GreetingServiceImpl())
            .build();

    server.start();
    System.out.println("Server is listening...");
    server.awaitTermination();
  }
}
