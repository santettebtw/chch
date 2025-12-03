package ch.heigvd.dai.client;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.List;

/**
 * Handles the terminal-based user interface for the CHCH client.
 * Manages rendering of channels, messages, and user input.
 */
public class ClientUI {
	private final Terminal terminal;
	private static final int LEFT_PANEL_WIDTH = 16;
	private int rows;
	private int columns;
	private StringBuilder currentInput = new StringBuilder();
	private int cursorOffset = 0;

	/**
	 * Initializes the terminal UI.
	 * 
	 * @throws IOException if terminal initialization fails
	 */
	public ClientUI() throws IOException {
		terminal = TerminalBuilder.builder().system(true).jna(true).build();
		this.rows = terminal.getHeight();
		this.columns = terminal.getWidth();
	}

	/**
	 * Gets the underlying terminal instance.
	 * 
	 * @return the terminal object
	 */
	public Terminal getTerminal() {
		return terminal;
	}

	/**
	 * Renders the full UI screen with channels list and messages.
	 * Clears the screen and redraws everything from scratch.
	 * 
	 * @param channels the list of available channels to display in the left panel
	 * @param messages the list of messages to display in the main area
	 */
	public void render(List<String> channels, List<String> messages) {
		terminal.puts(org.jline.utils.InfoCmp.Capability.clear_screen);

		// draw channel list
		for (int i = 0; i < rows - 1; i++) {
			terminal.writer().print("\033[" + (i + 1) + ";1H"); // move cursor
			if (i < channels.size()) {
				terminal.writer().print(padRight(channels.get(i), LEFT_PANEL_WIDTH));
			} else {
				terminal.writer().print(" ".repeat(LEFT_PANEL_WIDTH));
			}

			// draw vertical border
			terminal.writer().print("|");

			// draw message area
			int msgIndex = i;
			if (msgIndex < messages.size()) {
				String msg = truncate(messages.get(msgIndex), columns - LEFT_PANEL_WIDTH - 1);
				terminal.writer().print(msg);
			}
		}

		// draw input prompt
		terminal.writer().print("\033[" + rows + ";2H>");
		terminal.writer().print(currentInput.toString());
		terminal.writer().print("\033[" + rows + ";" + (3 + cursorOffset) + "H");
		terminal.flush();
	}

	/**
	 * Updates only the channel list panel without redrawing the entire screen.
	 * 
	 * @param channels the list of channels to display
	 */
	public void updateChannelPanel(List<String> channels) {
		for (int i = 0; i < rows - 1; i++) {
			terminal.writer().print("\033[" + (i + 1) + ";1H"); // move cursor
			if (i < channels.size()) {
				terminal.writer().print(padRight(channels.get(i), LEFT_PANEL_WIDTH));
			} else {
				terminal.writer().print(" ".repeat(LEFT_PANEL_WIDTH));
			}
		}
		terminal.flush();
	}

	/**
	 * Updates only the message area without redrawing the entire screen.
	 * More efficient than full render when only messages have changed.
	 * 
	 * @param messages the list of messages to display
	 */
	public void updateMessageArea(List<String> messages) {
		int msgAreaHeight = rows - 1;
		int startMsg = Math.max(0, messages.size() - msgAreaHeight);

		for (int y = 0; y < msgAreaHeight; y++) {
			terminal.writer().print("\033[" + (y + 1) + ";" + (LEFT_PANEL_WIDTH + 2) + "H");
			int msgIndex = startMsg + y;
			String line = (msgIndex < messages.size()) ? truncate(messages.get(msgIndex), columns - LEFT_PANEL_WIDTH - 1) : "";
			terminal.writer().print(padRight(line, columns - LEFT_PANEL_WIDTH - 1));
		}

		// restore cursor to input line
		terminal.writer().print("\033[" + rows + ";3H");
		terminal.writer().print(currentInput.toString());
		terminal.writer().print("\033[" + rows + ";" + (3 + cursorOffset) + "H");
		terminal.flush();
	}

	/**
	 * Sets the current input text (without cursor position).
	 * 
	 * @param input the input text to set
	 */
	public void setCurrentInput(String input) {
		currentInput = new StringBuilder(input);
	}

	/**
	 * Gets the current input text.
	 * 
	 * @return the current input string
	 */
	public String getCurrentInput() {
		return currentInput.toString();
	}

	/**
	 * Pads a string to the right to a specified width.
	 * If the string is longer than the width, it is truncated.
	 * 
	 * @param s the string to pad
	 * @param width the target width
	 * @return the padded or truncated string
	 */
	private String padRight(String s, int width) {
		if (s.length() >= width) return s.substring(0, width);
		return s + " ".repeat(width - s.length());
	}

	/**
	 * Truncates a string to a maximum width if it exceeds the limit.
	 * 
	 * @param s the string to truncate
	 * @param width the maximum width
	 * @return the truncated string, or the original string if it fits
	 */
	private String truncate(String s, int width) {
		if (s.length() <= width) return s;
		return s.substring(0, width);
	}

	/**
	 * Sets the current input text and cursor position.
	 * 
	 * @param input the input text to set
	 * @param cursorPos the cursor position offset
	 */
	public void setCurrentInput(String input, int cursorPos) {
		currentInput = new StringBuilder(input);
		cursorOffset = cursorPos;
	}

	/**
	 * Gets the current cursor offset position.
	 * 
	 * @return the cursor offset
	 */
	public int getCursorOffset() {
		return cursorOffset;
	}
}
