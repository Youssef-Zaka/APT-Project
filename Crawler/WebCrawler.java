import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class WebCrawler {

    //All the URLs that will be downloaded after crawler fills the HashMap
    static HashMap<String, Integer> URLMap = new HashMap<>(5000);
    static HashMap<String, Integer> checkerMap = new HashMap<>(5000);
    static HashMap<String, Integer> refusedURLs = new HashMap<>(20000);
    static HashSet<String> refusedByRobotURLs = new HashSet<>(10000);

    static HashMap<Integer, Set<Integer>> relevanceMap = new HashMap<>(5000);

    final static String URLFilePath = "URLs.txt";
    final static String refusedURLsFile = "RefusedURLs.txt";
    final static String checkerFilePath = "URLChecker.txt";
    final static String documentNumber = "documentCount.txt";
    final static String iteratorNumber = "iteratorCount.txt";
    final static String URLSources = "URLSources.txt";
    final static String ThreadNumber = "ThreadNumber.txt";
    final static String relevanceGraph = "RelevanceGraph.txt";

    static int THREAD_NUMBER;

    static Integer MAX_DOC_COUNT;

    static LinkedList<Integer> documentList = new LinkedList<>();
    static LinkedList<Integer> URLList = new LinkedList<>();

    public static void main(String[] args) throws IOException {
        Thread.currentThread().setName("Main Thread");
        BufferedReader br = null;
        try {
            File file = new File(ThreadNumber);
            br = new BufferedReader(new FileReader(file));

            THREAD_NUMBER = Integer.parseInt(br.readLine());
        } catch (Exception exception) {
            Scanner in = new Scanner(System.in);
            System.out.println("Enter number of threads: ");
            try {
                THREAD_NUMBER = in.nextInt();
                if (THREAD_NUMBER > 16 || THREAD_NUMBER < 1) {
                    System.out.println("Invalid Number of threads, exiting...");
                    return;
                }

                File file = new File(ThreadNumber);
                BufferedWriter bw;
                try {
                    bw = new BufferedWriter(new FileWriter(file));
                    bw.write(Integer.valueOf(THREAD_NUMBER).toString());
                    bw.flush();
                } catch (Exception exception1) {
                    System.out.println("Failed to save the number of threads entered");
                    return;
                }
            } catch (Exception exception1) {
                System.out.println("Invalid Number of threads, exiting...");
                return;
            }
        } finally {
            // Always close the BufferedReader
            if (br != null) {
                try {
                    br.close();
                } catch (Exception ignored) {
                }
            }
        }
        MAX_DOC_COUNT = 5000 + 100 * THREAD_NUMBER;

        File seedFile = new File("SeedSet.txt");
        Scanner URLScanner;
        int documentCount = loadCrawlerState();
        if (documentCount == 0) {
            int i = 0;
            while (i < MAX_DOC_COUNT)
                documentList.add(i++);
            try {
                URLScanner = new Scanner(seedFile);
                while (URLScanner.hasNextLine()) {

                    String currentURL = URLScanner.nextLine();

                    if (addIndex(currentURL, documentCount, -1)) documentCount++;

                }
            } catch (FileNotFoundException e) {
                System.out.println("Could not open SeedSet.txt");
                return;
            }
            for (int d = 0; d < documentCount; d++)
                documentList.removeFirst();
        }

        try {
            documentList.getFirst();
        } catch (Exception ignored) {
            PrintWriter fileClearer = new PrintWriter(URLFilePath);
            fileClearer.print("");
            fileClearer.close();
            System.out.println("\nFailed to load files");
            return;
        }

        System.out.println("\nWorking with " + THREAD_NUMBER + " Threads...");

        Thread[] crawlerThreads = new Thread[THREAD_NUMBER];

        for (int k = 0; k < THREAD_NUMBER; k++) {
            crawlerThreads[k] = new Thread(() -> {
                System.out.println(Thread.currentThread().getName() + " is UP!");
                while (true) {
                    int threadURLIterator;
                    synchronized (URLList) {
                        if (URLList.size() == 0)
                            continue;
                        threadURLIterator = URLList.getFirst();
                        URLList.removeFirst();
                    }
                    int threadDocCount, oldDocCount, remainingDocCount;
                    synchronized (documentList) {
                        threadDocCount = documentList.getFirst();
                        documentList.removeFirst();
                        remainingDocCount = documentList.size();
                    }
                    if (remainingDocCount < MAX_DOC_COUNT - 5000)
                        break;

                    oldDocCount = threadDocCount;

                    //System.out.println("\n" + Thread.currentThread().getName() + " fetching documents from URL #" + threadURLIterator + ":");
                    String documentName = "Documents\\" + (threadURLIterator / 1000 + 1) + "\\" + threadURLIterator + ".html";
                    File input = new File(documentName);
                    Document doc;
                    try {
                        doc = Jsoup.parse(input, "UTF-8", "http://example.com/");
                    } catch (Exception ignore) {
                        System.out.println("Nothing more to fetch! Number of documents fetched: " + (threadDocCount + 1));
                        return;
                    }

                    Elements links = doc.select("a[href]");

                    int j = 0;
                    while (j != links.size()) {
                        //The second condition causes the crawler to continue searching through other documents
                        String currentHyperlink = links.get(j).attr("href");
                        if (!refusedURLs.containsKey(currentHyperlink) && addIndex(currentHyperlink, threadDocCount, threadURLIterator)) {
                            threadDocCount++;
                            if (threadDocCount % 25 == 0) {
                                saveCrawlerState();
                                saveURLs();
                                saveRelevanceGraph();
                            }
                            if (oldDocCount != threadDocCount) {
                                synchronized (documentList) {
                                    if (documentList.size() == 0)
                                        continue;
                                    threadDocCount = documentList.getFirst();
                                    documentList.removeFirst();
                                    remainingDocCount = documentList.size();
                                }
                                oldDocCount = threadDocCount;
                            }
                        }

                        if (refusedURLs.containsKey(currentHyperlink)) {
                            int docCount = refusedURLs.get(currentHyperlink);
                            if (docCount > -1)
                                synchronized (relevanceMap) {
                                    relevanceMap.putIfAbsent(threadURLIterator, new HashSet<>());
                                    relevanceMap.get(threadURLIterator).add(docCount);
                                }
                        }

                        j++;
                    }
                    synchronized (documentList) {
                        documentList.addFirst(oldDocCount);
                    }
                }
                System.out.print("\n" + Thread.currentThread().getName() + " has finished!");
            });
            crawlerThreads[k].setName("Thread " + Integer.valueOf(k + 1).toString());
            crawlerThreads[k].start();
        }

        while (documentList.getFirst() < 4550) {
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException ignored) {

            }
            System.out.println("\n\n" + Thread.currentThread().getName() + ": Crawler Progress = " + (100 * documentList.getFirst() / 5000) + "%");
        }

        for (int j = 0; j < THREAD_NUMBER; j++) {
            try {
                crawlerThreads[j].join();
            } catch (Exception ignored) {
            }
        }

        checkMissingDoc();

        saveURLs();
        saveRelevanceGraph();

        PrintWriter fileClearer = new PrintWriter(checkerFilePath);
        fileClearer.print("");
        fileClearer.close();
        fileClearer = new PrintWriter(documentNumber);
        fileClearer.print("");
        fileClearer.close();
        fileClearer = new PrintWriter(iteratorNumber);
        fileClearer.print("");
        fileClearer.close();
    }

    public static boolean addIndex(String currentURL, int threadDocCount, int threadFetchingDoc) {
        URLConnection connection;
        System.setProperty("sun.net.client.defaultConnectTimeout", "2000");
        System.setProperty("sun.net.client.defaultReadTimeout", "2000");

        boolean refRob;
        synchronized (refusedByRobotURLs) {
            refRob = refusedByRobotURLs.contains(currentURL);
        }
        if (refRob) {
            System.out.print("\nURL: " + currentURL + " has been refused before by Robot");
            return false;
        }

        try {
            if (!robotSafe(new URL(currentURL))) {
                System.out.print("\nURL: " + currentURL + " has been refused by Robot");
                synchronized (refusedByRobotURLs) {
                    refusedByRobotURLs.add(currentURL);
                }
                return false;
            }
        } catch (MalformedURLException ignored) {
        }
        String content;
        try {
            connection = new URL(currentURL).openConnection();

            Scanner s = new Scanner(connection.getInputStream()).useDelimiter("\\A");
            content = s.hasNext() ? s.next() : "";
        } catch (Exception ex) {
            synchronized (refusedURLs) {
                refusedURLs.putIfAbsent(currentURL, -1);
            }
            return false;
        }

        boolean taken;
        int docCount = -2;
        synchronized (URLMap) {
            taken = URLMap.containsKey(currentURL);
            if (taken)
                docCount = URLMap.get(currentURL);
        }
        if (taken) {
            synchronized (refusedURLs) {
                refusedURLs.putIfAbsent(currentURL, docCount);
            }
            return false;
        }
        String checker;
        try {
            checker = (content.length() + content.substring(content.length() / 2, content.length() / 2 + 30)).replace("\n", "");
            boolean oldContent;
            synchronized (checkerMap) {
                oldContent = checkerMap.containsKey(checker);
                boolean invalid;
                synchronized (URLMap) {
                    invalid = oldContent && Objects.equals(checkerMap.get(checker), URLMap.get(currentURL));
                }
                if (invalid) {
                    synchronized (refusedURLs) {
                        refusedURLs.putIfAbsent(currentURL, docCount);
                    }
                    return false;
                }
            }
        } catch (Exception ignored) {
            synchronized (refusedURLs) {
                refusedURLs.putIfAbsent(currentURL, -1);
            }
            return false;
        }
        String documentName = "Documents\\" + (threadDocCount / 1000 + 1) + "\\" + threadDocCount + ".html";
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
                synchronized (URLMap) {
                    URLMap.putIfAbsent(currentURL, threadDocCount);
                }
                synchronized (checkerMap) {
                    checkerMap.putIfAbsent(checker, threadDocCount);
                }
                System.out.print("\n" + Thread.currentThread().getName() + " successfully added document #" + threadDocCount + " with length " + content.length() + " ");
            } else {
                synchronized (refusedURLs) {
                    refusedURLs.putIfAbsent(currentURL, -1);
                }
                return false;
            }
        } catch (Exception e) {
            synchronized (refusedURLs) {
                refusedURLs.putIfAbsent(currentURL, -1);
            }
            return false;
        }
        synchronized (URLList) {
            URLList.add(threadDocCount);
        }
        if (threadFetchingDoc > -1)
            synchronized (relevanceMap) {
                relevanceMap.putIfAbsent(threadFetchingDoc, new HashSet<>());
                relevanceMap.get(threadFetchingDoc).add(threadDocCount);
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

    public static void saveCrawlerState() {
        // File Objects
        File urlFile = new File(URLFilePath);
        File checkerFile = new File(checkerFilePath);
        File documentFile = new File(documentNumber);
        File iteratorFile = new File(iteratorNumber);

        BufferedWriter bfU = null;
        BufferedWriter bfC = null;
        BufferedWriter bfD = null;
        BufferedWriter bfE;

        try {
            // Create new BufferedWriter for each output file
            bfU = new BufferedWriter(new FileWriter(urlFile));
            // Iterate over the Map Entries
            synchronized (URLMap) {
                for (Map.Entry<String, Integer> entry : URLMap.entrySet())
                    bfU.write(entry.getKey() + "}" + entry.getValue() + "\n");
                bfU.flush();

                bfC = new BufferedWriter(new FileWriter(checkerFile));
                // Iterate over the Checker Entries
                //bfC.write(Integer.valueOf(URLIterator).toString() + "\n");
                String line;
                synchronized (checkerMap) {
                    for (Map.Entry<String, Integer> entry : checkerMap.entrySet()) {
                        line = entry.getKey() + "khalooda" + entry.getValue();
                        line.replace("\n", "");
                        bfC.write(line + "\n");
                    }
                    bfC.flush();

                    bfD = new BufferedWriter(new FileWriter(documentFile));
                    //bfD.write(Integer.valueOf(documentCount).toString());
                    synchronized (documentList) {
                        for (Integer docNum : documentList)
                            bfD.write(Integer.valueOf(docNum).toString() + "\n");
                        bfD.flush();

                        bfE = new BufferedWriter(new FileWriter(iteratorFile));
                        synchronized (URLList) {
                            for (Integer iteratorNum : URLList)
                                bfE.write(Integer.valueOf(iteratorNum).toString() + "\n");
                            bfE.flush();
                        }
                    }
                }

            }

        } catch (IOException e) {
            return;
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
        System.out.print("\n" + Thread.currentThread().getName() + " saved the crawler state successfully");
    }

    public static int loadCrawlerState() {
        BufferedReader br = null;

        int docCount;
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
        if (URLMap.isEmpty())
            return 0;

        try {
            File file = new File(checkerFilePath);
            br = new BufferedReader(new FileReader(file));

            String line;
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
            String line;
            while ((line = br.readLine()) != null) {
                documentList.add(Integer.parseInt(line));
            }

        } catch (Exception ignored) {
        }
        docCount = MAX_DOC_COUNT - documentList.size();

        file = new File(iteratorNumber);
        try {
            br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                URLList.add(Integer.parseInt(line));
            }

        } catch (Exception ignored) {
        }

        file = new File(refusedURLsFile);
        try {
            br = new BufferedReader(new FileReader(file));

            String line;
            while ((line = br.readLine()) != null) {

                // split the line by }
                String[] parts = line.split("}");

                String name = parts[0].trim();
                String number = parts[1].trim();

                if (!name.equals("") && !number.equals(""))
                    refusedURLs.put(name, Integer.valueOf(number));
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

        file = new File(relevanceGraph);
        try {
            br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                int starty = 0;
                int endy = 0;
                Integer doc = 0;
                for (char c : line.toCharArray()) {
                    if (c == ',') {
                        doc = Integer.parseInt(line.substring(starty, endy));
                        relevanceMap.putIfAbsent(doc, new HashSet<>());
                        starty = endy + 1;
                    } else if (c == '-') {
                        relevanceMap.get(doc).add(Integer.parseInt(line.substring(starty, endy)));
                        starty = endy + 1;
                    }
                    endy++;
                }
            }
        } catch (Exception ignored) {
        }


        return docCount;
    }

    public static void saveURLs() {
        // File Objects
        File urlFile = new File(URLSources);
        File urlFile2 = new File("URLSources2.txt");

        BufferedWriter bfU = null;
        HashMap<Integer, String> ReverseMap;
        String [] reverseList = new String[6000];
        synchronized (URLMap) {
            ReverseMap = new HashMap<>(URLMap.size());
        }
        Arrays.fill(reverseList, "\n");
        try {
            synchronized (URLMap) {
                for (Map.Entry<String, Integer> entry : URLMap.entrySet()) {
                    ReverseMap.put(entry.getValue(), entry.getKey());

                    reverseList[entry.getValue()] = entry.getKey();
                }
            }
            bfU = new BufferedWriter(new FileWriter(urlFile));
            for (Map.Entry<Integer, String> entry : ReverseMap.entrySet())
                bfU.write(entry.getValue() + "\n");
            bfU.flush();

            bfU = new BufferedWriter(new FileWriter(urlFile2));
            for (String url : reverseList) {
                if (!url.equals("\n"))
                    bfU.write(url + "\n");
                else
                    bfU.write("\n");
            }
            bfU.flush();
        } catch (IOException e) {
            return;
        } finally {
            try {
                // Close Writers
                assert bfU != null;
                bfU.close();
            } catch (Exception ignored) {
            }
        }

        urlFile = new File(refusedURLsFile);

        bfU = null;
        try {
            bfU = new BufferedWriter(new FileWriter(urlFile));
            synchronized (refusedURLs) {
                for (Map.Entry<String, Integer> entry : refusedURLs.entrySet())
                    bfU.write(entry + "}" + entry.getValue() + "\n");
            }
            bfU.flush();
        } catch (IOException e) {
            return;
        } finally {
            try {
                // Close Writers
                assert bfU != null;
                bfU.close();
            } catch (Exception ignored) {
            }
        }
        System.out.print("\n" + Thread.currentThread().getName() + " saved URLs successfully");
    }

    public static void saveRelevanceGraph() {
        // File Objects
        File relevanceFile = new File(relevanceGraph);

        BufferedWriter bfU = null;
        try {
            bfU = new BufferedWriter(new FileWriter(relevanceFile));
            synchronized (relevanceMap) {
                for (Map.Entry<Integer, Set<Integer>> entry : relevanceMap.entrySet()) {
                    bfU.write(entry.getKey().toString() + ",");
                    for (Integer k : entry.getValue())
                        bfU.write(k + "-");
                    bfU.write("\n");
                }
            }
            bfU.flush();
        } catch (IOException e) {
            return;
        } finally {
            try {
                // Close Writers
                assert bfU != null;
                bfU.close();
            } catch (Exception ignored) {
            }
        }
        System.out.print("\n" + Thread.currentThread().getName() + " saved relevance graph file successfully");
    }

    public static void checkMissingDoc() {
        int documentNumber = 0;
        System.out.print("\nChecking for missing documents");
        while (documentNumber < 5000) {
            String documentName = "Documents\\" + (documentNumber / 1000 + 1) + "\\" + documentNumber + ".html";
            try {
                File input = new File(documentName);
                if (input.length() > 0)
                    documentNumber++;
                else {
                    int threadURLIterator = URLList.getFirst();

                    documentName = "Documents\\" + (threadURLIterator / 1000 + 1) + "\\" + threadURLIterator + ".html";
                    Document doc = null;
                    try {
                        doc = Jsoup.parse(input, "UTF-8", "http://example.com/");
                    } catch (Exception ignore) {
                    }

                    assert doc != null;
                    Elements links = doc.select("a[href]");
                    int j = 0;
                    while (j != links.size()) {
                        //The second condition causes the crawler to continue searching through other documents
                        String currentHyperlink = links.get(j++).attr("href");
                        if (!refusedURLs.containsKey(currentHyperlink) && addIndex(currentHyperlink, documentNumber, threadURLIterator)) {
                            break;
                        }
                    }
                    documentNumber++;
                }
            } catch (Exception e) {
                int threadURLIterator = URLList.getFirst();

                documentName = "Documents\\" + (threadURLIterator / 1000 + 1) + "\\" + threadURLIterator + ".html";
                File input = new File(documentName);
                Document doc = null;
                try {
                    doc = Jsoup.parse(input, "UTF-8", "http://example.com/");
                } catch (Exception ignore) {
                }

                assert doc != null;
                Elements links = doc.select("a[href]");
                int j = 0;
                while (j != links.size()) {
                    //The second condition causes the crawler to continue searching through other documents
                    String currentHyperlink = links.get(j++).attr("href");
                    if (!refusedURLs.containsKey(currentHyperlink) && addIndex(currentHyperlink, documentNumber, threadURLIterator)) {
                        break;
                    }
                }
                documentNumber++;
            }
        }
        System.out.print("\nFinished checking");
    }

}
