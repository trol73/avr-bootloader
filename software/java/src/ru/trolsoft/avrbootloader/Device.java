package ru.trolsoft.avrbootloader;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;

import javax.swing.*;
import java.util.Random;

import static ru.trolsoft.avrbootloader.BootloaderListener.Operation.*;

/**
 * Provide low-level interface to bootloader device and implement bootloader commands
 *
 * Created on 25/01/17.
 */
public class Device implements AutoCloseable, CommandCodes {

    private static final int DEFAULT_TIMEOUT = 1000;

    private static final String SIGNATURE = "TSBL";
    private final SerialPort serialPort;
    private  BootloaderListener listener;

    private Statistic statistic;
    private boolean connected;

    /**
     *
     * @param serialPort
     */
    public Device(SerialPort serialPort) {
        this.serialPort = serialPort;
        this.connected = serialPort.isOpened();
    }

    /**
     *
     * @param portName
     */
    public Device(String portName) {
        serialPort = new SerialPort(portName);
        notifyOperation(DISCONNECTED);
    }



    public boolean initBootloader(int baudRate, int cmd[]) {
        // send bootloader initialization command
        boolean result = true;
        try {
            serialPort.openPort();
            serialPort.setParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        } catch (SerialPortException e) {
//            e.printStackTrace();
            result = false;
        }

        if (result) {
            tryToReadAllData();
            if (cmd != null) {
                try {
                    writeBytes(cmd);
                    result = true;
                } catch (DeviceException e) {
//                    e.printStackTrace();
                }
            } else {
                result = true;
            }
        }
        connected = result;
        return result;
    }


//    public boolean checkConnected() {
//        for (int attempt = 0; attempt < 5; attempt++) {
//            tryToReadAllData();
//            int v = attempt * 30 + 10;
//            try {
//                if (cmdSync(v) == v) {
//                    cmdAbout();
//                    return true;
//                }
//            } catch (DeviceException e) {
//                e.printStackTrace();
//            }
//        }
//        return false;
//    }



    /**
     *
     * @param baudRate
     * @throws DeviceException if any serial i/o or device error caused
     */
    public boolean open(int baudRate) throws DeviceException {
        try {
            serialPort.openPort();
            serialPort.setParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        } catch (SerialPortException e) {
            throw new DeviceException(e);
        }
        if (sync()) {
            return true;
        }

        close();
        return false;
    }

