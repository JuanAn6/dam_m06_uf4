/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.milaifontanals.persistence;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.milaifontanals.empresa.Departament;
import org.milaifontanals.empresa.Empleat;
import org.milaifontanals.empresa.Empresa;
import ru.ispras.sedna.driver.DatabaseManager;
import ru.ispras.sedna.driver.DriverException;
import ru.ispras.sedna.driver.SednaConnection;
import ru.ispras.sedna.driver.SednaSerializedResult;
import ru.ispras.sedna.driver.SednaStatement;

/**
 * Capa de persistència per la gestió de l'empresa amb els seus departaments i
 * empleats, segons el model definit en el projecte Empresa.
 *
 * L'aplicació que usi la capa de persistència ha de disposar d'un fitxer de
 * propietats (veure documentació dels constructors) accessible via load(Reader)
 * amb les propietats següents:<BR>
 * host : Màquina (nom / ip) on és el SGBD Sedna<BR>
 * port : Port pel que escolta el SGBD Sedna<BR>
 * database: Nom de la base de dades<BR>
 * user : Usuari<BR>
 * password: Contrasenya<BR>
 * path: Ruta del fitxer XML en el que es basa la capa de persistència en format
 * doc("nomFitxerXml","nomCol·lecció") o doc("nomFitxerXml") si no és dins cap
 * col·lecció.<BR>
 * Les dades es troben en un unic fitxer que segueix empresa.dtd
 *
 * @author Isidre Guixà
 */
public class EPSedna implements IGestorEmpresa {

    private SednaConnection con;
    private String path;

    /* Variable per control de transaccions */
    private boolean transOn;

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    // Estructures de memòria per guardar els objectes carregats en memòria
    private Empresa empresa;
    private HashMap<Integer, Departament> hmDept = new HashMap();
    private HashMap<Integer, Empleat> hmEmp = new HashMap();

    /**
     * Constructor que estableix connexió amb el servidor a partir de les dades
     * informades en fitxer de propietats de nom EPSedna.properties.
     *
     * @throws GestorEmpresaException si hi ha algun problema en el fitxer de
     * configuració o si no s'estableix la connexió
     */
    public EPSedna() {
        this("EPSedna.properties");
    }

