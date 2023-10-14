package com.grpc.chatroom.constants;

public class ErrorMessage {
  public static final String PREVIOUS_MESSAGE_NOT_FOUND = "Previous message not found!";
  public static final String BROADCAST_NOT_ALLOWED = "You cannot send a broadcast message because the previous message has not been liked by at least 2 other people!";
  public static final String ALREADY_REGISTERED = "You have already registered!";
}
