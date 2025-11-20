package ch.heigvd.dai.client;

class ClientConnection {
	private String host;
	private int port;

	ClientConnection(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public void connect() {
	}

	public void send(String msg) {
		// TODO
	}

	public String receive() {
		// TODO
		return null;
	}
}
