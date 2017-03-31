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


/** ���������� ��������������� ������������� ������� ���������� AbstractComponentBehaveUtil ��� ���������� org.zkoss.zul.Combobox.
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
		if (((Combobox)ac).getModel() != null) ((Selectable)((Combobox)ac).getModel()).clearSelection(); // value �� ���������� ?!
		((Combobox)ac).setValue(null);
	}
	
	/** Empty Combobox contains blank string or {@literal <all>} special value.
	 * @see StringUtils#isEmpty(CharSequence) StringUtils.isEmpty()
	 */
	@Override
	public boolean isEmpty(AbstractComponent ac) {
		String val = ((InputElement)ac).getText(); // null �� ������������, ������ ���� ������ "" !
		if ("<all>".equals(val)) val = ""; // combobox special value
		return StringUtils.isEmpty(val);
	}
	
	/** @return {@inheritDoc} ������ ������������ �������� {@literal <all>} ������������ ������ ������. {@literal <null> � <notnull>} ������������ ��� ���������.*/
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
	public Object getValue(AbstractComponent ac) { // ����� �� ����������� ((InputElement)ac).getValue() ?
		String compVal = null; // ���� �������������� ������ ���
  		@SuppressWarnings("unchecked")
		Set<String> comboSelection = (((Combobox)ac).getModel() == null ? null : ((Selectable<String>)((Combobox)ac).getModel()).getSelection());
  		if (comboSelection != null && !comboSelection.isEmpty()) {
  			compVal = comboSelection.iterator().next();
  		}
  		//compVal = ((InputElement)ac).getText(); // nullable ?
		return compVal;
	}
	
	/** �������� ������������ ������ (���� ��������� �������) �������� ������� � ������ ����������� �������� ����������: {@literal<all>, <null>, <notnull>}.
	 * @return ������ ��� ������� �������� ���������� ({@link StringUtils#isEmpty(CharSequence) StringUtils.isEmpty()}), �������� {@literal<all>} ����������, ��������� ��������� ��������, �������� ���������� {@literal<null>} ������������� ��� ������ ������ ({@link StringUtils#isBlank(CharSequence) StringUtils.isBlank()}), �������� ���������� {@literal<notnull>} ������������� ��� �������� ������.
	 * @see StringUtils#isEmpty(CharSequence) StringUtils.isEmpty()
	 * @see StringUtils#isBlank(CharSequence) StringUtils.isBlank()
	 */
	@Override
	public boolean theFilter(Object componentValue, Object otherValue) {
		return StringUtils.isEmpty((String)componentValue)
			|| "<all>".equals(componentValue)
			|| componentValue.equals(otherValue)
			|| "<null>".equals(componentValue) && StringUtils.isBlank((String)otherValue) // ����� �� ������ null, � ��� ������ (whitespace, empty ("") or null) !
			|| "<notnull>".equals(componentValue) && !StringUtils.isBlank((String)otherValue);
	}
	
	/** �������� ������������ ������ (���� ��������� �������) �������� ������� (������ ����������).
	 * @return ���� ���� ���� �� �������� ����� ({@link StringUtils#isEmpty(CharSequence) StringUtils.isEmpty()}) � ��� ����������� �������� ���������� ({@literal <all>, <null>, <notnull>}).
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