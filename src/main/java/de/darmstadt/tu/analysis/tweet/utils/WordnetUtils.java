package de.darmstadt.tu.analysis.tweet.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import rita.RiWordNet;

public class WordnetUtils {
	
	public static RiWordNet wordnet;
	
	public static String wordPOS;
	
	
	private static void getWordNetDictionary(String dictPath) {
		wordnet = new RiWordNet(dictPath);
	}

	public static RiWordNet getWordnet(String path) {
		getWordNetDictionary(path);
		return wordnet;
	}

	public static String getStem(String word) {
		String[] vStems = wordnet.getStems(word, RiWordNet.VERB);
		String[] nStems = wordnet.getStems(word, RiWordNet.NOUN);
		
		if (vStems.length > 0)
			return vStems[0];
		else if (nStems.length > 0)
			return nStems[0];
		else
			return null;
	}

	
	public static boolean isRelevantPOS(String word) {
		String[] pos = wordnet.getPos(word);
		List<String> posList = Arrays.asList(pos);
		
		boolean flag = true;
		
		//System.out.println("Relevant check: word: " + word + "posList :" + posList)
		;
		for (String eachPos: posList) {
			if (!eachPos.equalsIgnoreCase(RiWordNet.NOUN) || !eachPos.equalsIgnoreCase(RiWordNet.VERB)) {
				flag = false;
				break;
			}
		}
		return flag;
	}
	
	
	public static LinkedHashSet<String> getAllRelatedWords(String word) {
		
		LinkedHashSet<String> words = new LinkedHashSet<String>(50);
		word = WordnetUtils.getStem(word);
		
		if (word != null) {
			String[] pos = wordnet.getPos(word);
			for (int i = 0; i <pos.length; i++) {
				String[] str = wordnet.getSynset(word, pos[i], true);
				if (str.length != 0)
					words.addAll(Arrays.asList(str));

				str = wordnet.getAllSynonyms(word, pos[i], 5);
				if (str.length != 0)
					words.addAll(Arrays.asList(str));
				
				str = wordnet.getAllHyponyms(word, pos[i]);
				if (str.length != 0)
					words.addAll(Arrays.asList(str));
				
				str = wordnet.getAllHypernyms(word, pos[i]);
				if (str.length != 0)
					words.addAll(Arrays.asList(str));
				
				str = wordnet.getAllDerivedTerms(word, pos[i]);
				if (str.length != 0)
					words.addAll(Arrays.asList(str));
			}
			//System.out.println(words.size());
		}
		return words;
	}

	public static String[] getSynSet(String word) {
		String[] pos = wordnet.getPos(word);
		List<String> words = new ArrayList<String>();
		
		for (int i = 0; i <pos.length; i++) {
			String[] str = wordnet.getSynset(word, pos[i], true);
			if (str.length != 0)
				words.addAll(Arrays.asList(str));
		}
		//System.out.println(words);
		return words.toArray(new String[words.size()]);
	}
	
	public static String[] getSynonyms(String word) {
		String[] pos = wordnet.getPos(word);
		List<String> words = new ArrayList<String>();
		
		for (int i = 0; i <pos.length; i++) {
			String[] str = wordnet.getAllSynonyms(word, pos[i]);
			if (str.length != 0)
				words.addAll(Arrays.asList(str));
		}
		//System.out.println(words);
		return words.toArray(new String[words.size()]);
	}
}
