package fr.griffon;


import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Simulateur de communication avec une FC KISS.
 */
public class KissSimulator {
    private boolean running = true;
    private SerialPort serialPort;
    private ReadJoystickEvent readJoystickEvent;
    private GPSBuffer gpsBuffer;
    private boolean logSetCommand = false;
    private boolean logGetCommand = false;

    public KissSimulator(String portName, String gpsDataFileName, ReadJoystickEvent.JoystickController joystickController) {
        readJoystickEvent = new ReadJoystickEvent(joystickController);
        serialPort = new SerialPort(portName);
        gpsBuffer = new GPSBuffer(gpsDataFileName);
    }

    public static void main(String[] args) {
        String porName = "/dev/cu.SLAB_USBtoUART";
        String gpsDataFileName = null; //"receveiveData_10_100.byte";
        ReadJoystickEvent.JoystickController joystickController = ReadJoystickEvent.JoystickController.TARANIS;
        KissSimulator kissSimulator = new KissSimulator(porName, gpsDataFileName, joystickController);
        kissSimulator.setLogSetCommand(true);
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
        System.out.println("VTX Type : " + ByteUtils.byteToInt8(bytes[index]));
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
        gpsBuffer.initAndStartBuffer();
        try {
            System.out.println("Port opened: " + serialPort.openPort());
            serialPort.setParams(115200, 8, 1, 0);
            serialPort.addEventListener(new KissSimulatorEventListener());
            while (running) {
                Thread.sleep(2);
            }
            System.out.println("Port closed: " + serialPort.closePort());
            readJoystickEvent.stop();
            gpsBuffer.stop();;
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


    enum KissCommand {
        GET_TELEMETRY(0x20),
        GET_SETTINGS(0x30),
        GET_PIDS(0x43),
        SET_PIDS(0x44),
        GET_RATES(0x4D),
        SET_RATES(0x4E),
        GET_TPAS(0x4B),
        SET_TPAS(0x4C),
        GET_GPS(0x54),
        GET_FILTERS(0x47),
        SET_FILTERS(0x48),
        GET_VTX(0x45),
        SET_VTX(0x46),
        GET_DSETPOINT(0x52),
        SET_DSETPOINT(0x53);

        private static Map<Byte, KissCommand> cache = null;
        private byte byteCommand;

        KissCommand(int command) {
            this.byteCommand = ByteUtils.intToByte(command);
        }

        public static KissCommand fromByte(byte byteCommand) {
            if (cache == null) {
                cache = new HashMap<>();
                for (KissCommand kissCommand : KissCommand.values()) {
                    cache.put(kissCommand.getByteCommand(), kissCommand);
                }
            }
            return cache.get(byteCommand);
        }

        public byte getByteCommand() {
            return byteCommand;
        }

        public boolean isNeedChecksum() {
            return this != GET_TELEMETRY;
        }

        public boolean isGetCommand() {
            return this.toString().startsWith("GET");
        }

        public boolean isSetCommand() {
            return this.toString().startsWith("SET");
        }

    }

    enum OnOff {
        OFF,
        ON;

        public static String valueToString(int position) {
            if (position < 0 || position >= values().length) {
                System.out.println(position);
                return "bad value";
            }

            return values()[position].toString();
        }
    }

    enum Filters {
        Off,
        High,
        MedHigh,
        Medium,
        MedLow,
        Low,
        VeryLow;

        public static String valueToString(int position) {
            if (position < 0 || position >= values().length) {
                System.out.println(position);
                return "bad value";
            }

            return values()[position].toString();
        }
    }

    enum Band {
        A,
        B,
        E,
        FS,
        RB;
        public static String valueToString(int position) {
            if (position < 0 || position >= values().length) {
                System.out.println(position);
                return "bad value";
            }

            return values()[position].toString();
        }
    }

    private class KissSimulatorEventListener implements SerialPortEventListener {
        private byte[] serialBuffer = new byte[200];

        @Override
        public void serialEvent(SerialPortEvent serialPortEvent) {
            if (serialPortEvent.isRXCHAR() && serialPortEvent.getEventValue() > 0) {
                try {
                    byte[] readed = serialPort.readBytes(serialPortEvent.getEventValue());
                    if (readed.length > 0) {
                        KissCommand kissCommand = KissCommand.fromByte(readed[0]);
                        logReceiveCommand(kissCommand, readed);
                        switch (kissCommand) {
                            case GET_TELEMETRY:
                                sendTelemetry();
                                break;
                            case GET_PIDS:
                                simulateResponseOfGetPids();
                                break;
                            case GET_SETTINGS:
                                simulateResponseOfGetSettings();
                                break;
                            case GET_RATES:
                                simulateResponseOfGetRates();
                                break;
                            case GET_GPS:
                                simulateResponseOfGetGPS();
                                break;
                            case SET_RATES:
                                byte[] ratesSetting = new byte[readed.length - 1];
                                for (int i = 1; i < readed.length; i++) {
                                    ratesSetting[i - 1] = readed[i];
                                }
                                logRATEFromSetRATESbytes(ratesSetting);
                                break;
                            case GET_TPAS:
                                simulateResponseOfGetTpa();
                                break;
                            case SET_PIDS:
                                byte[] pidsSetting = new byte[readed.length - 1];
                                for (int i = 1; i < readed.length; i++) {
                                    pidsSetting[i - 1] = readed[i];
                                }
                                logPIDFromSetPIDSbytes(pidsSetting);
                                break;
                            case GET_FILTERS:
                                simulateResponseOfGetFilters();
                                break;
                            case SET_FILTERS:
                                logFitlersFromSetFiltersbytes(readed);
                                break;
                            case GET_VTX:
                                simulateResponseOfGetVTX();
                                break;
                            case SET_VTX:
                                logVTXfromSetVTX(readed);
                                break;
                            default:
                                System.out.print("unknown instruction :");
                                System.out.println(ByteUtils.bytesToHex(readed));
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

        private void write(KissCommand kissCommand, byte[] bytes) throws SerialPortException {
            for (byte b : bytes) {
                serialPort.writeByte(b);
            }
            logSendCommand(kissCommand, bytes);
        }

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
        }

        private void simulateResponseOfGetSettings() throws SerialPortException {
            byte[] data = ByteUtils.hexStringToByteArray("05 B6 0F A0 13 EC 14 82 00 30 00 41 00 32 2A 30 31 38 00 00 0F A0 00 28 27 10 00 00 00 00 07 D0 07 D0 06 72 02 8A 02 8A 02 80 00 DC 00 DC 00 D2 00 11 00 00 00 02 00 00 00 06 00 14 01 F4 00 46 03 E8 05 DC 00 00 00 00 00 00 00 14 21 45 00 02 CB 01 39 37 35 32 30 37 51 19 00 30 00 1D 75 01 B8 00 C8 00 C8 00 01 00 01 00 1E 32 1E 00 00 64 00 00 80 00 94 00 A8 82 64 46 26 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 C8 00 64 00 00 C8 00 64 23 03 00 19 03 20 00 00 00 00 00 00 03 03 03 00 00 02 01 00 25 01 00 00 64 00 00 50 00 F0 21 F6 FF 00 49 ");
            write(KissCommand.GET_SETTINGS, data);
        }

        private void simulateResponseOfGetPids() throws SerialPortException {
            byte[] data = ByteUtils.hexStringToByteArray("43 12 0F A0 00 30 2A 30 13 EC 00 41 31 38 14 82 00 32 00 00 D7");
            write(KissCommand.GET_PIDS, data);
        }

        private void simulateResponseOfGetRates() throws SerialPortException {
            byte[] data = ByteUtils.hexStringToByteArray("4D 12 07 D0 02 8A 00 DC 07 D0 02 8A 00 DC 06 72 02 80 00 D2 E4");
            write(KissCommand.GET_RATES, data);
        }

        private void simulateResponseOfGetTpa() throws SerialPortException {
            byte[] data = ByteUtils.hexStringToByteArray("4B 0D 01 B8 00 C8 00 C8 00 1E 32 1E 00 00 64 6E");
            write(KissCommand.GET_TPAS, data);
        }

        private void simulateResponseOfGetGPS() throws SerialPortException {
            byte[] data = ByteUtils.hexStringToByteArray(gpsBuffer.getGPSHex());
            write(KissCommand.GET_GPS, data);
        }

        private void simulateResponseOfGetFilters() throws SerialPortException {
            byte[] data = ByteUtils.hexStringToByteArray("47 0E 01 23 00 00 C8 00 64 00 00 C8 00 64 02 01 11");
            write(KissCommand.GET_FILTERS, data);
        }

        private void simulateResponseOfGetVTX() throws SerialPortException {
            byte[] data = ByteUtils.hexStringToByteArray("45 06 03 26 00 32 03 20 31");
            write(KissCommand.GET_VTX, data);
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
