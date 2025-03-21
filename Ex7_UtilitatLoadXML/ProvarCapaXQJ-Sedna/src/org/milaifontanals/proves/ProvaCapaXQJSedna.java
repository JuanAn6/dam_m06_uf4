/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.milaifontanals.proves;

import org.milaifontanals.ProvaLoadXml;

/**
 * Comprova capa LoadXmlXQJ per Sedna usant un fitxer configuració per la capa
 * @author Usuari
 */

public class ProvaCapaXQJSedna {
    public static void main(String[] args) {
        String t[] = {"org.milaifontanals.LoadXmlXQJ",
                      "connexioXQJ-Sedna.properties"};
        ProvaLoadXml.main(t);
    }
}
