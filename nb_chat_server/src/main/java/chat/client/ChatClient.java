package chat.client;

import chat.common.RequestMessage;
import chat.common.ResponseMessage;
import chat.server.NetworkEvent;
import chat.server.NetworkEventType;
import chat.server.NetworkListener;
import chat.server.NonBlockingNetwork;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static chat.common.RequestMessageType.GET_ROOMS;

public class ChatClient {
	private static final Logger logger = Logger.getLogger(ChatClient.class);

	private final String host;
	private final int port;
	private final List<ChatListener> chatListeners = new CopyOnWriteArrayList<ChatListener>();
	private final NonBlockingNetwork network;
	private final NetworkListener networkListener = networkEvent -> handle(networkEvent);

	private SocketChannel socketChannel;

	public ChatClient(String host, int port) {
		this.host = host;
		this.port = port;
		this.network = new NonBlockingNetwork();
		this.network.addNetworkListener(networkListener);
	}

	public void addChatListener(ChatListener chatListener) {
		chatListeners.add(chatListener);
	}

	public void removeChatListener(ChatListener chatListener) {
		chatListeners.remove(chatListener);
	}

	public void connect() throws IOException {
		network.connect(host, port);
	}

	public void stop() {
		logger.info("stopping client...");
		network.stop();
		network.removeNetworkListener(networkListener);
	}

	public void getChatRooms() {
		logger.info("sending get rooms requestMessage");

		String correlationId = UUID.randomUUID().toString();

		RequestMessage requestMessage = new RequestMessage();
		requestMessage.setRequestMessageType(GET_ROOMS);
		requestMessage.setCorrelationId(correlationId);

//		BlockingQueue<NetworkEvent> blockingQueue = new LinkedBlockingDeque<>();
//		Processor processor = (networkEvent) -> blockingQueue.offer(networkEvent);
//		processors.put(correlationId, processor);
//
//		NetworkEvent networkEvent = blockingQueue.poll(3, TimeUnit.SECONDS);

		String json = requestMessage.toJson();
		byte[] jsonBytes = json.getBytes();
		network.send(this.socketChannel, jsonBytes);
	}

	void handle(NetworkEvent networkEvent) {
		if (networkEvent.getType() == NetworkEventType.CONNECT) {
			logger.info("on connected");
			this.socketChannel = networkEvent.getSocketChannel();
		} else if (networkEvent.getType() == NetworkEventType.READ) {
			byte[] bytes = networkEvent.getData();
			ResponseMessage responseMessage = ResponseMessage.fromBytes(bytes);
			notifyChatListeners(responseMessage);
		}
	}

	void notifyChatListeners(ResponseMessage responseMessage) {
		for (ChatListener chatListener : chatListeners) {
			chatListener.onChatEvent(responseMessage);
		}
	}
}
