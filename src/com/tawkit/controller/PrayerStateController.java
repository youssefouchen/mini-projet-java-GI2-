package com.tawkit.controller;

import com.tawkit.model.ConfigManager;
import com.tawkit.view.MainFrame;

/**
 * [MEMBRE 3] Chef d'orchestre de la séquence :
 * DASHBOARD -> ADHAN_PLAYING -> BLACK_SCREEN (Timer + Micro) -> DASHBOARD
 */
public class PrayerStateController {
    private final ConfigManager configManager;
    private final MainFrame mainFrame;
    private final AdhanAudioPlayer audioPlayer;
    private final IqamaVoiceDetector voiceDetector;

    public PrayerStateController(ConfigManager configManager, MainFrame mainFrame) {
        this.configManager = configManager;
        this.mainFrame = mainFrame;
        this.audioPlayer = new AdhanAudioPlayer();
        this.voiceDetector = new IqamaVoiceDetector();
    }

    /**
     * Contrat d'intégration : Appelé par Membre 1 (TimeCheckService) à l'heure exacte d'une prière.
     * @param prayerKey Clé de la prière ("fajr", "dohr", "asr", "maghrib", "icha")
     */
    public void startAdhanSequence(String prayerKey) {
        audioPlayer.playAdhan(() -> onAdhanFinished(prayerKey));
    }

    private void onAdhanFinished(String prayerKey) {
        mainFrame.showBlackScreen();
        // TODO [MEMBRE 3] : Démarrer le minuteur d'Iqama et le IqamaVoiceDetector
    }

    public void returnToDashboard() {
        voiceDetector.stopListening();
        mainFrame.showDashboard();
    }
}
