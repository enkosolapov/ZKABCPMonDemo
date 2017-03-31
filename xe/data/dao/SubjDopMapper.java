package basos.xe.data.dao;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Options;
//import org.apache.ibatis.annotations.Results;
//import org.apache.ibatis.annotations.Result;
//import org.apache.ibatis.annotations.Many;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import basos.xe.data.entity.DaDataInfo;
import basos.xe.data.entity.LimitHistory;
import basos.xe.data.entity.LimitInfo;
import basos.xe.data.entity.SubjRestHistory;


/** Специфичные для субъектов разнородные запросы.
 *
 */
public interface SubjDopMapper {
	
	/** Состав группы лимита по ИД LM одного из участников. */
	@Select("select subj_name||' (ИНН: '||inn||'; ПИН Eq: '||subj_cod||')' inf from basos.mv_lmcp_cln where gr_id in (select gr_id from basos.mv_lmcp_cln where subj_id = #{subjId}) ORDER BY 1")
	@Options(useCache=true)
	List<String> selectGroupMembersBySubjId(int subjId);
	
	
	/** История задолженности по ОД (средняя за период и на точки; по РСБУ и УО) по ИД субъекта. */
	@Select("SELECT subj_id \"id\", dd_rest \"ddRest\", s_usd_bal \"restBalUSD\", s_usd_bal_prosr \"pastRestBalUSD\", rest_msfo_usd \"restUpravlUSD\", sum_msfo_chargeoff \"sumChargeoffUSD\", sum_provision_usd \"sumProvisionUSD\", sokball_int \"sokBallInt\" FROM mv_lmcp_cln_rests WHERE subj_id = #{subjId} ORDER BY dd_rest")
// HOWTO: через конструктор, т.к. entity иммутабельна; важен порядок параметров (а имена рефлективно не определить) !!
	@ConstructorArgs({
		@Arg(column="id", javaType=Integer.class),
		@Arg(column="ddRest", javaType=java.sql.Date.class),
		@Arg(column="restBalUSD", javaType=BigDecimal.class),
		@Arg(column="pastRestBalUSD", javaType=BigDecimal.class),
		@Arg(column="restUpravlUSD", javaType=BigDecimal.class),
		@Arg(column="sumChargeoffUSD", javaType=BigDecimal.class),
		@Arg(column="sumProvisionUSD", javaType=BigDecimal.class),
		@Arg(column="sokBallInt", javaType=BigDecimal.class)
	})
	List<SubjRestHistory> selectRestHistoryBySubjId(int subjId);
	
	
	/** Получить ИД лимита кредитования по ИД субъекта. */
	@Select("SELECT idLimit \"idLim\" FROM vw_subj_summ WHERE subj_id = #{subjId}")
	Integer selectIdLimitBySubjId(int subjId);
	
	/** История использования (выборки) лимита кредитования по ИД лимита. */
	@Select("SELECT idlimit \"idLim\", dd_rest \"ddRest\", limit_sum_usd \"limSumUSD\", exposure \"exposureUSD\", lim_filling \"limFilling\" FROM mv_limits_olap WHERE idLimit = #{idLimit} ORDER BY dd_rest")
	@ConstructorArgs({
		@Arg(column="idLim", javaType=Integer.class),
		@Arg(column="ddRest", javaType=java.sql.Date.class),
		@Arg(column="limSumUSD", javaType=BigDecimal.class),
		@Arg(column="exposureUSD", javaType=BigDecimal.class),
		@Arg(column="limFilling", javaType=BigDecimal.class)
	})
	List<LimitHistory> selectLimitHistoryByIdLimit(int idLimit);
	
