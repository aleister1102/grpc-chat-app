package com.grpc.chatroom;

import com.google.protobuf.Timestamp;
import grpc.chatroom.server.*;
import io.grpc.stub.StreamObserver;


import java.util.Scanner;

public class ChatRoomMenu {

  private static final Scanner scanner = new Scanner(System.in);

  private static boolean isAlreadyJoined = false;
  private static User currentUser;

  public static class MenuOptionLimit {
    public static final int MAIN_MENU = 4;
  }

  public static class MainMenuOption {
    public static final int INVALID_OPTION = -1;
    public static final int JOIN_CHAT_ROOM = 1;
    public static final int GET_USER_LIST = 2;
    public static final int SEND_MESSAGE = 3;
    public static final int EXIT = 4;
  }

  public static class RegisterMenuOption {
    public static final String BACK = "back";
    public static final String EXIT = "exit";
  }

  public static boolean isValidOption(int userOption, int optionLimit) {
    return userOption <= optionLimit && userOption >= 0;
  }

  public static String getUserInput() {
    return scanner.nextLine();
  }

  public static int getUserOption(int optionLimit) {
    int userOption;
    do {
      String userInput = getUserInput();
      try {
        userOption = Integer.parseInt(userInput);
      } catch (NumberFormatException e) {
        userOption = MainMenuOption.INVALID_OPTION;
      }
    } while (!isValidOption(userOption, optionLimit));

    return userOption;
  }

  public static void clearScreen() {
    System.out.print("\033[H\033[2J");
    System.out.flush();
  }

  public static void pause() {
    System.out.println("Press any key to continue...");
    getUserInput();
  }

  public static void exit() {
    System.out.println("Exiting...");
    System.exit(0);
  }

  public static void displayMainMenu() {
    clearScreen();
    System.out.printf("%d. Join the chat room\n", MainMenuOption.JOIN_CHAT_ROOM);
    System.out.printf("%d. Get the list of users\n", MainMenuOption.GET_USER_LIST);
    System.out.printf("%d. Send a message\n", MainMenuOption.SEND_MESSAGE);
    System.out.printf("%d. Exit\n", MainMenuOption.EXIT);
    System.out.print("Enter your choice: ");
  }

  public static void processMainMenu() {
    do {
      displayMainMenu();
      int userOption = getUserOption(MenuOptionLimit.MAIN_MENU);

      switch (userOption) {
        case MainMenuOption.JOIN_CHAT_ROOM: {
          if (isAlreadyJoined) {
            System.out.println("You have already joined the chat room");
            pause();
            continue;
          }
          displayRegisterMenu();
          processRegisterMenu();
          break;
        }
        case MainMenuOption.GET_USER_LIST: {
          displayUserListMenu();
          break;
        }
        case MainMenuOption.SEND_MESSAGE: {
          displaySendMessageMenu();
          processSendMessageMenu();
          break;
        }
        case MainMenuOption.EXIT: {
          exit();
        }
        default:
          break;
      }
    } while (true);
  }

  private static void displayRegisterMenu() {
    clearScreen();
    System.out.printf("Enter %s to turn back to the main menu\n", RegisterMenuOption.BACK);
    System.out.printf("Enter %s to exit\n", RegisterMenuOption.EXIT);
    System.out.print("Enter your name: ");
  }

  private static void processRegisterMenu() {
    String userInput = getUserInput();
    if (userInput.equals(RegisterMenuOption.BACK)) {
      return;
    } else if (userInput.equals(RegisterMenuOption.EXIT)) {
      exit();
    } else {
      User user = User.newBuilder().setName(userInput).build();
      isAlreadyJoined = ChatRoomClient.joinChatRoom(ChatRoomClient.blockingStub, user);
      if (isAlreadyJoined) {
        currentUser = user;
      }
      pause();
    }
  }

  private static void displayUserListMenu() {
    clearScreen();
    ChatRoomClient.getUserList(ChatRoomClient.blockingStub);
    pause();
  }

  private static void displaySendMessageMenu() {
    clearScreen();
    // TODO: Implement send option (broadcast, directly, like message)
    System.out.println("Sending chat message...");
  }

  private static void processSendMessageMenu() {
    StreamObserver<ChatMessage> streamToServer = ChatRoomClient.sendMessage(ChatRoomClient.asyncStub);

    Scanner scanner = new Scanner(System.in);
    do {
      System.out.println("Enter some message or enter 'q' to exit: ");
      String input = scanner.nextLine();
      System.out.println("You enter: " + input);
      if (input.equals("q")) {
        streamToServer.onCompleted();
        break;
      } else {
        Timestamp timestamp = Timestamp.newBuilder().setSeconds(System.currentTimeMillis()).build();
        ChatMessage chatMessage = ChatMessage.newBuilder()
                .setSender(currentUser)
                .setMessage(input)
                .setLikeCount(0)
                .setTimestamp(timestamp)
                .build();

        streamToServer.onNext(chatMessage);
      }
    } while (scanner.hasNextLine());
  }
}

