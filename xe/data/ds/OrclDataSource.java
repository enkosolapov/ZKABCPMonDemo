package basos.xe.data.ds;

//import javax.annotation.Resource;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Соединение с БД Oracle под названием "java:jboss/datasources/orcl", разрешается через JNDI. Singleton. */
public final class OrclDataSource {
	private static final Logger logger = LoggerFactory.getLogger(OrclDataSource.class);
// FIXME: разные JNDIName для разных серверов приложения (идентифицировать его по pmDesktop.getWebApp().getServletContext().getServerInfo()) ?
	private static final String orclDataSourceJNDIName = "java:jboss/datasources/orcl";
// https://docs.oracle.com/javase/tutorial/jdbc/basics/sqldatasources.html
// HOWTO: как мониторить пул соединений ?
	private static /*final*/ Context ctx;
	//@Resource(name=orclDataSourceJNDIName) // Resource injection enables you to inject any resource available in the JNDI namespace into any container-managed object, such as a servlet, an enterprise bean, or a managed bean.
	private static /*final*/ DataSource ds;
	static {
		try {
			ctx = new InitialContext();
			ds = (DataSource)ctx.lookup(orclDataSourceJNDIName); // FIXME: не работает под Jetty !
		} catch (NamingException e) {
			logger.error("NamingException on lookup DataSource '{}'", orclDataSourceJNDIName, e);
			throw new InternalError("NamingException on lookup DataSource '"+orclDataSourceJNDIName+"'", e);
		}		
	} // static
	
	private OrclDataSource () {}
	
	/** Возвращает (единственный) экземпляр "java:jboss/datasources/orcl". */
	public static DataSource getDataSource() {
		return ds;
	}
	
} // public final class OrclDataSource