	/** Инфо по лимиту кредитования включая историю использования (выборки) по ИД лимита. */
	@Select("SELECT id_limit \"idLim\", kk_number \"kkNum\", limit_sum \"limSum\", currency \"limCur\", id_owner \"idOwner\", owner \"nameOwner\", first_date \"firstDecisDate\", decision_date \"lastDecisDate\", committee \"committee\", decision_number \"decisNum\", d_confirm \"dconfirm\", d_end \"dend\" FROM lm_limit_report WHERE id_Limit = #{idLimit}")
	@ConstructorArgs({
		@Arg(column="idLim", javaType=Integer.class, id=true),
		@Arg(column="kkNum", javaType=Integer.class),
		@Arg(column="limSum", javaType=BigDecimal.class),
		@Arg(column="limCur", javaType=String.class),
		@Arg(column="idOwner", javaType=Integer.class),
		@Arg(column="nameOwner", javaType=String.class),
		@Arg(column="firstDecisDate", javaType=java.sql.Date.class),
		@Arg(column="lastDecisDate", javaType=java.sql.Date.class),
		@Arg(column="committee", javaType=String.class),
		@Arg(column="decisNum", javaType=String.class),
		@Arg(column="dconfirm", javaType=java.sql.Date.class),
		@Arg(column="dend", javaType=java.sql.Date.class),
		@Arg(column="idLim", javaType=List.class, select="basos.xe.data.dao.SubjDopMapper.selectLimitHistoryByIdLimit")
	})
	/*@Results({
		@Result(property="usageHistory", column="idLim", javaType=List.class, many=@Many(select="basos.xe.data.dao.SubjDopMapper.selectLimitHistoryByIdLimit"))
	})*/
	LimitInfo selectLimitInfoByIdLimit(int idlimit);
	
// FIXME: оптимизировать поиск ИД лимита, сделать одним вызовом
	/** Инфо по лимиту кредитования включая историю использования (выборки) по ИД субъекта. */
	//LimitInfo selectLimitInfoBySubjId(int subjId);
	
	
	/** Вернуть последнюю (по дате записи в БД) запись с данными сервиса DaData (+ комментарий) по ИНН. */
	@Select("SELECT ddi.inn \"inn\", ddi.suggestParty \"suggestParty\", ddi.userComment \"userComment\", ddi.userName \"userName\", ddi.partyActDateLong \"partyActDateLong\", ddi.partyActDate \"partyActDate\", ddi.saveTime \"saveTime\" FROM daDataInfo ddi WHERE ddi.inn = #{inn} AND ddi.saveTime = (SELECT MAX(ddi0.saveTime) FROM daDataInfo ddi0 WHERE ddi0.inn = ddi.inn)")
	@ConstructorArgs({
		@Arg(column="inn", javaType=String.class),
		@Arg(column="suggestParty", javaType=String.class),
		@Arg(column="userComment", javaType=String.class),
		@Arg(column="userName", javaType=String.class),
		@Arg(column="partyActDateLong", javaType=Long.class),
		@Arg(column="partyActDate", javaType=java.sql.Date.class),
		@Arg(column="saveTime", javaType=java.util.Date.class)
	})
	DaDataInfo selectDaDataInfoLatestByInn(String inn);
	
	
	/** Добавить новую запись (новый ИНН или новая ДатаАктуальности - partyActDateLong).
	 * userComment и userName не обязательны; saveTime не используется (заполняется на стороне БД текущим временем).
	 */
// RULE: для nullable полей нужно явно указывать jdbcType !!
	@Insert("INSERT INTO basos.daDataInfo (inn, suggestParty, userComment, userName, partyActDateLong, partyActDate) VALUES (#{inn}, #{suggestParty}, #{userComment,jdbcType=VARCHAR}, #{userName,jdbcType=VARCHAR}, #{partyActDateLong}, #{partyActDate})")
	int insertDaDataInfo(DaDataInfo daDataInfo);
	
	
	/** Обновить по ИНН + штампу_редакции (saveTime).
	 * Для обновления штамп daDataInfo.saveTime д.б. старый (на стороне БД обновляется текущим временем).
	 * Обновляем только коммент и юзера (при перегрузке из сервиса с новой ДА (проверка до вызова) всегда INSERT).
	 */
// RULE: для nullable полей нужно явно указывать jdbcType !!
	//@Update("UPDATE basos.daDataInfo SET saveTime = SYSTIMESTAMP, suggestParty = #{suggestParty}, userComment = #{userComment}, userName = #{userName}, partyActDateLong = #{partyActDateLong}, partyActDate = #{partyActDate} WHERE inn = #{inn} AND saveTime = #{saveTime}")
	@Update("UPDATE basos.daDataInfo SET saveTime = SYSTIMESTAMP, userComment = #{userComment,jdbcType=VARCHAR}, userName = #{userName,jdbcType=VARCHAR} WHERE inn = #{inn} AND saveTime = #{saveTime}")
	int updateDaDataInfoBySaveTime(DaDataInfo daDataInfo);
	
	/** Добавление/обновление записи по inn + ДатеАктуальности - partyActDateLong.
	 * Новый (по ДА) json всегда INSERT (userComment и userName не обязательны);
	 * ДА не поменялясь - UPDATE (userComment, userName, которые не проверяем).
	 * При любой операции обновляем saveTime = SYSTIMESTAMP, в записи поле штампа необязательно.
	 */
// MERGE statement (http://stackoverflow.com/questions/19593785/how-can-i-use-oracle-merge-statement-using-mybatis)
// https://docs.oracle.com/database/121/SQLRF/statements_9016.htm#SQLRF01606
	//#{suggestParty,javaType=String,jdbcType=CLOB}
	/* !!! INSERT проходит, при UPDATE: ### Error updating database.  Cause: java.sql.SQLRecoverableException: Данные для считывания из сокета отсутствуют
	 * http://stackoverflow.com/questions/11522919/java-sql-sqlrecoverableexception-no-more-data-to-read-from-socket
	 * http://stackoverflow.com/questions/7839907/no-more-data-to-read-from-socket-error?rq=1
	 * http://stackoverflow.com/questions/2305973/java-util-date-vs-java-sql-date?noredirect=1&lq=1
	 */
	@Update("MERGE INTO basos.daDataInfo ddi USING dual "
			+" ON (ddi.inn = #{inn} AND ddi.partyActDateLong = #{partyActDateLong}) "
			+" WHEN MATCHED THEN UPDATE SET ddi.userComment = #{userComment,jdbcType=VARCHAR}, ddi.userName = #{userName,jdbcType=VARCHAR}, ddi.saveTime = SYSTIMESTAMP "
			+" WHEN NOT MATCHED THEN INSERT (inn, suggestParty, userComment, userName, partyActDateLong, partyActDate) "
			  +" VALUES (#{inn}, #{suggestParty}, #{userComment,jdbcType=VARCHAR}, #{userName,jdbcType=VARCHAR}, #{partyActDateLong}, #{partyActDate})")
	int mergeDaDataInfoByActDate(DaDataInfo daDataInfo);
	
} // public interface SubjDopMapper