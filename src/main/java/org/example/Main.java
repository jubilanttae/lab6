package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.example.util.AppLogger;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            AppLogger.info("Uruchomienie aplikacji");

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/main_view.fxml"));
            BorderPane root = loader.load();

            primaryStage.setTitle("image app");
            primaryStage.setScene(new Scene(root));
            primaryStage.setMinWidth(900);
            primaryStage.setMinHeight(600);
            primaryStage.show();
        } catch (Exception e) {
            AppLogger.error("Błąd podczas uruchamiania aplikacji: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        AppLogger.info("Zamknięcie aplikacji");
    }

    public static void main(String[] args) {
        launch(args);
    }
}