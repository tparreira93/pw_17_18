package tutorials.configurations;

import tutorials.rank.MultiRanker;

import java.util.ArrayList;
import java.util.List;

public class TestConfig {
    private String fileName;
    private List<MultiRanker> multiRankers;
    private List<Ranker> configs;
    private String query;

    public TestConfig() {
        fileName = "";
        configs = new ArrayList<>();
        multiRankers = new ArrayList<>();
        query = "";
    }

    public boolean verboseValidate(){
        if (getFileName().isEmpty()
                || getQuery().isEmpty()
                || getMultiRankers().size() == 0) {
            return false;
        }
        for (MultiRanker config : getMultiRankers()) {
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
        return true;
    }

    public boolean loadTweets() {
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

    public List<Ranker> getConfigs() {
        return configs;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public void addRanker(MultiRanker ranker) {
        multiRankers.add(ranker);
    }

    public List<MultiRanker> getMultiRankers() {
        return multiRankers;
    }
}
