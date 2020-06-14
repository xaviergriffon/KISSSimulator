package fr.griffon;


import fr.griffon.enums.*;
import fr.griffon.input.ReadJoystickEvent;
import fr.griffon.utils.ByteUtils;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

import java.nio.ByteBuffer;
import java.util.Calendar;

/**
 * Simulateur de communication avec une FC KISS.
 */
public class KissSimulator {
    public static final int KISS_VERSION = 122;
    private boolean running = true;
    private SerialPort serialPort;
    private ReadJoystickEvent readJoystickEvent;
    private int mode = 0;
    private boolean logSetCommand = false;
    private boolean logGetCommand = false;

    public KissSimulator() {
        readJoystickEvent = new ReadJoystickEvent();
        serialPort = new SerialPort(ConfigurationManager.getInstance().getConfiguration().getPortName());
    }

    public static void main(String[] args) {
        Configuration configuration = ConfigurationManager.getInstance().getConfiguration();
        KissSimulator kissSimulator = new KissSimulator();
        kissSimulator.setLogSetCommand(configuration.isLogSetCommand());
        kissSimulator.setLogGetCommand(configuration.isLogGetCommand());
        kissSimulator.start();
    }

    public static void logPIDFromSetPIDSbytes(byte[] bytes) {
        String[] axes = {"Roll", "Pitch", "Yaw"};
        String[] pid = {"P", "I", "D"};
        int index = 1;
        for (String axis : axes) {
            System.out.print(axis + " ");
            for (String l : pid) {
                System.out.print(" " + l + ":");
                int value = ByteUtils.bytesToInt16(new byte[]{bytes[index], bytes[index + 1]});
                index += 2;
                System.out.print(value / 1000f);
            }
            System.out.println();
        }
    }

    public static void logFitlersFromSetFiltersbytes(byte[] bytes) {
        int index = 2;
        System.out.println("R/P LPF : " + Filters.valueToString(bytes[index]));
        index++;
        System.out.println("Yaw Filter : " + bytes[index]);
        index++;
        System.out.println("NF Roll on/off : " + OnOff.valueToString(bytes[index]));
        index++;
        int value = ByteUtils.bytesToInt16(new byte[]{bytes[index], bytes[index + 1]});
        index += 2;
        System.out.println("NF Roll center : " + value);
        value = ByteUtils.bytesToInt16(new byte[]{bytes[index], bytes[index + 1]});
        index += 2;
        System.out.println("NF Roll cutoff : " + value);

        System.out.println("NF Pitch on/off : " + OnOff.valueToString(bytes[index]));
        index++;
        value = ByteUtils.bytesToInt16(new byte[]{bytes[index], bytes[index + 1]});
        index += 2;
        System.out.println("NF Pitch center : " + value);
        value = ByteUtils.bytesToInt16(new byte[]{bytes[index], bytes[index + 1]});
        index += 2;
        System.out.println("NF Pitch cutoff : " + value);

        System.out.println("Yaw LPF : " + Filters.valueToString(bytes[index]));
        index++;
        System.out.println("Dterm LPF : " + Filters.valueToString(bytes[index]));
        index++;
    }

    public static void logVTXfromSetVTX(byte[] bytes) {
        int index = 2;
        System.out.println("VTX Type : " + VTXType.valueToString(ByteUtils.byteToInt8(bytes[index])));
        index++;
        int value = ByteUtils.byteToInt8(bytes[index]);
        int channel = value;
        System.out.println("N° channel : " + value);

        value = channel / 8;
        System.out.println("Band : " + Band.valueToString(value));
        value = channel % 8;
        System.out.println("Channel : " + (value + 1));
        index++;

        value = ByteUtils.bytesToInt16(new byte[]{bytes[index], bytes[index + 1]});
        index += 2;
        System.out.println("Low Power : " + value);
        value = ByteUtils.bytesToInt16(new byte[]{bytes[index], bytes[index + 1]});
        index += 2;
        System.out.println("Max Power : " + value);

    }

