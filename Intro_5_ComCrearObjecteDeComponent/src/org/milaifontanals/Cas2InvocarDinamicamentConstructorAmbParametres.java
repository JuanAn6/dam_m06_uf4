/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.milaifontanals;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

interface IComponentCas2 {
    // Suposem que aquesta és la interfície que defineix 
    // els mètodes que han de contenir qualsevol dels components
    // a usar
}

// Exemple de com invocar constructor amb paràmetres de qualsevol component
// que implementi la interfíci. Suposarem que el constructor a invocar sabem
// que té paràmetres (String p1, int p2, boolean p3)
public class Cas2InvocarDinamicamentConstructorAmbParametres {

    public static void main(String[] args) {
        try {
            String nomClasseDelComponent = null;
            // A continuació, els valors amb els que volem invocar el constructor:
            String v1 = null;
            Integer v2 = null;
            Boolean v3 = null;

            IComponentCas2 cp;      // Referència per apuntar al component que es vol crear
            Class compo = Class.forName(nomClasseDelComponent);
            // compo ja és la classe del compoment a usar
            // l'hem carregat dinàmicament - no hi ha cap "import" dins el codi
            
            // Per invocar un constructor amb paràmetres, cal demanar-li a Java que 
            // trobi el constructor indicat
            Constructor obj = compo.getConstructor(String.class, Integer.class, Boolean.class);
            // Cal indicar els tipus dels paràmetres i amb l'ordre que tenen en el constructor
            cp = (IComponentCas2) obj.newInstance(v1,v2,v3);
            // Invoquem el constructor amb els valors que corresponguin
            // cp ja apunta a objecte del component
        } catch (ClassNotFoundException ex) {
            // Acció que correspongui
        } catch (InstantiationException ex) {
            // Acció que correspongui
        } catch (IllegalAccessException ex) {
            // Acció que correspongui
        } catch (NoSuchMethodException ex) {
            // Acció que correspongui
        } catch (SecurityException ex) {
            // Acció que correspongui
        } catch (IllegalArgumentException ex) {
            // Acció que correspongui
        } catch (InvocationTargetException ex) {
            // Acció que correspongui
        }
    }

}
