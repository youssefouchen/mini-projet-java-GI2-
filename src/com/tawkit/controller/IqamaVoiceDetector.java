package com.tawkit.controller;

/**
 * [MEMBRE 3] Écoute Microphone en arrière-plan (calcul RMS / Vosk API) pendant l'écran noir.
 */
public class IqamaVoiceDetector {
    private volatile boolean running = false;

    public void startListening(int threshold, Runnable onIqamaDetected) {
        running = true;
        // TODO [MEMBRE 3] : Ouvrir TargetDataLine (16 kHz, 16 bits, Mono) dans un Thread séparé,
        // calculer le RMS et exécuter SwingUtilities.invokeLater(onIqamaDetected) si détecté.
    }

    public void stopListening() {
        running = false;
        // TODO [MEMBRE 3] : Fermer proprement la ligne microphone
    }

    public boolean isRunning() {
        return running;
    }
}
