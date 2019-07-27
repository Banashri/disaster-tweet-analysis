package de.darmstadt.tu.analysis.tweet.classifier;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import weka.attributeSelection.InfoGainAttributeEval;
import weka.attributeSelection.Ranker;
import weka.classifiers.bayes.NaiveBayesMultinomial;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.MultiFilter;
import weka.filters.supervised.attribute.AttributeSelection;
import weka.filters.unsupervised.attribute.StringToWordVector;
import de.darmstadt.tu.analysis.tweet.objects.TweetMessage;


public class TweetTopicClassifier {
	
	private static Instances trainData;
	private static Instances testData;
	
	private static StringToWordVector filter;
	private static FilteredClassifier classifier;
	private static FilteredClassifier savedClassifier;
    
	
	private static List<TweetMessage> tweets;

	public void setTweets(List<TweetMessage> tweets) {
		this.tweets = tweets;
	}

	/**
	 * This method makes the classifier to learn the data
	 */
	private void learn() {
		try {
			if (trainData.classIndex() == -1)
				trainData.setClassIndex(trainData.numAttributes() - 1);
	    	filter = new StringToWordVector();
			filter.setAttributeIndices("first");
			filter.setDoNotOperateOnPerClassBasis(true);
			filter.setLowerCaseTokens(true);
			
			classifier = new FilteredClassifier();
			AttributeSelection as = new AttributeSelection();
			as.setEvaluator(new InfoGainAttributeEval());
			Ranker r = new Ranker();
			r.setThreshold(0);
			as.setSearch(r);
			MultiFilter multiFilter = new MultiFilter();
			multiFilter.setFilters(new Filter[]{ filter, as });
			multiFilter.setInputFormat(trainData);
			classifier.setFilter(multiFilter);
		    classifier.setClassifier(new NaiveBayesMultinomial());
		    classifier.buildClassifier(trainData);
		} catch (Exception e) {
			System.out.println("Error encountered in learning" + e.getMessage());
		}
	}
	
	/**
	 * This method is used for classification of the test data
	 */
	public String classify() {
		try {
			double score = savedClassifier.classifyInstance(testData.instance(0)); 
			String prediction = testData.classAttribute().value((int)score);
			return prediction;
		}
		catch (Exception e) {
			System.out.println("Problem found when classifying the text");
			return null;
		}	
	}
	
	/**
	 * This method creates the instance to be classified, from the text that has been read.
	 */
	public void makeInstance(String text) {
		// Create the attributes, class and text
		FastVector fvNominalVal = new FastVector(11);
		fvNominalVal.addElement("derailment");
		fvNominalVal.addElement("fire");
		fvNominalVal.addElement("crash");
		fvNominalVal.addElement("wildfire");
		fvNominalVal.addElement("earthquake");
		fvNominalVal.addElement("bombings");
		fvNominalVal.addElement("shooting");
		fvNominalVal.addElement("flood");
		fvNominalVal.addElement("building_collapse");
		fvNominalVal.addElement("typhoon");
		fvNominalVal.addElement("explosion");
		
		Attribute attribute1 = new Attribute("text",(FastVector) null);
		Attribute attribute2 = new Attribute("class_attr", fvNominalVal);
		
		// Create list of instances with one element
		FastVector fvWekaAttributes = new FastVector(2);
		fvWekaAttributes.addElement(attribute1);
		fvWekaAttributes.addElement(attribute2);
		
		testData = new Instances("disaster_tweets", fvWekaAttributes, 1);           
		// Set class index
		testData.setClassIndex(1);
		// Create and add the instance
		Instance instance = new Instance(2);
		instance.setValue(attribute1, text);
		testData.add(instance);
	}

	/**
	 * This method is used  for loading the classifier model for prediction on test data
	 * @param fileName The name of the model file
	 */
	public void loadModel(String fileName) {
		try {
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(fileName));
            Object tmp = in.readObject();
            savedClassifier = (FilteredClassifier) tmp;
            in.close();
 			System.out.println("===== Loaded model: " + fileName + " =====");
       } 
		catch (Exception e) {
			// Given the cast, a ClassNotFoundException must be caught along with the IOException
			System.out.println("Problem found when reading: " + fileName);
		}
	}
	
	/**
	 * This method is used for saving the classifier model for future reusue
	 * @param fileName The name of the model file
	 */
	private void saveModel(String fileName) {
		try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fileName));
            out.writeObject(classifier);
            out.close();
 			System.out.println("===== Saved model: " + fileName + " =====");
        } 
		catch (IOException e) {
			System.out.println("Problem found when writing: " + fileName);
		}
	}
	
	/**
	 * 
	 * @param fileName
	 */
	private void loadDataset(String fileName) {
		try {
			DataSource dataSrc = new DataSource(fileName);
			trainData = dataSrc.getDataSet();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
	public static void main(String[] args) {
		TweetTopicClassifier c = new TweetTopicClassifier();
		
		// For learning
		c.loadDataset("data/analysis-files/train.arff");
		c.learn();
		c.saveModel("data/model/twitterClassifier.binary");
	    
		// For testing
	    c.loadModel("data/model/twitterClassifier.binary");
	    c.makeInstance("Tweets turned into flood maps that could help save lives");
	}
}