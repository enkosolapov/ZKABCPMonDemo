package basos.xe.abcpmon.zkui.compose;

import basos.data.GridData;
import basos.data.dao.GridDataProviderWPk;
import basos.data.zkmodel.GridDataModelMan;
import basos.data.zkmodel.ListModelListExt;
import basos.xe.data.dao.impl.DealDopService;
import basos.xe.data.entity.DealLastState;
import basos.xe.data.entity.DealRestHistory;
import basos.xe.data.entity.TrancheLastState;
import basos.zkui.UIUtil;
import basos.zkui.compose.SimpleGridDataComposer;

import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.zkoss.lang.Generics;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Path;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventQueue;
import org.zkoss.zk.ui.event.EventQueues;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.event.KeyEvent;
import org.zkoss.zk.ui.event.SelectEvent;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.event.SortEvent;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Div;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listcell;
import org.zkoss.zul.Listheader;
import org.zkoss.zul.Listitem;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Row;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.impl.HeaderElement;
import org.zkoss.zul.impl.MeshElement;


/** ���������� �������� dealsPage.
 *
 */
@VariableResolver(org.zkoss.zkplus.cdi.DelegatingVariableResolver.class) // �� ������������� ������ ���������� ����� � �������� ��� ���������� Weld, � ��������� ��� ����������� ZK, �.�. � ���� ���� ������ ��������� ����
public class DealsPageComposer extends SimpleGridDataComposer<Component> {
	private static final long serialVersionUID = 5043973253280324346L;

	private static final Logger logger = LoggerFactory.getLogger(DealsPageComposer.class);
	
	//@WireVariable private Desktop desktop;
	
	//@Wire private Grid dealsGrid; // ��� ������ (��� �������; ��������� ���������)
	@Wire
	private Listbox dealsLB; // ��� ������ (��� �������; ��������� ���������)
	
	@WireVariable("dealsProvider") // !!! org.zkoss.zkplus.cdi.DelegatingVariableResolver ����� �������� ������ ����������� ����, ���������� �������������� ��� ������������; � \WebContent\WEB-INF\beans.xml ����������� �� ������������ ��� �������� ������������ � ����� ������, ������ �������� ������ ���� �������������� ����� � ����������� ������ ��� ��������� ����������
	private GridDataProviderWPk<DealLastState> dealsProvider; // ��������� ������ ��� ������ ����� dealsLB (� �.�. ���� � ������� ��)
	
	private GridDataModelMan<DealLastState> dealsListboxModelManager; // ������ (�� ������ ����������) ��� dealsLB
	
	@Wire
	private Label dealsFootLableSummary; // ������� ��� dealsLB, ���� ������� ���-�� ����� � �����
	
	// ������ toolbar ����������� tabbox (��� tabpanels)
	//@Wire private Toolbarbutton toExcelToolBB;
		
	// ��������� ��� ����������� ����������� ��������� ZKWorkerWithTimerAndPM (tabpanel.borderlayout.south.hlayout, �.�. ��� �����)
	@Wire
	private Div dealsPmHolder;
	

	@Wire
	private Listbox tranchesLB; // ��� ������; ��������� ��������� (������ ��� ������ ����� ������)
	
	@WireVariable("tranchesProvider") // !!! org.zkoss.zkplus.cdi.DelegatingVariableResolver ����� �������� ������ ����������� ����, ���������� �������������� ��� ������������; � \WebContent\WEB-INF\beans.xml ����������� �� ������������ ��� �������� ������������ � ����� ������, ������ �������� ������ ���� �������������� ����� � ����������� ������ ��� ��������� ����������
	private GridDataProviderWPk<TrancheLastState> tranchesProvider; // ��������� ������ ��� ������ ����� tranchesLB (� �.�. ���� � ������� ��)
	
	private GridDataModelMan<TrancheLastState> tranchesListboxModelManager; // ������ (�� ������ ����������) ��� tranchesLB
	
