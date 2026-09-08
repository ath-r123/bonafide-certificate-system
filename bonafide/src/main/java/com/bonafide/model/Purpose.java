package com.bonafide.model;

public enum Purpose {
    BANK_LOAN("Bank Loan"), PASSPORT("Passport Application"), SCHOLARSHIP("Scholarship"), OTHER("Other");

    private final String label;
    Purpose(String label) { this.label = label; }
    public String label() { return label; }
}
