package basos.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
//import java.io.Serializable;
import java.lang.reflect.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import basos.core.TriConsumer;

import org.apache.commons.lang3.StringUtils;

//See http://poi.apache.org/overview.html about JARs (+ http://stackoverflow.com/questions/19739026/java-lang-classnotfoundexception-org-apache-xmlbeans-xmlobject-error)
import org.apache.poi.hssf.usermodel.HSSFWorkbook; // in poi-3.15.jar (+ poi-ooxml-schemas-3.15.jar)
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook; // Для создания больших листов (HugeData): SXSSF (http://poi.apache.org/spreadsheet/how-to.html#sxssf)
//import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook; // in poi-ooxml-3.15.jar (+ xmlbeans-2.6.0.jar + commons-collections4-4.1.jar)
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import org.zkoss.util.resource.Labels;

// FIXME: autoclosable !!
// TODO: метод сохранения книги в файл
// TODO: печать произвольного заголовка (бизнес-наименования полей) из массива/списка
// TODO: дополнительные столбцы в начале (см. colOffset) передавать в writeFieldNamesToRow, {печать_произвольного_заголовка}, writeBeanToRow через varargs (сквозной контроль кол-ва !)
// TODO: builder, включающий параметры форматирования, настройки и setZoom (их вызывать перед writeWbToBA ?)

/** Класс для выгрузки в Excel-файл (xls/xlsx) доменных объектов (дата-бинов).
 * Автоматическое форматирование на основе рефлексивного анализа класса бина.
 * Предполагается использование в цикле для списка объектов с выгрузкой на единственный лист в книге.
 * Технический формат задаётся в конструкторе. SXSSF позволяет формировать очень большие таблицы (проверено на 100 тыс.)
 *  с некоторыми ограничениями доступа в процессе (видимость строк в окне).
 * Параметр colOffset конструктора позволяет первые столбцы (номер по порядку и т.п.) записывать внешними для класса средствами.
 * Способ и формат записи в ячейки книги регулируется методами {@link #addTypedCellWriter()}, {@link #setDefaultCellWriter()}, {@link #fillTypedCellWriters()}.
 * @param <T> Тип бина.
 */
public class BeanToExcelWriter<T> {

	private /*static*/ final Logger logger = LoggerFactory.getLogger(BeanToExcelWriter.class);
	
	/** Доступные форматы файла: HSSF - old xls, XSSF - new xlsx, SXSSF - large (up to 100k) XSSF. */
	public static enum FileTypes {HSSF, XSSF, SXSSF};
	
	protected final Class<T> beanClass;
	protected final FileTypes fileType;
	protected String fileName;
	protected Workbook wb;
	protected Sheet sheet;
	protected final int colOffset; // сколько колонок пропустить в начале
	protected int firstDataRowNum; // при первом вызове writeBeanToRow() отмечаем размер заголовка в строках (+1)
	protected int curRowNum; // храним текущий номер строки (с 1!), для этого всегда создавать строки методом this.createRow() !
	protected Row curRow; // последняя созданная методом createRow() строка (чтобы можно было извне её пользовать)
	protected int colCnt; // общее кол-во столбцов
	protected final boolean externalWb; // признак использования готовой книги, сами не создаём, не выгружаем, не закрываем
	
	protected Field[] declaredFields;
	protected Optional<Boolean>[] accessibleOld; // права доступа к полям запоминаем при парсинге заголовка (перед выходом восстановим права доступа к полям)
	protected List<Field> dataFields; // без обёртки он иммутабелен, а мы будем удалять 
// в parseBeanFields() анализируем список полей доменного объекта и запоминаем в парном к dataFields массиве функции по заполнению и форматированию ячейки в зависимости от типа поля
	protected TriConsumer<Cell, Field, Object>[] setCellTriConsumer;
	protected ByteArrayOutputStream baos;
	
// можно установить до вызова init(), т.е. до первого обращения к функционалу (сразу после вызова конструктора)
	protected int maxDecimalScale = 10; // макс. scale для BigDecimal, для которого подготовлен формат (настройка)
	protected int maxColumnWidth = 15000; // макс. ширина колонки (при автоформатировании) (настройка)
	protected int sxssfRowBufferSize = 1000; // "окно" (настройка)
	protected String sheetName; // по умолчанию короткое имя класса бина
	
	// стили ячеек в зависимости от типа данных их значений
	protected CellStyle dateCS, dateTimeCS, headerCS;
	protected CellStyle[] decimalCSs;
	
	protected Map<String, TriConsumer<Cell, Field, Object>> typedCellWriters; // расширяемый набор правил для записи в заданную ячейку значения заданного поля заданного объекта
	
	protected DateFormat sdf = new SimpleDateFormat("dd.MM.yyyy"); // формат даты java.sql.Date по умолчанию в Ексель-ячейке (для случая передачи текстом)
	protected DateFormat sdtf = new SimpleDateFormat("dd.MM.yyyy hh:mm:ss"); // формат даты-времени java.util.Date по умолчанию в Ексель-ячейке (для случая передачи текстом)
	
