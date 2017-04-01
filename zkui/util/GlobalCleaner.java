package basos.zkui.util;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.zkoss.util.Cleanups;
//import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
//import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Session;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.WebApp;
import org.zkoss.zk.ui.WebApps;
//import org.zkoss.zk.ui.event.Event;
//import org.zkoss.zk.ui.event.EventThreadCleanup;
import org.zkoss.zk.ui.util.DesktopCleanup;
//import org.zkoss.zk.ui.util.ExecutionCleanup;
import org.zkoss.zk.ui.util.SessionCleanup;
import org.zkoss.zk.ui.util.WebAppCleanup;

// FIXME: !!! некоректно при перечтении страницы (сессия та же, задачи дублируются при повторном вызове doAfterCompose()) !!! временно использую clear()
// TESTME
// TODO: регистрировать в web.xml (see ZK Configuration Reference) ?

/** Для каждого скоупа поддерживается реестр финализирующих задач.
 * Приложение одно, для него список. Для сессий, десктопов, executions структуры типа Map<Object, List<Runnable>>.
 * В методах {@link #cleanup} выполняются все задачи, назначенные указанному экземпляру скоупа, список очищается.
 */
public class GlobalCleaner implements Cleanups.Cleanup/*, EventThreadCleanup*/, DesktopCleanup, SessionCleanup, WebAppCleanup/*, ExecutionCleanup*/, Serializable {
// Для каждого интерфейса (кроме Cleanups.Cleanup ?):
// An independent instance of the given class is instantiated each time before the method is invoked.
// It means it is thread safe, and all information stored in non-static members will be lost after called.
	private static final long serialVersionUID = -2466206638979240406L;

	private static final Logger logger = LoggerFactory.getLogger(GlobalCleaner.class);
	
// FIXME: здесь нужны WeakReference, чтобы не держать ссылки на протухшие объекты, которые не вызвали Cleanup !!
	// м.б. несколько команд на сессию
	private static ConcurrentHashMap<Object, List<Runnable>> sessionCommands = new ConcurrentHashMap<>();
	// м.б. несколько команд на десктоп
	private static ConcurrentHashMap<Object, List<Runnable>> desktopCommands = new ConcurrentHashMap<>();
	// м.б. несколько команд на приложение, но приложение одно (конкуренция минимальна)
	private static List<Runnable> appCommands = new LinkedList<>();
	// м.б. несколько команд на Execution (HttpServletRequest ?-? Page); запросов много в каждой сессии одного приложения
//	private static ConcurrentHashMap<Object, List<Runnable>> executionCommands = new ConcurrentHashMap<>();
	
	/** Очистка перечня финализирующих задач текущего экземпляра (current Session, Desktop etc.) указанного скоупа.
	 * @param scope SessionCleanup.class / DesktopCleanup.class / WebAppCleanup.class / ExecutionCleanup.class
	 */
	public static void clear(Class<?> scope) {
		if ( SessionCleanup.class.isAssignableFrom(scope) ) {
			sessionCommands.remove(Sessions.getCurrent());
		} else if ( DesktopCleanup.class.isAssignableFrom(scope)) {
			desktopCommands.remove(Executions.getCurrent().getDesktop());
		} else if ( WebAppCleanup.class.isAssignableFrom(scope)) {
			synchronized (appCommands) {
				appCommands.clear();
			}
/*		} else if ( ExecutionCleanup.class.isAssignableFrom(scope)) {
			executionCommands.remove(Executions.getCurrent());*/
		}
	} // public static void clear(Class<?> scope)
	
