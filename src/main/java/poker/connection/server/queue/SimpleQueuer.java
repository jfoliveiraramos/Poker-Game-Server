package poker.connection.server.queue;

import poker.Server;
import poker.connection.protocol.Connection;
import poker.connection.protocol.exceptions.ChannelException;
import poker.game.common.PokerConstants;

import java.util.ArrayList;

public class SimpleQueuer extends Queuer {

    public SimpleQueuer(Server server) {
        super(server);
    }

    public boolean createGame() {
        queueLock.lock();
        ArrayList<Connection> connections = new ArrayList<>(
                queue.subList(0, PokerConstants.NUM_PLAYERS)
        );
        boolean allAlive = true;
        for (Connection connection : connections) {
            if (connection.isBroken()) {
                allAlive = false;
                queue.remove(connection);
                break;
            }
        }

        if (allAlive) {
            for (Connection connection : connections) {
                queue.remove(connection);
            }
            startGame(connections);
        }
        queueLock.unlock();
        return allAlive;
    }

    @Override
    public synchronized void addToMainQueue(Connection connection) {

        try {
            queueLock.lock();
            if (connection.getChannel().requestMatchmaking()) {
                if (queue.stream().noneMatch(c -> c.getUsername().equals(connection.getUsername()))) {
                    queue.add(connection);
                    queueLock.unlock();
                    notify();
                } else {
                    updateMainQueue(connection);
                    queueLock.unlock();
                }
            }
        } catch (ChannelException e) {
            queueLock.unlock();
        }
    }
}
