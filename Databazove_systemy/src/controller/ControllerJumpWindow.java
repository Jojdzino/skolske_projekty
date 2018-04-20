package controller;

import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Created by User on 29.04.2017.
 */
///vyskakovacie okienko
public class ControllerJumpWindow implements Initializable {
    public Label label_id;
    public Button delete;
    private String label;
    public ControllerJumpWindow(String vypln){
        this.label=vypln;
    }

    public void close(){
        Stage s=(Stage)(delete.getScene().getWindow());
        s.close();
    }

    public void setLabel(Label label) {
        this.label_id = label;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.label_id.setText(this.label);
    }
}
