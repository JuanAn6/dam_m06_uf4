/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.milaifontanals.proves;

import org.milaifontanals.ProvaLoadXml;

/**
 * Comprova capa LoadXmlSedna usant un fitxer configuració per la capa
 * @author Usuari
 */

public class ProvaCapaSedna02 {
    public static void main(String[] args) {
        String t[] = {"org.milaifontanals.LoadXmlSedna",
                      "connexioSedna.properties"};
        ProvaLoadXml.main(t);
    }
}
