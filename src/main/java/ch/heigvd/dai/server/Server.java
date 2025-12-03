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
                    }
                }
            }

            //TODO ecrire/lecture fichier
            //TODO sauvegarder memoire discussion (map)
            //TODO envoye reicive history client
            //TODO issue

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
        for (ClientHandler client : clients.get(channel).values()) {
            if (client != sender) {
                client.send(message);
            }
        }
    }

    /**
     * On ajoute à la map le client en fonction de son channel
     * @param client
     */
    public static void add(ClientHandler client) {
        clients.get(client.getChannel()).put(client.getUsername(), client);
    }

    /**
     * On le retire de la liste des clients de son channel
     * @param client
     */
    public static void remove(ClientHandler client) {
        clients.get(client.getChannel()).remove(client.getUsername());
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
        return clients.get(channel).keySet();
    }
}
