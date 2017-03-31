package basos.zkui;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.zkoss.zk.ui.Component;
import org.zkoss.zul.Column;
import org.zkoss.zul.FieldComparator;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Listheader;
import org.zkoss.zul.impl.MeshElement;


/** Utility ����� ��� ������ � ������������. */
public final class UIUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(UIUtil.class);
	private static final Marker concurMarker = MarkerFactory.getMarker("CONCUR");
	
	private UIUtil() {}
    
    
	/** ��������� �������� � ������� ������ ������� ��������� ����� � ���� <gridId>.properties (UTF-8).
	 * ��������: <gridId>+'.col.idDeal.label = �� LM ������ (idDeal)' & <gridId>+'.col.idDeal.width = 81px'
	 * ����� � ���, ����� ������������ ��������� ������ ������� � ��������� ��.
	 * �������� ���������������� ������� ���� ���� ���� �� �������� ��������������.
	 * ��������� �� ������ �������� �������: '<column label="��� (inn)" width="120px" align="center" sort="auto(bean.inn)"/>'
	 */
	public static void writeMeshHeaderInfo(MeshElement mesh) {
// HOWTO: ? ��� ���������� ����� ���������� (���� �� ������ ����� �������� � zk-label.properties) ?
// TODO: ���������� ��� ������� ������������ ��� ���������� ������ (��� �� ��������� �����/������)
		String fname = "C:\\Work\\Java\\Eclipse\\ZKOrclReportGrid\\WebContent\\WEB-INF\\"+mesh.getId()+".properties";
    	PrintWriter pw = null; // http://stackoverflow.com/questions/2885173/how-do-i-create-a-file-and-write-to-it-in-java
    	try {
	    	pw = new PrintWriter(fname, "UTF-8"); /* \\WebContent\\WEB-INF\\ */ // def: C:\Install\Programming\Java\JBoss WildFly\wildfly1010F\bin\
// Column � Listheader ���� � ���������� HeaderElement, �� ������� ������������; ��������, getSortAscending() ���������� ����������, Grid.getColumns() vs Listbox.getListhead() etc.
	    	if (mesh instanceof Grid) {
	    		Grid grid = (Grid)mesh;
		    	for(Component comp : grid.getColumns().getChildren()) {
		    		if (! (comp instanceof Column)) continue;
		    		Column col = (Column)comp;
		    		int p1 = col.getLabel().indexOf("("), p2 = col.getLabel().indexOf(")"), p3 = ((FieldComparator)col.getSortAscending()).getRawOrderBy().lastIndexOf(".");
		    		String field_name = p1 < 0 ? col.getLabel() : col.getLabel().substring(p1+1, p2)
		    			  ,field_name2 = ((FieldComparator)col.getSortAscending()).getRawOrderBy().substring(Math.max(p3, -1)+1);
		    		logger.trace("writeGridColumnsInfo.  col {} : {}, field_name '{}', p1={}, p2={}, field_name2 '{}', p3={},  orderBy '{}', rawOrderBy '{}'", col.getLabel(), col.getWidth(), field_name, p1, p2, field_name2, p3, ((FieldComparator)col.getSortAscending()).getOrderBy(), ((FieldComparator)col.getSortAscending()).getRawOrderBy() );
		    		pw.println(mesh.getId()+".col."+field_name2+".label = " + col.getLabel());
		    	}
		    	for(Component comp : grid.getColumns().getChildren()) {
		    		if (! (comp instanceof Column)) continue;
		    		Column col = (Column)comp;
		    		int p3 = ((FieldComparator)col.getSortAscending()).getRawOrderBy().lastIndexOf(".");
		    		String field_name2 = ((FieldComparator)col.getSortAscending()).getRawOrderBy().substring(Math.max(p3, -1)+1);
		    		pw.println(mesh.getId()+".col."+field_name2+".width = " + col.getWidth());
		    	}
	    	} // grid
	    	else if (mesh instanceof Listbox) {
	    		Listbox grid = (Listbox)mesh;
		    	for(Component comp : grid.getListhead().getChildren()) {
		    		if (! (comp instanceof Listheader)) continue;
		    		Listheader col = (Listheader)comp;
		    		int p1 = col.getLabel().indexOf("("), p2 = col.getLabel().indexOf(")"), p3 = ((FieldComparator)col.getSortAscending()).getRawOrderBy().lastIndexOf(".");
		    		String field_name = p1 < 0 ? col.getLabel() : col.getLabel().substring(p1+1, p2)
		    			  ,field_name2 = ((FieldComparator)col.getSortAscending()).getRawOrderBy().substring(Math.max(p3, -1)+1);
		    		logger.trace("writeGridColumnsInfo.  col {} : {}, field_name '{}', p1={}, p2={}, field_name2 '{}', p3={},  orderBy '{}', rawOrderBy '{}'", col.getLabel(), col.getWidth(), field_name, p1, p2, field_name2, p3, ((FieldComparator)col.getSortAscending()).getOrderBy(), ((FieldComparator)col.getSortAscending()).getRawOrderBy() );
		    		pw.println(mesh.getId()+".col."+field_name2+".label = " + col.getLabel());
		    	}
		    	for(Component comp : grid.getListhead().getChildren()) {
		    		if (! (comp instanceof Listheader)) continue;
		    		Listheader col = (Listheader)comp;
		    		int p3 = ((FieldComparator)col.getSortAscending()).getRawOrderBy().lastIndexOf(".");
		    		String field_name2 = ((FieldComparator)col.getSortAscending()).getRawOrderBy().substring(Math.max(p3, -1)+1);
		    		pw.println(mesh.getId()+".col."+field_name2+".width = " + col.getWidth());
		    	}
	    	} // listbox
    	} catch(IOException e) {
    		logger.error("IOException in writeGridColumnsInfo", e);
    	} finally {
    		if (pw != null) pw.close();
    	}
	} // public static void writeMeshHeaderInfo(MeshElement mesh)
	
	
	/** �������� �� �����/listbox ������ ������������ ��� �������. ��� �������� ������� ��������� � Excel. */
	public static List<String> meshHeaderToList(MeshElement mesh) {
// TODO: ������� ����� stream
		ArrayList<String> hdr = null;
		if (mesh instanceof Grid) {
    		Grid dataGrid = (Grid)mesh;
    		hdr = new ArrayList<>(dataGrid.getColumns().getChildren().size());
    		int colN = 0;
    		for(Component comp : dataGrid.getColumns().getChildren()) {
    			if (! (comp instanceof Column)) continue;
    			Column col = (Column)comp;
    			String colName = col.getLabel();
    			logger.trace(" colName = {}, colN = {}", colName, colN);
    			colN++;
    			if (/*ListUtils.*/colN <= 2) continue;
    			hdr.add(colName);
    		}
    		logger.trace(" dataGrid.getColumns().getChildren().size() = {}, hdr.size() = {}", dataGrid.getColumns().getChildren().size(), hdr.size());
		} // grid
		else if (mesh instanceof Listbox) {
			Listbox dataGrid = (Listbox)mesh;
			hdr = new ArrayList<>(dataGrid.getListhead().getChildren().size());
			int colN = 0;
			for(Component comp : dataGrid.getListhead().getChildren()) {
				if (! (comp instanceof Listheader)) continue;
				Listheader col = (Listheader)comp;
				String colName = col.getLabel();
				logger.trace(" listheaderName = {}, colN = {}", colName, colN);
				colN++;
				if (/*ListUtils.*/colN <= 2) continue;
				hdr.add(colName);
			}
			logger.trace(" dataGrid.getListhead().getChildren().size() = {}, hdr.size() = {}", dataGrid.getListhead().getChildren().size(), hdr.size());
		} // listbox
		
		return hdr;
	} // public static List<String> meshHeaderToList(MeshElement mesh)
	
    /** ���� � �����/��������� �������� ��������������� (������� autoSort) � ����� ����� ���������, �� ����� ����� ������� ������������ �������.
     * ������� ������� ������������ (getModelRWLock().writeLock()) ������ (���������� � ������).
     */
