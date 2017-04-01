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


// FIXME: ? обеспечить неизменность данных (хотя бы колонки ПК) ?
// FIXME: ? где практически использовать ПК ?

/* TODO: Структура "ключа" (внутренний класс): наименование поля, тип поля, экземпляр компаратора (ленивый),
тип (Pk, Fk, Index), класс родителя (Fk), название родительского поля (Fk). -Поддержка составных ключей ?
Компаратор получаем по первому требованию, список ключей сразу (-от бина) (?). Бин определяет только нестандартные
компараторы, иначе (метод ниже ничего не вернул и тип поля Comparable) генерим экземпляр внутреннего класс (метод
setName получает имя поля, определяет геттер, compare использует его для получения значений, которые сравнивает
через compareTo)
*/	


/** Реализация интерфейса GridDataProvider - поставщика list of GridData, расширенная концепцией Первичного Ключа данных (PK).
 * При установлении ПК список Провайдера автоматически сортируется по ПК, что обеспечивает быстрый поиск (если список RandomAccess).
 * Конкретный класс должен реализовать определённый здесь абстрактный защищённый метод populateGridDataList() наполнения
 *  списка, который вызывается в getGridDataList() - геттере ленивого self-incapsulated fixed-size List<GridData<T>>.
 * Реализация должна явно задать класс бина (доменного объекта) для его оболочки GridData через конструктор с единственным параметром типа Class<T>.
 */
