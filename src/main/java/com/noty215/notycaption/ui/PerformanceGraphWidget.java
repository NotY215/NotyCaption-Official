package com.noty215.notycaption.ui;

import com.noty215.notycaption.hardware.HardwareMonitor;
import com.noty215.notycaption.models.HardwareSnapshot;
import com.noty215.notycaption.utils.Translator;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class PerformanceGraphWidget extends JPanel {
    private HardwareMonitor hardwareMonitor;
    private Translator translator;
    private GraphType graphType;
    private int timeRange;
    private List<Double> dataPoints;
    private Queue<Double> dataQueue;
    private Timer refreshTimer;

    public enum GraphType {
        CPU, GPU, RAM, NETWORK, DISK
    }

    public PerformanceGraphWidget() {
        this.hardwareMonitor = HardwareMonitor.getInstance();
        this.translator = Translator.getInstance();
        this.graphType = GraphType.CPU;
        this.timeRange = 60;
        this.dataPoints = new ArrayList<>();
        this.dataQueue = new ConcurrentLinkedQueue<>();

        setPreferredSize(new Dimension(800, 400));
        setBackground(new Color(20, 20, 30));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initUI();
        startMonitoring();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // Control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.setOpaque(false);

        JComboBox<GraphType> typeCombo = new JComboBox<>(GraphType.values());
        typeCombo.setSelectedItem(GraphType.CPU);
        typeCombo.addActionListener(e -> {
            graphType = (GraphType) typeCombo.getSelectedItem();
            dataPoints.clear();
            repaint();
        });
        controlPanel.add(new JLabel("Performance:"));
        controlPanel.add(typeCombo);

        JComboBox<String> rangeCombo = new JComboBox<>(new String[]{"30s", "1m", "5m", "15m", "30m", "1h"});
        rangeCombo.setSelectedItem("1m");
        rangeCombo.addActionListener(e -> {
            String selected = (String) rangeCombo.getSelectedItem();
            if (selected.endsWith("s")) timeRange = Integer.parseInt(selected.substring(0, selected.length() - 1));
            else if (selected.endsWith("m")) timeRange = Integer.parseInt(selected.substring(0, selected.length() - 1)) * 60;
            else if (selected.endsWith("h")) timeRange = Integer.parseInt(selected.substring(0, selected.length() - 1)) * 3600;
            dataPoints.clear();
        });
        controlPanel.add(rangeCombo);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.addActionListener(e -> {
            dataPoints.clear();
            repaint();
        });
        controlPanel.add(refreshBtn);

        JButton exportBtn = new JButton("Export");
        exportBtn.addActionListener(e -> exportGraph());
        controlPanel.add(exportBtn);

        add(controlPanel, BorderLayout.NORTH);
    }

    private void startMonitoring() {
        refreshTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateData();
                repaint();
            }
        });
        refreshTimer.start();
    }

    private void updateData() {
        Queue<HardwareSnapshot> history = hardwareMonitor.getHistory();
        if (history == null || history.isEmpty()) return;

        LocalDateTime now = LocalDateTime.now();
        List<HardwareSnapshot> recent = new ArrayList<>();

        for (HardwareSnapshot snapshot : history) {
            if (snapshot.getTimestamp().isAfter(now.minusSeconds(timeRange))) {
                recent.add(snapshot);
            }
        }

        dataPoints.clear();
        for (HardwareSnapshot snapshot : recent) {
            double value = getValue(snapshot);
            dataPoints.add(value);
        }
    }

    private double getValue(HardwareSnapshot snapshot) {
        switch (graphType) {
            case CPU:
                return snapshot.getCpuUsage();
            case GPU:
                List<Double> gpuUsage = snapshot.getGpuUsage();
                if (!gpuUsage.isEmpty()) {
                    return gpuUsage.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                }
                return 0;
            case RAM:
                return snapshot.getRamUsage();
            case NETWORK:
                long[] networkIO = snapshot.getNetworkIO();
                if (networkIO != null && networkIO.length > 1) {
                    return (networkIO[0] + networkIO[1]) / (1024.0 * 1024);
                }
                return 0;
            case DISK:
                java.util.Map<String, Double> diskUsage = snapshot.getDiskUsage();
                if (!diskUsage.isEmpty()) {
                    return diskUsage.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
                }
                return 0;
            default:
                return 0;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth() - 20;
        int height = getHeight() - 60;
        int xOffset = 10;
        int yOffset = 40;

        // Draw background grid
        g2d.setColor(new Color(50, 50, 70));
        for (int i = 0; i <= 4; i++) {
            int y = yOffset + (height * i / 4);
            g2d.drawLine(xOffset, y, xOffset + width, y);
        }

        for (int i = 0; i <= 4; i++) {
            int x = xOffset + (width * i / 4);
            g2d.drawLine(x, yOffset, x, yOffset + height);
        }

        // Draw labels
        g2d.setColor(Color.WHITE);
        for (int i = 0; i <= 4; i++) {
            int y = yOffset + (height * i / 4);
            g2d.drawString(String.valueOf(100 - (i * 25)), xOffset - 25, y + 5);
        }

        // Draw data
        if (dataPoints.isEmpty()) {
            g2d.setColor(new Color(100, 100, 120));
            g2d.drawString("No data available", xOffset + width / 2 - 50, yOffset + height / 2);
            return;
        }

        Path2D path = new Path2D.Double();
        double step = (double) width / dataPoints.size();

        for (int i = 0; i < dataPoints.size(); i++) {
            double value = dataPoints.get(i);
            double x = xOffset + i * step;
            double y = yOffset + height - (value / 100.0 * height);
            y = Math.max(yOffset, Math.min(yOffset + height, y));

            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }

        // Draw filled area
        Path2D fillPath = new Path2D.Double(path);
        fillPath.lineTo(xOffset + width, yOffset + height);
        fillPath.lineTo(xOffset, yOffset + height);
        fillPath.closePath();

        g2d.setColor(new Color(74, 111, 165, 50));
        g2d.fill(fillPath);

        // Draw line
        g2d.setColor(new Color(74, 111, 165));
        g2d.setStroke(new BasicStroke(2f));
        g2d.draw(path);

        // Draw current value
        if (!dataPoints.isEmpty()) {
            double currentValue = dataPoints.get(dataPoints.size() - 1);
            g2d.setColor(Color.WHITE);
            g2d.drawString(String.format("%.1f%%", currentValue), xOffset + width - 40, yOffset - 5);
        }

        // Draw title
        g2d.setFont(new Font("Segoe UI", Font.BOLD, 12));
        g2d.drawString(graphType.name() + " Usage", xOffset + width / 2 - 30, yOffset - 10);
    }

    private void exportGraph() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("performance_graph.png"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
                Graphics2D g2d = image.createGraphics();
                paintAll(g2d);
                g2d.dispose();
                ImageIO.write(image, "png", chooser.getSelectedFile());
                JOptionPane.showMessageDialog(this,
                        "Graph exported successfully!",
                        "Export Complete",
                        JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this,
                        "Export failed: " + e.getMessage(),
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void stopMonitoring() {
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
    }
}