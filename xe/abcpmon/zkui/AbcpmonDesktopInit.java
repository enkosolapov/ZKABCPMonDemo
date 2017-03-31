package basos.xe.abcpmon.zkui;

//import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Desktop;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventQueue;
import org.zkoss.zk.ui.event.EventQueues;
import org.zkoss.zk.ui.util.DesktopCleanup;
//import org.zkoss.zk.ui.event.Events;
//import org.zkoss.zk.ui.event.OpenEvent;
import org.zkoss.zk.ui.util.DesktopInit;

import basos.zkui.util.GlobalCleaner;


/** ��������� ���� ������ ������ �������� ��� ��� ������������� (desktop scope initialization logic); ������ �������� �������� � zk.xml
 * ��� ������� �������� ������ ������ �������������, ������� ����� �������� �� �������� "deskMutex".
 * � ������������ ������� �����������, ������� ������� ��� ������� EventQueue � ������� "syncEQ" � "interDeskEQ".
 */
public class AbcpmonDesktopInit implements DesktopInit {

	private static final Logger logger = LoggerFactory.getLogger(AbcpmonDesktopInit.class);
	
	private Object deskMutex = new Object(); // ��� ������������� ������ �������� ��������
	
    private Runnable cleanupCmndEQs = () -> {
    	EventQueue<Event> syncEQ = EventQueues.lookup("syncEQ", false);
    	EventQueue<Event> interDeskEQ = EventQueues.lookup("interDeskEQ", false);
   		logger.debug("cleanupCmndEQs.run(), syncEQ = {}, interDeskEQ = {}", syncEQ, interDeskEQ);
   		if (syncEQ != null && !syncEQ.isClose()) {
   			syncEQ.close();
   		}
   		if (interDeskEQ != null && !interDeskEQ.isClose()) {
   			interDeskEQ.close();
   		}
    }; // cleanupCmndEQs
	
	@Override
	public void init(Desktop desktop, Object request) throws Exception {
		logger.debug("AbcpmonDesktopInit.init");
		desktop.setAttribute("deskMutex", deskMutex);
		GlobalCleaner.registerCommand(cleanupCmndEQs, DesktopCleanup.class); // ���������� ��� ����� �� ��������, ��������, �������� �������(! � �.�. timeout.zul) �� ����, �� ����� ExecutionCleanup; ������ ������ ������ � ����� ������
//		desktop.addListener(new MyAuService()); // ������������ ����������� ������� �������� ������ (����� MyAuService ���������� ����� service() ����������� ����������, ��. DesktopImpl)
	} // public void init(Desktop desktop, Object request) throws Exception
	
	/** �������� ��� ��������������� ��������� ������� Events.ON_OPEN. */
/*	public class MyAuService implements org.zkoss.zk.au.AuService {
		@Override
		public boolean service(org.zkoss.zk.au.AuRequest request, boolean everError) {
			final String cmd = request.getCommand();
			final Component comp = request.getComponent();
			if (comp != null) {
				for (Method m : comp.getClass().getMethods()) {
					logger.debug("{} : {}", m.getDeclaringClass().getName(), m.getName());
				}
			}
			if (cmd.equals(Events.ON_OPEN)) {
				OpenEvent evt = OpenEvent.getOpenEvent(request);
				if (!evt.isOpen()) {
                    logger.debug("Closing... ");
// popup ����������� �� ������� �������, �� ��������������� (������ �������������)
//					if ("rest_msfo_usdCBT".equals(comp.getId())) throw new Error("TERMINATOR was here...");
//					return true; // block request
				}
			}
			if (comp != null) {
				logger.debug("comp_name = {}, comp_id = {}, cmd = {}", comp.getClass().getName(), comp.getId(), cmd);
			}
			return false; // true means block
		} // public boolean service(org.zkoss.zk.au.AuRequest request, boolean everError)		
	} // public class MyAuService implements org.zkoss.zk.au.AuService
*/
	
} // public class AbcpmonDesktopInit implements DesktopInit