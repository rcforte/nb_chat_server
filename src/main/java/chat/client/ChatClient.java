package chat.client;

import chat.common.RequestMessage;
import chat.common.ResponseMessage;
import network.NetworkEvent;
import network.NetworkEventType;
import network.NetworkListener;
import network.NonBlockingNetwork;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static chat.common.RequestMessageType.GET_ROOMS;
import static chat.common.RequestMessageType.GET_ROOM_USERS;
import static com.google.common.base.Preconditions.checkArgument;

public class ChatClient {
	private static final Logger logger = Logger.getLogger(ChatClient.class);

	private final String host;
	private final int port;
	private final List<ChatListener> chatListeners = new CopyOnWriteArrayList<ChatListener>();
	private final NonBlockingNetwork network;
	private final NetworkListener networkListener = networkEvent -> handle(networkEvent);
	private final Map<String, Processor> processors = Maps.newHashMap();

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

	public void getChatRooms(Processor processor) {
		logger.info("sending get rooms requestMessage");

		String correlationId = UUID.randomUUID().toString();
		RequestMessage requestMessage = new RequestMessage();
		requestMessage.setRequestMessageType(GET_ROOMS);
		requestMessage.setCorrelationId(correlationId);

		processors.put(correlationId, processor);

		String json = requestMessage.toJson();
		byte[] jsonBytes = json.getBytes();
		network.send(this.socketChannel, jsonBytes);
	}

	public void getChatRoomUsers(String chatRoom, Processor processor) {
		logger.info("sending get rooms requestMessage");


		String correlationId = UUID.randomUUID().toString();
		RequestMessage requestMessage = new RequestMessage();
		requestMessage.setRequestMessageType(GET_ROOM_USERS);
		Map<String, String> payload = Maps.newHashMap();
		payload.put("chatRoom", chatRoom);
		requestMessage.setPayload(payload);
		requestMessage.setCorrelationId(correlationId);

		processors.put(correlationId, processor);

		String json = requestMessage.toJson();
		byte[] jsonBytes = json.getBytes();
		network.send(this.socketChannel, jsonBytes);
	}

	public void send(RequestMessage requestMessage, Processor processor) {
		checkArgument(requestMessage != null, "requestMessage cannot be null");
		checkArgument(requestMessage.getCorrelationId() != null, "processor.correlationId cannot be null");
		checkArgument(processor != null, "processor cannot be null");

		processors.put(requestMessage.getCorrelationId(), processor);

		String json = requestMessage.toJson();
		byte[] jsonBytes = json.getBytes();
		network.send(this.socketChannel, jsonBytes);
	}

	void handle(NetworkEvent networkEvent) {
		if (networkEvent.getType() == NetworkEventType.CONNECT) {
			this.socketChannel = networkEvent.getSocketChannel();
		} else if (networkEvent.getType() == NetworkEventType.READ) {
			byte[] bytes = networkEvent.getData();
			ResponseMessage responseMessage = ResponseMessage.fromBytes(bytes);
			String correlationId = responseMessage.getCorrelationId();
			if (correlationId != null) {
				Processor processor = processors.get(correlationId);
				if (processor == null) {
					throw new RuntimeException("Processor not found");
				}
				processor.process(responseMessage);
			} else {
				notifyChatListeners(responseMessage);
			}
		}
	}

	void notifyChatListeners(ResponseMessage responseMessage) {
		for (ChatListener chatListener : chatListeners) {
			chatListener.onChatEvent(responseMessage);
		}
	}
}

