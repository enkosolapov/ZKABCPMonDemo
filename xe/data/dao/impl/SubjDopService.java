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


/** ����������� ��� ��������� ����������� ����-�������. */
public class SubjDopService {
	private static final Logger logger = LoggerFactory.getLogger(SubjDopService.class);
	
	
	/** ������ ������ ������ �� �� LM �������� (������ �� ����������). */
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

	
	/** ������� �������� �� �� LM ��������. */
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
	
	
	/** ���� �� ������ ������������ ������� ������� ������������� (�������) �� �� ��������.
	 * @return nullable
	 */
	public static LimitInfo selectLimitInfoBySubjId(int subjId) {
		SqlSession sess = MyBatisSqlSessionFactory.openSession();
		try {
			if (!sess.getConfiguration().hasMapper(SubjDopMapper.class)) {
				sess.getConfiguration().addMapper(SubjDopMapper.class);
			}
			SubjDopMapper mp = sess.getMapper(SubjDopMapper.class);
// FIXME: �������������� ����� �� ������, ������� ����� �������
			Integer idLimit = mp.selectIdLimitBySubjId(subjId);
			logger.trace("selectLimitInfoBySubjId.  subjId = {}, idLimit = {}", subjId, idLimit);
			if (idLimit == null) return null;
			return mp.selectLimitInfoByIdLimit(idLimit.intValue());
		} finally {
			sess.close();
		}
	} // public static LimitInfo selectLimitInfoBySubjId(int subjId)
	
	/** ���� �� ������ ������������ ������� ������� ������������� (�������) �� �� ������.
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
	
	
	/** ������� ��������� (�� ���� ������ � ��) ������ � ������� ������� DaData (+ �����������) �� ���. */
	public static DaDataInfo selectDaDataInfoLatestByInn(String inn) {
		SqlSession sess = MyBatisSqlSessionFactory.openSession();
		try {
			if (!sess.getConfiguration().hasMapper(SubjDopMapper.class)) {
				sess.getConfiguration().addMapper(SubjDopMapper.class);
			}
			SubjDopMapper mp = sess.getMapper(SubjDopMapper.class);
			DaDataInfo daDataInfo = mp.selectDaDataInfoLatestByInn(inn);
			if (daDataInfo != null) {
				daDataInfo.fixPartyActDateBeforeLong(); // ����� ���������� �� � ��������� ����
			}
			logger.trace("selectDaDataInfoLatestByInn.  inn = {}, found?with saveTime = {}, ActDate = {}", inn, daDataInfo == null ? "<not_found>" : daDataInfo.getSaveTime(), daDataInfo == null ? "<not_found>" : daDataInfo.getPartyActDate() );
			return daDataInfo;
		} finally {
			sess.close();
		}
	} // public static DaDataInfo selectDaDataInfoLatestByInn(String inn)
	
	/** �������� ����� ������ (����� ��� ��� ����� ���������������� - partyActDateLong).
	 * userComment � userName �� �����������; saveTime �� ������������ (����������� �� ������� �� ������� ��������).
	 */
// TODO: ��������� ������ (�������� �����������) !!
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
	
	/** �������� �� ��� + ������_�������� (saveTime).
	 * ��� ���������� ����� daDataInfo.saveTime �.�. ������ (�� ������� �� ����������� ������� ��������).
	 * ��������� ������ ������� � ����� (��� ���������� �� ������� � ����� �� (�������� �� ������) ������ INSERT).
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
	
	/** ����������/���������� ������ �� inn + ���������������� - partyActDateLong.
	 * ����� (�� ��) json ������ INSERT (userComment � userName �� �����������);
	 * �� �� ���������� - UPDATE (userComment, userName, ������� �� ���������).
	 * ��� ����� �������� ��������� saveTime = SYSTIMESTAMP, � ������ ���� ������ �������������.
	 */
// !!! INSERT ��������, ��� UPDATE: ### Error updating database.  Cause: java.sql.SQLRecoverableException: ������ ��� ���������� �� ������ �����������
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