package controller;

import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.*;
import org.postgresql.ds.PGConnectionPoolDataSource;

import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

/**
 * Created by User on 10.04.2017.
 */
public class ControllerUserDetails extends Controller implements Initializable {

    private PGConnectionPoolDataSource myConn;
    private Zakaznik zakaznik;
    public TableView<OdoslaneTransakcie> transakcie_table;
    public TableView<PoistenieZakaznika> poistenie_table;
    public TableView<Karta> kartaTable;
    public TextField meno_field,priezvisko_field;
    public TableView<Transakcia> prijmy_table,vydaje_table;

    public Button delete;

    public Label meno_id,priezvisko_id;
    ControllerUserDetails(PGConnectionPoolDataSource x, Zakaznik zakaznik){
        this.myConn=x;
        this.zakaznik=zakaznik;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        //aktualizacia na realne meno zakaznika
        this.meno_id.setText(this.zakaznik.getMeno());
        this.priezvisko_id.setText(this.zakaznik.getPriezvisko());
        //naplnenie tabulky pre transakcie
        transakcieTableUpdate();
        //naplnenie tabulky poistenia
        poistenieTableUpdate();
        //naplnenie tabulky pre zakaznikove karty
        kartyTableUpdate();
//*/
    }
    //naplnenie tabuliek pre sumu uctov zakaznika/ zobrazia sa po kliknuti na tlacidlo
    private void hotovostUpdate() throws Exception {
        Connection conn = myConn.getConnection();
        ObservableList<Transakcia> data=vydaje_table.getItems();
        //vyberiem sucet zo vsetkych jeho uctov sucet tych transakcii
        PreparedStatement vydaje = conn.prepareStatement(
                "SELECT zu.zalozenie,sum(t.hodnota) FROM zakaznik z\n" +
                        "RIGHT JOIN zakaznicky_ucet zu ON z.id=zu.zakaznik_id\n" +
                        "LEFT JOIN ucet u ON u.id=zu.ucet_id\n" +
                        "RIGHT JOIN transakcia t ON t.ucet1_id=u.id\n" +
                        "WHERE z.meno = ? AND z.priezvisko = ? AND z.vek=?\n" +
                        "GROUP BY 1"
        );
        vydaje.setString(1,this.zakaznik.getMeno());
        vydaje.setString(2,this.zakaznik.getPriezvisko());
        vydaje.setDate(3,this.zakaznik.getDatum());
        ResultSet rs= vydaje.executeQuery();
        while(rs.next()){
            data.add(new Transakcia(rs.getDate(1),rs.getInt(2)));
        }
        vydaje_table.setItems(data);

        data=prijmy_table.getItems();
        PreparedStatement prijmy = conn.prepareStatement(
                "SELECT zu.zalozenie,sum(t.hodnota) FROM zakaznik z\n" +
                        "RIGHT JOIN zakaznicky_ucet zu ON z.id=zu.zakaznik_id\n" +
                        "LEFT JOIN ucet u ON u.id=zu.ucet_id\n" +
                        "RIGHT JOIN transakcia t ON t.ucet2_id=u.id\n" +
                        "WHERE z.meno = ? AND z.priezvisko = ? AND z.vek=?\n" +
                        "GROUP BY 1"
        );
        prijmy.setString(1,this.zakaznik.getMeno());
        prijmy.setString(2,this.zakaznik.getPriezvisko());
        prijmy.setDate(3,this.zakaznik.getDatum());
        rs= prijmy.executeQuery();
        while(rs.next()){
            data.add(new Transakcia(rs.getDate(1),rs.getInt(2)));
        }
        prijmy_table.setItems(data);

    }