	// набор логики записи в ячейку в зависимости от типа (зависят от локальных стилей ячеек)
	protected TriConsumer<Cell, Field, Object> nullCellWriter = (Cell cell, Field field, Object subj) -> {}; // null ?
	protected TriConsumer<Cell, Field, Object> booleanCellWriter = (Cell cell, Field field, Object subj) -> {
		try {
			//logger.trace(String.valueOf(field.getBoolean(subj)));
			cell.setCellValue(field.getBoolean(subj)); // записывается как 0/1
		} catch (IllegalArgumentException | IllegalAccessException e) {
			logger.error("Exception on export to XLSX: booleanCellWriter", e);
		} // try / catch
	};
	protected TriConsumer<Cell, Field, Object> booleanObjCellWriter = (Cell cell, Field field, Object subj) -> {
		try {
			//logger.trace(String.valueOf(field.get(subj)));
			Boolean i = (Boolean)field.get(subj);
			if (i != null) cell.setCellValue(i);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			logger.error("Exception on export to XLSX: integerCellWriter", e);
		} // try / catch
	};
	protected TriConsumer<Cell, Field, Object> intCellWriter = (Cell cell, Field field, Object subj) -> {
		try {
			//logger.trace(String.valueOf(field.getInt(subj)));
			cell.setCellValue(field.getInt(subj));
		} catch (IllegalArgumentException | IllegalAccessException e) {
			logger.error("Exception on export to XLSX: intCellWriter", e);
		} // try / catch
	};
	protected TriConsumer<Cell, Field, Object> integerCellWriter = (Cell cell, Field field, Object subj) -> {
		try {
			//logger.trace(String.valueOf(field.get(subj)));
			Integer i = (Integer)field.get(subj);
			if (i != null) cell.setCellValue(i);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			logger.error("Exception on export to XLSX: integerCellWriter", e);
		} // try / catch
	};
	protected TriConsumer<Cell, Field, Object> stringCellWriter = (Cell cell, Field field, Object subj) -> {
		try {
			//logger.trace((String)field.get(subj));
			cell.setCellValue(((String)field.get(subj)));
		} catch (IllegalArgumentException | IllegalAccessException e) {
			logger.error("Exception on export to XLSX: stringCellWriter", e);
		} // try / catch
	};
	protected TriConsumer<Cell, Field, Object> dateCellWriter = (Cell cell, Field field, Object subj) -> {
		try {
			//logger.trace("{}, formatted: {}", field.get(subj), (field.get(subj) == null ? "" : sdf.format((java.sql.Date)field.get(subj))) );
			java.sql.Date f = (java.sql.Date)field.get(subj);
			//java.util.Date f = (java.util.Date)field.get(subj);
			if (f != null) {
				cell.setCellValue(f);
/*					
				f = new java.util.Date(f.getTime()); // java.sql.Date считает, что не содержит время и кидает IllegalArgumentException для getHours() и пр.
				if ( f.getHours() == 0 && f.getMinutes() == 0 && f.getSeconds() == 0 ) {
					cell.setCellStyle(getDateCS());
				} else {
					cell.setCellStyle(getDateTimeCS());
				}
*/					
				cell.setCellStyle(getDateCS());
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			logger.error("Exception on export to XLSX: dateCellWriter", e);
		} // try / catch
	};
	protected TriConsumer<Cell, Field, Object> dateTimeCellWriter = (Cell cell, Field field, Object subj) -> {
		try {
			//logger.trace("{}, formatted: {}", field.get(subj), (field.get(subj) == null ? "" : sdtf.format((java.util.Date)field.get(subj))) );
			java.util.Date f = (java.util.Date)field.get(subj);
			if (f != null) {
				cell.setCellValue(f);
				cell.setCellStyle(getDateTimeCS());
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			logger.error("Exception on export to XLSX: dateTimeCellWriter", e);
		} // try / catch
	};
	/** Команда записи значения указанного поля типа BigDecimal заданного объекта в целевую ячейку с использованием
	 *  формата {@link #getDecimalCSs()}. Количество десятичных знаков не более {@link #getMaxDecimalScale()}.
	 */
	protected TriConsumer<Cell, Field, Object> decimalCellWriter = (Cell cell, Field field, Object subj) -> {
		try {
			//if (logger.isTraceEnabled()) logger.trace( field.get(subj) + (field.get(subj) == null ? "" : ", doubleValue:"+((BigDecimal)field.get(subj)).doubleValue()+", toString:"+((BigDecimal)field.get(subj)).toString()+", toEngineeringString:"+((BigDecimal)field.get(subj)).toEngineeringString()+", toPlainString:"+((BigDecimal)field.get(subj)).toPlainString()+", scale="+((BigDecimal)field.get(subj)).scale() ) );
			//cell.setCellValue( field.get(subj) == null ? 0.00d : ((BigDecimal)field.get(subj)).doubleValue() );
			BigDecimal d = (BigDecimal)field.get(subj);
			if (d != null) {
				int scale = d.scale();
				cell.setCellValue( d.doubleValue() );
				if (scale >= 0 && scale < maxDecimalScale) {
					cell.setCellStyle(getDecimalCSs()[scale]);
				} else if (scale >= maxDecimalScale) {
					cell.setCellStyle(getDecimalCSs()[maxDecimalScale-1]);
					logger.warn("decimalCellWriter. Превышен допустимый scale = {}, subj = {}, field = {}", scale, subj, field);
				} else {
					cell.setCellStyle(getDecimalCSs()[0]);
					logger.warn("decimalCellWriter. Отрицательный scale = {}, subj = {}, field = {}", scale, subj, field);
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			logger.error("Exception on export to XLSX: decimalCellWriter", e);
		} // try / catch
	};
	
	/** Вариант с выводом в готовую (внешнюю) книгу.
	 * @param beanClass Класс бина.
	 * @param extWb Книга, в которую добавим лист.
	 * @param colOffset Отступ в штуках колонок (не поля бина).
	 */
	public BeanToExcelWriter(Class<T> beanClass, Workbook extWb, int colOffset) {
		this.beanClass = beanClass;
		this.colOffset = colOffset;
		externalWb = true;
		wb = extWb;
		if (extWb instanceof HSSFWorkbook) {
			this.fileType = FileTypes.HSSF;
		} else if (extWb instanceof XSSFWorkbook) {
			this.fileType = FileTypes.XSSF;
		} else if (extWb instanceof SXSSFWorkbook) {
			this.fileType = FileTypes.SXSSF;
		} else {
			logger.error("Книга {} неизвестного типа {}", extWb, extWb.getClass().getName());
			throw new IllegalArgumentException("Книга " + extWb + " неизвестного типа " + extWb.getClass().getName());
		}
	}
	
	/** Книга будет создана данным объектом (не внешняя).
	 * @param beanClass Класс бина.
	 * @param fileType Формат выгрузки.
	 * @param colOffset Отступ в штуках колонок (не поля бина).
	 */
	public BeanToExcelWriter(Class<T> beanClass, FileTypes fileType, int colOffset) {
		this.beanClass = beanClass;
		this.fileType = fileType;
		this.colOffset = colOffset;
		externalWb = false;
	}
	
	/** Признак использования готовой книги, сами не создаём, не выгружаем, не закрываем. */
	public boolean isExternalWb() {
		return externalWb;
	}
	
	/** Команда записи в ячейку по умолчанию (если не найдена по типу) - stringCellWriter. */
	protected TriConsumer<Cell, Field, Object> defaultCellWriter = stringCellWriter; // (? настройка ?) для типов данных, которым не поставлен в соответствие свой typedCellWriter 
	
	/** Команда записи в ячейку по умолчанию (если не найдена по типу) - stringCellWriter. */
	public TriConsumer<Cell, Field, Object> getDefaultCellWriter() {
		return defaultCellWriter;
	}
	
	/** Задать команду записи в ячейку по умолчанию (если не найдена по типу). */
	public void setDefaultCellWriter(TriConsumer<Cell, Field, Object> defaultCellWriter) {
		this.defaultCellWriter = defaultCellWriter;
	}
	
	/** Вся подготовка к выводу включая чтение настроек, создание книги (если не внешняя) и листа, построение набора
	 * команд записи {@link #fillTypedCellWriters()} и интроспекцию класса бина {@link #parseBeanFields()}.
	 * Вызывается многими методами перед первой операцией с листом, явно вызывать не нужно.
	 */
	@SuppressWarnings("unchecked")
	public void init() {
		if (sheet != null) { // защита от повторного запуска
			logger.error("BeanToExcelWriter.init(). Попытка повторной инициализации !");
			return;
		}
		curRowNum = 0;
		colCnt = 0; // общее кол-во столбцов
		/*if (sxssfRowBufferSize == 0) {
			sxssfRowBufferSize = Integer.valueOf(Labels.getLabel("excel.sxssfRowBufferSize", "1000")).intValue();
		}*/
		if (!externalWb) {
			switch(fileType) {
				case HSSF: wb = new HSSFWorkbook(); break; // HSSF - old xls
				case XSSF: wb = new XSSFWorkbook(); break; // XSSF - new xlsx
				case SXSSF: wb = new SXSSFWorkbook(sxssfRowBufferSize); // Для создания больших листов (HugeData): SXSSF (http://poi.apache.org/spreadsheet/how-to.html#sxssf)
							//((SXSSFWorkbook)wb).setCompressTempFiles(true); // temp files will be gzipped
							break; // SXSSF - large XSSF (keep 100 (100-def, -1-unlim) rows in memory, exceeding rows will be flushed to disk)
				default: wb = new HSSFWorkbook(); break; // HSSF - old xls
			}
		}
		
		/*if (maxDecimalScale == 0) {
			maxDecimalScale = Integer.valueOf(Labels.getLabel("excel.maxDecimalScale", "10")).intValue(); // макс. scale для BigDecimal, для которого подготовлен формат
		}
		if (maxColumnWidth == 0) {
			maxColumnWidth = Integer.valueOf(Labels.getLabel("excel.maxColumnWidth", "15000")).intValue(); // макс. ширина колонки (при автоформатировании)
		}*/
		
		sheet = wb.createSheet(getSheetName());
		declaredFields = beanClass.getDeclaredFields();
		accessibleOld = new /*boolean*/Optional[declaredFields.length]; // права доступа к полям запоминаем при парсинге заголовка (перед выходом восстановим права доступа к полям)
			Arrays.fill(accessibleOld, Optional.empty());
		dataFields = new ArrayList<Field>(Arrays.asList(declaredFields)); //без обёртки он иммутабелен, а мы будем удалять 
		setCellTriConsumer = new TriConsumer[declaredFields.length]; // ! будет не полностью заполнен(но без пропусков !), т.к. мапится на бизнес-поля (dataFields после парсинга + пустой хвост) !
		
		fillTypedCellWriters();
		
		parseBeanFields();
	} // public void init()
	
	
	/** Имя листа по умолчанию = <короткое имя класса бина>. */
	public String autoNameSheet() {
		return sheetName = beanClass.getSimpleName();
	}
	
	/** Имя файла по умолчанию = <короткое имя класса бина>_<текущее время в формате yyyyMMdd_HHmmss>.<расширение в зависимости от формата>. */
	public String autoNameFile() {
		return fileName = beanClass.getSimpleName()+"_"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.ROOT/*new Locale("ru", "RU")*/))+(fileType == FileTypes.HSSF ? ".xls" : ".xlsx");
	}
	
	/** Создаёт для заданной книги и возвращает стиль ячейки для типа данных Date с использованием шаблона "dd.mm.yyyy". */
	public static CellStyle createDateCellStyleForWB(Workbook wb) {
		CellStyle dateCellStyle = wb.createCellStyle();
		dateCellStyle.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("dd.mm.yyyy"));
		return dateCellStyle;
	}
	
	/** Создаёт для заданной книги и возвращает стиль ячейки для типа данных DateTime с использованием шаблона "dd.mm.yyyy hh:mm:ss". */
	public static CellStyle createDateTimeCellStyleForWB(Workbook wb) {
		CellStyle dateTimeCellStyle = wb.createCellStyle();
		dateTimeCellStyle.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("dd.mm.yyyy hh:mm:ss"));
		return dateTimeCellStyle;
	}
	
