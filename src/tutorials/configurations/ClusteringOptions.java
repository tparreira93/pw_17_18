package tutorials.configurations;

public class ClusteringOptions {
    int numClusters;
    boolean cluster;
    private int numClusteringDocs;

    public ClusteringOptions() {
    }

    public int getNumClusters() {
        return numClusters;
    }

    public void setNumClusters(int numClusters) {
        this.numClusters = numClusters;
    }

    public boolean isCluster() {
        return cluster;
    }

    public void setCluster(boolean cluster) {
        this.cluster = cluster;
    }

    @Override
    public String toString() {
        return isCluster() ? "KMeans(K=" + numClusters + "|numClusteringDocs=" + getNumClusteringDocs() + ")" : "";
    }

    public int getNumClusteringDocs() {
        return numClusteringDocs;
    }

    public void setNumClusteringDocs(int numClusteringDocs) {
        this.numClusteringDocs = numClusteringDocs;
    }
}
