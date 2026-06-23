# Image App

A JavaFX-based desktop application for image editing and processing.

## Features

- **Image Loading** - Load images from your file system and preview them
- **Image Rotation** - Rotate images 90° left or right
- **Image Scaling** - Resize images to custom dimensions
- **Thresholding** - Apply threshold effects to images with adjustable threshold values
- **Negative Effect** - Convert images to negative (inverted colors)
- **Image Export** - Save processed images to your Pictures folder
- **Application Logging** - All operations are logged with timestamps for debugging

## Technology Stack

- **Language**: Java 26
- **GUI Framework**: JavaFX 25.0.1
- **Build Tool**: Maven
- **Additional Dependencies**: 
  - javafx-controls
  - javafx-fxml
  - javafx-swing

## Project Structure

```
lab6/
├── src/main/java/org/example/
│   ├── Main.java                 # Application entry point
│   ├── controller/               # UI controller classes
│   ├── service/                  # Image processing services
│   └── util/                     # Utility classes (logging)
├── src/main/resources/
│   └── main_view.fxml           # JavaFX UI layout
├── pom.xml                       # Maven configuration
└── app_logs.txt                  # Application logs
```
