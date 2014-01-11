import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.standard.*;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.tartarus.snowball.ext.PorterStemmer;

/**
 * Below inside the main method are comments which specify which functionality is supported
 * We support functionality of outputting stem classes, clustering based off of all four association measures
 * finding most closely associated terms to the term computer, printing out specific stem classes for a clustering
 * as well as printing out stem class groupings based on size
 */
public class WordCluster {
	/**
	 * Given a directory containing a set of text files (CACM), return a mapping from stem to words with the given stem.
	 * @throws Exception 
	 */
	public static HashMap<String, HashSet<Integer>> index = new HashMap<String, HashSet<Integer>>();
	public static PorterStemmer stemmer = new PorterStemmer();
	public static SortedMap<String, SortedSet<String>> stem2words;
	public static int docCnt = 0;
	public static void main(String[] args) throws Exception{
		String text_dir = "data/txt";
		System.out.println("Creating stem -> word mapping...");
		stem2words = getStem2WordsMap(text_dir);
		System.out.println("Mapping complete...");
		
		//To print stem classes, uncomment below line
		//printStem(stem2words);
		
		
		//To see the most closely related words to "computer" uncomment below line
		//findCompAssoc();
		
		
		//gets count of stem class groups based on number of elements and then prints them out
		/*
		TreeMap<Integer, Integer> counts = getStem2WordCounts(stem2words);
		for(int i : counts.keySet())
			System.out.println(counts.get(i)+" stem class(es) have "+ i+" word(s)");*/
		
		//threshold is the threshold used as the value for association measure chosen
		double threshold = 0.062;
		
		//association measure used for clustering
		//0=dice, 1=MI, 2=EMI, 3=chi
		int measure = 0;
		
		SortedMap<String, SortedSet<String>> clusters = subclusterStem2WordsMap(measure, threshold, stem2words);
		
		//below block of code prints out new clusters of stem classes which
		//originally had more than ten words contained
		String[] stems = {"comput","gener","integr","continu"};
		String id = "";
		for(String base : stems ){
			for(int i = 1; i < 10; i++){
				id = base+i;
				if(clusters.containsKey(id)){
					System.out.print(id+" : ");
					for(String y : clusters.get(id))
						System.out.print(y+" ");
					System.out.println();
				}
				else
					break;
			}
		}
		
		
		//uncomment below section to print out all stemclass subclusters
		/*for(String stem: clusters.keySet()){
			System.out.print(stem + ":");
			for(String word : clusters.get(stem)){
				System.out.print(word + " ");
			}
			System.out.println();
		}*/
			
		
	}
	
	public static TreeMap<Integer, Integer> getStem2WordCounts(SortedMap<String, SortedSet<String>> map){
		TreeMap<Integer, Integer> counts= new TreeMap<Integer, Integer>();
		for(SortedSet<String> t : map.values()){
			int i = t.size();
			int count;
			if(counts.containsKey(i))
				count = counts.get(i)+1;
			else
				count = 1;
			counts.put(i, count);
		}
		return counts;
	}
	
	public static void printStem(SortedMap<String, SortedSet<String>> map){
		try {
		      //create an print writer for writing to a file
		      PrintWriter out = new PrintWriter(new FileWriter("stemclasses.txt"));
		      for(String x: map.keySet()){
		    	  System.out.println(x);
		    	  out.print(x+" : ");
		    	  for(String y: map.get(x))
		    		  out.print(y+" ");
		    	  out.println();
		      }
		      //close the file (VERY IMPORTANT!)
		      out.close();
		   }
		      catch(IOException e1) {
		        System.out.println("Error during reading/writing");
		   }
		
		
	}
	public static SortedMap<String, SortedSet<String>> getStem2WordsMap(String text_dir) throws Exception {
		TreeMap<String, SortedSet<String>> stem2words = new TreeMap<String, SortedSet<String>>();
		Version version = Version.LUCENE_44;
		
		//need to add code to handle all individual files in text_dir
		
		File dir = new File(text_dir);
		
		
		for(File f: dir.listFiles()){
		FileReader reader = new FileReader(f);
		StandardTokenizer src = new StandardTokenizer(version, reader);
		src.setMaxTokenLength(10000);
		TokenStream tokenStream = new StandardFilter(version, src);
		tokenStream = new LowerCaseFilter(version, tokenStream);
		tokenStream = new StopFilter(version, tokenStream, StandardAnalyzer.STOP_WORDS_SET);
		CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
		tokenStream.reset();
		
		while (tokenStream.incrementToken()) {
		   String term = charTermAttribute.toString(); 
		   mapStem(term, stem2words);
		   mapIndex(term, docCnt);
			}
		docCnt++;
		}
		return stem2words;
	}
	
