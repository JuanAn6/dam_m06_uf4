/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.milaifontanals.proves;

import java.io.File;

/**
 *
 * @author Usuari
 */
public class ProvaExecucioGuioJDBCOracle {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String t[] = {"org.milaifontanals.jdbc.UtilsBDRvJDBC",
            ".." + File.separator + "EmpresaOracleMR.sql",
            "jdbc.oracle.properties"};
        ProvaExecucioGuio.main(t);
    }

}
