package basos.zkui;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.zkoss.zk.ui.AbstractComponent;

import basos.data.GridData;
import basos.data.dao.GridDataProviderWPk;
import basos.xe.data.entity.SubjSumm;


/** ����������� ������ - ������ �����������, ��������������� ��������� � ����������� ����-������ ����� ��� ���������
 *  �� ��������� �� "��������� ����� �����", ��������������� ����-�����������.
 * ������ ������������ � ����������� (composer), ������� ��� ������, ��������� (�������������) ����� ������-���������;
 *  ����� � ��������� (GridDataFilterableModelMan), ��������� ��� � ������� (��������� ������� � �.�.).
 * ������-�������� ������������� �������� � �������� (clear ������� �������� � �.�.).
 */
// FIXME: ��������������� ������ ����� ���� ???
// FIXME: ������� ���������� �� ���������� (���� VS �� ���������; ���� ���������) !!!
// FIXME: ������������� ������������� ��������� ������ ��������� (�������� ����� ���������� ��� applet � ����������), ����� ������ ������ �� LinkedList !! ������� �� ���������������� ��������� ������� ������ GridData (��� ����������/�������� �������� ������ ��������� ������ ?) !!
public class GridDataFilter implements Serializable {

	private static final long serialVersionUID = 2989429602320596648L;
	
	private static final Logger logger = LoggerFactory.getLogger(GridDataFilter.class);
	
	private final GridDataProviderWPk<?> dataProvider;
	private final Class<?> beanClass;
 	private final AbstractComponent[] filterComponentArray; // ������ ���������, ����������� ������ (��� ����������� ���������)
 	private final String[] filterFieldArray; // ��������������� ������-��������� �������� ����� (���������������, �������� filterComponentArray !)
 	
 	private Map<String, AbstractComponentBehaveUtil> behavMap; // ��������� ������-��������� ������� ����
 	
// 	private GridDataFilter() {} // FIXME: ������������ ����������� JavaBeans !!!
 	
 	/** ����� ����-���������� �������� ����� ����, ��������� ������ ������-���������, �������������� ������ �������� ��������������� �� ����� ���� (��� ������ GridData).
 	 * @param dataProvider ��������� ������, ��������������� ������ ��������-������ ���� {@link GridData GridData<T>} ��� ����-������ �����.
 	 * @param behavMap ��������� ��� ������� �����������. ����� ���������� AbstractComponentBehaveUtil ��� ������� ���� ��������. ������������ �����������, ����� �� �������� (live) - ��� ���������� ������� ����� (����� ������ �� ����).
 	 * @param _filterComponentArray ������ ������������ ������ ����������� (�����-���������). �������� �� ����� 32. �������� �����, ����� ��������� ������. null-�������� � null-�������� ���������.
 	 * @exception IllegalArgumentException ��� ������� ��������� �������� ����� 32; ��� ������ ��� �� ��������������� ���������� ��� �� ������-���������; ��� ���������������� ����������� (�� ���������� ���������).
 	 */
// TODO: �������: https://docs.oracle.com/javase/8/docs/api/java/lang/SafeVarargs.html
 	@SafeVarargs
 	public GridDataFilter(GridDataProviderWPk<?> dataProvider, Map<String, AbstractComponentBehaveUtil> behavMap, final AbstractComponent... _filterComponentArray) {
// ����������� �� ���-�� ������-��������� (max - 32 - ���-�� ��� � GridData.filterFlags)
 		if ( _filterComponentArray != null && _filterComponentArray.length > 32 ) {
 			logger.error("GridDataFilter. [0-32] components in filter allowed, received: {}", _filterComponentArray.length);
 			throw new IllegalArgumentException("GridDataFilter. [0-32] components in filter allowed, received: "+_filterComponentArray.length);
 		}
 		this.dataProvider = dataProvider;
 		this.beanClass = dataProvider.getBeanClass();
// FIXME: �������� ��������� ������� !
 		this.filterComponentArray = ArrayUtils.nullToEmpty(ArrayUtils.removeAllOccurences(_filterComponentArray, null), AbstractComponent[].class); // (nullable) ������� �������� �����
 		this.behavMap = behavMap;
 		Arrays.sort(this.filterComponentArray, ABSTRACT_COMPONENT_BY_ID_COMPARATOR);
// FIXME: ��������� �� �� ��������� � ������������ !!!
 		filterFieldArray = new String[this.filterComponentArray.length]; // TODO: Map instead ?
 		for (int idx = 0; idx < filterComponentArray.length; idx++) {
 			filterFieldArray[idx] = componentToFieldName(filterComponentArray[idx]);
 		}
 		logger.trace("GridDataFilter(). filterComponentArray = '{}', length = {}"/*+", deepToString = '{}'"*/, filterComponentArray, this.filterComponentArray.length /*, Arrays.deepToString(filterComponentArray)*/ );
 	} // public GridDataFilter(GridDataProviderWPk<?> dataProvider, Map<String, AbstractComponentBehaveUtil> behavMap, AbstractComponent... _filterComponentArray)
 	
