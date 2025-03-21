/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.milaifontanals;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Properties;
import org.milaifontanals.empresa.Empleat;
import org.milaifontanals.persistence.GestorEmpresaException;
import org.milaifontanals.persistence.IGestorEmpresa;

/**
 *  Aplicacio que gestiona Empresa utilitzant qualsevol capa que implmenti la interficie IGestorEmpresa
 *  Cal invocar-la amb un paràmetre que correspongui a nom de fitxer de propietats amb les propietats:
 *  nomCapa, amb el nom del component a utilitzar
 *  nomFitxerConfiguracioCapa, amb el nom del fitxer de configuració que necessita el constructor de la capa
 * @author Juan Antonio
 */
public class ProvaCapes {

    public static void main(String[] args) {
        if (args.length!=1) {
            System.out.println("L'execució d'aquest programa necessita 1 argument:");
            System.out.println("\tNom del fitxer de configuració amb la informació adequada...");
            System.exit(1);
        }
        
        Properties props = new Properties();
        
        try{
            props.load(new FileInputStream(args[0]));
        }catch(FileNotFoundException ex){
            System.out.println("No es troba el fitxer de propietats: "+args[0]);
            System.exit(2);
        }catch(IOException ex){
            System.out.println("Error en intentar carregar el fitxer de propietats: "+args[0]);
            System.exit(2);
        }
        
        
        String nomCapa = props.getProperty("nomCapa");
        String nomFitxerConfiguracioCapa = props.getProperty("nomFitxerConfiguracioCapa");
        
        System.out.println("CAPA: "+nomCapa);
        System.out.println("FITXER: "+nomFitxerConfiguracioCapa);
        
        if(nomCapa== null || nomFitxerConfiguracioCapa == null || nomCapa.length() == 0 || nomFitxerConfiguracioCapa.length() == 0 ){
            System.out.println("Contingut del fitxer de propietats "+ args[0]+ " no es correcte.");
            System.exit(4);
        }
        
        
        IGestorEmpresa cp;
        
        try {
            Class compo = Class.forName(nomCapa);
            
            Constructor c = compo.getConstructor(String.class);
            cp = (IGestorEmpresa) c.newInstance(nomFitxerConfiguracioCapa);
            
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
        int tProva[] = {10 ,-5 , 90};
        for (int codi : tProva) {
            try {
                System.out.println("Cerquem departament de codi " + codi);
                System.out.println("Departament recuperada: " + cp.getDepartament(codi));
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
        
        /*
        System.out.println("\nComprovació de comptarSubordinats");
        int tprova3[] = {-1000, 8888, 7839, 7844, 7902, 9999};
        for (int codi : tprova3) {
            System.out.print("Qtat subordinats d'empleat amb " + codi + ": ");
            try {
                int q = cp.comptarSubordinats(codi);
                System.out.println(q);
            } catch (GestorEmpresaException ex) {
                System.out.println("\nProblema en comptar subordinats de l'empleat " + codi);
                infoError(ex);
            }
        }
        */
        
        /*
        System.out.println("\nComprovació de esSubordinatDirecteIndirecte");
        int tprova4[] = {7902, 7902, 7839, 7698}; // Empleats pels que comprovarem subordinats directe/indirecte
        int emp[] = {7839, 7369, 7369, 7369}; // Empleats a mirar si són subordinats directe/indirecte
        */
        /* En situació original de la BD:
           Subordinats de 7839: 7566 - 7698 - 7782
           Subordinats de 7566: 7788 - 7902
           Subordinats de 7698: 7499 - 7521 - 7654 - 7844 - 7900
           Subordinats de 7782: 7934
           Subordinats de 7788: 7876
           Subordinats de 7902: 7369
         */
        /*
        for (int i = 0; i < tprova4.length; i++) {
            try {
                System.out.println("Empleat " + tprova4[i] + " te per subordinat dir/ind l'empleat " + emp[i] + "? " + cp.esSubordinatDirecteIndirecte(tprova4[i], emp[i]));
            } catch (GestorEmpresaException ex) {
                System.out.println("Problema en recuperar l'empleat");
                infoError(ex);
            }
        }
        */
        
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
        /*
        for (int i = 0; i < tprova5.length; i++) {
            try {
                System.out.println("Crida eliminarEmpleat(" + tprova5[i] + "," + actCap[i] + ")");
                cp.eliminarEmpleat(tprova5[i], actCap[i]);
                cp.commit();
                System.out.println("Eliminació efectuada!");
            } catch (GestorEmpresaException ex) {
                System.out.println("Problema en eliminar empleat " + tprova5[i]);
                infoError(ex);
            }
        }
        */
        
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
