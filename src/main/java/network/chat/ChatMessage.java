package network.chat;

/**
 * Created by Rafael on 1/21/2017.
 */
public class ChatMessage {
  private String user;
  private String room;
  private String message;

  public String getUser() {
    return user;
  }

  public void setUser(String user) {
    this.user = user;
  }

  public String getRoom() {
    return room;
  }

  public void setRoom(String room) {
    this.room = room;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }
}
