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


/** Класс макрокомпонента (композит) between_filter_decimal.zul/between_filter_date.zul (диапазонный фильтр денежных значений / дат).
 * Композиция из двух InputElement, задающих диапазон значений. Включает также элемент (кнопку image) управления окном (в режиме "highlighted").
 * К композиту можно обращаться (что делает фильтр GridDataFilter) как к монолитному элементу наравне с представителями прочих типов виджетов.
 * Значения-ограничители minVal, maxVal (поумолчательные значения) читаются в конструкторе из соответствующих атрибутов макрокомпонента (не должны быть пустыми). Они (пока) неизменны.
 * Based on utility class {@link basos.zkui.BetweenFilters}
 * @param <C> Тип базового контрола.
 * @param <T> Тип значения базового контрола.
 */
// TODO: setMinVal, setMaxVal для установления ограничений во время выполнения (например, реальные границы выборки)
public class BetweenFilterMacro<C extends InputElement, T extends Comparable<? super T>> extends HtmlMacroComponent implements Disable {
// Параметризация абсолютно бесполезна, только как инфо в композере.
	
	private static final long serialVersionUID = 5485542396379205479L;
	
	private static final Logger logger = LoggerFactory.getLogger(BetweenFilterMacro.class);
	
	@Wire
	private Image fltrCtrl; // кнопка, открывающая окно макроэлемента
	@Wire
	private Window fltrWin; // окно макроэлемента
	@Wire("#fltrWin #fltrValFrom")
	private C fltrValFrom; // компонент, задающий левую границу интервала значений компонента
	@Wire("#fltrWin #fltrValTo")
	private C fltrValTo; // компонент, задающий правую границу интервала значений компонента
	
	private final T minVal, maxVal; // граничные значения интервала
	private T appliedValFrom, appliedValTo; // применённые (отданные вовне при закрытии с подтверждением) значения
	private final String minValStr, maxValStr;
	private boolean _disabled = false;
	
// TODO: добавить класс значения getValClass(), геттеры для отдельных значений getMinVal(), getMaxVal(); в основной класс сеттер из PairedValue	(для хранения и восстановления настроек фильтра)
	/** Композитное значение макрокомпонента, возвращаемое getValue(). Инкапсулирует логику проверки на пустоту и принадлежность величины к диапазону.
	 * Полезен для offline-проверки значения.
	 * (не храним здесь minVal, maxVal (допустумый диапазон значений макрокомпонента) в предположении их неизменности)
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
// TODO: ??? добавить параметр типа NVL (замена для null) ???
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
	
	
	/** Считываем значение в виде композита {@link BetweenFilterMacro.PairedValue} */
	public PairedValue getValue() {
		return this.new PairedValue(this.getValFrom(), this.getValTo());
	}
	
	
	/** В конструкторе читаются значения-ограничители minVal, maxVal (поумолчательные значения) из соответствующих атрибутов макрокомпонента (не должны быть пустыми). Они (пока) неизменны. */
	@SuppressWarnings("unchecked")
	public BetweenFilterMacro() {
		compose(); // for the template to be applied, and to wire members automatically
		// на этом этапе ИД макрокомпоненту ещё не присвоен
		// получаем и парсим из текста значения кастомных атрибутов с границами диапазона
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
	
	
	/** Применить фильтр при положительном (с подтверждением) закрытии макрокомпонента.
	 * Вызывается контроллером при перехвате события onApply макрокомпонента (закрытие с подтверждением ввода "Применить фильтр" - forward "applIm".onClick).
	 * Перед закрытием окна макрокомпонента запоминаются текущие значения, которые можно восстановить при последующем открытии с помощью вызова {@link #restore()}.
	 * Также меняется иконка управляющего контрола и закрывается окно.
	 * @param applyLogic Вызов применения фильтра (nullable). Не используется.
	 */
	@SuppressWarnings("unchecked")
	public void onApply(/*Consumer<Void> applyLogic*/) {
		// запоминаем значения перед применением фильтра для возможности восстановаления
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
			fltrCtrl.setSrc(clearFiltersIconName); // кнопка вызова модального диалога становится после применения фильтра кнопкой быстрой очистки
		} else {
			String emptyFiltersIconName = Labels.getLabel("betweenFilters.entryImage.emptyIcon", "/img/Data-Empty-Filter-icon(24).png");
			fltrCtrl.setSrc(emptyFiltersIconName); // кнопка вызова модального диалога принимает вид пустого фильтра
		}
		fltrWin.setVisible(false);
		//if (applyLogic != null ) { applyLogic.accept(null);	}
		//fltrWin.setVisible(false);
// TODO:HOWTO: разрешить нажатие
//		entryImage.enable();
//		BetweenFilters.toggleOnBetweenFilter(fltrCtrl, fltrValFrom, fltrValTo, applyLogic);
	} // public void apply(Consumer<Object> applyLogic)
	
