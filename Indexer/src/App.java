
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


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
					
					//Transform the text inside the tag to a List
					List<String> textList = new ArrayList<String>(Arrays.asList(element.ownText().toLowerCase().split("\\W+"))); 
					textList.removeAll(stopWordsList);
					
					for(int j = 0; j < textList.size();j++) {
						stemmer.setCurrent(textList.get(j));
						if(stemmer.stem()) { //If the word has been Stemmed update the list
							textList.set(j, stemmer.getCurrent());
						}
						String word = textList.get(j);
						if(word.matches("[a-zA-Z]+")) {//word has to have Alphabet characters only
							if(wordsMap.containsKey(word)) { //If there already exist a list
								if(wordsMap.get(word).containsKey(fileName)) {//If the current HTML already exists in the list just update it
									wordsMap.get(word).put(fileName, wordsMap.get(word).get(fileName).incrementTF(tag));
								}
								else {//If the current HTML doesn't exist in the list create one and insert it
									wordsMap.get(word).put(fileName, new DocInfo(fileName, tag));
								}
								
							}
							else { //If a list of HTMLs doesn't already exist Initialize one
								HashMap<String, DocInfo> docInfo = new HashMap<String, DocInfo>();
								docInfo.put(fileName, new DocInfo(fileName, tag));
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
          for (Map.Entry<String, HashMap<String, DocInfo>> entry : wordsMap.entrySet()) {

            writer.append(entry.getKey())
                  .append(',')
                  .append(entry.getValue().toString())
                  .append(eol);
          }
        } catch (IOException ex) {
          ex.printStackTrace(System.err);
        }
        System.out.println("Done");
    }
}