 	// ������ ����� ����������� ���������� �� ������������, �� ������ �� ��������� �� �������� � ����������� �� ����
 	private String componentToFieldName(AbstractComponent ac) {
 		String compType = ac.getClass().getSimpleName();
 		if ("selFilterCHB".equals(ac.getId())) return "sel"; // ����������� ���� GridData.sel
 		AbstractComponentBehaveUtil curBehav;
 		curBehav = behavMap.get(compType);
 		if ( curBehav == null ) {
 			logger.error("componentToFieldName. Unsupported element '{}' of type '{}' in filter.componentToFieldName()", ac, compType);
			throw new IllegalArgumentException("componentToFieldName. Unsupported control '"+ac+"' of type '"+compType+"' in filter.componentToFieldName()");
 		}
 		return curBehav.getCorrespondingFieldName(ac);
 		
 		/*String compId = ac.getId();
 		if ( StringUtils.isBlank(compId) ) {
 			logger.error("componentToFieldName. Nullable ID are disabled: problem on component '"+ac+"' of type '"+compType);
 			throw new IllegalArgumentException("componentToFieldName. Nullable ID are disabled: problem on component '"+ac+"' of type '"+compType);
 		}
		switch(compType) {
			case "Intbox": if (!compId.endsWith("IB") || compId.length() < 3) {
							   logger.error("componentToFieldName. Broken name convention: component ID '{}' of type '{}' must ends with 'IB'", compId, compType);
							   throw new IllegalArgumentException("componentToFieldName. Broken name convention: component ID '"+compId+"' of type '"+compType+"' must ends with 'IB'");
						   }
						   return compId.substring(0, compId.length()-2); // *IB // StringUtils.removeEnd(compId, "IB")
			case "Checkbox": if ("selFilterCHB".equals(compId)) return "sel"; // ����������� ���� GridData.sel
 							 else {
 								 if (!compId.endsWith("CHB") || compId.length() < 4) {
 								     logger.error("componentToFieldName. Broken name convention: component ID '{}' of type '{}' must ends with 'CHB'", compId, compType);
 								     throw new IllegalArgumentException("componentToFieldName. Broken name convention: component ID '"+compId+"' of type '"+compType+"' must ends with 'CHB'");
 								 }
 								 return compId.substring(0, compId.length()-3); // *CHB
 							 }
			case "Textbox": if (!compId.endsWith("TB") || compId.length() < 3) {
								logger.error("componentToFieldName. Broken name convention: component ID '{}' of type '{}' must ends with 'TB'", compId, compType);
								throw new IllegalArgumentException("componentToFieldName. Broken name convention: component ID '"+compId+"' of type '"+compType+"' must ends with 'TB'");
							}
							return compId.substring(0, compId.length()-2); // *TB
			case "Combobox": if (!compId.endsWith("Combo") || compId.length() < 6) {
								 logger.error("componentToFieldName. Broken name convention: component ID '{}' of type '{}' must ends with 'Combo'", compId, compType);
							     throw new IllegalArgumentException("componentToFieldName. Broken name convention: component ID '"+compId+"' of type '"+compType+"' must ends with 'Combo'");
							 }
							 return compId.substring(0, compId.length()-5); // *Combo
			case "BetweenFilterMacro": if (!compId.endsWith("Fltr") || compId.length() < 5) {
										   logger.error("componentToFieldName. Broken name convention: component ID '{}' of type '{}' must ends with 'Fltr'", compId, compType);
									       throw new IllegalArgumentException("componentToFieldName. Broken name convention: component ID '"+compId+"' of type '"+compType+"' must endsWith 'Fltr'");
									   }
									   return compId.substring(0, compId.length()-4); // *Fltr
			default: logger.error("componentToFieldName. Unsupported element '{}' of type '{}' in filter.componentToFieldName()", ac, compType);
					throw new IllegalArgumentException("componentToFieldName. Unsupported control '"+ac+"' of type '"+compType+"' in filter.componentToFieldName()");
		} // switch
*/ 	} // private String componentToFieldName(AbstractComponent ac)

 	/** �������� ��� ������-��������, ����� ������������ �������� PK, ��� �������� ���������.
 	 * @param clearPK ������� �� ������� (������������� ����������� � ����-����������, ������ ����� ��������) ������� ���������� �����.
 	 * @see AbstractComponentBehaveUtil#clear(AbstractComponent ac)
 	 */
// ���������� �� GridDataFilterableModelMan.applyFilter (���� ��������� PK) � �� SubjsPageComposer.clearFilter (������� � ���� ������� �� ZUL �� ������� toolbarbutton)
	public void clear(final boolean clearPK) { // ! ������� ���� �������� ��������� (@param clearPK ������� �� ������ �� ���������� �����) !
 		Optional<String> pkFieldName = dataProvider.getPk();
		if (pkFieldName.isPresent()) {
// ��������� pkIdx ��������� �� ��������������� PK ������-��������� � filterComponentArray
//			 			pkIdx = Arrays.asList(filterFieldArray).indexOf(pkFieldName); // (http://stackoverflow.com/questions/6171663/how-to-find-index-of-int-array-in-java-from-a-given-value)
			pkIdx = ArrayUtils.indexOf(filterFieldArray, pkFieldName.get()); // Returns: the index of the object within the array, INDEX_NOT_FOUND (-1) if not found or null array input
		} else { // PK �� �����
			pkIdx = -1;
		}
		int idx = -1;
 		for (AbstractComponent ac : filterComponentArray) {
 			++idx;
 			if ( !clearPK && idx == pkIdx ) continue; // ��� ����������� �� ��� ���������� !!
 	 		String compType = ac.getClass().getSimpleName();
 	 		AbstractComponentBehaveUtil curBehav;
 	 		curBehav = behavMap.get(compType);
 	 		if ( curBehav == null ) {
 	 			logger.error("clear. Unsupported element '{}' of type '{}' in filter.clear()", ac, compType);
				throw new IllegalStateException("Unsupported control type in filter.clear() :"+compType);
 	 		}
 	 		curBehav.clear(ac);
 			
 			/*switch(ac.getClass().getSimpleName()) {
 				case "Checkbox": ((Checkbox)ac).setChecked(false); break;
 				case "Intbox": if (clearPK || idx != pkIdx) {
// null ��� Intbox (0 �.�. ���������� ���������) !!!
							      ((Intbox)ac).setText(""); // .setValue(0)
 							   }
 							   break;
 				case "Combobox": if (((Combobox)ac).getModel() != null) ((Selectable)((Combobox)ac).getModel()).clearSelection(); // value ���������� ?
// 	 				case "": ;
 				case "Textbox": if (clearPK || idx != pkIdx) {
 									((InputElement)ac).setText("");
 								}
 								break;
 				case "BetweenFilterMacro": ((BetweenFilterMacro<?,?>)ac).clear(); break;
 				default: logger.error("clear. Unsupported element '{}' of type '{}' in filter.clear()", ac, ac.getClass().getSimpleName());
 						throw new IllegalStateException("Unsupported control type in filter.clear() :"+ac.getClass().getSimpleName());
 			} // switch*/
 	 		
 		} // for
 		logger.trace("Filter was cleared. clearPK = {}, pkIdx = {}, pkFieldName = '{}', idx(counter) = {}", clearPK, pkIdx, (pkIdx != -1 ? pkFieldName.get() : "<pk_not_def>"), idx);
 	} // public void clear(boolean clearPK)
	
