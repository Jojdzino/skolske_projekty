package model;


import java.sql.Date;

/**
 * Created by User on 08.04.2017.
 * Entita vytvorena na pouzitie v tabulke
 */

public class Zakaznik {
    public int id;
    public String meno;
    public String priezvisko;
    public Date datum;
    public int real_id;

    public Zakaznik(int idcko,int real_id,String firstName,String surname,Date x) {
        this.id=idcko;
        this.meno=firstName;
        this.priezvisko=surname;
        this.datum = x;
        this.real_id=real_id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMeno() {
        return meno;
    }

    public void setMeno(String meno) {
        this.meno = meno;
    }

    public String getPriezvisko() {
        return priezvisko;
    }

    public void setPriezvisko(String priezvisko) {
        this.priezvisko = priezvisko;
    }

    public Date getDatum() {
        return datum;
    }

    public void setDatum(Date datum) {
        this.datum = datum;
    }
}
// to je nejake divne :D