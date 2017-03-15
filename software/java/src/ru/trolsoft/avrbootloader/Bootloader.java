package ru.trolsoft.avrbootloader;


import cz.jaybee.intelhex.*;
import ru.trolsoft.utils.files.DataBlock;
import ru.trolsoft.utils.files.IntelHexReader;
import ru.trolsoft.utils.files.IntelHexWriter;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Created on 05.02.17.
 */
public class Bootloader {

    private static final int BYTES_PER_LINE_IN_HEX = 32;

    private final Device device;
    private DeviceInfo deviceInfo;


    public Bootloader(Device device) {
        this.device = device;
    }

    /**
     *
     * @param withBootloader
     * @return
     * @throws DeviceException
     */
    public byte[] readAllFlash(boolean withBootloader) throws DeviceException {
        int size = getDeviceInfo().bootloaderStart;
        if (withBootloader) {
            size += getDeviceInfo().bootloaderSize;
        }
        return readFlash(0, size);
    }


    /**
     *
     * @param offset
     * @param size
     * @return
     * @throws DeviceException
     */
    public byte[] readFlash(int offset, int size) throws DeviceException {
        byte[] result = new byte[size];

        int pos = 0;
        while (size > 0) {
            int readSize = size <= 0xfff0 ? size : 0xfff0;
            byte[] read = device.cmdReadFlash(offset >> 4, readSize);
            System.arraycopy(read, 0, result, pos, readSize);
            pos += readSize;
            size -= readSize;
            offset += readSize;
        }
        return result;
    }

    /**
     * Read firmware to file
     *
     * @param withBootloader read firmware with bootloader if true
     * @param fileName filepath to store firmware
     * @throws IOException if error on file i/o occurred
     * @throws DeviceException on bootloader error
     */
    public void readFlashToFile(boolean withBootloader, String fileName) throws DeviceException, IOException {
        try (FileWriter f = new FileWriter(fileName)) {
            IntelHexWriter writer = new IntelHexWriter(f);
            byte[] data = readAllFlash(withBootloader);
            writer.addData(0, data, BYTES_PER_LINE_IN_HEX);
            writer.done();
        }
    }

    public void writeFlash(int offset, byte data[]) throws DeviceException {
System.out.println("writeFlash " + offset + " " + data.length);
        final int pageSize = getDeviceInfo().pageSize;
        final int bootloaderStart = getDeviceInfo().bootloaderStart;
        final int totalPages = bootloaderStart / pageSize;

        // align data block, fill unused space
        int firstPage = offset / pageSize;
        int pagesToWrite = data.length / pageSize;
        while (offset + data.length > offset + pagesToWrite * pageSize) {
            pagesToWrite++;
        }
        if (pagesToWrite > totalPages) {
            pagesToWrite = totalPages;
        }
        // prepare new data array that aligned to page size
        if (data.length != pagesToWrite * pageSize) {
            byte[] newData = new byte[pagesToWrite * pageSize];
            Arrays.fill(newData, (byte)0xff);
            for (int p = firstPage; p < firstPage + pagesToWrite; p++) {
                for (int i = 0; i < pageSize; i++) {
                    int o = p*pageSize + i;
                    if (o >= offset && o < offset + data.length) {
                        newData[o - firstPage*pageSize] = data[o - offset];
                    }
                }
            }
            data = newData;
        }

        // read pages
        byte[] readData = readFlash(firstPage*pageSize, pagesToWrite*pageSize);

        // find changes and update changed pages
        int skipCount = 0;
        int writeCount = 0;
        byte[] pageData = new byte[pageSize];
//System.out.println("? " + data.length*1.0 / pageSize + "   " + readData.length*1.0 / pageSize);
        for (int page = firstPage; page < firstPage + pagesToWrite; page++) {
            if (comparePages(firstPage*pageSize, data, readData, page)) {
                skipCount++;
                continue;
            }
            System.arraycopy(data, page*pageSize, pageData, 0, pageSize);
writeFlashPage(page, pageData);
System.out.println("Write page "+ page);
            writeCount++;
        }
    }

    public void writeFlashFromFile(String fileName) throws IOException, IntelHexException, DeviceException {
        try (FileInputStream is = new FileInputStream(fileName)) {
            List<DataBlock> blocks = new IntelHexReader(is).read();
            //System.out.println(blocks);
            for (DataBlock data : blocks) {
                writeFlash(data.getAddress(), data.getData());
            }
        }
    }

    /**
     * Compare two pages in data arrays
     *
     * @param blockStart offset of first byte form data1 and data2 in the device
     * @param data1 data to compare
     * @param data2 data to compare
     * @param page page number
     * @return true if pages are equals
     * @throws DeviceException on device error (for cmdAbout)
     */
    private boolean comparePages(int blockStart, byte[] data1, byte[] data2, int page) throws DeviceException {
        final int pageSize = getDeviceInfo().pageSize;
        for (int i = 0; i < pageSize; i++) {
            int offset = page*pageSize + i - blockStart;
            if (data1[offset] != data2[offset]) {
                return false;
            }
        }
        return true;
    }

    private void writeFlashPage(int pageNumber, byte[] data) throws DeviceException {
System.out.println("write page " + pageNumber);
        device.cmdTransferPage(data);
        device.cmdErasePage(pageNumber);
        device.cmdWriteFlashPage(pageNumber);
    }

//    private void writeFlashBlock(int offset, byte[] data) throws DeviceException {
//        int pageSize = getDeviceInfo().pageSize;
//        int bootloaderStart = getDeviceInfo().bootloaderStart;
//        int totalPages = bootloaderStart / pageSize;
//
//    }
//
//    private void findChangedPages() {
//
//    }

    private DeviceInfo getDeviceInfo() throws DeviceException {
        if (deviceInfo == null) {
            deviceInfo = device.cmdAbout();
        }
        return deviceInfo;
    }


    public static void main(String[] args) throws DeviceException, IOException, IntelHexException {
        //Device dev = new Device("/dev/tty.wchusbserial14220");
        //dev.open(153600);

        Device dev = new Device("/dev/tty.wchusbserial14230");
        dev.open(230400);

        Bootloader bootloader = new Bootloader(dev);
//        bootloader.readFlashToFile(false, "/Users/trol/Projects/radio/avr-bootloader/read_firmware.hex");
        bootloader.readFlashToFile(true, "/Users/trol/Projects/radio/avr-bootloader/read_firmware_with_loader.hex");
        bootloader.writeFlashFromFile("/Users/trol/Projects/radio/avr-lcd-module-128x128/build/avr-lcd-module-128x128.hex");

        bootloader.device.cmdStartApp();
    }
}