	/** Создаёт для заданной книги и возвращает стиль ячейки заголовка: по центру, с переносом, жирный. */
	public static CellStyle createHeaderCellStyleForWB(Workbook wb) {
		CellStyle headerCellStyle = wb.createCellStyle();
		headerCellStyle.setAlignment(HorizontalAlignment.CENTER);
		headerCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		headerCellStyle.setWrapText(true); // с переносом строки (но как подогнать автоширину по данным без учёта заголовка, а заголовок расширить по высоте ?)
		Font boldFont = wb.createFont(); // HOWTO: как получить текущий шрифт ячейки ?
		boldFont.setBold(true);
		headerCellStyle.setFont(boldFont);
		return headerCellStyle;
	}
	
	/** Создаёт для заданной книги и возвращает стиль ячейки для типа данных Decimal с использованием шаблона "### ### ### ### ##0."+repeat("0", scale). */
	public static CellStyle createDecimalCellStyleForWB(Workbook wb, int scale) {
		CellStyle decimalCellStyle = wb.createCellStyle();
		decimalCellStyle.setDataFormat( wb.getCreationHelper().createDataFormat().getFormat("### ### ### ### ##0."+StringUtils.repeat("0", scale)) );
		return decimalCellStyle;
	}
	
	/** Создание всех предопределённых стилей ячеек. */
// TODO: для книги, переданной в конструкторе (externalWb) можно расшаривать и стили
	public void createCommonCellStyles() {
//		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy"/*, Locale.ROOT*//*new Locale("ru", "RU")*/); // формат даты LocalDate по умолчанию в Ексель-ячейке (для случая передачи текстом)
//		DateFormat sdf = new SimpleDateFormat("dd.MM.yyyy"); // формат даты java.sql.Date по умолчанию в Ексель-ячейке (для случая передачи текстом)
//		DateFormat sdtf = new SimpleDateFormat("dd.MM.yyyy hh:mm:ss"); // формат даты-времени java.util.Date по умолчанию в Ексель-ячейке (для случая передачи текстом)
/* HOWTO: ? как создавать стили независимо от WB, предварительно ?
Для XSSFWorkbook см. StylesTable (http://poi.apache.org/apidocs/org/apache/poi/xssf/model/StylesTable.html)
StylesTable st = new StylesTable();
CellStyle dateCS = st.createCellStyle();
...		
st.setWorkbook((XSSFWorkbook) wb);
Вылетает где-то с IllegalArgumentException("This Style does not belong to the supplied Workbook Stlyes Source. Are you trying to assign a style from one workbook to the cell of a differnt workbook?")
Подход со статикой: http://stackoverflow.com/questions/23594822/creating-a-cellstyle-library-in-apache-poi
*/			
		dateCS = createDateCellStyleForWB(getWb());
		/*dateCS = wb.createCellStyle();
			dateCS.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("dd.mm.yyyy"));*/
		
		dateTimeCS = createDateTimeCellStyleForWB(getWb());
		/*dateTimeCS = wb.createCellStyle();
			dateTimeCS.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("dd.mm.yyyy hh:mm:ss"));*/
			
		//CellStyle timestampCS = wb.createCellStyle();
		
		headerCS = createHeaderCellStyleForWB(getWb());
		/*headerCS = wb.createCellStyle();
			headerCS.setAlignment(HorizontalAlignment.CENTER);
			Font boldFont = wb.createFont(); // HOWTO: как получить текущий шрифт ячейки ?
			boldFont.setBold(true);
			headerCS.setFont(boldFont);*/
		
//TODO: сделать числовой формат более универсальным
		decimalCSs = new CellStyle[maxDecimalScale]; // предопределяем числовые стили со scale in [0, maxDecimalScale-1]
		for (int i = 0; i < maxDecimalScale; i++) {
			decimalCSs[i] = createDecimalCellStyleForWB(getWb(), i);
			/*decimalCSs[i] = wb.createCellStyle();
			decimalCSs[i].setDataFormat( wb.getCreationHelper().createDataFormat().getFormat("### ### ### ### ##0."+StringUtils.repeat("0", i)) );*/
		}
		
	} // public void createCommonCellStyles()
	
