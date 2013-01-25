package nbchat.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import nbchat.message.Chat;
import nbchat.server.chat.ChatUser;

import com.google.protobuf.InvalidProtocolBufferException;

/** handles chat requests */
public class ChatHandler implements Handler {
	private Map<SocketChannel, ChatUser> m_userByChannel = new HashMap<>();

	public void handleAccept(SelectionKey key) throws Exception {
		System.out.println("Accepting connection");
		SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
		channel.configureBlocking(false);
		channel.register(key.selector(), SelectionKey.OP_READ);
		m_userByChannel.put(channel, new ChatUser());
	}
	
	public void handleRead(SelectionKey key) throws Exception {
		// Read incoming bytes
		System.out.println("handleRead");
		SocketChannel channel = (SocketChannel) key.channel();
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		ByteArrayOutputStream requestBytes = new ByteArrayOutputStream();
		try {
			while (channel.read(buffer) > 0) {
				requestBytes.write(buffer.array());
				buffer.clear();
			}
		} catch (IOException e) {
			handleDisconnect(key); // disconnected
		}

		// Handle the request
		byte[] dataBytes = requestBytes.toByteArray();
		if (dataBytes.length > 0) {
			handleIncomingData(dataBytes);
		}
	}

	public void handleWrite(SelectionKey selk) throws Exception {
		SocketChannel channel = (SocketChannel) selk.channel();
		Queue<String> mqueue = null;//m_queueByKey.get(selk);
		byte[] data = mqueue.peek().getBytes();
		int written;

		if ((written = channel.write(ByteBuffer.wrap(data))) < data.length) {
			mqueue.add(new String(data, written, data.length));
		} else {
			mqueue.poll();
		}
		
		if (mqueue.isEmpty()) {
			selk.interestOps(selk.interestOps() & ~SelectionKey.OP_WRITE);
		}
	}

	public List<SocketChannel> getUserChannels() {
		return new ArrayList<SocketChannel>(m_userByChannel.keySet());
	}
	
	public ChatUser getUserByChannel(SocketChannel channel) {
		return m_userByChannel.get(channel);
	}

	private void handleDisconnect(SelectionKey key) {
		key.cancel();
	}

	private void handleIncomingData(byte[] data) {
		try {
			Chat.RequestMessage request = Chat.RequestMessage.parseFrom(data);
			System.out.println(request.getType());
		} catch (InvalidProtocolBufferException e) {
			e.printStackTrace();
		}
	}
}
