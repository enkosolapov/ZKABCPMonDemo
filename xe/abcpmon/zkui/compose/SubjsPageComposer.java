package basos.xe.abcpmon.zkui.compose;

import basos.core.SafeFormatter;
import basos.core.TriFunction;
import basos.data.GridData;
import basos.data.dao.GridDataProviderWPk;
import basos.data.zkmodel.GridDataFilterableModelMan;
import basos.data.zkmodel.GridDataModelMan;
import basos.xe.data.dao.impl.SubjDopService;
import basos.xe.data.entity.DaDataInfo;
import basos.xe.data.entity.DaDataInfo.JsonStates;
import basos.xe.data.entity.LimitHistory;
import basos.xe.data.entity.LimitInfo;
import basos.xe.data.entity.SubjRestHistory;
import basos.xe.data.entity.SubjSumm;
import basos.zkui.ExtPagingUtil;
import basos.zkui.GridDataFilter;
import basos.zkui.UIUtil;
import basos.zkui.compose.FilterableGridDataComposer;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/*import java.io.StringReader;
import javax.json.Json; // ���� ���������� wildfly1010F\modules\system\layers\base\org\glassfish\javax\json\main\javax.json-1.0.3.jar � ZKOrclReportGrid\WebContent\WEB-INF\lib\
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;*/

import org.apache.commons.collections4.EnumerationUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

/*// minimal-json, �� ����� Parser-API
import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;*/

import com.github.wnameless.json.flattener.JsonFlattener; // ��������������� json � ������� ��������� (����� ��������������� ����� �������� �����������), ������� ������ ���������� � ZUL-�����; ���������� minimal-json

//import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;

import org.zkoss.chart.Charts;
import org.zkoss.chart.XAxis;
import org.zkoss.chart.YAxis;
import org.zkoss.chart.model.CategoryModel;
import org.zkoss.chart.model.DefaultCategoryModel;

import org.zkoss.lang.Generics;
//import org.zkoss.util.Cleanups;
import org.zkoss.util.Locales;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.Path;
import org.zkoss.zk.ui.Sessions;
import org.zkoss.zk.ui.WebApps;
import org.zkoss.zk.ui.event.KeyEvent;
import org.zkoss.zk.ui.event.OpenEvent;
import org.zkoss.zk.ui.event.SelectEvent;
import org.zkoss.zk.ui.event.SerializableEventListener;
import org.zkoss.zk.ui.event.SortEvent;
import org.zkoss.zk.ui.event.ClientInfoEvent;
import org.zkoss.zk.ui.event.CreateEvent;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventQueue;
import org.zkoss.zk.ui.event.EventQueues;
import org.zkoss.zk.ui.ext.Scopes;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.event.ForwardEvent;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zk.ui.select.annotation.WireVariable;
//import org.zkoss.zk.ui.util.DesktopCleanup;
//import org.zkoss.zk.ui.util.ExecutionCleanup;
//import org.zkoss.zk.ui.util.SessionCleanup;
//import org.zkoss.zk.ui.util.WebAppCleanup;
import org.zkoss.zkplus.cdi.CDIUtil;
import org.zkoss.zk.ui.select.Selectors;
import org.zkoss.zk.ui.select.annotation.Listen;
import org.zkoss.zk.ui.select.annotation.VariableResolver;
import org.zkoss.zul.Grid;
import org.zkoss.zul.Menuitem;
import org.zkoss.zul.Menupopup;
import org.zkoss.zul.Messagebox;
import org.zkoss.zul.Paging;
import org.zkoss.zul.Popup;
import org.zkoss.zul.Row;
import org.zkoss.zul.Rows;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tabpanel;
import org.zkoss.zul.Textbox;
import org.zkoss.zul.Toolbar;
import org.zkoss.zul.Label;
import org.zkoss.zul.ListModelList;
import org.zkoss.zul.Toolbarbutton;
import org.zkoss.zul.Window;
import org.zkoss.zul.event.PagingEvent;
import org.zkoss.zul.impl.HeaderElement;
import org.zkoss.zul.impl.MeshElement;
import org.zkoss.zul.Caption;
import org.zkoss.zul.Checkbox;
import org.zkoss.zul.Column;
import org.zkoss.zul.Columns;
import org.zkoss.zul.Combobox;
import org.zkoss.zul.Combobutton;
import org.zkoss.zul.Div;


/** ���������� (��������) �������� subjsPage. */
@VariableResolver(org.zkoss.zkplus.cdi.DelegatingVariableResolver.class) // �� ������������� ������ ���������� ����� � �������� ��� ���������� Weld, � ��������� ��� ����������� ZK, �.�. � ���� ���� ������ ��������� ����
public class SubjsPageComposer extends FilterableGridDataComposer<Component> {
	private static final long serialVersionUID = 7659563013365157950L;
// HOWTO: ! ������ �.�. public ���� �� ��� ���������� �� ZUL ��� ������� ���������� Listen (� ���� ������ ������ ��� private ���������� !) !!
	
	private static final Logger logger = LoggerFactory.getLogger(SubjsPageComposer.class);
	
// �������������, ��������� �� �����
    //@Wire private Div pagingHolder;
    @Wire
    private Paging subjSummPaging; // ? �������������� (div > paging) ? ����� Paging � Grid / Listbox !
    
	@Wire
	private Label subjSummLabelSummary; // TODO: (low) ��������� �� ������� (onAfterRender)
	
	@Wire
	private Grid subjSummGrid;
	
	private GridDataFilter subjSummFilter; // ����������� ������ - ������ �����������, ��������������� ��������� � ����������� ����-������ ����� ��� ��������� �� ��������� �� "��������� ����� �����", ��������������� ����-�����������
	
// TODO: ��������� ��������� � ��������� ���������� (doAfterCompose)
/* https://www.zkoss.org/wiki/Small%20Talks/2012/September/Practices%20Of%20Using%20CDI%20In%20ZK
 By now(ZK 6.0.2) ZK's Variable Resolving approach can only work with named beans in CDI.
 If your CDI beans are based on Alternative or Producer you'll have to add an adoption layer(a facade or something)
  to make them accessible with Variable Resolver.
*/
	@WireVariable("subjSummProvider") // !!! org.zkoss.zkplus.cdi.DelegatingVariableResolver ����� �������� ������ ����������� ����, ���������� �������������� ��� ������������; � \WebContent\WEB-INF\beans.xml ����������� �� ������������ ��� �������� ������������ � ����� ������, ������ �������� ������ ���� �������������� ����� � ����������� ������ ��� ��������� ����������
	private GridDataProviderWPk<SubjSumm> subjSummProvider; // ��������� ������ ��� ������ ����� (� �.�. ���� � ������� ��)
    
	private GridDataFilterableModelMan<SubjSumm> subjSummModelManager; // ��������� ����� ����� ������������ �����, ����-����������� ������, ����� ������� �����, ����������� ��������
    
// ������ toolbar ����������� tabbox (��� tabpanels)
 	@Wire
 	private Toolbarbutton clearFilterToolBB;
 	
/* 	@Wire
 	private Charts chartSubjRests;
 	private CategoryModel chartSubjRestsCatModel;*/
 	
