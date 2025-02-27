/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.milaifontanals.jdbc;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 *
 * @author Usuari
 */
public class Connexio {

    /**
     * Establir connexió amb el SGBD indicat dins el fitxer de configuració, que
     * ha de ser un fitxer de propietats (format text) i contenir les propìetats
     * url, usuari, contrasenya
     *
     * @param args Ha de contenir 1 únic argument amb el nom del fitxer de
     * configuració
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Programa a invocar amb un paràmetre: nom de fitxer \"properties\" amb propietats de connexió");
            return;
        }
        Properties p = new Properties();
        try {
            p.load(new FileReader(args[0]));
        } catch (IOException ex) {
            System.out.println("Problemes en carregar el fitxer de configuració");
            System.out.println("Més info: " + ex.getMessage());
            System.exit(1);
        }
        // p conté les propietats necessàries per la connexió
        String url = p.getProperty("url");
        String usu = p.getProperty("usuari");
        String pwd = p.getProperty("contrasenya");
        if (url == null || usu == null || pwd == null) {
            System.out.println("Manca alguna de les propietats: url, usuari, contrasenya");
            return;
        }
        // Ja tenim les 3 propietats
        // Podem intentar establir connexió
        Connection con = null;
        try {
            con = DriverManager.getConnection(url, usu, pwd);
            System.out.println("Connexió establerta");
            System.out.println("Classe que implementa Connection: ");
            System.out.println("\t" + con.getClass().getName());

        } catch (SQLException ex) {
            System.out.println("Problemes en intentar la connexió:");
            Throwable t = ex;
            while (t != null) {
                if (t.getMessage() != null && t.getMessage().length() != 0) {
                    System.out.println("\t" + t.getMessage());
                }
                t = t.getCause();
            }
            return;
        }
        try {
            System.out.println("Autocommit: " + con.getAutoCommit());      // Sempre a cert després d'establir connexió
        } catch (SQLException ex) {
            System.out.println("Error en esbrinar l'estat d'autocommit: " + ex.getMessage());
        }
        // Feina amb el SGBD - Instruccions CRUD
        
        try {
            // Tancar la connexió... Aquí, excepcionalment, no cal preguntar
            // si con!=null, per què si ho és, ja haurem sortit amb exit
            con.close();
        } catch (SQLException ex) {
            System.out.println("Problemes en tancar la connexió");
            System.out.println("Més info: " + ex.getMessage());
        }
    }

}
