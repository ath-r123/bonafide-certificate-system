package com.bonafide.ui;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/** Console input helpers. A null/blank default means the value is mandatory. */
class ConsoleReader {

    /** Signals that stdin is exhausted (e.g. piped input or Ctrl-D) so the menu loop can stop. */
    static class EndOfInput extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    private final Scanner scanner = new Scanner(System.in);

    String line(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) throw new EndOfInput();
        return scanner.nextLine().trim();
    }

    String text(String label, String current) {
        while (true) {
            String value = line(prompt(label, current));
            if (!value.isEmpty()) return value;
            if (current != null) return current;
            System.out.println("  ! This field is required.");
        }
    }

    int number(String label, int min, int max, Integer current) {
        while (true) {
            String value = line(prompt(label, current == null ? null : String.valueOf(current)));
            if (value.isEmpty() && current != null) return current;
            try {
                int n = Integer.parseInt(value);
                if (n < min || n > max) System.out.println("  ! Enter a value between " + min + " and " + max + ".");
                else return n;
            } catch (NumberFormatException e) {
                System.out.println("  ! Enter a valid number.");
            }
        }
    }

    LocalDate date(String label, LocalDate current) {
        while (true) {
            String value = line(prompt(label + " (yyyy-mm-dd)", current == null ? null : current.toString()));
            if (value.isEmpty() && current != null) return current;
            try {
                return LocalDate.parse(value);
            } catch (DateTimeParseException e) {
                System.out.println("  ! Use the format yyyy-mm-dd, e.g. 2005-04-19.");
            }
        }
    }

    <E extends Enum<E>> E option(String label, E[] options, E current) {
        System.out.println(label + ":");
        for (int i = 0; i < options.length; i++) System.out.printf("  %d) %s%n", i + 1, options[i].name());
        int choice = number("  Choose", 1, options.length, current == null ? null : indexOf(options, current) + 1);
        return options[choice - 1];
    }

    boolean confirm(String prompt) {
        return line(prompt + " (y/N): ").equalsIgnoreCase("y");
    }

    private static <E> int indexOf(E[] options, E value) {
        for (int i = 0; i < options.length; i++) if (options[i] == value) return i;
        return 0;
    }

    private static String prompt(String label, String current) {
        return current == null ? label + ": " : label + " [" + current + "]: ";
    }
}
