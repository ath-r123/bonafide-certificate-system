package com.bonafide.model;

public enum Status {
    ACTIVE("Active"), PASSED_OUT("Passed Out");

    private final String label;
    Status(String label) { this.label = label; }
    public String label() { return label; }
}
