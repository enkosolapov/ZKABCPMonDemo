package basos.data.zkmodel;

import org.zkoss.util.media.AMedia;
import org.zkoss.util.resource.Labels;

import basos.data.GridData;
import basos.util.AsyncTask;
import basos.util.BeanToExcelWriter;

import java.io.Serializable;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.locks.StampedLock;

import org.apache.poi.ss.usermodel.*;
//import org.apache.poi.ss.util.CellRangeAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;


/** �������� ����-������ � ����� ������ GridData � Excel (���������� �� �������).
 * � ������ {@link #exportModelDownloadExcel} ������ ������ {@link BeanToExcelWriter} ����������� � ������ ������ ������.
 * ������������ ����������� AsyncTask<AMedia> � ���������� ���������� �������� � ������������ ���������.
 */
public class ListModelToExcelWriter<T extends Object & Serializable & Comparable<? super T>> {

	private static final Logger logger = LoggerFactory.getLogger(ListModelToExcelWriter.class);
	private static final Marker concurMarker = MarkerFactory.getMarker("CONCUR");

	private final Class<T> beanClass;
	private final ListModelListExt<GridData<T>> gridLML; // ������ ������ �����
    private final StampedLock modelRWLock; // RW-����� �������������� � ListModelListExt
	
    /** @param beanClass ����� ����.
     * @param gridLML ����-������, ������ ������� ����� ���������.
     */
	public ListModelToExcelWriter(Class<T> beanClass, ListModelListExt<GridData<T>> gridLML) {
		if (beanClass == null) {
			logger.error("Null argument 'beanClass' not allowed !");
			throw new NullPointerException("Null argument 'beanClass' not allowed !");
		}
		if (gridLML == null) {
			logger.error("Null argument 'gridLML' not allowed !");
			throw new NullPointerException("Null argument 'gridLML' not allowed !");
		}
		this.beanClass = beanClass;
		this.gridLML = gridLML;
		this.modelRWLock = gridLML.getModelRWLock();
	}
    
	/** @param gridLMLX ����-������ �� �������� {@link GridData GridData&lt;T&gt;}.
     */
	public ListModelToExcelWriter(GridDataModelMan<T> gridLMLX) {
		if (gridLMLX == null) {
			logger.error("Null argument 'gridLMLX' not allowed !");
			throw new NullPointerException("Null argument 'gridLMLX' not allowed !");
		}
		this.beanClass = gridLMLX.getBeanClass();
		this.gridLML = gridLMLX.getGridLML();
		this.modelRWLock = this.gridLML.getModelRWLock();
	}
	
	private List<String> secondHead;
	
	/** �������������� ��������� � ������������ ���������� �������. */
	public void setHeader(List<String> hdr) {
		this.secondHead = hdr;
	}
	
	private BeanToExcelWriter.FileTypes fileType = BeanToExcelWriter.FileTypes.SXSSF;
	
	/** ������ �����, �� ��������� SXSSF. */
	public void setFileType(BeanToExcelWriter.FileTypes fileType) {
		this.fileType = fileType;
	}
	
/* XSSF (xlsx): 6500 ������, 5555 �������; ��� ��� ������� (��� � 20) ���������
   HSSF (xls): 25 ���. ����� ����������� ����, �� 33 ���. ������ � ������ �� ��������� ����� �� Out of memory, ��� 77 ���. ����� ������ �� ����� ������������ �����
   SXSSF (xlsx): 80 ���. ����� ��������� �� 6,6 ���. ! � � ������ ������������� � ����� �� ����� � ������ !
     �� 100 ���. ����� �� ���������� �� ���, �������� �� ������� !
     �� 105 ���. ����� �������� �������, ���� ���� ������ �� �������
     �� 120 ���. ����� ������� �������� Out of Memory ��� �������������� ����������, �������� ���� ���������� �� ����� ������������ �����
     �� 160 ���. ����� Out of Memory ��� ������������ �����
 */ 
	
