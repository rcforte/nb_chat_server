package nbchat.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;

import nbchat.message.Chat.RequestMessage;
import nbchat.message.Chat.RequestMessage.Type;
import nbchat.message.Chat.ResponseMessage;

import org.apache.log4j.Logger;

import com.google.protobuf.InvalidProtocolBufferException;

public class ChatClient {
	private static final Logger s_logger = Logger.getLogger(ChatClient.class);
	
	private final List<ChatListener> m_listeners = new CopyOnWriteArrayList<ChatListener>();
	private final Queue<ByteBuffer> m_outputQueue = new LinkedList<ByteBuffer>();
	private final Map<Integer, RequestMessage> m_pendingRequests = new HashMap<Integer, RequestMessage>();
	private int m_nextRequestId;
	private Selector m_selector;
	private volatile boolean m_stop;
	
	public void stop() {
		s_logger.info("requesting server stop...");
		m_stop = true;
		m_selector.wakeup();
	}
	
	public void addListener(ChatListener listener) {
		m_listeners.add(listener);
	}

	public void connect() {
		try {
			m_selector = Selector.open();
			SocketChannel channel = SocketChannel.open();
			channel.configureBlocking(false);
			SelectionKey theKey = channel.register(m_selector, SelectionKey.OP_CONNECT);
			channel.connect(new InetSocketAddress("localhost", 9999));
			s_logger.info("requesting connetion...");
			
			for(;;) {
				if(m_stop) {
					break;
				}
				
				// Set write interest if there is any pending write
				synchronized(m_outputQueue) {
					if(!m_outputQueue.isEmpty()) {
						s_logger.debug("setting key interest to write...");
						if(theKey.isValid()) {
							theKey.interestOps(SelectionKey.OP_WRITE);
						}
					}
				}

				int n = m_selector.select();
				if(n == 0) {
					continue;
				}

				Iterator<SelectionKey> keys = m_selector.selectedKeys().iterator();
				while(keys.hasNext()) {
					SelectionKey key = keys.next();
					keys.remove();
					if(!key.isValid()) {
						continue;
					}

					if(key.isConnectable()) {
						handleConnect(key);
					} else if(key.isReadable()) {
						handleRead(key);
					} else if(key.isWritable()) {
						handleWrite(key);
					}
				}
			}
			
			// Stop client gracefully
			m_selector.close();
			channel.close();
		} catch(Exception e) {
			handleFail(e);
		}
	}

	private void handleFail(Exception e) {
		preProcessEvent();
		try {
			s_logger.error("failed", e);
			notifyListeners(ChatEvent.newFailEvent(e.getMessage()));
		} finally {
			postProcessEvent();
		}
	}

	protected void preProcessEvent() {
	}

	protected void postProcessEvent() {
	}

	private void handleConnect(SelectionKey key) {
		preProcessEvent();
		try {
			doHandleConnect(key);
		} finally {
			postProcessEvent();
		}
	}

	private void handleRead(SelectionKey key) {
		preProcessEvent();
		try {
			doHandleRead(key);
		} finally {
			postProcessEvent();
		}
	}

	private void handleWrite(SelectionKey key) {
		doHandleWrite(key);
	}

	private void doHandleWrite(SelectionKey key) {
		s_logger.info("handling write");
		
		try {
			synchronized(m_outputQueue) {
				ByteBuffer buffer = m_outputQueue.peek();
				// if(buffer == null) {
				// return;
				// }
				((SocketChannel) key.channel()).write(buffer);
				if(!buffer.hasRemaining()) {
					s_logger.debug("write complete, removing from queue");
					m_outputQueue.remove();
					key.interestOps(SelectionKey.OP_READ);
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	private void doHandleRead(SelectionKey key) {
		s_logger.info("handling read");
		
		SocketChannel channel = (SocketChannel) key.channel();
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();
		int nread = 0;
		
		try {
			while((nread = channel.read(buffer)) > 0) {
				responseBuffer.write(buffer.array(), 0, nread);
				buffer.clear();
			}
		} catch(IOException e) {
			nread = -1;
		}
		
		if(nread == -1) {
			s_logger.info("disconnected by the server...");
			stop();
		}
		
		byte[] responseData = responseBuffer.toByteArray();
		if(responseData.length > 0) {
			try {
				notifyListeners(ChatEvent.newGetChatRoomsEvent(
					ResponseMessage.parseFrom(responseData).getChatRoomList()
				));
			} catch(InvalidProtocolBufferException e) {
				e.printStackTrace();
			}
		}
	}

	private void doHandleConnect(SelectionKey key) {
		s_logger.info("handling connection...");
		
		try {
			((SocketChannel) key.channel()).finishConnect();
			notifyListeners(ChatEvent.newConnectEvent());
		} catch(Exception e) {
			handleFail(e);
		}
	}

	private void notifyListeners(ChatEvent event) {
		for(ChatListener listener : m_listeners) {
			switch(event.getType()) {
			case ON_CONNECT:
				listener.onConnected();
				break;
			case ON_FAIL:
				listener.onFail(event.getReason());
				break;
			case ON_GET_ROOMS:
				listener.onChatRooms(event.getChatRooms());
				break;
			}
		}
	}

	public void getChatRooms() {
		s_logger.info("sending get rooms request");
		
		RequestMessage request = RequestMessage.newBuilder()
			.setType(Type.GET_ROOMS)
			.build();
		m_pendingRequests.put(nextRequestId(), request);
		
		ByteBuffer buffer = ByteBuffer.wrap(request.toByteArray());
		synchronized(m_outputQueue) {
			m_outputQueue.offer(buffer);
		}
		
		m_selector.wakeup();
	}

	private synchronized int nextRequestId() {
		return m_nextRequestId++;
	}
}
