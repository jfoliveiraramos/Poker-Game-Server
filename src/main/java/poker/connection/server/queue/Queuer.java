package poker.connection.server.queue;

import poker.Server;
import poker.connection.protocol.Connection;
import poker.connection.protocol.exceptions.ChannelException;
import poker.connection.protocol.exceptions.ClosedConnectionException;
import poker.connection.server.game.Game;
import poker.connection.utils.VirtualThread;
import poker.game.common.PokerConstants;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Queuer extends VirtualThread {

    protected final Server server;
    protected final List<Connection> queue = new ArrayList<>();
    protected final Queue<Connection> playersRequeueing = new LinkedList<>();
    protected final ReentrantLock queueLock = new ReentrantLock();
    protected final ReentrantLock requeueLock = new ReentrantLock();
    protected final ReentrantLock gameRoomsLock = new ReentrantLock();
    private final Map<String, Game> gameRooms = new HashMap<>();
    private final HashSet<Requeuer> requeuers = new HashSet<>();

    public Queuer(Server server) {
        this.server = server;
    }

    @Override
    protected void run() {
        while (!this.isInterrupted()) {
            synchronized (this) {
                queueLock.lock();
                if (queue.size() < PokerConstants.NUM_PLAYERS) {
                    queueLock.unlock();
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        stop();
                        return;
                    }
                } else {
                    if (!createGame()) {
                        try {
                            queueLock.unlock();
                            wait();
                        } catch (InterruptedException e) {
                            return;
                        }
                    } else {
                        queueLock.unlock();
                    }
                }
                requeueLock.lock();
                while (!this.playersRequeueing.isEmpty()) {
                    Connection connection = this.playersRequeueing.poll();
                    Requeuer requeuer = new Requeuer(server, this, connection);
                    requeuer.start();
                    requeuers.add(requeuer);
                }
                requeueLock.unlock();
            }
        }
        stop();
    }

    public void stop() {
        requeueLock.lock();
        for (Requeuer requeuer : requeuers) {
            requeuer.interrupt();
        }
        requeueLock.unlock();
    }

    public abstract boolean createGame();

    public void queuePlayer(Connection connection) {
        gameRoomsLock.lock();
        if (gameRooms.get(connection.getUsername()) != null) {
            server.log("Player " + connection.getUsername() + " is reconnecting to a match");
            reconnectPlayerToGame(connection);
            gameRoomsLock.unlock();
        } else {
            gameRoomsLock.unlock();
            addToMainQueue(connection);
            server.log("Player " + connection.getUsername() + " queued");
        }
    }

    public abstract void addToMainQueue(Connection connection);

    public void updateMainQueue(Connection connection) {
        int index = -1;
        queueLock.lock();
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).getUsername().equals(connection.getUsername())) {
                index = i;
                break;
            }
        }
        if (index == -1) {
            server.log("Player not found in queue when updating main queue");
            return;
        }
        Connection oldConnection = queue.get(index);
        try {
            oldConnection.getChannel().requestConnectionEnd("Another connection was found for your account");
            server.log("Replaced old connection for player " + connection.getUsername() + " in main queue");
        } catch (ClosedConnectionException e) {
            server.log("Error while disconnecting old connection for player " + connection.getUsername());
        }
        queue.set(index, connection);
        queueLock.unlock();
    }

    public synchronized void requeuePlayers(List<Connection> connections) {
        List<String> logMessage = new ArrayList<>(List.of("Requeueing players: "));
        for (Connection connection : connections) {
            logMessage.add(connection.getUsername());
        }
        server.log(logMessage.toArray(new String[0]));
        requeueLock.lock();
        this.playersRequeueing.addAll(connections);
        requeueLock.unlock();
        notify();
    }

    public void assignPlayerToRoom(Connection connection, Game game) {
        gameRoomsLock.lock();
        this.gameRooms.put(connection.getUsername(), game);
        gameRoomsLock.unlock();
    }

    public void removePlayerFromRoom(Connection connection) {
        gameRoomsLock.lock();
        this.gameRooms.remove(connection.getUsername());
        gameRoomsLock.unlock();
    }

    public void startGame(ArrayList<Connection> connections) {
        Game game = new Game(server, connections);

        List<String> logMessage = new ArrayList<>(List.of("Starting game with players: "));
        for (Connection connection : connections) {
            assignPlayerToRoom(connection, game);
            logMessage.add(connection.getUsername());
        }
        server.log(logMessage.toArray(new String[0]));

        game.start();
    }

    public void reconnectPlayerToGame(Connection connection) {

        try {
            if (connection.getChannel().requestMatchReconnect()) {
                gameRoomsLock.lock();
                Game game = gameRooms.get(connection.getUsername());
                if (game.reconnectPlayer(connection))
                    server.log("Player " + connection.getUsername() + " reconnected to respective match");
                else
                    server.log("Player " + connection.getUsername() + " could not reconnect to match");
                gameRoomsLock.unlock();
            }
        } catch (ChannelException ignored) {
        }
    }
}
