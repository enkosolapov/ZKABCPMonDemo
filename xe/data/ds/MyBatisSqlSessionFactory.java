package basos.xe.data.ds;

import javax.sql.DataSource;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import basos.xe.data.dao.DealLastStateMapper;

/** SqlSessionFactory на основе Java-конфигурации (Singleton).
 * Регистрируются все мапперы (java+xml) из пакета basos.xe.data.dao.
 */
public class MyBatisSqlSessionFactory {
	
	private static final Logger logger = LoggerFactory.getLogger(MyBatisSqlSessionFactory.class);

	private static final SqlSessionFactory sqlSessionFactory;
	static {
		try {
			DataSource ds = OrclDataSource.getDataSource();
			Environment env = new Environment("zk", new JdbcTransactionFactory(), ds);
			Configuration cnf = new Configuration(env);
			//cnf.getTypeAliasRegistry().registerAlias("deals", DealLastState.class);
			//cnf.addMapper(DealLastStateMapper.class);
// !! загружать мапперы по мере использования, а не все оптом !!
//			cnf.addMappers("basos.xe.data.dao"); // HOWTO: ? как xml хранить отдельно от interface (в XML-конфиге у элемента mapper есть атрибут resource: см. использование XMLMapperBuilder в XMLConfigBuilder.mapperElement) ?
			sqlSessionFactory = new SqlSessionFactoryBuilder().build(cnf);
		} catch (Throwable e) {
			logger.error("Error in MyBatisSqlSessionFactory.getSqlSessionFactory()", e);
			throw new InternalError("Error in MyBatisSqlSessionFactory.getSqlSessionFactory()", e);
		}
	}

	private MyBatisSqlSessionFactory() {}
	
	public static SqlSessionFactory getSqlSessionFactory() {
		return sqlSessionFactory;
	} // public static SqlSessionFactory getSqlSessionFactory()
	
	public static SqlSession openSession() {
		return getSqlSessionFactory().openSession(); // HOWTO: parameters !!: ExecutorType.BATCH, autoCommit, IsolationLevel,...
	}

} // public class MyBatisSqlSessionFactory