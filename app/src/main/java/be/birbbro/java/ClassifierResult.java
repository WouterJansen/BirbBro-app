package be.birbbro.java;

public class ClassifierResult {
    private int index;
    private float percentage;

    public ClassifierResult(int index, float percentage) {
        this.index = index;
        this.percentage = percentage;
    }

    public int getIndex() {
        return index;
    }

    public float getPercentage() {
        return percentage;
    }
}
