package poker.connection.server.queue;

public class    Threshold {
    private int lowerBound;
    private int upperBound;

    private int range = 50;

    public Threshold(int midpoint) {
        this.lowerBound = midpoint - range;
        this.upperBound = midpoint + range;
    }

    public int getLowerBound() {
        return lowerBound;
    }

    public int getUpperBound() {
        return upperBound;
    }

    private boolean isWithinThreshold(int value) {
        return value >= lowerBound && value <= upperBound;
    }

    public boolean overlaps(Threshold other) {
        return this.isWithinThreshold(other.getLowerBound()) || this.isWithinThreshold(other.getUpperBound());
    }

    public boolean contains(int value) {
        return value >= lowerBound && value <= upperBound;
    }

    public void expand() {
        range *= 2;
        lowerBound -= range;
        upperBound += range;
    }
}
