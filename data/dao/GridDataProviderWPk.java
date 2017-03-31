package basos.data.dao;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import basos.data.GridData;


// FIXME: ? ���������� ������������ ������ (���� �� ������� ��) ?
// FIXME: ? ��� ����������� ������������ �� ?

/* TODO: ��������� "�����" (���������� �����): ������������ ����, ��� ����, ��������� ����������� (�������),
��� (Pk, Fk, Index), ����� �������� (Fk), �������� ������������� ���� (Fk). -��������� ��������� ������ ?
���������� �������� �� ������� ����������, ������ ������ ����� (-�� ����) (?). ��� ���������� ������ �������������
�����������, ����� (����� ���� ������ �� ������ � ��� ���� Comparable) ������� ��������� ����������� ����� (�����
setName �������� ��� ����, ���������� ������, compare ���������� ��� ��� ��������� ��������, ������� ����������
����� compareTo)
*/	


/** ���������� ���������� GridDataProvider - ���������� list of GridData, ����������� ���������� ���������� ����� ������ (PK).
 * ��� ������������ �� ������ ���������� ������������� ����������� �� ��, ��� ������������ ������� ����� (���� ������ RandomAccess).
 * ���������� ����� ������ ����������� ����������� ����� ����������� ���������� ����� populateGridDataList() ����������
 *  ������, ������� ���������� � getGridDataList() - ������� �������� self-incapsulated fixed-size List<GridData<T>>.
 * ���������� ������ ���� ������ ����� ���� (��������� �������) ��� ��� �������� GridData ����� ����������� � ������������ ���������� ���� Class<T>.
 */
