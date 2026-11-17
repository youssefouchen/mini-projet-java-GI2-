package com.tawkit.controller;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import javax.swing.SwingUtilities;

/**
 * [MEMBRE 3] Écoute du microphone en arrière-plan, uniquement pendant l'écran noir.
 * Ouvre une TargetDataLine (16 kHz, 16 bits, Mono) dans un Thread séparé,
 * calcule le volume RMS de chaque tampon audio et, si le seuil configuré est
 * dépassé en continu pendant ~2,5 secondes, exécute le callback
 * SwingUtilities.invokeLater(onIqamaDetected) pour couper l'écran noir.
 */
public class IqamaVoiceDetector {

    /** Format micro demandé : 16 kHz, 16 bits, Mono, signé, little-endian. */
    private static final AudioFormat MIC_FORMAT = new AudioFormat(16000f, 16, 1, true, false);

    /** Taille de lecture : 4096 octets = 2048 échantillons ≈ 128 ms. */
    public static final int READ_CHUNK_BYTES = 4096;

    /** Durée de voix continue avant détection (Condition A du cahier des charges). */
    private static final long DETECTION_DURATION_MS = 2500L;

    private volatile boolean running = false;
    private volatile boolean detectionTriggered = false;
    /** Ligne micro : possédée et fermée uniquement par le thread d'écoute. */
    private TargetDataLine micLine;
    private Thread listenerThread;

    /**
     * Démarre l'écoute du microphone dans un thread d'arrière-plan.
     *
     * @param threshold        seuil de déclenchement (échelle 0..100, identique à
     *                         computeRmsLevel, réglable via l'AdminDialog)
     * @param onIqamaDetected  callback exécuté sur l'EDT dès que l'Iqama est détectée
     */
    public void startListening(int threshold, Runnable onIqamaDetected) {
        if (running) {
            return; // Déjà en écoute
        }
        detectionTriggered = false;
        running = true;
        listenerThread = new Thread(() -> listenLoop(threshold, onIqamaDetected), "IqamaVoiceDetector");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    /**
     * Arrête l'écoute et libère le microphone (appelé au retour au Dashboard).
     * Sans effet si l'écoute est déjà terminée.
     */
    public void stopListening() {
        running = false; // Le thread sort de sa boucle et ferme lui-même la ligne.
    }

    /**
     * @return true tant que le thread d'écoute est actif
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * @return true si l'Iqama a été détectée lors de la dernière écoute
     */
    public boolean isDetectionTriggered() {
        return detectionTriggered;
    }

    // ------------------------------------------------------------------
    // Boucle d'écoute (thread d'arrière-plan)
    // ------------------------------------------------------------------

    private void listenLoop(int threshold, Runnable onIqamaDetected) {
        byte[] buffer = new byte[READ_CHUNK_BYTES];
        long continuousVoiceMs = 0L;
        try {
            micLine = openMicLine();
            micLine.start();
            while (running && !detectionTriggered) {
                int bytesRead = micLine.read(buffer, 0, buffer.length);
                if (bytesRead <= 0) {
                    continue;
                }
                double level = computeRmsLevel(buffer, bytesRead);
                long chunkMs = chunkDurationMs(bytesRead);

                if (level >= threshold || isIqamaKeyword(buffer, bytesRead)) {
                    continuousVoiceMs += chunkMs;
                    if (continuousVoiceMs >= DETECTION_DURATION_MS) {
                        detectionTriggered = true;
                        System.out.println("[IqamaVoiceDetector] Voix de l'Iqama détectée (niveau "
                                + String.format("%.1f", level) + ", seuil " + threshold + ")");
                        if (onIqamaDetected != null) {
                            // Modifier l'interface sans blocage : retour sur l'EDT.
                            SwingUtilities.invokeLater(onIqamaDetected);
                        }
                    }
                } else {
                    // La voix doit être continue : un silence remet le compteur à zéro.
                    continuousVoiceMs = 0L;
                }
            }
        } catch (Exception e) {
            System.err.println("[IqamaVoiceDetector] Microphone indisponible : "
                    + "le minuteur de secours (Condition B) prendra le relais.");
            e.printStackTrace();
        } finally {
            closeMicLine();
            running = false;
        }
    }

    /**
     * Ouvre la ligne microphone au format 16 kHz / 16 bits / Mono.
     */
    private TargetDataLine openMicLine() throws Exception {
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, MIC_FORMAT);
        if (!AudioSystem.isLineSupported(info)) {
            throw new IllegalStateException("Ligne micro 16 kHz / 16 bits / Mono non supportée");
        }
        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(MIC_FORMAT);
        return line;
    }

    /**
     * Ferme proprement la ligne micro (depuis le thread d'écoute uniquement).
     */
    private void closeMicLine() {
        TargetDataLine line = micLine;
        micLine = null;
        if (line != null) {
            try {
                line.stop();
                line.close();
            } catch (Exception ignored) {
                // La ligne est peut-être déjà fermée : rien à faire.
            }
        }
    }

    /**
     * Durée en millisecondes d'un tampon capté au format 16 kHz / 16 bits / Mono.
     */
    private static long chunkDurationMs(int bytesRead) {
        long sampleCount = bytesRead / 2L; // 16 bits = 2 octets par échantillon
        return sampleCount * 1000L / 16000L;
    }

    // ------------------------------------------------------------------
    // Calcul du niveau RMS
    // Méthode public static : l'AdminDialog (test de calibrage en direct)
    // utilise exactement la même échelle que la détection.
    // ------------------------------------------------------------------

    /**
     * Calcule le volume RMS d'un tampon PCM 16 bits signé little-endian :
     * RMS = sqrt( (1/N) * somme(xi²) ), normalisé sur une échelle 0..100.
     *
     * @param audioBytes tampon brut capté depuis la TargetDataLine
     * @param length     nombre d'octets réellement lus dans le tampon
     * @return niveau sonore de 0 (silence) à 100 (saturation)
     */
    public static double computeRmsLevel(byte[] audioBytes, int length) {
        if (audioBytes == null || length < 2) {
            return 0.0;
        }
        int sampleCount = length / 2;
        ByteBuffer wrapped = ByteBuffer.wrap(audioBytes, 0, sampleCount * 2)
                .order(ByteOrder.LITTLE_ENDIAN);
        double sumOfSquares = 0.0;
        for (int i = 0; i < sampleCount; i++) {
            double sample = wrapped.getShort(i * 2);
            sumOfSquares += sample * sample;
        }
        double rms = Math.sqrt(sumOfSquares / sampleCount);
        double normalized = rms / 32767.0 * 100.0;
        return Math.min(100.0, normalized);
    }

    /**
     * @return le format audio utilisé pour la capture micro
     */
    public static AudioFormat getMicAudioFormat() {
        return MIC_FORMAT;
    }

    // ------------------------------------------------------------------
    // [OPTION AVANCÉE — Vosk]
    // TODO : brancher ici l'API Vosk (modèle arabe) pour reconnaître les mots
    // "أكبر الله" (Allahu Akbar) ou "قامت قدم الصلاة" (Qad iqamatis salah)
    // dans le tampon audio, afin de détecter l'Iqama même si le seuil RMS
    // est mal calibré. Retourne false tant que Vosk n'est pas intégré :
    // la détection repose alors uniquement sur le seuil RMS.
    // ------------------------------------------------------------------
    private static boolean isIqamaKeyword(byte[] buffer, int length) {
        return false;
    }
}
