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
import org.apache.poi.xssf.streaming.SXSSFWorkbook; // ��� �������� ������� ������ (HugeData): SXSSF (http://poi.apache.org/spreadsheet/how-to.html#sxssf)
//import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook; // in poi-ooxml-3.15.jar (+ xmlbeans-2.6.0.jar + commons-collections4-4.1.jar)
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import org.zkoss.util.resource.Labels;

// FIXME: autoclosable !!
// TODO: ����� ���������� ����� � ����
// TODO: ������ ������������� ��������� (������-������������ �����) �� �������/������
// TODO: �������������� ������� � ������ (��. colOffset) ���������� � writeFieldNamesToRow, {������_�������������_���������}, writeBeanToRow ����� varargs (�������� �������� ���-�� !)
// TODO: builder, ���������� ��������� ��������������, ��������� � setZoom (�� �������� ����� writeWbToBA ?)

/** ����� ��� �������� � Excel-���� (xls/xlsx) �������� �������� (����-�����).
 * �������������� �������������� �� ������ ������������� ������� ������ ����.
 * �������������� ������������� � ����� ��� ������ �������� � ��������� �� ������������ ���� � �����.
 * ����������� ������ ������� � ������������. SXSSF ��������� ����������� ����� ������� ������� (��������� �� 100 ���.)
 *  � ���������� ������������� ������� � �������� (��������� ����� � ����).
 * �������� colOffset ������������ ��������� ������ ������� (����� �� ������� � �.�.) ���������� �������� ��� ������ ����������.
 * ������ � ������ ������ � ������ ����� ������������ �������� {@link #addTypedCellWriter()}, {@link #setDefaultCellWriter()}, {@link #fillTypedCellWriters()}.
 * @param <T> ��� ����.
 */
public class BeanToExcelWriter<T> {

	private /*static*/ final Logger logger = LoggerFactory.getLogger(BeanToExcelWriter.class);
	
	/** ��������� ������� �����: HSSF - old xls, XSSF - new xlsx, SXSSF - large (up to 100k) XSSF. */
	public static enum FileTypes {HSSF, XSSF, SXSSF};
	
	protected final Class<T> beanClass;
	protected final FileTypes fileType;
	protected String fileName;
	protected Workbook wb;
	protected Sheet sheet;
	protected final int colOffset; // ������� ������� ���������� � ������
	protected int firstDataRowNum; // ��� ������ ������ writeBeanToRow() �������� ������ ��������� � ������� (+1)
	protected int curRowNum; // ������ ������� ����� ������ (� 1!), ��� ����� ������ ��������� ������ ������� this.createRow() !
	protected Row curRow; // ��������� ��������� ������� createRow() ������ (����� ����� ���� ����� � ����������)
	protected int colCnt; // ����� ���-�� ��������
	protected final boolean externalWb; // ������� ������������� ������� �����, ���� �� ������, �� ���������, �� ���������
	
	protected Field[] declaredFields;
	protected Optional<Boolean>[] accessibleOld; // ����� ������� � ����� ���������� ��� �������� ��������� (����� ������� ����������� ����� ������� � �����)
	protected List<Field> dataFields; // ��� ������ �� �����������, � �� ����� ������� 
// � parseBeanFields() ����������� ������ ����� ��������� ������� � ���������� � ������ � dataFields ������� ������� �� ���������� � �������������� ������ � ����������� �� ���� ����
	protected TriConsumer<Cell, Field, Object>[] setCellTriConsumer;
	protected ByteArrayOutputStream baos;
	
// ����� ���������� �� ������ init(), �.�. �� ������� ��������� � ����������� (����� ����� ������ ������������)
	protected int maxDecimalScale = 10; // ����. scale ��� BigDecimal, ��� �������� ����������� ������ (���������)
	protected int maxColumnWidth = 15000; // ����. ������ ������� (��� ������������������) (���������)
	protected int sxssfRowBufferSize = 1000; // "����" (���������)
	protected String sheetName; // �� ��������� �������� ��� ������ ����
	
