package poker.connection.protocol.exceptions;

public class RequestTimeoutException extends ChannelException {

    public RequestTimeoutException(String message) {
        super(message);
    }
}
