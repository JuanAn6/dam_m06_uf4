/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.milaifontanals.persistence;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Query;
import org.milaifontanals.empresa.Departament;
import org.milaifontanals.empresa.Empleat;
import org.milaifontanals.empresa.Empresa;

/**
 * Capa de persistència per la gestió de l'empresa amb els seus departaments i
 * empleats, segons el model definit en el projecte Empresa.
 *
 * L'aplicació que usi la capa de persistència ha de disposar d'un fitxer de
 * propietats (veure documentació dels constructors) accessible via load(Reader)
 * amb les propietats següents, necesàries per la unitat de persistència:<BR>
 * jakarta.persistence.jdbc.url<BR>
 * jakarta.persistence.jdbc.user<BR>
 * jakarta.persistence.jdbc.password<BR>
 * jakarta.persistence.jdbc.driver<BR>
* <BR>
 * A més, haurà de tenir META-INF/persistence.xml amb el contingut adequat
 *
 * @author Isidre Guixà
 */
public class EPJPA implements IGestorEmpresa {

    private EntityManager em;

    private Empresa empresa;

    /**
     * Constructor que estableix connexió amb el servidor a partir de les dades
     * informades en fitxer de propietats de nom EPJPA.properties.
     *
     * @throws GestorEmpresaException si hi ha algun problema en el fitxer de
     * configuració
     */
    public EPJPA() {
        this("EPJPA.properties");
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
    public EPJPA(String nomFitxerPropietats) {
        if (nomFitxerPropietats == null || nomFitxerPropietats.equals("")) {
            nomFitxerPropietats = "EPJPA.properties";
        }
        Properties props = new Properties();
        try {
            props.load(new FileReader(nomFitxerPropietats));
        } catch (FileNotFoundException ex) {
            throw new GestorEmpresaException("No es troba fitxer de propietats", ex);
        } catch (IOException ex) {
            throw new GestorEmpresaException("Error en carregar fitxer de propietats", ex);
        }
//        String up = props.getProperty("up");
//        if (up == null) {
//            throw new GestorEmpresaException("Fitxer de propietats no conté propietat obligatòria <up>");
//        }
        
        EntityManagerFactory emf = null;
        try {
            emf = Persistence.createEntityManagerFactory("JPA",props);
            em = emf.createEntityManager();
        } catch (Exception ex) {
            if (emf != null) {
                emf.close();
            }
            throw new GestorEmpresaException("Error en crear EntityManagerFactory o EntityManager", ex);
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
        if (em != null) {
            EntityManagerFactory emf = null;
            try {
                emf = em.getEntityManagerFactory();
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                em.close();
            } catch (Exception ex) {
                throw new GestorEmpresaException("Error en tancar la connexió", ex);
            } finally {
                em = null;
                if (emf != null) {
                    emf.close();
                }
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
            if (!em.getTransaction().isActive()) {
                em.getTransaction().begin();
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            throw new GestorEmpresaException("Error en fer commit.", ex);
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
            if (!em.getTransaction().isActive()) {
                em.getTransaction().begin();
            }
            em.getTransaction().rollback();
        } catch (Exception ex) {
            throw new GestorEmpresaException("Error en fer rollback.", ex);
        }
    }
    
    public boolean isClose(){
        return em == null;
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
        Query q = null;
        try {
            q = em.createNativeQuery(cadena);
            try {
                Object t[] = (Object[]) q.getSingleResult();
                Timestamp aux = (Timestamp) t[1];
                Calendar c = new GregorianCalendar();
                c.setTime(aux);
                empresa = new Empresa((String) t[0], c);
                return empresa;
            } catch (NoResultException ex) {
                throw new GestorEmpresaException("No es troben dades de l'empresa");
            }
        } catch (PersistenceException ex) {
            throw new GestorEmpresaException("Problema en recuperar empresa", ex);
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
        if (codi <= 0 || codi > Byte.MAX_VALUE) {
            throw new GestorEmpresaException("Intent de recuperar departament amb codi " + codi + " erroni");
        }
        return em.find(Departament.class, (byte) codi);
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
        if (codi <= 0 || codi > Short.MAX_VALUE) {
            throw new GestorEmpresaException("Intent de cercar un empleat amb codi " + codi + " inadequat");
        }
        return em.find(Empleat.class, (short) codi);
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
        if (codi <= 0 || codi > Short.MAX_VALUE) {
            throw new GestorEmpresaException("Intent de cercar un empleat amb codi " + codi + " inadequat");
        }
        return em.find(Empleat.class, (short) codi) != null;
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
        if (codi <= 0 || codi > Short.MAX_VALUE) {
            throw new GestorEmpresaException("Intent de comptar subordinats d'empleat amb codi " + codi + " inadequat");
        }
        if (!existeixEmpleat(codi)) {
            throw new GestorEmpresaException("Intent de comptar empleats d'empleat amb codi " + codi + " inexistent");
        }
        // Arribats aquí, tenim la certesa de que existeix empleat amb codi indicat
        Query q = em.createNamedQuery("ComptarSubordinats");
        q.setParameter("codi", (short) codi);
        return ((Long) q.getSingleResult()).intValue();
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
        Query q = em.createNamedQuery("ObtenirCodisSubordinatsEmpleatViaCodi");
        q.setParameter("codi", cap);
        List<Short> subordinats = q.getResultList();
        if (subordinats.isEmpty()) {
            return false;
        }
        for (Short codi : subordinats) {
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
        if (codi <= 0 || codi > Short.MAX_VALUE) {
            throw new GestorEmpresaException("Intent d'eliminar empleat amb codi " + codi + " inadequat");
        }
        Empleat emp = em.find(Empleat.class, (short) codi);
        if (emp == null) {
            throw new GestorEmpresaException("Intent d'eliminar empleat amb codi " + codi + " inexistent");
        }
        // Sabem que l'empleat existeix...
        // Abans de procedir a eliminar-lo, hem de fer comprovacions respecte els empleats...
        int qEmp = this.comptarSubordinats(codi);
        if (qEmp == 0) {
            /* Eliminar-lo i no fer res mes */
            em.remove(emp);
        } else {
            /* Té subordinats. Cal actuar segons el paràmetre actCap */
            if (actCap < 0) {
                throw new GestorEmpresaException("Intent d'eliminar empleat amb codi " + codi + " que té subordinats sense cap acció sobre ells");
            } // Arribats aquí vol dir que té subordinats i hem d'actuar sobre ells
            // No podem fer instruccions JPQL update sobre els subordinats per què
            // els possibles empleats afectats JA gestionats per l'EntityManager no es 
            // quedaríen actualitzats en memòria. 
            // Ens convé obtenir els subordinats de l'empleat a eliminar
            if (actCap == 0) {
                Query q = em.createNamedQuery("ObtenirSubordinatsEmpleatViaObjecte");
                q.setParameter("emp", emp);
                List<Empleat> ll = q.getResultList();
                for (Empleat e : ll) {
                    e.setCap(null);
                }
                em.remove(emp);
            } else {
                /* Hem d'intentar substituir el cap dels subordinats */
                if (codi == actCap) {
                    throw new GestorEmpresaException("Intent d'eliminar empleat amb codi " + codi
                            + " reassignant els seus subordinats a ell mateix ");
                }
                if (actCap > Short.MAX_VALUE) {
                    throw new GestorEmpresaException("Intent d'assignar cap amb codi " + actCap + " inadequat");
                }
                Empleat empActCap = em.find(Empleat.class, (short) actCap);
                if (empActCap == null) {
                    throw new GestorEmpresaException("Intent d'eliminar empleat amb codi " + codi
                            + " reassignant els seus subordinats a empleat de codi " + actCap + " inexistent");
                }
                if (this.esSubordinatDirecteIndirecte(codi, actCap)) {
                    throw new GestorEmpresaException("Intent d'eliminar empleat amb codi " + codi
                            + " reassignant els seus subordinats a empleat de codi " + actCap
                            + " que és un subordinat (directe o indirecte) del cap");
                }
                /* Cal canviar el cap als subordinats i eliminar l'empleat */
                Query q = em.createNamedQuery("ObtenirSubordinatsEmpleatViaCodi");
                q.setParameter("codi", (short) codi);
                List<Empleat> ll = q.getResultList();
                for (Empleat e : ll) {
                    e.setCap(empActCap);
                }
                em.remove(emp);
            }
        }
    }
}