    /**
     * Constructor que estableix connexió amb el servidor a partir de les dades
     * informades en fitxer de propietats, i en cas de ser null o buit cercarà
     * el fitxer de nom EPSedna.properties.
     *
     * @param nomFitxerPropietats
     * @throws GestorEmpresaException si hi ha algun problema en el fitxer de
     * configuració o si no s'estableix la connexió
     */
    public EPSedna(String nomFitxerPropietats) {
        if (nomFitxerPropietats == null || nomFitxerPropietats.equals("")) {
            nomFitxerPropietats = "EPSedna.properties";
        }
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(nomFitxerPropietats));
        } catch (FileNotFoundException ex) {
            throw new GestorEmpresaException("No es troba el fitxer de propietats " + nomFitxerPropietats, ex);
        } catch (IOException ex) {
            throw new GestorEmpresaException("Error en intentar carregar el fitxer de propietats " + nomFitxerPropietats, ex);
        }
        String host = props.getProperty("host");
        int port;
        try {
            port = Integer.parseInt(props.getProperty("port"));
        } catch (NumberFormatException ex) {
            throw new GestorEmpresaException("Port obligatori en fitxer de propietats i amb valor enter vàlid");
        }

        String user = props.getProperty("user");
        String password = props.getProperty("password");
        String database = props.getProperty("database");

        try {
            con = DatabaseManager.getConnection(host + ":" + port, database, user, password);
        } catch (Throwable ex) {
            throw new GestorEmpresaException("No s'ha pogut establir la connexió", ex);
        }

        path = props.getProperty("path");
        if (path == null || path.equals("")) {
            closeCapa();
            throw new GestorEmpresaException("El fitxer de propietats no incorpora la propietat path amb valor adequat");
        }

        SednaStatement q;

        try {
            q = con.createStatement();
            con.begin();
            transOn = true;
            q.execute(path + "/x");
        } catch (DriverException ex) {
            closeCapa();
            throw new GestorEmpresaException("Error en comprovar existència de " + path + " en el SGBD.", ex);
        }
    }

    /**
     * Tanca la capa de persistència, tancant la connexió amb la BD.
     *
     * @throws GestorEmpresaException si hi ha algun problema en tancar la connexió
     */
    public void closeCapa() {
        if (con != null) {
            try {
                con.close();
            } catch (DriverException ex) {
                throw new GestorEmpresaException("Error en tancar la connexió", ex);
            } finally {
                con = null;
            }
        }
    }

    /**
     * Tanca la transacció activa validant els canvis a la BD.
     *
     * @throws GestorEmpresaException si hi ha algun problema
     */
    public void commit() {
        if (transOn) {
            try {
                con.commit();
                transOn = false;
            } catch (DriverException ex) {
                transOn = false;
                throw new GestorEmpresaException("Error en fer commit.", ex);
            }
        }
    }

    /**
     * Tanca la transacció activa sense validar els canvis a la BD.
     *
     * @throws GestorEmpresaException si hi ha algun problema
     */
    public void rollback() {
        if (transOn) {
            try {
                con.rollback();
                transOn = false;
            } catch (DriverException ex) {
                transOn = false;
                throw new GestorEmpresaException("Error en fer rollback.", ex);
            }
        }
    }

    /**
     * Obté l'objecte empresa
     *
     * @return Empresa
     * @throws GestorEmpresaException si hi ha algun problema en recuperar l'empresa
     */
    public Empresa getEmpresa() {
        if (empresa != null) {
            return empresa;
        }
        SednaStatement q = null;
        try {
            obrirTrans();
            q = con.createStatement();
            q.execute(path + "/empresa/nom/string()");
            String nom = q.getSerializedResult().next();
            q.execute(path + "/empresa/dataCreacio/string()");
            String aux = q.getSerializedResult().next();
            Calendar dataCreacio = new GregorianCalendar();
            dataCreacio.setTime(sdf.parse(aux));
            empresa = new Empresa(nom, dataCreacio);
            return empresa;
        } catch (DriverException ex) {
            transOn = false;
            throw new GestorEmpresaException("Error en getEmpresa", ex);
        } catch (Exception ex) {
            throw new GestorEmpresaException("Error en getEmpresa", ex);
        }
    }

    /**
     * Retorna el departament de codi indicat, si existix o null en cas
     * contrari.
     *
     * @param codi Codi del departament a obtenir
     * @return El departament o null
     * @throws GestorEmpresaException si es produeix algun error.
     */
    public Departament getDepartament(int codi) {
//        if (codi <= 0 || codi >= 99) {
//            throw new GestorEmpresaException("Intent de recuperar departament amb codi " + codi + " erroni");
//        }
        // El codi anterior, per comprovar si el codi és correcte, implica SABER la restricció
        // i si algun dia es modifica aquesta restricció a la classe Departament, també haurem
        // de recordar retocar-la aquí. Una manera alternativa, que permet no estar pendent d'això és:
        try {
            new Departament(codi, "???", null);
        } catch (RuntimeException ex) {
            throw new GestorEmpresaException("Intent de recuperar departament amb codi " + codi + " erroni");
        }

        // Primer comprovem si ja està en memòria
        Departament d = hmDept.get(codi);
        if (d != null) {
            return d;
        }
        // Cal cercar-lo a la BD
        SednaStatement q = null;
        String cad = path + "//dept[@codi='d" + codi + "']";
//        System.out.println("Consulta a executar: " + cad);
        try {
            obrirTrans();
            q = con.createStatement();
            q.execute(cad);
            String resultat = q.getSerializedResult().next();
            if (resultat == null) {
                return null;
            }
            // Dins "resultat" hi ha un node <dept>
            SAXBuilder sb = new SAXBuilder();
            Document doc = sb.build(new StringReader(resultat));
            Element arrel = doc.getRootElement();
            String nom = arrel.getChildText("nom");
            String loc = arrel.getChildText("localitat");
            d = new Departament(codi, nom, loc);
            hmDept.put(codi, d);
            return d;
        } catch (DriverException ex) {
            transOn = false;
            throw new GestorEmpresaException("Error en intentar recuperar departament amb codi " + codi, ex);
        } catch (Exception ex) {
            throw new GestorEmpresaException("Error en intentar recuperar departament amb codi " + codi, ex);
        }
    }

    /**
     * Intenta recuperar l'empleat amb codi indicat per paràmetre.
     *
     * @param codi de l'empleat a recuperar
     * @return L'objecte Empleat trobat o null si no existeix
     * @throws GestorEmpresaException si es produeix algun error (introduir un codi
     * inadequat genera també aquesta excepció)
     */
    public Empleat getEmpleat(int codi) {
        try {
            new Empleat(codi, "???", null);
        } catch (RuntimeException ex) {
            throw new GestorEmpresaException("Intent de cercar un empleat amb codi " + codi + " erroni");
        }
        // Mirem si ja el tenim en memòria:
        Empleat e = hmEmp.get(codi);
        if (e != null) {
            return e;
        }
        // L'hem de cercar a la BD:
        String cad = path + "//emp[@codi='e" + codi + "']";
        SednaStatement q = null;
        try {
            obrirTrans();
            q = con.createStatement();
            q.execute(cad);
            String resultat = q.getSerializedResult().next();
            if (resultat == null) {
                return null;
            }
            // Dins "resultat" hi ha un node <emp>
            SAXBuilder sb = new SAXBuilder();
            Document doc = sb.build(new StringReader(resultat));
            Element xEmp = doc.getRootElement();
            String cognom = xEmp.getChildText("cognom");
            String ofici = xEmp.getChildText("ofici");
            String sDataAlta = xEmp.getChildText("dataAlta");
            Calendar dataAlta = null;
            if (sDataAlta != null && !sDataAlta.equals("")) {
                dataAlta = new GregorianCalendar();
                dataAlta.setTime(sdf.parse(sDataAlta));
            }
            String sSalari = xEmp.getChildText("salari");
            Double salari = null;
            if (sSalari != null && !sSalari.equals("")) {
                salari = Double.parseDouble(sSalari);
            }
            String sComissio = xEmp.getChildText("comissio");
            Double comissio = null;
            if (sComissio != null && !sComissio.equals("")) {
                comissio = Double.parseDouble(sComissio);
            }
            String sDept = xEmp.getAttributeValue("dept");
            Departament dept = null;
            if (sDept != null) {
                dept = getDepartament(Integer.parseInt(sDept.substring(1)));
            }
            String sCap = xEmp.getAttributeValue("cap");
            Empleat cap = null;
            if (sCap != null) {
                cap = getEmpleat(Integer.parseInt(sCap.substring(1)));
            }
            e = new Empleat(codi, cognom, ofici, dataAlta,
                    salari, comissio, cap, dept);
            hmEmp.put(codi, e);
            return e;
        } catch (DriverException ex) {
            transOn = false;
            throw new GestorEmpresaException("Error en intentar recuperar empleat de codi " + codi, ex);
        } catch (Exception ex) {
            throw new GestorEmpresaException("Error en intentar recuperar empleat de codi " + codi, ex);
        }
    }

    /**
     * Informa de l'existència d'empleat amb el codi indicat per paràmetre.
     *
     * @return Valor booleà que indica l'existència de l'empleat indicat
     * @throws GestorEmpresaException si es produeix algun error (introduir un codi
     * inadequat genera també aquesta excepció)
     */
    public boolean existeixEmpleat(int codi) {
        try {
            new Empleat(codi, "???", null);
        } catch (RuntimeException ex) {
            throw new GestorEmpresaException("Intent de comprovar existència d'un empleat amb codi " + codi + " erroni");
        }
        if (hmEmp.containsKey(codi)) {
            return true;
        }
        // Cal cercar qualsevol dada que ens permeti decidir sobre la seva existència
        String cad = path + "//emp[@codi='e" + codi + "']/@codi/string()";
        SednaStatement q = null;
        try {
            obrirTrans();
            q = con.createStatement();
            q.execute(cad);
            String resultat = q.getSerializedResult().next();
            return resultat != null;
        } catch (DriverException ex) {
            transOn = false;
            throw new GestorEmpresaException("Error en comprovar existència d'empleat de codi " + codi, ex);
        } catch (Exception ex) {
            throw new GestorEmpresaException("Error en comprovar existència d'empleat de codi " + codi, ex);
        }
    }

    /**
     * Facilita el nombre de subordinats d'un empleat
     *
     * @param codi de l'empleat al què comptabilitzar els subordinats
     * @return El nombre de subordinats de l'empleat
     * @throws GestorEmpresaException si es produeix algun error (introduir un codi
     * inadequat o la inexistència d'empleat amb el codi indicat genera també
     * aquesta excepció)
     */
    public int comptarSubordinats(int codi) {
        if (!existeixEmpleat(codi)) {
            throw new GestorEmpresaException("Intent de comptar subordinats d'empleat amb codi " + codi + " inexistent");
        }
        // Cal comptar:
        String cad = "count(" + path + "//emp[@cap='e" + codi + "'])";
//        System.out.println(cad);
        SednaStatement q = null;
        try {
            obrirTrans();
            q = con.createStatement();
            q.execute(cad);
            String resultat = q.getSerializedResult().next();
            return Integer.parseInt(resultat);
        } catch (DriverException ex) {
            transOn = false;
            throw new GestorEmpresaException("Error en comptar els subordinats d'empleat de codi " + codi, ex);
        } catch (Exception ex) {
            throw new GestorEmpresaException("Error en comptar els subordinats d'empleat de codi " + codi, ex);
        }
    }

    /**
     * Comprova si un empleat és subordinat directe o indirecte (varis nivells)
     * del cap
     *
     * @param cap
     * @param emp
     * @return true si emp és subordinat (directe o indirecte) de cap i false en
     * cas contrari
     */
    public boolean esSubordinatDirecteIndirecte(int cap, int emp) {
        if (!this.existeixEmpleat(cap)) {
            throw new GestorEmpresaException("No existeix empleat amb codi " + cap);
        }
        if (!this.existeixEmpleat(emp)) {
            throw new GestorEmpresaException("No existeix empleat amb codi " + emp);
        }
        if (cap == emp) {
            throw new GestorEmpresaException("No te sentit comprovar si un empleat és subordinat de si mateix");
        }
        /* Instrucció que facilita tots els subordinats directes del "cap" */
        String cad = path + "//emp[@cap='e" + cap + "']/@codi/string()";
        SednaStatement q = null;
        try {
            obrirTrans();
            q = con.createStatement();
            q.execute(cad);
            SednaSerializedResult s = q.getSerializedResult();
            String eCodi = s.next();
            if (eCodi == null) {
                return false;
            }
            List<String> subordinats = new ArrayList();
            while (eCodi != null) {
                subordinats.add(eCodi);
                eCodi = s.next();
            }
            /*
            ALERTA 1:
                Mentre s’està processant un SednaSerializedResult, NO es pot processar 
                un altre SednaSerializedResult. En cas de fer-ho, el primer queda en un 
                estat inconsistent, i en intentar continuar el seu procés, el programa 
                queda “penjat” (no respon). Per això hem hagut de recollir primer tots
                els codis en una llista, doncs si agafem només un codi i el processem i
                en fer la crida recursiva es torna a processar un SednaSerializedResult,
                el SednaSerializedResult anterior quedaria inconsistent.
            ALERTA 2:
                En un SednaSerializedResult, a partir del 2n resultat comencen amb
                un salt de línia, que hem d'eliminar i que és diferent segons S.O.
             */
            for (String eCodiBis : subordinats) {
                eCodiBis = eCodiBis.substring(eCodiBis.indexOf("e") + 1);
                int codi = Integer.parseInt(eCodiBis);
                if (codi == emp) {
                    return true;
                } else {
                    if (esSubordinatDirecteIndirecte(codi, emp)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (DriverException ex) {
            transOn = false;
            throw new GestorEmpresaException("Error en cercar els subordinats d'empleat de codi " + cap, ex);
        } catch (Exception ex) {
            throw new GestorEmpresaException("Error en cercar els subordinats d'empleat de codi " + cap, ex);
        }
    }

    /**
     * Intenta eliminar un empleat de codi indicat per paràmetre.
     *
     * @param codi de l'empleat a eliminar
     * @param actCap: Indica l'actuació a tenir en compte vers els empleats dels
     * que pugui ser el cap. No té cap efecte si l'empleat no és cap de ningú.
     * Actuacions:<BR>
     * Valor 0: Deixar els empleats que el tenen per cap, "SENSE" cap<BR>
     * Valor &lt;0: Generar excepció indicant que no ha estat possible
     * l'eliminació per ser cap d'altres empleats<BR>
     * Valor &gt;0: Canviar el cap dels empleats afectats per l'empleat amb codi
     * = al valor del paràmetre actCap (que ha d'existir)<BR>
     *
     */
    public void eliminarEmpleat(int codi, int actCap) {
        if (!existeixEmpleat(codi)) {
            throw new GestorEmpresaException("Intent d'eliminar empleat amb codi " + codi + " inexistent");
        }
        if (codi == actCap) {
            throw new GestorEmpresaException("Intent d'eliminar empleat amb codi " + codi
                    + " reassignant els seus subordinats a ell mateix ");
        }
        if (actCap < 0) {
            throw new GestorEmpresaException("S'ha invocat mètode eliminarEmpleat amb 2n paràmetre erroni");
        }
        // Sabem que l'empleat a eliminar existeix
        List<String> llCad = new ArrayList();
        if (actCap == 0) {
            /* Deixar els subordinats sense cap i eliminar l'empleat */
            // Instrucció per eliminar dels subordinats, l'atribut "cap": 
            llCad.add("update delete " + path + "//emp[@cap='e" + codi + "']/@cap");
            // Instrucció per eliminar el propi empleat
            llCad.add("update delete " + path + "//emp[@codi='e" + codi + "']");
            // cad conté la seqûència d'instruccions per deixar els subordinats sense cap
            // i eliminar l'empleat
        } else {
            /* Hem d'intentar substituir el cap dels subordinats (pot ser que no n'hi hagi) */
            if (!this.existeixEmpleat(actCap)) {
                throw new GestorEmpresaException("Intent d'eliminar empleat amb codi " + codi
                        + " reassignant els seus subordinats a empleat de codi " + actCap + " inexistent");
            }
            if (this.esSubordinatDirecteIndirecte(codi, actCap)) {
                throw new GestorEmpresaException("Intent d'eliminar empleat amb codi " + codi
                        + " reassignant els seus subordinats a empleat de codi " + actCap
                        + " que és un subordinat (directe o indirecte) del cap");
            }
            /* Cal canviar el cap als subordinats directes i eliminar l'empleat
               En Sedna no hi ha possibilitat de substituir el valor d'un atribut
               i cal substituir l'atribut sencer amb "update replace"
             */
            llCad.add("update replace $n in " + path + "//emp[@cap='e" + codi + "']/@cap\n"
                    + "with (attribute cap {'e" + actCap + "'})");
            // Instrucció per eliminar el propi empleat
            llCad.add("update delete " + path + "//emp[@codi='e" + codi + "']");
            // cad conté la seqûència d'instruccions per canviar el cap als subordinats
            // i eliminar l'empleat
        }
        SednaStatement q = null;
        try {
            obrirTrans();
            q = con.createStatement();
            for (String cad : llCad) {
                q.execute(cad);
            }
            hmEmp.remove(codi);
            Collection empleats = hmEmp.values();
            Iterator<Empleat> iteEmp = null;
            if (actCap == 0) {
                // Cal eliminar el cap als possibles subordinats existents en memòria
                iteEmp = empleats.iterator();
                // ALERTA! Dins "else" següent, explicat per què NO es pot referenciar iteEmp després de crear "empleats"
                while (iteEmp.hasNext()) {
                    Empleat e = iteEmp.next();
                    if (e.getCap() != null && e.getCap().getCodi() == codi) {
                        e.setCap(null);
                    }
                }
            } else {
                // Cal actualitzar el cap als possibles subordinats existents en memòria
                // Primer cal tenir la referència que apunta al nou cap
                Empleat nouCap = getEmpleat(actCap);
                // En cas que actCap no hagués estat en memòria, el mètode getEmpleat el cerca i l'afegeix a iteEmp.
                // I Java NO permet recórrer un iterator d'un HashMap que hagi estat creat abans d'una modificació del HashMap
                // Es genera una ConcurrentModificationException!!!
                iteEmp = empleats.iterator();
                while (iteEmp.hasNext()) {
                    Empleat e = iteEmp.next();
                    if (e.getCap() != null && e.getCap().getCodi() == codi) {
                        e.setCap(nouCap);
                    }
                }
            }
        } catch (DriverException ex) {
            transOn = false;
            throw new GestorEmpresaException("Problema en intentar eliminar empleat amb codi " + codi, ex);
        } catch (Exception ex) {
            throw new GestorEmpresaException("Problema en intentar eliminar empleat amb codi " + codi, ex);
        }
    }


    /*
     *
     * MÈTODES PRIVATS, d'ajut pel desenvolupament de la capa
     *
     */
    private void obrirTrans() throws DriverException {
        if (!transOn) {
            con.begin();
            transOn = true;
        }
    }
}
