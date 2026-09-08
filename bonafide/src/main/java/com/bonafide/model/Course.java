package com.bonafide.model;

import com.bonafide.util.Validator;

public record Course(int id, String name, int durationYears, Department department) {
    public Course {
        name = Validator.required(name, "Course name");
        Validator.range(durationYears, 1, 6, "Course duration");
        Validator.notNull(department, "Department");
    }
}
