/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.milaifontanals;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * @author Usuari
 */
public class UtilsBDRFactoria {

    protected UtilsBDRFactoria() {
    }
    
    static public IUtilsBDR getInstance(String nomClasseComponent,
            String parametrePelConstructorDelComponent) throws UtilsBDRException {
        IUtilsBDR obj = null;
        try {
            Class c = Class.forName(nomClasseComponent);
            if (parametrePelConstructorDelComponent==null ||
                    parametrePelConstructorDelComponent.length()==0) {
                obj = (IUtilsBDR) c.newInstance();
            } else {
                Constructor co = c.getConstructor(String.class);
                obj = (IUtilsBDR) co.newInstance(parametrePelConstructorDelComponent);
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException ex) {
            throw new UtilsBDRException("Error " + nomClasseComponent, ex);
        }
        return obj;
    }
}
