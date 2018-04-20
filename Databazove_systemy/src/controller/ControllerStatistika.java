package controller;

import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import model.Statistika;
import org.postgresql.ds.PGConnectionPoolDataSource;

import javafx.scene.control.TableView;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ResourceBundle;

/**
 * Created by User on 23.04.2017.
 */
public class ControllerStatistika implements Initializable {
    PGConnectionPoolDataSource myConn;
    public TableView<Statistika> vyber_hotovosti_table;
    public TableView<Statistika> karty_table;

    public ControllerStatistika(PGConnectionPoolDataSource myConn) {
        this.myConn=myConn;
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ObservableList vyber_hotovosti_data=vyber_hotovosti_table.getItems();
        ObservableList karty_data=karty_table.getItems();
        ResultSet rs=null;
        //zobrazenie statistiky pre ucty
        try {
            Connection conn = myConn.getConnection();
            PreparedStatement uctyStatement=conn.prepareStatement(
                    "SELECT t.typ, count(v.velkost),\n" +
                            "round((count(v.velkost)*100/(SELECT count(v.velkost) FROM vyber_hotovosti v)))\n" +
                            "FROM vyber_hotovosti v\n" +
                            "LEFT JOIN ucet u ON u.id=v.ucet_id\n" +
                            "LEFT JOIN typ_uctu t ON t.id=u.typ\n" +
                            "GROUP BY 1"
            );
            rs=uctyStatement.executeQuery();
            while(rs.next()){
                vyber_hotovosti_data.add(new Statistika(
                        rs.getString(1),rs.getInt(2),rs.getInt(3))
                );
            }
            vyber_hotovosti_table.setItems(vyber_hotovosti_data);
            //zobrazenie statistiky pre karty
            PreparedStatement kartyStatement=conn.prepareStatement(
                    "SELECT tk.typ,count(tk.typ),\n" +
                            "round(count(tk.typ)*100/(SELECT count(karta.id) FROM karta))\n" +
                            " FROM karta k\n" +
                            "LEFT JOIN typ_karty tk ON tk.id=k.typ\n" +
                            "GROUP BY 1"
            );
            rs=kartyStatement.executeQuery();
            while(rs.next()){
                karty_data.add(new Statistika(rs.getString(1),rs.getInt(2),rs.getInt(3)));
            };
            karty_table.setItems(karty_data);
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
}