	public static void mapStem(String term, SortedMap<String, SortedSet<String>> stem2words){
		   stemmer.setCurrent(term);
		   stemmer.stem();
		   String stem = stemmer.getCurrent();
		   SortedSet<String> terms;
		   if(stem2words.containsKey(stem))
			   terms = stem2words.get(stem);
		   else
			   terms = new TreeSet<String>();
		   terms.add(term);
		   stem2words.put(stem, terms);	
	}
	
	public static void mapIndex(String term, int docID){
		   HashSet<Integer> ids;
		   if(index.containsKey(term))
			   ids = index.get(term);
		   else
			   ids = new HashSet<Integer>();
		   ids.add(docID);
		   index.put(term, ids);		
	}
	//calculate na for a term
	public static int calcNa(String term){
		return index.get(term).size();
	}
	//calculate nab for two terms
	public static int calcNAB(String a, String b){
		int count = 0;
		for(int x : index.get(a))
			for(int y : index.get(b))
				if(x==y)
					count++;
		return count;
	}
	//calculates association scores for all terms with "computer"
	//then prints top ten terms for each association measure
	public static void findCompAssoc(){
		HashMap<String, Integer> nab = getNABVal();
		int N = docCnt;
		int b = calcNa("computer");
		TreeMap<Double, HashSet<String>> chiScore = new TreeMap<Double, HashSet<String>>();
		TreeMap<Double, HashSet<String>> miScore = new TreeMap<Double, HashSet<String>>();
		TreeMap<Double, HashSet<String>> emiScore = new TreeMap<Double, HashSet<String>>();
		TreeMap<Double, HashSet<String>> diceScore = new TreeMap<Double, HashSet<String>>();
		for(String x : nab.keySet()){
			int a = calcNa(x);
			int ab = nab.get(x);
			chiScore = addToMap(chiCalc(a,b,ab,N), chiScore, x);
			emiScore = addToMap(EMICalc(a,b,ab,N), emiScore, x);
			miScore = addToMap(MICalc(a,b,ab,N), miScore, x);
			diceScore = addToMap(diceCalc(a,b,ab), diceScore, x);
		}
		System.out.println("Dice based: ");
		printTopTen(diceScore);
		System.out.println("EMI based: ");
		printTopTen(emiScore);
		System.out.println("MI based: ");
		printTopTen(miScore);
		System.out.println("Chi based: ");
		printTopTen(chiScore);
	}
	//returns a TreeMap containing the new entry passed as parameters
	public static TreeMap<Double, HashSet<String>> addToMap(double d, TreeMap<Double, HashSet<String>> map, String x){
		HashSet<String> temp;
		if(map.containsKey(d))
			temp = map.get(d);
		else
			temp = new HashSet<String>();
		temp.add(x);
		map.put(d, temp);
		return map;
	}
	//prints out the top ten keys with their mapped values inside of a treemap
	public static void printTopTen(TreeMap<Double, HashSet<String>> map){
		int i = 0;
		while(i<11){
			Entry<Double, HashSet<String>> entry = map.pollLastEntry();
			double x = entry.getKey();
			for(String y: entry.getValue()){
				if(i==11)
					break;
				else if(!y.equals("computer"))
				System.out.println("Rank: " + i++ + " Assoc: " + x + " Word: " + y);
			}
		}
	}
	//returns a hashmap mapping a string term and its idf
	public static HashMap<String, Integer> getNAVal(){
		HashMap<String, Integer> na = new HashMap<String, Integer>();
		for(String x : index.keySet())
			na.put(x,calcNa(x));
		return na;
	}
	//returns a hashmap mapping a term and its associated nab value with the term "computer"
	public static HashMap<String, Integer> getNABVal(){
		HashMap<String, Integer> nab = new HashMap<String, Integer>();
		for(String x : index.keySet())
				nab.put(x,calcNAB("computer",x));
		return nab;
	}
	/**
	 * Given a clustering of words with their stem as the key,
	 * return a new clustering of words, where each cluster is 
	 * a subcluster of a stem class, and the respective key
	 * can be something arbitrary (e.g. stem + number, such as "polic1", "polic2")
	 * 
	 */
	public static SortedMap<String, SortedSet<String>> subclusterStem2WordsMap(int meas, double T, SortedMap<String, SortedSet<String>> stem2WordsMap){
		HashSet<String> current = null;
		SortedSet<String> temp = null;
		TreeMap<String, SortedSet<String>> finClust = new TreeMap<String, SortedSet<String>>();
		TreeMap<String, HashSet<String>> subcluster = new TreeMap<String, HashSet<String>>();
		for(String x : stem2WordsMap.keySet()){
			temp = stem2WordsMap.get(x);
			if(temp.size()==1)
				finClust.put(x, temp);
			else{
				subcluster = new TreeMap<String, HashSet<String>>();
				for(String y : temp){
					current = new HashSet<String>();
					current.add(y);
					for(String z: temp){
						double calc = 0;
						switch(meas){
						case 0: calc = diceCalc(calcNa(y),calcNa(z),calcNAB(y,z));
							break;
						case 1: calc = MICalc(calcNa(y),calcNa(z),calcNAB(y,z), 3204);
							break;
						case 2: calc = EMICalc(calcNa(y),calcNa(z),calcNAB(y,z), 3204);
							break;
						case 3: calc = chiCalc(calcNa(y),calcNa(z),calcNAB(y,z), 3204);
							break;
						}	
						if(calc >= T)
							current.add(z);
						
						}
					subcluster.put(y, current);
					}
			finClust.putAll(getSubClust(x, subcluster));
			}
		}
		return finClust; 
	}
	
