package controller;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import org.postgresql.ds.PGConnectionPoolDataSource;

/**
 * Created by User on 06.04.2017.
 */
public class ControllerUsers  {
    private PGConnectionPoolDataSource myConn;
    public ControllerUsers(PGConnectionPoolDataSource x){
        this.myConn=x;
    }
    //zobrazenie pridavacieho okna
    public void addUser() throws Exception {
        Stage add_user= new Stage();
        ControllerAddUser userAdd=new ControllerAddUser(this.myConn);
        FXMLLoader fxmlLoader=new FXMLLoader((getClass().getResource("../sample/addUser.fxml")));
        fxmlLoader.setController(userAdd);
        Parent root = fxmlLoader.load();
        add_user.setTitle("Pridaj zákazníka");
        add_user.setScene(new Scene(root, 780, 360));
        add_user.show();
    }
    //zobrazenie vyhladavania
    public void findUser() throws Exception{
        Stage find_user= new Stage();

        ControllerFindUser findUser=new ControllerFindUser(this.myConn);

        FXMLLoader fxmlLoader=new FXMLLoader((getClass().getResource("../sample/findUser.fxml")));
        fxmlLoader.setController(findUser);
        Parent root = fxmlLoader.load();
        find_user.setTitle("Najdi zákazníka");
        find_user.setScene(new Scene(root, 600, 600));
        find_user.show();
    }
    //zobrazenie okna na sttaistiky
    public void statistika() throws Exception{
        Stage statstika =new Stage();

        ControllerStatistika statistika =new ControllerStatistika(this.myConn);
        FXMLLoader fxmlLoader=new FXMLLoader((getClass().getResource("../sample/statistika.fxml")));
        fxmlLoader.setController(statistika);
        Parent root = fxmlLoader.load();
        statstika.setTitle("Štatistiky bankového systému");
        statstika.setScene(new Scene(root, 1000, 500));
        statstika.show();
    }
}