 	private EventQueue<Event> interDeskEQ; // ���������� (UI) ������� inter-desktop ����� - ������������� ! // Returns the desktop-level event queue with the specified name in the current desktop, or if no such event queue, create one
    
/*    private Runnable cleanupCmnd = () -> {
   		logger.debug("cleanupCmnd.run(), asyncExecutor = {}", asyncExecutor);
   		if (asyncExecutor != null && !asyncExecutor.isTerminated()) {
   			asyncExecutor.shutdown();
   		}
    };
    private Runnable cleanupMockCmnd = () -> {
   		logger.debug("cleanupMockCmnd.run()");
    };
*/  
    
    
    /** {@inheritDoc} ����� ������������� ���������� ������ ����-����������, ����� ��������� ��� �������, ���
     * ����������� ������, ����-��������; �������� �� ���� ������ ����� � ��������� �; �������������� ����������� � ����.
     */
	@Override
	protected void initAfterCompose(Component comp) throws Exception { // implementation specific part of doAfterCompose (at the and of)
// FIXME: ������ ���������� � ������ ��������� ���������� (HOWTO: ��� (scope ?) ? getWebManager( Executions.getCurrent().getDesktop().getWebApp() or Sessions.getCurrent(false).getWebApp() or WebApps.getCurrent() - �� ���� � �� �� !! ).destroy() ? ) !
		
// TESTME !!
// FIXME: ? ��� ���������� ������������ ������ ��� ������/����������; �� ? ?
/*		GlobalCleaner.clear(WebAppCleanup.class); // !! ���� ���-�� �������� � ������������� ��� ������������� �������� !!
		GlobalCleaner.registerCommand(cleanupMockCmnd, WebAppCleanup.class);
		GlobalCleaner.registerCommand(cleanupCmnd, WebAppCleanup.class); // �� ������� �������� �������� ������
    	GlobalCleaner.clear(SessionCleanup.class);
		GlobalCleaner.registerCommand(cleanupMockCmnd, SessionCleanup.class);
		GlobalCleaner.registerCommand(cleanupCmnd, SessionCleanup.class); // ���������� ������ �� �������� (����� ExecutionCleanup � DesktopCleanup), ������ ������ �� ������ ������
		GlobalCleaner.registerCommand(cleanupMockCmnd, DesktopCleanup.class);
		GlobalCleaner.registerCommand(cleanupCmnd, DesktopCleanup.class); // ���������� ��� ����� �� ��������, ��������, �������� �������(! � �.�. timeout.zul) �� ����, �� ����� ExecutionCleanup; ������ ������ ������ � ����� ������
		//GlobalCleaner.registerCommand(cleanupCmnd, ExecutionCleanup.class);
    	Cleanups.add(new GlobalCleaner());
		Sessions.getCurrent(false).getWebApp().getConfiguration().addListener(GlobalCleaner.class);*/
		
    	
		if (subjSummProvider == null) subjSummProvider = new /*basos.xe.data.dao.impl.GridDataProviderOrclSubjSumm(false)*/basos.xe.data.dao.impl.TestGridDataProviderMockSubjSumm(888);
		
		changePkToolBB.setTooltiptext("����� PK, ������: " + subjSummProvider.getPk().orElse("null(uid)"));
		
		subjSummModelManager = initFilterableMesh(subjSummProvider, subjSummGrid, subjSummPaging, subjSummLabelSummary, subjSummPmHolder);
		subjSummFilter = subjSummModelManager.getDataFilter();
		
/*		initBehavMap();
		
		AbstractComponent[] filterComps = UIUtil.extractFilterControlsFromAuxhead(subjSummGrid);
// FIXME: ��������������� ������ ����� ���� ???
		try {
			subjSummFilter = new GridDataFilter(subjSummProvider, behavMap, filterComps); // lmc_rowidTB, cluidTB, idlimitIB, is_riskCHB, s_usd_balFltr, rest_msfo_usdFltr, dd_probl_proj_beginFltr, selFilterCHB, subj_idIB, subj_nameTB, gr_nameTB, ko_fioTB, curator_fioTB, acd_prodTB, yak_nameCombo, br_nameCombo, cityCombo, kko_otdelCombo, kko_upravlCombo, cat_nameCombo, clibr_nameCombo, b2segmCombo, asv_name_okeqCombo   //new ArrayList<AbstractComponent>(Arrays.asList(selFilterCHB, subj_idIB, subj_nameTB, gr_nameTB, ko_fioTB, curator_fioTB, acd_prodTB, yak_nameCombo, br_nameCombo, cityCombo, kko_otdelCombo, kko_upravlCombo, cat_nameCombo, clibr_nameCombo, b2segmCombo, asv_name_okeqCombo)).toArray(new AbstractComponent[0])
		} catch (Throwable e) {
// WebAppCleanup() -> cleanup() -> ExecutionCleanup()
			WebManager.getWebManager( WebApps.getCurrent()Executions.getCurrent().getDesktop().getWebApp() ).destroy();
			throw e;
		}
		
		subjSummModelManager = new GridDataFilterableModelMan<>(SubjSumm.class, subjSummProvider, subjSummFilter);
		
	    subjSummGrid.setModel(subjSummModelManager.getGridLML()); // ����-������ ����� �� ������, ��������������� ����������� (���������� �������� ����� ������� ������) � ����������� ��������
	    
	    subjSummGrid.setAttribute("GridDataModelMan", subjSummModelManager); // RULE: ��� ��������� ����/�������� � ���������� ��� ����-������ (GridDataModelMan)
	    subjSummGrid.setAttribute("ExtPaging", subjSummPaging); // RULE: ��� ��������� ����/�������� � ������� ���������� ������������
	    subjSummGrid.setAttribute("SummaryLabel", subjSummLabelSummary); // RULE: ��� ��������� ����/�������� � ������, � ������� ������� ����� ��� ��������� ������
	    subjSummGrid.setAttribute("PmHolder", subjSummPmHolder); // RULE: ��� ��������� ����/�������� � ����������� ��� UI-��������� ZKWorkerWithTimerAndPM 
	    
	    subjSummLabelSummary.setValue("����� �����: " + subjSummModelManager.getCurRowCount()); // TODO: (low) ��������� �� ������� (onAfterRender)
*/
	    
	    logger.trace("doAfterCompose. Rows.VisibleItemCount = {}", subjSummGrid.getRows().getVisibleItemCount()); // ���������� ����� ���-�� ����� � ������; �� �������� (��� ������) � doAfterRender
// TESTME: autowiring ! ��. Components.getImplicit()
		if (desktop == null) {
			logger.trace("desktop autowiring did not work.  Scopes.getImplicit = '{}'", Scopes.getImplicit("desktop", null));
			desktop = /*subjSummGrid.getDesktop()*/Executions.getCurrent().getDesktop();
		}
		logger.trace("doAfterCompose. Locales.getCurrent: {}, getAvailableLocales: {}", Locales.getCurrent(), Arrays.deepToString(Locale.getAvailableLocales()));
		logger.trace("doAfterCompose. subjSummGrid.pageSize from \\WebContent\\WEB-INF\\zk-label.properties (def 77): {}", Integer.valueOf(Labels.getLabel("dataGrid.pageSize", "77")).intValue() );
		
// ������������� �������� ������������� ��� ���-�� ����� 5000+ (� ��������� !)
		if (subjSummModelManager.getCurRowCount() > 5000) ExtPagingUtil.changePaging(subjSummPaging, subjSummGrid, pagingToolBB);
		
// ������������ request ������ ����������
//    	rest_msfo_usdCBTpopup.setAuService(new PopupBlockService());
//    	rest_msfo_usdCBT.setAuService(new CombobuttonBlockService());
		
		//int asyncExecutorFixedThreadPoolSize = Integer.valueOf(Labels.getLabel("dataGridComposer.asyncExecutorFixedThreadPoolSize", "3")).intValue();
		//asyncExecutor = Executors.newFixedThreadPool(asyncExecutorFixedThreadPoolSize)/*newSingleThreadExecutor()*/; // FIXME: ��������� (properties) ! Integer.valueOf(Labels.getLabel("workingThreadCnt", "3")).intValue()
		
// TODO: (see DR p.235) handle onClientInfo(ClientInfoEvent evt) registered on root element for retrieve browser info such as getTimeZone(), getScreenWidth(), getScreenHeight()
		logger.debug("SelectorComposer.doAfterCompose. comp: " + comp.getClass().getName() + ", compId: "+comp.getId()
			+ "\n, subjSummProvider: " + subjSummProvider.getClass().getName()
			+ "\n, subjSummProvider.beanClass: " + subjSummProvider.getBeanClass().getName()
			+ "\n, PK: " + subjSummProvider.getPk().orElse("<PK_not_defined>")
			+ "\n, PkComparator: " + (subjSummProvider.getBeanPkComparator().isPresent() ? subjSummProvider.getBeanPkComparator().get().getClass().getName() : "<PK_not_defined>")
			+ "\n, PkClass: " + (subjSummProvider.getPkClass().isPresent() ? subjSummProvider.getPkClass().get().getName() : "<PK_not_defined>")
			+ "\n, InFilterRowCount= " + subjSummModelManager.getCurRowCount()
			+ "\n, logger.className: " + logger.getClass().getName() // ch.qos.logback.classic.Logger
			+ "\n, beanManager.className: " + CDIUtil.getBeanManager().getClass().getName() // org.jboss.weld.bean.builtin.BeanManagerProxy
			+ "\n, AppName: " + WebApps.getCurrent()/*desktop.getWebApp()*/.getAppName() // ZK
			+ "\n, Directory: " + desktop.getWebApp().getDirectory() // null
			+ "\n, Version: " + desktop.getWebApp().getVersion() // Version: 8.0.1.1, Build: 2016020312
			+ "\n, Build: " + desktop.getWebApp().getBuild()
			+ "\n, Edition: " + WebApps.getEdition() // CE
///*��.�����*/			+ "\n, WebApp.Attributes (same as in ServletContext): " + WebApps.getCurrent().getAttributes().entrySet().stream().map((Map.Entry<String,Object> k)->{return "['"+k.getKey()+"':'"+k.getValue()+"']";}).collect(Collectors.joining(", ", "{ ", " }"))
			+ "\n, ServerInfo: " + desktop.getWebApp().getServletContext().getServerInfo() // WildFly 2.2.0.Final - 1.4.0.Final
			+ "\n, ServletContext.ContextPath: " + WebApps.getCurrent().getServletContext().getContextPath() // /ZKOrclReportGrid
			+ "\n, ServletContext.ServletContextName: " + WebApps.getCurrent().getServletContext().getServletContextName() // ZKOrclReportGrid
			+ "\n, ServletContext.VirtualServerName: " + WebApps.getCurrent().getServletContext().getVirtualServerName() // default-host
			//...and more in ServletContext...
			+ "\n, Session.DeviceType: " + Sessions.getCurrent(false).getDeviceType() // ajax
			+ "\n, Session.MaxInactiveInterval(timeout, sec.): " + Sessions.getCurrent(false).getMaxInactiveInterval() // 30
			+ "\n, Session.Attributes: " + Sessions.getCurrent(false).getAttributes().entrySet().stream().map((Map.Entry<String,Object> k)->{return "['"+k.getKey()+"':'"+k.getValue()+"']";}).collect(Collectors.joining(", ", "{ ", " }"))
			+ "\n, HttpSession.Id: " + ((javax.servlet.http.HttpSession)Sessions.getCurrent(false).getNativeSession()).getId()
			+ "\n, HttpSession.AttributeNames: " + EnumerationUtils.toList( ((javax.servlet.http.HttpSession)Sessions.getCurrent(false).getNativeSession()).getAttributeNames() ).stream().collect(Collectors.joining(", ", "{ ", " }"))
			+ "\n, Execution.Browser: " + Executions.getCurrent().getBrowser()
			+ "\n, Execution.RemoteAddr: " + Executions.getCurrent().getRemoteAddr()
			+ "\n, Execution.RemoteHost: " + Executions.getCurrent().getRemoteHost()
			+ "\n, Execution.RemoteUser: " + Executions.getCurrent().getRemoteUser()
			+ "\n, Execution.Attributes: " + Executions.getCurrent().getAttributes().entrySet().stream().map((Map.Entry<String,Object> k)->{return "['"+k.getKey()+"':'"+k.getValue()+"']";}).collect(Collectors.joining(", ", "{ ", " }")) // � �.�. ������ �� Desktop
			+ "\n, Execution.ParameterMap: " + Executions.getCurrent().getParameterMap().entrySet().stream().map((Map.Entry<String,String[]> k)->{return "['"+k.getKey()+"':'<"+k.getValue().toString()+">']";}).collect(Collectors.joining(", ", "{ ", " }")) // empty
			+ "\n, Desktop.Id: " + desktop.getId()
			+ "\n, Page.Id: " + subjSummGrid.getPage().getId() // empty
			+ "\n, Page.RequestPath: " + subjSummGrid.getPage().getRequestPath() // /index.zul
			+ "\n, Page.Attributes: " + subjSummGrid.getPage().getAttributes().entrySet().stream().map((Map.Entry<String,Object> k)->{return "['"+k.getKey()+"':'"+k.getValue()+"']";}).collect(Collectors.joining(", ", "{ ", " }")) // empty
			+ "\n, Page.Roots: " + subjSummGrid.getPage().getRoots().stream().map((Component c) -> {return c.getId();}).collect(Collectors.joining(", ", "{ ", " }")) // { dopstyle, tabbic }
			//...and more in Page...
		);
		
		// print Logback internal state
	    if ( "ch.qos.logback.classic.Logger".equals(logger.getClass().getName()) ) {
	    	LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
	    	StatusPrinter.print(lc);
	    }
	    
	    System.out.println("Where is my STDOUT ?");
	    System.err.println("Where is my STDERR ?");
	    
/* Logging (Slf4j + wildfly/logback)
 +Override logging in WildFly (��� ��������� logback ��� ���������� �� ����� ��������� �����������): http://stackoverflow.com/questions/21846329/override-logging-in-wildfly/26342226#26342226
    META-INF\jboss-deployment-structure.xml ��������� �� ������������ ��������� ����������� � ���������� (����������� ����������: exclude-subsystem prevents a subsystems deployment unit processors running on a deployment which gives basically the same effect as removing the subsystem, but it only affects single deployment)
  Enable logback in JBoss (�� ��������, ��������� ��������� ����������� � Logback): http://blog.anotheria.net/devops/enable-logback-in-jboss/
  WildFly v10 Logging Configuration (������ ���, ���������� ����): https://docs.jboss.org/author/display/WFLY10/Logging+Configuration?_sscc=t
  For web-applications, configuration files (logback-test.xml / logback.xml) can be placed directly under WEB-INF/classes
 (http://stackoverflow.com/questions/20427307/where-to-put-logback-xml-in-tomcat)
 (http://logback.qos.ch/faq.html#configFileLocation)
 (http://stackoverflow.com/questions/8278079/slf4j-and-logback-configuration-in-gwt-eclipse-and-jetty)
  �������� ������������� �������� � ����������:
    http://stackoverflow.com/questions/4165558/best-practices-for-using-markers-in-slf4j-logback
    http://stackoverflow.com/questions/16813032/what-is-markers-in-java-logging-frameworks-and-that-is-a-reason-to-use-them
  How to redirect System.out to a log file using Logback (�� ��������): http://stackoverflow.com/questions/39170008/how-to-redirect-system-out-to-a-log-file-using-logback
*/		
	    interDeskEQ = EventQueues.lookup("interDeskEQ"); // ���������� (UI) ������� inter-desktop ����� - ������������� ! // Returns the desktop-level event queue with the specified name in the current desktop, or if no such event queue, create one
	    
	    //initSubjRestsChartModel();
	    
    } // protected void initAfterCompose(Component comp) throws Exception
    
/*	private void initSubjRestsChartModel() {
		chartSubjRestsCatModel = new DefaultCategoryModel();
		chartSubjRestsCatModel.setValue("84", "2014", new Integer(100));
		chartSubjRestsCatModel.setValue("86", "2014", new Integer(140));
		chartSubjRestsCatModel.setValue("396", "2014", new Integer(80));
		chartSubjRestsCatModel.setValue("397", "2014", new Integer(120));
		chartSubjRestsCatModel.setValue("84", "2015", new Integer(125));
		chartSubjRestsCatModel.setValue("86", "2015", new Integer(130));
		chartSubjRestsCatModel.setValue("396", "2015", new Integer(178));
		chartSubjRestsCatModel.setValue("397", "2015", new Integer(150));
		chartSubjRestsCatModel.setValue("84", "2016", new Integer(96));
		chartSubjRestsCatModel.setValue("86", "2016", new Integer(123));
		chartSubjRestsCatModel.setValue("396", "2016", new Integer(111));
		chartSubjRestsCatModel.setValue("397", "2016", new Integer(210));
		chartSubjRests.setModel(chartSubjRestsCatModel);
	} // private void initSubjRestsChartModel()
*/	
    /*// �� ��������...
    public void processBeans(@javax.enterprise.event.Observes javax.enterprise.inject.spi.ProcessBean<?> event) {
    	javax.enterprise.inject.spi.AnnotatedMethod<?> info;
        if (event instanceof javax.enterprise.inject.spi.ProcessProducerMethod) {
            info = ((javax.enterprise.inject.spi.ProcessProducerMethod<?, ?>) event).getAnnotatedProducerMethod();
            //.getJavaMember()
            logger.trace("processBeans. producer_method_name = {}", info.getJavaMember().getName());
        } else logger.trace("processBeans. other_event: {}", event.getClass().getSimpleName());
    }*/
    
	@Wire Toolbar tools;
	@Listen("onSelect=tab")
	public void onChangeTab(SelectEvent<Tab, ?> sev) {
		Tab prevTab = sev.getUnselectedItems().iterator().next()
		   ,newTab = sev.getReference();
		// ��������� Toolbox'��
		if ( "subjsTab".equals(prevTab.getId()) ) { // �������� ������ ���������
			tools.setVisible(false);
		} else if ( "subjsTab".equals(newTab.getId()) ) { // ��������� �� ������ ���������
			tools.setVisible(true);
		}
		logger.trace("onChangeTab.  target = {}, ref = {}, data = {}, page = {}, UnselectedItems = {}, SelectedObjects = {}", sev.getTarget(), sev.getReference(), sev.getData(), sev.getPage(), sev.getUnselectedItems(), sev.getSelectedObjects());
	}
	
