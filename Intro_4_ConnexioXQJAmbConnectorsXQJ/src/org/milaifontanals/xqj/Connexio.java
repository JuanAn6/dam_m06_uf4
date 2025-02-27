/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.milaifontanals.xqj;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import javax.xml.xquery.XQConnection;
import javax.xml.xquery.XQDataSource;
import javax.xml.xquery.XQException;

/**
 *
 * @author Usuari
 */
public class Connexio {

    /**
     * @param args the command line arguments
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
            System.out.println("Més info: "+ex.getMessage());
            System.exit(1);
        }
        // props conté totes les propietats, nom de la classe inclòs
        String className = p.getProperty("className");
        if (className == null) {
            System.out.println("El fitxer de propietats " + args[0] + " no conté propietat className obligatòria");
            return;
        }
        p.remove("className");
        // props s'ha quedat amb les propietats que necessita per establir connexió
        XQDataSource xqs = null;
        try {
            xqs = (XQDataSource) Class.forName(className).newInstance();
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            System.out.println("Error en intentar obtenir XQDataSource: " + ex.getMessage());
            System.out.println("Classe Exception: " + ex.getClass().getName());
            return;
        }
        XQConnection xq = null;
        try {
            xqs.setProperties(p);
            xq = xqs.getConnection();
            System.out.println("Connexió establerta!");
            System.out.println("Classe que implementa XQConnection: ");
            System.out.println("\t" + xq.getClass().getName());
        } catch (XQException ex) {
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
            System.out.println("Autocommit: " + xq.getAutoCommit());      // Sempre a cert després d'establir connexió
        } catch (XQException ex) {
            System.out.println("Error en esbrinar l'estat d'autocommit: " + ex.getMessage());
        }
        
        
        
        
        
        
        try {
            xq.close();
            System.out.println("Connexió tancada!");
        } catch (XQException ex) {
            System.out.println("Error en tancar connexió: " + ex.getMessage());
        }
    }

}
