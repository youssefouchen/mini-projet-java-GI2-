package com.tawkit.view;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JPanel;
import com.tawkit.model.ConfigManager;
import com.tawkit.model.TimeCheckService;

/**
 * [MEMBRE 2] Tableau de bord principal :
 * - Zone Haute : Horloge numérique, nom de la mosquée, double date
 * - Zone Centrale : AnnouncementTicker
 * - Zone Basse : Grille des 6 cartes de prières (PrayerCardPanel)
 */
public class DashboardPanel extends JPanel {
    private final AnnouncementTicker announcementTicker;
    private final JPanel cardsGrid;

    public DashboardPanel(ConfigManager configManager, TimeCheckService timeCheckService) {
        setLayout(new BorderLayout());
        this.announcementTicker = new AnnouncementTicker(configManager.getProperty("announcement.text", ""));
        this.cardsGrid = new JPanel(new GridLayout(1, 6, 15, 0));

        add(announcementTicker, BorderLayout.CENTER);
        add(cardsGrid, BorderLayout.SOUTH);

        // TODO [MEMBRE 2] : Construire la zone haute et initialiser les 6 PrayerCardPanel
    }

    public void reloadUI(ConfigManager config) {
        // TODO [MEMBRE 2] : Mettre à jour les composants visuels avec la nouvelle configuration
    }
}
