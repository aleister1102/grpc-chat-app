package com.grpc.chatroom;

import com.google.protobuf.Timestamp;
import grpc.chatroom.server.ChatMessage;
import grpc.chatroom.server.User;

import java.util.Iterator;
import java.util.List;

public class Util {
  public static String parseLikeUsersToString(List<User> likeUsers) {
    return String.join(", ", likeUsers.stream().map(User::getName).toList());
  }

  public static void logChatMessage(ChatMessage chatMessage) {
    User sender = chatMessage.getSender();
    Long senderId = sender.getId();
    String senderName = sender.getName();
    String message = chatMessage.getMessage();
    Long messageId = chatMessage.getId();
    String likeUsers = parseLikeUsersToString(chatMessage.getLikeUsersList());
    Timestamp timestamp = chatMessage.getTimestamp();
    String readableTimestamp = new java.sql.Timestamp(timestamp.getSeconds()).toString();
    String messageType = chatMessage.getMessageType().toString();

    // format: [timestamp] senderName (ID: userId): message (ID: messageId, like users: , type: messageType)
    System.out.printf("\n[%s] %s - %d: %s (ID: %d, like users: %s, type: %s)\n",
            readableTimestamp,
            senderName,
            senderId,
            message,
            messageId,
            likeUsers,
            messageType);
  }
}
