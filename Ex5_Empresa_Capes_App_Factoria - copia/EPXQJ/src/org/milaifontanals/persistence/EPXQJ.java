/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.milaifontanals.persistence;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.xml.namespace.QName;
import javax.xml.xquery.XQConnection;
import javax.xml.xquery.XQDataSource;
import javax.xml.xquery.XQException;
import javax.xml.xquery.XQExpression;
import javax.xml.xquery.XQItemType;
import javax.xml.xquery.XQPreparedExpression;
import javax.xml.xquery.XQResultSequence;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.milaifontanals.empresa.Departament;
import org.milaifontanals.empresa.Empleat;
import org.milaifontanals.empresa.Empresa;

/**
 * Capa de persistència per la gestió de l'empresa amb els seus departaments i
 * empleats, segons el model definit en el projecte Empresa.
 *
 * L'aplicació que usi la capa de persistència ha de disposar d'un fitxer de
 * propietats (veure documentació dels constructors) accessible via load(Reader)
 * amb les propietats següents:<BR>
 * className: Nom de la classe XQDataSource del driver XQJ per connectar<BR>
 * path: Ruta del fitxer XML en el que es basa la capa de persistència
 * incorporant el nom de la BD en format adequat segons el SGBD<BR>
 * transactional: Per saber si el SGBD és o no és transaccional (Y/N)<BR>
 * updateVersion: Per saber el tipus de llenguatge d'actualització a usar
 * (PL/XQUF)<BR>
 * i les propietats específiques de connexió al SGBD, segons driver a usar<BR>
 *
 * @author Isidre Guixà
 */
public class EPXQJ implements IGestorEmpresa{

    private XQConnection con;

    /* Variables que informarem a partir dels fitxers de configuració */
    private String path;
    private String transactional;
    private String updateVersion;

    /* Variable per control de transaccions */
    private boolean transOn;

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    // Estructures de memòria per guardar els objectes carregats en memòria
    private Empresa empresa;
    private HashMap<Integer, Departament> hmDept = new HashMap();
    private HashMap<Integer, Empleat> hmEmp = new HashMap();

    // Consultes preparades
    private XQItemType xqitStr = null;

    private XQPreparedExpression xqpeDept = null;
    private XQPreparedExpression xqpeEmp = null;
    private XQPreparedExpression xqpeExistEmp = null;
    private XQPreparedExpression xqpeQtySub = null;
    private XQPreparedExpression xqpeSubordinatsDirectes = null;

    /**
     * Constructor que estableix connexió amb el servidor a partir de les dades
     * informades en fitxer de propietats de nom EPXQJ.properties.
     *
     * @throws GestorEmpresaException si hi ha algun problema en el fitxer de
     * configuració o si no s'estableix la connexió
     */
    public EPXQJ() {
        this("EPXQJ.properties");
    }

