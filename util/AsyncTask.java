package basos.util;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

//import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import org.zkoss.zk.ui.impl.EventProcessor;


/** ������ ����������� ����������� ������������ ��������� ���� V ������, ����������� � ������� (�� UI) ������.
 * �������������� ����������� ��������� ������� ����� doInBackground(), � ������� �.�. ������� ���������������� ������.
 * ������������ �������� ������� java.beans (bound properties). ��������� ������ ����������� PropertyChangeListener.
 * � ����� done() ��������� ������� "progress_down" (�������� � ���������� FutureTask, �������� ���������� ��� ������ ���������� RunnableFuture).
 * �� ��������� FutureTask, � ���������� ���, ����� ������������ �������������� �����������, �������������� ����������
 *  ���������������� ������ � (�� ���� �����������) ������ doInBackground().
 * �� ������ doInBackground() �����������, ����� ����� ���� ������������ ������������ ��� FutureTask ������������.
 * @param <V> The result type returned by this Future's get method.
 * @see RunnableFuture
 * @see FutureTask
 */
public class AsyncTask<V> implements RunnableFuture<V> /*extends FutureTask<V>*/ /*implements Serializable*/ {
//	private static final long serialVersionUID = -5237161188032188291L;
	
	private /*static*/ final Logger logger = LoggerFactory.getLogger(AsyncTask.class);
	
	private class FarFutureTask<W> extends FutureTask<W> {
	    public FarFutureTask(Callable<W> callable) {
	    	super(callable);
	    }
	    public FarFutureTask(Runnable runnable, W result) {
	    	super(runnable, result);
	    }
		@Override
	    protected void done() {
	    	firePropertyChange("progress_down", false, true);
			AsyncTask.this.done();
//	    	workingThread = null; // �� ������ ���� ������ �� ����� ������; ��������� .isAlive() !
	    }		
	} // private class FarFutureTask<W> extends FutureTask<W>
	
    private volatile Thread workingThread;
	private volatile/*final*/ PropertyChangeSupport propertyChangeSupport; // final or volatile, not both ! self-incapsulated
    private final FarFutureTask<V> ft; // "everything is run inside this FutureTask. Also it is used as a delegatee for the Future API"
	
    /** @see FutureTask#FutureTask(Callable) */
    public AsyncTask(Callable<V> callable) {
    	ft = new FarFutureTask<V>(callable);
//		propertyChangeSupport = new PropertyChangeSupport(this/*sourceBean*/); // ? � SwingWorker ���� ���������� - SwingWorkerPropertyChangeSupport (������������ firePropertyChange)
    }
    
    /** @see FutureTask#FutureTask(Runnable, Object) */
    public AsyncTask(Runnable runnable, V result) {
    	ft = new FarFutureTask<V>(runnable, result);
//		propertyChangeSupport = new PropertyChangeSupport(this/*sourceBean*/); // ? � SwingWorker ���� ���������� - SwingWorkerPropertyChangeSupport (������������ firePropertyChange)
    }
    
    /** �������������� ����������� ��������� ������� ����� doInBackground(), � ������� �.�. ������� ���������������� ������. */
	public AsyncTask() {
    	ft = new FarFutureTask<V>(() -> {
    		//setWorkingThread(Thread.currentThread());
    		return doInBackground();
        });

// ���������, ��� doInBackground ������������ (�� ����������� �.�. ������������ ���� �� � ����� ���������, ��� ����� �� abstract) -> ���� ����������� �� ������ ���������� �� ����� �������� - ZKWork
    	if (this.getClass().equals(AsyncTask.class)) {
    		logger.error("This non-arg type of AsyncTask() constructor not allowed from root AsyncTask class, only from subclass with overriden doInBackground() method (must contain task logic) !!!");
    		throw new InstantiationError("This non-arg type of AsyncTask() constructor not allowed from root AsyncTask class, only from subclass with overriden doInBackground() method (must contain task logic) !!!"); // IllegalStateException
    	}
    	// ���������� �� ���������, �� doInBackground() ����� ���� �� ������������ (�� ����������� �������������� � ������ ����������� �������, ����� � ����� �� �������������)
    	Method dm = null;
    	try {
    		dm = this.getClass().getDeclaredMethod("doInBackground"); // ����������� � ������ ����������� �������
    		logger.trace("AsyncTask() constructor. Right branch - fresh override :) className: '{}', superClassName: '{}', doInBackground declared at '{}'", this.getClass().getName(), this.getClass().getSuperclass(), dm.getDeclaringClass().getName());
    	} catch (NoSuchMethodException e) { // doInBackground() �� ������������ � ������ ����������� �������
    		try {
    			dm = this.getClass().getMethod("doInBackground"); // only PUBLIC member method of the class or interface
    			if (dm.getDeclaringClass().equals(AsyncTask.class)) { // ! ���� �� ������ �������, �.�. �� ���������� ��� �� ������ protected �������� ����� !
    				logger.error("AsyncTask() constructor. Wrong branch (but UNREAL !)... className: '{}', superClassName: '{}', doInBackground declared at '{}'", this.getClass().getName(), this.getClass().getSuperclass(), dm.getDeclaringClass().getName());
					throw new InstantiationError("Unable to construct AsyncTask without overriding doInBackground() method (must contain task logic) !"); // IllegalStateException
    			}
    			logger.trace("AsyncTask() constructor. Right branch. className: '{}', superClassName: '{}', doInBackground declared at '{}'", this.getClass().getName(), this.getClass().getSuperclass(), dm.getDeclaringClass().getName());
    		} catch (NoSuchMethodException ee) { // ? ���� ����� �������� �������, ���� ����� doInBackground ������������ � ������������� ������, �� �� public ?
    			logger.error("AsyncTask() constructor. No public doInBackground() at all.  className: '{}', superClassName: '{}', methods: {}", this.getClass().getName(), this.getClass().getSuperclass(), Arrays.deepToString(this.getClass().getMethods()));
				throw new InstantiationError("Unable to construct AsyncTask without overrided doInBackground() method (must contain task logic) !"); // IllegalStateException
    		}
		} catch (Throwable e) {
			logger.error("AsyncTask() constructor. Unexpetced Error occur: ", e);
			throw new InternalError("AsyncTask() constructor. Unexpetced Error occur.", e);
		}

//		propertyChangeSupport = new PropertyChangeSupport(this);
    } // public AsyncTask()
	
