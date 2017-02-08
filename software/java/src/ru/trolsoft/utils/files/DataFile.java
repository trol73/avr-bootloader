package ru.trolsoft.utils.files;

import ru.trolsoft.utils.StrUtils;

import java.io.Writer;

/**
 * Created on 31/01/17.
 */
public class DataFile {
    public enum Type {
        BINARY,
        INTEL_HEX,
        PACKAGE
    }

    public static void saveData(byte[] data, int offset, String filePath, Type type) {
        switch (type) {
            case BINARY:
                break;
            case INTEL_HEX:
                break;
            case PACKAGE:
                break;
        }
    }


    private static void writeHexFile(int offset, byte[] data, Writer writer) {
        int pos = 0;
        while (pos < data.length) {
            int l = 16;
            if (pos + l > data.length) {
                l = data.length - pos;
            }
            int crc = 0;
            String s = ":" + StrUtils.byteToHexStr(l);
            crc += l;
            //s += StrUtils.(offset);

        }
/*
with open(filename, "wb") as f:
            while offset < length:
                l = 16
                if offset + l > length:
                    l = length - offset
                crc = 0
                s = ':'
                s += strutils.hex2str(l, 1)
                crc += l
                s += strutils.hex2str(offset, 2)
                crc += (offset >> 8) & 0xFF
                crc += offset & 0xFF
                s += strutils.hex2str(0, 1)
                for i in range(0, l):
                    s += strutils.hex2str(self.data[offset+i], 1)
                    crc += self.data[offset+i]
                crc = - crc
                s += strutils.hex2str(crc, 1)
                s += "\x0D\x0A"
                offset += l
                f.write(s)

         */
    }
}
