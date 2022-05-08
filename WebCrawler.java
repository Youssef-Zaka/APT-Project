import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

public class WebCrawler {

    //All the URLs that will be downloaded after crawler fills the HashMap
    static HashMap<String, Integer> URLMap = new HashMap<>(5000);
    static HashMap<String, Integer> checkerMap = new HashMap<>(5000);

    final static String URLFilePath = "URLs.txt";
    final static String checkerFilePath = "URLChecker.txt";
    final static String documentNumber = "documentCount.txt";
    final static String iteratorNumber = "iteratorCount.txt";
    final static String URLSources = "URLSources.txt";
    final static String ThreadNumber = "ThreadNumber.txt";

    static int THREAD_NUMBER;

    static Integer MAX_DOC_COUNT;

    static LinkedList<Integer> documentList = new LinkedList<>();
    static LinkedList<Integer> URLList = new LinkedList<>();

    public static void main(String[] args) throws IOException {
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
                if (THREAD_NUMBER > 16) {
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

                    if (addIndex(currentURL, documentCount)) documentCount++;

                }
            } catch (FileNotFoundException e) {
                System.out.println("Could not open SeedSet.txt");
                return;
            }
        }
        System.out.println("\nWorking with " + THREAD_NUMBER + " Threads...");

        Thread[] crawlerThreads = new Thread[THREAD_NUMBER];

        for (int k = 0; k < THREAD_NUMBER; k++) {
            crawlerThreads[k] = new Thread(() -> {
                System.out.println(Thread.currentThread().getName() + " Up!");
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

                    int documentsFetched = 0;
                    int documentsNotFetched = 0;

                    long start = System.currentTimeMillis();

                    int j = 0;
                    while (true) {
                        if (j == links.size())
                            break;
                        long end = System.currentTimeMillis();
                        if ((end - start) / 1000 > 60)  //Some documents take a lot of time to be fetched, (skip those docs for this URL)
                            break;
                        if (documentsFetched == 100 || documentsNotFetched == 100)      //Fetch a maximum of 100 documents per document
                            break;
                        //The second condition causes the crawler to continue searching through other documents
                        String currentHyperlink = links.get(j).attr("href");
                        if (addIndex(currentHyperlink, threadDocCount)) {
                            threadDocCount++;
                            documentsFetched++;
                            if (threadDocCount % 20 == 0)
                                saveCrawlerState();
                            if (threadDocCount % 50 == 0)
                                saveURLs();
                            if (oldDocCount != threadDocCount) {
                                synchronized (documentList) {
                                    if (documentList.size() == 0)
                                        continue;
                                    threadDocCount = documentList.getFirst();
                                    documentList.removeFirst();
                                }
                                oldDocCount = threadDocCount;
                            }

                        } else {
                            //System.out.print(".");  //This is just an indicator that shows whether the crawler is fetching documents or not
                            documentsNotFetched++;
                        }
                        j++;
                    }
                    synchronized (documentList) {
                        documentList.addFirst(oldDocCount);
                    }
                }
            });
            crawlerThreads[k].setName("Thread " + Integer.valueOf(k + 1).toString());
            crawlerThreads[k].start();
        }

        for (int j = 0; j < THREAD_NUMBER; j++) {
            try {
                crawlerThreads[j].join();
            } catch (Exception ignored) {
                System.out.println(j);
            }
        }


        saveURLs();
        PrintWriter fileClearer = new PrintWriter(checkerFilePath);
        fileClearer.print("");
        fileClearer.close();
        fileClearer = new PrintWriter(URLFilePath);
        fileClearer.print("");
        fileClearer.close();
        fileClearer = new PrintWriter(documentNumber);
        fileClearer.print("");
        fileClearer.close();
        fileClearer = new PrintWriter(iteratorNumber);
        fileClearer.print("");
        fileClearer.close();
    }

    public static boolean addIndex(String currentURL, int threadDocCount) {
        URLConnection connection;
        System.setProperty("sun.net.client.defaultConnectTimeout", "2000");
        System.setProperty("sun.net.client.defaultReadTimeout", "2000");
        try {
            if (!robotSafe(new URL(currentURL))) {
                System.out.print("\nURL: " + currentURL +" has been refused by Robot");
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
            return false;
        }
        boolean taken;
        synchronized (URLMap) {
            taken = URLMap.containsKey(currentURL);
        }
        if (taken) return false;
        String checker;
        try {
            checker = (content.length() + content.substring(content.length() / 2, content.length() / 2 + 30)).replace("\n", "");
            boolean oldContent;
            synchronized (checkerMap) {
                oldContent = checkerMap.containsKey(checker);
                synchronized (URLMap) {
                    if (oldContent && Objects.equals(checkerMap.get(checker), URLMap.get(currentURL))) return false;
                }
            }
        } catch (Exception ignored) {
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
            } else return false;
        } catch (Exception e) {
            return false;
        }
        synchronized (URLList) {
            URLList.add(threadDocCount);
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
                            bfD.write(Integer.valueOf(docNum).toString()+"\n");
                        bfD.flush();

                        bfE = new BufferedWriter(new FileWriter(iteratorFile));
                        synchronized (URLList) {
                            for (Integer iteratorNum : URLList)
                                bfE.write(Integer.valueOf(iteratorNum).toString()+"\n");
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
        System.out.println("\n\n" + Thread.currentThread().getName() +" saved the crawler state successfully");
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

        if (URLMap.isEmpty())
            return 0;
        return docCount;
    }

    public static void saveURLs() {
        // File Objects
        File urlFile = new File(URLSources);

        BufferedWriter bfU = null;
        HashMap<Integer, String> ReverseMap;
        synchronized (URLMap) {
            ReverseMap = new HashMap<>(URLMap.size());
        }
        try {
            synchronized (URLMap) {
                for (Map.Entry<String, Integer> entry : URLMap.entrySet())
                    ReverseMap.put(entry.getValue(), entry.getKey());
            }
            bfU = new BufferedWriter(new FileWriter(urlFile));
            for (Map.Entry<Integer, String> entry : ReverseMap.entrySet())
                bfU.write(entry.getValue() + "\n");
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
        System.out.println("\n" + Thread.currentThread().getName() + " saved URLs successfully\n");
    }
}
