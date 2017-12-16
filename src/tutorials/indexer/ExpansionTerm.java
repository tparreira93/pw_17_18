package tutorials.indexer;

import tutorials.utils.ResultDocs;

public class ExpansionTerm implements Comparable<ExpansionTerm> {
    private String term;
    private int termFreq;
    private int docFreq;
    private float score;

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public int getTermFreq() {
        return termFreq;
    }

    public void setTermFreq(int termFreq) {
        this.termFreq = termFreq;
    }

    public int getDocFreq() {
        return docFreq;
    }

    public void setDocFreq(int docFreq) {
        this.docFreq = docFreq;
    }

    @Override
    public int compareTo(ExpansionTerm o) {
        return Float.compare(o.getScore(), this.score);
    }
}
