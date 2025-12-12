package view;

import controller.GameController;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

public class LoginView {

    private ViewManager viewManager;
    private GameController controller;
    private VBox layout;
    private Label errorLabel;

    public LoginView(ViewManager viewManager, GameController controller) {
        this.viewManager = viewManager;
        this.controller = controller;
        createLayout();
    }

    private void createLayout() {
        layout = new VBox(20);
        layout.setAlignment(Pos.TOP_CENTER);
        layout.setPadding(new Insets(50));

        Label title = new Label("Connexion au Serveur");
        title.setStyle("-fx-font-size: 24pt; -fx-font-weight: bold;");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setAlignment(Pos.CENTER);

        TextField nameField = new TextField();
        form.addRow(0, new Label("Pseudo :"), nameField);

        // BOUTON 1 : Vraie Connexion
        Button btnConnect = new Button("SE CONNECTER (Live)");
        btnConnect.setStyle("-fx-font-size: 14pt; -fx-background-color: #00897B; -fx-text-fill: white;");
        btnConnect.setPrefWidth(250);

        // BOUTON 2 : Mode Test
        Button btnDebug = new Button("MODE TEST (Interface seule)");
        btnDebug.setStyle("-fx-font-size: 11pt; -fx-background-color: #757575; -fx-text-fill: white;");
        btnDebug.setPrefWidth(250);

        errorLabel = new Label();
        errorLabel.setTextFill(Color.RED);

        // Action Vraie Connexion
        btnConnect.setOnAction(e -> {
            errorLabel.setText("Connexion...");
            errorLabel.setTextFill(Color.BLUE);
            boolean sent = controller.connect(nameField.getText());
            if (!sent) {
                errorLabel.setText("Serveur inaccessible.");
                errorLabel.setTextFill(Color.RED);
            }
        });

        // Action Mode Test
        btnDebug.setOnAction(e -> {
            controller.startDebugMode(nameField.getText());
        });

        layout.getChildren().addAll(title, form, btnConnect, btnDebug, errorLabel);
    }

    public Node getView() {
        return layout;
    }
}