	/** ���������� � ������ � ����� ������� ��������� ���������� �������, � �.�. ��������� �������� � ��������.
	 * ��������� � ��������� ������� ������ "Orientation", "DesktopWidth", "DesktopHeight".
	 */
	@Listen("onClientInfo=:root")
	public void clientInfo(ClientInfoEvent cie) {
		logger.debug("clientInfo.\n DesktopHeight: " + cie.getDesktopHeight()
		+ "\n, DesktopWidth: " + cie.getDesktopWidth()
		+ "\n, DesktopXOffset: " + cie.getDesktopXOffset()
		+ "\n, DesktopYOffset: " + cie.getDesktopYOffset()
		+ "\n, Orientation: " + cie.getOrientation()
		+ "\n, ScreenHeight: " + cie.getScreenHeight()
		+ "\n, ScreenWidth: " + cie.getScreenWidth()
		+ "\n, TimeZone: " + cie.getTimeZone()
		);
		Sessions.getCurrent(false).setAttribute("Orientation", cie.getOrientation()); // The orientation is portrait when the media feature height is greater than or equal to media feature width, otherwise is landscape.
		Sessions.getCurrent(false).setAttribute("DesktopWidth", Integer.valueOf(cie.getDesktopWidth()));
		Sessions.getCurrent(false).setAttribute("DesktopHeight", Integer.valueOf(cie.getDesktopHeight()));
	} // public void clientInfo(ClientInfoEvent cie)
	
	
	/** ���������� �� ����������� ���� ������ ������ �������� ��� ��������� ������� �� ��������. */
	@Listen("onOpen=popup#grNameCellPopup")
	public void popupGroupMember(OpenEvent oev) {
		Popup pop = (Popup)oev.getTarget();
// HOWTO: ! ����� ���������� � ZUL !
		if (oev.isOpen()) { // ��������
			Label ref = (Label)oev.getReference();
			if (ref != null && !StringUtils.isBlank(ref.getValue())) {
				StringBuilder sb = new StringBuilder("������ ������ ������: ");
				sb.append(ref.getValue());
				Object sid = ref.getParent().getAttribute("sid");
				if (sid != null && sid instanceof Integer) {
					int subjId = ((Integer)sid).intValue();
// TODO: ������� ��� ��������� � ��
					List<String> members = SubjDopService.getGroupMembersBySubjId(subjId);
					logger.trace("popupGroupMember.  members.size = {} for subjId = {}", members.size(), subjId);
					sb.append("\n");
					for (String s : members) {
						sb.append("\n").append(s);
					}
				}
// TODO: html ������ label !
				Label cont = new Label(sb.toString()); // ������������� �������
				cont.setPre(true); // preserve WS (incl. \n)
				//cont.setStyle("color:#b22222 !important;"); // B22222 - FireBrick
				//pop.setStyle("color:#b22222 !important;"); // B22222 - FireBrick
				pop.appendChild(cont);
			}
		} else { // ��������
			for ( Component ch : pop.getChildren() ) {
				pop.removeChild(ch);
			}
		}
	} // public void popupGroupMember(OpenEvent oev)
	
	
    @Listen("onCreate=menupopup#changePkMenu")
    public void fillPkMenu(CreateEvent crev) {
    	Menupopup/*Component*/ trg = (Menupopup)crev.getTarget();
    	logger.trace("fillPkMenu. ev_target = '{}'", trg );
    	//if (!(trg instanceof Menupopup)) return;
// <menuitem label="subj_id" forward="onClick=${active_page}changePkToolBB.onChangePk(${self.label})" />
    	Menuitem el;
// FIXME: �������� �� ���������� !!!
    	for ( String kn : subjSummProvider.getPkNames() ) {
    		el = new Menuitem(kn);
    		el.addForward(Events.ON_CLICK, changePkToolBB, "onChangePk", kn);
    		trg.appendChild(el);
    	}
		el = new Menuitem("null(uid)");
		el.addForward(Events.ON_CLICK, changePkToolBB, "onChangePk", "null(uid)");
		trg.appendChild(el);
    } // public void fillPkMenu(CreateEvent crev)
    
    
// ������ toolbar ����������� tabbox (��� tabpanels)
    @Wire
    private Combobutton changePkToolBB;
    
    /** ����� ���������� ����� ����� ����-��������� �� ������� ������������ �������, ����������� ����� ����������� ��������� ��������� ������. */
    @Listen("onChangePk=#changePkToolBB")
    public void changePk(ForwardEvent fev) { // ��� ���� �� ��������� � ���� �������
    	String newPk = (String)fev.getData();
//    	Comparator<SubjSumm> cmp = null;
    	//cmp = pksMap.get(newPk);
    	//cmp = SubjSumm.getPkMap().get(newPk);
//    	cmp = SubjSumm.getPkComparatorByName(newPk);
    	/*switch (newPk) {
    		case "subj_id": cmp = SubjSumm.getCompareBySubjId(); break;
    		case "lmc_rowid": cmp = SubjSumm.getCompareByRowid(); break;
    		case "cluid": cmp = SubjSumm.getCompareByCluid(); break;
    	}*/
    	if (/*cmp*/newPk == null || "null(uid)".equals(newPk)) {
    		subjSummProvider.setPk(null/*, null*/); // "null(uid)"
    		newPk = "null(uid)";
    	} else {
    		subjSummProvider.setPk(newPk/*, cmp*/);
    	}
    	changePkToolBB.setTooltiptext("����� PK, ������: " + newPk);
    	//logger.trace("changePk. ev_data = '{}', newPk(after) = '{}', pksMap.size: {}, pksMap.containsKey(ev_data): {}", fev.getData(), newPk, pksMap.size(), pksMap.containsKey(fev.getData()));
    	logger.trace("changePk. ev_data = '{}', newPk(after) = '{}'", fev.getData(), newPk );
    } // public void changePk(ForwardEvent fev)
    
    
    /** �������� ������� ���������� �� ������� ��� ����������� ������������� �� ������ �����. 
     * @see SubjsPageComposer#safeSort(HeaderElement, boolean, GridDataModelMan) safeSort
	 */
    @Listen("onSort=grid#subjSummGrid > columns > column")
    public void onSortCol(SortEvent sev) {
    	sev.stopPropagation();
    	boolean isAsc = sev.isAscending();
    	HeaderElement hdr = ((HeaderElement)sev.getTarget());
    	Component host = hdr.getParent().getParent();
    	GridDataModelMan<?> gdm = (GridDataModelMan<?>)host.getAttribute("GridDataModelMan");
    	safeSort(hdr, isAsc, gdm);
	} // public void onSortCol(SortEvent sev)
        
    /** ������������ ��������������� ������� �� �������� ����������� ����������� ����� (����� - ��� �����). */
    @Listen("onPaging=paging#subjSummPaging")
    public void onPagingSubjSummPaging(PagingEvent pe) { // ����������� �� ������������ � �����
    	logger.trace("onPagingSubjSummPaging. getActivePage() = {}", pe.getActivePage());
		pe.stopPropagation(); // �����, ����� ������ ��������� ������ � setActivePage � ����������� ����� (�������� -4777)
	}

    @Listen("onPaging=grid#subjSummGrid")
    public void onPagingSubjSummGrid(PagingEvent pe) { // �� ������ ���� �������� !!!
		//_startPageNumber = pe.getActivePage();
    	logger.error("onPagingSubjSummGrid. getActivePage() = {} !!!", pe.getActivePage());
		assert(false) : "onPaging  �� ������ �������� �� ����� !!!";
	}
    
/*    private void onChange(ListDataEvent lde) { // implements ListDataListener
		logger.trace("onChange. ����� �����: {}", subjSummModelManager.getInFilterRowCount());
    	refreshAfterDataChange();
    } // public void onChange(ListDataEvent lde) { // implements ListDataListener
*/
    
    
// ������ toolbar ����������� tabbox (��� tabpanels); paging ������ ������, �� ��� �����
    @Wire
    private Toolbarbutton pagingToolBB;
    
