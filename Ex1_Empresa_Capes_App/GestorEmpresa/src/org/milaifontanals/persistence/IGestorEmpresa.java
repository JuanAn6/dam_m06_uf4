/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package org.milaifontanals.persistence;

import org.milaifontanals.empresa.Departament;
import org.milaifontanals.empresa.Empleat;
import org.milaifontanals.empresa.Empresa;

/**
 *
 * @author Juan Antonio
 */
public interface IGestorEmpresa {

    /**
     * Tanca la capa de persistència, tancant la connexió amb la BD.
     *
     * @throws GestorEmpresaException si hi ha algun problema en tancar la connexió
     */
    void closeCapa();
    
    
    /**
     * Tanca la transacció activa validant els canvis a la BD.
     *
     * @throws GestorEmpresaException si hi ha algun problema
     */
    public void commit();

    /**
     * Tanca la transacció activa sense validar els canvis a la BD.
     *
     * @throws GestorEmpresaException si hi ha algun problema
     */
    public void rollback();
    

    /**
     * Facilita el nombre de subordinats d'un empleat
     *
     * @param codi de l'empleat al què comptabilitzar els subordinats
     * @return El nombre de subordinats de l'empleat
     * @throws GestorEmpresaException si es produeix algun error (introduir un codi
     * inadequat o la inexistència d'empleat amb el codi indicat genera també
     * aquesta excepció)
     */
    int comptarSubordinats(int codi);

    /**
     * Intenta eliminar un empleat de codi indicat per paràmetre.
     *
     * @param codi de l'empleat a eliminar
     * @param actCap: Indica l'actuació a tenir en compte vers els empleats dels
     * que pugui ser el cap. No té cap efecte si l'empleat no és cap de ningú.
     * Actuacions:<BR>
     * Valor 0: Deixar els empleats que el tenen per cap, "SENSE" cap<BR>
     * Valor &lt;0: Generar excepció indicant que no ha estat possible
     * l'eliminació per ser cap d'altres empleats<BR>
     * Valor &gt;0: Canviar el cap dels empleats afectats per l'empleat amb codi
     * = al valor del paràmetre actCap (que ha d'existir)<BR>
     *
     */
    void eliminarEmpleat(int codi, int actCap);

    /**
     * Comprova si un empleat és subordinat directe o indirecte (varis nivells)
     * del cap
     *
     * @param cap
     * @param emp
     * @return true si emp és subordinat (directe o indirecte) de cap i false en
     * cas contrari
     */
    boolean esSubordinatDirecteIndirecte(int cap, int emp);

    /**
     * Informa de l'existència d'empleat amb el codi indicat per paràmetre.
     *
     * @return Valor booleà que indica l'existència de l'empleat indicat
     * @throws GestorEmpresaException si es produeix algun error (introduir un codi
     * inadequat genera també aquesta excepció)
     */
    boolean existeixEmpleat(int codi);

    /**
     * Retorna el departament de codi indicat, si existix o null en cas
     * contrari.
     *
     * @param codi Codi del departament a obtenir
     * @return El departament o null
     * @throws GestorEmpresaException si es produeix algun error.
     */
    Departament getDepartament(int codi);

    /**
     * Intenta recuperar l'empleat amb codi indicat per paràmetre.
     *
     * @param codi de l'empleat a recuperar
     * @return L'objecte Empleat trobat o null si no existeix
     * @throws GestorEmpresaException si es produeix algun error (introduir un codi
     * inadequat genera també aquesta excepció)
     */
    Empleat getEmpleat(int codi);

    /**
     * Obté l'objecte empresa
     *
     * @return Empresa
     * @throws GestorEmpresaException si hi ha algun problema en recuperar l'empresa
     */
    Empresa getEmpresa();
    
}
