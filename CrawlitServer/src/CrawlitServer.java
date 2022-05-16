import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

import org.tartarus.snowball.ext.englishStemmer;


//TODO:
// - either comma or ; separators
// - Only document name eg: 6969.html
// - full example:
//      ExampleWord,6969.html,420.html,69420.html


public class CrawlitServer {

    // The port number on which the server will listen for incoming connections.
    public static final int PORT = 6666;

    //create a List of url and stop words entries
    static List<String> urlList = new ArrayList<String>();
    static List<String> stopWordsList = new ArrayList<String>();

    //create a map with a key of string and value of list of strings
    static Map<String, List<String>> invertedIndex = new HashMap<String, List<String>>();

    //main method
    public static void main(String[] args) {
        System.out.println("The server started .. ");

        //get access to Files holding the index and the crawled data
        File indexFile = new File("../Indexer/clean.csv");
        File UrlListFile = new File("URLSources.txt");
        //file to hold the list of stop words
        File stopWordsFile = new File("stopWords.txt");

        //read the index file
        Scanner indexScanner = null;
        Scanner urlScanner = null;
        Scanner stopWordsScanner = null;
        try {
            indexScanner = new Scanner(indexFile);
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
            System.out.println("Error: " + e.getMessage());
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
        private final Socket socket;


        public CrawlitServerThread(Socket socket) {
            this.socket = socket;
        }

        public void run() {

            List<String> list = new ArrayList<>();

            try {
                // Get the input stream from the socket
                DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                Scanner scanner = new Scanner(inputStream);
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                PrintWriter writer = new PrintWriter(outputStream, true);

                while (scanner.hasNextLine()) {


                    //receive search query
                    String query = scanner.nextLine();
                    System.out.println("Received query from client: " + query);

                    //empty all the elements in the list
                    list.clear();

                    //Process the query
                    englishStemmer stemmer = new englishStemmer();
                    //trim and remove stop words
                    List<String> queryWords = new ArrayList<String>(Arrays.asList(query.toLowerCase().trim().split("\\s+")));


                    queryWords.removeIf(word -> stopWordsList.contains(word));


                    for(int j = 0; j < queryWords.size();j++) {
                        stemmer.setCurrent(queryWords.get(j));
                        if (stemmer.stem()) { //If the word has been Stemmed update the list
                            queryWords.set(j, stemmer.getCurrent());
                            System.out.println(stemmer.getCurrent());

                        }
                    }

                    //print the query words
                    System.out.println("Query words: " + queryWords);

                    //for every word in the query
                    for(int i = 0; i < queryWords.size(); i++) {
                        //if the word is in the inverted index
                        if(invertedIndex.containsKey(queryWords.get(i))) {
                            //get the list of urls that contain the word
                            list.addAll(invertedIndex.get(queryWords.get(i)));
                        }

                    }

                    //remove duplicates
                    list = list.stream().distinct().collect(Collectors.toList());

                    //print the list of first 10 docs
                    System.out.println("First 10 docs: " + list.subList(0, Math.min(10, list.size())));

                    //if the list is empty
                    if(list.isEmpty()) {
                        writer.println(list);
                    }
                    else {
                        //replace each list entry with the url of url list (list)
                        for(int i = 0; i < list.size(); i++) {
                            if (Integer.parseInt(list.get(i)) < urlList.size()) {
                                list.set(i, urlList.get(Integer.parseInt(list.get(i))));
                            }
                            else {
                                list.remove(i);
                            }
                        }
                    }
                    //print what is to be written to the client, first 10
                    System.out.println("List to be written to client: " + list.subList(0, Math.min(10, list.size())));

                    //print list size
                    System.out.println("List size: " + list.size());

                    writer.println(list + "\n");
                }
            }
            catch (Exception e) {
                System.out.println("Error: " + e);
            }
        }
    }

}
