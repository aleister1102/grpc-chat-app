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
    int likeCount = chatMessage.getLikeCount();
    Timestamp timestamp = chatMessage.getTimestamp();
    String readableTimestamp = new java.sql.Timestamp(timestamp.getSeconds()).toString();

    // format: [timestamp] senderName (ID: userId): message (ID: messageId, like: likeCount)
    System.out.printf("\n[%s] %s - %d: %s (ID: %d, like: %d)\n",
            readableTimestamp,
            senderName,
            senderId,
            message,
            messageId,
            likeCount);
  }
}
