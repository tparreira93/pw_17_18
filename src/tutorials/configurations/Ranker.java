package tutorials.configurations;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.similarities.Similarity;
import tutorials.analyzer.TestAnalyzer;
import tutorials.utils.ResultDocs;

import java.util.List;
import java.util.UUID;

public class Ranker {
    private AnalyzerConfiguration analyzerConfiguration;
    private Similarity similarityConfiguration;
    private Boolean index;
    private Expand expand;
    private UUID id;
    private String name;
    private List<ResultDocs> results;

    public Ranker(String name) {
        analyzerConfiguration = new AnalyzerConfiguration();
        similarityConfiguration = null;
        expand = new Expand();
        index = false;
        id = UUID.randomUUID();
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIndexPath()
    {
        return "INDEX_" + getName();
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
        return TestAnalyzer.getAnalyzer(this);
    }

    public Similarity getSimilarityConfiguration() {
        return similarityConfiguration;
    }

    public void setSimilarityConfigurations(Similarity sim) {
        this.similarityConfiguration = sim;
    }

    public void setResults(List<ResultDocs> results) {
        this.results = results;
    }
}
