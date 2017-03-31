package basos.data.zkmodel;

import java.util.*;
import java.util.concurrent.locks.*;

import org.zkoss.zul.ListModelList;
import org.zkoss.zul.event.ListDataEvent;

/** ListModelList � ����������������� RW-������ StampedLock � ��������� ��� ���������� UI. */
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
	 * ����������� ������� {@link #notifyChange(Object)} ���� ������� � ������ (indexOf), �� �������� ������.
	 * ������ ������ �������, ����� ������ ����� � ����� ����.
	 * @param idx ������ ��������� ������ ������.
	 */
	public void notifyChange(int idx) {
		if (idx >= 0) {
			fireEvent(ListDataEvent.CONTENTS_CHANGED, idx, idx);
		}
	}

	/** �������� ���� ����� ��������� ���������� ���������� ������ - selectAllRows(). */
	public void notifyChangeAll() {
		fireEvent(ListDataEvent.CONTENTS_CHANGED, -1, -1);
	}
	
	/** RW-����� ��� ������������� ������. */
	public StampedLock getModelRWLock() {
		return modelRWLock;
	}

	/** ������� ����������� ������ (������ clear-addAll).
	 * <b>RW-�����������, �� ������������ ������� ����������.</b>
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
	
	/** ������� ����������� ������ (������ clear-addAll).
	 * � ������� �� {@link #safeReplaceInnerList(List)} �� ������������ ���������� ����� {@link #getModelRWLock()}. <b>NOT Thread-safe.</b>
	 */
	public void replaceInnerList(List<E> list) {
		clearSelection();
		_list = list;
		notifyChangeAll();
	}
	
	/** ������� ����������� ������.
	 * <b>RW-�����������, �� ������������ ������� ����������.</b>
	 */
	public void safeClear() {
		long stamp = modelRWLock.writeLock();
		try {
			clear();
		} finally {
			modelRWLock.unlock(stamp);
		}
	}

// TODO: ??? ���������� �����������������  public void sort(Comparator<E> cmpr, final boolean ascending) ???
	
} // public class ListModelListExt<E> extends ListModelList<E>