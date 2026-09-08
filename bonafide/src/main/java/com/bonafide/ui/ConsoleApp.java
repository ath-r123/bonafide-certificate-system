package com.bonafide.ui;

import com.bonafide.dao.CertificateDao;
import com.bonafide.dao.CourseDao;
import com.bonafide.dao.InstituteDao;
import com.bonafide.dao.StudentDao;
import com.bonafide.db.Database;
import com.bonafide.exception.DataAccessException;
import com.bonafide.exception.ValidationException;
import com.bonafide.model.Certificate;
import com.bonafide.model.Course;
import com.bonafide.model.Department;
import com.bonafide.model.Gender;
import com.bonafide.model.Institute;
import com.bonafide.model.Purpose;
import com.bonafide.model.Status;
import com.bonafide.model.Student;
import com.bonafide.util.Validator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/** Text menu front end. All operations are delegated to the DAOs. */
public class ConsoleApp {

    private final ConsoleReader in = new ConsoleReader();
    private final StudentDao students = new StudentDao();
    private final CourseDao courses = new CourseDao();
    private final CertificateDao certificates = new CertificateDao();
    private final Institute institute;

    private ConsoleApp(Institute institute) {
        this.institute = institute;
    }

    public static void main(String[] args) {
        Institute institute;
        try {
            Database.applyBrandingUpdates();
            institute = new InstituteDao().find();   // also verifies the connection at startup
        } catch (DataAccessException e) {
            System.err.println("Startup failed: " + e.getMessage());
            System.err.println("Check db.properties and make sure MySQL is running and schema.sql has been applied.");
            System.exit(1);
            return;
        }
        new ConsoleApp(institute).run();
    }

    private void run() {
        System.out.println("\n=== Bonafide Certificate Management System ===");
        System.out.println(institute.name());
        boolean running = true;
        while (running) {
            System.out.println("""
                    
                    1) Add student            5) View all students
                    2) Update student         6) Generate bonafide certificate
                    3) Delete student         7) View certificates of a student
                    4) Search students        8) Departments and courses
                    0) Exit""");
            try {
                switch (in.number("Choice", 0, 8, null)) {
                    case 1 -> addStudent();
                    case 2 -> updateStudent();
                    case 3 -> deleteStudent();
                    case 4 -> searchStudents();
                    case 5 -> printStudents(students.listAll());
                    case 6 -> generateCertificate();
                    case 7 -> viewCertificates();
                    case 8 -> listCatalog();
                    default -> running = false;
                }
            } catch (ConsoleReader.EndOfInput e) {
                running = false;
            } catch (ValidationException e) {
                System.out.println("! " + e.getMessage());
            } catch (DataAccessException e) {
                System.out.println("! Database error: " + e.getMessage());
            }
        }
        System.out.println("Bye.");
    }

    // ---------- students ----------

    private void addStudent() {
        Course course = selectCourse();
        Student student = new Student(0,
                in.text("Roll number", null),
                in.text("Name", null),
                in.text("Father's name", null),
                in.date("Date of birth", null),
                in.option("Gender", Gender.values(), null),
                course,
                in.number("Current year", 1, course.durationYears(), null),
                in.number("Current semester", 1, course.durationYears() * 2, null),
                in.number("Admission year", 1990, java.time.LocalDate.now().getYear(), null),
                in.text("Email", null),
                in.text("Phone (10 digits)", null),
                in.text("Address", null),
                Status.ACTIVE);
        int id = students.insert(student);
        System.out.println("Student added with id " + id + ".");
    }

    private void updateStudent() {
        Student current = requireStudent();
        System.out.println("Press Enter to keep the value shown in brackets.");
        Course course = in.confirm("Change course?") ? selectCourse() : current.course();
        Student updated = new Student(current.id(),
                in.text("Roll number", current.rollNo()),
                in.text("Name", current.name()),
                in.text("Father's name", current.fatherName()),
                in.date("Date of birth", current.dob()),
                in.option("Gender", Gender.values(), current.gender()),
                course,
                in.number("Current year", 1, course.durationYears(), current.currentYear()),
                in.number("Current semester", 1, course.durationYears() * 2, current.currentSemester()),
                in.number("Admission year", 1990, java.time.LocalDate.now().getYear(), current.admissionYear()),
                in.text("Email", current.email()),
                in.text("Phone", current.phone()),
                in.text("Address", current.address()),
                in.option("Status", Status.values(), current.status()));
        students.update(updated);
        System.out.println("Student updated.");
    }

