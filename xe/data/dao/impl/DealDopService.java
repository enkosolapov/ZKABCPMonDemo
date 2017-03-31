package basos.xe.data.dao.impl;

import java.util.List;

import org.apache.ibatis.session.SqlSession;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

import basos.xe.data.dao.DealDopMapper;
import basos.xe.data.ds.MyBatisSqlSessionFactory;
import basos.xe.data.entity.DealRestHistory;

/** Специфичные для сделок дата-сервисы. */
public class DealDopService {
//	private static final Logger logger = LoggerFactory.getLogger(DealDopService.class);
	
	/** История остатков по ИД LM субъекта. */
	public static List<DealRestHistory> getRestHistoryByIdDeal(int idDeal) {
		SqlSession sess = MyBatisSqlSessionFactory.openSession();
		try {
			if (!sess.getConfiguration().hasMapper(DealDopMapper.class)) {
				sess.getConfiguration().addMapper(DealDopMapper.class);
			}
			return sess.getMapper(DealDopMapper.class).selectRestHistoryByIdDeal(idDeal);
		} finally {
			sess.close();
		}
	} // public static List<DealRestHistory> getRestHistoryByIdDeal(int idDeal)

	
} // public class DealDopService