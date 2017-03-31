package basos.data.zkmodel;

import java.util.*;
import java.util.concurrent.locks.*;

import org.zkoss.zul.ListModelList;
import org.zkoss.zul.event.ListDataEvent;

/** ListModelList с инкапсулированным RW-замком StampedLock и событиями для обновления UI. */
public class ListModelListExt<E> extends ListModelList<E> {
	private static final long serialVersionUID = -7055467997780509220L;
	
	//private final ReentrantLock modelLock = new ReentrantLock(true); // fair !!
	private final StampedLock modelRWLock = new StampedLock();
	
	public ListModelListExt () { super(); }
	
	/** Main constructor with live backed list. {@inheritDoc ListModelList#ListModelList(List, boolean)} */
	public ListModelListExt(List<E> list, boolean live) { super(list, live); } // main
	
	/** It makes a copy of the specified collection (i.e., not live). */
	public ListModelListExt(Collection<? extends E> c) { super(c); }
	
	/** It makes a copy of the specified array (i.e., not live). */
	public ListModelListExt(E[] array) { super(array); }
	
	public ListModelListExt(int initialCapacity) { super(initialCapacity); }
	
	/** Notifies a change of the same element to trigger an event of {@link ListDataEvent#CONTENTS_CHANGED}.
	 * Стандартный вариант {@link #notifyChange(Object)} ищет элемент в модели (indexOf), но скрывает индекс.
	 * Данная версия полезна, когда индекс нужен в явном виде.
	 * @param idx индекс изменённой строки модели.
	 */
	public void notifyChange(int idx) {
		if (idx >= 0) {
			fireEvent(ListDataEvent.CONTENTS_CHANGED, idx, idx);
		}
	}

	/** Обновить грид после массового обновления подлежащих данных - selectAllRows(). */
	public void notifyChangeAll() {
		fireEvent(ListDataEvent.CONTENTS_CHANGED, -1, -1);
	}
	
	/** RW-замок для синхронизации модели. */
	public StampedLock getModelRWLock() {
		return modelRWLock;
	}

	/** Подмена внутреннего списка (вместо clear-addAll).
	 * <b>RW-блокируется, не использовать внешнюю блокировку.</b>
	 */
	public void safeReplaceInnerList(List<E> list) {
		long stamp = modelRWLock.writeLock();
		try {
			clearSelection();
			_list = list;
		} finally {
			modelRWLock.unlock(stamp);
		}
		notifyChangeAll();
	}
	
	/** Подмена внутреннего списка (вместо clear-addAll).
	 * В отличие от {@link #safeReplaceInnerList(List)} не используется внутренний замок {@link #getModelRWLock()}. <b>NOT Thread-safe.</b>
	 */
	public void replaceInnerList(List<E> list) {
		clearSelection();
		_list = list;
		notifyChangeAll();
	}
	
	/** Очистка внутреннего списка.
	 * <b>RW-блокируется, не использовать внешнюю блокировку.</b>
	 */
	public void safeClear() {
		long stamp = modelRWLock.writeLock();
		try {
			clear();
		} finally {
			modelRWLock.unlock(stamp);
		}
	}

// TODO: ??? переписать синхронизированно  public void sort(Comparator<E> cmpr, final boolean ascending) ???
	
} // public class ListModelListExt<E> extends ListModelList<E>