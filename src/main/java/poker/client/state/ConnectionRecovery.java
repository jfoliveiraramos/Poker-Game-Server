package poker.client.state;

import poker.client.LocalToken;
import poker.connection.protocol.channels.ClientChannel;
import poker.connection.protocol.exceptions.ChannelException;
import poker.connection.protocol.exceptions.ClosedConnectionException;
import poker.connection.protocol.exceptions.UnexpectedMessageException;
import poker.connection.protocol.message.Message;
import poker.utils.UserInput;

import java.util.Scanner;

public class ConnectionRecovery extends ClientState {

    public ConnectionRecovery(ClientChannel channel) {
        super(channel);
    }

    @Override
    public ClientState handle() {
        LocalToken token = LocalToken.retrieve();

        if (token != null) {
            if (intendsRecovery()) {
                try {
                    Message response = channel.recoverSession(token.toString());
                    return handleRecoveryResponse(response);
                } catch (ClosedConnectionException e) {
                    System.out.println("Connection to the server was lost.\n" + e.getMessage());
                    return null;
                } catch (ChannelException e) {
                    System.out.println("Error communicating with the server:\n" + e.getMessage());
                    return null;
                }
            }
        }
        return new Authentication(channel);
    }

    private boolean intendsRecovery() {
        String input = new UserInput().nextLine(
                "Do you wish to recover your previous session? (Y/N)",
                "N"
        );
        while (!input.equalsIgnoreCase("Y") && !input.equalsIgnoreCase("N")) {
            System.out.println("Invalid input. Please enter Y or N.");
            input = new Scanner(System.in).nextLine();
        }
        return input.equalsIgnoreCase("Y");
    }

    private ClientState handleRecoveryResponse(Message response) {
        System.out.println(response.getBody());
        if (response.isOk()) {
            String sessionToken = response.getAttribute("sessionToken");
            new LocalToken(sessionToken).save();
            channel.setSessionToken(sessionToken);
            try {
                Message message = channel.getRequest();

                switch (message.getState()) {
                    case MATCHMAKING -> {
                        channel.acceptMatchmaking();
                        return new Matchmaking(channel);
                    }
                    case MATCH_RECONNECT -> {
                        channel.acceptMatchReconnect();
                        return new Match(channel);
                    }
                    default -> {
                        throw new UnexpectedMessageException("Unexpected message received after authentication: " + message);
                    }
                }
            } catch (ClosedConnectionException e) {
                System.out.println("Connection to the server was lost.\n" + e.getMessage());
                return null;
            } catch (ChannelException e) {
                System.out.println("Failed communicating with the server after authentication:\n" + e.getMessage());
                return null;
            }
        }
        return new Authentication(channel);
    }
}