// ���������� �� refreshAfterDataChange() ����� ��������� ������ ����� (������� ���������� ����� ���������� �������, �� � ������ ����� ����������); �.�. ������������� ������ �������
	public static void provokeAutoSort(MeshElement mesh) {
    	List<Component> cols = null;
    	if (mesh instanceof Grid) {
    		cols = ((Grid)mesh).getColumns().getChildren();
    	} else if (mesh instanceof Listbox) {
    		cols = ((Listbox)mesh).getListhead().getChildren();
    	} else {
    		return;
    	}
    	for(Component comp : cols) { // ������������� ������� ������������ ���������
    		if ( comp instanceof Column ) {
	    		Column col = (Column)comp;
	    		String sortDir = col.getSortDirection();
	    		if ( !"natural".equals(sortDir) ) {
	// ! ������������� ��������� (������� ��������� ������� autoSort - �� ����������� ���� ����� setSortDirection() !)
	//    			((Column)col).setSortDirection("natural");
	//    			((Column)col).setSortDirection(sortDir);
					logger.debug(concurMarker, "setSortDirection (provokeAutoSort) on {} to {}.  Before sort.", col.getLabel(), sortDir);
					col.sort("ascending".equals(sortDir), true); // ������ � force ���������� !
	/*				String msg = "setSortDirection (provokeAutoSort) on "+col.getLabel()+" to "+sortDir;
	    			Consumer<Long> taskToRun = (stamp) -> {
						//long stamp = subjSummModel.getModelRWLock().writeLock();
		    			try {
		    				logger.debug(concurMarker, "{} (UI)...taskToRun. Before sort, stamp: {}", msg, stamp);
		    				col.sort("ascending".equals(sortDir), true); // ������ � force ���������� !
		    			} finally {
		    				if ( stamp != 0L ) {
		    					subjSummModel.getModelRWLock().unlock(stamp);
		    					logger.trace(concurMarker, "{} (UI)...WriteLock released in taskToRun; stamp = {}", msg, stamp);
		    				}
		    			}					
					};
					taskToRun.accept(0L);
					//dispatchGridModelLockingTask(subjSummModel, taskToRun, msg, null, null); // FIXME: �� ����� �����������, �� ��� ���������� �� ����������������� ������ (��� ������ �����������) !!!
					
	*/    			break;
	    		} // if (������������� �������)
    		} else if (comp instanceof Listheader) {
    			Listheader col = (Listheader)comp;
	    		String sortDir = col.getSortDirection();
	    		if ( !"natural".equals(sortDir) ) {
					logger.debug(concurMarker, "setSortDirection (provokeAutoSort) on {} to {}.  Before sort.", col.getLabel(), sortDir);
					col.sort("ascending".equals(sortDir), true); // ������ � force ���������� !
					break;
	    		} // if (������������� �������)
    		} // Listheader
    	} // for (����� ������� � ���������� ���������������)
    } // public void provokeAutoSort(MeshElement mesh)
    
} // public final class UIUtil