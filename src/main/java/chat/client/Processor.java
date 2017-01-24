package chat.client;

import chat.common.Message;

/**
 * Created by Rafael on 1/15/2017.
 */
public interface Processor {
  void process(Message msg);
}
