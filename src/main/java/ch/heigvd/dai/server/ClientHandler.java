package ch.heigvd.dai.server;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientHandler implements Runnable {
    private BufferedWriter out;
    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));

            String clientAddress = socket.getInetAddress().getHostAddress() + ":" + socket.getPort();
            System.out.println("[Server] Client connected: " + clientAddress);

            send("Bienvenue sur le chat !");

            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("[Server] Message from " + clientAddress + ": " + message);
                Server.broadcast(clientAddress + " dit : " + message, this);
            }
        }catch (IOException e) {
            System.out.println("[Server] Client disconnected");
        } finally {
            Server.remove(this);
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    public void send(String message) {
        try {
            out.write(message + "\n");
            out.flush();
        } catch (IOException e) {
            System.out.println("[Server] Send failed: " + e);
        }
    }
}
