package com.noty215.notycaption.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class GlassCardWidget extends JPanel {
    private float opacity;
    private Color borderColor;
    private Timer hoverTimer;
    private boolean isHovered;
    private int targetPadding;
    private int currentPadding;
    
    public GlassCardWidget() {
        this.opacity = 0.8f;
        this.borderColor = new Color(74, 111, 165);
        this.isHovered = false;
        this.targetPadding = 5;
        this.currentPadding = 0;
        
        setOpaque(false);
        setLayout(new BorderLayout());
        
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                isHovered = true;
                startHoverAnimation(true);
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                startHoverAnimation(false);
            }
        });
        
        hoverTimer = new Timer(16, e -> {
            currentPadding += (targetPadding - currentPadding) * 0.2f;
            if (Math.abs(targetPadding - currentPadding) < 0.5f) {
                hoverTimer.stop();
            }
            repaint();
        });
    }
    
    private void startHoverAnimation(boolean entering) {
        targetPadding = entering ? 5 : 0;
        hoverTimer.start();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int width = getWidth();
        int height = getHeight();
        int pad = (int) currentPadding;
        
        // Draw glass background
        Color bgColor = new Color(30, 30, 42, (int) (opacity * 255));
        g2d.setColor(bgColor);
        g2d.fillRoundRect(pad, pad, width - 1 - 2 * pad, height - 1 - 2 * pad, 15, 15);
        
        // Draw border
        if (isHovered) {
            g2d.setColor(borderColor);
            g2d.setStroke(new BasicStroke(2f));
            g2d.drawRoundRect(pad, pad, width - 1 - 2 * pad, height - 1 - 2 * pad, 15, 15);
        } else {
            g2d.setColor(new Color(60, 60, 80));
            g2d.drawRoundRect(pad, pad, width - 1 - 2 * pad, height - 1 - 2 * pad, 15, 15);
        }
        
        // Draw shadow effect
        g2d.setColor(new Color(0, 0, 0, 30));
        g2d.fillRoundRect(pad + 2, pad + 2, width - 1 - 2 * pad, height - 1 - 2 * pad, 15, 15);
        
        g2d.dispose();
        super.paintComponent(g);
    }
    
    public void setOpacity(float opacity) {
        this.opacity = Math.max(0.3f, Math.min(1.0f, opacity));
        repaint();
    }
    
    public void setBorderColor(Color color) {
        this.borderColor = color;
        repaint();
    }
}