package basos.zkui;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.zkoss.zk.ui.AbstractComponent;


/** Поведенческий интерфейс контролов, составляющих композит GridDataFilter.
 * Необходимо для каждого типа фильтр-компонента реализовать свою логику в подклассе.
 * Все методы должны бы были быть статическими, но статика не полиморфна, поведение КЛАССОВ не наследуется...
 */
public abstract class AbstractComponentBehaveUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(AbstractComponentBehaveUtil.class);

// FIXME: thread-safe Multiton (one instance per subclass): http://stackoverflow.com/questions/11126866/thread-safe-multitons-in-java
	//private static AbstractComponentBehaveUtil instance;
	
	protected AbstractComponentBehaveUtil() {/*регистрация создаваемого экземпляра (по одному на класс), не допускать утечки недосозданного объекта !*/};
	
	/** Возвращает новый экземпляр заданного подкласса (рефлективно) */
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
	
// TODO: суффиксы держать в enum ?
	/** Используется в {@link #getCorrespondingFieldName(AbstractComponent) getCorrespondingFieldName()} при таком соглашении имён, что ИД компонента задаётся добавлением суффикса (типа компонента) к имени соответствующего поля доменного объекта.
	 * @param ac Компонент, из ИД которого хотим получить название поля доменного объекта. Должен иметь непустой ИД, заканчивающийся на suffix.
	 * @param suffix Суффикс типа компонента.
	 * @return ИД без суффикса (StringUtils.removeEnd(compId, suffix)).
	 * @see StringUtils#removeEnd(String, String) StringUtils.removeEnd()
	 * @exception IllegalArgumentException Для пустого или некорректного ИД копмонента.
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

	/** Используя правила именования, получить название поля доменного объекта, для обслуживания которого создан контрол.
	 * @param ac Компонент, ID которого преобразуется в наименование поля.
	 * @return Наименование соответствующего поля доменного объекта в соответствием с соглашением имён.
	 * @see #getCorrespondingFieldNameAsIdWithoutSuffix(AbstractComponent, String) 
	 */
	public abstract String getCorrespondingFieldName(AbstractComponent ac);
	
	/** Очистить содержимое компонента.
	 * @param ac Компонент, значение которого обнуляется.
	 */
	public abstract void clear(AbstractComponent ac);
	
	/** Проверить компонент на пустое значение.
	 * @param ac Компонент, пустоту которого проверяем.
	 * @return Истина для пустого значения компонента.
	 */
	public abstract boolean isEmpty(AbstractComponent ac);
	
	/** Значение компонента в форме строки.
	 * @param ac Компонент, значение которого опрашиваем.
	 * @return Строковое представление значения компонента.
	 */
	public abstract String getText(AbstractComponent ac);
	
	/** Сделать компонент неактивным/активным.
	 * @param ac Компонент, который делаем недоступным.
	 * @param disable Запретить(true)/разрешить(false).
	 */
	public abstract void disable(AbstractComponent ac, boolean disable);
	
	/** Считать значение компонента с (объектным, т.е. поле м.б. int, возвращаем Integer) типом обслуживаемой колонки.
	 * @param ac Компонент, который читаем.
	 * @return Значение компонента (тип значения может быть определён как внутренный класс компонента, см. {@link BetweenFilterMacro.PairedValue}).
	 */
	public abstract Object getValue(AbstractComponent ac);
	
	/** Проверка третьей величины на соответствие значению компонента (абстрактный предикат, подразумевающий проверку значения поля текущей строки дата-модели против фильтра компонента).
	 * Типы параметров могут быть разные (например, для композита BetweenFilterMacro значение компонента может быть представлено типом {@link BetweenFilterMacro.PairedValue}; сравниваемая величина может иметь тип BigDecimal или Date), т.о. параметры не симметричны.
	 * @param componentValue Значение компонента. Может быть получено методом {@link #getValue(AbstractComponent) getValue()}.
	 * @param otherValue Оцениваемая величина (значение ячейки грида).
	 * @return Истина при выполнении условий абстрактного предиката (проходит ли значение поля фильтр компонента).
	 */
	public abstract boolean theFilter(Object componentValue, Object otherValue);
	
	/** Проверка третьей величины на строгое равенство значению компонента (предикат, подразумевающий проверку значения поля текущей строки дата-модели против эксклюзивного фильтра компонента).
	 * Типы параметров могут быть разные (например, для композита BetweenFilterMacro значение компонента может быть представлено типом {@link BetweenFilterMacro.PairedValue}; сравниваемая величина может иметь тип BigDecimal или Date), т.о. параметры не симметричны.
	 * @param componentValue Значение компонента. Может быть получено методом {@link #getValue(AbstractComponent) getValue()}.
	 * @param otherValue Оцениваемая величина (значение ячейки грида).
	 * @return Истина при равенстве значений.
	 */
	public abstract boolean exactMatch(Object componentValue, Object otherValue);

} // public abstract class AbstractComponentBehaveUtil