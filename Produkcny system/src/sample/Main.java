package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Controller x=new Controller();
        FXMLLoader fxmlLoader=new FXMLLoader((getClass().getResource("sample.fxml")));
        fxmlLoader.setController(x);
        Parent root = fxmlLoader.load();

        primaryStage.setTitle("Produkcny stroj");
        primaryStage.setScene(new Scene(root, 827, 645));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
