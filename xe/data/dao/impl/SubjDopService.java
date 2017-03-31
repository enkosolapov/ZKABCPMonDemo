package basos.xe.data.dao.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import basos.xe.data.ds.MyBatisSqlSessionFactory;
import basos.xe.data.dao.SubjDopMapper;
import basos.xe.data.entity.DaDataInfo;
import basos.xe.data.entity.LimitInfo;
import basos.xe.data.entity.SubjRestHistory;


/** Специфичные для субъектов разнородные дата-сервисы. */
public class SubjDopService {
	private static final Logger logger = LoggerFactory.getLogger(SubjDopService.class);
	
	
	/** Состав группы лимита по ИД LM субъекта (одного из участников). */
	public static List<String> getGroupMembersBySubjId(int subjId) {
		SqlSession sess = MyBatisSqlSessionFactory.openSession();
		try {
			if (!sess.getConfiguration().hasMapper(SubjDopMapper.class)) {
				sess.getConfiguration().addMapper(SubjDopMapper.class);
			}
			return sess.getMapper(SubjDopMapper.class).selectGroupMembersBySubjId(subjId);
		} finally {
			sess.close();
		}
	} // public static List<String> getGroupMembersBySubjId(int subjId)

	
	/** История остатков по ИД LM субъекта. */
	public static List<SubjRestHistory> getRestHistoryBySubjId(int subjId) {
		SqlSession sess = MyBatisSqlSessionFactory.openSession();
		try {
			if (!sess.getConfiguration().hasMapper(SubjDopMapper.class)) {
				sess.getConfiguration().addMapper(SubjDopMapper.class);
			}
			return sess.getMapper(SubjDopMapper.class).selectRestHistoryBySubjId(subjId);
		} finally {
			sess.close();
		}
	} // public static List<SubjRestHistory> getRestHistoryBySubjId(int subjId)
	
	
	/** Инфо по лимиту кредитования включая историю использования (выборки) по ИД субъекта.
	 * @return nullable
	 */
	public static LimitInfo selectLimitInfoBySubjId(int subjId) {
		SqlSession sess = MyBatisSqlSessionFactory.openSession();
		try {
			if (!sess.getConfiguration().hasMapper(SubjDopMapper.class)) {
				sess.getConfiguration().addMapper(SubjDopMapper.class);
			}
			SubjDopMapper mp = sess.getMapper(SubjDopMapper.class);
// FIXME: оптимизировать поиск ИД лимита, сделать одним вызовом
			Integer idLimit = mp.selectIdLimitBySubjId(subjId);
			logger.trace("selectLimitInfoBySubjId.  subjId = {}, idLimit = {}", subjId, idLimit);
			if (idLimit == null) return null;
			return mp.selectLimitInfoByIdLimit(idLimit.intValue());
		} finally {
			sess.close();
		}
	} // public static LimitInfo selectLimitInfoBySubjId(int subjId)
	
	/** Инфо по лимиту кредитования включая историю использования (выборки) по ИД лимита.
	 * @return nullable
	 */
	public static LimitInfo selectLimitInfoByIdLimit(int idLimit) {
		SqlSession sess = MyBatisSqlSessionFactory.openSession();
		try {
			if (!sess.getConfiguration().hasMapper(SubjDopMapper.class)) {
				sess.getConfiguration().addMapper(SubjDopMapper.class);
			}
			SubjDopMapper mp = sess.getMapper(SubjDopMapper.class);
			logger.trace("selectLimitInfoByIdLimit.  idLimit = {}", idLimit);
			return mp.selectLimitInfoByIdLimit(idLimit);
		} finally {
			sess.close();
		}
	} // public static LimitInfo selectLimitInfoByIdLimit(int idLimit)
	
	
	/** Вернуть последнюю (по дате записи в БД) запись с данными сервиса DaData (+ комментарий) по ИНН. */
	public static DaDataInfo selectDaDataInfoLatestByInn(String inn) {
		SqlSession sess = MyBatisSqlSessionFactory.openSession();
		try {
			if (!sess.getConfiguration().hasMapper(SubjDopMapper.class)) {
				sess.getConfiguration().addMapper(SubjDopMapper.class);
			}
			SubjDopMapper mp = sess.getMapper(SubjDopMapper.class);
			DaDataInfo daDataInfo = mp.selectDaDataInfoLatestByInn(inn);
			if (daDataInfo != null) {
				daDataInfo.fixPartyActDateBeforeLong(); // сразу архивируем ДА в отдельное поле
			}
			logger.trace("selectDaDataInfoLatestByInn.  inn = {}, found?with saveTime = {}, ActDate = {}", inn, daDataInfo == null ? "<not_found>" : daDataInfo.getSaveTime(), daDataInfo == null ? "<not_found>" : daDataInfo.getPartyActDate() );
			return daDataInfo;
		} finally {
			sess.close();
		}
	} // public static DaDataInfo selectDaDataInfoLatestByInn(String inn)
	
