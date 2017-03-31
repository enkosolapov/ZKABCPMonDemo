package basos.zkui;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.zkoss.zk.ui.AbstractComponent;
import org.zkoss.zul.Intbox;
import org.zkoss.zul.impl.InputElement;


/** ���������� ��������������� ������������� ������� ���������� AbstractComponentBehaveUtil ��� ���������� org.zkoss.zul.Intbox.
 * Singleton, use getInstance().
 * @author basos
*/
@javax.enterprise.context.ApplicationScoped
@javax.inject.Named("Intbox")
public class IntboxDefBehaveUtil extends AbstractComponentBehaveUtil implements Serializable {
	private static final long serialVersionUID = 8294361591534575107L;

	private static final Logger logger = LoggerFactory.getLogger(IntboxDefBehaveUtil.class);
	
	private static IntboxDefBehaveUtil instance;
	
	protected IntboxDefBehaveUtil() {
		super();
		logger.trace("instantiate IntboxDefBehaveUtil");
	};
	
	/** Singleton */
	public static synchronized IntboxDefBehaveUtil getInstance() {
		if ( instance == null ) {
			instance = new IntboxDefBehaveUtil(); //(IntboxDefBehaveUtil)AbstractComponentBehaveUtil.newInstance(IntboxDefBehaveUtil.class);
		}
		return instance;
	}
	
	/** fieldName = removeEnd(compId, "IB") */
	@Override
	public String getCorrespondingFieldName(AbstractComponent ac) {
		return AbstractComponentBehaveUtil.getCorrespondingFieldNameAsIdWithoutSuffix(ac, "IB"); // *IB // StringUtils.removeEnd(compId, "IB")
	}
	
	@Override
	public void clear(AbstractComponent ac) {
// null ��� Intbox (0 �.�. ���������� ���������) !!!
		((Intbox)ac).setText(""); // .setValue(0)
	}
	
	@Override
	public boolean isEmpty(AbstractComponent ac) {
		return ((Intbox)ac).getValue() == null; // .intValue() == 0 (NO !); Integer getValue(), might be null; int intValue(): If null, zero is returned
	}
	
	@Override
	public String getText(AbstractComponent ac) {
		return ((InputElement)ac).getText();
	}
	
	@Override
	public void disable(AbstractComponent ac, boolean disable) {
		((InputElement)ac).setDisabled(disable);
	}
	
	/** @return nullable Integer */
	@Override
	public Object getValue(AbstractComponent ac) {
		return ((Intbox)ac).getValue(); // nullable Integer
	}
	
	/** �������� �� ��������� ���� Integer.
	 * @return ������ ��� ������� �������� ���������� (null) � ��� ����� ����������� �������� (equals).
     * ���������� �� {@link #exactMatch} ���, ��� ������� ������� ������������� ����� �������� �������.
	 */
	@Override
	public boolean theFilter(Object componentValue, Object otherValue) {
// ���� ������ �.�. ��� int, ��� � Integer (nullable !)
		return ( componentValue == null /*|| componentValue.equals(0)*/ || componentValue.equals(otherValue) ); // ������ ����������
	}
	
	/** �������� �� ��������� ���� �������� Integer.
	 * @return ���� ���� ���� �� �������� ����� (null).
     * ���������� �� {@link theFilter} ���, ��� ������ ������ ����� �� ������.
	 */
	@Override
	public boolean exactMatch(Object componentValue, Object otherValue) {
// ���� ������ �.�. ��� int, ��� � Integer (nullable !)
		return ( componentValue != null && componentValue.equals(otherValue) ); // ������ ����������
	}

} // public class IntboxDefBehaveUtil extends AbstractComponentBehaveUtil