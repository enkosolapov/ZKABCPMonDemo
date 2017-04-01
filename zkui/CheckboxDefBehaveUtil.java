package basos.zkui;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.zkoss.zk.ui.AbstractComponent;
import org.zkoss.zul.Checkbox;


/** Реализация поумолчательных поведенческих методов интерфейса AbstractComponentBehaveUtil для компонента org.zkoss.zul.Checkbox.
 * Singleton, use getInstance().
 * @author basos
*/
@javax.enterprise.context.ApplicationScoped // не имеет значения, т.к. вайринг не делаю, а ищу через BeanManager и создаю статическим методом AbstractComponentBehaveUtil
@javax.inject.Named("Checkbox")
public class CheckboxDefBehaveUtil extends AbstractComponentBehaveUtil implements Serializable {
	private static final long serialVersionUID = -8488138261420657487L;

	private static final Logger logger = LoggerFactory.getLogger(CheckboxDefBehaveUtil.class);
	
	private static CheckboxDefBehaveUtil instance;
	
	protected CheckboxDefBehaveUtil() {
		super();
		logger.trace("instantiate CheckboxDefBehaveUtil");
	};
	
	/** Singleton */
	public static synchronized CheckboxDefBehaveUtil getInstance() {
		if ( instance == null ) {
			instance = new CheckboxDefBehaveUtil(); //(CheckboxDefBehaveUtil)AbstractComponentBehaveUtil.newInstance(CheckboxDefBehaveUtil.class);
		}
		return instance;
	}
	
	/** {@inheritDoc} fieldName = removeEnd(compId, "CHB") */
	@Override
	public String getCorrespondingFieldName(AbstractComponent ac) {
		return AbstractComponentBehaveUtil.getCorrespondingFieldNameAsIdWithoutSuffix(ac, "CHB"); // *CHB // StringUtils.removeEnd(compId, "CHB")
	}
	
	/** {@inheritDoc} Для Checkbox clear == uncheck */
	@Override
	public void clear(AbstractComponent ac) {
		((Checkbox)ac).setChecked(false);
	}
	
	/** {@inheritDoc} Для Checkbox empty == unchecked */
	@Override
	public boolean isEmpty(AbstractComponent ac) {
		return !((Checkbox)ac).isChecked();
	}
	
	/** @return "true"/"false" */
	@Override
	public String getText(AbstractComponent ac) {
		return String.valueOf( ((Checkbox)ac).isChecked() );
	}
	
	@Override
	public void disable(AbstractComponent ac, boolean disable) {
		((Checkbox)ac).setDisabled(disable);
	}
	
	/** @return Boolean (not null) == Checkbox.isChecked() */
	@Override
	public Object getValue(AbstractComponent ac) {
		return Boolean.valueOf( ((Checkbox)ac).isChecked() ); // Boolean (not null)
	}
	
	/** Проверка на равенство двух Boolean при взведённом чекбоксе (componentValue == Boolean.TRUE). Неотмеченный крыж проходят все, отмеченный только true.
	 * @return Истина для пустого значения компонента (unchecked) и для точно совпадающих значений (equals) - двух true.
	 */
	@Override
	public boolean theFilter(Object componentValue, Object otherValue) {
		return ( componentValue.equals(Boolean.FALSE) || componentValue.equals(otherValue) ); // для Checkbox getValue() not null
	}
	
	/** Проверка на равенство двух Boolean. Такой Checkbox всегда делит столбец на два взаимоисключающих множества. */
	@Override
	public boolean exactMatch(Object componentValue, Object otherValue) {
		return componentValue.equals(otherValue); // для Checkbox getValue() not null
	}
	
} // public class CheckboxDefBehaveUtil extends AbstractComponentBehaveUtil