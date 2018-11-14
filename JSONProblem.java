import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader; 

public class JSONProblem {
	public static void main(String[] args) {		
		queries("users-1.json", 1000);
		queries("users-2.json", 1000);
	}
	
	/*
	 * Runs queries on JSON data in file at the given filepath. Prints summary of every
	 * x records, where x is given by numRecords.
	 */
	private static void queries(String filepath, int numRecords) {		
		JsonArray jArray = jsonAsArray(filepath);		
		if (jArray != null) {
			// variables tracking information for each set of numRecords records
			Map<Integer, Integer> yearCount = new TreeMap<Integer, Integer>();
			int[] numFriends = new int[numRecords];
			int[] activeFemaleAge = new int[numRecords];
			int totalBalanceCents = 0;
			int totalUnreadMessages = 0;
			// iterates over each object in jArray
			for (int i = 0; i < jArray.size(); i++) {
				// gets the JSON object at the ith position in the jArray
				JsonElement objectAsElement = jArray.get(i);
				JsonObject obj = objectAsElement.getAsJsonObject();	
				
				// updates the tracking variables declared above with information from user data
				// in the current JSON object
				addYearRegistration(yearCount, 
						getRegistrationYear(obj.get("registered").toString()));	
				numFriends[i % numRecords] = obj.getAsJsonArray("friends").size();
				activeFemaleAge[i % numRecords] = Integer.parseInt(obj.get("age").toString());
				totalBalanceCents += removeNonDigits(obj.get("balance").toString());
				if (obj.get("gender").toString().replace("\"", "").equals("female") && 
						Boolean.parseBoolean((obj.get("isActive").toString())))
					totalUnreadMessages += removeNonDigits(obj.get("greeting").toString());
				
				// when we are on the last record of the records we want to include in the same
				// summary
				if (i % numRecords == numRecords - 1) {
					// formats totalBalanceCents into the average balance in dollars
					NumberFormat nf = NumberFormat.getCurrencyInstance();
					double averageBalanceDollars = 
							Math.floor((totalBalanceCents / (numRecords * 100.0)) * 100) / 100.0;
					String formattedAverageBalance = nf.format(averageBalanceDollars);
					// print the summary of the records
					printSummary(yearCount, getMedianValue(numFriends), getMedianValue(activeFemaleAge), 
							formattedAverageBalance, totalUnreadMessages, numRecords, i - numRecords + 2, 
							filepath.substring(4));
					// reset all tracking variables for the next set of records we want to summarize
					yearCount = new TreeMap<Integer, Integer>();
					numFriends = new int[numRecords];
					activeFemaleAge = new int[numRecords];
					totalBalanceCents = 0;
					totalUnreadMessages = 0;					
				}
			}
		} else {
			throw new IllegalStateException("Array of JSON objects is null");
		}
	}
	
	/*
	 * Prints a summary of user data over a certain number of records. yearCount is a map that
	 * maps years to the number of people who registered in that year, medianNumFriends is the
	 * median number of friends in the records being summarized, medianAge is the median user
	 * age in the records being summarized, averageBalance is the average balance of all users
	 * in the records being summarized, totalUnreadMessages is the total number of 
	 * unread messages of all users in the records being summarized, numRecords is the number
	 * of records being summarized, startRecordNum is the record number of the first record
	 * of the records being summarized in the file containing the records, and fileName is the
	 * name of the file containing the records being summarized.
	 */
	private static void printSummary(Map<Integer, Integer> yearCount, double medianNumFriends, 
			double medianAge, String averageBalance, int totalUnreadMessages, int numRecords,
			int startRecordNum, String fileName) {
		String summary = "Summary for records " + startRecordNum + "-" + 
				(startRecordNum + numRecords - 1) + " of file " + fileName + 
				":\nUsers Registered Each Year: " + registrationsPerYear(yearCount) + 
				"\nMedian Number of Friends: " + medianNumFriends + "\nMedian User Age: " + 
				medianAge + "\nAverage Balance: " + averageBalance + 
				"\nAverage Unread Messages for Active Females: " + 
				1.0 * totalUnreadMessages / numRecords + "\n";
		System.out.println(summary);
	}
	
	/*
	 * Returns a String that holds the keys and values in the given Map yearCount in the form of 
	 * [year: value, year: value,..., year: value].
	 */
	private static String registrationsPerYear(Map<Integer, Integer> yearCount) {
		String retVal = "[";
		Iterator<Integer> itr = yearCount.keySet().iterator();
		while (itr.hasNext()) {
			int year = (int) itr.next();
			retVal += year + ": " + yearCount.get(year);
			if (itr.hasNext()) {
				retVal += ", ";
			}
		}
		retVal += "]";
		return retVal;
	}
	
	/*
	 * Adds 1 to the number of registrations for the given year in the given Map yearCount, where
	 * the keys are years and the values are the number of registrations in that year.
	 */
	private static void addYearRegistration(Map<Integer, Integer> yearCount, int year) {
		if (yearCount.containsKey(year)) {
			yearCount.put(year, yearCount.get(year) + 1);
		} else {
			yearCount.put(year, 1);
		}
	}
	
	/*
	 * Assumes that the given registrationTime is the value of the "registered" key in the JSON
	 * Parses the given registrationTime and returns the registration year contained in it.
	 */
	private static int getRegistrationYear(String registrationTime) {
		return Integer.parseInt(registrationTime.substring(1, 5));
	}
	
	/*
	 * Returns the median value of the given int array arr.
	 */
	private static double getMedianValue(int[] arr) {
		int[] sorted = arr;
		Arrays.sort(sorted);
		int length = sorted.length;
		if (length % 2 == 1) {
			return sorted[length / 2];
		}
		return (sorted[length / 2] + sorted[(length / 2) - 1]) / 2.0;
	}
	
	/*
	 * Removes any non-digit characters in the given value and returns the remaining String as
	 * an int.
	 */
	private static int removeNonDigits(String value) {
		String retVal = value.replaceAll("[^\\d]", "");
		return Integer.parseInt(retVal);
	}
	
	/*
	 * Assumes that the given filepath is the path to a JSON file containing an array of JSON
	 * objects. Returns that array of JSON objects as a JsonArray.	 
	 */
	private static JsonArray jsonAsArray(String filepath) {
		JsonArray jArray = null;
		try {
			URL path = JSONProblem.class.getResource(filepath);
			File jsonFile = new File(path.getFile());
			InputStream jsonStream = new FileInputStream(jsonFile);
			JsonReader jReader = new JsonReader(new InputStreamReader(jsonStream));		
			JsonElement jElement = new JsonParser().parse(jReader);
			jArray = jElement.getAsJsonArray();			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return jArray;
	}
}
