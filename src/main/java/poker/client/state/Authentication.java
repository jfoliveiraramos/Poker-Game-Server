package poker.client.state;

import poker.client.LocalToken;
import poker.connection.protocol.channels.ClientChannel;
import poker.connection.protocol.exceptions.ChannelException;
import poker.connection.protocol.exceptions.ClosedConnectionException;
import poker.connection.protocol.exceptions.UnexpectedMessageException;
import poker.connection.protocol.message.Message;
import poker.utils.UserInput;

public class Authentication extends ClientState {

    public Authentication(ClientChannel channel) {
        super(channel);
    }

    @Override
    public ClientState handle() {

        String username = new UserInput().nextLine("Enter your username:");
        String password = new UserInput().nextLine("Enter your password:");

        Message response;
        try {
            response = channel.authenticate(username, password);
        } catch (ClosedConnectionException e) {
            System.out.println("Connection to the server was lost.\n" + e.getMessage());
            return null;
        } catch (ChannelException e) {
            System.out.println("Error communicating with the server:\n" + e.getMessage());
            return null;
        }

        System.out.println(response.getBody());

        if (response.isOk()) {
            return handleAuthResponse(response);
        }

        return new Authentication(channel);
    }

    private ClientState handleAuthResponse(Message response){

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
}
