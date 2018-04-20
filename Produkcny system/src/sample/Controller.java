package sample;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;
import sun.awt.image.ImageWatched;

import java.io.*;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    @FXML
    private TextArea facts, rules, messages;
    private LinkedList<Pravidlo> pravidla;
    private LinkedList<Fakt> fakty;

    public void jedenKrok(){
        nacitaj();
        //pre kazde pravidlo sprav
        for (Pravidlo pravidlo : pravidla) {
            LinkedList<LinkedList<String>> naviazania = zhoda(pravidlo);
            if (special(pravidlo))//ak je pravidlo specialne
                naviazania = specialne(naviazania);
            if (naviazania==null || naviazania.size() == 0) continue;
            if(pravidlo.vykonajAkcie(naviazania, fakty, facts, messages))
                return;
        }
    }

    public void mainLoop() {
        while(true)
            jedenKrok();
    }

    void nacitaj(){
        pravidla = new LinkedList<Pravidlo>();
        fakty = new LinkedList<Fakt>();
        String[] split = rules.getText().split("\\n\\n");
        for (String i : split)
            pravidla.add(new Pravidlo(i));

        split = facts.getText().split("\n");
        for (String i : split) {
            //System.out.println(i);
            fakty.add(new Fakt(i));
        }
    }
//pre kazdy list vyhovujuci pravidlu
    private LinkedList<LinkedList<String>> specialne( LinkedList<LinkedList<String>> naviazania) {


        for(Iterator<LinkedList<String>> iterator =naviazania.iterator();iterator.hasNext();
                ){
            LinkedList list=iterator.next();
            if(!vyhovuje(list)){//ak nevyhovuje list, zmaz ho
                iterator.remove();
            }
        }
    return naviazania;
    }
