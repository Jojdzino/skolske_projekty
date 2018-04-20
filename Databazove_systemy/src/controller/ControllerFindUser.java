package controller;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.Zakaznik;
import org.postgresql.ds.PGConnectionPoolDataSource;
import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

/**
 * Created by User on 08.04.2017.
 */
public class ControllerFindUser implements Initializable {

    private PGConnectionPoolDataSource myConn;
    public TableView<Zakaznik> tabulka;
    public TextField meno_find_field, priezvisko_find_field;
    public Button hladaj_button,next_button,prev_button;
    int lastId=-1;
    int disable_prev=0;// premenna na zistenie pocet stlaceni tlacidla previuos
    ObservableList<Zakaznik> data=null;
    Connection conn=null;
    String meno_string,priezvisko_string;
    ResultSet rs=null;

    public ControllerFindUser(PGConnectionPoolDataSource x){
        this.myConn=x;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        tabulka.setOnMouseClicked(event -> {
            if(event.getClickCount()>1){
                if(tabulka.getSelectionModel().getSelectedIndex()>=0){
                    ControllerUserDetails userDetails=new ControllerUserDetails(this.myConn,tabulka.getSelectionModel().getSelectedItem());
                    Stage userDetailsStage= new Stage();
                    FXMLLoader fxmlLoader=new FXMLLoader((getClass().getResource("../sample/userDetails.fxml")));
                    fxmlLoader.setController(userDetails);
                    Parent root = null;
                    try {
                        root = root = fxmlLoader.load();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    userDetailsStage.setTitle("Podrobnosti používateľa");
                    userDetailsStage.setScene(new Scene(root, 1200, 1000));
                    userDetailsStage.show();
                }
            }
        });
    }

    //dopyt na previous- robene cez seek metodu- bolo by lepsie ulozit si aktualne prve id
    // a cez neho, takto beriem 34 a zase to sekam na polovicu
    public void prev(ActionEvent actionEvent) throws Exception{
        load();
        PreparedStatement getPrev=conn.prepareStatement(
                "SELECT d.id,row_number() over() AS riadok,meno,priezvisko,vek FROM \n" +
                        "(SELECT * FROM \n" +
                        "(SELECT * FROM\n" +
                        "(SELECT z.id AS id, z.meno, z.priezvisko, z.vek\n" +
                        "FROM zakaznik AS z WHERE meno LIKE ?\n" +
                        "AND priezvisko LIKE ?\n" +
                        "AND id < ?)x\n" +
                        "ORDER BY id DESC\n" +
                        "LIMIT 34)z\n" +
                        "ORDER BY id ASC\n" +
                        "LIMIT 17)d"
                        );
        getPrev.setString(1, this.meno_string + "%");
        getPrev.setString(2, this.priezvisko_string + "%");
        getPrev.setInt(3,this.lastId+1);
        this.rs=getPrev.executeQuery();

        if(--this.disable_prev==0)this.prev_button.setDisable(true);
        fillTableWithResultSet(this.disable_prev);
    }
    //vyhlada dalsiu tabulku pouzivatelov-- pouzita seek metoda
    public void next(ActionEvent actionEvent) throws Exception{
        ++this.disable_prev;
        this.prev_button.setDisable(false);
        load();
        PreparedStatement getNext=conn.prepareStatement(
                "SELECT z.id AS id," +
                    "row_number() OVER() as riadok,\n" +
                    "\tz.meno,z.priezvisko,z.vek\n" +
                    "\tFROM zakaznik AS z WHERE z.meno LIKE ?" +
                    "AND z.priezvisko like ?\n" +
                    "\tAND id > ?\n" +
                    "\t limit 17");
        getNext.setString(1, this.meno_string + "%");
        getNext.setString(2, this.priezvisko_string + "%");
        getNext.setInt(3,this.lastId);
        this.rs=getNext.executeQuery();
        fillTableWithResultSet(this.disable_prev);
    }
    //vyhlada prvotnu tabulku pouzivatelov
    public void hladaj(ActionEvent actionEvent) throws Exception {
        this.disable_prev=0;
        this.lastId=-1;
        this.next_button.setDisable(false);
        this.prev_button.setDisable(true);
        load();
        PreparedStatement findUser = conn.prepareStatement(
                "SELECT z.id," +
                    "row_number() over() as riadok,\n" +
                    "\tz.meno," +
                    "z.priezvisko, " +
                    "z.vek " +
                    "FROM zakaznik AS z WHERE meno LIKE ?" +
                    " AND priezvisko LIKE ?" +
                    "LIMIT 17");
        findUser.setString(1, meno_string + "%");
        findUser.setString(2, priezvisko_string + "%");
        this.rs = findUser.executeQuery();
        fillTableWithResultSet(0);//x=kolkokrat je stlacene next/prev
    }

    private void load()throws Exception {
        this.tabulka.getItems().clear();//clear data
        this.conn = this.myConn.getConnection();
        this.data=this.tabulka.getItems();
        this.meno_string= this.meno_find_field.getText();
        this.priezvisko_string= this.priezvisko_find_field.getText();
    }

    //toto naplnanie tabulky je rovnake pre vsetky next previous a hladaj
    // jedine co sa meni su id cisielka(riadok) pre pekne zoradenie
    private void fillTableWithResultSet(int x)throws Exception{
        int id=0,riadok;
        while(this.rs.next()){
            //System.out.println("rs neni prazdny");
            id=this.rs.getInt("id");//realne id pouzivatela
            riadok=this.rs.getInt("riadok");//id pre zoradenie v tabulke
            this.meno_string=this.rs.getString("meno");
            this.priezvisko_string=this.rs.getString("priezvisko");
            Date vek=this.rs.getDate("vek");
            data.add(new Zakaznik(riadok+(17*x),id,this.meno_string,this.priezvisko_string,vek));
        }
        //ulozim si last id
        this.lastId=id;
        this.tabulka.setItems(this.data);
    }
}
