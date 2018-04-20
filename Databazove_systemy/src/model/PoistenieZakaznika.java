package model;

import java.sql.Date;

/**
 * Created by User on 11.04.2017.
 * Entita vytvorena na pouzitie v tabulke
 */
public class PoistenieZakaznika {
    private String typ_poistenia;
    private Date zaciatok,koniec;

    public PoistenieZakaznika(String typ_poistenia, Date zaciatok, Date koniec) {
    this.typ_poistenia=typ_poistenia;
    this.zaciatok=zaciatok;
    this.koniec=koniec;
    }

    public String getTyp_poistenia() {
        return typ_poistenia;
    }

    public void setTyp_Poistenia(String typ_poistenia) {
        this.typ_poistenia = typ_poistenia;
    }

    public Date getZaciatok() {
        return zaciatok;
    }

    public void setZaciatok(Date zaciatok) {
        this.zaciatok = zaciatok;
    }

    public Date getKoniec() {
        return koniec;
    }

    public void setKoniec(Date koniec) {
        this.koniec = koniec;
    }
}
