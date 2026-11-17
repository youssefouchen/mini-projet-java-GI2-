package com.tawkit.view;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * [MEMBRE 2] Écran Noir post-Adhan avec compte à rebours discret et indicateur micro.
 */
public class BlackScreenPanel extends JPanel {
    private final JLabel countdownLabel;
    private final JLabel micStatusLabel;

    public BlackScreenPanel() {
        setLayout(new BorderLayout());
        setBackground(Color.BLACK);

        countdownLabel = new JLabel("Attente Iqama : --:--", SwingConstants.CENTER);
        countdownLabel.setForeground(new Color(180, 130, 50)); // Ambre doux / gris foncé

        micStatusLabel = new JLabel("Micro actif", SwingConstants.CENTER);
        micStatusLabel.setForeground(Color.DARK_GRAY);

        add(countdownLabel, BorderLayout.CENTER);
        add(micStatusLabel, BorderLayout.SOUTH);
    }

    public void updateCountdown(String formattedTime) {
        countdownLabel.setText("Attente Iqama : " + formattedTime);
    }

    public void setMicActive(boolean active) {
        micStatusLabel.setVisible(active);
    }
}
