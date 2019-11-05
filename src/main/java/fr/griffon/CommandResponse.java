package fr.griffon;

import fr.griffon.utils.ByteUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Simulation d'une réponse pour une commande
 */
public class CommandResponse {

    /*
     * Liste des responses de commandes inscrites pour démarrer les buffers.
     */
    private static List<CommandResponse> responses = new ArrayList<>();

    private String hexCommand;
    private String hexResponse;
    private String collectedDatafileName;
    private DataBuffer dataBuffer;

    public CommandResponse() {
        addResponse(this);
    }

    public String getHexCommand() {
        return hexCommand;
    }

    public void setHexCommand(String hexCommand) {
        this.hexCommand = hexCommand;
    }

    public String getHexResponse() {
        return hexResponse;
    }

    public void setHexResponse(String hexResponse) {
        this.hexResponse = hexResponse;
    }

    public String getCollectedDatafileName() {
        return collectedDatafileName;
    }

    public void setCollectedDatafileName(String collectedDatafileName) {
        this.collectedDatafileName = collectedDatafileName;
    }

    public byte[] getResponse() {
        String dataHex = this.dataBuffer.getDataHex();
        return ByteUtils.hexStringToByteArray(dataHex);
    }

    private static void addResponse(CommandResponse response) {
        synchronized (responses) {
            responses.add(response);
        }
    }

    private static void dropResponse(CommandResponse response) {
        synchronized (responses) {
            responses.remove(response);
        }
    }

    public static void initAndStartBuffers() {
        synchronized (responses) {
            responses.forEach(response -> {
                DataBuffer dataBuffer = response.dataBuffer;
                if (dataBuffer != null) {
                    dataBuffer.stop();
                }

                dataBuffer = new DataBuffer(response.getCollectedDatafileName(), response.getHexResponse());
                response.dataBuffer = dataBuffer;
                dataBuffer.initAndStartBuffer();
            });
        }
    }

    public static void stopAllBuffers() {
        synchronized (responses) {
            responses.forEach(response -> {
                DataBuffer dataBuffer = response.dataBuffer;
                if (dataBuffer != null) {
                    dataBuffer.stop();
                }
            });
        }
    }
}
