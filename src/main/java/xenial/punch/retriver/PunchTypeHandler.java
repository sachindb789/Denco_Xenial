package xenial.punch.retriver;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import xenial.punch.DBConnection;

public class PunchTypeHandler {

    private static final String API_URL = "https://backoffice-api.xenial.com/Lookup/PunchType";
    private static final String API_KEY = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6InhwcnQifQ.eyJpbnRlZ3JhdG9yX2lkIjoiNjMxZjhkMzNiM2MxNThlN2U5OGE5OTdjIiwic3ViIjoiaHR0cHM6Ly94cHJ0YmFja2VuZC54ZW5pYWwuY29tL2ludGVncmF0b3IvYmEyMzU2ZjQtODc0Ny00ZjRkLTkwY2EtMDY0ZmEyMmFjNDJkIiwiaW50ZWdyYXRvcl9raWQiOiJiYTIzNTZmNC04NzQ3LTRmNGQtOTBjYS0wNjRmYTIyYWM0MmQiLCJjb21wYW55X2lkIjoiNWRkYWIwZTNkOTMxMDkwMDFkZjZhNzZjIiwiYXVkIjpbIlhQUlQiLCJYUE9TIiwiWE1PIiwiWEJPRiIsIlhETSIsIlhLUyIsIlJQVCIsIkhCVCIsIlhDQyIsIlhQSVAiLCJ4dmkiLCJYRUEiLCJYUFJMIl0sInRva2VuX3R5cGUiOiJpbnRlZ3JhdG9yIiwiaWF0IjoxNzI0NzU4MjQxLCJleHAiOjE3Mjg2NDYyNDEsImlzcyI6InhwcnRiYWNrZW5kLnhlbmlhbC5jb20ifQ.M_2yuMUe7O8cwots-QGEYShzVAKTVNp28vUUlBSpyxI";

    // Create the xenial_punch_types table if it does not exist
    public static void createPunchTypeTableIfNotExists(Connection connection) throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS xenial_punch_types (" +
                "PunchTypeId VARCHAR(255) PRIMARY KEY, " +
                "Name VARCHAR(255), " +
                "Description VARCHAR(255), " +
                "Code VARCHAR(50), " +
                "IsClockIn BOOLEAN, " +
                "SiteId VARCHAR(255) NULL, " +  // Allow SiteId to be NULL
                "IsActive BOOLEAN)";

        try (Statement statement = connection.createStatement()) {
            statement.execute(createTableSQL);
        }
    }

    // Fetch data from the API and insert it into the database
    public static void fetchAndInsertPunchTypes() {
        try {
            // Fetch data from the API
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("x-company-id", "5ddab0e3d93109001df6a76c");
            conn.setRequestProperty("Authorization", API_KEY);

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();

            // Parse JSON response
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response.toString());
            JsonNode dataArray = rootNode.path("Data");

            // Connect to database using DBConnection
            try (Connection connection = DBConnection.getConnection()) {
                // Create table if not exists
                createPunchTypeTableIfNotExists(connection);

                String insertSQL = "INSERT INTO xenial_punch_types (PunchTypeId, Name, Description, Code, IsClockIn, SiteId, IsActive) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement insertStatement = connection.prepareStatement(insertSQL)) {
                    for (JsonNode dataNode : dataArray) {
                        String punchTypeId = dataNode.path("PunchTypeId").asText();
                        String name = dataNode.path("Name").asText();
                        String description = dataNode.path("Description").asText();
                        String code = dataNode.path("Code").asText();
                        boolean isClockIn = dataNode.path("IsClockIn").asBoolean();
                        String siteId = dataNode.path("SiteId").asText();
                        boolean isActive = dataNode.path("IsActive").asBoolean();

                        insertStatement.setString(1, punchTypeId);
                        insertStatement.setString(2, name);
                        insertStatement.setString(3, description);
                        insertStatement.setString(4, code);
                        insertStatement.setBoolean(5, isClockIn);
                        if (siteId != null && !siteId.isEmpty()) {
                            insertStatement.setString(6, siteId);
                        } else {
                            insertStatement.setNull(6, java.sql.Types.VARCHAR);
                        }
                        insertStatement.setBoolean(7, isActive);

                        System.out.println(insertStatement);
                        insertStatement.addBatch();
                    }
                    insertStatement.executeBatch();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        fetchAndInsertPunchTypes();
        System.out.println("All records inserted successfully.");
    }
}
