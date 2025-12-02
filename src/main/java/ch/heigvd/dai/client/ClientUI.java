package ch.heigvd.dai.client;

import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

import java.io.IOException;
import java.util.List;

public class ClientUI {
	private final Terminal terminal;
	private static final int LEFT_PANEL_WIDTH = 16;
	private int rows;
	private int columns;
	private StringBuilder currentInput = new StringBuilder();
	private int cursorOffset = 0;

	public ClientUI() throws IOException {
		terminal = TerminalBuilder.builder().system(true).jna(true).build();
		this.rows = terminal.getHeight();
		this.columns = terminal.getWidth();
	}

	public Terminal getTerminal() {
		return terminal;
	}

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

	public void setCurrentInput(String input) {
		currentInput = new StringBuilder(input);
	}

	public String getCurrentInput() {
		return currentInput.toString();
	}

	private String padRight(String s, int width) {
		if (s.length() >= width) return s.substring(0, width);
		return s + " ".repeat(width - s.length());
	}

	private String truncate(String s, int width) {
		if (s.length() <= width) return s;
		return s.substring(0, width);
	}

	public void setCurrentInput(String input, int cursorPos) {
		currentInput = new StringBuilder(input);
		cursorOffset = cursorPos;
	}

	public int getCursorOffset() {
		return cursorOffset;
	}
}
