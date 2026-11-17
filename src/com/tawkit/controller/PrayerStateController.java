package com.tawkit.controller;

import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.tawkit.model.ConfigManager;
import com.tawkit.view.MainFrame;

/**
 * [MEMBRE 3] Chef d'orchestre de la séquence :
 * DASHBOARD -> ADHAN_PLAYING -> BLACK_SCREEN (Timer d'Iqama + Micro) -> DASHBOARD
 *
 * Contrats d'intégration :
 * - startAdhanSequence(prayerKey) : appelé par Membre 1 (TimeCheckService) dès que
 *   l'horloge atteint l'heure exacte d'une prière.
 * - mainFrame.showBlackScreen() puis mainFrame.showDashboard() : appelés chez
 *   Membre 2, à la fin de l'Adhan puis dès que l'Iqama est détectée au micro
 *   (Condition A) ou que le minuteur atteint 00:00 (Condition B).
 */
public class PrayerStateController {

    private final ConfigManager configManager;
    private final MainFrame mainFrame;
    private final AdhanAudioPlayer audioPlayer;
    private final IqamaVoiceDetector voiceDetector;

    /** Minuteur d'Iqama (1 tick par seconde, sur l'EDT). */
    private Timer iqamaCountdownTimer;
    /** Secondes restantes avant le retour automatique au Dashboard. */
    private int remainingSeconds;
    /** Garde-fou : la fin de séquence ne doit être exécutée qu'une seule fois. */
    private boolean sequenceEnded = false;
    /** Garde-fou : ignore les appels redondants de startAdhanSequence. */
    private boolean sequenceActive = false;

    public PrayerStateController(ConfigManager configManager, MainFrame mainFrame) {
        this.configManager = configManager;
        this.mainFrame = mainFrame;
        this.audioPlayer = new AdhanAudioPlayer();
        this.voiceDetector = new IqamaVoiceDetector();
    }

    /**
     * Contrat d'intégration : appelé par Membre 1 (TimeCheckService) à l'heure
     * exacte d'une prière. Lance la lecture de l'Adhan.
     *
     * @param prayerKey clé de la prière : "fajr", "dohr", "asr", "maghrib" ou "icha"
     */
    public void startAdhanSequence(String prayerKey) {
        runOnEdt(() -> {
            if (sequenceActive) {
                System.err.println("[PrayerStateController] Séquence déjà en cours, appel ignoré pour : "
                        + prayerKey);
                return;
            }
            sequenceActive = true;
            sequenceEnded = false;
            audioPlayer.playAdhan(() -> onAdhanFinished(prayerKey));
        });
    }

    /**
     * Fin de l'Adhan (EDT garanti par AdhanAudioPlayer) :
     * basculement sur l'écran noir, démarrage du minuteur d'Iqama
     * et de l'écoute micro en arrière-plan.
     */
    private void onAdhanFinished(String prayerKey) {
        int iqamaMinutes = getIqamaMinutes(prayerKey);
        mainFrame.showBlackScreen();
        startIqamaPhase(iqamaMinutes);
    }

    /**
     * Démarre le minuteur d'Iqama et, si la détection vocale est activée
     * dans la configuration, le IqamaVoiceDetector.
     */
    private void startIqamaPhase(int iqamaMinutes) {
        boolean voiceEnabled =
                Boolean.parseBoolean(configManager.getProperty("voice.enabled", "true"));

        // Indicateur "Micro actif" de l'écran noir (Membre 2)
        mainFrame.getBlackScreenPanel().setMicActive(voiceEnabled);

        remainingSeconds = iqamaMinutes * 60;
        updateCountdownLabel();

        if (voiceEnabled) {
            int threshold = parseIntSafe(configManager.getProperty("voice.threshold", "50"), 50);
            voiceDetector.startListening(threshold, this::onIqamaDetected);
        }

        // Condition B (secours) : un tick par seconde jusqu'à 00:00.
        iqamaCountdownTimer = new Timer(1000, event -> onCountdownTick());
        iqamaCountdownTimer.start();
    }

    /**
     * Tick du minuteur d'Iqama : mise à jour du compte à rebours discret
     * et retour automatique au Dashboard lorsque 00:00 est atteinte.
     */
    private void onCountdownTick() {
        if (sequenceEnded) {
            return;
        }
        remainingSeconds--;
        updateCountdownLabel();
        if (remainingSeconds <= 0) {
            // Condition B (secours) : minuteur à zéro sans voix captée.
            finishSequence();
        }
    }

    /**
     * Condition A (prioritaire) : la voix de l'Iqama a été détectée au micro
     * (le callback du détecteur est déjà sur l'EDT grâce à invokeLater).
     * Arrêt immédiat du minuteur, coupure du micro et retour instantané au Dashboard.
     */
    private void onIqamaDetected() {
        finishSequence();
    }

    /**
     * Termine la séquence une seule fois : arrêt du minuteur, coupure du micro
     * et retour au tableau de bord.
     */
    private void finishSequence() {
        if (sequenceEnded) {
            return;
        }
        sequenceEnded = true;
        sequenceActive = false;
        stopCountdownTimer();
        voiceDetector.stopListening();
        audioPlayer.stop(); // Par sécurité (ex. échec audio → fin immédiate).
        mainFrame.showDashboard();
    }

    /**
     * Contrat d'intégration : retour immédiat au tableau de bord.
     */
    public void returnToDashboard() {
        runOnEdt(this::finishSequence);
    }

    /**
     * Arrête tout (minuteur, micro, son) : utile à la fermeture de l'application.
     */
    public void stopAll() {
        sequenceEnded = true;
        sequenceActive = false;
        stopCountdownTimer();
        voiceDetector.stopListening();
        audioPlayer.stop();
    }

    private void stopCountdownTimer() {
        if (iqamaCountdownTimer != null) {
            iqamaCountdownTimer.stop();
            iqamaCountdownTimer = null;
        }
    }

    /**
     * Met à jour le compte à rebours discret de l'écran noir ("Attente Iqama : MM:SS").
     */
    private void updateCountdownLabel() {
        int seconds = Math.max(0, remainingSeconds);
        int minutes = seconds / 60;
        int secs = seconds % 60;
        mainFrame.getBlackScreenPanel().updateCountdown(String.format("%02d:%02d", minutes, secs));
    }

    /**
     * Lit "iqama.<prayerKey>" dans la configuration (valeur par défaut : 15 minutes,
     * bornée entre 1 et 60 comme dans l'AdminDialog).
     */
    private int getIqamaMinutes(String prayerKey) {
        int minutes = parseIntSafe(configManager.getProperty("iqama." + prayerKey, "15"), 15);
        return Math.max(1, Math.min(60, minutes));
    }

    /**
     * Analyse un entier de configuration avec valeur de secours si invalide.
     */
    private static int parseIntSafe(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Exécute sur l'EDT : TimeCheckService (Membre 1) peut appeler cette séquence
     * depuis n'importe quel thread ; les mises à jour Swing y restent sûres.
     */
    private static void runOnEdt(Runnable runnable) {
        if (SwingUtilities.isEventDispatchThread()) {
            runnable.run();
        } else {
            SwingUtilities.invokeLater(runnable);
        }
    }
}
