package com.bonafide.db;

import com.bonafide.exception.DataAccessException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
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
}