	// ����� ����� � ����������� �� ���� ������ �� ��������
	protected CellStyle dateCS, dateTimeCS, headerCS;
	protected CellStyle[] decimalCSs;
	
	protected Map<String, TriConsumer<Cell, Field, Object>> typedCellWriters; // ����������� ����� ������ ��� ������ � �������� ������ �������� ��������� ���� ��������� �������
	
	protected DateFormat sdf = new SimpleDateFormat("dd.MM.yyyy"); // ������ ���� java.sql.Date �� ��������� � ������-������ (��� ������ �������� �������)
	protected DateFormat sdtf = new SimpleDateFormat("dd.MM.yyyy hh:mm:ss"); // ������ ����-������� java.util.Date �� ��������� � ������-������ (��� ������ �������� �������)
	
	// ����� ������ ������ � ������ � ����������� �� ���� (������� �� ��������� ������ �����)
	protected TriConsumer<Cell, Field, Object> nullCellWriter = (Cell cell, Field field, Object subj) -> {}; // null ?
	protected TriConsumer<Cell, Field, Object> booleanCellWriter = (Cell cell, Field field, Object subj) -> {
		try {
			//logger.trace(String.valueOf(field.getBoolean(subj)));
			cell.setCellValue(field.getBoolean(subj)); // ������������ ��� 0/1
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
				f = new java.util.Date(f.getTime()); // java.sql.Date �������, ��� �� �������� ����� � ������ IllegalArgumentException ��� getHours() � ��.
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
	/** ������� ������ �������� ���������� ���� ���� BigDecimal ��������� ������� � ������� ������ � ��������������
	 *  ������� {@link #getDecimalCSs()}. ���������� ���������� ������ �� ����� {@link #getMaxDecimalScale()}.
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
					logger.warn("decimalCellWriter. �������� ���������� scale = {}, subj = {}, field = {}", scale, subj, field);
				} else {
					cell.setCellStyle(getDecimalCSs()[0]);
					logger.warn("decimalCellWriter. ������������� scale = {}, subj = {}, field = {}", scale, subj, field);
				}
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			logger.error("Exception on export to XLSX: decimalCellWriter", e);
		} // try / catch
	};
	
	/** ������� � ������� � ������� (�������) �����.
	 * @param beanClass ����� ����.
	 * @param extWb �����, � ������� ������� ����.
	 * @param colOffset ������ � ������ ������� (�� ���� ����).
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
			logger.error("����� {} ������������ ���� {}", extWb, extWb.getClass().getName());
			throw new IllegalArgumentException("����� " + extWb + " ������������ ���� " + extWb.getClass().getName());
		}
	}
	
	/** ����� ����� ������� ������ �������� (�� �������).
	 * @param beanClass ����� ����.
	 * @param fileType ������ ��������.
	 * @param colOffset ������ � ������ ������� (�� ���� ����).
	 */
	public BeanToExcelWriter(Class<T> beanClass, FileTypes fileType, int colOffset) {
		this.beanClass = beanClass;
		this.fileType = fileType;
		this.colOffset = colOffset;
		externalWb = false;
	}
	
	/** ������� ������������� ������� �����, ���� �� ������, �� ���������, �� ���������. */
	public boolean isExternalWb() {
		return externalWb;
	}
	
	/** ������� ������ � ������ �� ��������� (���� �� ������� �� ����) - stringCellWriter. */
	protected TriConsumer<Cell, Field, Object> defaultCellWriter = stringCellWriter; // (? ��������� ?) ��� ����� ������, ������� �� ��������� � ������������ ���� typedCellWriter 
	
	/** ������� ������ � ������ �� ��������� (���� �� ������� �� ����) - stringCellWriter. */
	public TriConsumer<Cell, Field, Object> getDefaultCellWriter() {
		return defaultCellWriter;
	}
	
	/** ������ ������� ������ � ������ �� ��������� (���� �� ������� �� ����). */
	public void setDefaultCellWriter(TriConsumer<Cell, Field, Object> defaultCellWriter) {
		this.defaultCellWriter = defaultCellWriter;
	}
	
	/** ��� ���������� � ������ ������� ������ ��������, �������� ����� (���� �� �������) � �����, ���������� ������
	 * ������ ������ {@link #fillTypedCellWriters()} � ������������ ������ ���� {@link #parseBeanFields()}.
	 * ���������� ������� �������� ����� ������ ��������� � ������, ���� �������� �� �����.
	 */
	@SuppressWarnings("unchecked")
	public void init() {
		if (sheet != null) { // ������ �� ���������� �������
			logger.error("BeanToExcelWriter.init(). ������� ��������� ������������� !");
			return;
		}
		curRowNum = 0;
		colCnt = 0; // ����� ���-�� ��������
		/*if (sxssfRowBufferSize == 0) {
			sxssfRowBufferSize = Integer.valueOf(Labels.getLabel("excel.sxssfRowBufferSize", "1000")).intValue();
		}*/
		if (!externalWb) {
			switch(fileType) {
				case HSSF: wb = new HSSFWorkbook(); break; // HSSF - old xls
				case XSSF: wb = new XSSFWorkbook(); break; // XSSF - new xlsx
				case SXSSF: wb = new SXSSFWorkbook(sxssfRowBufferSize); // ��� �������� ������� ������ (HugeData): SXSSF (http://poi.apache.org/spreadsheet/how-to.html#sxssf)
							//((SXSSFWorkbook)wb).setCompressTempFiles(true); // temp files will be gzipped
							break; // SXSSF - large XSSF (keep 100 (100-def, -1-unlim) rows in memory, exceeding rows will be flushed to disk)
				default: wb = new HSSFWorkbook(); break; // HSSF - old xls
			}
		}
		
		/*if (maxDecimalScale == 0) {
			maxDecimalScale = Integer.valueOf(Labels.getLabel("excel.maxDecimalScale", "10")).intValue(); // ����. scale ��� BigDecimal, ��� �������� ����������� ������
		}
		if (maxColumnWidth == 0) {
			maxColumnWidth = Integer.valueOf(Labels.getLabel("excel.maxColumnWidth", "15000")).intValue(); // ����. ������ ������� (��� ������������������)
		}*/
		
		sheet = wb.createSheet(getSheetName());
		declaredFields = beanClass.getDeclaredFields();
		accessibleOld = new /*boolean*/Optional[declaredFields.length]; // ����� ������� � ����� ���������� ��� �������� ��������� (����� ������� ����������� ����� ������� � �����)
			Arrays.fill(accessibleOld, Optional.empty());
		dataFields = new ArrayList<Field>(Arrays.asList(declaredFields)); //��� ������ �� �����������, � �� ����� ������� 
		setCellTriConsumer = new TriConsumer[declaredFields.length]; // ! ����� �� ��������� ��������(�� ��� ��������� !), �.�. ������� �� ������-���� (dataFields ����� �������� + ������ �����) !
		
		fillTypedCellWriters();
		
		parseBeanFields();
	} // public void init()
	
	
	/** ��� ����� �� ��������� = <�������� ��� ������ ����>. */
	public String autoNameSheet() {
		return sheetName = beanClass.getSimpleName();
	}
	
	/** ��� ����� �� ��������� = <�������� ��� ������ ����>_<������� ����� � ������� yyyyMMdd_HHmmss>.<���������� � ����������� �� �������>. */
	public String autoNameFile() {
		return fileName = beanClass.getSimpleName()+"_"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.ROOT/*new Locale("ru", "RU")*/))+(fileType == FileTypes.HSSF ? ".xls" : ".xlsx");
	}
	
