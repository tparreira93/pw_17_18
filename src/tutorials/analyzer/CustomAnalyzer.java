/**
 * 
 */
package tutorials.analyzer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.charfilter.HTMLStripCharFilter;
import org.apache.lucene.analysis.commongrams.CommonGramsFilter;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.ngram.EdgeNGramTokenFilter;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import tutorials.configurations.AnalyzerConfiguration;

import java.io.Reader;

/**
 * @author jmag
 *
 */
public class CustomAnalyzer extends Analyzer {
	private AnalyzerConfiguration config;

	/**
	 * Builds an analyzer with the default stop words ({@link #STOP_WORDS_SET}).
	 */
	public CustomAnalyzer() {

	}

	public CustomAnalyzer(AnalyzerConfiguration config) {
		this.config = config;
	}

	public AnalyzerConfiguration getConfig() {
		return config;
	}

	public void setConfig(AnalyzerConfiguration config) {
		this.config = config;
	}

	@Override
	protected TokenStreamComponents createComponents(final String fieldName) {

		// THE FIELD IS IGNORED
		// ___BUT___
		// you can provide different TokenStremComponents according to the
		// fieldName

		final StandardTokenizer src = new StandardTokenizer();

		TokenStream tok = new StandardFilter(src); // text into non punctuated
													// text
		for(AnalyzerConfiguration.AnalyzerFilters filter : config.getFilters())
		{
			switch (filter) {
				case StopFilter:
					tok = new StopFilter(tok, AnalyzerConfiguration.stopSet); // removes stop words
					break;
				case ShingleFilter:
					tok = new ShingleFilter(tok, config.getMinShingleSize(), config.getMaxShingleSize());
					break;
				case SnowballFilter:
					tok = new SnowballFilter(tok, "English");
					break;
				case StandardFilter:
					break;
				case LowerCaseFilter:
					tok = new LowerCaseFilter(tok); // changes all texto into lowercase
					break;
				case NGramTokenFilter:
					tok = new NGramTokenFilter(tok, config.getMinGram(), config.getMaxGram());// creates
					// word-grams
					// with
					// neighboring
					// works
					break;
				case CommonGramsFilter:
					tok = new CommonGramsFilter(tok, AnalyzerConfiguration.stopSet); // creates
					// word-grams
					// with
					// stopwords
					break;
				case WhitespaceTokenizer:
					tok = new WhitespaceTokenizer();
					break;
				case EdgeNGramTokenFilter:
					tok = new EdgeNGramTokenFilter(tok, config.getMinGram(), config.getMaxGram()); // creates
					// word-bounded
					// n-grams
					break;
			}
		}

		return new TokenStreamComponents(src, tok) {
			@Override
			protected void setReader(final Reader reader) {
				src.setMaxTokenLength(config.getMaxTokenLength());
				//super.setReader(new HTMLStripCharFilter(reader));
				super.setReader(new HTMLStripCharFilter(reader));
			}
		};
	}

	@Override
	public String toString() {
		return config.toString();
	}

	@Override
	protected TokenStream normalize(String fieldName, TokenStream in) {
		TokenStream result = new StandardFilter(in);
		result = new LowerCaseFilter(result);
		return result;
	}
}
