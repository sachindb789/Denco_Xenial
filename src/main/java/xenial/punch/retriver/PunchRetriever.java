package xenial.punch.retriver;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import xenial.punch.DBConnection;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static xenial.punch.retriver.TableCreator.createCorporatePunchDetailsTableIfNotExists;
import static xenial.punch.retriver.TableCreator.createPunchDetailsTableIfNotExists;

public class PunchRetriever {
    private static final Logger logger = LoggerFactory.getLogger(PunchRetriever.class);
    private static final String PUNCH_API_URL = "https://backoffice-api.xenial.com/Staff/EmployeeWorkTime/GetBy?fieldName=BusinessDate&value=";
    private static final String EMPLOYEE_API_URL_TEMPLATE = "https://backoffice-api.xenial.com/Staff/Employee/EmployeeDetail/?EmployeeId=";
    private static final String COMPANY_ID = "5ddab0e3d93109001df6a76c";
//    private static final String[] SITE_IDS = {"64542ad8aacd127443be6b7f", "620aca8d6f04f10008176eef"};
    private static final String[] SITE_IDS = {"64542ad8aacd127443be6b7f"};
    private static final String AUTH_TOKEN = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6InhwcnQifQ.eyJpbnRlZ3JhdG9yX2lkIjoiNjMxZjhkMzNiM2MxNThlN2U5OGE5OTdjIiwic3ViIjoiaHR0cHM6Ly94cHJ0YmFja2VuZC54ZW5pYWwuY29tL2ludGVncmF0b3IvYmEyMzU2ZjQtODc0Ny00ZjRkLTkwY2EtMDY0ZmEyMmFjNDJkIiwiaW50ZWdyYXRvcl9raWQiOiJiYTIzNTZmNC04NzQ3LTRmNGQtOTBjYS0wNjRmYTIyYWM0MmQiLCJjb21wYW55X2lkIjoiNWRkYWIwZTNkOTMxMDkwMDFkZjZhNzZjIiwiYXVkIjpbIlhQUlQiLCJYUE9TIiwiWE1PIiwiWEJPRiIsIlhETSIsIlhLUyIsIlJQVCIsIkhCVCIsIlhDQyIsIlhQSVAiLCJ4dmkiLCJYRUEiLCJYUFJMIl0sInRva2VuX3R5cGUiOiJpbnRlZ3JhdG9yIiwiaWF0IjoxNzI0NzU4MjQxLCJleHAiOjE3Mjg2NDYyNDEsImlzcyI6InhwcnRiYWNrZW5kLnhlbmlhbC5jb20ifQ.M_2yuMUe7O8cwots-QGEYShzVAKTVNp28vUUlBSpyxI";

