package tutorials.clustering;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.carrot2.core.Cluster;
import org.carrot2.core.Document;


import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.carrot2.clustering.kmeans.BisectingKMeansClusteringAlgorithm;

public class KMeans {

	public static List<ScoreDoc> clusterData(List<ScoreDoc> docs, IndexSearcher searcher) throws IOException {

		BisectingKMeansClusteringAlgorithm b = new BisectingKMeansClusteringAlgorithm();
		List<Document> documents = new LinkedList<Document>();

		Map<Integer, Float> scores = new HashMap<>();
		for (ScoreDoc d : docs) {

			org.apache.lucene.document.Document s = searcher.doc(d.doc);
			Document a = new Document(s.getField("Body").toString().split("<Body:")[1]);
			a.setField("IdLucene", d.doc);
			double score = d.score;
			//System.out.println(" SCORE:" + d.score +" Body: "+s.getField("Body").toString().split("<Body:")[1]);
			scores.put(d.doc, d.score);
			a.setScore(score);
			documents.add(a);
		}

		b.documents = documents;
		b.labelCount = 1;
		b.partitionCount = 4;
		b.clusterCount = 3;

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

			best.add(bestDoc);
		}

		List<ScoreDoc> results = new LinkedList<ScoreDoc>();
		System.out.println("\n");
		for (Document d : best) {
			System.out.println("Score:" + d.getScore() + " title:" + d.getTitle());
			int id = d.getField("IdLucene");
			results.add(new ScoreDoc(id, scores.get(id)));
		}

		return results;

	}

}
