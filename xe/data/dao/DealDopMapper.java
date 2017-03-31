package basos.xe.data.dao;

import java.math.BigDecimal;
import java.util.List;

import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Arg;
import org.apache.ibatis.annotations.ConstructorArgs;
import org.apache.ibatis.annotations.Select;

import basos.xe.data.entity.DealRestHistory;



/** Специфичные для сделок запросы.
 *
 */
public interface DealDopMapper {
		
	@Select("SELECT NVL(avwr.idParent, avwr.idDeal) \"id\", avwr.dd1 \"dd1\", avwr.dd2 \"dd2\", SUM(avwr.avw_rest_USD) \"avwRestUSD\", (SELECT lmcp.rest_USD FROM lm_cp_nay lmcp WHERE lmcp.idDeal = NVL(avwr.idParent, avwr.idDeal) AND lmcp.dd_rest = avwr.dd2) \"dd2RestUSD\" FROM t_lm_avw_rests avwr WHERE NVL(avwr.idParent, avwr.idDeal) = #{idDeal} GROUP BY NVL(avwr.idParent, avwr.idDeal), avwr.dd1, avwr.dd2 ORDER BY dd2")
	@ConstructorArgs(value={
		@Arg(column="id", javaType=Integer.class),
		@Arg(column="dd1", javaType=java.sql.Date.class),
		@Arg(column="dd2", javaType=java.sql.Date.class),
		@Arg(column="avwRestUSD", javaType=BigDecimal.class),
		@Arg(column="dd2RestUSD", javaType=BigDecimal.class)
	})
	@Options(useCache=true)
	List<DealRestHistory> selectRestHistoryByIdDeal(int idDeal);
	
} // public interface DealDopMapper