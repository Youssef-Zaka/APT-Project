import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;



//TODO:
// - either comma or ; separators
// - Only document name eg: 6969.html
// - full example:
//      ExampleWord,6969.html,420.html,69420.html


public class CrawlitServer {

    // The port number on which the server will listen for incoming connections.
    public static final int PORT = 6666;

    //main method
    public static void main(String[] args) {
        System.out.println("The server started .. ");

        //get access to Files holding the index and the crawled data
        File indexFile = new File("../Indexer/map.csv");
        //File UrlListFile = new File("../Indexer/urlList.txt");

        //read the index file
        Scanner indexScanner = null;
        try {
            indexScanner = new Scanner(indexFile);
        }
        catch (Exception e) {
            System.out.println("Error reading index file");
        }
        //create a List of index entries
        List<String> indexList = new ArrayList<String>();
        int count = 0;

        //get time before starting to read the index file
        long startTime = System.currentTimeMillis();
        while (indexScanner.hasNextLine()) {
            //add the index entry to the list after removing whitespace and space characters
            indexList.add(indexScanner.nextLine().replaceAll("\\s+", "").trim());
        }
        //get time after reading the index file
        long endTime = System.currentTimeMillis();
        //calculate the time it took to read the index file
        long elapsedTime = endTime - startTime;
        System.out.println("Time to read index file: " + elapsedTime + " milliseconds");

        //print first 100 index entries
        for (int i = 0; i < 100; i++) {
            System.out.println(indexList.get(i));
        }



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
            //assign a value to list
            list.add("http://www.google.com");
            list.add("http://www.yahoo.com");
            list.add("http://www.bing.com");
            list.add("http://www.facebook.com");
            list.add("http://www.twitter.com");
            list.add("http://www.linkedin.com");
            list.add("http://www.youtube.com");
            list.add("http://www.wikipedia.com");
            list.add("http://www.amazon.com");
            list.add("http://www.ebay.com");
            //add new unique links to the list
            list.add("http://stackoverflow.com");
            list.add("http://github.com");
            list.add("http://quora.com");
            list.add("http://reddit.com");
            list.add("http://wikipedia.org");
            try {
                // Get the input stream from the socket
                DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                Scanner scanner = new Scanner(inputStream);
                DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
                PrintWriter writer = new PrintWriter(outputStream, true);

                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    System.out.println("Received Message from client: " + line);
                    // Send the line to the client but add Zaka: to the beginning
                    writer.println(list + "\n");
                }

            }
            catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

}