	/** ���������� ��� ��������� �� ID �������� �� ������ AbstractComponent */
 	public static final Comparator<AbstractComponent> ABSTRACT_COMPONENT_BY_ID_COMPARATOR = new CompareACById();
 	
 	private static final class CompareACById implements Comparator<AbstractComponent>, Serializable {
		private static final long serialVersionUID = -2275134174101534842L;
        @Override
        public int compare(AbstractComponent o1, AbstractComponent o2) {
            if (o1 == null || o2 == null) {
            	logger.error("ABSTRACT_COMPONENT_BY_ID_COMPARATOR. null argument: o1 = {}, o2 = {}", o1, o2);
            	throw new NullPointerException("ABSTRACT_COMPONENT_BY_ID_COMPARATOR. null argument: o1 = "+o1+", o2 = "+o2);
            }
            if ( StringUtils.isBlank(o1.getId()) ) {
            	logger.error("ABSTRACT_COMPONENT_BY_ID_COMPARATOR. Nullable ID are disabled: problem on component '{}' of type '{}'", o1, o1.getClass().getSimpleName());
            	throw new IllegalArgumentException("ABSTRACT_COMPONENT_BY_ID_COMPARATOR. Nullable ID are disabled: problem on component '"+o1+"' of type '"+o1.getClass().getSimpleName());
            }
            if ( StringUtils.isBlank(o2.getId()) ) {
            	logger.error("ABSTRACT_COMPONENT_BY_ID_COMPARATOR. Nullable ID are disabled: problem on component '{}' of type '{}'", o2, o2.getClass().getSimpleName());
            	throw new IllegalArgumentException("ABSTRACT_COMPONENT_BY_ID_COMPARATOR. Nullable ID are disabled: problem on component '"+o2+"' of type '"+o2.getClass().getSimpleName());
            }
            return o1.getId().compareTo(o2.getId());
        } // public int compare(AbstractComponent o1, AbstractComponent o2) 
    }; // private static final class CompareACById implements Comparator<AbstractComponent>, Serializable

    /** ������� ���������� � ������� filterComponentArray ����������� �������. ����� ������������ ��� �������� ������� ���������� � �������.
     * @return &lt; 0 ���� ��������� �� ��������� � �������.
     * @see java.util.Arrays#binarySearch(Object[], Object, Comparator) Arrays.binarySearch()
     */
// ���������� �� GridDataFilterableModelMan.setComboModel ��� �������� ������� �����; � ������������ ������� �� ����������� ��� ������������ ������������� ���������� �������
 	public int getComponentIndex(AbstractComponent component) {
 		return Arrays.binarySearch(filterComponentArray // !!! ������ ������ ���� ������������ (������� ����������� � applyFilter) !!!
 								  ,component
 								  ,ABSTRACT_COMPONENT_BY_ID_COMPARATOR
 								  );
 	} // public int getComponentIndex(AbstractComponent component)

 	/** ����� ������-�������� �� ���������� ������� �� ��.
 	 * @see #getComponentIndex(AbstractComponent) getComponentIndex()
 	 */
// ���������� �� isComponentEmptyById (�� ���-��)
 	public int getComponentIndexById(String compId) {
        int low = 0;
        int high = filterComponentArray.length - 1;
        while (low <= high) { // ������ ������ ���� ������������ !
            int mid = (low + high) >>> 1;
            int cmp = filterComponentArray[mid].getId().compareTo(compId);
            if (cmp < 0) low = mid + 1;
            else if (cmp > 0) high = mid - 1;
            else return mid; // key found
        }
        return -(low + 1);  // key not found.
 	} // public int getComponentIndexById(String compId)
 	
