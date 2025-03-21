/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.milaifontanals.proves;

import org.milaifontanals.ProvaLoadXml;

/**
 * Comprova capa LoadXmlBaseX usant un fitxer configuració per la capa
 * @author Usuari
 */

public class ProvaCapaBaseX02 {
    public static void main(String[] args) {
        String t[] = {"org.milaifontanals.LoadXmlBaseX",
                      "connexioBaseX.properties"};
        ProvaLoadXml.main(t);
    }
}
