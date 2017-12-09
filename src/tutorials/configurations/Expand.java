package tutorials.configurations;

import org.omg.CORBA.PUBLIC_MEMBER;

public class Expand {
    private double weight;
    private boolean expand;

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
        return expand ? "Expansion " + String.format("%2.f", weight) : "";
    }
}
