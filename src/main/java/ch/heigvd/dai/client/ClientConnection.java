package ch.heigvd.dai.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;

class ClientConnection {
	private String host;
	private int port;
	private Socket socket;
	private BufferedReader reader;
	private BufferedWriter writer;

	private ConcurrentLinkedQueue<String> inbox;
	private volatile boolean running;
	private Thread receiveThread;
	

	ClientConnection(String host, int port) {
		this.host = host;
		this.port = port;
		this.inbox = new ConcurrentLinkedQueue<String>();
		this.running = false;
	}

	// TODO: do we want to handle exceptions here?
	public void connect() throws UnknownHostException, IOException {	
		socket = new Socket(host, port);
		reader = new BufferedReader(new InputStreamReader(
			socket.getInputStream(), StandardCharsets.UTF_8));
		writer = new BufferedWriter(new OutputStreamWriter(
			socket.getOutputStream(), StandardCharsets.UTF_8));

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

	// TODO: do we want to handle exceptions here?
	public void send(String msg) throws IOException {
		writer.write(msg);
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
