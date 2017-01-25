package network.chat;

import chat.common.Message;

/**
 * Created by Rafael on 1/24/2017.
 */
interface Command {
  void execute(Chat chat, Message message);
}
