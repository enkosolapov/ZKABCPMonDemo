package basos.data.zkmodel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.StampedLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.slf4j.Marker;
//import org.slf4j.MarkerFactory;

import org.zkoss.zul.Grid;
import org.zkoss.zul.Listbox;

import basos.data.GridData;
import basos.data.dao.GridDataProvider;
import basos.data.dao.GridDataProviderWPk;


/** �������� ����-������ {@link ListModelListExt} (�����/���������) � (�������������) ������� ����� ���� {@link GridData GridData&lt;T&gt;}.
 * ���������������� ����-����������� {@link GridDataProvider GridDataProvider&lt;T&gt;}.
 * ����������� ����-������ ���������� ������, ��������������� �����������, ������ (� �� fixed-size). ������ �������� �� {@link #getGridLML()}.
 * ������������� ��� ���� (�� �������� ����������� ����������, �� ����� ��������� � ����������� ��� ���������),
 *  ��������� ����������� �������� ��� list model of GridData.
 * ��� �����/��������� � ����������� �������� ������� ������������ {@link GridDataFilterableModelMan} (���������).
 * @param <T> ��� ���� (��������� ������), ������� ������������� ������� GridData&lt;T&gt;, �������������� ����� ������ ����-������.
 */
public class GridDataModelMan<T extends Object & Serializable & Comparable<? super T>> implements Serializable {
	private static final long serialVersionUID = -2639931301160333891L;
	
	private static final Logger logger = LoggerFactory.getLogger(GridDataModelMan.class);
//	private static final Marker concurMarker = MarkerFactory.getMarker("CONCUR");
	
	protected final Class<T> beanClass;
	protected final GridDataProvider<T> dataProvider; // (��������� � ������ ��� �����) ! ����������� �� �� � ������������ !
	protected /*final*/ List<GridData<T>> entireGridDataList; // ������ �������� �������� �� dataProvider, ������ ��� ����-������
	protected final ListModelListExt<GridData<T>> gridLML; // ������ ������ �����
	protected final StampedLock modelRWLock; // RW-����� �������������� � ListModelListExt
	
	/**  */
	public static enum ModelInitTypes {
		INIT_BLANK, INIT_ENTIRE_FIXED_SIZE, INIT_ENTIRE_MUTABLE
	}
	
	/**  */
	public static enum ModelStates {
		BLANK_STATE, ENTIRE_FIXED_SIZE_STATE, ENTIRE_MUTABLE_STATE, PARTIAL_STATE
	}
	
	protected ModelStates modelState;
	
	/**  */
	public ModelStates getModelState() {
		return modelState;
	}
	
	/** �� ��������� �������� ������ ������ �� ���������� ����� � �������������� �� ���� ������ ������, ������, ��� ������ ����� ���������.
	 * {@link #GridDataModelMan(Class, GridDataProvider, ModelInitTypes) GridDataModelMan(Class, GridDataProvider, GridDataModelMan.ModelInitTypes.INIT_ENTIRE_FIXED_SIZE)}
	 */
	public GridDataModelMan(Class<T> beanClass, GridDataProvider<T> dataProvider) {
		this(beanClass, dataProvider, GridDataModelMan.ModelInitTypes.INIT_ENTIRE_FIXED_SIZE/*live fixed-size entireGridDataList*/);
	}
	
