package basos.zkui.util;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;
//import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
//import java.util.UUID;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.impl.EventProcessor;
import org.zkoss.zk.ui.sys.DesktopCtrl;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Progressmeter;
import org.zkoss.zul.Timer;

import basos.core.SingleSharedLogic;
import basos.util.AsyncTask;


/** Для выполнения длительной операции в working (not UI) thread с визуализацией прогресса.
 * Прогресс слушает сам экземпляр класса и выводит в создаваемый Progressmeter.
 * Опубликовать прогресс: worker.firePropertyChange("progress", oldValue, newValue, false). Также слушается событие "indeterm_progress" -> showBusy.
 * Обращение к UI происходит из отдельного UI-потока, в котором обрабатываются события onTimer (таймер создаётся и
 *  запускается в начале run()), также через Server Push (когда недоступен Timer).
 * Тик таймера задаётся в файле настроек проекта в переменной по имени "worker.timerDelay".
 * Задача пользователя передаётся через первый параметр конструктора типа Supplier<V>; вторым также обязательным
 *  параметром типа Runnable идёт логика, которая выполняется в done() (обычно из неё вызывается worker.get()).
 */
public final class ZKWorkerWithTimerAndPM<V> extends AsyncTask<V> implements PropertyChangeListener, Serializable/*for enableServerPush()*/ {
	private static final long serialVersionUID = -4732700088811439005L;
	
	private /*static*/ final Logger logger = LoggerFactory.getLogger(ZKWorkerWithTimerAndPM.class);

/*	public ZKWorkerWithTimerAndPM(Callable<V> callable) {
		super(callable);
	}

	public ZKWorkerWithTimerAndPM(Runnable runnable, V result) {
		super(runnable, result);
	}*/
	
	private final Supplier<V> userMainWTask;
	private final Runnable userDoneUITask;
	private final Component pmParent; // здесь создаётся таймер, прогрессметр, выводятся оповещения
	
	private /*transient*/ volatile Timer workerTimer;
	private Progressmeter workerPM; // for long operations in working threads; operating by mean of JavaBeans events model
	private Desktop pmDesktop;
	private int timerDelay;
	
	private static final ThreadLocalRandom rnd = ThreadLocalRandom.current();
	private final /*transient*/ SingleSharedLogic workerSharedUILogic = new SingleSharedLogic(); // WT расшаривает задчу для выполнения в UI-Thread (перетирается, если не успела выполниться)
	//private final String workerTimerId = UUID.randomUUID().toString();
	