	/** �������� ������ �� �������� (�� UI) ������. {@inheritDoc} �������� ������ (����������� � ������������, {@link #doInBackground()} ��� ��������������� ������������) ���������� � �����. */
	@Override
	public void run() {
		//assert( !EventProcessor.inEventListener() ); // ���������, ��� �� UI-����� !
		setWorkingThread(Thread.currentThread());
		logger.trace("AsyncTask.run().  ��������� setWorkingThread, ����� ������� FarFutureTask.run(), ������� ������� Callable, � ������� ���������� doInBackground(), ������� �������� ZKWorkerWithTimerAndPM.userMainWTask");
		ft.run(); // ����� ���������� Callable
	}
	
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return ft.cancel(mayInterruptIfRunning);
	}
	
	@Override
	public V get() throws InterruptedException, ExecutionException {
		return ft.get();
	}
	
	@Override
	public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return ft.get(timeout, unit);
	}
	
	@Override
	public boolean isCancelled() {
		return ft.isCancelled();
	}
	
	@Override
	public boolean isDone() {
		return ft.isDone();
	}

	/** ��� ������ ��������������� ������������ ����� ����������� ���������������� ������. */
    protected V doInBackground() throws Exception { // �� ������ abstract ����� ���� ����������� ���-�� �������������� ������������
    	assert(false) : "doInBackground() ������ ���� ������������ !";
    	return null;
    }
    
    /** ���������� � ����� (������� ��) run() -> set()/setException(...)/cancel(...) -> finishCompletion()
     *  ��� �������� � ������ isDone()==true (whether normally or via cancellation).
     * ��������������� ����� ���� ������� � �������� (����, ����������� �� FutureTask, �������� ������������ ������ FutureTask) �������� ������� "progress_down".
     */
//    @Override
    protected void done() {
    	//ft.done();
    }
    
    /** �����, � ������� �������� AsyncTask. */
	public Thread getWorkingThread() {
		return workingThread;
	}
	
	private void setWorkingThread(Thread workingThread) {
		this.workingThread = workingThread;
		logger.trace("AsyncTask.setWorkingThread: {}", workingThread);
	}

	/** �������� ���������� ������� ����� �������� ���� {@link java.beans.PropertyChangeSupport}.
	 * ��������������, ��� ��������� �� �������� ������ (�� ���� ������ ���������� � UI ��� SP).
	 * ��������� ������ ���� �� null, ������ � ����� �������� ������ ���������� (����� ������ �� ���������).
	 * @see PropertyChangeSupport#firePropertyChange(String, Object, Object)
	 */
    public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
// � ��������� ����� ���������� ��� Object, int, boolean (�� � ������� �� ������������ � Object)
    	logger.trace("firePropertyChange. propertyName = '{}', newValue = '{}'", propertyName, newValue);
        getPropertyChangeSupport().firePropertyChange(propertyName, oldValue, newValue);
    } // public void firePropertyChange(String propertyName, Object oldValue, Object newValue)

    /** @see PropertyChangeSupport#addPropertyChangeListener(PropertyChangeListener) */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
//    	if (listener == this) {
    		// TODO: ���� ������������ ������
//    	} else {
    		getPropertyChangeSupport().addPropertyChangeListener(listener);
//    	}
    }
    
    /** @see PropertyChangeSupport#addPropertyChangeListener(String, PropertyChangeListener) */
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
//    	if (listener == this) {
    		// TODO: ���� ������������ ������
//    	} else {
    		getPropertyChangeSupport().addPropertyChangeListener(propertyName, listener);
//    	}
    }
    
    /** @see PropertyChangeSupport#removePropertyChangeListener */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
    	getPropertyChangeSupport().removePropertyChangeListener(listener);
    }
    
    /** ���������� lazy init, self-incapsulated {@link PropertyChangeSupport}, �������� ������������ ��������� "bound properties" (������� java.beans).
     * ��������� ������ ����������� ��������� {@link PropertyChangeListener}.
     * @see #addPropertyChangeListener
     * @see #removePropertyChangeListener
     * @see #firePropertyChange
     */
    public PropertyChangeSupport getPropertyChangeSupport() {
    	if (propertyChangeSupport == null) {
    		propertyChangeSupport = new PropertyChangeSupport(this/*sourceBean*/); // ? � SwingWorker ���� ���������� - SwingWorkerPropertyChangeSupport (������������ firePropertyChange)
    	}
        return propertyChangeSupport;
    }

//    public AsyncTask<V> getSelf() {return this;};
    
} // public class AsyncTask<V> implements RunnableFuture<V>