 	/** �������� ����-������ �� ���� ������, ������������� ����������� ({@link GridDataProvider#getAll()}). �������� ����� ������� ������.
 	 * ������ ����� ����� �������� ������� {@link #getGridLML()}.
 	 * @param beanClass ����� ���� (��������� ������), ������� ������������� ������� GridData<T>, �������������� ����� ������ ����-������.
 	 * @param dataProvider ��������� ������ ��� ����-������.
 	 * @param live True - ���������� ��� ������ ������ ���������� ������ (fixed-size); false - ����������� � ArrayList (�����������, ����� �����������).
 	 */
	public GridDataModelMan(Class<T> beanClass, GridDataProvider<T> dataProvider, GridDataModelMan.ModelInitTypes initModelType) {
// ?? ������ beanClass ??
		if (beanClass == null) {
			logger.error("Null argument 'beanClass' not allowed !");
			throw new NullPointerException("Null argument 'beanClass' not allowed !");
		}
		if (dataProvider == null) {
			logger.error("Null argument 'dataProvider' not allowed !");
			throw new NullPointerException("Null argument 'dataProvider' not allowed !");
		}

 		if ( !dataProvider.getBeanClass().equals(beanClass) ) {
 			logger.error("Can't instantiate GridDataFilterableModelMan(). Incosistent parameters. beanClass = '{}' needs to be same as dataProvider.beanClass = '{}'", beanClass.getName(), dataProvider.getBeanClass().getName());
 			throw new InstantiationError("Can't instantiate GridDataFilterableModelMan(). Incosistent parameters. beanClass = "+beanClass.getName()+" needs to be same as dataProvider.beanClass = "+dataProvider.getBeanClass().getName());
 		}
 		this.beanClass = beanClass;
 		this.dataProvider = dataProvider;
 		
 		if (initModelType == GridDataModelMan.ModelInitTypes.INIT_ENTIRE_FIXED_SIZE) { // ��� ������ ����� ������ ����������� ������ ���������� (�.�. ������ fixed-size)
 			entireGridDataList = this.dataProvider.getAll(); // !! ��� �������� ������ (��������� ����������, ������������ �� ������), ���������� �� �� ��� �������� SubjectListORCL()
 			gridLML = new ListModelListExt<GridData<T>>(entireGridDataList, true/*live*/);
 			modelState = GridDataModelMan.ModelStates.ENTIRE_FIXED_SIZE_STATE;
 		} else if (initModelType == GridDataModelMan.ModelInitTypes.INIT_ENTIRE_MUTABLE) { // ����������� ��������� ������������ ����������� ������ (����������)
 			entireGridDataList = this.dataProvider.getAll(); // !! ��� �������� ������ (��������� ����������, ������������ �� ������), ���������� �� �� ��� �������� SubjectListORCL()
 			gridLML = new ListModelListExt<GridData<T>>(new ArrayList<GridData<T>>(entireGridDataList), true/*live*/); // � ����-������ ������� ����� allSubj !
 			modelState = GridDataModelMan.ModelStates.ENTIRE_MUTABLE_STATE;
 		} else { // GridDataModelMan.ModelInitTypes.INIT_BLANK
// TODO: ���� ��� ������� � ��������������� ����� ListModelListExt(int initialCapacity)
 			gridLML = new ListModelListExt<GridData<T>>();
 			modelState = GridDataModelMan.ModelStates.BLANK_STATE;
 		}
    	
    	modelRWLock = gridLML.getModelRWLock();
	} // public GridDataModelMan(Class<T> beanClass, GridDataProvider<T> dataProvider, GridDataModelMan.ModelInitTypes initModelType)
	
	
	/** ������� ��� ���������� ������������� ����-������ ������ ������� �� ���������� (����� ������ ������������ � INIT_BLANK). Thread-safe. */
	public void reinitModelByEntireList(GridDataModelMan.ModelInitTypes initModelType) {
		if (entireGridDataList == null) {
			entireGridDataList = this.dataProvider.getAll();
		}
		if (initModelType == GridDataModelMan.ModelInitTypes.INIT_ENTIRE_FIXED_SIZE) {
			gridLML.safeReplaceInnerList(entireGridDataList);
			modelState = GridDataModelMan.ModelStates.ENTIRE_FIXED_SIZE_STATE;
		} else if (initModelType == GridDataModelMan.ModelInitTypes.INIT_ENTIRE_MUTABLE) {
			gridLML.safeReplaceInnerList(new ArrayList<GridData<T>>(entireGridDataList));
			modelState = GridDataModelMan.ModelStates.ENTIRE_MUTABLE_STATE;
		}
	}
	
	/** ������� ������ �������� �� ���������� �� ���������� �� �������� �������. Thread-safe.
	 * @param key �������� �����, �� �������� ������ ������� ������ ���� �������������.
	 */
	public <U extends Object & Comparable<? super U>> void reinitModelByRange(U key) {
		List<GridData<T>> l = dataProvider.getRange(key);
		gridLML.safeReplaceInnerList(l);
		modelState = GridDataModelMan.ModelStates.PARTIAL_STATE;
	}
	
	/** �������� ������. */
	public void clearModel() {
		gridLML.clearSelection();
		gridLML.clear();
		modelState = GridDataModelMan.ModelStates.BLANK_STATE;
	}
	
	/** ��������� ������ ����� ����-������. */
	public GridDataProvider<T> getDataProvider() { return dataProvider; }
	
 	/** ������� ����-������ �����, ��������� �� ������, ������������� ����������� ({@link GridDataProvider#getAll()}). */
 	protected ListModelListExt<GridData<T>> getGridLML() { return gridLML; }
 	
 	/** ��������� ����-������ ��������� ���������� �����. �������� ������ ������, �.�. ����������� ������; ������, ���� ��������� ������ �������. */
 	public void setModelFor(Grid mesh) { mesh.setModel(gridLML); }
 	/** ��������� ����-������ ��������� ���������� ���������. */
 	public void setModelFor(Listbox mesh) { mesh.setModel(gridLML); }
 	