	@Wire
	private Label tranchesFootLableSummary; // ������� ��� tranchesLB, ���� ������� ���-�� ����� � �����
	
	// ��������� ��� ����������� ����������� ��������� ZKWorkerWithTimerAndPM (tabpanel.borderlayout.south.hlayout, �.�. ��� �����)
	@Wire
	private Div tranchesPmHolder;
	
	@Wire
	private Grid dealRestHistGrid; // ������� �������� �� ������ (������ ��� ������ ����� ������)
	ListModelListExt<DealRestHistory> dealRestHistGridLMLX = new ListModelListExt<>();
	
	
	private Tabpanel dealsTabpanel; // !! �� �������� (subjsPage) �������� !!

	protected EventQueue<Event> interDeskEQ; // ���������� (UI) ������� inter-desktop ����� - ������������� ! // Returns the desktop-level event queue with the specified name in the current desktop, or if no such event queue, create one
	
	@Override
    protected void initAfterCompose(Component comp) throws Exception { // implementation specific part of doAfterCompose (at the and of)
		
// �������� �� ��������
		if (dealsProvider == null) dealsProvider = new basos.xe.data.dao.impl.GridDataProviderBatisDealLastState(false);
//	    logger.debug( "DealsService().selectDealsByClnId(400): {}", String.valueOf(new DealsService().selectDealsByClnId(400)/*selectAllDeals().size()*/) );
//	    dealsLB = (Listbox) Path.getComponent("//dealsPage/dealsLB");
	    if (dealsLB != null && dealsProvider != null) {
	    	//dealsGridLML = new ListModelListExt<GridData<DealLastState>>(dealsProvider.getGridDataList(), true/*live*/); // new ArrayList<GridData<DealLastState>>(dealsProvider.getGridDataList())
// ����� ������ �� ������, ������ ������ ������; ������ ��� �� ������ ��� ��� ������ ���������
	    	dealsProvider.setLoadAllForRange(false); // �� ������� ��, � ������ ������ ����������� � ��
	    	//_use_stream = true; // �������� ��� GridDataProviderWPk.getRange()
	    	dealsListboxModelManager = new GridDataModelMan<>(DealLastState.class, dealsProvider, GridDataModelMan.ModelInitTypes.INIT_BLANK);
	    	dealsListboxModelManager.setModelFor(dealsLB);
	    	dealsLB.setAttribute("GridDataModelMan", dealsListboxModelManager); // RULE: ��� ��������� ����/�������� � ���������� ��� ����-������ (GridDataModelMan)
	    	dealsLB.setAttribute("SummaryLabel", dealsFootLableSummary); // RULE: ��� ��������� ����/�������� � ������, � ������� ������� ����� ��� ��������� ������
	    	dealsLB.setAttribute("PmHolder", dealsPmHolder); // RULE: ��� ��������� ����/�������� � ����������� ��� UI-��������� ZKWorkerWithTimerAndPM
// TODO:HOWTO: ? ������ ������ ����� �� ��������������� ? ������ �������� � ����������� !
	    	((Listheader)dealsLB.getListhead().getFirstChild()).sort(true, true);
	    }
	    logger.debug("\nGridDataProviderBatisDealLastState."
		    	+ "\n getTotalRowCount() = " + dealsProvider.getTotalRowCount()
				+ "\n, dealsProvider: " + dealsProvider.getClass().getName()
				+ "\n, beanClass: " + dealsProvider.getBeanClass().getName()
				+ "\n, PK: " + dealsProvider.getPk().orElse("<PK_not_defined>")
				+ "\n, PkComparator: " + (dealsProvider.getBeanPkComparator().isPresent() ? dealsProvider.getBeanPkComparator().get().getClass().getName() : "<PK_not_defined>")
				+ "\n, PkClass: " + (dealsProvider.getPkClass().isPresent() ? dealsProvider.getPkClass().get().getName() : "<PK_not_defined>")
				+ "\n, dealsLB: " + dealsLB
		    );
		dealsFootLableSummary.setValue("����� ������: " + dealsListboxModelManager.getCurRowCount());
// FIXME: ��������� ���� ������� �� ������� !!
		
		if (dealRestHistGrid != null) {
			dealRestHistGrid.setModel(dealRestHistGridLMLX);
			//((Column)dealRestHistGrid.getColumns().getFirstChild()).sort(true, true); // TODO:HOWTO: ? ������ ������ ����� �� ��������������� ? ������ �������� � ����������� !
		}
		
// �������� � �������� (������)
		//tranchesLB.getPagingChild().setMold("os");
		if (tranchesProvider == null) tranchesProvider = new basos.xe.data.dao.impl.GridDataProviderBatisTrancheLastState(false);
	    if (tranchesLB != null && tranchesProvider != null) {
// ������ ���������� ��� ������ ������ (�����); ���������� �������� ������ ����-������, ������ ������ � ���������� �� �������������
	    	tranchesProvider.setLoadAllForRange(false); // �� ������� ��, � ������ ������ ����������� � ��
	    	tranchesListboxModelManager = new GridDataModelMan<>(TrancheLastState.class, tranchesProvider, GridDataModelMan.ModelInitTypes.INIT_BLANK);
	    	tranchesListboxModelManager.setModelFor(tranchesLB);
	    	tranchesLB.setAttribute("GridDataModelMan", tranchesListboxModelManager); // RULE: ��� ��������� ����/�������� � ���������� ��� ����-������ (GridDataModelMan)
	    	tranchesLB.setAttribute("SummaryLabel", tranchesFootLableSummary); // RULE: ��� ��������� ����/�������� � ������, � ������� ������� ����� ��� ��������� ������
	    	tranchesLB.setAttribute("PmHolder", tranchesPmHolder); // RULE: ��� ��������� ����/�������� � ����������� ��� UI-��������� ZKWorkerWithTimerAndPM
// TODO:HOWTO: ? ������ ������ ����� �� ��������������� ? ������ �������� � ����������� !
	    	((Listheader)tranchesLB.getListhead().getFirstChild()).sort(true, true);
	    }
	    logger.debug("\nGridDataProviderBatisTrancheLastState."
// FIXME: getTotalRowCount() ������ ����������� � getAll(), ������� � �� ���� ��������; ������������ SQL-������ � COUNT(*) 
		    	+ "\n getTotalRowCount() = " + tranchesProvider.getTotalRowCount()
		    	+ "\n getCurRowCount() = " + tranchesListboxModelManager.getCurRowCount()
				+ "\n, tranchesProvider: " + tranchesProvider.getClass().getName()
				+ "\n, beanClass: " + tranchesProvider.getBeanClass().getName()
				+ "\n, PK: " + tranchesProvider.getPk().orElse("<PK_not_defined>")
				+ "\n, PkComparator: " + (tranchesProvider.getBeanPkComparator().isPresent() ? tranchesProvider.getBeanPkComparator().get().getClass().getName() : "<PK_not_defined>")
				+ "\n, PkClass: " + (tranchesProvider.getPkClass().isPresent() ? tranchesProvider.getPkClass().get().getName() : "<PK_not_defined>")
				+ "\n, tranchesLB: " + tranchesLB
		    );
		tranchesFootLableSummary.setValue("����� �������: " + tranchesListboxModelManager.getCurRowCount());
		
		interDeskEQ = EventQueues.lookup("interDeskEQ"); // ���������� (UI) ������� inter-desktop ����� - ������������� ! // Returns the desktop-level event queue with the specified name in the current desktop, or if no such event queue, create one
		
		// ����� ������������� �� ������� �������� � ������� �� �������� ����� � ������ ��������
		interDeskEQ.subscribe(new SerializableEventListener<Event>() {
			private static final long serialVersionUID = -6678455287332246066L;
			@Override
			public void onEvent(Event ev) throws Exception {
				if ( "onGoToDeals".equals(ev.getName()) ) {
					Object data = ev.getData();
					if ( data != null && data.getClass().isArray() && ((Object[])data).length == 2 ) {
						asSubjectDetails((String)((Object[])data)[0], (Integer)((Object[])data)[1]);
					}
				} // onGoToDeals
			} // onEvent
		}); // interDeskEQ.subscribe(new SerializableEventListener...)
		
// dealsTabpanel �������� ������� fulfill="dealsTab.onSelect", �.�. �������� (������� this) ��� ������ ���������
// ��� ����������� ������������� (dealsTab.setSelected + sendEvent(Events.ON_SELECT, dealsTab...)) ���������� �����
// ����� ������� ����� ���, ��� ���������� �������� � ������������ �������� (ON_FULFILL ������� ����)
		Component tmp = Path.getComponent("//subjsPage/dealsTabpanel");
		if (tmp instanceof Tabpanel) {
			dealsTabpanel = (Tabpanel)tmp;
			Events.sendEvent("onAfterCreateDealsPage", dealsTabpanel, this);
		}
		
    } // protected void initAfterCompose(Component comp) throws Exception
	
	
	@Listen("onClick=#dealsAllButton")
	public void showAllDeals() {
		if (dealsListboxModelManager.getModelState() != GridDataModelMan.ModelStates.ENTIRE_MUTABLE_STATE) {
			dealsListboxModelManager.reinitModelByEntireList(GridDataModelMan.ModelInitTypes.INIT_ENTIRE_MUTABLE);
			dealsFootLableSummary.setValue("����� ������: " + dealsListboxModelManager.getCurRowCount());
			clearDetails();
			logger.trace("showAllDeals: dealsListboxModelManager.reinitModelByEntireList(GridDataModelMan.ModelInitTypes.INIT_ENTIRE_MUTABLE)");
		} else {
			logger.trace("showAllDeals: no model state changes, no reinit.");
		}
	}
	
