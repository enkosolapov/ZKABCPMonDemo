package basos.core;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;

/*import javax.json.JsonNumber;
import javax.json.JsonString;
import javax.json.JsonValue;*/

import org.apache.commons.lang3.StringUtils;


/** BigDecimal to String money pattern formatter. Constatlly 18 chars "right aligned" (left padded with space): '### ### ### ##0.00'. Thread-safe (ThreadLocal DecimalFormat). */
public final class SafeFormatter {
		
	private static final ThreadLocal<DecimalFormat> moneyFormatter = ThreadLocal.withInitial( () -> {
	    DecimalFormatSymbols dfs = new DecimalFormatSymbols(Locale.ROOT);
	    dfs.setGroupingSeparator(' ');
	    DecimalFormat moneyFormat = new DecimalFormat();
        moneyFormat.setDecimalFormatSymbols(dfs);
        moneyFormat.setGroupingUsed(true);
        moneyFormat.setGroupingSize(3);
        moneyFormat.setMinimumIntegerDigits(1); // лидирующие нули в целой части
        moneyFormat.setDecimalSeparatorAlwaysShown(true);
        moneyFormat.setMinimumFractionDigits(2);
		return moneyFormat;
	});
	
	private static final ThreadLocal<DateFormat> germanDateFormatter = ThreadLocal.withInitial( () -> {
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy"); // формат даты java.sql.Date
		return sdf;
	});
	
	private static final ThreadLocal<DateFormat> yyyymmddDateFormatter = ThreadLocal.withInitial( () -> {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		return sdf;
	});
	
	private SafeFormatter() {}
	
	/** Форматирует по маске '### ### ### ##0.00'.
	 * @return Для null возвращает пустую строку.
	 */ //  (всегда 18 символов, выравнено вправо)
	public static final String asMoney(BigDecimal sum) {
		if (sum == null) return "";
		return moneyFormatter.get().format(sum);
		//return StringUtils.leftPad(moneyFormatter.get().format(sum), 18); // '### ### ### ##0.00': 18 chars
	}
	
	/** Форматирует по маске 'dd.MM.yyyy'.
	 * @return Для null возвращает пустую строку.
	 */
	public static final String asGermanDate(Date dt) {
		if (dt == null) return "";
		return germanDateFormatter.get().format(dt);
	}
	
	/** Форматирует по маске 'yyyyMMdd'.
	 * @return Для null возвращает пустую строку.
	 */
	public static final String dateASyyyymmdd(Date dt) {
		if (dt == null) return "";
		return yyyymmddDateFormatter.get().format(dt);
	}
	
	/**  */
/*	public static final String asJsonStr(JsonValue on) {
		if (on == null || JsonValue.NULL.equals(on) || "null".equals(on)) return "";
		if (on instanceof JsonString) return ((JsonString)on).getString();
		if (on instanceof JsonNumber) return ((JsonNumber)on).toString();
		return on.toString();
	}*/
	
	/**  */
/*	public static final String asJsonDate(JsonValue on) {
		if (on == null || "null".equals(on)) return "";
		return asGermanDate( Date.from( Instant.ofEpochMilli( ((JsonNumber)on).longValueExact() ) ) );
	}*/
	
	/** @param on Дата как Long в строке */
	public static final String asStrDate(String on) {
		if (StringUtils.isEmpty(on) /*|| "null".equals(on)*/) return "";
		return asGermanDate( Date.from( Instant.ofEpochMilli( Long.parseLong(on) ) ) );
	}
	
} // public final class SafeFormatter