	/** Should be executed in UI Thead.
	 @param userMainWTask Задача пользователя (mandatory). Вызывается в doInBackground(), который вызывается из корневого run().
	 @param userDoneUITask Логика, которая выполняется в done() (обычно из неё вызывается worker.get()) (mandatory)
	 @param pmParent Родительский компонент, в котором создаётся таймер, прогрессметр, выводятся оповещения (mandatory)
    */
	public ZKWorkerWithTimerAndPM(Supplier<V> userMainWTask, Runnable userDoneUITask, Component pmParent) {
		super();
		if (userMainWTask == null) {
			logger.error("Null argument 'userMainWTask' not allowed !");
			throw new NullPointerException("Null argument 'userMainWTask' not allowed !");
		}
		this.userMainWTask = userMainWTask;
		if (userDoneUITask == null) {
			logger.error("Null argument 'userDoneUITask' not allowed !");
			throw new NullPointerException("Null argument 'userDoneUITask' not allowed !");
		}
		this.userDoneUITask = userDoneUITask;
		if (pmParent == null) {
			logger.error("Null argument 'pmParent' not allowed !");
			throw new NullPointerException("Null argument 'pmParent' not allowed !");
		}
		this.pmParent = pmParent;
		// в ui-thread разрешаем SP, который потребуется при инициализации в run() !!!
		if ( !EventProcessor.inEventListener() ) {
			logger.error("Should be executed in UI Thead !");
			throw new IllegalStateException("Should be executed in UI Thead !");
		}
		pmDesktop = pmParent.getDesktop();
		((DesktopCtrl)pmDesktop).enableServerPush(true, this/*иначе выключается EventQueue*/); // TODO: параметры (https://www.zkoss.org/javadoc/latest/zk/org/zkoss/zk/ui/sys/DesktopCtrl.html#enableServerPush(org.zkoss.zk.ui.sys.ServerPush)) ?
		timerDelay = Integer.valueOf(Labels.getLabel("worker.timerDelay", "100")).intValue();
	} // constr
	
	
	/** {@inheritDoc}
	 * <p/> До выполнения основной пользовательской задачи при вызове родительского метода, инициализируется UI:
	 *  создаются progressmeter, таймер (как дочерние компоненты к заданному в конструкторе) и слушатель таймера (WT расшаривает для него задачу визуализации прогресса).
	 * Тик таймера задаётся в файле настроек проекта в переменной по имени "worker.timerDelay".
	 */
	@Override
	public final void run() {
		addPropertyChangeListener("progress", this);
		//worker.addPropertyChangeListener("progress_down", worker); // перенесено в worker.done()
		addPropertyChangeListener("indeterm_progress", this);
		// we are in a working thread !!!
		Supplier<Integer> initUIOnRun = () -> {
			// <progressmeter id="workerPM" value="0" hflex="true" visible="false" vflex="max" />
			workerPM = new Progressmeter(0);
			workerPM.setParent(pmParent);
			workerPM.setVisible(false);
			workerPM.setVflex("max");
			workerPM.setHflex("true");
			workerTimer = new Timer(timerDelay);
			workerTimer.setRepeats(true);
			workerTimer.setParent(pmParent);
			//workerTimer.setId(workerTimerId);
			workerTimer.addEventListener("onTimer",
				new SerializableEventListener<Event>() {
					private static final long serialVersionUID = 8327898412431035743L;

					public void onEvent(Event ev) {
						//logger.trace("workerTimer listener: ev_target: {}, ev_class: {}, ev_name: {}", ev.getTarget(), ev.getClass().getName(), ev.getName());
		    			if ( "onTimer".equals(ev.getName()) && workerTimer.equals(ev.getTarget()) ) {
		    				applySharedLogicOnTimer(ev);
		    			}
					} // onEvent
				} // new SerializableEventListener<Event>
			);
			//workerPM.setValue(0);
			workerPM.setVisible(true);
			workerTimer.start();
			return 1;
		};
		if ( doOrShareUITaskFromWT(initUIOnRun, true, null) != 1 ) { // only through SP !
			logger.error("run. Unable to prepare ZKWorkerWithTimerAndPM !");
			throw new InternalError("run. Unable to prepare ZKWorkerWithTimerAndPM !");
		}
		logger.trace("Техническая UI-часть ZKWorkerWithTimerAndPM.run() завершена успешно (назначили себя слушателем 'progress' и 'indeterm_progress'; создан PM, запущен Timer и его EventListener), далее последует вызов super.run()");
		super.run();
	} // public void run()
	
	/** Выполняется задача, назначенная в конструкторе. Вызывается в {@link AsyncTask#run()} */
	@Override
	protected final V doInBackground() throws Exception { // не делаем abstract чтобы была возможность исп-ть унаследованные конструкторы
		logger.trace("ZKWorkerWithTimerAndPM.doInBackground() перед userMainWTask.get()");
    	return userMainWTask.get();
    }

	/** {@inheritDoc}
	 * <p/> Сначала выполняется необязательная пользовательская финализирующая задача, установленная через конструктор.
	 * Потом разбирается UI.
	 */
	@Override
	protected final void done() {
		Supplier<Integer> finishUITask = () -> {
			if ( userDoneUITask != null ) userDoneUITask.run();
			workerPM.setValue(100);
			workerSharedUILogic.clearSharedLogic(); // считая, что задачи относятся к одной выгрузке и эта всегда завершающая !!
			workerTimer.stop();
			workerTimer.detach();
			workerTimer = null;
			Clients.clearBusy(pmParent);
			//workerPM.setValue(0);
			//workerPM.setVisible(false);
			workerPM.detach();
			workerPM = null;
			return 1;
		};
		
		if ( doOrShareUITaskFromWT(finishUITask, true, null) != 1 ) { // only through SP !
			logger.error("Ошибка выполнения завершающей логики (finishUITask) в ZKWorkerWithTimerAndPM.done() !");
			Clients.showNotification("Unable to complete task in ZKWorkerWithTimerAndPM.done() !", Clients.NOTIFICATION_TYPE_ERROR, null, null, 2000);
		} else {
			logger.trace("ZKWorkerWithTimerAndPM.done() успешно завершён (вначале родителем послано 'progress_down', выполнены userDoneUITask.run(), завершающая UI-логика (clearSharedLogic, удалены таймер и PM; clearBusy))");
		}
	} // protected final void done()

