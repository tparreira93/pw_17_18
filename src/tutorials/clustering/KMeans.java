package tutorials.clustering;

import java.util.LinkedList;
import java.util.List;

import org.carrot2.core.Cluster;
import org.carrot2.core.Document;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.lucene.search.ScoreDoc;
import org.carrot2.clustering.kmeans.BisectingKMeansClusteringAlgorithm;

public class KMeans {
	

	public static void clusterData() {
		
		List<org.apache.lucene.document.Document> docs;
		
		BisectingKMeansClusteringAlgorithm b = new BisectingKMeansClusteringAlgorithm();
		List<Document> documents = new LinkedList<Document>();
		documents.add(new Document("WordA . WordA"));
		documents.add(new Document("WordB . WordB"));
		documents.add(new Document("WordC . WordC"));
		documents.add(new Document("WordA . WordA"));
		documents.add(new Document("WordB . WordB"));
		documents.add(new Document("WordC . WordC"));

		b.documents = documents;
		b.labelCount=1;
		b.partitionCount= 3;
		
		b.process();
		List<Cluster> cluster = b.clusters;
		
		for(Cluster c: cluster){
			System.out.println(c.getLabel());
		}
	
		
	}
	
	 public static void main(String[] args) {
		 
		 /*Logger logger = Logger.getLogger(KMeans.class);
		 BasicConfigurator.configure();*/
		 //logger.info("This is my first log4j's statement");
		 clusterData();
	 }

}
