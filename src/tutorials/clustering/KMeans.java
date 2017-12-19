package tutorials.clustering;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.carrot2.core.Cluster;
import org.carrot2.core.Document;
import org.carrot2.text.preprocessing.PreprocessedDocumentScanner;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.carrot2.clustering.kmeans.BisectingKMeansClusteringAlgorithm;

import tutorials.configurations.AnalyzerConfiguration;
import tutorials.configurations.ClusteringOptions;

public class KMeans {

	public static List<ScoreDoc> clusterData(Analyzer analyzer, List<ScoreDoc> docs, IndexSearcher searcher,
			ClusteringOptions clusterOptions) throws IOException {

		BisectingKMeansClusteringAlgorithm b = new BisectingKMeansClusteringAlgorithm();
		List<Document> documents = new LinkedList<>();

		Map<Integer, Float> scores = new HashMap<>();
		System.out.println("KMEANS " + docs.size() + "//\n\n");

		for (ScoreDoc d : docs) {

			org.apache.lucene.document.Document s = searcher.doc(d.doc);
			System.out.println("BEFORE STOP FILTER: " + s.getField("Body").stringValue());
			String title = processedTweet(s.getField("Body").stringValue(), analyzer);
			System.out.println("STOP FILTER: " + title);
			System.out.println();
			Document a = new Document(title);
			a.setField("IdLucene", d.doc);
			double score = d.score;
			scores.put(d.doc, d.score);
			a.setScore(score);
			documents.add(a);
		}

		b.documents = documents;
		b.labelCount = 1;
		// b.partitionCount = 4;

		b.clusterCount = 10;

		b.process();
		List<Cluster> cluster = b.clusters;

		List<Document> best = new LinkedList<Document>();

		for (Cluster c : cluster) {
			List<Document> temp = c.getDocuments();
			double score = Double.MIN_VALUE;
			Document bestDoc = null;
			for (int i = 0; i < temp.size(); i++) {
				Document b1 = temp.get(i);
				if (score < b1.getScore()) {
					score = b1.getScore();
					bestDoc = b1;
				}

			}

			if (bestDoc != null)
				best.add(bestDoc);
		}

		List<ScoreDoc> results = new LinkedList<ScoreDoc>();
		// System.out.println("\n");
		for (Document d : best) {
			System.out.println("FINAL \n");

			System.out.println("Score: " + d.getScore() + " title: " + d.getTitle());
			int id = 0;
			try {
				id = d.getField("IdLucene");
			} catch (Exception e) {
				;
			}
			results.add(new ScoreDoc(id, scores.get(id)));

			Collections.sort(results, new Comparator<ScoreDoc>() {

				@Override
				public int compare(ScoreDoc o1, ScoreDoc o2) {
					// TODO Auto-generated method stub
					if (o1.score < o2.score)
						return 1;
					if (o1.score > o2.score)
						return -1;
					return 0;

				}

			});
		}

		System.out.println("KMEANS FINISHED " + results.size() + "//\n\n");
		return results;

	}

	private static String processedTweet(String tweet, Analyzer analyzer) throws IOException {
		StringReader reader = new StringReader(tweet);
		TokenStream tokenStream = analyzer.tokenStream("content", reader);
		SnowballFilter sf = new SnowballFilter(tokenStream, "English");
		List<String> temp = new LinkedList<String>();

		CharTermAttribute charTermAttribute = sf.addAttribute(CharTermAttribute.class);
		sf.reset();
		while (sf.incrementToken()) {

			temp.add(charTermAttribute.toString());

		}

		sf.end();
		sf.close();

		StringBuilder builder = new StringBuilder();

		for (String string : temp) {
			if (builder.length() > 0) {
				builder.append(" ");
			}
			builder.append(string);
		}

		return builder.toString();
	}
}
