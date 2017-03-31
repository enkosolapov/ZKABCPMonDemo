package basos.zkui.compose;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.zkoss.lang.Generics;
import org.zkoss.zk.ui.AbstractComponent;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.WebApps;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.OpenEvent;
import org.zkoss.zk.ui.http.WebManager;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zul.Auxhead;
import org.zkoss.zul.Auxheader;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.impl.MeshElement;

import basos.data.dao.GridDataProviderWPk;
import basos.data.zkmodel.GridDataFilterableModelMan;
import basos.data.zkmodel.GridDataModelMan;
import basos.xe.abcpmon.zkui.AbcpmonWebAppInit;
import basos.zkui.AbstractComponentBehaveUtil;
import basos.zkui.GridDataFilter;
import basos.zkui.UIUtil;


/** ���������� ���������� SelectorComposer ��� ���������� �������/����������� � ����������� �������� ({@link GridDataFilter}).
 *
 */
public class FilterableGridDataComposer<T extends Component> extends SimpleGridDataComposer<Component> {
	private static final long serialVersionUID = 1902356627982965558L;
	
	private static final Logger logger = LoggerFactory.getLogger(FilterableGridDataComposer.class);
	
	
    /** ���� ����/�������� � ������� ������-��������� � ��������� � ����-���������, ������ ������, ������, �������� � �.�.
     * @param 
     */
    protected <U extends Object & Serializable & Comparable<? super U>>
    	GridDataFilterableModelMan<U> initFilterableMesh
       		   (GridDataProviderWPk<U> prov
    		   ,MeshElement mesh
    		   ,Paging pgctrl
    		   ,Label footer
    		   ,Component pmh) {
    	
// FIXME: �������� ����������
    	GridDataFilterableModelMan<U> mmm = null;
    	Class<U> beanClass = prov.getBeanClass();
    	GridDataFilter fltf = null;
    	
// �������� ������� ���������������� ���������, ����� ����� ��������������� ������ ��� ������� �������, ������������� ������ �����; ������� ����� �������� �� �������, � ������ �� ������
     	Map<String, AbstractComponentBehaveUtil> behavMap = cloneDefBehavMap();
		
		AbstractComponent[] filterComps = extractFilterControlsFromAuxhead(mesh);

// FIXME: ��������������� ������ ����� ���� ???
		try {
			fltf = new GridDataFilter(prov, behavMap, filterComps   /*lmc_rowidTB, cluidTB, idlimitIB, is_riskCHB, s_usd_balFltr, rest_msfo_usdFltr, dd_probl_proj_beginFltr, selFilterCHB, subj_idIB, subj_nameTB, gr_nameTB, ko_fioTB, curator_fioTB, acd_prodTB, yak_nameCombo, br_nameCombo, cityCombo, kko_otdelCombo, kko_upravlCombo, cat_nameCombo, clibr_nameCombo, b2segmCombo, asv_name_okeqCombo*/ /*new ArrayList<AbstractComponent>(Arrays.asList(selFilterCHB, subj_idIB, subj_nameTB, gr_nameTB, ko_fioTB, curator_fioTB, acd_prodTB, yak_nameCombo, br_nameCombo, cityCombo, kko_otdelCombo, kko_upravlCombo, cat_nameCombo, clibr_nameCombo, b2segmCombo, asv_name_okeqCombo)).toArray(new AbstractComponent[0])*/);
		} catch (Throwable e) {
// WebAppCleanup() -> cleanup() -> ExecutionCleanup()
			WebManager.getWebManager( WebApps.getCurrent()/*Executions.getCurrent().getDesktop().getWebApp()*/ ).destroy();
			throw e;
		}
		
		mmm = new GridDataFilterableModelMan<U>(beanClass, prov, fltf);
		
		if (mesh instanceof Grid) {
			mmm.setModelFor((Grid)mesh); // ����-������ ����� �� ������, ��������������� ����������� (���������� �������� ����� ������� ������) � ����������� ��������
		} else if (mesh instanceof Listbox) {
			mmm.setModelFor((Listbox)mesh); // ����-������ ����� �� ������, ��������������� ����������� (���������� �������� ����� ������� ������) � ����������� ��������
		}
	    
	    mesh.setAttribute("GridDataModelMan", mmm); // RULE: ��� ��������� ����/�������� � ���������� ��� ����-������ (GridDataModelMan)
	    mesh.setAttribute("ExtPaging", pgctrl); // RULE: ��� ��������� ����/�������� � ������� ���������� ������������
	    mesh.setAttribute("SummaryLabel", footer); // RULE: ��� ��������� ����/�������� � ������, � ������� ������� ����� ��� ��������� ������
	    mesh.setAttribute("PmHolder", pmh); // RULE: ��� ��������� ����/�������� � ����������� ��� UI-��������� ZKWorkerWithTimerAndPM 
	    
	    consolidateFilterEvents(filterComps, mesh);
	    
	    footer.setValue("����� �����: " + mmm.getCurRowCount()); // TODO: (low) ��������� �� ������� (onAfterRender)

    	return mmm;
    } // protected ... GridDataFilterableModelMan<U> initFilterableMesh(...)
    
	
 	/** ��������� ������� � ��������� ������-��������� �� ��������� (�� �������� 'defBehavMap' ����������, ������� 
 	 * �������� � {@link AbcpmonWebAppInit#init}) ��� ������������� � ���������� ����������� �������� (���������� �� {@link #initFilterableMesh}).
 	 */
	protected Map<String, AbstractComponentBehaveUtil> cloneDefBehavMap() {
    	Map<String, AbstractComponentBehaveUtil> behavMap = null;
    	Object tmp = WebApps.getCurrent().getAttribute("defBehavMap");
    	if ( tmp == null || !(tmp instanceof Map<?,?>) ) {
    		logger.error("cloneDefBehavMap. fail to resolve 'defBehavMap' WebApp scope attribute: {}", tmp);
    		throw new InternalError("cloneDefBehavMap. fail to resolve 'defBehavMap' WebApp scope attribute: "+tmp);
    	}
    	behavMap = new HashMap<>(Generics.cast(tmp));
		logger.trace("cloneDefBehavMap.  behavMap.size = {}", behavMap.size());
		return behavMap;
    } // protected Map<String, AbstractComponentBehaveUtil> cloneDefBehavMap()
    
