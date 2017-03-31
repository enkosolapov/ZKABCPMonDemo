package basos.zkui;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.HtmlMacroComponent;
import org.zkoss.zk.ui.ext.Disable;
import org.zkoss.zk.ui.select.annotation.*;
import org.zkoss.zul.Constraint;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Image;
import org.zkoss.zul.Window;
import org.zkoss.zul.impl.InputElement;


/** ����� ��������������� (��������) between_filter_decimal.zul/between_filter_date.zul (����������� ������ �������� �������� / ���).
 * ���������� �� ���� InputElement, �������� �������� ��������. �������� ����� ������� (������ image) ���������� ����� (� ������ "highlighted").
 * � ��������� ����� ���������� (��� ������ ������ GridDataFilter) ��� � ����������� �������� ������� � ��������������� ������ ����� ��������.
 * ��������-������������ minVal, maxVal (��������������� ��������) �������� � ������������ �� ��������������� ��������� ��������������� (�� ������ ���� �������). ��� (����) ���������.
 * Based on utility class {@link basos.zkui.BetweenFilters}
 * @param <C> ��� �������� ��������.
 * @param <T> ��� �������� �������� ��������.
 */
// TODO: setMinVal, setMaxVal ��� ������������ ����������� �� ����� ���������� (��������, �������� ������� �������)
public class BetweenFilterMacro<C extends InputElement, T extends Comparable<? super T>> extends HtmlMacroComponent implements Disable {
// �������������� ��������� ����������, ������ ��� ���� � ���������.
	
	private static final long serialVersionUID = 5485542396379205479L;
	
	private static final Logger logger = LoggerFactory.getLogger(BetweenFilterMacro.class);
	
	@Wire
	private Image fltrCtrl; // ������, ����������� ���� �������������
	@Wire
	private Window fltrWin; // ���� �������������
	@Wire("#fltrWin #fltrValFrom")
	private C fltrValFrom; // ���������, �������� ����� ������� ��������� �������� ����������
	@Wire("#fltrWin #fltrValTo")
	private C fltrValTo; // ���������, �������� ������ ������� ��������� �������� ����������
	
