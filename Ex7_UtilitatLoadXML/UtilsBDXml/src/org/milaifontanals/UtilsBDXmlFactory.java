package org.milaifontanals;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Usuari
 */
public class UtilsBDXmlFactory {

    private UtilsBDXmlFactory() {

    }

    /**
     * Invoca el constructor sense paràmetres del component
     *
     * @param nomCapa
     * @return
     * @throws org.milaifontanals.UtilsBDXmlException
     */
    public static IUtilsBDXml getUtilsBDXml(String nomCapa)
            throws UtilsBDXmlException {
        try {
            Class compo = Class.forName(nomCapa);
            IUtilsBDXml cp = (IUtilsBDXml) compo.newInstance();
            return cp;
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            throw new UtilsBDXmlException("Error en invocar getUtilsBDXml", ex);
        }
    }

    /**
     * Invoca el constructor amb 1 paràmetre del component
     *
     * @param nomCapa
     * @param nomFitxerPropietatsCapa
     * @return
     * @throws org.milaifontanals.UtilsBDXmlException
     */
    public static IUtilsBDXml getUtilsBDXml(String nomCapa, String nomFitxerPropietatsCapa)
            throws UtilsBDXmlException {

        try {
            Class classe = Class.forName(nomCapa);
            Constructor con = classe.getConstructor(String.class);
            IUtilsBDXml cp = (IUtilsBDXml) con.newInstance(nomFitxerPropietatsCapa);
            return cp;
        } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            throw new UtilsBDXmlException("Error en invocar getUtilsBDXml", ex);
        }

    }

}
