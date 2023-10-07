package com.grpc.chatroom;

import io.grpc.ServerBuilder;

import java.io.IOException;

public class Server {

  public static final int PORT = 9090;

  public static void main(String[] args) throws IOException, InterruptedException {
    io.grpc.Server server = ServerBuilder.forPort(PORT)
            .addService(new Service())
            .build();

    server.start();
    System.out.println("Server is running on 127.0.0.1:" + PORT + " ...");
    server.awaitTermination();
  }
}