	private void asSubjectDetails(String indName, Integer indValue) {
		if (dealsTabpanel != null && !dealsTabpanel.isSelected()) dealsTabpanel.getLinkedTab().setSelected(true);
		dealsLB.setActivePage(0);
		dealsLB.focus();
		dealsProvider.setPk(indName/*, DealLastState.getPkComparatorByName(indName)*/);
		dealsListboxModelManager.reinitModelByRange(indValue);
		dealsFootLableSummary.setValue("������ � "+(indName == "clnId" ? "��������" : "������ ��")+" �� LM "+indValue+" : " + dealsListboxModelManager.getCurRowCount());
//FIXME: ��� ����� ���������� ��� �� ������� (������) - ������ ������ ???
		clearDetails();
		logger.trace("asSubjectDetails.  indName = '{}', indValue = '{}'", indName, indValue);
	} // private void asSubjectDetails(String indName, Integer indValue)
	
	private void clearDetails() {
		tranchesListboxModelManager.clearModel();
		tranchesFootLableSummary.setValue("");
		dealRestHistGridLMLX.safeClear();
	}
	
	/** �� ����� �� ������ (������ ���������) �������� ������ � ��������� �������� (������). */
	@Listen("onSelect=#dealsLB")
	public void onSelectDeal( SelectEvent<Listitem, GridData<?>> sev ) {
		Listitem ref = sev.getReference(); // ��������������� ��������� ������
// !!! ����: ��� PgDn �.�. (�� ������ !) ������� �����������, �� Listitem.value � ��� �������� ������; ��� ���� �������� ��� �� ������ �� ����� �� ���������� !!!
		Object tmp = null;
		GridData<?> gd = null;
		tmp = ref.getValue();
		if (tmp != null) {
			gd = (GridData<?>)tmp;
		}
		int sidAttr = 0;
		long uidAttr = 0L;
		tmp = ref.getAttribute("sid"); // ((DealLastState)gd.getBean()).getIdDeal()
		if (tmp != null) {
			sidAttr = (int)tmp;
		}
    	tmp = ref.getAttribute("uid"); // gd.getUid()
		if (tmp != null) {
			uidAttr = (long)tmp;
		}
    	//int rnAttr = (int)ref.getAttribute("rn")/*((ForEachStatus)r.getParent().getAttribute("forEachStatus")).getIndex()*/;
// 94:50, 11652:41, 11655:2, 11663:138, 21804:0
// TODO: �������������� ��� ��� ��������� (sql, stream, binary search) ����� � ������ � ����� �� ���� ������
		//logger.trace("t1 = {}", System.currentTimeMillis());
		/*alert("onSelectDeal ["+sev.getTarget().getId()+"]. Reference_sid/uid: "+sidAttr+"/"+uidAttr
			+", GridData: "+gd
			+", Label: '" + ((Listcell)ref.getFirstChild()).getLabel()+"'"
			//+", SelectedItems : "+sev.getSelectedItems()
			//+", SelectedObjects: "+sev.getSelectedObjects().stream().map( so->{return String.valueOf(so.getUid());} ).collect(Collectors.joining(", ", "{ ", " }"))
			+", tr_cnt: "+l.size()
		);*/
		logger.trace("onSelectDeal (before reinitModel) ["+sev.getTarget().getId()+"]. Reference_sid/uid: "+sidAttr+"/"+uidAttr+", GridData: "+gd);
		if (gd == null || sidAttr == 0 || uidAttr == 0L) {
			return;
		}
		tranchesLB.setActivePage(0);
		tranchesListboxModelManager.reinitModelByRange(sidAttr);
// TODO: �� �������� LM ID () ������ () ������� �� () USD � ������� � () �� ()
		tranchesFootLableSummary.setValue("������� � ������ �� LM "+sidAttr+" : " + tranchesListboxModelManager.getCurRowCount());
		//logger.trace("t2 = {}", System.currentTimeMillis());
		dealRestHistGridLMLX.safeReplaceInnerList(DealDopService.getRestHistoryByIdDeal(sidAttr));
	} // public void onSelectDeal(SelectEvent<Listitem,GridData<?>> sev)
	
	
	/** ��� ��������� ��������� �������� � ������ ������� ����� ("����" ������ ������) ��������� ���� GridData.sel.
	 * �������� {@link GridDataModelMan#selectRow(boolean, int) GridDataModelMan.selectRow()} � ����������� ����-������ ����� ��������� {@link SimpleGridDataComposer#dispatchGridModelLockingTask(GridDataModelMan, Consumer, String, Runnable, Runnable) dispatchGridModelLockingTask()}.
	 */
// FIXME: !! �� ��������� ������ ������ ���� �� �������� !!
// FIXME: ? Pull Up ?
	@Listen("onCheckRow=#dealsLB;onCheckRow=#tranchesLB")
    public void onCheckRow(ForwardEvent fev) { // �������� ������� - ������� � ������ ����� ��� � Listitem > Listcell ���������, ��� listitem value="${each}" ��� row value="${each}" � ����-������ of GridData
    	Checkbox cb = (Checkbox)fev.getOrigin().getTarget();
    	GridDataModelMan<?> gdm = (GridDataModelMan<?>)fev.getTarget().getAttribute("GridDataModelMan");
    	boolean isChecked = cb.isChecked(); // ����� �������� ����� (true/false), ������� ����� �������� � ������.
    	GridData<?> gd = null; // ������ ����-������, � ������� ����� ���������������� �������� �����
    	int sidAttr = -1, rnAttr = -1;
    	if (cb.getParent() instanceof Row) {
    		Row row = (Row)cb.getParent();
    		gd = (GridData<?>)row.getValue()/*getParent().getAttribute("each")*/; // HOWTO: (ZUML Ref. p. 78): The 'each' object is actually stored in the parent component's attribute, so you could retrieve it in pure Java as follows: comp.getParent().getAttribute("each") / getAttribute("forEachStatus"). (DR p. 51) However, you cannot access the values of each and forEachStatus in an event listener because their values are reset after the XML element which forEach is associated has been evaluated. There is a simple solution: store the value in the component's attribute, so you can retrieve it when the event listener is called.
        	sidAttr = (int)row.getAttribute("sid"); // ((DealLastState)gd.getBean()).getIdDeal()
        	rnAttr = (int)row.getAttribute("rn")/*((ForEachStatus)r.getParent().getAttribute("forEachStatus")).getIndex()*/;
    	} else if (cb.getParent() instanceof Listcell) {
        	Listcell cell = (Listcell)cb.getParent();
        	Listitem item = (Listitem)cell.getParent();
        	gd = (GridData<?>)item.getValue()/*getParent().getAttribute("each")*/; // HOWTO: (ZUML Ref. p. 78): The 'each' object is actually stored in the parent component's attribute, so you could retrieve it in pure Java as follows: comp.getParent().getAttribute("each") / getAttribute("forEachStatus"). (DR p. 51) However, you cannot access the values of each and forEachStatus in an event listener because their values are reset after the XML element which forEach is associated has been evaluated. There is a simple solution: store the value in the component's attribute, so you can retrieve it when the event listener is called.
        	sidAttr = (int)item.getAttribute("sid"); // ((DealLastState)gd.getBean()).getIdDeal()
        	rnAttr = (int)item.getAttribute("rn")/*((ForEachStatus)r.getParent().getAttribute("forEachStatus")).getIndex()*/;
    	}
    	if (gd == null) return;
    	long uid = gd.getUid(); // (long)r.getAttribute("uid") // GridData.uid - ���������� ����� ���������� �������, �� ������ �� ���������. �� ������ ���� ������ � ������.
    	GridData<?> gdPar = gd;
		//int irn = dealsListboxModelManager.selectRow(isChecked, uid); // ����� ������ � �����, ������� �� �������, �� ������������� ������ (�������� � ����������� �� ������� view, ������ ���� � ������ �� uid)
    	//logger.debug("onCheckRow(after). isChecked = {}, uid = {}, irn = {}", isChecked, uid, irn);
    	
    	logger.trace("det onCheckRow. target_id='{}', origin: '{}', row_value(GridData) ='{}', uid ='{}', isChecked ='{}', GridDataModelMan ='{}', beanClass = '{}', Pk = '{}', sidAttr ='{}', rnAttr ='{}'", fev.getTarget().getId(), fev.getOrigin(), gd, uid, isChecked, gdm, gdm.getBeanClass().getSimpleName(), ((GridDataProviderWPk<?>)gdm.getDataProvider()).getPk(), sidAttr, rnAttr );
    	String msg = "onCheckRow. uid="+uid+", new_state:"+isChecked+", rn="+rnAttr+", uid="+uid;
    	Consumer<Long> taskToRun = (stamp) -> {
    		try {
    			int irn = gdm.selectRow(isChecked, Generics.cast(gdPar)/*uid*/); // ����� ������ � �����, ������� �� �������, �� ������������� ������ (�������� � ����������� �� ������� view, ������ ���� � ������ �� uid)
    			logger.trace(concurMarker, "{} (UI)...taskToRun. After change data... irn = {}, stamp = {}", msg, irn, stamp);
    		} finally {
    			if (stamp != 0L) {
    				gdm.getModelRWLock().unlock(stamp);
    				logger.trace(concurMarker, "{} (UI)...taskToRun. WriteLock released, stamp = {}", msg, stamp);
    			}
    		}
    	};
    	dispatchGridModelLockingTask(gdm, taskToRun, msg, null, null);
    	logger.debug("onCheckRow(after). isChecked = {}, uid = {}, rn = {}, sid = {}", isChecked, uid, rnAttr, sidAttr);
    } // public void onCheckRow(ForwardEvent fev)
    
    
    @Listen("onSort=listbox#dealsLB > listhead > listheader;onSort=listbox#tranchesLB > listhead > listheader")
// FIXME: ? Pull Up ?
    public void onSortCol(SortEvent sev) {
    	sev.stopPropagation();
    	boolean isAsc = sev.isAscending();
    	HeaderElement hdr = ((HeaderElement)sev.getTarget());
    	Component host = hdr.getParent().getParent();
    	GridDataModelMan<?> lmlx = (GridDataModelMan<?>)host.getAttribute("GridDataModelMan");
    	safeSort(hdr, isAsc, lmlx);
    } // public void onSortCol(SortEvent sev)
    
    
    /** local keystroke handler */
	@Listen("onCtrlKey=grid;onCancel=grid;onCtrlKey=listbox;onCancel=listbox") // {@2}Alt+2: 50, {@#f5}Alt+F5 : 116
// FIXME: ? split & Pull Up ?
	public void keyListener(KeyEvent kev) {
		//Grid grid = (Grid)kev.getTarget();
		//Listbox lb = (Listbox)kev.getTarget();
		MeshElement mesh = (MeshElement)kev.getTarget();
    	int keyCode = kev.getKeyCode();
    	logger.debug("keyListener. target: {}, id: {}, keyCode: {}", mesh, mesh.getId(), keyCode);
    	//alert("keyCode = "+keyCode);
    	if (keyCode == 50) { // ({@2}Alt+2: 50) ��������� ������ ������� (����������� ������������); !! ������ � ������� ����� properties �������� ������� (���� deals_grid.col.dd_rest.label) � ������ (deals_grid.col.dd_rest.width) !!
    		UIUtil.writeMeshHeaderInfo(mesh);
    	} // Alt+2 -> ��������� �������� � ������� ������ ������� � ���� deals.properties
// FIXME: ������������������ ��� ������ ���������� ������ (lmlx, holder)
    	else if (keyCode == 116) { // ({@#f5}Alt+F5 : 116) ��������� � Excel ���������� ����-������ �����
    		GridDataModelMan<?> gdm = (GridDataModelMan<?>)mesh.getAttribute("GridDataModelMan");
    		Component pmh = (Component)mesh.getAttribute("PmHolder");
    		Messagebox.show( "Download "+mesh.getId()+" to Excel ?"
    						,"toExcel"
    						,Messagebox.OK|Messagebox.CANCEL
    						,Messagebox.QUESTION
    						,new SerializableEventListener<Event>() {
    							private static final long serialVersionUID = 2827681413520028065L;
								@Override
								public void onEvent(Event e) throws Exception {
									if (Messagebox.ON_OK.equals(e.getName())) {
										//alert("Ok, go to Excel!");
										downloadToExcel(gdm, UIUtil.meshHeaderToList(mesh), pmh, null/*toExcelToolBB*/);
									}
								} // public void onEvent
    						} // SerializableEventListener
    		); // Messagebox.show
    	} // ({@#f5}Alt+F5 : 116) ��������� � Excel ���������� ����-������ �����
    	else if ( keyCode == 27 && gridModelToExcelWorker != null ) { // Esc: ���������� �������� � Excel
			Messagebox.show( "Terminate downloading to XLSX ?" // DR p. 612
					,"Confirmation"
					,Messagebox.OK|Messagebox.CANCEL
					,Messagebox.QUESTION
					,new SerializableEventListener<Event>() {
						private static final long serialVersionUID = 6302510109033213299L;
						public void onEvent(Event e) {
							if ( Messagebox.ON_OK.equals(e.getName()) && gridModelToExcelWorker != null ) {
								logger.debug("keyListener. Termination confirmed. sizeSharedLogic: {}", (gridModelToExcelWorker==null ? null : gridModelToExcelWorker.sizeSharedLogic()) );
								gridModelToExcelWorker.cancel(false/*true*/);
							}
						}
					}
			); // Messagebox.show
    	} // keyCode == 27 && gridModelToExcelWorker != null
	} // public void keyListener(KeyEvent kev)
	
} // public class DealsPageComposer extends SimpleGridDataComposer<Component>