    @Listen("onClick=toolbarbutton#pagingToolBB")
    public void onClickPagingToolBB(Event ev) {
    	ExtPagingUtil.changePaging(subjSummPaging, subjSummGrid, (Toolbarbutton)ev.getTarget());
    }
    
    
    /** �������� � ��������� ���� ������� �������� �� ���������� ��������.
     * �� Java, ZUL �� ������������.
     * @param page �� ����� �������� ������ �����.
     */
	private void showSubjRestHistModal(Integer subjId, Page page) {
// TODO: ��������� (����������; ����� ����) !!!
// TODO: ������� ��� ��������� � �� (������, � ������� ���������)
		List<SubjRestHistory> lst = SubjDopService.getRestHistoryBySubjId(subjId);
		
		int wd = (int)(((Integer) Sessions.getCurrent(false).getAttribute("DesktopWidth")).intValue()*0.8);
		int hi = (int)(((Integer) Sessions.getCurrent(false).getAttribute("DesktopHeight")).intValue()*0.8);
		int hi1 = (int)(((Integer) Sessions.getCurrent(false).getAttribute("DesktopHeight")).intValue()*0.4);
		int hi2 = (int)(((Integer) Sessions.getCurrent(false).getAttribute("DesktopHeight")).intValue()*0.4);

		logger.debug("showSubjRestHistModal.  lst.size = {} for subjId = {}, Width = {}, Height = {}, hi1 = {}, hi2 = {}", lst.size(), subjId, wd, hi, hi1, hi2);
		
		Window restsWin = new Window(null, "normal", true);
		Caption cpt = new Caption("������� �������� �� �������� �� LM "+subjId);
		cpt.setStyle("background:#cceeff;text-align:center;font-size:26px;font-weight:bold;color:#b22222;"); // font-style:italic; // B22222 - FireBrick, cceeff - 
		restsWin.appendChild(cpt);
		//restsWin.setHeight("500px"); restsWin.setWidth("800px");
// TODO:HOWTO: ? ��� �������, ����� �� ���� ������� ������������, �� �� �������� �� �����, � ���������� ��������� (� �����, �� � ����) - ��� paging ?
		restsWin.doHighlighted(); //restsWin.setMode(Window.HIGHLIGHTED);
		//restsWin.setShadow(true);
		restsWin.setSizable(true);
		
		Grid restsGrid = new Grid();
		//restsWin.setHeight("80%"); restsWin.setWidth("80%"); restsGrid.setVflex(true); restsGrid.setHflex("true");
		restsWin.setVflex("min"); restsWin.setHflex("min");
		//restsGrid.setVflex("min");
		//restsGrid.setHflex("min");
		restsGrid.setHeight(hi1+"px"/*"300px"*/);
		restsGrid.setWidth(wd+"px"/*"1400px"*/);
		//restsGrid.setMold("paging");
		//restsGrid.setPagingPosition("top");
		//restsGrid.setPageSize(8); // ��� ������������ ������ ����
		//restsGrid.setAutopaging(true); // ����� ����������: �� ���������, ����� ���������, �� ������� �� �����
		restsGrid.setEmptyMessage("������� �������� �� ������� �� �������� �� LM "+subjId);
		//restsGrid.setSizedByContent(true); // Sets whether sizing grid/listbox/tree column width by its content; it equals set hflex="min" on each column.
		restsGrid.setSpan(true);
		
		Columns clmns = new Columns();
		clmns.setVflex("min");
		clmns.setSizable(true);
		//clmns.setMenupopup("auto"); // �������� ��� ������� �������
		int i = -1;
		for (String h : SubjRestHistory.getHeader()) {
			++i;
			if (i == 0/*"�� LM".equals(h)*/) continue; // ���������� ������� � ID
			Column col = new Column(h);
			col.setAlign("center");
			clmns.appendChild(col);
		}
		clmns.setParent(restsGrid);
		
// TODO: ������ � �������� � Excel !
		Rows rs = new Rows();
		        
		BigDecimal maxBalVal = new BigDecimal(0);
		for (SubjRestHistory rh : lst) {
			Row r = new Row();
			r.setNowrap(true);
			//r.setAlign("right"); // �� ��������, col.setAlign("center") ������������
			r.setStyle("text-align:right !important"); // ������ ��� ��������; HOWTO: ? ��� ����������� �� ������ ������� (��� ���� ��������� �� ������) ?
			//r.appendChild(new Label(rh.getId().toString())); // ���������� ������� � ID
			r.appendChild( new Label( SafeFormatter.asGermanDate(rh.getDdRest()) )); // rh.getDdRest().toString()
			r.appendChild( new Label( SafeFormatter.asMoney(rh.getRestBalUSD()) ) );
			r.appendChild( new Label( SafeFormatter.asMoney(rh.getPastRestBalUSD()) ) );
			r.appendChild( new Label( SafeFormatter.asMoney(rh.getRestUpravlUSD()) ) );
			r.appendChild( new Label( SafeFormatter.asMoney(rh.getSumChargeoffUSD()) ) );
			r.appendChild( new Label( SafeFormatter.asMoney(rh.getSumProvisionUSD()) ) );
			r.appendChild( new Label( rh.getSokBallInt() == null ? "" : rh.getSokBallInt().toPlainString()) );
			r.setParent(rs);
			if ( rh.getRestBalUSD().compareTo(maxBalVal) > 0 ) {
				maxBalVal = rh.getRestBalUSD();
			}
		}
		rs.setParent(restsGrid);
		
		restsGrid.setParent(restsWin);
		
// FIXME: ��. �������� ����� ��������� �� 1839519, 29618 (�������� = ��� ?!)
		
		// ������ ��������
		Charts chrt = new Charts();
		chrt.setType(Charts.LINE);
		chrt.setTitle("");
		chrt.setHeight(hi2/*300*/);
		chrt.setWidth(wd/*1400*/);
		//chrt.setWidth(restsGrid.getWidth().);
		XAxis xa = chrt.getXAxis();
		xa.setTitle("�� ����");
		YAxis ya = chrt.getYAxis();
		ya.setMin(0);
		ya.setFloor(0);
		ya.setMax(maxBalVal); // ������� ����� ���������� ��������
		ya.setTitle("USD");
		CategoryModel chmd = new DefaultCategoryModel();
		for (SubjRestHistory rh : lst) {
// ������������� ������: https://www.zkoss.org/zkchartsdemo/area_stacked_percent
// ��������� ��� ������������ Area range (https://www.zkoss.org/zkchartsdemo/arearange_line)
// ��������� ��� ������������ Sparkline charts: (https://www.zkoss.org/zkchartsdemo/area_sparkline)
// ��������� ��� ������������ ����������� (������, ����������� �� ��������): https://www.zkoss.org/zkchartsdemo/line_time_series
// TODO: mm_yy (��. ) ? ������������� (�������, ����� �� ����) ������/������
// ������������ ������� �������: https://www.zkoss.org/zkchartsdemo/line_ajax
// ��� �������: https://www.zkoss.org/zkchartsdemo/line_labels
			if ( rh.getDdRest().toLocalDate().plusDays(1L).getDayOfMonth() != 1 ) continue; // ���������� ������������� (������ ����� ������ !)
			chmd.setValue("RestBalUSD", SafeFormatter.asGermanDate(rh.getDdRest()), rh.getRestBalUSD());
			chmd.setValue("PastRestBalUSD", SafeFormatter.asGermanDate(rh.getDdRest()), rh.getPastRestBalUSD());
		}
		logger.trace("showSubjRestHistModal.  subjId = {}, maxBalVal = {}", subjId, maxBalVal);
		chrt.setModel(chmd);
		chrt.setParent(restsWin);
		
		restsWin.setPage(page);
	} // private void showSubjRestHistModal(Integer subjId, Page page)
	
	
	/** ���������� � ��������� ���� ���� �� ������ ������������ ��������.
	 * (����� �������� �� limitinfo.zul � ��������� ���������� ����� Map; ���� ������ � �����.)
	 */
	private void showLimitUsageModal(int subjId, Integer idlimit) {
		if (idlimit == null) {
			alert("No lLimit info...");
			return;
		}
		//LimitInfo li = SubjDopService.selectLimitInfoBySubjId(subjId);
		LimitInfo li = SubjDopService.selectLimitInfoByIdLimit(idlimit.intValue());
		if (li == null) {
			alert("Limit info not found...");
			return;
		}
// TESTME: ����� ����� � ����, �� ��� ������� !!!
		ListModelList<LimitHistory> lhModel = new ListModelList<>(li.getUsageHistory(), true);
		Object[][] arrArg = {{"subjId", subjId}
							,{"li", li}
							,{"lhModel", lhModel}
							,{"yyy.xxxxx", subjId} // HOWTO: ������ �/� ����� ��������� (�� ������������, ������ �� �������) ?!
							,{"yyy_xxxxx", subjId} // � EL ������������� ����� ��������� ����� �� �������, ��� � Java ('_'/'$'), �� � ���� �� ������ ��������� !!
		};
		Map<String, Object> arg = Generics.cast(ArrayUtils.toMap(arrArg));
// FIXME: ������� �� ����� ���� � restsWin.doHighlighted();
		Component rootComp = Executions.getCurrent().createComponents("limitinfo.zul", null, arg);
		//Selectors.wireComponents(this, this, true);
		//Selectors.wireEventListeners(this, this);
		//Components.wireVariables(this, this);
		//Components.addForwards(this, this);
		logger.debug("showLimitUsageModal.  UsageHistory.size = {} for subjId = {} & idLimit = {}, rootComp = {}", li.getUsageHistory().size(), subjId, idlimit/*li.getIdLim()*/, rootComp);
	} // private void showLimitUsageModal(int subjId, int idlimit)
    
		
	/** �������� ����� � ������� �� ����������� �� ������� DaData.
	 * ����������� ���������� � �������� �� ��� � ����� dadata.ru ("API ���������").
	 * ������ �������������� ����������� �� �� ��� �� ������� � ��������� DaDataInfo.
	 * ��������� ��� ����� ������ �� json � ���������� Map, ������ ����� �� ZUL, ������ ���������, �����
	 *  �������� ��������� ���������� ����� ��� �������� ����� ������������ ��������.
	 * �� json ���������� ������ ������ ���� (������ MAIN ��� ������� �������� ?) - suggestions[0].
	 * @param subjId �� ��������.
	 * @param daDataInfo ������ � ����������� ���, json-������� �������; ��������, � ������������ ������������.
	 * @param fromDB �������, ��� ������ ��� ������� �����, �������������� � ������� � ��.
	 * @param parComp ������������ ��� ����� ��������� (Optional).
	 * @param actDateBeforeLong ���� ������������ �� �� �� ������ ���������� �� ������� (Optional).
	 * @return �������� ������� ��������� �����.
	 */
	public Component createDaDataForm(int subjId, DaDataInfo daDataInfo, Boolean fromDB, Component parComp, Long actDateBeforeLong) {
    	String inn = daDataInfo.getInn()
    		,val = daDataInfo.getSuggestParty() // json
    		,user�omment = daDataInfo.getUserComment(); // ����������� �� ��� �������� �� ��
    	java.util.Date saveTimeBefore = daDataInfo.getSaveTime();
    	
/*// JBoss JSON-P version
		JsonReader jr = Json.createReader(new StringReader(val));
		JsonArray suggArr = jr.readObject().getJsonArray("suggestions");
		if (suggArr.isEmpty()) {
			logger.warn("createDaDataForm.  unexpectedly empty array 'suggestions' for inn = {}, subjId = {}", inn, subjId);
			return;
		} else if (suggArr.size() > 1) {
// !!! ��������, ��� = 7735008954 (�� "����", subjId = 84) - 7 �������
			logger.warn("createDaDataForm.  too many results = {} for inn = {}, subjId = {}", suggArr.size(), inn, subjId);
		}
 		JsonObject res = (JsonObject)suggArr.get(0);*/
 		        
/*// minimal-json version 
		JsonArray suggArr = Json.parse(val).asObject().get("suggestions").asArray();
		if (suggArr.isEmpty()) {
			logger.warn("createDaDataForm.  unexpectedly empty array 'suggestions' for inn = {}, subjId = {}", inn, subjId);
			return;
		} else if (suggArr.size() > 1) {
// !!! ��������, ��� = 7735008954 (�� "����", subjId = 84) - 7 �������
			logger.warn("createDaDataForm.  too many results = {} for inn = {}, subjId = {}", suggArr.size(), inn, subjId);
		}
 		JsonObject res = (JsonObject)suggArr.get(0);
 		
 		logger.trace("createDaDataForm. res = '{}', res_class = '{}'", res, res.getClass().getSimpleName());
 		logger.trace("createDaDataForm. unrestricted_value: '{}'", res.getString("unrestricted_value", ""));
// ������ ��� ������ ����� !
        logger.trace("createDaDataForm. data.kpp: '{}', data.management.name: '{}'", res.get("data").asObject().getString("kpp", ""), res.get("data").asObject().get("management").asObject().getString("name", "") );
*/ 		
        
/* ZK �������� ��������� Map � ������������ � createComponents() ���������, ����� ���� ������������ ����� JsonObject (��� �������� ?)
 � ����� ���������� ���������� ��� �������������� JSON � ������� ��������� Map: ��. https://habrahabr.ru/company/luxoft/blog/280782/
 http://stackoverflow.com/questions/20355261/how-to-deserialize-json-into-flat-map-like-structure
+https://github.com/wnameless/json-flattener  (���������� minimal-json)

 ������ �� ����� ��������, �������� � ������ ������ (data.address.data.city �� ��� = 7723822598)
  - �������� � ���, ��� ��� �� ����� ��������� ����� => ������ ������������ ������� Map, ��� ����� � ������
  ? ��������� �� ������� (� ������� �������������� �������) ? ��� �������� POJO ?
 ����� ���������� �������������� ������ ��������, � �.�. ������ ������ �������
*/		
// HOWTO: (��. StringEscapePolicy): '_' ����������, ���� ���� � ���� (����., 'suggestions[0]_data[\"branch_count\"]'), ������ ����� '$', ������� EL ��������� ��� ����� ��������������
// TODO: ��� ����������� �� JsonFlattener: �����������, ����������� JsonValue (���� �������� ���������� ������, ����� ������ �� ������� ��� ���������� ����); ����� ���� ���������� (���������� ���������� - ��������); ���������� (��� � ����), �� ��� �������� ���������� ��� stream �������� (!); regex ��� �������������� ������; ������������� ������������ (�� ��� ������� - ���������� �������� ����� ���� - fias_level � �.�.) - ����� ��������� ?; 
        Map<String, Object> arg0 = new JsonFlattener(val).withSeparator('$').flattenAsMap();
		logger.trace("createDaDataForm.  flattened: {}", arg0.entrySet().stream().map((Map.Entry<String,Object> k)->{return "['"+k.getKey()+"':'"+k.getValue()+"']";}).collect(Collectors.joining(", ", "{ ", " }")) );
		Map<String, Object> arg = new HashMap<>(); // ������� �� ����������� !
			arg0
				.entrySet()
				.stream()
// !!! ���� ������ ������ ������ (������ MAIN ��� ������� �������� ?) !!!
				.filter(e -> e.getKey().startsWith("suggestions[0]"))
				//.collect(Collectors.toMap(e -> e.getKey().substring(15).replace('^', '_'), e -> e.getValue())) // NPE !!! ???
				.forEach( e -> {
					String k = e.getKey().substring(15)/*.replace('$', '_')*/;
					Object v = e.getValue();
					if (v != null) {
						switch (k) { // �������������� �� ������������
							case "data$address$data$fias_level" :
								v = DaDataInfo.FiasLevel.uncode(Integer.parseInt((String)v));
								break;
							case "data$state$status" :
								v = DaDataInfo.uncodeStateStatus((String)v);
								break;
							case "data$address$data$qc_geo" :
								v = DaDataInfo.uncodeQcGeo(Integer.parseInt((String)v));
								break;
							case "data$branch_type" :
								v = DaDataInfo.uncodeBranchType((String)v);
								break;
						}
					}
					arg.put(k, v);
				 })
				;
		logger.trace("createDaDataForm.  flattened_transformed: {}", arg.entrySet().stream().map((Map.Entry<String,Object> k)->{return "['"+k.getKey()+"':'"+k.getValue()+"']";}).collect(Collectors.joining(", ", "{ ", " }")) );
		
// ���� ������������ (�� ���� ����������); ��������� � ������� "actDateBefore" ������ ���� �� �� ��� ������ �� ����
// ������ ��������� � daDataInfo.getPartyActDateBeforeLong(), �� �� ����������� ������ (��� �������� �� ��)
 		long dtActEpochMilli = 0L;
 		if ( fromDB ) { // � �� ������������ ��� �������� � ��������� �����
 			dtActEpochMilli = daDataInfo.getPartyActDateLong();
 			if (dtActEpochMilli == 0L) {
 				logger.warn("createDaDataForm.  unknown dtAct for inn = {}, subjId = {}", inn, subjId);
 				dtActEpochMilli = System.currentTimeMillis(); // ������� ����� !!
 			}
 		} else if ( actDateBeforeLong != null && actDateBeforeLong != 0L ) { // ��������� �� ��, �� ����������� �� ������� �� �������� ����� ��� �� ������ "��������" �����
 			dtActEpochMilli = actDateBeforeLong.longValue();
	 		BigDecimal tmp = (BigDecimal)arg.get("data$state$actuality_date");
	 	    if ( tmp != null ) {
	 	    	daDataInfo.setActuality( Long.valueOf( tmp.longValue() ) ); // (����� != ��)
	 	    }
 		} else { // (�� == �����) ������ ������ �� ������� - ���� ������������ �� flattenMap
	 		BigDecimal tmp = (BigDecimal)arg.get("data$state$actuality_date");
	 	    if ( tmp != null ) {
	 	    	//dtActEpochMilli = tmp.longValue();
	 	    	daDataInfo.setActuality( Long.valueOf( tmp.longValue() ) );
	 	    }
 		}
 		
 		assert dtActEpochMilli == daDataInfo.getPartyActDateBeforeLong() : "������������ ActDateBeforeLong";
		
		// �������������� (� json) ��������� ��� �������� � �����
        arg.put("subjId", subjId); // ������ � caption
        arg.put("usercomment", StringUtils.isBlank(user�omment) ? "" : user�omment);
        //arg.put("ifFromDBrem", fromDB ? " (����������)" : ""); // ������ � caption
        arg.put("saveTimeBefore", saveTimeBefore); // ���� ������� �� ��, �� ����������� ��� ���������� �� �������
        
        Component rootComp = Executions.getCurrent().createComponents("dadata.zul", parComp, /*res*/arg);
        assert rootComp != null && "dadataRoot".equals(rootComp.getId()) : "��� ����� 'dadata.zul' ��������� �������� ��������� � �� = 'dadataRoot'";
        
// TODO: ��������� �����-�������� ��� ��������
		//Selectors.wireComponents(this.getSelf(), this, false/*true*/); // HOWTO: ignoreNonNull !! ����� �� ����������� �������, ��������, � ����� ���� �������� �������� ������� ����������: ignoreNonNull - ignore wiring when the value of the field is a Component (non-null) or a non-empty Collection
		Selectors.wireEventListeners(this.getSelf(), this);

		if (rootComp != null) { //
			rootComp.setAttribute("subjId", subjId);
			rootComp.setAttribute("daDataInfo", daDataInfo);
			//rootComp.setAttribute("inn", inn); // ������
			//rootComp.setAttribute("json", val); // ������
			//rootComp.setAttribute("actDateBefore", Long.valueOf(dtActEpochMilli)); // !! ������ !! (���-�� ���� DaDataInfo.partyActDateBeforeLong)
			//rootComp.setAttribute("usercommentBefore", user�omment); // ������
			rootComp.setAttribute("fromDB", fromDB); // !! ������ !! (���-�� ���� DaDataInfo.jsonState)
			//rootComp.setAttribute("saveTimeBefore", saveTimeBefore); // ������
			//rootComp.setAttribute("actDateAfter", Long.valueOf( daDataInfo.getPartyActDateLong() ) ); // ������
		} else {
			logger.error("createDaDataForm.  rootComp is null");
		}
		
		return rootComp;
		
	} // public Component createDaDataForm(int subjId, DaDataInfo daDataInfo, Boolean fromDB, Component parComp)
	
	
	/** �������� ����� DaData � ��������� ����. ����������� ���������� � �������� �� ��� � ����� dadata.ru ("API ���������").
	 * ������� ���� ���������� � �� ������. �� ����� - ������ ������. ���� ������� � ��� "��������", ����������
	 *  ������������� �� �������. ��������� ����� � ����.
	 * @param page �� ����� �������� ������ �����.
	 */
	private void showSuggestionModal(int subjId, String inn, Page page) {
// ��������� ���������, ������ � ��, ������ �� ���������� ��������
		if (inn == null || inn.length() != 10) {
    		alert("��� �������� �������� "+subjId+" ��������� ��� ������ 10 ����.");
    		return;
    	}
		
// FIXME: �������� �� ���� DaDataInfo.jsonState: FROMDB_LOADED, FIRST_FROMRS_LOADED, RELOADED_BEFORE_FORM, RELOADED_DURING_FORM
    	Boolean fromDB; // �������, ��� ������ ���� � �� � ��������� ������; ����� ��� ������� � ������������� � ����������� ������������
    	java.util.Date saveTimeBefore = null; // ��� �������� "�����������"
    	
// ������� ��������� ����, � �� ����� �� ������ ��������� _daDataInfo � ������� ������ ����� (���� � �� "dadataRoot")
    	TriFunction<DaDataInfo,Boolean,Long,Component> createFormInWin = (_daDataInfo, _fromDB, _actDateBeforeLong) -> {
    			Window /*this.*/dadataWin = new Window(null, "normal", true); // <window id="dadataWin" height="90%" width="50%" mode="highlighted" sizable="true" closable="true" border="normal" style="max-width:70%;"> <!--  hflex="min" vflex="min" overflow:auto; - �� �������� -->
    			dadataWin.setHeight("90%"); dadataWin.setWidth("60%");
    			dadataWin.setSizable(true);
    			dadataWin.setId("dadataWin");
    			Caption cpt = new Caption("������ ������� DaData"+(_fromDB ? " (����������)" : "")+" �� �������� �� LM "+subjId); // <caption label="������ ������� DaData${arg.ifFromDBrem} �� �������� �� LM ${arg.subjId}" style="background:#cceeff;text-align:center;font-size:24px;font-weight:bold;color:#b22222;" />
    			cpt.setStyle("background:#cceeff;text-align:center;font-size:24px;font-weight:bold;color:#b22222;");
    			dadataWin.appendChild(cpt);
    			dadataWin.setPage(page);
    			Component rootComp = createDaDataForm(subjId, _daDataInfo, _fromDB, dadataWin, _actDateBeforeLong);
    			dadataWin.doHighlighted();
    			return rootComp;
    	}; // createFormInWin()
    	
    	// ������� ���� ������ �� ��� � �� (��������� ��������)
    	DaDataInfo daDataInfo = SubjDopService.selectDaDataInfoLatestByInn(inn);
    	if (daDataInfo != null) { // ����� � ��; ��������� ������ ?
    		fromDB = Boolean.TRUE;
    		//daDataInfo.fixPartyActDateBeforeLong(); //daDataInfo.setPartyActDateBeforeLong(daDataInfo.getPartyActDateLong());
    		saveTimeBefore = daDataInfo.getSaveTime();
    		boolean mayReload = saveTimeBefore.toInstant().until(Instant.now(), ChronoUnit.HOURS) > 8;
    		logger.trace("showSuggestionModal.  subjId: {}, inn: {} found in DB with saveTimeBefore = {}, hours_until_now = {}, mayReload = {}", subjId, inn, saveTimeBefore, saveTimeBefore.toInstant().until(Instant.now(), ChronoUnit.HOURS), mayReload );
        	if ( mayReload ) { // ���������� ����������� ������ ��� �������� ����������
        		final DaDataInfo fin_daDataInfo = daDataInfo;
        		Messagebox.show(
					 "������ ����� ���� ��������� "+saveTimeBefore+". ����������� ?", "Prompt"
					,Messagebox.YES|Messagebox.NO, Messagebox.QUESTION
					,new SerializableEventListener<Event>() {
	        			private static final long serialVersionUID = -4439495004385792793L;
	        			public void onEvent(Event evt) {
	        				Component loc_rootComp = null;
	        				final DaDataInfo loc_daDataInfo = fin_daDataInfo;
	        				Boolean loc_fromDB = fromDB;
	        				switch( ((Integer)evt.getData()).intValue() ) {
	        					case Messagebox.YES:  // ����������� ������ ������ (daDataInfo �� ����� ! � ������), ������� ���������; ��� ������� ������������ ����������� �� ��; ������� ����2
	        						long actDateBeforeLong = loc_daDataInfo.getPartyActDateLong(); // �� ���������� ��������� ����������������
	        						if ( loc_daDataInfo.loadFromRS() ) { // ����� ��� ������ ������������� json, �������� ������������, saveTime �� ������ (�� ��, ����� ��� update)
	        							loc_fromDB = Boolean.FALSE;
	        							//loc_daDataInfo.setPartyActDateBeforeLong(actDateBeforeLong); // ������ � selectDaDataInfoLatestByInn
	        							loc_daDataInfo.setJsonState(JsonStates.RELOADED_BEFORE_FORM);
	        						} else { // �������� ��������: ����� ���� ���������, ������ �� ����� (��������, ���� �������)
	        							actDateBeforeLong = 0L;
	        							loc_daDataInfo.setJsonState(JsonStates.FROMDB_LOADED);
	        							logger.debug("showSuggestionModal.  ���������� �� ��� {} �� ������� ��� ������� ������������� �� �������� �����, subjId = {}", inn, subjId);
	        							alert("���������� �� ��� "+inn+" �� �������, ������� ����������.");
	        						}
	        						loc_rootComp = createFormInWin.apply(loc_daDataInfo, loc_fromDB, Long.valueOf(actDateBeforeLong));
	        						logger.debug("showSuggestionModal.  DaDataForm was created after YES answer (����������� ����������� ����������� �� �� �����), fromDB = {}, JsonState = {}, rootComp = {}", fromDB, loc_daDataInfo.getJsonState(), loc_rootComp);
	        						break;
	        					case Messagebox.NO: // ������������ ������ ������, ����������� �� ��
	        					default: // ������� ��������� ?
	        						loc_daDataInfo.setJsonState(JsonStates.FROMDB_LOADED);
	        						loc_rootComp = createFormInWin.apply(loc_daDataInfo, loc_fromDB, null);
	        						logger.debug("showSuggestionModal.  DaDataForm was created after NO answer (���������� ����������� ����������� �� �� �����), fromDB = {}, JsonState = {}, rootComp = {}", fromDB, loc_daDataInfo.getJsonState(), loc_rootComp);
	        						break;
	        				} // switch
	        			} // onEvent
					 } // EventListener
        		); // Messagebox.show
        		return; // ��������
        	} // �������
        	else { // �� �� � ������
        		daDataInfo.setJsonState(JsonStates.FROMDB_LOADED);
        	}
    	} // ������� � ��
    	else { // �������� ��������� ������ ��� �� �������
    		//loadThenShowModal(): ������� ����; loadDaDataInfoFromRS(DaDataInfo.getBlank(inn));...step2(subjId, daDataInfo, fromDB);
    		daDataInfo = DaDataInfo.getBlank(inn);
    		if ( !daDataInfo.loadFromRS() ) {
    			logger.debug("showSuggestionModal.  ���������� �� ��� {} �� ������� (��� ���������� � ��), subjId = {}", inn, subjId);
    			alert("���������� �� ��� "+inn+" �� �������");
    			//daDataInfo.setJsonState(JsonStates.BLANC);
    			return;
    		}
    		fromDB = Boolean.FALSE;
    		daDataInfo.setJsonState(JsonStates.FIRST_FROMRS_LOADED);
    	}
// ����� ����������� ���� �� ���� � �� � ������� ���������� ��� ���� � ���� � �� ���������� �������� (�� ������)
// ���� ���������� ��������, �� ����� �����������
    	
		Component rootComp = createFormInWin.apply(daDataInfo, fromDB, null);
		logger.debug("showSuggestionModal.  DaDataForm was created at once, fromDB = {}, JsonState = {}, rootComp = {}", fromDB, daDataInfo.getJsonState(), rootComp);
		
    } // private void showSuggestionModal(int subjId, String inn)
	
