package tutorials;

import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.jfree.ui.RefineryUtilities;
import org.junit.Test;
import tutorials.configurations.AnalyzerConfiguration;
import tutorials.configurations.Expand;
import tutorials.configurations.RunConfig;
import tutorials.configurations.TestConfig;
import tutorials.indexer.TwitterIndexer;
import tutorials.twitter.TwitterReader;
import tutorials.utils.DataSetTrec;
import tutorials.utils.JSONProfile;
import tutorials.utils.LineChart;
import twitter4j.Status;
import twitter4j.TwitterException;
import org.json.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        TestConfig testConfig = parseConfig(args);
        if (!validateTest(testConfig)) {
            return;
        }

        List<RunConfig> configs = testConfig.getConfigs();
        List<DataSetTrec> trecs = new LinkedList<>();
        List<JSONProfile> interestProfiles;
        TwitterIndexer baseline = new TwitterIndexer(trecs, LocalDate.parse("2016-08-02"), LocalDate.parse("2016-08-11"));
        boolean loadTweets = false;
        List<Status> tweets = new ArrayList<>();
        for (RunConfig config : configs) {
            if (config.createIndex()) {
                loadTweets = true;
                break;
            }
        }

        if (loadTweets) {
            tweets = loadTweets(testConfig.getFileName());
        }
        try {
            interestProfiles = readProfiles(testConfig.getQuery());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        for (RunConfig config : configs) {
            if (config.createIndex()) {
                baseline.openIndex(config);
                baseline.indexTweets(tweets);
                baseline.close();
            }

            baseline.indexSearch(config, interestProfiles);
        }

        LineChart chart = new LineChart("filters", "Precision x Documents", String.valueOf(TwitterIndexer.NUMDOCS), trecs,
                'p');
        chart.pack();
        RefineryUtilities.centerFrameOnScreen(chart);
        chart.setVisible(true);
        LineChart chart2 = new LineChart("filters", "Precision x Recall", String.valueOf(TwitterIndexer.NUMDOCS), trecs, 'r');
        chart2.pack();
        RefineryUtilities.centerFrameOnScreen(chart2);
        chart2.setVisible(true);

        try {
            baseline.writeCSV(trecs, "abc.csv");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean validateTest(TestConfig testConfig) {
        if (testConfig == null
                || testConfig.getFileName().isEmpty()
                || testConfig.getConfigs().size() == 0) {
            printUsage();
            return false;
        }
        for (RunConfig config : testConfig.getConfigs()) {
            if (!validateConfig(config)) {
                printUsage();
                return false;
            }
        }

        return true;
    }

    private static boolean validateConfig(RunConfig config) {
        boolean valid = true;
        if(config.getAnalyzer() == null)
        {
            System.out.println("Analyzer must be specified.");
            valid = false;
        }
        if(config.getSimilarityConfiguration() == null)
        {
            System.out.println("Similarity must be specified.");
            valid = false;
        }
        if(config.getExpand() == null)
        {
            System.out.println("Expansion must be defined.");
            valid = false;
        }

        return valid;
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
        System.out.println("web search - Project 1\n");
        System.out.println("Usage example:");
        System.out.println("[(-h|--help)] -f name -q name -r [index (filter string)] -r [index (filter string)] ....");
        System.out.println("-f: Twitter JSON");
        System.out.println("-f: Queries JSON");
        System.out.println("\t filename");
        System.out.println("-r [index (filter string)]:");
        System.out.println("-index: Create index");
        System.out.println("Filter:");
        System.out.println("-h or --help: Help.");
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
        List<RunConfig> configs = testConfig.getConfigs();
        RunConfig runConfig = null;
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-r":
                        if (args.length - i >= 1) {
                            runConfig = new RunConfig(args[++i]);
                            configs.add(runConfig);
                        }
                        break;
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
                    case "-index":
                        runConfig.setIndex(true);
                        break;
                    case "-sf":
                        runConfig.getAnalyzerConfiguration().getFilters().add(AnalyzerConfiguration.AnalyzerFilters.StopFilter);
                        break;
                    case "-shf":
                        if (args.length - i - 1 >= 2) {
                            runConfig.getAnalyzerConfiguration().setMinShingleSize(Integer.parseInt(args[++i]));
                            runConfig.getAnalyzerConfiguration().setMaxShingleSize(Integer.parseInt(args[++i]));
                            runConfig.getAnalyzerConfiguration().getFilters().add(AnalyzerConfiguration.AnalyzerFilters.ShingleFilter);
                        }
                        break;
                    case "-snf":
                        runConfig.getAnalyzerConfiguration().getFilters().add(AnalyzerConfiguration.AnalyzerFilters.SnowballFilter);
                        break;
                    case "-stdf":
                        runConfig.getAnalyzerConfiguration().getFilters().add(AnalyzerConfiguration.AnalyzerFilters.StandardFilter);
                        break;
                    case "-lcf":
                        runConfig.getAnalyzerConfiguration().getFilters().add(AnalyzerConfiguration.AnalyzerFilters.LowerCaseFilter);
                        break;
                    case "-ngtf":
                        if (args.length - i >= 2) {
                            runConfig.getAnalyzerConfiguration().setMinGram(Integer.parseInt(args[++i]));
                            runConfig.getAnalyzerConfiguration().setMaxGram(Integer.parseInt(args[++i]));
                            runConfig.getAnalyzerConfiguration().getFilters().add(AnalyzerConfiguration.AnalyzerFilters.NGramTokenFilter);
                        }
                        break;
                    case "-cgf":
                        runConfig.getAnalyzerConfiguration().getFilters().add(AnalyzerConfiguration.AnalyzerFilters.CommonGramsFilter);
                        break;
                    case "-wt":
                        runConfig.getAnalyzerConfiguration().getFilters().add(AnalyzerConfiguration.AnalyzerFilters.WhitespaceTokenizer);
                        break;
                    case "-edgtf":
                        if (args.length - i >= 2) {
                            runConfig.getAnalyzerConfiguration().setMinGram(Integer.parseInt(args[++i]));
                            runConfig.getAnalyzerConfiguration().setMaxGram(Integer.parseInt(args[++i]));
                            runConfig.getAnalyzerConfiguration().getFilters().add(AnalyzerConfiguration.AnalyzerFilters.EdgeNGramTokenFilter);
                        }
                        break;
                    case "-classic":
                        runConfig.setSimilarityConfigurations(new ClassicSimilarity());
                        break;
                    case "-bm25":
                        float k1 = 1.2f;
                        float b = 0.75f;
                        if (args.length - i >= 2) {
                            k1 = Float.parseFloat(args[++i]);
                            b = Float.parseFloat(args[++i]);
                        }
                        runConfig.setSimilarityConfigurations(new BM25Similarity(k1, b));
                        break;
                    case "-lmd":
                        float mu = 2000f;
                        if (args.length - i >= 1) {
                            mu = Float.parseFloat(args[++i]);
                        }
                        runConfig.setSimilarityConfigurations(new LMDirichletSimilarity(mu));
                        break;
                    case "-lmjm":
                        float lambda = 0.5f;
                        if (args.length - i >= 1) {
                            lambda = Float.parseFloat(args[++i]);
                        }
                        runConfig.setSimilarityConfigurations(new LMJelinekMercerSimilarity(lambda));
                        break;
                    case "-expand":
                        Expand exp = new Expand(0.5, true);
                        if (args.length - i >= 1) {
                            exp.setWeight(Float.parseFloat(args[++i]));
                            runConfig.setExpand(exp);
                        }
                        break;
                    case "-noexpand":
                        runConfig.setExpand(new Expand(0, false));
                        break;
                    case "-h":
                    case "--help":
                    default:
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
