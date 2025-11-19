package ch.heigvd.dai.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jline.utils.InfoCmp;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

public class ClientRunner {
	private String host;
	private int port;
	private TerminalDrawer termDrawer;
	private List<String> messages;
	private List<String> channels;
	private Terminal termSpec;

	// TODO: handle ioexception here?
	public ClientRunner(String host, int port) throws IOException {
		this.host = host;
		this.port = port;
		this.messages = new ArrayList<String>();
		this.channels = List.of("channel1", "channel2");
		// TODO: could be cool if we didn't have anything to do with termSpec in the client itself
		this.termSpec = TerminalBuilder.builder()
		.jna(true)
		.system(true)
		.build();
		this.termDrawer = new TerminalDrawer(termSpec);
	}

	/**
	 * @brief runs the client, initializing the connection with the server and
	 * starting the REPL.
	 */
	public void init() {	
		// TODO: could be cool if we didn't have anything to do with termSpec in the client itself
		LineReader reader = LineReaderBuilder.builder()
		.terminal(termSpec)
		.build();

		while (true) {
			// TODO: send and recieve messages
			termDrawer.drawScreen(channels, messages);
			// TODO: do we really want this in here?
			String input = reader.readLine(" > ");
			if (input.equals("/exit")) break;
			messages.add(input);
			if (messages.size() > 200) messages.remove(0);
		}
	}
}

// TODO document
// TODO change to its own thing?
class TerminalDrawer {
	static final int LEFT_PANEL_WIDTH = 16;
	private Terminal term;

	public TerminalDrawer(Terminal term) throws IOException {
		this.term = term;
	}

	public void drawScreen(List<String> channels,
		List<String> messages) {

		int width = term.getWidth();
		int height = term.getHeight();

		term.puts(InfoCmp.Capability.clear_screen);

		// LEFT PANEL
		for (int i = 0; i < channels.size(); i++) {
			term.puts(InfoCmp.Capability.cursor_address, i, 0);
			term.writer().print(padRight(channels.get(i), LEFT_PANEL_WIDTH));
		}

		// BORDER
		for (int y = 0; y < height; y++) {
			term.puts(InfoCmp.Capability.cursor_address, y, LEFT_PANEL_WIDTH);
			term.writer().print("|");
		}

		int rightX = LEFT_PANEL_WIDTH + 1;
		int usableHeight = height - 1;
		// +2 for padding
		int startMsg = Math.max(0, messages.size() - usableHeight + 2);

		// RIGHT PANEL (messages)
		for (int i = 0; i < usableHeight; i++) {
			int msgIndex = startMsg + i;
			term.puts(InfoCmp.Capability.cursor_address, i, rightX);

			if (msgIndex < messages.size()) {
				String msg = messages.get(msgIndex);
				int rightWidth = width - rightX - 1;
				term.writer().print(truncate(msg, rightWidth));
			} else {
				term.writer().print("");
			}
		}

		term.flush();
	}

	static String padRight(String s, int width) {
		if (s.length() >= width) return s.substring(0, width);
		return s + " ".repeat(width - s.length());
	}

	static String truncate(String s, int width) {
		if (s.length() <= width) return s;
		return s.substring(0, width);
	}
}
