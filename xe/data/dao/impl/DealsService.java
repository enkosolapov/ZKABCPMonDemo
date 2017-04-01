package basos.xe.data.dao.impl;

import java.util.List;

import org.apache.ibatis.session.SqlSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import basos.xe.data.dao.DealLastStateMapper;
import basos.xe.data.ds.MyBatisSqlSessionFactory;
import basos.xe.data.entity.DealLastState;

/** Вызов реализации DealLastStateMapper. <b>Не используется.</b>
 * Сервисные методы делегируются реализации "Mapper interface", получаемую через MyBatis SqlSession (см. MapperProxyFactory).
 * НО, доменные объекты в чистом виде не используем, только завёрнутые в GridData.
 * А там требуется обработка каждой строки, применяется SqlSession.select (не кэширует строки !!) с параметром ResultHandler.
 * Session per request.
 */
public class DealsService {
	
	private static final Logger logger = LoggerFactory.getLogger(DealsService.class);

	public DealsService() {
		logger.trace("DealsService instantiated");
	}
	
	public List<DealLastState> selectAllDeals() {
		SqlSession sess = MyBatisSqlSessionFactory.openSession();
		try {
			DealLastStateMapper dsm = sess.getMapper(DealLastStateMapper.class);
			return dsm.selectAll();
		} finally {
			sess.close();
		}
	} // public List<DealLastState> selectAllDeals()
	
	public List<DealLastState> selectDealsByClnId(int clnId) {
		SqlSession sess = MyBatisSqlSessionFactory.openSession();
		try {
			DealLastStateMapper dsm = sess.getMapper(DealLastStateMapper.class);
			return dsm.selectByClnId(clnId);
		} finally {
			sess.close();
		}
	} // public List<DealLastState> selectDealsByClnId()

} // public class DealsService