package com.grpc.chatroom;

import com.google.protobuf.Timestamp;
import grpc.chatroom.server.ChatMessage;
import grpc.chatroom.server.User;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class Util {
  public static String parseLikeUsersToString(List<User> likeUsers) {
    return String.join(", ", likeUsers.stream().map(User::getName).toList());
  }

  public static String logChatMessage(ChatMessage chatMessage) {
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
    String log = String.format("[%s] %s - %d: %s (ID: %d, like users: %s, type: %s)\n",
            readableTimestamp,
            senderName,
            senderId,
            message,
            messageId,
            likeUsers,
            messageType);
    System.out.printf(log);
    return log;
  }

  public static void writeLog(String log) {
    String fileName = "chatroom.log";
    try {
      FileWriter writer = new FileWriter(fileName, true);
      writer.write(log);
      writer.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