	/** Формируем набор правил записи в ячейки в зависимости от типа данных.
	 * Вслед за стилями реализации привязаны к книге.
	 */
	protected void fillTypedCellWriters() {
		typedCellWriters = new HashMap<>((int)(10/0.5), 0.5f); // If the initial capacity is greater than the maximum number of entries divided by the load factor, no rehash operations will ever occur.
		typedCellWriters.put("int", intCellWriter);
		typedCellWriters.put("java.util.Date", dateTimeCellWriter); // дата СО ВРЕМЕНЕМ
		typedCellWriters.put("java.sql.Date", dateCellWriter); // дата БЕЗ ВРЕМЕНИ; дата_текстом: curField.get(curSubj) == null ? "" : sdf.format((java.util.Date)curField.get(curSubj))
		//typedCellWriters.put("java.sql.Timestamp", timestampSetCell); // w nanos !
		typedCellWriters.put("java.lang.Integer", integerCellWriter);
		typedCellWriters.put("java.lang.String", stringCellWriter);
		typedCellWriters.put("java.math.BigDecimal", decimalCellWriter);
		typedCellWriters.put("boolean", booleanCellWriter);
		typedCellWriters.put("java.lang.Boolean", booleanObjCellWriter);
		logger.trace("fillTypedCellWriters. (int)(10/0.5)={}, (10/0.5)={}, typedCellWriters.size={}", (int)(10/0.5), (10/0.5), typedCellWriters.size() );
	} // protected void fillTypedCellWriters()
	