public abstract class GridDataProviderWPk<T extends Object & Serializable & Comparable<? super T>>
		implements GridDataProvider<T>, Serializable {
	private static final long serialVersionUID = -7374539983440924433L;

	private static final Logger logger = LoggerFactory.getLogger(GridDataProviderWPk.class);
	
	protected final Class<T> beanClass;
	
	/** Lazy init self-incapsulated fixed-size (set & sord enabled).
	 * �������� ����������� ������� populateGridDataList() ��� ������ ������ getGridDataList();
	 *  � �� �� ������������ ����������� ��� ������.
	 */
	private List<GridData<T>> gridDataArrayList;
	
	protected int totalRowCount;
	
	/** �������� ���� - ���������� ����� (or null, ����� ��������� �� GridData.uid).
	 * ��������������� � ������������ ������ �������������� ��� ������������ �� � {@link #setPk(String, Comparator)}.
	 */
	protected String pk;
	
	/** ���������� ����� � ������������ � ������������� ��. */
	protected Comparator<T> beanPkComparator;
	
	/** ���������� (�������) GridData. ���������� ��������� ����������� ����� ��� ������������� ��, ����� ���������� �� GridData.uid. */
	protected Comparator<GridData<T>> pkComparator;
	
	/** �������� ������� ���� �� ���� (������������ � setPk()). */
	protected String pkGetterName;
	
	/** ����� ������� ���� �� ���� (������������ � setPk()). */
	protected Method pkGetterMethod;
	
	/** ��� ������ ���� ���������� ����� (������������ � setPk()). */
	protected Class<?> pkClass; // Class<Comparable<?>>
	
	/** ������� �� ������ ������ ��� ������� ������������ �� �����. ����� ������������� �� ��. */
	protected boolean loadAllForRange = true;
	
	
	/** ���������� ���������� ������ ���� ������ ����� ���� (��������� �������) ��� ��� �������� GridData. */
	protected GridDataProviderWPk(Class<T> beanClass) { // http://stackoverflow.com/questions/260666/can-an-abstract-class-have-a-constructor
		this.beanClass = beanClass;
	}
	
	/** �������� �� ��� ������ ������ (������� � ���). */
	public boolean isAllLoaded() {
		return (gridDataArrayList != null);
	}
	
	/** ��������� ���������� ����� ������ � ���������������� ����������� �����.
	 * ������ ���������� ������������� ����������� �� ��, ��� ������������ ������� ����� (���� ������ RandomAccess).
	 * ��� ������ ���������� ����� ��� ��������� ������ ���� null, ����� ������ ����� ������������ �� GridData.uid.
	 */
// FIXME: ��������� � �� ����� (����, �������������, unique & not null); �������� ������������ ���������� !
// FIXME: ��������� �� � ������������ ������ (������ ��������, �� ������������ � ����������� �������)
	public void setPk(String pk, Comparator<T> beanPkComparator) {
		if (pk != null && beanPkComparator == null) {
			logger.error("setPk. ��������� ���������� ��� ���������� ���������� ����� '{}' � setPk", pk);
			throw new IllegalArgumentException("setPk. ��������� ���������� ��� ���������� ���������� '"+pk+"' ����� � setPk");
		}
		if (pk == null && beanPkComparator != null) {
			logger.error("setPk. ��� ������ ���������� ����� ��� ��������� ������ ���� null � setPk");
			throw new IllegalArgumentException("setPk. ��� ������ ���������� ����� ��� ��������� ������ ���� null � setPk");
		}
		if (pk == null && this.pk == null || pk != null && pk.equals(this.pk) && beanPkComparator == this.beanPkComparator) {
			logger.debug("setPk.  ���� �� ��������� - pk = '{}'", pk);
			return;
		}
		this.pkComparator = null; // �� ���������� beanPkComparator, �� �� �����
		this.pk = pk;
		this.beanPkComparator = beanPkComparator;
		if (pk == null) {
			pkGetterName = null;
			pkGetterMethod = null; 
			pkClass = null;
		} else /*if (!(beanPkComparator instanceof GridDataProviderWPk.CompareComparable))*/ { // � ������������ CompareComparable reflectPk() ��� ������
			reflectPkGetter(pk);
		}
		if (gridDataArrayList != null) { // ��� ��������� �� ���������� ����������� �� ��������� ������� ������
			getAll().sort(getPkComparator());
		}
		logger.debug("{}.setPk. PK: {}, BeanPkComparator: {}, PkClass: {}, PkComparator: {}, extractComparatorFromBeanClassByPkName: {}", this.getClass().getName(), getPk().orElse("<PK_no_defined>"), (getBeanPkComparator().isPresent() ? getBeanPkComparator().get().getClass().getName() : "<none>"), (getPkClass().isPresent() ? getPkClass().get().getName() : "<none>"), getPkComparator().getClass().getName(), (getPk().isPresent() ? extractComparatorFromBeanClassByPkName(pk) : null) );
	} // void setPk(String pk, Comparator<T> beanPkComparator)
	
	private void reflectPkGetter(String pkn) {
		pkGetterName = "get" + StringUtils.capitalize(pkn);
		try {
			pkGetterMethod = beanClass.getMethod(pkGetterName);
		} catch (NoSuchMethodException e) {
			logger.error("reflectPk. Exception on invoke {}", pkGetterName, e);
			throw new InternalError("reflectPk. Exception on invoke "+pkGetterName, e);
		}
		pkClass = pkGetterMethod.getReturnType();
		// ��������� �����: Comparable; int, Integer(Number), String
	}
	
	/** ��������� ���������� ����� ������, ����������� ����� ������������/�������� �������������.
	 * ���������� ������������ �� ������ ���� (����������� ����� ����������� ����� getPkComparatorByName) ���
	 * �������� ��� Comparable-����� � �������������� {@link Comparable#compareTo(Object) compareTo} ��� ���������
	 * �������� �������� �����, ���������� �����������.
	 */
	public void setPk(String pk) {
		if (pk == null) { // ����� - ���������� �� GridData.uid
			setPk(null, null);
			return;
		}
		Comparator<T> cmp = extractComparatorFromBeanClassByPkName(pk);
		if (cmp != null) { // ���������� �������� ��� ��������� ���� � ������ ����
			setPk(pk, cmp);
			return;
		}
		cmp = new CompareComparable();
		setPk(pk, cmp);
	} // public void setPk(String pk)
	
	
	/** ���������� (�������) GridData.
	 * ���������� ��������� ����������� ����� ��� ������������� ��, ����� ���������� �� GridData.uid.
	 */
	// ������������ � ����-���������� ��� ��������� ������ � get()
	public Comparator<GridData<T>> getPkComparator() {
		if (pkComparator == null) {
			if (pk != null)
				pkComparator = new CompareByPK();
			else {
				pkComparator = GridData.getCompareByUid();
			}
		}
		return pkComparator;
	} // public Comparator<GridData<T>> getPkComparator()
	
	/** ����� ������� ���� �� ���� (������������ � setPk()). */
	/*public Optional<Method> getPkGetterMethod() {
		return Optional.ofNullable(pkGetterMethod);
	}*/
	
	/** �������� ������� ���� �� ���� (������������ � setPk()). */
	/*public Optional<String> getPkGetterName() {
		return Optional.ofNullable(pkGetterName);
	}*/
	
	/** ��� ������ ���� ���������� ����� (������������ � setPk()). */
	public Optional<Class<?>> getPkClass() { // Optional<Class<Comparable<?>>>
		return Optional.ofNullable(pkClass);
	}
	
	/** ���������� GridData, ������������ ��������� ����������� ����� - beanPkComparator. */
	private final class CompareByPK implements Comparator<GridData<T>>, Serializable { // https://docs.oracle.com/javase/8/docs/api/java/util/Comparator.html#compare-T-T-
		private static final long serialVersionUID = 6011792781710678748L;
		public int compare(GridData<T> o1, GridData<T> o2) {
			if (o1 == null || o2 == null) {
				logger.error("CompareByPK.compare. null argument: o1 = {}, o2 = {}", o1, o2);
				throw new NullPointerException("CompareByPK.compare. null argument: o1 = "+o1+", o2 = "+o2);
			}
			return beanPkComparator.compare(o1.getBean(), o2.getBean());
		}
	}
	
	/** ���������� ����� � ������������ � ������������� ��. */
	public Optional<Comparator<T>> getBeanPkComparator() {
		return Optional.ofNullable((Comparator<T>)beanPkComparator);
	}
	
    @Override
	public Class<T> getBeanClass() {
		return beanClass;
	}
    
    
	/** ���������� �����, ���������� �������� ��������� ���� � ������������ � ������������ ��� ���������
	 * compareTo() ����������� ���������� �������� ��������� ����.
	 */
	private final class CompareComparable implements Comparator<T>, Serializable {
		private static final long serialVersionUID = -6151389573231832609L;
		/*String pkFieldName;
		private CompareComparable(String fn) {
			pkFieldName = fn;
			reflectPkGetter(fn); // ������������ pkGetterMethod, pkClass
		}*/
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public int compare(T o1, T o2) {
			if (o1 == null || o2 == null) {
				logger.error("CompareComparable.compare. null argument: o1 = {}, o2 = {}", o1, o2);
				throw new NullPointerException("CompareComparable.compare. null argument: o1 = "+o1+", o2 = "+o2);
			}
			Comparable val1 = null, val2 = null;
	    	try {
				val1 = (Comparable)pkGetterMethod.invoke(o1);
				val2 = (Comparable)pkGetterMethod.invoke(o2);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				logger.error("CompareComparable. error on invoke method '{}'", pkGetterName, e);
			} catch (ClassCastException e) {
				logger.error("CompareComparable. error on cast Pk '{}' to Comparable", pk, e);
			}
			if (val1 == null || val2 == null) {
				logger.error("CompareComparable.compare. null Pk value: val1 = {}, val2 = {}", val1, val2);
				throw new NullPointerException("CompareComparable.compare. null Pk value: val1 = "+val1+", val2 = "+val2);
			}
			return val1.compareTo(val2);
		} // public int compare(T o1, T o2)
	} // private final class CompareComparable implements Comparator<T>, Serializable
	
	
	/** ������� �������� ����� ��������� ������ (������ ��, �� ���� ��������!), �������������� �����.
	 * (��� ����� ������������ ���������� ����. NOT null, �� �.�. ������ ������.)
	 * @return ��� ����� ������� ���������� ������ ������.
	 */
	public @NotNull String[] getPkNames() {
		String[] namesArr = {};
    	Method m = null;
    	try {
// FIXME: �������� ������ ���� ��� (���� ����������)
			m = beanClass.getMethod("getPkNames");
		} catch (NoSuchMethodException | SecurityException e) {
			logger.error("getPkNames. error on getMethod", e);
		}
    	if (m == null) {
    		logger.error("getPkNames. static method 'getPkNames' not found in beanClass '{}'", beanClass.getName());
    		//throw new
    		return namesArr;
    	}
    	Object val = null;
    	try {
			val = m.invoke(beanClass); // static
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			logger.error("getPkNames. error on invoke method", e);
		}
    	if ( val == null || !val.getClass().isArray() ) {
    		logger.error("getPkNames. static method 'getPkNames' returned wrong type value '{}' in beanClass '{}'", val, beanClass.getName());
    		//throw new
    		return namesArr;
    	}
    	namesArr = (String[])val;
		return namesArr;
	} // public String[] getPkNames()
	
	
	/** �������� (������������) ���������� �� �� (�������).
	 * (��� ����� ������������ ���������� ����. nullable. ���������� � ������ ���� ����� ����� �������������
	 *  ��� ���������� � non-Comparable �����.)
	 * @param pkPar �������� ���� Pk, not null.
	 * @return ��� ����� ������� ���������� null.
	 */
    @SuppressWarnings("unchecked")
	private Comparator<T> extractComparatorFromBeanClassByPkName(@NotNull String pkPar) {
    	assert(pkPar != null):"NPE";
    	Method m = null;
    	try {
// FIXME: �������� ������ ���� ��� (���� ����������)
			m = beanClass.getMethod("getPkComparatorByName", String.class);
		} catch (NoSuchMethodException | SecurityException e) {
			logger.error("extractComparatorFromBeanClassByPkName. error on getMethod", e);
			return null;
		}
    	Object cmp = null;
    	try {
			cmp = m.invoke(beanClass, pkPar); // static
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			logger.error("extractComparatorFromBeanClassByPkName. error on invoke method", e);
		}
    	if ( cmp == null || !(cmp instanceof Comparator<?>) ) {
    		return null;
    	}
    	return (Comparator<T>)cmp;
    } // public Comparator<T> extractComparatorFromBeanClassByPkName(String pkPar)
	
    
    /** ������� �� ������ ������ ��� ������� ������������ �� ����� (�� ���������). ����� ������������� �� ��. */
    public boolean isLoadAllForRange() {
		return loadAllForRange;
	}
	
    /** ������� �� ������ ������ ��� ������� ������������ �� ����� (�� ���������). ����� ������������� �� ��.
     * ����� �� ������� � ������ ������ ������, ����� �� �������� {@link #getAll()} (� {@link #getTotalRowCount},
     *  {@link #setPk}, {@link #get}, {@link #getByPk}, {@link #indexOf}) � �������� ���� loadAllForRange �� ������� ������ {@link #getRange(Object) getRange}.
     */
	public void setLoadAllForRange(boolean loadAllForRange) {
		this.loadAllForRange = loadAllForRange;
	}
	
    /** ����� ������ ��������� (���������� RandomAccess), ��������� � ���������� ������ �������� GridData<T>, ������� ����� �������� getGridDataList(). ���������� �������. */
    protected abstract List<GridData<T>> populateGridDataList(); // run once !
    
    /** {@inheritDoc}
     * ������ ����������� � ����������� ������ populateGridDataList() �������� ��� ������ ���������.
     * ���������� ������������ ������ (live, �� �����), �������� � {@link ListUtils#fixedSizeList(List)}.
     */
	@Override
	public List<GridData<T>> getAll() { // lazy; wrap the list returned populateGridDataList() a fixed-dimension list through ListUtils.FixedSizeList<GridData<T>>
//			return new ArrayList<GridData>(gridDataArrayList);
		if (gridDataArrayList == null) {
			gridDataArrayList = ListUtils.fixedSizeList(populateGridDataList()); // vs Collections.unmodifiableList() prohibits set(), sort()
			totalRowCount = gridDataArrayList.size();
			if (true/*pk != null*/) { // ��� ��������� �� ���������� ����������� �� ��������� ������� ������; ���������� ������������� ���������� �� uid
				gridDataArrayList.sort(getPkComparator());
				logger.trace("getAll.  ������������� ������ '{}' �� ����� '{}' ��� ������ ���������", beanClass.getSimpleName(), pk);
			}
		}
		if (gridDataArrayList == null) {
			logger.error("getGridDataList. ���������� ������: populateGridDataList() ������ ������ gridDataArrayList");
			throw new IllegalStateException("getGridDataList. ���������� ������: populateGridDataList() ������ ������ gridDataArrayList");
		}
		return gridDataArrayList; // live !
	}
	
	/** {@inheritDoc} ���� ������ ������ �� ��������, �������� �������� � �� (��������� ���������� {@link #selectRowCount()}). */
	@Override
	public int getTotalRowCount() { // cached size of gridDataArrayList
		if (gridDataArrayList == null) {
			totalRowCount = selectRowCount();
		}
		return totalRowCount/*getAll().size()*/;
	}
	
	/** �������� ���-�� ����� �� ��. ���������� �� {@link #getTotalRowCount()} ���� ������ ������ �� �����������.
	 * ������������ � ���������������, � ����� ������ ���������� 0.
	 */
	protected int selectRowCount() {return 0;}
	
	/** �������� ���� ���������� ����� ���� PK ����������. */
	public Optional<String> getPk() {
		return Optional.ofNullable(pk);
	}
	
	/** {@inheritDoc}
	 * @see java.util.List#get(int) List.get
	 */
	@Override
	public GridData<T> get(int idx) {
		return getAll().get(idx);
	}
	
	/** ������� (in RandomAccess case) ����� � ��������������� �� ���� PK (- ���� ��� ��������� ��) ������ � �������������� ����������� ��������� ������ �� ���������� �����.
	 * @see Collections#binarySearch(List, Object, Comparator)
	 */
	@Override
	public int indexOf(GridData<T> subj) {
		return Collections.binarySearch(getAll(), subj, this.getPkComparator());
	};
    
	
	/** ������� (in RandomAccess case) ����� �� �������� ���������� �����.
	 * �� �� ���������������� ���� ������ ���� �������������� ���������� ������� {@link #setPk(String, Comparator) setPk()}.
	 * @return ������������ ������� �� ��������� ��������� ���� ������ ���������. ��� null.
	 */
// TESTME: �� ������������
	public <U extends Object & Comparable<? super U>> GridData<T> getByPk(U pkVal) {
    	if (pk == null && beanPkComparator == null) {
    		logger.error("getByPk. ��������� ��������������� ��������� ���������� �����");
    		throw new UnsupportedOperationException("getByPk. ��������� ��������������� ��������� ���������� �����");
    	}
    	if (pkVal == null) {
    		logger.error("getByPk. ������ �������� ���������� ����� (pkVal) �����������");
    		throw new NullPointerException("getByPk. ������ �������� ���������� ����� (pkVal) �����������");
    	}
    	if (!pkVal.getClass().equals(pkClass)) {
    		logger.error("getByPk. Argument type '{}' should be same as pkClass = '{}'", pkVal.getClass().getName(), pkClass.getName());
    		throw new IllegalArgumentException("getByPk. Argument type '"+pkVal.getClass().getName()+"' should be same as pkClass = '"+pkClass.getName()+"'");
    	}
// FIXME: ��. Collections.binarySearch: if (getGridDataList() instanceof RandomAccess || getGridDataList().size() < BINARYSEARCH_THRESHOLD(= 5000))
    	int ind = indexedBinarySearchByPk(getAll(), 0, getAll().size(), pkVal);
    	if (ind < 0) {
    		return null;
    	}
		return getAll().get(ind);
	} // getByPk
	
//	public GridData<T> getByIntPk(int pkIntVal)
	
	/** ������� ����� �� �������� �� � ��������������� �� �� ������.
	 * @param <U> ��� �������� ���� ��.
	 */
// ���������� �� getByPk (�� ���-��), getRange
// �� ������ Collections.indexedBinarySearch()
	@SuppressWarnings("unchecked")
	public <U extends Object & Comparable<? super U>>
	  int indexedBinarySearchByPk(List<GridData<T>> a, int fromIndex, int toIndex, U key) {
// FIXME: ��������� �� RandomAccess
		U midVal = null;
		int low = 0;
		int high = toIndex - 1;
		while (low <= high) {
			int mid = (low + high) >>> 1;
			try {
				midVal = (U)pkGetterMethod.invoke(a.get(mid).getBean()); // �������� ��������� ���� ��������� ������� (��� ��� �.�. Comparable !)
			} catch ( IllegalAccessException | IllegalArgumentException | java.lang.reflect.InvocationTargetException e) {
				logger.error("indexedBinarySearchByPk. Exception on invoke {}", pkGetterName, e);
				throw new InternalError("indexedBinarySearchByPk. Exception on invoke "+pkGetterName, e);
			} catch (ClassCastException e) {
				
			}
			if (midVal == null) {
				
			}
			int cmp = midVal.compareTo(key);
			if (cmp < 0)
				low = mid + 1;
			else if (cmp > 0)
				high = mid - 1;
			else
				return mid; // key found
		} // while
		return -(low + 1);  // key not found
    } // indexedBinarySearchByPk
	
// FIXME: !!! ������ ����� ������� !!!
	public boolean _use_stream = false;
	
	/** �������� ��� ������ � �������� ��������� ��������� ����.
	 * ������������ ������ ������ ��� ������������ ������ � �� ����� ���������� {@link #selectRange(String, Object) selectRange}.
	 * @return ������ ������, ���� �� �������.
	 */
	@Override
	public @NotNull <U extends Object & Comparable<? super U>> List<GridData<T>> getRange(U key) { // Comparable
		List<GridData<T>> l = null;
		if (gridDataArrayList == null && !loadAllForRange) { // ������ ������ �� ��������, �������� ������ �� ��
			l = selectRange(pk, key);
			if (l == null) {
				l = new ArrayList<>();
			}
			return l;
		}
// ������������ ������� ������
		Predicate<GridData<T>> predic = gdt -> { // �������� �������� ���� ���� ��������� GridData � ���������� key
			try {
				Object val = pkGetterMethod.invoke(gdt.getBean()); // �������� ��������� ���� ��������� �������
				return val.equals(key); //val.compareTo(key) == 0;
			} catch ( IllegalAccessException | IllegalArgumentException | java.lang.reflect.InvocationTargetException e) {
				logger.error("getRange. Exception on invoke {}", pkGetterName, e);
				throw new InternalError("getRange. Exception on invoke "+pkGetterName, e);
			}
		};
		
	if (_use_stream) {
// v1. stream.filter.collect
		l = getAll().stream().filter(predic).collect(Collectors.toList());
	} else {
// v2. binary search
		int ind = indexedBinarySearchByPk(getAll(), 0, getAll().size(), key); // ��������� (����) � ������������ ������� ������� ��������� ��������� � ����������� ��������� ��������� ����
		l = new ArrayList<>();
		if (ind < 0) {
			return l;
		}
		l.add(get(ind));
		GridData<T> tmp;
		int next = ind-1;
		while ( next >= 0 && predic.test(tmp = get(next)) ) {
			l.add(tmp);
			--next;
		}
		next = ind+1;
		while ( next < getAll().size() && predic.test(tmp = get(next)) ) {
			l.add(tmp);
			++next;
		}
	} // v2. binary search
	
		return l;
	}; // public List<GridData<T>> getRange(Object key)
	
	/** �������� ��������������� �� �� ��� ������ � �������� ��������� ��������� ����. ���������� �� {@link #getRange(Object) getRange}. */
	protected abstract List<GridData<T>> selectRange(String fieldName, Object key);

	
	/** ������� �������� �������� ��-���� �������� �������.
	 * {@link UnsupportedOperationException} ���� Pk �� ��������� �� ������ ������.
	 */
	public Object getPkValue(T bean) {
		if (bean == null) {
			logger.error("getPkValue. null bean argument");
			throw new NullPointerException("getPkValue. null bean argument");
		}
		if (pkGetterMethod == null) {
			logger.error("getPkValue. pk undefined");
			throw new UnsupportedOperationException("getPkValue. pk undefined");
		}
		Object val = null;
		try {
			val = pkGetterMethod.invoke(bean);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			logger.error("getPkValue. error on invoke method '{}'", pkGetterName, e);
		}
		return val;
	} // public Object getPkValue(T bean)
	
} // public abstract class GridDataProviderWPk<T extends Object & Serializable & Comparable<? super T>> implements GridDataProvider<T>