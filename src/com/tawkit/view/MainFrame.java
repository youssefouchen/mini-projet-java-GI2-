package com.tawkit.view;

import java.awt.CardLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
import com.tawkit.model.ConfigManager;
import com.tawkit.model.TimeCheckService;
import com.tawkit.controller.PrayerStateController;

/**
 * [MEMBRE 2] Conteneur Plein Écran & CardLayout ("DASHBOARD" et "BLACK_SCREEN").
 */
public class MainFrame extends JFrame {
    public static final String VIEW_DASHBOARD = "DASHBOARD";
    public static final String VIEW_BLACK_SCREEN = "BLACK_SCREEN";

    private final ConfigManager configManager;
    private final TimeCheckService timeCheckService;
    private PrayerStateController stateController;

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel containerPanel = new JPanel(cardLayout);
    private final DashboardPanel dashboardPanel;
    private final BlackScreenPanel blackScreenPanel;

    public MainFrame(ConfigManager configManager, TimeCheckService timeCheckService) {
        this.configManager = configManager;
        this.timeCheckService = timeCheckService;

        setTitle("Tawkit Desktop");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        this.dashboardPanel = new DashboardPanel(configManager, timeCheckService);
        this.blackScreenPanel = new BlackScreenPanel();

        containerPanel.add(dashboardPanel, VIEW_DASHBOARD);
        containerPanel.add(blackScreenPanel, VIEW_BLACK_SCREEN);
        add(containerPanel);

        // TODO [MEMBRE 2] : Ajouter raccourcis clavier globaux (ECHAP pour quitter, F12 pour AdminDialog)
    }

    public void setStateController(PrayerStateController stateController) {
        this.stateController = stateController;
    }

    /**
     * Contrat d'intégration : Appelé par Membre 3 (PrayerStateController) à la fin de l'Adhan.
     */
    public void showBlackScreen() {
        cardLayout.show(containerPanel, VIEW_BLACK_SCREEN);
    }

    /**
     * Contrat d'intégration : Appelé par Membre 3 (PrayerStateController) au retour de l'Iqama.
     */
    public void showDashboard() {
        cardLayout.show(containerPanel, VIEW_DASHBOARD);
    }

    /**
     * Contrat d'intégration : Appelé par Membre 3 (AdminDialog) après enregistrement.
     */
    public void reloadUI() {
        reloadUI(this.configManager);
    }

    public void reloadUI(ConfigManager config) {
        // TODO [MEMBRE 2] : Rafraîchir le nom de la mosquée, l'annonce et les horaires
        dashboardPanel.reloadUI(config);
    }

    public BlackScreenPanel getBlackScreenPanel() {
        return blackScreenPanel;
    }
}
