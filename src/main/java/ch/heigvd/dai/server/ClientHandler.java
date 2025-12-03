package ch.heigvd.dai.server;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

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

            String message;

            while ((message = in.readLine()) != null) {
                String[] commande = message.split(" ", 2);
				System.out.println("[Server] Received message: " + message);
				System.out.println("[Server] Parsed command array length: " + commande.length);
				if (commande.length > 0) {
					System.out.println("[Server] Command[0]: '" + commande[0] + "'");
				}
				if (commande.length > 1) {
					System.out.println("[Server] Command[1]: '" + commande[1] + "'");
				}

                //Switch des commandes possible
                try {
                    if (commande.length == 0 || commande[0] == null || commande[0].isEmpty()) {
                        System.out.println("[Server] Empty or invalid command");
                        send("ERROR 0");
                        continue;
                    }
                    switch (commande[0].toUpperCase()) {
                    case "JOIN":
                        if (commande.length < 2 || commande[1] == null) {
                            send("ERROR 0");
                            break;
                        }
                        String[] params = commande[1].split(" ", 3);
                        if (params.length < 2) {
                            send("ERROR 0");
                            break;
                        }

                        //check si le channel existe
                        System.out.println("[Server] Checking if channel exists: " + params[0]);
                        List<String> channels = Server.getListChannels();
                        if (channels == null) {
                            System.out.println("[Server] ERROR: getListChannels() returned null!");
                            send("ERROR 0");
                            break;
                        }
                        System.out.println("[Server] Available channels: " + channels);
                        if (!channels.contains(params[0])) {
                            System.out.println("[Server] Channel does not exist, sending ERROR 1");
                            send("ERROR 1");
                            break;
                        }

                        //check si le username est disponible
                        System.out.println("[Server] Checking if username is available: " + params[1] + " in channel " + params[0]);
                        if (Server.getUsernames(params[0]).contains(params[1])) {
                            System.out.println("[Server] Username already taken, sending ERROR 2");
                            send("ERROR 2");
                            break;
                        }

                        Server.remove(this); //le retire de l'ancien channel
                        channel = params[0]; //channel actuel
                        username = params[1]; //username pour ce channel
                        Server.add(this); //ajout à la liste des users du channel

                        System.out.println("[Server] Sending OK for JOIN: " + channel + " " + username);
                        send("OK");
                        System.out.println("[Server] OK sent, now broadcasting JOINED");
                        Server.broadcast(channel, "JOINED " + username, this);
                        System.out.println("[Server] Client change channel: " + channel);
                        break;
                    case "NICK":
                        if (Server.getUsernames(channel).contains(commande[1])) {
                            send("ERROR 1");
                            continue;
                        }

                        username = commande[1];
                        send("OK");
                        System.out.println("[Server] Client change username: " + username);
                        break;
                    case "MESSAGE":
                        System.out.println("[Server] Message from " + username + ": " + commande[1]);
                        Server.broadcast(channel,"RECEIVE " + username + " " + commande[1], this);
                        break;
                    case "CHANLIST":
                        StringBuilder chanList = new StringBuilder("CHANLIST");
                        for (String channel : Server.getListChannels()) {
                            chanList.append(" ").append(channel);
                        }
                        send(chanList.toString());
                        break;
                    case "USRLIST":
                        StringBuilder usrList = new StringBuilder("USRLIST");
                        for (String user : Server.getUsernames(channel)) {
                            usrList.append(" ").append(user);
                        }
                        send(usrList.toString());
                        break;
                    case "HISTORY":
                        for (String historyMessage : Server.getHistoryMessage(channel)){
                            send("RECEIVE " + historyMessage);
                        }
                        break;
                    case "QUIT":
                        System.out.println("[Server] Client " + username + " disconnected");
                        Server.broadcast(channel,"QUIT "+ username , this);
                        Server.remove(this);
                        break;
                    default:
                        System.out.println("[Server] Unknown command: " + commande[0]);
                        break;
                }
                } catch (Exception e) {
                    System.out.println("[Server] ERROR processing command: " + e.getClass().getName() + ": " + e.getMessage());
                    System.out.println("[Server] Command was: " + (commande.length > 0 ? commande[0] : "unknown"));
                    e.printStackTrace();
                    try {
                        send("ERROR 0");
                    } catch (Exception sendEx) {
                        System.out.println("[Server] Failed to send ERROR 0: " + sendEx);
                    }
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
            System.out.println("[Server] Sending: " + message);
            out.write(message + "\n");
            out.flush();
            System.out.println("[Server] Sent and flushed: " + message);
        } catch (IOException e) {
            System.out.println("[Server] Send failed: " + e);
            e.printStackTrace();
        }
    }

    public String getUsername() {return username;}
    public String getChannel() {return channel;}
}
