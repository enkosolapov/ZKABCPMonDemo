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

/** ��������� ������� � ������� ���������� ������������. */
public class ExtPagingUtil {

	private static final Logger logger = LoggerFactory.getLogger(ExtPagingUtil.class);
	
    /** ���./����. ���������������� ������ ��� �����/��������� � ������� ���������� ���������� ������������.
     * RULE: Paging �.�. � sizeable-���������� ������ HtmlBasedComponent (����., div).
     * ������ �������� �������� �� ��������� paginal.pageSize.
     * �������� �������� � ����������� �����. ���������� ���������� �������� ��� ��������������.
     * @param gridPaging ������� (�����������) ���������� ����������. 
     * @param host Grid / Listbox, � �������� ����������� ���������� Paging.
     * @param tbb ������� ���������� ������� (nullable). ��� �������� �� ��������: pagingToolBB.rollIcon � ������ ���������, pagingToolBB.pageIcon � ���������� ������.
     */
// ? �������������� (div > paging) ? ����� Paging � Grid / Listbox (�� �.�. ��������� ������������ ? ��� �������� ���������������) !
	public static void changePaging(Paging gridPaging, MeshElement host, Toolbarbutton tbb) { // ������������ ����� �������������� � ���������� �����
// see Concepts_and_Tricks.pdf (? Also, make sure you do NOT call box.setPaginal(paging) if you use this technique. ?)
    	HtmlBasedComponent pagingHolder = (HtmlBasedComponent)gridPaging.getParent(); // RULE: Paging �.�. � sizeable-���������� ������ HtmlBasedComponent (����., div)
    	int savedPage = 0; // ���������� (� �������� ����������) �������� �������� ��� �������������� ��� ������������ � ����� �������������
    	try {
//			Paging p = host.getPaginal(); // � ZUL ���� �� ������ � ������� ������������ Paging (����� �����)
	    	if (host.getMold() == "default") { // switch from scroll to paging
// !!! �� ���������� Paging �� ����� ������ ��������� ������ � org.zkoss.zul.Paging.setActivePage (������� ���������� ���-�� ������� ?) !!!
	    		logger.trace("Before paging on.");
// FIXME: ������ � ��������� ��� ���� !!
				pagingHolder.setHeight("38px");
				gridPaging.setPageSize(Integer.valueOf(Labels.getLabel("paginal.pageSize", "50")).intValue());
				if (host instanceof Grid) gridPaging.setTotalSize(((Grid)host).getModel().getSize()); else gridPaging.setTotalSize(((Listbox)host).getModel().getSize());// subjSummModel.getInFilterRowCount()
// ���������� � ��������������� �������� (���� �� ��������� ����������� ��������� ����� ������� �������) � �������� ��������
				savedPage = gridPaging.getAttribute("savedPage") != null ? ((Integer)gridPaging.getAttribute("savedPage")).intValue() : 0;
				if (savedPage > gridPaging.getTotalSize()/gridPaging.getPageSize()) {
					savedPage = 0;
				}
				gridPaging.setActivePage(savedPage);
				if (host instanceof Grid) {
					((Grid)host).setPaginal(gridPaging);
				} else if (host instanceof Listbox) {
					((Listbox)host).setPaginal(gridPaging);
				} // _pgi = pgi (���� �� ��� ��); if (inPagingMold())..._pgi.setTotalSize, addPagingListener(_pgi), smartUpdate("paginal", _pgi) (����� ������ ������)
				host.setMold("paging"); // if (_pgi != null) addPagingListener(_pgi); else newInternalPaging()
				gridPaging.setVisible(true);
//				host.setPagingPosition("both");
				Clients.resize(host); // HOWTO: ��� ������������ ������ ������������� ������ pagingHolder ��������, ��� �������� � ������ � ������ hlayout (������� ��������� ������� footer � ��� pmHolder) ��� ��� vflex="min"; ������� Clients.resize(subjSummGrid)
				if (tbb != null) {
					tbb.setImage(Labels.getLabel("pagingToolBB.rollIcon", "/img/Letter-Open-icon(32).png"));
	    			tbb.setTooltiptext("����� ���������");
				}
//	    		p = subjSummGrid.getPagingChild(); // ���������� ���������� �������
//    		p.setMold("os"); // default/os
	    		// getActivePage(), getPageCount(), getPageSize() ���������� � AbstractListModel; �� ������������� �������� �� �����
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
				}// !!! ����� �� ���������� ��� ��������� ������ ������ (����� 50) !!!
				Clients.resize(host); // HOWTO: ��� ������������ ������ ������������� ������ pagingHolder ��������, ��� �������� � ������ � ������ hlayout (������� ��������� ������� footer � ��� pmHolder) ��� ��� vflex="min"; ������� Clients.resize(subjSummGrid)
				if (tbb != null) {
					tbb.setImage(Labels.getLabel("pagingToolBB.pageIcon", "/img/Books-2-icon(32).png"));
	    			tbb.setTooltiptext("���������� �����");
				}
	    	}
    	} catch (Exception e) {
    		logger.error("Unexpected Exception on changePaging(): ", e);
    	}
    } // public void changePaging(Paging gridPaging, MeshElement host, Toolbarbutton tbb)

} // public class ExtPagingUtil