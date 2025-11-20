package ch.heigvd.dai.client;

import java.io.IOException;
import java.util.List;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.InfoCmp;

class ClientUI {
	private Terminal term;
	private LineReader reader;

	static final int LEFT_PANEL_WIDTH = 16;
	
	ClientUI() throws IOException {
		this.term = TerminalBuilder.builder()
		.jna(true)
		.system(true)
		.build();

		this.reader = LineReaderBuilder.builder()
		.terminal(this.term)
		.build();
	}

	public String readInput() {
		return reader.readLine(" > ");
	}

	public void render(List<String> channels, List<String> messages) {
		int width = term.getWidth();
		int height = term.getHeight();

		term.puts(InfoCmp.Capability.clear_screen);

		// LEFT PANEL
		// TODO: dynamic width ?
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
