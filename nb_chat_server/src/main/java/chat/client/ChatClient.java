package chat.client;

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


import org.apache.log4j.Logger;

import chat.message.Chat.RequestMessage;
import chat.message.Chat.ResponseMessage;
import chat.message.Chat.RequestMessage.Type;

import com.google.protobuf.InvalidProtocolBufferException;

public class ChatClient {
	private static final Logger LOGGER = Logger.getLogger(ChatClient.class);
	
	private final List<ChatListener> chatListeners = new CopyOnWriteArrayList<ChatListener>();
	private final Queue<ByteBuffer> outputQueue = new LinkedList<ByteBuffer>();
	private final Map<Integer, RequestMessage> pendingRequests = new HashMap<Integer, RequestMessage>();
	private int nextRequestId;
	private Selector selector;
	private volatile boolean stopped;
	
	public void stop() {
		LOGGER.info("requesting server stop...");
		stopped = true;
		selector.wakeup();
	}
	
	public void addListener(ChatListener listener) {
		chatListeners.add(listener);
	}

	public void connect() {
		try {
			selector = Selector.open();
			SocketChannel channel = SocketChannel.open();
			channel.configureBlocking(false);
			SelectionKey theKey = channel.register(selector, SelectionKey.OP_CONNECT);
			channel.connect(new InetSocketAddress("localhost", 9999));
			LOGGER.info("requesting connetion...");
			
			for(;;) {
				if(stopped) {
					break;
				}
				
				// Set write interest if there is any pending write
				synchronized(outputQueue) {
					if(!outputQueue.isEmpty()) {
						LOGGER.debug("setting key interest to write...");
						if(theKey.isValid()) {
							theKey.interestOps(SelectionKey.OP_WRITE);
						}
					}
				}

				int n = selector.select();
				if(n == 0) {
					continue;
				}

				Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
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
			selector.close();
			channel.close();
		} catch(Exception e) {
			handleFail(e);
		}
	}

	private void handleFail(Exception e) {
		preProcessEvent();
		try {
			LOGGER.error("failed", e);
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
		LOGGER.info("handling write");
		
		try {
			synchronized(outputQueue) {
				ByteBuffer buffer = outputQueue.peek();
				// if(buffer == null) {
				// return;
				// }
				((SocketChannel) key.channel()).write(buffer);
				if(!buffer.hasRemaining()) {
					LOGGER.debug("write complete, removing from queue");
					outputQueue.remove();
					key.interestOps(SelectionKey.OP_READ);
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	private void doHandleRead(SelectionKey key) {
		LOGGER.info("handling read");
		
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
			LOGGER.info("disconnected by the server...");
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
		LOGGER.info("handling connection...");
		
		try {
			((SocketChannel) key.channel()).finishConnect();
			notifyListeners(ChatEvent.newConnectEvent());
		} catch(Exception e) {
			handleFail(e);
		}
	}

	private void notifyListeners(ChatEvent event) {
		for(ChatListener listener : chatListeners) {
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
		LOGGER.info("sending get rooms request");
		
		RequestMessage request = RequestMessage.newBuilder()
			.setType(Type.GET_ROOMS)
			.build();
		pendingRequests.put(nextRequestId(), request);
		
		ByteBuffer buffer = ByteBuffer.wrap(request.toByteArray());
		synchronized(outputQueue) {
			outputQueue.offer(buffer);
		}
		
		selector.wakeup();
	}

	private synchronized int nextRequestId() {
		return nextRequestId++;
	}
}
