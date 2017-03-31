package basos.zkui;

import java.io.Serializable;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.zkoss.zk.ui.AbstractComponent;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.ext.Selectable;
import org.zkoss.zul.impl.InputElement;


/** Реализация поумолчательных поведенческих методов интерфейса AbstractComponentBehaveUtil для компонента org.zkoss.zul.Combobox.
 * Singleton, use getInstance().
 * @author basos
*/
//@javax.enterprise.context.RequestScoped
//@javax.inject.Singleton // don't resolved...
@javax.enterprise.context.ApplicationScoped
@javax.inject.Named("Combobox")
public class ComboboxDefBehaveUtil extends AbstractComponentBehaveUtil implements Serializable {
	private static final long serialVersionUID = 3380766055383921320L;

	private static final Logger logger = LoggerFactory.getLogger(ComboboxDefBehaveUtil.class);
	
	private static ComboboxDefBehaveUtil instance;
	
	protected ComboboxDefBehaveUtil() {
		super();
		logger.trace("instantiate ComboboxDefBehaveUtil");
	};
	
	/** Singleton */
	//@javax.enterprise.context.RequestScoped
	//@javax.enterprise.inject.Produces
	public static synchronized ComboboxDefBehaveUtil getInstance() {
		if ( instance == null ) {
			logger.trace("instantiate ComboboxDefBehaveUtil via getInstance");
			instance = new ComboboxDefBehaveUtil(); //(ComboboxDefBehaveUtil)AbstractComponentBehaveUtil.newInstance(ComboboxDefBehaveUtil.class);
		}
		return instance;
	}
	
	/** fieldName = StringUtils.removeEnd(compId, "Combo") */
	@Override
	public String getCorrespondingFieldName(AbstractComponent ac) {
		return AbstractComponentBehaveUtil.getCorrespondingFieldNameAsIdWithoutSuffix(ac, "Combo"); // *Combo // StringUtils.removeEnd(compId, "Combo")
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public void clear(AbstractComponent ac) {
		if (((Combobox)ac).getModel() != null) ((Selectable)((Combobox)ac).getModel()).clearSelection(); // value НЕ обнуляется ?!
		((Combobox)ac).setValue(null);
	}
	
	/** Empty Combobox contains blank string or {@literal <all>} special value.
	 * @see StringUtils#isEmpty(CharSequence) StringUtils.isEmpty()
	 */
	@Override
	public boolean isEmpty(AbstractComponent ac) {
		String val = ((InputElement)ac).getText(); // null не возвращается, вместо него всегда "" !
		if ("<all>".equals(val)) val = ""; // combobox special value
		return StringUtils.isEmpty(val);
	}
	
	/** @return {@inheritDoc} Вместо специального значения {@literal <all>} возвращается пустая строка. {@literal <null> и <notnull>} возвращаются без изменений.*/
	@Override
	public String getText(AbstractComponent ac) {
		String val = ((InputElement)ac).getText();
		if ("<all>".equals(val)) {
			val = ""; // combobox special value
		}
		return val;
	}
	
	@Override
	public void disable(AbstractComponent ac, boolean disable) {
		((InputElement)ac).setDisabled(disable);
	}
	
	/** @return String (nullable ?) representing Combobox value. */
	@Override
	public Object getValue(AbstractComponent ac) { // может ли расходиться ((InputElement)ac).getValue() ?
		String compVal = null; // пока множественного выбора нет
  		@SuppressWarnings("unchecked")
		Set<String> comboSelection = (((Combobox)ac).getModel() == null ? null : ((Selectable<String>)((Combobox)ac).getModel()).getSelection());
  		if (comboSelection != null && !comboSelection.isEmpty()) {
  			compVal = comboSelection.iterator().next();
  		}
  		//compVal = ((InputElement)ac).getText(); // nullable ?
		return compVal;
	}
	
	/** Проверка соответствия строки (поля текстовой колонки) значению комбика с учётом специальных значений последнего: {@literal<all>, <null>, <notnull>}.
	 * @return Истина для пустого значения компонента ({@link StringUtils#isEmpty(CharSequence) StringUtils.isEmpty()}), значения {@literal<all>} компонента, равенства строковых значений, значению компонента {@literal<null>} удовлетворяют все пустые строки ({@link StringUtils#isBlank(CharSequence) StringUtils.isBlank()}), значению компонента {@literal<notnull>} удовлетворяют все непустые строки.
	 * @see StringUtils#isEmpty(CharSequence) StringUtils.isEmpty()
	 * @see StringUtils#isBlank(CharSequence) StringUtils.isBlank()
	 */
	@Override
	public boolean theFilter(Object componentValue, Object otherValue) {
		return StringUtils.isEmpty((String)componentValue)
			|| "<all>".equals(componentValue)
			|| componentValue.equals(otherValue)
			|| "<null>".equals(componentValue) && StringUtils.isBlank((String)otherValue) // здесь по строго null, а все пустые (whitespace, empty ("") or null) !
			|| "<notnull>".equals(componentValue) && !StringUtils.isBlank((String)otherValue);
	}
	
	/** Проверка соответствия строки (поля текстовой колонки) значению комбика (точное совпадение).
	 * @return Ложь если одно из значений пусто ({@link StringUtils#isEmpty(CharSequence) StringUtils.isEmpty()}) и для специальных значений компонента ({@literal <all>, <null>, <notnull>}).
	 * @see StringUtils#isEmpty(CharSequence) StringUtils.isEmpty()
	 */
	@Override
	public boolean exactMatch(Object componentValue, Object otherValue) {
		return !StringUtils.isEmpty((String)componentValue)
			&& componentValue.equals(otherValue)
			&& !"<all>".equals(componentValue)
			&& !"<null>".equals(componentValue)
			&& !"<notnull>".equals(componentValue);
	}

} // public class ComboboxDefBehaveUtil extends AbstractComponentBehaveUtil