ClientSession con;
File xml;
String newName;
String collection;
String nameDB;
/* Suposem que les variables anteriors estan informades */
try {
    con.execute("open " + nameDB);
} catch (IOException ioe) {
    throw new RuntimeException("No es pot obrir la BD " + nameDB, ioe);
}
try {
    InputStream is;
    try {
        is = new FileInputStream(xml);
    } catch (IOException e) {
        throw new RuntimeException("Error en intentar accedir al fitxer " + xml.getAbsolutePath(), e);
    }
    if (collection == null || "".equals(collection)) {
        con.add(newName, is);
    } else {
        con.add(collection + "/" + newName, is);
    }
} catch (IOException ioe) {
    throw new RuntimeException("Error en executar la càrrega de fitxer", ioe);
} finally {
    try {
        con.execute("close");   // Tanca la BD oberta
    } catch (IOException ioe) {
        throw new RuntimeException("No es pot tancar la BD", ioe);
    }
}

ALERTA!!!
Per poder invocar el mètode "add", cal tenir la BD oberta!
A diferència de tot el què es va fer en la capa EPBaseX on les instruccions 
XPath i XQuery podien indicàven el nom de la BD dins la variable path,
en aquest cas cal conèixer el nom de la BD per obrir-la abans d'invocar "add" i,
per tant, caldrà tenir dins el fitxer de configuració de la capa el nom de la BD
(mentre que en aquesta capa no es necesita la variable "path")
Sembla lògic que una vegada s'hagi efectuat la càrrega, es tanqui la BD