    private void poistenieTableUpdate() {
        ObservableList<PoistenieZakaznika> data=poistenie_table.getItems();
        Connection conn= null;
        PreparedStatement tabulkaPoisteni=null;
        ResultSet rs=null;
        String typ_poistenia;
        Date zaciatok,koniec;
        try {
            conn = myConn.getConnection();
            tabulkaPoisteni=conn.prepareStatement(
                    "SELECT dp.druh,p.zaciatok,p.koniec\n" +
                    "FROM zakaznik z\n" +
                    "RIGHT JOIN poistenie p ON p.zakaznik_id=z.id\n" +
                    "LEFT JOIN druh_poistenia dp ON dp.id=p.druh\n" +
                    "WHERE z.meno=? AND z.priezvisko=? AND z.vek=?");
            tabulkaPoisteni.setString(1,this.zakaznik.getMeno());
            tabulkaPoisteni.setString(2,this.zakaznik.getPriezvisko());
            tabulkaPoisteni.setDate(3,this.zakaznik.getDatum());
            rs=tabulkaPoisteni.executeQuery();
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
        try {
            while(rs.next()){
                typ_poistenia=rs.getString(1);
                zaciatok=rs.getDate(2);
                koniec=rs.getDate(3);
                System.out.println(typ_poistenia + " " + zaciatok + " " + koniec);
                data.add(new PoistenieZakaznika(typ_poistenia,zaciatok,koniec));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        poistenie_table.setItems(data);

    }

    private void transakcieTableUpdate(){
        ObservableList<OdoslaneTransakcie> data=transakcie_table.getItems();
        Connection conn= null;
        PreparedStatement tabulkaTransakcii=null;
        ResultSet rs=null;
        String meno,priezvisko,typ;
        int suma;
        try {
            conn = this.myConn.getConnection();
            //vyberie vsetky transakcie patriace pod ucty zakaznika a zobrazi ich, tak ze k nim napise meno a typ prijimatela
            tabulkaTransakcii=conn.prepareStatement(
                    "SELECT z.meno AS odosielatel_meno, z.priezvisko AS odosielatel_priezvisko,"+
                            "prij_temp.suma AS prijata_hodnota, tu.typ AS typ FROM "+
                            "( \tSELECT z2.meno AS prijimatel_meno,"+
                            "\tz2.priezvisko AS prijimatel_priezvisko,"+
                            " \tt.ucet2_id AS prijimatel_ucet," +
                            "ks.druh,t.hodnota AS suma \tfrom transakcia t \t"+
                            "LEFT JOIN ucet u ON u.id=t.ucet1_id \t"+
                            "LEFT JOIN zakaznicky_ucet zu ON zu.ucet_id=u.id \t"+
                            "LEFT JOIN zakaznik z2 ON z2.id=zu.zakaznik_id \t"+
                            "LEFT JOIN konstantny_symbol ks ON ks.id=t.konst_id \t"+
                            "WHERE z2.meno LIKE ? AND z2.priezvisko LIKE ?  AND z2.vek =?)"+
                            " AS prij_temp "+
                            "LEFT JOIN ucet u ON u.id=prij_temp.prijimatel_ucet LEFT JOIN zakaznicky_ucet zk ON zk.ucet_id=u.id "+
                            "JOIN zakaznik z ON z.id=zk.zakaznik_id LEFT JOIN typ_uctu tu ON tu.id=u.typ");
            tabulkaTransakcii.setString(1,this.zakaznik.getMeno());
            tabulkaTransakcii.setString(2,this.zakaznik.getPriezvisko());
            tabulkaTransakcii.setDate(3,this.zakaznik.getDatum());
            rs=tabulkaTransakcii.executeQuery();

        } catch (SQLException e) {
            e.printStackTrace();
        }

        //naplnenie tabulky
        try {
            while(rs.next()){
                meno=rs.getString(1);
                priezvisko=rs.getString(2);
                suma=rs.getInt(3);
                typ=rs.getString(4);
                data.add(new OdoslaneTransakcie(suma,meno,priezvisko,typ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        transakcie_table.setItems(data);
    }

    private void kartyTableUpdate(){
        ObservableList<Karta> data=kartaTable.getItems();
        Connection conn= null;
        PreparedStatement tabulkaKariet=null;
        ResultSet rs=null;
        try {
            conn = this.myConn.getConnection();
            tabulkaKariet=conn.prepareStatement(
                    "SELECT * FROM" +
                            "(SELECT DISTINCT ON (t.typ) row_number() over() as prvyStlpec, t.typ FROM zakaznik z\n" +
                            "RIGHT JOIN karta k ON k.ucet_id=z.id\n" +
                            "LEFT JOIN typ_karty t ON t.id=k.typ\n" +
                            "WHERE z.meno LIKE ? AND z.priezvisko LIKE ? AND z.vek = ?)x " +
                            "ORDER BY prvyStlpec");
            tabulkaKariet.setString(1,this.zakaznik.getMeno());
            tabulkaKariet.setString(2,this.zakaznik.getPriezvisko());
            tabulkaKariet.setDate(3,this.zakaznik.getDatum());
            rs=tabulkaKariet.executeQuery();

        } catch (SQLException e) {
            e.printStackTrace();
        }
        int cislo;
        String typKarty;
        try {
            while(rs.next()){
                cislo=rs.getInt(1);
                typKarty=rs.getString(2);
                data.add(new Karta(typKarty,cislo));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        kartaTable.setItems(data);

    }

    public void update() throws SQLException {
        String meno=meno_field.getText();
        String priezvisko=priezvisko_field.getText();
        if(meno.isEmpty())meno=this.zakaznik.getMeno();
        if(priezvisko.isEmpty())priezvisko=this.zakaznik.getPriezvisko();
        PreparedStatement update=myConn.getConnection().prepareStatement("UPDATE zakaznik " +
                                                    "SET meno=? , priezvisko=? " +
                                                    "WHERE meno LIKE ? AND priezvisko LIKE ?" +
                                                    "AND vek = ?");

        update.setString(1,meno);
        update.setString(2,priezvisko);
        update.setString(3,this.zakaznik.getMeno());
        update.setString(4,this.zakaznik.getPriezvisko());
        update.setDate(5,this.zakaznik.getDatum());
        update.executeUpdate();
        this.priezvisko_id.setText(priezvisko);
        this.meno_id.setText(meno);
    }

    //odstrani vsetky entity ktore odkazuju na PK zakaznika
    public void delete()throws Exception{
        int fail=0;
        int id=0;
        Connection conn=null;
        try {

            conn=myConn.getConnection();
            conn.setAutoCommit(false);
            //odstraneni vsetkych vyberov hotovosti
            PreparedStatement deleteVyber = conn.prepareStatement(
                    "DELETE FROM vyber_hotovosti WHERE zakaznik_id=?"
            );
            deleteVyber.setInt(1,this.zakaznik.real_id);
            deleteVyber.executeUpdate();
            //odstranenie vsetkych jeho uctov
            PreparedStatement deleteUcet = conn.prepareStatement(
                    "DELETE FROM zakaznicky_ucet WHERE zakaznik_id=?"
            );
            deleteUcet.setInt(1,this.zakaznik.real_id);
            deleteUcet.executeUpdate();
            //odstranenie vsetkych jeho poisteni
            PreparedStatement deletePoistenie = conn.prepareStatement(
                    "DELETE FROM poistenie WHERE zakaznik_id=?"
            );
            deletePoistenie.setInt(1,this.zakaznik.real_id);
            deletePoistenie.executeUpdate();
            //odstranenie samotneho zakaznika
            PreparedStatement deleteZak = conn.prepareStatement(
                    "DELETE FROM zakaznik WHERE id=?"
            );
            deleteZak.setInt(1, this.zakaznik.real_id);
            deleteZak.executeUpdate();
        }
        catch(Exception e){
            e.printStackTrace();
            fail=0;
            openJumpWindow("Nieco sa pokazilo nastal rollback","Error");
            conn.rollback();
            return;
        }
        conn.commit();
        openJumpWindow("Odstranil sa zakaznik, informacie o jeho vyberoch hotovosti a poisteniach","Ok");
        Stage s=(Stage) delete.getScene().getWindow();
        s.close();
    }

    public void zobrazUcty(){
        try {
            prijmy_table.getItems().clear();
            vydaje_table.getItems().clear();
            hotovostUpdate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
