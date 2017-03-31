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

/** Обеспечиваем лист-модель списком из TrancheLastState (все транши в своём последнем состоянии), обёрнутых в GridData.
 * Источник OrclDataSource ("java:jboss/datasources/orcl"), мвью basos.mv_tranches_last_state. Через MyBatis (маппер TrancheLastStateMapper; MyBatisSqlSessionFactory). Список типа ArrayList.
 */
@javax.enterprise.context.Dependent // RequestScoped
@javax.inject.Named("tranchesProvider") // !!! org.zkoss.zkplus.cdi.DelegatingVariableResolver умеет внедрять только именованные бины, игнорирует аннотированные как альнернативы; в \WebContent\WEB-INF\beans.xml исключаются из сканирования все ненужные альтернативы с таким именем, должен остаться только один аннотированный класс с совпадающим именем как выбранная реализация
public /*final*/ class GridDataProviderBatisTrancheLastState extends GridDataProviderWPk<TrancheLastState> implements Serializable {
// <T extends Object & Serializable & Comparable<? super T>>
	private static final long serialVersionUID = -9157131309867418543L;

	private static final Logger logger = LoggerFactory.getLogger(GridDataProviderBatisTrancheLastState.class);
	
	private final boolean depersonalize; // требование обезличивания клиентских данных
	
	
	/** Обезличиваем по умолчанию клиентские данные. */
	public GridDataProviderBatisTrancheLastState() { this(/*true*/false); }
	
	
	/** Тип бина - TrancheLastState. ПК изначально не устанавливается.
	 * @param depersonalize Флаг обезличивания клиентских данных.
	 */	
	public GridDataProviderBatisTrancheLastState(boolean depersonalize) {
		super(TrancheLastState.class);
		this.depersonalize = depersonalize;
		logger.trace("instantiate GridDataProviderBatisTrancheLastState.  depersonalize = {}", depersonalize);
		//setPk("idDeal", TrancheLastState.getCompareByIdDeal());
		//setPk(null, null);
		setPk("idParent", TrancheLastState.getCompareByIdParent()); // это не Pk, а Fk на рамки.idDeal; нужна только сортировка для быстрого поиска деталей
	}
	
	
	/** Сделаем здесь со строкой данных то, что не позволяет маппер (или генератор): обезличивание, десятичная точность и т.п. */
	private TrancheLastState finishBean(TrancheLastState dataBean) {
		if (depersonalize) {
//нечего обезличивать
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
	
	
	/** Наполняет внутренный список (ArrayList) обёрток GridData бина TrancheLastState содержимым таблицы basos.mv_tranches_last_state
	 * из источника OrclDataSource ("java:jboss/datasources/orcl") с использованием MyBatis. Вызывается из {@link #getGridDataList}.
	 */
	@Override
	protected final List<GridData<TrancheLastState>> populateGridDataList() {
		List<GridData<TrancheLastState>> locGridDataArrayList = new ArrayList<>();
		SqlSession sess = MyBatisSqlSessionFactory.openSession();
		try {
			if ( !sess.getConfiguration().hasMapper(TrancheLastStateMapper.class) ) {
				sess.getConfiguration().addMapper(TrancheLastStateMapper.class);
			}
// ч/з ResultHandler, т.к. бин нужно заворачивать (лист-модель - список обёрток GridData сущностей TrancheLastState)
// класс бина мутабельный, чтобы сделать обезличивание и установить scale для BigDecimal
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
	
	
	/** Наполняет список (ArrayList) обёрток GridData бина TrancheLastState содержимым таблицы basos.mv_tranches_last_state
	 * по фильтру на значение индекса.
	 * из источника OrclDataSource ("java:jboss/datasources/orcl") с использованием MyBatis. Вызывается из {@link #getRange(Object)}.
	 * @param fieldName Название ключевого поля (транши имеют Fk с полей idParent, rsubjId, clnId).
	 * @param key Значение ключевого поля.
	 * @return Может вернуть пустой список, но не null.
	 */
	@Override
	protected final List<GridData<TrancheLastState>> selectRange(String fieldName, Object key) {
		List<GridData<TrancheLastState>> locGridDataArrayList = new ArrayList<>();
		String methodName = "";
		switch(fieldName) { // pk знаем и так (this.pk), но логикой управляет интерфейс
			case "clnId" : methodName = "basos.xe.data.dao.TrancheLastStateMapper.selectByClnId"; break;
			case "rsubjId" : methodName = "basos.xe.data.dao.TrancheLastStateMapper.selectByRsubjId"; break;
			case "idParent" : methodName = "basos.xe.data.dao.TrancheLastStateMapper.selectByIdParent"; break;
			default : logger.error("Поиск траншей по полю '{}' не поддерживается", fieldName);
				throw new UnsupportedOperationException("Поиск траншей не поддерживается по полю "+fieldName);
		}
		SqlSession sess = MyBatisSqlSessionFactory.openSession();
		try {
			if ( !sess.getConfiguration().hasMapper(TrancheLastStateMapper.class) ) {
				sess.getConfiguration().addMapper(TrancheLastStateMapper.class);
			}
// ч/з ResultHandler, т.к. бин нужно заворачивать (лист-модель - список обёрток GridData сущностей TrancheLastState)
// класс бина мутабельный, чтобы сделать обезличивание и установить scale для BigDecimal
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