    public static void main(String[] args) {
        try {
            processAllSites();
        } catch (IOException e) {
            logger.error("Error retrieving punch data.", e);
        }
    }
    public static String getDynamicPunchApiUrl() {
        LocalDate currentDate = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedDate = currentDate.format(formatter);
//        String dynamicPunchApiUrl = PUNCH_API_URL + formattedDate;
        String dynamicPunchApiUrl = PUNCH_API_URL + "2024-09-26";
        System.out.println(dynamicPunchApiUrl);
        return dynamicPunchApiUrl;
    }
    public static String getClockTypeName(String punchTypeId) {
        String selectSQL = "SELECT Name FROM xenial_punch_types WHERE PunchTypeId = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(selectSQL)) {

            preparedStatement.setString(1, punchTypeId);
            ResultSet resultSet = preparedStatement.executeQuery();

            // Return the punch type name if found, otherwise return null
            if (resultSet.next()) {
                return resultSet.getString("Name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // Return null if no match or in case of error
    }
    public static String getPunches(String siteId) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(getDynamicPunchApiUrl());
            request.setHeader("x-company-id", COMPANY_ID);
            request.setHeader("x-site-ids", siteId);  // Use the current siteId
            request.setHeader("Authorization", AUTH_TOKEN);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity) : null;
            }
        }
    }
    public static String getEmployeeDetails(String employeeId) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String apiUrl = EMPLOYEE_API_URL_TEMPLATE + employeeId;
            HttpGet request = new HttpGet(apiUrl);
            request.setHeader("Authorization", AUTH_TOKEN);
            request.setHeader("x-company-id", COMPANY_ID);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity) : null;
            }
        }
    }
    public static void processAllSites() throws IOException {
        for (String siteId : SITE_IDS) {
            logger.info("Fetching punch data for site: {}", siteId);
            String punchesJson = getPunches(siteId);
            System.out.println(punchesJson);
            if (punchesJson != null) {
                insertPunchDataIntoDB(punchesJson);
            }
        }
    }

    public static void insertPunchDataIntoDB(String punchesJson) {
        String insertSQL = "INSERT INTO xenial_punch_details (EmployeeWorkTimeId, EmployeeId, BusinessDate, ClockIn, ClockInTypeId, ClockInTypeName, ClockOut, ClockOutTypeId, ClockOutTypeName, IsActive, SiteId, EmployeeCorporateCode, Firstname, Lastname, jobCodeId, is_punch_send) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        String checkDuplicateSQL = "SELECT id, EmployeeCorporateCode, ClockOut FROM xenial_punch_details WHERE EmployeeWorkTimeId = ? AND BusinessDate = ?";

        String updateSQL = "UPDATE xenial_punch_details SET ClockOut = ?, ClockOutTypeId = ?, ClockOutTypeName = ? WHERE EmployeeWorkTimeId = ? AND BusinessDate = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement insertStatement = connection.prepareStatement(insertSQL, Statement.RETURN_GENERATED_KEYS);
             PreparedStatement checkDuplicateStatement = connection.prepareStatement(checkDuplicateSQL);
             PreparedStatement updateStatement = connection.prepareStatement(updateSQL)) {

            // Create the table if it does not exist
            createPunchDetailsTableIfNotExists(connection);
            createCorporatePunchDetailsTableIfNotExists(connection);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(punchesJson);
            JsonNode dataArray = rootNode.path("Data");

            for (JsonNode dataNode : dataArray) {
                String employeeWorkTimeId = dataNode.path("EmployeeWorkTimeId").asText();
                String employeeId = dataNode.path("EmployeeId").asText();
                String businessDate = dataNode.path("BusinessDate").asText();
                String clockIn = dataNode.path("ClockIn").asText();
                String clockInTypeId = dataNode.path("ClockInTypeId").asText();
                String clockInTypeName = getClockTypeName(clockInTypeId);
                String clockOut = dataNode.path("ClockOut").asText();
                String clockOutTypeId = dataNode.path("ClockOutTypeId").asText();
                String clockOutTypeName = getClockTypeName(clockOutTypeId);
                String isActive = dataNode.path("IsActive").asText();
                String siteId = dataNode.path("SiteId").asText();

                // Check if the record exists
                checkDuplicateStatement.setString(1, employeeWorkTimeId);
                checkDuplicateStatement.setString(2, businessDate);

                try (ResultSet resultSet = checkDuplicateStatement.executeQuery()) {
                    if (resultSet.next()) {
                        String dbClockOut = resultSet.getString("ClockOut");
                        String employeeCorporateCode = resultSet.getString("EmployeeCorporateCode");
                        int xenialPunchId = resultSet.getInt("id");

                        // If ClockOut is null in the database but present in the JSON, update the record
                        if ((dbClockOut == null || "null".equalsIgnoreCase(dbClockOut) || dbClockOut.trim().isEmpty())
                                && clockOut != null && !"null".equalsIgnoreCase(clockOut) && !clockOut.trim().isEmpty()) {
                            updateStatement.setString(1, clockOut);
                            updateStatement.setString(2, clockOutTypeId);
                            updateStatement.setString(3, clockOutTypeName);
                            updateStatement.setString(4, employeeWorkTimeId);
                            updateStatement.setString(5, businessDate);

                            int rowsUpdated = updateStatement.executeUpdate();
                            if (rowsUpdated > 0) {
                                // Insert into corporate punch details after the update
                                insertCorporatePunchDetail(connection, xenialPunchId, clockOutTypeName, clockOut, employeeCorporateCode);
                                logger.info("Record updated for EmployeeWorkTimeId: {} and BusinessDate: {}", employeeWorkTimeId, businessDate);
                            }
                        } else {
                            logger.info("Skipping duplicate punch for EmployeeWorkTimeId: {} and BusinessDate: {}", employeeWorkTimeId, businessDate);
                            continue;  // Skip inserting this record if it's a duplicate
                        }
                    } else {
                        // Fetch employee details and insert new record if no duplicate is found
                        String employeeJson = getEmployeeDetails(employeeId);
                        JsonNode employeeNode = objectMapper.readTree(employeeJson);

                        String employeeCorporateCode = employeeNode.path("EmployeeCorporateCode").asText();
                        String firstname = employeeNode.path("Firstname").asText();
                        String lastname = employeeNode.path("Lastname").asText();

                        // Extract JobCodeId from EmployeeJobs array
                        JsonNode employeeJobs = employeeNode.path("EmployeeJobs");
                        String jobCodeId = null;

                        // Ensure EmployeeJobs array is not empty and extract JobCodeId form the employee Json response
                        if (employeeJobs.isArray() && employeeJobs.size() > 0) {
                            JsonNode primaryJob = employeeJobs.get(0); // Assuming you want the first job
                            jobCodeId = primaryJob.path("JobCodeId").asText();
                        }

                        // Prepare the statement for insertion
                        insertStatement.setString(1, employeeWorkTimeId);
                        insertStatement.setString(2, employeeId);
                        insertStatement.setString(3, businessDate);
                        insertStatement.setString(4, clockIn);
                        insertStatement.setString(5, clockInTypeId);
                        insertStatement.setString(6, clockInTypeName);
                        insertStatement.setString(7, clockOut);
                        insertStatement.setString(8, clockOutTypeId);
                        insertStatement.setString(9, clockOutTypeName);
                        insertStatement.setBoolean(10, Boolean.parseBoolean(isActive));
                        insertStatement.setString(11, siteId);
                        insertStatement.setString(12, employeeCorporateCode);
                        insertStatement.setString(13, firstname);
                        insertStatement.setString(14, lastname);
                        insertStatement.setString(15, jobCodeId);
                        insertStatement.setNull(16, java.sql.Types.INTEGER);  // is_punch_send default

                        int affectedRows = insertStatement.executeUpdate();

                        // Get the generated keys (last inserted id)
                        if (affectedRows > 0) {
                            try (ResultSet generatedKeys = insertStatement.getGeneratedKeys()) {
                                if (generatedKeys.next()) {
                                    int xenialPunchId = generatedKeys.getInt(1);
                                    logger.info("Inserted punch with ID: {}", xenialPunchId);

                                    // Insert into corporate punch details if needed
                                    if (clockIn != null && !clockIn.isEmpty()) {
                                        insertCorporatePunchDetail(connection, xenialPunchId, clockInTypeName, clockIn, employeeCorporateCode);
                                    }
                                    if (clockOut != null && !clockOut.isEmpty()) {
                                        insertCorporatePunchDetail(connection, xenialPunchId, clockOutTypeName, clockOut, employeeCorporateCode);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            logger.info("Payroll and employee data inserted successfully.");

        } catch (SQLException | IOException e) {
            logger.error("Error inserting payroll and employee data into the database.", e);
        }
    }
    // Method to insert data into xenial_corporate_punch_details
    public static void insertCorporatePunchDetail(Connection connection, int xenialPunchId, String punchType, String punchTime, String employeeCorporateCode) throws SQLException {
        // Create the table if it does not exist
        createCorporatePunchDetailsTableIfNotExists(connection);

        String insertCorporateSQL = "INSERT INTO xenial_corporate_punch_details (xenial_punch_id, PunchType, PunchTime, eecode) " +
                "VALUES (?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(insertCorporateSQL)) {
            statement.setInt(1, xenialPunchId);
            statement.setString(2, punchType);
            statement.setString(3, punchTime);
            statement.setString(4, employeeCorporateCode);
            statement.executeUpdate();
        } catch (SQLException e) {
            logger.error("Error inserting corporate punch detail: {}", e.getMessage());
            throw e; // Re-throw the exception after logging
        }
    }
}