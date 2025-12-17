package view;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;

public class CardView extends VBox {
    private CardViewModel card;
    private double scaleFactor;

    public CardView(CardViewModel card) {
        this(card, 1.0); // Échelle par défaut
    }

    public CardView(CardViewModel card, double scaleFactor) {
        this.card = card;
        this.scaleFactor = scaleFactor;
        createCardUI();
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

        // STYLE STANDARD (Pas de catégories)
        // Fond gris clair, bordure gris foncé
        this.setStyle(
                "-fx-background-color: #ECEFF1;" +
                        "-fx-border-color: #546E7A;" +
                        "-fx-border-width: 2;" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-radius: 8;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 5, 0, 0, 1);"
        );
        this.setCursor(Cursor.HAND);

        // --- EN-TÊTE (Nom + ID discret) ---
        BorderPane header = new BorderPane();

        Label title = new Label(card.getName());
        title.setStyle("-fx-font-weight: bold; -fx-font-size: " + (11 * scaleFactor) + "pt; -fx-text-fill: #263238;");

        Label idLbl = new Label("#" + card.getId());
        idLbl.setStyle("-fx-text-fill: #78909C; -fx-font-size: " + (8 * scaleFactor) + "pt;");

        header.setCenter(title);
        header.setRight(idLbl);

        // --- IMAGE PLACEHOLDER ---
        VBox imgPlaceholder = new VBox();
        imgPlaceholder.setPrefHeight(NEW_IMG_HEIGHT);
        imgPlaceholder.setAlignment(Pos.CENTER);
        imgPlaceholder.setStyle("-fx-background-color: #FAFAFA; -fx-border-color: #CFD8DC; -fx-border-width: 1;");

        Label placeholderLabel = new Label("IMG");
        placeholderLabel.setStyle("-fx-text-fill: #B0BEC5; -fx-font-weight: bold;");
        imgPlaceholder.getChildren().add(placeholderLabel);
        VBox.setVgrow(imgPlaceholder, Priority.ALWAYS);

        // --- BARRE DE VIE ---
        // On suppose un max de 200 HP pour l'affichage visuel, ou on peut rendre ça dynamique
        StackPane healthBar = createHealthBar(card.getHealth(), 200, NEW_WIDTH - (15 * scaleFactor), scaleFactor);
        VBox.setMargin(healthBar, new Insets(2, 0, 2, 0));

        // --- STATS (ATK / DEF) ---
        HBox statRow = new HBox(8 * scaleFactor);
        statRow.setAlignment(Pos.CENTER);
        statRow.getChildren().addAll(
                createStatBox("ATK", card.getAttack(), "#EF6C00", scaleFactor), // Orange
                createStatBox("DEF", card.getDefense(), "#0277BD", scaleFactor)  // Bleu
        );

        this.getChildren().addAll(header, imgPlaceholder, healthBar, statRow);
    }

    private StackPane createHealthBar(int current, int max, double width, double scale) {
        double pct = Math.max(0, Math.min(1.0, (double) current / max));

        Rectangle bg = new Rectangle(width, 8 * scale, javafx.scene.paint.Color.web("#CFD8DC")); // Gris
        bg.setArcWidth(4); bg.setArcHeight(4);

        // Couleur dynamique selon PV (Vert -> Rouge)
        javafx.scene.paint.Color color = pct > 0.5 ?
                javafx.scene.paint.Color.web("#66BB6A") : // Vert
                javafx.scene.paint.Color.web("#EF5350");  // Rouge

        Rectangle fg = new Rectangle(width * pct, 8 * scale, color);
        fg.setArcWidth(4); fg.setArcHeight(4);

        StackPane pane = new StackPane(bg, fg);
        pane.setAlignment(Pos.CENTER_LEFT);

        // Petit texte PV par dessus
        Label lbl = new Label(current + " HP");
        lbl.setStyle("-fx-font-weight: bold; -fx-font-size: " + (7 * scale) + "pt; -fx-text-fill: #37474F;");

        return new StackPane(pane, lbl);
    }

    private HBox createStatBox(String label, int value, String color, double scale) {
        Label l = new Label(label + " " + value);
        l.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-padding: 2 6; " +
                "-fx-font-weight: bold; -fx-font-size: " + (9 * scale) + "pt; -fx-background-radius: 4;");
        return new HBox(l);
    }
}