public abstract class GridDataProviderWPk<T extends Object & Serializable & Comparable<? super T>>
		implements GridDataProvider<T>, Serializable {
	private static final long serialVersionUID = -7374539983440924433L;

	private static final Logger logger = LoggerFactory.getLogger(GridDataProviderWPk.class);
	
	protected final Class<T> beanClass;
	
	/** Lazy init self-incapsulated fixed-size (set & sord enabled).
	 * Создаётся абстрактным методом populateGridDataList() при первом вызове getGridDataList();
	 *  в нём же определяется конткретный тип списка.
	 */
	private List<GridData<T>> gridDataArrayList;
	
	protected int totalRowCount;
	
	/** Название поля - первичного ключа (or null, тогда сортируем по GridData.uid).
	 * Консистентность с компаратором должна обеспечиваться при установлении ПК в {@link #setPk(String, Comparator)}.
	 */
	protected String pk;
	
	/** Компаратор бинов в соответствии с установленным ПК. */
	protected Comparator<T> beanPkComparator;
	
	/** Компаратор (ленивый) GridData. Делегирует сравнение компаратору бинов при установленном ПК, иначе сравнивает по GridData.uid. */
	protected Comparator<GridData<T>> pkComparator;
	
	/** Название геттера поля ПК бина (определяется в setPk()). */
	protected String pkGetterName;
	
	/** Метод геттера поля ПК бина (определяется в setPk()). */
	protected Method pkGetterMethod;
	
	/** Тип данных поля первичного ключа (определяется в setPk()). */
	protected Class<?> pkClass; // Class<Comparable<?>>
	
	/** Грузить ли полный список при запросе подмножества по ключу. Иначе запрашивается из БД. */
	protected boolean loadAllForRange = true;
	
	
	/** Реализация Провайдера должна явно задать класс бина (доменного объекта) для его оболочки GridData. */
	protected GridDataProviderWPk(Class<T> beanClass) { // http://stackoverflow.com/questions/260666/can-an-abstract-class-have-a-constructor
		this.beanClass = beanClass;
	}
	
	/** Загружен ли уже полный список (другого и нет). */
	public boolean isAllLoaded() {
		return (gridDataArrayList != null);
	}
	
	/** Установка Первичного Ключа данных и соответствующего компаратора бинов.
	 * Список Провайдера автоматически сортируется по ПК, что обеспечивает быстрый поиск (если список RandomAccess).
	 * Для сброса первичного ключа оба параметра должны быть null, тогда список будет отсортирован по GridData.uid.
	 */
// FIXME: валидация и всё такое (типы, существование, unique & not null); проверка правильности сортировки !
// FIXME: разделить ПК и неуникальный индекс (разные проверки, ПК используется в композитном фильтре)
	public void setPk(String pk, Comparator<T> beanPkComparator) {
		if (pk != null && beanPkComparator == null) {
			logger.error("setPk. Требуется компаратор для указанного первичного ключа '{}' в setPk", pk);
			throw new IllegalArgumentException("setPk. Требуется компаратор для указанного первичного '"+pk+"' ключа в setPk");
		}
		if (pk == null && beanPkComparator != null) {
			logger.error("setPk. Для сброса первичного ключа оба параметра должны быть null в setPk");
			throw new IllegalArgumentException("setPk. Для сброса первичного ключа оба параметра должны быть null в setPk");
		}
		if (pk == null && this.pk == null || pk != null && pk.equals(this.pk) && beanPkComparator == this.beanPkComparator) {
			logger.debug("setPk.  Ключ не изменился - pk = '{}'", pk);
			return;
		}
		this.pkComparator = null; // он использует beanPkComparator, но он ленив
		this.pk = pk;
		this.beanPkComparator = beanPkComparator;
		if (pk == null) {
			pkGetterName = null;
			pkGetterMethod = null; 
			pkClass = null;
		} else /*if (!(beanPkComparator instanceof GridDataProviderWPk.CompareComparable))*/ { // в конструкторе CompareComparable reflectPk() уже вызван
			reflectPkGetter(pk);
		}
		if (gridDataArrayList != null) { // при установке ПК сортировку откладываем до получения полного списка
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
		// проверить класс: Comparable; int, Integer(Number), String
	}
	
	/** Установка Первичного Ключа данных, компаратора бинов определяется/создаётся автоматически.
	 * Компаратор определяется из класса бина (рефлексивно через статический метод getPkComparatorByName) или
	 * создаётся для Comparable-полей с использованием {@link Comparable#compareTo(Object) compareTo} для сравнения
	 * значений ключевых полей, полученных рефлексивно.
	 */
	public void setPk(String pk) {
		if (pk == null) { // сброс - сортировка по GridData.uid
			setPk(null, null);
			return;
		}
		Comparator<T> cmp = extractComparatorFromBeanClassByPkName(pk);
		if (cmp != null) { // компаратор определён для заданного поля в классе бина
			setPk(pk, cmp);
			return;
		}
		cmp = new CompareComparable();
		setPk(pk, cmp);
	} // public void setPk(String pk)
	
	
	/** Компаратор (ленивый) GridData.
	 * Делегирует сравнение компаратору бинов при установленном ПК, иначе сравнивает по GridData.uid.
	 */
	// используется в дата-провайдере для бинарного поиска в get()
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
	
	/** Метод геттера поля ПК бина (определяется в setPk()). */
	/*public Optional<Method> getPkGetterMethod() {
		return Optional.ofNullable(pkGetterMethod);
	}*/
	
	/** Название геттера поля ПК бина (определяется в setPk()). */
	/*public Optional<String> getPkGetterName() {
		return Optional.ofNullable(pkGetterName);
	}*/
	
	/** Тип данных поля первичного ключа (определяется в setPk()). */
	public Optional<Class<?>> getPkClass() { // Optional<Class<Comparable<?>>>
		return Optional.ofNullable(pkClass);
	}
	
	/** Компаратор GridData, делегирующий сравнение компаратору бинов - beanPkComparator. */
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
	
	/** Компаратор бинов в соответствии с установленным ПК. */
	public Optional<Comparator<T>> getBeanPkComparator() {
		return Optional.ofNullable((Comparator<T>)beanPkComparator);
	}
	
    @Override
	public Class<T> getBeanClass() {
		return beanClass;
	}
    
    
	/** Компаратор бинов, получающий название ключевого поля в конструкторе и использующий для сравнения
	 * compareTo() рефлексивно полученных значений ключевого поля.
	 */
	private final class CompareComparable implements Comparator<T>, Serializable {
		private static final long serialVersionUID = -6151389573231832609L;
		/*String pkFieldName;
		private CompareComparable(String fn) {
			pkFieldName = fn;
			reflectPkGetter(fn); // определяется pkGetterMethod, pkClass
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
	
	
	/** Таблица названий полей Первичных Ключей (только ПК, не всех индексов!), поддерживаемых бином.
	 * (Это часть статического интерфейса бина. NOT null, но м.б. пустой массив.)
	 * @return При любой неудаче возвращаем пустой массив.
	 */
	public @NotNull String[] getPkNames() {
		String[] namesArr = {};
    	Method m = null;
    	try {
// FIXME: получать только один раз (поле экземпляра)
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
	
	
	/** Получить (оригинальный) компаратор по ПК (индексу).
	 * (Это часть статического интерфейса бина. nullable. Компаратор в классе бина имеет смысл реализовывать
	 *  для примитивов и non-Comparable полей.)
	 * @param pkPar Название поля Pk, not null.
	 * @return При любой неудаче возвращаем null.
	 */
    @SuppressWarnings("unchecked")
	private Comparator<T> extractComparatorFromBeanClassByPkName(@NotNull String pkPar) {
    	assert(pkPar != null):"NPE";
    	Method m = null;
    	try {
// FIXME: получать только один раз (поле экземпляра)
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
	
    
    /** Грузить ли полный список при запросе подмножества по ключу (по умолчанию). Иначе запрашивается из БД. */
    public boolean isLoadAllForRange() {
		return loadAllForRange;
	}
	
    /** Грузить ли полный список при запросе подмножества по ключу (по умолчанию). Иначе запрашивается из БД.
     * Чтобы не грузить в память полный список, нужно не вызывать {@link #getAll()} (и {@link #getTotalRowCount},
     *  {@link #setPk}, {@link #get}, {@link #getByPk}, {@link #indexOf}) и сбросить флаг loadAllForRange до первого вызова {@link #getRange(Object) getRange}.
     */
	public void setLoadAllForRange(boolean loadAllForRange) {
		this.loadAllForRange = loadAllForRange;
	}
	
    /** Метод должен создавать (желательно RandomAccess), наполнять и возвращать список объектов GridData<T>, который будет доступен getGridDataList(). Вызывается однажды. */
    protected abstract List<GridData<T>> populateGridDataList(); // run once !
    
    /** {@inheritDoc}
     * Список наполняется в абстрактном методе populateGridDataList() единожды при первом обращении.
     * Возвращает оригинальный список (live, не копию), обёрнутый в {@link ListUtils#fixedSizeList(List)}.
     */
	@Override
	public List<GridData<T>> getAll() { // lazy; wrap the list returned populateGridDataList() a fixed-dimension list through ListUtils.FixedSizeList<GridData<T>>
//			return new ArrayList<GridData>(gridDataArrayList);
		if (gridDataArrayList == null) {
			gridDataArrayList = ListUtils.fixedSizeList(populateGridDataList()); // vs Collections.unmodifiableList() prohibits set(), sort()
			totalRowCount = gridDataArrayList.size();
			if (true/*pk != null*/) { // при установке ПК сортировку откладываем до получения полного списка; изначально устанавливаем сортировку по uid
				gridDataArrayList.sort(getPkComparator());
				logger.trace("getAll.  Отсортировали список '{}' по ключу '{}' при первом получении", beanClass.getSimpleName(), pk);
			}
		}
		if (gridDataArrayList == null) {
			logger.error("getGridDataList. Внутренняя ошибка: populateGridDataList() вернул пустой gridDataArrayList");
			throw new IllegalStateException("getGridDataList. Внутренняя ошибка: populateGridDataList() вернул пустой gridDataArrayList");
		}
		return gridDataArrayList; // live !
	}
	
	/** {@inheritDoc} Если полный список не загружен, получаем запросом к БД (требуется реализация {@link #selectRowCount()}). */
	@Override
	public int getTotalRowCount() { // cached size of gridDataArrayList
		if (gridDataArrayList == null) {
			totalRowCount = selectRowCount();
		}
		return totalRowCount/*getAll().size()*/;
	}
	
	/** Получить кол-во строк из БД. Вызывается из {@link #getTotalRowCount()} если полный список не сформирован.
	 * Необязателен к переопределению, в таком случае возвращает 0.
	 */
	protected int selectRowCount() {return 0;}
	
	/** Название поля Первичного Ключа если PK установлен. */
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
	
	/** Быстрый (in RandomAccess case) поиск в отсортированном по полю PK (- авто при установке ПК) списке с использованием компаратора доменного класса по первичному ключу.
	 * @see Collections#binarySearch(List, Object, Comparator)
	 */
	@Override
	public int indexOf(GridData<T> subj) {
		return Collections.binarySearch(getAll(), subj, this.getPkComparator());
	};
    
	
	/** Быстрый (in RandomAccess case) поиск по значению Первичного Ключа.
	 * ПК по соответствующему полю должен быть предварительно установлен вызовом {@link #setPk(String, Comparator) setPk()}.
	 * @return Единственный элемент со значением ключевого поля равным параметру. Или null.
	 */
// TESTME: не используется
	public <U extends Object & Comparable<? super U>> GridData<T> getByPk(U pkVal) {
    	if (pk == null && beanPkComparator == null) {
    		logger.error("getByPk. Требуется предварительная установка первичного ключа");
    		throw new UnsupportedOperationException("getByPk. Требуется предварительная установка первичного ключа");
    	}
    	if (pkVal == null) {
    		logger.error("getByPk. Пустое значение первичного ключа (pkVal) недопустимо");
    		throw new NullPointerException("getByPk. Пустое значение первичного ключа (pkVal) недопустимо");
    	}
    	if (!pkVal.getClass().equals(pkClass)) {
    		logger.error("getByPk. Argument type '{}' should be same as pkClass = '{}'", pkVal.getClass().getName(), pkClass.getName());
    		throw new IllegalArgumentException("getByPk. Argument type '"+pkVal.getClass().getName()+"' should be same as pkClass = '"+pkClass.getName()+"'");
    	}
// FIXME: см. Collections.binarySearch: if (getGridDataList() instanceof RandomAccess || getGridDataList().size() < BINARYSEARCH_THRESHOLD(= 5000))
    	int ind = indexedBinarySearchByPk(getAll(), 0, getAll().size(), pkVal);
    	if (ind < 0) {
    		return null;
    	}
		return getAll().get(ind);
	} // getByPk
	
//	public GridData<T> getByIntPk(int pkIntVal)
	
	/** Быстрый поиск по значению ПК в отсортированном по ПК списке.
	 * @param <U> Тип значения поля ПК.
	 */
// вызывается из getByPk (не исп-ся), getRange
// на основе Collections.indexedBinarySearch()
	@SuppressWarnings("unchecked")
	public <U extends Object & Comparable<? super U>>
	  int indexedBinarySearchByPk(List<GridData<T>> a, int fromIndex, int toIndex, U key) {
// FIXME: проверять на RandomAccess
		U midVal = null;
		int low = 0;
		int high = toIndex - 1;
		while (low <= high) {
			int mid = (low + high) >>> 1;
			try {
				midVal = (U)pkGetterMethod.invoke(a.get(mid).getBean()); // значение ключевого поля доменного объекта (его тип д.б. Comparable !)
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
	
// FIXME: !!! убрать после замеров !!!
	public boolean _use_stream = false;
	
	/** Получить все строки с заданным значением ключевого поля.
	 * Используется полный список или производится запрос к БД через реализацию {@link #selectRange(String, Object) selectRange}.
	 * @return Пустой список, если не найдено.
	 */
	@Override
	public @NotNull <U extends Object & Comparable<? super U>> List<GridData<T>> getRange(U key) { // Comparable
		List<GridData<T>> l = null;
		if (gridDataArrayList == null && !loadAllForRange) { // полный список не загружен, получаем данные из БД
			l = selectRange(pk, key);
			if (l == null) {
				l = new ArrayList<>();
			}
			return l;
		}
// подмножество полного списка
		Predicate<GridData<T>> predic = gdt -> { // сравнить ключевое поле бина заданного GridData с параметром key
			try {
				Object val = pkGetterMethod.invoke(gdt.getBean()); // значение ключевого поля доменного объекта
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
		int ind = indexedBinarySearchByPk(getAll(), 0, getAll().size(), key); // попадание (если) в произвольный элемент нужного диапазона элементов с совпадающим значением ключевого поля
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
	
	/** Получить непосредственно из БД все строки с заданным значением ключевого поля. Вызывается из {@link #getRange(Object) getRange}. */
	protected abstract List<GridData<T>> selectRange(String fieldName, Object key);

	
	/** Вернуть значение текущего ПК-поля частного объекта.
	 * {@link UnsupportedOperationException} если Pk не устанвлен на момент вызова.
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