	/** ������ ��� �������� ����� � ���������� ����� ������ ��� ���� ������ Date � �������������� ������� "dd.mm.yyyy". */
	public static CellStyle createDateCellStyleForWB(Workbook wb) {
		CellStyle dateCellStyle = wb.createCellStyle();
		dateCellStyle.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("dd.mm.yyyy"));
		return dateCellStyle;
	}
	
	/** ������ ��� �������� ����� � ���������� ����� ������ ��� ���� ������ DateTime � �������������� ������� "dd.mm.yyyy hh:mm:ss". */
	public static CellStyle createDateTimeCellStyleForWB(Workbook wb) {
		CellStyle dateTimeCellStyle = wb.createCellStyle();
		dateTimeCellStyle.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("dd.mm.yyyy hh:mm:ss"));
		return dateTimeCellStyle;
	}
	
	/** ������ ��� �������� ����� � ���������� ����� ������ ���������: �� ������, � ���������, ������. */
	public static CellStyle createHeaderCellStyleForWB(Workbook wb) {
		CellStyle headerCellStyle = wb.createCellStyle();
		headerCellStyle.setAlignment(HorizontalAlignment.CENTER);
		headerCellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
		headerCellStyle.setWrapText(true); // � ��������� ������ (�� ��� ��������� ���������� �� ������ ��� ����� ���������, � ��������� ��������� �� ������ ?)
		Font boldFont = wb.createFont(); // HOWTO: ��� �������� ������� ����� ������ ?
		boldFont.setBold(true);
		headerCellStyle.setFont(boldFont);
		return headerCellStyle;
	}
	
	/** ������ ��� �������� ����� � ���������� ����� ������ ��� ���� ������ Decimal � �������������� ������� "### ### ### ### ##0."+repeat("0", scale). */
	public static CellStyle createDecimalCellStyleForWB(Workbook wb, int scale) {
		CellStyle decimalCellStyle = wb.createCellStyle();
		decimalCellStyle.setDataFormat( wb.getCreationHelper().createDataFormat().getFormat("### ### ### ### ##0."+StringUtils.repeat("0", scale)) );
		return decimalCellStyle;
	}
	
	/** �������� ���� ��������������� ������ �����. */