    /**  */
    protected void applyFilter(GridDataFilterableModelMan<?> gdm, MeshElement host, Boolean force, Optional<Integer> rn, Long stamp) {
		if (gdm.applyFilter(force, rn, stamp) >= 0) {
			refreshAfterDataChange(host, gdm);
		}
    }
    
    /** ����� � ����������� ���������� �������: force=false, rn=Optional.empty(). */
// ������ �� �������� ������� �� �������� (���������� ����� ������������� ������) ��� ���������� ���������� � ������������
    protected boolean safeApplyFilter(Component comp, Toolbarbutton cftbb) {
    	return safeApplyFilter(comp, false, Optional.empty(), cftbb);
    }
    
    /**  */
    protected MeshElement getFilterCompMesh(Component comp) {
    	if ( comp == null ) return null;
    	Component tmp = comp.getParent();
    	if ( !(tmp instanceof Auxheader) ) return null;
    	tmp = tmp.getParent();
    	if ( !(tmp instanceof Auxhead) ) return null;
    	tmp = tmp.getParent();
    	if ( !(tmp instanceof MeshElement) ) return null;
    	return (MeshElement)tmp;
    }
    
    /**  */
    protected GridDataFilterableModelMan<?> getFilterableModelMan(MeshElement host) {
    	Object obj = host.getAttribute("GridDataModelMan");
    	if ( !(obj instanceof GridDataFilterableModelMan<?>) ) return null;
    	GridDataFilterableModelMan<?> gdm = (GridDataFilterableModelMan<?>)obj;
    	return gdm;
    }
    
    /**  */
    protected boolean safeApplyFilter(Component comp, Boolean force, Optional<Integer> rn, Toolbarbutton cftbb) {
    	MeshElement host = getFilterCompMesh(comp);
    	if (host == null) return false;
    	GridDataFilterableModelMan<?> gdm = getFilterableModelMan(host);
    	if (gdm == null) return false;
    	GridDataFilter fltr = gdm.getDataFilter();
    	if ( fltr.getComponentIndex((AbstractComponent)comp) < 0 ) { // ���������, ��� �������� ������� ������ � ������
    		return false;
    	}
    	safeApplyFilter(fltr, gdm, host, force, rn, cftbb);
		return true;
    } // protected boolean safeApplyFilter(Component comp, Boolean force, Optional<Integer> rn)

    protected void safeApplyFilter(MeshElement host, Toolbarbutton cftbb) {
    	GridDataFilterableModelMan<?> gdm = getFilterableModelMan(host);
    	if (gdm == null) return;
    	GridDataFilter fltr = gdm.getDataFilter();
    	safeApplyFilter(fltr, gdm, host, false, Optional.empty(), cftbb);
    }
    
    /**  */
// ���������� �� ������� clear (������� �� ������� � ����������� ��������� - ���������� �������; �������������)
    protected void safeApplyFilter(GridDataFilter fltr, GridDataFilterableModelMan<?> gdm, MeshElement host, Toolbarbutton cftbb) {
    	safeApplyFilter(fltr, gdm, host, false, Optional.empty(), cftbb);
    }
    
