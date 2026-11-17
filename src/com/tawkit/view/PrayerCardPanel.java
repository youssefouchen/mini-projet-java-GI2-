package com.tawkit.view;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JPanel;

/**
 * [MEMBRE 2] Carte individuelle d'une prière avec coins arrondis et mise en surbrillance.
 */
public class PrayerCardPanel extends JPanel {
    private String prayerName;
    private String prayerTime;
    private boolean highlighted;

    public PrayerCardPanel(String prayerName, String prayerTime) {
        this.prayerName = prayerName;
        this.prayerTime = prayerTime;
        setOpaque(false);
    }

    public void setHighlighted(boolean isNext) {
        this.highlighted = isNext;
        repaint();
    }

    public void setPrayerTime(String prayerTime) {
        this.prayerTime = prayerTime;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // TODO [MEMBRE 2] : Dessiner la carte aux coins arrondis et la bordure si highlighted == true
        g2d.dispose();
    }
}
