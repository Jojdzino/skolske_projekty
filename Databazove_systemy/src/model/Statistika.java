package model;

/**
 * Created by User on 23.04.2017.
 * Entita vytvorena na pouzitie v tabulke
 */
public class Statistika {
    private int suma,percenta;
    private String typ;


    public Statistika(String typ,int suma, int percenta ){
        this.suma = suma;
        this.percenta=percenta;
        this.typ=typ;
    }
    public int getSuma() {
        return suma;
    }

    public void setSuma(int suma) {
        this.suma = suma;
    }

    public int getPercenta() {
        return percenta;
    }

    public void setPercenta(int percenta) {
        this.percenta = percenta;
    }

    public String getTyp() {
        return typ;
    }

    public void setTyp(String typ) {
        this.typ = typ;
    }
}
