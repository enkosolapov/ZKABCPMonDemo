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


/** ��� ���������� ���������� �������� � working (not UI) thread � ������������� ���������.
 * �������� ������� ��� ��������� ������ � ������� � ����������� Progressmeter.
 * ������������ ��������: worker.firePropertyChange("progress", oldValue, newValue, false). ����� ��������� ������� "indeterm_progress" -> showBusy.
 * ��������� � UI ���������� �� ���������� UI-������, � ������� �������������� ������� onTimer (������ �������� �
 *  ����������� � ������ run()), ����� ����� Server Push (����� ���������� Timer).
 * ��� ������� ������� � ����� �������� ������� � ���������� �� ����� "worker.timerDelay".
 * ������ ������������ ��������� ����� ������ �������� ������������ ���� Supplier<V>; ������ ����� ������������
 *  ���������� ���� Runnable ��� ������, ������� ����������� � done() (������ �� �� ���������� worker.get()).
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
	private final Component pmParent; // ����� �������� ������, ������������, ��������� ����������
	
	private /*transient*/ volatile Timer workerTimer;
	private Progressmeter workerPM; // for long operations in working threads; operating by mean of JavaBeans events model
	private Desktop pmDesktop;
	private int timerDelay;
	
	private static final ThreadLocalRandom rnd = ThreadLocalRandom.current();
	private final /*transient*/ SingleSharedLogic workerSharedUILogic = new SingleSharedLogic(); // WT ����������� ����� ��� ���������� � UI-Thread (������������, ���� �� ������ �����������)
	//private final String workerTimerId = UUID.randomUUID().toString();
	
	/** Should be executed in UI Thead.
	 @param userMainWTask ������ ������������ (mandatory). ���������� � doInBackground(), ������� ���������� �� ��������� run().
	 @param userDoneUITask ������, ������� ����������� � done() (������ �� �� ���������� worker.get()) (mandatory)
	 @param pmParent ������������ ���������, � ������� �������� ������, ������������, ��������� ���������� (mandatory)
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
		// � ui-thread ��������� SP, ������� ����������� ��� ������������� � run() !!!
		if ( !EventProcessor.inEventListener() ) {
			logger.error("Should be executed in UI Thead !");
			throw new IllegalStateException("Should be executed in UI Thead !");
		}
		pmDesktop = pmParent.getDesktop();
		((DesktopCtrl)pmDesktop).enableServerPush(true, this/*����� ����������� EventQueue*/); // TODO: ��������� (https://www.zkoss.org/javadoc/latest/zk/org/zkoss/zk/ui/sys/DesktopCtrl.html#enableServerPush(org.zkoss.zk.ui.sys.ServerPush)) ?
		timerDelay = Integer.valueOf(Labels.getLabel("worker.timerDelay", "100")).intValue();
	} // constr
	
	
	/** {@inheritDoc}
	 * <p/> �� ���������� �������� ���������������� ������ ��� ������ ������������� ������, ���������������� UI:
	 *  ��������� progressmeter, ������ (��� �������� ���������� � ��������� � ������������) � ��������� ������� (WT ����������� ��� ���� ������ ������������ ���������).
	 * ��� ������� ������� � ����� �������� ������� � ���������� �� ����� "worker.timerDelay".
	 */
	@Override
	public final void run() {
		addPropertyChangeListener("progress", this);
		//worker.addPropertyChangeListener("progress_down", worker); // ���������� � worker.done()
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
		logger.trace("����������� UI-����� ZKWorkerWithTimerAndPM.run() ��������� ������� (��������� ���� ���������� 'progress' � 'indeterm_progress'; ������ PM, ������� Timer � ��� EventListener), ����� ��������� ����� super.run()");
		super.run();
	} // public void run()
	
	/** ����������� ������, ����������� � ������������. ���������� � {@link AsyncTask#run()} */
	@Override
	protected final V doInBackground() throws Exception { // �� ������ abstract ����� ���� ����������� ���-�� �������������� ������������
		logger.trace("ZKWorkerWithTimerAndPM.doInBackground() ����� userMainWTask.get()");
    	return userMainWTask.get();
    }

	/** {@inheritDoc}
	 * <p/> ������� ����������� �������������� ���������������� �������������� ������, ������������� ����� �����������.
	 * ����� ����������� UI.
	 */
	@Override
	protected final void done() {
		Supplier<Integer> finishUITask = () -> {
			if ( userDoneUITask != null ) userDoneUITask.run();
			workerPM.setValue(100);
			workerSharedUILogic.clearSharedLogic(); // ������, ��� ������ ��������� � ����� �������� � ��� ������ ����������� !!
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
			logger.error("������ ���������� ����������� ������ (finishUITask) � ZKWorkerWithTimerAndPM.done() !");
			Clients.showNotification("Unable to complete task in ZKWorkerWithTimerAndPM.done() !", Clients.NOTIFICATION_TYPE_ERROR, null, null, 2000);
		} else {
			logger.trace("ZKWorkerWithTimerAndPM.done() ������� �������� (������� ��������� ������� 'progress_down', ��������� userDoneUITask.run(), ����������� UI-������ (clearSharedLogic, ������� ������ � PM; clearBusy))");
		}
	} // protected final void done()

	/** ��������� (�� ������� �������) ����������� ������� ������� UI-������ (��������, ������������ ���������). */
	private void applySharedLogicOnTimer(Event ev) { // ���������� �� ����������� ������� onTimer ������� workerTimer
		int res = -1;
		if ( !isCancelled() ) {
			res = workerSharedUILogic.doSharedLogicOnce(); // ���������� -1 ��� ���������� ����������� ������
		}
		if (res != -1) logger.trace("ZKWorkerWithTimerAndPM.applySharedLogicOnTimer.  res = ", res); // "ev_class:"+ev.getClass().getName()
	} // public void applySharedLogicOnTimer(Event ev)

	
	/** ���������� ���������� java.beans.PropertyChangeListener, ��������� � ������������ �������� (�������
	 * "progress" � "indeterm_progress" -> showBusy) �� progressmeter. (��� ������� �����: ���� �������� - ���� �������).
	 * ������������ ����������� ���� ����������� SP, ���� ������������� ������ ��� ��������� ����������� �������.
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
// ��� ������������ ���������� UI, �� ����������� ����� ����� ���������� ������ � ������ ����������� ���������������
					//Clients.response(new AuSetAttribute(workerPM, "value", pr)); // ��� ������������� � ����������� (��������������� - ������, ����������� � ������������� �����) ����� ���������� applyFilter()
					return Integer.valueOf(workerPM.getValue());
        		} else return 0;
			};

        } else if ( pname == "indeterm_progress" && !isCancelled() ) {
// "indeterm_progress": showBusy() �� ����� ������������ �����, ����� ������ (��� down) clearBusy() !!
        	onEvent = () -> {
        		if (  !isCancelled() ) {
        			Clients.showBusy(pmParent, "Please wait while downloading Workbook...");
        			return 1;
        		} else return 0;
        	};
    		useSP = true;
/*
        } else if ( pname == "progress_down" ) { // ! ���������� (on "progress_down") ���� ��� ������ !
    		onEvent = downloadFile;
    		useSP = true;
*/    		
        }
        else return;
        // ����� ��������� ������ � ����������� �� ����������� UI
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
	/** ��������� UI-������ ����������� SP � ��������� ��� ��������� ����������� �������.
	 * �������� ����� �� ������ ������.
	 * SP �������� �������� � ����� � ������ ��������� (���� � ���� ��������).
	 * @param onEvent UI-������.
	 * @param useSP ���������� ������������ Server Push.
	 * @param sharedLogic Thread-safe-��������� ��� ������������ ������ (�����������).
	 * @return ��������� ���������� ������. -1 ���� �� ���������, -2 ���� ���������.
	 */
	private int doOrShareUITaskFromWT(Supplier<Integer> onEvent, boolean useSP, SingleSharedLogic sharedLogic/*nullable-optional*/) {
        boolean activated = false;
        boolean isUIThread = EventProcessor.inEventListener();
        int dfRes = -1;
		long startTime = System.nanoTime(), endTime = startTime;
		try {
			if ( !isUIThread && useSP ) { // �������� SP ������ ���� �� � UI thread
				logger.trace("ZKWorkerWithTimerAndPM.doOrShareUITaskFromWT. before desktop activation... pmDesktop = {}, rnd = {}", pmDesktop, rnd);
				for (int round = 5; round-- > 0; ) {
					activated = Executions.activate(pmDesktop, rnd.nextInt(1100, 1800)); // DR p.339
					if (activated || round <= 0) break;
					Thread.yield();
					Thread.sleep(rnd.nextInt(500, 1000));
				}
			}
			endTime = System.nanoTime(); // ������� ����� ��������� - 1-1,5 ���.
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
				sharedLogic.addSharedLogic(onEvent); // ������ �� �������������, � ���������� ������������� ������; � ������ ��� ������ ���������� ������������ ����� ��������� ������� �������� (��������� ����������� ������)
				dfRes = -2;
			} else {
				logger.error("���������� ��������� ������ � ZKWorkerWithTimerAndPM.doOrShareUITaskFromWT");
   			}
   		} catch (InterruptedException e) {
   			logger.warn("InterruptedException in ZKWorkerWithTimerAndPM.doOrShareUITaskFromWT (��� ��������� �������� �����)");
   			//Executions.deactivate(pmDesktop);
   			//Thread.currentThread().interrupt();
   		} catch(Throwable e) {
   			logger.error("Unpredictable Exception in ZKWorkerWithTimerAndPM.doOrShareUITaskFromWT" , e);
   			throw new InternalError("Unpredictable Exception in ZKWorkerWithTimerAndPM.doOrShareUITaskFromWT", e);
		}
		logger.debug("ZKWorkerWithTimerAndPM.doOrShareUITaskFromWT.  dfRes = {}, useSP = {}, desktop activated: {}, was waiting for {} ms. (currentThread: {}, class: {} UIThread ? : {})", dfRes, useSP, activated, Math.round((endTime-startTime)/1e6d), Thread.currentThread(), Thread.currentThread().getClass().getSimpleName(), isUIThread);
		return dfRes;
	} // private int doOrShareUITaskFromWT(Supplier<Integer> onEvent, boolean useSP, SingleSharedLogic sharedLogic)
	
	
	/** ��������� ������� ������, ����������� �� WT � UI-thread. */
	public final int sizeSharedLogic() {
		return workerSharedUILogic == null ? 0 : workerSharedUILogic.sizeSharedLogic();
	}
	
} // public class ZKWorkerWithTimerAndPM<V> extends AsyncTask<V> implements PropertyChangeListener, Serializable