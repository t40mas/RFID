package de.rfid.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(this.getClass().getResource("gui.fxml"));
        primaryStage.setTitle("RFID RMI Client");
        primaryStage.setScene(new Scene(root, 980, 754));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
