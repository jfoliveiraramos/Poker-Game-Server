package poker.connection.protocol.exceptions;

public class UnexpectedMessageException extends ChannelException {

    public UnexpectedMessageException(String message) {
        super(message);
    }
}
