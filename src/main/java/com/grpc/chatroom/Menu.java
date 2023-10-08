package com.grpc.chatroom;

import com.google.protobuf.Timestamp;
import com.grpc.chatroom.constants.JoinChatRoomMenuOption;
import com.grpc.chatroom.constants.MainMenuOption;
import com.grpc.chatroom.constants.MenuOptionLimit;
import com.grpc.chatroom.constants.Option;
import grpc.chatroom.server.*;

import java.util.Scanner;

public class Menu {
  private static final Scanner scanner = new Scanner(System.in);

  private static boolean isAlreadyRegistered = false;
  private static User currentUser;

  public static boolean isValidOption(int userOption, int optionLimit) {
    return userOption <= optionLimit && userOption >= 0;
  }

  public static boolean isRegisteredSuccessfully(RegisterResponse registerResponse) {
    return registerResponse.getResponseCode().equals(RegisterResponseCode.OK);
  }

  public static boolean checkWhetherUserIsRegistered() {
    return isAlreadyRegistered;
  }

  public static boolean checkWhetherUserIsRegisteredAndExit() {
    if (!checkWhetherUserIsRegistered()) {
      System.out.println("You have not registered yet!");
      pause();
      return false;
    }
    return true;
  }

  public static String getUserInput() {
    return scanner.nextLine();
  }

  public static String getUserInputWithPrompt(String prompt) {
    System.out.print(prompt);
    return getUserInput();
  }

  public static int getUserOption(int optionLimit) {
    int userOption;
    do {
      String userInput = getUserInputWithPrompt("Enter your choice: ");
      try {
        userOption = Integer.parseInt(userInput);
      } catch (NumberFormatException e) {
        userOption = Option.INVALID;

        System.out.println("Invalid option! Please enter a number!");
      }
    } while (!isValidOption(userOption, optionLimit));
    return userOption;
  }

  public static void clearScreen() {
    System.out.print("\033[H\033[2J");
    System.out.flush();
  }

  public static void pause() {
    System.out.print("Press any key to continue...");
    getUserInput();
  }

  public static void exit() {
    Client.streamToServer.onCompleted();
    System.out.println("Exiting...");
    System.exit(0);
  }

  public static void displayMainMenu() {
    System.out.printf("%d. Register\n", MainMenuOption.REGISTER);
    System.out.printf("%d. Get the list of users\n", MainMenuOption.GET_USER_LIST);
    System.out.printf("%d. Join the chat room\n", MainMenuOption.JOIN_CHAT_ROOM);
    System.out.printf("%d. Exit\n", MainMenuOption.EXIT);
  }

  private static void displayChatRoomMenu() {
    System.out.printf("%d. Turn back to the main menu\n", JoinChatRoomMenuOption.BACK_TO_MAIN);
    System.out.printf("%d. Send message to everybody\n", JoinChatRoomMenuOption.BROADCAST);
    System.out.printf("%d. Send message to a specific user\n", JoinChatRoomMenuOption.DIRECTLY);
    System.out.printf("%d. Get the list of chat messages\n", JoinChatRoomMenuOption.GET_CHAT_MESSAGE_LIST);
    System.out.printf("%d. Like a message\n", JoinChatRoomMenuOption.LIKE_MESSAGE);
  }

  public static void mainMenu() {
    do {
      clearScreen();
      displayMainMenu();

      int userOption = getUserOption(MenuOptionLimit.MAIN_MENU);
      switch (userOption) {
        case MainMenuOption.REGISTER: {
          clearScreen();
          registerMenu();
          pause();
          break;
        }
        case MainMenuOption.GET_USER_LIST: {
          clearScreen();
          userList();
          pause();
          break;
        }
        case MainMenuOption.JOIN_CHAT_ROOM: {
          if (!checkWhetherUserIsRegisteredAndExit()) break;

          boolean stayInChatRoom;
          do {
            clearScreen();
            displayChatRoomMenu();
            stayInChatRoom = chatRoomMenu();
          } while (stayInChatRoom);
          break;
        }
        case MainMenuOption.EXIT: {
          exit();
          break;
        }
        default:
          break;
      }
    } while (true);
  }

