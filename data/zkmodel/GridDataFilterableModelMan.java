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


/** Связующее звено между контроллером грида (composer)
 * ,дата-провайдером модели (полным списком - "эталонной моделью"), содержащим также инфо о типе данных (бине) и текущем Первичном Ключе (PK)
 * ,самой моделью грида, создаваемой менеджером и возвращаемой контроллеру
 * ,композитным фильтром (набором контролов).
 * Thread-safe. Использует RW-замок StampedLock дата-модели в т.ч. для защиты полей оболочки бина (GridData) на уровне
 *  всего списка. Конкуренции между UI-потоками нет, они выполняются синхронно.
 * К {@link GridDataModelMan} добавлен композитный фильтр.
 * @param <T> Тип бина (доменного класса), который оборачивается классом GridData<T>, представляющим собой строку дата-модели грида.
 */
public class GridDataFilterableModelMan<T extends Object & Serializable & Comparable<? super T>> extends GridDataModelMan<T> {
	private static final long serialVersionUID = 3039792128779475493L;
	
	private static final Logger logger = LoggerFactory.getLogger(GridDataFilterableModelMan.class);
	private static final Marker concurMarker = MarkerFactory.getMarker("CONCUR");
	
	private volatile int curGridModelHashCode; // HashCode фильтрованного списка субъектов (зависит от состояния фильтра); если оно меняется, то нужно перечитать содержание комбобоксов
 	private volatile String filterCompositeValue = ""; // содержание фильтра одной строкой (состояние); сохраняется при каждом изменении (применении) фильтра (applyFilter)
 	private final GridDataFilter dataFilter; // создаём и заполняем на стороне вызывающего контроллера
    
// 	private GridDataFilterableModelMan() { // FIXME: противоречит соглашениям JavaBeans !!!
// 	}
 	/** Создатся лист-модель на базе списка, возвращаемого провайдером ({@link GridDataProvider#getAll()}). Создаётся копия полного списка.
 	 * Модель далее можно получить методом {@link #getGridLML()}.
 	 * Сохраняются начальные значения хэш-кода модели (используется комбиками), состояние композитного фильтра.
 	 * @param beanClass Класс бина (доменного класса), который оборачивается классом GridData<T>, представляющим собой строку лист-модели.
 	 * @param dataProvider Провайдер данных для дата-модели грида.
 	 * @param dataFilter Инициализированный массивом компонентов композитный дата-фильтр.
 	 */
 	public GridDataFilterableModelMan(Class<T> beanClass, GridDataProvider<T> dataProvider, GridDataFilter dataFilter) {
// ?? убрать beanClass ??; dataFilter опционален ??
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
 	
 	
 	/** Композитный фильтр, применяемым к строкам модели. */
 	public GridDataFilter getDataFilter() { return dataFilter; }


	/** Формирование дата-модели комбобокса как уникального набора значений соответствующей колонки грида.
 	 * Комбик может не входить в фильтр (dataFilter), который формирует множество строк модели грида из эталонного
 	 *  множества провайдера. Тогда формируем модель комбика над фактической моделью грида.
 	 * Если кобмик участвует в фильтре, разрешаем выбор всех элементов, которые <b>distinct на множестве строк БЕЗ УЧЁТА
 	 * фильтра по соответствующему полю</b> (через битовую маску и набор битовых флагов GridData.filterFlags,
 	 *  показывающих прохождение фильтра каждым полем объекта).
 	 * <p><b>RULE:</b> ИД комбика = имя_поля_доменного_объекта (GridData.bean) + "Combo"; так происходит привязка котрола к полю
 	 * доменного объекта (модель комбика наполняется уникальными значениями из соответствующей колонки грида, с учётом битовой маски).
 	 * Предполагается, что колонка строкового типа.
 	 * <p>Атрибут комбика <b>filterHash</b> хранит хеш датамодели грида на момент последнего формирования модели комбика;
 	 *  модель комбика нужно менять только если изменился набор строк грида.
 	 * Первыми в модели идут специальные значения: {@literal <all>, <null>, <notnull>}.
 	 * Блокирует дата-модель неэксклюзивно, т.к. читает эталонный список строк и хеш модели.
 	 * @param combo Комбобокс, для которого (пере)создаём модель.
 	 */
// FIXME: (не здесь - onOpen, а при остальных событиях) защититься от ручного ввода невалидного значения (автокомплит и валидация на выходе) setConstraint ?
 	public void setComboModel(Combobox combo /*, boolean respectFilter - учитывать фильтр; пока применяется только с фильтром */) {
// TODO: перенести на клиента ?
 		String comboId = combo.getId();
// TODO:HOWTO: ? как параметризовать модель ?
//    	ListModelList<String> comboModel = (ListModelList<String>)combo.getModel(); // can't convert ListModel<Object> to ListModelList<String>
    	@SuppressWarnings({ "unchecked", "rawtypes" })
		ListModelList<String> comboModel = (ListModelList)combo.getModel();
    	String oldValue = combo.getValue();
    	int curFilterHashAttr = combo.getAttribute("filterHash") == null ? 0 : (Integer)combo.getAttribute("filterHash");
    	logger.trace("setComboModel.entry.  comboId: {}, comboModel: {}, oldValue = {}, curGridModelHashCode = {}, curFilterHashAttr (prev hash) = '{}'", comboId, comboModel, oldValue, curGridModelHashCode, curFilterHashAttr);
    	
     	if ( comboModel == null // первоначальное заполнение (combo.getItemCount() == 0)
    	  || curFilterHashAttr != curGridModelHashCode // не первое открытие И сменился фильтр (точнее, набор строк) по сравнению с предыдущим формированием комбо-модели (curGridModelHashCode пересчитывается в applyFilter()) 
    	   ) {
			
// RULE: ИД комбика = имя_поля_доменного_объекта (GridData.bean) + "Combo"
			String getterName = "get" + StringUtils.capitalize(StringUtils.removeEnd(comboId, "Combo"));// comboId.substring(0, 1).toUpperCase() + comboId.substring(1, comboId.length()-5);
			Method getterMethod;
			try {
				getterMethod = beanClass.getMethod(getterName);
			} catch (NoSuchMethodException e) {
				logger.error("setComboModel. NoSuchMethodException on invoke '"+getterName+"' for comboId = '"+comboId+"'. Naming Rule: getterName = 'get'+StringUtils.capitalize(StringUtils.removeEnd(comboId, 'Combo')", e);
				throw new InternalError("setComboModel. NoSuchMethodException on invoke '"+getterName+"' for comboId = '"+comboId+"'. Naming Rule: getterName = 'get'+StringUtils.capitalize(StringUtils.removeEnd(comboId, 'Combo')", e);
			}		
// запомнить выделение (не восстанавливаем - не треба, пока просто для сравнения)
			Set<String> comboSelection = (comboModel==null ? null : ((Selectable<String>)comboModel).getSelection());
			String comboSelectionNext = "";
	    	if (comboSelection != null && !comboSelection.isEmpty()) {
	    		comboSelectionNext = comboSelection.iterator().next(); // пока множественного выбора нет
    		}
	    	int dataFilterInd = dataFilter.getComponentIndex(combo);
	    	logger.debug("setComboModel. BEFORE_(re)set_model...  comboId: '{}', value before='{}', text before='{}', selection='{}', model size before: {}, curGridModelHashCode: {}, curFilterHashAttr (prev hash): {}, dataFilterInd: {}", comboId, oldValue, combo.getText(), comboSelectionNext, (comboModel==null ? 0 : comboModel.getSize()), curGridModelHashCode, curFilterHashAttr, dataFilterInd);
	    	BitField bitOrMask;
	    	if ( dataFilterInd < 0) {
	    		bitOrMask = new BitField(0); // если контрол не участвует в фильтре, то используем текущую модель
	    	} else {
	    		bitOrMask = new BitField(1 << dataFilterInd); // чтобы игнорировать фильтр по данному полю (как бы все строки его проходили)
	    	}
//    		BitField bitAndMask = new BitField(~(1 << dataFilter.getComponentIndex(combo)));
    		logger.trace("setComboModel. comboId: {}, getterName = {}, bitOrMask = {} (index={}) {}", comboId, getterName, Integer.toBinaryString(bitOrMask.getRawValue(0xFFFFFFFF)), dataFilterInd, bitOrMask.getRawValue(0xFFFFFFFF));
    		if (comboModel == null) {
// TODO: использовать sorted set ?
    			comboModel = new ListModelList<String>(30);
    			combo.setModel(comboModel);
    		} else {
    			comboModel.clear();
    		}
    		
// TODO: ограничение на размер списка (настройка in properties !); а как дальше ? !!!    		
    		int comboModelMaxSize = Integer.valueOf(Labels.getLabel("comboModelMaxSize", "20")).intValue();

    		logger.trace(concurMarker, "setComboModel. comboId = {}  ReadLock acquiring...", comboId);
    		long stamp = modelRWLock.readLock();
    		logger.trace(concurMarker, "setComboModel. comboId = {}  ReadLock successfully acquired.  stamp = {}", comboId, stamp);
    		boolean exceedComboModelMaxSize = false;
    		try {
    			for(ListIterator<GridData<T>> gmi = entireGridDataList.listIterator(); gmi.hasNext();) {
    				GridData<T> curSubj = gmi.next();
if (logger.isTraceEnabled() && curSubj.getBean() instanceof basos.xe.data.entity.SubjSumm && ((basos.xe.data.entity.SubjSumm)curSubj.getBean()).getSubj_id() == 11643) logger.trace("comboId = {}, filterFlags = '{}', apply orMask: '{}'", comboId, Integer.toBinaryString(curSubj.getFilterFlags()), Integer.toBinaryString(bitOrMask.set(curSubj.getFilterFlags())));
//    				if (curSubj.isInFilter()) { // не используется !
    				if (bitOrMask.set(curSubj.getFilterFlags()) == 0xFFFFFFFF) { // текущая строка проходит по всем фильтрам без учёта текущего -> включаем в модель
    					try {
    						String curVal = (String)getterMethod.invoke(curSubj.getBean()); // значение поля, связанного с комбиком по названию = атрибут доменного объекта
    						if ( !StringUtils.isEmpty(curVal) && !comboModel.contains(curVal) ) {
    							if ( comboModel.size() < comboModelMaxSize ) {
    								comboModel.add(curVal);
    							} else {
    								exceedComboModelMaxSize = true;
    								break;
// FIXME: что с остальными записями ?
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
        		combo.setAttribute("filterHash", new Integer(curGridModelHashCode)); // запоминаем состояние фильтра (хеш лист-модели грида) на момент построения списка элементов комбобокса
    		} finally {
    			modelRWLock.unlock(stamp);
    			logger.trace(concurMarker, "setComboModel. comboId = {}  ReadLock released; stamp = {}", comboId, stamp);
    		}
    		comboModel.sort(String.CASE_INSENSITIVE_ORDER);
// обработка специальных значений (null, notnull, all)
    		comboModel.add(0, "<all>"); comboModel.add(1, "<null>"); comboModel.add(2, "<notnull>"); // в начало списка !
//	    		combo.setMultiline(true); // error
//	    		comboModel.setMultiple(true); // зачем ?
// TODO: (HI) !!! для автокомплита (по настройке, с autodrop) (стр. 429): combo.setModel(ListModels.toListSubModel(new ListModelList(getAllItems(comboModel), компаратор_contains, 8))); !!!
//    		combo.setModel(comboModel);
    		if (comboModel.indexOf(oldValue) >= 0) {
    			combo.setValue(oldValue); // восстанавливаем значение после переформирования модели
    		} else {
    			combo.setValue(null);
    		}
    		if (comboModel.isSelectionEmpty()) {
    			comboModel.addToSelection(combo.getValue());
    		}
    		comboSelection = (Set<String>)comboModel.getSelection();
    		comboSelectionNext = "";
        	if ( comboSelection != null && !comboSelection.isEmpty() ) {
        		comboSelectionNext = comboSelection.iterator().next(); // пока множественного выбора нет
        	}
        	logger.debug("setComboModel. AFTER_(re)set_model...  comboId = '{}', value after = '{}', text after = '{}', selection = '{}', new model size = {}, newFilterHashAttr = {}", comboId, combo.getValue(), combo.getText(), comboSelectionNext, comboModel.getSize(), combo.getAttribute("filterHash"));
    	} // if изменились условия => требуется инициация/пересмотр модели
// даже при неизменной модели очищаем невалидное значение
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

    public int applyFilter(boolean force, Long stamp) { // --при изменении группы крыжиков
    	return applyFilter(this.dataFilter, force, Optional.empty(), stamp);
    }
    
    public int applyFilter(GridDataFilter _dataFilter, Long stamp) { // --при любом изменении любого фильтр-контрола
    	return applyFilter(_dataFilter, false, Optional.empty(), stamp);
    }
*/
 	/** Вызов {@link #applyFilter(GridDataFilter, boolean, Optional, Long)} с собственным фильром. */
    public int applyFilter(boolean force, Optional<Integer> rn, Long stamp) { // всегда (из simpleApplyLogic) --при снятии одного крыжика
    	return applyFilter(this.dataFilter, force, rn, stamp);
    }

    
    /** Применить изменённый композитный фильтр к модели.
     * Проверяет каждую строку полного (эталонного) списка allSubj на соответствие фильтру GridDataFilter
     *  и перестраивает дата-модель gridLML.
     * Чтобы действо началось, необходимо, чтобы штамп (хэш) фильтра (dataFilter.toString()) изменился по сравнению
     *  с ранее сохранённым в поле filterCompositeValue ИЛИ входной параметр force == true.
     * Отдельная ветка для пустого фильтра (без проверки данных).
     * Модель строится каждый раз с чистого листа (кроме как с параметром rn).
     * По обновлённой модели считается хэш и сохраняется в поле curGridModelHashCode.
     * Вызов должен быть совершён в условиях эксклюзивной блокировки модели.
     * @param _dataFilter Композитный фильтр, который накладываем на данные.
     * @param force True для обновления модели при неизменном фильтре, т.е. когда меняется содержимое строки (сейчас это только крыж GridData.sel).
     * @param rn Номер изменённой строки (необязательный). Если непустой, то сравнивается против фильтра только строка данных с этим индексом.
     * @param stamp Штамп эксклюзивной блокировки, в конце разблокирует им замок, если не 0L.
     * @return Возвращает кол-во строк в модели или -1 при неизменности (или если переколбас случился, но хэш, т.е. набор строк, не поменялся).
     */
	public int applyFilter(GridDataFilter _dataFilter, boolean force, Optional<Integer> rn, Long stamp) { // можно возвращать кол-во строк (-1 если не изменилось)
	  try {	
//		this.dataFilter = _dataFilter; // dataFilter - общий с SubjsPageComposer, можно ссылку не перечитывать (принята в конструкторе и не меняется)

		String tmpComposite = dataFilter.toString();
		boolean isChanged = !tmpComposite.equals(filterCompositeValue);
		logger.trace("applyFilter_begin. force = {}, rn = {}, stamp = {}, isChanged = {}", force, rn, stamp, isChanged);
		
		if (!force && !isChanged) return -1; // фильтр не изменился
		
		filterCompositeValue = tmpComposite; // новое состояние изменённого фильтра
				
		//long stamp = 0L; // for StampedLock
		
		// пустой фильтр не сочетается с прочими входными параметрами
		if ( dataFilter.isEmpty() && isChanged /*игнорируем force !*/ ) { // при пустом фильтре использовать allSubj, проставив везде setInFilter(true)
/*
			logger.trace(concurMarker, "gridDM.applyFilter before sync block (1)");
			boolean locked = true; // означает чужую блокировку
			while (locked) { // FIXME: или выход по таймауту
				try {
					locked = modelRWLock.isReadLocked() || modelRWLock.isWriteLocked(); //tryLock(10, TimeUnit.MILLISECONDS);
					if (locked) { // пока ждём разблокировки, выполняем расшаренную UI-задачу
//						long s = System.nanoTime(); while( (System.nanoTime() - s) / 1000000000 < 2) Thread.yield();
						int pr = doSharedLogicOnce();
						if (pr != -1) {
							logger.debug(concurMarker, "gridDM.applyFilter sharedLogic applied before sync block (1) progress = {}", pr);
							//TimeUnit.MILLISECONDS.sleep(2000);
						}
						TimeUnit.MILLISECONDS.sleep(50); // даём шанс Executions.activate(pmDesktop, 100) во время long operation
					}
				} catch (InterruptedException e) {
					logger.info(concurMarker, "Exception in sync block (1) :", e);
					//Thread.currentThread().interrupt();
				}
			} // на выходе замок модели должен быть свободен
			stamp = modelRWLock.writeLock();
*/			
			try {
				logger.trace(concurMarker, "gridDM.applyFilter inside sync block (1)");
 				entireGridDataList.forEach( s -> {
 					s.setInFilter(true);
 					s.setFilterFlags(0xFFFFFFFF);
 				}); // всем взвести признак inFilter, всем восстановить битовую маску (все ячейки удовлетворяют каждому компоненту фильтра)
				curGridModelHashCode = entireGridDataList.hashCode(); // на подмножестве списка вычислять так: https://docs.oracle.com/javase/8/docs/api/java/util/List.html#hashCode--
				logger.debug("applyFilter (ПУСТОЙ ФИЛЬТР). filterCompositeValue='{}', new hash= {}, allSubj.size={}", filterCompositeValue, curGridModelHashCode, entireGridDataList.size());
//	    		dataGrid.setModel(new ListModelList<SubjSumm>(allSubj, true)); // setModel без пейджинга ужасно тормозит на кол-ве строк от тысячи
// TODO: сравнить с replaceInnerList (entireGridDataList обернуть !)
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
			
			dataFilter.disableComps(false, true); // сделать доступными все фильтр-контролы (дизеблятся при фильтрации по ПервичномуКлючу, разрешаются при очитске ПК-контрола)
	    	return gridLML.getSize();
		} // пустой фильтр

/*		
		logger.trace(concurMarker, "gridDM.applyFilter before sync block (2)");
		Object stuff = new Object();
	  synchronized (stuff) {
		boolean locked = false; // означает нашу блокировку
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
//					TimeUnit.MILLISECONDS.sleep(500); // даём шанс Executions.activate(pmDesktop, 100) во время long operation
				}
			} catch (InterruptedException e) {
				logger.info(concurMarker, "Exception in sync block (2) :", e);
				Thread.currentThread().interrupt();
			}
		}
	  } // synchronized (stuff)
*/	  
	try { // здесь уже держим modelRWLock
		logger.trace(concurMarker, "gridDM.applyFilter inside sync block (2)");

		dataFilter.prepareToApply();
		
		if ( rn.isPresent() ) { // удаляем одну открыженную строку при фильтре по этому столбцу; другие строки проверять излишне
			int irn = rn.get();
			assert(irn >= 0 && irn < gridLML.size());
			GridData<T> curGridData = gridLML.get(irn);
			if ( dataFilter.evalGridData(curGridData) ) { // в любом случае нужно расставить битовые флаги
				curGridData.setInFilter(true);
				//assert(false); // такого не должно быть, но может при откладывании задачи (вызов с номером строки только для выкрыживания)
			} else {
				curGridData.setInFilter(false);
				gridLML.remove(irn);
				if (curGridData.getBean() instanceof basos.xe.data.entity.SubjSumm) logger.debug("applyFilter[rn branch] (after actions). subj_id = {}, curGridData.isSel = {}, curGridData.isInFilter = {}, curGridData.getUid = {}, irn = {}, gridLML.indexOf(curGridData) = {}, ((SubjSumm)gridLML.get(irn={}).getBean()).getSubj_id() = {}", ((basos.xe.data.entity.SubjSumm)curGridData.getBean()).getSubj_id(), curGridData.isSel(), curGridData.isInFilter(), curGridData.getUid(), irn, gridLML.indexOf(curGridData), irn, ( irn >= gridLML.size() ? "last" : ((basos.xe.data.entity.SubjSumm)gridLML.get(irn).getBean()).getSubj_id() ));
			}
		} // передан номер изменённой строки
		else { // номер строки не подан на вход - проверяем все против фильтра

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
			
		} // номер строки не подан на вход - проверяем все строки allSubj (эталона) против фильтра
		
	} finally {
		//modelRWLock.unlock(stamp);
	} // sync block (2)
		logger.trace(concurMarker, "gridDM.applyFilter outside sync block (2)");

		int tmpSubjListHashCode = gridLML.getInnerList().hashCode(); // на подмножестве списка вычислять так: https://docs.oracle.com/javase/8/docs/api/java/util/List.html#hashCode--
		logger.debug("applyFilter. filterCompositeValue='{}', prev hash= {}, new hash= {}, filteredSubj.size={}", filterCompositeValue, curGridModelHashCode, tmpSubjListHashCode, gridLML.size());

/*
		int pr = doSharedLogicOnce();
		if (pr != -1) {
			logger.debug(concurMarker, "gridDM.applyFilter sharedLogic applied after sync block (2) progress = {}", pr);
		}
*/		
		
		if (curGridModelHashCode != tmpSubjListHashCode) { // даже при изменившемся условии фильтра хеш лист-модели грида (набор строк) может не измениться (оптимизировать обновление модели !)
			curGridModelHashCode = tmpSubjListHashCode;
/*
setModel без пейджинга ужасно тормозит на кол-ве строк от тысячи (не помню, чтобы так было первоначально)
 решил очисткой и копированием строк из "эталонного" списка (можно отложить рендеринг до окончания всех операций с данными ?)
*/
//		dataGrid.setModel(gridLML = new ListModelList<SubjSumm>(filteredSubj, true/*live*/));
			return gridLML.getSize();
		}
		
		return -1; // набор строк не поменялся
		
	  } finally { // освобождение полученного на входе замка (0L если разблокируется вызывающим после)
		  if (stamp != 0L) {
			  modelRWLock.unlock(stamp);
			  logger.trace(concurMarker, "gridDM.applyFilter WriteLock released at end; stamp = {}", stamp);
	  	  }
	  }
	  
	} // public int applyFilter(GridDataFilter _dataFilter, boolean force, Optional<Integer> rn, Long stamp)
	
} // public class GridDataFilterableModelMan<> implements Serializable