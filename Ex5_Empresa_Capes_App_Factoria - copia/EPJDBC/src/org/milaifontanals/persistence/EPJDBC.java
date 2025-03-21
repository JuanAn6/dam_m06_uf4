/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.milaifontanals.persistence;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import org.milaifontanals.empresa.Departament;
import org.milaifontanals.empresa.Empleat;
import org.milaifontanals.empresa.Empresa;

/**
 * Capa de persistència per la gestió de l'empresa amb els seus departaments i
 * empleats, segons el model definit en el projecte Empresa.
 *
 * L'aplicació que usi la capa de persistència ha de disposar d'un fitxer de
 * propietats (veure documentació dels constructors) accessible via load(Reader)
 * amb les propietats següents:<BR>
 * url : URL per connectar amb el SGBDR<BR>
 * user : Usuari<BR>
 * password: Contrasenya<BR>
 *
 * @author Isidre Guixà
 */
public class EPJDBC implements IGestorEmpresa {

    private Connection con;

    private Empresa empresa;
    private HashMap<Integer, Empleat> hmEmp = new HashMap();
    private HashMap<Integer, Departament> hmDept = new HashMap();

    // Consultes parametritzades
    PreparedStatement psDept = null;    // Cercar departament per codi
    PreparedStatement psEmp = null;     // Cercar empleat per codi
    PreparedStatement psExistEmp = null;    // Comprovar existència d'empleat per codi
    PreparedStatement psQtySub = null;  // Comptar subordinats d'empleat per codi
    PreparedStatement psSubordinatsDirectes = null;     // Obtenir els subordinats directes d'un empleat
    PreparedStatement psDelEmp = null;  // Eliminar empleat per codi
    PreparedStatement psDelCap = null;  // Eliminar cap dels empleats que el tenen per cap
    PreparedStatement psUpdCap = null;  // Actualitzar cap d'empleats amb un determinat cap
    PreparedStatement psEmpCap = null;  // Comprovar si 

    /**
     * Constructor que estableix connexió amb el servidor a partir de les dades
     * informades en fitxer de propietats de nom EPJDBC.properties.
     *
     * @throws GestorEmpresaException si hi ha algun problema en el fitxer de
     * configuració
     */
    public EPJDBC() {
        this("EPJDBC.properties");
    }

