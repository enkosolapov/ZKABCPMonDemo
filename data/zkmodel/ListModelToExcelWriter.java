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


/** Выгрузка лист-модели с типом строки GridData в Excel (сохранение на клиенте).
 * В методе {@link #exportModelDownloadExcel} методы класса {@link BeanToExcelWriter} применяются к каждой строке списка.
 * Используется асинхронный AsyncTask<AMedia> с поддержкой прерывания процесса и отображением прогресса.
 */
public class ListModelToExcelWriter<T extends Object & Serializable & Comparable<? super T>> {

	private static final Logger logger = LoggerFactory.getLogger(ListModelToExcelWriter.class);
	private static final Marker concurMarker = MarkerFactory.getMarker("CONCUR");

	private final Class<T> beanClass;
	private final ListModelListExt<GridData<T>> gridLML; // модель данных грида
    private final StampedLock modelRWLock; // RW-замок инкапсулирован в ListModelListExt
	
    /** @param beanClass Класс бина.
     * @param gridLML Лист-модель, строки которой будем выгружать.
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
    
	/** @param gridLMLX Лист-модель со строками {@link GridData GridData&lt;T&gt;}.
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
	
	/** Дополнительный заголовок с читабельными названиями колонок. */
	public void setHeader(List<String> hdr) {
		this.secondHead = hdr;
	}
	
	private BeanToExcelWriter.FileTypes fileType = BeanToExcelWriter.FileTypes.SXSSF;
	
	/** Формат книги, по умолчанию SXSSF. */
	public void setFileType(BeanToExcelWriter.FileTypes fileType) {
		this.fileType = fileType;
	}
	
/* XSSF (xlsx): 6500 падает, 5555 вытянул; при том гораздо (раз в 20) медленнее
   HSSF (xls): 25 тыс. строк выгружаются лихо, на 33 тыс. пердит и падает на последнем этапе по Out of memory, при 77 тыс. строк падает на этапе формирования ячеек
   SXSSF (xlsx): 80 тыс. строк зафигачил за 6,6 сек. ! И в режиме пейджирования в гриде всё чётко и шустро !
     На 100 тыс. строк всё отработало на ура, завалить не удалось !
     На 105 тыс. строк выгрузка валится, даже если ничего не трогать
     На 120 тыс. строк удалось получить Out of Memory при стрессировании интерфейса, выгрузка тоже завалилась на этапе формирования файла
     На 160 тыс. строк Out of Memory при формировании грида
 */ 
	
