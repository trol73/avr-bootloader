package ru.trolsoft.avrbootloader;

/**
 * @author Trol
 * Created on 27/04/17.
 */
public interface BootloaderListener extends ProgressListener {

    enum Operation {
        DISCONNECTED,
        INIT,
        CONNECTED,
        READ_FLASH,
        WRITE_FLASH,
        ERASE_FLASH,
        READ_EEPROM,
        WRITE_EEPROM,
        READ_FUSES,
        CLOSE
    }

    void onOperation(Operation operation);
}