	private final T minVal, maxVal; // ��������� �������� ���������
	private T appliedValFrom, appliedValTo; // ���������� (�������� ����� ��� �������� � ��������������) ��������
	private final String minValStr, maxValStr;
	private boolean _disabled = false;
	
// TODO: �������� ����� �������� getValClass(), ������� ��� ��������� �������� getMinVal(), getMaxVal(); � �������� ����� ������ �� PairedValue	(��� �������� � �������������� �������� �������)
	/** ����������� �������� ���������������, ������������ getValue(). ������������� ������ �������� �� ������� � �������������� �������� � ���������.
	 * ������� ��� offline-�������� ��������.
	 * (�� ������ ����� minVal, maxVal (���������� �������� �������� ���������������) � ������������� �� ������������)
	 */
	public class PairedValue implements Serializable {
		private static final long serialVersionUID = -4795772570000095830L;
		private final T valFrom, valTo;
		private PairedValue(T valFrom, T valTo) {
			this.valFrom = valFrom;
			this.valTo = valTo;
		}
		/** @see BetweenFilterMacro#isEmpty() */
		public boolean isEmpty() {
			return (valFrom != null && valTo != null && minVal != null && maxVal != null && valFrom.equals(minVal) && valTo.equals(maxVal));
		}
// TODO: ??? �������� �������� ���� NVL (������ ��� null) ???
		/** @see BetweenFilterMacro#isValBetween(Comparable) BetweenFilterMacro.isValBetween() */
		public boolean isValBetween(T objValToEval) {
			boolean ret_res = false;
			ret_res = (objValToEval != null && valFrom.compareTo(objValToEval) <= 0 && valTo.compareTo(objValToEval) >= 0);
			logger.trace("PairedValue.isValBetween. ID: {}, objValToEval = {}, valFrom = {}, valTo = {}, res = {}", BetweenFilterMacro.this.getId(), objValToEval, valFrom, valTo, ret_res);
			return ret_res;
		}
		/** @see BetweenFilterMacro#getText() */
		public String getText() {
			String retStr = "unknown_type";
			if (valFrom instanceof BigDecimal) {
				retStr = ((BigDecimal)valFrom).toPlainString()+"-"+((BigDecimal)valTo).toPlainString();
			} else if (valFrom instanceof Date) {
				retStr = dateToString((Date)valFrom)+"-"+dateToString((Date)valTo);
			}
			else {
				logger.error("PairedValue.getText.  Unsupported data type: {}", valFrom.getClass().getName());
				throw new IllegalStateException("Unsupported data type in PairedValue.getText(): "+valFrom.getClass().getName()+", this= "+this);
			}
			return retStr;
		}
		/** {@link #getText()} */
		public String toString() {
			return getText();
		}
	} // public class PairedValue
	
	
	/** ��������� �������� � ���� ��������� {@link BetweenFilterMacro.PairedValue} */
	public PairedValue getValue() {
		return this.new PairedValue(this.getValFrom(), this.getValTo());
	}
	
	
	/** � ������������ �������� ��������-������������ minVal, maxVal (��������������� ��������) �� ��������������� ��������� ��������������� (�� ������ ���� �������). ��� (����) ���������. */
	@SuppressWarnings("unchecked")
	public BetweenFilterMacro() {
		compose(); // for the template to be applied, and to wire members automatically
		// �� ���� ����� �� ��������������� ��� �� ��������
		// �������� � ������ �� ������ �������� ��������� ��������� � ��������� ���������
		minValStr = (String)fltrWin.getAttribute("minVal");
		maxValStr = (String)fltrWin.getAttribute("maxVal");
		if ( minValStr == null || maxValStr == null ) {
			logger.error("Error on instantiating BetweenFilterMacro: nullable attribute minVal/maxVal not allowed !  minVal = '{}', maxVal = '{}'", minValStr, maxValStr);
			throw new NullPointerException("Error on instantiating BetweenFilterMacro: nullable attribute minVal/maxVal not allowed !  minVal = '"+minValStr+"', maxVal = '"+maxValStr+"'");
		}
		if (fltrValFrom instanceof Decimalbox) {
			minVal = (T) new BigDecimal(minValStr);
			maxVal = (T) new BigDecimal(maxValStr);
		} // Decimalbox - BigDecimal
		else if (fltrValFrom instanceof Datebox) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			try {
				minVal = (T) sdf.parse(minValStr);
				maxVal = (T) sdf.parse(maxValStr);
			} catch(ParseException e) {
				logger.error("Error parse date in BetweenFilterMacro(). format: "+sdf.toPattern()+", minValStr= "+minValStr+", maxValStr="+maxValStr, e);
				throw new InternalError("Error parse date in BetweenFilterMacro(). format: "+sdf.toPattern()+", minValStr= "+minValStr+", maxValStr="+maxValStr, e);
			}
		} // Datebox - java.util.Date
		else {
			logger.error("BetweenFilterMacro of invalid type: {}. Only Decimalbox & Datebox implemented !", fltrValFrom.getClass().getName());
			throw new InstantiationError("BetweenFilterMacro of invalid type: '"+fltrValFrom.getClass().getName()+"'. Only Decimalbox & Datebox implemented !");
		}
		if ( minVal.compareTo(maxVal) > 0 ) {
			logger.error("Error on instantiating BetweenFilterMacro: attribute minVal > maxVal not allowed !  minVal = '{}', maxVal = '{}'", minValStr, maxValStr);
			throw new InstantiationError("Error on instantiating BetweenFilterMacro: attribute minVal > maxVal not allowed !  minVal = '"+minValStr+"', maxVal = '"+maxValStr+"'");
		}
		onClear();
		logger.trace("BetweenFilterMacro construct. this.toString = '{}', getText = '{}', id = '{}', fltrWin: '{}', fltrValFrom: '{}', fltrValTo: '{}', minVal: {}, maxVal: {}", this.toString(), getText(), getId(), fltrWin, fltrValFrom, fltrValTo, minVal, maxVal);
	} // parametreless constructor BetweenFilterMacro()
	
	
	/** ��������� ������ ��� ������������� (� ��������������) �������� ���������������.
	 * ���������� ������������ ��� ��������� ������� onApply ��������������� (�������� � �������������� ����� "��������� ������" - forward "applIm".onClick).
	 * ����� ��������� ���� ��������������� ������������ ������� ��������, ������� ����� ������������ ��� ����������� �������� � ������� ������ {@link #restore()}.
	 * ����� �������� ������ ������������ �������� � ����������� ����.
	 * @param applyLogic ����� ���������� ������� (nullable). �� ������������.
	 */
	@SuppressWarnings("unchecked")
	public void onApply(/*Consumer<Void> applyLogic*/) {
		// ���������� �������� ����� ����������� ������� ��� ����������� ���������������
		if (fltrValFrom instanceof Decimalbox) {
			appliedValFrom = (T) ((Decimalbox)fltrValFrom).getValue();
			appliedValTo = (T) ((Decimalbox)fltrValTo).getValue();
		} // Decimalbox - BigDecimal
		else if (fltrValFrom instanceof Datebox) {
			appliedValFrom = (T) ((Datebox)fltrValFrom).getValue();
			appliedValTo = (T) ((Datebox)fltrValTo).getValue();
		} // Datebox - java.util.Date
		else {
			logger.error("apply.  Unsupported control type: {}", fltrValFrom.getClass().getName());
			throw new IllegalStateException("Unsupported control type in BetweenFilterMacro.apply(): "+fltrValFrom.getClass().getName()+", this= "+this);
		}
		logger.debug("apply. ID: {}, minVal = {}, maxVal = {}, appliedValFrom = {}, appliedValTo = {}", this.getId(), minVal, maxVal, appliedValFrom, appliedValTo);
		if (appliedValFrom != null && !appliedValFrom.equals(minVal) || appliedValTo != null && !appliedValTo.equals(maxVal)) {
			String clearFiltersIconName = Labels.getLabel("betweenFilters.entryImage.clearIcon", "/img/Data-Clear-Filters-icon_black(24).png");
			fltrCtrl.setSrc(clearFiltersIconName); // ������ ������ ���������� ������� ���������� ����� ���������� ������� ������� ������� �������
		} else {
			String emptyFiltersIconName = Labels.getLabel("betweenFilters.entryImage.emptyIcon", "/img/Data-Empty-Filter-icon(24).png");
			fltrCtrl.setSrc(emptyFiltersIconName); // ������ ������ ���������� ������� ��������� ��� ������� �������
		}
		fltrWin.setVisible(false);
		//if (applyLogic != null ) { applyLogic.accept(null);	}
		//fltrWin.setVisible(false);
// TODO:HOWTO: ��������� �������
//		entryImage.enable();
//		BetweenFilters.toggleOnBetweenFilter(fltrCtrl, fltrValFrom, fltrValTo, applyLogic);
	} // public void apply(Consumer<Object> applyLogic)
	
	/** ������������ �������� ��������� ������� �� ���������� ��� ��������� ���������� ������� ��� �� ��������������� �������� (�.�. ������ �� ������ �������� ����� �������) (������������ � �������). */
	public void restore() {
		if (appliedValFrom != null && appliedValTo != null) {
//			fltrValFrom.setRawValue(appliedValFrom); // setText(minValStr)
//			fltrValTo.setRawValue(appliedValTo); // setText(maxValStr) - �������� ����� - ����������� ��������� !
			if (fltrValFrom instanceof Decimalbox) {
				((Decimalbox)fltrValFrom).setValue((BigDecimal)appliedValFrom);
				((Decimalbox)fltrValTo).setValue((BigDecimal)appliedValTo);
			} // Decimalbox - BigDecimal
			else if (fltrValFrom instanceof Datebox) {
				((Datebox)fltrValFrom).setValue((Date)appliedValFrom);
				((Datebox)fltrValTo).setValue((Date)appliedValTo);
			} // Datebox - java.util.Date
			else {
				logger.error("restore.  Unsupported control type: {}", fltrValFrom.getClass().getName());
				throw new IllegalStateException("Unsupported control type in BetweenFilterMacro.restore(): "+fltrValFrom.getClass().getName()+", this= "+this);
			}
		} else { // ���������� ������� �� ��������������� ��������, �.�. ������ ������ � restoreBetweenFilter() �� �����������
			BetweenFilters.restoreBetweenFilter(fltrValFrom, fltrValTo, false);
		}
		logger.debug("restore. ID: {}, appliedValFrom = {}, = {}", this.getId(), appliedValFrom, appliedValTo);
	} // public void restore()
	
	/** �������������� ��������������� �������� (��� ������ ����� ����������, �������� �� GridDataFilter). */
	public void clear() {
		BetweenFilters.clearBetweenFilter(fltrCtrl, fltrValFrom, fltrValTo);
		appliedValFrom = minVal;
		appliedValTo = maxVal;
		logger.debug("clear. ID: {}", this.getId());
	} // public void clear()
	
	@Listen("onClick = #fltrCtrl")
	public void onShow() {
		fltrWin.setVisible(true);
	}
	
	/** ������ �����: ������������ ���������� �������� ��������� ������� � ������ ����. */
	@Listen("onClick = #fltrWin #cancelIm")
	public void onCancel() {
		restore();
		fltrWin.setVisible(false);
	}
	
	/** ������������ ���������� �������� ��������� ������� (���������� �����). */
	@Listen("onClick = #fltrWin #restoreIm")
	public void onUndo() {
		restore();
	}
	
	/** ���������� ��������������� �������� ��������� ������� ("��������") (���������� �����). */
	@Listen("onClick = #fltrWin #clearIm")
	public void onClear() {
		fltrValFrom.setRawValue(minVal); fltrValTo.setRawValue(maxVal);
		//BetweenFilters.restoreBetweenFilter(fltrValFrom, fltrValTo, true); // ����������� � ������������, �.�. ��������� ��
// TODO: (�������� ����� ����������� ����������) ����������� � ������������, �.�. ��������� �� (���������� BetweenFilters$BetweenValidator ��� setValue())
		/*if (fltrValFrom instanceof Decimalbox) {
			((Decimalbox)fltrValFrom).setValue((BigDecimal)minVal);
			((Decimalbox)fltrValTo).setValue((BigDecimal)maxVal);
		} // Decimalbox - BigDecimal
		else if (fltrValFrom instanceof Datebox) {
			((Datebox)fltrValFrom).setValue((Date)minVal);
			((Datebox)fltrValTo).setValue((Date)maxVal);
		} // Datebox - java.util.Date
		else {
			logger.error("onClear.  Unsupported control type: {}", fltrValFrom.getClass().getName());
			throw new IllegalStateException("Unsupported control type in BetweenFilterMacro.onClear(): "+fltrValFrom.getClass().getName()+", this= "+this);
		}*/
	} // public void onClear()
	
	/** ������� = ��������� ��������� (���������������) ���������. */
	public boolean isEmpty() {
		boolean ret_res = false;
		ret_res = BetweenFilters.isEmptyBetweenFilter(fltrValFrom, fltrValTo);
		logger.trace("isEmpty. ID: {}, res = {}", this.getId(), ret_res);
		return ret_res; // ��������� � ���-� getValue() �������� �������� �����
	} // public boolean isEmpty()
	
	/** ���������� ��������� ������������� ��������� ��������� (���������������) ��������. */
	public String getEmptyValAsString() {
		logger.trace("getEmptyValAsString. ID: {}, res = {}", this.getId(), minValStr+"-"+maxValStr);
		return minValStr+"-"+maxValStr;
	} // public String getEmptyValAsString()
	
