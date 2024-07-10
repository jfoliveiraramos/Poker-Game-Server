package poker.client.state;

import poker.connection.protocol.channels.ClientChannel;

public abstract class ClientState {

    protected final ClientChannel channel;

    protected ClientState (ClientChannel channel) {
        this.channel = channel;
    }

    public abstract ClientState handle();
}
