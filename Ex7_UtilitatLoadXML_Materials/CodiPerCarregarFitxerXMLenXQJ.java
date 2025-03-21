XQConnection2 conn;
File xml;
String newName;
String collection;
/* Suposem que les variables anteriors estan informades */
try {
	StreamSource ss;
	try {
		ss = new StreamSource(new FileInputStream(xml));
	} catch (IOException e) {
		throw new RuntimeException("Error en intentar accedir al fitxer!", e);
	}
	transOn=true;   // Després només es tindrà en compte pels SGBD transaccionals

	XQItem doc = conn.createItemFromDocument(ss, null);
	// En Sedna, el mètode XQDataFactory.createItemFromDocument(java.io.Reader,String,XQItemType)
	// falla en carregar arxiu mondial.xml. S'ha contactat amb
	// Charles Foster (09/05/2018) que ha recomanat utilitzar, mentre no aparegui nou driver XQJ Sedna
	// XQDataFactory.createItemFromDocument(javax.xml.transform.Source, XQItemType)

	if (collection == null || "".equals(collection)) {
		conn.insertItem(newName, doc, null);
	} else // Els SGBD BaseX i eXist-db incorporen la col·lecció a la URI del document
	// En aquests SGBD, la utilització del mètode insertItem incorporant la col·lecció en
	// el primer argument (URI del document) funciona perfectament. Si la col·lecció existeix,
	// hi fica el document i si no existeix, crea la col·lecció i hi fica el document.
	// Però en Sedna, el nom de la col·lecció és independent i no es pot incorporar a la URI...
	// La proposta XQJ2 de C.Foster incorpora una interfície XQInsertOptions amb dos mètodes:
	// getEncoding i SetEncoding... La implementació XQJ de Sedna incorpora la classe 
	// SednaXQInsertOptions que implementa aquesta interfície i, que a més a més dels mètodes
	// obligats per la interfície, té els mètodes setCollection i getCollection per poder indicar
	// quina és la col·lecció "activa". Però aquests mètodes no estan dins la interfície i, per tant,
	// per poder-ho incorporar en aquest component que ha de servir per a tots els SGBD amb XQJ, ens
	// cal fer un muntatge que... Déu n'hi doret.
	{
		if (conn.getMetaData().getProductName().contains("Sedna")) {
			// Estem connectats a un SGBD Sedna                    
			try {
				XQExpression xqe = conn.createExpression();
				xqe.executeCommand("CREATE COLLECTION '" + collection + "'");
			} catch (XQException xqe) {
				/* Apareix excepció XQJSE002 si la col·leció ja existeix */
				if (xqe.getMessage().contains("XQJSE002")) {
					// No fem res... La col·lecció ja existia...
				} else {
					throw xqe;  // Per si és alguna altra excepció
				}
			}
			// Arribats aquí, sabem que la collection existeix.
			// Hem de dir que la volem utilitzar
			XQInsertOptions xqi = (XQInsertOptions) Class.forName("net.xqj.sedna.SednaXQInsertOptions").newInstance();
			// Ens cal utilitzar "reflection" per poder executar el mètode setCollection() que sabem que existirà...
			Method m = Class.forName("net.xqj.sedna.SednaXQInsertOptions").getMethod("setCollection", String.class);
			m.invoke(xqi, collection);
			// Hem indicat a Sedna la collection amb la que treballarem
			// Ara ja procedim amb l'insertItem, sense introduir el nom de la col·lecció
			conn.insertItem(newName, doc, xqi);
		} else {
			// Manera senzilla de treballar amb BaseX i eXist-db
			conn.insertItem(collection + "/" + newName, doc, null);
		}
	}
} catch (Exception e) {
	throw new RuntimeException("Error: " + e.getMessage(), e);
}


