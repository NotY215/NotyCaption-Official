package com.noty215.notycaption.hardware;

import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Manages multiple monitor configurations
 */
public class MonitorManager {

    private List<Map<String, Object>> monitors;

    public MonitorManager() {
        monitors = new ArrayList<>();
        detectMonitors();
    }

    public void detectMonitors() {
        monitors.clear();
        List<Screen> screens = Screen.getScreens();

        for (int i = 0; i < screens.size(); i++) {
            Screen screen = screens.get(i);
            Rectangle2D bounds = screen.getBounds();
            Rectangle2D visualBounds = screen.getVisualBounds();

            Map<String, Object> monitor = Map.of(
                    "index", i,
                    "name", "Monitor " + (i + 1),
                    "geometry", Map.of(
                            "x", bounds.getMinX(),
                            "y", bounds.getMinY(),
                            "width", bounds.getWidth(),
                            "height", bounds.getHeight()
                    ),
                    "visual_bounds", Map.of(
                            "x", visualBounds.getMinX(),
                            "y", visualBounds.getMinY(),
                            "width", visualBounds.getWidth(),
                            "height", visualBounds.getHeight()
                    ),
                    "dpi", screen.getDpi(),
                    "primary", i == 0
            );
            monitors.add(monitor);
        }

        org.slf4j.LoggerFactory.getLogger(MonitorManager.class).info("Detected {} monitors", monitors.size());
    }

    public int getMonitorCount() {
        return monitors.size();
    }

    public Map<String, Object> getMonitor(int index) {
        if (index >= 0 && index < monitors.size()) {
            return monitors.get(index);
        }
        return null;
    }

    public Map<String, Object> getPrimaryMonitor() {
        for (Map<String, Object> monitor : monitors) {
            if ((boolean) monitor.get("primary")) {
                return monitor;
            }
        }
        return monitors.isEmpty() ? null : monitors.get(0);
    }

    public void moveWindowToMonitor(Stage window, int monitorIndex) {
        Map<String, Object> monitor = getMonitor(monitorIndex);
        if (monitor != null) {
            @SuppressWarnings("unchecked")
            Map<String, Number> geometry = (Map<String, Number>) monitor.get("geometry");
            double x = geometry.get("x").doubleValue();
            double y = geometry.get("y").doubleValue();
            double width = geometry.get("width").doubleValue();
            double height = geometry.get("height").doubleValue();

            window.setX(x);
            window.setY(y);
            window.setWidth(width);
            window.setHeight(height);
        }
    }

    public Stage createMonitorWindow(Stage parent, int monitorIndex) {
        Map<String, Object> monitor = getMonitor(monitorIndex);
        if (monitor == null) return null;

        @SuppressWarnings("unchecked")
        Map<String, Number> geometry = (Map<String, Number>) monitor.get("geometry");
        double x = geometry.get("x").doubleValue();
        double y = geometry.get("y").doubleValue();
        double width = geometry.get("width").doubleValue() / 2;
        double height = geometry.get("height").doubleValue() / 2;

        Stage newWindow = new Stage();
        newWindow.setTitle(parent.getTitle() + " - Monitor " + (monitorIndex + 1));
        newWindow.setX(x);
        newWindow.setY(y);
        newWindow.setWidth(width);
        newWindow.setHeight(height);

        return newWindow;
    }

    public Rectangle2D getMonitorBounds(int index) {
        Map<String, Object> monitor = getMonitor(index);
        if (monitor != null) {
            @SuppressWarnings("unchecked")
            Map<String, Number> geometry = (Map<String, Number>) monitor.get("geometry");
            return new Rectangle2D(
                    geometry.get("x").doubleValue(),
                    geometry.get("y").doubleValue(),
                    geometry.get("width").doubleValue(),
                    geometry.get("height").doubleValue()
            );
        }
        return null;
    }
}