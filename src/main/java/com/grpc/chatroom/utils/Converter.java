package com.grpc.chatroom.utils;

import grpc.chatroom.server.ChatMessage;
import grpc.chatroom.server.LikeMessage;
import grpc.chatroom.server.MessageType;
import grpc.chatroom.server.User;

import java.util.List;

public class Converter {
  public static String convertLikeUsersToString(List<User> likeUsers) {
    return String.join(", ", likeUsers.stream().map(User::getName).toList());
  }

  public static String convertMessageListToString(List<ChatMessage> messages) {
    return String.join("\n", messages.stream().map(Converter::convertChatMessageToString).toList());
  }

  public static String convertUserListToString(List<User> users) {
    return String.join("\n", users.stream()
            .map(user -> String.format("%d - %s", user.getId(), user.getName()))
            .toList());
  }

  public static String convertChatMessageToString(ChatMessage chatMessage) {
    User sender = chatMessage.getSender();
    Long senderId = sender.getId();
    String senderName = sender.getName();
    String message = chatMessage.getMessage();
    Long messageId = chatMessage.getId();
    String likeUsers = convertLikeUsersToString(chatMessage.getLikeUsersList());
    MessageType messageType = chatMessage.getMessageType();

    if (messageType.equals(MessageType.REPLY)) {
      // format: server: 'message'
      return String.format("server: '%s'", message);
    } else {
      // format: senderName (ID: senderId): 'message' (ID: messageId, like users: , type: messageType)
      String format = "%s - %d: '%s' (ID: %d, like users: %s, type: %s)";
      return String.format(format, senderName, senderId, message, messageId, likeUsers, messageType);
    }
  }

  public static String convertLikeMessageToString(LikeMessage likeMessage) {
    Long messageId = likeMessage.getMessageId();
    User sender = likeMessage.getSender();
    Long senderId = sender.getId();
    String senderName = sender.getName();

    // format: senderName (ID: senderId) liked message with ID = messageId
    return String.format("%s - %d is liking message with ID = %d",
            senderName,
            senderId,
            messageId);
  }
}
