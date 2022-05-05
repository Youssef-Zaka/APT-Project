import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class WebCrawler {

    //All the URLs that will be downloaded after crawler fills the HashMap
    static HashMap<String, Integer> URLMap = new HashMap<>(5000);
    static HashMap<String, Integer> checkerMap = new HashMap<>(5000);

    final static String URLFilePath = "URLs.txt";
    final static String checkerFilePath = "URLChecker.txt";
    final static String documentNumber = "documentCount.txt";

    static int THREAD_NUMBER;

    static int URLIterator = 0;
    static int documentCount = 0;

    public static void main(String[] args) throws IOException {

        /*Scanner in = new Scanner(System.in);
        System.out.println("Enter number of threads: ");
        THREAD_NUMBER = in.nextInt();*/

        File seedFile = new File("SeedSet.txt");
        Scanner URLScanner;
        documentCount = loadCrawlerState();
        if (documentCount == 0) {
            URLIterator = 0;
            try {
                URLScanner = new Scanner(seedFile);
                while (URLScanner.hasNextLine()) {

                    String currentURL = URLScanner.nextLine();

                    if (addIndex(currentURL)) documentCount++;

                }
            } catch (FileNotFoundException e) {
                System.out.println("Could not open SeedSet.txt");
                return;
            }
        }

        while (documentCount < 5000) {
            System.out.println("\nFetching documents from URL #" + URLIterator + ":");
            String documentName = "Documents\\" + URLIterator + ".html";
            File input = new File(documentName);
            Document doc = null;
            try {
                doc = Jsoup.parse(input, "UTF-8", "http://example.com/");
            } catch (Exception ignore) {
                System.out.println("Nothing more to fetch! Number of documents fetched: " + (documentCount + 1));
                return;
            }

            Elements links = doc.select("a[href]");

            int documentsFetched = 0;
            int documentsNotFetched = 0;

            long start = System.currentTimeMillis();

            for (Element link : links) {
                long end = System.currentTimeMillis();
                if ((end - start) / 1000 > 60)  //Some documents take a lot of time to be fetched, (skip those docs for this URL)
                    break;
                if (documentsFetched == 20 || documentsNotFetched == 20)      //Fetch a maximum of 20 documents per document
                    break;
                //The second condition causes the crawler to continue searching through other documents
                String currentHyperlink = link.attr("href");

                if (addIndex(currentHyperlink)) {
                    documentCount++;
                    documentsFetched++;
                    if (documentCount % 20 == 0)
                        saveCrawlerState();
                } else {
                    System.out.print(".");  //This is just an indicator that shows whether the crawler is fetching documents or not
                    documentsNotFetched++;
                }
            }
            if (documentsFetched == 0) System.out.println("\nNo new useful documents found...");
            else
                System.out.println("\nFetched " + documentsFetched + " documents, and failed to fetch " + documentsNotFetched + " documents...");
            URLIterator++;
        }
        PrintWriter fileClearer = new PrintWriter(checkerFilePath);
        fileClearer.print("");
        fileClearer.close();
        fileClearer = new PrintWriter(URLFilePath);
        fileClearer.print("");
        fileClearer.close();
        fileClearer = new PrintWriter(documentNumber);
        fileClearer.print("");
        fileClearer.close();
    }

    public static boolean addIndex(String currentURL) {
        URLConnection connection = null;
        System.setProperty("sun.net.client.defaultConnectTimeout", "4000");
        System.setProperty("sun.net.client.defaultReadTimeout", "4000");
        try {
            if (!robotSafe(new URL(currentURL))) {
                System.out.print("\nA URL has been refused by Robot");
                return false;
            }
        } catch (MalformedURLException ignored) {
        }
        String content;
        try {
            connection = new URL(currentURL).openConnection();

            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            long start = System.currentTimeMillis();
            long end;
            for (int length; (length = connection.getInputStream().read(buffer)) != -1; ) {
                result.write(buffer, 0, length);

                end = System.currentTimeMillis();
                if ((end - start) > 20000)  //Skip this document as it took a lot of time
                    return false;
            }
            content = result.toString(StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return false;
        }
        boolean taken = URLMap.containsKey(currentURL);
        if (taken) return false;
        String checker;
        try {
            checker = (content.length() + content.substring(content.length() / 4, content.length() / 4 + 20)).replace("\n", "");
            boolean oldContent = checkerMap.containsKey(checker);
            if (oldContent && Objects.equals(checkerMap.get(checker), URLMap.get(currentURL))) return false;
        } catch (Exception ignored) {
            return false;
        }
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
                URLMap.putIfAbsent(currentURL, documentCount);
                checkerMap.putIfAbsent(checker, documentCount);
                System.out.print("\nSuccessfully added document #" + documentCount + " with length " + content.length());
            } else return false;
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
                } else break;
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

    public static boolean saveCrawlerState() {
        // File Objects
        File urlFile = new File(URLFilePath);
        File checkerFile = new File(checkerFilePath);
        File documentFile = new File(documentNumber);

        BufferedWriter bfU = null;
        BufferedWriter bfC = null;
        BufferedWriter bfD = null;

        try {
            // Create new BufferedWriter for each output file
            bfU = new BufferedWriter(new FileWriter(urlFile));
            // Iterate over the Map Entries
            for (Map.Entry<String, Integer> entry : URLMap.entrySet())
                bfU.write(entry.getKey() + "}" + entry.getValue() + "\n");
            bfU.flush();

            bfC = new BufferedWriter(new FileWriter(checkerFile));
            // Iterate over the Checker Entries
            bfC.write(Integer.valueOf(URLIterator).toString() + "\n");
            String line;
            for (Map.Entry<String, Integer> entry : checkerMap.entrySet()) {
                line = entry.getKey() + "khalooda" + entry.getValue();
                line.replace("\n", "");
                bfC.write(line + "\n");
            }
            bfC.flush();

            bfD = new BufferedWriter(new FileWriter(documentFile));
            bfD.write(Integer.valueOf(documentCount).toString());

        } catch (IOException e) {
            return false;
        } finally {
            try {
                // Close Writers
                assert bfU != null;
                bfU.close();
                assert bfC != null;
                bfC.close();
                assert bfD != null;
                bfD.close();
            } catch (Exception ignored) {

            }
        }
        System.out.println("\nSaved State Successfully\n");
        return true;
    }

    public static int loadCrawlerState() {
        BufferedReader br = null;

        int docCount = 0;
        try {
            File file = new File(URLFilePath);
            br = new BufferedReader(new FileReader(file));

            String line;
            while ((line = br.readLine()) != null) {

                // split the line by }
                String[] parts = line.split("}");

                String name = parts[0].trim();
                String number = parts[1].trim();

                if (!name.equals("") && !number.equals(""))
                    URLMap.put(name, Integer.valueOf(number));
            }
        } catch (Exception ignored) {
        } finally {
            // Always close the BufferedReader
            if (br != null) {
                try {
                    br.close();
                } catch (Exception ignored) {
                }
            }
        }
        try {

            File file = new File(checkerFilePath);
            br = new BufferedReader(new FileReader(file));

            String line = br.readLine();
            URLIterator = Integer.parseInt(line);

            while ((line = br.readLine()) != null) {
                String[] parts = line.split("khalooda");

                String name = parts[0].trim();
                String number = parts[1].trim();
                if (!name.equals("") && !number.equals(""))
                    checkerMap.put(name, Integer.valueOf(number));
            }
        } catch (Exception ignored) {
        } finally {
            // Always close the BufferedReader
            if (br != null) {
                try {
                    br.close();
                } catch (Exception ignored) {
                }
            }
        }
        File file = new File(documentNumber);
        try {
            br = new BufferedReader(new FileReader(file));
            String line = br.readLine();
            docCount = Integer.parseInt(line);
        } catch (Exception ignored) {
        }
        if (URLMap.isEmpty())
            return 0;
        return docCount;
    }
}