	/** Зарегистрировать финализирующую задачу для текущего экземпляра (current Session, Desktop etc.) указанного скоупа.
	 * @param scope SessionCleanup.class / DesktopCleanup.class / WebAppCleanup.class / ExecutionCleanup.class
	 */
	public static void registerCommand(Runnable cmnd, Class<?> scope) {
		
		if ( SessionCleanup.class.isAssignableFrom(scope) ) {
			int ms = sessionCommands.size();
			Session sss = Sessions.getCurrent();
			List<Runnable> l = sessionCommands.get(sss);
			if (l == null) {
				l = new LinkedList<Runnable>();
				List<Runnable> prev = sessionCommands.putIfAbsent(sss, l);
				if (prev != null) {
					l = prev;
				}
			}
			synchronized (l) {
				int ls = l.size();
				l.add(cmnd);
				logger.debug("SessionCleanup command registered, sess = {}, sess_map_size_bef = {}, sess_map_size_aft = {}, cmd_list_size_bef = {}, cmd_list_size_aft = {}", sss, ms, sessionCommands.size(), ls, l.size());				
			}
			
		} else if ( DesktopCleanup.class.isAssignableFrom(scope)) {
			int ms = desktopCommands.size();
			Desktop dskt = Executions.getCurrent().getDesktop();
			//desktopCommands.put(Executions.getCurrent().getDesktop(), cmnd);
			//logger.debug("DesktopCleanup command registered, desktop = {}, desktop_map_size_after = {}", Executions.getCurrent().getDesktop(), desktopCommands.size());
			List<Runnable> l = desktopCommands.get(dskt);
			if (l == null) {
				l = new LinkedList<Runnable>();
				List<Runnable> prev = desktopCommands.putIfAbsent(dskt, l);
				if (prev != null) {
					l = prev;
				}
			}
			synchronized (l) {
				int ls = l.size();
				l.add(cmnd);
				logger.debug("DesktopCleanup command registered, desktop = {}, desktop_map_size_bef = {}, desktop_map_size_aft = {}, cmd_list_size_bef = {}, cmd_list_size_aft = {}", dskt, ms, desktopCommands.size(), ls, l.size());				
			}
		
		} else if ( WebAppCleanup.class.isAssignableFrom(scope)) {
			//appCommands.put(WebApps.getCurrent(), cmnd);
			synchronized (appCommands) {
				int sb = appCommands.size();
				appCommands.add(cmnd);
				logger.debug("WebAppCleanup command registered, app = {}, app_map_size_before = {}, app_map_size_after = {}", WebApps.getCurrent(), sb, appCommands.size());
			}
		
/*		} else if ( ExecutionCleanup.class.isAssignableFrom(scope)) {
			int ms = executionCommands.size();
			Execuition exc = Executions.getCurrent();
			//executionCommands.put(Executions.getCurrent(), cmnd);
			//logger.debug("ExecutionCleanup command registered, exec = {}, exec_map_size_after = {}", Executions.getCurrent(), executionCommands.size());
			List<Runnable> l = executionCommands.get(exc);
			if (l == null) {
				l = new LinkedList<Runnable>();
				executionCommands.put(exc, l);
			}
			synchronized (l) {
				int ls = l.size();
				l.add(cmnd);
				logger.debug("ExecutionsCleanup command registered, exec = {}, exec_map_size_bef = {}, exec_map_size_aft = {}, cmd_list_size_bef = {}, cmd_list_size_aft = {}", exc, ms, executionCommands.size(), ls, l.size());				
			}
*/			
		} else {
			logger.warn("registerCommand. unknown scope: {}", scope.getName());
		}
	} // public static void registerCommand(Runnable cmnd, Class<?> scope)
	
	public GlobalCleaner() {}
	
	@Override // Cleanups.Cleanup
	public void cleanup() { // ?? ЗАЧЕМ ??
    	logger.debug("GlobalCleaner. cleanup()"); // не удалось добиться неявного вызова
	}
	
	@Override // WebAppCleanup
	public void cleanup(WebApp wapp) throws Exception { // не удалось добиться неявного вызова
// HOWTO: (Remove and collect elements with Java streams) http://stackoverflow.com/questions/30042222/remove-and-collect-elements-with-java-streams
//		appCommands.entrySet().stream().filter((Map.Entry<Object,Runnable> e)->{return e.getKey().equals(wapp);}).forEach(e -> e.getValue().run());
//		appCommands.entrySet().removeIf((Map.Entry<Object,Runnable> e)->{return e.getKey().equals(wapp);});
//		logger.debug("GlobalCleaner. WebAppCleanup(). wapp = {}, app_map_size_after: {}", wapp, appCommands.size());
		synchronized (appCommands) {
			int sb = appCommands.size();
			appCommands.forEach(e -> e.run());
			appCommands.clear();
			logger.debug("GlobalCleaner. WebAppCleanup(). wapp = {}, app_list_size_before: {}, app_list_size_after: {}", wapp, sb, appCommands.size());
		}
	} // public void cleanup(WebApp wapp) throws Exception
	
	@Override // SessionCleanup
	public void cleanup(Session sess) throws Exception { // вызывается только по таймауту (после ExecutionCleanup и DesktopCleanup), причём дважды из одного потока
		//sessionCommands.entrySet().stream().filter((Map.Entry<Object,Runnable> e)->{return e.getKey().equals(sess);}).forEach(e -> e.getValue().run());
		//sessionCommands.entrySet().removeIf((Map.Entry<Object,Runnable> e)->{return e.getKey().equals(sess);});
		//logger.debug("GlobalCleaner. SessionCleanup(). sess = {}, sess_map_size_after = {}", sess, sessionCommands.size());
		int sb = sessionCommands.size();
		List<Runnable> l = sessionCommands.remove(sess);
		int ls = 0;
		if (l != null) synchronized(l) {
			ls = l.size();
			l.forEach(e -> e.run());
			l.clear();
		}
		logger.debug("GlobalCleaner. SessionCleanup(). sess = {}, sess_map_size_bef = {}, sess_map_size_aft = {}, cmd_list_size_bef = {}, cmd_list_size_bef = {}", sess, sb, sessionCommands.size(), (l == null ? "<not_exists>" : ls), (l == null ? "<not_exists>" : l.size()) );
	} // public void cleanup(Session sess) throws Exception
	
