package basos.zkui;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Path;
import org.zkoss.zk.ui.WrongValueException;
import org.zkoss.zul.Constraint;
import org.zkoss.zul.Datebox;
import org.zkoss.zul.Decimalbox;
import org.zkoss.zul.Image;
import org.zkoss.zul.impl.InputElement;


/** Utility class for BetweenFilterMacro working at solo-component level (not composite). */
public class BetweenFilters {

	private static final Logger logger = LoggerFactory.getLogger(BetweenFilters.class);

	private BetweenFilters() {}
	
/** Универсальный валидатор для фильтра Between
 * Для BigDecimal (денежный тип):
 *  не пусто;
 *  точность = 2 зн. после запятой;
 *  дипазон значенией [minVal, maxVal]
 *  valFrom <= valTo
 *  
 * Для java.util.Date:
 *  не пусто;
 *  дипазон значенией [minVal, maxVal]
 *  valFrom <= valTo
 *  (валидность формата даты проверяется автоматически: datebox lenient="false")
 */
	private static class BetweenValidator implements Constraint {
		@Override
		public void validate(Component comp, Object valStr) throws WrongValueException {
// TODO: Path требует ИД (в конструкторе не присвоен макрокомпоненту), реализовать через селекторы (comp.query(), see DR p. 29)
			String compPath = new Path(comp).getPath();
			String containerPath = compPath.substring(0, compPath.lastIndexOf('/'))/*compPath.replace("ValFrom", "FltrWin").replace("ValTo", "FltrWin")*/; // окно - носитель скоупа
			Component container = Path.getComponent(containerPath);
			logger.trace("BetweenValidator.validate.  containerPath = {}, container = {}", containerPath, container);
			String minValStr = (String)container.getAttribute("minVal")
				  ,maxValStr = (String)container.getAttribute("maxVal");
			logger.debug("BetweenValidator.validate.  comp: {} of type {}, valStr = {} of type {}, containerPath = {}", comp.getId(), comp.getClass().getName(), valStr, (valStr == null ? "" : valStr.getClass().getName()), containerPath);
			if (valStr == null) {
				throw new WrongValueException("Please enter nonempty value");
			}
			if (minValStr == null || maxValStr == null) {
				logger.error("BetweenValidator.validate. minVal (={}) & maxVal (={}) attributes shouldn't be null for control {} of type {}", minValStr, maxValStr, comp, comp.getClass().getName());
				throw new InternalError("BetweenValidator.validate. minVal (="+minValStr+") & maxVal (="+maxValStr+") attributes shouldn't be null for control "+comp+" of type "+comp.getClass().getName());
			}
			if (comp instanceof Decimalbox) { // "org.zkoss.zul.Decimalbox".equals(comp.getClass().getName())
				BigDecimal minVal = new BigDecimal(minValStr)
						  ,maxVal = new BigDecimal(maxValStr)
						  ,val = (BigDecimal)valStr
						  ,valPaired;
				if (comp.getId() != null && comp.getId().endsWith("ValFrom")) {
					String compPairedPath = compPath.replace("ValFrom", "ValTo");
					Decimalbox compPaired = ((Decimalbox)Path.getComponent(compPairedPath));
					valPaired = compPaired == null ? null : ((BigDecimal)compPaired.getRawValue()); // raw ???
					logger.trace(" Decimalbox/BigDecimal ValFrom:  val_scale = {}, valPaired(ValTo) = {}, compPairedPath = {}", val.scale(), valPaired, compPairedPath);
					if (val.compareTo(minVal) < 0) {
						throw new WrongValueException("ValueFrom ("+val+") must be GE than "+minVal);
					} else if (val.compareTo(maxVal) > 0) {
						throw new WrongValueException("ValueFrom ("+val+") must be LE than "+maxVal);
					} else if (val.scale() > 2) {
						throw new WrongValueException("Maximum 2 digits after point enabled");
					} else if (valPaired != null && val.compareTo(valPaired) > 0) {
						throw new WrongValueException("ValueFrom ("+val+") must be LE than "+valPaired);
					}
				} else if (comp.getId() != null && comp.getId().endsWith("ValTo")) {
					String compPairedPath = compPath.replace("ValTo", "ValFrom");
					Decimalbox compPaired = ((Decimalbox)Path.getComponent(compPairedPath));
					valPaired = compPaired == null ? null : ((BigDecimal)compPaired.getRawValue()); // raw ???
					logger.trace(" Decimalbox/BigDecimal Decimalbox/BigDecimal:  val_scale = {}, valPaired(ValFrom) = {}, compPairedPath = {}", val.scale(), valPaired, compPairedPath);
					if (val.compareTo(minVal) < 0) {
						throw new WrongValueException("ValueTo ("+val+") must be GE than "+minVal);
					} else if (val.compareTo(maxVal) > 0) {
						throw new WrongValueException("ValueTo ("+val+") must be LE than "+maxVal);
					} else if (val.scale() > 2) {
						throw new WrongValueException("Maximum 2 digits after point enabled");
					} else if (valPaired != null && val.compareTo(valPaired) < 0) {
						throw new WrongValueException("ValueTo ("+val+") must be GE than "+valPaired);
					}					
				} // ValFrom / ValTo
				else {
					logger.error("BetweenValidator.validate.  Wrong name (ID) for BetweenFilter control {} of type {} is {}. Must ends with 'ValFrom' OR 'ValTo' !", comp, comp.getClass().getName(), comp.getId());
					throw new IllegalArgumentException("BetweenValidator.validate.  Wrong name (ID) for BetweenFilter control "+comp+" of type "+comp.getClass().getName()+" is "+comp.getId()+". Must ends with 'ValFrom' OR 'ValTo' !");
				}
			} // java.math.BigDecimal
			else if (comp instanceof Datebox) { // "org.zkoss.zul.Datebox".equals(comp.getClass().getName())
				Date minVal
					,maxVal
					,val = (Date)valStr
					,valPaired;
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
				try {
					minVal = sdf.parse(minValStr);
					maxVal = sdf.parse(maxValStr);
				} catch(ParseException e) {
					logger.error("Error parse date in BetweenValidator.validate. format: "+sdf.toPattern()+", minValStr= "+minValStr+", maxValStr="+maxValStr, e);
					throw new InternalError("Error parse date in BetweenValidator.validate. format: "+sdf.toPattern()+", minValStr= "+minValStr+", maxValStr="+maxValStr, e);
				}
				if (comp.getId() != null && comp.getId().endsWith("ValFrom")) {
					String compPairedPath = compPath.replace("ValFrom", "ValTo");
					Datebox compPaired = ((Datebox)Path.getComponent(compPairedPath));
					valPaired = compPaired == null ? null : ((Date)compPaired.getRawValue());
					logger.trace(" Datebox/java.util.Date ValFrom:  valPaired(ValTo) = {}, compPairedPath = {}", valPaired, compPairedPath);
					if (val.compareTo(minVal) < 0) {
						throw new WrongValueException("ValueFrom ("+val+") must be GE than "+minVal);
					} else if (val.compareTo(maxVal) > 0) {
						throw new WrongValueException("ValueFrom ("+val+") must be LE than "+maxVal);
					} else if (valPaired != null && val.compareTo(valPaired) > 0) {
						throw new WrongValueException("ValueFrom ("+val+") must be LE than "+valPaired);
					}
				} // ValFrom
				else if (comp.getId() != null && comp.getId().endsWith("ValTo")) {
					String compPairedPath = compPath.replace("ValTo", "ValFrom");
					Datebox compPaired = ((Datebox)Path.getComponent(compPairedPath));
					valPaired = compPaired == null ? null : ((Date)compPaired.getRawValue());
					logger.trace(" Datebox/java.util.Date ValTo:  valPaired(ValFrom) = {}, compPairedPath = {}", valPaired, compPairedPath);
					if (val.compareTo(minVal) < 0) {
						throw new WrongValueException("ValueFrom ("+val+") must be GE than "+minVal);
					} else if (val.compareTo(maxVal) > 0) {
						throw new WrongValueException("ValueFrom ("+val+") must be LE than "+maxVal);
					} else if (valPaired != null && val.compareTo(valPaired) < 0) {
						throw new WrongValueException("ValueFrom ("+val+") must be GE than "+valPaired);
					}					
				} // ValFrom / ValTo
				else {
					logger.error("BetweenValidator.validate.  Wrong name (ID) for BetweenFilter control {} of type {} is {}. Must ends with 'ValFrom' OR 'ValTo' !", comp, comp.getClass().getName(), comp.getId());
					throw new IllegalArgumentException("BetweenValidator.validate.  Wrong name (ID) for BetweenFilter control "+comp+" of type "+comp.getClass().getName()+" is "+comp.getId()+". Must ends with 'ValFrom' OR 'ValTo' !");
				}
			} // java.util.Date
			else {
				logger.error("BetweenValidator.validate.  Unsupported control type: {}, comp: {}", comp.getClass().getName(), comp);
				throw new IllegalArgumentException("Unsupported control type in BetweenFilterMacro.getValTo(): "+comp.getClass().getName()+", comp= "+comp);
			}
		} // public void validate(Component comp, Object val)
	} // private static class BetweenValidator implements Constraint 

	
// TODO: !! маска (тчк) !!
/** Применить фильтр типа between.
 * Окно должно содержать атрибуты  "minVal" и "maxVal" - значения по умолчанию, они же мин. и макс. возможные значения.
 * Даты в формате "yyyy-MM-dd".
 * Компонент окна определяется как корень (носитель скоупа) в пути к valFromComp.
 * Тип данных определяется по типу компонента valFromComp.
 * Реализовано для: Decimalbox-BigDecimal, Datebox-java.util.Date.
 * Кнопка Image меняет изображение на очистку заполненного фильтра если значения отличаются от поумолчательных, иначе изображает пустой фильтр.
 * Фильтр применяется со считанными значениями компонент valFromComp и valToComp
 *  (логика применения к данным передаётся через реализацию функционального интерейса Consumer);
 * Окно скрывается.
 * @param entryImage Управляющий видимостью окна компонент (Image).
 * @param valFromComp Компонент содержащий нижнюю границу (суффикс "ValFrom").
 * @param valToComp Компонент содержащий верхнюю границу (суффикс "ValTo").
 * @param applyLogic Логика применения фильтра, выполняется в конце перед сокрытием окна (optional).
 */
	public static void toggleOnBetweenFilter(Image entryImage, Component valFromComp, Component valToComp, Consumer<Object> applyLogic) {
// TODO:HOWTO: разрешить нажатие
//		entryImage.enable();
		logger.debug("toggleOnBetweenFilter entry.  entryImage: "+entryImage+", valFromComp: "+valFromComp+", valToComp: "+valToComp);
		String compPath = new Path(valFromComp).getPath();
		String containerPath = compPath.substring(0, compPath.lastIndexOf('/'))/*compPath.replace("ValFrom", "FltrWin").replace("ValTo", "FltrWin")*/; // окно - носитель скоупа
		Component container = Path.getComponent(containerPath);
		String minValStr = (String)container.getAttribute("minVal")
			  ,maxValStr = (String)container.getAttribute("maxVal");
		Object minVal = null, maxVal = null, valFrom = null, valTo = null;
/*
		Path pth = new Path(sour);
		Decimalbox rest_msfo_usdFltrValFrom = (Decimalbox)Path.getComponent("/rest_msfo_usdFltrWin/rest_msfo_usdFltrValFrom")
		          ,rest_msfo_usdFltrValTo = (Decimalbox)Path.getComponent("/rest_msfo_usdFltrWin/rest_msfo_usdFltrValTo");		
		BigDecimal minVal = new BigDecimal( (String)rest_msfo_usdFltrWin.getAttribute("minVal") )
				  ,maxVal = new BigDecimal( (String)rest_msfo_usdFltrWin.getAttribute("maxVal") )
				  ,valFrom = rest_msfo_usdFltrValFrom.getValue()
				  ,valTo = rest_msfo_usdFltrValTo.getValue();
logger.debug("toggleOnBetweenFilter: sour:"+sour.getId()+" "+sour.getClass().getName()+", par:"+sour.getParent().getId()+" "+sour.getParent().getClass().getName()+", grandpar:"+sour.getParent().getParent().getId()+" "+sour.getParent().getParent().getClass().getName()+", root:"+sour.getRoot().getId()+" "+sour.getRoot().getClass().getName()+", spaceOwner="+sour.getSpaceOwner()+", path='"+pth.getPath()+"'");
*/
		
		if (logger.isTraceEnabled())
			logger.trace("toggleOnBetweenFilter: entryImage:"+entryImage.getId()+" "+entryImage.getClass().getName()
				+", valToComp:"+valToComp.getId()+" "+valToComp.getClass().getName()
				+", valFromComp:"+valFromComp.getId()+" "+valFromComp.getClass().getName()
				+", par:"+valFromComp.getParent().getId()+" "+valFromComp.getParent().getClass().getName()
				+", grandpar:"+valFromComp.getParent().getParent().getId()+" "+valFromComp.getParent().getParent().getClass().getName()
				+", great-grandpar:"+valFromComp.getParent().getParent().getParent().getId()+" "+valFromComp.getParent().getParent().getParent().getClass().getName()
				+", root:"+valFromComp.getRoot().getId()+" "+valFromComp.getRoot().getClass().getName()
				+", spaceOwner="+valFromComp.getSpaceOwner()
				+", compPath="+compPath
				+", containerPath="+containerPath
				+", container:"+container)
			;
		if (valFromComp instanceof Decimalbox) {
			minVal = new BigDecimal(minValStr);
			maxVal = new BigDecimal(maxValStr);
			valFrom = ((Decimalbox)valFromComp).getValue();
			valTo = ((Decimalbox)valToComp).getValue();
		} // Decimalbox - BigDecimal
		else if (valFromComp instanceof Datebox) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			try {
				minVal = sdf.parse(minValStr);
				maxVal = sdf.parse(maxValStr);
			} catch(ParseException e) {
				logger.error("Error parse date in toggleOnBetweenFilter. format: "+sdf.toPattern()+", minValStr= "+minValStr+", maxValStr="+maxValStr, e);
				throw new InternalError("Error parse date in toggleOnBetweenFilter. format: "+sdf.toPattern()+", minValStr= "+minValStr+", maxValStr="+maxValStr, e);
			}
			valFrom = ((Datebox)valFromComp).getValue();
			valTo = ((Datebox)valToComp).getValue();
		} // Datebox - java.util.Date
		else {
			logger.error("BetweenValidator.toggleOnBetweenFilter.  Unsupported control type: {}, valFromComp: {}", valFromComp.getClass().getName(), valFromComp);
			throw new IllegalArgumentException("Unsupported control type in BetweenFilterMacro.toggleOnBetweenFilter(): "+valFromComp.getClass().getName()+", valFromComp= "+valFromComp);
		}