//zisti ci vyhovuje
    private boolean vyhovuje(LinkedList<String> list) {
        LinkedList<String>premenne =new LinkedList<>();
        LinkedList<String>hodnoty=new LinkedList<>();
        String prem="",hodnota="";
        for(String s:list){
            String[] casti=s.split("=");
            prem=casti[0];
            hodnota=casti[1];

            if(!premenne.contains(prem)){
                premenne.add(prem);
            }
            if(!hodnoty.contains(hodnota)){
                hodnoty.add(hodnota);
            }
        }
        return hodnoty.size()==premenne.size();
    }

    private boolean special(Pravidlo pravidlo) {
        String[] str=pravidlo.getAk();
        for(String check:str)
            if(check!=null && check.contains("<>"))
                return true;
        return false;
    }

    public LinkedList<LinkedList<String>> zhoda(Pravidlo pravidlo) {
        //praca pre jedno pravidlo
        String naviazania = "";
        String[] pol = pravidlo.getAk();
        for (String x : pol) {//pre vsetky elementarne podmienky
            if (x == null) break;//ak prazdna podmienka break
            naviazania += elementarnaZhoda(x);//pridam hodnoty premennych
        }
        naviazania = naviazania.substring(0, naviazania.length() - 1);
        //naviazania spraven=e ako dvojice tak, zenormalne medzery oddeluju dvojice pri elementarnom pravidle
        //dve medzery oddeluju medzery dalsiu dvojicu
        //\n oddeluje dalsiu elementarnu podmienku
        String[][][] naviazaniaNew = parseNav(naviazania, pravidlo);
        LinkedList<LinkedList<String>> naviazania2 = kombinuj(naviazaniaNew, pravidlo);
        if(naviazania2==null) return null;
        System.out.println(pravidlo.getMeno());
        for (LinkedList<String> list : naviazania2) {
            for (String str : list)
                System.out.print(str);
            System.out.println();
        }
        return naviazania2;
        //System.out.println(pravidlo.getMeno());
        //System.out.println(naviazania);
    }

    //dostane pole stringov naviazani, vrati len take pole, ktore bude obsahovat
    //take stringy ktore maju spravne premnenne
    private String[] osetriNaviazanie(String[] naviazania2) {
        //oratam stringy
        int counter = 0;
        for (String str : naviazania2) {
            if (str != null) counter++;
            else break;
        }
        String[] returnArray = new String[counter];
        for (int i = 0; i < counter; i++)
            returnArray[i] = "";
        int iter = 0;
        for (String str : naviazania2) {
            //pre jeden riadok
            if (iter == counter) break;
            String[] parsedStr = str.split(" ");//{?X=peter},{?Y=Jano}...
            LinkedList<String> list = new LinkedList<String>();
            LinkedList<String> listEnd = new LinkedList<String>();
            for (String x : parsedStr)
                list.add(x);

            //list - b B a B
            //listComplete- A
            while (list.size() != 0) {

                listEnd.add(list.remove(0));//do druheho listu pridam prvy prvok, lebo ten proste musi platit
                for (String pom : listEnd) {
                    String[] parsedListEnd = pom.split("=");
                    for (String pom2 : list) {
                        String[] parsedList = pom2.split("=");
                        if (parsedListEnd[0].compareTo(parsedList[0]) == 0) {
                            list.remove(pom2);
                        }
                    }
                }

            }
            while (!listEnd.isEmpty())
                returnArray[iter] += listEnd.remove();
            iter++;
        }

        return returnArray;
    }

    private String[][][] parseNav(String naviazania, Pravidlo pravidlo) {
        //rozseka naviazania do trojrozmerneho pola stringo
        //prvy rozmer pre pravidla
        //ntice v jednom pravidle
        //konkretna jednoica
        int countEnter = 0, count2 = 0, count = 0;
        int aktCount = 0, aktCount2 = 0;
        int finalCounter2, finalCounter;
        for (String x : naviazania.split("\n")) {
            countEnter++;
            for (String y : x.split("  ")) {
                if (y.compareTo("") != 0) {
                    aktCount2++;
                    for (String z : y.split(" ")) {
                        if (z.compareTo("") != 0)
                            aktCount++;
                    }
                }
                if (aktCount > count)
                    count = aktCount;
                aktCount = 0;
            }
            if (aktCount2 > count2)
                count2 = aktCount2;
            aktCount2 = 0;
        }
        String[][][] pom = new String[countEnter][count2][count];
        finalCounter2 = count2;
        finalCounter = count;
        countEnter = 0;
        count2 = 0;
        count = 0;
        for (String x : naviazania.split("\n")) {
            for (String y : x.split("  ")) {
                if (y.compareTo("") != 0)
                    for (String z : y.split(" ")) {
                        if (z.compareTo("") != 0)
                            pom[countEnter][count2][count++] = z;
                    }
                count = 0;
                count2++;
            }
            count2 = 0;
            countEnter++;
        }
        System.out.println(pravidlo.getMeno());
        ;
        for (int i = 0; i < countEnter; i++) {
            for (int j = 0; j < finalCounter2; j++) {
                //System.out.println();
                for (int k = 0; k < finalCounter; k++)
                    if (pom[i][j][k] != null)
                        System.out.println(k + ": " + pom[i][j][k]);
            }
            //System.out.println();
        }
        pravidlo.setCount(finalCounter);
        pravidlo.setCount2(finalCounter2);
        pravidlo.setCountEnter(countEnter);
        return pom;
    }

    private LinkedList<LinkedList<String>> kombinuj(String[][][] naviazania, Pravidlo pravidlo) {
        //precistim string od s znakov
        String[][][] pom = new String[pravidlo.getCountEnter()][pravidlo.getCount2()][pravidlo.getCount()];

        for (int i = 0; i < pravidlo.getCountEnter(); i++) {
            for (int j = 0; j < pravidlo.getCount2(); j++)
                for (int k = 0; k < pravidlo.getCount(); k++) {
                    if (naviazania[i][j][k] != null && naviazania[i][j][k].compareTo("s") != 0)
                        pom[i][j][k] = naviazania[i][j][k];
                }
        }
        //ked mam prvy riadok typu pom[0][j][k], tak zlepim ten riadok j vzdy a k nemu pridam riadok 0++ zlepeny
        String[] pomString = new String[pravidlo.getCount2()];
        for (int i = 0; i < pravidlo.getCount2(); i++)
            pomString[i] = "";
//        for (int j = 0; j < pravidlo.getCount2(); j++)
//            for (int k = 0; k < pravidlo.getCount(); k++) {
//                if (k == pravidlo.getCount() - 1)
//                    pomString[j] += pom[0][j][k];
//                else
//                    pomString[j] += pom[0][j][k] + " ";
//            }

        LinkedList<LinkedList<String>> prvePravidlo = new LinkedList<>();
        LinkedList<LinkedList<String>> druhePravidlo = new LinkedList<>();
        for (int i = 0; i < pom[0].length; i++) {
            prvePravidlo.add(new LinkedList<>());
            for (int j = 0; j < pom[0][i].length; j++) {
                if (pom[0][i][j] == null) break;
                prvePravidlo.get(i).add(pom[0][i][j]);
            }
            if(prvePravidlo.getLast().size()==0)
                prvePravidlo.removeLast();
        }
        for (LinkedList<String> list : prvePravidlo) {
            if (list.size() == 0) return null;
        }
        int ko = 645 * 54;
        for (int i = 0; i < pom[1].length; i++) {
            if (pom[1][i][0] == null) break;
            druhePravidlo.add(new LinkedList<>());
            for (int j = 0; j < pom[1][i].length; j++) {
                if (pom[1][i][j] == null) {
                    break;
                }
                druhePravidlo.get(i).add(pom[1][i][j]);
            }
        }
        for (LinkedList<String> list : druhePravidlo) {
            if (list.size() == 0) return null;
        }


        LinkedList<LinkedList<String>> kombinacie = doCombine(prvePravidlo, druhePravidlo);
        LinkedList<LinkedList<String>> retVal = new LinkedList<>();

        //musim prejst tento lost a skontrolovat ci sa premenne a ich hodnoty rovnaju, ak nie tak blbost
        for (LinkedList<String> list : kombinacie) {
            LinkedList<String> listik = korekcia(list);
            if (listik != null)
                retVal.add(listik);
        }

        return retVal;
    }

    private LinkedList<String> korekcia(LinkedList<String> list) {
        LinkedList<String> p = new LinkedList<>();
        LinkedList<String> p1 = new LinkedList<>();
        for (String str : list) {
            if (!p.contains(str))
                p.add(str);
        }
        for (String str : list) {
            String[] arr = str.split("=");
            if (!p1.contains(arr[0]))
                p1.add(arr[0]);
        }
        if (p.size() == p1.size())
            return p;
        else return null;
    }

    private LinkedList<LinkedList<String>> doCombine(LinkedList<LinkedList<String>> prvePravidlo, LinkedList<LinkedList<String>> druhePravidlo) {
        LinkedList<LinkedList<String>> retVal = new LinkedList<>();
        int iterator = 0;
        for (LinkedList<String> first : prvePravidlo) {
            for (LinkedList<String> second : druhePravidlo) {
                retVal.add(new LinkedList<String>());
                for (String str1 : first) {
                    retVal.get(iterator).add(str1);
                }
                for (String str2 : second) {
                    retVal.get(iterator).add(str2);
                }
                iterator++;
            }
        }
        return retVal;
    }

    ///Zisti podobu elementarnej podmienky velkej podmienky v zavislosti na fakty
    private String elementarnaZhoda(String x) {
        if (x.contains("<>")) return "s ";
        String polePremennych = "";
        int pom = 0;
        for (Fakt elemPodm : this.fakty)//pre kazdy fakt
        {
            pom = 0;
            String polePredbeznychPremennych = "";
            String[] faktSplit = elemPodm.getCelyFakt().split(" ");
            String[] pravidloSplit = x.split(" ");
            if (faktSplit.length != pravidloSplit.length) continue;
            else {
                //prezriem pole tam kde su rovnake ok, tam kde nie ak je ? ulozim si predbezne
                //ako premennu, a ak takto dalej pridem s tym ze nerovnake ked su tak
                //podmienka ma otaznik tak si to ulozim ze ok
                for (int i = 0; i < pravidloSplit.length; i++) {
                    //kod na zistenie premennych z kazdeho faktu pre vsetky elem podm. pravidiel
                    if (pravidloSplit[i].compareTo(faktSplit[i]) != 0) {
                        if (pravidloSplit[i].contains("?")) {
                            polePredbeznychPremennych += pravidloSplit[i] + "=" + faktSplit[i] + " ";
                        } else {
                            pom = 1;
                            break;
                        }
                    }
                }
            }
            if (pom == 0)
                polePremennych += polePredbeznychPremennych + ' ';
        }
        polePremennych += '\n';
        return polePremennych;
    }

    public void loadRules() {
        this.rules.clear();
        File file = new File("rules.txt");
        BufferedReader reader = null;
        String str;
        try {
            reader = new BufferedReader(new FileReader(file));

            while ((str = reader.readLine()) != null) {
                this.rules.appendText(str);
                this.rules.appendText("\n");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
            }
        }
    }

    public void loadFacts() {
        this.facts.clear();
        File file = new File("facts.txt");
        BufferedReader reader = null;
        String str;
        try {
            reader = new BufferedReader(new FileReader(file));

            while ((str = reader.readLine()) != null) {
                this.facts.appendText(str);
                this.facts.appendText("\n");
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
            }
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadRules();
        loadFacts();
    }

    public LinkedList<Pravidlo> getPravidla() {
        return pravidla;
    }

    public void setPravidla(LinkedList<Pravidlo> pravidla) {
        this.pravidla = pravidla;
    }

    public void vypis() {

    }
}