    public boolean sync() {
        notifyOperation(INIT);
        tryToReadAllData();

        int syncSuccess = 0;
        Random random = new Random();
        try {
            for (int i = 0; i < 30; i++) {
                int rnd = syncSuccess == 0 ? 0 : Math.abs(random.nextInt()) & 0xff;
                System.out.println("try to sync " + rnd);
                try {
                    if (cmdSync(rnd) == rnd) {
                        if (syncSuccess++ >= 3) {
                            connected = true;
                            notifyOperation(CONNECTED);
                            return true;
                        }
                    } else {
                        syncSuccess = 0;
                    }
                } catch (SerialPortException e) {
                    syncSuccess = 0;
                }
            }
        } catch (DeviceException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Try to load all data (if exist)
     */
    private void tryToReadAllData() {
        while (true) {
            try {
                int v = readByte(250);
System.out.println("try to read all " + v);
            } catch (SerialPortException e) {
                break;
            }
        }
    }


    /**
     *
     * @throws DeviceException if any serial i/o or device error caused
     */
    public void close() throws DeviceException {
        connected = false;
        try {
            serialPort.closePort();
        } catch (SerialPortException e) {
            throw new DeviceException(e);
        }
    }


    /**
     *
     * @param val value to send
     * @return received value
     * @throws DeviceException if any serial i/o or device error caused
     */
    public int cmdSync(int val) throws DeviceException, SerialPortException {
        writeBytes(CMD_SYNC, val);
        int response = readByte(100);
        System.out.println("sync " + val + " -> " + response);
        return response;
    }


    /**
     *
     * @return {@link DeviceInfo} information about microcontroller and bootloader
     * @throws DeviceException if any serial i/o or device error caused
     */
    public DeviceInfo cmdAbout() throws DeviceException {
        writeByte(CMD_ABOUT);
        String signature = readString(4);
        if (!SIGNATURE.equals(signature)) {
            throw new DeviceException("Bad signature: " + signature);
        }
        return new DeviceInfo(
                signature,
                readWord(),
                readDword(),
                readWord(),
                readWord(),
                (readByte() << 16) + (readByte() << 8) + readByte()
        );
    }

    /**
     * Read flash block
     *
     * @param addressIn16bytePages address in 16 byte block (i.e. byte address / 16)
     * @param size16 number of bytes to read (0..FFFF)
     * @return byte array with read data
     * @throws DeviceException if any serial i/o or device error caused
     */
    public byte[] cmdReadFlash(int addressIn16bytePages, int size16, ProgressListener progressListener) throws DeviceException {
        notifyOperation(READ_FLASH);
        writeByte(CMD_READ_FLASH);
        writeWord(addressIn16bytePages);
        writeWord(size16);
        byte[] result = new byte[size16];
        for (int i = 0; i < size16; i++) {
            result[i] = (byte)readByte();
//System.out.println("read byte #" +  i + " " + Integer.toHexString(result[i] & 0xff));
            if (progressListener != null) {
                progressListener.onProgress(100f*(i+1)/size16);
            }
        }
        return result;
    }

    /**
     *
     * @param address address (in bytes)
     * @param size number of bytes to read
     * @return byte array with read data
     * @throws DeviceException if any serial i/o or device error caused
     */
    public byte[] cmdReadEeprom(int address, int size) throws DeviceException {
        notifyOperation(READ_EEPROM);
        writeByte(CMD_READ_EEPROM);
        writeWord(address);
        writeWord(size);
        byte[] result = new byte[size];
        for (int i = 0; i < size; i++) {
            result[i] = (byte)readByte();
        }
        return result;
    }


    /**
     * Read fuse bytes
     *
     * @return fuses
     * @throws DeviceException if any serial i/o or device error caused
     */
    public Fuses cmdReadFuses() throws DeviceException {
        notifyOperation(READ_FUSES);
        writeByte(CMD_READ_FUSES);
        int lo = readByte() & 0xff;
        int ex = readByte() & 0xff;
        int hi = readByte() & 0xff;
        return new Fuses(lo, ex, hi);
    }

    /**
     * Exit from bootloader and run main firmware
     *
     * @throws DeviceException if any serial i/o or device error caused
     */
    public void cmdStartApp() throws DeviceException {
        writeByte(CMD_START_APP);
        notifyOperation(CLOSE);
    }


    /**
     * Erase flash page (and prepare it for writing)
     *
     * @param pageNumber number of page
     * @throws DeviceException if any serial i/o or device error caused
     */
    public void cmdErasePage(int pageNumber) throws DeviceException {
        notifyOperation(ERASE_FLASH);
        writeByte(CMD_ERASE_PAGE);
        writeWord(pageNumber);
        if (statistic != null) {
            statistic.avrFlashPagesEraseCount++;
        }
    }

    /**
     * Write page to flash. Page must be preloaded with {@link #cmdTransferPage(byte[])} and erased with {@link #cmdErasePage(int)}.
     *
     * @param pageNumber page number (0..FFFF)
     * @return true
     * @throws DeviceException if any serial i/o or device error caused
     */
    public boolean cmdWriteFlashPage(int pageNumber) throws DeviceException {
        notifyOperation(WRITE_FLASH);
        writeByte(CMD_WRITE_FLASH_PAGE);
        writeWord(pageNumber);
        if (statistic != null) {
            statistic.avrFlashPagesWriteCount++;
        }
        return readByte() == 0;
    }

    /**
     * Transfer flash page to the microcontroller memory
     * @param data page data
     * @throws DeviceException if any serial i/o or device error caused
     */
    public void cmdTransferPage(byte[] data) throws DeviceException {
        writeByte(CMD_TRANSFER_PAGE);
        writeBytes(data);
    }

    public boolean isConnected() {
        return connected;
    }

    /**
     * Set object to collect statistic
     * @return object to collect statistic
     */
    public Statistic getStatistic() {
        return statistic;
    }

    /**
     * Set object to collect statistic
     * @param statistic object to collect statistic
     */
    public void setStatistic(Statistic statistic) {
        this.statistic = statistic;
    }

    public void setListener(BootloaderListener listener) {
        this.listener = listener;
    }

    private void writeWord(int word) throws DeviceException {
        writeByte(word >> 8);
        writeByte(word & 0xff);
    }

    protected void writeBytes(int... vals) throws DeviceException {
        for (int v : vals) {
            writeByte(v);
        }
    }

    private void writeBytes(byte... vals) throws DeviceException {
        int ints[] = new int[vals.length];
        for (int i = 0; i < vals.length; i++) {
            ints[i] = vals[i];
        }
        writeBytes(ints);
    }

    private int readByte() throws DeviceException {
        try {
            return readByte(DEFAULT_TIMEOUT);
        } catch (SerialPortException e) {
            throw new DeviceException(e);
        }
    }

    private int readWord() throws DeviceException {
        return (readByte() << 8) + readByte();
    }

    private int readDword() throws DeviceException {
        return (readWord() << 16) + readWord();
    }

    private char readChar() throws DeviceException {
        return (char)readByte();
    }

    private String readString(int len) throws DeviceException {
        String result = "";
        for (int i = 0; i < len; i++) {
            result += readChar();
        }
        return result;
    }


    protected int readByte(int timeout) throws SerialPortException {
        long t = System.currentTimeMillis();
        try {
            int[] buf = serialPort.readIntArray(1, timeout);
            t = System.currentTimeMillis() - t;
            if (statistic != null) {
                statistic.uartReceiveCount++;
                statistic.uartReceiveTime += t;
            }
//System.out.println("<< 0x" + Integer.toHexString(buf[0]));
            return buf[0];
        } catch (SerialPortTimeoutException e) {
            t = System.currentTimeMillis() - t;
            if (statistic != null) {
                statistic.uartReadTimeoutsCount++;
                statistic.uartReadWithTimeoutTime += t;
            }
            throw new SerialPortException(serialPort.getPortName(), "readByte", "timeout");
        }
    }

    protected void writeByte(int singleByte) throws DeviceException {
//System.out.println("write > " + singleByte);
        try {
            long t = System.currentTimeMillis();
            serialPort.writeInt(singleByte);
            if (statistic != null) {
                statistic.uartSendCount++;
                statistic.uartSendTime += t;
            }
        } catch (SerialPortException e) {
            throw new DeviceException(e);
        }
    }


    private void notifyOperation(BootloaderListener.Operation operation) {
System.out.println(operation);
        if (listener != null) {
            listener.onOperation(operation);
        }
    }


}