	/** Расширение/замена логики записи в ячейки и их форматирования в зависимости от типа данных (поля).
	 * @param k Имя (полное) типа данных, например "java.lang.Integer"
	 * @param v Логика записи и форматирования в ячейку Cell значения поля Field (используя рефлексию) объекта Object
	 * @return the previous value associated with key, or null if there was no mapping for key.
	*/
	public TriConsumer<Cell, Field, Object> addTypedCellWriter(String k, TriConsumer<Cell, Field, Object> v) {
		return typedCellWriters.put(k, v);
	}
	
	/** Интроспекция класса бина.
	 * Заполняем набор команд typedCellWriters, accessibleOld[] и setCellTriConsumer[], colCnt.
	 * <p/><b>RULE</b>: полями данных считаем все приватные нестатические поля.
	 */
	protected void parseBeanFields() {
		if (typedCellWriters == null) {
			fillTypedCellWriters();
		}
		// поля класса доменного объекта
		int colN = 0; //общий счётчик колонок (бизнес-полей)
		int fn = 0; // считаем все поля
		for (ListIterator<Field> bfi = dataFields.listIterator(); bfi.hasNext();) {
//			int fn = bfi.nextIndex();
			Field curField = bfi.next();
			Class<?> curFieldType = curField.getType();
			String curFieldTypeName = curFieldType.getName();
			String curFieldName = curField.getName();
			boolean curFIsAcc = curField.isAccessible();
			int curM = curField.getModifiers();
			if ( Modifier.isPrivate(curM) && !Modifier.isStatic(curM) ) { // RULE: к полям данных относим все приватные нестатические !
				accessibleOld[/*colN*/fn] = Optional.of(Boolean.valueOf(curFIsAcc));
// FIXME: здесь нужна синхронизация !
				curField.setAccessible(true); // иначе нет доступа к приватным полям !
				logger.trace("+++ fn = {}, colN = {}, curFieldTypeName = {}, curFieldName = {}, Modifiers: {} = '{}', isAccessible = {}", fn, colN, curFieldTypeName, curFieldName, curM, Modifier.toString(curM), curFIsAcc);
				TriConsumer<Cell, Field, Object> tc = typedCellWriters.get(curFieldTypeName);
				if ( tc != null ) {
					setCellTriConsumer[colN] = tc;
				} else {
					setCellTriConsumer[colN] = defaultCellWriter;
					logger.warn("\n unsupported type '{}' in parseBeanFields(); defaultCellWriter will be used.", curFieldTypeName);
				}
				++colN;
			} else { // служебные поля
				logger.trace("-------- fn = {}, curFieldTypeName = {}, curFieldName = {}, Modifiers: {} = '{}', isAccessible = {}", fn, curFieldTypeName, curFieldName, curM, Modifier.toString(curM), curFIsAcc);
				bfi.remove();
			}
			fn++;
		} // for_столбцы_заголовка
		colCnt = colN+colOffset; // штук столбцов ВСЕГО (с учётом colOffset и бизнес-полей)
	} // protected void parseBeanFields()
	
