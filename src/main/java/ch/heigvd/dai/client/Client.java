package ch.heigvd.dai.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Client {
	private ClientUI ui;
	private List<String> messages;
	private List<String> channels;
	private ClientConnection connection;

	// TODO: handle ioexception here?
	public Client(String host, int port) throws IOException {
		this.ui = new ClientUI();
		this.messages = new ArrayList<String>();
		this.channels = List.of("channel1", "channel2");
		this.connection = new ClientConnection(host, port);
	}

	/**
	 * @brief runs the client, initializing the connection with the server and
	 * starting the REPL.
	 */
	public void init() {	
		try {
			connection.connect();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		while (true) {
			// TODO: send and recieve messages
			String msg;
			while((msg = connection.receive()) != null) {
				messages.add(msg);
			}
			ui.render(channels, messages);
			// TODO: do we really want this in here?
			String input = ui.readInput();
			if (input.equals("/exit")) break;
			try {
				connection.send(input);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			messages.add(input);
			if (messages.size() > 200) messages.remove(0);
		}
	}
}

