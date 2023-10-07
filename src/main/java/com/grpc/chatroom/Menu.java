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

  public static String getUserInput() {
    return scanner.nextLine();
  }

  public static String getUserInputWithPrompt(String prompt) {
    System.out.println(prompt);
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
    RegisterResponse registerResponse = Client.register(Client.blockingStub, user);

    if (isRegisteredSuccessfully(registerResponse)) {
      currentUser = user;
      isAlreadyRegistered = true;
    }

    System.out.println("Register result: " + registerResponse.getMessage());
  }

  private static void userList() {
    UserList currentUserList = Client.getUserList(Client.blockingStub);

    System.out.println("Current users: " + currentUserList);

  }

  private static boolean chatRoomMenu() {
    int userOption = getUserOption(MenuOptionLimit.JOIN_CHAT_ROOM_MENU);

    if(userOption == JoinChatRoomMenuOption.BACK_TO_MAIN) {
      return false;
    }

    do {
      String prompt = String.format("Enter message or enter '%s' to turn back: ", Option.BACK);
      String userInput = getUserInputWithPrompt(prompt);

      if (userInput.equals(Option.BACK)) {
        return true;
      } else {
        switch (userOption) {
          case JoinChatRoomMenuOption.BROADCAST: {
            broadcastMessage(userInput);
            break;
          }
          case JoinChatRoomMenuOption.DIRECTLY: {
            String receiverName = getUserInputWithPrompt("Enter the receiver name: ");
            sendMessageDirectly(userInput, receiverName);
            break;
          }
          default:
            break;
        }
      }
    } while (scanner.hasNextLine());

    Client.streamToServer.onCompleted();
    return true;
  }


  private static void broadcastMessage(String userInput) {
    Timestamp timestamp = Timestamp.newBuilder().setSeconds(System.currentTimeMillis()).build();
    ChatMessage chatMessage = ChatMessage.newBuilder()
            .setSender(currentUser)
            .setMessage(userInput)
            .setLikeCount(0)
            .setTimestamp(timestamp)
            .build();
    Client.streamToServer.onNext(chatMessage);
  }

  private static void sendMessageDirectly(String userInput, String receiverName) {
    Timestamp timestamp = Timestamp.newBuilder().setSeconds(System.currentTimeMillis()).build();
    User receiver = User.newBuilder().setName(receiverName).build();
    ChatMessage chatMessage = ChatMessage.newBuilder()
            .setSender(currentUser)
            .setReceiver(receiver)
            .setMessage(userInput)
            .setLikeCount(0)
            .setTimestamp(timestamp)
            .build();
    Client.streamToServer.onNext(chatMessage);
  }

}

