package ru.trolsoft.avrbootloader;

/**
 * Created on 12.02.17.
 */
public class Main {

    private static String VERSION = "0.2";

    private static void usage() {
        System.out.println("AVR bootloader, v" + VERSION + ". Copyright (c) TrolSoft, 2017");
        System.out.println("Usage: avrboot [@port] [/baudrate] r|w|v:<memory>:<file>");
    }
}
