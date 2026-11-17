package com.tawkit.controller;

/**
 * [MEMBRE 3] Lecteur Audio de l'Adhan avec détection de fin de piste via LineListener.
 */
public class AdhanAudioPlayer {
    private static final String ADHAN_AUDIO_PATH = "resources/audio/adhan.wav";

    public void playAdhan(Runnable onAdhanFinished) {
        // TODO [MEMBRE 3] : Jouer resources/audio/adhan.wav avec javax.sound.sampled.Clip
        // et appeler onAdhanFinished lorsque LineEvent.Type.STOP est déclenché.
    }

    public void stop() {
        // TODO [MEMBRE 3] : Arrêter le Clip audio s'il est en cours de lecture
    }
}
