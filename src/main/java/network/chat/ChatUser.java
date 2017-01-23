package network.chat;

import network.NonBlockingNetwork;

import java.nio.channels.SocketChannel;

public class ChatUser {
	private final SocketChannel socketChannel;
	private final String name;

	public ChatUser(SocketChannel socketChannel, String name) {
		this.name = name;
		this.socketChannel = socketChannel;
	}

	public String getName() {
		return name;
	}

	public void send(NonBlockingNetwork network, byte[] bytes) {
		network.send(socketChannel, bytes);
	}

	public boolean isChannel(SocketChannel socketChannel) {
		return this.socketChannel == socketChannel;
	}
}