	/**  */
	@Override // DesktopCleanup
	public void cleanup(Desktop desktop) throws Exception { // вызывается при уходе со страницы, таймауте, закрытии вкладки(! в т.ч. timeout.zul) на пару, но после ExecutionCleanup; иногда дважды подряд в одном потоке
		//desktopCommands.entrySet().stream().filter((Map.Entry<Object,Runnable> e)->{return e.getKey().equals(desktop);}).forEach(e -> e.getValue().run());
		//desktopCommands.entrySet().removeIf((Map.Entry<Object,Runnable> e)->{return e.getKey().equals(desktop);});
		//logger.debug("GlobalCleaner. DesktopCleanup(). desktop = {}, desktop_map_size_after = {}", desktop, desktopCommands.size());
		int sb = desktopCommands.size();
		List<Runnable> l = desktopCommands.remove(desktop);
		int ls = 0;
		if (l != null) synchronized(l) {
			ls = l.size();
			l.forEach(e -> e.run());
			l.clear();
		}
		logger.debug("GlobalCleaner. DesktopCleanup(). desktop = {}, desktop_map_size_bef = {}, desktop_map_size_aft = {}, cmd_list_size_bef = {}, cmd_list_size_bef = {}", desktop, sb, desktopCommands.size(), (l == null ? "<not_exists>" : ls), (l == null ? "<not_exists>" : l.size()) );
	} // public void cleanup(Desktop desktop) throws Exception
	
	/*@Override // ExecutionCleanup
	public void cleanup(Execution exec, Execution parent, List<Throwable> errs) throws Exception { // вызывается при уходе со страницы, таймауте, закрытии(? в т.ч. timeout.zul) на пару, но перед DesktopCleanup, НО ТАКЖЕ ЕЩЁ РЕГУЛЯРНО (?); иногда дважды подряд в одном потоке
		//executionCommands.entrySet().stream().filter((Map.Entry<Object,Runnable> e)->{return e.getKey().equals(exec);}).forEach(e -> e.getValue().run());
		//executionCommands.entrySet().removeIf((Map.Entry<Object,Runnable> e)->{return e.getKey().equals(exec);});
		//logger.debug("GlobalCleaner. ExecutionCleanup(). exec = {}, exec_map_size_after = {}", exec, executionCommands.size());
		int sb = executionCommands.size();
		List<Runnable> l = executionCommands.remove(exec);
		int ls = 0;
		if (l != null) synchronized(l) {
			ls = l.size();
			l.forEach(e -> e.run());
			l.clear();
		}
		logger.debug("GlobalCleaner. ExecutionCleanup(). exec = {}, exec_map_size_bef = {}, exec_map_size_aft = {}, cmd_list_size_bef = {}, cmd_list_size_bef = {}", exec, sb, executionCommands.size(), (l == null ? "<not_exists>" : ls), (l == null ? "<not_exists>" : l.size()) );
	}*/ // public void cleanup(Execution exec, Execution parent, List<Throwable> errs) throws Exception
	
	/*@Override // EventThreadCleanup
	public void cleanup(Component comp, Event evt, List<Throwable> errs) throws Exception { // Notice that it is useless unless the event processing threads are enabled (it is disabled by default).
		logger.debug("GlobalCleaner. EventThreadCleanup.cleanup(). comp = {}, evt = {}", comp, evt);
	}

// If a listener implements this interface, an instance is created, and then the cleanup method is called in the event processing thread after the thread processes the event. Then, the complete method is called in the main thread (aka., the servlet thread), after the main thread is resumed. Note: The complete method won't be called if the corresponding cleanup method threw an exception. A typical use of this feature is to clean up unclosed transaction. Once registered, an instance is instantiated and the cleanup method is called after leaving the event processing thread.	

	@Override // EventThreadCleanup
	public void complete(Component comp, Event evt) throws Exception {
		logger.debug("GlobalCleaner. EventThreadCleanup.complete(). comp = {}, evt = {}", comp, evt);
	}*/
	
} // public class GlobalCleaner implements Cleanups.Cleanup, EventThreadCleanup, DesktopCleanup, SessionCleanup, WebAppCleanup, ExecutionCleanup