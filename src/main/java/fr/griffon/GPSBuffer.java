package fr.griffon;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Buffer de données GPS.
 * Il permet de lire un fichier construit avec CollectData et de distribuer les données en respectant leurs écarts dans le temps.
 */
public class GPSBuffer {
    private static String defaultGPSHex = "54 0F 1b 40 c3 90 02 d6 85 a6 00 1C 1F 3F 01 47 8A ca";
    private Date startDate;
    private GPSData gpsData = null;
    private GPSSetter currentRun = null;
    private String fileName;

    public GPSBuffer(String fileName) {
        this.fileName = fileName;
    }

    public void initAndStartBuffer() {
        try {
            if (currentRun != null) {
                currentRun.stop();
            }
            if (fileName != null) {
                List<GPSData> buffer = buildBuffer(fileName);

                currentRun = new GPSSetter(this, buffer);
                Thread thread = new Thread(currentRun);
                thread.start();
            }
            startDate = new Date();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (currentRun != null) {
            currentRun.stop();
        }
    }

    public String getGPSHex() {
        if(gpsData != null) {
            synchronized (gpsData) {
                return gpsData != null ? gpsData.gpsHex : defaultGPSHex;
            }
        }
        return defaultGPSHex;
    }

    private void setGPSData(GPSData gpsData) {
        synchronized (gpsData) {
            this.gpsData = gpsData;
        }
    }

    public List<GPSData> buildBuffer(String fileName) throws IOException {
        List<GPSData> result = new ArrayList<>();
        byte[] bytes = Files.readAllBytes(Paths.get(fileName));
        int len = 0;
        int cursor = 0;
        byte[] buffer = new byte[Long.BYTES];
        Type type = Type.time;
        GPSData gpsData = null;
        for (byte b : bytes) {
            switch (type) {
                case time:
                    buffer[cursor] = b;
                    cursor++;
                    if (cursor >= Long.BYTES) {
                        gpsData = new GPSData();
                        gpsData.time = ByteUtils.bytesToLong(buffer);
                        cursor = 0;
                        type = Type.len;
                    }
                    break;
                case len:
                    len = b;
                    buffer = new byte[len];
                    cursor = 0;
                    type = Type.data;
                    break;
                case data:
                    buffer[cursor] = b;
                    cursor++;
                    if (cursor >= len) {
                        gpsData.gpsHex = ByteUtils.bytesToHex(buffer);
                        result.add(gpsData);
                        // Passage au time
                        cursor = 0;
                        buffer = new byte[Long.BYTES];
                        type = Type.time;
                    }
                    break;
            }
        }
        return result;

    }

    enum Type {
        time,
        len,
        data
    }

    private static class GPSData {
        public long time;
        public String gpsHex;
    }

    private class GPSSetter implements Runnable {
        private GPSBuffer gpsBuffer;
        private List<GPSData> buffer;
        private boolean running = true;
        private int lastIndex = 0;

        public GPSSetter(GPSBuffer gpsBuffer, List<GPSData> buffer) {
            super();
            this.buffer = buffer;
            this.gpsBuffer = gpsBuffer;
        }

        @Override
        public void run() {
            gpsBuffer.setGPSData(buffer.get(lastIndex));
            while (running) {
                if (lastIndex >= buffer.size()) {
                    lastIndex = 0;
                    gpsBuffer.startDate = new Date();
                }
                long duration = getDuration();
                GPSData gpsData = null;
                while (gpsData == null) {
                    if (lastIndex < buffer.size() - 1) {
                        if (buffer.get(lastIndex + 1).time <= duration) {
                            lastIndex++;
                            gpsData = buffer.get(lastIndex);
                        } else {
                            gpsData = buffer.get(lastIndex);
                        }
                    } else if (lastIndex == (buffer.size() - 1)) {
                        gpsData = buffer.get(lastIndex);
                        lastIndex++;
                    }
                }
                gpsBuffer.setGPSData(gpsData);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private long getDuration() {
            return (new Date()).getTime() - gpsBuffer.startDate.getTime();
        }

        public void stop() {
            running = false;
        }
    }
}
