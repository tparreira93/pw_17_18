package tutorials.configurations;

import javafx.collections.transformation.SortedList;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.search.similarities.Similarity;
import tutorials.analyzer.CustomAnalyzer;
import tutorials.analyzer.TestAnalyzer;
import tutorials.rank.DailyDigest;
import tutorials.rank.ProfileDigest;
import tutorials.rank.RankFusion;
import tutorials.utils.ResultDocs;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
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
    private boolean multipleFields;

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

    public List<DailyDigest> getDigests(int top) {
        List<DailyDigest> digests = dailyDigests;

        if (multipleFields) {
            for (DailyDigest day : dailyDigests) {
                for (ProfileDigest digest : day.getDigests()) {
                    List<ResultDocs> resultsFollowers = new ArrayList<>();
                    List<ResultDocs> resultsVerified = new ArrayList<>();
                    List<ResultDocs> resultsStatusCount = new ArrayList<>();

                    int i = 1;
                    for (ResultDocs result : digest.getResultDocs()) {
                        Document doc = result.getDoc();
                        int followers = doc.getField("FollowersCount").numericValue().intValue();
                        int verified = doc.getField("Verified").numericValue().intValue();
                        int statusCount = doc.getField("StatusesCount").numericValue().intValue();

                        resultsFollowers.add(new ResultDocs(digest.getProfile().getTopicID(), result.getDocId(), followers, doc, 1, result.getDate()));
                        resultsVerified.add(new ResultDocs(digest.getProfile().getTopicID(), result.getDocId(), verified, doc, 1, result.getDate()));
                        resultsStatusCount.add(new ResultDocs(digest.getProfile().getTopicID(), result.getDocId(), statusCount, doc, 1, result.getDate()));

                        if(i == top)
                            break;
                        i++;
                    }

                    Collections.sort(resultsFollowers);
                    for (i = 0; i < resultsFollowers.size(); i++)
                        resultsFollowers.get(i).setRank(i+1);
                    Collections.sort(resultsVerified);
                    for (i = 0; i < resultsVerified.size(); i++)
                        resultsVerified.get(i).setRank(i+1);
                    Collections.sort(resultsStatusCount);
                    for (i = 0; i < resultsStatusCount.size(); i++)
                        resultsStatusCount.get(i).setRank(i+1);

                    digest.getResultDocs().addAll(resultsFollowers);
                    digest.getResultDocs().addAll(resultsVerified);
                    digest.getResultDocs().addAll(resultsStatusCount);
                }
            }
        }

        return digests;
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

        if (isMultipleFields()) {
            if(cluster.isCluster() || getExpand().isExpand())
                str += " and";
            str += " Multiple fields";
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

    public void setMultipleFields(boolean multipleFields) {
        this.multipleFields = multipleFields;
    }

    public boolean isMultipleFields() {
        return multipleFields;
    }
}
