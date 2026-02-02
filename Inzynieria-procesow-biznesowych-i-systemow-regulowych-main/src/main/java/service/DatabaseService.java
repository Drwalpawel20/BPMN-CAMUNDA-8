package service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

@Service
public class DatabaseService {

    private final String dbUrl;
    private final String dbUser;
    private final String dbPassword;

    public DatabaseService(
            @Value("${spring.datasource.url}") String dbUrl,
            @Value("${spring.datasource.username}") String dbUser,
            @Value("${spring.datasource.password}") String dbPassword
    ) {
        this.dbUrl = dbUrl;
        this.dbUser = dbUser;
        this.dbPassword = dbPassword;
    }

    public Connection getConnection() throws Exception {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    /**
     * Sprawdza ważność pracownika po imieniu i nazwisku (true/false)
     */
    public boolean getEmployeeIdValidity(String firstName, String lastName, String employee_id) {
        boolean isValid = false;
        String sql = "SELECT employee_id_valid FROM parking.staff WHERE first_name = ? AND last_name = ? AND employee_id = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            stmt.setString(3, employee_id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    isValid = rs.getBoolean("employee_id_valid");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return isValid;
    }

    public Map<String, Object> getStudentStatus(String firstName, String lastName, String indexNumber) {
        Map<String, Object> result = new HashMap<>();
        result.put("is_student", false);
        result.put("student_id_valid", false);
        result.put("is_disabled", false);

        String sql = "SELECT student_id_valid, is_disabled FROM parking.students " +
                "WHERE first_name = ? AND last_name = ? AND index_number = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, firstName);
            stmt.setString(2, lastName);
            stmt.setString(3, indexNumber);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    result.put("is_student", true);
                    result.put("student_id_valid", rs.getBoolean("student_id_valid"));
                    result.put("is_disabled", rs.getBoolean("is_disabled"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public int getFreeSpots(String category) {
        int freeSpots = -1; // -1 = błąd lub brak kategorii

        String sql = "SELECT SUM(free_spots) AS free FROM parking.parking_spots WHERE category = ?";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, category);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    freeSpots = rs.getInt("free");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return freeSpots;
    }

    public int assignParkingSpot(String category) {
        int remainingFreeSpots = 0;

        String selectSql = "SELECT free_spots FROM parking.parking_spots WHERE category = ?";
        String updateSql = "UPDATE parking.parking_spots SET free_spots = free_spots - 1 WHERE category = ? AND free_spots > 0";

        try (Connection conn = getConnection()) {

            // 1. Sprawdzenie aktualnej liczby wolnych miejsc
            try (PreparedStatement selectStmt = conn.prepareStatement(selectSql)) {
                selectStmt.setString(1, category);
                try (ResultSet rs = selectStmt.executeQuery()) {
                    if (rs.next()) {
                        remainingFreeSpots = rs.getInt("free_spots");
                    }
                }
            }

            // 2. Jeśli są wolne miejsca, zmniejszamy o 1
            if (remainingFreeSpots > 0) {
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, category);
                    int updated = updateStmt.executeUpdate();
                    if (updated > 0) {
                        remainingFreeSpots--;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return remainingFreeSpots;
    }
}
