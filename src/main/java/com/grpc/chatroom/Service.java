package com.grpc.chatroom;


import com.google.protobuf.Empty;
import com.grpc.chatroom.constants.ErrorMessage;
import com.grpc.chatroom.utils.Converter;
import com.grpc.chatroom.utils.Logger;
import grpc.chatroom.server.*;
import io.grpc.stub.StreamObserver;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Service extends ChatServiceGrpc.ChatServiceImplBase {
  private final List<User> users = new ArrayList<>();
  private long userCounter = 1;

  private final Map<String, StreamObserver<ChatMessageFromServer>> observers = new ConcurrentHashMap<>();
  private final List<ChatMessage> messages = new LinkedList<>();
  private long messageCounter = 1;

  @Override
  public void register(User user, StreamObserver<RegisterResponse> responseObserver) {
    String userName = user.getName();
    Timestamp now = new Timestamp(System.currentTimeMillis());
    RegisterResponse registerResponse;

    Logger logger = new Logger(String.format("User '%s' is registering...\n", userName));
    logger.logAndWriteWithTimeStamp(now);

    if (isUsernameExisted(userName)) {
      registerResponse = buildFailedRegisterResponse(userName);
    } else {
      user = user.toBuilder().setId(userCounter++).build();
      users.add(user);

      registerResponse = buildSucceedRegisterResponse(user);
    }

    now = new Timestamp(System.currentTimeMillis());
    logger = new Logger(registerResponse.getMessage());
    logger.logAndWriteWithTimeStamp(now);

    responseObserver.onNext(registerResponse);
    responseObserver.onCompleted();
  }

  private boolean isUsernameExisted(String username) {
    return users.stream().anyMatch(user -> user.getName().equals(username));
  }

  private RegisterResponse buildFailedRegisterResponse(String userName) {
    String failedJoinMessage = String.format("User '%s' registered failed because the username is already existed!", userName);
    return RegisterResponse.newBuilder()
            .setResponseCode(RegisterResponseCode.NAME_TAKEN)
            .setMessage(String.format(failedJoinMessage, userName))
            .build();
  }

  private RegisterResponse buildSucceedRegisterResponse(User user) {
    String succeedJoinMessage = String.format("User '%s' registered successfully!", user.getName());
    return RegisterResponse.newBuilder()
            .setResponseCode(RegisterResponseCode.OK)
            .setMessage(succeedJoinMessage)
            .setUser(user)
            .build();
  }


  @Override
  public void getUsers(Empty request, StreamObserver<UserList> responseObserver) {
    UserList userList = UserList.newBuilder()
            .addAllUsers(users)
            .build();

    responseObserver.onNext(userList);
    responseObserver.onCompleted();
  }


  @Override
  public void getMessages(Empty request, StreamObserver<ChatMessageList> responseObserver) {
    ChatMessageList chatMessageList = ChatMessageList.newBuilder()
            .addAllMessages(messages)
            .build();

    responseObserver.onNext(chatMessageList);
    responseObserver.onCompleted();
  }

  @Override
  public StreamObserver<ChatMessage> chat(StreamObserver<ChatMessageFromServer> responseObserver) {
    return new StreamObserver<>() {
      private String senderName;
      private Long senderId;

      @Override
      public void onNext(ChatMessage chatMessage) {
        chatMessage = chatMessage.toBuilder().setId(messageCounter++).build();
        String receiverName = chatMessage.getReceiver().getName();
        MessageType messageType = chatMessage.getMessageType();
        Timestamp now = new Timestamp(System.currentTimeMillis());

        // Save user's observer if message type is JOIN
        if (messageType.equals(MessageType.JOIN)) {
          senderName = chatMessage.getSender().getName();
          senderId = chatMessage.getSender().getId();
          observers.putIfAbsent(senderName, responseObserver);
        }

        // Save message
        messages.add(chatMessage);
        Logger logger = new Logger(Converter.convertChatMessageToString(chatMessage));
        logger.logAndWriteWithTimeStamp(now);

        // Send message
        ChatMessageFromServer chatMessageFromServer = buildChatMessageFromServer(chatMessage);
        for (var observer : observers.entrySet()) {
          String observerName = observer.getKey();
          StreamObserver<ChatMessageFromServer> streamObserver = observer.getValue();

          // Broadcast the message
          if (!chatMessage.hasReceiver()) {
            streamObserver.onNext(chatMessageFromServer);
          } else {
            // Send the message to the receiver and the sender
            if (receiverName.equals(observerName) || senderName.equals(observerName)) {
              streamObserver.onNext(chatMessageFromServer);
            }
          }
        }
      }

      private ChatMessageFromServer buildChatMessageFromServer(ChatMessage chatMessage) {
        return ChatMessageFromServer.newBuilder()
                .setMessageFromServer(chatMessage)
                .build();
      }

      @Override
      public void onError(Throwable t) {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Logger logger = new Logger(String.format("User: %s - %d has error: %s", senderName, senderId, t.getMessage()));
        logger.logAndWriteWithTimeStamp(now);

        removeCurrentSender();
      }

      @Override
      public void onCompleted() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        Logger logger = new Logger(String.format("User: %s - %d is leaving...", senderName, senderId));
        logger.logAndWriteWithTimeStamp(now);

        removeCurrentSender();
      }

      private void removeCurrentSender() {
        if (senderName != null) {
          User currentSender = users.stream().filter(u -> u.getName().equals(senderName)).findFirst().orElse(null);
          users.remove(currentSender);
          observers.remove(senderName);
        }
      }
    };
  }

  @Override
  public void like(LikeMessage requestMessage, StreamObserver<ChatMessageFromServer> responseObserver) {
    long messageId = requestMessage.getMessageId();
    User sender = requestMessage.getSender();
    String senderName = sender.getName();
    ChatMessage likingMessage = findMessageById(messageId);
    Timestamp now = new Timestamp(System.currentTimeMillis());

    Logger logger = new Logger(Converter.convertLikeMessageToString(requestMessage));
    logger.logAndWriteWithTimeStamp(now);

    ChatMessageFromServer messageFromServer;
    String errorMessage = "";

    // Check whether the message is found
    if (likingMessage == null) {
      errorMessage = String.format("Message with id '%d' is not found!", messageId);
      messageFromServer = buildChatMessageFromServer(errorMessage);
    } else {
      // Check whether the sender has liked the message
      if (isAlreadyLikedByUser(likingMessage, sender)) {
        errorMessage = String.format("Message with id '%d' is already liked by user '%s'!", messageId, senderName);
        messageFromServer = buildChatMessageFromServer(errorMessage);
      } else {
        // Replace the liking message with the liked message
        ChatMessage likedMessage = buildLikedMessage(likingMessage, sender);
        messages.set((int) messageId - 1, likedMessage);

        // Broadcast the liked message
        messageFromServer = buildChatMessageFromServer(requestMessage);
        for (var observer : observers.entrySet()) {
          if (observer.getKey().equals(senderName)) continue;
          StreamObserver<ChatMessageFromServer> streamObserver = observer.getValue();
          streamObserver.onNext(messageFromServer);
        }
      }
    }

    if (!errorMessage.isEmpty()) {
      logger = new Logger(errorMessage);
      logger.logAndWriteWithTimeStamp(now);
    }

    responseObserver.onNext(messageFromServer);
    responseObserver.onCompleted();
  }

  private ChatMessage findMessageById(Long messageId) {
    return messages.stream()
            .filter(message -> message.getId() == messageId)
            .findFirst()
            .orElse(null);
  }

  private ChatMessageFromServer buildChatMessageFromServer(String message) {
    return ChatMessageFromServer.newBuilder()
            .setMessageFromServer(buildChatMessage(message))
            .build();
  }

  private ChatMessageFromServer buildChatMessageFromServer(ChatMessage chatMessage) {
    return ChatMessageFromServer.newBuilder()
            .setMessageFromServer(chatMessage)
            .build();
  }

  private ChatMessageFromServer buildChatMessageFromServer(LikeMessage likeMessage) {
    String message = Converter.convertLikeMessageToString(likeMessage);
    return ChatMessageFromServer.newBuilder()
            .setMessageFromServer(buildChatMessage(message))
            .build();
  }

  private ChatMessage buildChatMessage(String message) {
    return ChatMessage.newBuilder()
            .setMessage(message)
            .setMessageType(MessageType.REPLY)
            .build();
  }

  private boolean isAlreadyLikedByUser(ChatMessage chatMessage, User user) {
    return chatMessage.getLikeUsersList().stream()
            .anyMatch(likeUser -> likeUser.getName().equals(user.getName()));
  }

  private ChatMessage buildLikedMessage(ChatMessage likingMessage, User sender) {
    return likingMessage.toBuilder()
            .addLikeUsers(sender)
            .build();
  }

  @Override
  public void getPreviousMessage(User sender, StreamObserver<ChatMessageFromServer> responseObserver) {
    String senderName = sender.getName();
    ChatMessage previousMessage = findPreviousMessageOfSenderName(senderName);
    ChatMessage errorMessage = ChatMessage.newBuilder().setMessage(ErrorMessage.PREVIOUS_MESSAGE_NOT_FOUND).build();

    Optional<ChatMessage> optionalMessage = Optional.ofNullable(previousMessage);
    ChatMessage returnMessage = optionalMessage.orElse(errorMessage);
    ChatMessageFromServer messageFromServer = buildChatMessageFromServer(returnMessage);

    responseObserver.onNext(messageFromServer);
    responseObserver.onCompleted();
  }

  public ChatMessage findPreviousMessageOfSenderName(String senderName) {
    return messages.stream()
            .filter(message -> message.getSender().getName().equals(senderName))
            .reduce((first, second) -> second)
            .orElse(null);
  }
}
