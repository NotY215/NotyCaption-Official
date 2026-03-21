package com.noty215.notycaption.ui;

import com.noty215.notycaption.subtitle.SubtitleEntry;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class PreviewWidget extends JPanel {
    private JLabel videoLabel;
    private JTextArea subtitleDisplay;
    private JSlider positionSlider;
    private JLabel timeLabel;
    private JButton playButton;
    private JSlider volumeSlider;
    private JButton volumeButton;
    private Timer updateTimer;
    private List<SubtitleEntry> subtitles;
    private boolean isPlaying;
    private int currentPosition;
    private int duration;
    private int volume;
    private boolean muted;
    
    public PreviewWidget() {
        setLayout(new BorderLayout(5, 5));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setBackground(new Color(26, 26, 38));
        
        initUI();
        
        volume = 100;
        muted = false;
        isPlaying = false;
        currentPosition = 0;
        duration = 0;
        
        updateTimer = new Timer(100, e -> updatePosition());
    }
    
    private void initUI() {
        // Video container
        JPanel videoContainer = new JPanel(new BorderLayout());
        videoContainer.setBackground(new Color(10, 10, 20));
        videoContainer.setBorder(BorderFactory.createLineBorder(new Color(50, 50, 70), 1));
        
        videoLabel = new JLabel("No video loaded", SwingConstants.CENTER);
        videoLabel.setForeground(new Color(100, 100, 120));
        videoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        videoContainer.add(videoLabel, BorderLayout.CENTER);
        
        add(videoContainer, BorderLayout.CENTER);
        
        // Subtitle preview
        JPanel subtitlePanel = new JPanel(new BorderLayout());
        subtitlePanel.setBackground(new Color(30, 30, 40));
        subtitlePanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 50, 70), 1),
            new EmptyBorder(5, 5, 5, 5)
        ));
        
        JLabel subtitleHeader = new JLabel("📝 Subtitle Preview");
        subtitleHeader.setForeground(new Color(74, 111, 165));
        subtitlePanel.add(subtitleHeader, BorderLayout.NORTH);
        
        subtitleDisplay = new JTextArea();
        subtitleDisplay.setEditable(false);
        subtitleDisplay.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleDisplay.setBackground(new Color(30, 30, 40));
        subtitleDisplay.setForeground(Color.WHITE);
        subtitleDisplay.setLineWrap(true);
        subtitleDisplay.setWrapStyleWord(true);
        subtitleDisplay.setRows(3);
        JScrollPane scrollPane = new JScrollPane(subtitleDisplay);
        scrollPane.setBorder(null);
        subtitlePanel.add(scrollPane, BorderLayout.CENTER);
        
        add(subtitlePanel, BorderLayout.SOUTH);
        
        // Control bar
        JPanel controlBar = new JPanel(new BorderLayout(5, 0));
        controlBar.setBackground(new Color(30, 30, 40));
        controlBar.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        // Play button
        playButton = new JButton("▶");
        playButton.setPreferredSize(new Dimension(40, 30));
        playButton.addActionListener(e -> togglePlayback());
        controlBar.add(playButton, BorderLayout.WEST);
        
        // Position slider
        positionSlider = new JSlider(0, 1000, 0);
        positionSlider.addChangeListener(e -> {
            if (!positionSlider.getValueIsAdjusting()) {
                seekPosition(positionSlider.getValue());
            }
        });
        controlBar.add(positionSlider, BorderLayout.CENTER);
        
        // Time label
        timeLabel = new JLabel("00:00/00:00");
        timeLabel.setForeground(new Color(150, 150, 170));
        timeLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        controlBar.add(timeLabel, BorderLayout.EAST);
        
        // Volume control
        JPanel volumePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        volumePanel.setOpaque(false);
        
        volumeButton = new JButton("🔊");
        volumeButton.setPreferredSize(new Dimension(40, 30));
        volumeButton.addActionListener(e -> toggleMute());
        volumePanel.add(volumeButton);
        
        volumeSlider = new JSlider(0, 100, 100);
        volumeSlider.setPreferredSize(new Dimension(80, 25));
        volumeSlider.addChangeListener(e -> {
            volume = volumeSlider.getValue();
            if (!muted) {
                volumeButton.setText(volume > 70 ? "🔊" : volume > 30 ? "🔉" : "🔈");
            }
        });
        volumePanel.add(volumeSlider);
        
        controlBar.add(volumePanel, BorderLayout.SOUTH);
        
        add(controlBar, BorderLayout.NORTH);
    }
    
    public void setMedia(File mediaFile) {
        if (mediaFile != null && mediaFile.exists()) {
            videoLabel.setText(mediaFile.getName());
            videoLabel.setForeground(Color.WHITE);
            // In a real implementation, this would load the media player
            duration = 300000; // Mock 5 minutes
            positionSlider.setMaximum(duration);
            updateTimer.start();
        }
    }
    
    public void setSubtitles(List<SubtitleEntry> subtitles) {
        this.subtitles = subtitles;
    }
    
    public void play() {
        isPlaying = true;
        playButton.setText("⏸");
        updateTimer.start();
    }
    
    public void pause() {
        isPlaying = false;
        playButton.setText("▶");
    }
    
    public void stop() {
        isPlaying = false;
        currentPosition = 0;
        positionSlider.setValue(0);
        playButton.setText("▶");
        updateTimer.stop();
    }
    
    private void togglePlayback() {
        if (isPlaying) {
            pause();
        } else {
            play();
        }
    }
    
    private void seekPosition(int position) {
        currentPosition = position;
        updateDisplay();
    }
    
    private void toggleMute() {
        muted = !muted;
        volumeButton.setText(muted ? "🔇" : (volume > 70 ? "🔊" : volume > 30 ? "🔉" : "🔈"));
        // In a real implementation, this would mute the player
    }
    
    private void updatePosition() {
        if (isPlaying) {
            currentPosition += 100;
            if (currentPosition > duration) {
                currentPosition = 0;
                pause();
            }
            positionSlider.setValue(currentPosition);
            updateDisplay();
        }
    }
    
    private void updateDisplay() {
        // Update time label
        long currentSec = TimeUnit.MILLISECONDS.toSeconds(currentPosition);
        long durationSec = TimeUnit.MILLISECONDS.toSeconds(duration);
        timeLabel.setText(String.format("%02d:%02d/%02d:%02d", 
            currentSec / 60, currentSec % 60,
            durationSec / 60, durationSec % 60));
        
        // Update subtitle display
        if (subtitles != null) {
            double currentSecDouble = currentPosition / 1000.0;
            for (SubtitleEntry entry : subtitles) {
                double start = entry.getStart().toMillis() / 1000.0;
                double end = entry.getEnd().toMillis() / 1000.0;
                if (start <= currentSecDouble && currentSecDouble <= end) {
                    subtitleDisplay.setText(entry.getText());
                    break;
                } else if (currentSecDouble > end) {
                    subtitleDisplay.setText("");
                }
            }
        }
    }
    
    public void dispose() {
        if (updateTimer != null) {
            updateTimer.stop();
        }
    }
}