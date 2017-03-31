package basos.xe.data.entity;

import java.math.BigDecimal;
//import java.sql.Date;


/** Остатки за дату. ИД может быть заёмщика, СПР, сделки... */
public final class SubjRestHistory {
	
	/** Конструктор рукописный, не сгенерён, преобразования (e.g. setScale) можно делать в нём.
	 * Но типы смаппировал через @ConstructorArgs + @Arg в интерфейсе.
	 */
	public SubjRestHistory(Integer/*BigDecimal*/ id, java.sql.Date/*java.sql.Timestamp*/ ddRest, BigDecimal restBalUSD, BigDecimal pastRestBalUSD,
			BigDecimal restUpravlUSD, BigDecimal sumChargeoffUSD, BigDecimal sumProvisionUSD, BigDecimal sokBallInt) {
		this.id = id; //Integer.valueOf(id.intValue());
		this.ddRest = ddRest; //new Date(ddRest.getTime());
		this.restBalUSD = restBalUSD.setScale(2);
		this.pastRestBalUSD = pastRestBalUSD.setScale(2);
		this.restUpravlUSD = restUpravlUSD.setScale(2);
		this.sumChargeoffUSD = sumChargeoffUSD.setScale(2);
		this.sumProvisionUSD = sumProvisionUSD.setScale(2);
		this.sokBallInt = sokBallInt == null ? null : sokBallInt.setScale(2);
	}
	
	private final Integer id;
	private final java.sql.Date ddRest;
	private final BigDecimal restBalUSD;
	private final BigDecimal pastRestBalUSD;
	private final BigDecimal restUpravlUSD;
	private final BigDecimal sumChargeoffUSD;
	private final BigDecimal sumProvisionUSD;
	private final BigDecimal sokBallInt;
	
	public final Integer getId() {
		return id;
	}
	public final java.sql.Date getDdRest() {
		return ddRest;
	}
	public final BigDecimal getRestBalUSD() {
		return restBalUSD;
	}
	public final BigDecimal getPastRestBalUSD() {
		return pastRestBalUSD;
	}
	public final BigDecimal getRestUpravlUSD() {
		return restUpravlUSD;
	}
	public final BigDecimal getSumChargeoffUSD() {
		return sumChargeoffUSD;
	}
	public final BigDecimal getSumProvisionUSD() {
		return sumProvisionUSD;
	}
	public final BigDecimal getSokBallInt() {
		return sokBallInt;
	}
	
	private static final String[] header = {"ИД LM", "За дату", "Задолженность в РСБУ, USD", "Просрочка в РСБУ, USD", "Позиция в МСФО, USD", "Списано в МСФО, USD", "Спецпровизия, USD", "Внутр. балл СОК"}; 
	/** Строка заголовка с читабельными названиями полей. */
	public static final String[] getHeader() {
		return header;
	}
	
} // public final class SubjRestHistory