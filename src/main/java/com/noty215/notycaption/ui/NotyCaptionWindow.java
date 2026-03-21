package com.noty215.notycaption.ui;

import com.noty215.notycaption.App;
import com.noty215.notycaption.models.*;
import com.noty215.notycaption.utils.*;
import com.noty215.notycaption.hardware.HardwareMonitor;
import com.noty215.notycaption.hardware.MonitorManager;
import com.noty215.notycaption.network.OnlineHandler;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NotyCaptionWindow extends JFrame {
    private static final java.util.logging.Logger logger = 
        java.util.logging.Logger.getLogger(NotyCaptionWindow.class.getName());
    
    // UI Components
    private JPanel mainPanel;
    private JTextArea captionTextArea;
    private JComboBox<String> modeCombo;
    private JComboBox<String> langCombo;
    private JSpinner wordsSpin;
    private JComboBox<String> formatCombo;
    private JTextField outFolderField;
    private JLabel statusValue;
    private JButton importBtn;
    private JButton enhanceBtn;
    private JButton generateBtn;
    private JButton editBtn;
    private JButton settingsBtn;
    private JButton downloadBtn;
    private JButton loginBtn;
    private JButton workspaceBtn;
    private JProgressBar mainProgressBar;
    private JPanel overlay;
    private JPanel downloadOverlay;
    
    // State
    private Map<String, Object> settings;
    private String inputFile;
    private String audioFile;
    private String outputFolder;
    private List<Map<String, Object>> subtitles;
    private List<String> displayLines;
    private boolean generated;
    private boolean editActive;
    private boolean isGenerating;
    private String mode;
    private String lastTempWav;
    
    // Managers
    private HardwareMonitor hardwareMonitor;
    private MonitorManager monitorManager;
    private OnlineHandler onlineHandler;
    private SessionManager sessionManager;
    private LayoutManager layoutManager;
    private PresetManager presetManager;
    private Translator translator;
    
    public NotyCaptionWindow() {
        initialize();
    }
    
    private void initialize() {
        // Load settings
        settings = EncryptionUtils.loadSettings();
        translator = Translator.getInstance();
        translator.setLanguage((String) settings.getOrDefault("language", "en"));
        
        // Set up window
        setTitle(translator.tr("window_title"));
        setMinimumSize(new Dimension(1024, 768));
        
        int width = (int) settings.getOrDefault("window_width", 1280);
        int height = (int) settings.getOrDefault("window_height", 800);
        setSize(width, height);
        
        if ((boolean) settings.getOrDefault("window_maximized", false)) {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
        
        // Center window
        setLocationRelativeTo(null);
        
        // Set icon
        ImageIcon icon = ResourcePath.getImageIcon("App.ico");
        if (icon != null) {
            setIconImage(icon.getImage());
        }
        
        // Initialize managers
        hardwareMonitor = HardwareMonitor.getInstance();
        monitorManager = new MonitorManager();
        onlineHandler = new OnlineHandler(this);
        sessionManager = new SessionManager();
        layoutManager = new LayoutManager();
        presetManager = new PresetManager();
        
        // Initialize state
        initializeState();
        
        // Create UI
        createMenuBar();
        createToolBar();
        createMainPanel();
        
        // Start hardware monitoring
        if ((boolean) settings.getOrDefault("hardware_monitoring", true)) {
            hardwareMonitor.startMonitoring(
                (int) settings.getOrDefault("monitoring_interval", 1000)
            );
        }
        
        // Load credentials
        loadExistingCredentials();
        
        logger.info("Main window fully initialized");
    }
    
    private void initializeState() {
        inputFile = null;
        audioFile = null;
        outputFolder = (String) settings.getOrDefault("last_output_folder", null);
        subtitles = new ArrayList<>();
        displayLines = new ArrayList<>();
        generated = false;
        editActive = false;
        isGenerating = false;
        mode = (String) settings.getOrDefault("last_mode", "normal");
        lastTempWav = null;
    }
    
    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        
        // File Menu
        JMenu fileMenu = new JMenu("File");
        JMenuItem importItem = new JMenuItem("📁 Import Media...");
        importItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        importItem.addActionListener(e -> importMediaFile());
        fileMenu.add(importItem);
        
        JMenuItem exportItem = new JMenuItem("📤 Export Subtitles...");
        exportItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.CTRL_DOWN_MASK));
        exportItem.addActionListener(e -> exportSubtitles());
        fileMenu.add(exportItem);
        
        fileMenu.addSeparator();
        
        JMenuItem saveSessionItem = new JMenuItem("💾 Save Session");
        saveSessionItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));
        saveSessionItem.addActionListener(e -> saveSession());
        fileMenu.add(saveSessionItem);
        
        JMenuItem loadSessionItem = new JMenuItem("📂 Load Session");
        loadSessionItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, InputEvent.CTRL_DOWN_MASK));
        loadSessionItem.addActionListener(e -> loadSession());
        fileMenu.add(loadSessionItem);
        
        fileMenu.addSeparator();
        
        JMenuItem exitItem = new JMenuItem("🚪 Exit");
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        exitItem.addActionListener(e -> closeWindow());
        fileMenu.add(exitItem);
        
        menuBar.add(fileMenu);
        
        // Edit Menu
        JMenu editMenu = new JMenu("Edit");
        JMenuItem undoItem = new JMenuItem("↩ Undo");
        undoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK));
        editMenu.add(undoItem);
        
        JMenuItem redoItem = new JMenuItem("↪ Redo");
        redoItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.CTRL_DOWN_MASK));
        editMenu.add(redoItem);
        
        editMenu.addSeparator();
        
        JMenuItem cutItem = new JMenuItem("✂️ Cut");
        cutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
        cutItem.addActionListener(e -> captionTextArea.cut());
        editMenu.add(cutItem);
        
        JMenuItem copyItem = new JMenuItem("📋 Copy");
        copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
        copyItem.addActionListener(e -> captionTextArea.copy());
        editMenu.add(copyItem);
        
        JMenuItem pasteItem = new JMenuItem("📌 Paste");
        pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
        pasteItem.addActionListener(e -> captionTextArea.paste());
        editMenu.add(pasteItem);
        
        editMenu.addSeparator();
        
        JMenuItem selectAllItem = new JMenuItem("🔍 Select All");
        selectAllItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK));
        selectAllItem.addActionListener(e -> captionTextArea.selectAll());
        editMenu.add(selectAllItem);
        
        menuBar.add(editMenu);
        
        // View Menu
        JMenu viewMenu = new JMenu("View");
        
        JMenuItem fullscreenItem = new JMenuItem("🖥️ Full Screen");
        fullscreenItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F11, 0));
        fullscreenItem.addActionListener(e -> toggleFullscreen());
        viewMenu.add(fullscreenItem);
        
        menuBar.add(viewMenu);
        
        // Tools Menu
        JMenu toolsMenu = new JMenu("🛠️ Tools");
        
        JMenuItem settingsItem = new JMenuItem("⚙️ Settings");
        settingsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_COMMA, InputEvent.CTRL_DOWN_MASK));
        settingsItem.addActionListener(e -> openSettingsDialog());
        toolsMenu.add(settingsItem);
        
        JMenuItem workspaceItem = new JMenuItem("🎨 Workspace Customizer");
        workspaceItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, InputEvent.CTRL_DOWN_MASK));
        workspaceItem.addActionListener(e -> openWorkspaceDialog());
        toolsMenu.add(workspaceItem);
        
        toolsMenu.addSeparator();
        
        JMenuItem downloadModelItem = new JMenuItem("📥 Download Model");
        downloadModelItem.addActionListener(e -> openModelDownloadDialog());
        toolsMenu.add(downloadModelItem);
        
        JMenuItem googleLoginItem = new JMenuItem("🔐 Google Login");
        googleLoginItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_DOWN_MASK));
        googleLoginItem.addActionListener(e -> initiateGoogleLogin());
        toolsMenu.add(googleLoginItem);
        
        menuBar.add(toolsMenu);
        
        // Help Menu
        JMenu helpMenu = new JMenu("❓ Help");
        
        JMenuItem docsItem = new JMenuItem("📚 Documentation");
        docsItem.addActionListener(e -> openDocumentation());
        helpMenu.add(docsItem);
        
        JMenuItem aboutItem = new JMenuItem("ℹ️ About");
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);
        
        menuBar.add(helpMenu);
        
        setJMenuBar(menuBar);
    }
    
    private void createToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        
        JButton importToolBtn = new JButton("📁 Import");
        importToolBtn.addActionListener(e -> importMediaFile());
        toolBar.add(importToolBtn);
        
        JButton generateToolBtn = new JButton("🚀 Generate");
        generateToolBtn.addActionListener(e -> startCaptionGeneration());
        toolBar.add(generateToolBtn);
        
        JButton enhanceToolBtn = new JButton("🎤 Enhance");
        enhanceToolBtn.addActionListener(e -> enhanceAudioVocals());
        toolBar.add(enhanceToolBtn);
        
        toolBar.addSeparator();
        
        JButton settingsToolBtn = new JButton("⚙️");
        settingsToolBtn.addActionListener(e -> openSettingsDialog());
        toolBar.add(settingsToolBtn);
        
        add(toolBar, BorderLayout.NORTH);
    }
    
    private void createMainPanel() {
        mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Split pane
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.3);
        
        // Left panel
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        
        // Title
        JLabel titleLabel = new JLabel(translator.tr("app_name"), SwingConstants.CENTER);
        titleLabel.setFont(new Font((String) settings.getOrDefault("font_family", "Segoe UI"), Font.BOLD, 36));
        titleLabel.setForeground(Color.WHITE);
        leftPanel.add(titleLabel, BorderLayout.NORTH);
        
        // Caption area
        captionTextArea = new JTextArea();
        captionTextArea.setEditable(false);
        captionTextArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        captionTextArea.setLineWrap(true);
        captionTextArea.setWrapStyleWord(true);
        JScrollPane scrollPane = new JScrollPane(captionTextArea);
        leftPanel.add(scrollPane, BorderLayout.CENTER);
        
        // Button row
        JPanel buttonRow = new JPanel(new GridLayout(1, 4, 5, 0));
        
        editBtn = new JButton(translator.tr("edit_captions"));
        editBtn.addActionListener(e -> toggleEditMode());
        editBtn.setEnabled(false);
        buttonRow.add(editBtn);
        
        workspaceBtn = new JButton(translator.tr("workspace"));
        workspaceBtn.addActionListener(e -> openWorkspaceDialog());
        buttonRow.add(workspaceBtn);
        
        settingsBtn = new JButton(translator.tr("settings"));
        settingsBtn.addActionListener(e -> openSettingsDialog());
        buttonRow.add(settingsBtn);
        
        downloadBtn = new JButton(translator.tr("download_model"));
        downloadBtn.addActionListener(e -> openModelDownloadDialog());
        buttonRow.add(downloadBtn);
        
        leftPanel.add(buttonRow, BorderLayout.SOUTH);
        
        splitPane.setLeftComponent(leftPanel);
        
        // Right panel
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        
        loginBtn = new JButton(translator.tr("login_google"));
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        loginBtn.addActionListener(e -> initiateGoogleLogin());
        rightPanel.add(loginBtn);
        
        rightPanel.add(Box.createVerticalStrut(10));
        
        // Mode card
        JPanel modeCard = createGlassCard();
        modeCard.setLayout(new BoxLayout(modeCard, BoxLayout.Y_AXIS));
        JLabel modeLabel = new JLabel(translator.tr("processing_mode"));
        modeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        modeCard.add(modeLabel);
        
        modeCombo = new JComboBox<>(new String[]{
            translator.tr("normal_mode"),
            translator.tr("online_mode")
        });
        modeCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        modeCombo.addActionListener(e -> onModeChange());
        modeCard.add(modeCombo);
        rightPanel.add(modeCard);
        
        rightPanel.add(Box.createVerticalStrut(10));
        
        // Language card
        JPanel langCard = createGlassCard();
        langCard.setLayout(new BoxLayout(langCard, BoxLayout.Y_AXIS));
        JLabel langLabel = new JLabel(translator.tr("language"));
        langLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        langCard.add(langLabel);
        
        langCombo = new JComboBox<>(new String[]{
            translator.tr("english_transcribe"),
            translator.tr("japanese_translate"),
            translator.tr("chinese_transcribe"),
            translator.tr("french_transcribe"),
            translator.tr("german_transcribe"),
            translator.tr("spanish_transcribe"),
            translator.tr("russian_transcribe"),
            translator.tr("arabic_transcribe"),
            translator.tr("hindi_transcribe"),
            translator.tr("urdu_transcribe")
        });
        langCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        langCombo.setSelectedItem(settings.getOrDefault("default_lang", translator.tr("english_transcribe")));
        langCard.add(langCombo);
        rightPanel.add(langCard);
        
        rightPanel.add(Box.createVerticalStrut(10));
        
        // Words per line card
        JPanel wplCard = createGlassCard();
        wplCard.setLayout(new BoxLayout(wplCard, BoxLayout.Y_AXIS));
        JLabel wplLabel = new JLabel(translator.tr("words_per_line"));
        wplLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        wplCard.add(wplLabel);
        
        wordsSpin = new JSpinner(new SpinnerNumberModel(
            settings.getOrDefault("words_per_line", 5), 1, 20, 1
        ));
        wordsSpin.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        wplCard.add(wordsSpin);
        rightPanel.add(wplCard);
        
        rightPanel.add(Box.createVerticalStrut(10));
        
        importBtn = new JButton(translator.tr("import_media"));
        importBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        importBtn.addActionListener(e -> importMediaFile());
        rightPanel.add(importBtn);
        
        rightPanel.add(Box.createVerticalStrut(10));
        
        // Format card
        JPanel fmtCard = createGlassCard();
        fmtCard.setLayout(new BoxLayout(fmtCard, BoxLayout.Y_AXIS));
        JLabel fmtLabel = new JLabel(translator.tr("output_format"));
        fmtLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        fmtCard.add(fmtLabel);
        
        formatCombo = new JComboBox<>(new String[]{
            translator.tr("srt_format"),
            translator.tr("ass_format")
        });
        formatCombo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        formatCombo.setSelectedItem(settings.getOrDefault("output_format", translator.tr("srt_format")));
        fmtCard.add(formatCombo);
        rightPanel.add(fmtCard);
        
        rightPanel.add(Box.createVerticalStrut(10));
        
        // Output folder card
        JPanel folderCard = createGlassCard();
        folderCard.setLayout(new BoxLayout(folderCard, BoxLayout.Y_AXIS));
        JLabel outLabel = new JLabel(translator.tr("output_folder"));
        outLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        folderCard.add(outLabel);
        
        outFolderField = new JTextField();
        outFolderField.setEditable(false);
        outFolderField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        if (outputFolder != null) {
            outFolderField.setText(outputFolder);
        }
        folderCard.add(outFolderField);
        
        JButton browseBtn = new JButton(translator.tr("browse_output"));
        browseBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        browseBtn.addActionListener(e -> browseOutputFolder());
        folderCard.add(browseBtn);
        rightPanel.add(folderCard);
        
        rightPanel.add(Box.createVerticalStrut(10));
        
        enhanceBtn = new JButton(translator.tr("enhance_audio"));
        enhanceBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        enhanceBtn.setEnabled(false);
        enhanceBtn.addActionListener(e -> enhanceAudioVocals());
        rightPanel.add(enhanceBtn);
        
        rightPanel.add(Box.createVerticalStrut(10));
        
        // Status card
        JPanel statusCard = createGlassCard();
        statusCard.setLayout(new BoxLayout(statusCard, BoxLayout.Y_AXIS));
        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusRow.setOpaque(false);
        JLabel statusLabel = new JLabel(translator.tr("status"));
        statusRow.add(statusLabel);
        
        statusValue = new JLabel(translator.tr("idle"));
        statusRow.add(statusValue);
        statusCard.add(statusRow);
        rightPanel.add(statusCard);
        
        rightPanel.add(Box.createVerticalGlue());
        
        JScrollPane rightScroll = new JScrollPane(rightPanel);
        rightScroll.setBorder(null);
        splitPane.setRightComponent(rightScroll);
        
        mainPanel.add(splitPane, BorderLayout.CENTER);
        
        // Progress bar
        mainProgressBar = new JProgressBar(0, 100);
        mainPanel.add(mainProgressBar, BorderLayout.SOUTH);
        
        add(mainPanel);
        
        // Create overlays
        createOverlays();
    }
    
    private JPanel createGlassCard() {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setComposite(AlphaComposite.SrcOver.derive(0.1f));
                g2d.setColor(Color.WHITE);
                g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 15, 15);
                g2d.dispose();
            }
        };
        card.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        card.setOpaque(false);
        return card;
    }
    
    private void createOverlays() {
        // Main overlay
        overlay = new JPanel(new GridBagLayout());
        overlay.setBackground(new Color(10, 10, 10, 230));
        overlay.setVisible(false);
        
        JPanel progressContainer = new JPanel();
        progressContainer.setLayout(new BoxLayout(progressContainer, BoxLayout.Y_AXIS));
        progressContainer.setBackground(new Color(26, 26, 26));
        progressContainer.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        
        JLabel progTitle = new JLabel(translator.tr("processing"));
        progTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        progTitle.setForeground(Color.WHITE);
        progTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressContainer.add(progTitle);
        
        progressContainer.add(Box.createVerticalStrut(20));
        
        JLabel progInfo = new JLabel(translator.tr("starting"));
        progInfo.setForeground(Color.LIGHT_GRAY);
        progInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressContainer.add(progInfo);
        
        progressContainer.add(Box.createVerticalStrut(10));
        
        JProgressBar opProgress = new JProgressBar(0, 100);
        opProgress.setPreferredSize(new Dimension(400, 30));
        opProgress.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressContainer.add(opProgress);
        
        progressContainer.add(Box.createVerticalStrut(10));
        
        JPanel speedPanel = new JPanel(new FlowLayout());
        speedPanel.setOpaque(false);
        JLabel speedLabel = new JLabel(translator.tr("speed") + " --");
        speedLabel.setForeground(Color.LIGHT_GRAY);
        speedPanel.add(speedLabel);
        
        JLabel etaLabel = new JLabel(translator.tr("eta") + " --");
        etaLabel.setForeground(Color.LIGHT_GRAY);
        speedPanel.add(etaLabel);
        progressContainer.add(speedPanel);
        
        progressContainer.add(Box.createVerticalStrut(20));
        
        JButton cancelBtn = new JButton(translator.tr("cancel"));
        cancelBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        cancelBtn.addActionListener(e -> cancelCurrentOperation(
            (boolean) settings.getOrDefault("confirm_cancel", true)
        ));
        progressContainer.add(cancelBtn);
        
        overlay.add(progressContainer);
        add(overlay);
        
        // Download overlay (similar structure)
        downloadOverlay = new JPanel(new GridBagLayout());
        downloadOverlay.setBackground(new Color(10, 10, 10, 230));
        downloadOverlay.setVisible(false);
        
        JPanel downloadContainer = new JPanel();
        downloadContainer.setLayout(new BoxLayout(downloadContainer, BoxLayout.Y_AXIS));
        downloadContainer.setBackground(new Color(26, 26, 26));
        downloadContainer.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        
        JLabel downloadTitle = new JLabel(translator.tr("downloading"));
        downloadTitle.setFont(new Font("Segoe UI", Font.BOLD, 24));
        downloadTitle.setForeground(Color.WHITE);
        downloadTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        downloadContainer.add(downloadTitle);
        
        downloadContainer.add(Box.createVerticalStrut(20));
        
        JLabel downloadInfo = new JLabel(translator.tr("starting"));
        downloadInfo.setForeground(Color.LIGHT_GRAY);
        downloadInfo.setAlignmentX(Component.CENTER_ALIGNMENT);
        downloadContainer.add(downloadInfo);
        
        downloadContainer.add(Box.createVerticalStrut(10));
        
        JProgressBar downloadProgress = new JProgressBar(0, 100);
        downloadProgress.setPreferredSize(new Dimension(400, 30));
        downloadProgress.setAlignmentX(Component.CENTER_ALIGNMENT);
        downloadContainer.add(downloadProgress);
        
        downloadContainer.add(Box.createVerticalStrut(10));
        
        JPanel downloadSpeedPanel = new JPanel(new FlowLayout());
        downloadSpeedPanel.setOpaque(false);
        JLabel downloadSpeedLabel = new JLabel(translator.tr("speed") + " --");
        downloadSpeedLabel.setForeground(Color.LIGHT_GRAY);
        downloadSpeedPanel.add(downloadSpeedLabel);
        
        JLabel downloadEtaLabel = new JLabel(translator.tr("eta") + " --");
        downloadEtaLabel.setForeground(Color.LIGHT_GRAY);
        downloadSpeedPanel.add(downloadEtaLabel);
        downloadContainer.add(downloadSpeedPanel);
        
        downloadContainer.add(Box.createVerticalStrut(20));
        
        JButton downloadCancelBtn = new JButton(translator.tr("cancel"));
        downloadCancelBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        downloadCancelBtn.addActionListener(e -> cancelCurrentOperation(
            (boolean) settings.getOrDefault("confirm_cancel", true)
        ));
        downloadContainer.add(downloadCancelBtn);
        
        downloadOverlay.add(downloadContainer);
        add(downloadOverlay);
    }
    
    private void importMediaFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(translator.tr("import_media"));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Media Files", "mp4", "mkv", "avi", "mov", "webm", "flv", "wmv", 
            "mp3", "wav", "m4a", "aac", "flac", "ogg", "wma", "amr", "opus"
        ));
        
        String lastFile = (String) settings.getOrDefault("last_input_file", "");
        if (!lastFile.isEmpty()) {
            fileChooser.setCurrentDirectory(new File(lastFile).getParentFile());
        }
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            inputFile = file.getAbsolutePath();
            outputFolder = file.getParent();
            outFolderField.setText(outputFolder);
            enhanceBtn.setEnabled(true);
            
            settings.put("last_input_file", inputFile);
            settings.put("last_output_folder", outputFolder);
            EncryptionUtils.saveSettings(settings);
            
            sessionManager.saveOperationState("import", Map.of(
                "file", inputFile,
                "timestamp", new Date().toString()
            ));
            
            // Extract audio (simplified - would use FFmpeg in real implementation)
            audioFile = inputFile; // Placeholder
            
            JOptionPane.showMessageDialog(this, 
                translator.tr("import_success"),
                translator.tr("import_complete"),
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void exportSubtitles() {
        if (!generated || subtitles.isEmpty()) {
            JOptionPane.showMessageDialog(this, 
                "No subtitles to export.", 
                "No Subtitles", 
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Subtitles");
        fileChooser.setSelectedFile(new File("subtitles_" + 
            new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".srt"));
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
            "Subtitle Files", "srt", "ass", "ssa", "vtt"
        ));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            String fmt = "." + file.getName().substring(file.getName().lastIndexOf('.') + 1);
            saveSubtitlesToFile(subtitles, fmt, file.getAbsolutePath());
            JOptionPane.showMessageDialog(this, 
                "Subtitles exported to:\n" + file.getAbsolutePath(),
                "Export Complete",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void saveSubtitlesToFile(List<Map<String, Object>> subtitles, String fmt, String path) {
        // Implementation would write to file based on format
        logger.info("Saving subtitles to: " + path);
    }
    
    private void saveSession() {
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("last_input_file", inputFile);
        sessionData.put("last_output_folder", outputFolder);
        sessionData.put("subtitles", subtitles);
        sessionData.put("display_lines", displayLines);
        sessionData.put("generated", generated);
        sessionData.put("mode", mode);
        sessionData.put("timestamp", new Date().toString());
        
        if (sessionManager.saveSession(sessionData)) {
            JOptionPane.showMessageDialog(this, 
                "Current session has been saved.",
                "Session Saved",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    private void loadSession() {
        Map<String, Object> sessionData = sessionManager.loadSession();
        if (sessionData != null) {
            String lastFile = (String) sessionData.get("last_input_file");
            if (lastFile != null && new File(lastFile).exists()) {
                inputFile = lastFile;
                outputFolder = (String) sessionData.get("last_output_folder");
                subtitles = (List<Map<String, Object>>) sessionData.get("subtitles");
                displayLines = (List<String>) sessionData.get("display_lines");
                generated = (boolean) sessionData.getOrDefault("generated", false);
                
                if (generated) {
                    StringBuilder preview = new StringBuilder();
                    for (String line : displayLines) {
                        if (preview.length() > 0) preview.append("\n\n");
                        preview.append(line);
                    }
                    captionTextArea.setText(preview.toString());
                    editBtn.setEnabled(true);
                }
                
                JOptionPane.showMessageDialog(this, 
                    "Session loaded successfully.",
                    "Session Loaded",
                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, 
                    "Session file not found.",
                    "Session Error",
                    JOptionPane.WARNING_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(this, 
                "No saved session found.",
                "Session Error",
                JOptionPane.WARNING_MESSAGE);
        }
    }
    
    private void openSettingsDialog() {
        SettingsDialog dialog = new SettingsDialog(this, settings);
        dialog.setVisible(true);
    }
    
    private void openWorkspaceDialog() {
        WorkspaceCustomizeDialog dialog = new WorkspaceCustomizeDialog(this, settings);
        dialog.setVisible(true);
    }
    
    private void openModelDownloadDialog() {
        // Implementation
        logger.info("Opening model download dialog");
    }
    
    private void initiateGoogleLogin() {
        onlineHandler.initiateLogin();
    }
    
    private void loadExistingCredentials() {
        onlineHandler.loadExistingCredentials();
        if (onlineHandler.isLoggedIn()) {
            loginBtn.setVisible(false);
        }
    }
    
    private void onModeChange() {
        mode = modeCombo.getSelectedIndex() == 0 ? "normal" : "online";
        settings.put("last_mode", mode);
        EncryptionUtils.saveSettings(settings);
        logger.info("Mode switched to: " + mode);
    }
    
    private void startCaptionGeneration() {
        if (isGenerating) {
            JOptionPane.showMessageDialog(this, 
                "Generation already in progress.",
                "In Progress",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        if (audioFile == null || !new File(audioFile).exists()) {
            JOptionPane.showMessageDialog(this, 
                "Import media first.",
                "No Media",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        isGenerating = true;
        freezeUI(true, translator.tr("generating"));
        
        logger.info("=== Secure Caption Generation Started ===");
        
        // Proceed with transcription
        proceedToTranscription(audioFile);
    }
    
    private void proceedToTranscription(String audioToUse) {
        String langText = (String) langCombo.getSelectedItem();
        Map<String, String> langMap = new HashMap<>();
        langMap.put(translator.tr("english_transcribe"), "en");
        langMap.put(translator.tr("japanese_translate"), "ja");
        langMap.put(translator.tr("chinese_transcribe"), "zh");
        langMap.put(translator.tr("french_transcribe"), "fr");
        langMap.put(translator.tr("german_transcribe"), "de");
        langMap.put(translator.tr("spanish_transcribe"), "es");
        langMap.put(translator.tr("russian_transcribe"), "ru");
        langMap.put(translator.tr("arabic_transcribe"), "ar");
        langMap.put(translator.tr("hindi_transcribe"), "hi");
        langMap.put(translator.tr("urdu_transcribe"), "ur");
        
        String langCode = langMap.getOrDefault(langText, "en");
        String task = langText.contains("Translate") ? "translate" : "transcribe";
        
        int wpl = (int) wordsSpin.getValue();
        String fmt = formatCombo.getSelectedIndex() == 0 ? ".srt" : ".ass";
        
        String base = inputFile != null ? 
            new File(inputFile).getName().replaceFirst("[.][^.]+$", "") : "audio";
        String outPath = (outputFolder != null ? outputFolder : App.APP_DATA_DIR) + 
                        "/" + base + "_captions" + fmt;
        
        File outFile = new File(outPath);
        if (outFile.exists()) {
            int result = JOptionPane.showConfirmDialog(this,
                "File exists:\n" + outPath + "\nOverwrite?",
                "Overwrite File?",
                JOptionPane.YES_NO_OPTION);
            if (result != JOptionPane.YES_OPTION) {
                isGenerating = false;
                freezeUI(false);
                return;
            }
        }
        
        logger.info(String.format("Transcription params: lang=%s, task=%s, wpl=%d, fmt=%s", 
                                  langCode, task, wpl, fmt));
        
        if (mode.equals("online")) {
            boolean success = onlineHandler.handleOnline(
                audioToUse, langCode, task, wpl, fmt, base, outPath);
            if (!success) {
                isGenerating = false;
                freezeUI(false);
            }
        } else {
            performLocalTranscription(audioToUse, langCode, task, wpl, fmt, outPath);
        }
    }
    
    private void performLocalTranscription(String audioPath, String langCode, 
                                           String task, int wpl, String fmt, String outPath) {
        // Implementation would call Whisper model
        logger.info("Starting local transcription...");
        
        // Placeholder for actual transcription
        subtitles = new ArrayList<>();
        displayLines = new ArrayList<>();
        
        for (int i = 0; i < 10; i++) {
            Map<String, Object> sub = new HashMap<>();
            sub.put("index", i + 1);
            sub.put("start", i * 5.0);
            sub.put("end", i * 5.0 + 4.0);
            sub.put("text", "Sample subtitle line " + (i + 1));
            subtitles.add(sub);
            displayLines.add("Sample subtitle line " + (i + 1));
        }
        
        StringBuilder preview = new StringBuilder();
        for (String line : displayLines) {
            if (preview.length() > 0) preview.append("\n\n");
            preview.append(line);
        }
        captionTextArea.setText(preview.toString());
        generated = true;
        editBtn.setEnabled(true);
        
        saveSubtitlesToFile(subtitles, fmt, outPath);
        
        isGenerating = false;
        freezeUI(false);
        
        JOptionPane.showMessageDialog(this,
            "Captions saved:\n" + outPath,
            "Generation Complete",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void enhanceAudioVocals() {
        if (audioFile == null || !new File(audioFile).exists()) {
            JOptionPane.showMessageDialog(this, 
                "No audio file loaded.",
                "No Audio",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        logger.info("Starting vocal enhancement...");
        freezeUI(true, "Enhancing...");
        
        // Implementation would call Spleeter
        // Placeholder for enhancement
        
        String base = inputFile != null ? 
            new File(inputFile).getName().replaceFirst("[.][^.]+$", "") : "audio";
        String finalPath = (outputFolder != null ? outputFolder : App.APP_DATA_DIR) +
                          "/" + base + "_enhanced_vocals.wav";
        
        audioFile = finalPath;
        lastTempWav = finalPath;
        
        freezeUI(false);
        
        JOptionPane.showMessageDialog(this,
            "Vocals extracted:\n" + finalPath,
            "Enhancement Complete",
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void toggleEditMode() {
        if (!generated) return;
        
        editActive = !editActive;
        captionTextArea.setEditable(editActive);
        editBtn.setText(editActive ? translator.tr("save_exit_edit") : translator.tr("edit_captions"));
        
        if (!editActive) {
            applyEditedCaptions();
        } else {
            captionTextArea.requestFocus();
        }
        
        logger.info("Edit mode: " + (editActive ? "enabled" : "disabled"));
    }
    
    private void applyEditedCaptions() {
        String textContent = captionTextArea.getText().trim();
        String[] editedLines = textContent.split("\n\n");
        
        if (editedLines.length != subtitles.size()) {
            JOptionPane.showMessageDialog(this,
                "Line count changed. Changes discarded.",
                "Mismatch",
                JOptionPane.WARNING_MESSAGE);
            refreshCaptionPreview();
            return;
        }
        
        for (int i = 0; i < editedLines.length; i++) {
            subtitles.get(i).put("text", editedLines[i].trim());
            displayLines.set(i, editedLines[i].trim());
        }
        
        refreshCaptionPreview();
        logger.info("Edits applied to subtitles");
    }
    
    private void refreshCaptionPreview() {
        StringBuilder preview = new StringBuilder();
        for (String line : displayLines) {
            if (preview.length() > 0) preview.append("\n\n");
            preview.append(line);
        }
        captionTextArea.setText(preview.toString());
    }
    
    private void cancelCurrentOperation(boolean withConfirmation) {
        if (withConfirmation) {
            int result = JOptionPane.showConfirmDialog(this,
                translator.tr("cancel_confirm_msg"),
                translator.tr("cancel_confirm"),
                JOptionPane.YES_NO_OPTION);
            if (result != JOptionPane.YES_OPTION) return;
        }
        
        logger.info("Cancel button pressed - stopping current operation");
        
        // Cancel various operations
        if (onlineHandler != null) {
            onlineHandler.cancelOperation();
        }
        
        isGenerating = false;
        freezeUI(false);
    }
    
    private void freezeUI(boolean freeze, String message) {
        // Enable/disable UI components
        importBtn.setEnabled(!freeze);
        enhanceBtn.setEnabled(!freeze && audioFile != null);
        generateBtn.setEnabled(!freeze);
        editBtn.setEnabled(!freeze && generated);
        downloadBtn.setEnabled(!freeze);
        loginBtn.setEnabled(!freeze);
        modeCombo.setEnabled(!freeze);
        langCombo.setEnabled(!freeze);
        wordsSpin.setEnabled(!freeze);
        formatCombo.setEnabled(!freeze);
        
        if (freeze) {
            overlay.setVisible(true);
            statusValue.setText(message);
        } else {
            overlay.setVisible(false);
            statusValue.setText(translator.tr("idle"));
        }
    }
    
    private void browseOutputFolder() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle(translator.tr("browse_output"));
        
        if (outputFolder != null) {
            chooser.setCurrentDirectory(new File(outputFolder));
        }
        
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputFolder = chooser.getSelectedFile().getAbsolutePath();
            outFolderField.setText(outputFolder);
            settings.put("last_output_folder", outputFolder);
            EncryptionUtils.saveSettings(settings);
            logger.info("Output folder set to: " + outputFolder);
        }
    }
    
    private void toggleFullscreen() {
        dispose();
        if ((getExtendedState() & JFrame.MAXIMIZED_BOTH) != 0) {
            setExtendedState(JFrame.NORMAL);
        } else {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
        setVisible(true);
    }
    
    private void openDocumentation() {
        try {
            Desktop.getDesktop().browse(new java.net.URI("https://github.com/NotY215/NotyCaption"));
        } catch (Exception e) {
            logger.warning("Failed to open documentation: " + e.getMessage());
        }
    }
    
    private void showAboutDialog() {
        String aboutText = String.format(
            "<html><h1>%s</h1><h3>Version %s</h3>" +
            "<p>Professional AI-Powered Caption Generator</p>" +
            "<p>Build: %s</p>" +
            "<p>%s</p>" +
            "<hr><p><b>Author:</b> NotY215</p>" +
            "<p><b>Website:</b> <a href='https://github.com/NotY215'>GitHub</a></p></html>",
            App.APP_NAME, App.VERSION, App.BUILD, App.COPYRIGHT
        );
        
        JOptionPane.showMessageDialog(this, 
            aboutText, 
            "About NotyCaption Pro", 
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void closeWindow() {
        if (isGenerating) {
            int result = JOptionPane.showConfirmDialog(this,
                "An operation is currently in progress.\n\nAre you sure you want to exit?",
                "Operation in Progress",
                JOptionPane.YES_NO_OPTION);
            if (result != JOptionPane.YES_OPTION) return;
        }
        
        // Save settings
        if (getExtendedState() != JFrame.MAXIMIZED_BOTH) {
            settings.put("window_width", getWidth());
            settings.put("window_height", getHeight());
        }
        settings.put("window_maximized", (getExtendedState() & JFrame.MAXIMIZED_BOTH) != 0);
        EncryptionUtils.saveSettings(settings);
        
        // Stop monitoring
        if (hardwareMonitor != null) {
            hardwareMonitor.stopMonitoring();
        }
        
        logger.info("=".repeat(80));
        logger.info("NotyCaption Pro Secure Shutdown");
        logger.info("=".repeat(80));
        
        dispose();
    }
    
    @Override
    protected void processWindowEvent(WindowEvent e) {
        if (e.getID() == WindowEvent.WINDOW_CLOSING) {
            closeWindow();
        } else {
            super.processWindowEvent(e);
        }
    }
}