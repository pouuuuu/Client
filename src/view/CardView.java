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
    // Action à exécuter quand l'utilisateur confirme la sélection dans la popup
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

        // Simulation de catégorie pour la couleur (basé sur l'ID)
        int category = (card.getId() % 5) + 1;
        updateCardStyle(category);

        this.setCursor(Cursor.HAND);

        // --- GESTION DU CLIC : Ouvre la Popup d'action ---
        this.setOnMouseClicked(e -> showSelectionPopup(category));

        // --- EN-TÊTE ---
        HBox topRow = new HBox(5);
        topRow.setAlignment(Pos.CENTER);

        Label title = new Label(card.getName());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: " + (11 * scaleFactor) + "pt; " +
                "-fx-text-fill: #333333;");

        // Badge Catégorie
        Label catBadge = new Label("Niv." + category);
        catBadge.setStyle("-fx-background-color: rgba(0,0,0,0.1); -fx-text-fill: #333; " +
                "-fx-padding: 2 5; -fx-font-size: " + (8 * scaleFactor) + "pt; -fx-background-radius: 5;");

        BorderPane header = new BorderPane();
        header.setCenter(title);
        header.setRight(catBadge);

        // --- IMAGE ---
        VBox imgPlaceholder = new VBox();
        imgPlaceholder.setPrefHeight(NEW_IMG_HEIGHT);
        imgPlaceholder.setAlignment(Pos.CENTER);
        imgPlaceholder.setStyle("-fx-background-color: #F5F5F5; -fx-border-color: #333; -fx-border-width: 1;");
        Label placeholderLabel = new Label("IMG");
        imgPlaceholder.getChildren().add(placeholderLabel);
        VBox.setVgrow(imgPlaceholder, Priority.ALWAYS);

        // --- BARRE DE VIE ---
        // On suppose PV Max = 100 ou 200 par défaut si non fourni dans le ViewModel, ou on prend HP actuel comme base
        StackPane healthBar = createHealthBar(card.getHealth(), 200, NEW_WIDTH - (15*scaleFactor), scaleFactor);

        // --- STATS ---
        HBox statRow = new HBox(10 * scaleFactor);
        statRow.setAlignment(Pos.CENTER);
        statRow.getChildren().addAll(
                createStatBox("ATK", card.getAttack(), "#FF9800", scaleFactor),
                createStatBox("DEF", card.getDefense(), "#2196F3", scaleFactor)
        );

        this.getChildren().addAll(header, imgPlaceholder, healthBar, statRow);
    }

    private void updateCardStyle(int category) {
        String style = getCategoryStyle(category);
        if (isSelected) {
            // Effet brillant si sélectionné
            style += "-fx-effect: dropshadow(three-pass-box, gold, 15, 0.7, 0, 0); -fx-border-color: gold; -fx-border-width: 4;";
        }
        this.setStyle(style);
    }

    // --- POPUP D'ACTION (Sélectionner / Détails) ---
    private void showSelectionPopup(int category) {
        final Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(this.getScene().getWindow());
        dialog.setTitle("Action : " + card.getName());

        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(20));
        layout.setPrefSize(350, 250);
        layout.setStyle(getCategoryStyle(category) + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 0);");

        Label title = new Label(card.getName().toUpperCase());
        title.setStyle("-fx-font-size: 18pt; -fx-font-weight: bold; -fx-text-fill: #333;");
        BorderPane.setAlignment(title, Pos.CENTER);
        layout.setTop(title);

        VBox centerBox = new VBox(15);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setPadding(new Insets(20));

        // Bouton SÉLECTIONNER
        Button btnSelect = new Button(isSelected ? "DÉSÉLECTIONNER" : "SÉLECTIONNER");
        String color = isSelected ? "#F44336" : "#4CAF50"; // Rouge si désélection, Vert si sélection
        btnSelect.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-size: 12pt; -fx-font-weight: bold; -fx-padding: 10 20;");
        btnSelect.setMaxWidth(Double.MAX_VALUE);

        btnSelect.setOnAction(e -> {
            if (onSelectCallback != null) onSelectCallback.run(); // Déclenche la logique dans GameBoardView
            dialog.close();
        });

        // Bouton DÉTAILS
        Button btnDetails = new Button("📋 VOIR DÉTAILS");
        btnDetails.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 12pt; -fx-padding: 10 20;");
        btnDetails.setMaxWidth(Double.MAX_VALUE);
        btnDetails.setOnAction(e -> {
            dialog.close();
            showCardDetailsPopup(category);
        });

        Button btnCancel = new Button("Fermer");
        btnCancel.setOnAction(e -> dialog.close());

        centerBox.getChildren().addAll(btnSelect, btnDetails, btnCancel);
        layout.setCenter(centerBox);

        Scene scene = new Scene(layout);
        dialog.setScene(scene);
        dialog.show();
    }

    // --- POPUP DÉTAILS (Grande Fiche) ---
    private void showCardDetailsPopup(int category) {
        final Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(this.getScene().getWindow());
        dialog.setTitle("Fiche : " + card.getName());

        VBox layout = new VBox(20);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setPadding(new Insets(20));
        layout.setStyle(getCategoryStyle(category)); // Fond coloré
        layout.setPrefSize(400, 550);

        Label title = new Label(card.getName().toUpperCase());
        title.setStyle("-fx-font-size: 24pt; -fx-font-weight: bold; -fx-text-fill: #333;");

        // Grande Image Placeholder
        VBox imgBox = new VBox();
        imgBox.setPrefSize(250, 200);
        imgBox.setAlignment(Pos.CENTER);
        imgBox.setStyle("-fx-background-color: white; -fx-border-color: black;");
        imgBox.getChildren().add(new Label("ILLUSTRATION"));

        // Stats en gros
        HBox stats = new HBox(30);
        stats.setAlignment(Pos.CENTER);
        stats.getChildren().addAll(
                createStatBox("ATK", card.getAttack(), "#E65100", 1.5),
                createStatBox("DEF", card.getDefense(), "#0288D1", 1.5)
        );

        // Barre de vie large
        StackPane hpBar = createHealthBar(card.getHealth(), 200, 300, 1.5);

        Button btnClose = new Button("Fermer");
        btnClose.setStyle("-fx-font-size: 14pt;");
        btnClose.setOnAction(e -> dialog.close());

        layout.getChildren().addAll(title, imgBox, hpBar, stats, btnClose);
        dialog.setScene(new Scene(layout));
        dialog.show();
    }

    // --- OUTILS GRAPHIQUES (Styles) ---

    private String getCategoryStyle(int category) {
        String bg, border;
        switch (category) {
            case 1: bg = "#8BC34A"; border = "#4CAF50"; break; // Nature
            case 2: bg = "#B0BEC5"; border = "#78909C"; break; // Neutre
            case 3: bg = "#FF8A65"; border = "#F44336"; break; // Feu
            case 4: bg = "#9C27B0"; border = "#673AB7"; break; // Dark
            case 5: bg = "#4FC3F7"; border = "#03A9F4"; break; // Ice
            default: bg = "#CFD8DC"; border = "#607D8B"; break;
        }
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