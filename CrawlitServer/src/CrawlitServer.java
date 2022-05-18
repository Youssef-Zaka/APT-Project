import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

import org.tartarus.snowball.ext.englishStemmer;





public class CrawlitServer {
    static List<String> Suggestions = new ArrayList<String>();


    // The port number on which the server will listen for incoming connections.
    public static final int PORT = 6666;

    //create a List of url and stop words entries
    static List<String> urlList = new ArrayList<String>();
    static List<String> stopWordsList = new ArrayList<String>();

    //create a map with a key of string and value of list of strings
    static Map<String, List<String>> invertedIndex = new HashMap<String, List<String>>();
    static Map<String, List<DocInfo>> wordsMap = new HashMap<String, List<DocInfo>>();


    //main method
    public static void main(String[] args) {
        Suggestions.add("search engine");
        Suggestions.add("crawlit");
        System.out.println("The server started .. ");

        //get access to Files holding the index and the crawled data
        File indexFile = new File("../Indexer/clean.csv");
        File mapFile = new File("../indexer/map.csv");
        File UrlListFile = new File("URLSources.txt");
        //file to hold the list of stop words
        File stopWordsFile = new File("stopWords.txt");

        //read the index file
        Scanner indexScanner = null;
        Scanner mapScanner = null;
        Scanner urlScanner = null;
        Scanner stopWordsScanner = null;
        try {
            indexScanner = new Scanner(indexFile);
            mapScanner = new Scanner(mapFile);
            urlScanner = new Scanner(UrlListFile);
            stopWordsScanner = new Scanner(stopWordsFile);
        }
        catch (Exception e) {
            System.out.println("Error reading index file");
        }


        //get time before starting to read the index file
        long startTime = System.currentTimeMillis();

        //read the index file and create the inverted index
        while (indexScanner.hasNextLine()) {
            String line = indexScanner.nextLine();
            //trim and remove spaces
            line = line.trim();
            line = line.replaceAll("\\s+", "");
            //split the line into words
            String[] lineArray = line.split(",");
            //the first element of the array is the map key and all other elements are the values
            String key = lineArray[0];
            List<String> value = new ArrayList<String>();
            for (int i = 1; i < lineArray.length; i++) {
                value.add(lineArray[i]);
            }
            invertedIndex.put(key, value);
        }
        
        Integer wordLine = 0;
        while(mapScanner.hasNextLine()) {
        	wordLine += 1;
        	System.out.println(wordLine);
        	String line = mapScanner.nextLine();
        	String key = line.split(",")[0];
        	List<String> valuesArray = Arrays.asList(line.split(","));
        	valuesArray = valuesArray.subList(0, Math.min(1500, valuesArray.size()));
        	for (int i = 1; i < valuesArray.size(); i++) {
        		List<String> values = Arrays.asList(valuesArray.get(i).split("\\|"));
				if (!wordsMap.containsKey(key)) {
					List<DocInfo> newlist = new ArrayList<DocInfo>();
					newlist.add(new DocInfo(values));
					wordsMap.put(key, newlist);
				}
				else {
					List<DocInfo> prevList = wordsMap.get(key);
					prevList.add(new DocInfo(values));
					wordsMap.put(key, prevList);
				}
			}
        }
        while (urlScanner.hasNextLine()) {
            //add the index entry to the list after removing whitespace
            urlList.add(urlScanner.nextLine().trim());
        }

        while (stopWordsScanner.hasNextLine()) {
            //add the index entry to the list after removing whitespace
            stopWordsList.add(stopWordsScanner.nextLine().trim());
        }
        //get time after reading the index file
        long endTime = System.currentTimeMillis();
        //calculate the time it took to read the index file
        long elapsedTime = endTime - startTime;
        System.out.println("Time to read index and url files: " + elapsedTime/1000.0 + " Seconds");

        //print waiting for connection message
        System.out.println("Waiting for connection .. ");

        // Create a new server socket
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(PORT);

        }   catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }

        // Listen for incoming connections and create a new thread for each one
        while (true) {
            try {
                new CrawlitServerThread(serverSocket.accept()).start();
            }
            catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    public static class CrawlitServerThread extends Thread {
        private static final Comparator<? super DocInfo> DocComparator = null;
		private final Socket socket;


        public CrawlitServerThread(Socket socket) {
            this.socket = socket;
        }

        public void run() {

            List<String> list = new ArrayList<>();
            boolean suggestions = true;


            try {
                // Get the input stream from the socket
                DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                Scanner scanner = new Scanner(inputStream);
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                PrintWriter writer = new PrintWriter(outputStream, true);

                while (scanner.hasNextLine()) {

                    //receive search query
                    String query = scanner.nextLine();
                    Boolean quotes = false;
                    if (query.charAt(0) == '\"') {
                    	quotes = true;
                    }
                    query = query.replaceAll("[^a-zA-Z\\s]", "");
              
                    if (suggestions) {
                        //send the suggestions to the client
                        writer.println(Suggestions + "\n");
                        suggestions = false;
                        continue;
                    }

                    System.out.println("Received query from client: " + query);
                    System.out.println(quotes);
                    //add the query to the list of suggestions
                    Suggestions.add(query);

                    //empty all the elements in the list
                    list.clear();

                    //Process the query
                    englishStemmer stemmer = new englishStemmer();
                    //trim and remove stop words
                    List<String> queryWords = new ArrayList<String>(Arrays.asList(query.toLowerCase().trim().split("\\s+")));

                    queryWords.removeIf(word -> stopWordsList.contains(word));
                    List<String> stemmedWords = new ArrayList<String>(queryWords);


                    for(int j = 0; j < queryWords.size();j++) {
                        stemmer.setCurrent(queryWords.get(j));
                        if (stemmer.stem()) { //If the word has been Stemmed update the list
                            stemmedWords.set(j, stemmer.getCurrent());
                            System.out.println(stemmer.getCurrent());
                        }
                    }
                    
                    

                    //print the query words
                    System.out.println("Query words: " + queryWords);
                    System.out.println("Stemmed words: " + stemmedWords);
                    
                    List<Set<String>> docsList = new ArrayList<Set<String>>();
                    Set<String> docsSet;
                    for (int i = 0; i < stemmedWords.size(); i++) {
                    	docsSet = new HashSet<String>();
						for(DocInfo doc : wordsMap.get(stemmedWords.get(i))) {
							docsSet.add(doc.document);
						}
						docsList.add(docsSet);
					}
                    System.out.println(docsList);
                    
                    Set<String> commonDocs = docsList.get(0);
                    
                    for (int i = 1; i < docsList.size(); i++) {
						commonDocs.retainAll(docsList.get(i));
					}
                   
                    System.out.println(commonDocs);
                    List<DocInfo> consecList = new ArrayList<DocInfo>();
                    Set<String> consecutive;
                    List<Set<String>> allConsec = new ArrayList<Set<String>>();
                    
                    for (int i = 0; i < stemmedWords.size() - 1; i++) {
                    	consecutive = new HashSet<String>();
                    	for(DocInfo doc : wordsMap.get(stemmedWords.get(i))) {
                    		if(commonDocs.contains(doc.document)) {
                    			List<DocInfo> nextDocs = new ArrayList<DocInfo>(wordsMap.get(stemmedWords.get(i + 1)));
                    			for(DocInfo nextDoc : nextDocs) {
                    				if(doc.document.equals(nextDoc.document)) {
                    					for (int j = 0; j < doc.tags.size(); j++) {
											for (int k = 0; k < nextDoc.tags.size(); k++) {
												if(doc.tags.get(j).tag.equals(nextDoc.tags.get(k).tag)
												   && doc.tags.get(j).unstemmed.equals(queryWords.get(i))
												   && nextDoc.tags.get(k).unstemmed.equals(queryWords.get(i + 1))
												   && doc.tags.get(j).index + 1 == nextDoc.tags.get(k).index) {
													if(!consecutive.contains(doc.document)) {
														consecList.add(doc);
													}
													consecutive.add(doc.document);
												}
											}
										}
                    				}
                    			}
                    		}
                    	}
                    	allConsec.add(consecutive);
                    }
                    System.out.println("All Consec " + allConsec);

                    DocComparator dc = new DocComparator();
                    Set<String> consecConsec = new HashSet<String>(allConsec.get(0));
                    for (int i = 1; i < allConsec.size(); i++) {
						consecConsec.retainAll(allConsec.get(i));
					}
                    System.out.println("ConsecConsec" + consecConsec);
                    
                    List<DocInfo> allConsecList = new ArrayList<>();
                    for (int i = 0; i < consecList.size(); i++) {
						if (consecConsec.contains(consecList.get(i).document)) {
							allConsecList.add(consecList.get(i));
						}
					}
                    System.out.println("List Size " + consecList.size());
                    allConsecList.sort(dc);
                    for (int i = 0; i < allConsecList.size(); i++) {
						System.out.print(allConsecList.get(i).document + ", ");
					}
                    System.out.println();
                    
                    if(!quotes)
                    {
                    	consecList.sort(dc);
                    	allConsecList.addAll(consecList);
                    }
                    
                    List<String> docNames = new ArrayList<String>();
                    
                    for(DocInfo doc : allConsecList) {
                    	docNames.add(doc.document.split("\\.")[0]);
                    }
                    if(!quotes) {
                    	for (int i = 0; i < stemmedWords.size(); i++) {
                    		for(DocInfo doc : wordsMap.get(stemmedWords.get(i))) {
                    			String [] docName = doc.document.split("\\.");
                    			if(docName.length > 0)
                    			{
                    				docNames.add(doc.document.split("\\.")[0]);								
                    			}
                    		}
                    	}                    	
                    }
                    
                    System.out.println("Before Distinct" + docNames);
                    docNames = docNames.stream().distinct().collect(Collectors.toList());
                    System.out.println("After Distinct" + docNames);
                    
                    if(docNames.isEmpty()) {
                      writer.println(docNames);
	                  }
				    else {
				      //replace each list entry with the url of url list (list)
				      for(int i = 0; i < docNames.size(); i++) {
				          if (Integer.parseInt(docNames.get(i)) < urlList.size()) {
				        	  docNames.set(i, urlList.get(Integer.parseInt(docNames.get(i))));
				          }
				          else {
				        	  docNames.remove(i);
				          }
				      }
				    }
                    System.out.println(quotes + ", " + docNames.size());
                    System.out.println(docNames);
                    
                    writer.println(docNames + "\n");
                    
                    
                    //for every word in the query
//                    for(int i = 0; i < queryWords.size(); i++) {
//                        //if the word is in the inverted index
//                        if(invertedIndex.containsKey(queryWords.get(i))) {
//                            //get the list of urls that contain the word
//                            list.addAll(invertedIndex.get(queryWords.get(i)));
//                        }
//
//                    }

                    //remove duplicates
//                    list = list.stream().distinct().collect(Collectors.toList());
//
//                    //print the list of first 10 docs
//                    System.out.println("First 10 docs: " + list.subList(0, Math.min(10, list.size())));
//
//                    //if the list is empty
//                    if(list.isEmpty()) {
//                        writer.println(list);
//                    }
//                    else {
//                        //replace each list entry with the url of url list (list)
//                        for(int i = 0; i < list.size(); i++) {
//                            if (Integer.parseInt(list.get(i)) < urlList.size()) {
//                                list.set(i, urlList.get(Integer.parseInt(list.get(i))));
//                            }
//                            else {
//                                list.remove(i);
//                            }
//                        }
//                    }
//                    //print what is to be written to the client, first 10
//                    System.out.println("List to be written to client: " + list.subList(0, Math.min(10, list.size())));
//
//                    //print list size
//                    System.out.println("List size: " + list.size());

                    
                }
            }
            catch (Exception e) {
                System.out.println("Error: " + e);
            }
        }
    }

}

class DocComparator implements Comparator<DocInfo>{
	@Override
	public int compare(DocInfo d1, DocInfo d2) {
		return Double.compare(d2.score, d1.score);
	}
}
