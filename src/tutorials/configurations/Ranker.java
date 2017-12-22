package tutorials.configurations;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.search.similarities.Similarity;
import tutorials.analyzer.CustomAnalyzer;
import tutorials.analyzer.TestAnalyzer;
import tutorials.rank.DailyDigest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Ranker {
    private AnalyzerConfiguration analyzerConfiguration;
    private Similarity similarityConfiguration;
    private Boolean index;
    private Expand expand;
    private ClusteringOptions cluster;
    private UUID id;
    private String indexName;
    private List<DailyDigest> dailyDigests;

    private JaccardConfiguration jaccard;

    public Ranker(String indexName) {
        analyzerConfiguration = new AnalyzerConfiguration();
        similarityConfiguration = null;
        expand = new Expand();
        jaccard = new JaccardConfiguration();
        cluster = new ClusteringOptions();
        index = false;
        id = UUID.randomUUID();
        this.indexName = indexName;
        dailyDigests = new ArrayList<>();
    }

    public void addDailyDigest(DailyDigest digest) {
        dailyDigests.add(digest);
    }

    public List<DailyDigest> getDigests() {
        return dailyDigests;
    }

    public UUID getId() {
        return id;
    }

    @Override
    public String toString() {
        String str = "";
        str += similarityConfiguration.toString();
        if(cluster.isCluster())
            str += " " + cluster.toString();

        if (getExpand().isExpand()) {
            if(cluster.isCluster())
                str += " and";
            str += " " + expand.toString();
        }

        return str;
    }

    public String getIndexName() {
        return indexName;
    }

    public String getIndexPath()
    {
        return "INDEX_" + getIndexName();
    }

    public Boolean createIndex() {
        return index;
    }

    public void setIndex(Boolean index) {
        this.index = index;
    }

    public Expand getExpand() {
        return expand;
    }

    public void setExpand(Expand expand) {
        this.expand = expand;
    }

    public AnalyzerConfiguration getAnalyzerConfiguration() {
        return analyzerConfiguration;
    }

    public Analyzer getAnalyzer(){
        if(analyzerConfiguration.getFilters().size() == 0)
            return new EnglishAnalyzer(AnalyzerConfiguration.stopSet);
            //return new EnglishAnalyzer();
        return TestAnalyzer.getAnalyzer(this);
    }

    public Similarity getSimilarityConfiguration() {
        return similarityConfiguration;
    }

    public void setSimilarityConfigurations(Similarity sim) {
        this.similarityConfiguration = sim;
    }

    public void setClustering(ClusteringOptions cluster) {
        this.cluster = cluster;
    }

    public ClusteringOptions getClustering() {
        return cluster;
    }

    public void setJaccard(JaccardConfiguration jaccard) {
        this.jaccard = jaccard;
    }

    public JaccardConfiguration getJaccard() {
        return jaccard;
    }
}