    /**  */
    protected void safeApplyFilter(GridDataFilter fltr, GridDataFilterableModelMan<?> gdm, MeshElement host, Boolean force, Optional<Integer> rn, Toolbarbutton cftbb) {
		Consumer<Long> taskToRun = (stamp) -> {
			/*if (gdm.applyFilter(force, rn, stamp) >= 0) {
				refreshAfterDataChange(host, gdm);
			}*/
			applyFilter(gdm, host, force, rn, stamp);
		};
		Runnable beforePostpone = () -> {
			fltr.disableComps(true, true); // �� ����� �������� ��������� ��� ������-��������
    		if (cftbb != null) cftbb.setDisabled(true);
		}, beforeDeferredRun = () -> {
			fltr.disableComps(false, true); // �������� ����������� - ������������ ��� ������-�������� (��������� ���������� ���� ��������� ���)
			if (cftbb != null) cftbb.setDisabled(false);
		};
		String msg = "safeApplyFilter::";
		dispatchGridModelLockingTask(gdm, taskToRun, msg, beforePostpone, beforeDeferredRun);
    }
	
	/** ����� ��������� ������ ����� �����/��������� �������� ���� � ������ ("SummaryLabel" - ���-�� ����� � �������)
	 * � ������������ ���������� (��. {@link UIUtil#provokeAutoSort(MeshElement)}).
	 * @param mesh ����/��������, ����� ����� ������ �������� ���������.
	 * @param gdm ��������������� �������� ������.
	 */
    protected void refreshAfterDataChange(MeshElement mesh, GridDataModelMan<?> gdm) {
    	//GridDataModelMan<?> gdm = (GridDataModelMan<?>)mesh.getAttribute("GridDataModelMan"); // ���������� �� applyFilter, � ��� ��� �������� �������� ������
    	Label ft = (Label)mesh.getAttribute("SummaryLabel");
		logger.trace("refreshAfterDataChange. ����� �����: {}", gdm.getCurRowCount());
// TODO: ����� ���������� ���������� (����� ��� ���������)
		if (ft != null) {
			ft.setValue("����� �����: " + gdm.getCurRowCount()); // TODO: (low) ��������� �� ������� (onAfterRender)
		}
    	UIUtil.provokeAutoSort(mesh);
    } // protected void refreshAfterDataChange(MeshElement mesh, GridDataModelMan<?> gdm)
    
    
	/** ��� ����� ������� ����������� ��������� (Auxhead), � ������� �� ������ �� Auxheader ������� ������-��������.
     * ��������� ���������� ���������� � �������, ������� ����� �������������� ����������� ������.
     */
    public static AbstractComponent[] extractFilterControlsFromAuxhead(MeshElement dataGrid) {
// FIXME: �������� ����������, array bounds
    	AbstractComponent[] fc = new AbstractComponent[32];
    	Auxhead ahd = null;
    	int idx = -1;
    	for(Component comp : dataGrid.getChildren()) {
    		if (! (comp instanceof Auxhead)) continue;
    		ahd = (Auxhead)comp;
    		break;
    	}
    	if (ahd == null) return null;
    	for(Component comp : ahd.getChildren()) {
    		if (! (comp instanceof Auxheader)) continue;
    		//Auxheader cur = (Auxheader)comp;
    		Component ch = comp.getFirstChild(); // �������, ��� � Auxheader �.�. �� ����� ������ ��������� �������� � ��� ������-�������
    		if (ch != null) {
    			fc[++idx] = (AbstractComponent)ch;
    		}
    	}
    	return fc;
    } // public static AbstractComponent[] extractFilterControlsFromAuxhead(MeshElement dataGrid)
    
    
    /** ��������������� ���� �������, ����������� �� ��������� ���������� �������� ����������, �� ���������
     * ������������ ������� � ���� ������� onApplyFilter.
     */
    public static void consolidateFilterEvents(AbstractComponent[] members, Component target) {
    	for (AbstractComponent comp : members) {
    		logger.trace("consolidateFilterEvents.  comp = '{}'", comp);
    		if (comp == null) continue; // ������ ������-��������� ������������� ����������� � �������� ������ ������ � ������
    		switch ( comp.getClass().getSimpleName() ) {
				case "BetweenFilterMacro":
					comp.addForward("onApply", target, "onApplyFilter");
					break; // between_filter_date, between_filter_decimal
				case "Checkbox":
					comp.addForward(Events.ON_CHECK, target, "onApplyFilter");
					break;
				case "Intbox":
					comp.addForward(Events.ON_CHANGE, target, "onApplyFilter");
					//comp.addForward(Events.ON_OK, target, "onApplyFilter");
					break;
				case "Textbox":
					comp.addForward(Events.ON_CHANGE, target, "onApplyFilter");
					//comp.addForward(Events.ON_OK, target, "onApplyFilter");
					break;
				case "Combobox":
					//comp.addForward(Events.ON_CHANGE, target, "onApplyFilter"); // !! ������ ��� Enter ��� ������ ������ (Tab), ������ ���������� ��� �������� ��������� ������ !!
// HOWTO: ? ��� ���������� �������, ������� ����� (see Concepts_and_Tricks.pdf !! see Deferrable) ?
					comp.addForward(Events.ON_OK, target, "onApplyFilter"); // �� ������� Enter ������� onCange �� �����������, �� ��� ������� ������� ����������� onOK - ����������� !
					comp.addForward(Events.ON_BLUR, target, "onApplyFilter");
					comp.addForward(Events.ON_OPEN, target, "onApplyFilter");
					break;
				default:
					logger.error("consolidateFilterEvents.  Composite filter members of type '{}' don't supported. Component: '{}', target: '{}'.", comp.getClass().getName(), comp, target);
					throw new InternalError("consolidateFilterEvents.  Composite filter members of type '"+comp.getClass().getName()+"' don't supported. Component: '"+comp+"', target: '{"+target+"}'.");
			} // switch
    	} // for
    } // public static void consolidateFilterEvents(AbstractComponent[] members)
    
    
    @Listen("onOK = auxheader > intbox")
    public void onOKIB(/*KeyEvent kev*/) {
// �� ������� Enter ������� onCange �� �����������, �� ��� ������� ������� ����������� onOK - ����������� !
//    	kev.stopPropagation();
//    	Events.sendEvent(new InputEvent(Events.ON_CHANGE, subj_nameTB, subj_nameTB.getValue(), null)); // Enter=Tab - ����� �� ���� � ���������� �������
    }
    