		logger.debug("toggleOnBetweenFilter. minVal = {}, maxVal = {}, valFrom = {}, valTo = {}", minVal, maxVal, valFrom, valTo);
		if (valFrom != null && !valFrom.equals(minVal) || valTo != null && !valTo.equals(maxVal)) {
			String clearFiltersIconName = Labels.getLabel("betweenFilters.entryImage.clearIcon", "/img/Data-Clear-Filters-icon_black(24).png");
			entryImage.setSrc(clearFiltersIconName); // кнопка вызова модального диалога становится после применения фильтра кнопкой быстрой очистки
		} else {
			String emptyFiltersIconName = Labels.getLabel("betweenFilters.entryImage.emptyIcon", "/img/Data-Empty-Filter-icon(24).png");
			entryImage.setSrc(emptyFiltersIconName); // кнопка вызова модального диалога принимает вид пустого фильтра
		}
// передаём как лямбда-параметр, чтобы метод был static
//		if (gridDM.applyFilter(readFilter()) >= 0) refreshAfterDataChange(); // применить весь фильтр
		if (applyLogic != null) {
			applyLogic.accept(null);
		}
		container.setVisible(false);
// TODO:HOWTO: разрешить нажатие
//		entryImage.enable();
	} // public static void toggleOnBetweenFilter(Component sour)

	/** Восстановить значения компонент фильтра из применённого фильтра или из поумолчательных значений
	 * (т.е. бывших на момент открытия формы фильтра).
	 * @param valFromComp Компонент содержащий нижнюю границу (суффикс "ValFrom").
	 * @param valToComp Компонент содержащий верхнюю границу (суффикс "ValTo").
	 * @param clear Очищать фильтр (сейчас всегда только очищается).
	 */
	public static void restoreBetweenFilter(InputElement valFromComp, InputElement valToComp, boolean clear) {
		String compPath = new Path(valFromComp).getPath();
		String containerPath = compPath.substring(0, compPath.lastIndexOf('/'))/*compPath.replace("ValFrom", "FltrWin").replace("ValTo", "FltrWin")*/; // окно - носитель скоупа
		Component container = Path.getComponent(containerPath);
		String minValStr = (String)container.getAttribute("minVal")
			  ,maxValStr = (String)container.getAttribute("maxVal");
		Object minVal = null, maxVal = null;
		if (!clear) {
// TODO: читать применённые значения из dataFilter
//			return;
		}
// если затребована очистка или компонент не участвует в фильтре
		if (valFromComp instanceof Decimalbox) {
			minVal = new BigDecimal(minValStr);
			maxVal = new BigDecimal(maxValStr);
			((Decimalbox)valFromComp).setValue((BigDecimal)minVal);
			((Decimalbox)valToComp).setValue((BigDecimal)maxVal);
		} else if (valFromComp instanceof Datebox) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			try {
				minVal = sdf.parse(minValStr);
				maxVal = sdf.parse(maxValStr);
			} catch (ParseException e) {
				logger.error("Error parse date in restoreBetweenFilter. format: "+sdf.toPattern()+", minValStr= "+minValStr+", maxValStr="+maxValStr, e);
				throw new InternalError("Error parse date in restoreBetweenFilter. format: "+sdf.toPattern()+", minValStr= "+minValStr+", maxValStr="+maxValStr, e);
			}
			((Datebox)valFromComp).setValue((Date)minVal);
			((Datebox)valToComp).setValue((Date)maxVal);
		}
		else {
			logger.error("BetweenValidator.restoreBetweenFilter.  Unsupported control type: {}, valFromComp: {}", valFromComp.getClass().getName(), valFromComp);
			throw new IllegalArgumentException("Unsupported control type in BetweenFilterMacro.restoreBetweenFilter(): "+valFromComp.getClass().getName()+", valFromComp= "+valFromComp);
		}
		
		logger.debug("restoreBetweenFilter.  valFromComp: {} of type {}, valToComp: {} of type {}, minValStr = {}, minVal = {}, maxValStr = {}, maxVal = {}", valFromComp.getId(), valFromComp.getClass().getName(), valToComp.getId(), valToComp.getClass().getName(), minValStr, minVal, maxValStr, maxVal);
