package basos.data; 

import java.io.Serializable;
//import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import basos.xe.data.entity.SubjSumm;


/** Обёртка объекта доменного класса для использования в гриде.
 * RULE: все приватные нестатические поля доменного объекта класс beanClass должны относиться к полям данных.
 * Thread-UNSAFE (синхронизация обеспечивается на уровне списка объектов).
 * @param <T> Тип бина (класса доменного объекта).
 */
public class GridData<T extends Object & Serializable & Comparable<? super T>>
		implements Serializable, Comparable<GridData<T>> {

/* DR p.349: The data models, such as ListModel and ChartModel, have to be serializable. Otherwise, the UI
 object (such as grid) won't behave correctly. The implementations provided by ZK are serializable.
 However, the items to be stored in the data models have to be serializable too. */
	private static final long serialVersionUID = -2935354347892417220L;

	private static final Logger logger = LoggerFactory.getLogger(GridData.class);
	
	private static AtomicLong throughoutNum = new AtomicLong(0L); // счётчик объектов
	private final long uid; // порядковый номер созданного GridData
	private final T bean; // доменный объект
	private final Class<T> beanClass;
	private boolean sel; /** крыжик - признак выбора (чекбокс в первой колонке) (false для нового) */
    private boolean inFilter; // (true для нового) TODO: inFilter лишний ?
    private int filterFlags; // проходит по фильтру на уровне компонент фильтра (сортированный массив) (все биты единицы для нового) - max 32 контрола (long не поддерживается классом BitField !)
	
//    private GridData() {} // FIXME: противоречит соглашению JavaBeans !!!
    
    /** Оборачивает бин типа T для использования в гриде. Поумолчательные значения внутренних полей: sel = false, inFilter = true, filterFlags = 0xFFFFFFFF.
     * @param bean Экземпляр доменного объекта, который оборачиваем.
     * @param beanClass Класс бина для runtime-проверки.
     */
	public GridData(T bean, Class<T> beanClass) {
		if (bean == null) {
			logger.error("Null bean-parameter prohibited in GridData");
			throw new NullPointerException("Null bean-parameter brohibited in GridData");
		}
		if ( !bean.getClass().equals(beanClass) ) { // TODO: ? см. Эккеля !!!
			logger.error("Error on create GridData: bean class '{}' should be same as the parameter beanClass '{}'", bean.getClass().getName(), beanClass.getName());
			throw new IllegalArgumentException("Error on create GridData: bean class "+bean.getClass().getName()+" should be same as the parameter beanClass "+beanClass.getName());			
		}
		this.sel = false;
		this.inFilter = true;
		this.filterFlags = 0xFFFFFFFF;
		this.beanClass = beanClass;
		this.bean = bean;
		this.uid = throughoutNum.incrementAndGet();
	}
	
	/** Для генерации тестовых данных (RULE: бин должен иметь для этих целей безаргументный конструктор). */
	public GridData(Class<T> beanClass) {
		this.sel = false;
		this.inFilter = true;
		this.filterFlags = 0xFFFFFFFF;
		this.beanClass = beanClass;
		try {
			this.bean = beanClass.newInstance(); // так генерируем мок-бины !
		} catch(IllegalAccessException | InstantiationException | ExceptionInInitializerError | SecurityException e) {
			logger.error("Error on creation TEST instance of {}", beanClass.getSimpleName(), e);
			throw new InternalError("Error on creation TEST instance of " + beanClass.getSimpleName(), e);
		}
		this.uid = throughoutNum.incrementAndGet();
	}

	public T getBean() {
		return bean;
	}

	public Class<T> getBeanClass() {
		return beanClass;
	}
	
	/** Счётчик созданных объектов класса, из которого инициализируется uid объекта. */
	public static long getThroughoutNum() {
		return throughoutNum.get();
	}
	
	/** Сквозной порядковый номер созданного объекта. */
	public long getUid() {
		return uid;
	}
	
	/** Флаг выбора строки ("крыж"). */
	public boolean isSel() {
		return sel;
	}
	
	/** Пометить как выбранный. */
	public void setSel(boolean sel) {
		this.sel = sel;
	}
	
	/** Флаг прохождения фильтра. */
	public boolean isInFilter() {
		return inFilter;
	}
	
	/** Установить флаг прохождения фильтра. */
	public void setInFilter(boolean inFilter) {
		this.inFilter = inFilter;
	}
	
	/** Битовая маска покомпонентного прохождения фильтра. */
	public int getFilterFlags() {
		return filterFlags;
	}
	
	/** Установить (целиком) битовую маску прохождения фильтра. */
	public void setFilterFlags(int filterFlags) {
		this.filterFlags = filterFlags;
	}
	
	/** Устанавливает заданный бит поля filterFlags в указанное значение.
	 * @param n Бит номер [0, 31]
	 * @param value Установить в 1 (true) / обнулить (false)
	 * @return value parameter
	 */
	public boolean setFilterFlagsBit(int n, boolean value) {
		if (n < 0 || n > 31) {
			logger.error("setFilterFlagsBit. [0-31] n-bit allowed, but par. n = {}", n);
			throw new IllegalArgumentException("GridDada.setFilterFlagsBit. [0-31] n-bit allowed, but par. n = " + n);
		}
		if (value)
			filterFlags = (filterFlags | (1 << n)); // set bit n
		else filterFlags = (filterFlags & ~(1 << n)); // clear bit n
		if (logger.isTraceEnabled() && bean instanceof SubjSumm && ((SubjSumm)bean).getSubj_id() == 11643 && n == 1/*15*/) logger.trace("set bit {} to {} result = '{}' <=> '{}'", n, value, filterFlags, Integer.toBinaryString(filterFlags));
		return value;
	}

	public static long getSerialVersionUID() {
		return serialVersionUID;
	}
	
    /** hashCode() делегирован бину. */
	// для списка объектов хэшкод идентифицирует состояние фильтра
	@Override
	public int hashCode() { // https://docs.oracle.com/javase/8/docs/api/java/lang/Object.html#hashCode--
		return bean.hashCode();
	}

	/** equals(Object obj) делегирован бину. */
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) { // https://docs.oracle.com/javase/8/docs/api/java/lang/Object.html#equals-java.lang.Object-
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof GridData))
			return false;
		GridData<T> other = (GridData<T>) obj;
		return bean.equals(other.bean);
	}
	
	/** compareTo(GridData<T> obj) делегирован бину. */
	// Comparable implementation (https://docs.oracle.com/javase/8/docs/api/java/lang/Comparable.html)
	@Override
	public int compareTo(GridData<T> obj) {
		if (this == obj)
			return 0;
		if (obj == null) {
			logger.error("null parameter in GridData.comparTo. this = {}", this);
			throw new NullPointerException("null parameter in GridData.comparTo. this = "+this);
		}
		return this.bean.compareTo(obj.bean);
	}
	
	
	private static Object/*Comparator<GridData<?>>*/ compareByUid = new CompareByUid<GridData<?>>();

	/** Возвращает компаратор по полю uid (порядковый номер созданного объекта).
	 * При отсутствии PK будет использоваться дата-провайдером при сортировке и бинарном поиске.
	 */
	@SuppressWarnings("unchecked")
	public static /*synchronized*/ <T extends Object & Serializable & Comparable<? super T>>
			Comparator<GridData<T>> getCompareByUid() {
		/*if (compareByUid == null) {
			compareByUid = new CompareByUid<GridData<T>>();
		}*/
		return (Comparator<GridData<T>>)compareByUid;
	}
	
	private static final class CompareByUid <T extends Object & Serializable & Comparable<? super T>>
			implements Comparator<GridData<T>>, Serializable { // https://docs.oracle.com/javase/8/docs/api/java/util/Comparator.html#compare-T-T-
		private static final long serialVersionUID = -7089030316132473988L;
		public int compare(GridData<T> o1, GridData<T> o2) {
			if (o1 == null || o2 == null) {
				logger.error("GridData.CompareByUid.compare. null parameter: o1 = {}, o2 = {}", o1, o2);
				throw new NullPointerException("GridData.CompareByUid.compare. null parameter: o1 = " + o1 + ", o2 = " + o2);
			}
			return o1.uid == o2.uid ? 0 : o1.uid > o2.uid ? 1 : -1;
		}
	}

	/** Получить индекс в неотсортированном списке по сквозному UID объекта GridData.
	 * Используется при обработке событий для доступа к произвольной строке по номеру из ZUL (onCheckSel()) во внутреннем списке дата-модели.
	 * -1 если не найден.
	 */
	public static <T extends Object & Serializable & Comparable<? super T>>
			int searchByUid(List<GridData<T>> a, long uid) {
/*		if ( sorted_by_Uid && a instanceof RandomAccess ) { // TODO: быстрый поиск на отсортированном по uid списке
			return indexedBinarySearchByUid(a, uid);
		}*/
		for (ListIterator<GridData<T>> li = a.listIterator(); li.hasNext();) {
			if (li.next().getUid() == uid) return li.previousIndex();
		}
		return -1;
	}
	
} // public class GridData