package de.darmstadt.tu.analysis.tweet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.geometry.euclidean.twod.hull.ConvexHull2D;
import org.apache.commons.math3.geometry.euclidean.twod.hull.ConvexHullGenerator2D;
import org.apache.commons.math3.geometry.euclidean.twod.hull.MonotoneChain;

import de.darmstadt.tu.analysis.tweet.classifier.TweetTopicClassifier;
import de.darmstadt.tu.analysis.tweet.clustering.TweetTopicClusterering;
import de.darmstadt.tu.analysis.tweet.objects.TweetMessage;
import de.darmstadt.tu.analysis.tweet.utils.TweetUtils;

public class TweetServiceClient {

	private List<TweetMessage> tweets;
	private TweetReader reader;
	private TweetTopicClassifier tweetClassifier;
	
	private final static String FILE_LOCATION = "data/cluster-file/";
	
	public TweetServiceClient() {
		tweetClassifier = new TweetTopicClassifier();
		tweetClassifier.loadModel("data/model/twitterClassifier.binary");
	}

	/**
	 * This is method used for grouping the tweets based on disaster and location
	 * @param tweetJSONFileName The name of the JSON file which contains the tweets as test data
	 * @return A list of tweets groped based on locations and disaster topics
	 */
	private List<TweetMessage> analyzeTweets(TweetMessage[] tweets, TweetUtils utils) throws Exception{
		reader = new TweetReader(tweets, utils);
		tweetClassifier.setTweets(reader.getNonTaggedDisasterTweets());

		System.out.println("ALL TAGGED-TWEETS: "+reader.getTaggedDisasterTweets().size());
		System.out.println("----------------------------------------");
		for (TweetMessage tweet: reader.getTaggedDisasterTweets()) {
			System.out.println("Tweet text: "+tweet.getText()+", topic:"+tweet.getTopic());
		}
		System.out.println("ALL NON-TAGGED-TWEETS: "+reader.getNonTaggedDisasterTweets().size());
		System.out.println("----------------------------------------");
		for (TweetMessage tweet: reader.getNonTaggedDisasterTweets()) {
			tweetClassifier.makeInstance(tweet.getText());
			tweet.setTopic(tweetClassifier.classify());
			System.out.println("Tweet text: "+tweet.getText()+", topic:"+tweet.getTopic());
		}
		System.out.println("NON-DISASTER RELATED TWEETS: "+ (tweets.length-reader.getNonTaggedDisasterTweets().size()-reader.getTaggedDisasterTweets().size()));
		System.out.println("----------------------------------------");
		Set<String> tweetTopics = new HashSet<String>();
		Map<String, String> fileForTopics = new HashMap<String,String>(); 

		List<TweetMessage> allTweets = new ArrayList<TweetMessage>(reader.getNonTaggedDisasterTweets());
		allTweets.addAll(reader.getTaggedDisasterTweets());
		
		/**
		 * This data structure contains topic-Name and the file location for this topic.
		 * The file will list all tweets assigned to this topic
		 */
		Map<String, TweetMessage> allTweetsMap = new HashMap<String, TweetMessage>();

		for (TweetMessage eachTweet: allTweets) {
			tweetTopics.add(eachTweet.getTopic());
			allTweetsMap.put(eachTweet.getTweetId(), eachTweet);
			fileForTopics.put(eachTweet.getTopic(), FILE_LOCATION + eachTweet.getTopic()+".txt");
		}

		/**
		 * Lat-long values are stored into the file specific to every topics (predefined+classified)
		 * For Clustering
		 */
		for (String eachTopic: fileForTopics.keySet()) {
			File f = new File(fileForTopics.get(eachTopic));
			if (!f.exists())
				f.createNewFile();

			FileWriter fw = new FileWriter(f.getAbsoluteFile(), true); // to append
			BufferedWriter bw = new BufferedWriter(fw);
			
			/**
			 * allTweets contain only new tweets which are with predefined and classified topics
			 */
			for (TweetMessage eachTweet: allTweets) {
				if (eachTweet.getTopic().equalsIgnoreCase(eachTopic)) {
					bw.write(eachTweet.getTweetId()+"| "+eachTweet.getLatitude()+"| "+eachTweet.getLongitude()+"| '"+eachTweet.getOriginalTxt()+"'");
					bw.write('\n');
				}
			}
			bw.close();
		}
		/**
		 * Clustering for every topic
		 */
		TweetTopicClusterering topicClustering = new TweetTopicClusterering("tweet-analysis.properties");

		for (String eachTopic: fileForTopics.keySet()) {
			List<List<TweetMessage>> clustersWithTweets = topicClustering.getCluster(eachTopic); 
			List<ConvexHull2D> allHulls = new ArrayList<ConvexHull2D>();
			
			for (List<TweetMessage> tweetListACluster: clustersWithTweets) {
				
				List<Vector2D> points = new ArrayList<Vector2D>();
				
				for (TweetMessage tweet: tweetListACluster) {
					points.add(new Vector2D(new double[]
							{Double.parseDouble(tweet.getLatitude()), Double.parseDouble(tweet.getLongitude())}));
				}

				ConvexHullGenerator2D generator = new MonotoneChain(true, 1e-6);
				ConvexHull2D hull = generator.generate(points);
				
				System.out.print("GENERATED HULL: ");
				for (int i = 0; i <hull.getVertices().length; i++) {
					System.out.print(" "+hull.getVertices()[i]+" ");
				}
				System.out.println();
				allHulls.add(hull);
			}
			System.out.println("All hulls: "+allHulls);
			System.out.println();
		}
		return null;
	}
	
	/**
	 * This is the code for classification
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		TweetMessage[] tweets = TweetUtils.readTweetJSONFile("disaster-tweets.txt");
		TweetUtils utils = new TweetUtils("dict");
		TweetServiceClient client = new TweetServiceClient();
		client.analyzeTweets(tweets, utils);
	}
}