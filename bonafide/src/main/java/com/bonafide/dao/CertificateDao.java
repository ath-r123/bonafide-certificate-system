package com.bonafide.dao;

import com.bonafide.cert.CertificateGenerator;
import com.bonafide.db.Database;
import com.bonafide.exception.DataAccessException;
import com.bonafide.model.Certificate;
import com.bonafide.model.Institute;
import com.bonafide.model.Purpose;
import com.bonafide.model.Student;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CertificateDao {

    private static final String RESERVE_SEQ = """
            INSERT INTO certificate_seq (academic_year, last_seq) VALUES (?, 1)
            ON DUPLICATE KEY UPDATE last_seq = last_seq + 1""";
    private static final String READ_SEQ =
            "SELECT last_seq FROM certificate_seq WHERE academic_year = ?";

    private static final String INSERT = """
            INSERT INTO certificate (certificate_no, student_id, academic_year, purpose,
                                     purpose_detail, issue_date, file_path)
            VALUES (?,?,?,?,?,?,?)""";

    private static final String SELECT_BY_STUDENT = """
            SELECT certificate_id, certificate_no, student_id, academic_year, purpose,
                   purpose_detail, issue_date, file_path
            FROM certificate WHERE student_id = ? ORDER BY issue_date DESC, certificate_id DESC""";

    /**
     * Issues a certificate as one atomic unit: reserve the next number for the academic year,
     * insert the record, then render the HTML file. Any failure rolls the number and the row back
     * and removes a partially written file, so numbers and files can never disagree with the table.
     */
    public Certificate issue(Institute institute, Student student, String academicYear,
                             Purpose purpose, String purposeDetail) {
        Path file = null;
        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String certificateNo = String.format("BC/%s/%04d", academicYear, nextSequence(conn, academicYear));
                file = CertificateGenerator.pathFor(certificateNo);

                Certificate certificate = new Certificate(0, certificateNo, student.id(), academicYear,
                        purpose, purposeDetail, LocalDate.now(), file.toString().replace('\\', '/'));

                int id = insert(conn, certificate);
                CertificateGenerator.write(file, CertificateGenerator.render(institute, student, certificate));
                conn.commit();

                return new Certificate(id, certificateNo, student.id(), academicYear, purpose,
                        purposeDetail, certificate.issueDate(), certificate.filePath());
            } catch (IOException | RuntimeException | SQLException e) {
                conn.rollback();
                deleteQuietly(file);
                if (e instanceof IOException)
                    throw new DataAccessException("Certificate file could not be written: " + e.getMessage(), e);
                if (e instanceof RuntimeException re) throw re;
                throw (SQLException) e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DataAccessException("Certificate generation failed: " + e.getMessage(), e);
        }
    }

    public List<Certificate> findByStudent(int studentId) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_STUDENT)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Certificate> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new Certificate(rs.getInt("certificate_id"), rs.getString("certificate_no"),
                            rs.getInt("student_id"), rs.getString("academic_year"),
                            Purpose.valueOf(rs.getString("purpose")), rs.getString("purpose_detail"),
                            rs.getDate("issue_date").toLocalDate(), rs.getString("file_path")));
                }
                return list;
            }
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load certificates: " + e.getMessage(), e);
        }
    }

    /**
     * Reserves the next number for the academic year. The insert-or-increment takes an exclusive
     * row lock that is held until this transaction ends, so concurrent sessions queue up behind it
     * and can never be handed the same number, including for the first certificate of a new year.
     */
    private static int nextSequence(Connection conn, String academicYear) throws SQLException {
        try (PreparedStatement reserve = conn.prepareStatement(RESERVE_SEQ)) {
            reserve.setString(1, academicYear);
            reserve.executeUpdate();
        }
        try (PreparedStatement read = conn.prepareStatement(READ_SEQ)) {
            read.setString(1, academicYear);
            try (ResultSet rs = read.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static int insert(Connection conn, Certificate c) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.certificateNo());
            ps.setInt(2, c.studentId());
            ps.setString(3, c.academicYear());
            ps.setString(4, c.purpose().name());
            ps.setString(5, c.purposeDetail());
            ps.setDate(6, Date.valueOf(c.issueDate()));
            ps.setString(7, c.filePath());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                keys.next();
                return keys.getInt(1);
            }
        }
    }

    private static void deleteQuietly(Path file) {
        if (file != null) {
            try { Files.deleteIfExists(file); } catch (IOException ignored) { }
        }
    }
}
