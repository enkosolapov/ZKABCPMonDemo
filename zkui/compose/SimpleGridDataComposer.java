package basos.zkui.compose;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import org.zkoss.util.media.AMedia;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.sys.DesktopCtrl;
import org.zkoss.zk.ui.impl.EventProcessor;
import org.zkoss.zk.ui.select.SelectorComposer;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zk.ui.util.DesktopCleanup;
import org.zkoss.zul.Column;
import org.zkoss.zul.Filedownload;
import org.zkoss.zul.Listheader;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.impl.HeaderElement;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.EventQueue;
import org.zkoss.zk.ui.event.EventQueues;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.event.impl.DesktopEventQueue;

import basos.data.zkmodel.GridDataModelMan;
import basos.data.zkmodel.ListModelToExcelWriter;
import basos.zkui.util.GlobalCleaner;
import basos.zkui.util.ZKWorkerWithTimerAndPM;


/** Расширение {@link SelectorComposer} с поддержкой гридов/листбоксов с моделью {@link GridDataModelMan GridDataModelMan&lt;GridData&lt;T&gt;&gt;}.
 * Выгрузка в Excel. Диспетчеризация конкурирующих за лист-модель задач. Thread-safe сортировка заголовков.
 * Наследник должен переопределить абстрактный метод {@link #initAfterCompose}, который вызывается из {@link #doAfterCompose}.
 */
public class SimpleGridDataComposer<T extends Component> extends SelectorComposer<Component> {
	private static final long serialVersionUID = 5086794140452503736L;
	
	private static final Logger logger = LoggerFactory.getLogger(SimpleGridDataComposer.class);
	protected/*public*/ static final Marker concurMarker = MarkerFactory.getMarker("CONCUR");
	
	@WireVariable // autowire implcit var
	protected/*private*/ Desktop desktop;

	// кнопки toolbar принадлежат tabbox (вне tabpanels)
	//@Wire private Toolbarbutton toExcelToolBB;
    
    /** Implementation specific part of doAfterCompose (exec at the and of). */
    protected void initAfterCompose(Component comp) throws Exception {}
    
	@Override
	public void doAfterCompose(Component comp) throws Exception {
    	super.doAfterCompose(comp);
// FIXME: параметры (https://www.zkoss.org/javadoc/latest/zk/org/zkoss/zk/ui/sys/DesktopCtrl.html#enableServerPush(org.zkoss.zk.ui.sys.ServerPush)) ?
		((DesktopCtrl)desktop).enableServerPush(true, this/*иначе EventQueue выключает*/);
    	initAfterCompose(comp); // частности
	} // public void doAfterCompose(Component comp) throws Exception
	
	
	/** Desktop-level thread pool for WTs. Отложенная инициализация. Хранится в атрибуте десктопа "asyncExecutor".
	 * При создании регистрируется финализирующая команда.
	 */
// TODO:HOWTO: уровень WebApp(!) ?? хранить в атрибутах WebApp(!)/Singleton; объявлять в WebAppInit; создавать сразу ! размер зависит от кол-ва пользователей (пиковая нагрузка; но тогда и высвобождения не будет) ?
	public ExecutorService getAsyncExecutor() { // сейчас используется при выгрузке в Excel и диспетчером
		ExecutorService asyncExecutor = null;
		Object tmp = desktop.getAttribute("asyncExecutor");
		if (tmp == null) {
			Object deskMutex = desktop.getAttribute("deskMutex"); // создаётся в AbcpmocDesktopInit
			if (deskMutex == null) {
				logger.error("Desktop-level attribute 'deskMutex' not found.");
				throw new InternalError("Desktop-level attribute 'deskMutex' not found.");
			}
			synchronized (deskMutex) {
				tmp = desktop.getAttribute("asyncExecutor");
				if (tmp == null) { // отложенная инициализация
					int asyncExecutorFixedThreadPoolSize = Integer.valueOf(Labels.getLabel("dataGridComposer.asyncExecutorFixedThreadPoolSize", "3")).intValue();
					asyncExecutor = Executors.newFixedThreadPool(asyncExecutorFixedThreadPoolSize)/*newSingleThreadExecutor()*/;
					ExecutorService asyncExecutorParam = asyncExecutor;
					Runnable cleanupCmndAE = () -> {
				   		logger.debug("cleanupCmndAE.run(), asyncExecutor = {}", asyncExecutorParam);
				   		if (asyncExecutorParam != null && !asyncExecutorParam.isTerminated()) {
				   			asyncExecutorParam.shutdown();
				   		}
				    };
					GlobalCleaner.registerCommand(cleanupCmndAE, DesktopCleanup.class); // вызывается при уходе со страницы, таймауте, закрытии вкладки(! в т.ч. timeout.zul) на пару, но после ExecutionCleanup; иногда дважды подряд в одном потоке
					desktop.setAttribute("asyncExecutor", asyncExecutor);
					logger.trace("getAsyncExecutor.  ExecutorService was created with {} pool size for Desktop {}", asyncExecutorFixedThreadPoolSize, desktop.getId());
				} else { asyncExecutor = (ExecutorService)tmp; }
			} // synchronized
		} else { asyncExecutor = (ExecutorService)tmp; }
		return asyncExecutor;
	} // public ExecutorService getAsyncExecutor()
	
	
	protected transient volatile ZKWorkerWithTimerAndPM<AMedia> gridModelToExcelWorker; // создаётся заново при каждой выгрузке в Excel

