package ch.heigvd.dai.client;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Main client class for the CHCH chat protocol.
 * Handles user input, command processing, and server communication.
 */
public class Client {
	private final ClientUI ui;
	private final List<String> messages;
	private final List<String> channels;
	private final ClientConnection connection;
	private boolean running;
	private String currentChannel;
	private String currentUsername;
	private static final long RESPONSE_TIMEOUT_MS = 5000;

	/**
	 * Creates a new client instance.
	 * 
	 * @param host the server hostname or IP address
	 * @param port the server port number
	 * @throws RuntimeException if UI initialization fails
	 */
	public Client(String host, int port) {
		try {
			ui = new ClientUI();
		} catch (IOException e) {
			throw new RuntimeException("failed to initialize UI: " + e.getMessage(), e);
		}
		messages = new CopyOnWriteArrayList<>();
		channels = new CopyOnWriteArrayList<>();
		connection = new ClientConnection(host, port);
	}

	/**
	 * Processes user input and routes it to the appropriate handler.
	 * Supports slash commands (/join, /nick, /usrlist, /chanlist, /exit) and regular messages.
	 * 
	 * @param input the user input string to process
	 */
	private void handleInput(String input) {
		if (input.startsWith("/")) {
			String splitmsg[] = input.split(" ", 2);
			String command = splitmsg[0].substring(1);
			
			switch(command) {
				case "exit": {
					running = false;
					break;
				}
				case "join": {
					if (splitmsg.length < 2) {
						messages.add("/join: incorrect format. Please use /join <channel> <username>");
						return;
					}
					String[] parts = splitmsg[1].split(" ", 2);
					if (parts.length != 2) {
						messages.add("/join: incorrect format. Please use /join <channel> <username>");
						return;
					}
					handleJoin(parts[0], parts[1]);
					break;
				}
				case "nick": {
					if (splitmsg.length < 2) {
						messages.add("/nick: incorrect format. Please use /nick <new_username>");
						return;
					}
					handleNick(splitmsg[1]);
					break;
				}
				case "usrlist": {
					handleUsrList();
					break;
				}
				case "chanlist": {
					handleChanList();
					break;
				}
				default: {
					messages.add("slash command \"" + command + "\" not found.");
					break;
				}
			}
		} else {
			if (currentChannel == null) {
				messages.add("Error: you must join a channel first using /join <channel> <username>");
				return;
			}
			if (!connection.send("MESSAGE " + input)) {
				messages.add("Error: failed to send message. Connection may be lost.");
				return;
			}
			messages.add("(you) " + input);
		}
	}

