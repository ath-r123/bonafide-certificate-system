package com.bonafide.dao;

import com.bonafide.db.Database;
import com.bonafide.exception.DataAccessException;
import com.bonafide.exception.ValidationException;
import com.bonafide.model.Gender;
import com.bonafide.model.Status;
import com.bonafide.model.Student;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StudentDao {

    /** Single source of truth for the student projection; every finder appends its own filter. */
    private static final String SELECT_BASE = """
            SELECT s.student_id, s.roll_no, s.name, s.father_name, s.dob, s.gender, s.current_year,
                   s.current_semester, s.admission_year, s.email, s.phone, s.address, s.status,
                   c.course_id, c.course_name, c.duration_years, d.dept_id, d.dept_name
            FROM student s
            JOIN course c ON s.course_id = c.course_id
            JOIN department d ON c.dept_id = d.dept_id
            """;

    private static final String INSERT = """
            INSERT INTO student (roll_no, name, father_name, dob, gender, course_id, current_year,
                                 current_semester, admission_year, email, phone, address, status)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)""";

    private static final String UPDATE = """
            UPDATE student SET roll_no=?, name=?, father_name=?, dob=?, gender=?, course_id=?, current_year=?,
                               current_semester=?, admission_year=?, email=?, phone=?, address=?, status=?
            WHERE student_id=?""";

    private static final String COUNT_CERTIFICATES = "SELECT COUNT(*) FROM certificate WHERE student_id = ?";
    private static final String DELETE = "DELETE FROM student WHERE student_id = ?";

    public int insert(Student s) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, s);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            if (isConstraintViolation(e)) throw new ValidationException("Roll number or email already exists.");
            throw new DataAccessException("Failed to add student: " + e.getMessage(), e);
        }
    }

    public void update(Student s) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(UPDATE)) {
            int i = bind(ps, s);
            ps.setInt(i, s.id());
            if (ps.executeUpdate() == 0) throw new ValidationException("Student no longer exists.");
        } catch (SQLException e) {
            if (isConstraintViolation(e))
                throw new ValidationException("Roll number or email already belongs to another student.");
            throw new DataAccessException("Failed to update student: " + e.getMessage(), e);
        }
    }

    /**
     * Deletion is blocked while certificate history exists. The check and the delete run in one
     * transaction so a certificate issued concurrently cannot slip through; the FK's ON DELETE
     * RESTRICT in MySQL is the final safety layer if it ever does.
     */
    public void delete(int studentId) {
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int certificates;
                try (PreparedStatement ps = conn.prepareStatement(COUNT_CERTIFICATES)) {
                    ps.setInt(1, studentId);
                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        certificates = rs.getInt(1);
                    }
                }
                if (certificates > 0)
                    throw new ValidationException("Cannot delete this student: " + certificates
                            + " certificate(s) already issued. Certificate history is preserved permanently.");

                try (PreparedStatement ps = conn.prepareStatement(DELETE)) {
                    ps.setInt(1, studentId);
                    if (ps.executeUpdate() == 0) throw new ValidationException("Student no longer exists.");
                }
                conn.commit();
            } catch (RuntimeException | SQLException e) {
                conn.rollback();
                if (e instanceof SQLException sql && isConstraintViolation(sql))
                    throw new ValidationException("Cannot delete this student: certificate records reference it.");
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to delete student: " + e.getMessage(), e);
        }
    }

    public Optional<Student> findByRollNo(String rollNo) {
        List<Student> found = query(SELECT_BASE + "WHERE s.roll_no = ?", rollNo.trim().toUpperCase());
        return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
    }

    /** Prefix search so idx_student_name is usable. */
    public List<Student> searchByName(String namePrefix) {
        return query(SELECT_BASE + "WHERE s.name LIKE ? ORDER BY s.name", namePrefix.trim() + "%");
    }

    public List<Student> findByDepartment(int deptId) {
        return query(SELECT_BASE + "WHERE d.dept_id = ? ORDER BY s.roll_no", deptId);
    }

    public List<Student> findByCourse(int courseId) {
        return query(SELECT_BASE + "WHERE c.course_id = ? ORDER BY s.roll_no", courseId);
    }

    public List<Student> findByAdmissionYear(int year) {
        return query(SELECT_BASE + "WHERE s.admission_year = ? ORDER BY s.roll_no", year);
    }

    public List<Student> listAll() {
        return query(SELECT_BASE + "ORDER BY s.roll_no");
    }

    private List<Student> query(String sql, Object... params) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                if (params[i] instanceof Integer n) ps.setInt(i + 1, n);
                else ps.setString(i + 1, (String) params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Student> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Student query failed: " + e.getMessage(), e);
        }
    }

    private static int bind(PreparedStatement ps, Student s) throws SQLException {
        ps.setString(1, s.rollNo());
        ps.setString(2, s.name());
        ps.setString(3, s.fatherName());
        ps.setDate(4, Date.valueOf(s.dob()));
        ps.setString(5, s.gender().name());
        ps.setInt(6, s.course().id());
        ps.setInt(7, s.currentYear());
        ps.setInt(8, s.currentSemester());
        ps.setInt(9, s.admissionYear());
        ps.setString(10, s.email());
        ps.setString(11, s.phone());
        ps.setString(12, s.address());
        ps.setString(13, s.status().name());
        return 14;
    }

    private static Student map(ResultSet rs) throws SQLException {
        return new Student(
                rs.getInt("student_id"), rs.getString("roll_no"), rs.getString("name"),
                rs.getString("father_name"), rs.getDate("dob").toLocalDate(),
                Gender.valueOf(rs.getString("gender")), CourseDao.mapCourse(rs),
                rs.getInt("current_year"), rs.getInt("current_semester"), rs.getInt("admission_year"),
                rs.getString("email"), rs.getString("phone"), rs.getString("address"),
                Status.valueOf(rs.getString("status")));
    }

    /** True for unique-key and foreign-key violations (SQLState class 23), independent of driver. */
    private static boolean isConstraintViolation(SQLException e) {
        return e instanceof SQLIntegrityConstraintViolationException
                || (e.getSQLState() != null && e.getSQLState().startsWith("23"));
    }
}