	protected <U extends Object & Serializable & Comparable<? super U>> void downloadToExcel
			( GridDataModelMan<U> lmlx
			, List<String> hdr/*nullable*/
			, Component holder
			, Toolbarbutton tbb/*nullable*/) {
		
		if (gridModelToExcelWorker != null) { // разрешаем не более одной выгрузки в одно время
			Clients.showNotification("Parallel work prohibited !");
			//logger.error("Inconsistency in downloadToExcel: gridModelToExcelWorker != null");
			return;
		}
// FIXME: проверка входных параметров
		if (lmlx == null) {
			logger.error("Null argument 'lmlx' not allowed !");
			throw new NullPointerException("Null argument 'lmlx' not allowed !");
		}
		if (holder == null) {
			logger.error("Null argument 'holder' not allowed !");
			throw new NullPointerException("Null argument 'holder' not allowed !");
		}

		Supplier<Integer> downloadFile = () -> { // UI task (ZKWorkerWithTimerAndPM:userDoneUITask) !
			int ret_res = 0;
			logger.trace("Supplier composer.downloadFile. entry");
			if ( gridModelToExcelWorker != null && gridModelToExcelWorker.isDone() && !gridModelToExcelWorker.isCancelled() ) {
				AMedia amedia;
				try {
					amedia = gridModelToExcelWorker.get();
					if (amedia != null) {
						Filedownload.save(amedia);
						ret_res = 1;
					} else {
						logger.error("amedia is null in Supplier composer.downloadFile");
					}
				} catch(java.util.concurrent.ExecutionException | InterruptedException e) {
					logger.error("Exception in Supplier composer.downloadFile :", e);
				} catch(Exception ex) {
					logger.error("Unpredictable Exception in Supplier composer.downloadFile :", ex);
				} finally {
					//gridModelToExcelWorker = null;
				}
			}
			if (tbb != null) tbb.setDisabled(false);
			if ( gridModelToExcelWorker != null && gridModelToExcelWorker.isCancelled() ) {
				Clients.showNotification("downloadToXLSX() was terminated !", Clients.NOTIFICATION_TYPE_INFO, null, null, 2000);
			}
			logger.trace("Supplier composer.downloadFile.  ret_res = {}, gridModelToExcelWorker.isCancelled() = {}", ret_res, gridModelToExcelWorker.isCancelled());
			gridModelToExcelWorker = null; // ? (этот код при SP вызывает сам worker) ?
			return Integer.valueOf(ret_res);
		}; // Supplier<Integer> downloadFile
		
		ListModelToExcelWriter<U> listModelToExcelWriter = new ListModelToExcelWriter<>(lmlx);
		if (hdr != null) {
			listModelToExcelWriter.setHeader(hdr);
		}
		if (tbb != null) {
			tbb.setDisabled(true); // защита от повторного запуска (в ZUL autodisable="+self", т.е. нужно разрешить явно)
		}
			
// запускаем в отдельном процессе (каждое событие обрабатывается в отдельном процессе, но последовательно, синхронно) !!!
// вместо этого можно инициировать асинхронное событие (тоже working thread, см. Long Operations, EventQueues p. 264)
		logger.trace("downloadToExcel. before construct Worker.  UIThread ? : {}", EventProcessor.inEventListener() );
		
		gridModelToExcelWorker = new ZKWorkerWithTimerAndPM<AMedia>(
				() -> { return listModelToExcelWriter.exportModelDownloadExcel(gridModelToExcelWorker, null/*hdr*/); 
				}
			   ,() -> { int dfRes = downloadFile.get().intValue();
			   			logger.trace("ZKWorkerWithTimerAndPM.downloadToExcel.userDoneUITask завершён с рез-том dfRes = {}", dfRes);
			   	}
			   ,holder);
		
		//new Thread(gridModelToExcelWorker, "export_thread").start();
		getAsyncExecutor().execute(gridModelToExcelWorker);
		logger.trace("downloadToExcel().  gridModelToExcelWorker executed");						
	} // protected void downloadToExcel
	
	
	private static ThreadLocalRandom rnd = ThreadLocalRandom.current();
	
