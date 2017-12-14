package tutorials.configurations;

import org.json.JSONArray;
import org.json.JSONObject;
import tutorials.rank.MultiRanker;
import tutorials.twitter.TwitterReader;
import tutorials.utils.JSONProfile;
import twitter4j.Status;
import twitter4j.TwitterException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TestConfig {
    private String fileName;
    private String query;
    private String evaluation;
    private boolean splitData;
    private boolean isTest;
    private boolean isTrain;
    private List<MultiRanker> multiRankers;
    private List<JSONProfile> testData;
    private List<JSONProfile> trainData;
    private List<Status> tweets;


    public TestConfig() {
        query = "";
        fileName = "";
        evaluation = "";
        tweets = new ArrayList<>();

        testData = new ArrayList<>();
        trainData = new ArrayList<>();
        multiRankers = new ArrayList<>();
    }

    public List<Status> getTweets() {
        return tweets;
    }

    public boolean verboseValidate(){
        if (getFileName().isEmpty()
                || getQuery().isEmpty()
                || getMultiRankers().size() == 0) {
            return false;
        }
        for (MultiRanker config : getMultiRankers()) {
            if (config.getK() < 0.0) {
                System.out.println("Fuse factor k must be greater or equal to 0.");
                return false;
            }
            for (Ranker ranker : config.getRankers()) {
                if(ranker.getAnalyzer() == null)
                {
                    System.out.println("Analyzer must be specified.");
                    return false;
                }
                if(ranker.getSimilarityConfiguration() == null)
                {
                    System.out.println("Similarity must be specified.");
                    return false;
                }
                if(ranker.getExpand() == null)
                {
                    System.out.println("Expansion must be defined.");
                    return false;
                }
            }
        }

        if(!isSplitData())
            setTest(true);

        return true;
    }

    public boolean isLoadTweets() {
        boolean loadTweets = false;

        for (MultiRanker config : getMultiRankers()) {
            for (Ranker ranker : config.getRankers()) {
                if (ranker.createIndex()) {
                    loadTweets = true;
                    break;
                }
            }
        }
        return loadTweets;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public void setEvaluation(String evaluation) {
        this.evaluation = evaluation;
    }

    public void addRanker(MultiRanker ranker) {
        multiRankers.add(ranker);
    }

    public List<MultiRanker> getMultiRankers() {
        return multiRankers;
    }

    public String getEvaluation() {
        return evaluation;
    }

    public boolean isSplitData() {
        return splitData;
    }

    public void setSplitData(boolean splitData) {
        this.splitData = splitData;
    }

    public boolean isTest() {
        return isTest;
    }

    public void setTest(boolean test) {
        isTest = test;
    }

    public boolean isTrain() {
        return isTrain;
    }

    public void setTrain(boolean train) {
        isTrain = train;
    }

    public List<JSONProfile> getTestData() {
        return testData;
    }

    public List<JSONProfile> getTrainData() {
        return trainData;
    }

    public void loadTweets() {
        try {
            tweets = TwitterReader.ReadFile(fileName);
        } catch (TwitterException | IOException e) {
            e.printStackTrace();
        }
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

    public void loadProfiles() throws IOException {
        String jsonData = readFile(getQuery());

        JSONArray array = new JSONArray(jsonData);
        for (int i = 0; i < array.length(); i++) {
            JSONObject obj = array.getJSONObject(i);
            String id = obj.getString("topid");
            String title = obj.getString("title");
            String description = obj.getString("description");
            String narrative = obj.getString("narrative");

            JSONProfile profile = new JSONProfile(id, title, description, narrative);
            if (!isSplitData() || ((i + 1) % 2 == 1)) {
                testData.add(profile);
            } else {
                trainData.add(profile);
            }
        }
    }

}
