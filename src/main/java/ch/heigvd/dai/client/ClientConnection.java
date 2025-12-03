package ch.heigvd.dai.client;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Manages the TCP connection to the CHCH chat server.
 * Handles sending messages, receiving responses, and managing the message queue.
 */
public class ClientConnection {
	private final String host;
	private final int port;
	private Socket socket;
	private BufferedReader reader;
	private BufferedWriter writer;
	private final BlockingQueue<String> inbox;
	private volatile boolean running;
	private Thread receiveThread;

	/**
	 * Creates a new client connection instance.
	 * 
	 * @param host the server hostname or IP address
	 * @param port the server port number
	 */
	public ClientConnection(String host, int port) {
		this.host = host;
		this.port = port;
		this.inbox = new LinkedBlockingQueue<>();
		this.running = false;
	}

	/**
	 * Connects to the server.
	 * 
	 * @return true if connection was successful, false otherwise
	 */
	public boolean connect() {
		try {
			socket = new Socket(host, port);
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
			writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

			running = true;
			receiveThread = Thread.startVirtualThread(this::readLoop);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Background thread loop that continuously reads messages from the server.
	 * Adds all received messages to the inbox queue for processing.
	 * Stops when the connection is closed or an error occurs.
	 */
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

	/**
	 * Sends a message to the server.
	 * 
	 * @param msg the message to send
	 * @return true if the message was sent successfully, false otherwise
	 */
	public boolean send(String msg) {
		if (writer == null) {
			return false;
		}
		try {
			writer.write(msg);
			writer.newLine();
			writer.flush();
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Non-blocking method to receive a message from the server.
	 * Returns immediately with a message if available, or null if the queue is empty.
	 * 
	 * @return the next message from the server, or null if no message is available
	 */
	public String receive() {
		return inbox.poll();
	}

	/**
	 * Puts a message back into the inbox queue.
	 * Used when a message was accidentally consumed but should be handled by another method.
	 * 
	 * @param msg the message to put back
	 */
	public void putBack(String msg) {
		inbox.offer(msg);
	}

	/**
	 * Waits for any message from the server (command response or broadcast).
	 * 
	 * @param timeoutMillis Maximum time to wait in milliseconds
	 * @return The received message, or null if timeout
	 * @throws InterruptedException if interrupted while waiting
	 */
	public String waitForResponse(long timeoutMillis) throws InterruptedException {
		return inbox.poll(timeoutMillis, TimeUnit.MILLISECONDS);
	}

	/**
	 * Waits for a command response from the server. This method filters out
	 * broadcast messages (RECEIVE, JOINED) and only returns command responses.
	 * Blocks until a command response is received or the timeout expires.
	 * 
	 * @param timeoutMillis Maximum time to wait in milliseconds
	 * @param broadcastHandler Callback to handle broadcast messages received while waiting
	 * @return The command response, or null if timeout
	 * @throws InterruptedException if interrupted while waiting
	 */
	public String waitForCommandResponse(long timeoutMillis, java.util.function.Consumer<String> broadcastHandler) throws InterruptedException {
		long startTime = System.currentTimeMillis();
		long remainingTime = timeoutMillis;
		
		while (remainingTime > 0) {
			String msg = inbox.poll(remainingTime, TimeUnit.MILLISECONDS);
			if (msg == null) {
				// probably timeout
				return null;
			}
			
			if (msg.equals("OK") || msg.startsWith("ERROR") || 
			    msg.startsWith("USRLIST") || msg.startsWith("CHANLIST")) {
				return msg;
			}
			
			if (broadcastHandler != null) {
				broadcastHandler.accept(msg);
			}
			
			long elapsed = System.currentTimeMillis() - startTime;
			remainingTime = timeoutMillis - elapsed;
		}
		
		return null; // timeout
	}

	/**
	 * Waits for a command response from the server without handling broadcasts.
	 * Use this if you want to handle broadcasts separately.
	 * 
	 * @param timeoutMillis Maximum time to wait in milliseconds
	 * @return The command response, or null if timeout
	 * @throws InterruptedException if interrupted while waiting
	 */
	public String waitForCommandResponse(long timeoutMillis) throws InterruptedException {
		return waitForCommandResponse(timeoutMillis, null);
	}

	/**
	 * Closes the connection to the server.
	 */
	public void close() {
		running = false;
		if (receiveThread != null) {
			receiveThread.interrupt();
		}
		if (socket != null) {
			try {
				socket.close();
			} catch (IOException ignored) {
			}
		}
	}

	/**
	 * Checks if the connection is currently active.
	 * 
	 * @return true if connected, false otherwise
	 */
	public boolean isConnected() {
		return running && socket != null && !socket.isClosed();
	}
}
