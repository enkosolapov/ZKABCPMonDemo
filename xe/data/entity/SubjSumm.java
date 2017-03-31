package basos.xe.data.entity;

import java.io.Serializable;
//import java.util.Calendar;
import java.math.BigDecimal;
//import java.math.MathContext;
import java.math.RoundingMode;
//import java.sql.Date;
//import java.sql.RowId;
import java.time.*;
import java.time.temporal.ChronoUnit;
//import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
//import java.util.HashMap;
//import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Entity class "сводные данные субъекта (СПР/заёмщик) на последний срез с остатками", обёртываемый в GridData для использования в гриде.
 * RULE: все приватные нестатические поля доменного объекта класс beanClass должны относиться к полям данных.
 * Метод {@link #getPkMap()} - часть статического интерфейса.
 */
public final class SubjSumm implements Serializable, Comparable<SubjSumm> {
	private static final long serialVersionUID = 6989352457009426911L;
	
	private static final Logger logger = LoggerFactory.getLogger(SubjSumm.class);
	
	private static AtomicLong throughoutNum = new AtomicLong(0L); // порядковый номер (не используется)
	private static ThreadLocalRandom rnd = ThreadLocalRandom.current(); // Random = new Random(System.currentTimeMillis());
	static final String[] finsostArray = {"ПЛОХОЕ", "СРЕДНЕЕ", "ХОРОШЕЕ", null};
	static final String[] yaknameArray = {"Прочие отрасли", "Машиностроение", "Оптовая торговля", "АПК и пищевая промышленность", null};
	static final String[] cityArray = {"Чикаго", "Нью-Йорк", "Бостон", "Мытищи", "Люберцы", null};
	static final String[] catArray = {"Средний Региональный Бизнес", "Средний Отраслевой Бизнес", "Крупный Отраслевой Бизнес", "10-100", "Аффилированные компании", null};
	static final String[] subjtypeArray = {"КЛИЕНТ (ФЛ)", "КЛИЕНТ (ЮЛ)", "КЛИЕНТ (ИП)", "ГРУППА ЛИМИТА"};
	
	/** RULE: конструктор по умолчанию генерит тестовые данные. */
	public SubjSumm() {
//		String[] finsostArray = {"ПЛОХОЕ", "СРЕДНЕЕ", "ХОРОШЕЕ", null}; // static ThreadLocal<String[]>...
		throughoutNum.incrementAndGet();
		this.subj_id = rnd.nextInt(80, 100_000_000);
/* Взаимная конверсия датовых типов:
 http://stackoverflow.com/documentation/java/164/date-class#t=201612111809027021163
 http://stackoverflow.com/documentation/java/4813/dates-and-time-java-time#t=201611231701424697591
 http://stackoverflow.com/questions/29168494/how-to-convert-localdate-to-sql-date-java
 http://www.java67.com/2016/03/how-to-convert-date-to-localdatetime-in-java8-example.html

(rnd date) http://stackoverflow.com/questions/3985392/generate-random-date-of-birth

 new java.sql.Date(new java.util.Date().getTime()) - со временем !
*/
		this.dd_rest = java.sql.Date.valueOf( LocalDate.of(2016, 8, 31)/*.now().with(TemporalAdjusters.lastDayOfMonth())*/.minusMonths(rnd.nextInt(0, 45/*excl*/)) ); // 31.12.2012 - 31.08.2016
		this.pin_eq = "U"+RandomStringUtils.randomAlphanumeric(5).toUpperCase(); // !!! nullable, upper
		this.inn = RandomStringUtils.randomNumeric(10);
		this.subj_type = subjtypeArray[rnd.nextInt(0, subjtypeArray.length)];
		this.is_risk = rnd.nextBoolean();
		this.subj_name = "Субъект № "+this.subj_id;
		this.gr_name = RandomStringUtils.randomAlphabetic(8, 20).toUpperCase();
		this.cpr_id = "QD"+RandomStringUtils.randomNumeric(8);
		this.cpr_name = RandomStringUtils.randomAlphabetic(3, 10).toUpperCase();
		this.yak_name = yaknameArray[rnd.nextInt(0, yaknameArray.length)];
		this.br_name = this.yak_name == null ? null : this.yak_name + "_" + RandomStringUtils.random(5, "345678ABCDEF$@");
		this.city = cityArray[rnd.nextInt(0, cityArray.length)];
		this.ko = this.city == null ? null : "Отдел №"+rnd.nextInt(1, 30)+" города "+this.city;
		this.ko_fio = StringUtils.capitalize(RandomStringUtils.randomAlphabetic(2, 12).toLowerCase())+" "+StringUtils.capitalize(RandomStringUtils.randomAlphabetic(4, 9).toLowerCase())+" "+StringUtils.capitalize(RandomStringUtils.randomAlphabetic(7, 12).toLowerCase());
		this.kko_otdel = "Отдел № "+rnd.nextInt(1, 10);
		this.kko_upravl = "Управление № "+rnd.nextInt(1, 5);
		this.curator_fio = StringUtils.capitalize(RandomStringUtils.randomAlphabetic(2, 12).toLowerCase())+" "+StringUtils.capitalize(RandomStringUtils.randomAlphabetic(4, 9).toLowerCase())+" "+StringUtils.capitalize(RandomStringUtils.randomAlphabetic(7, 12).toLowerCase());
		this.cat_name = catArray[rnd.nextInt(0, catArray.length)];
		this.clibr_name = RandomStringUtils.random(5, "UIGDLWS");
/* Округление с заданной точностью: round(double x, int scale) есть в org.apache.commons.math3.util.Precision, он использует BigDecimal.setScale(scale, RoundingMode.HALF_UP)
 http://stackoverflow.com/questions/4796746/double-value-to-round-up-in-java
 http://stackoverflow.com/questions/2808535/round-a-double-to-2-decimal-places
 http://stackoverflow.com/questions/153724/how-to-round-a-number-to-n-decimal-places-in-java

 !! http://stackoverflow.com/questions/3843440/bigdecimal-setscale-and-round
One important point that is alluded to but not directly addressed is the difference between "precision" and "scale"
 and how they are used in the two statements. "precision" is the total number of significant digits in a number.
 "scale" is the number of digits to the right of the decimal point.
The MathContext constructor only accepts precision and RoundingMode as arguments, and therefore scale is never
 specified in the first statement.
setScale() obviously accepts scale as an argument, as well as RoundingMode, however precision is never specified
 in the second statement.
*/
		this.s_usd_bal = new BigDecimal(/*rnd.nextLong(100_000_000_000L)/100D ???*/rnd.nextDouble() * 1_000_000_000D).setScale(2, RoundingMode.HALF_UP);
		this.rest_msfo_usd = BigDecimal.valueOf(/*rnd.nextDouble(0, 1_000_000_000D) ???*/rnd.nextDouble() * 1_000_000_000D).setScale(2, RoundingMode.HALF_UP); // preferred way to convert from double
		this.s_usd_overbal = BigDecimal.valueOf(Math.round(rnd.nextDouble()*this.s_usd_bal.doubleValue()*100)/100D).setScale(2, RoundingMode.HALF_UP);
		this.s_usd_prosr = BigDecimal.valueOf(rnd.nextDouble()*(this.s_usd_bal.doubleValue()-this.s_usd_overbal.doubleValue())).setScale(2, RoundingMode.HALF_UP/*BigDecimal.ROUND_HALF_UP*/); // RoundingMode enum should be used instead !
		this.rest_money_usd = new BigDecimal(Math.round(rnd.nextDouble()*this.s_usd_bal.doubleValue()*100)/100D).setScale(2, RoundingMode.HALF_UP);
		this.rate_o = rnd.nextInt(0, 101);
		this.rate_uo = rnd.nextInt(0, 100-this.rate_o+1);
		this.rate_no = 100 - this.rate_o - this.rate_uo;
		this.cnt_tr = rnd.nextInt(0, 100);
		this.cnt_dgvr = rnd.nextInt(0, this.cnt_tr+1);
		this.dgvr_last = java.sql.Date.valueOf( LocalDate.of(2016, 8, 31).minus(rnd.nextInt(1400), ChronoUnit.DAYS) );
		this.b2segm = "b2segm";
		this.subj_rate = RandomStringUtils.random(1, "ABCDEF");
		this.sokball_int = BigDecimal.valueOf(rnd.nextDouble()*10).setScale(2, RoundingMode.HALF_UP/*BigDecimal.ROUND_HALF_UP*/);
		this.final_pd = BigDecimal.valueOf(rnd.nextDouble()).setScale(8, RoundingMode.HALF_UP/*BigDecimal.ROUND_HALF_UP*/); // include 1
		this.finsost = finsostArray[rnd.nextInt(0, finsostArray.length)];
		this.debtlevel_cp = "debtlevel_cp";
		this.subj_risk_dd_edit = new java.util.Date( LocalDateTime.now().minus(rnd.nextInt(1400*24*60*60), ChronoUnit.SECONDS).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() ); // DATE+TIME !!
		this.dd_probl_proj_begin = rnd.nextBoolean() ? null : java.sql.Date.valueOf(LocalDate.of(2016, 8, 31).minus(rnd.nextInt(4000), ChronoUnit.DAYS));
// Дней между датами: http://stackoverflow.com/questions/27005861/calculate-days-between-two-dates-in-java-8
		this.dd_probl_proj_close = (this.dd_probl_proj_begin == null || rnd.nextBoolean() || (this.dd_probl_proj_begin.toLocalDate().until(LocalDate.of(2016, 8, 31), ChronoUnit.DAYS) < 101)) ? null : java.sql.Date.valueOf( this.dd_probl_proj_begin.toLocalDate().plusDays(rnd.nextInt(100, (int)ChronoUnit.DAYS.between(this.dd_probl_proj_begin.toLocalDate(), LocalDate.of(2016, 8, 31))+1 )) );
		this.rate_provision = BigDecimal.valueOf(rnd.nextDouble()*99.9).setScale(2, BigDecimal.ROUND_HALF_UP);
		this.is_msfo_charge_off = rnd.nextBoolean();
		this.wl_zone = "wl_zone";
		this.cln_def_stat = "cln_def_stat";
		this.sme_flag = "sme_flag";
		this.subjectlevel = "subjectlevel";
		this.acd_committee_kk = "acd_committee_kk";
		this.idlimit = rnd.nextInt(100, 1000000);
		this.acd_limits = rnd.nextBoolean() ? null : RandomStringUtils.randomAscii(5, 50);
		this.okved = "okved";
		this.okved_name = "okved_name";
		this.asv_name_okeq = StringUtils.capitalize(RandomStringUtils.randomAlphabetic(5, 8).toLowerCase());
		this.sok_vyr_mln_rub_2015 = BigDecimal.valueOf(rnd.nextDouble()+rnd.nextInt(0, 1_000_000)).setScale(2, RoundingMode.HALF_UP/*BigDecimal.ROUND_HALF_UP*/);
		this.spark_vyr_mln_rub_2015 = BigDecimal.valueOf(rnd.nextLong(1_000_000), 0/*scale*/);
		this.acd_prod = rnd.nextBoolean() ? null : RandomStringUtils.randomAscii(5, 50);
		this.lmc_rowid = RandomStringUtils.randomAlphanumeric(18);
		this.cluid = RandomStringUtils.randomAlphanumeric(5, 13).toUpperCase();
	} // no-arg constructor of mock object

    public SubjSumm(int subj_id, java.sql.Date dd_rest, String pin_eq, String inn, String subj_type, boolean is_risk, String subj_name,
			String gr_name, String cpr_id, String cpr_name, String yak_name, String br_name, String city, String ko,
			String ko_fio, String kko_otdel, String kko_upravl, String curator_fio, String cat_name, String clibr_name,
			BigDecimal s_usd_bal, BigDecimal rest_msfo_usd, BigDecimal s_usd_overbal, BigDecimal s_usd_prosr,
			BigDecimal rest_money_usd, int rate_o, int rate_uo, int rate_no, int cnt_tr, int cnt_dgvr, java.sql.Date dgvr_last,
			String b2segm, String subj_rate, BigDecimal sokball_int, BigDecimal final_pd, String finsost,
			String debtlevel_cp, java.util.Date subj_risk_dd_edit, java.sql.Date dd_probl_proj_begin, java.sql.Date dd_probl_proj_close, BigDecimal rate_provision,
			boolean is_msfo_charge_off, String wl_zone, String cln_def_stat, String sme_flag, String subjectlevel,
			String acd_committee_kk, Integer idlimit, String acd_limits, String okved, String okved_name,
			String asv_name_okeq, BigDecimal sok_vyr_mln_rub_2015, BigDecimal spark_vyr_mln_rub_2015, String acd_prod,
			String/*RowId*/ lmc_rowid, String cluid) {
    	this.subj_id = subj_id;
		this.dd_rest = dd_rest;
		this.pin_eq = pin_eq;
		this.inn = inn;
		this.subj_type = subj_type;
		this.is_risk = is_risk;
		this.subj_name = subj_name;
		this.gr_name = gr_name;
		this.cpr_id = cpr_id;
		this.cpr_name = cpr_name;
		this.yak_name = yak_name;
		this.br_name = br_name;
		this.city = city;
		this.ko = ko;
		this.ko_fio = ko_fio;
		this.kko_otdel = kko_otdel;
		this.kko_upravl = kko_upravl;
		this.curator_fio = curator_fio;
		this.cat_name = cat_name;
		this.clibr_name = clibr_name;
		this.s_usd_bal = s_usd_bal;
		this.rest_msfo_usd = rest_msfo_usd;
		this.s_usd_overbal = s_usd_overbal;
		this.s_usd_prosr = s_usd_prosr;
		this.rest_money_usd = rest_money_usd;
		this.rate_o = rate_o;
		this.rate_uo = rate_uo;
		this.rate_no = rate_no;
		this.cnt_tr = cnt_tr;
		this.cnt_dgvr = cnt_dgvr;
		this.dgvr_last = dgvr_last;
		this.b2segm = b2segm;
		this.subj_rate = subj_rate;
		this.sokball_int = sokball_int;
		this.final_pd = final_pd;
		this.finsost = finsost;
		this.debtlevel_cp = debtlevel_cp;
		this.subj_risk_dd_edit = subj_risk_dd_edit;
		this.dd_probl_proj_begin = dd_probl_proj_begin;
		this.dd_probl_proj_close = dd_probl_proj_close;
		this.rate_provision = rate_provision;
		this.is_msfo_charge_off = is_msfo_charge_off;
		this.wl_zone = wl_zone;
		this.cln_def_stat = cln_def_stat;
		this.sme_flag = sme_flag;
		this.subjectlevel = subjectlevel;
		this.acd_committee_kk = acd_committee_kk;
		this.idlimit = idlimit;
		this.acd_limits = acd_limits;
		this.okved = okved;
		this.okved_name = okved_name;
		this.asv_name_okeq = asv_name_okeq;
		this.sok_vyr_mln_rub_2015 = sok_vyr_mln_rub_2015;
		this.spark_vyr_mln_rub_2015 = spark_vyr_mln_rub_2015;
		this.acd_prod = acd_prod;
		this.lmc_rowid = lmc_rowid;
		this.cluid = cluid;
		throughoutNum.incrementAndGet();
	} // public Subject(...)

	private final int subj_id; // NUMBER (9, 0)
    private final java.sql.Date dd_rest; // DATE
    private final String pin_eq; // 
    private final String inn; // 
    private final String subj_type; // VARCHAR2 (32 BYTE)
    private final boolean is_risk; // int
    private final String subj_name; // VARCHAR2 (512 BYTE)
    private final String gr_name; // VARCHAR2 (512 BYTE)
    private final String cpr_id; // 
    private final String cpr_name; // 
    private final String yak_name; // 
    private final String br_name; // VARCHAR2 (256 BYTE)
    private final String city; // VARCHAR2 (32 BYTE)
    private final String ko; // 
    private final String ko_fio; // 
    private final String kko_otdel; // VARCHAR2 (200 BYTE)
    private final String kko_upravl; // 
    private final String curator_fio; // 
    private final String cat_name; // VARCHAR2 (40 CHAR)
    private final String clibr_name; // VARCHAR2 (256 BYTE)
    private final BigDecimal s_usd_bal; // NUMBER
    private final BigDecimal rest_msfo_usd; // NUMBER
    private final BigDecimal s_usd_overbal; // 
    private final BigDecimal s_usd_prosr; // 
    private final BigDecimal rest_money_usd; // 
    private final int rate_o; // ? Byte
    private final int rate_uo; // ? Byte
    private final int rate_no; // ? Byte
    private final int cnt_tr; // ? Byte
    private final int cnt_dgvr; // ? Byte 
    private final java.sql.Date dgvr_last; // DATE
    private final String b2segm; // 
    private final String subj_rate; // 
    private final BigDecimal sokball_int; // 
    private final BigDecimal final_pd; // 
    private final String finsost; // 
    private final String debtlevel_cp; // 
    private final java.util.Date subj_risk_dd_edit; // DATE+TIME !! new 2016-12-06
    private final java.sql.Date dd_probl_proj_begin; // DATE
    private final java.sql.Date dd_probl_proj_close; // DATE
    private final BigDecimal rate_provision; // 
    private final boolean is_msfo_charge_off; // int -> boolean 
    private final String wl_zone; // 
    private final String cln_def_stat; // 
    private final String sme_flag; // 
    private final String subjectlevel; // 
    private final String acd_committee_kk; // 
    private final Integer idlimit; // 
    private final String acd_limits; // 
    private final String okved; // 
	private final String okved_name; // 
    private final String asv_name_okeq; // 
    private final BigDecimal sok_vyr_mln_rub_2015; // 
    private final BigDecimal spark_vyr_mln_rub_2015; // 
    private final String acd_prod; // 
    private final String/*RowId*/ lmc_rowid; // 
    private final String cluid; // 
    
	public int getSubj_id() {
		return subj_id;
	}

/*	public void setSubj_id(int subj_id) {
		this.subj_id = subj_id;
	}
*/
	public java.sql.Date getDd_rest() {
		return dd_rest;
	}

/*	public void setDd_rest(java.sql.Date dd_rest) {
		this.dd_rest = dd_rest;
	}
*/
	public String getPin_eq() {
		return pin_eq;
	}

/*	public void setPin_eq(String pin_eq) {
		this.pin_eq = pin_eq;
	}
*/
	public String getInn() {
		return inn;
	}

/*	public void setInn(String inn) {
		this.inn = inn;
	}
*/
	public String getSubj_type() {
		return subj_type;
	}

/*	public void setSubj_type(String subj_type) {
		this.subj_type = subj_type;
	}
*/
	public boolean isIs_risk() {
		return is_risk;
	}

/*	public void setIs_risk(boolean is_risk) {
		this.is_risk = is_risk;
	}
*/
	public String getSubj_name() {
		return subj_name;
	}

/*	public void setSubj_name(String subj_name) {
		this.subj_name = subj_name;
	}
*/
	public String getGr_name() {
		return gr_name;
	}

/*	public void setGr_name(String gr_name) {
		this.gr_name = gr_name;
	}
*/
	public String getCpr_id() {
		return cpr_id;
	}

/*	public void setCpr_id(String cpr_id) {
		this.cpr_id = cpr_id;
	}
*/
	public String getCpr_name() {
		return cpr_name;
	}

/*	public void setCpr_name(String cpr_name) {
		this.cpr_name = cpr_name;
	}
*/
	public String getYak_name() {
		return yak_name;
	}

/*	public void setYak_name(String yak_name) {
		this.yak_name = yak_name;
	}
*/
	public String getBr_name() {
		return br_name;
	}

/*	public void setBr_name(String br_name) {
		this.br_name = br_name;
	}
*/
	public String getCity() {
		return city;
	}

/*	public void setCity(String city) {
		this.city = city;
	}
*/
	public String getKo() {
		return ko;
	}

/*	public void setKo(String ko) {
		this.ko = ko;
	}
*/
	public String getKo_fio() {
		return ko_fio;
	}

/*	public void setKo_fio(String ko_fio) {
		this.ko_fio = ko_fio;
	}
*/
	public String getKko_otdel() {
		return kko_otdel;
	}

/*	public void setKko_otdel(String kko_otdel) {
		this.kko_otdel = kko_otdel;
	}
*/
	public String getKko_upravl() {
		return kko_upravl;
	}

/*	public void setKko_upravl(String kko_upravl) {
		this.kko_upravl = kko_upravl;
	}
*/
	public String getCurator_fio() {
		return curator_fio;
	}

/*	public void setCurator_fio(String curator_fio) {
		this.curator_fio = curator_fio;
	}
*/
	public String getCat_name() {
		return cat_name;
	}

/*	public void setCat_name(String cat_name) {
		this.cat_name = cat_name;
	}
*/
	public String getClibr_name() {
		return clibr_name;
	}

/*	public void setClibr_name(String clibr_name) {
		this.clibr_name = clibr_name;
	}
*/
	public BigDecimal getS_usd_bal() {
		return s_usd_bal;
	}

/*	public void setS_usd_bal(BigDecimal s_usd_bal) {
		this.s_usd_bal = s_usd_bal;
	}
*/
	public BigDecimal getRest_msfo_usd() {
		return rest_msfo_usd;
	}

/*	public void setRest_msfo_usd(BigDecimal rest_msfo_usd) {
		this.rest_msfo_usd = rest_msfo_usd;
	}
*/
	public BigDecimal getS_usd_overbal() {
		return s_usd_overbal;
	}

/*	public void setS_usd_overbal(BigDecimal s_usd_overbal) {
		this.s_usd_overbal = s_usd_overbal;
	}
*/
	public BigDecimal getS_usd_prosr() {
		return s_usd_prosr;
	}

/*	public void setS_usd_prosr(BigDecimal s_usd_prosr) {
		this.s_usd_prosr = s_usd_prosr;
	}
*/
	public BigDecimal getRest_money_usd() {
		return rest_money_usd;
	}

/*	public void setRest_money_usd(BigDecimal rest_money_usd) {
		this.rest_money_usd = rest_money_usd;
	}
*/
	public int getRate_o() {
		return rate_o;
	}

/*	public void setRate_o(int rate_o) {
		this.rate_o = rate_o;
	}
*/
	public int getRate_uo() {
		return rate_uo;
	}

/*	public void setRate_uo(int rate_uo) {
		this.rate_uo = rate_uo;
	}
*/
	public int getRate_no() {
		return rate_no;
	}

/*	public void setRate_no(int rate_no) {
		this.rate_no = rate_no;
	}
*/
	public int getCnt_tr() {
		return cnt_tr;
	}

/*	public void setCnt_tr(int cnt_tr) {
		this.cnt_tr = cnt_tr;
	}
*/
	public int getCnt_dgvr() {
		return cnt_dgvr;
	}

/*	public void setCnt_dgvr(int cnt_dgvr) {
		this.cnt_dgvr = cnt_dgvr;
	}
*/
	public java.sql.Date getDgvr_last() {
		return dgvr_last;
	}

/*	public void setDgvr_last(java.sql.Date dgvr_last) {
		this.dgvr_last = dgvr_last;
	}
*/
	public String getB2segm() {
		return b2segm;
	}

/*	public void setB2segm(String b2segm) {
		this.b2segm = b2segm;
	}
*/
	public String getSubj_rate() {
		return subj_rate;
	}

/*	public void setSubj_rate(String subj_rate) {
		this.subj_rate = subj_rate;
	}
*/
	public BigDecimal getSokball_int() {
		return sokball_int;
	}

/*	public void setSokball_int(BigDecimal sokball_int) {
		this.sokball_int = sokball_int;
	}
*/
	public BigDecimal getFinal_pd() {
		return final_pd;
	}

/*	public void setFinal_pd(BigDecimal final_pd) {
		this.final_pd = final_pd;
	}
*/
	public String getFinsost() {
		return finsost;
	}

/*	public void setFinsost(String finsost) {
		this.finsost = finsost;
	}
*/
	public String getDebtlevel_cp() {
		return debtlevel_cp;
	}

/*	public void setDebtlevel_cp(String debtlevel_cp) {
		this.debtlevel_cp = debtlevel_cp;
	}
*/
	public java.util.Date getSubj_risk_dd_edit() {
		return subj_risk_dd_edit;
	}

/*	public void setSubj_risk_dd_edit(java.util.Date subj_risk_dd_edit) {
		this.subj_risk_dd_edit = subj_risk_dd_edit;
	}
*/
	public java.sql.Date getDd_probl_proj_begin() {
		return dd_probl_proj_begin;
	}

/*	public void setDd_probl_proj_begin(java.sql.Date dd_probl_proj_begin) {
		this.dd_probl_proj_begin = dd_probl_proj_begin;
	}
*/
	public java.sql.Date getDd_probl_proj_close() {
		return dd_probl_proj_close;
	}

/*	public void setDd_probl_proj_close(java.sql.Date dd_probl_proj_close) {
		this.dd_probl_proj_close = dd_probl_proj_close;
	}
*/
	public BigDecimal getRate_provision() {
		return rate_provision;
	}

/*	public void setRate_provision(BigDecimal rate_provision) {
		this.rate_provision = rate_provision;
	}
*/
	public boolean isIs_msfo_charge_off() {
		return is_msfo_charge_off;
	}

/*	public void setIs_msfo_charge_off(boolean is_msfo_charge_off) {
		this.is_msfo_charge_off = is_msfo_charge_off;
	}
*/
	public String getWl_zone() {
		return wl_zone;
	}

/*	public void setWl_zone(String wl_zone) {
		this.wl_zone = wl_zone;
	}
*/
	public String getCln_def_stat() {
		return cln_def_stat;
	}

/*	public void setCln_def_stat(String cln_def_stat) {
		this.cln_def_stat = cln_def_stat;
	}
*/
	public String getSme_flag() {
		return sme_flag;
	}

/*	public void setSme_flag(String sme_flag) {
		this.sme_flag = sme_flag;
	}
*/
	public String getSubjectlevel() {
		return subjectlevel;
	}

/*	public void setSubjectlevel(String subjectlevel) {
		this.subjectlevel = subjectlevel;
	}
*/
	public String getAcd_committee_kk() {
		return acd_committee_kk;
	}

/*	public void setAcd_committee_kk(String acd_committee_kk) {
		this.acd_committee_kk = acd_committee_kk;
	}
*/
	public Integer getIdlimit() {
		return idlimit;
	}

/*	public void setIdlimit(Integer idlimit) {
		this.idlimit = idlimit;
	}
*/
	public String getAcd_limits() {
		return acd_limits;
	}

/*	public void setAcd_limits(String acd_limits) {
		this.acd_limits = acd_limits;
	}
*/
	public String getOkved() {
		return okved;
	}

/*	public void setOkved(String okved) {
		this.okved = okved;
	}
*/
	public String getOkved_name() {
		return okved_name;
	}

/*	public void setOkved_name(String okved_name) {
		this.okved_name = okved_name;
	}
*/
	public String getAsv_name_okeq() {
		return asv_name_okeq;
	}

/*	public void setAsv_name_okeq(String asv_name_okeq) {
		this.asv_name_okeq = asv_name_okeq;
	}
*/
	public BigDecimal getSok_vyr_mln_rub_2015() {
		return sok_vyr_mln_rub_2015;
	}

/*	public void setSok_vyr_mln_rub_2015(BigDecimal sok_vyr_mln_rub_2015) {
		this.sok_vyr_mln_rub_2015 = sok_vyr_mln_rub_2015;
	}
*/
	public BigDecimal getSpark_vyr_mln_rub_2015() {
		return spark_vyr_mln_rub_2015;
	}

/*	public void setSpark_vyr_mln_rub_2015(BigDecimal spark_vyr_mln_rub_2015) {
		this.spark_vyr_mln_rub_2015 = spark_vyr_mln_rub_2015;
	}
*/
	public String getAcd_prod() {
		return acd_prod;
	}

/*	public void setAcd_prod(String acd_prod) {
		this.acd_prod = acd_prod;
	}
*/    
    public /*RowId*/String getLmc_rowid() {
		return lmc_rowid;
	}

/*	public void setLmc_rowid(String lmc_rowid) { // RowId
		this.lmc_rowid = lmc_rowid;
	}
*/
	public String getCluid() {
		return cluid;
	}

/*	public void setCluid(String cluid) {
		this.cluid = cluid;
	}
*/
	public static long getSerialVersionUID() {
		return serialVersionUID;
	}

// (вызывается из hashCode() грид-обёртки GridData) для списка объектов хэшкод идентифицирует состояние фильтра
	@Override
	public int hashCode() { // https://docs.oracle.com/javase/8/docs/api/java/lang/Object.html#hashCode--
		return subj_id;
	}

// вызывается из equals() грид-обёртки GridData 
	@Override
	public boolean equals(Object obj) { // https://docs.oracle.com/javase/8/docs/api/java/lang/Object.html#equals-java.lang.Object-
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof SubjSumm))
			return false;
		SubjSumm other = (SubjSumm) obj;
		if (this.subj_id != other.subj_id) // getSubj_id()
			return false;
		return true;
	} // public boolean equals(Object obj)
	
