package com.noty215.notycaption.ui;

import com.noty215.notycaption.Main;
import com.noty215.notycaption.hardware.HardwareMonitor;
import com.noty215.notycaption.hardware.MonitorManager;
import com.noty215.notycaption.models.*;
import com.noty215.notycaption.subtitle.SubtitleEntry;
import com.noty215.notycaption.subtitle.SubtitleManager;
import com.noty215.notycaption.utils.*;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Main window for NotyCaption Pro
 */
public class NotyCaptionWindow {

    private static final Logger logger = LoggerFactory.getLogger(NotyCaptionWindow.class);

    private Stage stage;
    private SettingsManager settings;
    private Translator translator;
    private SessionManager sessionManager;
    private LayoutManager layoutManager;
    private PresetManager presetManager;
    private HardwareMonitor hardwareMonitor;
    private MonitorManager monitorManager;
    private SubtitleManager subtitleManager;

    private BorderPane root;
    private TextArea captionEdit;
    private ComboBox<String> modeCombo;
    private ComboBox<String> langCombo;
    private Spinner<Integer> wordsSpinner;
    private ComboBox<String> formatCombo;
    private TextField outputFolderField;
    private Label statusLabel;
    private Button generateButton;
    private Button enhanceButton;
    private Button importButton;
    private Button editButton;
    private Button settingsButton;
    private Button workspaceButton;
    private Button downloadButton;
    private Button loginButton;
    private ProgressBar progressBar;
    private Label progressLabel;
    private Label speedLabel;
    private Label etaLabel;
    private Hyperlink colabLink;
    private WebView previewWebView;

    private File currentAudioFile;
    private File currentMediaFile;
    private List<SubtitleEntry> subtitles;
    private List<String> displayLines;
    private boolean generated;
    private boolean editActive;
    private boolean isGenerating;
    private String currentNotebookUrl;
    private AtomicBoolean cancelRequested;
    private ScheduledExecutorService monitorExecutor;
    private javafx.scene.media.MediaPlayer mediaPlayer;
    private Timer timer;

    public NotyCaptionWindow() {
        this.settings = new SettingsManager();
        this.translator = new Translator(settings.getLanguage());
        this.sessionManager = new SessionManager();
        this.layoutManager = new LayoutManager();
        this.presetManager = new PresetManager();
        this.hardwareMonitor = new HardwareMonitor();
        this.monitorManager = new MonitorManager();
        this.subtitleManager = new SubtitleManager();
        this.subtitles = new ArrayList<>();
        this.displayLines = new ArrayList<>();
        this.generated = false;
        this.editActive = false;
        this.isGenerating = false;
        this.cancelRequested = new AtomicBoolean(false);
        this.monitorExecutor = Executors.newSingleThreadScheduledExecutor();

        initializeUI();
        loadSettings();
        setupEventHandlers();
        startHardwareMonitoring();

        logger.info("Main window initialized");
    }

