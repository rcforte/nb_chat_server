package nbserver.chat;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import nbserver.Handler;

/** handles chat requests */
public class ChatHandler implements Handler {
	/**
	 * Each user will be uniquely identified by a SelectionKey. 
	 * This map provides one message queue per user. 
	 */
	private final Map<SelectionKeyWrapper, Queue<String>> m_queueByKey = 
		new HashMap<SelectionKeyWrapper, Queue<String>>();

	/**
	 * Called when the system is ready to accept a client connection. It will
	 * create the client socket channel, set it as non-blocking, and add a
	 * selection for reading operations.
	 */
	public void 
	handleAccept(SelectionKey serversk) throws Exception 
	{
		ServerSocketChannel serverch = (ServerSocketChannel) serversk.channel();
		SocketChannel clientch = serverch.accept();
		clientch.configureBlocking(false);
		SelectionKey clientsk = clientch.register(serversk.selector(), SelectionKey.OP_READ);
		m_queueByKey.put(new SelectionKeyWrapper(clientsk), new LinkedList<String>());
	}

	/**
	 * Called when the system is ready to perform a read operation. It will read
	 * whatever is available and add it to the message queue of all other clients.
	 * Once the message is queued, register write interest in each client selection
	 * key, so that they get notified when the system is ready for writes.
	 */
	public void 
	handleRead(SelectionKey selk) throws Exception 
	{
		SocketChannel channel = (SocketChannel) selk.channel();
		Charset charset = Charset.defaultCharset();
		CharsetDecoder decoder = charset.newDecoder();
		StringBuilder msg = new StringBuilder();
		ByteBuffer buffer = ByteBuffer.allocate(1024);
		int size;

		while ( (size = channel.read(buffer)) > 0) {
			buffer.flip();
			msg.append(decoder.decode(buffer).toString());
			buffer.clear();
		}
		if (size == -1) {
			handleDisconnect(selk); // disconnected
		} else {
			for (Map.Entry<SelectionKeyWrapper, Queue<String>> entry : m_queueByKey.entrySet()) {
				SelectionKey clisk = entry.getKey().getSelectionKey();
				Queue<String> climq = entry.getValue();
				if (clisk != selk) { // don't send to itself
					climq.add(msg.toString());
					clisk.interestOps(selk.interestOps() | SelectionKey.OP_WRITE);
				}
			}
		}
	}

	/**
	 * Called when the system is ready to write. It will get the messages
	 * from the queue, write whatever it can. If the message is completely
	 * written, remove it from the queue. If the queue is empty remove write
	 * interest from the selector.
	 */
	public void 
	handleWrite(SelectionKey selk) throws Exception 
	{
		SocketChannel channel = (SocketChannel) selk.channel();
		Queue<String> mqueue = m_queueByKey.get(selk);
		byte[] data = mqueue.peek().getBytes();
		int written;
		
		if ( (written = channel.write(ByteBuffer.wrap(data))) < data.length) {
			mqueue.add(new String(data, written, data.length));
		} else {
			mqueue.poll();
		}
		if (mqueue.isEmpty()) {
			selk.interestOps(selk.interestOps() & ~SelectionKey.OP_WRITE);
		}
	}
	
	private void 
	handleDisconnect(SelectionKey selkey) 
	{
		selkey.cancel();
		m_queueByKey.remove(selkey);
	}
}
