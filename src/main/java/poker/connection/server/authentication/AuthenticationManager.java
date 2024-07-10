package poker.connection.server.authentication;

import poker.Server;
import poker.connection.protocol.channels.ServerChannel;
import poker.connection.utils.VirtualThread;

import javax.net.ssl.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.security.KeyStore;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

public class AuthenticationManager extends VirtualThread {
    private final Server server;
    private final int port;
    private final Set<Authenticator> authenticators = new HashSet<>();

    public AuthenticationManager(Server server, int port) {
        this.server = server;
        this.port = port;
    }

    @Override
    protected void run() {
        handleAuthentication();
    }

    private void handleAuthentication() {
        SSLServerSocketFactory serverSocketFactory = getServerSocketFactory();
        try (SSLServerSocket serverSocket = (SSLServerSocket) serverSocketFactory.createServerSocket(port)) {
            System.out.println("Server is listening on port " + port);

            SSLSocket socket;

            while (!this.isInterrupted() && (socket = (SSLSocket) serverSocket.accept()) != null) {
                Authenticator authenticator = new Authenticator(server, new ServerChannel(socket));
                server.log("New connection from " + socket.getInetAddress().getHostAddress());
                authenticators.add(authenticator);
                authenticator.start();
            }

            if (this.isInterrupted()) {
                server.log("Authentication manager interrupted");
                server.log("Closing all connections");
                for (Authenticator authenticator : authenticators) {
                    authenticator.interrupt();
                }
            }
        } catch (SocketException e) {
            System.out.println("Socket application.connection closed");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private SSLServerSocketFactory getServerSocketFactory() {
        SSLServerSocketFactory serverSocketFactory;
        try {
            InputStream keyStoreInputStream = getClass().getClassLoader().getResourceAsStream("server_keystore.p12");
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(keyStoreInputStream, "server_keystore".toCharArray());

            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, "server_keystore".toCharArray());

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(keyManagerFactory.getKeyManagers(), null, null);

            serverSocketFactory = sslContext.getServerSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return serverSocketFactory;
    }
}
