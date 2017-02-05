package ru.trolsoft.avrbootloader;

import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;

import java.util.Random;

/**
 * Provide low-level interface to bootloader device and implement bootloader commands
 *
 * Created on 25/01/17.
 */
public class Device implements AutoCloseable, CommandCodes {

    private static final int DEFAULT_TIMEOUT = 1000;
    private final SerialPort serialPort;

    private boolean connected;

    /**
     *
     * @param serialPort
     */
    public Device(SerialPort serialPort) {
        this.serialPort = serialPort;
    }

    /**
     *
     * @param portName
     */
    public Device(String portName) {
        serialPort = new SerialPort(portName);
    }


    /**
     *
     * @param baudRate
     * @throws DeviceException
     */
    public boolean open(int baudRate) throws DeviceException {
        try {
            serialPort.openPort();
            serialPort.setParams(baudRate, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
        } catch (SerialPortException e) {
            throw new DeviceException(e);
        }
        // try to load all data (if exist)
        while (true) {
            try {
                readByte(250);
            } catch (SerialPortException e) {
                break;
            }
        }
        // synchronization
        int syncSuccess = 0;
        Random random = new Random();
        try {
            for (int i = 0; i < 512; i++) {
                int rnd = Math.abs(random.nextInt()) & 0xff;
                if (cmdSync(rnd) == rnd) {
                    if (syncSuccess++ >= 10) {
                        connected = true;
                        return true;
                    }
                }
            }
        } catch (DeviceException e) {
            e.printStackTrace();
        }
        close();
        return false;
    }

    /**
     *
     * @throws DeviceException
     */
    public void close() throws DeviceException {
        try {
            serialPort.closePort();
        } catch (SerialPortException e) {
            throw new DeviceException(e);
        }
    }


    /**
     *
     * @param val
     * @return
     * @throws DeviceException
     */
    public int cmdSync(int val) throws DeviceException {
        writeBytes(CMD_SYNC, val);
        return readByte();
    }


    /**
     *
     * @return
     * @throws DeviceException
     */
    public DeviceInfo cmdAbout() throws DeviceException {
        writeByte(CMD_ABOUT);
        return new DeviceInfo(
                readString(4),
                readWord(),
                readDword(),
                readWord(),
                readWord(),
                (readByte() << 16) + (readByte() << 8) + readByte()
        );
    }

    /**
     *
     * @param addressIn16bytePages
     * @param size16
     * @return
     * @throws DeviceException
     */
    public int[] cmdReadFlash(int addressIn16bytePages, int size16) throws DeviceException {
        writeByte(CMD_READ_FLASH);
        writeWord(addressIn16bytePages);
        writeWord(size16);
        int[] result = new int[size16];
        for (int i = 0; i < size16; i++) {
            result[i] = readByte();
        }
        return result;
    }

    /**
     *
     * @param address
     * @param size
     * @return
     * @throws DeviceException
     */
    public int[] cmdReadEeprom(int address, int size) throws DeviceException {
        writeByte(CMD_READ_EEPROM);
        writeWord(address);
        writeWord(size);
        int[] result = new int[size];
        for (int i = 0; i < size; i++) {
            result[i] = readByte();
        }
        return result;
    }

    /**
     *
     * @throws DeviceException
     */
    public void cmdStartApp() throws DeviceException {
        writeByte(CMD_START_APP);
    }


    /**
     *
     * @param pageNumber
     * @throws DeviceException
     */
    public void cmdErasePage(int pageNumber) throws DeviceException {
        writeByte(CMD_ERASE_PAGE);
        writeWord(pageNumber);
    }

    /**
     *
     * @param pageNumber
     * @return
     * @throws DeviceException
     */
    public boolean cmdWriteFlashPage(int pageNumber) throws DeviceException {
        writeByte(CMD_WRITE_FLASH_PAGE);
        writeWord(pageNumber);
        return readByte() == 0;
    }

    /**
     *
     * @param data
     * @throws DeviceException
     */
    public void cmdTransferPage(int[] data) throws DeviceException {
        writeByte(CMD_TRANSFER_PAGE);
        writeBytes(data);
    }

    private void writeByte(int singleByte) throws DeviceException {
System.out.println("-> " + singleByte);
        try {
            serialPort.writeInt(singleByte);
        } catch (SerialPortException e) {
            throw new DeviceException(e);
        }
    }

    private void writeWord(int word) throws DeviceException {
        writeByte(word >> 8);
        writeByte(word & 0xff);
    }

    private void writeBytes(int... vals) throws DeviceException {
        for (int v : vals) {
            writeByte(v);
        }
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


    private int readByte(int timeout) throws SerialPortException {
        try {
            int[] buf = serialPort.readIntArray(1, timeout);
            return buf[0];
        } catch (SerialPortTimeoutException e) {
            throw new SerialPortException(serialPort.getPortName(), "readByte", "timeout");
        }
    }

}
