package com.grpc.chatroom.menu;

import com.google.protobuf.Timestamp;
import com.grpc.chatroom.Client;
import com.grpc.chatroom.constants.*;
import grpc.chatroom.server.*;

import java.util.List;
import java.util.Scanner;

public class Menu {
  private final Scanner scanner = new Scanner(System.in);
  protected User currentUser;

  public boolean isValidOption(int userOption, int optionLimit) {
    return userOption <= optionLimit && userOption >= 0;
  }

  public String getUserInput() {
    return scanner.nextLine();
  }

  public String getUserInputWithPrompt(String prompt) {
    System.out.print(prompt);
    return getUserInput();
  }

  public int getUserOption(int optionLimit) {
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

  public void clearScreen() {
    System.out.print("\033[H\033[2J");
    System.out.flush();
  }

  public void pause() {
    System.out.print("Press any key to continue...");
    getUserInput();
  }

  public void exit() {
    Client.streamToServer.onCompleted();
    System.out.println("Exiting...");
    System.exit(0);
  }
}
