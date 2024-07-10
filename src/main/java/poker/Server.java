package poker;

import poker.connection.protocol.Connection;
import poker.connection.protocol.exceptions.ClosedConnectionException;
import poker.connection.server.authentication.AuthenticationManager;
import poker.connection.server.database.DatabaseInterface;
import poker.connection.server.queue.Queuer;
import poker.connection.server.queue.RankedQueuer;
import poker.connection.server.queue.SimpleQueuer;

import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

public class Server {
    private final AuthenticationManager authenticationManager;
    private final Queuer queuer;
    private final boolean loggingEnabled;
    private final boolean rankedMode;
    private final DatabaseInterface database = new DatabaseInterface();
    private final Set<Connection> connections = new HashSet<>();
    private final ReentrantLock connectionLock = new ReentrantLock();

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Arguments: <port> [-l] [-r]");
            return;
        }

        int port = Integer.parseInt(args[0]);
        boolean loggingEnabled = false;
        boolean rankedMode = false;
        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "-l":
                    loggingEnabled = true;
                    System.out.println("Logging enabled");
                    break;
                case "-r":
                    rankedMode = true;
                    System.out.println("Ranked mode enabled");
                    break;
                default:
                    System.out.println("Usage: java TimeServer <port> [-l] [-r]");
                    return;
            }
        }

        Server server = new Server(port, loggingEnabled, rankedMode);
        server.init();
    }

    private Server(int port, boolean loggingEnabled, boolean rankedMode) {
        this.loggingEnabled = loggingEnabled;
        this.rankedMode = rankedMode;
        this.authenticationManager = new AuthenticationManager(this, port);
        if (rankedMode) {
            this.queuer = new RankedQueuer(this);
        } else {
            this.queuer = new SimpleQueuer(this);
        }
    }

    public Queuer getQueuer() {
        return queuer;
    }

    public DatabaseInterface getDatabase() {
        return database;
    }

    public boolean isLoggingEnabled() {
        return loggingEnabled;
    }

    public boolean isRankedMode() {
        return rankedMode;
    }

    private void init() {
        startServices();
        System.out.println("Press [ENTER] to stop the server\n");
        new Scanner(System.in).nextLine();
        interruptServices();
        disconnect();
    }

    private void startServices() {
        authenticationManager.start();
        queuer.start();
    }

    private void interruptServices() {
        authenticationManager.interrupt();
        queuer.interrupt();
    }

    public void queuePlayer(Connection connection) {
        connectionLock.lock();
        connections.add(connection);
        connectionLock.unlock();
        queuer.queuePlayer(connection);
    }

    private void disconnect() {

        log("Disconnecting all players\n");
        connectionLock.lock();
        for (Connection connection : connections) {
            log("Disconnecting " + connection.getUsername());
            try {
                connection.getChannel().requestConnectionEnd("Server is shutting down");
            } catch (ClosedConnectionException ignored) {
                log("Channel was already closed for " + connection.getUsername() + "\n");
            }
        }
        connectionLock.unlock();
        log("Server stopped");
    }

    public synchronized void log(String... message) {
        if (isLoggingEnabled()) {
            System.out.println(String.join("\n", message) + "\n");
        }
    }
}
