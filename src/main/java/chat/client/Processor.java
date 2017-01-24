package chat.client;

import chat.common.Response;

/**
 * Created by Rafael on 1/15/2017.
 */
public interface Processor {
  void process(Response response);
}