	/** {@link #writeFieldNamesToRow(Row)} с созданием новой строки. */
	public void writeFieldNamesToRow() {
		writeFieldNamesToRow(createRow());
	}
	
	/** Вывод названий полей доменного объекта в строку (заголовок) используя формат {@link #getHeaderCS()}.
	 * Пропускает {@link #getColOffset()} колонок (заполнены вне этого класса).
	 */
	public void writeFieldNamesToRow(Row row) {
		if (dataFields == null) {
			init();
		}
		row.setRowStyle(getHeaderCS());
		int colN = colOffset; //общий счётчик колонок
		for (ListIterator<Field> bfi = dataFields.listIterator(); bfi.hasNext();) {
//			int fn = bfi.nextIndex();
			Field curField = bfi.next();
			String curFieldName = curField.getName();
			Cell currCell = row.createCell(colN);
			currCell.setCellValue(curFieldName);
			if (fileType == FileTypes.HSSF) { // в xls формат уровня строки не работает
				currCell.setCellStyle(getHeaderCS());
			}
			++colN;
		} // for_столбцы_заголовка
	} // public void writeFieldNamesToRow(Row row)
	
	/** Создание строки и запись в неё полей доменного объекта. */
	public void writeBeanToRow(T curSubj) {
		writeBeanToRow(createRow(), curSubj);
	}
	
	/** Вывод полей доменного объекта (с инициализацией при необходимости). */
	public void writeBeanToRow(Row row, T curSubj) {
		if (dataFields == null || setCellTriConsumer == null) {
			init();
		}
		if (firstDataRowNum == 0) { // при первом вызове writeBeanToRow() отмечаем размер(+1) заголовка в строках
			firstDataRowNum = curRowNum;
		}
		int colN = 0; // с учётом colOffset уже занятых столбцов
		for (ListIterator<Field> bfi = dataFields.listIterator(); bfi.hasNext();) { // поля доменного объекта
			Field curField = bfi.next();
			/*if (logger.isTraceEnabled()) {
				Class<?> curFieldType = curField.getType();
				String curFieldTypeName = curFieldType.getName();
				String curFieldName = curField.getName();
				if ("java.math.BigDecimal".equals(curFieldTypeName)) logger.trace("+++ colN = {}, curFieldTypeName = {}, curFieldName = {}, VAL = ...", colN, curFieldTypeName, curFieldName);
			}*/
			Cell currCell = row.createCell(colN+colOffset);
			setCellTriConsumer[colN].accept(currCell, curField, curSubj); // запись в ячейку значения соответствующего поля доменного объекта и форматирование в соответствии с ранее определённым типом данных
			++colN;
		} // for_столбцы_доменного_объекта_текущей_строки_дата-модели
	} // public void writeBeanToRow(Row row, T curSubj)
	
	/** {@link #formatSheet(boolean, boolean, boolean)} со "всё включено". */
	public void formatSheet() {
		formatSheet(true, true, true);
	}
	
