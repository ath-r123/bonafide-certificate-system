package com.bonafide.model;

import com.bonafide.util.Validator;

public record Department(int id, String name) {
    public Department {
        name = Validator.required(name, "Department name");
    }
}
