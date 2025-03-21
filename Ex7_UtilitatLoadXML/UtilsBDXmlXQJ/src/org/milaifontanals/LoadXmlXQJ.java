/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.milaifontanals;

import com.xqj2.XQConnection2;
import com.xqj2.XQInsertOptions;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Properties;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xquery.XQDataSource;
import javax.xml.xquery.XQException;
import javax.xml.xquery.XQExpression;
import javax.xml.xquery.XQItem;

/**
 * Capa de persistència que implementa IUtilsLoadXml usant l'api XQJ2.
 *
 * L'aplicació que usi la capa de persistència ha de disposar d'un fitxer de
 * propietats (veure documentació dels constructors) accessible via load(Reader)
 * amb les propietats següents:<BR>
 * className: Nom de la classe XQDataSource del driver XQJ per connectar<BR>
 * transactional: Per saber si el SGBD és o no és transaccional (Y/N)<BR>
 * updateVersion: Per saber el tipus de llenguatge d'actualització a usar
 * (PL/XQUF)<BR>
 * i les propietats específiques de connexió al SGBD, segons driver a usar<BR>
 *
 * @author Isidre Guixà
 */
public class LoadXmlXQJ implements IUtilsBDXml {

    private XQConnection2 con;
    /* Variable per control de transaccions */
    private boolean transOn;

    private String transactional;
    private String updateVersion;

    /**
     * Constructor que estableix connexió amb el servidor a partir de les dades
     * informades en fitxer de propietats de nom connexioXQJ.properties.
     *
     * @throws UtilsBDXmlException si hi ha algun problema en el fitxer de
     * configuració
     */
    public LoadXmlXQJ() throws UtilsBDXmlException {
        this("connexioXQJ.properties");
    }

    /**
     * Constructor que estableix connexió amb el servidor a partir de les dades
     * informades en fitxer de propietats, i en cas de ser null cercarà el
     * fitxer de nom connexioXQJ.properties.
     *
     * @param nomFitxerPropietats
     * @throws UtilsBDXmlException si hi ha algun problema en el fitxer de
     * configuració o si no s'estableix la connexió
     */
    public LoadXmlXQJ(String nomFitxerPropietats) throws UtilsBDXmlException {
        if (nomFitxerPropietats == null) {
            nomFitxerPropietats = "connexioXQJ.properties";
        }
        Properties props = new Properties();
        try {
            props.load(new FileReader(nomFitxerPropietats));
        } catch (FileNotFoundException ex) {
            throw new UtilsBDXmlException("No es troba fitxer de propietats", ex);
        } catch (IOException ex) {
            throw new UtilsBDXmlException("Error en carregar fitxer de propietats", ex);
        }

        String className = props.getProperty("className");
        if (className == null || className.length() == 0) {
            throw new UtilsBDXmlException("Error en carregar fitxer de propietats");
        }
        props.remove("className");

        updateVersion = props.getProperty("updateVersion");
        if (updateVersion == null) {
            throw new UtilsBDXmlException("Error en carregar fitxer de propietats");
        }
        if (!updateVersion.equals("PL") && !updateVersion.equals("XQUF")) {
            throw new UtilsBDXmlException("Error en carregar fitxer de propietats");
        }
        props.remove("updateVersion");

        transactional = props.getProperty("transactional");
        if (transactional == null) {
            throw new UtilsBDXmlException("Error en carregar fitxer de propietats");
        }
        if (!transactional.equals("N") && !transactional.equals("Y")) {
            throw new UtilsBDXmlException("Error en carregar fitxer de propietats");
        }
        props.remove("transactional");

        XQDataSource xqs;
        try {
            xqs = (XQDataSource) Class.forName(className).newInstance();
            xqs.setProperties(props);
            con = (XQConnection2) xqs.getConnection();
            if (transactional.equals("Y")) {
                // Desactivem autocommit, que per defecte, en XQJ està activat
                con.setAutoCommit(false);
            }
        } catch (XQException ex) {
            throw new UtilsBDXmlException("Error en intentar establir connexió.", ex);
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            throw new UtilsBDXmlException("Error en obrir la connexió.", ex);
        }
    }

