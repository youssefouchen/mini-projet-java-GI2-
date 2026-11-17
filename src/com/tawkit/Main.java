package com.tawkit;

import javax.swing.SwingUtilities;
import com.tawkit.model.ConfigManager;
import com.tawkit.model.TimeCheckService;
import com.tawkit.view.MainFrame;
import com.tawkit.controller.PrayerStateController;

/**
 * Point d'entrée commun de l'application Tawkit Desktop.
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ConfigManager configManager = new ConfigManager();
            TimeCheckService timeCheckService = new TimeCheckService(configManager);
            MainFrame mainFrame = new MainFrame(configManager, timeCheckService);
            PrayerStateController stateController = new PrayerStateController(configManager, mainFrame);
            
            timeCheckService.setStateController(stateController);
            mainFrame.setStateController(stateController);
            
            mainFrame.setVisible(true);
        });
    }
}
