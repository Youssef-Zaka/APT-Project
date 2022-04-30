import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class WebCrawler {

    //All the URLs that will be downloaded after crawler fills the HashMap
    static ArrayList<String> URLs = new ArrayList<>(5000);
    static HashMap<String, String> URLMap = new HashMap<>(5000);
    static HashMap<String, ArrayList<String>> URLRobotMap = new HashMap<>(5000);
    static ArrayList<String> URLContents = new ArrayList<>(5000);

    public static void main(String[] args) throws IOException {

        File seedFile = new File("SeedSet.txt");
        Scanner URLScanner;
        int documentCount = 0;
        try {
            URLScanner = new Scanner(seedFile);
            while (URLScanner.hasNextLine()) {

                String currentURL = URLScanner.nextLine();

                if (addIndex(currentURL, documentCount))
                    documentCount++;

            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int URLIterator = 0;

        while (true) {
            if (URLIterator == 5000)
                break;
            System.out.println(URLIterator + ":");
            String documentName = "Documents\\" + URLIterator++ + ".html";
            File input = new File(documentName);
            Document doc = Jsoup.parse(input, "UTF-8", "http://example.com/");

            Elements links = doc.select("a[href]");

            for (Element e : links) {
                String currentHyperlink = e.attr("href");

                if (addIndex(currentHyperlink, documentCount))
                    documentCount++;
            }
        }
    }

    public static boolean addIndex(String currentURL, int documentCount) {
        URLConnection connection;
        String content;
        try {
            connection = new URL(currentURL).openConnection();

            Scanner documentScanner = new Scanner(connection.getInputStream());
            documentScanner.useDelimiter("\\Z");

            content = documentScanner.next();
            documentScanner.close();
        } catch (Exception ex) {
            return false;
        }
            boolean taken = URLMap.containsKey(currentURL);
            if (taken) return false;
            boolean oldContent = URLMap.containsValue(content);
            if (oldContent) return false;
            URLMap.putIfAbsent(currentURL, content);
            System.out.println(content.length());

            URLs.add(currentURL);
            String documentName = "Documents\\" + documentCount + ".html";
            try {
                File index = new File(documentName);
                index.createNewFile();
                try {
                    FileWriter myWriter = new FileWriter(documentName);
                    myWriter.write(content);
                    myWriter.close();
                    System.out.println("Successfully created document.");
                } catch (IOException e) {
                    System.out.println("An error occurred.");
                    e.printStackTrace();
                }
                URLContents.add(content);

            } catch (Exception e) {
                return false;
            }
            return true;
    }
}
