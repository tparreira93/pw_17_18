package tutorials.configurations;

public class JaccardConfiguration {

    private float similarity;
    private boolean isJaccard;

    public float getSimilarity() {
        return similarity;
    }

    public void setSimilarity(float similarity) {
        this.similarity = similarity;
    }

    public boolean isJaccard() {
        return isJaccard;
    }

    public void setJaccard(boolean jaccard) {
        isJaccard = jaccard;
    }
}
