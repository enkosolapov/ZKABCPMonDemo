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


/** Запуск асинхронной прерываемой возвращающей результат типа V задачи, запускаемой в рабочем (не UI) потоке.
 * Безаргументный конструктор назначает задачей вызов doInBackground(), в котором д.б. описана пользовательская логика.
 * Используется механизм событий java.beans (bound properties). Слушатели должны реализовать PropertyChangeListener.
 * В конце done() публикует событие "progress_down" (встроено в наследника FutureTask, которому делегируем все вызовы интерфейса RunnableFuture).
 * Не расширяем FutureTask, а декорируем его, чтобы использовать безаргументный конструктор, обеспечивающий выполнение
 *  пользовательской задачи в (по сути абстрактном) методе doInBackground().
 * Не делаем doInBackground() абстрактным, чтобы можно было использовать традиционные для FutureTask конструкторы.
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
//	    	workingThread = null; // на случай сбоя отсюда до конца метода; проверять .isAlive() !
	    }		
	} // private class FarFutureTask<W> extends FutureTask<W>
	
    private volatile Thread workingThread;
	private volatile/*final*/ PropertyChangeSupport propertyChangeSupport; // final or volatile, not both ! self-incapsulated
    private final FarFutureTask<V> ft; // "everything is run inside this FutureTask. Also it is used as a delegatee for the Future API"
	
    /** @see FutureTask#FutureTask(Callable) */
    public AsyncTask(Callable<V> callable) {
    	ft = new FarFutureTask<V>(callable);
//		propertyChangeSupport = new PropertyChangeSupport(this/*sourceBean*/); // ? в SwingWorker своя реализация - SwingWorkerPropertyChangeSupport (переопределён firePropertyChange)
    }
    
    /** @see FutureTask#FutureTask(Runnable, Object) */
    public AsyncTask(Runnable runnable, V result) {
    	ft = new FarFutureTask<V>(runnable, result);
//		propertyChangeSupport = new PropertyChangeSupport(this/*sourceBean*/); // ? в SwingWorker своя реализация - SwingWorkerPropertyChangeSupport (переопределён firePropertyChange)
    }
    
    /** Безаргументный конструктор назначает задачей вызов doInBackground(), в котором д.б. описана пользовательская логика. */
	public AsyncTask() {
    	ft = new FarFutureTask<V>(() -> {
    		//setWorkingThread(Thread.currentThread());
    		return doInBackground();
        });

// проверить, что doInBackground переопределён (он обязательно д.б. переопределён хотя бы в одном подклассе, как будто бы abstract) -> этот конструктор не должен вызываться из корня иерархии - ZKWork
    	if (this.getClass().equals(AsyncTask.class)) {
    		logger.error("This non-arg type of AsyncTask() constructor not allowed from root AsyncTask class, only from subclass with overriden doInBackground() method (must contain task logic) !!!");
    		throw new InstantiationError("This non-arg type of AsyncTask() constructor not allowed from root AsyncTask class, only from subclass with overriden doInBackground() method (must contain task logic) !!!"); // IllegalStateException
    	}
    	// вызывается из подкласса, но doInBackground() может быть не переопределён (не обязательно переопределять в классе вызывающего объекта, можно в одном из промежуточных)
    	Method dm = null;
    	try {
    		dm = this.getClass().getDeclaredMethod("doInBackground"); // определённый в классе вызывающего объекта
    		logger.trace("AsyncTask() constructor. Right branch - fresh override :) className: '{}', superClassName: '{}', doInBackground declared at '{}'", this.getClass().getName(), this.getClass().getSuperclass(), dm.getDeclaringClass().getName());
    	} catch (NoSuchMethodException e) { // doInBackground() не переопределён в классе вызывающего объекта
    		try {
    			dm = this.getClass().getMethod("doInBackground"); // only PUBLIC member method of the class or interface
    			if (dm.getDeclaringClass().equals(AsyncTask.class)) { // ! сюда не должен попасть, т.к. из подклассов уже не увидит protected корневой метод !
    				logger.error("AsyncTask() constructor. Wrong branch (but UNREAL !)... className: '{}', superClassName: '{}', doInBackground declared at '{}'", this.getClass().getName(), this.getClass().getSuperclass(), dm.getDeclaringClass().getName());
					throw new InstantiationError("Unable to construct AsyncTask without overriding doInBackground() method (must contain task logic) !"); // IllegalStateException
    			}
    			logger.trace("AsyncTask() constructor. Right branch. className: '{}', superClassName: '{}', doInBackground declared at '{}'", this.getClass().getName(), this.getClass().getSuperclass(), dm.getDeclaringClass().getName());
    		} catch (NoSuchMethodException ee) { // ? сюда может ошибочно попасть, если метод doInBackground переопределён в промежуточном классе, но не public ?
    			logger.error("AsyncTask() constructor. No public doInBackground() at all.  className: '{}', superClassName: '{}', methods: {}", this.getClass().getName(), this.getClass().getSuperclass(), Arrays.deepToString(this.getClass().getMethods()));
				throw new InstantiationError("Unable to construct AsyncTask without overrided doInBackground() method (must contain task logic) !"); // IllegalStateException
    		}
		} catch (Throwable e) {
			logger.error("AsyncTask() constructor. Unexpetced Error occur: ", e);
			throw new InternalError("AsyncTask() constructor. Unexpetced Error occur.", e);
		}

//		propertyChangeSupport = new PropertyChangeSupport(this);
    } // public AsyncTask()
	
	/** Вызывать только из рабочего (не UI) потока. {@inheritDoc} Основная задача (назначенная в конструкторе, {@link #doInBackground()} для безаргументного конструктора) вызывается в конце. */
	@Override
	public void run() {
		//assert( !EventProcessor.inEventListener() ); // проверить, что не UI-поток !
		setWorkingThread(Thread.currentThread());
		logger.trace("AsyncTask.run().  выполнили setWorkingThread, далее вызовем FarFutureTask.run(), который вызовет Callable, в котором вызывается doInBackground(), который вызывает ZKWorkerWithTimerAndPM.userMainWTask");
		ft.run(); // здесь вызывается Callable
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

	/** Для случая безаргументного конструктора здесь реализуется пользовательская логика. */
    protected V doInBackground() throws Exception { // не делаем abstract чтобы была возможность исп-ть унаследованные конструкторы
    	assert(false) : "doInBackground() должен быть переопределён !";
    	return null;
    }
    
    /** Вызывается в конце (каждого из) run() -> set()/setException(...)/cancel(...) -> finishCompletion()
     *  при переходе в статус isDone()==true (whether normally or via cancellation).
     * Непосредственно перед этим вызовом в родителе (поле, производном от FutureTask, которому делегируются логика FutureTask) создаётся событие "progress_down".
     */
//    @Override
    protected void done() {
    	//ft.done();
    }
    
    /** Поток, в котором работает AsyncTask. */
	public Thread getWorkingThread() {
		return workingThread;
	}
	
	private void setWorkingThread(Thread workingThread) {
		this.workingThread = workingThread;
		logger.trace("AsyncTask.setWorkingThread: {}", workingThread);
	}

	/** Посылаем слушателям событие через делегата типа {@link java.beans.PropertyChangeSupport}.
	 * Предполагается, что запускаем из рабочего потока (из него нельзя обращаться к UI без SP).
	 * Аргументы должны быть не null, старое и новое значения должны отличаться (иначе ничего не произойдёт).
	 * @see PropertyChangeSupport#firePropertyChange(String, Object, Object)
	 */
    public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
// В оригинале метод перегружен для Object, int, boolean (но в событии всё превращается в Object)
    	logger.trace("firePropertyChange. propertyName = '{}', newValue = '{}'", propertyName, newValue);
        getPropertyChangeSupport().firePropertyChange(propertyName, oldValue, newValue);
    } // public void firePropertyChange(String propertyName, Object oldValue, Object newValue)

    /** @see PropertyChangeSupport#addPropertyChangeListener(PropertyChangeListener) */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
