package xenial.punch;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/ncr_db";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Str0ngP@ssw0rd!";

    // Private constructor to prevent instantiation
    private DBConnection() {
        throw new UnsupportedOperationException("Utility class");
    }

    // Method to establish a database connection
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}

