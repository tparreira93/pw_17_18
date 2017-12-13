package tutorials.utils;

import org.apache.lucene.document.Document;

public class ResultDocs implements Comparable<ResultDocs> {
	private String queryId;
	public final static String CONST_QO = "Q0";
	public final static String CONST_RUN = "1";
	private long docId;
	private int rank;
	private float score;
	private Document doc;

	public ResultDocs(String queryId, long docId, float score, Document doc, int rankPos) {
		this.queryId = queryId;
		this.docId = docId;
		this.score = score;
		this.rank = rankPos;
		this.doc = doc;
	}

	public Document getDoc(){
		return doc;
	}

	public void setDoc(Document doc){
		this.doc = doc;
	}

	public long getDocId() {
		return docId;
	}

	public String getQueryId() {
		return queryId;
	}

	public int getRank() {
		return rank;
	}

	public static String getConstQo() {
		return CONST_QO;
	}

	public float getScore() {
		return score;
	}

	public void setScore(float score) {
		this.score = score;
	}

	public static String getConstRun() {
		return CONST_RUN;
	}

	@Override
	public int compareTo(ResultDocs o) {
		return Float.compare(o.getScore(), this.score);
	}

}
