/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.milaifontanals;

interface IComponentCas1 {
    // Suposem que aquesta és la interfície que defineix 
    // els mètodes que han de contenir qualsevol dels components
    // a usar
}

// Exemple de com invocar constructor sense paràmetres de qualsevol component
// que implementi la interfíci
public class Cas1InvocarDinamicamentConstructorSenseParàmetres {

    public static void main(String[] args) {
        try {
            String nomClasseDelComponent = null;

            IComponentCas1 cp;      // Referència per apuntar al component que es vol crear
            Class compo = Class.forName(nomClasseDelComponent);
            // compo ja és la classe del compoment a usar
            // l'hem carregat dinàmicament - no hi ha cap "import" dins el codi
            cp = (IComponentCas1) compo.newInstance();
            // Forma d'incocar el constructor sense paràmetres
            // cp ja apunta a objecte del component
        } catch (ClassNotFoundException ex) {
            // Acció que correspongui
        } catch (InstantiationException ex) {
            // Acció que correspongui
        } catch (IllegalAccessException ex) {
            // Acció que correspongui
        }
    }

}
