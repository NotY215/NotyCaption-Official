package com.noty215.notycaption.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class GlowButton extends JButton {
    private Color glowColor;
    private int glowIntensity;
    private Timer glowTimer;
    private boolean isHovered;
    private float currentGlow;
    private float targetGlow;
    
    public GlowButton(String text) {
        super(text);
        this.glowColor = new Color(74, 111, 165);
        this.glowIntensity = 50;
        this.isHovered = false;
        this.currentGlow = 0;
        this.targetGlow = 0;
        
        setOpaque(false);
        setContentAreaFilled(false);
        setBorder(new EmptyBorder(8, 16, 8, 16));
        setFont(new Font("Segoe UI", Font.BOLD, 13));
        setForeground(Color.WHITE);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                startGlowAnimation(1.0f);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                startGlowAnimation(0.0f);
            }
            
            @Override
            public void mousePressed(MouseEvent e) {
                startPressAnimation();
            }
        });
        
        glowTimer = new Timer(16, e -> {
            currentGlow += (targetGlow - currentGlow) * 0.2f;
            if (Math.abs(targetGlow - currentGlow) < 0.01f) {
                glowTimer.stop();
            }
            repaint();
        });
    }
    
    private void startGlowAnimation(float target) {
        targetGlow = target;
        glowTimer.start();
    }
    
    private void startPressAnimation() {
        new Timer(100, new AbstractAction() {
            float scale = 1.0f;
            int elapsed = 0;
            
            @Override
            public void actionPerformed(ActionEvent e) {
                elapsed += 16;
                if (elapsed < 100) {
                    scale = 0.95f + (elapsed / 2000.0f);
                    repaint();
                } else {
                    ((Timer) e.getSource()).stop();
                }
            }
        }).start();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int width = getWidth();
        int height = getHeight();
        
        // Draw background
        GradientPaint gradient = new GradientPaint(0, 0, new Color(42, 42, 58), 
            width, height, new Color(26, 26, 38));
        g2d.setPaint(gradient);
        g2d.fillRoundRect(0, 0, width, height, 10, 10);
        
        // Draw glow effect
        if (currentGlow > 0.01f) {
            int glowRadius = (int) (glowIntensity * currentGlow);
            Color glow = new Color(glowColor.getRed(), glowColor.getGreen(), 
                glowColor.getBlue(), (int) (100 * currentGlow));
            g2d.setColor(glow);
            for (int i = 0; i < 3; i++) {
                g2d.drawRoundRect(i, i, width - 1 - 2 * i, height - 1 - 2 * i, 10, 10);
            }
        }
        
        // Draw border
        if (isHovered) {
            g2d.setColor(glowColor);
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawRoundRect(1, 1, width - 3, height - 3, 10, 10);
        }
        
        // Draw text
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (width - fm.stringWidth(getText())) / 2;
        int textY = (height + fm.getAscent() - fm.getDescent()) / 2;
        g2d.setColor(getForeground());
        g2d.drawString(getText(), textX, textY);
        
        g2d.dispose();
        super.paintComponent(g);
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