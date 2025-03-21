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
import org.basex.api.client.ClientSession;

/**
 * Capa de persistència que implementa IUtilsLoadXml usant l'api BaseX.
 *
 * L'aplicació que usi la capa de persistència ha de disposar d'un fitxer de
 * propietats (veure documentació dels constructors) accessible via load(Reader)
 * amb les propietats següents:<BR>
 * host : Màquina (nom / ip) on és el SGBD BaseX<BR>
 * port : Port pel que escolta el SGBD BaseX<BR>
 * user : Usuari<BR>
 * password: Contrasenya<BR>
 * nameDB: Nom de la BD
 *
 * @author Isidre Guixà
 */
public class LoadXmlBaseX implements IUtilsBDXml {

    private ClientSession con;
    private String nameDB;

    /**
     * Constructor que estableix connexió amb el servidor a partir de les dades
     * informades en fitxer de propietats de nom connexioBaseX.properties.
     *
     * @throws UtilsBDXmlException si hi ha algun problema en el fitxer de
     * configuració
     */
    public LoadXmlBaseX() throws UtilsBDXmlException {
        this("connexioBaseX.properties");
    }

    /**
     * Constructor que estableix connexió amb el servidor a partir de les dades
     * informades en fitxer de propietats, i en cas de ser null cercarà el
     * fitxer de nom connexioBaseX.properties.
     *
     * @param nomFitxerPropietats
     * @throws UtilsBDXmlException si hi ha algun problema en el fitxer de
     * configuració o si no s'estableix la connexió
     */
    public LoadXmlBaseX(String nomFitxerPropietats) throws UtilsBDXmlException {
        if (nomFitxerPropietats == null || nomFitxerPropietats.equals("")) {
            nomFitxerPropietats = "connexioBaseX.properties";
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
        String user = props.getProperty("user");
        String password = props.getProperty("password");

        nameDB = props.getProperty("nameDB");
        if (nameDB == null || nameDB.equals("")) {
            throw new UtilsBDXmlException("Manquen el nom de la base de dades");
        }

        try {
            con = new ClientSession(host, port, user, password);
        } catch (IOException ex) {
            throw new UtilsBDXmlException("No s'ha pogut establir connexió", ex);
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
            } catch (IOException ex) {
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
            con.execute("open " + nameDB);
        } catch (IOException ioe) {
            throw new UtilsBDXmlException("No es pot obrir la BD " + nameDB, ioe);
        }
        try {
            InputStream is;
            try {
                is = new FileInputStream(xml);
            } catch (IOException e) {
                throw new UtilsBDXmlException("Error en intentar accedir al fitxer " + xml.getAbsolutePath(), e);
            }
            if (collection == null || "".equals(collection)) {
                con.add(newName, is);
            } else {
                con.add(collection + "/" + newName, is);
            }
        } catch (IOException ioe) {
            throw new UtilsBDXmlException("Error en executar la càrrega de fitxer", ioe);
        } finally {
            try {
                con.execute("close");     // Tanca la BD oberta
            } catch (IOException ioe) {
                throw new UtilsBDXmlException("No es pot tancar la BD", ioe);
            }
        }
    }

    @Override
    public void commit() throws UtilsBDXmlException {
    }

    @Override
    public void rollback() throws UtilsBDXmlException {
    }

}
