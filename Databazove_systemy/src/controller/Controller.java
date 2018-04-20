package controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Created by User on 29.04.2017.
 */
public class Controller {

    //metoda na  fixnutie duplicitneho kodu- vyskakovacie okno na error a ok
    public void openJumpWindow(String label,String title) throws IOException {
        Stage jumpWindow=new Stage();

        ControllerJumpWindow controller=new ControllerJumpWindow(label);

        FXMLLoader loader=new FXMLLoader((getClass().getResource("../sample/jumpWindow.fxml")));
        loader.setController(controller);
        Parent root=loader.load();
        jumpWindow.setTitle(title);
        jumpWindow.setScene(new Scene(root,275,174));
        jumpWindow.show();
    }
}
