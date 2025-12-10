module cardgame.client {
    requires javafx.controls;
    requires javafx.graphics;

    exports view;
    exports model;
    exports controller;
    exports network;

    opens view to javafx.graphics;
}