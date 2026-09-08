package com.bonafide.model;

import com.bonafide.exception.ValidationException;
import com.bonafide.util.Validator;
import java.time.LocalDate;

public record Student(int id, String rollNo, String name, String fatherName, LocalDate dob, Gender gender,
                      Course course, int currentYear, int currentSemester, int admissionYear,
                      String email, String phone, String address, Status status) {
    public Student {
        rollNo = Validator.required(rollNo, "Roll number").toUpperCase();
        name = Validator.required(name, "Name");
        fatherName = Validator.required(fatherName, "Father's name");
        Validator.notNull(dob, "Date of birth");
        Validator.notNull(gender, "Gender");
        Validator.notNull(status, "Status");
        Validator.notNull(course, "Course");
        if (!dob.isBefore(LocalDate.now().minusYears(10)))
            throw new ValidationException("Date of birth is not plausible for a student.");
        // Absolute bounds only. The course-specific limit is applied when the value is entered:
        // a stored row must never become unreadable because its course was later shortened.
        Validator.range(currentYear, 1, 6, "Current year");
        Validator.range(currentSemester, 1, 12, "Current semester");
        Validator.range(admissionYear, 1990, LocalDate.now().getYear(), "Admission year");
        email = Validator.email(email);
        phone = Validator.phone(phone);
        address = Validator.required(address, "Address");
    }
}
