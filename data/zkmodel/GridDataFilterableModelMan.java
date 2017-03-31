package basos.data.zkmodel;

import java.io.Serializable;
import java.util.ListIterator;
import java.util.Optional;
import java.util.Set;
import java.lang.reflect.*;

import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.ext.Selectable;

import basos.data.GridData;
import basos.data.dao.GridDataProvider;
import basos.zkui.GridDataFilter;

import org.apache.commons.lang3.BitField;
import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;


/** ��������� ����� ����� ������������ ����� (composer)
 * ,����-����������� ������ (������ ������� - "��������� �������"), ���������� ����� ���� � ���� ������ (����) � ������� ��������� ����� (PK)
 * ,����� ������� �����, ����������� ���������� � ������������ �����������
 * ,����������� �������� (������� ���������).
 * Thread-safe. ���������� RW-����� StampedLock ����-������ � �.�. ��� ������ ����� �������� ���� (GridData) �� ������
 *  ����� ������. ����������� ����� UI-�������� ���, ��� ����������� ���������.
 * � {@link GridDataModelMan} �������� ����������� ������.
 * @param <T> ��� ���� (��������� ������), ������� ������������� ������� GridData<T>, �������������� ����� ������ ����-������ �����.
 */
public class GridDataFilterableModelMan<T extends Object & Serializable & Comparable<? super T>> extends GridDataModelMan<T> {
	private static final long serialVersionUID = 3039792128779475493L;
	
	private static final Logger logger = LoggerFactory.getLogger(GridDataFilterableModelMan.class);
	private static final Marker concurMarker = MarkerFactory.getMarker("CONCUR");
	
	private volatile int curGridModelHashCode; // HashCode �������������� ������ ��������� (������� �� ��������� �������); ���� ��� ��������, �� ����� ���������� ���������� �����������
 	private volatile String filterCompositeValue = ""; // ���������� ������� ����� ������� (���������); ����������� ��� ������ ��������� (����������) ������� (applyFilter)
 	private final GridDataFilter dataFilter; // ������ � ��������� �� ������� ����������� �����������
    
// 	private GridDataFilterableModelMan() { // FIXME: ������������ ����������� JavaBeans !!!
// 	}
 	/** �������� ����-������ �� ���� ������, ������������� ����������� ({@link GridDataProvider#getAll()}). �������� ����� ������� ������.
 	 * ������ ����� ����� �������� ������� {@link #getGridLML()}.
 	 * ����������� ��������� �������� ���-���� ������ (������������ ���������), ��������� ������������ �������.
 	 * @param beanClass ����� ���� (��������� ������), ������� ������������� ������� GridData<T>, �������������� ����� ������ ����-������.
 	 * @param dataProvider ��������� ������ ��� ����-������ �����.
 	 * @param dataFilter ������������������ �������� ����������� ����������� ����-������.
 	 */
 	public GridDataFilterableModelMan(Class<T> beanClass, GridDataProvider<T> dataProvider, GridDataFilter dataFilter) {
// ?? ������ beanClass ??; dataFilter ���������� ??
 		super(beanClass, dataProvider, GridDataModelMan.ModelInitTypes.INIT_ENTIRE_MUTABLE/*copy of entireGridDataList*/);
		if (dataFilter == null) {
			logger.error("Null argument 'dataFilter' not allowed !");
			throw new NullPointerException("Null argument 'dataFilter' not allowed !");
		}
 		this.dataFilter = dataFilter;
		if (dataFilter != null) {
			filterCompositeValue = dataFilter.toString();
		}
    	curGridModelHashCode = entireGridDataList.hashCode();
 	} // public GridDataFilterableModelMan(Class<T> beanClass, GridDataProvider<T> dataProvider, GridDataFilter dataFilter)
 	
 	
 	/** ����������� ������, ����������� � ������� ������. */
 	public GridDataFilter getDataFilter() { return dataFilter; }


