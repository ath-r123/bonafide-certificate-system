package com.bonafide.util;

import com.bonafide.exception.ValidationException;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Validator {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");
    private static final Pattern PHONE = Pattern.compile("^\\d{10}$");
    private static final Pattern ACADEMIC_YEAR = Pattern.compile("^(\\d{4})-(\\d{2})$");

    private Validator() { }

    public static String required(String value, String field) {
        if (value == null || value.isBlank()) throw new ValidationException(field + " is required.");
        return value.trim();
    }

    public static <T> T notNull(T value, String field) {
        if (value == null) throw new ValidationException(field + " is required.");
        return value;
    }

    public static int range(int value, int min, int max, String field) {
        if (value < min || value > max)
            throw new ValidationException(field + " must be between " + min + " and " + max + ".");
        return value;
    }

    public static String email(String value) {
        String v = required(value, "Email").toLowerCase();
        if (!EMAIL.matcher(v).matches()) throw new ValidationException("Email format is invalid.");
        return v;
    }

    public static String phone(String value) {
        String v = required(value, "Phone").replaceAll("[\\s-]", "");
        if (!PHONE.matcher(v).matches()) throw new ValidationException("Phone must be exactly 10 digits.");
        return v;
    }

    /** Accepts 2025-26 style values where the second part is the year following the first. */
    public static String academicYear(String value) {
        String v = required(value, "Academic year");
        Matcher m = ACADEMIC_YEAR.matcher(v);
        if (!m.matches()) throw new ValidationException("Academic year must look like 2025-26.");
        int start = Integer.parseInt(m.group(1));
        if ((start + 1) % 100 != Integer.parseInt(m.group(2)))
            throw new ValidationException("Academic year must span consecutive years, e.g. 2025-26.");
        return v;
    }

    /** Academic year is assumed to start in June. */
    public static String currentAcademicYear() {
        LocalDate today = LocalDate.now();
        int start = today.getMonthValue() >= 6 ? today.getYear() : today.getYear() - 1;
        return String.format("%d-%02d", start, (start + 1) % 100);
    }
}
