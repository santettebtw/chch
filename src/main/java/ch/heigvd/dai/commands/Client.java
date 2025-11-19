package ch.heigvd.dai.commands;

import java.io.IOException;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import ch.heigvd.dai.client.ClientRunner;

@CommandLine.Command(name = "client", description = "Start the client part of the network game.")
public class Client implements Callable<Integer> {

	@CommandLine.Option(
		names = {"-H", "--host"},
		description = "Host to connect to.",
		required = true)
	protected String host;

	@CommandLine.Option(
		names = {"-p", "--port"},
		description = "Port to use (default: ${DEFAULT-VALUE}).",
		defaultValue = "6433")
	protected int port;

	@Override
	public Integer call() {
		ClientRunner client;
		try {
			client = new ClientRunner(host, port);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return 1;
		}

		client.init();

		return 0;
	}
}
