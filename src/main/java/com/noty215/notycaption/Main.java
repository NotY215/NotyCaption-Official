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

        // Create splash screen
        JWindow splash = createSplashScreen();

        // Launch application
        SwingUtilities.invokeLater(() -> {
            try {
                NotyCaptionWindow window = new NotyCaptionWindow();
                if (splash != null) {
                    splash.setVisible(false);
                    splash.dispose();
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
                System.exit(1);
            }
        });
    }

    private static JWindow createSplashScreen() {
        try {
            JWindow splash = new JWindow();
            JPanel panel = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    Graphics2D g2d = (Graphics2D) g;
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    // Background gradient
                    GradientPaint gradient = new GradientPaint(0, 0, new Color(26, 26, 46),
                            getWidth(), getHeight(), new Color(10, 10, 20));
                    g2d.setPaint(gradient);
                    g2d.fillRect(0, 0, getWidth(), getHeight());

                    // Draw border
                    g2d.setColor(new Color(74, 111, 165));
                    g2d.setStroke(new BasicStroke(2f));
                    g2d.drawRect(5, 5, getWidth() - 11, getHeight() - 11);

                    // Draw title
                    g2d.setColor(Color.WHITE);
                    g2d.setFont(new Font("Segoe UI", Font.BOLD, 28));
                    FontMetrics fm = g2d.getFontMetrics();
                    String title = App.APP_NAME;
                    int titleX = (getWidth() - fm.stringWidth(title)) / 2;
                    int titleY = getHeight() / 2 - 30;
                    g2d.drawString(title, titleX, titleY);

                    // Draw subtitle
                    g2d.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                    g2d.setColor(new Color(176, 176, 176));
                    fm = g2d.getFontMetrics();
                    String subtitle = "Professional AI Caption Generator";
                    int subX = (getWidth() - fm.stringWidth(subtitle)) / 2;
                    int subY = titleY + 40;
                    g2d.drawString(subtitle, subX, subY);

                    // Draw version
                    g2d.setFont(new Font("Segoe UI", Font.PLAIN, 11));
                    g2d.setColor(new Color(100, 100, 120));
                    String version = "Version " + App.VERSION;
                    int versionX = (getWidth() - fm.stringWidth(version)) / 2;
                    int versionY = getHeight() - 30;
                    g2d.drawString(version, versionX, versionY);

                    // Draw loading indicator
                    g2d.setColor(new Color(74, 111, 165));
                    int dotSize = 8;
                    int spacing = 12;
                    int startX = (getWidth() - (3 * dotSize + 2 * spacing)) / 2;
                    int y = getHeight() - 60;
                    for (int i = 0; i < 3; i++) {
                        int alpha = 100 + (int)(155 * Math.sin(System.currentTimeMillis() / 200.0 + i));
                        g2d.setColor(new Color(74, 111, 165, alpha));
                        g2d.fillOval(startX + i * (dotSize + spacing), y, dotSize, dotSize);
                    }
                }
            };
            panel.setPreferredSize(new Dimension(600, 400));
            splash.add(panel);
            splash.pack();
            splash.setLocationRelativeTo(null);
            splash.setVisible(true);

            // Animate splash
            new Timer(50, e -> panel.repaint()).start();

            return splash;
        } catch (Exception e) {
            LOGGER.warning("Failed to create splash screen: " + e.getMessage());
            return null;
        }
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
}