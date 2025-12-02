package ch.heigvd.dai.client;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientConnection {
	private final String host;
	private final int port;
	private Socket socket;
	private BufferedReader reader;
	private BufferedWriter writer;
	private final ConcurrentLinkedQueue<String> inbox;
	private volatile boolean running;
	private Thread receiveThread;

	public ClientConnection(String host, int port) {
		this.host = host;
		this.port = port;
		this.inbox = new ConcurrentLinkedQueue<>();
		this.running = false;
	}

	public void connect() throws IOException {
		socket = new Socket(host, port);
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
		writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

		running = true;
		receiveThread = Thread.startVirtualThread(this::readLoop);
	}

	private void readLoop() {
		try {
			while (running) {
				String line = reader.readLine();
				if (line == null) break;
				inbox.add(line);
			}
		} catch (IOException ignored) {
		} finally {
			running = false;
		}
	}

	public void send(String msg) throws IOException {
		writer.write(msg);
		writer.newLine();
		writer.flush();
	}

	public String receive() {
		return inbox.poll();
	}

	public void close() throws IOException {
		running = false;
		if (receiveThread != null) receiveThread.interrupt();
		if (socket != null) socket.close();
	}
}