	/** �������� � ������� OOXML (Microsoft Excel XML 2007+, ������ ���������� ����� {@link #setFileType}) ���� ����� � ������� ����-������.
	 * ����������� ��� ���������� ������ � WT ��� ����������� AsyncTask<AMedia>, ������� ����� ������������ ���������� � ���������� �������� (���������� ������������ ������� "indeterm" � "indeterm_progress").
	 * Apache POI ���������� �������� (��. ����� {@link BeanToExcelWriter}), ��������� ����������� ZK EE �� ������������.
	 * �������������� ����� ������������ � ByteArrayOutputStream, ��� ������������� � ������, ����������� AMedia, ������� ������������ � ���� �� ������� (Filedownload.save(AMedia(...byte[])) � ���������� ������).
	 * ������ ��������� ����������� �� ������������ ����� ������ ���� T, ������ (��������������, ��. {@link #setHeader}) - ������������� ����������� ��������.
	 * ������ ��� ������� ����������� - ����� �� ������� � ���� ������ ������ � UI.
	 * ����-������ ����������� ������������� �� ������ �������� �����.
	 * ������ ������ �������������� ����� ���������� ������� {@link #setFileType(basos.util.BeanToExcelWriter.FileTypes) setFileType}.
	 * @param worker ����������� ���� ����� ����������� ������ (FutureTask), ������� �� ��������, ������� ������ "��������" (��������� �� �����������).
	 * @param hdr ������ ��������� � ������������ ���������� ����� (������������). ��� ����� �������������� ���������� ������� {@link #setHeader(List)}.
	 */
	public AMedia exportModelDownloadExcel(AsyncTask<AMedia> worker, List<String> hdr/*nullable*/) { // ���������� �� ��������� � ��������� working thread (� ��� �� ZUL �� ������� toolbarbutton)
		if (hdr != null) this.secondHead = hdr;
// ��� �������� ������� ������ (HugeData): SXSSF (http://poi.apache.org/spreadsheet/how-to.html#sxssf)
		if (worker == null || worker.isCancelled()) {
			logger.info("exportModelDownloadExcel worker.isCancelled �� �����");
			return null;
		}
		AMedia amedia = null;
		BeanToExcelWriter<T> exporter = new BeanToExcelWriter<>( beanClass
																,fileType
																,2/*colOffset*/
		);
		
		exporter.setSxssfRowBufferSize( Integer.valueOf(Labels.getLabel("excel.sxssfRowBufferSize", "1000")).intValue() );
		exporter.setMaxDecimalScale( Integer.valueOf(Labels.getLabel("excel.maxDecimalScale", "10")).intValue() ); // ����. scale ��� BigDecimal, ��� �������� ����������� ������
		exporter.setMaxColumnWidth( Integer.valueOf(Labels.getLabel("excel.maxColumnWidth", "15000")).intValue() ); // ����. ������ ������� (��� ������������������)
		
		//String fileName = exporter.autoNameFile();
	try {
		//exporter.init();
		//exporter.createCommonCellStyles();
		
		// header; ������ ��� ������� - ��������� ����
		Row row = exporter.createRow(); // ����� �� ���������� ���������� ������� �����
		Cell currCell = row.createCell(0);
		currCell.setCellValue("npp"); // ����� �� ������� (1+)
		currCell.setCellStyle(exporter.getHeaderCS());
		currCell = row.createCell(1);
		currCell.setCellValue("selected"); // ������ (�� ����� ��������� �������, ���� GridData)
		currCell.setCellStyle(exporter.getHeaderCS());
/*		currCell = row.createCell(2);
		currCell.setCellValue("UID");
		currCell.setCellStyle(exporter.getHeaderCS());
		currCell = row.createCell(3);
		currCell.setCellValue("filterFlags");
		currCell.setCellStyle(exporter.getHeaderCS());*/
		
		//exporter.parseBeanFields(); // ���������� � init()
		exporter.writeFieldNamesToRow(row); // ��������� �� ������������ �����
		
// ������ ������������� ��������� (������-������������ �����)
		if (secondHead != null) {
			row = exporter.createRow();
			row.setRowStyle(exporter.getHeaderCS());
			currCell = row.createCell(0);
			currCell.setCellValue("npp"); // ����� �� ������� (1+)
			currCell.setCellStyle(exporter.getHeaderCS());
			currCell = row.createCell(1);
			currCell.setCellValue("selected"); // ������ (�� ����� ��������� �������, ���� GridData)
			currCell.setCellStyle(exporter.getHeaderCS());
			int colN = 2;
			for (ListIterator<String> bfi = secondHead.listIterator(); bfi.hasNext();) {
				String curFieldName = bfi.next();
				currCell = row.createCell(colN);
				currCell.setCellValue(curFieldName);
				currCell.setCellStyle(exporter.getHeaderCS());
				logger.trace(" curFieldName = {}, colN = {}", curFieldName, colN);
				++colN;
			} // for_�������_���������
			//exporter.getSheet().getRow(0).setHeightInPoints(0f); // (�� ����������) ��� ���������� ��������� �������� ������ ������ � ������������ ���������� �����
		}

		if (worker == null || worker.isCancelled()) {
			logger.info("exportModelDownloadExcel worker.isCancelled ����� ������������");
			return null;
		}
		
		long startNanoTime = System.nanoTime();
		int rn = 0;
		
		logger.trace(concurMarker, "exportModelDownloadExcel before sync block");
		long stamp = modelRWLock.readLock(); // multiple readers (long operations) allowed !
		try {
			logger.trace(concurMarker, "exportModelDownloadExcel inside sync block, read_stamp = {}", stamp); 	
			int rowCnt = gridLML.size();
//			if (rowCnt < 1) return null;		

			logger.debug("exportModelDownloadExcel before gridLML.listIterator.  workingThread: {}, currentThread: {}, class: {}, progress step = {}, AsyncTask: {}, rowCnt = {}", worker.getWorkingThread(), Thread.currentThread(), Thread.currentThread().getClass().getSimpleName(), Math.min(500, rowCnt/10), worker, rowCnt);
			int prgr = 0;
			for (ListIterator<GridData<T>> gmi = gridLML.listIterator(); gmi.hasNext();) { // ����-������ ���������
				++rn; // = gmi.nextIndex() + 1;
				if (rn%(Math.max(1, Math.min(1000, rowCnt/10))) == 0) { // ������� progressmeter
					logger.trace("exportModelDownloadExcel inside gridLML.listIterator.  progress_bef :" + prgr );
//try { TimeUnit.MILLISECONDS.sleep(500); } catch(InterruptedException e) { logger.trace("sleep() interrupted"); Thread.currentThread().interrupt(); }					
//					Events.sendEvent(pm, new PropertyChangeEvent(this/*source*/, "progress"/*propertyName*/, prgr/*oldValue*/, (int)((double)rn/rowCnt*100)/*newValue*/));
					if (worker != null) {
						worker.firePropertyChange("progress", prgr/*oldValue*/, (int)((double)rn/rowCnt*100)/*newValue*/);
					}
					prgr = (int)((double)rn/rowCnt*100);
					logger.trace("exportModelDownloadExcel inside gridLML.listIterator.  progress_aft = {}", prgr );
				} // if �� ���� ������ ���������� ��������
				GridData<T> curGridData = gmi.next();
				T curSubj = curGridData.getBean();
				
				row = exporter.createRow(); // ����� �� ���������� ���������� ������� �����
				row.createCell(0).setCellValue(rn); // ������� "����� �� �������"
				row.createCell(1).setCellValue(curGridData.isSel()); // ������� ��������
/*				row.createCell(2).setCellValue(curGridData.getUid());
				row.createCell(3).setCellValue(Integer.toBinaryString(curGridData.getFilterFlags()));*/
	
				exporter.writeBeanToRow(row, curSubj);
				
				if (worker == null || worker.isCancelled()) {
					logger.info("exportModelDownloadExcel worker.isCancelled � for_������_����-������");
					return null;
				}

				//if (rn >= 10 && logger.isTraceEnabled()) break;
			} // for_������_����-������
		} finally {
			modelRWLock.unlock(stamp);
			logger.trace(concurMarker, "exportModelDownloadExcel in sync block finally; unlock read_stamp = {}", stamp);
		} // sync block
		
// "indeterm_progress": showBusy() �� ����� ������������ �����, ����� ������ (��� down) clearBusy()
		worker.firePropertyChange("indeterm_progress", false/*oldValue*/, true/*newValue*/);
		
		logger.trace(concurMarker, "exportModelDownloadExcel outside sync block");
		
//(!) ����������� �������� �� ���-�� ������� ��������� ��-�� "progress", �� 4776 ������� (���, �����): 40000(0 �������): 10.18 �; 500 (~ ������ 10%): 22.7 � (� ����������� - 70-100 �); 20 (�� 1%, �.�. 101 ���������) : 157.7 �.
		logger.info("exportModelDownloadExcel before return. fileName: {}, sheetName='{}', colCnt={}, dataFields.size()={}, rowCnt(rn)={}, curRowNum={}, firstDataRowNum={}, fileType={}, spreadsheetVersion={}, duration: {} sec.", exporter.getFileName(), exporter.getSheetName(), exporter.getColCnt(), exporter.getDataFields().size(), rn, exporter.getCurRowNum(), exporter.getFirstDataRowNum(), exporter.getFileType(), exporter.getSpreadsheetVersion(), (System.nanoTime()-startNanoTime) / 1.0e9d );
		
/*		if ( rn > 0 ) {
// ������ ��������� ��������������
			SheetConditionalFormatting sheetCF = exporter.getSheet().getSheetConditionalFormatting();
			ConditionalFormattingRule cfr = sheetCF.createConditionalFormattingRule("$X"+exporter.getFirstDataRowNum()+" < 1000"); // rest_msfo_usd < 1000 - ���������� ��� �����
			PatternFormatting pf = cfr.createPatternFormatting();
			//pf.setFillForegroundColor(IndexedColors.GREEN.index); // ? ��� ���������� ������� ?
			pf.setFillBackgroundColor(IndexedColors.GREY_25_PERCENT.index);
			pf.setFillPattern(PatternFormatting.SOLID_FOREGROUND);
			FontFormatting ff = cfr.createFontFormatting();
			ff.setFontColorIndex(IndexedColors.BLUE.index);
			ff.setFontStyle(true, false);
			CellRangeAddress[] regions = {new CellRangeAddress(exporter.getFirstDataRowNum()-1, exporter.getCurRowNum()-1, 0, exporter.getColCnt()-1)};
			sheetCF.addConditionalFormatting(regions, cfr);
		}*/
		
		exporter.getSheet().setZoom(80);
		exporter.formatSheet(); // autosize col, autofilter, freeze pane

		amedia = new AMedia(exporter.getFileName(), "xls", "application/file", exporter.writeWbToBA());
		//amedia = exporter.formAMedia();
		
	} finally {
		exporter.onExit();
	}
		
		return amedia;
	} // public AMedia exportModelDownloadExcel(AsyncTask<AMedia> worker, List<String> hdr)

} // public class ListModelToExcelWriter<T extends Object & Serializable & Comparable<? super T>>