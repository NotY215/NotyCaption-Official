package com.noty215.notycaption.ui;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

/**
 * Glass-like card widget with hover animation
 */
public class GlassCardWidget extends VBox {

    private final DropShadow shadow;
    private final Timeline hoverTimeline;

    public GlassCardWidget() {
        setPadding(new Insets(20));
        setSpacing(10);

        // Style
        setStyle("-fx-background-color: linear-gradient(to bottom right, rgba(30,30,30,0.8), rgba(20,20,20,0.9)); " +
                "-fx-border-color: #333; -fx-border-radius: 15; -fx-background-radius: 15;");

        // Shadow effect
        shadow = new DropShadow();
        shadow.setRadius(20);
        shadow.setColor(Color.rgb(0, 0, 0, 0.2));
        shadow.setOffsetY(5);
        setEffect(shadow);

        // Hover animation
        hoverTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(scaleXProperty(), 1, Interpolator.EASE_BOTH),
                        new KeyValue(scaleYProperty(), 1, Interpolator.EASE_BOTH),
                        new KeyValue(shadow.radiusProperty(), 20, Interpolator.EASE_BOTH),
                        new KeyValue(shadow.colorProperty(), Color.rgb(0, 0, 0, 0.2), Interpolator.EASE_BOTH)),
                new KeyFrame(Duration.millis(300),
                        new KeyValue(scaleXProperty(), 1.02, Interpolator.EASE_BOTH),
                        new KeyValue(scaleYProperty(), 1.02, Interpolator.EASE_BOTH),
                        new KeyValue(shadow.radiusProperty(), 30, Interpolator.EASE_BOTH),
                        new KeyValue(shadow.colorProperty(), Color.rgb(74, 144, 226, 0.12), Interpolator.EASE_BOTH))
        );

        setOnMouseEntered(e -> hoverTimeline.play());
        setOnMouseExited(e -> {
            hoverTimeline.setRate(-1);
            hoverTimeline.play();
        });
    }

    public void setCardOpacity(int opacity) {
        double alpha = opacity / 100.0;
        setStyle("-fx-background-color: linear-gradient(to bottom right, rgba(30,30,30," + alpha + "), " +
                "rgba(20,20,20," + alpha + ")); -fx-border-color: #333; -fx-border-radius: 15; -fx-background-radius: 15;");
    }

    public void setAccentColor(String colorHex) {
        setOnMouseEntered(e -> {
            shadow.setColor(Color.web(colorHex, 0.12));
        });
        setOnMouseExited(e -> {
            shadow.setColor(Color.rgb(0, 0, 0, 0.2));
        });
    }
}