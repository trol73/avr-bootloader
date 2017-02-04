package ru.trolsoft.avrbootloader;

/**
 * Created on 25/01/17.
 */
interface Commands {
    int CMD_SYNC = 0;
    int CMD_ABOUT = 1;
    int CMD_READ_FLASH = 2;
    int CMD_READ_EEPROM = 3;
    int CMD_READ_FUSES = 4;
    int CMD_START_APP = 5;
    int CMD_ERASE_PAGE = 6;
    int CMD_WRITE_FLASH_PAGE = 7;
    int CMD_TRANSFER_PAGE = 8;
}
