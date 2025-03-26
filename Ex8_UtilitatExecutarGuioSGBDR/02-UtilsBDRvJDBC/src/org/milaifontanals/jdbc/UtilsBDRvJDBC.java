/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.milaifontanals.jdbc;

import org.milaifontanals.UtilsBDRException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
import java.util.Scanner;
import org.milaifontanals.IUtilsBDR;

/**
 * Component que implementa IUtilsBDR via tecnologia JDBC.
 *
 * L'aplicació que usi la capa de persistència ha de disposar d'un fitxer de
 * propietats accessible via load(Reader) amb les propietats següents:<BR>
 * url (oblitagòria) amb la URL segons sintaxi del driver JDBC<BR>
 * user (optatiu si el SGBD no el necessita)<BR>
 * password (optatiu si el SGBD no el necessita)<BR>
 * driver (obligatori si el driver és anterior a JDBC4.0)
 *
 * @author Isidre Guixà
 */
public class UtilsBDRvJDBC implements IUtilsBDR {

    private Connection con;

    /**
     * Constructor que estableix connexió amb el servidor a partir de les dades
     * informades en fitxer de propietats de nom UtilsBDRvJDBC.properties.
     *
     * @throws UtilsBDRException si hi ha algun problema en el fitxer de
     * propietats o en establir la connexió
     */
    public UtilsBDRvJDBC() throws UtilsBDRException {
        this("UtilsBDRvJDBC.properties");
    }

    /**
     * Constructor que estableix connexió amb el servidor a partir de les dades
     * informades en fitxer de propietats, i en cas de ser null cercarà el
     * fitxer de nom UtilsBDRvJDBC.properties.
     *
     */
    public UtilsBDRvJDBC(String nomFitxerPropietats) throws UtilsBDRException {
        if (nomFitxerPropietats == null || nomFitxerPropietats.equals("")) {
            nomFitxerPropietats = "UtilsBDRvJDBC.properties";
        }
        Properties p = new Properties();
        try {
            p.load(new FileInputStream(nomFitxerPropietats));
        } catch (IOException ex) {
            throw new UtilsBDRException("Error en llegir de fitxer de propietats", ex);
        }
        String url = p.getProperty("url");
        if (url == null || url.length() == 0) {
            throw new UtilsBDRException("Fitxer de propietats " + nomFitxerPropietats + " no inclou propietat \"url\"");
        }
        String user = p.getProperty("user");
        String password = p.getProperty("password");
        String driver = p.getProperty("driver");    // optatiu
        // Si ens passen driver, ens estan dient que l'hem de carregar
        // Si no ens passen driver, no l'hem de carregar (suposat >= JDBC 4.0)
        if (driver != null && driver.length() > 0) {
            try {
                Class.forName(driver).newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                throw new UtilsBDRException("Problemes en carregar el driver ", ex);
            }
        }
        try {
            if (user != null && user.length() > 0) {
                con = DriverManager.getConnection(url, user, password);
            } else {
                con = DriverManager.getConnection(url);
            }
        } catch (SQLException ex) {
            throw new UtilsBDRException("Problemes en establir la connexió ", ex);
        }
        try {
            con.setAutoCommit(false);
        } catch (SQLException ex) {
            throw new UtilsBDRException("Problemes en desactivar autocommit ", ex);
        }
    }

    @Override
    public void execute(File script) throws UtilsBDRException {
        Scanner sc = null;
        try {
            sc = new Scanner(script);
        } catch (FileNotFoundException ex) {
            throw new UtilsBDRException("Problemes en llegir guió ", ex);
        }
        sc.useDelimiter(";");
        String inst = null;
        Statement st = null;
        try {
            st = con.createStatement();
            while (sc.hasNext()) {
                inst = sc.next();
                // La següent expressió regular elimina els comentaris unilinia
                // tant si n'hi ha un com varis, però amb -- al principi de línia
                // NO sap tractar comentaris que comencin a mitja línia
                // En cas de voler-los tractar, cal tenir en compte que hi pot haver
                // símbols -- enmig de cometes simples, que no serien comentaris i 
                // no s'hauria de tocar
                // També queden pendents de tractar els comentaris multilínia /* */
                // La gestió de comentaris no és simple... L'scanner ens està donant 
                // les instruccions trencades per ";" i... la gestió és incorrecta 
                // si enmig d'un comentari unilinia o multilínia hi ha un ";"...
                inst = inst.replaceAll("(^--|[\n]+--)(.)*(\\n|\\r|$)", "\n");
                inst = inst.trim();
                if (inst.length() == 0) {
                    continue;       // La saltem
                }
                if (inst.toLowerCase().startsWith("select ")) {
                    continue;       // La saltem
                }
                // Executarem inst; 
                System.out.println(inst);
                try {
                    st.executeUpdate(inst);
                } catch (SQLException ex) {
                    if (!inst.toLowerCase().startsWith("drop ")) {
                        throw ex;
                    }
                }
            }
        } catch (SQLException ex) {
            boolean rollback = true;
            try {
                con.rollback();
            } catch (SQLException ex1) {
                rollback = false;
            }
            if (rollback == false) {
                throw new UtilsBDRException("Problemes en executar guió i no s'ha pogut efectuar rollback", ex);
            } else {
                throw new UtilsBDRException("Problemes en executar guió ", ex);
            }
        } finally {
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException ex) {
                    throw new UtilsBDRException("Error en tancar sentència utilitzada per executar el guió.", ex);
                }
            }
            sc.close();
        }
    }

    @Override
    public void execute(String script) throws UtilsBDRException {
        execute(new File(script));
    }

    @Override
    public void close() throws UtilsBDRException {
        if (con != null) {
            try {
                con.rollback();
                con.close();
            } catch (SQLException ex) {
                throw new UtilsBDRException("Problemes en tancar la connexió ", ex);
            }
            con = null;
        }
    }

    @Override
    public void closeTransaction(char typeClose) throws UtilsBDRException {
        typeClose = Character.toUpperCase(typeClose);
        if (typeClose != 'C' && typeClose != 'R') {
            throw new UtilsBDRException("Paràmetre " + typeClose + " erroni en closeTransaction");
        }
        if (typeClose == 'C') {
            try {
                con.commit();
            } catch (SQLException ex) {
                throw new UtilsBDRException("Error en fer commit", ex);
            }
        } else {
            try {
                con.rollback();
            } catch (SQLException ex) {
                throw new UtilsBDRException("Error en fer rollback", ex);
            }
        }
    }

}
