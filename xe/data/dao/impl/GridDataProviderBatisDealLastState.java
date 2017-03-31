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
import basos.xe.data.dao.DealLastStateMapper;
import basos.xe.data.ds.MyBatisSqlSessionFactory;
import basos.xe.data.entity.DealLastState;

/** Обеспечиваем лист-модель списком из DealLastState (все сделки без траншей в своём последнем состоянии), обёрнутых в GridData.
 * Источник OrclDataSource ("java:jboss/datasources/orcl"), мвью basos.mv_deals_last_state. Через MyBatis (маппер DealLastStateMapper; MyBatisSqlSessionFactory). Список типа ArrayList.
 */
@javax.enterprise.context.Dependent // RequestScoped
@javax.inject.Named("dealsProvider") // !!! org.zkoss.zkplus.cdi.DelegatingVariableResolver умеет внедрять только именованные бины, игнорирует аннотированные как альнернативы; в \WebContent\WEB-INF\beans.xml исключаются из сканирования все ненужные альтернативы с таким именем, должен остаться только один аннотированный класс с совпадающим именем как выбранная реализация
// вдруг возникло 01/03/17 !!! WELD-001503: Bean class which has interceptors cannot be declared final ???
public /*final*/ class GridDataProviderBatisDealLastState extends GridDataProviderWPk<DealLastState> implements Serializable {
// <T extends Object & Serializable & Comparable<? super T>>
	private static final long serialVersionUID = -9157131309867418543L;

	private static final Logger logger = LoggerFactory.getLogger(GridDataProviderBatisDealLastState.class);
	
	private final boolean depersonalize; // требование обезличивания клиентских данных
	
	/** Обезличиваем по умолчанию клиентские данные. */
	public GridDataProviderBatisDealLastState() { this(true/*false*/); }
	
	/** Тип бина - DealLastState. ПК изначально не устанавливается.
	 * @param depersonalize Флаг обезличивания клиентских данных.
	 */	
	public GridDataProviderBatisDealLastState(boolean depersonalize) {
		super(DealLastState.class);
		this.depersonalize = depersonalize;
		logger.trace("instantiate GridDataProviderBatisDealLastState.  depersonalize = {}", depersonalize);
		//setPk("idDeal", DealLastState.getCompareByIdDeal());
		//setPk(null, null);
	}
	
	/** Сделаем здесь со строкой данных то, что не позволяет маппер (или генератор): обезличивание, десятичная точность и т.п. */
	private DealLastState finishBean(DealLastState dataBean) {
		if (depersonalize) {
			dataBean.setClnName("Заёмщик № "+dataBean.getClnId());
			dataBean.setRsubjName("СПР № "+dataBean.getRsubjId());
		}
		BigDecimal tmp;
		
		tmp = dataBean.getYdur(); if (tmp != null) dataBean.setYdur(tmp.setScale(2));
		tmp = dataBean.getDgvrSum(); if (tmp != null) dataBean.setDgvrSum(tmp.setScale(2));
		tmp = dataBean.getDgvrSumUsd(); if (tmp != null) dataBean.setDgvrSumUsd(tmp.setScale(2));
		tmp = dataBean.getSumMsfoChargeoff(); if (tmp != null) dataBean.setSumMsfoChargeoff(tmp.setScale(2));
		tmp = dataBean.getSumProvisionUsd(); if (tmp != null) dataBean.setSumProvisionUsd(tmp.setScale(2));
		
		tmp = dataBean.getPerc(); if (tmp != null) dataBean.setPerc(tmp.setScale(4));
		tmp = dataBean.getFondRate(); if (tmp != null) dataBean.setFondRate(tmp.setScale(4));
		tmp = dataBean.getRiskaddRate(); if (tmp != null) dataBean.setRiskaddRate(tmp.setScale(4));
		tmp = dataBean.getMrashRate(); if (tmp != null) dataBean.setMrashRate(tmp.setScale(4));
		
		return dataBean;
	} // private DealLastState finishBean(DealLastState dataBean)
	
	/** Наполняет внутренный список (ArrayList) обёрток GridData бина DealLastState содержимым таблицы basos.mv_deals_last_state
	 * из источника OrclDataSource ("java:jboss/datasources/orcl") с использованием MyBatis. Вызывается из {@link #getGridDataList}.
	 */
	@Override
	protected final List<GridData<DealLastState>> populateGridDataList() {
		List<GridData<DealLastState>> locGridDataArrayList;
		locGridDataArrayList = new ArrayList<>();
		SqlSession sess = MyBatisSqlSessionFactory.openSession();
		try {
			if ( !sess.getConfiguration().hasMapper(DealLastStateMapper.class) ) {
				sess.getConfiguration().addMapper(DealLastStateMapper.class);
			}
// ч/з ResultHandler, т.к. бин нужно заворачивать (лист-модель - список обёрток GridData сущностей DealLastState)
// класс бина мутабельный, чтобы сделать обезличивание и установить scale для BigDecimal
			sess.select("basos.xe.data.dao.DealLastStateMapper.selectAll", new ResultHandler<DealLastState>() {
				@Override
				public void handleResult(ResultContext<? extends DealLastState> resultContext) {
					locGridDataArrayList.add( new GridData<DealLastState>(finishBean(resultContext.getResultObject()), beanClass) );
				}
			});
			
		} finally {
			sess.close();
		}
		((ArrayList<GridData<DealLastState>>)locGridDataArrayList).trimToSize();
		return locGridDataArrayList;
	} // protected final List<GridData<DealLastState>> populateGridDataList()

	
	@Override
	protected final int selectRowCount() {
		SqlSession sess = MyBatisSqlSessionFactory.openSession();
		try {
			if ( !sess.getConfiguration().hasMapper(DealLastStateMapper.class) ) {
				sess.getConfiguration().addMapper(DealLastStateMapper.class);
			}
			DealLastStateMapper mp = sess.getMapper(DealLastStateMapper.class);
			return mp.rowCount();
		} finally {
			sess.close();
		}
	}
	
	
	/** Наполняет список (ArrayList) обёрток GridData бина DealLastState содержимым таблицы basos.mv_deals_last_state
	 * по фильтру на значение индекса.
	 * из источника OrclDataSource ("java:jboss/datasources/orcl") с использованием MyBatis. Вызывается из {@link #getRange(Object)}.
	 * @param fieldName Название ключевого поля (сделки имеют Fk с полей rsubjId, clnId).
	 * @param key Значение ключевого поля.
	 * @return Может вернуть пустой список, но не null.
	 */
	@Override
	protected List<GridData<DealLastState>> selectRange(String fieldName, Object key) {
		List<GridData<DealLastState>> locGridDataArrayList = new ArrayList<>();
		String methodName = "";
		switch(fieldName) { // pk знаем и так (this.pk), но логикой управляет интерфейс
			case "clnId" : methodName = "basos.xe.data.dao.DealLastStateMapper.selectByClnId"; break;
			case "rsubjId" : methodName = "basos.xe.data.dao.DealLastStateMapper.selectByRsubjId"; break;
			default : logger.error("Поиск сделок по полю '{}' не поддерживается", fieldName);
				throw new UnsupportedOperationException("Поиск сделок не поддерживается по полю "+fieldName);
		}
		SqlSession sess = MyBatisSqlSessionFactory.openSession();
		try {
			if ( !sess.getConfiguration().hasMapper(DealLastStateMapper.class) ) {
				sess.getConfiguration().addMapper(DealLastStateMapper.class);
			}
// ч/з ResultHandler, т.к. бин нужно заворачивать (лист-модель - список обёрток GridData сущностей DealLastState)
// класс бина мутабельный, чтобы сделать обезличивание и установить scale для BigDecimal
			sess.select(methodName, key, new ResultHandler<DealLastState>() {
				@Override
				public void handleResult(ResultContext<? extends DealLastState> resultContext) {
					locGridDataArrayList.add( new GridData<DealLastState>(finishBean(resultContext.getResultObject()), beanClass) );
				}
			});
		} finally {
			sess.close();
		}
		((ArrayList<GridData<DealLastState>>)locGridDataArrayList).trimToSize(); // ???
		return locGridDataArrayList;
	} // protected List<GridData<DealLastState>> selectRange(String fieldName, Object key)
	
	
} // public final class GridDataProviderBatisDealLastState extends GridDataProviderWPk<DealLastState> implements Serializable