	/** Восстановить значения компонент фильтра из сохранённых при последнем применении фильтра или из поумолчательных значений (т.е. бывших на момент открытия формы фильтра) (используется в шаблоне). */
	public void restore() {
		if (appliedValFrom != null && appliedValTo != null) {
//			fltrValFrom.setRawValue(appliedValFrom); // setText(minValStr)
//			fltrValTo.setRawValue(appliedValTo); // setText(maxValStr) - теряется точка - срабатывает валидатор !
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
		} else { // фактически очистка на поумолчательные значения, т.к. другой логики в restoreBetweenFilter() не реализовано
			BetweenFilters.restoreBetweenFilter(fltrValFrom, fltrValTo, false);
		}
		logger.debug("restore. ID: {}, appliedValFrom = {}, = {}", this.getId(), appliedValFrom, appliedValTo);
	} // public void restore()
	
	/** Восстановление поумолчательных значений (для вызова извне компонента, например из GridDataFilter). */
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
	
	/** Отмена ввода: восстановить сохранённые значения компонент фильтра и скрыть окно. */
	@Listen("onClick = #fltrWin #cancelIm")
	public void onCancel() {
		restore();
		fltrWin.setVisible(false);
	}
	
	/** Восстановить сохранённые значения компонент фильтра (внутренний вызов). */
	@Listen("onClick = #fltrWin #restoreIm")
	public void onUndo() {
		restore();
	}
	
