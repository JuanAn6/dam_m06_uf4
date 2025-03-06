/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.milaifontanals;

import org.milaifontanals.empresa.Empleat;
import org.milaifontanals.persistence.EPSedna;
import org.milaifontanals.persistence.EPSednaException;

/**
 *
 * @author Isidre Guixà
 */
public class ProvaEPSedna {

    public static void main(String[] args) {
        EPSedna cp;
        try {
            cp = new EPSedna();
            System.out.println("Connexió establerta");
        } catch (Exception ex) {
            System.out.println("Problema en crear la capa de persistència");
            infoError(ex);
            System.out.println("Avortem programa");
            return;
        }

        try {
            System.out.println("\nComprovació de getEmpresa:");
            System.out.println("Empresa recuperada: " + cp.getEmpresa());
        } catch (Exception ex) {
            infoError(ex);
        }

        System.out.println("\nComprovació de getDepartament:");
        int tProva[] = {-5, 10, 90};
        for (int codi : tProva) {
            try {
                System.out.println("Cerquem departament de codi " + codi);
                System.out.println("Empresa recuperada: " + cp.getDepartament(codi));
            } catch (Exception ex) {
                System.out.println("Problema en recuperar el departament");
                infoError(ex);
            }
        }

        System.out.println("\nComprovació de getEmpleat");
        int tprova2[] = {-1000, 8888, 7839, 7844, 9999};
        for (int codi : tprova2) {
            System.out.println("Cerquem empleat de codi " + codi + ": ");
            try {
                Empleat e = cp.getEmpleat(codi);
                System.out.println(e);
            } catch (Exception ex) {
                System.out.println("Problema en recuperar l'empleat");
                infoError(ex);
            }
        }

        System.out.println("\nComprovació de comptarSubordinats");
        int tprova3[] = {-1000, 8888, 7839, 7844, 7902, 9999};
        for (int codi : tprova3) {
            System.out.print("Qtat subordinats d'empleat amb " + codi + ": ");
            try {
                int q = cp.comptarSubordinats(codi);
                System.out.println(q);
            } catch (EPSednaException ex) {
                System.out.println("\nProblema en comptar subordinats de l'empleat " + codi);
                infoError(ex);
            }
        }

        System.out.println("\nComprovació de esSubordinatDirecteIndirecte");
        int tprova4[] = {7902, 7902, 7839, 7698}; // Empleats pels que comprovarem subordinats directe/indirecte
        int emp[] = {7839, 7369, 7369, 7369}; // Empleats a mirar si són subordinats directe/indirecte
        /* En situació original de la BD:
           Subordinats de 7839: 7566 - 7698 - 7782
           Subordinats de 7566: 7788 - 7902
           Subordinats de 7698: 7499 - 7521 - 7654 - 7844 - 7900
           Subordinats de 7782: 7934
           Subordinats de 7788: 7876
           Subordinats de 7902: 7369
         */
        for (int i = 0; i < tprova4.length; i++) {
            try {
                System.out.println("Empleat " + tprova4[i] + " te per subordinat dir/ind l'empleat " + emp[i] + "? " + cp.esSubordinatDirecteIndirecte(tprova4[i], emp[i]));
            } catch (EPSednaException ex) {
                System.out.println("Problema en recuperar l'empleat");
                infoError(ex);
            }
        }

        System.out.println("\nComprovació d'eliminar empleat");
        int tprova5[] = {-1000, 7782, 7902, 7902, 7566, 7566, 7566};
        int actCap[] = {0, 0, -1, 7900, 7788, 7876, 7698};
        /* En situació original de la BD:
            El 7782 té 1 subordinat: 7934
            El 7902 té 1 subordinat: 7369
            El 7566 té subordinat 7788 (->7876): Fem 2 proves: 
                La primera intentant substituir 7566 per subordinat directe 7788
                La segona intentant substituir 7566 per subordinat indirecte 7876
                La tercera intentant substituir 7566 per 7698 que no és subordinat (directe ni indirecte) 
        */
        for (int i = 0; i < tprova5.length; i++) {
            try {
                System.out.println("Crida eliminarEmpleat(" + tprova5[i] + "," + actCap[i] + ")");
                cp.eliminarEmpleat(tprova5[i], actCap[i]);
                System.out.println("Eliminació efectuada!");
            } catch (EPSednaException ex) {
                System.out.println("Problema en eliminar empleat " + tprova5[i]);
                infoError(ex);
            }
        }
        
        try {
            cp.closeCapa();
            System.out.println("\nCapa de persistència tancada");
        } catch (Exception ex) {
            System.out.println("Error en tancar capa de persistència");
            infoError(ex);
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
