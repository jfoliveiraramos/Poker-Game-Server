package database;

import poker.connection.server.database.DatabaseInterface;

import java.io.IOException;
import java.sql.SQLException;

public class DatabaseInitializer {

    public static void main(String[] args) throws SQLException, IOException {
        DatabaseInterface database = new DatabaseInterface();
        database.reset();
        populate(database);
    }

    private static void populate(DatabaseInterface database) {
        database.registerUser("marco", "marco", 1000);
        database.registerUser("tiago", "tiago", 2000);
        database.registerUser("ramos", "ramos", 1000);
        database.registerUser("joao", "joao", 1500);
        database.registerUser("rita", "rita", 500);
        database.registerUser("jorge", "jorge", 0);
        database.registerUser("afonso", "afonso", 5000);
        database.registerUser("camilla", "camilla", 5000);
        database.registerUser("baquero", "baquero");
        database.registerUser("alberto", "alberto");
        database.registerUser("veronica", "veronica");
    }
}
