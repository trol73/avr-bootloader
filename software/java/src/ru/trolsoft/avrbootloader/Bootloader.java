package ru.trolsoft.avrbootloader;

/**
 * Created on 05.02.17.
 */
public class Bootloader {

    private final Device device;
    private DeviceInfo deviceInfo;


    public Bootloader(Device device) {
        this.device = device;
    }

    public int[] readAllFlash(boolean withBootloader) {
        return null;
    }

    public int[] readFlash(int offset, int size) {
        return null;
    }

    public void writeFlash(int data[]) {

    }

    private void findChangedPages() {

    }


    public static void main(String[] args) throws DeviceException {
        Device dev = new Device("/dev/tty.wchusbserial14220");
        dev.open(153600);
        System.out.println(dev.cmdSync(1));
        System.out.println(dev.cmdSync(2));
        System.out.println(dev.cmdSync(3));
        System.out.println(dev.cmdAbout());
    }
}
