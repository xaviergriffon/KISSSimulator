package fr.griffon;

import fr.griffon.enums.KissCommand;
import fr.griffon.utils.ByteUtils;

import java.nio.ByteBuffer;

public class KissProtocol {

    public static byte[] buildRequest(int kissVersion, KissCommand command, byte[] data) {
        int crc = 0;
        if (kissVersion > 0 && kissVersion < 109) {
            crc = ByteUtils.computeChecksum(data);
        } else {
            crc = ByteUtils.computeCRC8(data);
        }

        int dataLength = data.length;
        ByteBuffer byteBuffer = ByteBuffer.allocate(dataLength + 3);;
        if (command != KissCommand.GET_TELEMETRY && command != KissCommand.GET_SETTINGS) {
            byteBuffer.put(command.getByteCommand());
            byteBuffer.put(ByteUtils.intToByte(dataLength));
            for (int i = 0; i < dataLength;i++) {
                byteBuffer.put(data[i]);
            }
            byteBuffer.put(ByteUtils.intToByte(crc));
        } else {
            int KISSFRAMEINIT = 5;
            byteBuffer.put(ByteUtils.intToByte(KISSFRAMEINIT));
            byteBuffer.put(ByteUtils.shortToByte((short) dataLength));
            byteBuffer.put(data);
            int checksum = ByteUtils.computeChecksum(data);
            byte check = ByteUtils.intToByte(checksum / dataLength);
            byteBuffer.put(check);
        }

        return byteBuffer.array();
    }
}
