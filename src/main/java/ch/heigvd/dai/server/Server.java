package ch.heigvd.dai.server;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static int PORT = 4269;
    private static final Set<ClientHandler> clients = ConcurrentHashMap.newKeySet();

    public Server(int port){
        PORT = port;
    }


    public void createServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT);
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor(); ) {
            System.out.println("[Server] listening on port " + PORT);

            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                executor.submit(clientHandler);
            }
        } catch (IOException e) {
            System.out.println("[Server] exception: " + e);
        }
    }

    public static void broadcast(String message, ClientHandler sender) {
        for (ClientHandler client : clients) {
            if (client != sender) {
                client.send(message);
            }
        }
    }

    public static void remove(ClientHandler client) {
        clients.remove(client);
    }
}
