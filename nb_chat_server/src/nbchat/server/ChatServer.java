package nbchat.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.log4j.Logger;

import nbchat.message.Chat.RequestMessage;
import nbchat.message.Chat.RequestMessage.Type;
import nbchat.message.Chat.ResponseMessage;
import nbchat.server.chat.ChatUser;

import com.google.protobuf.InvalidProtocolBufferException;

/** non blocking server */
public class ChatServer {
	private static final Logger s_logger = Logger.getLogger(ChatServer.class);
	
	private final int m_port;
	private final Selector m_selector;
	private final Map<SocketChannel, ChatUser> m_userByChannel = new HashMap<>();
	private final Queue<WriteTicket> m_outputQueue = new LinkedList<>();
	private volatile boolean m_stop = false;
	
	public ChatServer(int port) throws Exception {
		m_port = port;
		m_selector = Selector.open();
	}

	/** performs NIO plumbing */
	public void start() throws Exception {
		s_logger.info("starting server...");
		
		ServerSocketChannel channel = ServerSocketChannel.open();
		channel.configureBlocking(false);
		channel.register(m_selector, SelectionKey.OP_ACCEPT);

		ServerSocket server = channel.socket();
		server.bind(new InetSocketAddress(m_port));
		s_logger.info("server started on port " + m_port);
		
		for(;;) {
			if(m_stop) {
				break;
			}
		
			if(!m_outputQueue.isEmpty()) {
				for(WriteTicket ticket : m_outputQueue) {
					ticket.setWriteInterest(m_selector);
				}
			}
			
			int n = m_selector.select();
			if(n == 0) {
				continue;
			}
			
			Iterator<SelectionKey> it = m_selector.selectedKeys().iterator();
			while(it.hasNext()) {
				SelectionKey key = it.next();
				it.remove();
				
				if(!key.isValid()) {
					continue;
				}
				
				if(key.isAcceptable()) {
					handleAccept(key);
				} else if(key.isReadable()) {
					handleRead(key);
				} else if(key.isWritable()) {
					handleWrite(key);
				}
			}
		}

		// Close the server gracefully
		try {
			s_logger.info("stopping server...");
			m_selector.close();
			channel.close();
			server.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void stop() {
		s_logger.info("requesting server stop...");
		m_stop = true;
		m_selector.wakeup();
	}
	
	public void handleAccept(SelectionKey key) 
		throws Exception 
	{
		s_logger.info("handling accept...");
		SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
		channel.configureBlocking(false);
		channel.register(key.selector(), SelectionKey.OP_READ);
		m_userByChannel.put(channel, new ChatUser());
	}
	
	public void handleRead(SelectionKey key) 
		throws Exception 
	{
		// Read incoming bytes
		SocketChannel channel = (SocketChannel) key.channel();
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		ByteArrayOutputStream requestBytes = new ByteArrayOutputStream();
		int nread = 0;
	
		try {
			while((nread = channel.read(buffer)) > 0) {
				requestBytes.write(buffer.array(), 0, nread);
				buffer.clear();
			}
		} catch(IOException e) {
			nread = -1;
		}
		
		if(nread == -1) {
			handleDisconnect(key); // disconnected
		}
		
		// Handle the request
		byte[] dataBytes = requestBytes.toByteArray();
		if(dataBytes.length > 0) {
			try {
				RequestMessage request = RequestMessage.parseFrom(dataBytes);
				if(request.getType() == Type.GET_ROOMS) {
					handleGetRooms(channel, request);
				}
			} catch(InvalidProtocolBufferException e) {
				e.printStackTrace();
			}
		}
	}

	public void handleWrite(SelectionKey key) 
		throws Exception 
	{
		// Write to client
		ByteBuffer buffer = m_outputQueue.peek().getBuffer();
		((SocketChannel) key.channel()).write(buffer);

		// If all is written, remove from queue
		if(!buffer.hasRemaining()) {
			m_outputQueue.remove();
		}
		
		// If queue is empty, set read interest
		if(m_outputQueue.isEmpty()) {
			key.interestOps(SelectionKey.OP_READ);
		}
	}

	public List<SocketChannel> getUserChannels() {
		return new ArrayList<SocketChannel>(m_userByChannel.keySet());
	}
	
	public ChatUser getUserByChannel(SocketChannel channel) {
		return m_userByChannel.get(channel);
	}

	private void handleDisconnect(SelectionKey key) {
		// TODO: remove all info related to the channel
		key.cancel();
	}

	private void handleGetRooms(SocketChannel channel, RequestMessage request) {
		s_logger.info("handling get rooms request...");
		
		ResponseMessage response = ResponseMessage.newBuilder()
			.addChatRoom("room1")
			.addChatRoom("room2")
			.build();
		
		// I need the channel to set write interest in the selection loop
		m_outputQueue.offer(new WriteTicket(
			channel, ByteBuffer.wrap(response.toByteArray())
		));
		
		m_selector.wakeup();
	}
}
