/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.milaifontanals.jpa;

import java.io.File;
import org.milaifontanals.UtilsBDRException;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import jakarta.persistence.Query;
import org.milaifontanals.IUtilsBDR;

/**
 * Component que implementa IUtilsBDR via tecnologia JPA.
 *
 * L'aplicació que usi la capa de persistència ha de disposar d'un fitxer de
 * propietats accessible via load(Reader) amb les propietats següents,
 * necesàries per la unitat de persistència:<BR>
 * jakarta.persistence.jdbc.url<BR>
 * jakarta.persistence.jdbc.user<BR>
 * jakarta.persistence.jdbc.password<BR>
 * jakarta.persistence.jdbc.driver<BR>
 *
 * @author Isidre Guixà
 */
public class UtilsBDRvJPA implements IUtilsBDR {

    private EntityManager em;

    /**
     * Constructor que estableix connexió amb el servidor a partir de les dades
     * informades en fitxer de propietats de nom UtilsBDRvJPA.properties.
     *
     * @throws UtilsBDRException si hi ha algun problema en el fitxer de
     * propietats o en establir la connexió
     */
    public UtilsBDRvJPA() throws UtilsBDRException {
        this("UtilsBDRvJPA.properties");
    }

    /**
     * Constructor que estableix connexió amb el servidor a partir de les dades
     * informades en fitxer de propietats, i en cas de ser null cercarà el
     * fitxer de nom UtilsBDRvJPA.properties.
     *
     * @param nomFitxerPropietats
     * @throws UtilsBDRException si hi ha algun problema en el fitxer de
     * propietats o en establir la connexió
     */
    public UtilsBDRvJPA(String nomFitxerPropietats) throws UtilsBDRException {
        if (nomFitxerPropietats == null || nomFitxerPropietats.equals("")) {
            nomFitxerPropietats = "UtilsBDRvJPA.properties";
        }
        Properties props = new Properties();
        try {
            props.load(new FileReader(nomFitxerPropietats));
        } catch (FileNotFoundException ex) {
            throw new UtilsBDRException("No es troba fitxer de propietats " + nomFitxerPropietats, ex);
        } catch (IOException ex) {
            throw new UtilsBDRException("Error en carregar fitxer de propietats " + nomFitxerPropietats, ex);
        }

        EntityManagerFactory emf = null;
        try {
            emf = Persistence.createEntityManagerFactory("JPA", props);
//            System.out.println("EntityManagerFactory creada");
            em = emf.createEntityManager();
//            System.out.println("EntityManager creat");
        } catch (Exception ex) {
            if (emf != null) {
                try {
                    emf.close();
                } catch (Exception e) {
                }
            }
            throw new UtilsBDRException("Error en crear EntityManagerFactory o EntityManager", ex);
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
        EntityTransaction et = em.getTransaction();
        if (!et.isActive()) {
            et.begin();
        }
        try {
            while (sc.hasNext()) {
                inst = sc.next();
                // La següent expressió regular elimina els comentaris unilinia
                // tant si n'hi ha un com varis, però amb -- al principi de línia
                // NO sap tractat comentaris que comencin a mitja línia
                // En cas de voler-los tractar, cal tenir en compte que hi pot haver
                // símbols -- enmig de cometes simples, que no serien comentaris i 
                // no s'hauria de tocar
                // També queden pendents de tractar els comentaris multilínia /* */
                // La gestió de comentaris no és simple... L'scanner ens està donant 
                // les instruccions trencades per ";" i... la gestió és incorrecte 
                // si enmig d'un comentari unilinia o multilínia hi ha un ";"...
                inst = inst.replaceAll("(^--|[\n]+--)(.)*(\\n|\\r|$)", "\n");
                inst = inst.trim();
                if (inst.length() == 0) {
                    continue;       // La saltem
                }
                if (inst.toLowerCase().startsWith("select ")) {
                    continue;       // La saltem
                }
                System.out.println(inst);
                Query q = em.createNativeQuery(inst);
                try {
                    q.executeUpdate();
                } catch (Exception ex) {
                    if (!inst.toLowerCase().startsWith("drop ")) {
                        throw ex;
                    }
                }
            }
        } catch (Exception ex) {
            throw new UtilsBDRException("Problemes en executar guió ", ex);
        } finally {
            sc.close();
        }
    }

    @Override
    public void execute(String script) throws UtilsBDRException {
        execute(new File(script));
    }

    @Override
    public void close() throws UtilsBDRException {
        if (em != null) {
            EntityManagerFactory emf = em.getEntityManagerFactory();
            try {
                em.close();
                em = null;
            } catch (Exception ex) {
                throw new UtilsBDRException("Problemes en tancar l'EntityManager", ex);
            }
            try {
                emf.close();
            } catch (Exception ex) {
                throw new UtilsBDRException("Problemes en tancar l'EntityManagerFactory", ex);
            }
        }
    }

    @Override
    public void closeTransaction(char typeClose) throws UtilsBDRException {
        typeClose = Character.toUpperCase(typeClose);
        if (typeClose != 'C' && typeClose != 'R') {
            throw new UtilsBDRException("Paràmetre " + typeClose + " erroni en closeTransaction");
        }
        try {
            EntityTransaction et = em.getTransaction();
            if (et.isActive()) {
                if (typeClose == 'C') {
                    et.commit();
                } else {
                    et.rollback();
                }
            }
        } catch (Exception ex) {
            throw new UtilsBDRException("Problemes en gestionar commit/rollback", ex);
        }
    }
}
