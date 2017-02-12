package ru.trolsoft.avrbootloader;

/**
 * Created on 11.02.17.
 */
public class Fuses {
    final int low;
    final int high;
    final int extended;

    public Fuses(int low, int extended, int high) {
        this.low = low;
        this.extended = extended;
        this.high = high;
    }

    public int getLow() {
        return low;
    }

    public int getExtended() {
        return extended;
    }

    public int getHigh() {
        return high;
    }

}
