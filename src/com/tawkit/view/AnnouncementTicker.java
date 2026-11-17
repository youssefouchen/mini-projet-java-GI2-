package com.tawkit.view;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * [MEMBRE 2] Bandeau affichant les annonces de la mosquée ou des hadiths.
 */
public class AnnouncementTicker extends JPanel {
    private final JLabel messageLabel;

    public AnnouncementTicker(String initialText) {
        this.messageLabel = new JLabel(initialText, SwingConstants.CENTER);
        add(messageLabel);
        // TODO [MEMBRE 2] : Faire défiler ou alterner les annonces toutes les 30 secondes
    }

    public void setAnnouncementText(String text) {
        messageLabel.setText(text);
    }
}
