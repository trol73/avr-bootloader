package ru.trolsoft.utils.files;

import ru.trolsoft.utils.StrUtils;

import java.io.*;
import java.util.Random;

/**
 * Created on 07/02/17.
 */
public class IntelHexWriter {
    private final Writer writer;
    /**
     *
     */
    private int segmentAddress = 0;

    public IntelHexWriter(Writer writer) {
        if (writer instanceof BufferedWriter) {
            this.writer = writer;
        } else {
            this.writer = new BufferedWriter(writer);
        }
    }

//    public IntelHexWriter(String fileName) throws IOException {
//        this(new FileWriter(fileName));
//    }



    public void addData(int offset, byte data[], int bytesPerLine) throws IOException {
        if (data.length == 0) {
            return;
        }
//System.out.println("::" + data.length);
        byte buf[] = new byte[bytesPerLine];
        int pos = 0;
        int bytesToAdd = data.length;
        while (bytesToAdd > 0) {
            if (offset % bytesPerLine != 0) {     // can be true for first line if offset doesn't aligned
                buf = new byte[bytesPerLine - offset % bytesPerLine];
            } else if (bytesToAdd < bytesPerLine) {     // last line
                buf = new byte[bytesToAdd];
            } else if (buf.length != bytesPerLine) {
                buf = new byte[bytesPerLine];
            }
            System.arraycopy(data, pos, buf, 0, buf.length);
            // Goto next segment if no more space available in current
            if (offset + buf.length - 1 > segmentAddress + 0xffff) {
                int nextSegment = ((offset + bytesPerLine) >> 4) << 4;
                addSegmentRecord(nextSegment);
                segmentAddress = nextSegment;
            }
            addDataRecord(offset & 0xffff, buf);
            bytesToAdd -= buf.length;
            offset += buf.length;
            pos += buf.length;
        }
    }

    private void addSegmentRecord(int offset) throws IOException {
        int paragraph = offset >> 4;
        int hi = (paragraph >> 8) & 0xff;
        int lo = paragraph & 0xff;
        int crc = 2 + 2 + hi + lo;
        crc = (-crc) & 0xff;
        String rec = ":02000002" + hex(hi) + hex(lo) + hex(crc);
        write(rec);
        // 02 0000 02 10 00 EC
        //:02 0000 04 00 01 F9

    }


    private void addEofRecord() throws IOException {
        write(":00000001FF");
    }


    private void write(String s) throws IOException {
        writer.write(s);
        //writer.write(0x0d);
        writer.write(0x0a);
    }


    private void addDataRecord(int offset, byte[] data) throws IOException {
        int hi = (offset >> 8) & 0xff;
        int lo = offset & 0xff;
        int crc = data.length + hi + lo;
        String rec = ":" + hex(data.length) + hex(hi) + hex(lo) + "00";
        for (byte d : data) {
            rec += hex(d);
            crc += d;
        }
        crc = (-crc) & 0xff;
        rec += hex(crc);
        write(rec);
    }

    private static String hex(int b) {
        return StrUtils.byteToHexStr((byte)b);
    }

    public void done() throws IOException {
        addEofRecord();
        writer.flush();
    }


    public static void main(String ... args) throws IOException {
        IntelHexWriter w = new IntelHexWriter(new OutputStreamWriter(System.out));
//        w.addDataRecord(0x0190, new byte[] {0x56, 0x45, 0x52, 0x53, 0x49, 0x4F, 0x4E, 0x0D, 0x0A, 0x00, 0x0D, 0x0A, 0x41,
//                0x54, 0x0D, 0x0A});

        byte[] data = new byte[Math.abs(new Random().nextInt() % 1024)];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 0xff);
        }
        w.addData(0x10000 - 0x100, data, 16);
        w.done();
    }
}