// вызывается из compareTo() грид-обёртки GridData
	// Comparable implementation (https://docs.oracle.com/javase/8/docs/api/java/lang/Comparable.html)
	public int compareTo(SubjSumm obj) {
		if (this == obj)
			return 0;
		if (obj == null) {
			logger.error("null parameter in SubjSumm.comparTo. this = {}", this);
			throw new NullPointerException("null parameter in SubjSumm.comparTo. this = "+this);
		}
		return this.subj_id == obj.subj_id ? 0 : this.subj_id > obj.subj_id ? 1 : -1;
	} // public int compareTo(Subject obj)
	
	
	//private static Comparator<SubjSumm> compareBySubjId, compareByRowid, compareByCluid;
	
	//private static Map<String, Comparator<SubjSumm>> pkMap;
	/*static {
		pkMap = new HashMap<>((int)(3/0.5+0.01), 0.5f);
		pkMap.put("subj_id", SubjSumm.getCompareBySubjId());
		pkMap.put("lmc_rowid", SubjSumm.getCompareByRowid());
		pkMap.put("cluid", SubjSumm.getCompareByCluid());
	}*/
	
	// ради отложенной инициализации компараторов отказался от Map
	private static String[] pkNames = {"subj_id", "lmc_rowid", "cluid"};
	/** Таблица названий полей Первичных Ключей, поддерживаемых бином.
	 * Например, для наполнения меню выбора ПК. Далее по выбранному названию получаем компаратор вызовом {@link #getPkComparatorByName(String)}.
	 * (Это часть статического интерфейса бина. NOT null, но м.б. пустой массив.)
	 */
	public static String[] getPkNames() {
		return pkNames;
	}
	/** По названию поля ПК (одного из {@link #getPkNames()}) получить соответствующий Comparator<SubjSumm> для сравнения по этому полю.
	 * @return null если поле не является ПК.
	 */
	public static Comparator<SubjSumm> getPkComparatorByName(String namePk) {
    	switch (namePk) {
			case "subj_id": return getCompareBySubjId();
			case "lmc_rowid": return getCompareByRowid();
			case "cluid": return getCompareByCluid();
    	}
    	return null;
	}
	
