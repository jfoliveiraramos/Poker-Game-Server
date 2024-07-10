package poker.client.state;

import poker.connection.protocol.channels.ClientChannel;
import poker.connection.protocol.exceptions.ChannelException;
import poker.connection.protocol.exceptions.ClosedConnectionException;

public class Matchmaking extends ClientState {

    public Matchmaking(ClientChannel channel) {
        super(channel);
    }

    @Override
    public ClientState handle() {

        System.out.println("Waiting for other players to join...");

        try {
            channel.handleGameStartRequest();
        } catch (ClosedConnectionException e) {
            System.out.println("Connection to the server was lost.\n" + e.getMessage());
            return null;
        } catch (ChannelException e) {
            System.out.println("Error communicating with the server:\n" + e.getMessage());
            return null;
        }

        System.out.println("Game is starting...");

        return new Match(channel);
    }
}