    /**
     * Constructor que estableix connexió amb el servidor a partir de les dades
     * informades en fitxer de propietats, i en cas de ser null cercarà el
     * fitxer de nom EPBaseX.properties.
     *
     * @param nomFitxerPropietats
     * @throws GestorEmpresaException si hi ha algun problema en el fitxer de
     * configuració o si no s'estableix la connexió
     */
    public EPJDBC(String nomFitxerPropietats) {
        if (nomFitxerPropietats == null || nomFitxerPropietats.equals("")) {
            nomFitxerPropietats = "EPJDBC.properties";
        }
        Properties props = new Properties();
        try {
            props.load(new FileReader(nomFitxerPropietats));
        } catch (FileNotFoundException ex) {
            throw new GestorEmpresaException("No es troba fitxer de propietats", ex);
        } catch (IOException ex) {
            throw new GestorEmpresaException("Error en carregar fitxer de propietats", ex);
        }

        String url = props.getProperty("url");
        String user = props.getProperty("user");
        String password = props.getProperty("password");
        if (url == null || user == null || password == null) {
            throw new GestorEmpresaException("El fitxer de propietats no incorpora totes les propietats necessàries: url,user,password");
        }

        try {
            con = DriverManager.getConnection(url, user, password);
        } catch (SQLException ex) {
            throw new GestorEmpresaException("No es pot establir la connexió", ex);
        }

        try {
            try {
                con.setAutoCommit(false);
            } catch (SQLException ex) {
                throw new GestorEmpresaException("No s'ha pogut desactivar l'autocommit", ex);
            }
            // Creació dels diversos PreparedStatement
            try {
                String cadena = "select dnom, loc from dept "
                        + "where dept_no = ?";
                psDept = con.prepareStatement(cadena);
            } catch (SQLException ex) {
                throw new GestorEmpresaException("Error en preparar sentència psDept", ex);
            }
            try {
                String cadena = "select cognom, ofici, cap, data_alta, salari, comissio, dept_no "
                        + "from emp where emp_no = ?";
                psEmp = con.prepareStatement(cadena);
            } catch (SQLException ex) {
                throw new GestorEmpresaException("Error en preparar sentència psEmp", ex);
            }
            try {
                String cadena = "select 1 from emp where emp_no = ?";
                psExistEmp = con.prepareStatement(cadena);
            } catch (SQLException ex) {
                throw new GestorEmpresaException("Error en preparar sentència psExistEmp", ex);
            }
            try {
                String cadena = "select count(emp_no) from emp where cap = ?";
                psQtySub = con.prepareStatement(cadena);
            } catch (SQLException ex) {
                throw new GestorEmpresaException("Error en preparar sentència psQtySub", ex);
            }
            try {
                String cadena = "select emp_no from emp where cap = ?";
                psSubordinatsDirectes = con.prepareStatement(cadena);
            } catch (SQLException ex) {
                throw new GestorEmpresaException("Error en preparar sentència psSubordinatsDirectes", ex);
            }
            try {
                String cadena = "delete from emp where emp_no = ?";
                psDelEmp = con.prepareStatement(cadena);
            } catch (SQLException ex) {
                throw new GestorEmpresaException("Error en preparar sentència psDelEmp", ex);
            }
            try {
                String cadena = "update emp set cap = null where cap = ?";
                psDelCap = con.prepareStatement(cadena);
            } catch (SQLException ex) {
                throw new GestorEmpresaException("Error en preparar sentència psDelCap", ex);
            }
            try {
                String cadena = "update emp set cap = ? where cap = ?";
                psUpdCap = con.prepareStatement(cadena);
            } catch (SQLException ex) {
                throw new GestorEmpresaException("Error en preparar sentència psUpdCap", ex);
            }
            try {
                String cadena = "select 1 from emp where cap = ? and emp_no = ?";
                psEmpCap = con.prepareStatement(cadena);
            } catch (SQLException ex) {
                throw new GestorEmpresaException("Error en preparar sentència psUpdCap", ex);
            }
        } catch (Exception ex) {
            try {
                con.close();
            } catch (SQLException ex1) {
            }
        }
    }

    /**
     * Tanca la capa de persistència, tancant la connexió amb la BD.
     *
     * @throws GestorEmpresaException si hi ha algun problema en tancar la
     * connexió
     */
    @Override
    public void closeCapa() {
        if (con != null) {
            try {
                con.rollback();
            } catch (SQLException ex) {
            }
            try {
                con.close();
            } catch (SQLException ex) {
                throw new GestorEmpresaException("Error en tancar la connexió", ex);
            } finally {
                con = null;
            }
        }
    }

    /**
     * Tanca la transacció activa validant els canvis a la BD.
     *
     * @throws GestorEmpresaException si hi ha algun problema
     */
    @Override
    public void commit() {
        try {
            con.commit();
        } catch (SQLException ex) {
            throw new GestorEmpresaException("Error en fer commit", ex);
        }
    }

    /**
     * Tanca la transacció activa sense validar els canvis a la BD.
     *
     * @throws GestorEmpresaException si hi ha algun problema
     */
    @Override
    public void rollback() {
        try {
            con.rollback();
        } catch (SQLException ex) {
            throw new GestorEmpresaException("Error en fer rollback", ex);
        }
    }

