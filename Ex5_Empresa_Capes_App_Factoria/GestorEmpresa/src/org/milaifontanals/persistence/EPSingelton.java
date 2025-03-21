/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.milaifontanals.persistence;

import java.lang.reflect.Constructor;

/**
 * Factoria per obtenir objecte dels components que implementin IGestorEmpresa.
 * Els componenets que poden ser invocats per aquesta factoria han de facilitar algun dels constructors següets:
 * - Constructor sense paràmetres.
 * - Constructor amb 1 paràmetre, corresponenet al nom del fitxer de la configuració de la capa
 * @author Juan Antonio
 */
public class EPSingelton {
    
    private static IGestorEmpresa gestor = null;
    
    private EPSingelton(){
        
    }
    
    public static IGestorEmpresa getGestorEmpresa(String nomCapa){
        if(gestor != null && !gestor.isClose()) return gestor;
        try {
            Class compo = Class.forName(nomCapa);
            return (IGestorEmpresa) compo.newInstance();
        } catch (Exception ex) {
           throw new GestorEmpresaException("Problema en crear la capa", ex);
        }
        
    }
    
    public static IGestorEmpresa getGestorEmpresa(String nomCapa, String nomFitxerConfiguracioCapa){
        if(gestor != null && !gestor.isClose()) return gestor;
        try {
            Class compo = Class.forName(nomCapa);            
            Constructor c = compo.getConstructor(String.class);
            return (IGestorEmpresa) c.newInstance(nomFitxerConfiguracioCapa);
        } catch (Exception ex) {
            throw new GestorEmpresaException("Problema en crear la capa", ex);
        }
        
    }
    
    
}