    /**
     * Tanca la capa de persistència, tancant la connexió amb la BD.
     *
     * @throws UtilsBDXmlException si hi ha algun problema en tancar la connexió
     */
    @Override
    public void close() throws UtilsBDXmlException {
        if (con != null) {
            try {
                con.close();
            } catch (XQException ex) {
                if (!ex.getMessage().contains("SE4611")) {
                    throw new UtilsBDXmlException("Error en tancar la connexió", ex);
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
     * @throws UtilsBDXmlException si hi ha algun problema
     */
    @Override
    public void commit() throws UtilsBDXmlException {
        if (transactional.equals("Y") && transOn) {
            try {
                con.commit();
                transOn = false;
            } catch (XQException ex) {
                transOn = false;
                throw new UtilsBDXmlException("Error en fer commit.", ex);
            }
        }
    }

    /**
     * Tanca la transacció activa sense validar els canvis a la BD.
     *
     * @throws UtilsBDXmlException si hi ha algun problema
     */
    @Override
    public void rollback() throws UtilsBDXmlException {
        if (transactional.equals("Y") && transOn) {
            try {
                con.rollback();
                transOn = false;
            } catch (XQException ex) {
                transOn = false;
                throw new UtilsBDXmlException("Error en fer rollback.", ex);
            }
        }
    }

    @Override
    public void load(String xml, String newName, String collection) throws UtilsBDXmlException {
        load(new File(xml), newName, collection);
    }

    @Override
    public void load(File xml, String newName, String collection) throws UtilsBDXmlException {
        try {
            StreamSource ss;
            try {
                ss = new StreamSource(new FileInputStream(xml));
            } catch (IOException e) {
                throw new RuntimeException("Error en intentar accedir al fitxer!", e);
            }
            transOn = true;   // Després només es tindrà en compte pels SGBD transaccionals

            XQItem doc = con.createItemFromDocument(ss, null);
            // En Sedna, el mètode XQDataFactory.createItemFromDocument(java.io.Reader,String,XQItemType)
            // falla en carregar arxiu mondial.xml. S'ha contactat amb
            // Charles Foster (09/05/2018) que ha recomanat utilitzar, mentre no aparegui nou driver XQJ Sedna
            // XQDataFactory.createItemFromDocument(javax.xml.transform.Source, XQItemType)

            if (collection == null || "".equals(collection)) {
                con.insertItem(newName, doc, null);
            } else // Els SGBD BaseX i eXist-db incorporen la col·lecció a la URI del document
            // En aquests SGBD, la utilització del mètode insertItem incorporant la col·lecció en
            // el primer argument (URI del document) funciona perfectament. Si la col·lecció existeix,
            // hi fica el document i si no existeix, crea la col·lecció i hi fica el document.
            // Però en Sedna, el nom de la col·lecció és independent i no es pot incorporar a la URI...
            // La proposta XQJ2 de C.Foster incorpora una interfície XQInsertOptions amb dos mètodes:
            // getEncoding i SetEncoding... La implementació XQJ de Sedna incorpora la classe 
            // SednaXQInsertOptions que implementa aquesta interfície i, que a més a més dels mètodes
            // obligats per la interfície, té els mètodes setCollection i getCollection per poder indicar
            // quina és la col·lecció "activa". Però aquests mètodes no estan dins la interfície i, per tant,
            // per poder-ho incorporar en aquest component que ha de servir per a tots els SGBD amb XQJ, ens
            // cal fer un muntatge que... Déu n'hi doret.
            {
                if (con.getMetaData().getProductName().contains("Sedna")) {
                    // Estem connectats a un SGBD Sedna                    
                    try {
                        XQExpression xqe = con.createExpression();
                        xqe.executeCommand("CREATE COLLECTION '" + collection + "'");
                    } catch (XQException xqe) {
                        /* Apareix excepció XQJSE002 si la col·leció ja existeix */
                        if (xqe.getMessage().contains("XQJSE002")) {
                            // No fem res... La col·lecció ja existia...
                        } else {
                            throw xqe;  // Per si és alguna altra excepció
                        }
                    }
                    // Arribats aquí, sabem que la collection existeix.
                    // Hem de dir que la volem utilitzar
                    XQInsertOptions xqi = (XQInsertOptions) Class.forName("net.xqj.sedna.SednaXQInsertOptions").newInstance();
                    // Ens cal utilitzar "reflection" per poder executar el mètode setCollection() que sabem que existirà...
                    Method m = Class.forName("net.xqj.sedna.SednaXQInsertOptions").getMethod("setCollection", String.class);
                    m.invoke(xqi, collection);
                    // Hem indicat a Sedna la collection amb la que treballarem
                    // Ara ja procedim amb l'insertItem, sense introduir el nom de la col·lecció
                    con.insertItem(newName, doc, xqi);
                } else {
                    // Manera senzilla de treballar amb BaseX i eXist-db
                    con.insertItem(collection + "/" + newName, doc, null);
                }
            }
        } catch (XQException e) {
            transOn = false;
            throw new UtilsBDXmlException("Error: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new UtilsBDXmlException("Error: " + e.getMessage(), e);
        }
    }

}
