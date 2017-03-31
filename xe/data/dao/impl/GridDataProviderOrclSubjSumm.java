package basos.xe.data.dao.impl;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.RandomStringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import basos.data.GridData;
import basos.data.dao.GridDataProviderWPk;
import basos.xe.data.ds.OrclDataSource;
import basos.xe.data.entity.SubjSumm;


/** Сводные клиентские данные (тип бина - SubjSumm) из источника OrclDataSource ("java:jboss/datasources/orcl"), вью basos.vw_subj_summ.
 * Список реализован через ArrayList. Pure JDBC.
 */
@javax.enterprise.context.Dependent // RequestScoped
@javax.inject.Named("subjSummProvider") // !!! org.zkoss.zkplus.cdi.DelegatingVariableResolver умеет внедрять только именованные бины, игнорирует аннотированные как альнернативы; в \WebContent\WEB-INF\beans.xml исключаются из сканирования все ненужные альтернативы с таким именем, должен остаться только один аннотированный класс с совпадающим именем как выбранная реализация
//@javax.enterprise.inject.Alternative
public /*final*/ class GridDataProviderOrclSubjSumm extends GridDataProviderWPk<SubjSumm> implements Serializable {
// <T extends Object & Serializable & Comparable<? super T>>
	private static final long serialVersionUID = 8200252913572880358L;

	private static final Logger logger = LoggerFactory.getLogger(GridDataProviderOrclSubjSumm.class);
	
// FIXME: текст запроса вынести в конфигурацию
	private static final String SUBJ_SUMM_SQL = "SELECT * from basos.vw_subj_summ order by subj_id";

	private final boolean depersonalize; // требование обезличивания клиентских данных
	
	/** Обезличиваем по умолчанию клиентские данные. */
	public GridDataProviderOrclSubjSumm() { this(/*false*/true); }
	
	/** Тип бина - SubjSumm. ПК изначально не устанавливается.
	 * @param depersonalize Флаг обезличивания клиентских данных.
	 */
	public GridDataProviderOrclSubjSumm(boolean depersonalize) {
		super(SubjSumm.class); // http://stackoverflow.com/questions/260666/can-an-abstract-class-have-a-constructor
		this.depersonalize = depersonalize;
		logger.trace("instantiate GridDataProviderOrclSubjSumm.  depersonalize = {}", depersonalize);
//		setPk("subj_id", SubjSumm.getCompareBySubjId());
//		setPk(null, null);
	} // public GridDataProviderOrclSubjSumm(boolean depersonalize)
	
	
	/** Наполняет внутренный список (ArrayList) обёрток GridData бина SubjSumm содержимым таблицы basos.vw_subj_summ из источника OrclDataSource ("java:jboss/datasources/orcl"). Вызывается из {@link #getGridDataList}. */
	@Override
	protected final List<GridData<SubjSumm>> populateGridDataList() {
		List<GridData<SubjSumm>> locGridDataArrayList;
//		try(Connection conn = DriverManager.getConnection(url)) {
		try(Connection conn = OrclDataSource.getDataSource().getConnection()) { // thread-safe
// HOWTO: как определить кол-во строк в рекордсете (http://stackoverflow.com/questions/7886462/how-to-get-row-count-using-resultset-in-java)
			try(Statement stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)) {
				try(ResultSet rs = stmt.executeQuery(SUBJ_SUMM_SQL)) {
				    rs.last();
				    this.totalRowCount = rs.getRow();
				    rs.beforeFirst();
				    logger.trace("populateGridDataList. RowCount = {}", this.totalRowCount);
					locGridDataArrayList = new ArrayList<>(this.totalRowCount);
					SubjSumm dataBean = null;
					while (rs.next()) {
						BigDecimal tmp;
						dataBean = new SubjSumm( rs.getInt("subj_id") // getLong ?? зачем ? numeric(9,0) ставки: numeric(12,4)
											   ,rs.getDate("dd_rest")
											   ,rs.getString("pin_eq")
											   ,depersonalize ? RandomStringUtils.randomNumeric(10) : rs.getString("inn")
											   ,rs.getString("subj_type")
											   ,rs.getBoolean("is_risk") // getInt("is_risk") 
											   ,depersonalize ? "Субъект № "+rs.getInt("subj_id") : rs.getString("subj_name")
											   ,depersonalize ? RandomStringUtils.randomAlphabetic(8, 20).toUpperCase() : rs.getString("gr_name")
											   ,rs.getString("cpr_id")
											   ,depersonalize ? RandomStringUtils.randomAlphabetic(3, 10).toUpperCase() : rs.getString("cpr_name")
											   ,rs.getString("yak_name")
											   ,rs.getString("br_name")
											   ,rs.getString("city")
											   ,rs.getString("ko")
											   ,rs.getString("ko_fio")
											   ,rs.getString("kko_otdel")
											   ,rs.getString("kko_upravl")
											   ,rs.getString("curator_fio")
											   ,rs.getString("cat_name")
											   ,rs.getString("clibr_name")
											   ,rs.getBigDecimal("s_usd_bal").setScale(2) // MS SQL numeric(14,2)
											   ,rs.getBigDecimal("rest_msfo_usd").setScale(2)
											   ,rs.getBigDecimal("s_usd_overbal").setScale(2)
											   ,rs.getBigDecimal("s_usd_prosr").setScale(2)
											   ,(tmp = rs.getBigDecimal("rest_money_usd")) == null ? null : tmp.setScale(2)
											   ,rs.getInt("rate_o") // ? Byte - в NumberInputElement нет меньше Intbox !!
											   ,rs.getInt("rate_uo")
											   ,rs.getInt("rate_no")
											   ,rs.getInt("cnt_tr") // ? Short - в NumberInputElement нет меньше Intbox !!
											   ,rs.getInt("cnt_dgvr") 
											   ,rs.getDate("dgvr_last")
											   ,rs.getString("b2segm")
											   ,rs.getString("subj_rate")
											   ,(tmp = rs.getBigDecimal("sokball_int")) == null ? null : tmp.setScale(2) // numeric(3,2)
											   ,(tmp = rs.getBigDecimal("final_pd")) == null ? null : tmp.setScale(8) // 8 зн. ?
											   ,rs.getString("finsost")
											   ,rs.getString("debtlevel_cp")
// RULE: используем для даты java.sql.Date, для даты+времени(сек) java.util.Date, для времени с наносами java.sql.Timestamp ! 
											   ,rs.getDate("subj_risk_dd_edit") == null ? null : new java.util.Date(rs.getDate("subj_risk_dd_edit").getTime()) // DATE+TIME !! new 2016-12-06   NOT getTimestamp (nanos !)
											   ,rs.getDate("dd_probl_proj_begin")
											   ,rs.getDate("dd_probl_proj_close")
											   ,(tmp = rs.getBigDecimal("rate_provision")) == null ? null : tmp.setScale(2) // 2 зн.
											   ,rs.getBoolean("is_msfo_charge_off") // int -> boolean
											   ,rs.getString("wl_zone")
											   ,rs.getString("cln_def_stat")
											   ,rs.getString("sme_flag")
											   ,rs.getString("subjectlevel")
											   ,rs.getString("acd_committee_kk")
											   ,rs.getObject("idlimit", Integer.class) // int but nullable !! // getString("acd_idlimit_lk")
											   ,depersonalize ? RandomStringUtils.randomAscii(0, 50) : rs.getString("acd_limits")
											   ,rs.getString("okved")
											   ,rs.getString("okved_name")
											   ,rs.getString("asv_name_okeq")
											   ,(tmp = rs.getBigDecimal("sok_vyr_mln_rub_2015")) == null ? null : tmp.setScale(2) // 2 зн.
											   ,(tmp = rs.getBigDecimal("spark_vyr_mln_rub_2015")) == null ? null : tmp.setScale(0) // 0 зн.
											   ,rs.getString("acd_prod")
											   ,rs.getRowId("lmc_rowid").toString()
											   ,rs.getString("cluid")
								);
						locGridDataArrayList.add( new GridData<SubjSumm>(dataBean, this.beanClass) );
					} // обход ResultSet
				} // try ResultSet
			} // try Statement
		} catch (SQLException e) {
			logger.error("SQLException in populateGridDataList", e);
			throw new InternalError("SQLException in populateGridDataList", e);
		} // try Connection
		((ArrayList<GridData<SubjSumm>>)locGridDataArrayList).trimToSize();
		
		return locGridDataArrayList;
	}; // protected List<GridData<SubjSumm>> populateGridDataList()
	
	@Override
	protected List<GridData<SubjSumm>> selectRange(String fieldName, Object key) {
		throw new UnsupportedOperationException("selectRange unsupported by GridDataProviderOrclSubjSumm at all.");
	}
	
	/** Быстрый поиск в списке GridData<SubjSumm> по полю subj_id. */
// not used
	public static int indexedBinarySearchBySubjId(/*Array*/List<GridData<SubjSumm>> a, int fromIndex, int toIndex, int key) {
// FIXME: проверять на RandomAccess !
		int low = 0;
		int high = toIndex - 1;
		while (low <= high) {
			int mid = (low + high) >>> 1;
			int midVal = ((SubjSumm)a.get(mid).getBean()).getSubj_id();
			if (midVal < key)
				low = mid + 1;
			else if (midVal > key)
				high = mid - 1;
			else
				return mid; // key found
		}
		return -(low + 1);  // key not found.
    } // public static int indexedBinarySearchBySubjId(List<GridData>/*RandomAccess*/ a, int fromIndex, int toIndex, int key)

} // public class GridDataProviderOrclSubjSumm extends GridDataProviderWPk<SubjSumm>