 	/** ���������� (����������) ������ ������ �� ����������, ������� � ������ ����-������. */
 	public int getTotalRowCount() { return dataProvider.getTotalRowCount(); }
 	
 	/** ���������� ������� ������ ������ ����-������ (����� ��� ��������). */
 	public int getCurRowCount() {return gridLML.size();}
 	
 	/** ����� ���� (��������� ������), ������������ ����������� ����-����������. */
	public Class<T> getBeanClass() { return beanClass; }
	
	/** �������� RW-����� ����-������. */
	public StampedLock getModelRWLock() { return modelRWLock; }
	
	/** �������� �� ����� ��� ����������. */
	public boolean isModelLocked() { return modelRWLock.isReadLocked() || modelRWLock.isWriteLocked(); }
	
	
 	/** ���������� �������� ����� ������ ("����" - ���� GridData.sel) ��� ������ ������ � �������� �������.
 	 * ����� ������ ���� �������� � �������� ������������ ���������� ������.
 	 * @param isChecked ����� �������� ����� (true/false).
 	 * @param uid �������� ����������� �������������� GridData.uid �������-������ (���� �� ���� ������).
 	 * @return ������ �������� � ����-������ (�.�. < 0).
 	 */
//	 * @param irn ������ �������� � ����-������ �����.
// ���������� ������� ����������� onCheckSel(), ������� ����������� ��������� (����� ���������)
 	public int selectRow(boolean isChecked, /*int irn*//*long uid*/GridData<T> curGridData) {
 		long uid = curGridData.getUid();
// ---FIXME: �������������� ����� (���������� ������ ��������) ? ������ � ��������������� ������ ������ �� �� (���� �� ���������� � ������ ������ �������� ��� �� uid ��� ���������� ��) !!!
 		/*int irn = GridData.searchByUid(gridLML.getInnerList(), uid);
		if (irn < 0) return irn; // �� ����� �������� ������ ����� ���������� !
 		GridData<T> curGridData = gridLML.get(irn);*/
 		curGridData.setSel(isChecked);
 		//gridLML.notifyChange(curGridData);
// � ����� ������ ���������� ������� � ������� ����-������
 		int irn = gridLML.indexOf(curGridData);
 		gridLML.notifyChange(irn);
 		String pk = "<provider_wo_pk>";
 		Object pkVal = "<undef>";
 		if (dataProvider instanceof GridDataProviderWPk<?>) {
 			pk = ((GridDataProviderWPk<T>)dataProvider).getPk().orElse("<pk_undef>");
 			if (pk != "<pk_undef>") {
 				pkVal = ((GridDataProviderWPk<T>)dataProvider).getPkValue(curGridData.getBean());
 			}
 		}
 		logger.debug("selectRow (after actions). isChecked = {}, uid = {}, irn = {}, beanClass = {}, Pk = {}, Pk_val = {}, curGridData.isSel = {}, curGridData.isInFilter = {}"
 				,isChecked, uid, irn, dataProvider.getBeanClass().getSimpleName(), pk, pkVal, curGridData.isSel(), curGridData.isInFilter()); // ( curGridData.getBean() instanceof basos.xe.data.entity.SubjSumm ? ((basos.xe.data.entity.SubjSumm)curGridData.getBean()).getSubj_id() : "<WARN: GridData.getBean() instanceof SubjSumm expected !>" )
 		return irn;
 	} // public int selectRow(boolean isChecked, /*int irn*//*long uid*/GridData<T> curGridData)
 	
 	
 	/** ��� ���� ����� ������ �������� ��������� ���� GridData.sel (��������/��������� ��� ������).
 	 * ����� ������ ���� �������� � �������� ������������ ���������� ������.
 	 * @param isChecked ����� �������� ����� (true/false).
 	 */
// ���������� ������� ����������� onCheckSelectAllCHB(), ������� ����������� ��������� (����� ���������)
 	public void selectAllRows(boolean isChecked) {
// HOWTO: ����� ����� ���������������, �������� ����������������� ����-������ ������� � ���������� (dataGrid.getModel() �� ���������������)
		gridLML.forEach(
			(GridData<T> s) -> { s.setSel(isChecked);
						    } 
		);
		gridLML.notifyChangeAll();
		logger.debug("selectAllRows (after actions). isChecked = {}", isChecked);
// HOWTO: !!! SubjsPageComposer.onChange() ���������� ��� ������ ������. �������� ���������� � ����� ����� (deferred) ? !!!
// ? GridDataLoader.doListDataChange(ListDataEvent event) == GridDataLoader.syncModel(-1, -1) ? Grid.getDataLoader() - access-modifiers: empty (package private)
 	} // public void selectAllRows(boolean isChecked)
 	
} // public class GridDataModelMan<T extends Object & Serializable & Comparable<? super T>> implements Serializable