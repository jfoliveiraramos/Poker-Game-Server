package poker.connection.protocol.exceptions;

public class TokenMismatchException extends ChannelException {

    public TokenMismatchException(String message) {
        super(message);
    }
}
