package view;

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
        this.setPadding(new Insets(5));

        // Mise à jour du style (plus de catégorie)
        updateCardStyle();

        this.setCursor(Cursor.HAND);
        this.setOnMouseClicked(e -> showSelectionPopup());

        // --- EN-TÊTE ---
        HBox topRow = new HBox(5);
        topRow.setAlignment(Pos.CENTER);

        Label title = new Label(card.getName());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: " + (11 * scaleFactor) + "pt; -fx-text-fill: #333333;");

        BorderPane header = new BorderPane();
        header.setCenter(title);
        // Le badge "Niv." a été supprimé ici

        // --- IMAGE ---
        VBox imgPlaceholder = new VBox();
        imgPlaceholder.setPrefHeight(NEW_IMG_HEIGHT);
        imgPlaceholder.setAlignment(Pos.CENTER);
        imgPlaceholder.setStyle("-fx-background-color: #F5F5F5; -fx-border-color: #333; -fx-border-width: 1;");
        imgPlaceholder.getChildren().add(new Label("IMG"));
        VBox.setVgrow(imgPlaceholder, Priority.ALWAYS);

        // --- BARRE DE VIE ---
        // Utilisation de maxHealth (100 par défaut si non défini dans le ViewModel)
        int maxHp = card.getMaxHealth() > 0 ? card.getMaxHealth() : 100;
        StackPane healthBar = createHealthBar(card.getHealth(), maxHp, NEW_WIDTH - (15*scaleFactor), scaleFactor);

        // --- STATS ---
        HBox statRow = new HBox(10 * scaleFactor);
        statRow.setAlignment(Pos.CENTER);
        statRow.getChildren().addAll(
                createStatBox("ATK", card.getAttack(), "#FF9800", scaleFactor),
                createStatBox("DEF", card.getDefense(), "#2196F3", scaleFactor)
        );

        this.getChildren().addAll(header, imgPlaceholder, healthBar, statRow);
    }

    private void updateCardStyle() {
        String style = getDefaultStyle();
        if (isSelected) {
            style += "-fx-effect: dropshadow(three-pass-box, gold, 15, 0.7, 0, 0); -fx-border-color: gold; -fx-border-width: 4;";
        }
        this.setStyle(style);
    }

    // --- POPUP D'ACTION ---
    private void showSelectionPopup() {
        final Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(this.getScene().getWindow());
        dialog.setTitle("Action : " + card.getName());

        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(20));
        layout.setPrefSize(350, 250);
        layout.setStyle(getDefaultStyle() + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 0);");

        Label title = new Label(card.getName().toUpperCase());
        title.setStyle("-fx-font-size: 18pt; -fx-font-weight: bold; -fx-text-fill: #333;");
        BorderPane.setAlignment(title, Pos.CENTER);
        layout.setTop(title);

        VBox centerBox = new VBox(15);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(20));

        Button btnSelect = new Button(isSelected ? "DÉSÉLECTIONNER" : "SÉLECTIONNER");
        String color = isSelected ? "#F44336" : "#4CAF50";
        btnSelect.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-size: 12pt; -fx-font-weight: bold; -fx-padding: 10 20;");
        btnSelect.setMaxWidth(Double.MAX_VALUE);

        btnSelect.setOnAction(e -> {
            if (onSelectCallback != null) onSelectCallback.run();
            dialog.close();
        });

        Button btnDetails = new Button("📋 VOIR DÉTAILS");
        btnDetails.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 12pt; -fx-padding: 10 20;");
        btnDetails.setMaxWidth(Double.MAX_VALUE);
        btnDetails.setOnAction(e -> {
            dialog.close();
            showCardDetailsPopup();
        });

        Button btnCancel = new Button("Fermer");
        btnCancel.setOnAction(e -> dialog.close());

        centerBox.getChildren().addAll(btnSelect, btnDetails, btnCancel);
        layout.setCenter(centerBox);

        Scene scene = new Scene(layout);
        dialog.setScene(scene);
        dialog.show();
    }

    // --- POPUP DÉTAILS ---
    private void showCardDetailsPopup() {
        final Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(this.getScene().getWindow());
        dialog.setTitle("Fiche : " + card.getName());

        VBox layout = new VBox(20);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setPadding(new Insets(20));
        layout.setStyle(getDefaultStyle());
        layout.setPrefSize(400, 550);

        Label title = new Label(card.getName().toUpperCase());
        title.setStyle("-fx-font-size: 24pt; -fx-font-weight: bold; -fx-text-fill: #333;");

        VBox imgBox = new VBox();
        imgBox.setPrefSize(250, 200);
        imgBox.setAlignment(Pos.CENTER);
        imgBox.setStyle("-fx-background-color: white; -fx-border-color: black;");
        imgBox.getChildren().add(new Label("ILLUSTRATION"));

        HBox stats = new HBox(30);
        stats.setAlignment(Pos.CENTER);
        stats.getChildren().addAll(
                createStatBox("ATK", card.getAttack(), "#E65100", 1.5),
                createStatBox("DEF", card.getDefense(), "#0288D1", 1.5)
        );

        // Utilisation de maxHealth ici aussi
        int maxHp = card.getMaxHealth() > 0 ? card.getMaxHealth() : 100;
        StackPane hpBar = createHealthBar(card.getHealth(), maxHp, 300, 1.5);

        Button btnClose = new Button("Fermer");
        btnClose.setStyle("-fx-font-size: 14pt;");
        btnClose.setOnAction(e -> dialog.close());

        layout.getChildren().addAll(title, imgBox, hpBar, stats, btnClose);
        dialog.setScene(new Scene(layout));
        dialog.show();
    }

    // --- OUTILS GRAPHIQUES ---

    // Style unique par défaut (Gris bleuté neutre)
    private String getDefaultStyle() {
        String bg = "#CFD8DC";
        String border = "#607D8B";
        return "-fx-background-color: " + bg + "; -fx-border-color: " + border + "; -fx-border-width: 3; -fx-background-radius: 10; -fx-border-radius: 8;";
    }

    private StackPane createHealthBar(int current, int max, double width, double scale) {
        double pct = Math.max(0, Math.min(1.0, (double) current / max));
        Rectangle bg = new Rectangle(width, 10 * scale, javafx.scene.paint.Color.web("#BDBDBD"));
        bg.setArcWidth(10); bg.setArcHeight(10);

        Rectangle fg = new Rectangle(width * pct, 10 * scale,
                pct > 0.5 ? javafx.scene.paint.Color.web("#4CAF50") : javafx.scene.paint.Color.web("#F44336"));
        fg.setArcWidth(10); fg.setArcHeight(10);

        StackPane p = new StackPane(bg, fg);
        p.setAlignment(Pos.CENTER_LEFT);
        Label l = new Label(current + " HP");
        l.setStyle("-fx-font-weight: bold; -fx-font-size: " + (8 * scale) + "pt;");
        return new StackPane(p, l);
    }

    private HBox createStatBox(String l, int v, String c, double s) {
        Label lbl = new Label(l + ": " + v);
        lbl.setStyle("-fx-background-color: " + c + "; -fx-text-fill: white; -fx-padding: 3 6; -fx-font-weight: bold; -fx-font-size: " + (9 * s) + "pt; -fx-background-radius: 4;");
        return new HBox(lbl);
    }
}