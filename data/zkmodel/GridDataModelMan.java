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


/** Менеджер лист-модели {@link ListModelListExt} (грида/листбокса) с (фиксированным) набором строк типа {@link GridData GridData&lt;T&gt;}.
 * Инициализируется дата-провайдером {@link GridDataProvider GridDataProvider&lt;T&gt;}.
 * Создаваемая лист-модель использует список, предоставленный провайдером, вживую (а он fixed-size). Модель доступна по {@link #getGridLML()}.
 * Инкапсулирует тип бина (он определён реализацией провайдера, но также передаётся в конструктор для валидации),
 *  реализует специфичные операции для list model of GridData.
 * Для грида/листбокса с композитным фильтром следует использовать {@link GridDataFilterableModelMan} (наследник).
 * @param <T> Тип бина (доменного класса), который оборачивается классом GridData&lt;T&gt;, представляющим собой строку лист-модели.
 */
public class GridDataModelMan<T extends Object & Serializable & Comparable<? super T>> implements Serializable {
	private static final long serialVersionUID = -2639931301160333891L;
	
	private static final Logger logger = LoggerFactory.getLogger(GridDataModelMan.class);
//	private static final Marker concurMarker = MarkerFactory.getMarker("CONCUR");
	
	protected final Class<T> beanClass;
	protected final GridDataProvider<T> dataProvider; // (интерфейс к данным для грида) ! заполняется из БД в конструкторе !
	protected /*final*/ List<GridData<T>> entireGridDataList; // массив доменных объектов из dataProvider, основа для лист-модели
	protected final ListModelListExt<GridData<T>> gridLML; // модель данных грида
	protected final StampedLock modelRWLock; // RW-замок инкапсулирован в ListModelListExt
	
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
	
	/** По умолчанию получаем полный список от провайдера сразу и инициализируем им лист модель вживую, считая, что модель будет неизменна.
	 * {@link #GridDataModelMan(Class, GridDataProvider, ModelInitTypes) GridDataModelMan(Class, GridDataProvider, GridDataModelMan.ModelInitTypes.INIT_ENTIRE_FIXED_SIZE)}
	 */
	public GridDataModelMan(Class<T> beanClass, GridDataProvider<T> dataProvider) {
		this(beanClass, dataProvider, GridDataModelMan.ModelInitTypes.INIT_ENTIRE_FIXED_SIZE/*live fixed-size entireGridDataList*/);
	}
	
 	/** Создатся лист-модель на базе списка, возвращаемого провайдером ({@link GridDataProvider#getAll()}). Создаётся копия полного списка.
 	 * Модель далее можно получить методом {@link #getGridLML()}.
 	 * @param beanClass Класс бина (доменного класса), который оборачивается классом GridData<T>, представляющим собой строку лист-модели.
 	 * @param dataProvider Провайдер данных для лист-модели.
 	 * @param live True - используем для модели список провайдера вживую (fixed-size); false - оборачиваем в ArrayList (мутабельный, можно фильтровать).
 	 */
	public GridDataModelMan(Class<T> beanClass, GridDataProvider<T> dataProvider, GridDataModelMan.ModelInitTypes initModelType) {
// ?? убрать beanClass ??
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
 		
 		if (initModelType == GridDataModelMan.ModelInitTypes.INIT_ENTIRE_FIXED_SIZE) { // при вызове извне только оборачиваем список провайдера (т.к. список fixed-size)
 			entireGridDataList = this.dataProvider.getAll(); // !! это исходный список (приватная переменная, возвращённая по ссылке), полученный из БД при создании SubjectListORCL()
 			gridLML = new ListModelListExt<GridData<T>>(entireGridDataList, true/*live*/);
 			modelState = GridDataModelMan.ModelStates.ENTIRE_FIXED_SIZE_STATE;
 		} else if (initModelType == GridDataModelMan.ModelInitTypes.INIT_ENTIRE_MUTABLE) { // наследникам позволяем использовать мутабельный список (фильтрация)
 			entireGridDataList = this.dataProvider.getAll(); // !! это исходный список (приватная переменная, возвращённая по ссылке), полученный из БД при создании SubjectListORCL()
 			gridLML = new ListModelListExt<GridData<T>>(new ArrayList<GridData<T>>(entireGridDataList), true/*live*/); // в лист-модель передаём копию allSubj !
 			modelState = GridDataModelMan.ModelStates.ENTIRE_MUTABLE_STATE;
 		} else { // GridDataModelMan.ModelInitTypes.INIT_BLANK
// TODO: есть ещё вариант с резервированием места ListModelListExt(int initialCapacity)
 			gridLML = new ListModelListExt<GridData<T>>();
 			modelState = GridDataModelMan.ModelStates.BLANK_STATE;
 		}
    	
    	modelRWLock = gridLML.getModelRWLock();
	} // public GridDataModelMan(Class<T> beanClass, GridDataProvider<T> dataProvider, GridDataModelMan.ModelInitTypes initModelType)
	
	
	/** Задуман для отложенной инициализации лист-модели полным списком от провайдера (после вызова конструктора с INIT_BLANK). Thread-safe. */
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
	