	/** Диспетчер, через который запускаем на выполнение операции, меняющие лист-модель, обеспечивая синхронизацию.
	 * Invoker that executes command taskToRun immediately or defers execution trying to acquire model lock in
	 *  separate WT (first subscribes a synchronous listener) then publish event wich in turn run command in UI thread.
	 * Задачи, меняющие дата-модель грида (и требующие эксклюзивной блокировки), запускаем сразу при свободном замке
	 *  (самостоятельно управляя блокировкой) или ставим в очередь (рабочий поток - WT - ждёт блокировки),
	 *  чтобы не держать UI-Thread (иначе интерфейс становтся неотзывчивым).
	 * В случае отложенной задачи WT ждёт и лочит модель, передаёт штамп в обработчик события, который (не передавая
	 *  его задаче, точнее, передавая 0L) разлочивает оконечно замок сам. Для надёжности WT синхронизируется с
	 *  событием и сам разлочивает через 5 сек. при отсутствии сигнала начала выполнения события;
	 *  т.о. гарантируется разблокировка, если событие не случилось или затянулось.
	 * Событие отсылается посредством SP при активном Desktop, с чем есть проблемы, потому пытаемся в цикле.
	 * (DR p.280) [Since ZK 7.0.0 deprecated to enable the event thread according to Java Servlet Specification that may prohibit the creation of new threads]
	 * @param gdm Лист-модель, операции по изменению которой синхронизируем.
	 * @param taskToRun Основная команда.
	 * @param msg Текстовое сообщение (основа), выводимое в лог и на экран (Clients.showNotification()).
	 * @param beforePostpone Команда (nullable), выполняемая в случае отложенного запуска сразу (online, в вызывающем UI-потоке), до ожидания блокировки в WT. Например, блокировка контролов на период ожидания.
	 * @param beforeDeferredRun Команда (nullable), выполняемая в случае отложенного запуска в синхронном обработчике непосредственно перед выполнением основной команды. Например, разблокировка контролов, зблокированных в beforePostpone.
	 */
	protected void dispatchGridModelLockingTask(GridDataModelMan<?> gdm, Consumer<Long> taskToRun, String msg, Runnable beforePostpone, Runnable beforeDeferredRun) {
// FIXME: проверка входных параметров !!
// TODO: контролируем, что вызов в UIThread (EventProcessor.inEventListener()) !!
// FIXME: очередь на блокирующей очереди и ОДНОМ рабочем потоке-диспетчере !! Ещё есть длительные асинхронные операции блокировкой на чтение в пуле рабочих потоков !
// TODO: оптимизация: из накопившихся однотипных задач (сортировка, применение фильтра) имеет смысл запускать только последнюю !!
    	logger.trace(concurMarker, "{} (UI)...entry...isLocked: {}", msg, gdm.isModelLocked());
		
    	if ( gdm.isModelLocked() ) {
    		
    	    EventQueue<Event> syncEQ = EventQueues.lookup("syncEQ"); // синхронная (UI) очередь отложенных задач - сериализуемая ! // Returns the desktop-level event queue with the specified name in the current desktop, or if no such event queue, create one
			Object synchroStuff = new Object();
			String uniqueEventName = UUID.randomUUID().toString();
			AtomicReference<Thread> evThread = new AtomicReference<>();
			
			EventListener<Event> listener = new SerializableEventListener<Event>() {
				private static final long serialVersionUID = -7720662733806148360L;

				public void onEvent(Event ev) {
					if ( uniqueEventName.equals(ev.getName()) ) {
						long stamp = 0L;
						boolean unlock = false;
						try {
							stamp = ((Long)ev.getData()).longValue();
							//synchronized(synchroStuff) {TimeUnit.MILLISECONDS.timedWait(synchroStuff, 7000);}
							if (stamp != 0L) synchronized(synchroStuff) {
								evThread.set(Thread.currentThread());
								synchroStuff.notifyAll(); // сообщаем ланчеру, что всё пучком - можно завершаться: замок будет гарантированно разлочен
							}
							if (beforeDeferredRun != null) beforeDeferredRun.run();
							if ( !gdm.isModelLocked() ) {
								logger.error(concurMarker, "{} (UI)... INCONSISTENT STATE on handle task {}: already unlocked WriteLock with stamp {}. RETURN without exec task !!", msg, uniqueEventName, stamp);
								stamp = 0L;
								return;
							}
							Clients.showNotification("Grid data model is unlocked, do postponed task "+uniqueEventName, Clients.NOTIFICATION_TYPE_INFO, null, null, 2000);
							taskToRun.accept(0L/*stamp*/); // задача замок не получает; синхронизируем всю задачу из обработчика события
							//synchronized(synchroStuff) {TimeUnit.MILLISECONDS.timedWait(synchroStuff, 7000);}
						//} catch(InterruptedException e) {
						} finally {
			    			if ( stamp != 0L ) {
		    					try {
		    						gdm.getModelRWLock().unlock(stamp);
		    						unlock = true;
		    					} catch (Throwable e) {}
			    			}
							syncEQ.unsubscribe(this/*listener*/); // this$0 ??
						}
						logger.trace("{} (UI)...onEvent. Complete task {} then {} release WriteLock; stamp = {}, syncEQ.isIdle() ? {}", msg, uniqueEventName, (unlock ? "successfully":"can't"), stamp, ((DesktopEventQueue<Event>)syncEQ).isIdle());
					}
				} // onEvent
			}; // EventListener
			
			logger.debug("{} (UI)...before execute async waiter & subscribe listener for event {}, syncEQ.isIdle() ? {}", msg, uniqueEventName, ((DesktopEventQueue<Event>)syncEQ).isIdle());
			syncEQ.subscribe(listener);
			if (beforePostpone != null) beforePostpone.run();
			Clients.showNotification("Grid data model is locked, postpone task "+uniqueEventName, Clients.NOTIFICATION_TYPE_INFO, null, null, 2000);
			
			getAsyncExecutor().execute( () -> { // в рабочем потоке (ланчере) ждём освобождения модели, после чего публикуем событие, обработчик которого запустит отложенную задачу в новом UI Thread (UI поток не должен висеть и ждать блокировки !)
				logger.trace(concurMarker, "{} (ASYNC)...waiting for WriteLock before send event {}", msg, uniqueEventName);
	    		long stamp = gdm.getModelRWLock().writeLock(); // StampedLock с передачей штампа в UIThread (чтобы гарантировать перевоочередную обработку своего события)
	    		boolean activated = false;
	    		try {
					for (int round = 3; round-- > 0; ) { // 1) иначе не выполнить syncEQ.publish(); 2) берегись deadlock !!!
						logger.trace(concurMarker, "{} (ASYNC)...acquire WriteLock, stamp = {}, wait for desktop activation...round {} of 3", msg, stamp, (3-round));
						activated = Executions.activate(desktop, rnd.nextInt(1300, 2000)); // DR p.339
						if (activated || round <= 0) break;
						gdm.getModelRWLock().unlock(stamp);
						Thread.yield();
						Thread.sleep(rnd.nextInt(300, 1200));
						stamp = gdm.getModelRWLock().writeLock();
					}
	    		} catch (InterruptedException e) {
	    			//Thread.currentThread().interrupt();
	    			logger.warn(concurMarker, "{} (ASYNC)...InterruptedException in composer.dispatchGridModelLockingTask(execute)", msg);
	    		} catch (Exception e) {
	    			logger.error(concurMarker, "{} (ASYNC)...Unexpetced Exception in composer.dispatchGridModelLockingTask(execute)", msg, e);
	    		} finally {
	    			//gdm.getModelRWLock().unlock(stamp);
	    			if (activated) {
	    				logger.trace("{} (ASYNC)...activate desktop then publish event {}, write_stamp = {}, syncEQ.isIdle() ? {}", msg, uniqueEventName, stamp, ((DesktopEventQueue<Event>)syncEQ).isIdle());
						syncEQ.publish(new Event(uniqueEventName, null, Long.valueOf(stamp))); // notify it is done (with usage SP)
						Executions.deactivate(desktop);
						try {
							synchronized(synchroStuff) {
								for (int round = 5; evThread.get() == null & round-- > 0; ) {
									TimeUnit.MILLISECONDS.timedWait(synchroStuff, 1000);
								}
							}
						} catch (InterruptedException e) {
						} finally {
							if ( evThread.get() == null ) { // не дождались запуска события
		    					syncEQ.unsubscribe(listener);
		    					boolean unlock = false;
		    					try {
		    						gdm.getModelRWLock().unlock(stamp);
		    						unlock = true;
		    					} catch (Throwable e) {
		    						
		    					}
		    					logger.warn(concurMarker, "{} (ASYNC)...event didn't occur within 5 sec. -> drop event {} & {} release WriteLock with stamp {}, syncEQ.isIdle() ? {}", msg, uniqueEventName, (unlock ? "successfully":"can't"), stamp, ((DesktopEventQueue<Event>)syncEQ).isIdle());
							} else {
								// FIXME: проверить syncEQ на утечку ! (https://en.wikipedia.org/wiki/Lapsed_listener_problem)
							}
						}
	    			} else { // замок захватили, но десктоп активировать не смогли
	    				syncEQ.unsubscribe(listener);
	    				gdm.getModelRWLock().unlock(stamp);
	    				logger.warn(concurMarker, "{} (ASYNC)...unable to activate desktop -> drop event {} & RELEASE WriteLock with stamp {}, syncEQ.isIdle() ? {}", msg, uniqueEventName, stamp, ((DesktopEventQueue<Event>)syncEQ).isIdle());	    				
	    			}
	    		} // finally
			}); // execute luncher
		} else { // FIXME: (запуск в текущем UI-потоке в отсутствие блокировки) всё меняется, делать tryLock, если уже захвачен - идти по ветке ланчера !!
			long stamp = gdm.getModelRWLock().writeLock();
			logger.trace(concurMarker, "{} (UI)...acquire WriteLock; stamp = {}", msg, stamp);
			try {
				taskToRun.accept(0L/*stamp*/); // замок модели был свободен - запускаем задачу сразу (в этом UI-потоке)
			} finally {
				gdm.getModelRWLock().unlock(stamp);
				logger.trace(concurMarker, "{} (UI)...release WriteLock after complete (right away) task; stamp = {}", msg, stamp);
			}
		}
    } // void dispatchGridModelLockingTask
	
	
	/** Сортировка колонки грида/листбокса с синхронизацией лист-модели.
	 * @param hdr Колонка, в которой нужно сортировать.
	 * @param isAsc Направление сортировки.
	 * @param lmlx Лист-модель грида/листбокса.
	 */
	public void safeSort(HeaderElement hdr, boolean isAsc, GridDataModelMan<?> lmlx) {
		if ( hdr instanceof Listheader ) {
			Listheader col = (Listheader)hdr;
	    	String msg = "onSortCol on "+col.getLabel()+" to "+(isAsc ? "ASC" : "DESC");
	    	Consumer<Long> taskToRun = (stamp) -> {
	    		try {
	    			logger.debug(concurMarker, "{} (UI)...taskToRun. Before sort, stamp: {}", msg, stamp);
	    			col.sort(isAsc);
	    		} finally {
	    			if ( stamp != 0L ) {
	    				lmlx.getModelRWLock().unlock(stamp);
	    				logger.trace(concurMarker, "{} (UI)...WriteLock released in taskToRun; stamp = {}", msg, stamp);
	    			}
	    		}
	    	};
			dispatchGridModelLockingTask(lmlx, taskToRun, msg, null, null);
		} // listbox
		else if (hdr instanceof Column) {
			Column col = (Column)hdr;
	    	String msg = "onSortCol on "+col.getLabel()+" to "+(isAsc ? "ASC" : "DESC");
	    	Consumer<Long> taskToRun = (stamp) -> {
	    		try {
	    			logger.debug(concurMarker, "{} (UI)...taskToRun. Before sort, stamp: {}", msg, stamp);
	    			col.sort(isAsc);
	    		} finally {
	    			if ( stamp != 0L ) {
	    				lmlx.getModelRWLock().unlock(stamp);
	    				logger.trace(concurMarker, "{} (UI)...WriteLock released in taskToRun; stamp = {}", msg, stamp);
	    			}
	    		}
	    	};
			dispatchGridModelLockingTask(lmlx, taskToRun, msg, null, null);
		} // grid
	} // public void safeSort(HeaderElement hdr, boolean isAsc, GridDataModelMan<?> lmlx)
	
} // public class SimpleGridDataComposer extends SelectorComposer<Component>