    private void deleteStudent() {
        Student student = requireStudent();
        System.out.printf("%s (%s), %s%n", student.name(), student.rollNo(), student.course().name());
        if (!in.confirm("Delete this student permanently?")) {
            System.out.println("Cancelled.");
            return;
        }
        students.delete(student.id());
        System.out.println("Student deleted.");
    }

    private void searchStudents() {
        System.out.println("""
                1) By roll number   2) By name (starts with)
                3) By department    4) By course
                5) By admission year""");
        switch (in.number("Search by", 1, 5, null)) {
            case 1 -> printStudents(students.findByRollNo(in.text("Roll number", null))
                    .map(List::of).orElse(List.of()));
            case 2 -> printStudents(students.searchByName(in.text("Name starts with", null)));
            case 3 -> printStudents(students.findByDepartment(selectDepartment().id()));
            case 4 -> printStudents(students.findByCourse(selectCourse().id()));
            default -> printStudents(students.findByAdmissionYear(
                    in.number("Admission year", 1990, java.time.LocalDate.now().getYear(), null)));
        }
    }

    // ---------- certificates ----------

    private void generateCertificate() {
        Student student = requireStudent();
        System.out.printf("%s (%s), %s, Year %d%n",
                student.name(), student.rollNo(), student.course().name(), student.currentYear());
        String academicYear = Validator.academicYear(
                in.text("Academic year", Validator.currentAcademicYear()));
        Purpose purpose = in.option("Purpose", Purpose.values(), null);
        String detail = purpose == Purpose.OTHER ? in.text("Describe the purpose", null) : null;

        Certificate certificate = certificates.issue(institute, student, academicYear, purpose, detail);
        System.out.println("Certificate " + certificate.certificateNo() + " generated.");
        System.out.println("File: " + certificate.filePath().replace('\\', '/')
                + "  (open in a browser, then print or save as PDF)");
    }

    private void viewCertificates() {
        Student student = requireStudent();
        List<Certificate> list = certificates.findByStudent(student.id());
        if (list.isEmpty()) {
            System.out.println("No certificates issued for " + student.name() + ".");
            return;
        }
        System.out.printf("%n%-20s %-12s %-24s %-12s %s%n",
                "CERTIFICATE NO", "ISSUED", "PURPOSE", "ACAD. YEAR", "FILE");
        for (Certificate c : list) {
            boolean present = Files.exists(Path.of(c.filePath()));
            System.out.printf("%-20s %-12s %-24s %-12s %s%s%n",
                    c.certificateNo(), c.issueDate(), c.purposeText(), c.academicYear(), c.filePath(),
                    present ? "" : "   [FILE MISSING]");
        }
    }

    // ---------- shared ----------

    private void listCatalog() {
        for (Department d : courses.listDepartments()) {
            System.out.println("\n" + d.name());
            for (Course c : courses.listCourses(d.id()))
                System.out.printf("   %s (%d years)%n", c.name(), c.durationYears());
        }
    }

    private Student requireStudent() {
        Optional<Student> found = students.findByRollNo(in.text("Student roll number", null));
        return found.orElseThrow(() -> new ValidationException("No student found with that roll number."));
    }

    private Department selectDepartment() {
        return choose("Department", courses.listDepartments(), Department::name,
                "No departments configured. Run schema.sql first.");
    }

    private Course selectCourse() {
        return choose("Course", courses.listCourses(selectDepartment().id()), Course::name,
                "This department has no courses.");
    }

    /** Prints a numbered list and returns the chosen item. */
    private <T> T choose(String label, List<T> items, Function<T, String> nameOf, String emptyMessage) {
        if (items.isEmpty()) throw new ValidationException(emptyMessage);
        for (int i = 0; i < items.size(); i++) System.out.printf("  %d) %s%n", i + 1, nameOf.apply(items.get(i)));
        return items.get(in.number(label, 1, items.size(), null) - 1);
    }

    private static void printStudents(List<Student> list) {
        if (list.isEmpty()) {
            System.out.println("No matching students.");
            return;
        }
        System.out.printf("%n%-12s %-24s %-30s %-6s %-6s %s%n",
                "ROLL NO", "NAME", "COURSE", "YEAR", "SEM", "STATUS");
        for (Student s : list)
            System.out.printf("%-12s %-24s %-30s %-6d %-6d %s%n",
                    s.rollNo(), s.name(), s.course().name(), s.currentYear(), s.currentSemester(), s.status().label());
        System.out.println(list.size() + " record(s).");
    }
}
