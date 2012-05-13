package nbserver.chat;
public class Message {
	private static final int LIMIT_TIME = 10000;
	
	private final long m_time;
	private final String m_content;
	
	public long time() { return m_time; }
	public String content() { return m_content; }
	
	public Message(String content, long time) {
		m_content = content;
		m_time = time;
	}
	
	public boolean isOld() {
		return System.currentTimeMillis() - m_time >= LIMIT_TIME;
	}
}
