package com.noty215.notycaption.ui;

import com.noty215.notycaption.hardware.HardwareMonitor;
import com.noty215.notycaption.models.HardwareSnapshot;
import com.noty215.notycaption.utils.Translator;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Widget for displaying performance graphs
 */
public class PerformanceGraphWidget extends VBox {

    private final HardwareMonitor hardwareMonitor;
    private final Translator translator;

    private ComboBox<String> typeCombo;
    private ComboBox<String> rangeCombo;
    private LineChart<Number, Number> chart;
    private XYChart.Series<Number, Number> dataSeries;

    private List<HardwareSnapshot> history;
    private AnimationTimer timer;
    private String currentType = "cpu";
    private int timeRange = 60; // seconds

    public PerformanceGraphWidget(HardwareMonitor hardwareMonitor, Translator translator) {
        this.hardwareMonitor = hardwareMonitor;
        this.translator = translator;
        this.history = new ArrayList<>();

        setupUI();
        startAnimation();
    }

    private void setupUI() {
        setPadding(new Insets(10));
        setSpacing(10);

        // Controls
        HBox controls = new HBox(10);

        typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("CPU", "GPU", "RAM", "Network", "Disk");
        typeCombo.setValue("CPU");
        typeCombo.setOnAction(e -> changeGraphType());

        rangeCombo = new ComboBox<>();
        rangeCombo.getItems().addAll("30s", "1m", "5m", "15m", "30m", "1h");
        rangeCombo.setValue("1m");
        rangeCombo.setOnAction(e -> changeTimeRange());

        Button refreshBtn = new Button(translator.tr("refresh"));
        refreshBtn.setOnAction(e -> refreshGraph());

        Button exportBtn = new Button(translator.tr("export"));
        exportBtn.setOnAction(e -> exportGraph());

        controls.getChildren().addAll(new Label(translator.tr("performance") + ":"), typeCombo,
                new Label(translator.tr("refresh_rate") + ":"), rangeCombo,
                refreshBtn, exportBtn);

        // Chart
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel(translator.tr("time"));
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel(translator.tr("percentage"));

        chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle(translator.tr("performance"));
        chart.setCreateSymbols(false);
        chart.setAnimated(false);

        dataSeries = new XYChart.Series<>();
        chart.getData().add(dataSeries);

        getChildren().addAll(controls, chart);
    }

    private void changeGraphType() {
        currentType = typeCombo.getValue().toLowerCase();
        refreshGraph();
    }

    private void changeTimeRange() {
        String value = rangeCombo.getValue();
        if (value.endsWith("s")) {
            timeRange = Integer.parseInt(value.substring(0, value.length() - 1));
        } else if (value.endsWith("m")) {
            timeRange = Integer.parseInt(value.substring(0, value.length() - 1)) * 60;
        } else if (value.endsWith("h")) {
            timeRange = Integer.parseInt(value.substring(0, value.length() - 1)) * 3600;
        }
        refreshGraph();
    }

    private void refreshGraph() {
        updateGraph();
    }

    private void exportGraph() {
        // In a real implementation, you'd export the chart as an image
        showInfo(translator.tr("export"), "Graph export would be implemented here");
    }

    private void startAnimation() {
        timer = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (now - lastUpdate > 1_000_000_000) { // Update every second
                    updateGraph();
                    lastUpdate = now;
                }
            }
        };
        timer.start();
    }

    private void updateGraph() {
        List<HardwareSnapshot> snapshots = hardwareMonitor.getHistory();
        if (snapshots == null || snapshots.isEmpty()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        List<HardwareSnapshot> recent = new ArrayList<>();

        for (HardwareSnapshot snapshot : snapshots) {
            if (ChronoUnit.SECONDS.between(snapshot.getTimestamp(), now) <= timeRange) {
                recent.add(snapshot);
            }
        }

        if (recent.isEmpty()) {
            return;
        }

        dataSeries.getData().clear();

        double startTime = recent.get(0).getTimestamp().toEpochSecond(java.time.ZoneOffset.UTC);

        for (int i = 0; i < recent.size(); i++) {
            HardwareSnapshot snapshot = recent.get(i);
            double time = snapshot.getTimestamp().toEpochSecond(java.time.ZoneOffset.UTC) - startTime;
            double value = getValue(snapshot);

            dataSeries.getData().add(new XYChart.Data<>(time, value));
        }

        // Update axis labels
        NumberAxis yAxis = (NumberAxis) chart.getYAxis();
        switch (currentType) {
            case "network":
            case "disk":
                yAxis.setLabel("MB/s");
                break;
            default:
                yAxis.setLabel("%");
        }

        chart.setTitle(translator.tr("performance") + " - " + currentType.toUpperCase());
    }

    private double getValue(HardwareSnapshot snapshot) {
        switch (currentType) {
            case "cpu":
                return snapshot.getCpuUsage();
            case "gpu":
                if (snapshot.getGpuUsage() != null && !snapshot.getGpuUsage().isEmpty()) {
                    // Average of all GPUs
                    return snapshot.getGpuUsage().stream().mapToDouble(Double::doubleValue).average().orElse(0);
                }
                return 0;
            case "ram":
                return snapshot.getRamUsage();
            case "network":
                if (snapshot.getNetworkIo() != null && snapshot.getNetworkIo().length > 0) {
                    // Network speed would need delta calculation
                    return snapshot.getNetworkIo()[0] / (1024.0 * 1024); // Convert to MB
                }
                return 0;
            case "disk":
                if (snapshot.getDiskIo() != null && !snapshot.getDiskIo().isEmpty()) {
                    // Average read+write speed
                    long[] first = snapshot.getDiskIo().values().iterator().next();
                    return (first[0] + first[1]) / (1024.0 * 1024);
                }
                return 0;
            default:
                return 0;
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static class HBox extends javafx.scene.layout.HBox {
        public HBox(double spacing) {
            super(spacing);
        }
    }
}