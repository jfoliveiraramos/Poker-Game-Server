package poker.connection.server.authentication;

import org.mindrot.jbcrypt.BCrypt;
import poker.Server;
import poker.connection.protocol.Connection;
import poker.connection.protocol.channels.ServerChannel;
import poker.connection.protocol.exceptions.ChannelException;
import poker.connection.protocol.exceptions.ClosedConnectionException;
import poker.connection.protocol.exceptions.RequestTimeoutException;
import poker.connection.protocol.message.Message;
import poker.connection.server.database.DatabaseInterface;
import poker.connection.utils.VirtualThread;

import java.sql.SQLException;

public class Authenticator extends VirtualThread {
    private final Server server;
    private final ServerChannel channel;
    private final DatabaseInterface database;

    private int authenticationAttempts = 3;

    public Authenticator(Server server, ServerChannel channel) {
        this.server = server;
        this.channel = channel;
        this.database = server.getDatabase();
    }

    @Override
    protected void run() {
        try {
            Connection connection = handleRequests();
            if (connection != null) {
                server.log(
                        "Authentication completed for " + connection.getUsername(),
                        "From address: " + channel.getAddress(),
                        "With session token: " + connection.getSession()
                );
                channel.setSessionToken(connection.getSession());
                server.queuePlayer(connection);
            }
        } catch (ClosedConnectionException ignored) {}
    }

    private Connection handleRequests() throws ClosedConnectionException {
        Connection connection = null;
        Message request;
        while (channel.isOpen() && connection == null && !this.isInterrupted()) {
            try {
                request = channel.getRequest(30);
            } catch (RequestTimeoutException e) {
                server.log("Authentication timed out for " + channel.getAddress());
                return null;
            } catch (ClosedConnectionException e) {
                server.log("Connection with " + channel.getAddress() + " closed during authentication");
                return null;
            } catch (ChannelException e) {
                terminateConnection(e.getMessage());
                server.log("Error while reading request from " + channel.getAddress() + " during authentication");
                return null;
            }
            switch (request.getState()) {
                case AUTHENTICATION -> {
                    connection = authenticateUser(request);
                }
                case CONNECTION_RECOVERY -> {
                    connection = recoverSession(request);
                }
                case null, default -> {
                    terminateConnection("Invalid request");
                    server.log("Invalid request from " + channel.getAddress() + " during authentication");
                    return null;
                }
            }
        }

        return connection;
    }

    private void terminateConnection(String body) {
        try {
            channel.requestConnectionEnd(body);
        } catch (ClosedConnectionException ignored) {}
    }

    private Connection recoverSession(Message message) throws ClosedConnectionException {
        if (!message.hasAttribute("sessionToken")) {
            channel.rejectConnectionRecovery("Missing session token");
            return null;
        }

        String token = message.getAttribute("sessionToken");
        String username = database.recoverSession(token);

        if (username != null) {
            String newToken = generateSession(username);
            if (newToken != null) {
                String body = "Session recovered successfully. Welcome back, " + username + "!";
                channel.acceptConnectionRecovery(body, newToken);
                return new Connection(username, newToken, channel, database.getUserRank(username));
            }
            else
                rejectAuthentication("Something went wrong while generating session");
        } else {
            channel.rejectConnectionRecovery("Invalid or expired session token");
        }

        return null;
    }

    private Connection authenticateUser(Message request) throws ClosedConnectionException {
        if (!(request.hasAttribute("username") && request.hasAttribute("password"))) {
            channel.rejectAuthentication("Missing username or password");
            return null;
        }

        String username = request.getAttribute("username");
        String password = request.getAttribute("password");

        try {
            return database.userExists(username) ? handleLogin(username, password) : handleRegistration(username, password);

        } catch (SQLException e) {
            rejectAuthentication("Something went wrong while authenticating user");
            return null;
        }
    }

    private Connection handleRegistration(String username, String password) throws SQLException, ClosedConnectionException {
        if (database.registerUser(username, password)) {
            Connection connection = login(username, password);
            if (connection != null) {
                channel.acceptAuthentication("User successfully registered", connection.getSession());
                return connection;
            }
        }
        else
            rejectAuthentication("Something went wrong while registering user");
        return null;
    }

    private Connection handleLogin(String username, String password) throws SQLException, ClosedConnectionException {
        Connection connection = login(username, password);
        if (connection != null) {
            channel.acceptAuthentication("User successfully logged in", connection.getSession());
            return connection;
        }
        return null;
    }

    private Connection login(String username, String password) throws SQLException, ClosedConnectionException {
        if (database.authenticateUser(username, password)) {
            String token = generateSession(username);
            if (token != null) {
                return new Connection(username, token, channel, database.getUserRank(username));
            } else
                rejectAuthentication("Something went wrong while generating session");
        } else
            rejectAuthentication("Invalid username or password");
        return null;
    }

    private void rejectAuthentication(String body) throws ClosedConnectionException {
        if (--authenticationAttempts == 0)
            terminateConnection("Too many failed authentication attempts");
        else
            channel.rejectAuthentication(body);
    }

    private String generateSession(String username) {
        String token = BCrypt.hashpw(username, BCrypt.gensalt());
        long durationMillis = 24 * 3600 * 1000;
        if (database.createSession(username, token, durationMillis)) {
            return token;
        }
        return null;
    }
}