	/** Установить поумолчательные значения компонент фильтра ("очистить") (внутренний вызов). */
	@Listen("onClick = #fltrWin #clearIm")
	public void onClear() {
		fltrValFrom.setRawValue(minVal); fltrValTo.setRawValue(maxVal);
		//BetweenFilters.restoreBetweenFilter(fltrValFrom, fltrValTo, true); // неприменимо в конструкторе, т.к. требуется ИД
// TODO: (включить после исправления валидатора) неприменимо в конструкторе, т.к. требуется ИД (валидатору BetweenFilters$BetweenValidator при setValue())
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
	
	/** Пустота = равенство граничным (поумолчательным) значениям. */
	public boolean isEmpty() {
		boolean ret_res = false;
		ret_res = BetweenFilters.isEmptyBetweenFilter(fltrValFrom, fltrValTo);
		logger.trace("isEmpty. ID: {}, res = {}", this.getId(), ret_res);
		return ret_res; // сравнение с исп-м getValue() значений реальных типов
	} // public boolean isEmpty()
	
	/** Возвращает строковое представление диапазона граничных (поумолчательных) значений. */
	public String getEmptyValAsString() {
		logger.trace("getEmptyValAsString. ID: {}, res = {}", this.getId(), minValStr+"-"+maxValStr);
		return minValStr+"-"+maxValStr;
	} // public String getEmptyValAsString()
	
// TODO: ??? добавить параметр типа NVL (замена для null) ???
	/** Проверка величины на принадлежность к текущему диапазону значений компонента.
	 * @param objValToEval Оцениваемая величина.
	 * @return Ложь для параметра null.
	 * @see BetweenFilters#evalBetweenFilter(Comparable, InputElement, InputElement) BetweenFilters.evalBetweenFilter() 
	 */
	public boolean isValBetween(T objValToEval) {
		boolean ret_res = false;
		ret_res = BetweenFilters.evalBetweenFilter(objValToEval, fltrValFrom, fltrValTo); // исп-ся вызовы getValue()
		logger.debug("isValBetween. ID: {}, objValToEval = {}, res = {}", this.getId(), objValToEval, ret_res);
		return ret_res;
	} // public boolean isValBetween(T objValToEval)
	
	/** Возвращет набор проверок значений компонента (используется в шаблоне).
	 * @see BetweenFilters#genBetweenValidator()
	 */
	public Constraint genBetweenValidator() {
		return BetweenFilters.genBetweenValidator();
	} // public Constraint genBetweenValidator()
	
// '0.00-99999999999.99' - Ok !
// '1900-11-14-2999-12-31' - Ok !
	/** Возвращает текстовое представление текущего диапазона значений.
	 * Для Datebox: "yyyy1-mm1-dd1-yyyy2-mm2-dd2", для Decimalbox: valFrom.toPlainString()+"-"+valTo.toPlainString()
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
	/** Текущее значение левой границы. */
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
	
	/** Текущее значение правой границы. */
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
	
	/** Текущее "последнее проверенное" значение левой границы.
	 * @see org.zkoss.zul.impl.InputElement#getRawValue()
	 */
	@SuppressWarnings("unchecked")
	public T getRawValFrom() {return (T)fltrValFrom.getRawValue();}
	
	/** Текущее "последнее проверенное" значение правой границы.
	 * @see org.zkoss.zul.impl.InputElement#getRawValue()
	 */
	@SuppressWarnings("unchecked")
	public T getRawValTo() {return (T)fltrValTo.getRawValue();}
	
	/** Тип компонента как он поименован при декларировании в ZUL (between_filter_date, between_filter_decimal): getDefinition().getName() */
	public String getName() {
		return this.getDefinition().getName(); // "between_filter_date" / "between_filter_decimal"
	}
	
	/** Подтип макрокомпонента в зависимости от типа InputElement: "DecimalBetweenFilterMacro" / "DateBetweenFilterMacro" */
	public String getExtMacroType() {
		if (fltrValFrom instanceof Decimalbox) return "DecimalBetweenFilterMacro";
		if (fltrValFrom instanceof Datebox) return "DateBetweenFilterMacro";
		return "unknown_type";
	} // public String getExtMacroType()
	
// FIXME: fltrValFrom.getClass() :))
	/** Класс контролируемого типа данных: BigDecimal.class / java.util.Date.class */
	@SuppressWarnings("unchecked")
	public Class<T> getValClass() {
		if (fltrValFrom instanceof Decimalbox) return (Class<T>)BigDecimal.class;
		if (fltrValFrom instanceof Datebox) return (Class<T>)Date.class;
		return null;
	} // public Class<T> getValClass() 	
	
	/** Минимальное левое значение (атрибут minVal, преобразованный к контролируемому типу данных) */
	public T getMinVal() { return minVal; }
	
	/** Максимальное правое значение (атрибут maxVal, преобразованный к контролируемому типу данных) */
	public T getMaxVal() { return maxVal; }
	
	/** Значение атрибута minVal "как есть в шаблоне" */
    public String getMinValStr() { return minValStr; }
    
    /** Значение атрибута maxVal "как есть в шаблоне" */
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
    
    /** Реализация интерфейса org.zkoss.zk.ui.ext.Disable {@inheritDoc} для управляющего контрола. */
    @Override
    public boolean isDisabled() {
		return _disabled;
	}
    
    /** Реализация интерфейса org.zkoss.zk.ui.ext.Disable - деактивируем/активируем управляющий контрол.
     * @param disabled Disable (true) / enable (false)
     */
    @Override
    public void setDisabled(boolean disabled) {
		if (_disabled != disabled) {
			_disabled = disabled;
			fltrCtrl.setVisible(!disabled); // TODO: сделать красивше
		}
    } // public void setDisabled(boolean disable)
    
} // public class BetweenFilterMacro<C extends InputElement, T extends Comparable<?>> extends HtmlMacroComponent