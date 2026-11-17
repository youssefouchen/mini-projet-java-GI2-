package com.tawkit.controller;

import javax.swing.JDialog;
import javax.swing.JTabbedPane;
import com.tawkit.model.ConfigManager;
import com.tawkit.view.MainFrame;

/**
 * [MEMBRE 3] Interface d'administration en 3 onglets :
 * 1. Mosquée & Ville
 * 2. Minuteurs d'Iqama
 * 3. Microphone & Calibrage Iqama
 */
public class AdminDialog extends JDialog {
    private final ConfigManager configManager;
    private final MainFrame mainFrame;

    public AdminDialog(MainFrame mainFrame, ConfigManager configManager) {
        super(mainFrame, "Administration - Tawkit Desktop", true);
        this.mainFrame = mainFrame;
        this.configManager = configManager;

        setSize(600, 450);
        setLocationRelativeTo(mainFrame);

        JTabbedPane tabbedPane = new JTabbedPane();
        // TODO [MEMBRE 3] : Créer et ajouter les 3 onglets + bouton Enregistrer
        add(tabbedPane);
    }

    /**
     * Contrat d'intégration : Sauvegarder via ConfigManager (Membre 1) et rafraîchir MainFrame (Membre 2).
     */
    private void onSaveClicked() {
        configManager.save();
        mainFrame.reloadUI();
        dispose();
    }
}
