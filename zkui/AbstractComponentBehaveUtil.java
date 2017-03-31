package basos.zkui;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.zkoss.zk.ui.AbstractComponent;


/** ������������� ��������� ���������, ������������ �������� GridDataFilter.
 * ���������� ��� ������� ���� ������-���������� ����������� ���� ������ � ���������.
 * ��� ������ ������ �� ���� ���� ������������, �� ������� �� ����������, ��������� ������� �� �����������...
 */
public abstract class AbstractComponentBehaveUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(AbstractComponentBehaveUtil.class);

// FIXME: thread-safe Multiton (one instance per subclass): http://stackoverflow.com/questions/11126866/thread-safe-multitons-in-java
	//private static AbstractComponentBehaveUtil instance;
	
	protected AbstractComponentBehaveUtil() {/*����������� ������������ ���������� (�� ������ �� �����), �� ��������� ������ �������������� ������� !*/};
	
	/** ���������� ����� ��������� ��������� ��������� (�����������) */
	public/*protected*/ static <T extends AbstractComponentBehaveUtil> T newInstance(Class<T> instClass) {
		T instance = null;
		if ( instance == null ) {
			try {
				instance = instClass.newInstance();
			} catch(IllegalAccessException | InstantiationException | ExceptionInInitializerError | SecurityException e) {
				logger.error("Error on creation instance of {}", instClass.getSimpleName(), e);
				throw new InternalError("Error on creation instance of " + instClass.getSimpleName(), e);
			}
		}
		return instance;
	}
	
// TODO: �������� ������� � enum ?
	/** ������������ � {@link #getCorrespondingFieldName(AbstractComponent) getCorrespondingFieldName()} ��� ����� ���������� ���, ��� �� ���������� ������� ����������� �������� (���� ����������) � ����� ���������������� ���� ��������� �������.
	 * @param ac ���������, �� �� �������� ����� �������� �������� ���� ��������� �������. ������ ����� �������� ��, ��������������� �� suffix.
	 * @param suffix ������� ���� ����������.
	 * @return �� ��� �������� (StringUtils.removeEnd(compId, suffix)).
	 * @see StringUtils#removeEnd(String, String) StringUtils.removeEnd()
	 * @exception IllegalArgumentException ��� ������� ��� ������������� �� ����������.
	 */
	public static String getCorrespondingFieldNameAsIdWithoutSuffix(AbstractComponent ac, String suffix) {
		String compId = ac.getId()
			  ,compType = ac.getClass().getSimpleName();
		int suffixLen = suffix.length();
 		if ( StringUtils.isBlank(compId) ) {
 			logger.error("getCorrespondingFieldName. Nullable ID are disabled: problem on component '"+ac+"' of type '"+compType);
 			throw new IllegalArgumentException("getCorrespondingFieldName. Nullable ID are disabled: problem on component '"+ac+"' of type '"+compType);
 		}
 		if ( !compId.endsWith(suffix) || compId.length() <= suffixLen ) {
 			logger.error("getCorrespondingFieldName. Broken name convention: component ID '{}' of type '{}' must ends with '{}'", compId, compType, suffix);
 			throw new IllegalArgumentException("getCorrespondingFieldName. Broken name convention: component ID '"+compId+"' of type '"+compType+"' must ends with '"+suffix+"'");
 		}
 		return compId.substring(0, compId.length()-suffixLen); // *suffix // StringUtils.removeEnd(compId, suffix)
	}

	/** ��������� ������� ����������, �������� �������� ���� ��������� �������, ��� ������������ �������� ������ �������.
	 * @param ac ���������, ID �������� ������������� � ������������ ����.
	 * @return ������������ ���������������� ���� ��������� ������� � ������������� � ����������� ���.
	 * @see #getCorrespondingFieldNameAsIdWithoutSuffix(AbstractComponent, String) 
	 */
	public abstract String getCorrespondingFieldName(AbstractComponent ac);
	
	/** �������� ���������� ����������.
	 * @param ac ���������, �������� �������� ����������.
	 */
	public abstract void clear(AbstractComponent ac);
	
	/** ��������� ��������� �� ������ ��������.
	 * @param ac ���������, ������� �������� ���������.
	 * @return ������ ��� ������� �������� ����������.
	 */
	public abstract boolean isEmpty(AbstractComponent ac);
	
	/** �������� ���������� � ����� ������.
	 * @param ac ���������, �������� �������� ����������.
	 * @return ��������� ������������� �������� ����������.
	 */
	public abstract String getText(AbstractComponent ac);
	
	/** ������� ��������� ����������/��������.
	 * @param ac ���������, ������� ������ �����������.
	 * @param disable ���������(true)/���������(false).
	 */
	public abstract void disable(AbstractComponent ac, boolean disable);
	
	/** ������� �������� ���������� � (���������, �.�. ���� �.�. int, ���������� Integer) ����� ������������� �������.
	 * @param ac ���������, ������� ������.
	 * @return �������� ���������� (��� �������� ����� ���� �������� ��� ���������� ����� ����������, ��. {@link BetweenFilterMacro.PairedValue}).
	 */
	public abstract Object getValue(AbstractComponent ac);
	
	/** �������� ������� �������� �� ������������ �������� ���������� (����������� ��������, ��������������� �������� �������� ���� ������� ������ ����-������ ������ ������� ����������).
	 * ���� ���������� ����� ���� ������ (��������, ��� ��������� BetweenFilterMacro �������� ���������� ����� ���� ������������ ����� {@link BetweenFilterMacro.PairedValue}; ������������ �������� ����� ����� ��� BigDecimal ��� Date), �.�. ��������� �� �����������.
	 * @param componentValue �������� ����������. ����� ���� �������� ������� {@link #getValue(AbstractComponent) getValue()}.
	 * @param otherValue ����������� �������� (�������� ������ �����).
	 * @return ������ ��� ���������� ������� ������������ ��������� (�������� �� �������� ���� ������ ����������).
	 */
	public abstract boolean theFilter(Object componentValue, Object otherValue);
	
	/** �������� ������� �������� �� ������� ��������� �������� ���������� (��������, ��������������� �������� �������� ���� ������� ������ ����-������ ������ ������������� ������� ����������).
	 * ���� ���������� ����� ���� ������ (��������, ��� ��������� BetweenFilterMacro �������� ���������� ����� ���� ������������ ����� {@link BetweenFilterMacro.PairedValue}; ������������ �������� ����� ����� ��� BigDecimal ��� Date), �.�. ��������� �� �����������.
	 * @param componentValue �������� ����������. ����� ���� �������� ������� {@link #getValue(AbstractComponent) getValue()}.
	 * @param otherValue ����������� �������� (�������� ������ �����).
	 * @return ������ ��� ��������� ��������.
	 */
	public abstract boolean exactMatch(Object componentValue, Object otherValue);

} // public abstract class AbstractComponentBehaveUtil