	/** Выполнить (по событию таймера) расшаренную рабочим потоком UI-задачу (например, визуализация прогресса). */
	private void applySharedLogicOnTimer(Event ev) { // вызывается из обработчика событий onTimer таймера workerTimer
		int res = -1;
		if ( !isCancelled() ) {
			res = workerSharedUILogic.doSharedLogicOnce(); // возвращает -1 при отсутствии расшаренной задачи
		}
		if (res != -1) logger.trace("ZKWorkerWithTimerAndPM.applySharedLogicOnTimer.  res = ", res); // "ev_class:"+ev.getClass().getName()
	} // public void applySharedLogicOnTimer(Event ev)

	
	/** Реализация интерфейса java.beans.PropertyChangeListener, слушающая и отображающая прогресс (события
	 * "progress" и "indeterm_progress" -> showBusy) на progressmeter. (нет вызовов извне: сами посылаем - сами слушаем).
	 * Визуализация выполняется либо посредством SP, либо расшариванием задачи для слушателя внутреннего таймера.
	 */
	@Override
	public final void propertyChange(PropertyChangeEvent pce) {
		Supplier<Integer> onEvent;
		//int pr;
		boolean useSP = false;
		Object src = pce.getSource();
		String pname = pce.getPropertyName();
		logger.trace("ZKWorkerWithTimerAndPM.propertyChange entry.  source = {}, propertyName = {}, new_value = {}, isCancelled() = {}", src, pname, pce.getNewValue(), isCancelled());
		
        if ( pname == "progress" && !isCancelled() ) {
        	int pr = (int)pce.getNewValue();
        	onEvent = () -> {
        		if ( !isCancelled() ) {
        			workerPM.setValue(pr);
// ищу немедленного обновления UI, но обновляется разом после завершения потока и потоки выполняются последовательно
					//Clients.response(new AuSetAttribute(workerPM, "value", pr)); // так накапливается и применяется (последовательно - плавно, параллельно с формированием книги) после завершения applyFilter()
					return Integer.valueOf(workerPM.getValue());
        		} else return 0;
			};

        } else if ( pname == "indeterm_progress" && !isCancelled() ) {
// "indeterm_progress": showBusy() на время формирования файла, потом неявно (при down) clearBusy() !!
        	onEvent = () -> {
        		if (  !isCancelled() ) {
        			Clients.showBusy(pmParent, "Please wait while downloading Workbook...");
        			return 1;
        		} else return 0;
        	};
    		useSP = true;
/*
        } else if ( pname == "progress_down" ) { // ! завершение (on "progress_down") даже при отмене !
    		onEvent = downloadFile;
    		useSP = true;
*/    		
        }
        else return;
        // далее выполняем задачу в зависимости от доступности UI
        int dfRes = doOrShareUITaskFromWT(onEvent, useSP, workerSharedUILogic);
        logger.trace("ZKWorkerWithTimerAndPM.propertyChange. '{}' new_value = {}, dfRes = {}", pname, pce.getNewValue(), dfRes);
	} // public void propertyChange(PropertyChangeEvent pce)
	
	
/* https://www.zkoss.org/javadoc/latest/zk/org/zkoss/zk/ui/Executions.html#activate(org.zkoss.zk.ui.Desktop)
 When you call this method, ZK framework would require a lock on the thread. In the rare case that the framework,
  at the same time, is waiting on the release of another lock from your application, then a deadlock would result.
 Returns:whether it is activated or it is timeout. The only reason it returns false is timeout.
 Throws:
  java.lang.InterruptedException - if it is interrupted by other thread;
  DesktopUnavailableException - if the desktop is removed (when activating);
  java.lang.IllegalStateException - if the server push is not enabled for this desktop yet (Desktop.enableServerPush(boolean)).
*/	
/*	protected int doOrShareUITaskFromWT(Supplier<Integer> onEvent, boolean useSP) {
		return doOrShareUITaskFromWT(onEvent, useSP, workerSharedUILogic);
	}
*/	
	/** Выполнить UI-задачу посредством SP и расшарить для слушателя внутреннего таймера.
	 * Вызывать можно из любого потока.
	 * SP пытаемся включить в цикле с разной задержкой (есть с этим проблемы).
	 * @param onEvent UI-задача.
	 * @param useSP Разрешение использовать Server Push.
	 * @param sharedLogic Thread-safe-контейнер для расшаривания задачи (опционально).
	 * @return Результат выполнения задачи. -1 если не выполнена, -2 если расшарена.
	 */
	private int doOrShareUITaskFromWT(Supplier<Integer> onEvent, boolean useSP, SingleSharedLogic sharedLogic/*nullable-optional*/) {
        boolean activated = false;
        boolean isUIThread = EventProcessor.inEventListener();
        int dfRes = -1;
		long startTime = System.nanoTime(), endTime = startTime;
		try {
			if ( !isUIThread && useSP ) { // включать SP только если не в UI thread
				logger.trace("ZKWorkerWithTimerAndPM.doOrShareUITaskFromWT. before desktop activation... pmDesktop = {}, rnd = {}", pmDesktop, rnd);
				for (int round = 5; round-- > 0; ) {
					activated = Executions.activate(pmDesktop, rnd.nextInt(1100, 1800)); // DR p.339
					if (activated || round <= 0) break;
					Thread.yield();
					Thread.sleep(rnd.nextInt(500, 1000));
				}
			}
			endTime = System.nanoTime(); // обычное время активации - 1-1,5 сек.
			if ( activated ) { // to Update UI in a Working Thread (p.339)
				try {
					dfRes = onEvent.get().intValue();
				} finally {
					Executions.deactivate(pmDesktop);
				}
			} else if ( isUIThread ) {
				dfRes = onEvent.get().intValue();
			} else if ( !isUIThread && sharedLogic != null/*gridDM.getModelLock().isLocked()*//*isHeldByCurrentThread()*/ ) { // working thread & desktop not activated & hold modelLock -> share task
				logger.trace("ZKWorkerWithTimerAndPM.doOrShareUITaskFromWT. Share logic.");
				sharedLogic.addSharedLogic(onEvent); // сейчас не накапливается, а перетирает невыполненную задачу; а таймер или ждущий блокировки интерфейсный поток попробует вывести прогресс (выполнить расшаренную задачу)
				dfRes = -2;
			} else {
				logger.error("Невозможно выполнить задачу в ZKWorkerWithTimerAndPM.doOrShareUITaskFromWT");
   			}
   		} catch (InterruptedException e) {
   			logger.warn("InterruptedException in ZKWorkerWithTimerAndPM.doOrShareUITaskFromWT (при активации рабочего стола)");
   			//Executions.deactivate(pmDesktop);
   			//Thread.currentThread().interrupt();
   		} catch(Throwable e) {
   			logger.error("Unpredictable Exception in ZKWorkerWithTimerAndPM.doOrShareUITaskFromWT" , e);
   			throw new InternalError("Unpredictable Exception in ZKWorkerWithTimerAndPM.doOrShareUITaskFromWT", e);
		}
		logger.debug("ZKWorkerWithTimerAndPM.doOrShareUITaskFromWT.  dfRes = {}, useSP = {}, desktop activated: {}, was waiting for {} ms. (currentThread: {}, class: {} UIThread ? : {})", dfRes, useSP, activated, Math.round((endTime-startTime)/1e6d), Thread.currentThread(), Thread.currentThread().getClass().getSimpleName(), isUIThread);
		return dfRes;
	} // private int doOrShareUITaskFromWT(Supplier<Integer> onEvent, boolean useSP, SingleSharedLogic sharedLogic)
	
	
	/** Проверить наличие задачи, расшаренной от WT к UI-thread. */
	public final int sizeSharedLogic() {
		return workerSharedUILogic == null ? 0 : workerSharedUILogic.sizeSharedLogic();
	}
	
} // public class ZKWorkerWithTimerAndPM<V> extends AsyncTask<V> implements PropertyChangeListener, Serializable