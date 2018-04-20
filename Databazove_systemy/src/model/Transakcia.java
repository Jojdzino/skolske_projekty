package model;

import java.util.Date;

/**
 * Created by User on 24.04.2017.
 * Entita vytvorena na pouzitie v tabulke
 */
public class Transakcia {

    private int suma;
    private Date datum;


    public Transakcia(Date y,int x){
        this.datum=y;
        this.suma=x;
    }
    public int getSuma() {
        return suma;
    }

    public void setSuma(int suma) {
        this.suma = suma;
    }

    public Date getDatum() {
        return datum;
    }

    public void setDatum(Date typ) {
        this.datum = typ;
    }
}