	/**
	 * Handles the JOIN command to join a channel with a username.
	 * Sends the JOIN request to the server and processes the response.
	 * Updates the current channel and username on success.
	 * 
	 * @param channel the channel name to join
	 * @param username the username to use in the channel
	 */
	private void handleJoin(String channel, String username) {
		if (!connection.send("JOIN " + channel + " " + username)) {
			messages.add("Error: failed to send JOIN command. Connection may be lost.");
			return;
		}

		try {
			String response = connection.waitForCommandResponse(RESPONSE_TIMEOUT_MS, this::handleServerMessage);
			
			if (response == null) {
				messages.add("Error: timeout waiting for server response");
				return;
			}
			
			if (response.equals("OK")) {
				// clear previous messages when switching channels
				messages.clear();
				currentChannel = channel;
				currentUsername = username;
				messages.add("joined channel: " + channel + " as " + username);
				
				// refresh channel list to include the newly joined channel
				requestChannelListSilent();
				
				// update UI to show cleared messages and updated channel list
				ui.render(channels, messages);
				
				// request message history for the new channel
				// history messages will arrive as RECEIVE broadcasts and update the UI automatically
				requestHistory();
			} else if (response.startsWith("ERROR")) {
				String[] parts = response.split(" ");
				if (parts.length >= 2) {
					try {
						int errorCode = Integer.parseInt(parts[1]);
						switch (errorCode) {
							case 1:
								messages.add("Error: channel does not exist");
								break;
							case 2:
								messages.add("Error: username already taken in this channel");
								break;
							default:
								messages.add("Error: " + response);
						}
					} catch (NumberFormatException e) {
						messages.add("Error: invalid error code in response");
					}
				} else {
					messages.add("Error: " + response);
				}
			} else {
				messages.add("unexpected response: " + response);
			}
		} catch (InterruptedException e) {
			messages.add("Error: interrupted while waiting for response");
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Handles the NICK command to change the current username.
	 * Requires the client to be joined to a channel first.
	 * 
	 * @param newUsername the new username to use
	 */
	private void handleNick(String newUsername) {
		if (currentChannel == null) {
			messages.add("Error: you must join a channel first using /join <channel> <username>");
			return;
		}
		
		if (!connection.send("NICK " + newUsername)) {
			messages.add("Error: failed to send NICK command. Connection may be lost.");
			return;
		}

		try {
			String response = connection.waitForCommandResponse(RESPONSE_TIMEOUT_MS, this::handleServerMessage);
			
			if (response == null) {
				messages.add("Error: timeout waiting for server response");
				return;
			}
			
			if (response.equals("OK")) {
				messages.add("username changed from " + currentUsername + " to " + newUsername);
				currentUsername = newUsername;
			} else if (response.startsWith("ERROR")) {
				String[] parts = response.split(" ");
				if (parts.length >= 2) {
					try {
						int errorCode = Integer.parseInt(parts[1]);
						switch (errorCode) {
							case 1:
								messages.add("Error: username already taken in this channel");
								break;
							default:
								messages.add("Error: " + response);
						}
					} catch (NumberFormatException e) {
						messages.add("Error: invalid error code in response");
					}
				} else {
					messages.add("Error: " + response);
				}
			} else {
				messages.add("unexpected response: " + response);
			}
		} catch (InterruptedException e) {
			messages.add("Error: interrupted while waiting for response");
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Handles the USRLIST command to retrieve the list of users in the current channel.
	 * Requires the client to be joined to a channel first.
	 */
	private void handleUsrList() {
		if (currentChannel == null) {
			messages.add("Error: you must join a channel first using /join <channel> <username>");
			return;
		}
		
		if (!connection.send("USRLIST")) {
			messages.add("Error: failed to send USRLIST command. Connection may be lost.");
			return;
		}

		try {
			String response = connection.waitForCommandResponse(RESPONSE_TIMEOUT_MS, this::handleServerMessage);
			
			if (response == null) {
				messages.add("Error: timeout waiting for server response");
				return;
			}
			
			if (response.startsWith("USRLIST")) {
				String[] parts = response.split(" ");
				if (parts.length > 1) {
					StringBuilder userList = new StringBuilder("Users in channel: ");
					for (int i = 1; i < parts.length; i++) {
						if (i > 1) userList.append(", ");
						userList.append(parts[i]);
					}
					messages.add(userList.toString());
				} else {
					messages.add("no users in channel");
				}
			} else {
				messages.add("unexpected response: " + response);
			}
		} catch (InterruptedException e) {
			messages.add("Error: interrupted while waiting for response");
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Requests the message history for the current channel from the server.
	 * The server will respond with multiple RECEIVE messages, one for each messages in history.
	 * These messages will be automatically processed by the background message handler.
	 * Requires the client to be joined to a channel first.
	 */
	private void requestHistory() {
		if (currentChannel == null) {
			// Should not happen, but check anyway
			return;
		}

		if (!connection.send("HISTORY")) {
			messages.add("Error: failed to send HISTORY command. Connection may be lost.");
			return;
		}

		// history messages will arrive as RECEIVE broadcasts and be handled automatically
		// by the background thread handleServerMessage
	}

	/**
	 * Requests the channel list from the server and updates the local channel list.
	 * This is a silent operation that does not add messages to the message box.
	 * Used during initialization to populate the channel list.
	 */
	private void requestChannelListSilent() {
		if (!connection.send("CHANLIST")) {
			// Silent failure - don't add to messages
			return;
		}

		try {
			String response = connection.waitForCommandResponse(RESPONSE_TIMEOUT_MS, this::handleServerMessage);
			
			if (response != null && response.startsWith("CHANLIST")) {
				String[] parts = response.split(" ");
				channels.clear();
				if (parts.length > 1) {
					for (int i = 1; i < parts.length; i++) {
						channels.add(parts[i]);
					}
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Handles the CHANLIST command to retrieve the list of all available channels.
	 * Updates the local channel list with the server's response and displays it in the message box.
	 */
	private void handleChanList() {
		if (!connection.send("CHANLIST")) {
			messages.add("Error: failed to send CHANLIST command. Connection may be lost.");
			return;
		}

		try {
			String response = connection.waitForCommandResponse(RESPONSE_TIMEOUT_MS, this::handleServerMessage);
			
			if (response == null) {
				messages.add("Error: timeout waiting for server response");
				return;
			}
			
			if (response.startsWith("CHANLIST")) {
				String[] parts = response.split(" ");
				channels.clear();
				if (parts.length > 1) {
					messages.add("Available channels:");
					for (int i = 1; i < parts.length; i++) {
						channels.add(parts[i]);
						messages.add("  - " + parts[i]);
					}
				} else {
					messages.add("No channels available");
				}
				// Update the channel panel in the UI
				ui.updateChannelPanel(channels);
			} else {
				messages.add("unexpected response: " + response);
			}
		} catch (InterruptedException e) {
			messages.add("Error: interrupted while waiting for response");
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Initializes and starts the client.
	 * Connects to the server, sets up the UI, and starts the message receiving thread.
	 * Enters the main input loop to process user commands and messages.
	 */
	public void init() {
		running = true;
		
		if (!connection.connect()) {
			messages.add("Error: failed to connect to server. Please check the host and port.");
			cleanup();
			return;
		}

		// Request channel list silently during initialization
		requestChannelListSilent();

		// Render UI with the fetched channel list
		ui.render(channels, messages);

		Thread.startVirtualThread(() -> {
			while (running) {
				try {
					String msg = connection.waitForResponse(100);
					if (msg != null) {
						// only process broadcasts, not command responses
						// command responses are handled by waitForCommandResponse
						if (isBroadcast(msg)) {
							handleServerMessage(msg);
							ui.updateMessageArea(messages);
						} else {
							// this is a command response, put it back in the queue
							// so waitForCommandResponse can handle it
							connection.putBack(msg);
						}
					}
				} catch (InterruptedException e) {
					break;
				}
			}
		});

		Scanner scanner = new Scanner(System.in);
		String currentInput = "";
		int cursorPos = 0;

		try {
			while (running) {
				ui.setCurrentInput(currentInput, cursorPos);
				System.out.print("\033[" + ui.getTerminal().getHeight() + ";3H");

				String input = scanner.nextLine();

				handleInput(input);

				ui.render(channels, messages);
				currentInput = "";
				cursorPos = 0;
			}
		} finally {
			scanner.close();
			cleanup();
		}
	}

	/**
	 * Checks if a message is a broadcast message (as opposed to a command response).
	 * Broadcasts are RECEIVE and JOINED messages sent by the server.
	 * Command responses are OK, ERROR, USRLIST, and CHANLIST.
	 * 
	 * @param msg the message to check
	 * @return true if the message is a broadcast, false otherwise
	 */
	private boolean isBroadcast(String msg) {
		return msg.startsWith("RECEIVE") || msg.startsWith("JOINED");
	}

	/**
	 * Handles incoming server broadcast messages.
	 * Processes RECEIVE (chat messages) and JOINED (user join notifications) messages.
	 * 
	 * @param msg the broadcast message from the server
	 */
	private void handleServerMessage(String msg) {
		if (msg.startsWith("RECEIVE")) {
			// Format: RECEIVE <username> <message>
			String[] parts = msg.split(" ", 3);
			if (parts.length >= 3) {
				messages.add(parts[1] + ": " + parts[2]);
			} else {
				messages.add("malformed RECEIVE message: " + msg);
			}
		} else if (msg.startsWith("JOINED")) {
			// Format: JOINED <username>
			String[] parts = msg.split(" ", 2);
			if (parts.length >= 2) {
				messages.add(parts[1] + " joined the channel");
			} else {
				messages.add("malformed JOINED message: " + msg);
			}
		}
	}

	/**
	 * Cleans up resources and closes the connection to the server.
	 * Called when the client is shutting down.
	 */
	private void cleanup() {
		connection.close();
	}
}
