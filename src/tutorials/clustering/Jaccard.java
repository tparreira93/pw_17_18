package tutorials.clustering;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;

import tutorials.configurations.JaccardConfiguration;

public class Jaccard {

	public static List<ScoreDoc> process(Analyzer analyzer, IndexSearcher searcher, List<ScoreDoc> docs,
			JaccardConfiguration config) throws IOException {
			
		List<String> s1 = new LinkedList<String>();
		List<String> s2 = new LinkedList<String>();
		List<ScoreDoc> temp = docs;
		List<ScoreDoc> tested = new LinkedList<ScoreDoc>();
	
		while (temp.size() > 0) {

			Document d = searcher.doc(temp.get(0).doc);
			String sentence = d.getField("Body").stringValue()
					.replaceAll("\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]", "");

			StringReader reader = new StringReader(sentence);
			TokenStream tokenStream = analyzer.tokenStream("content", reader);
			ShingleFilter sf = new ShingleFilter(tokenStream);

			CharTermAttribute charTermAttribute = sf.addAttribute(CharTermAttribute.class);
			sf.reset();
			while (sf.incrementToken()) {

				s1.add(charTermAttribute.toString());

			}
			sf.end();
			sf.close();

			for (int i = 1; i < temp.size(); i++) {
				Document d1 = searcher.doc(temp.get(i).doc);
				String sentence2 = d1.getField("Body").stringValue();
				reader = new StringReader(sentence2);
				tokenStream = analyzer.tokenStream("content", reader);
				sf = new ShingleFilter(tokenStream);
				charTermAttribute = sf.addAttribute(CharTermAttribute.class);
				sf.reset();
				while (sf.incrementToken()) {
					s2.add(charTermAttribute.toString());

				}
				sf.end();
				sf.close();

				if (similarity(s1, s2) > config.getSimilarity()) {
					temp.remove(i);
				}
			}

			tested.add(temp.remove(0));

		}

		return tested;

	}

	public static float similarity(List<String> s1, List<String> s2) {
		float intersections = 0;

		if (s1 == null) {
			throw new NullPointerException("s1 must not be null");
		}

		if (s2 == null) {
			throw new NullPointerException("s2 must not be null");
		}

		Set<String> shingles = new HashSet<String>();
		shingles.addAll(s1);
		shingles.addAll(s2);

		for (String t : s1) {
			if (s2.contains(t)) {
				intersections++;
			}
		}

		return intersections / shingles.size();

	}

	public static float distance(List<String> s1, List<String> s2) {

		return 1 - similarity(s1, s2);
	}

}
