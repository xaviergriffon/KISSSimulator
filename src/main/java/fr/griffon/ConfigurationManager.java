package fr.griffon;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URL;

public class ConfigurationManager {
    private static ConfigurationManager currentInstance = null;

    private Configuration configuration;

    private ConfigurationManager() {
    }

    public static ConfigurationManager getInstance() {
        if (currentInstance == null) {
            currentInstance = new ConfigurationManager();
            try {
                synchronized (currentInstance) {
                    currentInstance.loadConfigurationFile();
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        return currentInstance;
    }

    private void loadConfigurationFile() throws FileNotFoundException {
        URL configurationFile = getClass().getResource("configuration.json");
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setLenient();
        Gson gson = gsonBuilder.create();
        configuration = gson.fromJson(new FileReader(configurationFile.getFile()), Configuration.class);
    }

    public Configuration getConfiguration() {
        return configuration;
    }


}
