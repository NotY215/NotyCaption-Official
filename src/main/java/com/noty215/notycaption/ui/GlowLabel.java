package com.noty215.notycaption.ui;

import javax.swing.*;
import java.awt.*;

public class GlowLabel extends JLabel {
    private Color glowColor;
    private int glowIntensity;
    private boolean glowing;
    private Timer glowTimer;
    private float currentGlow;
    private float direction;
    
    public GlowLabel(String text) {
        super(text);
        this.glowColor = new Color(74, 111, 165);
        this.glowIntensity = 50;
        this.glowing = false;
        this.currentGlow = 0;
        this.direction = 0.05f;
        
        setFont(new Font("Segoe UI", Font.BOLD, 14));
        setForeground(Color.WHITE);
        
        glowTimer = new Timer(50, e -> {
            if (glowing) {
                currentGlow += direction;
                if (currentGlow >= 1.0f) {
                    currentGlow = 1.0f;
                    direction = -0.05f;
                } else if (currentGlow <= 0.0f) {
                    currentGlow = 0.0f;
                    direction = 0.05f;
                }
                repaint();
            }
        });
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        FontMetrics fm = g2d.getFontMetrics();
        int x = getAlignmentX() == CENTER ? (getWidth() - fm.stringWidth(getText())) / 2 : 0;
        int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
        
        // Draw glow effect
        if (glowing || currentGlow > 0) {
            float alpha = glowIntensity / 100.0f * currentGlow;
            Color glow = new Color(glowColor.getRed(), glowColor.getGreen(), 
                glowColor.getBlue(), (int) (150 * alpha));
            g2d.setColor(glow);
            for (int i = 1; i <= 3; i++) {
                g2d.drawString(getText(), x + i, y);
                g2d.drawString(getText(), x - i, y);
                g2d.drawString(getText(), x, y + i);
                g2d.drawString(getText(), x, y - i);
            }
        }
        
        // Draw text
        g2d.setColor(getForeground());
        g2d.drawString(getText(), x, y);
        
        g2d.dispose();
    }
    
    public void startGlow() {
        glowing = true;
        currentGlow = 0;
        direction = 0.05f;
        glowTimer.start();
    }
    
    public void stopGlow() {
        glowing = false;
        glowTimer.stop();
        currentGlow = 0;
        repaint();
    }
    
    public void setGlowColor(Color color) {
        this.glowColor = color;
        repaint();
    }
    
    public void setGlowIntensity(int intensity) {
        this.glowIntensity = Math.max(0, Math.min(100, intensity));
        repaint();
    }
}