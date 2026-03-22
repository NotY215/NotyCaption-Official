package com.noty215.notycaption.ui;

import com.noty215.notycaption.subtitle.SubtitleEntry;
import com.noty215.notycaption.utils.Translator;
import javafx.animation.AnimationTimer;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

import java.io.File;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Preview widget for media playback with subtitle preview
 */
public class PreviewWidget extends VBox {

    private final Translator translator;

    private MediaView mediaView;
    private MediaPlayer mediaPlayer;
    private TextArea subtitleDisplay;
    private Slider positionSlider;
    private Slider volumeSlider;
    private Label timeLabel;
    private Button playButton;
    private Button muteButton;

    private List<SubtitleEntry> subtitles;
    private boolean muted = false;
    private Timer timer;

    public PreviewWidget(Translator translator) {
        this.translator = translator;

        setupUI();
    }

    private void setupUI() {
        setPadding(new Insets(10));
        setSpacing(10);
        setStyle("-fx-background-color: #1a1a1a; -fx-border-color: #333; -fx-border-radius: 15; -fx-background-radius: 15;");

        // Video container
        VBox videoContainer = new VBox();
        videoContainer.setStyle("-fx-background-color: #0a0a0a; -fx-border-color: #333; -fx-border-radius: 10; -fx-background-radius: 10;");
        videoContainer.setMinHeight(200);

        mediaView = new MediaView();
        mediaView.setFitWidth(400);
        mediaView.setPreserveRatio(true);
        videoContainer.getChildren().add(mediaView);

        Label noVideoLabel = new Label(translator.tr("no_media"));
        noVideoLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 16px;");
        noVideoLabel.setAlignment(javafx.geometry.Pos.CENTER);
        noVideoLabel.setMaxWidth(Double.MAX_VALUE);
        videoContainer.getChildren().add(noVideoLabel);

        getChildren().add(videoContainer);

        // Subtitle container
        VBox subtitleContainer = new VBox(5);
        subtitleContainer.setStyle("-fx-background-color: rgba(30,30,30,0.9); -fx-border-color: #333; " +
                "-fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 10;");

        HBox subtitleHeader = new HBox(10);
        Label subtitleLabel = new Label("📝 " + translator.tr("preview"));
        subtitleLabel.setStyle("-fx-text-fill: #4a6fa5;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        timeLabel = new Label("00:00 / 00:00");
        timeLabel.setStyle("-fx-text-fill: #4a6fa5; -fx-font-family: monospace;");
        subtitleHeader.getChildren().addAll(subtitleLabel, spacer, timeLabel);

        subtitleDisplay = new TextArea();
        subtitleDisplay.setEditable(false);
        subtitleDisplay.setMaxHeight(100);
        subtitleDisplay.setStyle("-fx-background-color: #2a2a2a; -fx-text-fill: #ffffff; " +
                "-fx-border-color: #4a6fa5; -fx-border-radius: 8; -fx-background-radius: 8;");

        subtitleContainer.getChildren().addAll(subtitleHeader, subtitleDisplay);
        getChildren().add(subtitleContainer);

        // Controls
        HBox controls = new HBox(5);
        controls.setStyle("-fx-background-color: #2a2a2a; -fx-border-color: #333; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 5;");

        playButton = new Button("▶");
        playButton.setPrefSize(40, 30);
        playButton.setOnAction(e -> togglePlayback());

        positionSlider = new Slider();
        positionSlider.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(positionSlider, Priority.ALWAYS);
        positionSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null && mediaPlayer.getStatus() != MediaPlayer.Status.STOPPED) {
                mediaPlayer.seek(Duration.millis(newVal.doubleValue()));
            }
        });

        timeLabel = new Label("00:00/00:00");

        muteButton = new Button("🔊");
        muteButton.setPrefSize(40, 30);
        muteButton.setOnAction(e -> toggleMute());

        volumeSlider = new Slider(0, 100, 100);
        volumeSlider.setPrefWidth(100);
        volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (mediaPlayer != null) {
                mediaPlayer.setVolume(newVal.doubleValue() / 100.0);
                updateVolumeIcon();
            }
        });

        controls.getChildren().addAll(playButton, positionSlider, timeLabel, muteButton, volumeSlider);
        getChildren().add(controls);

        // Setup timer for position updates
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                javafx.application.Platform.runLater(() -> updatePosition());
            }
        }, 0, 50);
    }

    public void setMedia(File mediaFile) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.dispose();
        }

        String url = mediaFile.toURI().toString();
        Media media = new Media(url);
        mediaPlayer = new MediaPlayer(media);
        mediaView.setMediaPlayer(mediaPlayer);

        mediaPlayer.setOnReady(() -> {
            positionSlider.setMax(mediaPlayer.getTotalDuration().toMillis());
            updateTimeDisplay(0, mediaPlayer.getTotalDuration().toMillis());
        });

        mediaPlayer.currentTimeProperty().addListener((obs, oldVal, newVal) -> {
            positionSlider.setValue(newVal.toMillis());
            updateSubtitleHighlight(newVal.toMillis());
        });

        // Hide no video label if video
        if (mediaFile.getName().toLowerCase().matches(".*\\.(mp4|mkv|avi|mov|webm|flv|wmv)$")) {
            mediaView.setVisible(true);
            if (mediaView.getParent() instanceof VBox) {
                ((VBox) mediaView.getParent()).getChildren().get(0).setVisible(false);
            }
        }

        mediaPlayer.play();
    }

    public void setSubtitles(List<SubtitleEntry> subtitles) {
        this.subtitles = subtitles;
    }

    private void togglePlayback() {
        if (mediaPlayer == null) return;

        if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
            mediaPlayer.pause();
            playButton.setText("▶");
        } else {
            mediaPlayer.play();
            playButton.setText("⏸");
        }
    }

    private void toggleMute() {
        if (mediaPlayer == null) return;

        muted = !muted;
        mediaPlayer.setMute(muted);
        updateVolumeIcon();
    }

    private void updateVolumeIcon() {
        if (muted || volumeSlider.getValue() == 0) {
            muteButton.setText("🔇");
        } else if (volumeSlider.getValue() < 30) {
            muteButton.setText("🔈");
        } else if (volumeSlider.getValue() < 70) {
            muteButton.setText("🔉");
        } else {
            muteButton.setText("🔊");
        }
    }

    private void updatePosition() {
        if (mediaPlayer == null) return;

        double current = mediaPlayer.getCurrentTime().toMillis();
        double total = mediaPlayer.getTotalDuration().toMillis();
        updateTimeDisplay(current, total);

        positionSlider.setValue(current);
    }

    private void updateTimeDisplay(double current, double total) {
        String currentStr = formatTime((long) current);
        String totalStr = formatTime((long) total);
        timeLabel.setText(currentStr + " / " + totalStr);
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void updateSubtitleHighlight(double millis) {
        if (subtitles == null || subtitles.isEmpty()) {
            return;
        }

        double seconds = millis / 1000.0;
        for (SubtitleEntry sub : subtitles) {
            if (sub.getStart() <= seconds && seconds <= sub.getEnd()) {
                subtitleDisplay.setText(sub.getText());
                return;
            }
        }
        subtitleDisplay.clear();
    }

    public void stop() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
        if (timer != null) {
            timer.cancel();
        }
    }
}