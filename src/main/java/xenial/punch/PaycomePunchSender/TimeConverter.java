package xenial.punch.PaycomePunchSender;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class TimeConverter {

    // Method to convert Unix timestamp to PST time
    public static String convertUnixToPST(long unixTimestamp) {
        try {
            // Convert the Unix timestamp (in milliseconds) to an Instant
            Instant instant = Instant.ofEpochMilli(unixTimestamp);

            // Convert the Instant to a ZonedDateTime in the PST time zone
            ZonedDateTime pstDateTime = instant.atZone(ZoneId.of("America/Los_Angeles"));

            // Format the ZonedDateTime to a readable string in ISO format
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");
            return pstDateTime.format(formatter); // Return formatted PST time
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        // Example Unix timestamp (in milliseconds)
        long unixTimestamp = 1726495486; // Corresponds to "2024-09-17T06:40:01-07:00" in PDT

        // Convert the Unix timestamp to PST
        String pstTime = convertUnixToPST(unixTimestamp);
        System.out.println("PST Time: " + pstTime);
    }
}


