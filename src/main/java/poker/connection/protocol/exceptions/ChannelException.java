package poker.connection.protocol.exceptions;

public abstract class ChannelException extends Exception {

    public ChannelException(String message) {
        super(message);
    }
}