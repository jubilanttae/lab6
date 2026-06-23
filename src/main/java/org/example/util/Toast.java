package org.example.util;

import javafx.animation.FadeTransition;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

public class Toast {
    public static void show(Stage ownerStage, String toastMsg) {
        Stage toastStage = new Stage();
        toastStage.initOwner(ownerStage);
        toastStage.setResizable(false);
        toastStage.initStyle(StageStyle.TRANSPARENT);

        Text text = new Text(toastMsg);
        text.setStyle("-fx-font-size: 14px; -fx-fill: white;");

        StackPane root = new StackPane(text);
        root.setStyle("-fx-background-color: rgba(50, 50, 50, 0.85); -fx-padding: 10px; -fx-background-radius: 20;");

        Scene scene = new Scene(root);
        scene.setFill(null);
        toastStage.setScene(scene);

        toastStage.setX(ownerStage.getX() + ownerStage.getWidth() / 2 - 100);
        toastStage.setY(ownerStage.getY() + ownerStage.getHeight() * 0.8);

        toastStage.show();

        FadeTransition fadeOut = new FadeTransition(Duration.millis(1500), root);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setDelay(Duration.millis(1000));
        fadeOut.setOnFinished(e -> toastStage.close());
        fadeOut.play();
    }
}