    /**
     * Recupera l'objecte Empresa
     *
     * @return L'objecte Empresa
     * @throws GestorEmpresaException si hi ha algun problema en recuperar
     * l'empresa
     */
    @Override
    public Empresa getEmpresa() {
        if (empresa != null) {
            return empresa;
        }
        String cadena = "select nom,data_creacio from rao_social where codi=1";
        Statement st = null;
        try {
            st = con.createStatement();
            ResultSet rs = st.executeQuery(cadena);
            if (rs.next()) {
                Date aux = rs.getDate("data_creacio");
                Calendar c = new GregorianCalendar();
                c.setTime(aux);
                empresa = new Empresa(rs.getString("nom"), c);
                return empresa;
            } else {
                throw new GestorEmpresaException("No es troben dades de l'empresa");
            }
        } catch (SQLException ex) {
            throw new GestorEmpresaException("Problema en recuperar empresa", ex);
        } finally {
            try {
                if (st != null) {
                    st.close();
                }
            } catch (SQLException ex) {
            }
        }
    }

    /**
     * Intenta recuperar el departament amb codi indicat per paràmetre.
     *
     * @param codi del departament a recuperar
     * @return L'objecte Departament trobat o null si no existeix
     * @throws GestorEmpresaException si es produeix algun error (introduir un
     * codi inadequat genera també aquesta excepció)
     */
    @Override
    public Departament getDepartament(int codi) {
        if (codi <= 0) {
            throw new GestorEmpresaException("Intent de recuperar departament amb codi " + codi + " erroni");
        }
        Departament d = hmDept.get(codi);
        if (d != null) {
            return d;
        }
        // Si encara no teniem el departament en memòria:
        try {
            psDept.setInt(1, codi);
            ResultSet rs = psDept.executeQuery();
            if (!rs.next()) {
                return null;
            } else {
                d = new Departament(codi, rs.getString("dnom"),
                        rs.getString("loc"));
                hmDept.put(codi, d);
                return d;
            }
        } catch (SQLException ex) {
            throw new GestorEmpresaException("Error en recuperar departament", ex);
        }
    }

    /**
     * Intenta recuperar l'empleat amb codi indicat per paràmetre.
     *
     * @param codi de l'empleat a recuperar
     * @return L'objecte Empleat trobat o null si no existeix
     * @throws GestorEmpresaException si es produeix algun error (introduir un
     * codi inadequat genera també aquesta excepció)
     */
    @Override
    public Empleat getEmpleat(int codi) {
        if (codi <= 0) {
            throw new GestorEmpresaException("Intent de cercar un empleat amb codi " + codi + " inadequat");
        }
        Empleat e = hmEmp.get(codi);
        if (e != null) {
            return e;
        }
        // Si encara no teniem l'empleat en memòria
        try {
            psEmp.setInt(1, codi);
            ResultSet rs = psEmp.executeQuery();
            if (!rs.next()) {
                return null;
            } else {
                Date data_alta = rs.getDate("data_alta"); // ALERTA: Pot ser null!!!
                Calendar c = null;
                if (data_alta != null) {
                    c = new GregorianCalendar();
                    c.setTime(data_alta);
                }
                Double salari = rs.getDouble("salari");   // ALERTA: Pot ser null!!!
                if (rs.wasNull()) {
                    salari = null;
                }
                Double comissio = rs.getDouble("comissio");  // ALERTA: Pot ser null!!!
                if (rs.wasNull()) {
                    comissio = null;
                }
                Integer codiDept = rs.getInt("dept_no");    // ALERTA: Pot ser null!!!
                Departament d = null;
                if (!rs.wasNull()) {
                    d = getDepartament(codiDept);
                }
                String cognom = rs.getString("cognom");
                String ofici = rs.getString("ofici");
                Integer codiCap = rs.getInt("cap");      // ALERTA: Pot ser null!!!
                // ATENCIÓ! A continuació s'invoca el propi mètode, fet que implica que
                // es tornarà a usar el mateix PreparedStatement... Això provoca que el ResultSet
                // de l'anterior execució quedi tancat i... per tant, abans d'invocar de nou 
                // getEmpleat, cal guardar-se tots els valors del ResultSet de l'execució anterior
                Empleat cap = null;
                if (!rs.wasNull()) {
                    cap = getEmpleat(codiCap);
                }
                e = new Empleat(codi, cognom, ofici, c, salari, comissio, cap, d);
                hmEmp.put(codi, e);
                return e;
            }
        } catch (SQLException ex) {
            throw new GestorEmpresaException("Error en recuperar empleat", ex);
        }
    }

