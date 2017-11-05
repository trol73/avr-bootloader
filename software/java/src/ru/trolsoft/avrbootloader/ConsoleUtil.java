package ru.trolsoft.avrbootloader;

import cz.jaybee.intelhex.IntelHexException;
import jssc.SerialNativeInterface;
import org.apache.commons.cli.*;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class ConsoleUtil {
    enum Operation {
        READ, WRITE, VERIFY, INFO
    }

    private Options options;

    private void process(String args[]) {
        Operation operation = getOperation(args);

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd;
        try {
            cmd = parser.parse(getOptions(), args);
        } catch (ParseException e) {
            printUsage();
            System.exit(1);
            return;
        }
        String portBaudrate = cmd.getOptionValue("bauds");
        Integer baudrate;
        try {
            baudrate = portBaudrate != null ? Integer.parseInt(portBaudrate) : null;
        } catch (Exception e) {
            System.err.println("wrong baudrate: " + portBaudrate);
            System.exit(1);
            return;
        }
        String fileName = cmd.getOptionValue("file");
        String portName = cmd.getOptionValue("port");
        if (operation == Operation.READ || operation == Operation.WRITE || operation == Operation.VERIFY) {
            if (fileName == null) {
                System.err.println("firmware file doesn't defined");
                printUsage();
                System.exit(1);
            }
            if (!new File(fileName).exists() && (operation == Operation.WRITE || operation == Operation.VERIFY)) {
                System.err.println("file not found: " + fileName);
                System.exit(1);
            }
        }
        if (portName == null) {
            portName = selectPortName();
            System.out.println("Serial port: " + portName);
        }

        Device dev;
        if (baudrate != null) {
System.out.println("baudrate " + baudrate);
            dev = Bootloader.tryConnect(portName, baudrate, null, 5, null);
        } else {
            dev = Bootloader.tryConnect(portName, 57600, null, 5, null);
            if (dev != null) {
                System.out.println("Baudrate detected: " + 57600);
            }
            dev = Bootloader.tryConnect(portName, 76800, null, 5, null);
            if (dev != null) {
                System.out.println("Baudrate detected: " + 76800);
            }
            dev = Bootloader.tryConnect(portName, 153600, null, 5, null);
            if (dev != null) {
                System.out.println("Baudrate detected: " + 153600);
            }
            dev = Bootloader.tryConnect(portName, 230400, null, 5, null);
            if (dev != null) {
                System.out.println("Baudrate detected: " + 230400);
            }
        }
        if (dev == null) {
            System.err.println("Device not found");
            System.exit(2);
        }
        Bootloader bootloader = new Bootloader(dev);
        switch (operation) {
            case INFO:
                for (int i = 0; i < 500; i++)
                bootloaderInfo(bootloader);
                break;
            case READ:
                try {
                    bootloader.readFlashToFile(true, fileName);
                } catch (IOException | DeviceException e) {
                    e.printStackTrace();
                }
                break;
            case WRITE:
                try {
                    bootloader.writeFlashFromFile(fileName);
                } catch (IOException | IntelHexException | DeviceException e) {
                    e.printStackTrace();
                }
                break;
            case VERIFY:
                break;
        }
        if (cmd.hasOption("start")) {
            if (bootloader.startApp()) {
                System.out.println("Main firmware started successfully");
            } else {
                System.err.println("Can't start main firmware");
            }
        }

    }

    private void bootloaderInfo(Bootloader bootloader) {
        try {
            System.out.println(bootloader.getDeviceInfo());
        } catch (DeviceException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) throws DeviceException, IOException, IntelHexException {
        new ConsoleUtil().process(args);
    }

    private void printUsage() {
        String commands = "Commands:\n"+
                "  write      write firmware to device\n" +
                "  read       read firmware from device\n" +
                "  verify     verify device firmware\n" +
                "  info       show info about bootloader\n";
        new HelpFormatter().printHelp("bootloader <command> [options]\n" + commands +"Options:", getOptions());
    }

    private  Options getOptions() {
        if (options == null) {
            options = new Options();

            Option port = new Option("p", "port", true, "serial port name");
            port.setRequired(false);
            options.addOption(port);

            Option bauds = new Option("b", "bauds", true, "serial baudrate");
            bauds.setRequired(false);
            bauds.setRequired(false);
            options.addOption(bauds);

            Option filename = new Option("f", "file", true, "firmware filename");
            filename.setRequired(false);
            options.addOption(filename);

            Option start = new Option("s", "start", false, "start firmware");
            start.setRequired(false);
            options.addOption(start);


//            options.addOption(new Option("read", null, false, "read firmware"));
//            options.addOption(new Option("write", null, false, "write firmware"));
//            options.addOption(new Option("verify", null, false, "verify firmware"));
//            options.addOption(new Option("info", null, false, "show bootloader info"));
        }
        return options;
    }

    private Operation getOperation(String args[]) {
        for (String s : args) {
            if ("read".equalsIgnoreCase(s)) {
                return Operation.READ;
            } else if ("write".equalsIgnoreCase(s)) {
                return Operation.WRITE;
            } else if ("verify".equalsIgnoreCase(s)) {
                return Operation.VERIFY;
            } else if ("info".equalsIgnoreCase(s)) {
                return Operation.INFO;
            }
        }
        System.err.println("Unknown command");
        printUsage();
        System.exit(1);
        return null;
    }

    private String selectSerialPort(String[] ports) {
        List<String> suitable = new ArrayList<>();
        for (String port : ports) {
            if (SerialNativeInterface.getOsType() == SerialNativeInterface.OS_MAC_OS_X) {
                if (port.contains("cu.wchusbserial")) {
                    suitable.add(port);
                    continue;
                }
            }
        }

        return suitable.size() == 1 ? suitable.get(0) : null;
    }


    private String selectPortName() {
        String ports[] = SerialPortList.getPortNames();
        if (ports.length == 0) {
            System.err.println("Serial port doesn't defined");
            System.exit(1);
        }
        String defaultPort = selectSerialPort(ports);
        if (defaultPort != null) {
            return defaultPort;
        }
        System.out.println("Select serial port:");
        int i = 1;
        for (String s : ports) {
            System.out.println("\t[" +i + "]\t" + s);
            i++;
        }
        Scanner reader = new Scanner(System.in);
        System.out.print("Select port (1.." + ports.length + "): ");
        try {
            return ports[reader.nextInt()-1];
        } catch (Exception e) {
            System.out.println("Wrong choice");
            System.exit(1);
            return null;
        }

    }


}
