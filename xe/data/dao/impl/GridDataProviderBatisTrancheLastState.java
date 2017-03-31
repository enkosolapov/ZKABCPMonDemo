package basos.xe.data.dao.impl;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.session.ResultContext;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.SqlSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import basos.data.GridData;
import basos.data.dao.GridDataProviderWPk;
import basos.xe.data.dao.TrancheLastStateMapper;
import basos.xe.data.ds.MyBatisSqlSessionFactory;
import basos.xe.data.entity.TrancheLastState;

/** ������������ ����-������ ������� �� TrancheLastState (��� ������ � ���� ��������� ���������), �������� � GridData.
 * �������� OrclDataSource ("java:jboss/datasources/orcl"), ���� basos.mv_tranches_last_state. ����� MyBatis (������ TrancheLastStateMapper; MyBatisSqlSessionFactory). ������ ���� ArrayList.
 */
@javax.enterprise.context.Dependent // RequestScoped
@javax.inject.Named("tranchesProvider") // !!! org.zkoss.zkplus.cdi.DelegatingVariableResolver ����� �������� ������ ����������� ����, ���������� �������������� ��� ������������; � \WebContent\WEB-INF\beans.xml ����������� �� ������������ ��� �������� ������������ � ����� ������, ������ �������� ������ ���� �������������� ����� � ����������� ������ ��� ��������� ����������
public /*final*/ class GridDataProviderBatisTrancheLastState extends GridDataProviderWPk<TrancheLastState> implements Serializable {
// <T extends Object & Serializable & Comparable<? super T>>
	private static final long serialVersionUID = -9157131309867418543L;

	private static final Logger logger = LoggerFactory.getLogger(GridDataProviderBatisTrancheLastState.class);
	
	private final boolean depersonalize; // ���������� ������������� ���������� ������
	
	
	/** ������������ �� ��������� ���������� ������. */
	public GridDataProviderBatisTrancheLastState() { this(/*true*/false); }
	
	
	/** ��� ���� - TrancheLastState. �� ���������� �� ���������������.
	 * @param depersonalize ���� ������������� ���������� ������.
	 */	
	public GridDataProviderBatisTrancheLastState(boolean depersonalize) {
		super(TrancheLastState.class);
		this.depersonalize = depersonalize;
		logger.trace("instantiate GridDataProviderBatisTrancheLastState.  depersonalize = {}", depersonalize);
		//setPk("idDeal", TrancheLastState.getCompareByIdDeal());
		//setPk(null, null);
		setPk("idParent", TrancheLastState.getCompareByIdParent()); // ��� �� Pk, � Fk �� �����.idDeal; ����� ������ ���������� ��� �������� ������ �������
	}
	
	
	/** ������� ����� �� ������� ������ ��, ��� �� ��������� ������ (��� ���������): �������������, ���������� �������� � �.�. */
	private TrancheLastState finishBean(TrancheLastState dataBean) {
		if (depersonalize) {
//������ ������������
		}
		BigDecimal tmp;
		
		tmp = dataBean.getYdur(); if (tmp != null) dataBean.setYdur(tmp.setScale(2));
		tmp = dataBean.getDgvrSum(); if (tmp != null) dataBean.setDgvrSum(tmp.setScale(2));
		tmp = dataBean.getDgvrSumUSD(); if (tmp != null) dataBean.setDgvrSumUSD(tmp.setScale(2));
		
		tmp = dataBean.getPerc(); if (tmp != null) dataBean.setPerc(tmp.setScale(4));
		tmp = dataBean.getFondRate(); if (tmp != null) dataBean.setFondRate(tmp.setScale(4));
		tmp = dataBean.getRiskAddRate(); if (tmp != null) dataBean.setRiskAddRate(tmp.setScale(4));
		tmp = dataBean.getMrashRate(); if (tmp != null) dataBean.setMrashRate(tmp.setScale(4));
		return dataBean;
	}
	
	
	/** ��������� ���������� ������ (ArrayList) ������ GridData ���� TrancheLastState ���������� ������� basos.mv_tranches_last_state
	 * �� ��������� OrclDataSource ("java:jboss/datasources/orcl") � �������������� MyBatis. ���������� �� {@link #getGridDataList}.
	 */
	@Override
	protected final List<GridData<TrancheLastState>> populateGridDataList() {
		List<GridData<TrancheLastState>> locGridDataArrayList = new ArrayList<>();
		SqlSession sess = MyBatisSqlSessionFactory.openSession();
		try {
			if ( !sess.getConfiguration().hasMapper(TrancheLastStateMapper.class) ) {
				sess.getConfiguration().addMapper(TrancheLastStateMapper.class);
			}
// �/� ResultHandler, �.�. ��� ����� ������������ (����-������ - ������ ������ GridData ��������� TrancheLastState)
// ����� ���� �����������, ����� ������� ������������� � ���������� scale ��� BigDecimal
			sess.select("basos.xe.data.dao.TrancheLastStateMapper.selectAll", new ResultHandler<TrancheLastState>() {
				@Override
				public void handleResult(ResultContext<? extends TrancheLastState> resultContext) {
					locGridDataArrayList.add( new GridData<TrancheLastState>(finishBean(resultContext.getResultObject()), beanClass) );
				}
			});
		} finally {
			sess.close();
		}
		((ArrayList<GridData<TrancheLastState>>)locGridDataArrayList).trimToSize();
		return locGridDataArrayList;
	} // protected final List<GridData<TrancheLastState>> populateGridDataList()
	
	
	@Override
	protected final int selectRowCount() {
		SqlSession sess = MyBatisSqlSessionFactory.openSession();
		try {
			if ( !sess.getConfiguration().hasMapper(TrancheLastStateMapper.class) ) {
				sess.getConfiguration().addMapper(TrancheLastStateMapper.class);
			}
			TrancheLastStateMapper mp = sess.getMapper(TrancheLastStateMapper.class);
			return mp.rowCount();
		} finally {
			sess.close();
		}
	}
	
	
	/** ��������� ������ (ArrayList) ������ GridData ���� TrancheLastState ���������� ������� basos.mv_tranches_last_state
	 * �� ������� �� �������� �������.
	 * �� ��������� OrclDataSource ("java:jboss/datasources/orcl") � �������������� MyBatis. ���������� �� {@link #getRange(Object)}.
	 * @param fieldName �������� ��������� ���� (������ ����� Fk � ����� idParent, rsubjId, clnId).
	 * @param key �������� ��������� ����.
	 * @return ����� ������� ������ ������, �� �� null.
	 */
	@Override
	protected final List<GridData<TrancheLastState>> selectRange(String fieldName, Object key) {
		List<GridData<TrancheLastState>> locGridDataArrayList = new ArrayList<>();
		String methodName = "";
		switch(fieldName) { // pk ����� � ��� (this.pk), �� ������� ��������� ���������
			case "clnId" : methodName = "basos.xe.data.dao.TrancheLastStateMapper.selectByClnId"; break;
			case "rsubjId" : methodName = "basos.xe.data.dao.TrancheLastStateMapper.selectByRsubjId"; break;
			case "idParent" : methodName = "basos.xe.data.dao.TrancheLastStateMapper.selectByIdParent"; break;
			default : logger.error("����� ������� �� ���� '{}' �� ��������������", fieldName);
				throw new UnsupportedOperationException("����� ������� �� �������������� �� ���� "+fieldName);
		}
		SqlSession sess = MyBatisSqlSessionFactory.openSession();
		try {
			if ( !sess.getConfiguration().hasMapper(TrancheLastStateMapper.class) ) {
				sess.getConfiguration().addMapper(TrancheLastStateMapper.class);
			}
// �/� ResultHandler, �.�. ��� ����� ������������ (����-������ - ������ ������ GridData ��������� TrancheLastState)
// ����� ���� �����������, ����� ������� ������������� � ���������� scale ��� BigDecimal
			sess.select(methodName, key, new ResultHandler<TrancheLastState>() {
				@Override
				public void handleResult(ResultContext<? extends TrancheLastState> resultContext) {
					locGridDataArrayList.add( new GridData<TrancheLastState>(finishBean(resultContext.getResultObject()), beanClass) );
				}
			});
		} finally {
			sess.close();
		}
		((ArrayList<GridData<TrancheLastState>>)locGridDataArrayList).trimToSize(); // ???
		return locGridDataArrayList;
	} // protected final List<GridData<TrancheLastState>> selectRange(String fieldName, Object key)
	
} // public final class GridDataProviderBatisTrancheLastState extends GridDataProviderWPk<TrancheLastState> implements Serializable