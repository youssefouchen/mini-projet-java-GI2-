package com.tawkit.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * [MEMBRE 1] Gestion des paramètres sans modifier le code.
 * Lit et écrit dans config/tawkit_config.properties.
 */
public class ConfigManager {
    private static final String CONFIG_PATH = "config/tawkit_config.properties";
    private final Properties properties = new Properties();

    public ConfigManager() {
        loadOrCreateDefault();
    }

    private void loadOrCreateDefault() {
        File file = new File(CONFIG_PATH);
        if (!file.exists()) {
            setDefaults();
            save();
        } else {
            try (FileInputStream fis = new FileInputStream(file)) {
                properties.load(fis);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void setDefaults() {
        properties.setProperty("mosque.name", "Grande Mosquée");
        properties.setProperty("city.csv", "horaires_ville.csv");
        properties.setProperty("iqama.fajr", "20");
        properties.setProperty("iqama.dohr", "15");
        properties.setProperty("iqama.asr", "15");
        properties.setProperty("iqama.maghrib", "10");
        properties.setProperty("iqama.icha", "15");
        properties.setProperty("voice.enabled", "true");
        properties.setProperty("voice.threshold", "50");
        properties.setProperty("hijri.adjustment", "0");
        properties.setProperty("announcement.text", "Bienvenue à la mosquée.");
    }

    public void save() {
        File file = new File(CONFIG_PATH);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            properties.store(fos, "Tawkit Desktop Configuration");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }
}
