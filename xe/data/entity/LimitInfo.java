package basos.xe.data.entity;

import java.math.BigDecimal;
import java.util.List;
//import java.sql.Date;


/** Информация по лимиту кредитования. */
public final class LimitInfo {
	
	/** Конструктор рукописный, не сгенерён, преобразования (e.g. setScale) можно делать в нём.
	 * Но типы смаппировал через @ConstructorArgs + @Arg в интерфейсе.
	 */
	public LimitInfo(Integer idLim, Integer kkNum, BigDecimal limSum, String limCur, Integer idOwner, String nameOwner,
			java.sql.Date firstDecisDate, java.sql.Date lastDecisDate, String committee, String decisNum,
			java.sql.Date dconfirm, java.sql.Date dend, List<LimitHistory> usageHistory) {
		this.idLim = idLim;
		this.kkNum = kkNum;
		this.limSum = limSum.setScale(2);
		this.limCur = limCur;
		this.idOwner = idOwner;
		this.nameOwner = nameOwner;
		this.firstDecisDate = firstDecisDate;
		this.lastDecisDate = lastDecisDate;
		this.committee = committee;
		this.decisNum = decisNum;
		this.dconfirm = dconfirm;
		this.dend = dend;
		this.usageHistory = usageHistory;
	}

	private final Integer idLim;
	private final Integer kkNum;
	private final BigDecimal limSum;
	private final String limCur;
	private final Integer idOwner;
	private final String nameOwner;
	private final java.sql.Date firstDecisDate;
	private final java.sql.Date lastDecisDate;
	private final String committee;
	private final String decisNum;
	private final java.sql.Date dconfirm;
	private final java.sql.Date dend;
	
	private final List<LimitHistory> usageHistory; // one-to-many

	public final Integer getIdLim() {
		return idLim;
	}
	public final Integer getKkNum() {
		return kkNum;
	}
	public final BigDecimal getLimSum() {
		return limSum;
	}
	public final String getLimCur() {
		return limCur;
	}
	public final Integer getIdOwner() {
		return idOwner;
	}
	public final String getNameOwner() {
		return nameOwner;
	}
	public final java.sql.Date getFirstDecisDate() {
		return firstDecisDate;
	}
	public final java.sql.Date getLastDecisDate() {
		return lastDecisDate;
	}
	public final String getCommittee() {
		return committee;
	}
	public final String getDecisNum() {
		return decisNum;
	}
	public final java.sql.Date getDconfirm() {
		return dconfirm;
	}
	public final java.sql.Date getDend() {
		return dend;
	}
	public final List<LimitHistory> getUsageHistory() {
		return usageHistory;
	}
	
} // public final class LimitInfo