 	/** ��������� ��������� �� ������ ��������.
 	 * @param compIndex ������� �������� �� ���������� �������. ��. {@link #getComponentIndex(AbstractComponent)}
 	 * @see AbstractComponentBehaveUtil#isEmpty(AbstractComponent)
 	 * @exception IllegalArgumentException ��� ������������ �������.
 	 */
 	// ���������� ��: isComponentEmptyById, prepareToApply (��� PK-��������), isEmpty
 	public boolean isComponentEmptyByIndex(int compIndex) {
 		if (compIndex < 0 || compIndex >= filterComponentArray.length) {
 			logger.error("Index out of range in isComponentEmptyByIndex: {}, but must be between 0 and {}", compIndex, filterComponentArray.length);
 			throw new IllegalArgumentException("Index out of range in isComponentEmptyByIndex: "+compIndex+", but must be between 0 and "+filterComponentArray.length);
 		}
 		AbstractComponent ac = filterComponentArray[compIndex];
 		String compType = ac.getClass().getSimpleName();
 		AbstractComponentBehaveUtil curBehav;
 		curBehav = behavMap.get(compType);
 		if ( curBehav == null ) {
 			logger.error("Unsupported element '{}' of type '{}' at index {} in isComponentEmptyByIndex", ac, compType, compIndex);
			throw new IllegalStateException("Unsupported control '"+ac+"' of type '"+compType+"' in isComponentEmptyByIndex at index "+compIndex);
 		}
 		return curBehav.isEmpty(ac);

 		
		/*switch(filterComponentArray[compIndex].getClass().getSimpleName()) {
// getValue() == null ��� ������� Intbox (0 �.�. ���������� ���������, � intValue() �� ��������� 0 � null) !!!
			case "Intbox": return ((Intbox)filterComponentArray[compIndex]).getValue() == null; // .intValue() == 0; Integer getValue(), might be null; int intValue(): If null, zero is returned
			case "Checkbox": return !((Checkbox)filterComponentArray[compIndex]).isChecked();
			case "Textbox": return StringUtils.isBlank( ((Textbox)filterComponentArray[compIndex]).getValue() );
			case "Combobox": String val = ((InputElement)filterComponentArray[compIndex]).getText(); // null �� ������������, ������ ���� ������ "" !
							if ("<all>".equals(val)) val = ""; // combobox special value
							return StringUtils.isEmpty(val);
			case "BetweenFilterMacro": return ((BetweenFilterMacro<?,?>)filterComponentArray[compIndex]).isEmpty();
			default: logger.error("Unsupported element '{}' of type '{}' at index {} in isComponentEmptyByIndex", filterComponentArray[compIndex], filterComponentArray[compIndex].getClass().getSimpleName(), compIndex);
					throw new IllegalStateException("Unsupported control '"+filterComponentArray[compIndex]+"' of type '"+filterComponentArray[compIndex].getClass().getSimpleName()+"' in isComponentEmptyByIndex at index "+compIndex);
//					return false;
		} // switch
*/		
 	} // public boolean isComponentEmptyByIndex(int compIndex)
 	
 	/** ��������� ��������� �� ������ ��������.
 	 * @param compId �� ������������ ��������.
 	 * @see #getComponentIndexById(String)
 	 * @see #isComponentEmptyByIndex(int)
 	 */
 	// not used
 	public boolean isComponentEmptyById(String compId) {
 		int compIndex = getComponentIndexById(compId);
 		assert(compIndex >= 0 && compIndex < filterComponentArray.length);
 		if (compIndex >= 0) {
 			return isComponentEmptyByIndex(compIndex);
 		}
 		return false;
 	} // public boolean isComponentEmptyById(String compId)
 	
// ��������� ��������� ������� (�������� � GridDataFilterableModelMan.filterCompositeValue)
 	private String buildFilterCompositeValue() {
 		StringBuilder sb = new StringBuilder();
 		for (AbstractComponent ac : filterComponentArray) {
 			String val = "";
 			
 	 		String compType = ac.getClass().getSimpleName();
 	 		AbstractComponentBehaveUtil curBehav;
 	 		curBehav = behavMap.get(compType);
 	 		if ( curBehav == null ) {
 	 			val = "unsupported element '"+compType+"'";
 	 			logger.error("Unsupported element '{}' of type '{}' in buildFilterCompositeValue()", ac, compType);
 	 			assert(false):val;
 	 		}
 	 		val = curBehav.getText(ac);
 			
 			/*switch(ac.getClass().getSimpleName()) {
 				case "Combobox":
 				case "Intbox":
 				case "Datebox":
 				case "Decimalbox":
 				case "Textbox": val = ((InputElement)ac).getText();
 								if ("<all>".equals(val)) val = ""; // combobox special value
 								break;
 				case "Checkbox": val = String.valueOf(((Checkbox)ac).isChecked()); break;
 				case "BetweenFilterMacro": val = ((BetweenFilterMacro<?,?>)ac).getText(); break;
 				default: val = "unsupported element '"+ac.getClass().getSimpleName()+"'";
 						 assert(false):val;
 			} // switch*/
 	 		
 			sb.append(ac.getId()).append("=").append(val).append("^~");
 		} // for
 		logger.trace("buildFilterCompositeValue = '{}'", sb.toString());
 		return sb.toString(); 
 	} // private String buildFilterCompositeValue()

 	/** ������������ ���� ������-��������� � ����� compID=compVal^~ */
	@Override
	public String toString() {
		return buildFilterCompositeValue();
	}
	
	/** ����� �� ��� ������-�������� ? @see {@link #isComponentEmptyByIndex(int) isComponentEmptyByIndex()} */
// ���������� �� GridDataFilterableModelMan.applyFilter
	public boolean isEmpty() {
		boolean ret_val = false;
		for (int idx = 0; idx < filterComponentArray.length; idx++) {
			ret_val = isComponentEmptyByIndex(idx);
			if (!ret_val) return false;
		}
		return true;
	} // public boolean isEmpty()
	