	public static TreeMap<String, TreeSet<String>> getSubClust(String x, TreeMap<String, HashSet<String>> map){
		int count = 1;
		TreeMap<String, TreeSet<String>> cluster = new TreeMap<String, TreeSet<String>>();
		String id = "";
		TreeSet<String> Ncurrent = null;
		HashSet<String> terms = new HashSet<String>();
		for(String term : map.keySet()){
			if(terms.contains(term))
				continue;
			else{
				Ncurrent = new TreeSet<String>();
				PriorityQueue<String> que = new PriorityQueue<String>();
				que.addAll(map.get(term));
				Ncurrent.addAll(map.get(term));
				String next = "";
				while(!que.isEmpty()){
					next = que.poll();
					for(String zeta : map.get(next)){
						if(!Ncurrent.contains(zeta)){
							que.add(zeta);
							Ncurrent.add(zeta);
						}
					}
				}
			}
			id = x+count++;
			cluster.put(id, Ncurrent);
			terms.addAll(Ncurrent);
		}
		return cluster;
	}
	
	public static double diceCalc(int na, int nb, int nab){
		double x = 2*nab;
		return x/(na+nb);
	}
	
	public static double MICalc(int na, int nb, int nab, int N){
		if(nab==0)
			return 0;
		double x = ((double)(N*nab))/(na*nb);
		return Math.log(x);
	}
	
	public static double EMICalc(int na, int nb, int nab, int N){
		if(nab == 0)
			return 0;
		double x = ((double)(N*nab))/(na*nb);
		double y = ((double)nab)/N;
		return y*Math.log(x);
	}
	
	public static double chiCalc(int na, int nb, int nab, int N){
		double x = ((double)(na*nb))/N;
		double y = nab - x;
		return y*y/x;
	}
	/**
	 * Given a map that maps a key to a set of words,
	 * return a map that maps each word in the set to the key.
	 * e.g. {"polic":{"police","policy"}} --> {"policy":"polic", "police":"polic"}
	 */
	public static SortedMap<String, String> getWord2KeyMap(SortedMap<String, SortedSet<String>> key2wordsMap){
		SortedMap<String, String> words2keyMap = new TreeMap<String, String>();
		for(String key: key2wordsMap.keySet()){
			for(String word : key2wordsMap.get(key)){
				words2keyMap.put(word, key);
			}
		}
		return words2keyMap;
	}
}