	/** Подмена списка объектов на полученный от провайдера по значению индекса. Thread-safe.
	 * @param key Значение ключа, по которому модель заранее должна быть отсортирована.
	 */
	public <U extends Object & Comparable<? super U>> void reinitModelByRange(U key) {
		List<GridData<T>> l = dataProvider.getRange(key);
		gridLML.safeReplaceInnerList(l);
		modelState = GridDataModelMan.ModelStates.PARTIAL_STATE;
	}
	
	/** Очистить модель. */
	public void clearModel() {
		gridLML.clearSelection();
		gridLML.clear();
		modelState = GridDataModelMan.ModelStates.BLANK_STATE;
	}
	
	/** Провайдер списка строк лист-модели. */
	public GridDataProvider<T> getDataProvider() { return dataProvider; }
	
 	/** Вернуть дата-модель грида, созданную из списка, возвращаемого провайдером ({@link GridDataProvider#getAll()}). */
 	protected ListModelListExt<GridData<T>> getGridLML() { return gridLML; }
 	
 	/** Назначить лист-модель менеджера указанному гриду. Пытаемся скрыть модель, т.к. отслеживаем статус; однако, грид позволяет модель извлечь. */
 	public void setModelFor(Grid mesh) { mesh.setModel(gridLML); }
 	/** Назначить лист-модель менеджера указанному листбоксу. */
 	public void setModelFor(Listbox mesh) { mesh.setModel(gridLML); }
 	
 	/** Возвращает (постоянный) размер списка от провайдера, который в основе лист-модели. */
 	public int getTotalRowCount() { return dataProvider.getTotalRowCount(); }
 	
 	/** Возвращает текущий размер списка дата-модели (строк под фильтром). */
 	public int getCurRowCount() {return gridLML.size();}
 	
 	/** Класс бина (доменного класса), определяется реализацией дата-провайдера. */
	public Class<T> getBeanClass() { return beanClass; }
	
	/** Получить RW-замок дата-модели. */
	public StampedLock getModelRWLock() { return modelRWLock; }
	
	/** Проверка на любой тип блокировки. */
	public boolean isModelLocked() { return modelRWLock.isReadLocked() || modelRWLock.isWriteLocked(); }
	
	
 	/** Установить значение флага выбора ("крыж" - поле GridData.sel) для строки модели с заданным номером.
 	 * Вызов должен быть совершён в условиях эксклюзивной блокировки модели.
 	 * @param isChecked Новое значение флага (true/false).
 	 * @param uid Значение уникального идентификатора GridData.uid объекта-обёртки (ищем по нему строку).
 	 * @return Индекс элемента в дата-модели (м.б. < 0).
 	 */
//	 * @param irn Индекс элемента в дата-модели грида.
// вызывается методом контроллера onCheckSel(), который выполняется защищённо (через диспетчер)
 	public int selectRow(boolean isChecked, /*int irn*//*long uid*/GridData<T> curGridData) {
 		long uid = curGridData.getUid();
// ---FIXME: оптимизировать поиск (сортировка модели меняется) ? искать в отсортированном полном списке по ПК (если ПК установлен и полный список загружен ИЛИ по uid при отсутствии ПК) !!!
 		/*int irn = GridData.searchByUid(gridLML.getInnerList(), uid);
		if (irn < 0) return irn; // за время ожидания многое могло измениться !
 		GridData<T> curGridData = gridLML.get(irn);*/
 		curGridData.setSel(isChecked);
 		//gridLML.notifyChange(curGridData);
// в любом случае необходима позиция в текущей лист-модели
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
 	
 	
 	/** Для всех строк модели отразить изменение поля GridData.sel (пометили/разметили все строки).
 	 * Вызов должен быть совершён в условиях эксклюзивной блокировки модели.
 	 * @param isChecked Новое значение флага (true/false).
 	 */
// вызывается методом контроллера onCheckSelectAllCHB(), который выполняется защищённо (через диспетчер)
 	public void selectAllRows(boolean isChecked) {
// HOWTO: чтобы здесь параметризовать, пришлось параметризованную лист-модель вынести в переменную (dataGrid.getModel() не параметризуется)
		gridLML.forEach(
			(GridData<T> s) -> { s.setSel(isChecked);
						    } 
		);
		gridLML.notifyChangeAll();
		logger.debug("selectAllRows (after actions). isChecked = {}", isChecked);
// HOWTO: !!! SubjsPageComposer.onChange() вызывается для каждой строки. Отложить оповещение и одним разом (deferred) ? !!!
// ? GridDataLoader.doListDataChange(ListDataEvent event) == GridDataLoader.syncModel(-1, -1) ? Grid.getDataLoader() - access-modifiers: empty (package private)
 	} // public void selectAllRows(boolean isChecked)
 	
} // public class GridDataModelMan<T extends Object & Serializable & Comparable<? super T>> implements Serializable