	//@Wire("#dadataWin #dadataUserCommTB") Textbox dadataUserCommTB;
	//@Wire Window dadataWin;
	
	/** ���������� � �� ��� �������� ����� DaData. */
	@Listen("onClick = #dadataWin #dadataSaveBtn; onClose = #dadataWin")
// TODO: ��������� �����-�������� ��� ��������
	public void onSaveDaDataInfo(Event ev) { // ���� ��������� ������� ��� ������ �����������, ���������
		Component rootComp = ev.getTarget().getFellow("dadataRoot");
		Component dadataWin = rootComp.getParent() == null || !(rootComp.getParent() instanceof Window) ? null : rootComp.getParent(); //rootComp.getFellow("dadataWin"); //Path.getComponent("dadataWin");
		Textbox dadataUserCommTB = (Textbox)ev.getTarget().getFellow("dadataUserCommTB");
		DaDataInfo daDataInfo = (DaDataInfo)rootComp.getAttribute("daDataInfo");
		String inn = daDataInfo.getInn() //(String)rootComp.getAttribute("inn")
			  //,suggestPartyRaw = daDataInfo.getSuggestParty() //(String)rootComp.getAttribute("json")
			  ,usercommentBefore = StringUtils.isBlank(daDataInfo.getUserComment()) ? "" : daDataInfo.getUserComment() //(String)rootComp.getAttribute("usercommentBefore")
			  ,usercommentAfter = "";
		Boolean fromDB = (Boolean)rootComp.getAttribute("fromDB");
		DaDataInfo.JsonStates jsonState = daDataInfo.getJsonState();
		Long actDateBeforeLong = Long.valueOf( daDataInfo.getPartyActDateBeforeLong() ) //(Long)rootComp.getAttribute("actDateBefore")
			,actDateAfterLong = Long.valueOf( daDataInfo.getPartyActDateLong() ); //(Long)rootComp.getAttribute("actDateAfter");
		java.util.Date saveTimeBefore = daDataInfo.getSaveTime(); // �� ���-�� //(java.util.Date)rootComp.getAttribute("saveTimeBefore");
				
		if (dadataUserCommTB != null) {
			usercommentAfter = StringUtils.isBlank(dadataUserCommTB.getValue()) ? "" : dadataUserCommTB.getValue();
		}
		
		boolean commentChanged = !usercommentAfter.equals(usercommentBefore)
				// json ���������, ����� �� ��, �� ����������� � ����� ��; ��� ������� �������� �� �������
				// �� ��������� ���� �� �� � �� ����������� ��� �� �� ����������
				,loadedNewParty = ( fromDB == null || !fromDB  || jsonState != null && jsonState != DaDataInfo.JsonStates.BLANC && jsonState != DaDataInfo.JsonStates.FROMDB_LOADED ) // ������ ������
							   && (actDateBeforeLong == null || actDateBeforeLong == 0L || !actDateAfterLong.equals(actDateBeforeLong));
		
		logger.trace("onSaveDaDataInfo.  ev_name = '{}', rootComp = '{}', ev_class = '{}', ev_target = '{}', ev_target_fellow_win = '{}', dadataUserCommTB = '{}', inn = {}", ev.getName(), rootComp, ev.getClass().getSimpleName(), ev.getTarget(), ev.getTarget().getFellowIfAny("dadataWin"), dadataUserCommTB, inn);
		logger.debug("onSaveDaDataInfo.  inn = {}, usercommentBefore = '{}', usercommentAfter = '{}', jsonState = '{}', fromDB = '{}', actDateBefore = '{}', actDateAfter = '{}', commentChanged = {}, loadNew = {}, saveTimeBefore = {}", inn, usercommentBefore, usercommentAfter, jsonState, fromDB, actDateBeforeLong, actDateAfterLong, commentChanged, loadedNewParty, saveTimeBefore);
		
		Supplier<Integer> saveToDB = () -> {
			Integer rc = 0;
			if ( loadedNewParty ) {
				rc = Integer.valueOf( SubjDopService.insertDaDataInfo(daDataInfo) );
				logger.debug("onSaveDaDataInfo_NEW.  INSERT, sqlRowCount = {}", rc);
			} else if ( commentChanged ) { // ��������� ������ ������� � ����� (��� ���������� �� ������� ������ INSERT)
				rc = Integer.valueOf( SubjDopService.updateDaDataInfoBySaveTime(daDataInfo) );
				logger.debug("onSaveDaDataInfo_NEW.  UPDATE, sqlRowCount = {}", rc);
			} else {
				logger.trace("onSaveDaDataInfo_NEW.  nothing to save");
			}
			logger.trace("onSaveDaDataInfo_NEW.  daDataInfo = {}", daDataInfo);
			return rc;
		};
		
		// �������� �� �������� (�� �� ������): �������� � ����������, �� ����� json � �� ��������� ������
		if ( "onClose".equals(ev.getName()) && (loadedNewParty || commentChanged) ) {
			String loc_usercommentAfter = usercommentAfter;
    		Messagebox.show( "��������� ������ ?", "Prompt", Messagebox.YES|Messagebox.NO, Messagebox.QUESTION
				,new SerializableEventListener<Event>() {
					private static final long serialVersionUID = 9124127831123105130L;
					public void onEvent(Event evt) {
						int rc = 0; // SQLRowCount
        				switch( ((Integer)evt.getData()).intValue() ) {
        					case Messagebox.YES:  // ���������
        						daDataInfo.setUserInput(loc_usercommentAfter, "basos");
        						rc = saveToDB.get().intValue();
        						logger.trace("onSaveDaDataInfo.  ������������� YES ����� �������� ���� ���������. choise: {}, SQLRowCount: {}", ((Integer)evt.getData()).intValue(), rc);
        						break;
        					case Messagebox.NO: // ���������� ���������, �� ������������� json �� ����� ���������
        					default: // ������� ��������� ?
        						// ������� �� ��������� !! �� null ������, ��� ��� null ����� ���� ��������� jdbcType :)
        						daDataInfo.setUserInput(usercommentBefore/*""*/, "basos"); // !! �������� ������, �� �������� !!
        						if (loadedNewParty) {
        							rc = saveToDB.get().intValue();
        						}
        						logger.trace("onSaveDaDataInfo.  ������������� NO ����� �������� ���� ���������. choise: {}, SQLRowCount: {}", ((Integer)evt.getData()).intValue(), rc);
        						break;
        				} // switch
        				if ( dadataWin != null ) {
        					dadataWin.detach(); // ������� ����
							//dadataWin = null;
        				}
        			} // onEvent
				 } // EventListener
    		); // Messagebox.show
			return;
		} // if �������� ���� ������� � ���� ���������
		
		// ����� ����������� ���� �������� �� ������ "���������" //��� ���������, �� ������ ��������� �� ����
		int sqlRowCount = 0;
// ������� - ������� saveTime: ������� ��������, ��� �� �� �� �������, � ������� ������� �� �������;
// �������� saveTime ��������, ��� �������� �� ��, �� ����� ����������� �� ������� (�� �������� ����� �� ������� ��� �� ������ "��������")
// ������� ���������� �� ������� (��� ������ �.�. �� �� ��, ����� insert; update ����� � ������, ����� ��������� ������ �������): ���������������� (������� ������)
		if ( loadedNewParty || commentChanged ) { // �� ���� ����� ��������
			daDataInfo.setUserInput(usercommentAfter, "basos");
/*			// MERGE statement (����� �� ����� ������ ���������� ��) !!! (http://stackoverflow.com/questions/19593785/how-can-i-use-oracle-merge-statement-using-mybatis)
			// !!! INSERT ��������, ��� UPDATE: ### Error updating database.  Cause: java.sql.SQLRecoverableException: ������ ��� ���������� �� ������ �����������
			sqlRowCount =  Integer.valueOf( SubjDopService.mergeDaDataInfoByActDate(daDataInfo) );
			logger.trace("onSaveDaDataInfo.  MERGE, sqlRowCount = {}, daDataInfo = {}", sqlRowCount, daDataInfo);
*/			
			sqlRowCount = saveToDB.get().intValue();
			logger.trace("onSaveDaDataInfo.  ���������� ����� �������� �������: sqlRowCount = {}", sqlRowCount);
		}
		
/*		DaDataInfo tmp_daDataInfo = null;
		if ( loadedNewParty ) {
			tmp_daDataInfo = new DaDataInfo( inn
										,suggestPartyRaw // suggestParty
										,usercommentAfter // userComment
										,"basos" // userName
										,actDateAfter
										,new java.sql.Date(actDateAfter)
										,null //new java.util.Date() // ����������� �� ������� ��
						);
			sqlRowCount = SubjDopService.insertDaDataInfo(tmp_daDataInfo);
			logger.trace("onSaveDaDataInfo.  INSERT, sqlRowCount = {}", sqlRowCount);
		} else if ( commentChanged ) {
// ��������� ������ ������� � ����� (��� ���������� �� ������� ������ INSERT)
			tmp_daDataInfo = new DaDataInfo( inn
										,null //suggestPartyRaw
										,usercommentAfter // userComment
										,"basos" // userName
										,null //actDateBefore
										,null //new java.sql.Date(actDateBefore)
										,saveTimeBefore // (!! ��� ���������� ����� daDataInfo.saveTime �.�. ������ (����� �� ����� ����������� ������); ����� ����������� � SQL !!) new java.util.Date()
						);
			sqlRowCount = SubjDopService.updateDaDataInfoBySaveTime(tmp_daDataInfo);
			logger.trace("onSaveDaDataInfo.  UPDATE, sqlRowCount = {}", sqlRowCount);
		} else {
			logger.trace("onSaveDaDataInfo.  nothing to save");
		}
		logger.trace("onSaveDaDataInfo.  daDataInfo = {}", tmp_daDataInfo);
*/		

		if ( dadataWin != null ) {
			dadataWin.detach(); // ������� ����
			//dadataWin = null;
		}
		
	} // onSaveDaDataInfo
	
	
	/** ��������� � ������� ����� DaData �� ���������� ������. */
	@Listen("onClick = #dadataWin #dadataRefreshBtn")
// TODO: ��������� �����-�������� ��� ��������
	public void onRefreshDaDataInfo(Event ev) {
		Component oldRoot = ev.getTarget().getFellow("dadataRoot");
		DaDataInfo daDataInfo = (DaDataInfo)oldRoot.getAttribute("daDataInfo");
		Boolean fromDB = (Boolean)oldRoot.getAttribute("fromDB");
		DaDataInfo.JsonStates jsonState = daDataInfo.getJsonState();
		String inn = daDataInfo.getInn(); //(String)rootComp.getAttribute("inn");
		Integer subjId = (Integer)oldRoot.getAttribute("subjId");
		
		logger.debug("onRefreshDaDataInfo.  subjId = {}, inn = {}, fromDB = {}, jsonState = {}", subjId, inn, fromDB, jsonState);
		if ( fromDB == null || !fromDB || jsonState != null && jsonState != DaDataInfo.JsonStates.BLANC && jsonState != DaDataInfo.JsonStates.FROMDB_LOADED ) { // ������ ��� (�������) ������� �� �������� ��� � ����� ���� �����
			alert("�������� ����� ������ ����� ����������� ������");
			return;
		}
		// ����� ��������� �� ��, ����� ����������� � �������� �����, ���� ������ ���������� (�� ���� ������������)
		// ���� ������������ �� ���������, �.�. �� ������
		// ��-��������, ����� �������� �������������, �� �������� ��������...
		
		Long actDateBeforeLong = Long.valueOf( daDataInfo.getPartyActDateBeforeLong() ); //(Long)oldRoot.getAttribute("actDateBefore");
		//long dtActEpochMilli = daDataInfo.getPartyActDateLong(); // �� ���������� ��������� ����������������

		Textbox dadataUserCommTB = (Textbox)ev.getTarget().getFellow("dadataUserCommTB");
		String usercomment = ""; // ������� � ����� ����������� � ���� ������� ������ ����� ����������� � ��
		if (dadataUserCommTB != null) {
			usercomment = StringUtils.isBlank(dadataUserCommTB.getValue()) ? "" : dadataUserCommTB.getValue();
		}
		
		if ( daDataInfo.loadFromRS() ) { // ����� ��� ������ ������������� json, �������� ������������, saveTime �� ������ (�� ��, ����� ��� update)
			fromDB = Boolean.FALSE; // ������� ��������� ���������� ��������������� � createDaDataForm
			daDataInfo.setJsonState(JsonStates.RELOADED_DURING_FORM);
		} else { // �������� ��������: ����� ���� ���������, ������ �� ����� (��������, ���� �������)
			alert("���������� �� ��� "+inn+" �� �������.");
			logger.debug("onRefreshDaDataInfo.  ���������� �� ��� {} �� ������� ��� ������������ �� �����, subjId = {}", inn, subjId);
			return;
		}
		
// ������������ ����� - �� ����, ����� �� ������ �������� ��������� � ������ ��������
		//Component oldRoot = rootComp/*dadataWin.getFellow("dadataRoot")*/;
		Component dadataWin = oldRoot.getParent(); //oldRoot.getFellow("dadataWin"); //Path.getComponent("dadataWin");
		oldRoot.detach();
		Component newRoot = createDaDataForm(subjId.intValue(), daDataInfo, fromDB, dadataWin, actDateBeforeLong/*Long.valueOf(dtActEpochMilli)*/ );
		//newRoot.setAttribute("fromDB", fromDB); // ��������������� � createDaDataForm()
		logger.trace("onRefreshDaDataInfo.  DaDataForm was reloaded (�� ������ �� �����): subjId = {}, inn = {}, fromDB = {}, jsonState = {}, oldRoot = {}, newRoot = {}, dadataWin = {}", subjId, inn, fromDB, jsonState, oldRoot, newRoot, dadataWin);
		
		dadataUserCommTB = (Textbox)newRoot.getFellow("dadataUserCommTB"); // ����� ��������� � ����� ������ !!
		if ( !StringUtils.isBlank(usercomment) ) {
			 dadataUserCommTB.setValue(usercomment);
		}
		
	} // public void onRefreshDaDataInfo(Event ev)
	
	
	@Wire private Tabpanel subjsTabpanel;
    @Wire private Tabpanel dealsTabpanel;
	/** ����������� ���� � ������� �������� �� ��������.
	 * @param subjId �� ��������.
	 * @param parComp ������������ �������.
	 */
	private void subjActionsMenu(Integer subjId, Component parComp, SubjSumm bean) {
		Menupopup menu = new Menupopup();
// TODO:HOWTO: ? ��� �������� ����� ������� ���� (������ ��� ������� ��������� ����� "altpopupstyle" �� ZUL) ?
// .z-menupopup .z-menupopup-content .z-menuitem .z-menuitem-content .z-menuitem-text {font-size:15px;font-weight:bold;font-style:italic;color:#FF1493 !important;}
		//menu.setStyle("border:3px solid #B0E0E6; border-radius:3px; .z-menuitem-text {font-size:16px;font-weight:bold;color:#FF1493 !important;}"); // B0E0E6 - PowderBlue, BC8F8F - RosyBrown, FF7F50 - Coral, 7B68EE - MediumSlateBlue, 48D1CC - MediumTurquoise, C71585 - MediumVioletRed, FF4500 - OrangeRed, FF6347 - Tomato, 4682B4 - SteelBlue, 708090 - SlateGrey, DB7093 - PaleVioletRed, CD5C5C - IndianRed, D2691E - Chocolate
    	Menuitem el1 = new Menuitem("������� � ������� �������")
    			,el2 = new Menuitem("������� � ������� ���")
    			,el3 = new Menuitem("�������� ��������")
    			,el4 = new Menuitem("����� ������������")
    			,el5 = new Menuitem("��������� �� ��� �� DaData");
    	
    	SerializableEventListener<Event> onClickMI = new SerializableEventListener<Event>() {
			private static final long serialVersionUID = 9124127831123105130L;
			@Override
    		public void onEvent(Event event) throws Exception {
				Menuitem el = (Menuitem) event.getTarget();
				String fk = null;
				menu.close();
				menu.detach();
				if (el1.equals(el)) { // ������� � ������� �������
					fk = "clnId";
				} else if (el2.equals(el)) { // ������� � ������� ���
					fk = "rsubjId";
				} else if (el3.equals(el)) { // �������� ��������
					showSubjRestHistModal(subjId, subjSummGrid.getPage());
					return;
				} else if (el4.equals(el)) { // ����� ������������
					if (bean == null) {
						alert("�� ������� �������� ��� ���� SubjSumm !");
						return;
					}
					Integer idLimit = bean.getIdlimit(); // Integer !!! subjId = 608940: null
					showLimitUsageModal(subjId.intValue(), idLimit);
					return;
				} else if (el5.equals(el)) { // ��������� �� ��� �� DaData
					if (bean == null) {
						alert("�� ������� �������� ��� ���� SubjSumm !");
						return;
					}
					showSuggestionModal(subjId.intValue(), bean.getInn(), subjSummGrid.getPage());
					return;
				}
				// ������� � ������� �������/���
				String fkPar = fk;
				//dealsTab.setSelected(true); // !!! fulfill �� ����������� (������ ������, ���� ������� �� ��������) !!!
				//tabbic.setSelectedTab(dealsTab); // same as above
// ��������� ������������� �������� dealsPage, ������� �������� �� ������� fulfill �� dealsTabpanel, � ������, ���� �� ��������� ����� (Tab)
// HOWTO: ������ �������� ("//dealsPage") �� ������� ��� ��������� ?
				Component dlb = Path.getComponent("//dealsPage/dealsLB"); // "//dealsPage/dealsLB"
				if (dlb == null) { // ������ ��������� ������ dealsTabpanel; ��� �������������, ����� ��������� ������� � �������
					logger.trace("subjActionsMenu. add onAfterCreateDealsPage listener, send onSelect.");
// ON_FULFILL ������� ����; ���������� ���������� doAfterCompose ����������� �������� dealsPage
					dealsTabpanel.addEventListener("onAfterCreateDealsPage", new SerializableEventListener<Event>() {
						private static final long serialVersionUID = 4995193078703309766L;
						public void onEvent(Event event) throws Exception {
							logger.trace("subjActionsMenu. dealsTabpanel.onAfterCreateDealsPage  dealsLB(now) = '{}', dealsPage = '{}', ev_data(composer) = '{}'", Path.getComponent("//dealsPage/dealsLB"), desktop.getPageIfAny("dealsPage"), event.getData());
							//goToDeals.accept( Path.getComponent("//dealsPage/dealsLB"), fkPar);
							//((DealsPageComposer)event.getData()).asSubjectDetails(fkPar, subjId);
							Object[] params = {fkPar, subjId};
							interDeskEQ.publish(new Event("onGoToDeals", null, params));
							dealsTabpanel.removeEventListener("onAfterCreateDealsPage", this); // ����������������� ����
						}; // onAfterCreateDealsPage onEvent
					}); // dealsTabpanel.addEventListener("onAfterCreateDealsPage", new SerializableEventListener<Event>()
					//Events.sendEvent(Events.ON_SELECT, /*dealsTab*/dealsTabpanel.getLinkedTab(), null); // ������������� ������ ������ ���������� �����, sleep �� ��������
					Events.sendEvent( new SelectEvent<>(Events.ON_SELECT // name
											,dealsTabpanel.getLinkedTab() // target
											,Collections.singleton(dealsTabpanel.getLinkedTab()) // selectedItems
											,Collections.singleton(subjsTabpanel.getLinkedTab()) // previousSelectedItems
											,Collections.singleton(subjsTabpanel.getLinkedTab()) // unselectedItems
											,Collections.emptySet() // selectedObjects
											,Collections.emptySet() // prevSelectedObjects
											,Collections.emptySet() // unselectedObjects
											,dealsTabpanel.getLinkedTab() // ref
											,null // data
											,0 // keys
									)
					);
				} // dealsLB == null
				else { // �������� ��� �������
					Object[] params = {fk, subjId};
					Events.sendEvent( new SelectEvent<>(Events.ON_SELECT // name
											,dealsTabpanel.getLinkedTab() // target
											,Collections.singleton(dealsTabpanel.getLinkedTab()) // selectedItems
											,Collections.singleton(subjsTabpanel.getLinkedTab()) // previousSelectedItems
											,Collections.singleton(subjsTabpanel.getLinkedTab()) // unselectedItems
											,Collections.emptySet() // selectedObjects
											,Collections.emptySet() // prevSelectedObjects
											,Collections.emptySet() // unselectedObjects
											,dealsTabpanel.getLinkedTab() // ref
											,null // data
											,0 // keys
									)
					);
					interDeskEQ.publish(new Event("onGoToDeals", null, params));
				}
				logger.trace("subjActionsMenu(after choice).  onClick item(el.label): '{}', sid(subj_id) = '{}', dealsLB: '{}', dealsPage: '{}'", el.getLabel(), subjId, dlb, desktop.getPageIfAny("dealsPage"));
    		} // onEvent
		}; // SerializableEventListener onClickMI
    	
    	//el.addForward(Events.ON_CLICK, changePkToolBB, "onChangePk", kn);
    	el1.addEventListener(Events.ON_CLICK, onClickMI);
    	menu.appendChild(el1);
    	
		el2.addEventListener(Events.ON_CLICK, onClickMI);
    	menu.appendChild(el2);
    	
    	el3.addEventListener(Events.ON_CLICK, onClickMI);
    	menu.appendChild(el3);
    	
    	el4.addEventListener(Events.ON_CLICK, onClickMI);
    	menu.appendChild(el4);
    	
    	el5.addEventListener(Events.ON_CLICK, onClickMI);
    	menu.appendChild(el5);
    	
		menu.setParent(parComp);
		menu.open(parComp);
	} // private void subjActionsMenu(Integer subjId, Component parComp)
	
	
//    @Listen("onClick = grid#subjSummGrid > rows > row")
    @Listen("onClickRow=#subjSummGrid") // ���� �� ������ �� ��������
    public void onClickSubj(ForwardEvent fev) {
    	String lb = (String)fev.getData(); // �� �������� - �������� ������
    	if (lb == null) {
    		return;
    	}
    	Integer subjId = Integer.valueOf(lb);
    	Component aOrig = fev.getOrigin().getTarget(); // ������ ���� ������ ("A") ������� ������ � ������� subj_id
    	Component origPar = aOrig.getParent();
    	SubjSumm bean = null;
    	if (origPar instanceof Row) {
    		Object each = ((Row)origPar).getValue();
    		if (each instanceof GridData<?>) {
    			bean = (SubjSumm)((GridData<?>)each).getBean();
    		}
    	}
    	subjActionsMenu(subjId, aOrig, bean);
    } // public void onClickSubj(String lb)


