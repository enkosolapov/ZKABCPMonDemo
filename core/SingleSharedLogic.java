package basos.core;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/* TODO: для очереди (или Map ?) (Collections.synchronizedList(new LinkedList<>()) -ConcurrentLinkedQueue ?): addUITask, pollUITask, clearUIQueue, isEmptyUITask, sizeUIQueue,... */

/** Задача, передаваемая между потоками в единственном числе (перетираемая). Thread-safe.
 * Задача д.б. представлена в виде Supplier&lt;Integer&gt;.
 * Значение -1 не должно возвращаться продюсером (специальное значение результата выполнения doSharedLogicOnce() означает, что нет расшаренной задачи).
 */
public class SingleSharedLogic {

	private final AtomicReference<Supplier<Integer>> sharedLogic = new AtomicReference<>(); // RULE: -1 - не должно возвращаться продюсером (специальное значение результата выполнения doSharedLogicOnce() означает, что нет расшаренной задачи)

	/** Назначить задачу. Перетирает предыдущую без предупреждения.
	 * @param sharedLogicToSet Not null. Значение -1 не должно возвращаться продюсером.
	 */
	public void addSharedLogic(Supplier<Integer> sharedLogicToSet) {
// TODO: инфорировать о том, что перезаписана поверх другой задачи (возвраать предыдущую ?)
		if (sharedLogicToSet == null) {
			throw new NullPointerException("sharedLogic should not be null");
		}
		sharedLogic.set(sharedLogicToSet);
	}

	/** Достать-обнулить и выполнить.
	 * @return Результат выполнения задачи или -1 при отсутствии назначенной задачи.
	 */
	public int doSharedLogicOnce() {
		Supplier<Integer> shl = sharedLogic.getAndSet(null);
		if (shl == null) {
			return -1;
		}
		return shl.get().intValue();
	}
	
	/** Достать и обнулить. */
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