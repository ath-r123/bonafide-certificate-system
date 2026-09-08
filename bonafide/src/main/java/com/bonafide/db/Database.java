package com.bonafide.db;

import com.bonafide.exception.DataAccessException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Loads credentials from db.properties (working directory first, then classpath)
 * and hands out connections. Credentials are never hard-coded in source.
 */
public final class Database {

    private static final String FILE = "db.properties";
    private static final String url;
    private static final String user;
    private static final String password;

    static {
        Properties p = new Properties();
        try {
            Path local = Path.of(FILE);
            if (Files.exists(local)) {
                try (InputStream in = Files.newInputStream(local)) { p.load(in); }
            } else {
                try (InputStream in = Database.class.getResourceAsStream("/" + FILE)) {
                    if (in == null) throw new IOException(FILE + " not found in working directory or on classpath");
                    p.load(in);
                }
            }
        } catch (IOException e) {
            throw new DataAccessException("Unable to read " + FILE, e);
        }
        url = p.getProperty("db.url");
        user = p.getProperty("db.user");
        password = p.getProperty("db.password", "");
        if (url == null || user == null) throw new DataAccessException("db.url and db.user must be set in " + FILE, null);
    }

    private Database() { }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    /** Applies the one-time naming update for databases created by earlier releases. */
    public static void applyBrandingUpdates() {
        String updateInstitute = """
                UPDATE institute SET name = 'Apex Engineering College'
                WHERE institute_id = 1 AND name = 'Sinhgad College of Engineering'""";
        String updateCourses = """
                UPDATE course SET course_name = CASE course_name
                    WHEN 'B.E. Computer Engineering' THEN 'B.Tech Computer Engineering'
                    WHEN 'M.E. Computer Engineering' THEN 'M.Tech Computer Engineering'
                    WHEN 'B.E. Information Technology' THEN 'B.Tech Information Technology'
                    WHEN 'B.E. Electronics and Telecommunication' THEN 'B.Tech Electronics and Telecommunication'
                    WHEN 'B.E. Mechanical Engineering' THEN 'B.Tech Mechanical Engineering'
                END
                WHERE course_name IN (
                    'B.E. Computer Engineering', 'M.E. Computer Engineering',
                    'B.E. Information Technology', 'B.E. Electronics and Telecommunication',
                    'B.E. Mechanical Engineering')""";
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate(updateInstitute);
            statement.executeUpdate(updateCourses);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to apply college naming updates: " + e.getMessage(), e);
        }
    }
}
