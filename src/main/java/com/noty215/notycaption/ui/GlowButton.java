package com.noty215.notycaption.ui;

import javafx.animation.*;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Button with glow effect animation
 */
public class GlowButton extends Button {

    private final DropShadow shadow;
    private final Timeline hoverTimeline;
    private final Timeline glowTimeline;

    public GlowButton(String text) {
        super(text);

        // Style
        setStyle("-fx-background-color: #4a6fa5; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-background-radius: 10; -fx-padding: 8 16;");

        // Shadow effect
        shadow = new DropShadow();
        shadow.setRadius(10);
        shadow.setColor(Color.rgb(0, 0, 0, 0.3));
        shadow.setOffsetY(2);
        setEffect(shadow);

        // Hover animation
        hoverTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(scaleXProperty(), 1, Interpolator.EASE_BOTH),
                        new KeyValue(scaleYProperty(), 1, Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(200), new KeyValue(scaleXProperty(), 1.02, Interpolator.EASE_BOTH),
                        new KeyValue(scaleYProperty(), 1.02, Interpolator.EASE_BOTH))
        );

        // Glow animation
        glowTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(shadow.radiusProperty(), 10, Interpolator.EASE_BOTH),
                        new KeyValue(shadow.colorProperty(), Color.rgb(0, 0, 0, 0.3), Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(500), new KeyValue(shadow.radiusProperty(), 20, Interpolator.EASE_BOTH),
                        new KeyValue(shadow.colorProperty(), Color.rgb(74, 144, 226, 0.5), Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(1000), new KeyValue(shadow.radiusProperty(), 10, Interpolator.EASE_BOTH),
                        new KeyValue(shadow.colorProperty(), Color.rgb(0, 0, 0, 0.3), Interpolator.EASE_BOTH))
        );
        glowTimeline.setCycleCount(Timeline.INDEFINITE);

        // Event handlers
        setOnMouseEntered(e -> {
            hoverTimeline.play();
            glowTimeline.play();
            setStyle("-fx-background-color: derive(#4a6fa5, 20%); -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-background-radius: 10; -fx-padding: 8 16;");
        });

        setOnMouseExited(e -> {
            hoverTimeline.setRate(-1);
            hoverTimeline.play();
            glowTimeline.stop();
            setStyle("-fx-background-color: #4a6fa5; -fx-text-fill: white; -fx-font-weight: bold; " +
                    "-fx-background-radius: 10; -fx-padding: 8 16;");
            shadow.setRadius(10);
            shadow.setColor(Color.rgb(0, 0, 0, 0.3));
        });

        setOnMousePressed(e -> {
            scaleXProperty().set(0.98);
            scaleYProperty().set(0.98);
        });

        setOnMouseReleased(e -> {
            scaleXProperty().set(1);
            scaleYProperty().set(1);
        });
    }

    public void setGlowColor(String colorHex) {
        Color glowColor = Color.web(colorHex);
        glowTimeline.getKeyFrames().set(1,
                new KeyFrame(Duration.millis(500), new KeyValue(shadow.colorProperty(), glowColor, Interpolator.EASE_BOTH))
        );
    }

    public void setGlowIntensity(int intensity) {
        double radius = intensity / 5.0;
        shadow.setRadius(radius);

        KeyFrame frame = glowTimeline.getKeyFrames().get(1);
        glowTimeline.getKeyFrames().set(1,
                new KeyFrame(Duration.millis(500),
                        new KeyValue(shadow.radiusProperty(), radius, Interpolator.EASE_BOTH),
                        new KeyValue(shadow.colorProperty(), shadow.getColor(), Interpolator.EASE_BOTH))
        );
    }
}