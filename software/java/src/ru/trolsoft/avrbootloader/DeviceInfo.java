package ru.trolsoft.avrbootloader;

/**
 * Created on 31/01/17.
 */
public class DeviceInfo {
    /**
     * Bootloader signature, 'TSBL'
     */
    final String signature;

    /**
     * Bootloader version (2 bytes)
     */
    final int version;

    /**
     * Bootloader start offset
     */
    final int bootloaderStart;

    /**
     * Bootloader size in bytes
     */
    final int bootloaderSize;

    /**
     * Device page size in bytes
     */
    final int pageSize;

    /**
     * Device signature (3 bytes), can be undefined for some chips (if 0)
     */
    final int deviceSignature;

    DeviceInfo(String signature, int version, int bootloaderStart, int bootloaderSize, int pageSize, int deviceSignature) {
        this.signature = signature;
        this.version = version;
        this.bootloaderStart = bootloaderStart;
        this.bootloaderSize = bootloaderSize;
        this.pageSize = pageSize;
        this.deviceSignature = deviceSignature;
    }
}
