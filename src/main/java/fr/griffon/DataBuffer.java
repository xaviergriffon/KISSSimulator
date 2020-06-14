package fr.griffon;

import fr.griffon.utils.ByteUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * Buffer de données.
 * Il permet de lire un fichier construit avec CollectData et de distribuer les données en respectant leurs écarts dans le temps.
 */
public class DataBuffer {
    private String defaultDataHex;
    private Date startDate;
    private Data data = null;
    private DataSetter currentRun = null;
    private String fileName;

    public DataBuffer(String fileName, String defaultDataHex) {
        this.fileName = fileName;
        Objects.requireNonNull(defaultDataHex);
        this.defaultDataHex = defaultDataHex;
    }

    public void initAndStartBuffer() {
        try {
            if (currentRun != null) {
                currentRun.stop();
            }
            if (fileName != null) {
                List<Data> buffer = buildBuffer(fileName);

                currentRun = new DataSetter(this, buffer);
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

    public String getDataHex() {
        if(data != null) {
            synchronized (data) {
                return data != null ? data.dataHex : defaultDataHex;
            }
        }
        return defaultDataHex;
    }

    private void setData(Data data) {
        synchronized (data) {
            this.data = data;
        }
    }

    public List<Data> buildBuffer(String fileName) throws IOException {
        List<Data> result = new ArrayList<>();
        byte[] bytes = Files.readAllBytes(Paths.get(fileName));
        int len = 0;
        int cursor = 0;
        byte[] buffer = new byte[Long.BYTES];
        Type type = Type.time;
        Data data = null;
        for (byte b : bytes) {
            switch (type) {
                case time:
                    buffer[cursor] = b;
                    cursor++;
                    if (cursor >= Long.BYTES) {
                        data = new Data();
                        data.time = ByteUtils.bytesToLong(buffer);
                        cursor = 0;
                        type = Type.len;
                    }
                    break;
                case len:
                    len = b;
                    if (b > 0) {
                        buffer = new byte[len];
                        cursor = 0;
                        type = Type.data;
                    } else {
                        cursor = 0;
                        buffer = new byte[Long.BYTES];
                        type = Type.time;
                    }
                    break;
                case data:
                    if (cursor < buffer.length) {
                        buffer[cursor] = b;
                        cursor++;
                        if (cursor >= len) {
                            data.dataHex = ByteUtils.bytesToHex(buffer);
                            result.add(data);
                            // Passage au time
                            cursor = 0;
                            buffer = new byte[Long.BYTES];
                            type = Type.time;
                        }
                    } else {
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

    private static class Data {
        public long time;
        public String dataHex;
    }

    private class DataSetter implements Runnable {
        private DataBuffer dataBuffer;
        private List<Data> buffer;
        private boolean running = true;
        private int lastIndex = 0;

        public DataSetter(DataBuffer dataBuffer, List<Data> buffer) {
            super();
            this.buffer = buffer;
            this.dataBuffer = dataBuffer;
        }

        @Override
        public void run() {
            dataBuffer.setData(buffer.get(lastIndex));
            while (running) {
                if (lastIndex >= buffer.size()) {
                    lastIndex = 0;
                    dataBuffer.startDate = new Date();
                }
                long duration = getDuration();
                Data data = null;
                while (data == null) {
                    if (lastIndex < buffer.size() - 1) {
                        if (buffer.get(lastIndex + 1).time <= duration) {
                            lastIndex++;
                            data = buffer.get(lastIndex);
                        } else {
                            data = buffer.get(lastIndex);
                        }
                    } else if (lastIndex == (buffer.size() - 1)) {
                        data = buffer.get(lastIndex);
                        lastIndex++;
                    }
                }
                dataBuffer.setData(data);
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private long getDuration() {
            return (new Date()).getTime() - dataBuffer.startDate.getTime();
        }

        public void stop() {
            running = false;
        }
    }
}
