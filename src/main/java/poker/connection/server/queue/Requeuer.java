package poker.connection.server.queue;

import poker.Server;
import poker.connection.protocol.Connection;
import poker.connection.protocol.exceptions.ChannelException;
import poker.connection.protocol.exceptions.ClosedConnectionException;
import poker.connection.protocol.exceptions.RequestTimeoutException;
import poker.connection.protocol.message.Message;

public class Requeuer extends Thread {
    private final Server server;
    private final Queuer queuer;
    private final Connection connection;

    public Requeuer(Server server, Queuer queuer, Connection connection) {
        this.server = server;
        this.queuer = queuer;
        this.connection = connection;
    }

    private boolean playerToRequeue() {
        try {
            Message response = connection.getChannel().sendRequeueRequest(30);
            return response.getBooleanAttribute("requeue");
        } catch (RequestTimeoutException e) {
            try {
                connection.getChannel().requestConnectionEnd("Requeue request timed out");
                return false;
            } catch (ClosedConnectionException ex) {
                return false;
            }
        } catch (ChannelException e) {
            return false;
        }
    }

    @Override
    public void run() {
        if (playerToRequeue()) {
            server.log("Player " + connection.getUsername() + " requeued");
            if (!this.isInterrupted()) {
                queuer.queuePlayer(connection);
            }
        }
    }
}