	/** Добавить новую запись (новый ИНН или новая ДатаАктуальности - partyActDateLong).
	 * userComment и userName не обязательны; saveTime не используется (заполняется на стороне БД текущим временем).
	 */
// TODO: обработка ошибок (возможна конкуренция) !!
	public static int insertDaDataInfo(DaDataInfo daDataInfo) {
		if ( StringUtils.isEmpty(daDataInfo.getInn()) || StringUtils.isEmpty(daDataInfo.getSuggestParty()) || daDataInfo.getPartyActDate() == null || daDataInfo.getPartyActDateLong() == 0L ) {
			logger.error("insertDaDataInfo.  inconsistent object: {}", daDataInfo);
			throw new IllegalArgumentException("insertDaDataInfo.  inconsistent object: "+daDataInfo);
		}
		SqlSession sess = MyBatisSqlSessionFactory.openSession();
		int sqlRowCount = 0;
		try {
			if (!sess.getConfiguration().hasMapper(SubjDopMapper.class)) {
				sess.getConfiguration().addMapper(SubjDopMapper.class);
			}
			SubjDopMapper mp = sess.getMapper(SubjDopMapper.class);
			sqlRowCount = mp.insertDaDataInfo(daDataInfo);
			sess.commit();
			return sqlRowCount;
		} finally {
			sess.close();
			logger.trace("insertDaDataInfo.  inn = {}, sqlRowCount = {}", daDataInfo.getInn(), sqlRowCount);
		}
	} // public static int insertDaDataInfo(DaDataInfo daDataInfo)
	
	/** Обновить по ИНН + штампу_редакции (saveTime).
	 * Для обновления штамп daDataInfo.saveTime д.б. старый (на стороне БД обновляется текущим временем).
	 * Обновляем только коммент и юзера (при перегрузке из сервиса с новой ДА (проверка до вызова) всегда INSERT).
	 */
	public static int updateDaDataInfoBySaveTime(DaDataInfo daDataInfo) {
 		if ( StringUtils.isEmpty(daDataInfo.getInn()) || daDataInfo.getSaveTime() == null ) {
			logger.error("updateDaDataInfoBySaveTime.  inconsistent object (key fields: inn+SaveTime): {}", daDataInfo);
			throw new IllegalArgumentException("updateDaDataInfoBySaveTime.  inconsistent object (key fields: inn+SaveTime): "+daDataInfo);
		}
		SqlSession sess = MyBatisSqlSessionFactory.openSession();
		int sqlRowCount = 0;
		try {
			if (!sess.getConfiguration().hasMapper(SubjDopMapper.class)) {
				sess.getConfiguration().addMapper(SubjDopMapper.class);
			}
			SubjDopMapper mp = sess.getMapper(SubjDopMapper.class);
			sqlRowCount = mp.updateDaDataInfoBySaveTime(daDataInfo);
			sess.commit();
			return sqlRowCount;
		} finally {
			sess.close();
			logger.trace("updateDaDataInfoBySaveTime.  inn = {}, sqlRowCount = {}", daDataInfo.getInn(), sqlRowCount);
		}
	} // public static int updateDaDataInfoBySaveTime(DaDataInfo daDataInfo)
	
	/** Добавление/обновление записи по inn + ДатеАктуальности - partyActDateLong.
	 * Новый (по ДА) json всегда INSERT (userComment и userName не обязательны);
	 * ДА не поменялясь - UPDATE (userComment, userName, которые не проверяем).
	 * При любой операции обновляем saveTime = SYSTIMESTAMP, в записи поле штампа необязательно.
	 */
// !!! INSERT проходит, при UPDATE: ### Error updating database.  Cause: java.sql.SQLRecoverableException: Данные для считывания из сокета отсутствуют
// MERGE statement (http://stackoverflow.com/questions/19593785/how-can-i-use-oracle-merge-statement-using-mybatis)
// https://docs.oracle.com/database/121/SQLRF/statements_9016.htm#SQLRF01606
	public static int mergeDaDataInfoByActDate(DaDataInfo daDataInfo) {
		if ( StringUtils.isEmpty(daDataInfo.getInn()) || StringUtils.isEmpty(daDataInfo.getSuggestParty()) || daDataInfo.getPartyActDate() == null || daDataInfo.getPartyActDateLong() == 0L ) {
			logger.error("mergeDaDataInfoByActDate.  inconsistent object: {}", daDataInfo);
			throw new IllegalArgumentException("mergeDaDataInfoByActDate.  inconsistent object: "+daDataInfo);
		}
		SqlSession sess = MyBatisSqlSessionFactory.openSession();
		int sqlRowCount = 0;
		try {
			if (!sess.getConfiguration().hasMapper(SubjDopMapper.class)) {
				sess.getConfiguration().addMapper(SubjDopMapper.class);
			}
			SubjDopMapper mp = sess.getMapper(SubjDopMapper.class);
			sqlRowCount = mp.mergeDaDataInfoByActDate(daDataInfo);
			sess.commit();
			return sqlRowCount;
		} finally {
			sess.close();
			logger.trace("mergeDaDataInfoByActDate.  inn = {}, sqlRowCount = {}", daDataInfo.getInn(), sqlRowCount);
		}
	} // public static int mergeDaDataInfoByActDate(DaDataInfo daDataInfo)
	
} // public class SubjDopService