/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.milaifontanals.proves;

import org.milaifontanals.UtilsBDRException;
import org.milaifontanals.UtilsBDRFactoria;
import org.milaifontanals.IUtilsBDR;

/**
 *
 * @author Isidre Guixà
 */
public class ProvaExecucioGuio {

    /**
     * Programa per comprovar capes que implementen IUtilsBDR
     *
     * @param args amb 1, 2 o 3 paràmetres<BR>
     * arg[0]: Nom del component encarregat d'executar el guió<BR>
     * arg[1]: Nom del guió SQL a carregar (amb la seva ubicació)<BR>
     * arg[2]: (Optatiu) Nom de fitxer de propietats del component
     */
    public static void main(String[] args) {

        if (args.length < 2 || args.length > 3) {
            System.out.println("El programa necessita entre 2 i 3 arguments:");
            System.out.println("1r. Nom del component encarregat d'executar el guió");
            System.out.println("2n. Nom del guió SQL a carregar (amb la seva ubicació)");
            System.out.println("3è. (Optatiu) Nom de fitxer de propietats del component");
            return;
        }
        String nomClasseComponent = args[0];
        String guio = args[1];
        String nomFitxerPropietatsComponent = null;
        if (args.length == 3) {
            nomFitxerPropietatsComponent = args[2];
        }

        IUtilsBDR obj = null;
        try {
            // Hem d'aconseguir crear la capa de persistència invocant el constructor
            // de la capa que pertoqui. Per tant:
            // - Necessita conèixer el nom de la classe corresponent a la capa de persistència

            obj = UtilsBDRFactoria.getInstance(nomClasseComponent, nomFitxerPropietatsComponent);
            System.out.println("Component carregat!");
        } catch (UtilsBDRException ex) {
            System.out.println("Problema en crear el component");
            mesInfo(ex);
            System.out.println("Avortem aplicació");
            return;
        }
        try {
            obj.execute(guio);
            System.out.println("Guió executat");
            obj.closeTransaction('C');
        } catch (Exception ex) {
            System.out.println("Error:" + ex.getMessage());
            while (ex.getCause() != null) {
                ex = (Exception) ex.getCause();
                System.out.println(ex.getMessage());
            }
        } finally {
            if (obj != null) {
                try {
                    obj.closeTransaction('R');
                    obj.close();
                } catch (Exception ex) {
                    System.out.println("Error:" + ex.getMessage());
                    while (ex.getCause() != null) {
                        ex = (Exception) ex.getCause();
                        System.out.println(ex.getMessage());
                    }
                }
            }
        }

    }

    private static void mesInfo(Throwable t) {
        while (t.getCause() != null) {
            t = t.getCause();
            if (t.getMessage() != null) {
                System.out.println("\t" + t.getMessage());
            }
        }

    }
}
