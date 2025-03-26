/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.milaifontanals;

import java.io.File;

/**
 *
 * @author Usuari
 */
public interface IUtilsBDR {

    /**
     * Executa el guió passat per paràmetre
     *
     * @param script Ruta del fitxer que conté el guió a executar
     * @throws UtilsBDRException en cas que es produeixi alguna excepció en
     * executar el guió
     */
    void execute(File script) throws UtilsBDRException;
    
    /**
     * Executa el guió passat per paràmetre
     *
     * @param script Ruta del fitxer que conté el guió a executar
     * @throws UtilsBDRException en cas que es produeixi alguna excepció en
     * executar el guió
     */
    void execute(String script) throws UtilsBDRException;
    
    /**
     * Tanca la connexió amb el SGBD
     * @throws UtilsBDRException en cas que es produeixi alguna excepció 
     */
    void close () throws UtilsBDRException;
    
    /**
     * Tanca la transacció activa fent commit o rollback segons paràmetre
     * @param typeClose r o R per rollback / c o C per commit
     */
    void closeTransaction (char typeClose) throws UtilsBDRException;
    
}