    private void initializeUI() {
        stage = new Stage();
        stage.setTitle(translator.tr("window_title"));
        stage.setMinWidth(1024);
        stage.setMinHeight(768);

        // Load icon
        String iconPath = ResourcePath.getPath("App.ico");
        if (ResourcePath.exists("App.ico")) {
            stage.getIcons().add(new Image(new File(iconPath).toURI().toString()));
        }

        root = new BorderPane();
        root.setStyle("-fx-background-color: #0a0a0a;");

        // Create top menu bar
        MenuBar menuBar = createMenuBar();
        root.setTop(menuBar);

        // Create main split pane
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.setDividerPositions(0.3);

        // Left panel
        VBox leftPanel = createLeftPanel();
        splitPane.getItems().add(leftPanel);

        // Right panel with scroll
        ScrollPane rightScroll = new ScrollPane();
        rightScroll.setFitToWidth(true);
        rightScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        VBox rightPanel = createRightPanel();
        rightScroll.setContent(rightPanel);
        splitPane.getItems().add(rightScroll);

        root.setCenter(splitPane);

        // Create dock widgets
        createDockWidgets();

        Scene scene = new Scene(root);
        stage.setScene(scene);

        // Apply theme
        applyTheme();

        // Center window
        stage.centerOnScreen();
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        // File menu
        Menu fileMenu = new Menu("&File");

        MenuItem importItem = new MenuItem("📁 " + translator.tr("import_media"));
        importItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.O,
                javafx.scene.input.KeyCombination.CONTROL_DOWN));
        importItem.setOnAction(e -> importMedia());
        fileMenu.getItems().add(importItem);

        MenuItem exportItem = new MenuItem("📤 " + translator.tr("export"));
        exportItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.E,
                javafx.scene.input.KeyCombination.CONTROL_DOWN));
        exportItem.setOnAction(e -> exportSubtitles());
        fileMenu.getItems().add(exportItem);

        fileMenu.getItems().add(new SeparatorMenuItem());

        MenuItem saveSessionItem = new MenuItem("💾 " + translator.tr("save_layout"));
        saveSessionItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.S,
                javafx.scene.input.KeyCombination.CONTROL_DOWN));
        saveSessionItem.setOnAction(e -> saveSession());
        fileMenu.getItems().add(saveSessionItem);

        MenuItem loadSessionItem = new MenuItem("📂 " + translator.tr("load_layout"));
        loadSessionItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.L,
                javafx.scene.input.KeyCombination.CONTROL_DOWN));
        loadSessionItem.setOnAction(e -> loadSession());
        fileMenu.getItems().add(loadSessionItem);

        fileMenu.getItems().add(new SeparatorMenuItem());

        MenuItem exitItem = new MenuItem("🚪 " + translator.tr("cancel"));
        exitItem.setOnAction(e -> closeWindow());
        fileMenu.getItems().add(exitItem);

        // Edit menu
        Menu editMenu = new Menu("&Edit");

        MenuItem undoItem = new MenuItem("↩ " + translator.tr("cancel"));
        undoItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.Z,
                javafx.scene.input.KeyCombination.CONTROL_DOWN));
        editMenu.getItems().add(undoItem);

        MenuItem redoItem = new MenuItem("↪ " + translator.tr("redo"));
        redoItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.Y,
                javafx.scene.input.KeyCombination.CONTROL_DOWN));
        editMenu.getItems().add(redoItem);

        editMenu.getItems().add(new SeparatorMenuItem());

        MenuItem cutItem = new MenuItem("✂️ " + translator.tr("cut"));
        cutItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.X,
                javafx.scene.input.KeyCombination.CONTROL_DOWN));
        cutItem.setOnAction(e -> captionEdit.cut());
        editMenu.getItems().add(cutItem);

        MenuItem copyItem = new MenuItem("📋 " + translator.tr("copy"));
        copyItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.C,
                javafx.scene.input.KeyCombination.CONTROL_DOWN));
        copyItem.setOnAction(e -> captionEdit.copy());
        editMenu.getItems().add(copyItem);

        MenuItem pasteItem = new MenuItem("📌 " + translator.tr("paste"));
        pasteItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.V,
                javafx.scene.input.KeyCombination.CONTROL_DOWN));
        pasteItem.setOnAction(e -> captionEdit.paste());
        editMenu.getItems().add(pasteItem);

        editMenu.getItems().add(new SeparatorMenuItem());

        MenuItem selectAllItem = new MenuItem("🔍 " + translator.tr("select_all"));
        selectAllItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.A,
                javafx.scene.input.KeyCombination.CONTROL_DOWN));
        selectAllItem.setOnAction(e -> captionEdit.selectAll());
        editMenu.getItems().add(selectAllItem);

        // View menu
        Menu viewMenu = new Menu("&View");

        CheckMenuItem hardwareMonitorItem = new CheckMenuItem("🖥️ " + translator.tr("hardware_monitor"));
        hardwareMonitorItem.setSelected(true);
        viewMenu.getItems().add(hardwareMonitorItem);

        CheckMenuItem performanceGraphItem = new CheckMenuItem("📈 " + translator.tr("performance"));
        performanceGraphItem.setSelected(true);
        viewMenu.getItems().add(performanceGraphItem);

        CheckMenuItem previewWidgetItem = new CheckMenuItem("👁️ " + translator.tr("preview"));
        previewWidgetItem.setSelected(true);
        viewMenu.getItems().add(previewWidgetItem);

        viewMenu.getItems().add(new SeparatorMenuItem());

        MenuItem fullscreenItem = new MenuItem("🖥️ " + translator.tr("fullscreen"));
        fullscreenItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.F11));
        fullscreenItem.setOnAction(e -> stage.setFullScreen(!stage.isFullScreen()));
        viewMenu.getItems().add(fullscreenItem);

        // Tools menu
        Menu toolsMenu = new Menu("🛠️ &Tools");

        MenuItem settingsItem = new MenuItem("⚙️ " + translator.tr("settings"));
        settingsItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.COMMA,
                javafx.scene.input.KeyCombination.CONTROL_DOWN));
        settingsItem.setOnAction(e -> openSettings());
        toolsMenu.getItems().add(settingsItem);

        MenuItem workspaceItem = new MenuItem("🎨 " + translator.tr("workspace_customize"));
        workspaceItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.W,
                javafx.scene.input.KeyCombination.CONTROL_DOWN));
        workspaceItem.setOnAction(e -> openWorkspaceDialog());
        toolsMenu.getItems().add(workspaceItem);

        toolsMenu.getItems().add(new SeparatorMenuItem());

        MenuItem downloadModelItem = new MenuItem("📥 " + translator.tr("download_model"));
        downloadModelItem.setOnAction(e -> downloadModel());
        toolsMenu.getItems().add(downloadModelItem);

        MenuItem googleLoginItem = new MenuItem("🔐 " + translator.tr("login_google"));
        googleLoginItem.setAccelerator(new javafx.scene.input.KeyCodeCombination(javafx.scene.input.KeyCode.G,
                javafx.scene.input.KeyCombination.CONTROL_DOWN));
        googleLoginItem.setOnAction(e -> googleLogin());
        toolsMenu.getItems().add(googleLoginItem);

        // Help menu
        Menu helpMenu = new Menu("❓ &Help");

        MenuItem documentationItem = new MenuItem("📚 " + translator.tr("documentation"));
        documentationItem.setOnAction(e -> openDocumentation());
        helpMenu.getItems().add(documentationItem);

        MenuItem aboutItem = new MenuItem("ℹ️ " + translator.tr("about"));
        aboutItem.setOnAction(e -> showAbout());
        helpMenu.getItems().add(aboutItem);

        menuBar.getMenus().addAll(fileMenu, editMenu, viewMenu, toolsMenu, helpMenu);

        return menuBar;
    }

    private VBox createLeftPanel() {
        VBox leftPanel = new VBox(10);
        leftPanel.setPadding(new Insets(15));
        leftPanel.setStyle("-fx-background-color: #1a1a1a;");

        // App title
        Label titleLabel = new Label(translator.tr("app_name"));
        titleLabel.setStyle("-fx-text-fill: #89b4fa; -fx-font-size: 36px; -fx-font-weight: bold;");
        titleLabel.setAlignment(Pos.CENTER);
        titleLabel.setMaxWidth(Double.MAX_VALUE);

        Label subtitleLabel = new Label(translator.tr("app_subtitle"));
        subtitleLabel.setStyle("-fx-text-fill: #cdd6f5; -fx-font-size: 14px;");
        subtitleLabel.setAlignment(Pos.CENTER);
        subtitleLabel.setMaxWidth(Double.MAX_VALUE);

        leftPanel.getChildren().addAll(titleLabel, subtitleLabel);

        // Caption editor
        captionEdit = new TextArea();
        captionEdit.setEditable(false);
        captionEdit.setWrapText(true);
        captionEdit.setStyle("-fx-font-family: Consolas; -fx-font-size: 12px; -fx-background-color: #2a2a2a; " +
                "-fx-text-fill: #ffffff; -fx-control-inner-background: #2a2a2a;");
        captionEdit.setPromptText(translator.tr("ai_caption_editor"));
        VBox.setVgrow(captionEdit, Priority.ALWAYS);
        leftPanel.getChildren().add(captionEdit);

        // Button row
        HBox buttonRow = new HBox(10);
        buttonRow.setAlignment(Pos.CENTER);

        editButton = createStyledButton(translator.tr("edit_captions"), "#4a6fa5");
        editButton.setDisable(true);
        editButton.setOnAction(e -> toggleEditMode());

        workspaceButton = createStyledButton(translator.tr("workspace"), "#4a6fa5");
        workspaceButton.setOnAction(e -> openWorkspaceDialog());

        settingsButton = createStyledButton(translator.tr("settings"), "#4a6fa5");
        settingsButton.setOnAction(e -> openSettings());

        downloadButton = createStyledButton(translator.tr("download_model"), "#4a6fa5");
        downloadButton.setOnAction(e -> downloadModel());

        buttonRow.getChildren().addAll(editButton, workspaceButton, settingsButton, downloadButton);
        leftPanel.getChildren().add(buttonRow);

        return leftPanel;
    }

    private VBox createRightPanel() {
        VBox rightPanel = new VBox(15);
        rightPanel.setPadding(new Insets(15));
        rightPanel.setStyle("-fx-background-color: transparent;");

        // Login button
        loginButton = createStyledButton(translator.tr("login_google"), "#4285f4");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(e -> googleLogin());
        rightPanel.getChildren().add(loginButton);

        // Mode selection
        VBox modeCard = createCard();
        Label modeLabel = new Label(translator.tr("processing_mode"));
        modeLabel.setStyle("-fx-text-fill: #4a6fa5; -fx-font-weight: bold;");

        modeCombo = new ComboBox<>();
        modeCombo.getItems().addAll(translator.tr("normal_mode"), translator.tr("online_mode"));
        modeCombo.setValue(settings.getProcessingMode() == ProcessingMode.ONLINE ?
                translator.tr("online_mode") : translator.tr("normal_mode"));
        modeCombo.setMaxWidth(Double.MAX_VALUE);
        modeCard.getChildren().addAll(modeLabel, modeCombo);
        rightPanel.getChildren().add(modeCard);

        // Language selection
        VBox langCard = createCard();
        Label langLabel = new Label(translator.tr("language"));
        langLabel.setStyle("-fx-text-fill: #4a6fa5; -fx-font-weight: bold;");

        langCombo = new ComboBox<>();
        langCombo.getItems().addAll(
                translator.tr("english_transcribe"), translator.tr("japanese_translate"),
                translator.tr("chinese_transcribe"), translator.tr("french_transcribe"),
                translator.tr("german_transcribe"), translator.tr("spanish_transcribe"),
                translator.tr("russian_transcribe"), translator.tr("arabic_transcribe"),
                translator.tr("hindi_transcribe"), translator.tr("bengali_transcribe"),
                translator.tr("urdu_transcribe"), translator.tr("portuguese_transcribe"),
                translator.tr("italian_transcribe"), translator.tr("dutch_transcribe"),
                translator.tr("polish_transcribe"), translator.tr("turkish_transcribe"),
                translator.tr("vietnamese_transcribe"), translator.tr("thai_transcribe"),
                translator.tr("korean_transcribe")
        );
        langCombo.setValue(translator.tr("english_transcribe"));
        langCombo.setMaxWidth(Double.MAX_VALUE);
        langCard.getChildren().addAll(langLabel, langCombo);
        rightPanel.getChildren().add(langCard);

        // Words per line
        VBox wplCard = createCard();
        Label wplLabel = new Label(translator.tr("words_per_line"));
        wplLabel.setStyle("-fx-text-fill: #4a6fa5; -fx-font-weight: bold;");

        wordsSpinner = new Spinner<>(1, 20, settings.getInt("words_per_line"));
        wordsSpinner.setMaxWidth(Double.MAX_VALUE);
        wplCard.getChildren().addAll(wplLabel, wordsSpinner);
        rightPanel.getChildren().add(wplCard);

        // Import button
        importButton = createStyledButton(translator.tr("import_media"), "#27ae60");
        importButton.setMaxWidth(Double.MAX_VALUE);
        importButton.setOnAction(e -> importMedia());
        rightPanel.getChildren().add(importButton);

        // Format selection
        VBox fmtCard = createCard();
        Label fmtLabel = new Label(translator.tr("output_format"));
        fmtLabel.setStyle("-fx-text-fill: #4a6fa5; -fx-font-weight: bold;");

        formatCombo = new ComboBox<>();
        formatCombo.getItems().addAll(translator.tr("srt_format"), translator.tr("ass_format"));
        formatCombo.setValue(translator.tr("srt_format"));
        formatCombo.setMaxWidth(Double.MAX_VALUE);
        fmtCard.getChildren().addAll(fmtLabel, formatCombo);
        rightPanel.getChildren().add(fmtCard);

        // Output folder
        VBox folderCard = createCard();
        Label folderLabel = new Label(translator.tr("output_folder"));
        folderLabel.setStyle("-fx-text-fill: #4a6fa5; -fx-font-weight: bold;");

        HBox folderRow = new HBox(5);
        outputFolderField = new TextField();
        outputFolderField.setEditable(false);
        outputFolderField.setPromptText(translator.tr("temp_dir_placeholder"));
        outputFolderField.setText(settings.getString("last_output_folder"));
        HBox.setHgrow(outputFolderField, Priority.ALWAYS);

        Button browseButton = createStyledButton(translator.tr("browse_folder"), "#4a6fa5");
        browseButton.setOnAction(e -> browseOutputFolder());

        folderRow.getChildren().addAll(outputFolderField, browseButton);
        folderCard.getChildren().addAll(folderLabel, folderRow);
        rightPanel.getChildren().add(folderCard);

        // Enhance button
        enhanceButton = createStyledButton(translator.tr("enhance_audio"), "#e67e22");
        enhanceButton.setMaxWidth(Double.MAX_VALUE);
        enhanceButton.setDisable(true);
        enhanceButton.setOnAction(e -> enhanceAudio());
        rightPanel.getChildren().add(enhanceButton);

        // Generate button
        generateButton = createStyledButton(translator.tr("generate"), "#2ecc71");
        generateButton.setMaxWidth(Double.MAX_VALUE);
        generateButton.setOnAction(e -> generateCaptions());
        rightPanel.getChildren().add(generateButton);

        // Status card
        VBox statusCard = createCard();
        HBox statusRow = new HBox(10);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        Label statusTitle = new Label(translator.tr("status"));
        statusTitle.setStyle("-fx-text-fill: #4a6fa5; -fx-font-weight: bold;");

        statusLabel = new Label(translator.tr("idle"));
        statusLabel.setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;");

        statusRow.getChildren().addAll(statusTitle, statusLabel);
        statusCard.getChildren().add(statusRow);
        rightPanel.getChildren().add(statusCard);

        // Progress area
        VBox progressBox = new VBox(5);
        progressBox.setVisible(false);

        progressBar = new ProgressBar(0);
        progressBar.setMaxWidth(Double.MAX_VALUE);

        HBox progressInfoRow = new HBox(10);
        progressLabel = new Label("0%");
        speedLabel = new Label(translator.tr("speed") + " --");
        etaLabel = new Label(translator.tr("eta") + " --");
        progressInfoRow.getChildren().addAll(progressLabel, speedLabel, etaLabel);

        colabLink = new Hyperlink();
        colabLink.setVisible(false);
        colabLink.setOnAction(e -> {
            if (currentNotebookUrl != null) {
                Main.getAppHostServices().showDocument(currentNotebookUrl);
            }
        });

        progressBox.getChildren().addAll(progressBar, progressInfoRow, colabLink);
        rightPanel.getChildren().add(progressBox);

        return rightPanel;
    }

    private VBox createCard() {
        VBox card = new VBox(5);
        card.setPadding(new Insets(10));
        card.setStyle("-fx-background-color: #2a2a2a; -fx-background-radius: 10; -fx-border-color: #4a6fa5; " +
                "-fx-border-radius: 10; -fx-border-width: 1;");
        return card;
    }

    private Button createStyledButton(String text, String color) {
        Button button = new Button(text);
        button.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 5; -fx-padding: 8 16;");

        // Add hover effect
        button.setOnMouseEntered(e ->
                button.setStyle("-fx-background-color: derive(" + color + ", 20%); -fx-text-fill: white; " +
                        "-fx-font-weight: bold; -fx-background-radius: 5; -fx-padding: 8 16;")
        );
        button.setOnMouseExited(e ->
                button.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; " +
                        "-fx-background-radius: 5; -fx-padding: 8 16;")
        );

        return button;
    }

    private void createDockWidgets() {
        // This would be implemented with JavaFX's built-in capabilities
        // For simplicity, we'll add hardware monitor to the right panel
        // In a full implementation, you'd use Stage or Popup windows
    }

    private void setupEventHandlers() {
        // Mode change
        modeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            boolean isOnline = translator.tr("online_mode").equals(newVal);
            settings.set("last_mode", isOnline ? ProcessingMode.ONLINE.name() : ProcessingMode.LOCAL.name());
            updateDownloadButtonVisibility();
        });

        // Language change
        langCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            settings.set("default_lang", getLanguageCode(newVal));
        });

        // Words per line change
        wordsSpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
            settings.set("words_per_line", newVal);
        });

        // Format change
        formatCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            settings.set("output_format", newVal);
        });

        // Window close
        stage.setOnCloseRequest(e -> {
            if (!confirmExit()) {
                e.consume();
            } else {
                cleanupBeforeExit();
            }
        });
    }

    private String getLanguageCode(String languageName) {
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
        langMap.put(translator.tr("bengali_transcribe"), "bn");
        langMap.put(translator.tr("urdu_transcribe"), "ur");
        langMap.put(translator.tr("portuguese_transcribe"), "pt");
        langMap.put(translator.tr("italian_transcribe"), "it");
        langMap.put(translator.tr("dutch_transcribe"), "nl");
        langMap.put(translator.tr("polish_transcribe"), "pl");
        langMap.put(translator.tr("turkish_transcribe"), "tr");
        langMap.put(translator.tr("vietnamese_transcribe"), "vi");
        langMap.put(translator.tr("thai_transcribe"), "th");
        langMap.put(translator.tr("korean_transcribe"), "ko");

        return langMap.getOrDefault(languageName, "en");
    }

    private void updateDownloadButtonVisibility() {
        boolean isOnline = translator.tr("online_mode").equals(modeCombo.getValue());
        downloadButton.setVisible(!isOnline);

        if (!isOnline) {
            boolean modelExists = ModelValidator.isModelDownloaded(settings.getString("models_dir"), "large-v1");
            downloadButton.setDisable(modelExists);
        }
    }

    private void loadSettings() {
        settings.load();

        // Apply window size
        int width = settings.getInt("window_width");
        int height = settings.getInt("window_height");
        if (width > 0 && height > 0) {
            stage.setWidth(width);
            stage.setHeight(height);
        }

        boolean maximized = settings.getBoolean("window_maximized");
        if (maximized) {
            stage.setMaximized(true);
        }

        // Apply language
        translator.setLanguage(settings.getString("language"));

        // Apply other settings
        String lastOutput = settings.getString("last_output_folder");
        if (lastOutput != null && !lastOutput.isEmpty()) {
            outputFolderField.setText(lastOutput);
        }
    }

    private void applyTheme() {
        Theme theme = settings.getTheme();
        String accentColor = settings.getString("accent_color");
        int glowIntensity = settings.getInt("glow_intensity");
        int cardOpacity = settings.getInt("card_opacity");
        String fontFamily = settings.getString("font_family");
        int fontSize = settings.getInt("font_size");

        // Apply theme via CSS
        String css = ThemeManager.getStylesheet(theme, accentColor, glowIntensity, cardOpacity, fontFamily, fontSize);
        Scene scene = stage.getScene();
        if (scene != null) {
            scene.getStylesheets().clear();
            // In a real implementation, you'd create a temporary CSS file
        }
    }

    private void startHardwareMonitoring() {
        if (settings.getBoolean("hardware_monitoring")) {
            hardwareMonitor.startMonitoring(settings.getInt("monitoring_interval"));

            monitorExecutor.scheduleAtFixedRate(() -> {
                Platform.runLater(() -> {
                    updateHardwareDisplay();
                });
            }, 0, settings.getInt("monitoring_interval"), TimeUnit.MILLISECONDS);
        }
    }

    private void updateHardwareDisplay() {
        // Update status bar with hardware info
        String cpuInfo = hardwareMonitor.getCPUInfo().toString();
        String ramInfo = hardwareMonitor.getRAMInfo().toString();

        // Could update status bar or a dedicated widget
        stage.setTitle(translator.tr("window_title") + " - " + cpuInfo + " | " + ramInfo);
    }

    private void importMedia() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(translator.tr("import_media"));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Media Files", "*.mp4", "*.mkv", "*.avi", "*.mov", "*.webm",
                        "*.flv", "*.wmv", "*.mp3", "*.wav", "*.m4a", "*.aac", "*.flac", "*.ogg", "*.wma")
        );

        String lastInput = settings.getString("last_input_file");
        if (lastInput != null && !lastInput.isEmpty()) {
            File lastFile = new File(lastInput);
            if (lastFile.exists()) {
                fileChooser.setInitialDirectory(lastFile.getParentFile());
            }
        }

        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile != null) {
            processMediaImport(selectedFile);
        }
    }

    private void processMediaImport(File mediaFile) {
        logger.info("Importing: {}", mediaFile.getAbsolutePath());

        currentMediaFile = mediaFile;

        // Set output folder to media's parent if not set
        if (outputFolderField.getText().isEmpty()) {
            outputFolderField.setText(mediaFile.getParent());
            settings.set("last_output_folder", mediaFile.getParent());
        }

        settings.set("last_input_file", mediaFile.getAbsolutePath());
        settings.save();

        // Extract audio if video
        if (isVideoFile(mediaFile)) {
            extractAudioFromVideo(mediaFile);
        } else {
            convertAudioToWav(mediaFile);
        }

        // Save session state
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("last_input_file", mediaFile.getAbsolutePath());
        sessionData.put("last_output_folder", outputFolderField.getText());
        sessionManager.saveOperationState("import", sessionData);

        enhanceButton.setDisable(false);
        generateButton.setDisable(false);

        logger.info("Media import complete");
        showInfo(translator.tr("import_complete"), translator.tr("import_success"));
    }

    private boolean isVideoFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi") ||
                name.endsWith(".mov") || name.endsWith(".webm") || name.endsWith(".flv") ||
                name.endsWith(".wmv");
    }

    private void extractAudioFromVideo(File videoFile) {
        try {
            String tempDir = settings.getString("temp_dir");
            String tempName = videoFile.getName().replaceFirst("[.][^.]+$", "") + ".temp.wav";
            File tempAudio = new File(tempDir, tempName);

            // Use FFmpeg to extract audio
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-i", videoFile.getAbsolutePath(),
                    "-vn", "-acodec", "pcm_s16le", "-ar", "16000",
                    "-ac", "1", tempAudio.getAbsolutePath(), "-y"
            );

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0 && tempAudio.exists()) {
                currentAudioFile = tempAudio;
                logger.info("Audio extracted to: {}", tempAudio.getAbsolutePath());
            } else {
                logger.error("FFmpeg extraction failed with exit code: {}", exitCode);
                showError(translator.tr("conversion_warning"), translator.tr("conversion_warning_msg"));
                currentAudioFile = videoFile;
            }
        } catch (Exception e) {
            logger.error("Failed to extract audio", e);
            showError(translator.tr("conversion_warning"), e.getMessage());
            currentAudioFile = videoFile;
        }
    }

    private void convertAudioToWav(File audioFile) {
        try {
            String tempDir = settings.getString("temp_dir");
            String tempName = audioFile.getName().replaceFirst("[.][^.]+$", "") + ".temp.wav";
            File tempAudio = new File(tempDir, tempName);

            // Use FFmpeg to convert
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", "-i", audioFile.getAbsolutePath(),
                    "-acodec", "pcm_s16le", "-ar", "16000",
                    "-ac", "1", tempAudio.getAbsolutePath(), "-y"
            );

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode == 0 && tempAudio.exists()) {
                currentAudioFile = tempAudio;
                logger.info("Audio converted to: {}", tempAudio.getAbsolutePath());
            } else {
                logger.error("FFmpeg conversion failed with exit code: {}", exitCode);
                currentAudioFile = audioFile;
            }
        } catch (Exception e) {
            logger.error("Failed to convert audio", e);
            currentAudioFile = audioFile;
        }
    }

    private void browseOutputFolder() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(translator.tr("browse_output"));
        fileChooser.setInitialDirectory(new File(outputFolderField.getText()));

        File selectedDir = fileChooser.showSaveDialog(stage);
        if (selectedDir != null) {
            String dirPath = selectedDir.getParent();
            outputFolderField.setText(dirPath);
            settings.set("last_output_folder", dirPath);
            settings.save();
        }
    }

    private void enhanceAudio() {
        if (currentAudioFile == null || !currentAudioFile.exists()) {
            showWarning(translator.tr("no_audio"), translator.tr("no_audio_msg"));
            return;
        }

        // This would call Spleeter via Python subprocess
        // For now, show a placeholder message
        showInfo("Enhance Audio", "Vocal enhancement would run here using Spleeter");
    }

    private void generateCaptions() {
        if (currentAudioFile == null || !currentAudioFile.exists()) {
            showWarning(translator.tr("no_media"), translator.tr("no_media_msg"));
            return;
        }

        if (isGenerating) {
            showWarning("In Progress", "Caption generation already in progress");
            return;
        }

        isGenerating = true;
        cancelRequested.set(false);

        String mode = modeCombo.getValue();
        boolean isOnline = translator.tr("online_mode").equals(mode);

        String languageName = langCombo.getValue();
        String languageCode = getLanguageCode(languageName);
        boolean isTranslate = languageName.contains("→");
        String task = isTranslate ? "translate" : "transcribe";

        int wordsPerLine = wordsSpinner.getValue();
        String format = formatCombo.getValue();
        String formatExt = translator.tr("srt_format").equals(format) ? ".srt" : ".ass";

        String outputDir = outputFolderField.getText();
        if (outputDir == null || outputDir.isEmpty()) {
            outputDir = System.getProperty("user.home");
        }

        String baseName = currentMediaFile != null ?
                currentMediaFile.getName().replaceFirst("[.][^.]+$", "") : "captions";
        File outputFile = new File(outputDir, baseName + "_captions" + formatExt);

        // Check for overwrite
        if (outputFile.exists()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle(translator.tr("overwrite"));
            alert.setHeaderText(translator.tr("overwrite_msg").replace("{}", outputFile.getName()));
            alert.setContentText(translator.tr("overwrite"));

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() != ButtonType.OK) {
                isGenerating = false;
                return;
            }
        }

        // Show progress UI
        showProgress(true);

        if (isOnline) {
            // Online mode using Google Colab
            startOnlineGeneration(currentAudioFile, languageCode, task, wordsPerLine, formatExt, outputFile);
        } else {
            // Local mode using Whisper
            startLocalGeneration(currentAudioFile, languageCode, task, wordsPerLine, formatExt, outputFile);
        }
    }

    private void startLocalGeneration(File audioFile, String languageCode, String task,
                                      int wordsPerLine, String formatExt, File outputFile) {
        // This would call Whisper via Python subprocess
        // For now, show placeholder
        new Thread(() -> {
            try {
                // Simulate progress
                for (int i = 0; i <= 100; i += 10) {
                    if (cancelRequested.get()) {
                        Platform.runLater(() -> {
                            showProgress(false);
                            isGenerating = false;
                            showInfo(translator.tr("canceled"), "Generation canceled");
                        });
                        return;
                    }

                    final int progress = i;
                    Platform.runLater(() -> {
                        progressBar.setProgress(progress / 100.0);
                        progressLabel.setText(progress + "%");
                    });

                    Thread.sleep(500);
                }

                // Create sample subtitles
                subtitles.clear();
                displayLines.clear();

                SubtitleEntry entry = new SubtitleEntry();
                entry.setIndex(1);
                entry.setStart(0);
                entry.setEnd(5000);
                entry.setText("Sample subtitle text from local generation");
                subtitles.add(entry);
                displayLines.add(entry.getText());

                // Save to file
                if (formatExt.equals(".srt")) {
                    subtitleManager.saveSRT(subtitles, outputFile);
                } else {
                    subtitleManager.saveASS(subtitles, outputFile);
                }

                generated = true;
                editButton.setDisable(false);

                // Update UI
                Platform.runLater(() -> {
                    captionEdit.setText(String.join("\n\n", displayLines));
                    showProgress(false);
                    isGenerating = false;
                    showInfo(translator.tr("generation_complete"),
                            translator.tr("generation_success") + "\n" + outputFile.getAbsolutePath());
                });

            } catch (Exception e) {
                logger.error("Generation failed", e);
                Platform.runLater(() -> {
                    showProgress(false);
                    isGenerating = false;
                    showError(translator.tr("failed"), e.getMessage());
                });
            }
        }).start();
    }

    private void startOnlineGeneration(File audioFile, String languageCode, String task,
                                       int wordsPerLine, String formatExt, File outputFile) {
        // This would use Google Drive API and Colab
        // For now, show placeholder
        showInfo("Online Mode", "Online mode would use Google Colab with large-v3 model");
        showProgress(false);
        isGenerating = false;
    }

    private void showProgress(boolean show) {
        progressBar.getParent().setVisible(show);
        progressBar.setProgress(0);
        progressLabel.setText("0%");
        speedLabel.setText(translator.tr("speed") + " --");
        etaLabel.setText(translator.tr("eta") + " --");

        // Disable buttons during processing
        importButton.setDisable(show);
        enhanceButton.setDisable(show);
        generateButton.setDisable(show);
        editButton.setDisable(show);
        settingsButton.setDisable(show);
        workspaceButton.setDisable(show);
        downloadButton.setDisable(show);
        loginButton.setDisable(show);
        modeCombo.setDisable(show);
        langCombo.setDisable(show);
        wordsSpinner.setDisable(show);
        formatCombo.setDisable(show);

        if (show) {
            statusLabel.setText(translator.tr("processing"));
            statusLabel.setStyle("-fx-text-fill: #f39c12;");
        } else {
            statusLabel.setText(translator.tr("idle"));
            statusLabel.setStyle("-fx-text-fill: #2ecc71;");
        }
    }

    private void toggleEditMode() {
        if (!generated) {
            return;
        }

        editActive = !editActive;
        captionEdit.setEditable(editActive);
        editButton.setText(editActive ? translator.tr("save_exit_edit") : translator.tr("edit_captions"));

        if (!editActive) {
            applyEditedCaptions();
        } else {
            captionEdit.requestFocus();
        }
    }

    private void applyEditedCaptions() {
        String textContent = captionEdit.getText();
        String[] editedLines = textContent.split("\n\n");

        if (editedLines.length != subtitles.size()) {
            showWarning(translator.tr("mismatch"), "Line count changed. Edits not applied.");
            refreshCaptionPreview();
            return;
        }

        for (int i = 0; i < editedLines.length; i++) {
            subtitles.get(i).setText(editedLines[i].trim());
            displayLines.set(i, editedLines[i].trim());
        }

        refreshCaptionPreview();
        showInfo(translator.tr("saved"), translator.tr("edits_applied"));
    }

    private void refreshCaptionPreview() {
        captionEdit.setText(String.join("\n\n", displayLines));
    }

    private void exportSubtitles() {
        if (!generated || subtitles.isEmpty()) {
            showWarning("No Subtitles", "No subtitles to export");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(translator.tr("export"));
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Subtitle Files", "*.srt", "*.ass", "*.ssa", "*.vtt")
        );

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        fileChooser.setInitialFileName("subtitles_" + timestamp + ".srt");

        File outputFile = fileChooser.showSaveDialog(stage);
        if (outputFile != null) {
            try {
                String ext = outputFile.getName().substring(outputFile.getName().lastIndexOf("."));
                if (ext.equals(".srt")) {
                    subtitleManager.saveSRT(subtitles, outputFile);
                } else if (ext.equals(".ass") || ext.equals(".ssa")) {
                    subtitleManager.saveASS(subtitles, outputFile);
                } else {
                    subtitleManager.saveSRT(subtitles, outputFile);
                }
                showInfo("Export Complete", "Subtitles exported to:\n" + outputFile.getAbsolutePath());
            } catch (Exception e) {
                logger.error("Export failed", e);
                showError("Export Failed", e.getMessage());
            }
        }
    }

    private void saveSession() {
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("last_input_file", currentMediaFile != null ? currentMediaFile.getAbsolutePath() : "");
        sessionData.put("last_output_folder", outputFolderField.getText());
        sessionData.put("subtitles", subtitles);
        sessionData.put("display_lines", displayLines);
        sessionData.put("generated", generated);
        sessionData.put("mode", modeCombo.getValue());

        sessionManager.saveSession(sessionData);
        showInfo("Session Saved", "Current session has been saved.");
    }

    @SuppressWarnings("unchecked")
    private void loadSession() {
        Map<String, Object> sessionData = sessionManager.loadSession();
        if (sessionData != null) {
            String lastInput = (String) sessionData.get("last_input_file");
            if (lastInput != null && !lastInput.isEmpty()) {
                File inputFile = new File(lastInput);
                if (inputFile.exists()) {
                    processMediaImport(inputFile);
                }
            }

            String lastOutput = (String) sessionData.get("last_output_folder");
            if (lastOutput != null && !lastOutput.isEmpty()) {
                outputFolderField.setText(lastOutput);
            }

            List<SubtitleEntry> loadedSubtitles = (List<SubtitleEntry>) sessionData.get("subtitles");
            if (loadedSubtitles != null && !loadedSubtitles.isEmpty()) {
                subtitles = loadedSubtitles;
                displayLines = (List<String>) sessionData.get("display_lines");
                generated = (boolean) sessionData.get("generated");

                if (generated) {
                    captionEdit.setText(String.join("\n\n", displayLines));
                    editButton.setDisable(false);
                }
            }

            showInfo("Session Loaded", "Session loaded successfully.");
        } else {
            showWarning("Session Error", "No saved session found.");
        }
    }

    private void openSettings() {
        SettingsDialog dialog = new SettingsDialog(settings, translator);
        dialog.showAndWait().ifPresent(newSettings -> {
            settings = newSettings;
            translator.setLanguage(settings.getString("language"));
            applyTheme();
            loadSettings();
            updateDownloadButtonVisibility();
        });
    }

    private void openWorkspaceDialog() {
        WorkspaceCustomizeDialog dialog = new WorkspaceCustomizeDialog(settings, translator);
        dialog.showAndWait().ifPresent(updatedSettings -> {
            settings = updatedSettings;
            applyTheme();
            saveSession();
        });
    }

    private void downloadModel() {
        if (isGenerating) {
            return;
        }

        String modelsDir = settings.getString("models_dir");
        boolean modelExists = ModelValidator.isModelDownloaded(modelsDir, "large-v1");

        if (modelExists) {
            showInfo("Model Exists", "Whisper large-v1 model is already downloaded.");
            updateDownloadButtonVisibility();
            return;
        }

        // Start download in background
        new Thread(() -> {
            try {
                Platform.runLater(() -> {
                    showProgress(true);
                    statusLabel.setText("Downloading model...");
                });

                // Simulate download progress
                for (int i = 0; i <= 100; i += 5) {
                    if (cancelRequested.get()) {
                        Platform.runLater(() -> {
                            showProgress(false);
                            statusLabel.setText(translator.tr("idle"));
                            showInfo(translator.tr("canceled"), "Download canceled");
                        });
                        return;
                    }

                    final int progress = i;
                    Platform.runLater(() -> {
                        progressBar.setProgress(progress / 100.0);
                        progressLabel.setText(progress + "%");
                        double speed = 50.0 * 1024 * 1024; // 50 MB/s
                        speedLabel.setText(translator.tr("speed") + " " +
                                String.format("%.1f MB/s", speed / (1024 * 1024)));
                        etaLabel.setText(translator.tr("eta") + " " +
                                String.format("%d seconds", (100 - progress) * 2));
                    });

                    Thread.sleep(100);
                }

                // In real implementation, download the model
                // For now, create a placeholder file
                File modelFile = new File(modelsDir, "large-v1.pt");
                if (!modelFile.exists()) {
                    // Create dummy file
                    Files.write(modelFile.toPath(), new byte[1024 * 1024 * 100]); // 100 MB dummy
                }

                Platform.runLater(() -> {
                    showProgress(false);
                    statusLabel.setText(translator.tr("idle"));
                    updateDownloadButtonVisibility();
                    showInfo(translator.tr("download_complete"), translator.tr("download_success"));
                });

            } catch (Exception e) {
                logger.error("Download failed", e);
                Platform.runLater(() -> {
                    showProgress(false);
                    statusLabel.setText(translator.tr("idle"));
                    showError(translator.tr("download_failed"), e.getMessage());
                });
            }
        }).start();
    }

    private void googleLogin() {
        // This would use Google OAuth
        showInfo("Google Login", "Google Drive integration would be configured here");
    }

    private void openDocumentation() {
        Main.getAppHostServices().showDocument("https://github.com/NotY215/NotyCaption");
    }

    private void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About NotyCaption Pro");
        alert.setHeaderText(translator.tr("app_name") + " v2026.5.0");
        alert.setContentText(
                "Professional AI-Powered Caption Generator\n\n" +
                        "Author: NotY215\n" +
                        "License: Proprietary\n\n" +
                        "Features:\n" +
                        "• AI transcription using OpenAI Whisper\n" +
                        "• Hardware acceleration support\n" +
                        "• Real-time hardware monitoring\n" +
                        "• Multi-language support\n" +
                        "• Google Drive integration\n" +
                        "• Vocal enhancement with Spleeter\n" +
                        "• Advanced subtitle editing\n\n" +
                        "Copyright © 2026 NotY215. All rights reserved."
        );
        alert.showAndWait();
    }

    private boolean confirmExit() {
        if (isGenerating) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Operation in Progress");
            alert.setHeaderText("An operation is currently in progress.");
            alert.setContentText("Are you sure you want to exit?");

            Optional<ButtonType> result = alert.showAndWait();
            return result.isPresent() && result.get() == ButtonType.OK;
        }

        return true;
    }

    public void cleanupBeforeExit() {
        logger.info("Cleaning up before exit");

        // Stop hardware monitoring
        if (hardwareMonitor != null) {
            hardwareMonitor.stopMonitoring();
        }
        if (monitorExecutor != null) {
            monitorExecutor.shutdown();
        }

        // Stop media player
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
        if (timer != null) {
            timer.cancel();
        }

        // Save settings
        if (settings != null) {
            settings.set("window_width", (int) stage.getWidth());
            settings.set("window_height", (int) stage.getHeight());
            settings.set("window_maximized", stage.isMaximized());
            settings.save();
        }

        // Save session
        if (settings != null && settings.getBoolean("auto_save")) {
            saveSession();
        }

        // Clean up temp files
        if (currentAudioFile != null && currentAudioFile.getName().contains(".temp.") && currentAudioFile.exists()) {
            currentAudioFile.delete();
        }

        logger.info("Cleanup complete");
    }

    public void closeWindow() {
        cleanupBeforeExit();
        stage.close();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void show() {
        stage.show();
    }

    public Stage getStage() {
        return stage;
    }
}