package xenial.punch.retriver;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
public class TableCreator {
    public static void createPunchDetailsTableIfNotExists(Connection connection) throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS xenial_punch_details (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "EmployeeWorkTimeId VARCHAR(255), " +  // From JSON
                "EmployeeId VARCHAR(255), " +          // From JSON
//                "EmployeeJobId VARCHAR(255), " +       // From JSON
                "BusinessDate DATE, " +                // From JSON
                "ClockIn VARCHAR(255), " +           // From JSON
                "ClockInTypeId VARCHAR(255), " +       // From JSON
                "ClockInTypeName VARCHAR(255), " +     // Additional column
                "ClockOut VARCHAR(255), " +            // From JSON (nullable, changed to VARCHAR)
                "ClockOutTypeId VARCHAR(255), " +      // From JSON
                "ClockOutTypeName VARCHAR(255), " +     // Additional column
                // Additional columns
                "EmployeeCorporateCode VARCHAR(255), " +
//                "Gender VARCHAR(10), " +
                "Firstname VARCHAR(255), " +
                "Lastname VARCHAR(255), " +
                "JobCodeId VARCHAR(255), " +
//                "WorkTypeId VARCHAR(255), " +          // From JSON
//                "ShiftId VARCHAR(255), " +             // From JSON
                "IsActive BOOLEAN, " +                 // From JSON
                "SiteId VARCHAR(255), " +              // From JSON
//                "BreakTimeId VARCHAR(255), " +         // From JSON (nullable)
//                "Source VARCHAR(255), " +              // From JSON (nullable)
//                "ActualUpdateDate VARCHAR(255), " +    // From JSON (nullable, changed to VARCHAR)
                "is_punch_send INT DEFAULT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "+
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP)";
        try (Statement statement = connection.createStatement()) {
            statement.execute(createTableSQL);
        }
    }
    public static void createCorporatePunchDetailsTableIfNotExists(Connection connection) throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS xenial_corporate_punch_details ( " +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "xenial_punch_id INT NOT NULL, " +
                "eecode VARCHAR(255) NOT NULL, " +
                "PunchTime VARCHAR(255) NOT NULL, " +
                "PunchType VARCHAR(255) NOT NULL, " +
                "PunchId VARCHAR(255) NULL, " +
                "Response VARCHAR(255) NULL, " +
                "FOREIGN KEY (xenial_punch_id) REFERENCES xenial_punch_details(id) ON DELETE CASCADE " +
                ")";

        try (Statement statement = connection.createStatement()) {
            statement.execute(createTableSQL);
        }
    }
}
