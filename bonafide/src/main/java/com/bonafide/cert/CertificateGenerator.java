package com.bonafide.cert;

import com.bonafide.exception.DataAccessException;
import com.bonafide.model.Certificate;
import com.bonafide.model.Gender;
import com.bonafide.model.Institute;
import com.bonafide.model.Student;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Fills the reusable HTML template with data from the selected student.
 * No student information is hard-coded; every value comes from the record passed in.
 */
public final class CertificateGenerator {

    private static final Path OUTPUT_DIR = Path.of("certificates");

    private static final String TEMPLATE_RESOURCE = "/certificate-template.html";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);
    private static final String template = loadTemplate();

    private CertificateGenerator() { }

    public static Path pathFor(String certificateNo) {
        return OUTPUT_DIR.resolve(certificateNo.replace('/', '-') + ".html");
    }

    public static String render(Institute institute, Student student, Certificate certificate) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("institute_name", institute.name());
        values.put("institute_address", institute.address());
        values.put("principal_name", institute.principalName());
        values.put("place", institute.place());
        values.put("certificate_no", certificate.certificateNo());
        values.put("issue_date", certificate.issueDate().format(DATE));
        values.put("academic_year", certificate.academicYear());
        values.put("purpose", certificate.purposeText());
        values.put("salutation", salutation(student.gender()));
        values.put("relation", student.gender() == Gender.FEMALE ? "daughter" : "son");
        values.put("pronoun", pronoun(student.gender()));
        values.put("possessive", possessive(student.gender()));
        values.put("student_name", student.name());
        values.put("father_name", student.fatherName());
        values.put("roll_no", student.rollNo());
        values.put("dob", student.dob().format(DATE));
        values.put("course_name", student.course().name());
        values.put("department_name", student.course().department().name());
        values.put("current_year", String.valueOf(student.currentYear()));
        values.put("current_semester", String.valueOf(student.currentSemester()));
        values.put("admission_year", String.valueOf(student.admissionYear()));

        String html = template;
        for (Map.Entry<String, String> e : values.entrySet())
            html = html.replace("{{" + e.getKey() + "}}", escape(e.getValue()));
        return html;
    }

    public static void write(Path file, String html) throws IOException {
        Files.createDirectories(file.getParent());
        Files.writeString(file, html, StandardCharsets.UTF_8);
    }

    private static String salutation(Gender g) {
        return switch (g) { case MALE -> "Mr. "; case FEMALE -> "Ms. "; case OTHER -> ""; };
    }

    private static String pronoun(Gender g) {
        return switch (g) { case MALE -> "He"; case FEMALE -> "She"; case OTHER -> "The student"; };
    }

    private static String possessive(Gender g) {
        return switch (g) { case MALE -> "his"; case FEMALE -> "her"; case OTHER -> "the student's"; };
    }

    /** Student data is user supplied, so it is escaped before it reaches the HTML output. */
    private static String escape(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String loadTemplate() {
        try (InputStream in = CertificateGenerator.class.getResourceAsStream(TEMPLATE_RESOURCE)) {
            if (in == null) throw new IOException(TEMPLATE_RESOURCE + " not found on classpath");
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DataAccessException("Certificate template could not be loaded", e);
        }
    }
}
