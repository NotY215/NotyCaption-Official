package com.noty215.notycaption.ui;

import javafx.animation.*;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Label with glow effect animation
 */
public class GlowLabel extends Label {

    private final DropShadow shadow;
    private final Timeline glowTimeline;

    public GlowLabel(String text) {
        super(text);

        // Shadow effect
        shadow = new DropShadow();
        shadow.setRadius(10);
        shadow.setColor(Color.rgb(74, 144, 226, 0.5));
        setEffect(shadow);

        // Glow animation
        glowTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(shadow.radiusProperty(), 5, Interpolator.EASE_BOTH),
                        new KeyValue(shadow.colorProperty(), Color.rgb(74, 144, 226, 0.3), Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(1000), new KeyValue(shadow.radiusProperty(), 20, Interpolator.EASE_BOTH),
                        new KeyValue(shadow.colorProperty(), Color.rgb(74, 144, 226, 0.8), Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(2000), new KeyValue(shadow.radiusProperty(), 5, Interpolator.EASE_BOTH),
                        new KeyValue(shadow.colorProperty(), Color.rgb(74, 144, 226, 0.3), Interpolator.EASE_BOTH))
        );
        glowTimeline.setCycleCount(Timeline.INDEFINITE);
        glowTimeline.play();

        // Style
        setStyle("-fx-font-weight: bold; -fx-text-fill: #ffffff;");
    }

    public void setGlowIntensity(int intensity) {
        double radius = intensity / 5.0;

        KeyFrame frame1 = glowTimeline.getKeyFrames().get(0);
        KeyFrame frame2 = glowTimeline.getKeyFrames().get(1);

        glowTimeline.getKeyFrames().set(0,
                new KeyFrame(Duration.ZERO,
                        new KeyValue(shadow.radiusProperty(), radius / 2, Interpolator.EASE_BOTH),
                        new KeyValue(shadow.colorProperty(), shadow.getColor(), Interpolator.EASE_BOTH))
        );

        glowTimeline.getKeyFrames().set(1,
                new KeyFrame(Duration.millis(1000),
                        new KeyValue(shadow.radiusProperty(), radius, Interpolator.EASE_BOTH),
                        new KeyValue(shadow.colorProperty(), shadow.getColor(), Interpolator.EASE_BOTH))
        );
    }

    public void setGlowColor(String colorHex) {
        Color glowColor = Color.web(colorHex);
        shadow.setColor(glowColor);

        KeyFrame frame1 = glowTimeline.getKeyFrames().get(0);
        KeyFrame frame2 = glowTimeline.getKeyFrames().get(1);

        glowTimeline.getKeyFrames().set(0,
                new KeyFrame(Duration.ZERO,
                        new KeyValue(shadow.colorProperty(), glowColor.deriveColor(0, 1, 0.3, 1), Interpolator.EASE_BOTH))
        );

        glowTimeline.getKeyFrames().set(1,
                new KeyFrame(Duration.millis(1000),
                        new KeyValue(shadow.colorProperty(), glowColor, Interpolator.EASE_BOTH))
        );
    }
}