    public static void logGPSFromGetGPSbytes(byte[] bytes) {
        int latitudeIdx = 0; // 32
        int longitudeIdx = 4; // 32
        int speedIdx = 8; // 16
        int courseIdx = 10; // 16
        int altitudeIdx = 12; // 16
        int fixIdx = 14; // 8
        System.out.print("latitude : ");
        System.out.println(ByteUtils.bytesToInt32(ByteUtils.extractByte(bytes, latitudeIdx + 2, 4)));

        System.out.print("longitude : ");
        System.out.println(ByteUtils.bytesToInt32(ByteUtils.extractByte(bytes, longitudeIdx + 2, 4)));

        System.out.print("speed : ");
        System.out.println(ByteUtils.bytesToInt16(ByteUtils.extractByte(bytes, speedIdx + 2, 2)));

        System.out.print("course : ");
        System.out.println(ByteUtils.bytesToInt16(ByteUtils.extractByte(bytes, courseIdx + 2, 2)));

        System.out.print("altitude : ");
        System.out.println(ByteUtils.bytesToInt16(ByteUtils.extractByte(bytes, altitudeIdx + 2, 2)));

        System.out.print("fix : ");
        int fix = ByteUtils.bytesToInt8(ByteUtils.extractByte(bytes, fixIdx + 2, 1));
        System.out.println(fix >> 7);
        System.out.print("sat : ");
        System.out.println(fix & 0x7F);

    }

    public static void logRATEFromSetRATESbytes(byte[] bytes) {
        String[] axes = {"Roll", "Pitch", "Yaw"};
        String[] rate = {"RC", "Rate", "Curve"};
        int index = 1;
        for (String axis : axes) {
            System.out.print(axis + " ");
            for (String l : rate) {
                System.out.print(" " + l + ":");
                int value = ByteUtils.bytesToInt16(new byte[]{bytes[index], bytes[index + 1]});
                index += 2;
                System.out.print(value / 1000f);
            }
            System.out.println();
        }
    }

    public boolean isLogSetCommand() {
        return logSetCommand;
    }

    public void setLogSetCommand(boolean logSetCommand) {
        this.logSetCommand = logSetCommand;
    }

    public boolean isLogGetCommand() {
        return logGetCommand;
    }

    public void setLogGetCommand(boolean logGetCommand) {
        this.logGetCommand = logGetCommand;
    }

    public void start() {
        readJoystickEvent.start();
        CommandResponse.initAndStartBuffers();
        try {
            System.out.println("Port opened: " + serialPort.openPort());
            serialPort.setParams(115200, 8, 1, 0);
            serialPort.addEventListener(new KissSimulatorEventListener());
            while (running) {
                Thread.sleep(2);
            }
            System.out.println("Port closed: " + serialPort.closePort());
            readJoystickEvent.stop();
            CommandResponse.stopAllBuffers();;
            Thread.sleep(500);
        } catch (SerialPortException | InterruptedException ex) {
            System.out.println(ex);
        }
    }

    private void logReceiveCommand(KissCommand kissCommand, byte[] bytes) {
        logCommand("Receive", kissCommand, bytes);
    }

    private void logSendCommand(KissCommand kissCommand, byte[] bytes) {
        logCommand("Send", kissCommand, bytes);
    }

    private void logCommand(String direction, KissCommand kissCommand, byte[] bytes) {
        if (kissCommand == null || (kissCommand.isGetCommand() && !logGetCommand) || (kissCommand.isSetCommand() && !logSetCommand)) {
            return;
        }

        StringBuffer log = new StringBuffer();
        log.append(kissCommand.toString());
        log.append(" ").append(direction).append(" : ");
        log.append(ByteUtils.bytesToHex(bytes));
        System.out.println(log.toString());
    }

    private class KissSimulatorEventListener implements SerialPortEventListener {
        private byte[] serialBuffer = new byte[200];

        @Override
        public void serialEvent(SerialPortEvent serialPortEvent) {
            if (serialPortEvent.isRXCHAR() && serialPortEvent.getEventValue() > 0) {
                try {
                    byte[] readed = serialPort.readBytes(serialPortEvent.getEventValue());
                    if (readed.length > 0) {
                        byte byteCommand = readed[0];
                        KissCommand kissCommand = KissCommand.fromByte(byteCommand);
                        logReceiveCommand(kissCommand, readed);
                        switch (kissCommand) {
                            case GET_TELEMETRY:
                                sendTelemetry();
                                break;
                            case GET_MESSAGE:
                                sendMessage();
                                break;
                            case SET_RATES:
                                byte[] ratesSetting = new byte[readed.length - 1];
                                for (int i = 1; i < readed.length; i++) {
                                    ratesSetting[i - 1] = readed[i];
                                }
                                logRATEFromSetRATESbytes(ratesSetting);
                                break;
                            case SET_PIDS:
                                byte[] pidsSetting = new byte[readed.length - 1];
                                for (int i = 1; i < readed.length; i++) {
                                    pidsSetting[i - 1] = readed[i];
                                }
                                logPIDFromSetPIDSbytes(pidsSetting);
                                break;
                            case SET_FILTERS:
                                logFitlersFromSetFiltersbytes(readed);
                                break;
                            case SET_VTX:
                                logVTXfromSetVTX(readed);
                                break;
                            default:
                                CommandResponse commandResponse = ConfigurationManager.getInstance().getConfiguration()
                                        .getResponseForCommand(ByteUtils.byteToHex(byteCommand));
                                if (commandResponse != null) {
                                    if ("54".equals(commandResponse.getHexCommand())) {
                                        logGPSFromGetGPSbytes(commandResponse.getResponse());
                                    }
                                    write(kissCommand, commandResponse.getResponse());
                                } else {
                                    System.out.print("unknown instruction :");
                                    System.out.println(ByteUtils.bytesToHex(readed));
                                }
                        }
                    }

                } catch (SerialPortException ex) {
                    System.out.println("Error in receiving string from COM-port: " + ex);
                }
            } else {
                System.out.println(serialPortEvent.toString());
                running = false;
            }
        }

