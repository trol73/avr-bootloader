package ru.trolsoft.utils.files;

import cz.jaybee.intelhex.*;
import cz.jaybee.intelhex.listeners.RangeDetector;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created on 12.02.17.
 */
public class IntelHexReader {

    private final InputStream is;

    public IntelHexReader(InputStream is) {
        if (is instanceof BufferedInputStream) {
            this.is = is;
        } else {
            this.is = new BufferedInputStream(is);
        }
    }

//    public IntelHexReader(String fileName) throws FileNotFoundException {
//        this(new FileInputStream(fileName));
//    }

    public List<DataBlock> read() throws IntelHexException, IOException {
        Parser parser = new Parser(is);

        // calculate maximum output range and read data
        byte[] buf = new byte[1024*1024];
        RangeDetector rangeDetector = new RangeDetector() {
            @Override
            public void data(int address, byte[] data) {
                super.data(address, data);
                System.arraycopy(data, 0, buf, address, data.length);
            }
        };
        parser.setDataListener(rangeDetector);
        parser.parse();

        MemoryRegions regions = rangeDetector.getMemoryRegions();
        List<DataBlock> result = new ArrayList<>();
        for (int ri = 0; ri < regions.size(); ri++) {
            Region region = regions.get(ri);
            System.out.println("Region[" + ri + "] - "  + region);
            int address = region.getAddressStart();
            int size = region.getLength();
            byte[] data = new byte[size];
            System.arraycopy(buf, address, data, 0, size);
            result.add(new DataBlock(address, data));
        }
        return result;
    }
}
