
package xenial.punch.PaycomePunchSender;

import org.json.JSONException;
import xenial.punch.DBConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class XenialPunchDataSender {
    public static long convertPunchTimeToUnix(String punchTime) throws Exception {
        // Parse the input time string
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        inputFormat.setTimeZone(TimeZone.getTimeZone("PST"));
        Date parsedDateTime = inputFormat.parse(punchTime);

        // Define the output format
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm a");
        outputFormat.setTimeZone(TimeZone.getTimeZone("PST"));

        // Format the date
        String formattedDate = outputFormat.format(parsedDateTime);

        // Get the Unix timestamp in seconds
        long unixTimestamp = parsedDateTime.getTime() / 1000; // Convert milliseconds to seconds

        return unixTimestamp; // Return the Unix timestamp
    }

    // Method to fetch data from xenial_corporate_punch_details and EmployeeJobId from xenial_punch_details
    public static void fetchAndSendPunchDetails() {
        String query = "SELECT cpd.*, pd.JobCodeId " +
                "FROM xenial_corporate_punch_details cpd " +
                "LEFT JOIN xenial_punch_details pd ON cpd.xenial_punch_id = pd.id " +
                "WHERE cpd.PunchId IS NULL"; // Select records where PunchId is NULL

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            if (!resultSet.isBeforeFirst()) { // No rows in the ResultSet
                System.out.println("No punch for now.");
                return; // Exit the method early
            }

            while (resultSet.next()) {
                int corporatePunchId = resultSet.getInt("id");
                String eecode = resultSet.getString("eecode");
                String punchTime = resultSet.getString("PunchTime");
                String punchType = resultSet.getString("PunchType");
                String xenialPunchId = resultSet.getString("xenial_punch_id");
                String xenialJobCodeId = resultSet.getString("JobCodeId");
                System.out.println(xenialJobCodeId);
                String JobCodeId = String.valueOf(getMappedJobCode(xenialJobCodeId));
                // Skip if punchTime is null or "null" (as a string)
                if (punchTime == null || "NULL".equalsIgnoreCase(punchTime.trim())) {
                    System.out.println("Skipping record with Corporate Punch ID: " + eecode + " due to null punchTime.");
                    continue;
                }
                Long unixPunchTime = convertPunchTimeToUnix(punchTime);

                // Create JSON data to send to the API
                JSONArray jsonDataArray = createJsonDataArray(eecode, unixPunchTime, punchType, JobCodeId);
                // Print JSON data for debugging
                System.out.println(jsonDataArray.toString(4));

                // Send data to API and capture the response
                String response = sendDataToApi(jsonDataArray.toString());

                // Print response for debugging
                System.out.println("Received API response: " + response);

                // Update the response and punchId in the database
                updateDatabaseWithResponse(connection, corporatePunchId, response);

                // Print update success message
                System.out.println("Database updated successfully for Corporate Punch ID: " + corporatePunchId);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    // Method to map jobCodeId with provided values
    public static String getMappedJobCode(String jobCodeId) {
        // Create a map of jobCodeId and its corresponding value
        Map<String, String> jobCodeMap = new HashMap<>();
        jobCodeMap.put("58e63c06fbf17de5ced22074", "0530");
        jobCodeMap.put("6092a3608b59ab0034619253", "0533");
        jobCodeMap.put("609aae81437da80034034366", "0530");
        jobCodeMap.put("6092a43f33e5e700340bfb00", "0530");
        jobCodeMap.put("6092a4bd0e7f0000350b370f", "0530");
        jobCodeMap.put("6092a2a9e480d100349c3f35", "0506");
        jobCodeMap.put("60801b66fd0ed700359d15ff", "0575");
        jobCodeMap.put("58e63c06fbf17de5ced22064", "0585");
        jobCodeMap.put("60801b8bdea3e70034684ab9", "0540");
        jobCodeMap.put("607dd5e8caf4ee003496de11", "0535");
        jobCodeMap.put("6092a66c5624480034a1c8c1", "0540");
        jobCodeMap.put("6001e52c0d0d87003fbbab34", "0540");
        jobCodeMap.put("61f752bf45eb9500405d6cd3", "0540");
        jobCodeMap.put("607dd6e5bd73220034f82bbd", "0545");
        jobCodeMap.put("609ab1c8b6613e00342ea85c", "0545");
        jobCodeMap.put("6092a486f4fa0c0034a94868", "0542");
        jobCodeMap.put("609aaf1bdac63e00342957bd", "0506");
        jobCodeMap.put("6092a5d948721500347be686", "0515");
        jobCodeMap.put("60801c30a737ef0034937d68", "0515");
        jobCodeMap.put("6092a61d860ef90034786d10", "0510");
        jobCodeMap.put("6092a56b2cedea0034aced0a", "0506");
        jobCodeMap.put("607dd5857d88ef0034c5e4e9", "0500");
        jobCodeMap.put("6092a511860ef90034786c2e", "0500");
        jobCodeMap.put("6092a539a737ef0034a367dd", "0506");
        jobCodeMap.put("58e63c06fbf17de5ced22014", "0506");
        jobCodeMap.put("649472ef7c2a9c74947e1bca", "0506");
        jobCodeMap.put("6081b53bd84efe003494358c", "0533");

        // Return mapped value or default "0540" if not found
        return jobCodeMap.getOrDefault(jobCodeId, "0540");
    }
    private static Map<String, String> punchTypeMap = new HashMap<>();
    static {
        punchTypeMap.put("Clock-in", "ID");
        punchTypeMap.put("Clock-out", "OD");
        punchTypeMap.put("Break Clock-out", "OB");
        punchTypeMap.put("Break Clock-in", "IB");
        punchTypeMap.put("End of day Clock-in-in", "ID");
        punchTypeMap.put("End of day Clock-out", "OD");
    }
    // Method to create JSON data for API request
    private static JSONArray createJsonDataArray(String eecode, Long unixPunchTime, String punchType, String JobCodeId) {
        JSONArray jsonArray = new JSONArray();
        JSONObject jsonObject = new JSONObject();
        String mappedPunchType = punchTypeMap.getOrDefault(punchType, null);

        jsonObject.put("clocktype", "K");
        jsonObject.put("entrytype", "1");
        jsonObject.put("punchtype", mappedPunchType); // Use mapped value from punchTypeMap
//        jsonObject.put("eecode", eecode);
        jsonObject.put("eecode", "A5RA");
        jsonObject.put("timezone", "PST");
        jsonObject.put("punchtime", unixPunchTime.toString());
        jsonObject.put("deptcode", JobCodeId);
        jsonObject.put("punchdesc", "API user");
        jsonObject.put("taxprofid", "0");

        jsonArray.put(jsonObject); // Add the JSON object to the JSON array
        return jsonArray;
    }

    // Method to send data to the API and return the response
    private static String sendDataToApi(String jsonData) {
        String apiUrl = "https://api.paycomonline.net/v4/rest/index.php/api/v1.1/punchimport";
        StringBuilder response = new StringBuilder();

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Basic ZTk4MjJmNzJiMjE2M2FhM2QxYmU1MzIxYTk0OTEzYTc4MmQxOGNkMjU0YzVjNTQ3Njg2M2UwMTVlOTkwMzY1ZTphY2E3NDA1ODg3ZGMyODM0YTU5MDJiN2I2NDc2MWZlYmNiZjNmMDBhMDE4ZWNmYmQyNDkxOTAwNTVmZWFlNmJh");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Send JSON data
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonData.getBytes("utf-8"));
            }
            // Check response code
            int responseCode = connection.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            // Read response based on the response code
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(responseCode >= 200 && responseCode < 300 ? connection.getInputStream() : connection.getErrorStream(), "utf-8"))) {
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response.toString(); // Return the response
    }
    // Method to update the database with the response from the API
    private static void updateDatabaseWithResponse(Connection connection, int corporatePunchId, String response) {
        String updateQuery = "UPDATE xenial_corporate_punch_details SET response = ?, punchId = ? WHERE id = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
            // Parse the JSON response
            JSONObject jsonResponse = new JSONObject(response);

            // Check if result is true (success case)
            if (jsonResponse.getBoolean("result")) {
                JSONArray dataArray = jsonResponse.getJSONArray("data");
                if (dataArray.length() > 0) {
                    // Get the first record
                    JSONObject firstRecord = dataArray.getJSONObject(0);

                    // Handle punchId as either a String or a Number
                    Object punchIdObject = firstRecord.get("punchId");
                    String punchId;
                    if (punchIdObject instanceof Integer || punchIdObject instanceof Long) {
                        punchId = String.valueOf(punchIdObject);  // Convert number to string
                    } else {
                        punchId = punchIdObject.toString();  // Handle as a string
                    }
                    // Set response as "success" and punchId
                    preparedStatement.setString(1, "success");
                    preparedStatement.setString(2, punchId);
                    preparedStatement.setInt(3, corporatePunchId);
                }
            } else {
                // Error case: capture the error message
                JSONArray errorArray = jsonResponse.getJSONArray("errors");
                if (errorArray.length() > 0) {
                    // Get the first error message
                    JSONObject errorObject = errorArray.getJSONObject(0);
                    JSONArray errorMessages = errorObject.getJSONArray("errors");
                    String errorMessage = errorMessages.getString(0);

                    // Set the response with the error message and punchId as NULL
                    preparedStatement.setString(1, errorMessage);
                    preparedStatement.setNull(2, java.sql.Types.VARCHAR);
                    preparedStatement.setInt(3, corporatePunchId);
                }
            }
            // Execute the update
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            // Handle potential JSON parsing issues
            e.printStackTrace();
        }
    }
    public static void main(String[] args) {

        fetchAndSendPunchDetails();
    }
}
