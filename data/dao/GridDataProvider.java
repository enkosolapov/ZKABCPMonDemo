package basos.data.dao;

import java.io.Serializable;
import java.util.List;

import basos.data.GridData;

/** Поставщик данных для лист-дата-модели грида.
 * Получает от определённого в реализации провайдера список объектов GridData (содержащих бин - экземпляр доменного класса T).
 * Получение по частям, обновление, запись в БД не предусмотрены.
 * @param T Класс бина.
 */
public interface GridDataProvider<T extends Object & Serializable & Comparable<? super T>> {
	
	/** Класс доменных объектов, заворачиваемых в GridData. */
	Class<T> getBeanClass();
	
	/** Извлечённый целиком за один раз при первом обращении (lazy), live, fixed-size (but set & sort allowed) list. */
	List<GridData<T>> getAll();
	
	/** Возвращает (фиксированную) размерность списка. */
	int getTotalRowCount();
	
	/** Элемент в заданной позиции полного списка.
	 * @see java.util.List#get(int) List.get
	 */
	GridData<T> get(int idx);
	
	/** Индекс экземпляра GridData в списке.
	 * @return The index of the search key, if it is contained in the list; otherwise, (-(insertion point) - 1). 
	 */
	int indexOf(GridData<T> subj);
	
	/** Вернуть список строк, удовлетворяющих значению (неуникального) индекса. */
	<U extends Object & Comparable<? super U>> List<GridData<T>> getRange(U key);
	
	//void reRead();
	//Object[] getColumnArray(String fieldName);
	//boolean[] getColumnBooleanArray(String fieldName);
	//int[] getColumnIntArray(String fieldName);
	
	//int indefOfBean(T subj); // м.б. компаратор, по-любому Comparable
	
	//List<T> getBeanList();

} // public interface GridDataProvider<T extends Object & Serializable & Comparable<? super T>>