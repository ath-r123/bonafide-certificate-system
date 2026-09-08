package com.bonafide.dao;

import com.bonafide.db.Database;
import com.bonafide.exception.DataAccessException;
import com.bonafide.model.Course;
import com.bonafide.model.Department;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CourseDao {

    private static final String SELECT_DEPARTMENTS =
            "SELECT dept_id, dept_name FROM department ORDER BY dept_name";

    private static final String SELECT_COURSES_BY_DEPT = """
            SELECT c.course_id, c.course_name, c.duration_years, d.dept_id, d.dept_name
            FROM course c JOIN department d ON c.dept_id = d.dept_id
            WHERE d.dept_id = ? ORDER BY c.course_name""";

    public List<Department> listDepartments() {
        List<Department> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_DEPARTMENTS);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(new Department(rs.getInt("dept_id"), rs.getString("dept_name")));
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load departments: " + e.getMessage(), e);
        }
    }

    public List<Course> listCourses(int deptId) {
        List<Course> list = new ArrayList<>();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_COURSES_BY_DEPT)) {
            ps.setInt(1, deptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapCourse(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to load courses: " + e.getMessage(), e);
        }
    }

    /** Shared by StudentDao, which selects the same course/department columns. */
    static Course mapCourse(ResultSet rs) throws SQLException {
        Department dept = new Department(rs.getInt("dept_id"), rs.getString("dept_name"));
        return new Course(rs.getInt("course_id"), rs.getString("course_name"), rs.getInt("duration_years"), dept);
    }
}
