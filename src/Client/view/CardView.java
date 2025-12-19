package Client.view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class CardView extends VBox {
    private CardViewModel card;
    private double scaleFactor;
    private boolean isSelected;
    private Runnable onSelectCallback;

    public CardView(CardViewModel card, double scaleFactor, boolean isSelected, Runnable onSelectCallback) {
        this.card = card;
        this.scaleFactor = scaleFactor;
        this.isSelected = isSelected;
        this.onSelectCallback = onSelectCallback;
        createCardUI();
    }

    public CardView(CardViewModel card, double scaleFactor) {
        this(card, scaleFactor, false, null);
    }

    private void createCardUI() {
        final double ORIGINAL_WIDTH = 160;
        final double ORIGINAL_HEIGHT = 240;
        final double NEW_WIDTH = ORIGINAL_WIDTH * scaleFactor;
        final double NEW_HEIGHT = ORIGINAL_HEIGHT * scaleFactor;
        final double NEW_IMG_HEIGHT = 130 * scaleFactor;

        this.setAlignment(Pos.TOP_CENTER);
        this.setPrefSize(NEW_WIDTH, NEW_HEIGHT);
        this.setSpacing(5 * scaleFactor);
        this.setPadding(new Insets(5 * scaleFactor));

        updateCardStyle();
        this.setCursor(Cursor.HAND);

        // Gestion du clic
        this.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) {
                showSelectionPopup();
            }
        });

        // Hover effect
        this.setOnMouseEntered(e -> {
            if (!isSelected) {
                this.setStyle(getDefaultStyle() +
                        "-fx-effect: dropshadow(three-pass-box, rgba(255,255,255,0.5), 10, 0.5, 0, 0);");
            }
        });

        this.setOnMouseExited(e -> {
            updateCardStyle();
        });

        // --- EN-TÊTE ---
        HBox topRow = new HBox(5 * scaleFactor);
        topRow.setAlignment(Pos.CENTER);
        Label title = new Label(card.getName());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: " + (14 * scaleFactor * 0.7) + "pt; " +
                "-fx-text-fill: #333333; -fx-effect: dropshadow(gaussian, white, 2, 1.0, 0, 0);");

        HBox header = new HBox();
        HBox.setHgrow(header, Priority.ALWAYS);
        header.setAlignment(Pos.CENTER);
        header.getChildren().addAll(title);

        // --- IMAGE/ILLUSTRATION ---
        VBox imgPlaceholder = new VBox();
        imgPlaceholder.setPrefHeight(NEW_IMG_HEIGHT);
        imgPlaceholder.setAlignment(Pos.CENTER);
        imgPlaceholder.setStyle("-fx-background-color: #F5F5F5; -fx-background-radius: 0; " +
                "-fx-border-color: #333333; -fx-border-width: 1;");
        Label placeholderLabel = new Label("[Image de la carte]");
        placeholderLabel.setStyle("-fx-text-fill: #616161; -fx-font-size: " +
                (12 * scaleFactor * 0.7) + "pt;");
        imgPlaceholder.getChildren().add(placeholderLabel);
        VBox.setVgrow(imgPlaceholder, Priority.ALWAYS);

        // --- BARRE DE VIE ---
        int maxHp = card.getMaxHealth() > 0 ? card.getMaxHealth() : 100;
        StackPane healthBar = createHealthBar(card.getHealth(), maxHp, NEW_WIDTH - 10 * scaleFactor, scaleFactor);
        VBox.setMargin(healthBar, new Insets(0, 0, 5 * scaleFactor, 0));

        // --- STATISTIQUES ATTAQUE/DÉFENSE ---
        HBox statRow = new HBox(10 * scaleFactor);
        statRow.setAlignment(Pos.CENTER);
        statRow.getChildren().addAll(
                createStatBox("ATK", card.getAttack(), "#FF9800", scaleFactor),
                createStatBox("DEF", card.getDefense(), "#2196F3", scaleFactor)
        );

        this.getChildren().addAll(header, imgPlaceholder, healthBar, statRow);
    }

    public void updateCardStyle() {
        String style = getDefaultStyle();

        if (isSelected) {
            style += "-fx-effect: dropshadow(three-pass-box, gold, 15, 0.7, 0, 0); -fx-border-color: gold; -fx-border-width: 4;";
        }

        this.setStyle(style);
    }

    private String getDefaultStyle() {
        String backgroundColor = "#B0BEC5"; // Couleur neutre/commune (comme CAT 2)
        String borderColor = "#78909C";

        return "-fx-border-color: " + borderColor + ";" +
                "-fx-border-width: 4;" +
                "-fx-background-color: " + backgroundColor + ";" +
                "-fx-background-radius: 12;" +
                "-fx-border-radius: 8;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.8), 10, 0.5, 0, 0);";
    }

    private void showSelectionPopup() {
        final Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(this.getScene().getWindow());
        dialog.setTitle("Action sur la carte: " + card.getName());

        BorderPane mainDialogLayout = new BorderPane();
        mainDialogLayout.setPadding(new Insets(20));
        mainDialogLayout.setPrefSize(1000, 1000);

        mainDialogLayout.setStyle(
                getDefaultStyle() +
                        "-fx-border-radius: 0;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 20, 0, 0, 5);"
        );

        VBox topBox = new VBox(10);
        topBox.setAlignment(Pos.CENTER);
        Label title = new Label(card.getName().toUpperCase());
        title.setStyle("-fx-font-size: 20pt; -fx-font-weight: bold; -fx-text-fill: #333333;");

        mainDialogLayout.setTop(topBox);
        BorderPane.setMargin(topBox, new Insets(0, 0, 20, 0));

        // --- CENTER : Options ---
        VBox centerBox = new VBox(20);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(20));

        // Bouton qui change selon l'état
        Button btnToggleSelection = new Button();
        if (isSelected) {
            btnToggleSelection.setText("DÉSÉLECTIONNER");
            btnToggleSelection.setStyle("-fx-font-size: 14pt; -fx-background-color: #F44336; -fx-text-fill: white; " +
                    "-fx-padding: 15 30; -fx-background-radius: 10; -fx-font-weight: bold;");
        } else {
            btnToggleSelection.setText("SÉLECTIONNER");
            btnToggleSelection.setStyle("-fx-font-size: 14pt; -fx-background-color: #4CAF50; -fx-text-fill: white; " +
                    "-fx-padding: 15 30; -fx-background-radius: 10; -fx-font-weight: bold;");
        }

        btnToggleSelection.setOnAction(e -> {
            if (onSelectCallback != null) onSelectCallback.run();
            dialog.close();
        });

        Button btnDetails = new Button("VOIR LES DÉTAILS");
        btnDetails.setStyle("-fx-font-size: 14pt; -fx-background-color: #2196F3; -fx-text-fill: white; " +
                "-fx-padding: 15 30; -fx-background-radius: 10;");
        btnDetails.setOnAction(e -> {
            dialog.close();
            showCardDetailsPopup();
        });

        Button btnCancel = new Button("FERMER");
        btnCancel.setStyle("-fx-font-size: 14pt; -fx-background-color: #9E9E9E; -fx-text-fill: white; " +
                "-fx-padding: 15 30; -fx-background-radius: 10;");
        btnCancel.setOnAction(e -> {
            dialog.close();
        });

        centerBox.getChildren().addAll(btnToggleSelection, btnDetails, btnCancel);
        mainDialogLayout.setCenter(centerBox);

        Scene dialogScene = new Scene(mainDialogLayout);
        dialog.setScene(dialogScene);
        dialog.show();
    }

    private void showCardDetailsPopup() {
        final Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(this.getScene().getWindow());
        dialog.setTitle("Fiche Détaillée: " + card.getName());

        BorderPane mainDialogLayout = new BorderPane();
        mainDialogLayout.setPadding(new Insets(20));
        mainDialogLayout.setPrefSize(1000, 1000);

        mainDialogLayout.setStyle(
                getDefaultStyle() +
                        "-fx-border-radius: 0;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 20, 0, 0, 5);"
        );

        VBox topBox = new VBox(5);
        topBox.setAlignment(Pos.CENTER);
        Label title = new Label(card.getName().toUpperCase());
        title.setStyle("-fx-font-size: 24pt; -fx-font-weight: bold; -fx-text-fill: #333333; -fx-effect: dropshadow(gaussian, white, 2, 1.0, 0, 0);");
        Label idLabel = new Label("ID: " + card.getId() + " | Propriétaire: Carte");
        topBox.getChildren().addAll(title, idLabel);

        mainDialogLayout.setTop(topBox);
        BorderPane.setMargin(topBox, new Insets(0, 0, 15, 0));

        VBox centerBox = new VBox(15);
        centerBox.setAlignment(Pos.TOP_CENTER);
        centerBox.setStyle("-fx-background-color: rgba(255, 255, 255, 0.8); -fx-background-radius: 15; -fx-padding: 15;");

        VBox imgPlaceholder = new VBox();
        imgPlaceholder.setPrefSize(300, 300);
        imgPlaceholder.setAlignment(Pos.CENTER);
        imgPlaceholder.setStyle("-fx-border-color: black; -fx-border-width: 3; -fx-background-color: #EFEBE9; -fx-background-radius: 10; -fx-effect: innershadow(gaussian, rgba(0,0,0,0.2), 5, 0, 1, 1);");
        Label placeholderLabel = new Label("ILLUSTRATION MAX\n" + card.getName());
        placeholderLabel.setAlignment(Pos.CENTER);
        placeholderLabel.setStyle("-fx-font-size: 16pt; -fx-text-fill: #6D4C41;");
        imgPlaceholder.getChildren().add(placeholderLabel);

        int maxHp = card.getMaxHealth() > 0 ? card.getMaxHealth() : 100;
        StackPane healthBar = createHealthBar(card.getHealth(), maxHp, 300, 2.0);
        HBox healthBarWrapper = new HBox();
        healthBarWrapper.setAlignment(Pos.CENTER);
        healthBarWrapper.getChildren().add(healthBar);

        HBox statRow = new HBox(40);
        statRow.setAlignment(Pos.CENTER);
        statRow.getChildren().addAll(
                createStatBox("ATK", card.getAttack(), "#E65100", 1.8),
                createStatBox("DEF", card.getDefense(), "#0288D1", 1.8)
        );

        centerBox.getChildren().addAll(imgPlaceholder, healthBarWrapper, statRow);
        mainDialogLayout.setCenter(centerBox);

        VBox bottomBox = new VBox(10);
        bottomBox.setAlignment(Pos.CENTER);
        BorderPane.setMargin(bottomBox, new Insets(15, 0, 0, 0));

        Button closeButton = new Button("Fermer");
        closeButton.setStyle("-fx-font-size: 14pt; -fx-background-color: #607D8B; -fx-text-fill: white; -fx-padding: 8 20; -fx-background-radius: 8;");
        closeButton.setOnAction(e -> dialog.close());

        bottomBox.getChildren().addAll(closeButton);
        mainDialogLayout.setBottom(bottomBox);

        Scene dialogScene = new Scene(mainDialogLayout);
        dialog.setScene(dialogScene);
        dialog.show();
    }

    private StackPane createHealthBar(int currentHP, int maxHP, double barWidth, double scaleFactor) {
        double percentage = Math.max(0, Math.min(1.0, (double) currentHP / maxHP));

        javafx.scene.paint.Color fillColor;
        if (percentage > 0.7) fillColor = javafx.scene.paint.Color.web("#4CAF50");
        else if (percentage > 0.4) fillColor = javafx.scene.paint.Color.web("#FFEB3B");
        else if (percentage > 0.15) fillColor = javafx.scene.paint.Color.web("#FF9800");
        else fillColor = javafx.scene.paint.Color.web("#F44336");

        double height = 18 * scaleFactor;

        Rectangle background = new Rectangle(barWidth, height);
        background.setArcWidth(20);
        background.setArcHeight(20);
        background.setFill(javafx.scene.paint.Color.web("#BDBDBD"));
        background.setStroke(javafx.scene.paint.Color.BLACK);
        background.setStrokeWidth(1);

        double healthWidth = barWidth * percentage;
        Rectangle health = new Rectangle(healthWidth, height);
        health.setFill(fillColor);

        if (percentage >= 1.0) {
            health.setArcWidth(20);
            health.setArcHeight(20);
        } else {
            health.setArcWidth(20);
            health.setArcHeight(20);
        }

        StackPane bar = new StackPane();
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getChildren().addAll(background, health);

        Label hpLabel = new Label(currentHP + "/" + maxHP + " PV");
        hpLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: black; " +
                "-fx-font-size: " + (11 * scaleFactor) + "pt;");

        StackPane finalBar = new StackPane();
        finalBar.getChildren().addAll(bar, hpLabel);
        finalBar.setPrefWidth(barWidth);

        return finalBar;
    }

    private HBox createStatBox(String label, int value, String color, double scaleFactor) {
        Label statLabel = new Label(label + ": " + value);
        statLabel.setStyle("-fx-background-color: " + color + ";" +
                "-fx-text-fill: white;" +
                "-fx-padding: 2 6;" +
                "-fx-font-weight: bold;" +
                "-fx-font-size: " + (12 * scaleFactor * 0.7) + "pt;" +
                "-fx-background-radius: 5;");
        HBox box = new HBox(statLabel);
        box.setAlignment(Pos.CENTER);
        return box;
    }
}