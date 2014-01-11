import java.io.BufferedReader;
import java.util.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;
import org.tartarus.snowball.ext.PorterStemmer;

public class EvaluateQueries {
	private static SortedMap<String, String> word2keyMap = null;
	private static SortedMap<String, SortedSet<String>> key2wordsMap = null;
	private static final CharArraySet STOPWORDS = StandardAnalyzer.STOP_WORDS_SET;
	private static final Version VERSION = Version.LUCENE_44;
	private static final String DATA_DIR = "data/";
	//preprocessing done to query, 0 = none, 1 = replace with stem, 2 = replace with stem class
	private static final int preProcess = 2;
	private static final boolean cluster = true;
	private static PorterStemmer stemmer = new PorterStemmer();
	public static void main(String[] args) {
		
		String docsDir = DATA_DIR + "txt"; // directory containing documents
		String indexDir = DATA_DIR + "index"; // the directory where index is written into
		String queryFile = DATA_DIR + "cacm_processed.query";    // query file
		String answerFile = DATA_DIR + "cacm_processed.rel";   // relevance judgements file
		double threshold = 0.1;//threshold for the clusters used and association measure
		int measure = 0;//association measure for the clustering
		//0=dice, 1=MI, 2 = EMI, 3 = chi
		int numResults = 5;   
		System.out.println("Attempting to build stem classes mapping...");
		try {
			key2wordsMap = WordCluster.getStem2WordsMap(docsDir);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//determines whether to cluster the stem class mappings or not
		if(cluster)
		key2wordsMap = WordCluster.subclusterStem2WordsMap(measure,threshold,key2wordsMap);
		
		word2keyMap = WordCluster.getWord2KeyMap(key2wordsMap);
		
		System.out.println("Average P@5: " + evaluate(indexDir, docsDir, queryFile,
				answerFile, numResults));
	}
	
	/**
	 * Once you finish implementing WordCluster class, you can use 
	 * "key2wordsMap" and "word2keyMap" to transform the query.
	 * (You may also structure the code differently, 
	 * where you may/may not make use of these maps.)
	 * 
	 * @param query: original query extracted from cacm_processed.query file
	 * @return a modified query
	 */
	private static String preprocessQuery(String query){
		String newQuery = "";
		switch(preProcess){
		case 0: newQuery = query;
		break;
		case 1: 
			for(String word : query.toLowerCase().split(" ")){
				stemmer.setCurrent(word);
				stemmer.stem();
				newQuery += stemmer.getCurrent() + " ";
			}
		break;
		case 2: 
			for(String word : query.toLowerCase().split(" ")){
				stemmer.setCurrent(word);
				stemmer.stem();
				String stem;
				if(cluster & word2keyMap.containsKey(word))
					stem = word2keyMap.get(word);
				else
					stem = stemmer.getCurrent();
				if(key2wordsMap.containsKey(stem)){
				for(String words : key2wordsMap.get(stem))
					newQuery += words + " ";
				}
				else{
					newQuery += word + " ";
				}
			}
			break;
		}
		return newQuery.trim();
	}

	private static Map<Integer, String> loadQueries(String filename) {
		Map<Integer, String> queryIdMap = new HashMap<Integer, String>();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(
					new File(filename)));
		} catch (FileNotFoundException e) {
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
		}

		String line;
		try {
			while ((line = in.readLine()) != null) {
				int pos = line.indexOf(',');
				queryIdMap.put(Integer.parseInt(line.substring(0, pos)), preprocessQuery(line.substring(pos + 1)));
			}
		} catch(IOException e) {
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
		} finally {
			try {
				in.close();
			} catch(IOException e) {
				System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
			}
		}
		return queryIdMap;
	}

	private static Map<Integer, HashSet<String>> loadAnswers(String filename) {
		HashMap<Integer, HashSet<String>> queryAnswerMap = new HashMap<Integer, HashSet<String>>();
		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(new File(filename)));

			String line;
			while ((line = in.readLine()) != null) {
				String[] parts = line.split(" ");
				HashSet<String> answers = new HashSet<String>();
				for (int i = 1; i < parts.length; i++) {
					answers.add(parts[i]);
				}
				queryAnswerMap.put(Integer.parseInt(parts[0]), answers);
			}
		} catch(IOException e) {
			System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
		} finally {
			try {
				in.close();
			} catch(IOException e) {
				System.out.println(" caught a " + e.getClass() + "\n with message: " + e.getMessage());
			}
		}
		return queryAnswerMap;
	}

	private static double precision(HashSet<String> answers,
			List<String> results) {
		double matches = 0;
		for (String result : results) {
			if (answers.contains(result))
				matches++;
		}

		return matches / results.size();
	}

	private static double evaluate(String indexDir, String docsDir,
			String queryFile, String answerFile, int numResults) {

		// Build Index
		IndexFiles.buildIndex(indexDir, docsDir, STOPWORDS);

		// load queries and answer
		Map<Integer, String> queries = loadQueries(queryFile);
		Map<Integer, HashSet<String>> queryAnswers = loadAnswers(answerFile);

		// Search and evaluate
		double sum = 0;
		for (Integer i : queries.keySet()) {
			List<String> results = SearchFiles.searchQuery(indexDir, queries
					.get(i), numResults, STOPWORDS);
			sum += precision(queryAnswers.get(i), results);
		}

		return sum / queries.size();
	}
}
