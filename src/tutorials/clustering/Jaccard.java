package tutorials.clustering;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;


public class Jaccard {

	private float similarity;

	public Jaccard(float similarity) {
		this.similarity = similarity;

	}

	public List<ScoreDoc> process(Analyzer analyzer, IndexSearcher searcher, List<ScoreDoc> docs) throws IOException {

		List<String> s1 = new LinkedList<String>();
		List<String> s2 = new LinkedList<String>();
		List<ScoreDoc> temp = docs;
		List<ScoreDoc> tested = new LinkedList<ScoreDoc>();
		Map<Long, List<String>> shingles = new HashMap<Long, List<String>>();

		while (temp.size() > 0) {

			Document d = searcher.doc(temp.get(0).doc);
			Long id = d.getField("Id").numericValue().longValue();
			if (shingles.containsKey(id)) {
				s1 = shingles.get(id);
			} else {
				s1 = generateShingles(d.getField("Body").stringValue(), analyzer);
				shingles.put(id, s1);
			}

			for (int i = 1; i < temp.size(); i++) {
				Document d1 = searcher.doc(temp.get(i).doc);
				Long id2 = d1.getField("Id").numericValue().longValue();
				if (shingles.containsKey(id2)) {
					s2 = shingles.get(id2);
				} else {
					s2 = generateShingles(d1.getField("Body").stringValue(), analyzer);
					shingles.put(id2, s2);

				}

				if (similarity(s1, s2) > similarity) {
					temp.remove(i);
				}
			}

			tested.add(temp.remove(0));

		}

		return tested;

	}

	private List<String> generateShingles(String sentence, Analyzer analyzer) throws IOException {

		String sentence2 = sentence
				.replaceAll("\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]", "");
		List<String> s1 = new LinkedList<String>();

		StringReader reader = new StringReader(sentence2);
		TokenStream tokenStream = analyzer.tokenStream("content", reader);
		ShingleFilter sf = new ShingleFilter(tokenStream);

		CharTermAttribute charTermAttribute = sf.addAttribute(CharTermAttribute.class);
		sf.reset();
		while (sf.incrementToken()) {

			s1.add(charTermAttribute.toString());

		}
		sf.end();
		sf.close();
		return s1;
	}

	private float similarity(List<String> s1, List<String> s2) {
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



}
