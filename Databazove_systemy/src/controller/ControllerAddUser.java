package controller;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import model.Typ;
import org.postgresql.ds.PGConnectionPoolDataSource;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.ResourceBundle;

import static org.postgresql.core.SqlCommandType.INSERT;

/**
 * Created by User on 06.04.2017.
 */
public class ControllerAddUser extends Controller implements Initializable {
    private PGConnectionPoolDataSource myConn;
    public TextField meno_field,priezvisko_field,suma_field;
    public DatePicker datum;
    public CheckBox ucet_checkbox,poistenie_checkbox;
    public DatePicker datum_zalozenia_id,zaciatok_id,koniec_id;
    public ChoiceBox<Typ> ucet_typ;
    public ChoiceBox<Typ> poistenie_typ;
    public ObservableList<Typ> typy=null;
    int error=0;

    public ControllerAddUser(PGConnectionPoolDataSource x){
        this.myConn=x;
    }


    public void addUser(ActionEvent actionEvent) throws SQLException {

        Connection conn = this.myConn.getConnection();
        conn.setAutoCommit(false);
        //adding user
        PreparedStatement insertUser = conn.prepareStatement(
                "INSERT INTO zakaznik VALUES(DEFAULT,?,?,?)"
        );

        try {
            String meno=meno_field.getText();
            String priezvisko=priezvisko_field.getText();
            LocalDate ld = datum.getValue();

            insertUser.setString(1,meno);
            insertUser.setString(2,priezvisko);
            insertUser.setDate(3, Date.valueOf(ld));
            insertUser.executeUpdate();
        } catch (Exception e) {
            try {
                openJumpWindow("Pridanie používateľa sa nepodarilo","Error");
            } catch (IOException x) {
                x.printStackTrace();
            }
            conn.rollback();
            return;
        }
        //pridanie uctu
        if(ucet_checkbox.isSelected()){
            try {
                //insertnutie uctu
                PreparedStatement insertUcet = conn.prepareStatement(
                        "INSERT INTO ucet VALUES(DEFAULT,?,?)"
                );
                insertUcet.setInt(1, Integer.parseInt(suma_field.getText()));
                insertUcet.setInt(2, ucet_typ.getValue().getId());
                insertUcet.executeUpdate();
                //naviazanie vazobnej entity
                PreparedStatement insertZakUcet = conn.prepareStatement(
                        "INSERT INTO zakaznicky_ucet " +
                                "VALUES (DEFAULT ,currval('zakaznik_id_seq'),currval('ucet_id_seq'),?)"
                );
                insertZakUcet.setDate(1, Date.valueOf(datum_zalozenia_id.getValue()));
                insertZakUcet.executeUpdate();
            }
            catch(Exception e){
                try {
                    openJumpWindow("Pridanie zakaznickeho uctu sa nepodarilo","Error");
                } catch (IOException x) {
                    x.printStackTrace();
                }
                conn.rollback();
                return;
            }
        }
        //pridanie poistenia
        if(poistenie_checkbox.isSelected()) {
            try {
                PreparedStatement insertPoistenie = conn.prepareStatement(
                        "INSERT INTO poistenie VALUES (DEFAULT,?,?,?,currval('zakaznik_id_seq'))"
                );
                insertPoistenie.setInt(1, poistenie_typ.getValue().getId());
                insertPoistenie.setDate(2, Date.valueOf(zaciatok_id.getValue()));
                insertPoistenie.setDate(3, Date.valueOf(koniec_id.getValue()));
                insertPoistenie.executeUpdate();
            }
            catch(Exception e){
                try {
                    openJumpWindow("Pridanie poistenia sa nepodarilo","Error");
                } catch (IOException x) {
                    x.printStackTrace();
                }
                conn.rollback();
                return;
            }
        }
        //ak sa podarilo vytvorit zakaznika so vsetkymi hodnotami zadanymi pouzivatelom
        try {
            openJumpWindow("Pridanie sa podarilo úspešne","Pridanie");
        } catch (IOException e) {
            e.printStackTrace();
        }
        conn.commit();

    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //nacitanie checkboxov
        try {
            Connection conn = this.myConn.getConnection();
            ResultSet populateChoiceBox = conn.prepareStatement(
                    "SELECT * FROM druh_poistenia " +
                            "ORDER BY 1").executeQuery();
            typy=poistenie_typ.getItems();
            while (populateChoiceBox.next()) {
                typy.add(new Typ(populateChoiceBox.getInt("id"), populateChoiceBox.getString("druh")));
            }
            poistenie_typ.setItems(typy);
            poistenie_typ.getSelectionModel().select(0);
        }
        catch(Exception e){
            e.printStackTrace();
        }



        try {
            Connection conn = this.myConn.getConnection();
            ResultSet populateChoiceBox = conn.prepareStatement(
                "SELECT * FROM typ_uctu " +
                    "ORDER BY 1").executeQuery();
            typy=ucet_typ.getItems();
            while (populateChoiceBox.next()) {
                typy.add(new Typ(populateChoiceBox.getInt("id"), populateChoiceBox.getString("typ")));
            }
            ucet_typ.setItems(typy);
            ucet_typ.getSelectionModel().select(0);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }


}
