package ru.trolsoft.utils.files;

import ru.trolsoft.utils.StrUtils;

/**
 * Created on 12.02.17.
 */
public class DataBlock {
    private final int address;
    private final byte[] data;

    public DataBlock(int address, byte[] data) {
        this.address = address;
        this.data = data;
    }

    public int getAddress() {
        return address;
    }

    public byte[] getData() {
        return data;
    }

    @Override
    public String toString() {
        int pos = 0;
        int address = this.address;
        int len = 1000;
        StringBuilder result = new StringBuilder();
        while (pos < data.length) {
            if (len >= 16) {
//                if (result.length() > 0) {
                    result.append('\n');
//                }
                result.append(StrUtils.dwordToHexStr(address)).append(":");
                len = 0;
            }
            result.append(' ').append(StrUtils.byteToHexStr(data[pos]));
            pos++;
            address++;
            len++;
        }
        return result.toString();
    }
}
