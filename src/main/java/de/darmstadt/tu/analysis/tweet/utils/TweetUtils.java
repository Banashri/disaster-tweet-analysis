package de.darmstadt.tu.analysis.tweet.utils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;

import rita.RiWordNet;
import de.darmstadt.tu.analysis.tweet.objects.TweetMessage;

/**
 * This class contains methods related to tweet text analysis, useful for
 * reading JSON file, processing the tweet as it contains polluted texts and
 * writing it to a sample output file
 */
public class TweetUtils {
	
	private final static String CLASS_NAME = TweetUtils.class.getName();
	private final static Logger LOGGER = Logger.getLogger(CLASS_NAME); 
	
	private final String DISASTER_RELATED_WORDS_FILE = "data/analysis-files/disaster-related-words.txt";
	
	private RiWordNet wordnet;

	private final List<String> commonWords = Arrays.asList("a", "am", "an", "and", "are", "as", "at", "be", "by", "for", "from", "has", "he", "i", "in", "is", "it", "its", "of", "on", "she", "that", "the", "they", "this", "to", "you", "was", "we", "were", "will", "with");
	
	private static LinkedHashSet<String> allDisasterRelatedWords;
	
	public void setAllDisasterRelatedWords(LinkedHashSet<String> allDisasterRelatedWords) {
		TweetUtils.allDisasterRelatedWords = allDisasterRelatedWords;
	}
	
	public TweetUtils(String dictPath) {
		wordnet = WordnetUtils.getWordnet(dictPath);
	}
	
	/**
	 * This implementation is about reading tweets from tweet JSON file
	 * @return
	 */
	public static TweetMessage[] readTweetJSONFile(String tweetJSONFile) {
		
		BufferedReader br = null;
		try {
			String sCurrentLine;
			br = new BufferedReader(new FileReader(tweetJSONFile));
			StringBuffer sbf = new StringBuffer();
			while ((sCurrentLine = br.readLine()) != null) {
				sbf.append(sCurrentLine);
			}
			tweetJSONFile = sbf.toString();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
		TweetMessage[] tweets = null;
		try {
	        JsonReader jsonReader = Json.createReader(new StringReader(tweetJSONFile));
	        JsonObject jsonObject = jsonReader.readObject();
	        jsonReader.close();

	        JsonArray jsonArray = jsonObject.getJsonArray("data");
			TweetMessage tweetMessage = null;
			JsonObject temp = null;
			int index = 0;
			
			int totalTweets = jsonArray.size();
			tweets = new TweetMessage[totalTweets];
			
	        for (JsonValue obj: jsonArray) {
	        	temp = (JsonObject) obj;
	        	tweetMessage = new TweetMessage();
	        	tweetMessage.setText(temp.getString("text").toLowerCase());
	        	tweetMessage.setOriginalTxt(temp.getString("text"));
	        	tweetMessage.setUserId(temp.getString("tweetId"));
	        	tweetMessage.setTweetId(temp.getString("tweetId"));
	        	tweetMessage.setUserName(temp.getString("uname"));
	        	tweetMessage.setTime(temp.getString("time"));
	        	tweetMessage.setHashtags(new ArrayList<String>());
	        	tweetMessage.setLatitude(temp.getJsonArray("location").getString(0));
	        	tweetMessage.setLongitude(temp.getJsonArray("location").getString(1));
	        	tweets[index] = tweetMessage;
	        	index ++;
	        }
		} catch (Exception e) {
			e.printStackTrace();
		}
		return tweets;
	}
	
	/**
	 * This method implements the preprocessing of tweet message.
	 * It separates the tokens and removes the tweeter specific symbols and URLs.
	 * @param message
	 * @return
	 */
	public TweetMessage processTweet(TweetMessage tweet) {
		
		tweet.setText(tweet.getText().trim().toLowerCase());
		
		try {
			byte[] utf8Bytes = tweet.getText().getBytes("UTF-8");
			String utf8tweet = new String(utf8Bytes, "UTF-8");
			
			Pattern unicodeOutliers = Pattern.compile("[^\\x00-\\x7F]",
	                Pattern.UNICODE_CASE | Pattern.CANON_EQ
	                        | Pattern.CASE_INSENSITIVE);
	        Matcher unicodeOutlierMatcher = unicodeOutliers.matcher(utf8tweet);

	        utf8tweet = unicodeOutlierMatcher.replaceAll(" ");
	        tweet.setText(utf8tweet);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		StringTokenizer tokens = new StringTokenizer(tweet.getText());
		StringBuffer tweetBf = new StringBuffer();

		while (tokens.hasMoreTokens()) {

			String token = (String) tokens.nextElement();

			if (token.length() > 1) {
				if (token.startsWith("#")) {
					tweet.getHashtags().add(token);
				}
				/**
				 * There is no need to add hashtagged words to tweets, to avoid
				 * including it into feature space
				 */
				else {
					if (!token.equalsIgnoreCase("RT") && !token.startsWith("@")
							&& !token.startsWith("http") && !token.startsWith("www")) {
						if (commonWords.contains(token))
							tweetBf.append(" ");
						else {
							if (token.matches("[a-zA-Z ]*\\d+.*"))
								tweetBf.append(" ");
							else
								tweetBf.append(token).append(" ");
						}
					}
					else
						tweetBf.append(" ");
				}
			}
		}
		tweet.setText(tweetBf.toString().trim());
		String tweetText = tweetBf.toString().replaceAll("[^\\dA-Za-z ]", "").replaceAll("\\s+", " ");
	
		tweet.setText(tweetText);
		/**
		 * The following code considers only the noun and verbs of the tweet from the WordNet dictionary.
		 * All other part of speech are removed.
		 */
		if (wordnet != null) {
			tokens = new StringTokenizer(tweet.getText());
			tweetBf = new StringBuffer("");
			
			/**
			 * Stemmatization of each words in the tweet
			 */
			while (tokens.hasMoreTokens()) {
				String token = (String) tokens.nextElement();
				String word = WordnetUtils.getStem(token);
				if (word != null)
					tweetBf.append(word.toLowerCase()).append(" ");
			}
		}
		tweet.setText(tweetBf.toString().trim());
		return tweet;
	}
	/**
	 * This method is the implementation of the following logic.
	 * 1. Reads the disaster-related-words file, taken from Internet
	 * 2. For every word (noun or verb), all hyponyms and related words are collected from Wordnet dictionary
	 * 3. Large word-set is added to disaster-related-words file, which will contain list of words to detect disaster-specific tweets
	 */
	public LinkedHashSet<String> getAllDisasterWords() throws FileNotFoundException, IOException {
		
		LinkedHashSet<String> words = new LinkedHashSet<String>(8000);
		InputStream fis = new FileInputStream(DISASTER_RELATED_WORDS_FILE);
		BufferedReader bfr = new BufferedReader(new InputStreamReader(fis));
		
		// STEP: 1
		String line, word;
		while ((line = bfr.readLine()) != null) {
			word = line;
			LinkedHashSet<String> relatedWords = WordnetUtils.getAllRelatedWords(word);
			words.addAll(relatedWords);
		}
		return words;
	}
	public boolean isDisasterTweet(String tweet) {
		StringTokenizer strs = new StringTokenizer(tweet);
		while (strs.hasMoreTokens()) {
			String token = strs.nextToken();
			if (allDisasterRelatedWords.contains(token)) {
				//System.out.println(token + " matched");
				return true;
			}
		}
		return false;
	}
}
