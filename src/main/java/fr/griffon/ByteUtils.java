package fr.griffon;

import java.nio.ByteBuffer;

/**
 * Classe utilitaire pour les conversions de byte
 */
public class ByteUtils {

    private static ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);

    public static byte intToByte(final int data) {
        return intToBytes(data)[3];
    }

    public static byte[] int16ToByte(final int data) {
        byte[] bytes = intToBytes(data);
        return new byte[]{bytes[2], bytes[3]};
    }

    public static byte[] intToBytes(final int data) {
        return new byte[]{
                (byte) ((data >> 24) & 0xff),
                (byte) ((data >> 16) & 0xff),
                (byte) ((data >> 8) & 0xff),
                (byte) ((data >> 0) & 0xff)
        };
    }

    public static byte shortToByte(final short data) {
        return shortToBytes(data)[1];
    }

    public static byte[] shortToBytes(final short data) {
        return new byte[]{
                (byte) ((data >> 8) & 0xff),
                (byte) ((data >> 0) & 0xff)
        };
    }

    public static int computeCRC8(byte[] bytes) {
        int crc8 = 0;

        for (int b = 0; b < bytes.length; b++) {
            crc8 ^= bytes[b];
            for (int i = 0; i < 8; i++) {
                if ((crc8 & 0x80) != 0) {
                    crc8 = ((crc8 << 1) ^ 0xD5) & 0xFF;
                } else {
                    crc8 <<= 1;
                }
            }
        }

        return crc8;
    }

    public static int computeChecksum(byte[] bytes) {
        int sum = 0;
        for (byte b : bytes) {
            sum += (b & 0xFF);
        }
        return sum;
    }

    public static String bytesToHex(byte[] hashInBytes) {

        StringBuilder sb = new StringBuilder();
        for (byte b : hashInBytes) {
            sb.append(String.format("%02x ", b));
        }
        return sb.toString();

    }

    public static int bytesToInt32(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) |
                ((bytes[1] & 0xFF) << 16) |
                ((bytes[2] & 0xFF) << 8) |
                ((bytes[3] & 0xFF) << 0);
    }

    public static int bytesToInt16(byte[] bytes) {
        return (bytes[0] << 8) & 0x0000ff00 | (bytes[1] << 0) & 0x000000ff;
    }

    public static int bytesToInt8(byte[] b) {
        return byteToInt8(b[0]);
    }

    public static int byteToInt8(byte b) {
        return (b << 8) & 0x0000ff00;
    }

    public static byte[] hexStringToByteArray(String s) {
        String string = s.replaceAll(" ", "").replaceAll("_", "");
        int len = string.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(string.charAt(i), 16) << 4)
                    + Character.digit(string.charAt(i + 1), 16));
        }
        return data;
    }

    public static byte[] longToBytes(long x) {
        buffer.putLong(0, x);
        return buffer.array();
    }

    public static long bytesToLong(byte[] bytes) {
        try {
            buffer.clear();
            buffer.put(bytes, 0, bytes.length);
        } catch (Exception e) {
            e.printStackTrace();
        }
        buffer.flip();
        return buffer.getLong();
    }

    public static byte[] extractByte(byte[] bytes, int start, int len) {
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++) {
            result[i] = bytes[start + i];
        }
        return result;
    }
}
