package com.grpc.chatroom;

import com.google.protobuf.Timestamp;
import grpc.chatroom.server.ChatMessage;
import grpc.chatroom.server.User;

public class Util {
  public static void logChatMessage(ChatMessage chatMessage) {

    User sender = chatMessage.getSender();
    Long senderId = sender.getId();
    String senderName = sender.getName();
    String message = chatMessage.getMessage();
    Long messageId = chatMessage.getId();
    Timestamp timestamp = chatMessage.getTimestamp();
    String readableTimestamp = new java.sql.Timestamp(timestamp.getSeconds()).toString();

    // format: [timestamp] senderName - userId: message (messageId)
    System.out.printf("\n[%s] %s - %d: %s (%d)\n", readableTimestamp, senderName, senderId, message, messageId);
  }
}
