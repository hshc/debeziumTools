package fr.hshc.db.tools.dbtranslator;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;

public class DateSnow {

	public DateSnow() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) {
		long timestamp = 1733731510357L; // Timestamp in milliseconds
		Date date = new Date(timestamp); // Convert to Date object

		System.out.println("Date: " + date);
		
        
        // Convert the timestamp to an Instant (which represents a point in time)
        Instant instant = Instant.ofEpochMilli(timestamp);
        
        // Convert to ZonedDateTime to display in a specific time zone (e.g., UTC)
        ZonedDateTime dateTime = instant.atZone(ZoneId.of("UTC"));
        
        System.out.println("Date: " + dateTime);
	}

}
