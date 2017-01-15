package chat.client;

import chat.common.ResponseMessage;

/**
 * Created by Rafael on 1/15/2017.
 */
public interface Processor {
	void process(ResponseMessage responseMessage);
}
