package org.example.model;

import javafx.scene.image.Image;
import java.io.File;

public class ImageModel {
    private Image originalImage;
    private Image processedImage;
    private File originalFile;
    private boolean isProcessed = false;

    public void clear() {
        this.originalImage = null;
        this.processedImage = null;
        this.originalFile = null;
        this.isProcessed = false;
    }

    public Image getOriginalImage() { return originalImage; }
    public void setOriginalImage(Image originalImage) { this.originalImage = originalImage; }

    public Image getProcessedImage() { return processedImage; }
    public void setProcessedImage(Image processedImage) { this.processedImage = processedImage; }

    public File getOriginalFile() { return originalFile; }
    public void setOriginalFile(File originalFile) { this.originalFile = originalFile; }

    public boolean isProcessed() { return isProcessed; }
    public void setProcessed(boolean processed) { isProcessed = processed; }
}