	/** Выгрузка в формате OOXML (Microsoft Excel XML 2007+, формат выбирается через {@link #setFileType}) всех строк и колонок лист-модели.
	 * Запускается как длительная задача в WT под управлением AsyncTask<AMedia>, который умеет обрабатывать прерывание и отображать прогресс (слушателям направляются события "indeterm" и "indeterm_progress").
	 * Apache POI вызывается напрямую (см. класс {@link BeanToExcelWriter}), связанные возможности ZK EE не используются.
	 * Сформированная книга записывается в ByteArrayOutputStream, тот преобразуется в массив, порождающий AMedia, который записывается в файл на клиенте (Filedownload.save(AMedia(...byte[])) в вызывающем методе).
	 * Первый заголовок формируется из наименований полей класса бина T, второй (необязательный, см. {@link #setHeader}) - подразумевает читабельные названия.
	 * Первые две колонки технические - номер по порядку и флаг выбора строки в UI.
	 * Дата-модель блокируется неэксклюзивно на период перебора строк.
	 * Формат екселя предварительно можно установить вызовом {@link #setFileType(basos.util.BeanToExcelWriter.FileTypes) setFileType}.
	 * @param worker Выполняющая этот вызов асинхронная задача (FutureTask), передаём ей прогресс, слушаем статус "отменено" (прерываем по возможности).
	 * @param hdr Второй заголовок с читабельными названиями полей (необязателен). Его можно предварительно установить вызовом {@link #setHeader(List)}.
	 */
	public AMedia exportModelDownloadExcel(AsyncTask<AMedia> worker, List<String> hdr/*nullable*/) { // вызывается из композера в отдельной working thread (а тот из ZUL по нажатию toolbarbutton)
		if (hdr != null) this.secondHead = hdr;
// Для создания больших листов (HugeData): SXSSF (http://poi.apache.org/spreadsheet/how-to.html#sxssf)
		if (worker == null || worker.isCancelled()) {
			logger.info("exportModelDownloadExcel worker.isCancelled на входе");
			return null;
		}
		AMedia amedia = null;
		BeanToExcelWriter<T> exporter = new BeanToExcelWriter<>( beanClass
																,fileType
																,2/*colOffset*/
		);
		
		exporter.setSxssfRowBufferSize( Integer.valueOf(Labels.getLabel("excel.sxssfRowBufferSize", "1000")).intValue() );
		exporter.setMaxDecimalScale( Integer.valueOf(Labels.getLabel("excel.maxDecimalScale", "10")).intValue() ); // макс. scale для BigDecimal, для которого подготовлен формат
		exporter.setMaxColumnWidth( Integer.valueOf(Labels.getLabel("excel.maxColumnWidth", "15000")).intValue() ); // макс. ширина колонки (при автоформатировании)
		
		//String fileName = exporter.autoNameFile();
	try {
		//exporter.init();
		//exporter.createCommonCellStyles();
		
		// header; первые две колонки - служебные поля
		Row row = exporter.createRow(); // чтобы не обманывать внутренний счётчик строк
		Cell currCell = row.createCell(0);
		currCell.setCellValue("npp"); // номер по порядку (1+)
		currCell.setCellStyle(exporter.getHeaderCS());
		currCell = row.createCell(1);
		currCell.setCellValue("selected"); // крыжик (НЕ часть доменного объекта, поле GridData)
		currCell.setCellStyle(exporter.getHeaderCS());
/*		currCell = row.createCell(2);
		currCell.setCellValue("UID");
		currCell.setCellStyle(exporter.getHeaderCS());
		currCell = row.createCell(3);
		currCell.setCellValue("filterFlags");
		currCell.setCellStyle(exporter.getHeaderCS());*/
		
		//exporter.parseBeanFields(); // перенесено в init()
		exporter.writeFieldNamesToRow(row); // заголовок из наименований полей
		
// печать произвольного заголовка (бизнес-наименования полей)
		if (secondHead != null) {
			row = exporter.createRow();
			row.setRowStyle(exporter.getHeaderCS());
			currCell = row.createCell(0);
			currCell.setCellValue("npp"); // номер по порядку (1+)
			currCell.setCellStyle(exporter.getHeaderCS());
			currCell = row.createCell(1);
			currCell.setCellValue("selected"); // крыжик (НЕ часть доменного объекта, поле GridData)
			currCell.setCellStyle(exporter.getHeaderCS());
			int colN = 2;
			for (ListIterator<String> bfi = secondHead.listIterator(); bfi.hasNext();) {
				String curFieldName = bfi.next();
				currCell = row.createCell(colN);
				currCell.setCellValue(curFieldName);
				currCell.setCellStyle(exporter.getHeaderCS());
				logger.trace(" curFieldName = {}, colN = {}", curFieldName, colN);
				++colN;
			} // for_столбцы_заголовка
			//exporter.getSheet().getRow(0).setHeightInPoints(0f); // (не получилось) при нормальном заголовке скрываем первую строку с техническими названиями полей
		}

		if (worker == null || worker.isCancelled()) {
			logger.info("exportModelDownloadExcel worker.isCancelled перед синхроблоком");
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
			for (ListIterator<GridData<T>> gmi = gridLML.listIterator(); gmi.hasNext();) { // дата-модель построчно
				++rn; // = gmi.nextIndex() + 1;
				if (rn%(Math.max(1, Math.min(1000, rowCnt/10))) == 0) { // обновим progressmeter
					logger.trace("exportModelDownloadExcel inside gridLML.listIterator.  progress_bef :" + prgr );
//try { TimeUnit.MILLISECONDS.sleep(500); } catch(InterruptedException e) { logger.trace("sleep() interrupted"); Thread.currentThread().interrupt(); }					
//					Events.sendEvent(pm, new PropertyChangeEvent(this/*source*/, "progress"/*propertyName*/, prgr/*oldValue*/, (int)((double)rn/rowCnt*100)/*newValue*/));
					if (worker != null) {
						worker.firePropertyChange("progress", prgr/*oldValue*/, (int)((double)rn/rowCnt*100)/*newValue*/);
					}
					prgr = (int)((double)rn/rowCnt*100);
					logger.trace("exportModelDownloadExcel inside gridLML.listIterator.  progress_aft = {}", prgr );
				} // if на этой строке отображать прогресс
				GridData<T> curGridData = gmi.next();
				T curSubj = curGridData.getBean();
				
				row = exporter.createRow(); // чтобы не обманывать внутренний счётчик строк
				row.createCell(0).setCellValue(rn); // колонка "номер по порядку"
				row.createCell(1).setCellValue(curGridData.isSel()); // колонка крыжиков
/*				row.createCell(2).setCellValue(curGridData.getUid());
				row.createCell(3).setCellValue(Integer.toBinaryString(curGridData.getFilterFlags()));*/
	
				exporter.writeBeanToRow(row, curSubj);
				
				if (worker == null || worker.isCancelled()) {
					logger.info("exportModelDownloadExcel worker.isCancelled в for_строки_дата-модели");
					return null;
				}

				//if (rn >= 10 && logger.isTraceEnabled()) break;
			} // for_строки_дата-модели
		} finally {
			modelRWLock.unlock(stamp);
			logger.trace(concurMarker, "exportModelDownloadExcel in sync block finally; unlock read_stamp = {}", stamp);
		} // sync block
		
// "indeterm_progress": showBusy() на время формирования файла, потом неявно (при down) clearBusy()
		worker.firePropertyChange("indeterm_progress", false/*oldValue*/, true/*newValue*/);
		
		logger.trace(concurMarker, "exportModelDownloadExcel outside sync block");
		
//(!) зависимость скорости от кол-ва событий изменения св-ва "progress", на 4776 строках (шаг, строк): 40000(0 событий): 10.18 с; 500 (~ каждые 10%): 22.7 с (с блокировкой - 70-100 с); 20 (по 1%, т.е. 101 обращение) : 157.7 с.
		logger.info("exportModelDownloadExcel before return. fileName: {}, sheetName='{}', colCnt={}, dataFields.size()={}, rowCnt(rn)={}, curRowNum={}, firstDataRowNum={}, fileType={}, spreadsheetVersion={}, duration: {} sec.", exporter.getFileName(), exporter.getSheetName(), exporter.getColCnt(), exporter.getDataFields().size(), rn, exporter.getCurRowNum(), exporter.getFirstDataRowNum(), exporter.getFileType(), exporter.getSpreadsheetVersion(), (System.nanoTime()-startNanoTime) / 1.0e9d );
		
/*		if ( rn > 0 ) {
// пример условного форматирования
			SheetConditionalFormatting sheetCF = exporter.getSheet().getSheetConditionalFormatting();
			ConditionalFormattingRule cfr = sheetCF.createConditionalFormattingRule("$X"+exporter.getFirstDataRowNum()+" < 1000"); // rest_msfo_usd < 1000 - неактивные СПР серым
			PatternFormatting pf = cfr.createPatternFormatting();
			//pf.setFillForegroundColor(IndexedColors.GREEN.index); // ? для несплошной заливки ?
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