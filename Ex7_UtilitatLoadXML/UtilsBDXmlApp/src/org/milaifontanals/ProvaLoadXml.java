/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.milaifontanals;

import java.io.File;

/**
 *
 * @author Usuari
 */
public class ProvaLoadXml {

    /* Objectiu d'aquest programa:
        Comprovar que pot carregar un fitxer XML dins una BD
        indicant una col·lecció concreta o sense col·lecció
        utilitzant la utilitat load de la classe UtilsBDXml
     */
    /**
     *
     * @param args[0] => Nom de la capa a invocar (Obligatori)
     * @param args[1] => Nom de fitxer de propietats per la capa (Optatiu). Si
     * no hi és, cal invocar constructor de capa sense paràmetres. Si existeix,
     * cal inocar constructor de capa amb 1 paràmetre
     * (nomFitxerConfiguracióCapa).
     *
     */
    public static void main(String[] args) {

        if (args.length == 0 || args.length > 2) {
            System.out.println("Aquest programa necessita d'1 o 2 paràmetres");
            System.out.println("Paràmetre 1: nom de la capa a usar (obligatori)");
            System.out.println("Paràmetre 2: nom de fitxer configuració per la capa (optatiu)");
            System.out.println("Avortem execució");
            return;
        }

        IUtilsBDXml capa;
        try {
            if (args.length == 1) {
                capa = UtilsBDXmlFactory.getUtilsBDXml(args[0]);
            } else {
                capa = UtilsBDXmlFactory.getUtilsBDXml(args[0], args[1]);
            }
        } catch (UtilsBDXmlException ex) {
            System.out.println("Error en invocar la factoria");
            infoError(ex);
            System.out.println("Avortem programa");
            return;
        }
        // Capa creada - Ja podem comprovar funcionament de mètodes 
        try {
            // Comprovació de càrrega de fitxer XML a l'arrel de la BD
            capa.load(".." + File.separator + "empresa.xml", "ProvaLoad.xml", null);
            System.out.println("Prova càrrega a l'arrel efectuada");
        } catch (UtilsBDXmlException ex) {
            System.out.println("Error en càrrega a l'arrel");
            infoError(ex);
        }

        try {
            // Comprovació de càrrega de fitxer XML en una jerarquia de col·leccions
            capa.load(".." + File.separator + "empresa.xml", "ProvaLoad.xml",
                    "colA/colB");
            System.out.println("Prova càrrega dins col·lecció efectuada");
        } catch (UtilsBDXmlException ex) {
            System.out.println("Error en càrrega dins col·lecció");
            infoError(ex);
        }

        try {
            capa.commit();
        } catch (UtilsBDXmlException ex) {
            System.out.println("No s'ha pogut fer el commit");
            infoError(ex);
        }
        
        try {
            capa.close();
        } catch (UtilsBDXmlException ex) {
            System.out.println("Problemes en tancar la capa...");
            System.out.println("... però acabem igualment");
        }
    }

    private static void infoError(Throwable aux) {
        do {
            if (aux.getMessage() != null) {
                System.out.println("\t" + aux.getMessage());
            }
            aux = aux.getCause();
        } while (aux != null);
    }

}
