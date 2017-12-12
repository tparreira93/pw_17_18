package tutorials.analyzer;

import org.apache.lucene.analysis.Analyzer;
import tutorials.configurations.Ranker;

public class TestAnalyzer {
    public static Analyzer getAnalyzer(Ranker config)
    {
        return new CustomAnalyzer(config.getAnalyzerConfiguration());
    }
}