    /**
     * Informa de l'existència d'empleat amb el codi indicat per paràmetre.
     *
     * @return Valor booleà que indica l'existència de l'empleat indicat
     * @throws GestorEmpresaException si es produeix algun error (introduir un
     * codi inadequat genera també aquesta excepció)
     */
    @Override
    public boolean existeixEmpleat(int codi) {
        if (codi <= 0) {
            throw new GestorEmpresaException("Intent de cercar un empleat amb codi " + codi + " inadequat");
        }
        if (hmEmp.containsKey(codi)) {
            return true;
        }
        try {
            psExistEmp.setInt(1, codi);
            ResultSet rs = psExistEmp.executeQuery();
            return rs.next();
        } catch (SQLException ex) {
            throw new GestorEmpresaException("Error en comprovar existència d'empleat", ex);
        }
    }

    /**
     * Facilita el nombre de subordinats d'un empleat
     *
     * @param codi de l'empleat al què comptabilitzar els subordinats
     * @return El nombre de subordinats de l'empleat
     * @throws GestorEmpresaException si es produeix algun error (introduir un
     * codi inadequat o la inexistència d'empleat amb el codi indicat genera
     * també aquesta excepció)
     */
    @Override
    public int comptarSubordinats(int codi) {
        if (codi <= 0) {
            throw new GestorEmpresaException("Intent de comptar subordinats d'empleat amb codi " + codi + " inadequat");
        }
        if (!existeixEmpleat(codi)) {
            throw new GestorEmpresaException("Intent de comptar empleats d'empleat amb codi " + codi + " inexistent");
        }
        // Arribats aquí, tenim la certesa de que existeix empleat amb codi indicat
        try {
            psQtySub.setInt(1, codi);
            ResultSet rs = psQtySub.executeQuery();
            rs.next();
            return rs.getInt(1);
        } catch (SQLException ex) {
            throw new GestorEmpresaException("Error en comptar subordinats d'empleat " + codi, ex);
        }
    }

