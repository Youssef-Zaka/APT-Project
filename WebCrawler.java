import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;
import java.util.Scanner;

public class WebCrawler {

    //All the URLs that will be downloaded after crawler fills the HashMap
    static ArrayList<String> URLs = new ArrayList<>(5000);
    static HashMap<String, String> URLMap = new HashMap<>(5000);
    static ArrayList<String> URLContents = new ArrayList<>(5000);

    static int THREAD_NUMBER;

    public static void main(String[] args) throws IOException {

        /*Scanner in = new Scanner(System.in);
        System.out.println("Enter number of threads: ");
        THREAD_NUMBER = in.nextInt();*/

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
            System.out.println("No could not open SeedSet.txt");
            return;
        }

        int URLIterator = 0;

        while (documentCount < 5000) {
            System.out.println("\nFetching documents from URL #" + URLIterator + ":");
            String documentName = "Documents\\" + URLIterator++ + ".html";
            File input = new File(documentName);
            Document doc = Jsoup.parse(input, "UTF-8", "http://example.com/");

            Elements links = doc.select("a[href]");

            int documentsFetched = 0;

            for (Element link : links) {
                if (documentsFetched == 10)      //Fetch a maximum of 10 documents per document
                    break;
                String currentHyperlink = link.attr("href");

                if (addIndex(currentHyperlink, documentCount)) {
                    documentCount++;
                    documentsFetched++;
                }
            }
        }
    }

    public static boolean addIndex(String currentURL, int documentCount) {
        URLConnection connection;
        try {
            if (!robotSafe(new URL(currentURL)))
                return false;
        } catch (MalformedURLException e) {
            return false;
        }
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

        String documentName = "Documents\\" + documentCount + ".html";
        try {
            File index = new File(documentName);
            index.createNewFile();
            try {
                FileWriter myWriter = new FileWriter(documentName);
                myWriter.write(content);
                myWriter.close();
            } catch (IOException e) {
                System.out.println("An error occurred.");
                e.printStackTrace();
            }

            Document doc = Jsoup.parse(index, "UTF-8", "http://example.com/");
            String docLanguage = Objects.requireNonNull(doc.select("html").first()).attr("lang");

            if (docLanguage.contains("en")) {
                //This condition prevents the crawler (crawler thread) from generating html documents
                URLs.add(currentURL);
                URLMap.putIfAbsent(currentURL, content);
                System.out.println("Successfully added document #" + documentCount + " with length " + content.length());
                URLContents.add(content);
            } else
                return false;
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public static class RobotRule {
        public String userAgent;
        public String rule;

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            String NEW_LINE = System.getProperty("line.separator");
            result.append(this.getClass().getName()).append(" Object {").append(NEW_LINE);
            result.append("   userAgent: ").append(this.userAgent).append(NEW_LINE);
            result.append("   rule: ").append(this.rule).append(NEW_LINE);
            result.append("}");
            return result.toString();
        }
    }

    public static boolean robotSafe(URL url) {
        String strHost = url.getHost();

        String strRobot = "http://" + strHost + "/robots.txt";
        URL urlRobot;
        try {
            urlRobot = new URL(strRobot);
        } catch (MalformedURLException e) {
            // something weird is happening, so don't trust it
            return false;
        }

        StringBuilder strCommands;
        try {
            InputStream urlRobotStream = urlRobot.openStream();
            byte[] b = new byte[1000];
            int numRead;
            strCommands = new StringBuilder("");
            while (true) {
                numRead = urlRobotStream.read(b);
                if (numRead != -1) {
                    String newCommands = new String(b, 0, numRead);
                    strCommands.append(newCommands);
                }
                else
                    break;
            }
            urlRobotStream.close();
        } catch (IOException e) {
            return true; // if there is no robots.txt file, it is OK to search
        }

        if (strCommands.toString().toLowerCase().contains("disallow")) // if there are no "disallow" values, then they are not blocking anything.
        {
            String[] split = strCommands.toString().split("\n");
            ArrayList<RobotRule> robotRules = new ArrayList<>();
            String mostRecentUserAgent = null;
            for (String s : split) {
                String line = s.trim();
                if (line.toLowerCase().startsWith("user-agent")) {
                    int start = line.indexOf(":") + 1;
                    int end = line.length();
                    mostRecentUserAgent = line.substring(start, end).trim();
                } else if (line.toLowerCase().startsWith("disallow")) {
                    if (mostRecentUserAgent != null) {
                        RobotRule r = new RobotRule();
                        r.userAgent = mostRecentUserAgent;
                        int start = line.indexOf(":") + 1;
                        int end = line.length();
                        r.rule = line.substring(start, end).trim();
                        robotRules.add(r);
                    }
                }
            }

            for (RobotRule robotRule : robotRules) {
                String path = url.getPath();
                if (robotRule.rule.length() == 0) return true; // allows everything if BLANK
                if (robotRule.rule.equals("/")) return false;       // allows nothing if /

                if (robotRule.rule.length() <= path.length()) {
                    String pathCompare = path.substring(0, robotRule.rule.length());
                    if (pathCompare.equals(robotRule.rule)) return false;
                }
            }
        }
        return true;
    }
}
