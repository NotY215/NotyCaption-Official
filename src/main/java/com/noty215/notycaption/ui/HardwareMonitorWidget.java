package com.noty215.notycaption.ui;

import com.noty215.notycaption.hardware.HardwareMonitor;
import com.noty215.notycaption.utils.Translator;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;

/**
 * Widget for displaying hardware monitoring information
 */
public class HardwareMonitorWidget extends VBox {

    private final HardwareMonitor hardwareMonitor;
    private final Translator translator;

    private TextArea cpuInfo;
    private TextArea gpuInfo;
    private TextArea ramInfo;
    private TextArea diskInfo;
    private TextArea networkInfo;

    public HardwareMonitorWidget(HardwareMonitor hardwareMonitor, Translator translator) {
        this.hardwareMonitor = hardwareMonitor;
        this.translator = translator;

        setupUI();
        startUpdates();
    }

    private void setupUI() {
        setPadding(new Insets(10));
        setSpacing(10);

        TabPane tabPane = new TabPane();

        // CPU tab
        Tab cpuTab = new Tab(translator.tr("cpu_tab"));
        cpuInfo = new TextArea();
        cpuInfo.setEditable(false);
        cpuInfo.setWrapText(true);
        cpuTab.setContent(cpuInfo);

        // GPU tab
        Tab gpuTab = new Tab(translator.tr("gpu_tab"));
        gpuInfo = new TextArea();
        gpuInfo.setEditable(false);
        gpuInfo.setWrapText(true);
        gpuTab.setContent(gpuInfo);

        // RAM tab
        Tab ramTab = new Tab(translator.tr("ram_tab"));
        ramInfo = new TextArea();
        ramInfo.setEditable(false);
        ramInfo.setWrapText(true);
        ramTab.setContent(ramInfo);

        // Disk tab
        Tab diskTab = new Tab(translator.tr("disk_tab"));
        diskInfo = new TextArea();
        diskInfo.setEditable(false);
        diskInfo.setWrapText(true);
        diskTab.setContent(diskInfo);

        // Network tab
        Tab networkTab = new Tab(translator.tr("network_tab"));
        networkInfo = new TextArea();
        networkInfo.setEditable(false);
        networkInfo.setWrapText(true);
        networkTab.setContent(networkInfo);

        tabPane.getTabs().addAll(cpuTab, gpuTab, ramTab, diskTab, networkTab);

        getChildren().add(tabPane);
    }

    private void startUpdates() {
        // Update every 2 seconds
        Thread updateThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2000);
                    Platform.runLater(this::updateDisplay);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        updateThread.setDaemon(true);
        updateThread.start();
    }

    private void updateDisplay() {
        cpuInfo.setText(hardwareMonitor.getCPUInfo().toString());
        gpuInfo.setText(hardwareMonitor.getGPUInfo().toString());
        ramInfo.setText(hardwareMonitor.getRAMInfo().toString());

        StringBuilder diskText = new StringBuilder();
        for (var disk : hardwareMonitor.getDiskInfo()) {
            diskText.append(disk.toString()).append("\n\n");
        }
        diskInfo.setText(diskText.toString());

        StringBuilder networkText = new StringBuilder();
        for (var net : hardwareMonitor.getNetworkInfo()) {
            networkText.append(net.toString()).append("\n\n");
        }
        networkInfo.setText(networkText.toString());
    }
}