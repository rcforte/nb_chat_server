package chat.server;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;


import org.apache.log4j.Logger;

import chat.client.ChatClient;

final class BlockingChatClient extends ChatClient {
	private static final Logger s_logger = Logger.getLogger(BlockingChatClient.class);
	
	private final BlockingQueue<String> m_controlQueue;

	BlockingChatClient() {
		this.m_controlQueue = new LinkedBlockingQueue<>();
	}

	@Override
	protected void preProcessEvent() {
		s_logger.info("pre-processing response, clearing control blocking queue...");
		m_controlQueue.clear();
	}

	@Override
	protected void postProcessEvent() {
		s_logger.info("post-processing response, adding to control blocking queue...");
		m_controlQueue.offer("DONE");
	}

	public void waitForResult() {
		s_logger.info("waiting until response is ready...");
		try {
			m_controlQueue.take();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}