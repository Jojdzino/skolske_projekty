package model;

/**
 * Created by User on 10.04.2017.
 * Entita vytvorena na pouzitie v tabulke
 */
public class OdoslaneTransakcie {
    private String meno,priezvisko,typ_uctu;
    private int suma;

    public OdoslaneTransakcie(int sum, String name, String surname, String typ){
        this.suma=sum;
        this.meno=name;
        this.priezvisko=surname;
        this.typ_uctu=typ;
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

    public String getTyp_uctu() {
        return typ_uctu;
    }

    public void setTyp_uctu(String typ_uctu) {
        this.typ_uctu = typ_uctu;
    }

    public int getSuma() {
        return suma;
    }

    public void setSuma(int suma) {
        this.suma = suma;
    }
}