        private void sendTelemetry() throws SerialPortException {
            ByteBuffer byteBuffer = ByteBuffer.allocate(serialBuffer.length + 3);
            int KISSFRAMEINIT = 5;
            byteBuffer.put(ByteUtils.intToByte(KISSFRAMEINIT));
            initTelemetryBuffer();
            byteBuffer.put(ByteUtils.shortToByte((short) serialBuffer.length));
            byteBuffer.put(serialBuffer);
            int checksum = ByteUtils.computeChecksum(serialBuffer);
            byte check = ByteUtils.intToByte(checksum / serialBuffer.length);
            byteBuffer.put(check);
            write(KissCommand.GET_TELEMETRY, byteBuffer.array());
        }

        private void sendMessage() throws SerialPortException {
            String message = "Text long de plus de 21";
            int duration = 3000;//3s
            int priority = 0;
            ByteBuffer byteBuffer = ByteBuffer.allocate(2 + 1 + message.length() + 1);
            byteBuffer.put(ByteUtils.int16ToByte(duration));
            byteBuffer.put(ByteUtils.intToByte(priority));
            for (char c : message.toCharArray()) {
                byteBuffer.put(ByteUtils.charToByte(c));
            }
            // Fin de string en C
            byteBuffer.put(ByteUtils.intToByte(0));

            write(KissCommand.GET_MESSAGE, KissProtocol.buildRequest(KISS_VERSION, KissCommand.GET_MESSAGE, byteBuffer.array()));
        }

        private void write(KissCommand kissCommand, byte[] bytes) throws SerialPortException {
            for (byte b : bytes) {
                serialPort.writeByte(b);
            }
            logSendCommand(kissCommand, bytes);
        }

        Calendar lastSendMode = Calendar.getInstance();
        private void initTelemetryBuffer() {
            for (int i = 0; i < serialBuffer.length; i++) {
                serialBuffer[i] = 0;
            }

            addShortAtPosition(0, (short) (readJoystickEvent.getThrottle() - 1000));
            addShortAtPosition(2, (short) (readJoystickEvent.getRoll() - 1500));
            addShortAtPosition(4, (short) (readJoystickEvent.getPitch() - 1500));
            addShortAtPosition(6, (short) (readJoystickEvent.getYaw() - 1500));
            addShortAtPosition(8, (short) 0);
            addShortAtPosition(10, (short) 0);
            addShortAtPosition(12, (short) 0);
            addShortAtPosition(14, (short) 0);
            // KISS_INDEX_CURRENT_ARMED 16
            serialBuffer[16] = ByteUtils.intToByte(readJoystickEvent.getArmed() ? 1 : 0);
            // KISS_INDEX_LIPOVOLT 17 // INT 16
            addInt16AtPosition(17, 1680);
            if (Calendar.getInstance().getTimeInMillis() - lastSendMode.getTimeInMillis() >= 3000) {
                lastSendMode = Calendar.getInstance();
                mode++;
                if (mode > 5) {
                    mode = 0;
                }
            }
            serialBuffer[65] = ByteUtils.intToByte(mode);
        }

        private void addShortAtPosition(int position, short data) {
            byte[] bytes = ByteUtils.shortToBytes(data);
            for (int i = 0; i < bytes.length; i++) {
                serialBuffer[position + i] = bytes[i];
            }
        }

        private void addInt16AtPosition(int position, int data) {
            byte[] bytes = ByteUtils.int16ToByte(data);
            for (int i = 0; i < bytes.length; i++) {
                serialBuffer[position + i] = bytes[i];
            }
        }


    }
}
