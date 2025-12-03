package ch.heigvd.dai.server;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static int PORT = 4269;
    private static final Map<String, Map<String, ClientHandler>> clients = new ConcurrentHashMap<>();
    private static List<String> listChannels;
    private static final Map<String, List<String>> historyMessages = new ConcurrentHashMap<>();

    public Server(int port){
        PORT = port;
        listChannels = new ArrayList<>();
    }


    /**
     * On crée le serveur et on attend que les clients se connecte
     */
    public void createServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT);
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor(); ) {
            System.out.println("[Server] listening on port " + PORT);

            //on crée les channels en fonction des fichiers existant dans le dossier /data
            Path path = Paths.get("./data");
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                for (Path entry : stream) {
                    if (Files.isRegularFile(entry)) {
                        String name = entry.getFileName().toString();
                        //On supprime l'extension pour le nom des canaux
                        String withoutExt = name.contains(".") ? name.substring(0, name.lastIndexOf('.')) : name;
                        listChannels.add(withoutExt);
                        clients.put(withoutExt, new ConcurrentHashMap<>());
                        historyMessages.put(withoutExt, new ArrayList<>());
                    }
                }
            }

            //TODO ecrire/lecture fichier

            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                executor.submit(clientHandler);
            }
        } catch (IOException e) {
            System.out.println("[Server] exception: " + e);
        }
    }

    /**
     * On envoie un message à tous les clients du channel choisi sauf lui-même
     * @param channel
     * @param message
     * @param sender
     */
    public static void broadcast(String channel, String message, ClientHandler sender) {
        //On sauvegarde dans le server par channel et on split pour enlever RECEIVE
        // Only save RECEIVE messages (chat messages) to history, not JOINED or other broadcasts
        if (message.startsWith("RECEIVE ")) {
            List<String> history = historyMessages.get(channel);
            if (history != null) {
                history.add(message.split(" ", 2)[1]);
            }
        }

        Map<String, ClientHandler> channelClients = clients.get(channel);
        if (channelClients != null) {
            for (ClientHandler client : channelClients.values()) {
                if (client != sender) {
                    client.send(message);
                }
            }
        }
    }

    /**
     * On ajoute à la map le client en fonction de son channel
     * @param client
     */
    public static void add(ClientHandler client) {
        Map<String, ClientHandler> channelClients = clients.get(client.getChannel());
        if (channelClients == null) {
            System.err.println("[Server] ERROR: Channel '" + client.getChannel() + "' does not exist in clients map!");
            System.err.println("[Server] Available channels in map: " + clients.keySet());
            throw new IllegalStateException("Channel '" + client.getChannel() + "' does not exist in clients map");
        }
        channelClients.put(client.getUsername(), client);
    }

    /**
     * On le retire de la liste des clients de son channel
     * @param client
     */
    public static void remove(ClientHandler client) {
        if (client == null) {
            return;
        }
        String channel = client.getChannel();
        String username = client.getUsername();
        
        if (channel == null || username == null) {
            // Client not yet fully initialized or not in any channel
            return;
        }
        
        Map<String, ClientHandler> channelClients = clients.get(channel);
        if (channelClients != null) {
            channelClients.remove(username);
        }
    }

    /**
     * Retourne la liste de tous les channels
     * @return
     */
    public static List<String> getListChannels() {return listChannels;}

    /**
     * Retourne un set de tous les usernames du channel
     * @param channel
     * @return
     */
    public static Set<String> getUsernames(String channel) {
        Map<String, ClientHandler> channelClients = clients.get(channel);
        if (channelClients != null) {
            return channelClients.keySet();
        }
        return java.util.Collections.emptySet();
    }

    /**
     * Retourne la liste de tous les messages enregistré dans le server
     * @param channel
     * @return
     */
    public static List<String> getHistoryMessage(String channel) {
        return historyMessages.get(channel);
    }
}
