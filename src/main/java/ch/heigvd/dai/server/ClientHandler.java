package ch.heigvd.dai.server;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {
    private BufferedWriter out;
    private final Socket socket;
    private String username;
    private String channel;
    private enum commandes{MESSAGE,JOIN,CHANGE}

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.channel = "global";
    }

    /**
     * Methode pour le thread client qui va exécuter les bonnes commandes en fonction des entrées du client
     */
    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            String clientAddress = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
            System.out.println("[Server] Client connected: " + clientAddress);

            send("Bienvenue sur le chat !");

            String message;

            //TODO regarder doc pour format message
            //TODO OK/ERROR
            while ((message = in.readLine()) != null) {
                String[] commande = message.split(" ", 2);

                //Switch des commandes possible
                switch (commande[0].toUpperCase()) {
                    case "JOIN":
                        //TODO brodcast join
                        String[] params = commande[1].split(" ", 3);

                        //check si le channel existe
                        if (!Server.getListChannels().contains(params[0])) {
                            send("Le channel n'existe pas !");
                            continue;
                        }

                        //check si le username est disponible
                        if (Server.getUsernames(params[0]).contains(params[1])) {
                            send("Le pseudo n'est pas disponible !");
                            continue;
                        }

                        Server.remove(this); //le retire de l'ancien channel
                        channel = params[0]; //channel actuel
                        username = params[1]; //username pour ce channel
                        Server.add(this); //ajout à la liste des users du channel

                        send("Join the channel with success");
                        System.out.println("[Server] Client change channel: " + channel);
                        break;
                    case "CHANGE":
                        username = commande[1];
                        send("Change the username to " + username);
                        System.out.println("[Server] Client change username: " + username);
                        break;
                    case "MESSAGE":
                        System.out.println("[Server] Message from " + username + ": " + commande[1]);
                        Server.broadcast(channel,username + " " + commande[1], this);
                        break;
                    case "LIST":
                        for (String channel : Server.getListChannels()) {
                            send("- " + channel);
                        }
                        break;
                    case "QUIT":
                        System.out.println("[Server] Client " + username + " disconnected");
                        send("Disconnect the chat");
                        Server.broadcast(channel,"User "+ username + " quit the channel !", this);
                        Server.remove(this);
                        break;
                }
            }
        }catch (IOException e) {
            System.out.println("[Server] Client disconnected");
        } finally {
            Server.remove(this);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    /**
     * Envoie un message au client
     * @param message
     */
    public void send(String message) {
        try {
            out.write(message + "\n");
            out.flush();
        } catch (IOException e) {
            System.out.println("[Server] Send failed: " + e);
        }
    }

    public String getUsername() {return username;}
    public String getChannel() {return channel;}
}
