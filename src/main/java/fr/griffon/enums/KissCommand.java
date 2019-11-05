package fr.griffon.enums;

import fr.griffon.utils.ByteUtils;

import java.util.HashMap;
import java.util.Map;

public enum KissCommand {
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
    SET_DSETPOINT(0x53),
    UNKNOW(-1);

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
        KissCommand kissCommand = cache.get(byteCommand);
        return kissCommand != null ? kissCommand : KissCommand.UNKNOW;
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
