package nbchat.server;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import nbchat.client.ChatListener;

final class FakeChatListener implements ChatListener {
	private static final Logger s_logger = Logger.getLogger(FakeChatListener.class);
	public static final String ON_CONNECTED = "onConnected";
	public static final String ON_FAIL = "onFail";

	private final List<String> m_results;

	FakeChatListener() {
		this.m_results = new ArrayList<>();
	}

	@Override
	public void onFail(String reason) {
		s_logger.info("onFail: " + reason);
		m_results.clear();
		m_results.add(ON_FAIL);
	}

	@Override
	public void onConnected() {
		s_logger.info("onConnected");
		m_results.clear();
		m_results.add(ON_CONNECTED);
	}

	@Override
	public void onChatRooms(List<String> rooms) {
		s_logger.info("onChatRooms: " + rooms);
		m_results.clear();
		m_results.addAll(rooms);
	}

	public String getResult() {
		return m_results.get(0);
	}

	public List<String> getResults() {
		return m_results;
	}
}