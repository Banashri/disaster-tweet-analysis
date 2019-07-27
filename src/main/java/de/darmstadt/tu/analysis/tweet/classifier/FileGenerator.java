package de.darmstadt.tu.analysis.tweet.classifier;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import de.darmstadt.tu.analysis.tweet.objects.TweetMessage;
import de.darmstadt.tu.analysis.tweet.utils.TweetUtils;

public class FileGenerator {
	
	//CSV file header
	private static final String FILE_HEADER = "id,text,topic";

	//Delimiter used in CSV file
	private static final String COMMA_DELIMITER = ",";
	private static final String NEW_LINE_SEPARATOR = "\n";

	public static void main(String[] args) {
		run("data/input/"+"2012_Colorado_wildfires-tweets_labeled.csv", "formatted_Colorado_wildfires-tweets_labeled.csv", "wildfire");
		run("data/input/"+"2012_Philipinnes_floods-tweets_labeled.csv", "formatted_Philipinnes_floods-tweets_labeled.csv", "flood");
		run("data/input/"+"2013_Bohol_earthquake-tweets_labeled.csv", "formatted_Bohol_earthquake-tweets_labeled", "earthquake");
		run("data/input/"+"2013_Glasgow_helicopter_crash-tweets_labeled.csv", "formatted_Glasgow_helicopter_crash-tweets_labeled", "crash");
		run("data/input/"+"2013_NY_train_crash-tweets_labeled.csv", "formatted_NY_train_crash-tweets_labeled.csv", "derailment");
		run("data/input/"+"2012_Typhoon_Pablo-tweets_labeled.csv", "formatted_Typhoon_Pablo-tweets_labeled.csv", "typhoon");
		run("data/input/"+"2013_Boston_bombings-tweets_labeled.csv", "formatted_Boston_bombings-tweets_labeled.csv", "bombings");
		
		run("data/input/"+"2013_West_Texas_explosion-tweets_labeled.csv", "formatted_West_Texas_explosion-tweets_labeled.csv", "explosion");
		run("data/input/"+"2013_LA_airport_shootings-tweets_labeled.csv", "formatted_LA_airport_shootings-tweets_labeled.csv", "shooting");
		
		run("data/input/"+"2013_Australia_bushfire-tweets_labeled.csv", "formatted_Australia_bushfire-tweets_labeled.csv", "fire");
		run("data/input/"+"2013_Savar_building_collapse-tweets_labeled.csv", "formatted_Savar_building_collapse-tweets_labeled.csv", "building_collapse");
	}
	public static void run(String inputFile, String outputFile, String topic) {
		System.out.println("Processing file: " + inputFile);

		String csvFile = inputFile;
		BufferedReader br = null;
		String line = "";
		String cvsSplitBy = "\",";
		
		FileWriter fileWriter = null;

		try {
			
			File file = new File(outputFile);
			file.createNewFile();
			fileWriter = new FileWriter(file);

			//Write the CSV file header
			//fileWriter.append(FILE_HEADER.toString());
			//Add a new line separator after the header
			//fileWriter.append(NEW_LINE_SEPARATOR);
			
			String token = null;
			TweetMessage tweet = null;
			
			br = new BufferedReader(new FileReader(csvFile));
			
			
			StringBuffer sb = null;
			Set<String> lines = new HashSet<String>(10000); // maybe should be bigger
			while ((line = br.readLine()) != null) {
				sb = new StringBuffer();
				// use comma as separator
				String[] tokens = line.split(cvsSplitBy);
				
				token = tokens[1].substring(1, tokens[1].length()-1);
				System.out.println(token);
				tweet = new TweetMessage();
				//tweet.setText("En el curso de primeros auxilios con @mvallvey @Tore_Baza95 @drecouvreur #alvaro y #pablo");
				tweet.setText(token);
				
				TweetUtils utils = new TweetUtils("C:\\Program Files (x86)\\WordNet\\2.1\\dict");
				
				token = utils.processTweet(tweet).getText();
				System.out.println("after : "+token);
				if (!tokens[2].startsWith("Not")) {
					if (token.length() != 0) {
						sb.append("'").append(token).append("'");
						sb.append(COMMA_DELIMITER);
						sb.append(topic);
						lines.add(sb.toString());
					}
				}
			}
			for (String unique : lines) {
				fileWriter.append(unique);
				fileWriter.append(NEW_LINE_SEPARATOR);
		    }
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				fileWriter.flush();
				fileWriter.close();
				} catch (IOException e) {
				System.out.println("Error while flushing/closing fileWriter !!!");
				e.printStackTrace();
           }
		}
		System.out.println("Done");
	  }
}