//		valFromComp.setRawValue(minVal); // setText(minValStr)
//		valToComp.setRawValue(maxVal); // setText(maxValStr) - теряется точка - срабатывает валидатор !
	} // public static void restoreBetweenFilter(InputElement valFromComp, InputElement valToComp, boolean clear)

	/** Очистка - восстановление поумолчательных значений (для вызова извне макрокомпонента, например, из GridDataFilter).
	 * Предполагается, что вызывающий метод применяет после фильтр, потому меняем иконку управляющей кнопки на "чистый фильтр".
	 * @param entryImage Управляющий видимостью окна компонент (Image).
	 * @param valFromComp Компонент содержащий нижнюю границу (суффикс "ValFrom").
	 * @param valToComp Компонент содержащий верхнюю границу (суффикс "ValTo").
	 * @see #restoreBetweenFilter(InputElement, InputElement, boolean)
	 */
	public static void clearBetweenFilter(Image entryImage, InputElement valFromComp, InputElement valToComp) {
		restoreBetweenFilter(valFromComp, valToComp, true);
		String emptyFiltersIconName = Labels.getLabel("betweenFilters.entryImage.emptyIcon", "/img/Data-Empty-Filter-icon(24).png");
		entryImage.setSrc(emptyFiltersIconName); // кнопка вызова модального диалога принимает вид пустого фильтра
	} // public static void clearBetweenFilter(InputElement valFromComp, InputElement valToComp)

	/** Проверка на пустоту (поумолчательность) значений (для вызова из GridDataFilter).
	 * @param valFromComp Компонент содержащий нижнюю границу (суффикс "ValFrom").
	 * @param valToComp Компонент содержащий верхнюю границу (суффикс "ValTo").
	 */
	public static boolean isEmptyBetweenFilter(InputElement valFromComp, InputElement valToComp) {
		String compPath = new Path(valFromComp).getPath();
		String containerPath = compPath.substring(0, compPath.lastIndexOf('/'))/*compPath.replace("ValFrom", "FltrWin").replace("ValTo", "FltrWin")*/; // окно - носитель скоупа
		Component container = Path.getComponent(containerPath);
		String minValStr = (String)container.getAttribute("minVal")
			  ,maxValStr = (String)container.getAttribute("maxVal");
		Object minVal = null, maxVal = null, valFrom = null, valTo = null;
		if (valFromComp instanceof Decimalbox) {
			minVal = new BigDecimal(minValStr);
			maxVal = new BigDecimal(maxValStr);
			valFrom = ((Decimalbox)valFromComp).getValue();
			valTo = ((Decimalbox)valToComp).getValue();
			logger.debug("isEmptyBetweenFilter Decimalbox:  valFromComp: {} of type {}, valToComp: {} of type {}, minValStr = {}, maxValStr = {}, minVal = {}, maxVal = {}, valFrom = {}, valTo = {}", valFromComp.getId(), valFromComp.getClass().getName(), valToComp.getId(), valToComp.getClass().getName(), minValStr, maxValStr, minVal, maxVal, valFrom, valTo);
		} else if (valFromComp instanceof Datebox) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			try {
				minVal = sdf.parse(minValStr);
				maxVal = sdf.parse(maxValStr);
			} catch (ParseException e) {
				logger.error("Error parse date in isEmptyBetweenFilter. format: "+sdf.toPattern()+", minValStr= "+minValStr+", maxValStr="+maxValStr, e);
				throw new InternalError("Error parse date in isEmptyBetweenFilter. format: "+sdf.toPattern()+", minValStr= "+minValStr+", maxValStr="+maxValStr, e);
			}
			valFrom = ((Datebox)valFromComp).getValue();
			valTo = ((Datebox)valToComp).getValue();
			logger.debug("isEmptyBetweenFilter Datebox:  valFromComp: {} of type {}, valToComp: {} of type {}, minValStr = {}, maxValStr = {}, minVal = {}, maxVal = {}, valFrom = {}, valTo = {}", valFromComp.getId(), valFromComp.getClass().getName(), valToComp.getId(), valToComp.getClass().getName(), minValStr, maxValStr, minVal, maxVal, valFrom, valTo);
		} // Decimalbox / Datebox
		else {
			logger.error("BetweenValidator.isEmptyBetweenFilter.  Unsupported control type: {}, valFromComp: {}", valFromComp.getClass().getName(), valFromComp);
			throw new IllegalArgumentException("Unsupported control type in BetweenFilterMacro.isEmptyBetweenFilter(): "+valFromComp.getClass().getName()+", valFromComp= "+valFromComp);
		}

		if (valFrom != null && valTo != null && minVal != null && maxVal != null && valFrom.equals(minVal) && valTo.equals(maxVal)) {
			return true;
		}
		return false;
	} // public static boolean isEmptyBetweenFilter(InputElement valFromComp, InputElement valToComp)

