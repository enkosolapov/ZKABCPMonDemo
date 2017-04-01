package basos.xe.data.entity;

import java.math.BigDecimal;
//import java.sql.Date;


/** Остатки за дату. ИД может быть сделки, транша. */
public final class DealRestHistory {
	
	/** Конструктор рукописный, не сгенерён, преобразования (e.g. setScale) можно делать в нём.
	 * Но типы смаппировал через @ConstructorArgs + @Arg в интерфейсе.
	 */
	public DealRestHistory(Integer/*BigDecimal*/ id, java.sql.Date/*java.sql.Timestamp*/ dd1, java.sql.Date/*java.sql.Timestamp*/ dd2, BigDecimal avwRestUSD, BigDecimal dd2RestUSD) {
		this.id = id; //Integer.valueOf(id.intValue());
		this.dd1 = dd1; //new Date(dd1.getTime());
		this.dd2 = dd2; //new Date(dd2.getTime());
		this.avwRestUSD = avwRestUSD.setScale(2);
		this.dd2RestUSD = dd2RestUSD == null ? null : dd2RestUSD.setScale(2);
	}
	
	private final Integer id;
	private final java.sql.Date dd1;
	private final java.sql.Date dd2;
	private final BigDecimal avwRestUSD;
	private final BigDecimal dd2RestUSD;
	
	public final Integer getId() {
		return id;
	}
	public final java.sql.Date getDd1() {
		return dd1;
	}
	public final java.sql.Date getDd2() {
		return dd2;
	}

	public final BigDecimal getAvwRestUSD() {
		return avwRestUSD;
	}
	public final BigDecimal getDd2RestUSD() {
		return dd2RestUSD;
	}
	
	private static final String[] header = {"ИД LM", "Дата с", "Дата по", "Средняя задолженность в периоде, USD", "Задолженность на конец периода, USD"}; 
	/** Строка заголовка с читабельными названиями полей. */
	public static final String[] getHeader() {
		return header;
	}
	
} // public final class DealRestHistory