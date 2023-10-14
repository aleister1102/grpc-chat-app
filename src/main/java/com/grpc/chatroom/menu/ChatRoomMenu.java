package com.grpc.chatroom.menu;

import com.grpc.chatroom.Client;
import com.grpc.chatroom.utils.Converter;
import com.grpc.chatroom.utils.Logger;
import com.grpc.chatroom.constants.ChatRoomMenuOption;
import com.grpc.chatroom.constants.OptionLimit;
import com.grpc.chatroom.constants.Option;
import grpc.chatroom.server.*;

public class ChatRoomMenu extends Menu {

  public ChatRoomMenu(User user) {
    this.currentUser = user;
  }

  public void display() {
    System.out.printf("%d. Turn back to the main menu\n", ChatRoomMenuOption.BACK_TO_MAIN);
    System.out.printf("%d. Send message to everybody\n", ChatRoomMenuOption.BROADCAST);
    System.out.printf("%d. Send message to a specific user\n", ChatRoomMenuOption.DIRECTLY);
    System.out.printf("%d. Get the list of chat messages\n", ChatRoomMenuOption.GET_MESSAGE_LIST);
    System.out.printf("%d. Like a message\n", ChatRoomMenuOption.LIKE_MESSAGE);
  }

  public boolean execute() {
    System.out.printf("Current username: %s\n", currentUser.getName());
    int userOption = getUserOption(OptionLimit.CHAT_ROOM_MENU);
    if (userOption == ChatRoomMenuOption.BACK_TO_MAIN) return false;

    System.out.printf("Enter message, '%s' to turn back or '%s' to exit \n", Option.BACK, Option.EXIT);
    String userMessage;
    do {
      switch (userOption) {
        case ChatRoomMenuOption.BROADCAST: {
          userMessage = getUserInput();
          if (userMessage.equals(Option.EXIT)) exit();
          if (userMessage.equals(Option.BACK)) return true;

          Client.broadcastMessage(currentUser, userMessage);
          break;
        }
        case ChatRoomMenuOption.DIRECTLY: {
          userMessage = getUserInput();
          if (userMessage.equals(Option.EXIT)) exit();
          if (userMessage.equals(Option.BACK)) return true;

          String receiverName = getUserInputWithPrompt("Enter the receiver's name: ");
          Client.sendMessageDirectly(currentUser, receiverName, userMessage);
          break;
        }
        case ChatRoomMenuOption.GET_MESSAGE_LIST: {
          displayMessageList();
          pause();
          return true;
        }
        case ChatRoomMenuOption.LIKE_MESSAGE: {
          likeMessage();
          pause();
          return true;
        }
        default:
          break;
      }
    } while (true);
  }

  private void displayMessageList() {
    ChatMessageList chatMessageList = Client.getMessageList();
    String chatMessageListAsString = Converter.convertMessageListToString(chatMessageList.getMessagesList());

    System.out.println("Chat message list: \n" + chatMessageListAsString);
  }

  private void likeMessage() {
    String userInput = getUserInputWithPrompt("Enter the message id: ");

    try {
      long messageId = Long.parseLong(userInput);
      ChatMessage chatMessageFromServer = Client.likeMessage(currentUser, messageId).getMessageFromServer();
      Logger logger = new Logger(Converter.convertChatMessageToString(chatMessageFromServer));
      logger.logWithCurrentTimeStamp();
    } catch (NumberFormatException e) {
      System.out.println("Invalid message id! Please enter a number!");
    }
  }
}
