package com.tawkit.controller;

import java.io.File;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.swing.SwingUtilities;

/**
 * [MEMBRE 3] Lecteur Audio de l'Adhan avec détection de fin de piste via LineListener.
 * Joue resources/audio/adhan.wav avec javax.sound.sampled.Clip et déclenche le
 * passage à l'écran noir dès que LineEvent.Type.STOP est détecté (fin naturelle du son).
 */
public class AdhanAudioPlayer {

    private static final String ADHAN_AUDIO_PATH = "resources/audio/adhan.wav";

    private volatile Clip clip;
    /** Distingue un arrêt manuel (stop()) d'une fin de piste naturelle. */
    private volatile boolean manuallyStopped = false;

    /**
     * Joue l'Adhan et déclenche onAdhanFinished (sur l'EDT) dès que le son se termine.
     * En cas d'erreur audio (fichier manquant, format non supporté, périphérique
     * indisponible), le callback est malgré tout appelé afin que la séquence
     * (écran noir + minuteur d'Iqama) puisse se poursuivre.
     *
     * @param onAdhanFinished callback exécuté sur l'EDT à la fin de la lecture
     */
    public void playAdhan(Runnable onAdhanFinished) {
        manuallyStopped = false;
        try {
            File adhanFile = new File(ADHAN_AUDIO_PATH);
            if (!adhanFile.exists()) {
                System.err.println("[AdhanAudioPlayer] Fichier introuvable : " + adhanFile.getAbsolutePath());
            }
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(adhanFile);
            double durationSec = audioStream.getFrameLength()
                    / audioStream.getFormat().getFrameRate();
            System.out.println("[AdhanAudioPlayer] Lecture de l'Adhan : " + adhanFile.getAbsolutePath()
                    + " (" + String.format("%.1f", durationSec) + " s)");
            if (durationSec < 2.0) {
                System.err.println("[AdhanAudioPlayer] ATTENTION : fichier de "
                        + String.format("%.1f", durationSec)
                        + " s seulement - remplacer resources/audio/adhan.wav par un vrai Adhan.");
            }
            Clip newClip = AudioSystem.getClip();
            newClip.addLineListener(new LineListener() {
                @Override
                public void update(LineEvent event) {
                    // STOP est déclenché à la fin naturelle de la piste :
                    // on passe automatiquement à l'écran noir.
                    if (event.getType() == LineEvent.Type.STOP && !manuallyStopped) {
                        closeClip();
                        SwingUtilities.invokeLater(onAdhanFinished);
                    }
                }
            });
            this.clip = newClip;
            newClip.open(audioStream);
            newClip.start();
        } catch (Exception e) {
            System.err.println("[AdhanAudioPlayer] Impossible de jouer l'Adhan : " + e.getMessage());
            e.printStackTrace();
            closeClip();
            // Ne pas bloquer la machine à états : enchaîner malgré tout la séquence.
            SwingUtilities.invokeLater(onAdhanFinished);
        }
    }

    /**
     * Arrête immédiatement la lecture de l'Adhan (sans déclencher la fin de séquence).
     */
    public void stop() {
        manuallyStopped = true;
        closeClip();
    }

    /**
     * @return true si un Adhan est actuellement en cours de lecture
     */
    public boolean isPlaying() {
        Clip current = clip;
        return current != null && current.isRunning();
    }

    /**
     * Ferme et libère le Clip. Sans effet si aucun son n'est en cours.
     */
    private void closeClip() {
        Clip current = clip;
        clip = null;
        if (current != null) {
            try {
                current.stop();
                current.close();
            } catch (Exception ignored) {
                // La ligne est peut-être déjà fermée : rien à faire.
            }
        }
    }
}
