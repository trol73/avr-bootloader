package ru.trolsoft.avrbootloader;

/**
 * Created on 09.02.17.
 */
public class Statistic {
    public int uartSendCount;
    public int uartReceiveCount;
    public int uartReadTimeoutsCount;

    public int avrFlashBytesReadCount;
    public int avrFlashBytesWriteCount;
    public int avrFlashPagesWriteCount;
    public int avrFlashPagesEraseCount;

    public long uartSendTime;
    public long uartReceiveTime;
    public long uartReadWithTimeoutTime;

    public long avrFullReadTime;
    public long avrFullWriteTime;



    /**
     * Reset all statistic values
     */
    public void reset() {
        uartSendCount = 0;
        uartReceiveCount = 0;
        uartReadTimeoutsCount = 0;

        avrFlashBytesReadCount = 0;
        avrFlashBytesWriteCount = 0;
        avrFlashPagesWriteCount = 0;

        uartSendTime = 0;
        uartReceiveTime = 0;
        uartReadWithTimeoutTime = 0;

        avrFullReadTime = 0;
        avrFullWriteTime = 0;
    }
}
