>>> Suposant que tenim
Connection con;
String script;

>>> Codi:
Scanner sc = null;
try {
    sc = new Scanner(script);
} catch (FileNotFoundException ex) {
    throw new RuntimeException("Problemes en llegir guió ", ex);
}
sc.useDelimiter(";");
String inst = null;
Statement st = null;
try {
    st = con.createStatement();
    while (sc.hasNext()) {
        inst = sc.next();
        // La següent expressió regular elimina els comentaris unilinia
        // tant si n'hi ha un com varis, però amb -- al principi de línia
        // NO sap tractar comentaris que comencin a mitja línia
        // En cas de voler-los tractar, cal tenir en compte que hi pot haver
        // símbols -- enmig de cometes simples, que no serien comentaris i 
        // no s'hauria de tocar
        // També queden pendents de tractar els comentaris multilínia /* */
        // La gestió de comentaris no és simple... L'scanner ens està donant 
        // les instruccions trencades per ";" i... la gestió és incorrecta 
        // si enmig d'un comentari unilinia o multilínia hi ha un ";"...
        inst = inst.replaceAll("(^--|[\n]+--)(.)*(\\n|\\r|$)", "\n");
        inst = inst.trim();
        if (inst.length() == 0) {
            continue;       // La saltem
        }
        if (inst.toLowerCase().startsWith("select ")) {
            continue;       // La saltem
        }
        // Executarem inst; 
        System.out.println(inst);
        try {
            st.executeUpdate(inst);
        } catch (SQLException ex) {
            if (!inst.toLowerCase().startsWith("drop ")) {
                throw ex;
            }
        }
    }
} catch (SQLException ex) {
    boolean rollback = true;
    try {
        con.rollback();
    } catch (SQLException ex1) {
        rollback = false;
    }
    if (rollback == false) {
        throw new RuntimeException("Problemes en executar guió i no s'ha pogut efectuar rollback", ex);
    } else {
        throw new RuntimeException("Problemes en executar guió ", ex);
    }
} finally {
    if (st != null) {
        try {
            st.close();
        } catch (SQLException ex) {
            throw new RuntimeException("Error en tancar sentència utilitzada per executar el guió.", ex);
        }
    }
    sc.close();
}
