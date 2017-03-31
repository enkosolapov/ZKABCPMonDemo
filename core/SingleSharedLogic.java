package basos.core;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/* TODO: ��� ������� (��� Map ?) (Collections.synchronizedList(new LinkedList<>()) -ConcurrentLinkedQueue ?): addUITask, pollUITask, clearUIQueue, isEmptyUITask, sizeUIQueue,... */

/** ������, ������������ ����� �������� � ������������ ����� (������������). Thread-safe.
 * ������ �.�. ������������ � ���� Supplier&lt;Integer&gt;.
 * �������� -1 �� ������ ������������ ���������� (����������� �������� ���������� ���������� doSharedLogicOnce() ��������, ��� ��� ����������� ������).
 */
public class SingleSharedLogic {

	private final AtomicReference<Supplier<Integer>> sharedLogic = new AtomicReference<>(); // RULE: -1 - �� ������ ������������ ���������� (����������� �������� ���������� ���������� doSharedLogicOnce() ��������, ��� ��� ����������� ������)

	/** ��������� ������. ���������� ���������� ��� ��������������.
	 * @param sharedLogicToSet Not null. �������� -1 �� ������ ������������ ����������.
	 */
	public void addSharedLogic(Supplier<Integer> sharedLogicToSet) {
// TODO: ������������ � ���, ��� ������������ ������ ������ ������ (��������� ���������� ?)
		if (sharedLogicToSet == null) {
			throw new NullPointerException("sharedLogic should not be null");
		}
		sharedLogic.set(sharedLogicToSet);
	}

	/** �������-�������� � ���������.
	 * @return ��������� ���������� ������ ��� -1 ��� ���������� ����������� ������.
	 */
	public int doSharedLogicOnce() {
		Supplier<Integer> shl = sharedLogic.getAndSet(null);
		if (shl == null) {
			return -1;
		}
		return shl.get().intValue();
	}
	
	/** ������� � ��������. */
	public Supplier<Integer> pollSharedLogic() {
		return sharedLogic.getAndSet(null);
	}
	
	public void clearSharedLogic() {
		sharedLogic.set(null);
	}

	public boolean isEmptySharedLogic() {
		return sharedLogic.get() == null;
	}
	
	/** 0/1 */
	public int sizeSharedLogic() {
		return sharedLogic.get() == null ? 0 : 1;
	}

} // public class SingleSharedLogic