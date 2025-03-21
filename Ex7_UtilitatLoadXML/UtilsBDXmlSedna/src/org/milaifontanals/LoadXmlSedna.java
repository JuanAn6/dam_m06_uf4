/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.milaifontanals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import ru.ispras.sedna.driver.DatabaseManager;
import ru.ispras.sedna.driver.DriverException;
import ru.ispras.sedna.driver.SednaConnection;
import ru.ispras.sedna.driver.SednaStatement;

/**
 * Capa de persistència que implementa IUtilsLoadXml usant l'api Sedna.
 *
 * L'aplicació que usi la capa de persistència ha de disposar d'un fitxer de
 * propietats (veure documentació dels constructors) accessible via load(Reader)
 * amb les propietats següents:<BR>
 * host : Màquina (nom / ip) on és el SGBD Sedna<BR>
 * port : Port pel que escolta el SGBD Sedna<BR>
 * bd : Nom de la base de dades<BR>
 * user : Usuari<BR>
 * password: Contrasenya
 *
 * @author Isidre Guixà
 */
public class LoadXmlSedna implements IUtilsBDXml {

    private SednaConnection con;
    /* Variable per control de transaccions */
    private boolean transOn;

    /**
     * Constructor que estableix connexió amb el servidor a partir de les dades
     * informades en fitxer de propietats de nom connexioSedna.properties.
     *
     * @throws UtilsBDXmlException si hi ha algun problema en el fitxer de
     * configuració
     */
    public LoadXmlSedna() throws UtilsBDXmlException {
        this("connexioSedna.properties");
    }

    /**
     * Constructor que estableix connexió amb el servidor a partir de les dades
     * informades en fitxer de propietats, i en cas de ser null cercarà el
     * fitxer de nom connexioSedna.properties.
     *
     * @param nomFitxerPropietats
     * @throws UtilsBDXmlException si hi ha algun problema en el fitxer de
     * configuració o si no s'estableix la connexió
     */
    public LoadXmlSedna(String nomFitxerPropietats) throws UtilsBDXmlException {
        if (nomFitxerPropietats == null) {
            nomFitxerPropietats = "EPSedna.properties";
        }
        Properties props = new Properties();
        try {
            props.load(new FileReader(nomFitxerPropietats));
        } catch (FileNotFoundException ex) {
            throw new UtilsBDXmlException("No es troba fitxer de propietats", ex);
        } catch (IOException ex) {
            throw new UtilsBDXmlException("Error en carregar fitxer de propietats", ex);
        }

        String host = props.getProperty("host");
        int port = 0;
        try {
            port = Integer.parseInt(props.getProperty("port"));
        } catch (NumberFormatException ex) {
            throw new UtilsBDXmlException("Port obligatori i amb valor enter vàlid");
        }
        String bd = props.getProperty("bd");
        String user = props.getProperty("user");
        String password = props.getProperty("password");
        try {
            con = DatabaseManager.getConnection(host + ":" + port, bd, user, password);
        } catch (DriverException ex) {
            throw new UtilsBDXmlException("No s'ha pogut establir connexió", ex);
        }
        SednaStatement q = null;
    }

    /**
     * Tanca la transacció activa validant els canvis a la BD.
     *
     * @throws UtilsBDXmlException si hi ha algun problema
     */
    @Override
    public void commit() throws UtilsBDXmlException {
        if (transOn) {
            try {
                con.commit();
                transOn = false;
            } catch (DriverException ex) {
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
        if (transOn) {
            try {
                con.rollback();
                transOn = false;
            } catch (DriverException ex) {
                transOn = false;
                throw new UtilsBDXmlException("Error en fer rollback.", ex);
            }
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
            } catch (DriverException ex) {
                throw new UtilsBDXmlException("Error en tancar la connexió", ex);
            } finally {
                con = null;
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
            InputStream is;
            try {
                is = new FileInputStream(xml);
            } catch (IOException e) {
                throw new RuntimeException("Error en intentar accedir al fitxer!");
            }
            obrirTrans();
            SednaStatement st = con.createStatement();
            if (collection == null || "".equals(collection)) {
                st.loadDocument(is, newName);
            } else {
                try {
                    st.execute("CREATE COLLECTION '" + collection + "'");
                } catch (Exception drex) {
                    /* Apareix excepció SE2002 si la col·lecció ja existeix */
                    if (drex.getMessage().contains("SE2002")) {
                        // No fem res... La col·l3cció ja existia...
                    } else {
                        throw drex;  // Per si fós alguna altra excepció
                    }
                    con.begin();
                    /* L'excepció anterior tanca la transacció i
			 cal tornar-la a iniciar */
                }
                st.loadDocument(is, newName, collection);
            }
        } catch (DriverException e) {
            transOn=false;
            throw new UtilsBDXmlException("Error en executar la càrrega de document", e);
        } catch (Exception e) {
            throw new UtilsBDXmlException("Error en executar la càrrega de document", e);
        }
    }

    /**
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
