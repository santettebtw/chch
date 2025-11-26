package ch.heigvd.dai.commands;

import java.util.concurrent.Callable;
import ch.heigvd.dai.server.Server;
import picocli.CommandLine;

@CommandLine.Command(name = "server", description = "Start the server part of CHCH")
public class ServerCommand implements Callable<Integer> {

  @CommandLine.Option(
      names = {"-p", "--port"},
      description = "Port to use (default: ${DEFAULT-VALUE}).",
      defaultValue = "4269")
  protected int port;

  @Override
  public Integer call() {
      Server server = new Server(port);
      server.createServer();
      return 0;
  }
}
