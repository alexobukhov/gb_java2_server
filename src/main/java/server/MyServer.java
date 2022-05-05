package server;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import server.authentication.AuthenticationService;
import server.authentication.BaseAuthenticationService;
import server.handler.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MyServer {

    private Logger logger;
    private final ServerSocket serverSocket;
    private final AuthenticationService authenticationService;
    private final List<ClientHandler> clients;

    public MyServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        authenticationService = new BaseAuthenticationService();
        clients = new ArrayList<>();
        PropertyConfigurator.configure("src/main/resources/log/config/log4j.properties");
        logger = Logger.getLogger("file");
    }


    public void start() {
        System.out.println("СЕРВЕР ЗАПУЩЕН!");
        logger.info("СЕРВЕР ЗАПУЩЕН!");
        System.out.println("----------------");

        try {
            while(true) {
                waitAndProcessNewClientConnection();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void waitAndProcessNewClientConnection() throws IOException {
        System.out.println("Ожидание клиента...");
        logger.trace("Ожидание клиента...");
        Socket socket = serverSocket.accept();
        System.out.println("Клиент подключился!");
        logger.info("Клиент подключился!");

        processClientConnection(socket);
    }

    private void processClientConnection(Socket socket) throws IOException {
        ClientHandler handler = new ClientHandler(this, socket);
        handler.handle();
    }

    public synchronized void subscribe(ClientHandler clientHandler) {
        clients.add(clientHandler);
    }

    public synchronized void unSubscribe(ClientHandler clientHandler) {
        clients.remove(clientHandler);
    }

    public synchronized boolean isUsernameBusy(String username) {
        for (ClientHandler client : clients) {
            if (client.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    public AuthenticationService getAuthenticationService() {
        return authenticationService;
    }

    public synchronized void sendPrivateMessage(String message, String recipient, ClientHandler sender) throws IOException {
        ClientHandler client = clients.stream().
                filter((c -> c.getUsername().equals(recipient))).
                findFirst().
                orElse(null);
        if (client != null) {
            client.sendMessage(sender.getUsername(), message);
            logger.info("Пользователь " + sender.getUsername() + " отправил сообщение пользователю "
                    + client.getUsername());
        }
    }

    public synchronized void broadcastMessage(String message, ClientHandler sender) throws IOException {
        for (ClientHandler client : clients) {
            if (client == sender) {
                continue;
            }
            client.sendMessage(sender.getUsername(), message);
            logger.info("Пользователь " + sender + " отправил сообщение пользователю " + client.getUsername());
        }
    }

    public synchronized void broadcastClients(ClientHandler clientHandler) throws IOException {
        for (ClientHandler client : clients) {
            client.sendServerMessage(String.format("%s присоединился к чату", clientHandler.getUsername()));
            logger.info("Пользователь " + clientHandler.getUsername() + " присоединился к чату");
            client.sendClientsList(clients);
        }
    }

    public synchronized void broadcastClientDisconnected(ClientHandler clientHandler) throws IOException {
        for (ClientHandler client : clients) {
            if (client == clientHandler) {
                continue;
            }

            client.sendServerMessage(String.format("%s отключился", clientHandler.getUsername()));
            logger.warn("Пользователь " + clientHandler.getUsername() + "отключился");
            client.sendClientsList(clients);
        }
    }
}