    /**
     * Constructor que estableix connexió amb el servidor a partir de les dades
     * informades en fitxer de propietats, i en cas de ser null o buit cercarà
     * el fitxer de nom EPXQJ.properties.
     *
     * @param nomFitxerPropietats
     * @throws GestorEmpresaException si hi ha algun problema en el fitxer de
     * configuració o si no s'estableix la connexió
     */
    public EPXQJ(String nomFitxerPropietats) {
        if (nomFitxerPropietats == null || nomFitxerPropietats.equals("")) {
            nomFitxerPropietats = "EPXQJ.properties";
        }
        Properties props = new Properties();
        try {
            props.load(new FileInputStream(nomFitxerPropietats));
        } catch (FileNotFoundException ex) {
            throw new GestorEmpresaException("No es troba el fitxer de propietats " + nomFitxerPropietats, ex);
        } catch (IOException ex) {
            throw new GestorEmpresaException("Error en intentar carregar el fitxer de propietats " + nomFitxerPropietats, ex);
        }
        String className = props.getProperty("className");
        if (className == null || className.equals("")) {
            throw new GestorEmpresaException("Error en carregar variable className de fitxer de configuració");
        }
        props.remove("className");

        path = props.getProperty("path");
        if (path == null || path.equals("")) {
            throw new GestorEmpresaException("El fitxer de propietats no incorpora la propietat path amb valor adequat");
        }
        props.remove("path");

        updateVersion = props.getProperty("updateVersion");
        if (updateVersion == null || updateVersion.equals("")) {
            throw new GestorEmpresaException("El fitxer de propietats no incorpora la propietat updateVersion amb valor adequat");
        }
        if (!updateVersion.equals("PL") && !updateVersion.equals("XQUF")) {
            throw new GestorEmpresaException("El fitxer de propietats no incorpora la propietat updateVersion amb valor adequat");
        }
        props.remove("updateVersion");

        transactional = props.getProperty("transactional");
        if (transactional == null || transactional.equals("")) {
            throw new GestorEmpresaException("El fitxer de propietats no incorpora la propietat transactional amb valor adequat");
        }
        if (!transactional.equals("N") && !transactional.equals("Y")) {
            throw new GestorEmpresaException("El fitxer de propietats no incorpora la propietat transactional amb valor adequat");
        }
        props.remove("transactional");

        XQDataSource xqs = null;
        try {
            xqs = (XQDataSource) Class.forName(className).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            throw new GestorEmpresaException("Error en usar la classe " + className, ex);
        }
        try {
            xqs.setProperties(props);
            con = xqs.getConnection();
            if (transactional.equals("Y")) {
                // Desactivem autocommit, que per defecte, en XQJ està activat
                con.setAutoCommit(false);
            }
        } catch (XQException ex) {
            throw new GestorEmpresaException("Error en intentar establir connexió", ex);
        }

        XQExpression q = null;

        try {
            transOn = true;
            q = con.createExpression();
//            XQResultSequence xqrs = q.executeQuery(path + "/x");
            // En BaseX i Sedna, una instrucció anterior "peta" si el path és erroni
            // Però en eXist-db no "peta". Simplement com que no troba el que se li demana
            // retorna un XQResultSequence "buit". Per això cal fer una consulta demanant
            // una dada que tinguem la seguretat que existeixi, com el nom de l'empresa
            // i posteriorment, via xqrs.next() veure que existeix.
            // En el cas d'eXist-db l'execute "no peta" i continua per la instruccií "if"
            // següent. Si entra dins aquest "if" podem concloure que el path és erroni
            // i provocar la mateixa excepció que en BaseX i Sedna té lloc en l'executeQuery.
            XQResultSequence xqrs = q.executeQuery(path + "/empresa/nom/string()");
            if (!xqrs.next()) {
                closeCapa();
                throw new GestorEmpresaException("Error en comprovar existència de " + path + " en el SGBD.");
            }
        } catch (XQException ex) {
            closeCapa();
            throw new GestorEmpresaException("Error en comprovar existència de " + path + " en el SGBD.", ex);
        } finally {
            if (con != null) {
                tancarExpression(q);
            }
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
            } catch (XQException ex) {
                if (!ex.getMessage().contains("SE4611")) {
                    throw new GestorEmpresaException("Error en tancar la connexió", ex);
                }
            } finally {
                con = null;
                transOn = false;
            }
        }
    }

    /**
     * Tanca la transacció activa validant els canvis a la BD.
     *
     * @throws GestorEmpresaException si hi ha algun problema
     */
    public void commit() {
        if (transactional.equals("Y") && transOn) {
            try {
                con.commit();
                transOn = false;
            } catch (XQException ex) {
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
        if (transactional.equals("Y") && transOn) {
            try {
                con.rollback();
                transOn = false;
            } catch (XQException ex) {
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
        XQExpression q = null;
        try {
            transOn = true;
            q = con.createExpression();
            XQResultSequence xqrs = q.executeQuery(path + "/empresa/nom/string()");
            xqrs.next();
            String nom = xqrs.getItemAsString(null);
            xqrs = q.executeQuery(path + "/empresa/dataCreacio/string()");
            xqrs.next();
            String aux = xqrs.getItemAsString(null);
            Calendar dataCreacio = new GregorianCalendar();
            dataCreacio.setTime(sdf.parse(aux));
            empresa = new Empresa(nom, dataCreacio);
            return empresa;
        } catch (XQException ex) {
            transOn = false;
            throw new GestorEmpresaException("Error en getEmpresa", ex);
        } catch (Exception ex) {
            throw new GestorEmpresaException("Error en getEmpresa", ex);
        } finally {
            tancarExpression(q);
        }
    }

    /**
     * Intenta recuperar el departament amb codi indicat per paràmetre.
     *
     * @param codi del departament a recuperar
     * @return L'objecte Departament trobat o null si no existeix
     * @throws GestorEmpresaException si es produeix algun error (introduir un codi
     * inadequat genera també aquesta excepció)
     */
    public Departament getDepartament(int codi) {
        if (codi <= 0) {
            throw new GestorEmpresaException("Intent de cercar un departament amb codi " + codi + " inadequat");
        }
        Departament d = hmDept.get(codi);
        if (d != null) {
            return d;
        }
        // No el tenim en memòria... El cerquem a la BD
        if (xqpeDept == null) {
            String cad = "declare variable $codi external;";
            cad = cad + path + "//departaments/dept[@codi=$codi]";
            try {
                xqpeDept = con.prepareExpression(cad);
            } catch (XQException ex) {
                transOn = false;
                throw new GestorEmpresaException("Error al crear el prepared expression", ex);
            }
        }

        String aux = "d" + codi;
        try {
            transOn = true;
            xqpeDept.bindString(new QName("codi"), aux, xqitStr);
            XQResultSequence xqrs = xqpeDept.executeQuery();

            if (xqrs.next() == false) {
                return null;
            }

            String resultat = xqrs.getItemAsString(null);
            // Dins "resultat" hi ha un node <dept>
            SAXBuilder sb = new SAXBuilder();
            Document doc = sb.build(new StringReader(resultat));
            Element xDept = doc.getRootElement();
            String nom = xDept.getChildText("nom");
            String localitat = xDept.getChildText("localitat");
            d = new Departament(codi, nom, localitat);
            hmDept.put(codi, d);
            return d;
        } catch (XQException ex) {
            transOn = false;
            throw new GestorEmpresaException("Problema en intentar recuperar departament amb " + codi, ex);
        } catch (Exception ex) {
            throw new GestorEmpresaException("Problema en intentar recuperar departament amb " + codi, ex);
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
        if (codi <= 0) {
            throw new GestorEmpresaException("Intent de cercar un empleat amb codi " + codi + " inadequat");
        }
        Empleat e = hmEmp.get(codi);
        if (e != null) {
            return e;
        }
        if (xqpeEmp == null) {
            String cad = "declare variable $codi external;";
            cad = cad + path + "//empleats/emp[@codi=$codi]";
            try {
                xqpeEmp = con.prepareExpression(cad);
            } catch (XQException ex) {
                transOn = false;
                throw new GestorEmpresaException("Error al crear el prepared expression", ex);
            }
        }

        String aux = "e" + codi;
        try {
            transOn = true;
            xqpeEmp.bindString(new QName("codi"), aux, xqitStr);
            XQResultSequence xqrs = xqpeEmp.executeQuery();

            if (xqrs.next() == false) {
                return null;
            }

            String resultat = xqrs.getItemAsString(null);
            // Dins resultat hi ha un node <emp>
            SAXBuilder sb = new SAXBuilder();
            Document doc = sb.build(new StringReader(resultat));
            Element xEmp = doc.getRootElement();
            String cognom = xEmp.getChildText("cognom");
            String ofici = xEmp.getChildText("ofici");
            String sDataAlta = xEmp.getChildText("dataAlta");
            Calendar auxDataAlta = null;
            if (sDataAlta != null && sDataAlta.length() != 0) {
                try {
                    auxDataAlta = new GregorianCalendar();
                    auxDataAlta.setTime(sdf.parse(sDataAlta));
                } catch (ParseException ex) {
                }
            }
            String sComissio = xEmp.getChildText("comissio");
            String sSalari = xEmp.getChildText("salari");
            String sDept = xEmp.getAttributeValue("dept");
            Departament auxDept = null;
            if (sDept != null) {
                auxDept = getDepartament(Integer.parseInt(sDept.substring(1)));
            }
            String sCap = xEmp.getAttributeValue("cap");
            Empleat auxCap = null;
            if (sCap != null) {
                auxCap = getEmpleat(Integer.parseInt(sCap.substring(1)));
            }
            e = new Empleat(codi, cognom, ofici, auxDataAlta,
                    sSalari == null || sSalari.length() == 0 ? null : Double.parseDouble(sSalari),
                    sComissio == null || sComissio.length() == 0 ? null : Double.parseDouble(sComissio),
                    auxCap, auxDept);
            hmEmp.put(codi, e);    // S'afegeix a la llista d'objectes en memòria
            return e;
        } catch (XQException ex) {
            transOn = false;
            throw new GestorEmpresaException("Problema en intentar recuperar empleat amb codi " + codi, ex);
        } catch (Exception ex) {
            throw new GestorEmpresaException("Problema en intentar recuperar empleat amb codi " + codi, ex);
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
        if (codi <= 0) {
            throw new GestorEmpresaException("Intent de cercar un empleat amb codi " + codi + " inadequat");
        }
        if (hmEmp.containsKey(codi)) {
            return true;
        }
        if (xqpeExistEmp == null) {
            String cad = "declare variable $codi external;";
            cad = cad + path + "//emp[@codi=$codi]/@codi";
            try {
                xqpeExistEmp = con.prepareExpression(cad);
            } catch (XQException ex) {
                transOn = false;
                throw new GestorEmpresaException("Error al crear el prepared expression", ex);
            }
        }

        String aux = "e" + codi;
        try {
            transOn = true;
            xqpeExistEmp.bindString(new QName("codi"), aux, xqitStr);
            XQResultSequence xqrs = xqpeExistEmp.executeQuery();
            return xqrs.next();

        } catch (XQException ex) {
            transOn = false;
            throw new GestorEmpresaException("Problema en comprovar existència d'empleat amb codi " + codi, ex);
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
        if (codi <= 0) {
            throw new GestorEmpresaException("Intent de comptar subordinats d'empleat amb codi " + codi + " inadequat");
        }
        if (!existeixEmpleat(codi)) {
            throw new GestorEmpresaException("Intent de comptar empleats d'empleat amb codi " + codi + " inexistent");
        }
        // Arribats aquí, tenim la certesa de que existeix empleat amb codi indicat
        if (xqpeQtySub == null) {
            String cad = "declare variable $codi external;";
            cad = cad + "count(" + path + "//emp[@cap=$codi])";
            try {
                xqpeQtySub = con.prepareExpression(cad);
            } catch (XQException ex) {
                transOn = false;
                throw new GestorEmpresaException("Error al crear el prepared expression", ex);
            }
        }

        String aux = "e" + codi;
        try {
            transOn = true;
            xqpeQtySub.bindString(new QName("codi"), aux, xqitStr);
            XQResultSequence xqrs = xqpeQtySub.executeQuery();
            xqrs.next();
//            return Integer.parseInt(xqrs.getItemAsString(null));
            return xqrs.getInt();
        } catch (XQException ex) {
            transOn = false;
            throw new GestorEmpresaException("Problema en comprovar existència d'empleat amb codi " + codi, ex);
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

        /* PreparedExpression que facilita tots els subordinats directes de l'empleat $codi*/
        if (xqpeSubordinatsDirectes == null) {
            String cad = "declare variable $codi external;";
            cad = cad + path + "//emp[@cap=$codi]/@codi/string()";
            try {
                xqpeSubordinatsDirectes = con.prepareExpression(cad);
            } catch (XQException ex) {
                transOn = false;
                throw new GestorEmpresaException("Error al crear el prepared expression", ex);
            }
        }
        String eCap = "e" + cap;
        try {
            transOn = true;
            xqpeSubordinatsDirectes.bindString(new QName("codi"), eCap, xqitStr);
            XQResultSequence xqrs = xqpeSubordinatsDirectes.executeQuery();
            // Ens guardem tots els subordinats en una llista
            List<String> subordinats = new ArrayList();
            while (xqrs.next()) {
                subordinats.add(xqrs.getItemAsString(null));
            }
            if (subordinats.isEmpty()) {
                return false;
            }

            for (String eCodiBis : subordinats) {
                int codi = Integer.parseInt(eCodiBis.substring(1));
                if (codi == emp) {
                    return true;
                } else {
                    boolean aux = esSubordinatDirecteIndirecte(codi, emp);
                    if (aux) {
                        return aux;
                    }
                }
            }
            return false;
        } catch (XQException ex) {
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
        if (updateVersion.equals("XQUF")) {
            eliminarEmpleatXQUF(codi, actCap);
        } else {
            eliminarEmpleatPL(codi, actCap);
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
    }

    private void eliminarEmpleatXQUF(int codi, int actCap) {
        // Sabem que l'empleat a eliminar existeix
        String cad = null;
        if (actCap == 0) {
            /* Deixar els subordinats sense cap i eliminar l'empleat */
            // Instrucció per eliminar dels subordinats, l'atribut "cap": 
            cad = "delete node " + path + "//emp[@cap='e" + codi + "']/@cap";
            cad = cad + ",";
            // Instrucció per eliminar el propi empleat
            cad = cad + "delete node " + path + "//emp[@codi='e" + codi + "']";
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
            /* Cal canviar el cap als subordinats directes i eliminar l'empleat */
//            cad = "replace value of node " + path
//                    + "//emp[@cap='e" + codi + "']/@cap with 'e" + actCap + "'\n";
//          La instrucció anterior només funcionaria quan ha de canviar el valor de l'atribut
//          en un ÚNIC node!!! 
//          Com que pot ser en varis nodes (depèn dels subordinats de l'empleat, cal fer-ho amb FLWOR
            cad = "for $n in " + path + "//emp[@cap='e" + codi + "']\n"
                    + "return replace value of node $n/@cap with 'e" + actCap + "'\n";
            cad = cad + ",";
            // Instrucció per eliminar el propi empleat
            cad = cad + "delete node " + path + "//emp[@codi='e" + codi + "']";
            // cad conté la seqûència d'instruccions per canviar el cap als subordinats
            // i eliminar l'empleat
        }
        XQExpression q = null;
        try {
            q = con.createExpression();
            q.executeQuery(cad);
        } catch (XQException ex) {
            transOn = false;
            throw new GestorEmpresaException("Problema en intentar eliminar empleat amb codi " + codi, ex);
        } catch (Exception ex) {
            throw new GestorEmpresaException("Problema en intentar eliminar empleat amb codi " + codi, ex);
        } finally {
            tancarExpression(q);
        }
    }

    private void eliminarEmpleatPL(int codi, int actCap) {
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
               ALERTA!!! XAPUÇA!!! Però la instrucció "update replace" no és 
               igual per a tots els SGBD-XML de tipus PL
               Preguntem al SGBD si és Sedna, per usar "update replace $n in..."
               i si és PL però no és Sedna, usarem "update replace ..." sense "$n in"
             */
            String aux = "";
            try {
                if (con.getMetaData().getProductName().startsWith("Sedna")) {
                    /* Manera de saber que es tracta de Sedna... */
                    aux = "$n in ";
                }
            } catch (XQException ex) {
            }
            llCad.add("update replace " +aux + path + "//emp[@cap='e" + codi + "']/@cap\n"
                    + "with (attribute cap {'e" + actCap + "'})");
            // Instrucció per eliminar el propi empleat
            llCad.add("update delete " + path + "//emp[@codi='e" + codi + "']");
            // cad conté la seqûència d'instruccions per canviar el cap als subordinats
            // i eliminar l'empleat
        }
        XQExpression q = null;
        try {
            q = con.createExpression();
            for (String cad : llCad) {
                q.executeCommand(cad);
            }
        } catch (XQException ex) {
            transOn = false;
            throw new GestorEmpresaException("Problema en intentar eliminar empleat amb codi " + codi, ex);
        } catch (Exception ex) {
            throw new GestorEmpresaException("Problema en intentar eliminar empleat amb codi " + codi, ex);
        } finally {
            tancarExpression(q);
        }
        
    }
    /*
     *
     * MÈTODES PRIVATS, d'ajut pel desenvolupament de la capa
     *
     */
    /**
     * Tanca la query indicada per paràmetre.
     *
     * @param q Query a tancar
     */
    private void tancarExpression(XQExpression q) {
        if (q != null) {
            try {
                q.close();
            } catch (XQException ex) {
                throw new GestorEmpresaException("Error en close xqexpression", ex);
            }
        }
    }

}
