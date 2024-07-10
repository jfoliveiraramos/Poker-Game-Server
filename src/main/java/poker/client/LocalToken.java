package poker.client;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class LocalToken {
    String token;

    public LocalToken(String token) {
        this.token = token;
    }

    static public LocalToken retrieve() {
        try {
            String path = System.getProperty("user.dir") + "/src/main/java/poker/client/";
            File file = new File(path + "session.txt");
            String token = new Scanner(file).nextLine();
            return new LocalToken(token);
        } catch (IOException e) {
            return null;
        }
    }

    public void save() {
        try {
            String path = System.getProperty("user.dir") + "/src/main/java/poker/client/";
            File file = new File(path + "session.txt");
            FileWriter writer = new FileWriter(file);
            writer.write(token);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String toString() {
        return token;
    }
}
