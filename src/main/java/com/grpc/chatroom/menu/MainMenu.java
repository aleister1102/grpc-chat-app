package com.grpc.chatroom.menu;

import com.grpc.chatroom.Client;
import com.grpc.chatroom.utils.Converter;
import com.grpc.chatroom.constants.ErrorMessage;
import com.grpc.chatroom.constants.MainMenuOption;
import com.grpc.chatroom.constants.OptionLimit;
import com.grpc.chatroom.constants.Option;
import grpc.chatroom.server.RegisterResponse;
import grpc.chatroom.server.RegisterResponseCode;
import grpc.chatroom.server.User;
import grpc.chatroom.server.UserList;

import java.util.List;

public class MainMenu extends Menu {
  private boolean isAlreadyRegistered = false;
  private ChatRoomMenu chatRoomMenu;

  public boolean checkWhetherUserIsRegistered() {
    return isAlreadyRegistered;
  }

  public boolean checkWhetherUserIsRegisteredAndExit() {
    if (!checkWhetherUserIsRegistered()) {
      System.out.println("You have not registered yet!");
      pause();
      return false;
    }
    return true;
  }

  public void display() {
    System.out.printf("%d. Exit\n", MainMenuOption.EXIT);
    System.out.printf("%d. Register\n", MainMenuOption.REGISTER);
    System.out.printf("%d. Get the list of users\n", MainMenuOption.GET_USER_LIST);
    System.out.printf("%d. Join the chat room\n", MainMenuOption.JOIN_CHAT_ROOM);
  }

  public void execute() {
    do {
      clearScreen();
      display();

      int userOption = getUserOption(OptionLimit.MAIN_MENU);
      switch (userOption) {
        case MainMenuOption.REGISTER: {
          clearScreen();
          register();
          pause();
          break;
        }
        case MainMenuOption.GET_USER_LIST: {
          clearScreen();
          displayUserList();
          pause();
          break;
        }
        case MainMenuOption.JOIN_CHAT_ROOM: {
          if (!checkWhetherUserIsRegisteredAndExit()) break;

          Client.joinChatRoom(currentUser);

          boolean stayInChatRoom;
          do {
            clearScreen();
            chatRoomMenu.display();
            stayInChatRoom = chatRoomMenu.execute();
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

  private void register() {
    if (isAlreadyRegistered) {
      System.out.println(ErrorMessage.ALREADY_REGISTERED);
      return;
    }

    String prompt = String.format("Enter your name or press '%s' to turn back: ", Option.BACK);
    String userInput = getUserInputWithPrompt(prompt);
    if (userInput.equals(Option.BACK)) return;

    RegisterResponse registerResponse = Client.register(userInput);

    if (isRegisteredSuccessfully(registerResponse)) {
      isAlreadyRegistered = true;
      currentUser = registerResponse.getUser();
      chatRoomMenu = new ChatRoomMenu(currentUser);
    }

    System.out.println("Register result: " + registerResponse.getMessage());
  }

  public boolean isRegisteredSuccessfully(RegisterResponse registerResponse) {
    return registerResponse.getResponseCode().equals(RegisterResponseCode.OK);
  }

  private void displayUserList() {
    UserList currentUserList = Client.getUserList();
    List<User> users = currentUserList.getUsersList();

    System.out.println("Current users: \n" + Converter.convertUserListToString(users));
  }
}
