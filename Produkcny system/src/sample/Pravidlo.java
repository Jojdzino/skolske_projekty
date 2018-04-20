package sample;

import javafx.scene.control.TextArea;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by User on 06.05.2017.
 */
public class Pravidlo {
    private String celePravidlo;
    private String meno;
    private String[] ak=new String[10];//maximalne 10 podmienok
    private String[] potom=new String[10];
    private int akcia[]=new int[10];//pridaj 0,vymaz 1, sprava 2
    private int countEnter,count2,count;

    public Pravidlo(String pravidloString){
        this.celePravidlo=pravidloString;
        parse();
        //write();
        //System.out.println("\n");
    }

    private void write() {
        System.out.println("Meno "+ this.meno);
        System.out.println("Podmienka:");
        for(String x:this.ak)
            if(x!=null)
            System.out.println(x);
        System.out.println("Potom sa stane:");
        for(String x:this.potom)
            if(x!=null)
            System.out.println(x);
    }

    private void parse() {
        //rozdel string na meno, ak potom
        //dostal som pravidlo
        String[] pom=this.celePravidlo.split("\n");
        String menoSplit=pom[0];
        String akSplit  =pom[1];
        String potomSplit=pom[2];
        //parse mena
        pom=menoSplit.split(" ");
        for(int i=1;i<pom.length;i++) {
            if(this.meno==null) {
                this.meno = pom[i];
                continue;
            }
            this.meno = this.meno + " " + pom[i];
        }

        //parse ak
        pom=akSplit.split("    ");
        String pomAk= "";
        for(int i=1;i<pom.length;i++)//vyberie to pravidlo
            pomAk= pomAk + pom[i];

        pom=pomAk.split(",");//rozdeli na viacero pravidiel
        for(int k=0;k<pom.length;k++){
            this.ak[k]=pom[k];
        }


        //Rozseknem potom na cele potom, dalej ho rozsekam podla ciastok
        pom=potomSplit.split(" ");
        String pomPotom="";
        for(int i=1;i<pom.length;i++) {
            if(pomPotom==""){
                pomPotom= pom[i];
                continue;
            }
            pomPotom = pomPotom + " " + pom[i];
        }
       // System.out.println("\n\n"+pomPotom+"\n\n");
        pom=pomPotom.split(",");//rozdeli na viac pod Potom akcii
        for(int x=0;x<pom.length;x++)
            this.potom[x]=pom[x];
        //ok mam v mene meno, v ak celu podmienku, tu asi bude treba rozparsovat na male kusky stringov
        int p=0;
        //prejdem vsekty stringy v potom, a pozriem sa na prvy podstring v nich, zistim co maju spravit
        for(String x:this.potom){
            String pom2="";
            if(x==null)continue;
            pom=x.split(" ");//mam cele pravidlo posekane po slovach

            for(int l=1;l<pom.length;l++){
                if(pom2==""){
                    pom2=pom[l];
                    continue;
                }
                pom2=pom2 + " " + pom[l];
            }
            this.potom[p]=pom2;//nahradim ten string za iny skrateny

            if(0==pom[0].compareTo("pridaj")) {
                this.akcia[p++] = 0;
                continue;
            }

            if(0==pom[0].compareTo("vymaz")) {
                this.akcia[p++] = 1;
                continue;
            }

            if(0==pom[0].compareTo("sprava")) {
                this.akcia[p++] = 2;
                continue;
            }
        }
    }

    public String getCelePravidlo() {
        return celePravidlo;
    }

    public void setCelePravidlo(String celePravidlo) {
        this.celePravidlo = celePravidlo;
    }

    public String getMeno() {
        return meno;
    }

    public void setMeno(String meno) {
        this.meno = meno;
    }

    public String[] getAk() {
        return ak;
    }

    public void setAk(String[] ak) {
        this.ak = ak;
    }

    public String[] getPotom() {
        return potom;
    }

    public void setPotom(String[] potom) {
        this.potom = potom;
    }

    public int[] getAkcia() {
        return akcia;
    }

    public void setAkcia(int[] akcia) {
        this.akcia = akcia;
    }

    public int getCountEnter() {
        return countEnter;
    }

    public void setCountEnter(int countEnter) {
        this.countEnter = countEnter;
    }

    public int getCount2() {
        return count2;
    }

    public void setCount2(int count2) {
        this.count2 = count2;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public boolean vykonajAkcie(LinkedList<LinkedList<String>> naviazania, LinkedList<Fakt> fakty, TextArea facts, TextArea messages) {

        LinkedList<LinkedList<String>> list=new LinkedList<>();
        for(LinkedList<String>x:naviazania){
            //list obsahuje vsetky uz naviazania stringy, ktore treba len porovnat
            //s faktami
            list.add(this.zmenNaString(x));//msuim prejst stringy aj s typom akcie
        }
        //checkni tento list ci ma take prvky ako fakty
        int[] akcie=this.getAkcia();
        if(akcia[0]!=1) {
            for (int i = 0; i < fakty.size(); i++) {
                if (!list.isEmpty() && fakty.get(i).getCelyFakt().compareTo(list.getFirst().getFirst()) == 0) {
                    list.remove();
                    i = 0;
                }
            }
        }
        if(list.isEmpty())return false;
        int iterator=0;

        for(LinkedList<String>x:list){
            iterator=0;
            //prejdem elementy a pozriem sa na akciu
            for(String str:x){
                if(akcie[iterator]==0){
                    //vypis
                    facts.appendText(str+ "\n");
                    if(x.size()==iterator+1)
                    return true;
                }
                if(akcie[iterator]==1){
                    String[] arr=facts.getText().split("\n");
                    LinkedList<String> pom=new LinkedList<>();
                    for(String string:arr){
                        pom.add(string);
                    }

                    for(Iterator<String> iter=pom.iterator();iter.hasNext();){
                        String pomocnyString=iter.next();
                        if(pomocnyString.compareTo(str)==0)
                            iter.remove();
                    }
                    facts.clear();
                    for(String string:pom)
                        facts.appendText(string+"\n");

                    if(x.size()==iterator+1)
                        return true;
                }
                if(akcie[iterator]==2){
                    messages.appendText(str+ "\n");
                    if(x.size()==iterator+1)
                        return true;
                }
                iterator++;
            }
        }

        if(list.isEmpty())
            return false;
        return true;//vrati akciu na vypis
    }

    private LinkedList<String> zmenNaString(LinkedList<String> list) {

        //list jedneho naviazania, premenne a hodnoty
        LinkedList<String> akciePreJednoNaviazanie=new LinkedList<>();
        LinkedList<String>hodnoty=new LinkedList<>();
        LinkedList<String>premenne=new LinkedList<>();

        for(String x:list){
            String[] arr=x.split("=");
            premenne.add(arr[0]);
            hodnoty.add(arr[1]);
        }
        //zoberiem jednu akciu a rozbijem ho cez medzery
        for(String x:this.getPotom()){
            if(x==null)break;
            String[] arr=x.split(" ");
            for(int j=0;j<arr.length;j++){
                for(int i=0;i<premenne.size();i++){
                    if(arr[j].compareTo(premenne.get(i))==0){
                        arr[j]=hodnoty.get(i);
                    }
                }
            }
            akciePreJednoNaviazanie.add(String.join(" ",arr));
        }
        return akciePreJednoNaviazanie;
    }
}
