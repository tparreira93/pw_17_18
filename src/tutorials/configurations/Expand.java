package tutorials.configurations;

public class Expand {
    private double weight;
    private boolean expand;
    private int numTerms;
    private int numExpansionDocs;

    public Expand(double weight, boolean expand) {
        this.weight = weight;
        this.expand = expand;
    }

    public Expand() {
        expand = false;
        weight = 0.5;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public boolean isExpand() {
        return expand;
    }

    public void setExpand(boolean expand) {
        this.expand = expand;
    }

    @Override
    public String toString() {
        return expand ? "Expansion(terms=" + getNumTerms() + "|relevant=" + getNumExpansionDocs() + "|alfa=" + Math.round(getWeight() * 100f) / 100f + ")" : "";
    }

    public int getNumTerms() {
        return numTerms;
    }

    public void setNumTerms(int numTerms) {
        this.numTerms = numTerms;
    }

    public int getNumExpansionDocs() {
        return numExpansionDocs;
    }

    public void setNumExpansionDocs(int numExpansionDocs) {
        this.numExpansionDocs = numExpansionDocs;
    }
}
