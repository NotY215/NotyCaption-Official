package com.noty215.notycaption;

import com.noty215.notycaption.ui.NotyCaptionWindow;
import com.noty215.notycaption.utils.SingleInstance;
import com.noty215.notycaption.utils.ResourcePath;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.logging.*;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    
    public static void main(String[] args) {
        // Set up logging
        setupLogging();
        
        // Check single instance
        SingleInstance instance = new SingleInstance();
        if (instance.isAlreadyRunning()) {
            LOGGER.warning("Duplicate instance detected");
            JOptionPane.showMessageDialog(null, 
                "NotyCaption is already open in another window.", 
                "Already Running", 
                JOptionPane.WARNING_MESSAGE);
            System.exit(1);
        }
        
        // Set system look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            LOGGER.warning("Failed to set system look and feel: " + e.getMessage());
        }
        
        // Create and show splash screen
        SplashScreen splash = showSplashScreen();
        
        // Launch application
        SwingUtilities.invokeLater(() -> {
            try {
                NotyCaptionWindow window = new NotyCaptionWindow();
                if (splash != null) {
                    splash.close();
                }
                window.setVisible(true);
                LOGGER.info("Main window displayed");
            } catch (Exception e) {
                LOGGER.severe("Failed to start application: " + e.getMessage());
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                    "Failed to start application: " + e.getMessage(),
                    "Startup Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        });
    }
    
    private static void setupLogging() {
        try {
            String appDataDir = ResourcePath.getAppDataDir();
            String logDir = appDataDir + "/logs";
            new java.io.File(logDir).mkdirs();
            
            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss.SSS")
                .format(new java.util.Date());
            String logFile = logDir + "/NotyCaption_" + timestamp + ".log";
            
            FileHandler fileHandler = new FileHandler(logFile);
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
            
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(consoleHandler);
            
            LOGGER.setLevel(Level.INFO);
            
            LOGGER.info("=".repeat(80));
            LOGGER.info("NotyCaption Pro Launch - Version " + App.VERSION);
            LOGGER.info("=".repeat(80));
            LOGGER.info("Java version: " + System.getProperty("java.version"));
            LOGGER.info("Platform: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
            LOGGER.info("Working directory: " + System.getProperty("user.dir"));
            LOGGER.info("App data directory: " + appDataDir);
            LOGGER.info("Log file: " + logFile);
            
        } catch (IOException e) {
            System.err.println("Failed to setup logging: " + e.getMessage());
        }
    }
    
    private static SplashScreen showSplashScreen() {
        try {
            ImageIcon icon = ResourcePath.getImageIcon("splash.png");
            if (icon == null) {
                // Create default splash
                JWindow splash = new JWindow();
                JPanel panel = new JPanel() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        g.setColor(new Color(26, 26, 26));
                        g.fillRect(0, 0, getWidth(), getHeight());
                        g.setColor(new Color(74, 111, 165));
                        g.setFont(new Font("Segoe UI", Font.BOLD, 24));
                        FontMetrics fm = g.getFontMetrics();
                        String text = App.APP_NAME;
                        int x = (getWidth() - fm.stringWidth(text)) / 2;
                        int y = (getHeight() + fm.getAscent()) / 2;
                        g.drawString(text, x, y);
                    }
                };
                panel.setPreferredSize(new Dimension(800, 600));
                splash.add(panel);
                splash.pack();
                splash.setLocationRelativeTo(null);
                splash.setVisible(true);
                return new SplashScreen(splash);
            } else {
                JWindow splash = new JWindow();
                JLabel label = new JLabel(icon);
                splash.add(label);
                splash.pack();
                splash.setLocationRelativeTo(null);
                splash.setVisible(true);
                return new SplashScreen(splash);
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to show splash screen: " + e.getMessage());
            return null;
        }
    }
    
    private static class SplashScreen {
        private final JWindow window;
        
        SplashScreen(JWindow window) {
            this.window = window;
        }
        
        void close() {
            if (window != null) {
                window.dispose();
            }
        }
    }
}