	/** Форматировать книгу.
	 * @param isAutosizeColumns Автоширина колонок (но не более {@link #getMaxColumnWidth()}).
	 * @param isAutoFilter Автофильтр.
	 * @param isFreezePane Закрепление областей. Из полей бина считаем, что 1-е - ключевое, его и закрепляем.
	 */
	public void formatSheet(boolean isAutosizeColumns, boolean isAutoFilter, boolean isFreezePane) {
		if (sheet == null) {
			logger.error("formatSheet() вызван до инициализации");
			return;
		}
		if (isAutosizeColumns) {
			// здесь даже лучше - не учтёт заголовок (выравняет по последнему буферу) !
			if (fileType == FileTypes.SXSSF) ((SXSSFSheet)sheet).trackAllColumnsForAutoSizing(); // Tracks all columns in the sheet for auto-sizing. If this is called, individual columns do not need to be tracked. Because determining the best-fit width for a cell is expensive, this may affect the performance. (http://poi.apache.org/apidocs/org/apache/poi/xssf/streaming/SXSSFSheet.html#trackAllColumnsForAutoSizing())
			for (int colN = 0; colN < colCnt; colN++) { // ширина столбцов
				sheet.autoSizeColumn(colN);
				logger.trace( "column '{}{} # {} ({}) of size {}", (colN > 25 ? (char)((int)'A'+colN/26-1) : ""), (char)((int)'A'+colN%26), colN, (colN >= 2 ? dataFields.get(colN-2).getName() : colN == 0 ? "npp" : "selected")/*, sheet.getRow(0).getCell(colN).getStringCellValue()*/, sheet.getColumnWidth(colN));
				if (sheet.getColumnWidth(colN) > maxColumnWidth) sheet.setColumnWidth(colN, maxColumnWidth);
			}
		}
		if (isAutoFilter && firstDataRowNum > 1) {
			logger.trace("  setAutoFilter: 'A:{}{}', colCnt = {}", (colCnt > 25 ? (char)((int)'A'+colCnt/26-1) : ""), (char)((int)'A'+(colCnt-1)%26), colCnt);
			sheet.setAutoFilter( new CellRangeAddress(firstDataRowNum-2, curRowNum-1, 0, colCnt-1)/*CellRangeAddress.valueOf("A:"+(colCnt > 25 ? (char)((int)'A'+colCnt/26-1) : "") + (char)((int)'A'+(colCnt-1)%26))*/ );
		}
		if (isFreezePane && firstDataRowNum > 1) {
			sheet.createFreezePane(1+colOffset, firstDataRowNum-1); // из полей бина считаем, что 1-е - ключевое, его и закрепляем !
		}
	} // public void formatSheet(boolean isAutosizeColumns, boolean isAutoFilter, boolean isFreezePane)
	
	/** Записать книгу. Не финализирующий метод, после его вызова можно продолжать работать с книгой. Не должен вызываться для книги, созданной вовне (внешней). */
	public byte[] writeWbToBA() { // (уровень книги) формирование AMedia из готовой книги;
		if (externalWb) {
			logger.error("writeWbToBA() не должен вызываться для книги, созданной вовне (externalWb) !");
			return null;
		}
// FIXME: baos локализовать в этом методе и перед возвратом закрывать ! 
// TODO: замерить длительность формирования/записи
		baos = new ByteArrayOutputStream();
		try {
			getWb().write(baos);
		} catch(IOException e) {
			logger.error("IOException on export to Excel (writeWbToBA()): wb.write", e);
		}
		byte[] ba = baos.toByteArray();
		return ba;
	} // public AMedia writeWbToBA()

	/** Завершающий код (вызывать в finally). Закрытие книги и т.п. Книгу должен закрывать создавший объект (т.е. внешняя не закрывается). */
//TESTME: проверить на разных этапах, включая отмену до запуска
	public void onExit() {
		if (accessibleOld == null || declaredFields == null) {
			logger.error("onExit() вызван до инициализации");
			return;
		}
		// 1. восстановление прав на поля класса доменного объекта
		assert(accessibleOld.length == declaredFields.length/*dataFields.size()*/) : "accessibleOld.length = "+accessibleOld.length+" <> "+dataFields.size()+" = dataFields.size()";
		/*for (int colN = 0; colN < colCnt; colN++) {
			if (colN >= colOffset) dataFields.get(colN-colOffset).setAccessible(accessibleOld[colN]);
		}*/
		int colN = 0;
		for (Field f : declaredFields) {
			Optional<Boolean> curFA = accessibleOld[colN];
			logger.trace("BeanToExcelWriter.onExit.  colN = {}, fieldName = '{}', isAccessible = {}, restore to {}", colN, f.getName(), f.isAccessible(), (curFA.isPresent() ? curFA.get().booleanValue() : "NOTHING"));
			if ( curFA.isPresent() ) {
				f.setAccessible(curFA.get().booleanValue());
			}
			colN++;
		}
		// 2. закрытие ByteArrayOutputStream и Workbook
		try {
			if (baos != null) baos.close();
			if (!externalWb && wb != null) { // ! книгу должен закрывать создавший объект !
				wb.close();
				if (fileType == FileTypes.SXSSF) {
					boolean succDisposed = ((SXSSFWorkbook)wb).dispose(); // SXSSF allocates temporary files that you must always clean up explicitly, by calling the dispose method
					if (!succDisposed) logger.warn("Temporary SXSSFWorkbook files failed to dispose !");
				}
			}
		} catch (IOException e) {
			logger.error("IOException in BeanToExcelWriter.onExit(): (wb|baos).close", e);
		}
	} // public void onExit()
	
	/** Создать новую строку в нашей книге. Использовать извне при добавлении строк, иначе счётчик строк {@link #getCurRowNum()} будет врать. */
	public Row createRow() {
		return curRow = getSheet().createRow(curRowNum++);
	}
	
	/** Лист (ленивый). */
	public Sheet getSheet() {
		if (sheet == null) {
			init();
		}
		return sheet;
	}
	
	/** Общее количество столбцов (ленивый). */
	public int getColCnt() {
		if (colCnt == 0) {
			init();
		}
		return colCnt;
	}
	
