package basos.xe.data.entity;

import java.math.BigDecimal;
//import java.sql.Date;


/** ������� ������������� (�������) ������. */
public final class LimitHistory {
	
	/** ����������� ����������, �� �������, �������������� (e.g. setScale) ����� ������ � ��.
	 * �� ���� ����������� ����� @ConstructorArgs + @Arg � ����������.
	 */
	public LimitHistory(Integer idLim, java.sql.Date ddRest, BigDecimal limSumUSD, BigDecimal exposureUSD, BigDecimal limFilling) {
		this.idLim = idLim;
		this.ddRest = ddRest;
		this.limSumUSD = limSumUSD.setScale(2);
		this.exposureUSD = exposureUSD.setScale(2);
		this.limFilling = limFilling.setScale(4);
	}
	
	private final Integer idLim;
	private final java.sql.Date ddRest;
	private final BigDecimal limSumUSD;
	private final BigDecimal exposureUSD;
	private final BigDecimal limFilling; // scale: 4
	
	public final Integer getIdLim() {
		return idLim;
	}
	public final java.sql.Date getDdRest() {
		return ddRest;
	}
	public final BigDecimal getLimSumUSD() {
		return limSumUSD;
	}
	public final BigDecimal getExposureUSD() {
		return exposureUSD;
	}
	public final BigDecimal getLimFilling() {
		return limFilling;
	}
	
} // public final class LimitHistory