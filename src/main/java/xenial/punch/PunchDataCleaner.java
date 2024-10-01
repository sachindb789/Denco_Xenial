package xenial.punch;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class PunchDataCleaner {

    // Method to delete punch records older than 3 days
    public static void deleteOldPunchRecords(Connection connection) throws SQLException {
        // Calculate the date three days ago
        LocalDate threeDaysAgo = LocalDate.now().minusDays(3);
        String dateToDeleteBefore = threeDaysAgo.toString(); // Format as YYYY-MM-DD

        // SQL to select IDs from xenial_punch_details that need to be deleted
        String selectPunchDetailsSQL = "SELECT id FROM xenial_punch_details WHERE BusinessDate < ?";
        try (PreparedStatement selectStatement = connection.prepareStatement(selectPunchDetailsSQL)) {
            selectStatement.setString(1, dateToDeleteBefore);

            try (ResultSet resultSet = selectStatement.executeQuery()) {
                // Collect IDs of records to delete
                StringBuilder idsToDelete = new StringBuilder();
                while (resultSet.next()) {
                    if (idsToDelete.length() > 0) {
                        idsToDelete.append(", ");
                    }
                    idsToDelete.append(resultSet.getLong("id"));
                }

                // If there are IDs to delete, delete from xenial_corporate_punch_details first
                if (idsToDelete.length() > 0) {
                    String deleteCorporatePunchDetailsSQL = "DELETE FROM xenial_corporate_punch_details WHERE xenial_punch_id IN (" + idsToDelete.toString() + ")";
                    try (PreparedStatement corporatePunchDetailsStatement = connection.prepareStatement(deleteCorporatePunchDetailsSQL)) {
                        int corporatePunchDetailsDeleted = corporatePunchDetailsStatement.executeUpdate();
                        System.out.println("Deleted " + corporatePunchDetailsDeleted + " records from xenial_corporate_punch_details.");
                    }

                    // Now delete from xenial_punch_details
                    String deletePunchDetailsSQL = "DELETE FROM xenial_punch_details WHERE BusinessDate < ?";
                    try (PreparedStatement punchDetailsStatement = connection.prepareStatement(deletePunchDetailsSQL)) {
                        punchDetailsStatement.setString(1, dateToDeleteBefore);
                        int punchDetailsDeleted = punchDetailsStatement.executeUpdate();
                        System.out.println("Deleted " + punchDetailsDeleted + " records from xenial_punch_details.");
                    }
                } else {
                    System.out.println("No records to delete from xenial_punch_details.");
                }
            }
        }
    }

    // Main method to execute the deletion
    public static void main(String[] args) {
        try (Connection connection = DBConnection.getConnection()) {
            deleteOldPunchRecords(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
