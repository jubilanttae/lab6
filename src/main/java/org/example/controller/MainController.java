package org.example.controller;

import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.example.model.ImageModel;
import org.example.util.AppLogger;
import org.example.util.Toast;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainController {

    @FXML private ImageView originalImageView;
    @FXML private ImageView processedImageView;
    @FXML private ComboBox<String> opComboBox;
    @FXML private Button btnExecute, btnSave, btnLoad;
    @FXML private Button btnRotateLeft, btnRotateRight, btnScale;

    private ImageModel model = new ImageModel();

    @FXML
    public void initialize() {
        opComboBox.getItems().addAll(" ", "Negatyw", "Progowanie", "Konturowanie");
        opComboBox.getSelectionModel().select(0);

        btnExecute.setDisable(true);
        btnSave.setDisable(true);
        btnRotateLeft.setDisable(true);
        btnRotateRight.setDisable(true);
        btnScale.setDisable(true);
    }

    private Image getCurrentSourceImage() {
        return (model.isProcessed() && model.getProcessedImage() != null)
                ? model.getProcessedImage() : model.getOriginalImage();
    }

    private void updateProcessedImage(Image newImage, String operationName) {
        model.setProcessed(true);
        model.setProcessedImage(newImage);
        processedImageView.setImage(newImage);
        AppLogger.info("Zaktualizowano podgląd po operacji: " + operationName);
    }

    @FXML
    private void handleLoadImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Wybierz plik obrazu");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Obrazy JPG", "*.jpg", "*.JPG"));

        Stage stage = (Stage) btnLoad.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            model.clear();
            try {
                if (!selectedFile.getName().toLowerCase().endsWith(".jpg")) {
                    AppLogger.warn("Próba wczytania pliku o niedozwolonym rozszerzeniu: " + selectedFile.getName());
                    Toast.show(stage, "Niedozwolony format pliku");
                    return;
                }
                Image img = new Image(selectedFile.toURI().toString());
                if (img.isError()) throw new Exception("Błąd ładowania obrazu");

                model.setOriginalImage(img);
                model.setOriginalFile(selectedFile);
                originalImageView.setImage(img);
                processedImageView.setImage(img);

                btnExecute.setDisable(false);
                btnSave.setDisable(false);
                btnRotateLeft.setDisable(false);
                btnRotateRight.setDisable(false);
                btnScale.setDisable(false);

                AppLogger.info("Pomyślnie załadowano plik: " + selectedFile.getAbsolutePath());
                Toast.show(stage, "Pomyślnie załadowano plik");
            } catch (Exception e) {
                AppLogger.error("Nie udało się załadować pliku " + selectedFile.getName() + ": " + e.getMessage());
                Toast.show(stage, "Nie udało się załadować pliku");
            }
        }
    }

    @FXML
    private void handleExecute(ActionEvent event) {
        String selectedOp = opComboBox.getSelectionModel().getSelectedItem();
        Stage stage = (Stage) btnExecute.getScene().getWindow();

        if (selectedOp == null || selectedOp.equals(" ")) {
            AppLogger.warn("Próba wykonania operacji bez wyboru z listy.");
            Toast.show(stage, "Nie wybrano operacji do wykonania");
            return;
        }

        AppLogger.info("Rozpoczęto operację: " + selectedOp);
        try {
            Image source = getCurrentSourceImage();

            switch (selectedOp) {
                case "Negatyw":
                    updateProcessedImage(applyNegative(source), "Negatyw");
                    Toast.show(stage, "Negatyw został wygenerowany pomyślnie!");
                    break;
                case "Progowanie":
                    openThresholdModal(source, stage);
                    break;
                case "Konturowanie":
                    updateProcessedImage(applyContour(source), "Konturowanie");
                    Toast.show(stage, "Konturowanie zostało przeprowadzone pomyślnie!");
                    break;
            }
        } catch (Exception e) {
            AppLogger.error("Błąd podczas operacji " + selectedOp + ": " + e.getMessage());
            Toast.show(stage, "Nie udało się wykonać operacji: " + selectedOp.toLowerCase() + ".");
        }
    }

    private interface PixelOperation {
        void process(int[] srcBuffer, int[] dstBuffer, int w, int h, int startY, int endY);
    }

    private Image processParallel(Image img, PixelOperation op) throws Exception {
        int w = (int) img.getWidth();
        int h = (int) img.getHeight();
        WritableImage out = new WritableImage(w, h);
        PixelReader pr = img.getPixelReader();

        int[] srcBuffer = new int[w * h];
        int[] dstBuffer = new int[w * h];
        pr.getPixels(0, 0, w, h, javafx.scene.image.WritablePixelFormat.getIntArgbInstance(), srcBuffer, 0, w);

        int threads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        int chunkHeight = h / threads;
        List<Callable<Void>> tasks = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final int startY = i * chunkHeight;
            final int endY = (i == threads - 1) ? h : (startY + chunkHeight);

            tasks.add(() -> {
                op.process(srcBuffer, dstBuffer, w, h, startY, endY);
                return null;
            });
        }

        executor.invokeAll(tasks);
        executor.shutdown();

        out.getPixelWriter().setPixels(0, 0, w, h, javafx.scene.image.WritablePixelFormat.getIntArgbInstance(), dstBuffer, 0, w);
        return out;
    }

    private Image applyNegative(Image img) throws Exception {
        return processParallel(img, (src, dst, w, h, startY, endY) -> {
            for (int y = startY; y < endY; y++) {
                for (int x = 0; x < w; x++) {
                    int idx = y * w + x;
                    int argb = src[idx];
                    int a = (argb >> 24) & 0xff;
                    int r = 255 - ((argb >> 16) & 0xff);
                    int g = 255 - ((argb >> 8) & 0xff);
                    int b = 255 - (argb & 0xff);
                    dst[idx] = (a << 24) | (r << 16) | (g << 8) | b;
                }
            }
        });
    }

    private Image applyContour(Image img) throws Exception {
        return processParallel(img, (src, dst, w, h, startY, endY) -> {
            for (int y = startY; y < endY; y++) {
                for (int x = 0; x < w; x++) {
                    int idx1 = y * w + x;
                    int nx = Math.min(x + 1, w - 1);
                    int ny = Math.min(y + 1, h - 1);
                    int idx2 = ny * w + nx;

                    int argb1 = src[idx1];
                    double gray1 = (((argb1 >> 16) & 0xff) + ((argb1 >> 8) & 0xff) + (argb1 & 0xff)) / 3.0;

                    int argb2 = src[idx2];
                    double gray2 = (((argb2 >> 16) & 0xff) + ((argb2 >> 8) & 0xff) + (argb2 & 0xff)) / 3.0;

                    double diff = Math.abs(gray1 - gray2);
                    int val = (diff > 20.4) ? 0 : 255;
                    int a = (argb1 >> 24) & 0xff;
                    dst[idx1] = (a << 24) | (val << 16) | (val << 8) | val;
                }
            }
        });
    }

    private void openThresholdModal(Image source, Stage mainStage) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Wybierz wartość progu");

        VBox layout = new VBox(10);
        layout.setStyle("-fx-padding: 20; -fx-alignment: center;");

        TextField tfThreshold = new TextField();
        tfThreshold.setPromptText("Próg (0-255)");

        tfThreshold.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.matches("\\d*")) tfThreshold.setText(newV.replaceAll("[^\\d]", ""));
            if (!tfThreshold.getText().isEmpty() && Integer.parseInt(tfThreshold.getText()) > 255) {
                tfThreshold.setText("255");
            }
        });

        Button btnApply = new Button("Wykonaj progowanie");
        Button btnCancel = new Button("Anuluj");
        btnCancel.setOnAction(e -> stage.close());

        btnApply.setOnAction(e -> {
            if (tfThreshold.getText().isEmpty()) return;
            try {
                int threshold = Integer.parseInt(tfThreshold.getText());
                Image result = processParallel(source, (src, dst, w, h, startY, endY) -> {
                    for (int y = startY; y < endY; y++) {
                        for (int x = 0; x < w; x++) {
                            int idx = y * w + x;
                            int argb = src[idx];
                            int a = (argb >> 24) & 0xff;
                            int r = (argb >> 16) & 0xff;
                            int g = (argb >> 8) & 0xff;
                            int b = argb & 0xff;
                            double gray = (r + g + b) / 3.0;
                            int val = (gray >= threshold) ? 255 : 0;
                            dst[idx] = (a << 24) | (val << 16) | (val << 8) | val;
                        }
                    }
                });

                updateProcessedImage(result, "Progowanie (próg: " + threshold + ")");
                stage.close();
                Toast.show(mainStage, "Progowanie zostało przeprowadzone pomyślnie!");
            } catch (Exception ex) {
                AppLogger.error("Błąd podczas progowania: " + ex.getMessage());
                stage.close();
                Toast.show(mainStage, "Nie udało się wykonać progowania.");
            }
        });

        HBox btns = new HBox(10, btnApply, btnCancel);
        btns.setStyle("-fx-alignment: center;");
        layout.getChildren().addAll(new Label("Wartość progu (0-255):"), tfThreshold, btns);

        stage.setScene(new Scene(layout, 350, 150));
        stage.showAndWait();
    }


    @FXML
    private void handleRotateLeft(ActionEvent event) {
        AppLogger.info("Wykonano obrót o 90 stopni w lewo.");
        updateProcessedImage(rotateImage(getCurrentSourceImage(), false), "Obrót w lewo");
    }

    @FXML
    private void handleRotateRight(ActionEvent event) {
        AppLogger.info("Wykonano obrót o 90 stopni w prawo.");
        updateProcessedImage(rotateImage(getCurrentSourceImage(), true), "Obrót w prawo");
    }

    private Image rotateImage(Image img, boolean right) {
        int w = (int) img.getWidth();
        int h = (int) img.getHeight();
        WritableImage rotated = new WritableImage(h, w);
        PixelReader pr = img.getPixelReader();
        PixelWriter pw = rotated.getPixelWriter();

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (right) {
                    pw.setArgb(h - 1 - y, x, pr.getArgb(x, y));
                } else {
                    pw.setArgb(y, w - 1 - x, pr.getArgb(x, y));
                }
            }
        }
        return rotated;
    }

    @FXML
    private void handleScaleImage(ActionEvent event) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Skalowanie obrazu");

        VBox layout = new VBox(10);
        layout.setStyle("-fx-padding: 20; -fx-alignment: center;");

        TextField tfWidth = new TextField();
        tfWidth.setPromptText("Szerokość (0-3000)");
        Label errW = new Label(); errW.setStyle("-fx-text-fill: red;");

        TextField tfHeight = new TextField();
        tfHeight.setPromptText("Wysokość (0-3000)");
        Label errH = new Label(); errH.setStyle("-fx-text-fill: red;");

        setupNumberInput(tfWidth);
        setupNumberInput(tfHeight);

        Button btnApply = new Button("Zmień rozmiar");
        Button btnCancel = new Button("Anuluj");
        Button btnReset = new Button("Przywróć oryginalne");

        btnCancel.setOnAction(e -> stage.close());

        btnReset.setOnAction(e -> {
            AppLogger.info("Przywrócono oryginalne wymiary obrazu.");
            updateProcessedImage(model.getOriginalImage(), "Reset skalowania");
            stage.close();
            Toast.show((Stage) btnScale.getScene().getWindow(), "Przywrócono oryginalne wymiary");
        });

        btnApply.setOnAction(e -> {
            boolean valid = true;
            if (tfWidth.getText().isEmpty()) { errW.setText("Pole jest wymagane"); valid = false; } else { errW.setText(""); }
            if (tfHeight.getText().isEmpty()) { errH.setText("Pole jest wymagane"); valid = false; } else { errH.setText(""); }

            if (valid) {
                int newW = Integer.parseInt(tfWidth.getText());
                int newH = Integer.parseInt(tfHeight.getText());

                Canvas canvas = new Canvas(newW, newH);
                GraphicsContext gc = canvas.getGraphicsContext2D();
                gc.drawImage(getCurrentSourceImage(), 0, 0, newW, newH);

                SnapshotParameters params = new SnapshotParameters();
                params.setFill(Color.TRANSPARENT);

                AppLogger.info(String.format("Przeskalowano obraz do wymiarów %dx%d.", newW, newH));
                updateProcessedImage(canvas.snapshot(params, null), "Skalowanie");

                stage.close();
                Toast.show((Stage) btnScale.getScene().getWindow(), "Obraz został przeskalowany.");
            }
        });

        HBox btns = new HBox(10, btnApply, btnReset, btnCancel);
        btns.setStyle("-fx-alignment: center;");
        layout.getChildren().addAll(new Label("Nowa szerokość:"), tfWidth, errW, new Label("Nowa wysokość:"), tfHeight, errH, btns);

        stage.setScene(new Scene(layout, 400, 300));
        stage.showAndWait();
    }

    private void setupNumberInput(TextField tf) {
        tf.textProperty().addListener((obs, oldV, newV) -> {
            if (!newV.matches("\\d*")) tf.setText(newV.replaceAll("[^\\d]", ""));
            if (!tf.getText().isEmpty()) {
                int val = Integer.parseInt(tf.getText());
                if (val > 3000) tf.setText("3000");
            }
        });
    }

    @FXML
    private void handleSaveImage(ActionEvent event) {
        Stage mainWindowStage = (Stage) btnSave.getScene().getWindow();
        Stage modalStage = new Stage();
        modalStage.initModality(Modality.APPLICATION_MODAL);
        modalStage.initOwner(mainWindowStage);
        modalStage.setTitle("Zapisz plik");

        VBox layout = new VBox(15);
        layout.setStyle("-fx-padding: 20; -fx-alignment: center;");

        if (!model.isProcessed()) {
            Label alertLabel = new Label("Na pliku nie zostały wykonane żadne operacje!");
            alertLabel.setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
            layout.getChildren().add(alertLabel);
        }

        TextField nameField = new TextField();
        nameField.setPromptText("Wpisz nazwę pliku (3-100 znaków)");
        nameField.setPrefWidth(250);
        nameField.textProperty().addListener((obs, oldV, newV) -> {
            if (newV.length() > 100) nameField.setText(oldV);
        });

        Label errorLabel = new Label(); errorLabel.setStyle("-fx-text-fill: red;");
        Button btnConfirmSave = new Button("Zapisz");
        Button btnCancel = new Button("Anuluj");

        btnCancel.setOnAction(e -> modalStage.close());

        btnConfirmSave.setOnAction(e -> {
            String fileName = nameField.getText().trim();
            if (fileName.length() < 3) {
                errorLabel.setText("Wpisz co najmniej 3 znaki"); return;
            }
            if (!fileName.toLowerCase().endsWith(".jpg")) fileName += ".jpg";

            File outputFile = new File(new File(System.getProperty("user.home"), "Pictures"), fileName);

            if (outputFile.exists()) {
                AppLogger.warn("Próba zapisu odrzucona - plik " + fileName + " już istnieje.");
                Toast.show(modalStage, "Plik " + fileName + " już istnieje w systemie. Podaj inną nazwę pliku!");
                return;
            }

            try {
                Image imageToSave = getCurrentSourceImage();
                BufferedImage bImage = SwingFXUtils.fromFXImage(imageToSave, null);

                BufferedImage rgbImage = new BufferedImage(bImage.getWidth(), bImage.getHeight(), BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D g = rgbImage.createGraphics();
                g.drawImage(bImage, 0, 0, java.awt.Color.WHITE, null);
                g.dispose();

                if (ImageIO.write(rgbImage, "jpg", outputFile)) {
                    AppLogger.info("Sukces: Zapisano obraz jako " + outputFile.getAbsolutePath());
                    Toast.show(mainWindowStage, "Zapisano obraz w pliku " + fileName);
                    modalStage.close();
                } else {
                    throw new Exception("Błąd wejścia/wyjścia systemu");
                }
            } catch (Exception ex) {
                AppLogger.error("Błąd podczas zapisu pliku " + fileName + ": " + ex.getMessage());
                Toast.show(modalStage, "Nie udało się zapisać pliku " + fileName);
            }
        });

        HBox buttons = new HBox(10, btnConfirmSave, btnCancel);
        buttons.setStyle("-fx-alignment: center;");
        layout.getChildren().addAll(new Label("Nazwa pliku:"), nameField, errorLabel, buttons);

        modalStage.setScene(new Scene(layout, 400, 250));
        modalStage.showAndWait();
    }
}