	/** ������� (��)���������� ��� ������-��������.
	 * @param disable ���������(true)/���������(false).
	 * @param disablePK ������ �� (��)��������� ���������, ����������� �������� ���������� �����.
	 * @see AbstractComponentBehaveUtil#disable(AbstractComponent, boolean) AbstractComponentBehaveUtil.disable()
	 */
	public void disableComps(boolean disable, final boolean disablePK) {
		Optional<String> pkFieldName = dataProvider.getPk();
		if (pkFieldName.isPresent()) {
// ��������� pkIdx ��������� �� ��������������� PK ������-��������� � filterComponentArray
//			pkIdx = Arrays.asList(filterFieldArray).indexOf(pkFieldName); // (http://stackoverflow.com/questions/6171663/how-to-find-index-of-int-array-in-java-from-a-given-value)
			pkIdx = ArrayUtils.indexOf(filterFieldArray, pkFieldName.get()); // Returns: the index of the object within the array, INDEX_NOT_FOUND (-1) if not found or null array input
		} else { // PK �� �����
			pkIdx = -1;
		}
		int idx = -1;
 		for (AbstractComponent ac : filterComponentArray) {
 			++idx;
 			if (!disablePK && idx == pkIdx) continue;
 	 		String compType = ac.getClass().getSimpleName();
 	 		AbstractComponentBehaveUtil curBehav;
 	 		curBehav = behavMap.get(compType);
 	 		if ( curBehav == null ) {
 	 			logger.error("Unsupported element '{}' of type '{}' in disableComps()", ac, compType);
 	 			assert(false):"Unsupported element '"+ac+"' of type '"+compType+"' in disableComps()";
 	 		}
 	 		curBehav.disable(ac, disable);
 			
 			/*switch(ac.getClass().getSimpleName()) {
 				case "Checkbox": ((Checkbox)ac).setDisabled(disable); break;
 				case "Textbox": 
 				case "Intbox": if (idx == pkIdx && !disablePK) break;
 				case "Combobox": ((InputElement)ac).setDisabled(disable); break;
 				case "BetweenFilterMacro": ((BetweenFilterMacro<?,?>)ac).setDisabled(disable); break;
 				default: logger.error("Unsupported element '{}' of type '{}' in filter.disableComps()", ac, ac.getClass().getSimpleName());
 			}*/
 			
 		} // for
	} // public void disableComps(boolean disable, boolean disablePK)
    
	
	/** �������������� ������������ ������ ��� ������ ����������� ������� �� ������� ���� (��������, ��������� �
	 * ������): �������� ������-���������, ������ ������� � ����� ������, ������ ����������.
	 */
// HOWTO: static (������� ? - ��. #22 � �����) ???
	private static final class FilterField implements Serializable {
		private static final long serialVersionUID = 4714793949421773384L;
		private final String fieldName, getterName, compId;
		private final Object compVal1;
		private final transient Method getterMethod; // !!! non serializable !!!
		private final Predicate<Object> evalPredicate;
		public FilterField (final String compId, final String fieldName, final String getterName, final Object compVal, final Method getterMethod, final Predicate<Object> evalPredicate) {
			this.compId = compId;
			this.fieldName = fieldName;
			this.getterName = getterName;
			this.compVal1 = compVal;
			this.getterMethod = getterMethod;
			this.evalPredicate = evalPredicate;
		}
		public final String getCompId() {return compId;}
		public final String getFieldName() {return fieldName;}
		public final String getGetterName() {return getterName;}
		public final Object getCompVal() {return compVal1;}
		private Object fieldVal;
		public final Object getFieldVal() {return fieldVal;}
		public final boolean eval(final Object curObj) {
			try {
				fieldVal = getterMethod.invoke(curObj); // �������� ���� ��������� �������
			} catch ( IllegalAccessException | IllegalArgumentException | java.lang.reflect.InvocationTargetException e) {
				logger.error("FilterField.eval. Exception on invoke {}", getterName, e);
				throw new InternalError("FilterField.eval. Exception on invoke '"+getterName+"' for fieldName = '"+fieldName+"' on object of type "+curObj.getClass().getName(), e);
			}
			return evalPredicate.test(fieldVal);
		} // private final boolean eval(final Object curObj)
	} // private final class FilterField

	// ��� ��������� ���������� �������� � prepareToApply (���������� � PK ����������� ������ ���), �������� � evalGridData (����� ������ ������ ������ �������������� ����� prepareToApply !)
	private FilterField[] filterFields; // �������������� ������ � ������ ��� ���������� ���������� ����-������ (����� filterComponentArray) 
	private boolean pkCtrlNotEmpty; // ���� ��������� �������� �������� (������ Intbox || Textbox !) ��������������� (��� �������� �� prepareToApply � evalGridData)
	private int pkIdx = -1; // ������ �������� ��������������� � ������� filterComponentArray (��� �������� �� prepareToApply � evalGridData)