// TODO: ��� �����, ���������� � ������������ (externalWb) ����� ����������� � �����
	public void createCommonCellStyles() {
//		DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd.MM.yyyy"/*, Locale.ROOT*//*new Locale("ru", "RU")*/); // ������ ���� LocalDate �� ��������� � ������-������ (��� ������ �������� �������)
//		DateFormat sdf = new SimpleDateFormat("dd.MM.yyyy"); // ������ ���� java.sql.Date �� ��������� � ������-������ (��� ������ �������� �������)
//		DateFormat sdtf = new SimpleDateFormat("dd.MM.yyyy hh:mm:ss"); // ������ ����-������� java.util.Date �� ��������� � ������-������ (��� ������ �������� �������)
/* HOWTO: ? ��� ��������� ����� ���������� �� WB, �������������� ?
��� XSSFWorkbook ��. StylesTable (http://poi.apache.org/apidocs/org/apache/poi/xssf/model/StylesTable.html)
StylesTable st = new StylesTable();
CellStyle dateCS = st.createCellStyle();
...		
st.setWorkbook((XSSFWorkbook) wb);
�������� ���-�� � IllegalArgumentException("This Style does not belong to the supplied Workbook Stlyes Source. Are you trying to assign a style from one workbook to the cell of a differnt workbook?")
������ �� ��������: http://stackoverflow.com/questions/23594822/creating-a-cellstyle-library-in-apache-poi
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
			Font boldFont = wb.createFont(); // HOWTO: ��� �������� ������� ����� ������ ?
			boldFont.setBold(true);
			headerCS.setFont(boldFont);*/
		
//TODO: ������� �������� ������ ����� �������������
		decimalCSs = new CellStyle[maxDecimalScale]; // �������������� �������� ����� �� scale in [0, maxDecimalScale-1]
		for (int i = 0; i < maxDecimalScale; i++) {
			decimalCSs[i] = createDecimalCellStyleForWB(getWb(), i);
			/*decimalCSs[i] = wb.createCellStyle();
			decimalCSs[i].setDataFormat( wb.getCreationHelper().createDataFormat().getFormat("### ### ### ### ##0."+StringUtils.repeat("0", i)) );*/
		}
		
	} // public void createCommonCellStyles()
	
	/** ��������� ����� ������ ������ � ������ � ����������� �� ���� ������.
	 * ����� �� ������� ���������� ��������� � �����.
	 */
	protected void fillTypedCellWriters() {
		typedCellWriters = new HashMap<>((int)(10/0.5), 0.5f); // If the initial capacity is greater than the maximum number of entries divided by the load factor, no rehash operations will ever occur.
		typedCellWriters.put("int", intCellWriter);
		typedCellWriters.put("java.util.Date", dateTimeCellWriter); // ���� �� ��������
		typedCellWriters.put("java.sql.Date", dateCellWriter); // ���� ��� �������; ����_�������: curField.get(curSubj) == null ? "" : sdf.format((java.util.Date)curField.get(curSubj))
		//typedCellWriters.put("java.sql.Timestamp", timestampSetCell); // w nanos !
		typedCellWriters.put("java.lang.Integer", integerCellWriter);
		typedCellWriters.put("java.lang.String", stringCellWriter);
		typedCellWriters.put("java.math.BigDecimal", decimalCellWriter);
		typedCellWriters.put("boolean", booleanCellWriter);
		typedCellWriters.put("java.lang.Boolean", booleanObjCellWriter);
		logger.trace("fillTypedCellWriters. (int)(10/0.5)={}, (10/0.5)={}, typedCellWriters.size={}", (int)(10/0.5), (10/0.5), typedCellWriters.size() );
	} // protected void fillTypedCellWriters()
	
	/** ����������/������ ������ ������ � ������ � �� �������������� � ����������� �� ���� ������ (����).
	 * @param k ��� (������) ���� ������, �������� "java.lang.Integer"
	 * @param v ������ ������ � �������������� � ������ Cell �������� ���� Field (��������� ���������) ������� Object
	 * @return the previous value associated with key, or null if there was no mapping for key.
	*/
	public TriConsumer<Cell, Field, Object> addTypedCellWriter(String k, TriConsumer<Cell, Field, Object> v) {
		return typedCellWriters.put(k, v);
	}
	
	/** ������������ ������ ����.
	 * ��������� ����� ������ typedCellWriters, accessibleOld[] � setCellTriConsumer[], colCnt.
	 * <p/><b>RULE</b>: ������ ������ ������� ��� ��������� ������������� ����.
	 */
	protected void parseBeanFields() {
		if (typedCellWriters == null) {
			fillTypedCellWriters();
		}
		// ���� ������ ��������� �������
		int colN = 0; //����� ������� ������� (������-�����)
		int fn = 0; // ������� ��� ����
		for (ListIterator<Field> bfi = dataFields.listIterator(); bfi.hasNext();) {
//			int fn = bfi.nextIndex();
			Field curField = bfi.next();
			Class<?> curFieldType = curField.getType();
			String curFieldTypeName = curFieldType.getName();
			String curFieldName = curField.getName();
			boolean curFIsAcc = curField.isAccessible();
			int curM = curField.getModifiers();
			if ( Modifier.isPrivate(curM) && !Modifier.isStatic(curM) ) { // RULE: � ����� ������ ������� ��� ��������� ������������� !
				accessibleOld[/*colN*/fn] = Optional.of(Boolean.valueOf(curFIsAcc));
// FIXME: ����� ����� ������������� !
				curField.setAccessible(true); // ����� ��� ������� � ��������� ����� !
				logger.trace("+++ fn = {}, colN = {}, curFieldTypeName = {}, curFieldName = {}, Modifiers: {} = '{}', isAccessible = {}", fn, colN, curFieldTypeName, curFieldName, curM, Modifier.toString(curM), curFIsAcc);
				TriConsumer<Cell, Field, Object> tc = typedCellWriters.get(curFieldTypeName);
				if ( tc != null ) {
					setCellTriConsumer[colN] = tc;
				} else {
					setCellTriConsumer[colN] = defaultCellWriter;
					logger.warn("\n unsupported type '{}' in parseBeanFields(); defaultCellWriter will be used.", curFieldTypeName);
				}
				++colN;
			} else { // ��������� ����
				logger.trace("-------- fn = {}, curFieldTypeName = {}, curFieldName = {}, Modifiers: {} = '{}', isAccessible = {}", fn, curFieldTypeName, curFieldName, curM, Modifier.toString(curM), curFIsAcc);
				bfi.remove();
			}
			fn++;
		} // for_�������_���������
		colCnt = colN+colOffset; // ���� �������� ����� (� ������ colOffset � ������-�����)
	} // protected void parseBeanFields()
	
	/** {@link #writeFieldNamesToRow(Row)} � ��������� ����� ������. */
	public void writeFieldNamesToRow() {
		writeFieldNamesToRow(createRow());
	}
	
	/** ����� �������� ����� ��������� ������� � ������ (���������) ��������� ������ {@link #getHeaderCS()}.
	 * ���������� {@link #getColOffset()} ������� (��������� ��� ����� ������).
	 */
	public void writeFieldNamesToRow(Row row) {
		if (dataFields == null) {
			init();
		}
		row.setRowStyle(getHeaderCS());
		int colN = colOffset; //����� ������� �������
		for (ListIterator<Field> bfi = dataFields.listIterator(); bfi.hasNext();) {
//			int fn = bfi.nextIndex();
			Field curField = bfi.next();
			String curFieldName = curField.getName();
			Cell currCell = row.createCell(colN);
			currCell.setCellValue(curFieldName);
			if (fileType == FileTypes.HSSF) { // � xls ������ ������ ������ �� ��������
				currCell.setCellStyle(getHeaderCS());
			}
			++colN;
		} // for_�������_���������
	} // public void writeFieldNamesToRow(Row row)
	
	/** �������� ������ � ������ � �� ����� ��������� �������. */
	public void writeBeanToRow(T curSubj) {
		writeBeanToRow(createRow(), curSubj);
	}
	
	/** ����� ����� ��������� ������� (� �������������� ��� �������������). */
	public void writeBeanToRow(Row row, T curSubj) {
		if (dataFields == null || setCellTriConsumer == null) {
			init();
		}
		if (firstDataRowNum == 0) { // ��� ������ ������ writeBeanToRow() �������� ������(+1) ��������� � �������
			firstDataRowNum = curRowNum;
		}
		int colN = 0; // � ������ colOffset ��� ������� ��������
		for (ListIterator<Field> bfi = dataFields.listIterator(); bfi.hasNext();) { // ���� ��������� �������
			Field curField = bfi.next();
			/*if (logger.isTraceEnabled()) {
				Class<?> curFieldType = curField.getType();
				String curFieldTypeName = curFieldType.getName();
				String curFieldName = curField.getName();
				if ("java.math.BigDecimal".equals(curFieldTypeName)) logger.trace("+++ colN = {}, curFieldTypeName = {}, curFieldName = {}, VAL = ...", colN, curFieldTypeName, curFieldName);
			}*/
			Cell currCell = row.createCell(colN+colOffset);
			setCellTriConsumer[colN].accept(currCell, curField, curSubj); // ������ � ������ �������� ���������������� ���� ��������� ������� � �������������� � ������������ � ����� ����������� ����� ������
			++colN;
		} // for_�������_���������_�������_�������_������_����-������
	} // public void writeBeanToRow(Row row, T curSubj)
	
	/** {@link #formatSheet(boolean, boolean, boolean)} �� "�� ��������". */
	public void formatSheet() {
		formatSheet(true, true, true);
	}
	
	/** ������������� �����.
	 * @param isAutosizeColumns ���������� ������� (�� �� ����� {@link #getMaxColumnWidth()}).
	 * @param isAutoFilter ����������.
	 * @param isFreezePane ����������� ��������. �� ����� ���� �������, ��� 1-� - ��������, ��� � ����������.
	 */
	public void formatSheet(boolean isAutosizeColumns, boolean isAutoFilter, boolean isFreezePane) {
		if (sheet == null) {
			logger.error("formatSheet() ������ �� �������������");
			return;
		}
		if (isAutosizeColumns) {
			// ����� ���� ����� - �� ���� ��������� (��������� �� ���������� ������) !
			if (fileType == FileTypes.SXSSF) ((SXSSFSheet)sheet).trackAllColumnsForAutoSizing(); // Tracks all columns in the sheet for auto-sizing. If this is called, individual columns do not need to be tracked. Because determining the best-fit width for a cell is expensive, this may affect the performance. (http://poi.apache.org/apidocs/org/apache/poi/xssf/streaming/SXSSFSheet.html#trackAllColumnsForAutoSizing())
			for (int colN = 0; colN < colCnt; colN++) { // ������ ��������
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
			sheet.createFreezePane(1+colOffset, firstDataRowNum-1); // �� ����� ���� �������, ��� 1-� - ��������, ��� � ���������� !
		}
	} // public void formatSheet(boolean isAutosizeColumns, boolean isAutoFilter, boolean isFreezePane)
	
	/** �������� �����. �� �������������� �����, ����� ��� ������ ����� ���������� �������� � ������. �� ������ ���������� ��� �����, ��������� ����� (�������). */
	public byte[] writeWbToBA() { // (������� �����) ������������ AMedia �� ������� �����;
		if (externalWb) {
			logger.error("writeWbToBA() �� ������ ���������� ��� �����, ��������� ����� (externalWb) !");
			return null;
		}
// FIXME: baos ������������ � ���� ������ � ����� ��������� ��������� ! 
// TODO: �������� ������������ ������������/������
		baos = new ByteArrayOutputStream();
		try {
			getWb().write(baos);
		} catch(IOException e) {
			logger.error("IOException on export to Excel (writeWbToBA()): wb.write", e);
		}
		byte[] ba = baos.toByteArray();
		return ba;
	} // public AMedia writeWbToBA()

	/** ����������� ��� (�������� � finally). �������� ����� � �.�. ����� ������ ��������� ��������� ������ (�.�. ������� �� �����������). */
//TESTME: ��������� �� ������ ������, ������� ������ �� �������
	public void onExit() {
		if (accessibleOld == null || declaredFields == null) {
			logger.error("onExit() ������ �� �������������");
			return;
		}
		// 1. �������������� ���� �� ���� ������ ��������� �������
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
		// 2. �������� ByteArrayOutputStream � Workbook
		try {
			if (baos != null) baos.close();
			if (!externalWb && wb != null) { // ! ����� ������ ��������� ��������� ������ !
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
	
	/** ������� ����� ������ � ����� �����. ������������ ����� ��� ���������� �����, ����� ������� ����� {@link #getCurRowNum()} ����� �����. */
	public Row createRow() {
		return curRow = getSheet().createRow(curRowNum++);
	}
	
	/** ���� (�������). */
	public Sheet getSheet() {
		if (sheet == null) {
			init();
		}
		return sheet;
	}
	
	/** ����� ���������� �������� (�������). */
	public int getColCnt() {
		if (colCnt == 0) {
			init();
		}
		return colCnt;
	}
	
	/** ������ �����. */
	public FileTypes getFileType() {
		return fileType;
	}
	
	/** ��� ����� ����� (�������). �� ��������� {@link #autoNameFile()}. */
	public String getFileName() {
		if (fileName == null) {
			autoNameFile();
		}
		return fileName;
	}
	
	/** ������ �������� ����� �����. */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	/** ����� (�������). */
	public Workbook getWb() {
		if (wb == null) {
			init();
		}
		return wb;
	}
	
	/** ������� ������� ���������� � ������. */
	public int getColOffset() {
		return colOffset;
	}
	
	/** ��������� ��������� ������� createRow() ������ (����� ����� ���� ����� � ����������). */
	public Row getCurRow() { // nullable !
		return curRow;
	}
	
	/** ������� �������� �������� ����� (� 1!), ��� ����� ������ ��������� ������ ������� this.createRow() ! */
	public int getCurRowNum() {
		return curRowNum;
	}
	
	/** ������������ scale ��� BigDecimal, ��� �������� ����������� ������ (���������). */
	public int getMaxDecimalScale() {
		return maxDecimalScale;
	}
	
	/** ������������ scale ��� BigDecimal, ��� �������� ����������� ������ (���������). ����� ���������� �� ������ init(), �.�. �� ������� ��������� � ����� (����� ����� ������ ������������). */
	public void setMaxDecimalScale(int maxDecimalScale) {
		this.maxDecimalScale = maxDecimalScale;
	}
	
	/** ������������ ������ ������� (��� ������������������) (���������). */
	public int getMaxColumnWidth() {
		return maxColumnWidth;
	}
	
	/** ������������ ������ ������� (��� ������������������) (���������).
	 * ����� ���������� �� ������ init(), �.�. �� ������� ��������� � ����� (����� ����� ������ ������������).
	 */
	public void setMaxColumnWidth(int maxColumnWidth) {
		this.maxColumnWidth = maxColumnWidth;
	}
	
	/** "���� ��������� �����" ��� ������� SXSSF (���������). */
	public int getSxssfRowBufferSize() {
		return sxssfRowBufferSize;
	}
	
	/** "���� ��������� �����" ��� ������� SXSSF (���������). ����� ���������� �� ������ init(), �.�. �� ������� ��������� � ����� (����� ����� ������ ������������). */
	public void setSxssfRowBufferSize(int sxssfRowBufferSize) {
		this.sxssfRowBufferSize = sxssfRowBufferSize;
	}
	
	/** �������� �����, �� ��������� (���������������� {@link #autoNameSheet()} ��� ������ ���������) �������� ��� ������ ����. */
	public String getSheetName() {
		if (sheetName == null) {
			autoNameSheet();
		}
		return sheetName;
	}
	
	/** �������� �����. */
	public void setSheetName(String sheetName) {
		this.sheetName = sheetName;
	}
	
	/** ����� ����. */
	public Class<T> getBeanClass() {
		return beanClass;
	}
	
	/** ��� ������ ������ writeBeanToRow() �������� ������ ��������� � ������� (+1). */
	public int getFirstDataRowNum() {
		return firstDataRowNum;
	}
	
	/** ������ ����� ������ ������ ����; ������� �� �������.
	 * ���������������� �� declaredFields � {@link #init()}, ������������ ����������� � {@link #parseBeanFields()}.
	 * <p/><b>RULE:</b> � ����� ������ ������� ��� ��������� �������������.
	 */
	public List<Field> getDataFields() {
		return dataFields;
	}
	
	/** ����� ��� ������ ��������� (�������). */
	public CellStyle getHeaderCS() {
		if (headerCS == null) {
			headerCS = createHeaderCellStyleForWB(getWb());
		}
		return headerCS;
	}
	
	/** ����� ������ ��� ���� ������ Date (�������). �����: "dd.mm.yyyy". */
	public CellStyle getDateCS() {
		if (dateCS == null) {
			dateCS = createDateCellStyleForWB(getWb());
		}
		return dateCS;
	}
	
	/** ����� ������ ��� ���� ������ DateTime (�������). �����: "dd.mm.yyyy hh:mm:ss". */
	public CellStyle getDateTimeCS() {
		if (dateTimeCS == null) {
			dateTimeCS = createDateTimeCellStyleForWB(getWb());
		}
		return dateTimeCS;
	}
	
	/** ����� ������ ��� ���� ������ Decimal (�������). {@link #getMaxDecimalScale()} ���� (scale in [0, maxDecimalScale-1]). */
	public CellStyle[] getDecimalCSs() {
		if (decimalCSs == null) {
			decimalCSs = new CellStyle[maxDecimalScale]; // �������������� �������� ����� �� scale in [0, maxDecimalScale-1]
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