// FIXME: thread-safe: ? Enum + EnumMap ?
	
	/** Таблица пар <имя, компаратор> Первичных Ключей, поддерживаемых бином. */
	// это часть статического интерфейса
	/*public static Map<String, Comparator<SubjSumm>> getPkMap() {
//		if (SubjSumm.pkMap == null) { // !! NOT thread-safe !!
//			pkMap = new HashMap<>((int)(3/0.5+0.01), 0.5f);
//			pkMap.put("subj_id", SubjSumm.getCompareBySubjId());
//			pkMap.put("lmc_rowid", SubjSumm.getCompareByRowid());
//			pkMap.put("cluid", SubjSumm.getCompareByCluid());
//		}
		return SubjSumm.pkMap;
	}*/
	
	/** Возвращает компаратор по ПК subj_id. Единственный консистентен с hashCode, equals, compareTo. */
	// вызывается из class CompareByPK implements Comparator<GridData>, который задаётся совместно с PK в дата-провайдере и используется при сортировке и бинарном поиске объекта GridData
	public static /*synchronized*/ Comparator<SubjSumm> getCompareBySubjId() {
		/*if (SubjSumm.compareBySubjId == null) {
			compareBySubjId = new CompareBySubjId();
		}
		return SubjSumm.compareBySubjId;*/
		return SubjSumm.CompareBySubjId.compareBySubjId; // (? Initialization-on-demand holder idiom) https://en.wikipedia.org/wiki/Initialization-on-demand_holder_idiom	http://stackoverflow.com/questions/16106260/thread-safe-singleton-class
	}
	/** Возвращает компаратор по искуственному ПК lmc_rowid (для тестирования). */
	public static /*synchronized*/ Comparator<SubjSumm> getCompareByRowid() {
		/*if (SubjSumm.compareByRowid == null) {
			compareByRowid = new CompareByRowid();
		}
		return SubjSumm.compareByRowid;*/
		return SubjSumm.CompareByRowid.compareByRowid;
	}
	/** Возвращает компаратор по искуственному ПК cluid (для тестирования). */
	public static /*synchronized*/ Comparator<SubjSumm> getCompareByCluid() {
		/*if (SubjSumm.compareByCluid == null) {
			compareByCluid = new CompareByCluid();
		}
		return SubjSumm.compareByCluid;*/
		return SubjSumm.CompareByCluid.compareByCluid;
	}
	
	private static final class CompareBySubjId implements Comparator<SubjSumm>, Serializable { // https://docs.oracle.com/javase/8/docs/api/java/util/Comparator.html#compare-T-T-
		private static final long serialVersionUID = -8955607435321698717L;
		private static final Comparator<SubjSumm> compareBySubjId = new CompareBySubjId();
		private CompareBySubjId() {logger.trace("CompareBySubjId created !");}
		public int compare(SubjSumm o1, SubjSumm o2) {
			if (o1 == null || o2 == null) {
				logger.error("SubjSumm.CompareBySubjId.compare. null parameter: o1 = {}, o2 = {}", o1, o2);
				throw new NullPointerException("SubjSumm.CompareBySubjId.compare. null parameter: o1 = " + o1 + ", o2 = " + o2);
			}
			return o1.subj_id == o2.subj_id ? 0 : o1.subj_id > o2.subj_id ? 1 : -1;
		} // public int compare(SubjSumm o1, SubjSumm o2)
	} // private static final class CompareBySubjId

	private static final class CompareByRowid implements Comparator<SubjSumm>, Serializable { // https://docs.oracle.com/javase/8/docs/api/java/util/Comparator.html#compare-T-T-
		private static final long serialVersionUID = 4029090453222560942L;
		private static final Comparator<SubjSumm> compareByRowid = new CompareByRowid();
		private CompareByRowid() {logger.trace("CompareByRowid created !");}
		public int compare(SubjSumm o1, SubjSumm o2) {
			if (o1 == null || o2 == null) {
				logger.error("SubjSumm.CompareByRowid.compare. null parameter: o1 = {}, o2 = {}", o1, o2);
				throw new NullPointerException("SubjSumm.CompareByRowid.compare. null parameter: o1 = " + o1 + ", o2 = " + o2);
			}
			return o1.lmc_rowid.compareTo(o2.lmc_rowid);
		} // public int compare(SubjSumm o1, SubjSumm o2)
	} // private static final class CompareByRowid

	private static final class CompareByCluid implements Comparator<SubjSumm>, Serializable { // https://docs.oracle.com/javase/8/docs/api/java/util/Comparator.html#compare-T-T-
		private static final long serialVersionUID = -1750299855474287730L;
		private static final Comparator<SubjSumm> compareByCluid = new CompareByCluid();
		private CompareByCluid() {logger.trace("CompareByCluid created !");}
		public int compare(SubjSumm o1, SubjSumm o2) {
			if (o1 == null || o2 == null) {
				logger.error("SubjSumm.CompareByCluid.compare. null parameter: o1 = {}, o2 = {}", o1, o2);
				throw new NullPointerException("SubjSumm.CompareByCluid.compare. null parameter: o1 = " + o1 + ", o2 = " + o2);
			}
			return o1.cluid.compareTo(o2.cluid);
		} // public int compare(SubjSumm o1, SubjSumm o2)
	} // private static final class CompareByCluid
	
} // public class SubjSumm implements Serializable, Comparable<SubjSumm>