	/** ����� ������� ����� ����-������ ����� ������ ������� ������ ��������, ������������ ���� ������ � ������� ������-���������.
	 * �� ������ ����������� ������� �������� ������ ����������� ������ FilterField, ����� ����������� ���������� � ��������� ���������� ��� ������������� � evalGridData.
	 * ���������� ����������� �������� ����� ������ �������� ����� ("���������") ����-������ ����� (evalGridData).
	 * ������������, ��� �� ������� ���������� ����� ��������� ������� ����� ���� ������ Intbox ��� Textbox.
	 * ���� �������, ��� ����� ������-��������� ��������.
	 * ��� �������� ��������, ����������� ������� ���������� ����� (PK), ������ ����� ������������ (��� ����� ������ �������); ��� ��������� ������-�������� ��������� � ���������� ������������ ��� �����. ����� ��� �������� �����������.
	 * @exception InternalError(NoSuchMethodException) ��� ������� � ����� ���� (��� ������� ��� �� ������������� ���������� JavaBean ��� ���������� ������������ ����, ���������� �� �� ��������, �����������); IllegalStateException ��� ������������� ��������� ��� ���� ��������. 
	 */
// TESTME: �������������� �� ��������� PK, � �.�. �� null � ""
// TODO: (low) ? ������������ ��������� Apache Commons (��. ������ ������� �� SmallTalk) ??
	//@SuppressWarnings("unchecked")
	public void prepareToApply() {
		Optional<String> pkFieldName = dataProvider.getPk();
		if (filterFields == null || filterFields.length != filterComponentArray.length) {
			filterFields = new FilterField[filterComponentArray.length];
		}
		pkCtrlNotEmpty = false; // ���������� ������ �� PK (�� ���� ��������������� �������)
		if (pkFieldName.isPresent()) {
// ��������� pkIdx ��������� �� ��������������� PK ������-��������� � filterComponentArray
// 			pkIdx = Arrays.asList(filterFieldArray).indexOf(pkFieldName); // (http://stackoverflow.com/questions/6171663/how-to-find-index-of-int-array-in-java-from-a-given-value)
			pkIdx = ArrayUtils.indexOf(filterFieldArray, pkFieldName.get()); // Returns: the index of the object within the array, INDEX_NOT_FOUND (-1) if not found or null array input
		} else { // PK �� �����
			pkIdx = -1;
		}
// PK �.�. int || Integer || String, --�� �������������� ������ ��� int ! �� PK ����� �������������...
// TODO: ��������: ���� ������������ ���������, �� �������������� ����, ��� ����� �� ��������� !! ? ��������� ������� �������������� (������� ���������) ?
// FIXME: ����� �� ���� ������������ �� ��� ��������, ������� ����� ��������� �� !
		if ( pkIdx >= 0 &&
			 ( pkCtrlNotEmpty = ( !isComponentEmptyByIndex(pkIdx) &&
									( filterComponentArray[pkIdx].getClass().getSimpleName().equals("Intbox") ||
									  filterComponentArray[pkIdx].getClass().getSimpleName().equals("Textbox")
									)
							    )
			 ) 
		   ) { // ������������ ������ �� PK: ������ ������ ���������� � ��� ���������� ���� (������ int, �� ����� !) => ������ ���� ������ �� ������
			clear(false); // PK-������-������� �� ������� !!!
			disableComps(true, false); // ��������� ��� ��������� �������� �� ������ ������������� �������
			logger.trace("prepareToApply: clear&disable filter components (PK exclusive filter)");
		} else { // PK �� ���������� ��� ��� ������� ����
			disableComps(false, true); // ������� ���������� ��� ������-�������� (�����: ���������� ��� ���������� �� ���������������, ����������� ��� ������� �������)
			logger.trace("prepareToApply: enable filter components (non-exclusive filter)");
		}

		int idx = -1;
		for (AbstractComponent ac : filterComponentArray) {
			++idx;
			Predicate<Object> evalPredicate = null;
			Object compVal; // effectively final
			String compId = ac.getId()
				  ,fieldName = filterFieldArray[idx]
				  ,getterName = ""; // ����� ����� "may not be initialized"
			Method getterMethod = null;
			
 	 		String compType = ac.getClass().getSimpleName();
			if ( "Checkbox".equals(compType) ) {
				try {
					if ("selFilterCHB".equals(compId)) { // ����������� ���� GridData.sel
						getterName = "isSel";
						//fieldName = "sel";
						getterMethod = GridData.class.getMethod(getterName);
					} else {
						getterName = "is" + StringUtils.capitalize(fieldName);
						getterMethod = beanClass.getMethod(getterName);
					}
				} catch (NoSuchMethodException e) {
					logger.error("prepareToApply. NoSuchMethodException on invoke '"+getterName+"' for fieldName '"+fieldName+"'. See (JavaBean) Rule for control of type '"+compType+"'. beanClass: "+beanClass.getName()+", compId: "+compId, e);
					throw new InternalError("prepareToApply. NoSuchMethodException on invoke '"+getterName+"' for fieldName '"+fieldName+"'. See (JavaBean) Rule for control of type '"+compType+"'. beanClass: "+beanClass.getName()+", compId: "+compId, e);
				}
			} else {
 			//if ( getterMethod == null && !StringUtils.isBlank(getterName) ) { // ����� Checkbox
				try {
					getterName = "get" + StringUtils.capitalize(fieldName);
					getterMethod = beanClass.getMethod(getterName);
				} catch (NoSuchMethodException e) {
					logger.error("prepareToApply. NoSuchMethodException on invoke '"+getterName+"' for fieldName '"+fieldName+"'. See (JavaBean) Rule for control of type '"+ac.getClass().getSimpleName()+"'. beanClass: "+beanClass.getName()+", compId: "+compId, e);
					throw new InternalError("prepareToApply. NoSuchMethodException on invoke '"+getterName+"' for fieldName '"+fieldName+"'. See (JavaBean) Rule for control of type '"+ac.getClass().getSimpleName()+"'. beanClass: "+beanClass.getName()+", compId: "+compId, e);
				}
 			}
 	 		AbstractComponentBehaveUtil curBehav;
 	 		curBehav = behavMap.get(compType);
 	 		if ( curBehav != null ) {
 	 			compVal = curBehav.getValue(ac);
 	 			if ( pkCtrlNotEmpty && idx == pkIdx /*&& "Textbox".equals(compType)*/ ) { // ��� ���������� PK ������ ����������, ��� ���������� ���� �� ���������; ��� �������������� ������ ������ ���������� (�� ������� ��� null-�������(!pkCtrlNotEmpty): ��� �������� ������� �������� ������ ������, ���� ���� ��� �� �������) ! 
					//evalPredicate = fieldVal -> ( StringUtils.isEmpty((String)compVal) || fieldVal != null && ((String)fieldVal).equals((String)compVal) );
 	 				evalPredicate = fieldVal -> curBehav.exactMatch(compVal, fieldVal);
 	 			} else {
					evalPredicate = fieldVal -> curBehav.theFilter(compVal, fieldVal);
				}
 	 		} else {
				compVal = fieldName = getterName = "";
				logger.error("Unsupported element '{}' of type '{}' in prepareToApply()", ac, compType);
				throw new IllegalStateException("Unsupported element '{"+ac+"}' of type '{"+compType+"}' in prepareToApply()");
 	 		}
			
 			/*switch(ac.getClass().getSimpleName()) {
				case "Intbox":
// getValue() == null ��� ������� Intbox (0 �.�. ���������� ���������, � intValue() �� ��������� 0 � null) !!!
					compVal = ((Intbox)ac).getValue(); // Integer getValue(), might be null; int intValue(): If null, zero is returned
// ���� ������ �.�. ��� int, ��� � Integer (nullable !)
					evalPredicate = fieldVal -> ( compVal == null || compVal.equals(fieldVal) ); // ������ ����������
					break;
 				case "Checkbox":
 					getterName = ""; // �� �������, �� ����� ����� "may not be initialized"
 					try {
 						if ("selFilterCHB".equals(compId)) { // ����������� ���� GridData.sel
 							getterName = "isSel";
 							fieldName = "sel";
 							getterMethod = GridData.class.getMethod(getterName);
 						} else {
// 							fieldName = compId.substring(1, compId.length()-3); // *CHB
 							getterName = "is" + StringUtils.capitalize(fieldName);
// 							fieldName = compId.substring(0, 1) + fieldName;
 							getterMethod = beanClass.getMethod(getterName);
 						}
 					} catch (NoSuchMethodException e) {
 						logger.error("prepareToApply. NoSuchMethodException on invoke '"+getterName+"' for fieldName '"+fieldName+"'. See (JavaBean) Rule for control of type '"+ac.getClass().getSimpleName()+"'. beanClass: "+beanClass.getName()+", compId: "+compId, e);
 						throw new InternalError("prepareToApply. NoSuchMethodException on invoke '"+getterName+"' for fieldName '"+fieldName+"'. See (JavaBean) Rule for control of type '"+ac.getClass().getSimpleName()+"'. beanClass: "+beanClass.getName()+", compId: "+compId, e);
 					}
 					compVal = Boolean.valueOf( ((Checkbox)ac).isChecked() ); // Boolean (not null)
 					evalPredicate = fieldVal -> ( compVal.equals(Boolean.FALSE) || compVal.equals(fieldVal) );
 					break;
 				case "Textbox":
 					compVal = ((Textbox)ac).getValue(); // String (not null)
// TODO: (low) ����������� regex ������ contains
 					if (idx == pkIdx) // ��� PK ������ ���������� ! 
 						evalPredicate = fieldVal -> ( StringUtils.isEmpty((String)compVal) || fieldVal != null && ((String)fieldVal).equals((String)compVal) );
 					else evalPredicate = fieldVal -> ( StringUtils.isEmpty((String)compVal) || fieldVal != null && ((String)fieldVal).toUpperCase().contains(((String)compVal).toUpperCase()) );
 					break;
 				case "Combobox":
			  		Set<String> comboSelection = (((Combobox)ac).getModel() == null ? null : ((Selectable<String>)((Combobox)ac).getModel()).getSelection());
			  		if (comboSelection != null && !comboSelection.isEmpty())
			  			compVal = comboSelection.iterator().next();
			  		else compVal = null; // ���� �������������� ������ ���
//			  		compVal = ((InputElement)ac).getText(); // nullable ?
 					evalPredicate = fieldVal -> (
 							StringUtils.isEmpty((String)compVal)
 							|| "<all>".equals(compVal)
 							|| compVal.equals(fieldVal)
 							|| "<null>".equals(compVal) && StringUtils.isBlank((String)fieldVal) // ����� �� ������ null, � ��� ������ (whitespace, empty ("") or null) !
 							|| "<notnull>".equals(compVal) && !StringUtils.isBlank((String)fieldVal)
 					);
 					break;
 				case "BetweenFilterMacro":
 					// ��� ��������������� ����� offline-������, �.�. ������ ������� "����� � ������"; ����� �� ����� ������ �������� ����� ���� ��� ������
					//compVal = ((BetweenFilterMacro<?,?>)ac).getText();
//					evalPredicate = (Comparable<?> fieldVal) -> ((BetweenFilterMacro<?,?>)ac).isEmpty() || ((BetweenFilterMacro<?,Comparable<?>>)ac).isValBetween(fieldVal); // online read
 					compVal = ((BetweenFilterMacro<?,Comparable<? super Object>>)ac).getValue();
 					//evalPredicate = ((BetweenFilterMacro<?,Comparable<Object>>.PairedValue)compVal)::isValBetween;
 					evalPredicate = fieldVal -> ((BetweenFilterMacro<?,Comparable<? super Object>>.PairedValue)compVal).isValBetween((Comparable<? super Object>)fieldVal);
// FIXME: ??? ��� ������������ ������ �������� ��������� ������� [isValBetweenByComponentId(null, ...)] == false ??? �������� �������� ������ ���� NVL � ���� �� ����� ???
// FIXME: ??? ���� ���� ������������� ������ null (��������, ���������� ������������); �������� ������ "���������� ������" �� ����� ???
 					break;
 				default:
 					compVal = fieldName = getterName = "";
 					logger.error("Unsupported element '{}' of type '{}' in filter.prepareToApply()", ac, ac.getClass().getSimpleName());
 					throw new IllegalStateException("Unsupported element '{"+ac+"}' of type '{"+ac.getClass().getSimpleName()+"}' in filter.prepareToApply()");
 			} // switch*/ 			
 			
 			
 			/*if ( getterMethod == null && !StringUtils.isBlank(getterName) ) { // ����� Checkbox
				try {
					getterMethod = beanClass.getMethod(getterName);
				} catch (NoSuchMethodException e) {
					logger.error("prepareToApply. NoSuchMethodException on invoke '"+getterName+"' for fieldName '"+fieldName+"'. See (JavaBean) Rule for control of type '"+ac.getClass().getSimpleName()+"'. beanClass: "+beanClass.getName()+", compId: "+compId, e);
					throw new InternalError("prepareToApply. NoSuchMethodException on invoke '"+getterName+"' for fieldName '"+fieldName+"'. See (JavaBean) Rule for control of type '"+ac.getClass().getSimpleName()+"'. beanClass: "+beanClass.getName()+", compId: "+compId, e);
				}
 			}*/
// TODO: ������ ����� ������ �������� � ���� ������ ��������� �������
 			Class<?> fieldType = getterMethod.getReturnType();
 			logger.trace("prepareToApply.  filter_control # {}:  compId: '{}', compVal: '{}', control_type: '{}', curBehav: '{}', fieldName: '{}', fieldType: '{}', getterName: '{}', beanClass: '{}', pkFieldName: '{}', pkIdx: {}, pkCtrlNotEmpty: {}", idx, compId, compVal, compType, curBehav.getClass().getName(), fieldName, fieldType.getName(), getterName, beanClass.getName(), (pkFieldName.isPresent() ? pkFieldName.get() : "<PK_undef>"), pkIdx, pkCtrlNotEmpty);
 			filterFields[idx] = /*this.*/new FilterField(compId, fieldName, getterName, compVal, getterMethod, evalPredicate);
 		} // for
	} // public void prepareToApply()
	
	
	/** True ���� ��� (���������������) ���� GridData ������������� �������� �������, ��������� �� ��������� (filterComponentArray).
	 * ��� ������������� ��������� ����� (PK) � �������� ��������������� ������-�������� ���������� �������� ����������� �� ���� ������� (��������� ������� � ������ �� �����������).
	 * <p><b>����� ������ ������ �� ������� ����-������ ���������� �������� ��������� ����� prepareToApply(), ������� ������
	 *  ��������, ������������ � ������ ��������� ������� (������, � GridData), ������� ������ �������� FilterField
	 *  (Method �������, ������ (�������� ��� ��������� �������� ���� � ��������) � �.�. ��� ������ �����������
	 *  ������� ������� ���� ���������� ������ (�������� GridData).</b>
     * <p>�������� ��������� �������-��������� curGridData (������ ���� ��������), � ���������, ������������� ������� ����� ����������� ������� ������ �����������.
     * @param curGridData ������ ������ ��� �������� ������ �������, ��������� �������� ������� � {@link #prepareToApply()}
     * @return ������ ���� ������ �������� ������.
     * @exception NullPointerException ��� ������� ���������, IllegalStateException ���� �� ��� �������� prepareToApply.
	 */
	public boolean evalGridData(GridData<?> curGridData) {
		if (filterFields == null || filterFields.length != filterComponentArray.length) {
			logger.error("filterFields not initialized. prepareToApply should be executed befor evalGridData.");
			throw new IllegalStateException("filterFields not initialized. prepareToApply should be executed befor evalGridData.");
		}
		if (curGridData == null) {
			logger.error("Illegal null parameter in evalGridData");
			throw new NullPointerException("Illegal null parameter in evalGridData");
		}
		boolean ret_res = true
			   ,pk_res = false; // ������� PK �������� ������ (������� ������ ��� pkCtrlNotEmpty == true) -> ������������ ������ �� PK
		int idx = -1;
		Object curSubj = curGridData.getBean();
		for (FilterField ff : filterFields/*AbstractComponent ac : filterComponentArray*/) {
			++idx;
			//String compId = ac.getId();
			boolean loc_res = false;
			if ("selFilterCHB".equals(ff.getCompId()/*compId*/)) { // ����������� ���� GridData.sel
				loc_res = ff.eval(curGridData);
			} else {
				loc_res = ff.eval(curSubj);
			}
			if (idx == pkIdx) {
				pk_res = loc_res;
				//if (pkCtrlNotEmpty) { ret_res = loc_res; } // ������������ ������: ������ ���� ������ �� ������
			}
 			curGridData.setFilterFlagsBit(idx, loc_res); // TODO: �������������� - ������������
 			if (!loc_res && !pkCtrlNotEmpty) ret_res = false; // ��������� ��� ���� �������, �.�. ����� ���������� ������� �����
			if (logger.isTraceEnabled() && curSubj instanceof SubjSumm && /*( ((SubjSumm)curSubj).getSubj_id() == 1007200 ||*/ ((SubjSumm)curSubj).getSubj_id() == 400 /*)*/ )
				logger.trace("evalGridData. subj_id = "+((SubjSumm)curSubj).getSubj_id()+", idx = "+idx+", ret_res = "+ret_res+", loc_res = "+loc_res+", compId = '"+ff.getCompId()+"', getterName = '"+ff.getGetterName()+"', compVal = '"+ff.getCompVal()+"', fieldVal = '"+ff.getFieldVal()/*+"', pkFieldName: '"+pkFieldName*/+"', pkIdx = "+pkIdx+", pkCtrlNotEmpty = "+pkCtrlNotEmpty+", pk_res = "+pk_res+", filterFlags = "+Integer.toBinaryString(curGridData.getFilterFlags())+", fieldName = '"+ff.getFieldName()+"', beanClass: "+curSubj.getClass().getName() );
 		} // for
		if (pkCtrlNotEmpty) {ret_res = pk_res;} // PK ����� ������������� ���� �� ���������� ������ ���� �� ����� !
// TODO: ��������� ���� ?: curGridData.setInFilter(ret_res);
		return ret_res;
	} // public boolean evalGridData(GridData curGridData)
	
} // public class GridDataFilter