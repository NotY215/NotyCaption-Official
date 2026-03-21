package com.noty215.notycaption.ui;

import com.noty215.notycaption.hardware.HardwareMonitor;
import com.noty215.notycaption.utils.Translator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class HardwareMonitorWidget extends JPanel {
    private HardwareMonitor hardwareMonitor;
    private Translator translator;
    private JTextArea cpuInfoArea;
    private JTextArea gpuInfoArea;
    private JTextArea ramInfoArea;
    private JTextArea diskInfoArea;
    private JTextArea networkInfoArea;
    private Timer refreshTimer;
    
    public HardwareMonitorWidget() {
        this.hardwareMonitor = HardwareMonitor.getInstance();
        this.translator = Translator.getInstance();
        
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(5, 5, 5, 5));
        
        initUI();
        startMonitoring();
    }
    
    private void initUI() {
        JTabbedPane tabbedPane = new JTabbedPane();
        
        // CPU Tab
        cpuInfoArea = createTextArea();
        tabbedPane.addTab(translator.tr("cpu_tab"), new JScrollPane(cpuInfoArea));
        
        // GPU Tab
        gpuInfoArea = createTextArea();
        tabbedPane.addTab(translator.tr("gpu_tab"), new JScrollPane(gpuInfoArea));
        
        // RAM Tab
        ramInfoArea = createTextArea();
        tabbedPane.addTab(translator.tr("ram_tab"), new JScrollPane(ramInfoArea));
        
        // Disk Tab
        diskInfoArea = createTextArea();
        tabbedPane.addTab(translator.tr("disk_tab"), new JScrollPane(diskInfoArea));
        
        // Network Tab
        networkInfoArea = createTextArea();
        tabbedPane.addTab(translator.tr("network_tab"), new JScrollPane(networkInfoArea));
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // Control panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refreshBtn = new JButton(translator.tr("refresh"));
        refreshBtn.addActionListener(e -> updateDisplay());
        controlPanel.add(refreshBtn);
        
        JButton exportBtn = new JButton(translator.tr("export_data"));
        exportBtn.addActionListener(e -> exportData());
        controlPanel.add(exportBtn);
        
        add(controlPanel, BorderLayout.SOUTH);
    }
    
    private JTextArea createTextArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 11));
        area.setBackground(new Color(30, 30, 40));
        area.setForeground(Color.WHITE);
        return area;
    }
    
    private void startMonitoring() {
        refreshTimer = new Timer(2000, e -> updateDisplay());
        refreshTimer.start();
        updateDisplay();
    }
    
    private void updateDisplay() {
        SwingUtilities.invokeLater(() -> {
            cpuInfoArea.setText(hardwareMonitor.getCpuSummary());
            gpuInfoArea.setText(hardwareMonitor.getGpuSummary());
            ramInfoArea.setText(hardwareMonitor.getRamSummary());
            
            StringBuilder diskText = new StringBuilder();
            for (var disk : hardwareMonitor.getDisks()) {
                diskText.append(String.format("Disk: %s\n", disk.getDevice()));
                diskText.append(String.format("  Mount: %s\n", disk.getMountpoint()));
                diskText.append(String.format("  Total: %.1f GB\n", disk.getTotal() / (1024.0 * 1024 * 1024)));
                diskText.append(String.format("  Used: %.1f GB (%.1f%%)\n", 
                    disk.getUsed() / (1024.0 * 1024 * 1024), disk.getUtilization()));
                diskText.append(String.format("  Free: %.1f GB\n", disk.getFree() / (1024.0 * 1024 * 1024)));
                diskText.append(String.format("  Filesystem: %s\n", disk.getFilesystem()));
                if (disk.getModel() != null) {
                    diskText.append(String.format("  Model: %s\n", disk.getModel()));
                }
                diskText.append("\n");
            }
            diskInfoArea.setText(diskText.toString());
            
            StringBuilder networkText = new StringBuilder();
            for (var net : hardwareMonitor.getNetworks()) {
                networkText.append(String.format("Interface: %s\n", net.getInterface()));
                networkText.append(String.format("  MAC: %s\n", net.getMacAddress()));
                networkText.append(String.format("  IP: %s\n", String.join(", ", net.getIpAddresses())));
                networkText.append(String.format("  Speed: %d Mbps\n", net.getSpeed()));
                networkText.append(String.format("  Status: %s\n", net.isLinkStatus() ? "Up" : "Down"));
                networkText.append("\n");
            }
            networkInfoArea.setText(networkText.toString());
        });
    }
    
    private void exportData() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("hardware_info.txt"));
        if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try (java.io.FileWriter writer = new java.io.FileWriter(chooser.getSelectedFile())) {
                writer.write("=== Hardware Monitor Report ===\n\n");
                writer.write("=== CPU ===\n");
                writer.write(hardwareMonitor.getCpuSummary());
                writer.write("\n\n=== GPU ===\n");
                writer.write(hardwareMonitor.getGpuSummary());
                writer.write("\n\n=== RAM ===\n");
                writer.write(hardwareMonitor.getRamSummary());
                writer.write("\n\n=== Disks ===\n");
                writer.write(diskInfoArea.getText());
                writer.write("\n=== Network ===\n");
                writer.write(networkInfoArea.getText());
                
                JOptionPane.showMessageDialog(this, 
                    "Data exported successfully!", 
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