// TODO: ??? �������� �������� ���� NVL (������ ��� null) ???
	/** �������� �������� �� �������������� � �������� ��������� �������� ����������.
	 * @param objValToEval ����������� ��������.
	 * @return ���� ��� ��������� null.
	 * @see BetweenFilters#evalBetweenFilter(Comparable, InputElement, InputElement) BetweenFilters.evalBetweenFilter() 
	 */
	public boolean isValBetween(T objValToEval) {
		boolean ret_res = false;
		ret_res = BetweenFilters.evalBetweenFilter(objValToEval, fltrValFrom, fltrValTo); // ���-�� ������ getValue()
		logger.debug("isValBetween. ID: {}, objValToEval = {}, res = {}", this.getId(), objValToEval, ret_res);
		return ret_res;
	} // public boolean isValBetween(T objValToEval)
	
	/** ��������� ����� �������� �������� ���������� (������������ � �������).
	 * @see BetweenFilters#genBetweenValidator()
	 */
	public Constraint genBetweenValidator() {
		return BetweenFilters.genBetweenValidator();
	} // public Constraint genBetweenValidator()
	
// '0.00-99999999999.99' - Ok !
// '1900-11-14-2999-12-31' - Ok !
	/** ���������� ��������� ������������� �������� ��������� ��������.
	 * ��� Datebox: "yyyy1-mm1-dd1-yyyy2-mm2-dd2", ��� Decimalbox: valFrom.toPlainString()+"-"+valTo.toPlainString()
	 */
	public String getText() {
		String retStr = "unknown_type";
		if (fltrValFrom instanceof Decimalbox) {
			retStr = ((BigDecimal)fltrValFrom.getRawValue()).toPlainString()+"-"+((BigDecimal)fltrValTo.getRawValue()).toPlainString(); // fltrValFrom.getRawText()+"-"+fltrValTo.getRawText()
		} else if (fltrValFrom instanceof Datebox) {
			retStr = dateToString((Date)fltrValFrom.getRawValue())+"-"+dateToString((Date)fltrValTo.getRawValue()); // fltrValFrom.getRawText()+"-"+fltrValTo.getRawText()			
		}
		else {
			logger.error("getText.  Unsupported control type: {}", fltrValFrom.getClass().getName());
			throw new IllegalStateException("Unsupported control type in BetweenFilterMacro.getText(): "+fltrValFrom.getClass().getName()+", this= "+this);
		}
		logger.trace("getText.  {}.getText = '{}', getEmptyValAsString = '{}', getExtMacroType: {}, getValClass: {}", this.getId(), retStr, getEmptyValAsString(), getExtMacroType(), getValClass());
		//logger.trace("this.class.getTypeParameters: {}", java.util.Arrays.deepToString(this.getClass().getTypeParameters())); // getTypeParameters:[C, T]
		//logger.trace("this.class.toGenericString: {}", this.getClass().toGenericString()); // toGenericString:public class basos.zkui.BetweenFilterMacro<C,T>
		return retStr;
	} // public String getText()
	
	/** {@link #getText()} */
	//public String toString() { return getText(); }
	
	@SuppressWarnings("unchecked")
	/** ������� �������� ����� �������. */
	public T getValFrom() {
		if (fltrValFrom instanceof Decimalbox) {
			return (T) ((Decimalbox)fltrValFrom).getValue();
		} // Decimalbox - BigDecimal
		else if (fltrValFrom instanceof Datebox) {
			return (T) ((Datebox)fltrValFrom).getValue();
		} // Datebox - java.util.Date
		else {
			logger.error("getValFrom.  Unsupported control type: {}", fltrValFrom.getClass().getName());
			throw new IllegalStateException("Unsupported control type in BetweenFilterMacro.getValFrom(): "+fltrValFrom.getClass().getName()+", this= "+this);
		}
	} // public T getValFrom()
	
	/** ������� �������� ������ �������. */
	@SuppressWarnings("unchecked")
	public T getValTo() {
		if (fltrValTo instanceof Decimalbox) {
			return (T) ((Decimalbox)fltrValTo).getValue();
		} // Decimalbox - BigDecimal
		else if (fltrValTo instanceof Datebox) {
			return (T) ((Datebox)fltrValTo).getValue();
		} // Datebox - java.util.Date
		else {
			logger.error("getValTo.  Unsupported control type: {}", fltrValFrom.getClass().getName());
			throw new IllegalStateException("Unsupported control type in BetweenFilterMacro.getValTo(): "+fltrValTo.getClass().getName()+", this= "+this);
		}
	} // public T getValTo()
	
	/** ������� "��������� �����������" �������� ����� �������.
	 * @see org.zkoss.zul.impl.InputElement#getRawValue()
	 */
	@SuppressWarnings("unchecked")
	public T getRawValFrom() {return (T)fltrValFrom.getRawValue();}
	
	/** ������� "��������� �����������" �������� ������ �������.
	 * @see org.zkoss.zul.impl.InputElement#getRawValue()
	 */
	@SuppressWarnings("unchecked")
	public T getRawValTo() {return (T)fltrValTo.getRawValue();}
	
	/** ��� ���������� ��� �� ���������� ��� �������������� � ZUL (between_filter_date, between_filter_decimal): getDefinition().getName() */
	public String getName() {
		return this.getDefinition().getName(); // "between_filter_date" / "between_filter_decimal"
	}
	
	/** ������ ��������������� � ����������� �� ���� InputElement: "DecimalBetweenFilterMacro" / "DateBetweenFilterMacro" */
	public String getExtMacroType() {
		if (fltrValFrom instanceof Decimalbox) return "DecimalBetweenFilterMacro";
		if (fltrValFrom instanceof Datebox) return "DateBetweenFilterMacro";
		return "unknown_type";
	} // public String getExtMacroType()
	
