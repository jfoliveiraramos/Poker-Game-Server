package poker.connection.protocol.exceptions;

public class ClosedConnectionException extends ChannelException {

    public ClosedConnectionException(String message) {
        super(message);
    }
}
