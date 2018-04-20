package model;

/**
 * Created by User on 18.04.2017.
 * Entita vytvorena na pouzitie v tabulke
 */
public class Karta {

    private int cislo;
    private String typ;

    public Karta(String typ, int cislo) {
        this.typ=typ;
        this.cislo=cislo;
    }

    public int getCislo() {
        return cislo;
    }

    public void setCislo(int cislo) {
        this.cislo = cislo;
    }

    public String getTyp() {
        return typ;
    }

    public void setTyp(String typ) {
        this.typ = typ;
    }
}