	/** Формат книги. */
	public FileTypes getFileType() {
		return fileType;
	}
	
	/** Имя файла книги (ленивый). По умолчанию {@link #autoNameFile()}. */
	public String getFileName() {
		if (fileName == null) {
			autoNameFile();
		}
		return fileName;
	}
	
	/** Задать название файла книги. */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	/** Книга (ленивый). */
	public Workbook getWb() {
		if (wb == null) {
			init();
		}
		return wb;
	}
	
	/** Сколько колонок пропустить в начале. */
	public int getColOffset() {
		return colOffset;
	}
	
	/** Последняя созданная методом createRow() строка (чтобы можно было извне её пользовать). */
	public Row getCurRow() { // nullable !
		return curRow;
	}
	
	/** Текущее значение счётчика строк (с 1!), для этого всегда создавать строки методом this.createRow() ! */
	public int getCurRowNum() {
		return curRowNum;
	}
	
	/** Максимальный scale для BigDecimal, для которого подготовлен формат (настройка). */
	public int getMaxDecimalScale() {
		return maxDecimalScale;
	}
	
	/** Максимальный scale для BigDecimal, для которого подготовлен формат (настройка). Можно установить до вызова init(), т.е. до первого обращения к листу (сразу после вызова конструктора). */
	public void setMaxDecimalScale(int maxDecimalScale) {
		this.maxDecimalScale = maxDecimalScale;
	}
	
	/** Максимальная ширина колонки (при автоформатировании) (настройка). */
	public int getMaxColumnWidth() {
		return maxColumnWidth;
	}
	
	/** Максимальная ширина колонки (при автоформатировании) (настройка).
	 * Можно установить до вызова init(), т.е. до первого обращения к листу (сразу после вызова конструктора).
	 */
	public void setMaxColumnWidth(int maxColumnWidth) {
		this.maxColumnWidth = maxColumnWidth;
	}
	
	/** "Окно видимости строк" для формата SXSSF (настройка). */
	public int getSxssfRowBufferSize() {
		return sxssfRowBufferSize;
	}
	
	/** "Окно видимости строк" для формата SXSSF (настройка). Можно установить до вызова init(), т.е. до первого обращения к листу (сразу после вызова конструктора). */
	public void setSxssfRowBufferSize(int sxssfRowBufferSize) {
		this.sxssfRowBufferSize = sxssfRowBufferSize;
	}
	
	/** Название листа, по умолчанию (инициализируется {@link #autoNameSheet()} при первом обращении) короткое имя класса бина. */
	public String getSheetName() {
		if (sheetName == null) {
			autoNameSheet();
		}
		return sheetName;
	}
	
	/** Название листа. */
	public void setSheetName(String sheetName) {
		this.sheetName = sheetName;
	}
	
	/** Класс бина. */
	public Class<T> getBeanClass() {
		return beanClass;
	}
	
	/** При первом вызове writeBeanToRow() отмечаем размер заголовка в строках (+1). */
	public int getFirstDataRowNum() {
		return firstDataRowNum;
	}
	
	/** Список полей данных класса бина; каждому по колонке.
	 * Инициализируется из declaredFields в {@link #init()}, окончательно формируется в {@link #parseBeanFields()}.
	 * <p/><b>RULE:</b> к полям данных относим все приватные нестатические.
	 */
	public List<Field> getDataFields() {
		return dataFields;
	}
	
	/** Стиль для ячейки заголовка (ленивый). */
	public CellStyle getHeaderCS() {
		if (headerCS == null) {
			headerCS = createHeaderCellStyleForWB(getWb());
		}
		return headerCS;
	}
	
	/** Стиль ячейки для типа данных Date (ленивый). Маска: "dd.mm.yyyy". */
	public CellStyle getDateCS() {
		if (dateCS == null) {
			dateCS = createDateCellStyleForWB(getWb());
		}
		return dateCS;
	}
	
	/** Стиль ячейки для типа данных DateTime (ленивый). Маска: "dd.mm.yyyy hh:mm:ss". */
	public CellStyle getDateTimeCS() {
		if (dateTimeCS == null) {
			dateTimeCS = createDateTimeCellStyleForWB(getWb());
		}
		return dateTimeCS;
	}
	
	/** Стили ячейки для типа данных Decimal (ленивый). {@link #getMaxDecimalScale()} штук (scale in [0, maxDecimalScale-1]). */
	public CellStyle[] getDecimalCSs() {
		if (decimalCSs == null) {
			decimalCSs = new CellStyle[maxDecimalScale]; // предопределяем числовые стили со scale in [0, maxDecimalScale-1]
			for (int i = 0; i < maxDecimalScale; i++) {
				decimalCSs[i] = createDecimalCellStyleForWB(getWb(), i);
			}
		}
		return decimalCSs;
	}
	
	/** @see Workbook#getSpreadsheetVersion() */
	public SpreadsheetVersion getSpreadsheetVersion() {
		return getWb().getSpreadsheetVersion();
	}
	
} // public class BeanToExcelWriter