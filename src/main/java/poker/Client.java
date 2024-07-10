package poker;

import poker.client.state.ClientState;
import poker.client.state.ConnectionRecovery;
import poker.connection.client.ClientChannelFactory;
import poker.connection.protocol.channels.ClientChannel;

import java.net.ConnectException;

public class Client {
    private final ClientChannel channel;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Arguments: <host> <port>");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);

        try {
            new Client(host, port).init();
        } catch (ConnectException e) {
            System.out.println("Server is currently offline");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Client(String host, int port) throws Exception {
        channel = new ClientChannelFactory().createChannel(host, port);
    }


    public ClientChannel getChannel() {
        return channel;
    }

    private void init() {
        ClientState state = new ConnectionRecovery(channel);

        while ((state = state.handle()) != null) ;
    }
}
