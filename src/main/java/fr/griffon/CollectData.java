package fr.griffon;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;

/**
 * Classe permettant de récolter des données de la FC KISS pendant une durée.
 */
public class CollectData {
    private final static byte[] EMPTY_BYTE_ARRAY = new byte[]{};

    public static void main(String[] args) {
        // Durée en millisecondes
        long duration = 5 * 60 * 1000;
        KissSimulator.KissCommand command = KissSimulator.KissCommand.GET_GPS;
        String portName = "/dev/cu.SLAB_USBtoUART";
        String fileName = "receveiveData.byte";

        command = KissSimulator.KissCommand.GET_GPS;

        CollectData collectData = new CollectData(command, duration, portName, fileName);
        collectData.start();
    }

    private boolean running = true;
    private SerialPort serialPort;
    private Boolean receive;
    private Date startDate;
    private CollectDataListener collectDataListener;
    private long extractDuration;
    private KissSimulator.KissCommand command;

    public CollectData(KissSimulator.KissCommand command, long extractDuration, String portName, String fileName) {
        this.command = command;
        this.extractDuration = extractDuration;
        serialPort = new SerialPort(portName);
        collectDataListener = new CollectDataListener(fileName);
    }

    public void start() {
        try {
            System.out.println("Port opened: " + serialPort.openPort());
            serialPort.setParams(SerialPort.BAUDRATE_115200, SerialPort.DATABITS_8, SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
            serialPort.addEventListener(collectDataListener);

            Thread.sleep(1000);
            receive = null;
            startDate = new Date();
            Date last = new Date();
            while (running) {
                if (!Boolean.FALSE.equals(receive) || (new Date()).getTime() - last.getTime() > 10) {
                    //running = false;
                    long recordedTime = (new Date()).getTime() - startDate.getTime();
                    if ((extractDuration != 0 && recordedTime >= extractDuration) || (extractDuration == 0 && Boolean.TRUE.equals(receive))) {
                        running = false;
                        break;
                    }
                    receive = false;
                    Thread.sleep(2);
                    sendCommand(command);

                    last = new Date();
                }
            }
            System.out.println("Port closed: " + serialPort.closePort());
        } catch (SerialPortException | InterruptedException ex) {
            System.out.println(ex);
        }
    }

    private void sendCommand(KissSimulator.KissCommand command) throws SerialPortException {
        byte[] toWrite;
        if (command.isNeedChecksum()) {
            int checksum = ByteUtils.computeChecksum(EMPTY_BYTE_ARRAY);
            toWrite = new byte[]{command.getByteCommand(), ByteUtils.intToByte(EMPTY_BYTE_ARRAY.length), ByteUtils.intToByte(checksum)};
        } else {
            toWrite = new byte[]{command.getByteCommand()};
        }

        System.out.println("send");
        serialPort.writeBytes(toWrite);
    }

    private class CollectDataListener implements SerialPortEventListener {

        private Path path;

        public CollectDataListener(String fileName) {
            this.path = Paths.get(fileName);
        }

        @Override
        public void serialEvent(SerialPortEvent serialPortEvent) {
            if (serialPortEvent.isRXCHAR() && serialPortEvent.getEventValue() > 0) {
                try {
                    byte[] readed = serialPort.readBytes();
                    byte[] timeAndSize = getTimeAndSize(readed.length);
                    Files.write(path, timeAndSize, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    Files.write(path, readed, StandardOpenOption.APPEND);
                    System.out.println("receive");
                    receive = true;
                } catch (SerialPortException | IOException ex) {
                    System.out.println("Error in receiving string from COM-port: " + ex);
                }
            }
        }

        private byte[] getTimeAndSize(int length) {
            long time = (new Date()).getTime() - startDate.getTime();
            byte[] timeInBytes = ByteUtils.longToBytes(time);
            byte[] result = new byte[timeInBytes.length + 1];
            for (int l = 0; l < timeInBytes.length; l++) {
                result[l] = timeInBytes[l];
            }
            result[timeInBytes.length] = ByteUtils.intToByte(length);
            return result;
        }
    }

}