    /**
     * Comprova si un empleat és subordinat directe o indirecte (varis nivells)
     * del cap
     *
     * @param cap
     * @param emp
     * @return true si emp és subordinat (directe o indirecte) de cap i false en
     * cas contrari
     */
    @Override
    public boolean esSubordinatDirecteIndirecte(int cap, int emp) {
        if (!this.existeixEmpleat(cap)) {
            throw new GestorEmpresaException("No existeix empleat amb codi " + cap);
        }
        if (!this.existeixEmpleat(emp)) {
            throw new GestorEmpresaException("No existeix empleat amb codi " + emp);
        }
        try {
            psSubordinatsDirectes.setInt(1, cap);
            ResultSet rs = psSubordinatsDirectes.executeQuery();
            List<Integer> subordinats = new ArrayList();
            while (rs.next()) {
                subordinats.add(rs.getInt("emp_no"));
            }
            if (subordinats.isEmpty()) {
                return false;
            }
            for (Integer codi : subordinats) {
                if (codi == emp) {
                    return true;
                } else {
                    boolean aux = esSubordinatDirecteIndirecte(codi, emp);
                    if (aux) {
                        return aux;
                    }
                }
            }
            return false;
        } catch (NumberFormatException | SQLException ex) {
            throw new GestorEmpresaException("Error en cercar els subordinats d'empleat de codi " + cap, ex);
        }
    }

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
    @Override
    public void eliminarEmpleat(int codi, int actCap) {
        if (codi <= 0) {
            throw new GestorEmpresaException("Intent d'eliminar empleat amb codi " + codi + " inadequat");
        }
        if (!this.existeixEmpleat(codi)) {
            throw new GestorEmpresaException("Intent d'eliminar empleat amb codi " + codi + " inexistent");
        }
        // Sabem que l'empleat existeix...
        if (actCap == 0) {
            /* Deixar els subordinats sense cap i eliminar l'empleat */
            // Instrucció per eliminar dels subordinats, el contingut de l'atribut "cap": 
            Savepoint sp = null;
            try {
                sp = con.setSavepoint("XYZ");
                psDelCap.setInt(1, codi);
                psDelCap.executeUpdate();
                psDelEmp.setInt(1, codi);
                psDelEmp.executeUpdate();
            } catch (SQLException ex) {
                try {
                    con.rollback(sp);
                } catch (SQLException ex1) {
                }
                throw new GestorEmpresaException("Error en eliminar empleat amb codi " + codi, ex);
            }
        } else {
            if ((actCap < 0 || actCap>Short.MAX_VALUE) &&  comptarSubordinats(codi)>0){
                throw new GestorEmpresaException("Intent d'eliminar empleat amb codi " + codi + " que té subordinats sense cap acció sobre ells");
            }
            /* Hem d'intentar substituir el cap dels subordinats (pot ser que no n'hi hagi) */
            if (!this.existeixEmpleat(actCap)) {
                throw new GestorEmpresaException("Intent d'eliminar empleat amb codi " + codi
                        + " reassignant els seus subordinats a empleat de codi " + actCap + " inexistent");
            }
            if (this.esSubordinatDirecteIndirecte(codi, actCap)) {
                throw new GestorEmpresaException("Intent d'eliminar empleat amb codi " + codi
                        + " reassignant els seus subordinats a empleat de codi " + actCap
                        + " que és un subordinat (directe o indirecte) del cap");
            }
            /* Cal canviar el cap als subordinats i eliminar l'empleat */
            Savepoint sp = null;
            try {
                sp = con.setSavepoint("XYZ");
                psUpdCap.setInt(1, actCap);       // Paràmetre 1: Nou cap
                psUpdCap.setInt(2, codi);         // Paràmetre 2: Cap antic
                psUpdCap.executeUpdate();
                psDelEmp.setInt(1, codi);
                psDelEmp.executeUpdate();
            } catch (SQLException ex) {
                try {
                    con.rollback(sp);
                } catch (SQLException ex1) {
                }
                throw new GestorEmpresaException("Error en eliminar empleat amb codi " + codi, ex);
            }
        }
        // Actualitzem informació en les HashMap
        hmEmp.remove(codi);
        // Aplicar els canvis en els empleats afectats que hi pugui haver a hmEmp no és senzill,
        // doncs pot provocar que en alguns casos haguem d'assignar cap que no tinguem en memòria
        Collection empleats = hmEmp.values();
        Iterator<Empleat> iteEmp = empleats.iterator();
        if (actCap == 0) {
            // Cal eliminar el cap als possibles subordinats existents en memòria
            iteEmp = empleats.iterator();
            // ALERTA! Dins "else" següent, explicat per què NO es pot referenciar iteEmp després de crear "empleats"
            while (iteEmp.hasNext()) {
                Empleat e = iteEmp.next();
                if (e.getCap() != null && e.getCap().getCodi() == codi) {
                    e.setCap(null);
                }
            }
        } else {
            // Cal actualitzar el cap als possibles subordinats existents en memòria
            // Primer cal tenir la referència que apunta al nou cap
            Empleat nouCap = getEmpleat(actCap);
            // En cas que actCap no hagués estat en memòria, el mètode getEmpleat el cerca i l'afegeix a iteEmp.
            // I Java NO permet recórrer un iterator d'un HashMap que hagi estat creat abans d'una modificació del HashMap
            // Es genera una ConcurrentModificationException!!!
            iteEmp = empleats.iterator();
            while (iteEmp.hasNext()) {
                Empleat e = iteEmp.next();
                if (e.getCap() != null && e.getCap().getCodi() == codi) {
                    e.setCap(nouCap);
                }
            }
        }
    }
}
