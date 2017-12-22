package tutorials.configurations;

import org.apache.lucene.analysis.CharArraySet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AnalyzerConfiguration
{
    private List<AnalyzerFilters> filters;
    private Integer maxTokenLength;
    private Integer minShingleSize;
    private Integer maxShingleSize;

    public List<AnalyzerFilters> getFilters() {
        return filters;
    }

    public void setFilters() {
        this.filters = filters;
    }

    public Integer getMaxTokenLength() {
        return maxTokenLength;
    }

    public void setMaxTokenLength(Integer maxTokenLength) {
        this.maxTokenLength = maxTokenLength;
    }

    public Integer getMinShingleSize() {
        return minShingleSize;
    }

    public void setMinShingleSize(Integer minShingleSize) {
        this.minShingleSize = minShingleSize;
    }

    public Integer getMaxShingleSize() {
        return maxShingleSize;
    }

    public void setMaxShingleSize(Integer maxShingleSize) {
        this.maxShingleSize = maxShingleSize;
    }

    public Integer getMinGram() {
        return minGram;
    }

    public void setMinGram(Integer minGram) {
        this.minGram = minGram;
    }

    public Integer getMaxGram() {
        return maxGram;
    }

    public void setMaxGram(Integer maxGram) {
        this.maxGram = maxGram;
    }

    private Integer minGram;
    private Integer maxGram;

    /*public static List<String> stopWords = Arrays.asList("a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if",
            "in", "into", "is", "it", "no", "not", "of", "on", "or", "such", "that", "the", "their", "then", "there",
            "these", "they", "this", "to", "was", "will", "with", "et", "al", "some", "can","-");*/
    public static List<String> stopWords = Arrays.asList("a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if",
            "in", "into", "is", "it", "no", "not", "of", "on", "or", "such", "that", "the", "their", "then", "there",
            "these", "they", "this", "to", "was", "will", "with", "et", "al", "some", "can",

            "i'm", "i", "my");
    public static CharArraySet stopSet = new CharArraySet(stopWords, false);

    public AnalyzerConfiguration(List<AnalyzerFilters> filters) {
        this.filters = filters;
        this.maxTokenLength = 25;
        this.minShingleSize = 2;
        this.maxShingleSize = 3;
        this.minGram = 2;
        this.maxGram = 5;
    }

    public AnalyzerConfiguration() {
        this.filters = new ArrayList<>();
        this.maxTokenLength = 25;
        this.minShingleSize = 2;
        this.maxShingleSize = 3;
        this.minGram = 2;
        this.maxGram = 5;
    }

    public enum AnalyzerFilters {
        StandardFilter("Standard Filter"),				// text into non punctuated text
        LowerCaseFilter("Lower Case Filter"),				// changes all texto into lowercase
        StopFilter("Stop Filter"),				// removes stop words
        WhitespaceTokenizer("Whitespace Tokenizer"),
        ShingleFilter("Shingle Filter"),				// creates word-grams with neighboring works
        CommonGramsFilter("Common Grams Filter"),	// creates word-grams with stopwords
        NGramTokenFilter("N-Gram Token Filter"),			// creates unbounded n-grams
        EdgeNGramTokenFilter("Edge N-Gram Token Filter"),
        SnowballFilter("Snowball Filter");

        private final String text;

        AnalyzerFilters(final String text) {
            this.text = text;
        }
        public String toString() {
            return text;
        }
    }

    @Override
    public String toString() {
        String filterName = "";

        for(AnalyzerConfiguration.AnalyzerFilters filter : getFilters()) {
            if(!filterName.isEmpty())
                filterName += " -> ";
            filterName += filter.toString();
        }
        return super.toString();
    }
}