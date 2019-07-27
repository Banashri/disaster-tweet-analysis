package de.darmstadt.tu.analysis.tweet.clustering;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;

import org.apache.commons.math3.exception.NullArgumentException;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.apache.commons.math3.ml.clustering.DoublePoint;

import de.darmstadt.tu.analysis.tweet.objects.TweetMessage;

public class TweetTopicClusterering {
	
	private final static String FILE_LOCATION = "data/cluster-file/";
	
	private Properties props;
	
	/**
	 * tweetsInFile contains the (lat,long) as key and list of tweets having same (lat-long)
	 */
	private Map<String, List<TweetMessage>> tweetsInFile;
	
	public TweetTopicClusterering(String propertyFileName) {
		try {
			props = new Properties();

			InputStream in = new FileInputStream(propertyFileName);
			
			if (in != null) {
				props.load(in);
				in.close();
			}
			else
				throw new FileNotFoundException("property file not found in the classpath");
		} catch (IOException e) {
			props = null;
			e.printStackTrace();
		} 
	}
	
	/**
	 * This implements the reading of the tweets from the file in order to obtain GPS location of the tweet senders.
	 * @param f The file to be read, which is specific to a topic
	 * @return List of coordinates of the location of tweets
	 */
	private List<DoublePoint> getGPS(File f) {
	    
		tweetsInFile = new HashMap<String, List<TweetMessage>>();

		List<DoublePoint> points = new ArrayList<DoublePoint>();
	    
        FileInputStream inputStream = null;
        Scanner sc = null;
        String line;

        try {
	        inputStream = new FileInputStream(f);
	        sc = new Scanner(inputStream, "UTF-8");
	        while (sc.hasNextLine()) {
	        	line = sc.nextLine();
	        	double[] d = new double[2];
                d[0] = Double.parseDouble(line.split("\\|")[1]);
                d[1] = Double.parseDouble(line.split("\\|")[2]);
                points.add(new DoublePoint(d));
                
                TweetMessage tweet = new TweetMessage();
                tweet.setTweetId(line.split("\\|")[0]);
                tweet.setLatitude(line.split("\\|")[1]);
                tweet.setLongitude(line.split("\\|")[2]);
                tweet.setText(line.split("\\|")[3]);
                tweet.setOriginalTxt(line.split("\\|")[3]);
                
                String key = d[0]+" "+d[1];
                if (tweetsInFile.containsKey(key)) {
                	List<TweetMessage> l = tweetsInFile.get(key);
                	l.add(tweet);
                	tweetsInFile.put(key, l);
                } else {
                	List<TweetMessage> l = new ArrayList<TweetMessage>();
                	l.add(tweet);
                	tweetsInFile.put(key, l);
                }
	        }
	        if (sc.ioException() != null)
	        	throw sc.ioException();
        } catch (IOException e) {
			e.printStackTrace();
		} finally {
        	if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
        	}
        	if (sc != null)
        		sc.close();
        }
	    return points;
	}

	/**
	 * This method is used to find list of all clusters which contain tweets
	 * @param topic
	 * @return List of clusters which are list of tweets. The format is: List(List(1,2,10), List(3,4), List(1))
	 */
	public List<List<TweetMessage>> getCluster(String topic) {
		
		System.out.println("BEGIN: CLUSTERING FOR TOPIC: "+topic);
		System.out.println("-------------------------------------");
		try {
			double epsilon = 0;
			
			String epsilonKey = null;
			
			switch (topic) {
				case "flood" : epsilonKey = "FLOOD_EPSILON"; break;
				case "wildfire" : epsilonKey = "FLOOD_EPSILON"; break;
				case "earthquake" : epsilonKey = "EARTHQUAKE_EPSILON"; break;
				case "quake" : epsilonKey = "EARTHQUAKE_EPSILON"; break;
				default: epsilonKey = "DEFAULT_EPSILON"; break;
			}
			
			String epsilonStr = props.getProperty(epsilonKey);
			if (epsilonStr != null) {
				epsilon = Double.parseDouble(epsilonStr);
			}
			
			System.out.println("EPSILON USED: "+epsilon);
			
			DBSCANClusterer dbScan = new DBSCANClusterer(epsilon, 1, new LatLongDistanceMeasure());
			List<Cluster<DoublePoint>> cluster = dbScan.cluster(getGPS(new File(FILE_LOCATION+topic+".txt")));

			System.out.println("NO. OF CLUSTERS FORMED FOR TOPIC: "+topic+" : "+cluster.size());
			
			List<List<TweetMessage>> clusters = new ArrayList<List<TweetMessage>>();
			Set<String> clusteredLatLong = new HashSet<String>();

			for(Cluster<DoublePoint> c: cluster){
				//System.out.println("Cluster["+i+ "] contains the points:" +c.getPoints().size());
				List<TweetMessage> tweetsInEachCluster = new ArrayList<TweetMessage>();

				for (DoublePoint p: c.getPoints()) {
					String key = p.getPoint()[0]+" "+p.getPoint()[1];
	                clusteredLatLong.add(key);
	                tweetsInEachCluster.addAll(tweetsInFile.get(key));
				}
				clusters.add(tweetsInEachCluster);
		    }
			
			/**
			 * At this point, we have list of all locations which belong to generated clusters
			 * But some single points are not clustered, so we need to find out them and make a single point cluster
			 */

			Set<String> allLatLong = tweetsInFile.keySet();
			
			System.out.println("ALL GPS LOCATIONS FOR THIS TOPIC ARE: "+allLatLong);
			System.out.println("POINTS INSIDE THE CLUSTERS ARE: "+clusteredLatLong);
			allLatLong.removeAll(clusteredLatLong);
			System.out.println("POINTS OUTSIDE THE CLUSTERS ARE: "+ allLatLong);

			for (String eachEntry: allLatLong) {
				clusters.add(tweetsInFile.get(eachEntry));
			}
			System.out.println("-------- FINAL CLUSTERS --------------");
			for (List<TweetMessage> tweets: clusters) {
				System.out.println("............ CLUSTER  ..............");
				for (TweetMessage tweet: tweets) {
					System.out.println("Tweet details: "+tweet.getTweetId()+"|"+tweet.getLatitude()+"|"+tweet.getLongitude()+"|"+tweet.getOriginalTxt());
				}
			}
			System.out.println("END: CLUSTERING FOR TOPIC: "+topic);
			System.out.println("-------------------------------------");
			
			return clusters;
		} catch (NullArgumentException e) {
			e.printStackTrace();
			return null;
		}
	}
}