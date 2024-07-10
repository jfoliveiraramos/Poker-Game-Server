package poker.connection.protocol;

import org.json.JSONObject;
import poker.connection.protocol.exceptions.*;
import poker.connection.protocol.message.Message;
import poker.connection.protocol.message.State;
import poker.connection.protocol.message.Status;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.*;

import static poker.connection.protocol.message.State.CONNECTION_CHECK;
import static poker.connection.protocol.message.State.CONNECTION_END;
import static poker.connection.protocol.message.Status.OK;
import static poker.connection.protocol.message.Status.REQUEST;

public abstract class Channel {
    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private String sessionToken;

    public Channel(Socket socket) throws IOException {
        this.socket = socket;
        InputStream input = socket.getInputStream();
        OutputStream output = socket.getOutputStream();
        reader = new BufferedReader(new InputStreamReader(input));
        writer = new PrintWriter(output, true);
        sessionToken = null;
    }

    public String getAddress() {
        return socket.getInetAddress().getHostAddress();
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    protected void sendMessage(Message message) throws ClosedConnectionException {
        try {
            writer.println(message);
        } catch (Exception e) {
            throw new ClosedConnectionException("Connection closed by the other party");
        }
    }

    protected void sendMessage(State state, Status status, String body, Map<String, Object> data) throws ClosedConnectionException {
        sendMessage(new Message(state, status, body, data, sessionToken));
    }

    private Message getMessage() throws ChannelException {

        try {
            String line = reader.readLine();
            JSONObject json = new JSONObject(line);
            return new Message(json);
        } catch (IOException e) {
            throw new ClosedConnectionException("Connection closed by the other party: " + e.getMessage());
        }
    }

    private Message getMessage(int timeout) throws ChannelException {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        Future<String> future = executor.submit(reader::readLine);
        try {
            String line;

            line = future.get(timeout, TimeUnit.SECONDS);
            JSONObject json = new JSONObject(line);
            return new Message(json);

        } catch (TimeoutException e) {
            throw new RequestTimeoutException("Timeout while waiting for message");
        } catch (ExecutionException | InterruptedException e) {
            throw new ClosedConnectionException("Connection closed by the other party: " + e.getMessage());
        }
    }

    protected Message getMessage(State expectedState, boolean isRequestExpected, Integer timeout) throws ChannelException {

        if (isClosed()) {
            throw new ClosedConnectionException("Connection is closed");
        }

        Message message = (timeout != null) ? getMessage(timeout) : getMessage();

        if (!message.matchesSessionToken(sessionToken)) {
            throw new TokenMismatchException(String.format(
                    "Expected session token %s but got %s:\n%s",
                    sessionToken, message.getAttribute("sessionToken"), message)
            );
        }
        if (message.isConnectionEndRequest()) {
            throw new ClosedConnectionException(String.format(
                    "Connection closed by the other party: %s", message.getBody())
            );
        }
        if (message.isConnectionCheckRequest()) {
            acceptConnectionCheck();
            return getMessage(expectedState, isRequestExpected, timeout);
        }
        if (expectedState != null && message.getState() != expectedState) {
            throw new UnexpectedMessageException(String.format(
                    "Expected state %s but got %s:\n%s",
                    expectedState, message.getState(), message)
            );
        }
        if (isRequestExpected && !message.isRequest()) {
            throw new UnexpectedMessageException("Expected request but got response:\n" + message);
        } else if (!isRequestExpected && message.isRequest()) {
            throw new UnexpectedMessageException("Expected response but got request:\n" + message);
        }
        return message;
    }

    public Message getResponse(State expectedState) throws ChannelException {
        return getMessage(expectedState, false, null);
    }

    public Message getResponse(State expectedState, Integer timeout) throws ChannelException {
        return getMessage(expectedState, false, timeout);
    }

    public Message getRequest() throws ChannelException {
        return getMessage(null, true, null);
    }

    public Message getRequest(Integer timeout) throws ChannelException {
        return getMessage(null, true, timeout);
    }

    public Message getRequest(State expectedState) throws ChannelException {
        return getMessage(expectedState, true, null);
    }

    public Message getRequest(State expectedState, Integer timeout) throws ChannelException {
        return getMessage(expectedState, true, timeout);
    }

    public void requestConnectionEnd(String body) throws ClosedConnectionException {
        sendMessage(CONNECTION_END, REQUEST, body, null);
    }

    private Message requestConnectionCheck() throws ChannelException {
        sendMessage(CONNECTION_CHECK, REQUEST, null, null);
        return getResponse(CONNECTION_CHECK, 3);
    }

    private void acceptConnectionCheck() throws ClosedConnectionException {
        sendMessage(CONNECTION_CHECK, OK, null, null);
    }

    public boolean isOpen() {
        return !socket.isClosed();
    }

    public boolean isClosed() {
        return socket.isClosed();
    }

    public boolean isAlive() {
        Message response;
        try {
            response = requestConnectionCheck();
        } catch (ChannelException e) {
            return false;
        }
        return response != null && response.isOk();
    }

    public boolean isBroken() {
        return !isAlive();
    }
}