	/** ��� ��������� ��������� �������� � ������ ������� ����� ("����" ������ ������) ��������� ���� GridData.sel.
	 * ����������� ����� ��������� dispatchGridModelLockingTask � ������������ ����������� ������.
	 * �������� {@link GridDataFilterableModelMan#selectRow(boolean, int) GridDataFilterableModelMan.selectRow()}, ����� ��������� ������ (��� �������������).
	 * @param isChecked ����� �������� ����� (true/false), ������� ����� �������� � ������.
	 * @param subj_id �� �������� (���� ��������� ������� ���� SubjSumm - <b>�� ������������</b>).
	 * @param rn ����� ������ � �����, ������� �� �������, �� ������������� ������ (�������� � ����������� - <b>�� ������������</b>).
	 * @param uid GridData.uid - ���������� ����� ���������� �������, �� ������ �� ���������. �� ������ ���� ������ � ������.
	 */
// FIXME: ��� �������������� ����� (���������� ������ ��������) ?
// --��������� � ZUL �� ������� ����� � ������ ������� // @Listen("onClick = grid#subjSummGrid > rows > row")
    @Listen("onCheckRow=#subjSummGrid")
    public void onCheckRow(ForwardEvent fev) { // �������� ��������� ������� (GridData.sel) � ������
// HOWTO: (ZUML Ref. p. 78): The 'each' object is actually stored in the parent component's attribute, so you could retrieve it in pure Java as follows: comp.getParent().getAttribute("each") / getAttribute("forEachStatus")
// (DR p. 51) However, you cannot access the values of each and forEachStatus in an event listener because their values are reset after the XML element which forEach is associated has been evaluated. There is a simple solution: store the value in the component's attribute, so you can retrieve it when the event listener is called.
    	//fev.stopPropagation(); // ???
    	//logger.trace("t1 = {}", System.currentTimeMillis());
    	Checkbox cb = (Checkbox)fev.getOrigin().getTarget();
    	MeshElement host = (MeshElement)fev.getTarget();
    	GridDataFilterableModelMan<?> gdm = (GridDataFilterableModelMan<?>)host.getAttribute("GridDataModelMan");
    	Row r = (Row)cb.getParent();
    	Rows rs = (Rows)r.getParent();
    	logger.trace("det onCheckRow. target(host)_id='{}', origin: '{}', orig_target(Checkbox): '{}', orig_target_id ='{}', row_value ='{}', row_attrs ='{}', rows ='{}', rows_attrs ='{}', rows_par ='{}', rows_par_attrs ='{}', rows_model_template = '{}', GridDataModelMan ='{}', beanClass = '{}', Pk = '{}'", host.getId(), fev.getOrigin(), cb, cb.getId(), r.getValue(), r.getAttributes().entrySet().stream().map(k -> {return "['"+k.getKey()+"':'"+k.getValue()+"']";}).collect(Collectors.joining(", ", "{ ", " }")), rs, rs.getAttributes().entrySet().stream().map(k -> {return "['"+k.getKey()+"':'"+k.getValue()+"']";}).collect(Collectors.joining(", ", "{ ", " }")), rs.getParent(), rs.getParent().getAttributes().entrySet().stream().map(k -> {return "['"+k.getKey()+"':'"+k.getValue()+"']";}).collect(Collectors.joining(", ", "{ ", " }")), rs.getTemplate("model"), gdm, gdm.getBeanClass(), ((GridDataProviderWPk<?>)gdm.getDataProvider()).getPk() );
    	GridData<?> gd = (GridData<?>)r.getValue()/*getParent().getAttribute("each")*/;
    	boolean isChecked = cb.isChecked();
    	int sidAttr = (int)r.getAttribute("sid"); // ((SubjSumm)gd.getBean()).getSubj_id()
    	Integer rnAttr = (Integer)r.getAttribute("rn")/*((ForEachStatus)r.getParent().getAttribute("forEachStatus")).getIndex()*/;
    	long uid = gd.getUid(); // (long)r.getAttribute("uid")
    	Checkbox _selFilterCHB = (Checkbox)/*subjSummGrid*/cb.query("checkbox#selFilterCHB"); // ?? ��������� ������ � ����� ������������ ??
    	String msg = "onCheckRow. sid="+sidAttr+", new_state:"+isChecked+", rn="+rnAttr+", uid="+uid;
    	Consumer<Long> taskToRun = (stamp) -> {
    		try {
    			//int irn = GridData.searchByUid(gridLML.getInnerList(), uid); // �� ����� �������� ������ ����� ���������� !
    			//logger.trace("t3 = {}", System.currentTimeMillis());
    			int irn = gdm.selectRow(isChecked, Generics.cast(gd)/*uid*/); // 1-� ���������������� ��������
    			//logger.trace("t4 = {}", System.currentTimeMillis());
    			boolean filtered = true;
    			if (_selFilterCHB != null) {
    				filtered = _selFilterCHB.isChecked();
    			}
    			logger.trace(concurMarker, "{} (UI)...taskToRun. After change data, before filter... irn = {}, stamp = {}, filtered = {}, _selFilterCHB = {}, _selFilterCHB.spaceOwner = {}, cb.spaceOwner = {}", msg, irn, stamp, filtered, _selFilterCHB, _selFilterCHB.getSpaceOwner(), cb.getSpaceOwner());
    			if (irn < 0) return; // ���������� �������� ����� ����������� �� ������������ ������
    			if ( filtered /*&& !isChecked*//*selFilterCHB.isChecked()*/ ) { // (����� ��������� ��������������� �� ����� ���������� ����� ���������� ��������) ��������� ������ (����� �����, ������ ���� ������� ��������� ��������� � ������� � ������ ���������); ��������� ������ ������� ������ ����������� ������, ��������� �� ����� ����������
					applyFilter(gdm, host, true, Optional.of(irn), 0L/*stamp*/); // 2-� ���������������� �������� // �� �������������� ������ applyFilter()
				}
    		} finally {
    			if ( stamp != 0L ) {
    				gdm.getModelRWLock().unlock(stamp);
    				logger.trace(concurMarker, "{} (UI)...taskToRun. WriteLock released, stamp = {}", msg, stamp);
    			}
    		}
    	};
    	//logger.trace("t2 = {}", System.currentTimeMillis());
    	dispatchGridModelLockingTask(gdm, taskToRun, msg, null, null);
    	//logger.trace("t5 = {}", System.currentTimeMillis());
    	logger.debug("onCheckRow(after). isChecked = {}, uid = {}, rn = {}, sid = {}", isChecked, uid, rnAttr, sidAttr);
    } // public void onCheckRow(ForwardEvent fev)
    
    
    /** ��� ������� �������� �� ��������� ������� "������" (�������/����� ���), ��������� ���� GridData.sel �� ���� ��������� �� ������� ������� ������.
     * ����������� ����� ��������� dispatchGridModelLockingTask � ������������ ����������� ������.
     * �������� {@link GridDataFilterableModelMan#selectAllRows(boolean) GridDataFilterableModelMan.selectAllRows()}, ����� ��������� ������.
     */
    //@Listen("onCheck=#selectAllCHB")
    @Listen("onSelectAll=#subjSummGrid") // �������������� � "onCheck=#selectAllCHB"
    public void onSelectAll(ForwardEvent fev) { // ����� ������� "������� ���" (� ������� ��������� �����)
    	boolean isChecked =  ((Checkbox)fev.getOrigin().getTarget()).isChecked();
    	MeshElement host = (MeshElement)fev.getTarget();
    	GridDataFilterableModelMan<?> gdm = (GridDataFilterableModelMan<?>)host.getAttribute("GridDataModelMan");
    	logger.debug("onSelectAll. target(host)ID: '{}', orig_targetID: '{}', selectAllCHB.isChecked = {}, GridDataModelMan ='{}', beanClass = '{}', orig_ev_class = '{}'", host.getId(), fev.getOrigin().getTarget().getId(), isChecked, gdm, gdm.getBeanClass(), fev.getOrigin().getClass().getSimpleName() );
    	String msg = "onSelectAll.  new_state: "+isChecked;
    	Consumer<Long> taskToRun = (stamp) -> {
    		logger.trace(concurMarker, "{} (UI)...taskToRun. Before do .. stamp = {}", msg, stamp);
    		gdm.selectAllRows(isChecked);
// ��������� ������ ���� ������� ��������� ��������� � ������� (�������� ������ ����� �� -> ������ ������ - ���� �� �� ���������� �����)
    		applyFilter(gdm, host, true, Optional.empty(), 0L/*stamp*/); // 2-� ���������������� �������� // �� �������������� ������ applyFilter()
    	};
    	dispatchGridModelLockingTask(gdm, taskToRun, msg, null, null);
    } // public void onSelectAll(ForwardEvent fev)
    
    
    /**  */
	@Listen("onApplyFilter=#subjSummGrid")
	public void onApplyFilter(ForwardEvent fev) { // ���� ���������������� ��� ������ ������� (�������� ��������� �������) �� ��������� ������������ �������; �� ������� ��������� ������
		Event origin = fev.getOrigin();
		logger.debug("onApplyFilter.  origin: {} '{}'.{}, target(mesh): '{}'({})", origin.getTarget().getClass().getSimpleName(), origin.getTarget().getId(), origin.getName(), fev.getTarget().getId(), fev.getTarget().getClass().getSimpleName() );
    	if (origin instanceof OpenEvent && ((OpenEvent)origin).isOpen() && origin.getTarget() instanceof Combobox) {
    		return; // ���������� ������ "onClose", � ����� �������� ������� - ��������� ������ � ������ �����������
		}
// TODO: ����� ��� ����� ��������� �� ����������� �������� (�� ���� ��� ��������� ��������� ����������, �� ���������� - �� ��������� !)
		//safeApplyFilter(origin.getTarget(), clearFilterToolBB);
		safeApplyFilter((MeshElement)fev.getTarget(), clearFilterToolBB);
		//safeApplyFilter(subjSummFilter, subjSummModelManager, subjSummGrid, clearFilterToolBB);
	} // public void onApplyFilter()
	
	
    @Listen("onClick=#clearFilterToolBB")
    public void clearFilter() { // �� ������� toolbarbutton clearFilterToolBB (��������� ������ ����������� ����� �� ������ ����������� ���������� �������)
    	logger.debug("clearFilter");
    	subjSummFilter.clear(true);
    	safeApplyFilter(subjSummFilter, subjSummModelManager, subjSummGrid, clearFilterToolBB);
    }
    
    
// ������ toolbar ����������� tabbox (��� tabpanels)
	@Wire
	private Toolbarbutton toExcelToolBB;

/* ��������: ����������� ��������� ONLINE (Progressmeter) �� async working tread
  ������ ������� (������ ������ �����������):
 1) ����������� ���������� ������ ���� (����� ������� PropertyChange, ������������ � ��� �� ������) ��������� UI � �������������� Server Push. �� ��������: ��� ������� ������� ���������� ������� UI Thread. ������ ������� � ���� ZK ("����������" UI Thread �� ������ ��������).
 2) ��� ������������� ������������ Desktop ��� SP, ������� ������ ��������� ������� UI-������. �� ��������: ������ ����� ������������, �� UI ����������� ����� ������ ����� ��� ����������. ������ ������� � ���� ZK (���������� ���������� UI).
 +3) ������������� UI-����� �� ���, � �����������. �� ������� ��������� ����������� ������, ������� ������ ����������� ������ � ��������� ������� ��� ���������� �������������� �������� ������ (�� Thread.join ��� Lock.lock).
 4) (����������� �������� �� �������� � SP) �� ������� UI-����� �� ��������, �� working thread ��������� UI �� ����� SP, � ������� ������� � ��������� UI-������. �� ��������: �� working thread ��� SP �� ������������ ������� (publish)
 5) (�����������) ��������� ���������� model list. �������� long operation ������ allSubj (��� �� ������, �� ���� ���� �������� ����������� - sel, inFilter (���������� ������) !). ������ �� ��������...
!+6) Timer (CR p.396), ������� ��������� SharedLogic. ������� �������, ��� SP (������� ��� � ������� �������� deadlock'�) !
*/
	
// ��������� ��� ����������� ����������� ��������� ZKWorkerWithTimerAndPM (tabpanel.borderlayout.south.hlayout, �.�. ��� �����)
	@Wire private Div subjSummPmHolder;
	
	
	@Listen("onClick=#toExcelToolBB")
	public void onClickToExcelToolBB() {
// FIXME: ������������������ ��� ������ ���������� ������ (grid, lmlx, holder)
		downloadToExcel(subjSummModelManager, UIUtil.meshHeaderToList(subjSummGrid), subjSummPmHolder, toExcelToolBB);
	} // public void onClickToExcelToolBB()
		
/*	
	@Listen("onClick=#toCSVToolBB")
	public void downloadToCSV() throws InterruptedException {
		logger.trace("composer.downloadToCSV.");
	} // public void downloadToCSV()
	
	@Listen("onClick=#toPDFToolBB")
	public void downloadToPDF() {
		logger.debug("composer.downloadToPDF.");
	} // public void downloadToPDF()
*/	

// FIXME:HOWTO: ��� ����������� ��� ������ �� �����-���� �������� ??? ���������� ����� ���������� �� �����-���� ��������� ?
	@Listen("onCtrlKey=grid;onCancel=grid;onCtrlKey=listbox;onCancel=listbox") // {@2}Alt+2: 50, {@#f5}Alt+F5 : 116
	public void keyListener(KeyEvent kev) {
		MeshElement grid = (MeshElement)kev.getTarget();
    	int keyCode = kev.getKeyCode();
    	logger.debug("keyListener. target: {}, id: {}, keyCode: {}", kev.getTarget(), kev.getTarget().getId(), keyCode);
    	if (keyCode == 50) { // ({@2}Alt+2: 50) ��������� ������ ������� (����������� ������������); !! ������ � ������� ����� properties �������� ������� (���� deals_grid.col.dd_rest.label) � ������ (deals_grid.col.dd_rest.width) !!
    		UIUtil.writeMeshHeaderInfo(grid);
    	} // Alt+2 -> ��������� �������� � ������� ������ ������� � ���� deals.properties
    	else if (keyCode == 116) { // ({@#f5}Alt+F5 : 116) ��������� � Excel ���������� ����-������ �����
    		Messagebox.show( "Download deals to Excel ?"
    						,"toExcel"
    						,Messagebox.OK|Messagebox.CANCEL
    						,Messagebox.QUESTION
    						,new SerializableEventListener<Event>() {
    							private static final long serialVersionUID = 2827681413520028065L;
								@Override
								public void onEvent(Event e) throws Exception {
									if (Messagebox.ON_OK.equals(e.getName())) {
										//alert("Ok, go to Excel!");
// FIXME: ������������������ ��� ������ ���������� ������ (lmlx, holder)
										downloadToExcel(subjSummModelManager, UIUtil.meshHeaderToList(grid), subjSummPmHolder, toExcelToolBB);
									}
								} // public void onEvent
    						} // SerializableEventListener
    		); // Messagebox.show
    	} // ({@#f5}Alt+F5 : 116) ��������� � Excel ���������� ����-������ �����
    	else if ( keyCode == 27 && gridModelToExcelWorker != null ) {
			//if (gridModelToExcelWorker != null && gridModelToExcelWorker/*subjSummModelManager*/.getWorkingThread() != null && gridModelToExcelWorker/*subjSummModelManager*/.getWorkingThread().isAlive()) 
			Messagebox.show( "Terminate downloading to XLSX ?" // DR p. 282
							,"Confirmation"
							,Messagebox.OK|Messagebox.CANCEL
							,Messagebox.QUESTION
							,new SerializableEventListener<Event>() {
								private static final long serialVersionUID = 1814340614195193260L;

								public void onEvent(Event e) {
									if ( Messagebox.ON_OK.equals(e.getName()) && gridModelToExcelWorker != null ) {
										logger.debug("keyListener. Termination confirmed. sizeSharedLogic: {}", (gridModelToExcelWorker==null ? null : gridModelToExcelWorker.sizeSharedLogic()) );
										//toXLSXsharedLogic.clearSharedLogic(); // �� ��������, �� ������ ����� ������ �� ���������
										gridModelToExcelWorker.cancel(false/*true*/);
									}
								}
							}
			);
		} // keyCode == 27 && gridModelToExcelWorker != null
	} // public void keyListener(KeyEvent kev)
    
} // public class SubjsPageComposer extends SelectorComposer<Component>