    @Listen("onOK = auxheader > textbox")
    public void onOKTB(/*KeyEvent kev*/) {
// �� ������� Enter ������� onCange �� �����������, �� ��� ������� ������� ����������� onOK - ����������� !
//    	kev.stopPropagation();
//    	Events.sendEvent(new InputEvent(Events.ON_CHANGE, subj_nameTB, subj_nameTB.getValue(), null)); // Enter=Tab - ����� �� ���� � ���������� �������
    }
    
    /** ������� ����������� �������� ������� � ������� ��� ������ ������. */
    @Listen("onBlur = auxheader > combobox") // ������ ������ (� �.�. ������������ �� ������ ���������� !!)
    public void onBlurCombo(Event ev) { // ��� onChanging iev.getValue ��� ��������� �������� ������ �����, ������� �� ��������� value ���������� � �� �������� �� ������
    	Combobox combo = (Combobox)ev.getTarget();
    	ListModelList<Object> comboModel = (ListModelList<Object>)combo.getModel();
    	String val = combo.getValue();
		logger.debug("onBlurCombo. comboID: '{}', value = '{}'; comboModel: {}", combo.getId(), val, comboModel);
		if ( !StringUtils.isEmpty(val) && (comboModel == null || !comboModel.contains(val)) ){ // �������� �� ������� �� ������, � ���������� �� ����� - ������� ����� ������
			combo.setValue(null); // ������ ������� - ��������� WrongValueException
			logger.debug("onBlurCombo. Invalid value was cleared.");
		}
    } // public void onBlurCombo(Event ev)
    
    
    /** ������������ ����-������ ���������� � ������� ��� ��������.
     * @see GridDataFilterableModelMan#setComboModel(Combobox)
     */
    @Listen("onOpen = auxheader > combobox")
    public void onOpenCombo(OpenEvent oev) {
		logger.debug("onOpenCombo. targetID: '{}', value = '{}', isOpen ?: {}; ev_class: {}", oev.getTarget().getId(), ((Combobox)oev.getTarget()).getValue(), oev.isOpen(), oev.getClass().getName());
    	if (oev.isOpen()) { // ��������, � �� ��������
    		MeshElement host = getFilterCompMesh(oev.getTarget());
    		if (host == null) return;
    		GridDataFilterableModelMan<?> mmm = getFilterableModelMan(host);
    		if (mmm == null) return;
    		mmm.setComboModel((Combobox)oev.getTarget());
    	}
    } // public void onOpenCombo(OpenEvent oev)
    
} // public class FilterableGridDataComposer<T extends Component> extends SimpleGridDataComposer<Component>