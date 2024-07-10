package poker.connection.server.database;

import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;

public class DatabaseInterface {
    private final Connection database;
    private final String path = System.getProperty("user.dir") + "/database/";

    public DatabaseInterface() {
        String dbFile = path + "poker.db";

        try {
            if (!Files.exists(Paths.get(dbFile))) {
                database = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
                reset();
            } else {
                database = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
            }
        } catch (SQLException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean userExists(String username) throws SQLException {
        String query = "SELECT * FROM User WHERE username = ?";

        PreparedStatement stmt = database.prepareStatement(query);
        stmt.setString(1, username);

        return stmt.executeQuery().next();
    }

    public boolean authenticateUser(String username, String password) throws SQLException {
        String query = "SELECT password FROM User WHERE username = ?";

        PreparedStatement stmt = database.prepareStatement(query);
        stmt.setString(1, username);

        ResultSet rs = stmt.executeQuery();
        if (!rs.next()) {
            return false;
        }

        String hashedPassword = rs.getString("password");
        return BCrypt.checkpw(password, hashedPassword);
    }

    public boolean registerUser(String username, String password) {
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        String query = "INSERT INTO User (username, password) VALUES (?, ?)";

        try {
            PreparedStatement stmt = database.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean registerUser(String username, String password, int rank) {
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());
        String query = "INSERT INTO User (username, password, rank) VALUES (?, ?, ?)";

        try {
            PreparedStatement stmt = database.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, hashedPassword);
            stmt.setInt(3, rank);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public boolean createSession(String username, String sessionToken, long duration) {
        String query = "UPDATE User SET session_token = ?, session_expiration = ? WHERE username = ?";

        try {
            PreparedStatement stmt = database.prepareStatement(query);
            stmt.setString(1, sessionToken);
            stmt.setDate(2, new Date(System.currentTimeMillis() + duration));
            stmt.setString(3, username);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public String recoverSession(String sessionToken) {
        String query = "SELECT username FROM User WHERE session_token = ? AND session_expiration > ?";

        try {
            PreparedStatement stmt = database.prepareStatement(query);
            stmt.setString(1, sessionToken);
            stmt.setDate(2, new Date(System.currentTimeMillis()));
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("username");
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public String getUserSession(String username) {
        String query = "SELECT session_token FROM User WHERE username = ?";

        try {
            PreparedStatement stmt = database.prepareStatement(query);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getString("session_token");
            }
            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public int getUserRank(String username) {
        String query = "SELECT rank FROM User WHERE username = ?";

        try {
            PreparedStatement stmt = database.prepareStatement(query);
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("rank");
            }
            return -1;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateRank(String username, int rankIncrement) {
        String query = "UPDATE User SET rank = rank + ? WHERE username = ?";

        try {
            PreparedStatement stmt = database.prepareStatement(query);
            stmt.setInt(1, rankIncrement);
            stmt.setString(2, username);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void reset() throws IOException, SQLException {
        String sqlFile = path + "poker.sql";

        String sql = Files.readString(Paths.get(sqlFile));

        database.createStatement().executeUpdate(sql);
    }
}