	/** ������������ ����-������ ���������� ��� ����������� ������ �������� ��������������� ������� �����.
 	 * ������ ����� �� ������� � ������ (dataFilter), ������� ��������� ��������� ����� ������ ����� �� ����������
 	 *  ��������� ����������. ����� ��������� ������ ������� ��� ����������� ������� �����.
 	 * ���� ������ ��������� � �������, ��������� ����� ���� ���������, ������� <b>distinct �� ��������� ����� ��� �ר��
 	 * ������� �� ���������������� ����</b> (����� ������� ����� � ����� ������� ������ GridData.filterFlags,
 	 *  ������������ ����������� ������� ������ ����� �������).
 	 * <p><b>RULE:</b> �� ������� = ���_����_���������_������� (GridData.bean) + "Combo"; ��� ���������� �������� ������� � ����
 	 * ��������� ������� (������ ������� ����������� ����������� ���������� �� ��������������� ������� �����, � ������ ������� �����).
 	 * ��������������, ��� ������� ���������� ����.
 	 * <p>������� ������� <b>filterHash</b> ������ ��� ���������� ����� �� ������ ���������� ������������ ������ �������;
 	 *  ������ ������� ����� ������ ������ ���� ��������� ����� ����� �����.
 	 * ������� � ������ ���� ����������� ��������: {@literal <all>, <null>, <notnull>}.
 	 * ��������� ����-������ �������������, �.�. ������ ��������� ������ ����� � ��� ������.
 	 * @param combo ���������, ��� �������� (����)������ ������.
 	 */
// FIXME: (�� ����� - onOpen, � ��� ��������� ��������) ���������� �� ������� ����� ����������� �������� (����������� � ��������� �� ������) setConstraint ?
 	public void setComboModel(Combobox combo /*, boolean respectFilter - ��������� ������; ���� ����������� ������ � �������� */) {
// TODO: ��������� �� ������� ?
 		String comboId = combo.getId();
// TODO:HOWTO: ? ��� ��������������� ������ ?
//    	ListModelList<String> comboModel = (ListModelList<String>)combo.getModel(); // can't convert ListModel<Object> to ListModelList<String>
    	@SuppressWarnings({ "unchecked", "rawtypes" })
		ListModelList<String> comboModel = (ListModelList)combo.getModel();
    	String oldValue = combo.getValue();
    	int curFilterHashAttr = combo.getAttribute("filterHash") == null ? 0 : (Integer)combo.getAttribute("filterHash");
    	logger.trace("setComboModel.entry.  comboId: {}, comboModel: {}, oldValue = {}, curGridModelHashCode = {}, curFilterHashAttr (prev hash) = '{}'", comboId, comboModel, oldValue, curGridModelHashCode, curFilterHashAttr);
    	
     	if ( comboModel == null // �������������� ���������� (combo.getItemCount() == 0)
    	  || curFilterHashAttr != curGridModelHashCode // �� ������ �������� � �������� ������ (������, ����� �����) �� ��������� � ���������� ������������� �����-������ (curGridModelHashCode ��������������� � applyFilter()) 
    	   ) {
			
// RULE: �� ������� = ���_����_���������_������� (GridData.bean) + "Combo"
			String getterName = "get" + StringUtils.capitalize(StringUtils.removeEnd(comboId, "Combo"));// comboId.substring(0, 1).toUpperCase() + comboId.substring(1, comboId.length()-5);
			Method getterMethod;
			try {
				getterMethod = beanClass.getMethod(getterName);
			} catch (NoSuchMethodException e) {
				logger.error("setComboModel. NoSuchMethodException on invoke '"+getterName+"' for comboId = '"+comboId+"'. Naming Rule: getterName = 'get'+StringUtils.capitalize(StringUtils.removeEnd(comboId, 'Combo')", e);
				throw new InternalError("setComboModel. NoSuchMethodException on invoke '"+getterName+"' for comboId = '"+comboId+"'. Naming Rule: getterName = 'get'+StringUtils.capitalize(StringUtils.removeEnd(comboId, 'Combo')", e);
			}		
// ��������� ��������� (�� ��������������� - �� �����, ���� ������ ��� ���������)
			Set<String> comboSelection = (comboModel==null ? null : ((Selectable<String>)comboModel).getSelection());
			String comboSelectionNext = "";
	    	if (comboSelection != null && !comboSelection.isEmpty()) {
	    		comboSelectionNext = comboSelection.iterator().next(); // ���� �������������� ������ ���
    		}
	    	int dataFilterInd = dataFilter.getComponentIndex(combo);
	    	logger.debug("setComboModel. BEFORE_(re)set_model...  comboId: '{}', value before='{}', text before='{}', selection='{}', model size before: {}, curGridModelHashCode: {}, curFilterHashAttr (prev hash): {}, dataFilterInd: {}", comboId, oldValue, combo.getText(), comboSelectionNext, (comboModel==null ? 0 : comboModel.getSize()), curGridModelHashCode, curFilterHashAttr, dataFilterInd);
	    	BitField bitOrMask;
	    	if ( dataFilterInd < 0) {
	    		bitOrMask = new BitField(0); // ���� ������� �� ��������� � �������, �� ���������� ������� ������
	    	} else {
	    		bitOrMask = new BitField(1 << dataFilterInd); // ����� ������������ ������ �� ������� ���� (��� �� ��� ������ ��� ���������)
	    	}
//    		BitField bitAndMask = new BitField(~(1 << dataFilter.getComponentIndex(combo)));
    		logger.trace("setComboModel. comboId: {}, getterName = {}, bitOrMask = {} (index={}) {}", comboId, getterName, Integer.toBinaryString(bitOrMask.getRawValue(0xFFFFFFFF)), dataFilterInd, bitOrMask.getRawValue(0xFFFFFFFF));
    		if (comboModel == null) {
// TODO: ������������ sorted set ?
    			comboModel = new ListModelList<String>(30);
    			combo.setModel(comboModel);
    		} else {
    			comboModel.clear();
    		}
    		
// TODO: ����������� �� ������ ������ (��������� in properties !); � ��� ������ ? !!!    		
    		int comboModelMaxSize = Integer.valueOf(Labels.getLabel("comboModelMaxSize", "20")).intValue();

    		logger.trace(concurMarker, "setComboModel. comboId = {}  ReadLock acquiring...", comboId);
    		long stamp = modelRWLock.readLock();
    		logger.trace(concurMarker, "setComboModel. comboId = {}  ReadLock successfully acquired.  stamp = {}", comboId, stamp);
    		boolean exceedComboModelMaxSize = false;
    		try {
    			for(ListIterator<GridData<T>> gmi = entireGridDataList.listIterator(); gmi.hasNext();) {
    				GridData<T> curSubj = gmi.next();
if (logger.isTraceEnabled() && curSubj.getBean() instanceof basos.xe.data.entity.SubjSumm && ((basos.xe.data.entity.SubjSumm)curSubj.getBean()).getSubj_id() == 11643) logger.trace("comboId = {}, filterFlags = '{}', apply orMask: '{}'", comboId, Integer.toBinaryString(curSubj.getFilterFlags()), Integer.toBinaryString(bitOrMask.set(curSubj.getFilterFlags())));
//    				if (curSubj.isInFilter()) { // �� ������������ !
    				if (bitOrMask.set(curSubj.getFilterFlags()) == 0xFFFFFFFF) { // ������� ������ �������� �� ���� �������� ��� ����� �������� -> �������� � ������
    					try {
    						String curVal = (String)getterMethod.invoke(curSubj.getBean()); // �������� ����, ���������� � �������� �� �������� = ������� ��������� �������
    						if ( !StringUtils.isEmpty(curVal) && !comboModel.contains(curVal) ) {
    							if ( comboModel.size() < comboModelMaxSize ) {
    								comboModel.add(curVal);
    							} else {
    								exceedComboModelMaxSize = true;
    								break;
// FIXME: ��� � ���������� �������� ?
    							}
    						}
    					} catch ( IllegalAccessException | IllegalArgumentException | java.lang.reflect.InvocationTargetException e) {
    						logger.error("setComboModel.  Exception on invoke "+getterName+" for comboId = "+comboId, e);
    						throw new InternalError("setComboModel.  Exception on invoke "+getterName+" for comboId = "+comboId, e);
    					}
    				}
    			} // for
    			if (exceedComboModelMaxSize) {
    				logger.warn("setComboModel. comboModelMaxSize limit ({}) exceeds for {}", comboModelMaxSize, comboId);
    				Clients.showNotification("comboModelMaxSize limit ("+comboModelMaxSize+") exceeds for "+comboId, Clients.NOTIFICATION_TYPE_WARNING, null, null, 2000);
    			}
        		combo.setAttribute("filterHash", new Integer(curGridModelHashCode)); // ���������� ��������� ������� (��� ����-������ �����) �� ������ ���������� ������ ��������� ����������
    		} finally {
    			modelRWLock.unlock(stamp);
    			logger.trace(concurMarker, "setComboModel. comboId = {}  ReadLock released; stamp = {}", comboId, stamp);
    		}
    		comboModel.sort(String.CASE_INSENSITIVE_ORDER);
// ��������� ����������� �������� (null, notnull, all)
    		comboModel.add(0, "<all>"); comboModel.add(1, "<null>"); comboModel.add(2, "<notnull>"); // � ������ ������ !
//	    		combo.setMultiline(true); // error
//	    		comboModel.setMultiple(true); // ����� ?
// TODO: (HI) !!! ��� ������������ (�� ���������, � autodrop) (���. 429): combo.setModel(ListModels.toListSubModel(new ListModelList(getAllItems(comboModel), ����������_contains, 8))); !!!
//    		combo.setModel(comboModel);
    		if (comboModel.indexOf(oldValue) >= 0) {
    			combo.setValue(oldValue); // ��������������� �������� ����� ���������������� ������
    		} else {
    			combo.setValue(null);
    		}
    		if (comboModel.isSelectionEmpty()) {
    			comboModel.addToSelection(combo.getValue());
    		}
    		comboSelection = (Set<String>)comboModel.getSelection();
    		comboSelectionNext = "";
        	if ( comboSelection != null && !comboSelection.isEmpty() ) {
        		comboSelectionNext = comboSelection.iterator().next(); // ���� �������������� ������ ���
        	}
        	logger.debug("setComboModel. AFTER_(re)set_model...  comboId = '{}', value after = '{}', text after = '{}', selection = '{}', new model size = {}, newFilterHashAttr = {}", comboId, combo.getValue(), combo.getText(), comboSelectionNext, comboModel.getSize(), combo.getAttribute("filterHash"));
    	} // if ���������� ������� => ��������� ���������/��������� ������
// ���� ��� ���������� ������ ������� ���������� ��������
    	else if (!StringUtils.isEmpty(oldValue) && comboModel.indexOf(oldValue) < 0) {
    		combo.setValue(null);
    		logger.debug("setComboModel.  comboId = '{}'.  No reset model, but clear invalid combo value '{}'", comboId, oldValue);
    	}
    	//logger.debug("END setComboModel, comboId="+combo.getId());
    } // public void setComboModel(Combobox combo)

/*
    public int applyFilter(Long stamp) { // not used
    	return applyFilter(this.dataFilter, stamp);
    }

    public int applyFilter(boolean force, Long stamp) { // --��� ��������� ������ ��������
    	return applyFilter(this.dataFilter, force, Optional.empty(), stamp);
    }
    
    public int applyFilter(GridDataFilter _dataFilter, Long stamp) { // --��� ����� ��������� ������ ������-��������
    	return applyFilter(_dataFilter, false, Optional.empty(), stamp);
    }
*/
 	/** ����� {@link #applyFilter(GridDataFilter, boolean, Optional, Long)} � ����������� �������. */
    public int applyFilter(boolean force, Optional<Integer> rn, Long stamp) { // ������ (�� simpleApplyLogic) --��� ������ ������ �������
    	return applyFilter(this.dataFilter, force, rn, stamp);
    }

    
    /** ��������� ��������� ����������� ������ � ������.
     * ��������� ������ ������ ������� (����������) ������ allSubj �� ������������ ������� GridDataFilter
     *  � ������������� ����-������ gridLML.
     * ����� ������� ��������, ����������, ����� ����� (���) ������� (dataFilter.toString()) ��������� �� ���������
     *  � ����� ���������� � ���� filterCompositeValue ��� ������� �������� force == true.
     * ��������� ����� ��� ������� ������� (��� �������� ������).
     * ������ �������� ������ ��� � ������� ����� (����� ��� � ���������� rn).
     * �� ���������� ������ ��������� ��� � ����������� � ���� curGridModelHashCode.
     * ����� ������ ���� �������� � �������� ������������ ���������� ������.
     * @param _dataFilter ����������� ������, ������� ����������� �� ������.
     * @param force True ��� ���������� ������ ��� ���������� �������, �.�. ����� �������� ���������� ������ (������ ��� ������ ���� GridData.sel).
     * @param rn ����� ��������� ������ (��������������). ���� ��������, �� ������������ ������ ������� ������ ������ ������ � ���� ��������.
     * @param stamp ����� ������������ ����������, � ����� ������������ �� �����, ���� �� 0L.
     * @return ���������� ���-�� ����� � ������ ��� -1 ��� ������������ (��� ���� ���������� ��������, �� ���, �.�. ����� �����, �� ���������).
     */
	public int applyFilter(GridDataFilter _dataFilter, boolean force, Optional<Integer> rn, Long stamp) { // ����� ���������� ���-�� ����� (-1 ���� �� ����������)
	  try {	
//		this.dataFilter = _dataFilter; // dataFilter - ����� � SubjsPageComposer, ����� ������ �� ������������ (������� � ������������ � �� ��������)

		String tmpComposite = dataFilter.toString();
		boolean isChanged = !tmpComposite.equals(filterCompositeValue);
		logger.trace("applyFilter_begin. force = {}, rn = {}, stamp = {}, isChanged = {}", force, rn, stamp, isChanged);
		
		if (!force && !isChanged) return -1; // ������ �� ���������
		
		filterCompositeValue = tmpComposite; // ����� ��������� ���������� �������
				
		//long stamp = 0L; // for StampedLock
		
		// ������ ������ �� ���������� � ������� �������� �����������
		if ( dataFilter.isEmpty() && isChanged /*���������� force !*/ ) { // ��� ������ ������� ������������ allSubj, ��������� ����� setInFilter(true)
/*
			logger.trace(concurMarker, "gridDM.applyFilter before sync block (1)");
			boolean locked = true; // �������� ����� ����������
			while (locked) { // FIXME: ��� ����� �� ��������
				try {
					locked = modelRWLock.isReadLocked() || modelRWLock.isWriteLocked(); //tryLock(10, TimeUnit.MILLISECONDS);
					if (locked) { // ���� ��� �������������, ��������� ����������� UI-������
//						long s = System.nanoTime(); while( (System.nanoTime() - s) / 1000000000 < 2) Thread.yield();
						int pr = doSharedLogicOnce();
						if (pr != -1) {
							logger.debug(concurMarker, "gridDM.applyFilter sharedLogic applied before sync block (1) progress = {}", pr);
							//TimeUnit.MILLISECONDS.sleep(2000);
						}
						TimeUnit.MILLISECONDS.sleep(50); // ��� ���� Executions.activate(pmDesktop, 100) �� ����� long operation
					}
				} catch (InterruptedException e) {
					logger.info(concurMarker, "Exception in sync block (1) :", e);
					//Thread.currentThread().interrupt();
				}
			} // �� ������ ����� ������ ������ ���� ��������
			stamp = modelRWLock.writeLock();
*/			
			try {
				logger.trace(concurMarker, "gridDM.applyFilter inside sync block (1)");
 				entireGridDataList.forEach( s -> {
 					s.setInFilter(true);
 					s.setFilterFlags(0xFFFFFFFF);
 				}); // ���� ������� ������� inFilter, ���� ������������ ������� ����� (��� ������ ������������� ������� ���������� �������)
				curGridModelHashCode = entireGridDataList.hashCode(); // �� ������������ ������ ��������� ���: https://docs.oracle.com/javase/8/docs/api/java/util/List.html#hashCode--
				logger.debug("applyFilter (������ ������). filterCompositeValue='{}', new hash= {}, allSubj.size={}", filterCompositeValue, curGridModelHashCode, entireGridDataList.size());
//	    		dataGrid.setModel(new ListModelList<SubjSumm>(allSubj, true)); // setModel ��� ��������� ������ �������� �� ���-�� ����� �� ������
// TODO: �������� � replaceInnerList (entireGridDataList �������� !)
				gridLML.clear(); gridLML.addAll(entireGridDataList);
			} finally {
				//modelRWLock.unlock(stamp);
			} // sync block (1)
			logger.trace(concurMarker, "gridDM.applyFilter outside sync block (1)");

/*			
			int pr = doSharedLogicOnce();
			if (pr != -1) {
				logger.debug(concurMarker, "gridDM.applyFilter sharedLogic applied after sync block (1) progress = {}", pr);
			}
*/			
			
			dataFilter.disableComps(false, true); // ������� ���������� ��� ������-�������� (���������� ��� ���������� �� ���������������, ����������� ��� ������� ��-��������)
	    	return gridLML.getSize();
		} // ������ ������

/*		
		logger.trace(concurMarker, "gridDM.applyFilter before sync block (2)");
		Object stuff = new Object();
	  synchronized (stuff) {
		boolean locked = false; // �������� ���� ����������
		while (!locked) {
			try {
				locked = (stamp = modelRWLock.tryWriteLock(10, TimeUnit.MILLISECONDS)) != 0L;
				if (!locked) {
					int pr = doSharedLogicOnce();
					if (pr != -1) {
						logger.debug(concurMarker, "gridDM.applyFilter sharedLogic applied before sync block (2) progress = {}", pr);
						//TimeUnit.MILLISECONDS.sleep(1000);
					}
//					Thread.yield();
					TimeUnit.MILLISECONDS.timedWait(stuff, 50);
//					TimeUnit.MILLISECONDS.sleep(500); // ��� ���� Executions.activate(pmDesktop, 100) �� ����� long operation
				}
			} catch (InterruptedException e) {
				logger.info(concurMarker, "Exception in sync block (2) :", e);
				Thread.currentThread().interrupt();
			}
		}
	  } // synchronized (stuff)
*/	  
	try { // ����� ��� ������ modelRWLock
		logger.trace(concurMarker, "gridDM.applyFilter inside sync block (2)");

		dataFilter.prepareToApply();
		
		if ( rn.isPresent() ) { // ������� ���� ����������� ������ ��� ������� �� ����� �������; ������ ������ ��������� �������
			int irn = rn.get();
			assert(irn >= 0 && irn < gridLML.size());
			GridData<T> curGridData = gridLML.get(irn);
			if ( dataFilter.evalGridData(curGridData) ) { // � ����� ������ ����� ���������� ������� �����
				curGridData.setInFilter(true);
				//assert(false); // ������ �� ������ ����, �� ����� ��� ������������ ������ (����� � ������� ������ ������ ��� ������������)
			} else {
				curGridData.setInFilter(false);
				gridLML.remove(irn);
				if (curGridData.getBean() instanceof basos.xe.data.entity.SubjSumm) logger.debug("applyFilter[rn branch] (after actions). subj_id = {}, curGridData.isSel = {}, curGridData.isInFilter = {}, curGridData.getUid = {}, irn = {}, gridLML.indexOf(curGridData) = {}, ((SubjSumm)gridLML.get(irn={}).getBean()).getSubj_id() = {}", ((basos.xe.data.entity.SubjSumm)curGridData.getBean()).getSubj_id(), curGridData.isSel(), curGridData.isInFilter(), curGridData.getUid(), irn, gridLML.indexOf(curGridData), irn, ( irn >= gridLML.size() ? "last" : ((basos.xe.data.entity.SubjSumm)gridLML.get(irn).getBean()).getSubj_id() ));
			}
		} // ������� ����� ��������� ������
		else { // ����� ������ �� ����� �� ���� - ��������� ��� ������ �������

			gridLML.clear();
			
			entireGridDataList.forEach(curGridData -> {
				if ( dataFilter.evalGridData(curGridData) ) {
					curGridData.setInFilter(true);
					gridLML.add(curGridData);
				} else {
					curGridData.setInFilter(false);
				}
				if (logger.isTraceEnabled() && curGridData.getBean() instanceof basos.xe.data.entity.SubjSumm && ((basos.xe.data.entity.SubjSumm)curGridData.getBean()).getSubj_id() == 400) logger.trace("subj_id = {}, filterFlags = {}", ((basos.xe.data.entity.SubjSumm)curGridData.getBean()).getSubj_id(), Integer.toBinaryString(curGridData.getFilterFlags()));
			});
			
		} // ����� ������ �� ����� �� ���� - ��������� ��� ������ allSubj (�������) ������ �������
		
	} finally {
		//modelRWLock.unlock(stamp);
	} // sync block (2)
		logger.trace(concurMarker, "gridDM.applyFilter outside sync block (2)");

		int tmpSubjListHashCode = gridLML.getInnerList().hashCode(); // �� ������������ ������ ��������� ���: https://docs.oracle.com/javase/8/docs/api/java/util/List.html#hashCode--
		logger.debug("applyFilter. filterCompositeValue='{}', prev hash= {}, new hash= {}, filteredSubj.size={}", filterCompositeValue, curGridModelHashCode, tmpSubjListHashCode, gridLML.size());

/*
		int pr = doSharedLogicOnce();
		if (pr != -1) {
			logger.debug(concurMarker, "gridDM.applyFilter sharedLogic applied after sync block (2) progress = {}", pr);
		}
*/		
		
		if (curGridModelHashCode != tmpSubjListHashCode) { // ���� ��� ������������ ������� ������� ��� ����-������ ����� (����� �����) ����� �� ���������� (�������������� ���������� ������ !)
			curGridModelHashCode = tmpSubjListHashCode;
/*
setModel ��� ��������� ������ �������� �� ���-�� ����� �� ������ (�� �����, ����� ��� ���� �������������)
 ����� �������� � ������������ ����� �� "����������" ������ (����� �������� ��������� �� ��������� ���� �������� � ������� ?)
*/
//		dataGrid.setModel(gridLML = new ListModelList<SubjSumm>(filteredSubj, true/*live*/));
			return gridLML.getSize();
		}
		
		return -1; // ����� ����� �� ���������
		
	  } finally { // ������������ ����������� �� ����� ����� (0L ���� �������������� ���������� �����)
		  if (stamp != 0L) {
			  modelRWLock.unlock(stamp);
			  logger.trace(concurMarker, "gridDM.applyFilter WriteLock released at end; stamp = {}", stamp);
	  	  }
	  }
	  
	} // public int applyFilter(GridDataFilter _dataFilter, boolean force, Optional<Integer> rn, Long stamp)
	
} // public class GridDataFilterableModelMan<> implements Serializable