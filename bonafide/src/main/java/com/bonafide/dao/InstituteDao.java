package com.bonafide.dao;

import com.bonafide.db.Database;
import com.bonafide.exception.DataAccessException;
import com.bonafide.model.Institute;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class InstituteDao {

    private static final String SELECT =
            "SELECT name, address, principal_name, place FROM institute WHERE institute_id = 1";

    /** Read once at startup; the institute row never changes during a session. */
    public Institute find() {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw new DataAccessException("Institute row is missing. Run schema.sql first.", null);
            return new Institute(rs.getString("name"), rs.getString("address"),
                    rs.getString("principal_name"), rs.getString("place"));
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load institute details: " + e.getMessage(), e);
        }
    }
}
