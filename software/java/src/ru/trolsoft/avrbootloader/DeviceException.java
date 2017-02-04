package ru.trolsoft.avrbootloader;

/**
 * Created on 25/01/17.
 */
public class DeviceException extends Exception {

    public DeviceException() {
        super();
    }

    public DeviceException(String message) {
        super(message);
    }

    public DeviceException(String message, Throwable cause) {
        super(message, cause);
    }

    public DeviceException(Throwable cause) {
        super(cause);
    }

}