  private static void registerMenu() {
    if (isAlreadyRegistered) {
      System.out.println("You have already registered!");
      return;
    }

    String prompt = String.format("Enter your name or press '%s' to turn back: ", Option.BACK);
    String userInput = getUserInputWithPrompt(prompt);

    if (userInput.equals(Option.BACK)) {
      return;
    }

    User user = User.newBuilder().setName(userInput).build();
    RegisterResponse registerResponse = Client.register(user);
    long userId = registerResponse.getUser().getId();
    user = user.toBuilder().setId(userId).build();

    if (isRegisteredSuccessfully(registerResponse)) {
      isAlreadyRegistered = true;
      currentUser = user;
    }

    System.out.println("Register result: " + registerResponse.getMessage());
  }

  private static void userList() {
    UserList currentUserList = Client.getUserList();

    System.out.println("Current users: " + currentUserList);

  }

  private static boolean chatRoomMenu() {
    System.out.printf("Current username: %s\n", currentUser.getName());
    System.out.printf("Current observer: %s\n", Client.streamToServer);
    int userOption = getUserOption(MenuOptionLimit.JOIN_CHAT_ROOM_MENU);
    if (userOption == JoinChatRoomMenuOption.BACK_TO_MAIN) {
      return false;
    }

    System.out.printf("Enter message, '%s' to turn back or '%s' to exit \n", Option.BACK, Option.EXIT);
    String userMessage;
    do {


      switch (userOption) {
        case JoinChatRoomMenuOption.BROADCAST: {
          userMessage = getUserInput();
          if (userMessage.equals(Option.EXIT)) exit();
          if (userMessage.equals(Option.BACK)) return true;

          broadcastMessage(userMessage);
          break;
        }
        case JoinChatRoomMenuOption.DIRECTLY: {
          userMessage = getUserInput();
          if (userMessage.equals(Option.EXIT)) exit();
          if (userMessage.equals(Option.BACK)) return true;

          String prompt = "Enter the receiver's name: ";
          String receiverName = getUserInputWithPrompt(prompt);
          sendMessageDirectly(userMessage, receiverName);
          break;
        }
        case JoinChatRoomMenuOption.GET_CHAT_MESSAGE_LIST: {
          chatMessageList();
          pause();
          return true;
        }
        case JoinChatRoomMenuOption.LIKE_MESSAGE: {
          likeMessage();
          pause();
          return true;
        }
        default:
          break;
      }
    } while (true);
  }


  private static void broadcastMessage(String userMessage) {
    Timestamp timestamp = Timestamp.newBuilder().setSeconds(System.currentTimeMillis()).build();
    ChatMessage chatMessage = ChatMessage.newBuilder().setSender(currentUser).setMessage(userMessage).setLikeCount(0).setTimestamp(timestamp).build();
    Client.streamToServer.onNext(chatMessage);
  }

  private static void sendMessageDirectly(String userInput, String receiverName) {
    Timestamp timestamp = Timestamp.newBuilder().setSeconds(System.currentTimeMillis()).build();
    User receiver = User.newBuilder().setName(receiverName).build();
    ChatMessage chatMessage = ChatMessage.newBuilder().setSender(currentUser).setReceiver(receiver).setMessage(userInput).setLikeCount(0).setTimestamp(timestamp).build();
    Client.streamToServer.onNext(chatMessage);
  }

  private static void chatMessageList() {
    ChatMessageList chatMessageList = Client.getMessageList();

    System.out.println("Current chat message list: " + chatMessageList);
  }

  private static void likeMessage() {
    String prompt = "Enter the message id: ";
    String userInput = getUserInputWithPrompt(prompt);

    try {
      long messageId = Long.parseLong(userInput);
      ChatMessageFromServer chatMessageFromServer = Client.like(messageId);
      Util.logChatMessage(chatMessageFromServer.getMessageFromServer());
    } catch (NumberFormatException e) {
      System.out.println("Invalid message id! Please enter a number!");
    }
  }
}

