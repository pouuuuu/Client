package view;

import controller.GameController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

public class LoginView {

    private ViewManager viewManager;
    private GameController controller;
    private BorderPane mainContainer;
    private Label errorLabel;

    public LoginView(ViewManager viewManager, GameController controller) {
        this.viewManager = viewManager;
        this.controller = controller;
        createLayout();
    }

    private void createLayout() {
        mainContainer = new BorderPane();
        mainContainer.setPadding(new Insets(50));

        VBox box = new VBox(20);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30, 40, 30, 40));

        box.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.7);" +
                        "-fx-background-radius: 15;" +
                        "-fx-border-radius: 15;" +
                        "-fx-border-color: #B0BEC5;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 5);"
        );
        box.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        Label title = new Label("Connexion au serveur");
        title.setStyle("-fx-font-size: 20pt; -fx-font-weight: bold; -fx-text-fill: #37474F;");

        TextField nameField = new TextField();
        nameField.setPromptText("Entrer votre pseudo");
        nameField.setPrefWidth(250);
        nameField.setStyle("-fx-font-size: 12pt;");

        Button btnConnect = new Button("Se connecter");
        btnConnect.setStyle(
                "-fx-font-size: 14pt;" +
                        "-fx-background-color: #00897B;" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;"
        );
        btnConnect.setPrefWidth(250);

        errorLabel = new Label();
        errorLabel.setTextFill(Color.RED);

        btnConnect.setOnAction(e -> {
            errorLabel.setText("Connexion...");
            errorLabel.setTextFill(Color.BLUE);
            boolean success = controller.connect(nameField.getText());
            if (!success) {
                errorLabel.setText("Connecxion échouée (Serveur indisponible ?)");
                errorLabel.setTextFill(Color.RED);
            }
        });

        Button btnDebug = new Button("Mode hors-ligne");
        btnDebug.setStyle("-fx-background-color: transparent; -fx-text-fill: gray; -fx-underline: true;");
        btnDebug.setOnAction(e -> controller.startDebugMode(nameField.getText().isEmpty() ? "Tester" : nameField.getText()));

        box.getChildren().addAll(title, nameField, btnConnect, errorLabel, btnDebug);

        mainContainer.setCenter(box);
    }

    public Node getView() {
        return mainContainer;
    }
}