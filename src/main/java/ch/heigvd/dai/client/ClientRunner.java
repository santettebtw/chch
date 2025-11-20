package ch.heigvd.dai.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ClientRunner {
	private String host;
	private int port;
	private ClientUI ui;
	private List<String> messages;
	private List<String> channels;

	// TODO: handle ioexception here?
	public ClientRunner(String host, int port) throws IOException {
		this.host = host;
		this.port = port;
		this.messages = new ArrayList<String>();
		this.channels = List.of("channel1", "channel2");
	}

	/**
	 * @brief runs the client, initializing the connection with the server and
	 * starting the REPL.
	 */
	public void init() {	
		while (true) {
			// TODO: send and recieve messages
			ui.render(channels, messages);
			// TODO: do we really want this in here?
			String input = ui.readInput();
			if (input.equals("/exit")) break;
			messages.add(input);
			if (messages.size() > 200) messages.remove(0);
		}
	}
}

