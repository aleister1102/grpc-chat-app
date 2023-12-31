package com.grpc.chatroom.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;

public class Logger {
  private final String message;

  public Logger(String message) {
    this.message = message;
  }

  public String toStringWithTimestamp(Timestamp timestamp) {
    return "[" +
            timestamp.toLocalDateTime() +
            "] " +
            this.message +
            "\n";
  }

  public void logWithCurrentTimeStamp() {
    Timestamp now = new Timestamp(System.currentTimeMillis());
    System.out.println(this.toStringWithTimestamp(now));
  }

  public void logWithTimeStamp(Timestamp timestamp) {
    System.out.println(this.toStringWithTimestamp(timestamp));
  }

  public void log() {
    System.out.println(this.message);
  }

  public void log(String logMessage) {
    System.out.println(logMessage);
  }

  public void logAndWriteWithTimeStamp(Timestamp timestamp) {
    if (!this.message.isEmpty()) {
      String logMessage = this.toStringWithTimestamp(timestamp);
      this.log(logMessage);
      this.writeLog(logMessage);
    }
  }

  // TODO: fix redundant line break in log file
  public void writeLog(String log) {
    try {
      String logFileName = "chatroom.log";
      FileWriter writer = new FileWriter(logFileName, true);
      writer.write(log);
      writer.close();
    } catch (IOException e) {
      System.out.println("An error have been occurred while writing log to file: " + e.getMessage());
    }
  }
}
