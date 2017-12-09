package tutorials.analyzer;

import org.apache.lucene.analysis.Analyzer;
import tutorials.configurations.RunConfig;

public class TestAnalyzer {
    public static Analyzer getAnalyzer(RunConfig config)
    {
        return new CustomAnalyzer(config.getAnalyzerConfiguration());
    }
}
