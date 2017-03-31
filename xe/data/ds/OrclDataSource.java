package basos.xe.data.ds;

//import javax.annotation.Resource;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** ���������� � �� Oracle ��� ��������� "java:jboss/datasources/orcl", ����������� ����� JNDI. Singleton. */
public final class OrclDataSource {
	private static final Logger logger = LoggerFactory.getLogger(OrclDataSource.class);
// FIXME: ������ JNDIName ��� ������ �������� ���������� (���������������� ��� �� pmDesktop.getWebApp().getServletContext().getServerInfo()) ?
	private static final String orclDataSourceJNDIName = "java:jboss/datasources/orcl";
// https://docs.oracle.com/javase/tutorial/jdbc/basics/sqldatasources.html
// HOWTO: ��� ���������� ��� ���������� ?
	private static /*final*/ Context ctx;
	//@Resource(name=orclDataSourceJNDIName) // Resource injection enables you to inject any resource available in the JNDI namespace into any container-managed object, such as a servlet, an enterprise bean, or a managed bean.
	private static /*final*/ DataSource ds;
	static {
		try {
			ctx = new InitialContext();
			ds = (DataSource)ctx.lookup(orclDataSourceJNDIName); // FIXME: �� �������� ��� Jetty !
		} catch (NamingException e) {
			logger.error("NamingException on lookup DataSource '{}'", orclDataSourceJNDIName, e);
			throw new InternalError("NamingException on lookup DataSource '"+orclDataSourceJNDIName+"'", e);
		}		
	} // static
	
	private OrclDataSource () {}
	
	/** ���������� (������������) ��������� "java:jboss/datasources/orcl". */
	public static DataSource getDataSource() {
		return ds;
	}
	
} // public final class OrclDataSource