// FIXME: fltrValFrom.getClass() :))
	/** ����� ��������������� ���� ������: BigDecimal.class / java.util.Date.class */
	@SuppressWarnings("unchecked")
	public Class<T> getValClass() {
		if (fltrValFrom instanceof Decimalbox) return (Class<T>)BigDecimal.class;
		if (fltrValFrom instanceof Datebox) return (Class<T>)Date.class;
		return null;
	} // public Class<T> getValClass() 	
	
	/** ����������� ����� �������� (������� minVal, ��������������� � ��������������� ���� ������) */
	public T getMinVal() { return minVal; }
	
	/** ������������ ������ �������� (������� maxVal, ��������������� � ��������������� ���� ������) */
	public T getMaxVal() { return maxVal; }
	
	/** �������� �������� minVal "��� ���� � �������" */
    public String getMinValStr() { return minValStr; }
    
    /** �������� �������� maxVal "��� ���� � �������" */
	public String getMaxValStr() { return maxValStr; }
		
	/**
     * Formats a date in the date escape format yyyy-mm-dd.
     * <P>
     * @return a String in yyyy-mm-dd format
     */
    @SuppressWarnings("deprecation")
    private String dateToString(Date val) { // copied from java.sql.Date.toString
        int year = val.getYear() + 1900;
        int month = val.getMonth() + 1;
        int day = val.getDate();
        
        char buf[] = "2000-00-00".toCharArray();
        buf[0] = Character.forDigit(year/1000,10);
        buf[1] = Character.forDigit((year/100)%10,10);
        buf[2] = Character.forDigit((year/10)%10,10);
        buf[3] = Character.forDigit(year%10,10);
        buf[5] = Character.forDigit(month/10,10);
        buf[6] = Character.forDigit(month%10,10);
        buf[8] = Character.forDigit(day/10,10);
        buf[9] = Character.forDigit(day%10,10);
        
        return new String(buf);
    } // private String dateToString(Date val)
    
    /** ���������� ���������� org.zkoss.zk.ui.ext.Disable {@inheritDoc} ��� ������������ ��������. */
    @Override
    public boolean isDisabled() {
		return _disabled;
	}
    
    /** ���������� ���������� org.zkoss.zk.ui.ext.Disable - ������������/���������� ����������� �������.
     * @param disabled Disable (true) / enable (false)
     */
    @Override
    public void setDisabled(boolean disabled) {
		if (_disabled != disabled) {
			_disabled = disabled;
			fltrCtrl.setVisible(!disabled); // TODO: ������� ��������
		}
    } // public void setDisabled(boolean disable)
    
} // public class BetweenFilterMacro<C extends InputElement, T extends Comparable<?>> extends HtmlMacroComponent