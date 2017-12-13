package tutorials;

import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.jfree.ui.RefineryUtilities;
import org.json.JSONArray;
import org.json.JSONObject;

import tutorials.clustering.Jaccard;
import tutorials.configurations.AnalyzerConfiguration;
import tutorials.configurations.Expand;
import tutorials.configurations.Ranker;
import tutorials.configurations.TestConfig;
import tutorials.indexer.TwitterIndexer;
import tutorials.rank.MultiRanker;
import tutorials.rank.RankFusion;
import tutorials.twitter.TwitterReader;
import tutorials.utils.*;
import twitter4j.Status;
import twitter4j.TwitterException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        TestConfig testConfig = parseConfig(args);
        if (testConfig == null || !testConfig.verboseValidate()) {
            printUsage();
            return;
        }
        
        
        try {
            LocalDate startDate = LocalDate.parse("2016-08-02");
            LocalDate endDate = LocalDate.parse("2016-08-11");
            List<MultiRanker> multiRankers = testConfig.getMultiRankers();
            List<DataSetTrec> trecs = new LinkedList<>();
            List<JSONProfile> interestProfiles;
            TwitterIndexer indexer = new TwitterIndexer(startDate, endDate);
            List<Status> tweets = new ArrayList<>();

            try {
                if (testConfig.loadTweets()) {
                    System.out.println("Loading tweets...");
                    tweets = loadTweets(testConfig.getFileName());
                    System.out.println(tweets.size() + " tweets loaded!");
                }
                System.out.println("Loading interest profiles...");
                interestProfiles = readProfiles(testConfig.getQuery());
                System.out.println(interestProfiles.size() + " profiles loaded!");
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            System.out.println("");

            for (MultiRanker multiRanker : multiRankers) {
                for (Ranker ranker : multiRanker.getRankers()) {
                    if (ranker.createIndex()) {
                        System.out.println("Indexing " + ranker.toString() + "...");
                        indexer.openIndex(ranker);
                        indexer.indexTweets(tweets);
                        indexer.close();
                    }
                    

                    System.out.println("Creating digests for " + ranker.toString() + "...");
                    System.out.println("");
                    indexer.indexSearch(ranker, interestProfiles);
                }
                System.out.println("");
                System.out.println("Fusing scores of " + multiRanker.toString() + "...");

                RankFusion fusion = new RankFusion();
                List<ResultDocs> fused = fusion.Fuse(multiRanker);

                DataSetTrec t  = multiRanker.createTrec("tmp.txt", testConfig.getEvaluation(), fused);
                trecs.add(t);


                System.out.println("");
                System.out.println("");
                System.out.println(t.getName());
                System.out.println(t.getRawText());
            }

            plotTrecs(trecs);
            writeCSV(trecs, "abc.csv");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void writeCSV(List<DataSetTrec> t, String csvFile) throws IOException {
        FileWriter writer = new FileWriter(csvFile);

        CSVUtils.writeLine(writer, Arrays.asList("Filter", "map"));

        // custom separator + quote
        for (DataSetTrec d : t) {
            CSVUtils.writeLine(writer, Arrays.asList(d.getName(), String.valueOf(d.getMap())));
        }

        writer.flush();
        writer.close();
    }

    private static void plotTrecs(List<DataSetTrec> trecs) {
        plot("Precision x Documents", "Precision x Documents",
                String.valueOf(TwitterIndexer.NUMDOCS), trecs,
                'p');

        plot("Precision x Recall", "Precision x Recall",
                String.valueOf(TwitterIndexer.NUMDOCS),
                trecs, 'r');
    }

    private static void plot(String applicationTitle, String chartTitle, String sub, List<DataSetTrec> trecs, char type) {
        LineChart lineChart = new LineChart(applicationTitle, chartTitle, sub, trecs, type);
        lineChart.pack();
        RefineryUtilities.centerFrameOnScreen(lineChart);
        lineChart.setVisible(true);
    }

    private static String readFile(String filename) throws IOException {
        String result;
        BufferedReader br = new BufferedReader(new FileReader(filename));
        StringBuilder sb = new StringBuilder();
        String line = br.readLine();
        while (line != null) {
            sb.append(line);
            line = br.readLine();
        }
        result = sb.toString();
        return result;
    }

    private static List<JSONProfile> readProfiles(String path) throws IOException {
        List<JSONProfile> profiles = new ArrayList<>();
        String jsonData = readFile(path);

        JSONArray array = new JSONArray(jsonData);
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            String id = obj.getString("topid");
            String title = obj.getString("title");
            String description = obj.getString("description");
            String narrative = obj.getString("narrative");

            JSONProfile profile = new JSONProfile(id, title, description, narrative);
            profiles.add(profile);
        }
        return profiles;
    }

    private static List<Status> loadTweets(String fileName) {
        List<Status> tweets = new ArrayList<>();
        try {
            tweets = TwitterReader.ReadFile(fileName);
        } catch (TwitterException | IOException e) {
            e.printStackTrace();
        }

        return tweets;
    }

    private static void printUsage() {
        System.out.println("web search - Project\n");
        System.out.println("Usage example:");
        System.out.println("[(-h|--help)] -f name -q name -e evaluation (-fuse | -nofuse) " +
                "-r [index (filter string | nothing(defaults to english analyzer))] .." +
                " -r [index (filter string)] (-fuse | -nofuse) -r [index (filter string | nothing(defaults to english analyzer))] ....");
        System.out.println("-h or --help: Help.");
        System.out.println("-f: Twitter JSON");
        System.out.println("\t filename");
        System.out.println("-q: Queries JSON");
        System.out.println("\t filename");
        System.out.println("-e: Evaluation");
        System.out.println("\t filename");
        System.out.println("-fuse: Fuse following ranks");
        System.out.println("-nofuse: No fusion for following ranks");
        System.out.println("-r [index (filter string)]:");
        System.out.println("-index: Create index");
        System.out.println("Filters:");
        System.out.println("-sf: Stop Filter.");
        System.out.println("-shf: Shingle Filter");
        System.out.println("\t MinShingleSize integer");
        System.out.println("\t MaxShingleSize integer");
        System.out.println("-snf: Snowball Filter");
        System.out.println("-stdf: Standard Filter");
        System.out.println("-lcf: Lower Case Filter");
        System.out.println("-ngtf: N-Gram Token Filter");
        System.out.println("\t MinGram integer");
        System.out.println("\t MaxGram integer");
        System.out.println("-cgf: Common Grams Filter");
        System.out.println("-edgtf: Edge N-Gram Token Filter");
        System.out.println("\t MinGram integer");
        System.out.println("\t MaxGram integer");
        System.out.println("Similarities:");
        System.out.println("-classic: Classic similarity (vector space model)");
        System.out.println("-bm25: BM25 similarity");
        System.out.println("\t k1 float");
        System.out.println("\t b float");
        System.out.println("-lmd: LM Dirichlet smoothing");
        System.out.println("\t mu float");
        System.out.println("-lmjm: LM Jelineck-Mercer");
        System.out.println("\t lambda float");
    }

    private static TestConfig parseConfig(String[] args) {
        TestConfig testConfig = new TestConfig();
        Ranker ranker = null;
        MultiRanker multiRanker = new MultiRanker();
        Boolean fuse = false;
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-f":
                        if (args.length - i >= 1) {
                            testConfig.setFileName(args[++i]);
                        }
                        break;
                    case "-q":
                        if (args.length - i >= 1) {
                            testConfig.setQuery(args[++i]);
                        }
                        break;
                    case "-e":
                        if (args.length - i >= 1) {
                            testConfig.setEvaluation(args[++i]);
                        }
                        break;
                    case "-fuse":
                        fuse = true;
                        multiRanker = new MultiRanker();
                        if (args.length - i >= 1) {
                            multiRanker.setK(Float.parseFloat(args[++i]));
                        }
                        testConfig.addRanker(multiRanker);
                        break;
                    case "-nofuse":
                        fuse = false;
                        break;
                    case "-r":
                        if (args.length - i >= 1) {
                            ranker = new Ranker(args[++i]);
                            if (fuse) {
                                multiRanker.addRanker(ranker);
                            } else {
                                multiRanker = new MultiRanker();
                                multiRanker.addRanker(ranker);
                                testConfig.addRanker(multiRanker);
                            }
                        }
                        break;
                    case "-index":
                        if (ranker != null) {
                            ranker.setIndex(true);
                        }
                        break;
                    case "-sf":
                        if (ranker != null) {
                            ranker.getAnalyzerConfiguration().getFilters().add(AnalyzerConfiguration.AnalyzerFilters.StopFilter);
                        }
                        break;
                    case "-shf":
                        if (args.length - i - 1 >= 2) {
                            if (ranker != null) {
                                ranker.getAnalyzerConfiguration().setMinShingleSize(Integer.parseInt(args[++i]));
                                ranker.getAnalyzerConfiguration().setMaxShingleSize(Integer.parseInt(args[++i]));
                                ranker.getAnalyzerConfiguration().getFilters().add(AnalyzerConfiguration.AnalyzerFilters.ShingleFilter);
                            }
                        }
                        break;
                    case "-snf":
                        if (ranker != null) {
                            ranker.getAnalyzerConfiguration().getFilters().add(AnalyzerConfiguration.AnalyzerFilters.SnowballFilter);
                        }
                        break;
                    case "-stdf":
                        if (ranker != null) {
                            ranker.getAnalyzerConfiguration().getFilters().add(AnalyzerConfiguration.AnalyzerFilters.StandardFilter);
                        }
                        break;
                    case "-lcf":
                        if (ranker != null) {
                            ranker.getAnalyzerConfiguration().getFilters().add(AnalyzerConfiguration.AnalyzerFilters.LowerCaseFilter);
                        }
                        break;
                    case "-ngtf":
                        if (args.length - i >= 2) {
                            if (ranker != null) {
                                ranker.getAnalyzerConfiguration().setMinGram(Integer.parseInt(args[++i]));
                                ranker.getAnalyzerConfiguration().setMaxGram(Integer.parseInt(args[++i]));
                                ranker.getAnalyzerConfiguration().getFilters().add(AnalyzerConfiguration.AnalyzerFilters.NGramTokenFilter);
                            }
                        }
                        break;
                    case "-cgf":
                        if (ranker != null) {
                            ranker.getAnalyzerConfiguration().getFilters().add(AnalyzerConfiguration.AnalyzerFilters.CommonGramsFilter);
                        }
                        break;
                    case "-wt":
                        if (ranker != null) {
                            ranker.getAnalyzerConfiguration().getFilters().add(AnalyzerConfiguration.AnalyzerFilters.WhitespaceTokenizer);
                        }
                        break;
                    case "-edgtf":
                        if (args.length - i >= 2) {
                            if (ranker != null) {
                                ranker.getAnalyzerConfiguration().setMinGram(Integer.parseInt(args[++i]));
                                ranker.getAnalyzerConfiguration().setMaxGram(Integer.parseInt(args[++i]));
                                ranker.getAnalyzerConfiguration().getFilters().add(AnalyzerConfiguration.AnalyzerFilters.EdgeNGramTokenFilter);
                            }
                        }
                        break;
                    case "-classic":
                        if (ranker != null) {
                            ranker.setSimilarityConfigurations(new ClassicSimilarity());
                        }
                        break;
                    case "-bm25":
                        float k1 = 1.2f;
                        float b = 0.75f;
                        if (args.length - i >= 2) {
                            k1 = Float.parseFloat(args[++i]);
                            b = Float.parseFloat(args[++i]);
                        }
                        if (ranker != null) {
                            ranker.setSimilarityConfigurations(new BM25Similarity(k1, b));
                        }
                        break;
                    case "-lmd":
                        float mu = 2000f;
                        if (args.length - i >= 1) {
                            mu = Float.parseFloat(args[++i]);
                        }
                        if (ranker != null) {
                            ranker.setSimilarityConfigurations(new LMDirichletSimilarity(mu));
                        }
                        break;
                    case "-lmjm":
                        float lambda = 0.5f;
                        if (args.length - i >= 1) {
                            lambda = Float.parseFloat(args[++i]);
                        }
                        if (ranker != null) {
                            ranker.setSimilarityConfigurations(new LMJelinekMercerSimilarity(lambda));
                        }
                        break;
                    case "-expand":
                        Expand exp = new Expand(0.5, true);
                        if (args.length - i >= 1) {
                            exp.setWeight(Float.parseFloat(args[++i]));
                            if (ranker != null) {
                                ranker.setExpand(exp);
                            }
                        }
                        break;
                    case "-noexpand":
                        if (ranker != null) {
                            ranker.setExpand(new Expand(0, false));
                        }
                        break;
                    case "-h":
                    case "--help":
                        return null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return testConfig;
    }
}
