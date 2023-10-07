package com.grpc.chatroom;

import com.google.protobuf.Timestamp;
import grpc.chatroom.server.ChatMessage;
import grpc.chatroom.server.User;

public class Util {
  public static void logChatMessage(ChatMessage chatMessage) {
    User sender = chatMessage.getSender();
    String senderName = sender.getName();
    String message = chatMessage.getMessage();
    Timestamp timestamp = chatMessage.getTimestamp();
    String readableTimestamp = new java.sql.Timestamp(timestamp.getSeconds()).toString();

    System.out.println("[" + readableTimestamp + "] - " + senderName + ": " + message);
  }
}
