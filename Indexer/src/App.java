
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import org.tartarus.snowball.ext.englishStemmer;



public class App {
    public static void main(String[] args) throws Exception {
    	File stopWordsFile = new File("stopWords.txt");
    	Scanner myReader = new Scanner(stopWordsFile);
    	List<String> stopWordsList = new ArrayList<String>();
    	
    	while (myReader.hasNextLine()) {
    		stopWordsList.add(myReader.next());
        }
        myReader.close();
    	
        String [] files = {"1","2","3","4","5","6"}; 
        HashMap<String, HashMap<String, DocInfo>> wordsMap = new HashMap<String, HashMap<String, DocInfo>>();
        Set<String> tags = new HashSet<String>();
        
        for (int i = 0; i < files.length; i++) {			
		
	        File dir = new File("Documents/" + files[i]);
	        File[] directoryListing = dir.listFiles();
	        if (directoryListing != null) {
	          for (File input : directoryListing) { //read all the HTMLs in the directory
	        	System.out.println(input.getName());
	        	String fileName = input.getName();
				Document doc = Jsoup.parse(input, "UTF-8", "http://example.com/");
				
				Elements bodyElements = doc.body().select("*"); //Get all the elements of the HTML
				
				englishStemmer stemmer = new englishStemmer();
			
				
				for (Element element : bodyElements) {	//loop on all elements
					String tag = element.tagName(); //tag (a, div, p ,...)
					tags.add(tag);
					//Transform the text inside the tag to a List
					List<String> textList = new ArrayList<String>(Arrays.asList(element.ownText().toLowerCase().split("\\W+"))); 
					textList.removeAll(stopWordsList);
					
					for(int j = 0; j < textList.size();j++) {
						String unstemmed = textList.get(j);
						stemmer.setCurrent(unstemmed);
						if(stemmer.stem()) { //If the word has been Stemmed update the list
							textList.set(j, stemmer.getCurrent());
						}
						String word = textList.get(j);
						if(word.matches("[a-zA-Z]+")) {//word has to have Alphabet characters only
							if(wordsMap.containsKey(word)) { //If there already exist a list
								if(wordsMap.get(word).containsKey(fileName)) {//If the current HTML already exists in the list just update it
									wordsMap.get(word).put(fileName, wordsMap.get(word).get(fileName).incrementTF(tag, unstemmed, j));
								}
								else {//If the current HTML doesn't exist in the list create one and insert it
									wordsMap.get(word).put(fileName, new DocInfo(fileName, tag, unstemmed, j));
								}
								
							}
							else { //If a list of HTMLs doesn't already exist Initialize one
								HashMap<String, DocInfo> docInfo = new HashMap<String, DocInfo>();
								docInfo.put(fileName, new DocInfo(fileName, tag, unstemmed, j));
								wordsMap.put(word, docInfo);
							}
						}
					}
					String text = String.join(" ", textList);
					element.text(text);
				}
		      }
		    }
       }
        String eol = System.getProperty("line.separator");
    	
        try (Writer writer = new FileWriter("map.csv")) {
        	Writer cleanCSV = new FileWriter("clean.csv");
        	DocComparator dc = new DocComparator();
          for (Map.Entry<String, HashMap<String, DocInfo>> entry : wordsMap.entrySet()) {
        	  List<DocInfo> valueHashMap = new ArrayList<DocInfo>(entry.getValue().values());
        	  valueHashMap.sort(dc);
        	  List<String> keyStrings = new ArrayList<String>();
        	  for (DocInfo docInfo : valueHashMap) {
        		  keyStrings.add(docInfo.document.split("\\.")[0]);
        	  }
        	  String valueString = valueHashMap.toString();
        	  
        	  String keyString = keyStrings.toString();
        	  
            writer.append(entry.getKey())
                  .append(',')
                  .append(valueString.substring(1, valueString.length() - 1))
                  .append(eol);
            cleanCSV.append(entry.getKey())
		            .append(',')
		            .append(keyString.substring(1, keyString.length() - 1))
		            .append(eol);
          }
          cleanCSV.close();
        } catch (IOException ex) {
          ex.printStackTrace(System.err);
        }
        System.out.println("Done");
        FileWriter tagsFile = new FileWriter ("tags.txt");
        tagsFile.write(tags.toString());
        tagsFile.close();
    }
}

class DocComparator implements Comparator<DocInfo>{
	@Override
	public int compare(DocInfo d1, DocInfo d2) {
		return d1.score > d2.score ? -1 : d1.score == d2.score ? 0 : 1;
	}
}

