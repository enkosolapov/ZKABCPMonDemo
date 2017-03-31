package basos.data.dao;

import java.io.Serializable;
import java.util.List;

import basos.data.GridData;

/** ��������� ������ ��� ����-����-������ �����.
 * �������� �� ������������ � ���������� ���������� ������ �������� GridData (���������� ��� - ��������� ��������� ������ T).
 * ��������� �� ������, ����������, ������ � �� �� �������������.
 * @param T ����� ����.
 */
public interface GridDataProvider<T extends Object & Serializable & Comparable<? super T>> {
	
	/** ����� �������� ��������, �������������� � GridData. */
	Class<T> getBeanClass();
	
	/** ����������� ������� �� ���� ��� ��� ������ ��������� (lazy), live, fixed-size (but set & sort allowed) list. */
	List<GridData<T>> getAll();
	
	/** ���������� (�������������) ����������� ������. */
	int getTotalRowCount();
	
	/** ������� � �������� ������� ������� ������.
	 * @see java.util.List#get(int) List.get
	 */
	GridData<T> get(int idx);
	
	/** ������ ���������� GridData � ������.
	 * @return The index of the search key, if it is contained in the list; otherwise, (-(insertion point) - 1). 
	 */
	int indexOf(GridData<T> subj);
	
	/** ������� ������ �����, ��������������� �������� (�������������) �������. */
	<U extends Object & Comparable<? super U>> List<GridData<T>> getRange(U key);
	
	//void reRead();
	//Object[] getColumnArray(String fieldName);
	//boolean[] getColumnBooleanArray(String fieldName);
	//int[] getColumnIntArray(String fieldName);
	
	//int indefOfBean(T subj); // �.�. ����������, ��-������ Comparable
	
	//List<T> getBeanList();

} // public interface GridDataProvider<T extends Object & Serializable & Comparable<? super T>>