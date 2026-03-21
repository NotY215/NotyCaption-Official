package com.noty215.notycaption.hardware;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class MonitorManager {
    private static final Logger logger = Logger.getLogger(MonitorManager.class.getName());
    private List<MonitorInfo> monitors;

    public static class MonitorInfo {
        private int index;
        private String name;
        private Rectangle geometry;
        private Rectangle availableGeometry;
        private Dimension size;
        private Dimension physicalSize;
        private double logicalDpi;
        private double physicalDpi;
        private double devicePixelRatio;
        private double refreshRate;
        private int colorDepth;
        private boolean primary;

        public MonitorInfo(GraphicsDevice device, int index) {
            this.index = index;
            this.name = device.getIDstring();
            this.geometry = device.getDefaultConfiguration().getBounds();
            this.availableGeometry = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration().getBounds();
            this.size = device.getDefaultConfiguration().getBounds().getSize();
            this.physicalSize = Toolkit.getDefaultToolkit().getScreenSize();
            this.logicalDpi = Toolkit.getDefaultToolkit().getScreenResolution();
            this.physicalDpi = Toolkit.getDefaultToolkit().getScreenResolution();
            this.devicePixelRatio = 1.0;
            this.refreshRate = 60.0;
            this.colorDepth = device.getDefaultConfiguration().getColorModel().getPixelSize();
            this.primary = (index == 0);
        }

        public int getIndex() { return index; }
        public String getName() { return name; }
        public Rectangle getGeometry() { return geometry; }
        public Rectangle getAvailableGeometry() { return availableGeometry; }
        public Dimension getSize() { return size; }
        public Dimension getPhysicalSize() { return physicalSize; }
        public double getLogicalDpi() { return logicalDpi; }
        public double getPhysicalDpi() { return physicalDpi; }
        public double getDevicePixelRatio() { return devicePixelRatio; }
        public double getRefreshRate() { return refreshRate; }
        public int getColorDepth() { return colorDepth; }
        public boolean isPrimary() { return primary; }

        @Override
        public String toString() {
            return String.format("Monitor %d: %s (%dx%d) - %s", 
                index + 1, name, geometry.width, geometry.height, 
                primary ? "Primary" : "Secondary");
        }
    }

    public MonitorManager() {
        monitors = new ArrayList<>();
        detectMonitors();
    }

    public void detectMonitors() {
        monitors.clear();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] devices = ge.getScreenDevices();

        for (int i = 0; i < devices.length; i++) {
            MonitorInfo info = new MonitorInfo(devices[i], i);
            monitors.add(info);
            logger.info("Detected monitor: " + info);
        }
    }

    public int getMonitorCount() {
        return monitors.size();
    }

    public MonitorInfo getMonitor(int index) {
        if (index >= 0 && index < monitors.size()) {
            return monitors.get(index);
        }
        return null;
    }

    public MonitorInfo getPrimaryMonitor() {
        for (MonitorInfo monitor : monitors) {
            if (monitor.isPrimary()) {
                return monitor;
            }
        }
        return monitors.isEmpty() ? null : monitors.get(0);
    }

    public void moveWindowToMonitor(JFrame window, int monitorIndex) {
        MonitorInfo monitor = getMonitor(monitorIndex);
        if (monitor != null) {
            Rectangle geometry = monitor.getGeometry();
            window.setLocation(geometry.x, geometry.y);
            window.setSize(geometry.width, geometry.height);
            logger.info("Moved window to monitor " + monitorIndex);
        }
    }

    public JFrame createMonitorWindow(JFrame parent, int monitorIndex) {
        MonitorInfo monitor = getMonitor(monitorIndex);
        if (monitor == null) return null;

        JFrame window = new JFrame("NotyCaption - Monitor " + (monitorIndex + 1));
        Rectangle geometry = monitor.getGeometry();
        window.setBounds(geometry.x, geometry.y, geometry.width / 2, geometry.height / 2);
        
        if (parent != null) {
            window.setIconImage(parent.getIconImage());
        }
        
        return window;
    }

    public List<MonitorInfo> getMonitors() {
        return new ArrayList<>(monitors);
    }

    public String getMonitorsSummary() {
        StringBuilder sb = new StringBuilder();
        for (MonitorInfo monitor : monitors) {
            sb.append(monitor.toString()).append("\n");
            sb.append("  Resolution: ").append(monitor.getGeometry().width)
              .append("x").append(monitor.getGeometry().height).append("\n");
            sb.append("  Refresh Rate: ").append(monitor.getRefreshRate()).append(" Hz\n");
            sb.append("  DPI: ").append(monitor.getLogicalDpi()).append("\n");
        }
        return sb.toString();
    }
}