package basos.zkui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.HtmlBasedComponent;
import org.zkoss.zk.ui.util.Clients;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Listbox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.impl.MeshElement;

/** Временное решение с внешним страничным контроллером. */
public class ExtPagingUtil {

	private static final Logger logger = LoggerFactory.getLogger(ExtPagingUtil.class);
	
    /** Вкл./выкл. многостраничного режима для гриба/листбокса с внешним страничным скрываемым контроллером.
     * RULE: Paging д.б. в sizeable-контейнере класса HtmlBasedComponent (напр., div).
     * Размер страницы читается из настройки paginal.pageSize.
     * Временно вынесено в статический метод. Постоянная реализация задумана как макрокомпонент.
     * @param gridPaging Внешний (независимый) страничный контроллер. 
     * @param host Grid / Listbox, к которому динамически подключаем Paging.
     * @param tbb Элемент управления режимом (nullable). Вид читается из настроек: pagingToolBB.rollIcon в режиме прокрутки, pagingToolBB.pageIcon в страничном режиме.
     */
// ? макрокомпонент (div > paging) ? связь Paging с Grid / Listbox (их м.б. несколько одновременно ? или меняться последовательно) !
	public static void changePaging(Paging gridPaging, MeshElement host, Toolbarbutton tbb) { // переключение между пейджированием и прокруткой грида
// see Concepts_and_Tricks.pdf (? Also, make sure you do NOT call box.setPaginal(paging) if you use this technique. ?)
    	HtmlBasedComponent pagingHolder = (HtmlBasedComponent)gridPaging.getParent(); // RULE: Paging д.б. в sizeable-контейнере класса HtmlBasedComponent (напр., div)
    	int savedPage = 0; // запоминаем (в атрибуте компонента) активную страницу для восстановления при переключении в режим пейджирования
    	try {
//			Paging p = host.getPaginal(); // в ZUL грид не связан с внешним контроллером Paging (иначе глюки)
	    	if (host.getMold() == "default") { // switch from scroll to paging
// !!! со встроенным Paging из конца списка возникает ошибка в org.zkoss.zul.Paging.setActivePage (неверно определяет кол-во страниц ?) !!!
	    		logger.trace("Before paging on.");
// FIXME: высоту в настройки или авто !!
				pagingHolder.setHeight("38px");
				gridPaging.setPageSize(Integer.valueOf(Labels.getLabel("paginal.pageSize", "50")).intValue());
				if (host instanceof Grid) gridPaging.setTotalSize(((Grid)host).getModel().getSize()); else gridPaging.setTotalSize(((Listbox)host).getModel().getSize());// subjSummModel.getInFilterRowCount()
// запоминаем и восстанавливаем страницу (если не превышает максимально доступную после сужения фильтра) в атрибуте контрола
				savedPage = gridPaging.getAttribute("savedPage") != null ? ((Integer)gridPaging.getAttribute("savedPage")).intValue() : 0;
				if (savedPage > gridPaging.getTotalSize()/gridPaging.getPageSize()) {
					savedPage = 0;
				}
				gridPaging.setActivePage(savedPage);
				if (host instanceof Grid) {
					((Grid)host).setPaginal(gridPaging);
				} else if (host instanceof Listbox) {
					((Listbox)host).setPaginal(gridPaging);
				} // _pgi = pgi (если не тот же); if (inPagingMold())..._pgi.setTotalSize, addPagingListener(_pgi), smartUpdate("paginal", _pgi) (иначе больше ничего)
				host.setMold("paging"); // if (_pgi != null) addPagingListener(_pgi); else newInternalPaging()
				gridPaging.setVisible(true);
//				host.setPagingPosition("both");
				Clients.resize(host); // HOWTO: при переключении режима пейджирования высота pagingHolder меняется, что приводит к глюкам с нижним hlayout (который выполняет функцию footer и где pmHolder) при его vflex="min"; лечится Clients.resize(subjSummGrid)
				if (tbb != null) {
					tbb.setImage(Labels.getLabel("pagingToolBB.rollIcon", "/img/Letter-Open-icon(32).png"));
	    			tbb.setTooltiptext("Режим прокрутки");
				}
//	    		p = subjSummGrid.getPagingChild(); // встроенный контроллер страниц
//    		p.setMold("os"); // default/os
	    		// getActivePage(), getPageCount(), getPageSize() определены в AbstractListModel; не рекомендуется вызывать из грида
	    		logger.trace("After paging on. TotalSize = {}, PageCount = {}, PageSize = {}, ActivePage = {}, gridPaging = {}, host = {}, tbb = {}", gridPaging.getTotalSize(), gridPaging.getPageCount(), gridPaging.getPageSize(), gridPaging.getActivePage(), gridPaging, host, tbb);
	    		//frozen columns="1" start="0"
	    	} else { // switch from paging to scroll
	    		logger.trace("Before paging off. TotalSize = {}, PageCount = {}, PageSize = {}, ActivePage = {}, gridPaging = {}, host = {}, tbb = {}", gridPaging.getTotalSize(), gridPaging.getPageCount(), gridPaging.getPageSize(), gridPaging.getActivePage(), gridPaging, host, tbb);
				gridPaging.setVisible(false);
				pagingHolder.setHeight("0px");
				savedPage = gridPaging.getActivePage();
				gridPaging.setAttribute("savedPage", savedPage);
				host.setMold("default"); // if (_pgi != null) removePagingListener(_pgi)
				if (host instanceof Grid) {
					((Grid)host).setPaginal(null);
				} else if (host instanceof Listbox) {
					((Listbox)host).setPaginal(null);
				}// !!! иначе не показывает при прокрутке нижние строки (после 50) !!!
				Clients.resize(host); // HOWTO: при переключении режима пейджирования высота pagingHolder меняется, что приводит к глюкам с нижним hlayout (который выполняет функцию footer и где pmHolder) при его vflex="min"; лечится Clients.resize(subjSummGrid)
				if (tbb != null) {
					tbb.setImage(Labels.getLabel("pagingToolBB.pageIcon", "/img/Books-2-icon(32).png"));
	    			tbb.setTooltiptext("Страничный режим");
				}
	    	}
    	} catch (Exception e) {
    		logger.error("Unexpected Exception on changePaging(): ", e);
    	}
    } // public void changePaging(Paging gridPaging, MeshElement host, Toolbarbutton tbb)

} // public class ExtPagingUtil