//    	if (listener == this) {
    		// TODO: сами обрабатываем короче
//    	} else {
    		getPropertyChangeSupport().addPropertyChangeListener(listener);
//    	}
    }
    
    /** @see PropertyChangeSupport#addPropertyChangeListener(String, PropertyChangeListener) */
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
//    	if (listener == this) {
    		// TODO: сами обрабатываем короче
//    	} else {
    		getPropertyChangeSupport().addPropertyChangeListener(propertyName, listener);
//    	}
    }
    
    /** @see PropertyChangeSupport#removePropertyChangeListener */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
    	getPropertyChangeSupport().removePropertyChangeListener(listener);
    }
    
    /** Возвращает lazy init, self-incapsulated {@link PropertyChangeSupport}, которому делегирована поддержка "bound properties" (события java.beans).
     * Слушатель должен реализовать интерфейс {@link PropertyChangeListener}.
     * @see #addPropertyChangeListener
     * @see #removePropertyChangeListener
     * @see #firePropertyChange
     */
    public PropertyChangeSupport getPropertyChangeSupport() {
    	if (propertyChangeSupport == null) {
    		propertyChangeSupport = new PropertyChangeSupport(this/*sourceBean*/); // ? в SwingWorker своя реализация - SwingWorkerPropertyChangeSupport (переопределён firePropertyChange)
    	}
        return propertyChangeSupport;
    }

//    public AsyncTask<V> getSelf() {return this;};
    
} // public class AsyncTask<V> implements RunnableFuture<V>