package network.chat;

import chat.common.RequestMessage;
import chat.common.RequestMessageType;
import chat.common.ResponseMessage;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import network.NetworkEvent;
import network.NetworkEventType;
import network.NetworkListener;
import network.NonBlockingNetwork;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChatServer {
    private static final Logger logger = Logger.getLogger(ChatServer.class);
    private final int port;
    private final NonBlockingNetwork network;
    private final NetworkListener networkListener = networkEvent -> handle(networkEvent);
    private Chat chat;

    public ChatServer(int port) {
        this.port = port;
        this.network = new NonBlockingNetwork();
        this.network.addNetworkListener(networkListener);
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }

    public void start() throws IOException {
        logger.info("starting server");
        network.bind(port);
    }

    public void stop() throws IOException {
        logger.info("stopping server...");
        network.stop();
    }

    void handle(NetworkEvent networkEvent)
    {
        if (networkEvent.getType() == NetworkEventType.DISCONNECT)
        {
            logger.info("handling disconnect");
            chat.removeUser(networkEvent.getSocketChannel());
        }
        else if (networkEvent.getType() == NetworkEventType.READ)
        {
            logger.info("decoding request");
            SocketChannel socketChannel = networkEvent.getSocketChannel();
            byte[] data = networkEvent.getData();
            RequestMessage requestMessage = RequestMessage.fromBytes(data);

            if (requestMessage.getRequestMessageType() == RequestMessageType.GET_ROOMS)
            {
                logger.info("handling request: type=GET_ROOMS");
                ResponseMessage responseMessage = new ResponseMessage();
                responseMessage.setCorrelationId(requestMessage.getCorrelationId());
                for (ChatRoom room : chat.getRooms())
                {
                    responseMessage.addRoom(room.getName());
                }

                logger.info("encoding response");
                String json = responseMessage.toJson();
                byte[] jsonBytes = json.getBytes();

                logger.info("sending response");
                network.send(socketChannel, jsonBytes);
            }
            else if (requestMessage.getRequestMessageType() == RequestMessageType.GET_ROOM_USERS)
            {
                Map<String, String> payload = requestMessage.getPayload();
                String chatRoom = payload.get("chatRoom");
                logger.info("handling request: room="+chatRoom);
                List<ChatUser> users = chat.getUsers(chatRoom);
                List<String> chatUsers = users.stream().map(u -> u.getName()).collect(Collectors.toList());
                logger.info("room users: " + chatUsers);

                logger.info("encoding response");
                ResponseMessage responseMessage = new ResponseMessage();
                responseMessage.setCorrelationId(requestMessage.getCorrelationId());
                Map<String, List<String>> responsePayload = Maps.newHashMap();
                responsePayload.put("chatUsers", chatUsers);
                responseMessage.setPayload(responsePayload);
                String json = responseMessage.toJson();
                byte[] jsonBytes = json.getBytes();

                logger.info("sending response");
                network.send(socketChannel, jsonBytes);
            }
            else if (requestMessage.getRequestMessageType() == RequestMessageType.JOIN)
            {
                Map<String, String> payload = requestMessage.getPayload();
                String user = payload.get("user");
                String room = payload.get("room");
                logger.info("handling request: type=JOIN, user=" + user + ", room=" + room);
                chat.join(socketChannel, user, room);

                logger.info("encoding response");
                ResponseMessage responseMessage = new ResponseMessage();
                Map<String, List<String>> responsePayload = Maps.newHashMap();
                responsePayload.put("message", Lists.newArrayList(String.format("%s has joined the chat", user)));
                responseMessage.setPayload(responsePayload);
                String json = responseMessage.toJson();
                byte[] jsonBytes = json.getBytes();

                logger.info("sending response");
                chat.send(network, room, jsonBytes);
            }
            else if (requestMessage.getRequestMessageType() == RequestMessageType.MESSAGE)
            {
                Map<String, String> payload = requestMessage.getPayload();
                String room = payload.get("room");
                String user = payload.get("user");
                String message = payload.get("message");
                logger.info("handling request: type=MESSAGE, room="+room+", user="+user+", message=" + message);

                logger.info("encoding response");
                ResponseMessage responseMessage = new ResponseMessage();
                Map<String, List<String>> responsePayload = Maps.newHashMap();
                responsePayload.put("message", Lists.newArrayList(user+" says: " + message));
                responseMessage.setPayload(responsePayload);
                String json = responseMessage.toJson();
                byte[] jsonBytes = json.getBytes();

                logger.info("sending response");
                chat.send(network, room, jsonBytes);
            }
            else if (requestMessage.getRequestMessageType() == RequestMessageType.LEAVE)
            {
                Map<String, String> payload = requestMessage.getPayload();
                String room = payload.get("room");
                logger.info("handling request: type=LEAVE, room="+room);
                ChatUser user = chat.removeUser(socketChannel);

                logger.info("encoding response");
                Map<String, List<String>> responsePayload = Maps.newHashMap();
                responsePayload.put("message", Lists.newArrayList(user.getName()+" left the room"));
                ResponseMessage responseMessage = new ResponseMessage();
                responseMessage.setPayload(responsePayload);
                String json = responseMessage.toJson();
                byte[] jsonBytes = json.getBytes();

                logger.info("sending response");
                chat.send(network, room, jsonBytes);
            }
        }
    }
}
