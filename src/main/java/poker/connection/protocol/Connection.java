package poker.connection.protocol;

import poker.connection.protocol.channels.ServerChannel;

public class Connection {
    private final String username;
    private final String sessionToken;
    private final ServerChannel channel;
    private int rank;

    public Connection(String username, String sessionToken, ServerChannel channel, int rank) {
        this.username = username;
        this.sessionToken = sessionToken;
        this.channel = channel;
        this.rank = rank;
    }

    public String getUsername() {
        return username;
    }

    public String getSession() {
        return sessionToken;
    }

    public ServerChannel getChannel() {
        return channel;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public boolean isBroken() {
        return channel.isBroken();
    }
}
