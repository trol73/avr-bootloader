package ru.trolsoft.utils.packing;

import java.util.ArrayList;
import java.util.List;

/**
 * Created on 31/01/17.
 */
public class BitStream {
    private final List<Integer> data = new ArrayList<>();
    private int val;
    private int valSize;

    public void append(int size, int value) {
        while (size > 0) {
            boolean bit = (value & (1 << (size-1))) != 0;
            appendBit(bit);
            size--;
        }
    }

    public void appendBit(boolean bit) {
        if (bit) {
            val |= 1 << valSize;
        }
        valSize++;
        if (valSize == 8) {
            data.add(val);
            val = 0;
            valSize = 0;
        }
    }

    public int[] toArray() {
        int[] result = new int[data.size() + (valSize > 0 ? 1 : 0)];
        for (int i = 0; i < result.length; i++) {
            result[i] = data.get(i);
        }
        if (result.length > data.size()) {
            result[result.length-1] = val;
        }
        return result;
    }
    @Override
    public String toString() {
        return data.toString() + " " + val +  "(" + valSize + ")";
    }


    public static void main(String... args) {
        BitStream bs = new BitStream();
        bs.append(16, 1024);
//        bs.append(1, 1);
//        bs.append(1, 1);
//        bs.append(1, 1);
//        bs.append(1, 1);
//        bs.append(1, 0);
//        bs.append(1, 0);
//        bs.append(1, 0);
//        bs.append(1, 0);
//
//        bs.append(8, 0xff);
//
//        bs.append(8, 123);

//        bs.append(1, 1);
        System.out.println(bs);

    }
}
