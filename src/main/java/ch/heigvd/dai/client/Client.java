package ch.heigvd.dai.client;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

public class Client {
	private final ClientUI ui;
	private final List<String> messages;
	private final List<String> channels;
	private final ClientConnection connection;
	private boolean running;

	public Client(String host, int port) throws IOException {
		ui = new ClientUI();
		messages = new CopyOnWriteArrayList<>();
		channels = List.of("channel1", "channel2");
		connection = new ClientConnection(host, port);
	}

	private void handleInput(String input) throws IOException { 
		if (input.startsWith("/")) {
			String splitmsg[] = input.split(" ");
			switch(splitmsg[0].substring(1)) {
				case "exit": {
					running = false;
					break;
				}
				case "join": {
					if (splitmsg.length != 3) {
						messages.add(splitmsg[0] + ": incorrect format. Please use /join <channel> <username>");
						return;
					}
					StringBuilder sb = new StringBuilder("JOIN ");
					sb.append(splitmsg[1] + " ").append(splitmsg[2]);
					connection.send(sb.toString());
					break;
				}
				case "nick": {
					// TODO(sss)
					break;
				}
				default: {
					messages.add("slash command \"" + input + "\" not found.");
					break;
				}
			}
		} else {
			connection.send("MESSAGE " + input);
			messages.add("(you) " + input);
		}
	}

	public void init() {
		running = true;
		try {
			connection.connect();

			ui.render(channels, messages);

			Thread.startVirtualThread(() -> {
				while (true) {
					String msg = connection.receive();
					if (msg != null) {
						messages.add(msg);
						ui.updateMessageArea(messages);
					} else {
						try {
							Thread.sleep(50);
						} catch (InterruptedException e) {
							break;
						}
					}
				}
			});

			Scanner scanner = new Scanner(System.in);
			String currentInput = "";
			int cursorPos = 0;

			while (running) {
				ui.setCurrentInput(currentInput, cursorPos);
				System.out.print("\033[" + ui.getTerminal().getHeight() + ";3H");

				String input = scanner.nextLine();

				handleInput(input);

				ui.render(channels, messages);
				currentInput = "";
				cursorPos = 0;
			}

		} catch (IOException e) {
			System.err.println("Connection error: " + e.getMessage());
			e.printStackTrace();
		} finally {
			cleanup();
		}
	}

	private void cleanup() {
		try {
			connection.close();
		} catch (IOException e) {
			System.err.println("Error closing connection: " + e.getMessage());
		}
	}

	public static void main(String[] args) throws IOException {
		Client client = new Client("localhost", 12345);
		client.init();
	}
}