// TODO: ??? добавить параметр типа NVL (замена для null) ???
	/** Оценка значения на прохождение фильтра (для вызова из GridDataFilterableModelMan.applyFilter()).
	 * @param objValToEval Значение, которое нужно оценить против фильтра.
	 * @param valFromComp Компонент содержащий нижнюю границу (суффикс "ValFrom").
	 * @param valToComp Компонент содержащий верхнюю границу (суффикс "ValTo").
	 * False for null.
	 */
	public static boolean evalBetweenFilter(Comparable<?> objValToEval, InputElement valFromComp, InputElement valToComp) {
		boolean retRes = false;
//		Comparable<?> valFrom = null, valTo = null, valToEval = null;
		if (valFromComp instanceof Decimalbox) {
			BigDecimal valFrom = null, valTo = null, valToEval = null;
			valFrom = ((Decimalbox)valFromComp).getValue();
			valTo = ((Decimalbox)valToComp).getValue();
			valToEval = (BigDecimal)objValToEval;
			if (valFrom != null && valTo != null && valToEval != null && valFrom.compareTo(valToEval) <= 0 && valTo.compareTo(valToEval) >= 0) {
				retRes = true;
			}
			logger.debug("evalBetweenFilter Decimalbox/BigDecimal:  valFromComp: {} of type {}, valToEval = {}, valFrom = {}, valTo = {}, return {}", valFromComp.getId(), valFromComp.getClass().getName(), valToEval, valFrom, valTo, retRes);
		} else if (valFromComp instanceof Datebox) {
			Date valFrom = null, valTo = null, valToEval = null;
			valFrom = ((Datebox)valFromComp).getValue();
			valTo = ((Datebox)valToComp).getValue();
			valToEval = (Date)objValToEval; // здесь реально будет java.sql.Date (потомок java.util.Date)
			if (valFrom != null && valTo != null && valToEval != null && valFrom.compareTo(valToEval) <= 0 && valTo.compareTo(valToEval) >= 0) {
				retRes = true;
			}
			logger.debug("evalBetweenFilter Datebox/java.util.Date:  valFromComp: {} of type {}, valToEval = {}, valFrom = {}, valTo = {}, return {}", valFromComp.getId(), valFromComp.getClass().getName(), valToEval, valFrom, valTo, retRes);
		} // Decimalbox / Datebox
		else {
			logger.error("BetweenValidator.evalBetweenFilter.  Unsupported control type: {}, valFromComp: {}", valFromComp.getClass().getName(), valFromComp);
			throw new IllegalArgumentException("Unsupported control type in BetweenFilterMacro.evalBetweenFilter(): "+valFromComp.getClass().getName()+", valFromComp= "+valFromComp);
		}
		return retRes;
	} // public static boolean evalBetweenFilter(Comparable<?> objValToEval, InputElement valFromComp, InputElement valToComp)
	
	/** Вернуть экземпляр (внутреннего) класса валидатора. */
	public static Constraint genBetweenValidator() { // потому что не умею создать объект вложенного класса в ZUL
		return new BetweenValidator();
	}

} // public class BetweenFilters