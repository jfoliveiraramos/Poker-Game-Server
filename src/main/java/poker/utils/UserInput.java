package poker.utils;

import java.util.Scanner;

public class UserInput {

    public String nextLine() {
        return new Scanner(System.in).nextLine().trim();
    }

    public String nextLine(String prompt) {
        System.out.println(prompt);
        return nextLine();
    }

    public String nextLine(String prompt, String defaultValue) {
        System.out.println(prompt);
        String input = nextLine();
        return input.isEmpty() ? defaultValue : input;
    }

    public int nextInt() {
        return new Scanner(System.in).nextInt();
    }

    public int nextInt(String prompt) {
        System.out.println(prompt);
        return nextInt();
    }

    public int nextInt(String prompt, int defaultValue) {
        System.out.println(prompt);
        String input = nextLine();
        return input.isEmpty() ? defaultValue : Integer.parseInt(input);
    }
}
