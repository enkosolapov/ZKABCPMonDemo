package basos.zkui;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.zkoss.zk.ui.AbstractComponent;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.impl.InputElement;


/** Реализация поумолчательных поведенческих методов интерфейса AbstractComponentBehaveUtil для компонента org.zkoss.zul.Textbox.
 * Singleton, use getInstance().
 * @author basos
*/
@javax.enterprise.context.ApplicationScoped
@javax.inject.Named("Textbox")
public class TextboxDefBehaveUtil extends AbstractComponentBehaveUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(TextboxDefBehaveUtil.class);
	
	private static TextboxDefBehaveUtil instance;
	
	protected TextboxDefBehaveUtil() {
		super();
		logger.trace("instantiate TextboxDefBehaveUtil");
	};
	
	/** Singleton */
	public static synchronized TextboxDefBehaveUtil getInstance() {
		if ( instance == null ) {
			instance = new TextboxDefBehaveUtil(); //(TextboxDefBehaveUtil)AbstractComponentBehaveUtil.newInstance(TextboxDefBehaveUtil.class);
		}
		return instance;
	}
	
	/** fieldName = removeEnd(compId, "TB") */
	@Override
	public String getCorrespondingFieldName(AbstractComponent ac) {
		return AbstractComponentBehaveUtil.getCorrespondingFieldNameAsIdWithoutSuffix(ac, "TB"); // *TB // StringUtils.removeEnd(compId, "TB")
	}
	
	
	@Override
	public void clear(AbstractComponent ac) {
		((InputElement)ac).setText("");
	}
	
	
	/** Empty Textbox contains blank string.
	 * @see StringUtils#isBlank(CharSequence)
	 */
	@Override
	public boolean isEmpty(AbstractComponent ac) {
		return StringUtils.isBlank( ((Textbox)ac).getValue() ); // Textbox.getValue() not null
	}
	
	
	@Override
	public String getText(AbstractComponent ac) {
		return ((InputElement)ac).getText();
	}
	
	
	@Override
	public void disable(AbstractComponent ac, boolean disable) {
		((InputElement)ac).setDisabled(disable);
	}
	
	
	/** @return String (not null) represented Textbox value. */
	@Override
	public Object getValue(AbstractComponent ac) {
		return ((Textbox)ac).getValue(); // String (not null)
	}
	
	
	/** Не чувствительная к регистру проверка на вхождение первой строки во вторую.
	 * @return Истина для пустого значения компонента ({@link StringUtils#isEmpty(CharSequence) StringUtils.isEmpty()}) и если значение компонента является подстрокой другого значения (contains) без учёта регистра.
	 * @see StringUtils#isEmpty(CharSequence) StringUtils.isEmpty()
	 */
	@Override
	public boolean theFilter(Object componentValue, Object otherValue) {
		return ( StringUtils.isEmpty((String)componentValue) || otherValue != null && ((String)otherValue).toUpperCase().contains(((String)componentValue).toUpperCase()) );
	}
	
	/** Чувствительная к регистру проверка на строгое соответствие двух строк.
	 * @return Ложь если одно из значений null, истина при точном совпадении двух ненулевых строк.
	 */
	@Override
	public boolean exactMatch(Object componentValue, Object otherValue) {
		return ( componentValue != null && otherValue != null && ((String)componentValue).equals/*IgnoreCase*/((String)otherValue) );
	}

} // public class TextboxDefBehaveUtil extends AbstractComponentBehaveUtil