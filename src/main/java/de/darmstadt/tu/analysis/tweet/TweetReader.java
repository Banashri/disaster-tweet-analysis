/**
 * 
 */
package de.darmstadt.tu.analysis.tweet;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import de.darmstadt.tu.analysis.tweet.objects.TweetMessage;
import de.darmstadt.tu.analysis.tweet.utils.TweetUtils;

public class TweetReader {

	private final static String CLASS_NAME = TweetReader.class.getName();
	private final static Logger LOGGER = Logger.getLogger(CLASS_NAME); 

	private TweetMessage[] tweets;
	
	private List<TweetMessage> nonTaggedDisasterTweets;
	
	private List<TweetMessage> taggedDisasterTweets;
	
	public List<TweetMessage> getNonTaggedDisasterTweets() {
		return nonTaggedDisasterTweets;
	}

	public List<TweetMessage> getTaggedDisasterTweets() {
		return taggedDisasterTweets;
	}

	/**
	 * http://www.journaldev.com/2315/java-json-processing-api-example-tutorial
	 * 
	 * @param args The name of the file which contains all tweets which are to be classified
	 */
	public TweetReader(TweetMessage[] tweets, TweetUtils tweetUtils) {
		
		final String METHOD_NAME = "constructor";
		
		LOGGER.entering(CLASS_NAME, METHOD_NAME);
		
		nonTaggedDisasterTweets = new ArrayList<TweetMessage>();
		taggedDisasterTweets = new ArrayList<TweetMessage>();

		/**
		 * STEP 1: 
		 */
		this.tweets = tweets;
		/**
		 * STEP 2: Pre-processing of the Tweets because it contains mostly URLs, and twitter specific tags and hashtags
		 * In this step, those are removed for further processing of only string tokens of the message.
		 * Also the special characters and tweet emotions are removed here. The result tweet contains lemmatization and stematization
		 * form of the word tokens.
		 */
		TweetMessage[] processedTweets = new TweetMessage[tweets.length];
		int index = 0;
		for (TweetMessage tweet: tweets) {
			tweet = tweetUtils.processTweet(tweet);
			processedTweets[index] = tweet;
			index ++;
		}
		/*for(TweetMessage t: processedTweets) {
			System.out.println("After processing: text:"+t.getText()+", hashtags:"+t.getHashtags());
		}*/

		/**
		 * STEP 3: Tweet filtering based on existing disaster related keywords
		 */
		try {
			LinkedHashSet<String> allWords = tweetUtils.getAllDisasterWords();
			tweetUtils.setAllDisasterRelatedWords(allWords);
			
			List<TweetMessage> filteredTweets = new ArrayList();
			boolean isPredefindHashtagUsed = false;
			
			
			InputStream in = new FileInputStream("tweet-analysis.properties");
			Properties prop = new Properties();
			
			List<String> existingDisasters = new ArrayList<String>();
			
			if (in != null) {
				prop.load(in);
				String predefinedDisastersStr = prop.getProperty("PREDEFINED_HASHTAGS", null);
				if (predefinedDisastersStr != null) {
					StringTokenizer tokenizer = new StringTokenizer(predefinedDisastersStr);
					
					while (tokenizer.hasMoreElements())
						existingDisasters.add((String)tokenizer.nextElement());
				}
			}
			//System.out.println("Existing disasters: "+existingDisasters);

			for (TweetMessage tweet: processedTweets) {
				isPredefindHashtagUsed = false;

				for (String hashtag: tweet.getHashtags()) {
					if (hashtag.startsWith("##")) {
						filteredTweets.add(tweet);
						tweet.setTopic(hashtag.substring(2));
						taggedDisasterTweets.add(tweet);
						isPredefindHashtagUsed = true;
						break;
					} else {
						
					}
				}
				if (!isPredefindHashtagUsed) {
					if (tweetUtils.isDisasterTweet(tweet.getText())) {
						filteredTweets.add(tweet);
						System.out.println("Disaster tweet detected: "+tweet.getText()+" and hashtags:"+tweet.getHashtags());
						if (tweet.getHashtags() != null && tweet.getHashtags().size() > 0) {
							boolean matched = false;
							for (String hashtag: tweet.getHashtags()) {
								if (existingDisasters.contains(hashtag.toLowerCase().substring(1))) {
									tweet.setTopic(hashtag.substring(1));
									taggedDisasterTweets.add(tweet);
									matched = true;
								}
							}
							if (!matched)
								nonTaggedDisasterTweets.add(tweet);
						}
						else 				
							nonTaggedDisasterTweets.add(tweet);
					}
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		LOGGER.exiting(CLASS_NAME, METHOD_NAME);
	}
}