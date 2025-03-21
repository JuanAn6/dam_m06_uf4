SednaConnection session;
File xml;
String newName;
String collection;
/* Suposem que les variables anteriors estan informades */
try {
	InputStream is;
	try {
		is = new FileInputStream(xml);
	} catch (IOException e) {
		throw new RuntimeException("Error en intentar accedir al fitxer!");
	}
	if (!transOn) {
		session.begin();
		transOn=true;
	}
	SednaStatement st = session.createStatement();
	if (collection == null || "".equals(collection)) {
		st.loadDocument(is, newName);
	} else {
		try {
			st.execute("CREATE COLLECTION '" + collection + "'");
		} catch (Exception drex) {
			/* Apareix excepció SE2002 si la col·lecció ja existeix */
			if (drex.getMessage().contains("SE2002")) {
				// No fem res... La col·l3cció ja existia...
			} else {
				throw drex;  // Per si fós alguna altra excepció
			}
			session.begin();
			/* L'excepció anterior tanca la transacció i
			 cal tornar-la a iniciar */
		}
		st.loadDocument(is, newName, collection);
	}
} catch (DriverException e) {
    transOn=false;
    throw new UtilsBDXmlException("Error en executar la càrrega de document", e);
} catch (Exception e) {
    throw new UtilsBDXmlException("Error en executar la càrrega de document", e);
}
