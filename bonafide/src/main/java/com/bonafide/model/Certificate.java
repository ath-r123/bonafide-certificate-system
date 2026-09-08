package com.bonafide.model;

import com.bonafide.util.Validator;
import java.time.LocalDate;

public record Certificate(int id, String certificateNo, int studentId, String academicYear,
                          Purpose purpose, String purposeDetail, LocalDate issueDate, String filePath) {
    public Certificate {
        certificateNo = Validator.required(certificateNo, "Certificate number");
        academicYear = Validator.academicYear(academicYear);
        Validator.notNull(purpose, "Purpose");
        Validator.notNull(issueDate, "Issue date");
        filePath = Validator.required(filePath, "File path");
        if (purpose == Purpose.OTHER) purposeDetail = Validator.required(purposeDetail, "Purpose detail");
        else if (purposeDetail != null && purposeDetail.isBlank()) purposeDetail = null;
    }

    /** Purpose text as printed on the certificate. */
    public String purposeText() {
        return purpose == Purpose.OTHER ? purposeDetail : purpose.label();
    }
}
