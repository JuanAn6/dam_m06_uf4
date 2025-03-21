>>> Suposant que tenim
EntityManager em;
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
EntityTransaction et = em.getTransaction();
if (!et.isActive()) {
    et.begin();
}
try {
    while (sc.hasNext()) {
        inst = sc.next();
        // La següent expressió regular elimina els comentaris unilinia
        // tant si n'hi ha un com varis, però amb -- al principi de línia
        // NO sap tractat comentaris que comencin a mitja línia
        // En cas de voler-los tractar, cal tenir en compte que hi pot haver
        // símbols -- enmig de cometes simples, que no serien comentaris i 
        // no s'hauria de tocar
        // També queden pendents de tractar els comentaris multilínia /* */
        // La gestió de comentaris no és simple... L'scanner ens està donant 
        // les instruccions trencades per ";" i... la gestió és incorrecte 
        // si enmig d'un comentari unilinia o multilínia hi ha un ";"...
        inst = inst.replaceAll("(^--|[\n]+--)(.)*(\\n|\\r|$)", "\n");
        inst = inst.trim();
        if (inst.length() == 0) {
            continue;       // La saltem
        }
        if (inst.toLowerCase().startsWith("select ")) {
            continue;       // La saltem
        }
        System.out.println(inst);
        Query q = em.createNativeQuery(inst);
        try {
            q.executeUpdate();
        } catch (Exception ex) {
            if (!inst.toLowerCase().startsWith("drop ")) {
                throw ex;
            }
        }
    }
} catch (Exception ex) {
    throw new RuntimeException("Problemes en executar guió ", ex);
} finally {
    sc.close();
}
