package org.milaifontanals;


import java.io.File;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author Usuari
 */
public interface IUtilsBDXml {

    /**
     * Carrega l'XML passat per paràmetre, amb el nou nom indicat i dins la
     * col·lecció indicada
     *
     * @param xml Nom del fitxer que conté l'xml a carregar. Les sentències
     * SELECT seran ignorades.
     * @param newName Nom del fitxer dins la BD
     * @param collection Col·lecció on carregar-lo. Si null, a l'arrel.
     * @throws UtilsBDXmlException En cas que es produeixi alguna incidència
     */
    public void load(String xml, String newName, String collection) throws UtilsBDXmlException;

    /**
     * Carrega l'XML passat per paràmetre, amb el nou nom indicat i dins la
     * col·lecció indicada
     *
     * @param xml File que conté l'xml a carregar. Les sentències SELECT seran
     * ignorades.
     * @param newName Nom del fitxer dins la BD
     * @param collection Col·lecció on carregar-lo. Si null, a l'arrel.
     * @throws UtilsBDXmlException En cas que es produeixi alguna incidència
     */
    public void load(File xml, String newName, String collection) throws UtilsBDXmlException;

    /**
     * Valida els canvis pendents de validació, si el SGBD és transaccional
     *
     * @throws UtilsBDXmlException
     */
    public void commit() throws UtilsBDXmlException;

    /**
     * Desfa els canvis pendents de validació, si el SGBD és transaccional
     *
     * @throws UtilsBDXmlException
     */
    public void rollback() throws UtilsBDXmlException;

    /**
     * Tanca la connexió amb el SGBD, la qual s'haurà establert en crear
     * l'objecte de la classe que invoca la interfície
     *
     * @throws UtilsBDXmlException
     */
    public void close() throws UtilsBDXmlException;
}
