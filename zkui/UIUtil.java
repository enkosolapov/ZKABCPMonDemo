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


/** Utility класс для работы с компонентами. */
public final class UIUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(UIUtil.class);
	private static final Marker concurMarker = MarkerFactory.getMarker("CONCUR");
	
	private UIUtil() {}
    
    
	/** Сохранить названия и текущие ширины колонок заданного грида в файл <gridId>.properties (UTF-8).
	 * Например: <gridId>+'.col.idDeal.label = ИД LM сделки (idDeal)' & <gridId>+'.col.idDeal.width = 81px'
	 * Смысл в том, чтобы интерактивно настроить ширины колонок и запомнить их.
	 * Названия соответствующего колонке поля бина берём из атрибута автосортировки.
	 * Опираемся на формат описания колонки: '<column label="ИНН (inn)" width="120px" align="center" sort="auto(bean.inn)"/>'
	 */
	public static void writeMeshHeaderInfo(MeshElement mesh) {
// HOWTO: ? как определить папку приложения (хотя бы задать через свойство в zk-label.properties) ?
// TODO: запоминать для каждого пользователя при завершении сеанса (или по отдельной опции/кнопке)
		String fname = "C:\\Work\\Java\\Eclipse\\ZKOrclReportGrid\\WebContent\\WEB-INF\\"+mesh.getId()+".properties";
    	PrintWriter pw = null; // http://stackoverflow.com/questions/2885173/how-do-i-create-a-file-and-write-to-it-in-java
    	try {
	    	pw = new PrintWriter(fname, "UTF-8"); /* \\WebContent\\WEB-INF\\ */ // def: C:\Install\Programming\Java\JBoss WildFly\wildfly1010F\bin\
// Column и Listheader хоть и наследники HeaderElement, но хреново унаследованы; например, getSortAscending() определены независимо, Grid.getColumns() vs Listbox.getListhead() etc.
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
	
	
	/** Получить из грида/listbox список наименований его колонок. Для выгрузки второго заголовка в Excel. */
	public static List<String> meshHeaderToList(MeshElement mesh) {
// TODO: сделать через stream
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
	
    /** Если в гриде/листбоксе включена автосортировака (атрибут autoSort) и набор строк изменился, то после нужно вручную восстановить порядок.
     * Требует внешней сихронизации (getModelRWLock().writeLock()) модели (сортировка её меняет).
     */
// вызывается из refreshAfterDataChange() после изменения набора строк (который вызывается после применения фильтра, всё в рамках одной блокировки); т.о. синхронизация всегда внешняя
	public static void provokeAutoSort(MeshElement mesh) {
    	List<Component> cols = null;
    	if (mesh instanceof Grid) {
    		cols = ((Grid)mesh).getColumns().getChildren();
    	} else if (mesh instanceof Listbox) {
    		cols = ((Listbox)mesh).getListhead().getChildren();
    	} else {
    		return;
    	}
    	for(Component comp : cols) { // сортированная колонка определяется перебором
    		if ( comp instanceof Column ) {
	    		Column col = (Column)comp;
	    		String sortDir = col.getSortDirection();
	    		if ( !"natural".equals(sortDir) ) {
	// ! принудительно сортируем (включил кастомный атрибут autoSort - не сортируется само после setSortDirection() !)
	//    			((Column)col).setSortDirection("natural");
	//    			((Column)col).setSortDirection(sortDir);
					logger.debug(concurMarker, "setSortDirection (provokeAutoSort) on {} to {}.  Before sort.", col.getLabel(), sortDir);
					col.sort("ascending".equals(sortDir), true); // только с force заработало !
	/*				String msg = "setSortDirection (provokeAutoSort) on "+col.getLabel()+" to "+sortDir;
	    			Consumer<Long> taskToRun = (stamp) -> {
						//long stamp = subjSummModel.getModelRWLock().writeLock();
		    			try {
		    				logger.debug(concurMarker, "{} (UI)...taskToRun. Before sort, stamp: {}", msg, stamp);
		    				col.sort("ascending".equals(sortDir), true); // только с force заработало !
		    			} finally {
		    				if ( stamp != 0L ) {
		    					subjSummModel.getModelRWLock().unlock(stamp);
		    					logger.trace(concurMarker, "{} (UI)...WriteLock released in taskToRun; stamp = {}", msg, stamp);
		    				}
		    			}					
					};
					taskToRun.accept(0L);
					//dispatchGridModelLockingTask(subjSummModel, taskToRun, msg, null, null); // FIXME: не нужно откладывать, он уже вызывается из потокобезопасного метода (ждёт своего вызывающего) !!!
					
	*/    			break;
	    		} // if (сортированная колонка)
    		} else if (comp instanceof Listheader) {
    			Listheader col = (Listheader)comp;
	    		String sortDir = col.getSortDirection();
	    		if ( !"natural".equals(sortDir) ) {
					logger.debug(concurMarker, "setSortDirection (provokeAutoSort) on {} to {}.  Before sort.", col.getLabel(), sortDir);
					col.sort("ascending".equals(sortDir), true); // только с force заработало !
					break;
	    		} // if (сортированная колонка)
    		} // Listheader
    	} // for (поиск колонки с включенной автосортировкой)
    } // public void provokeAutoSort(MeshElement mesh)
    
} // public final class UIUtil