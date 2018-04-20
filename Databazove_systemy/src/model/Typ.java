package model;

/**
 * Created by User on 21.04.2017.
 * Entita vytvorena na pouzitie v tabulke
 */
public class Typ {

    private int id;
    private String typ;

    public Typ(int id,String typ){
        this.id=id;
        this.typ=typ;
    }
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTyp() {
        return typ;
    }

    public void setTyp(String typ) {
        this.typ = typ;
    }

    public String toString() {    return typ;  }
}
