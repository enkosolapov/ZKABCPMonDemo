package basos.xe.abcpmon.zkui;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.zkoss.lang.Generics;
import org.zkoss.zk.ui.WebApp;
import org.zkoss.zk.ui.util.WebAppInit;
import org.zkoss.zkplus.cdi.CDIUtil;

import basos.zkui.AbstractComponentBehaveUtil;


/** ��������� ���� ������ ������ ���������� ��� ��� ������������� (application scope initialization logic); ������ �������� �������� � zk.xml.
 * ������ ��������������� ������ ��� ������-��������� � �������� ������� � ������� (������ ����������) "defBehavMap";
 *  ������ ���������� � ����������� �������� (���������� �� FilterableGridDataComposer) �������� �������, �����
 *  ����� �������������� ������ (��������� ����� ��� ��������������� ���� ������-��������).
 */
public class AbcpmonWebAppInit implements WebAppInit {
	
	private static final Logger logger = LoggerFactory.getLogger(AbcpmonWebAppInit.class);
	
	
 	/** ��������� ������-��������� �� ��������� ��������� ������������������ ������, �������������� SimpleName
 	 * ������ �������� (�������� "BetweenFilterMacro" ��� "Combobox"); ������� �������������� ���������� �����
 	 * ���������, �������� ������ (����� �� ����� � @Named) � beans.xml; ����� ����������� ����������� ��������
 	 * @Alternative ��� ������ BeanManager.resolve()
 	 */
    private Map<String, AbstractComponentBehaveUtil> createDefBehavMap() {
// TODO: ����������� Multiton ��� �������� ����� @Produces
	    javax.enterprise.inject.spi.BeanManager _beanMgr;
		_beanMgr = CDIUtil.getBeanManager();
		final java.util.Set<javax.enterprise.inject.spi.Bean<?>> beans = _beanMgr.getBeans(AbstractComponentBehaveUtil.class);
		logger.trace("beans.size for AbstractComponentBehaveUtil: {}", beans.size());
		Map<String, AbstractComponentBehaveUtil> defBehavMap = new HashMap<>((int)(beans.size()/0.5), 0.5f);
		for (javax.enterprise.inject.spi.Bean<?> bean : beans) {
			//behavMap.put(bean.getName(), AbstractComponentBehaveUtil.newInstance( (Class<AbstractComponentBehaveUtil>)bean.getBeanClass()) );
// ������ ������ ����� ��������� ����������� � ��������� ���� �������� �� ����� � ���������
			defBehavMap.put(bean.getName(), AbstractComponentBehaveUtil.newInstance( Generics.cast(bean.getBeanClass())) );
			logger.trace("beans:  beanClassSimpleName: {}, name: {}, scopeSimpleName: {}, isAlternative: {}, isNullable: {}, injectionPoints_size: {}, qualifiers_size: {}, qualifiers: {}, stereotypes_size: {}, types_size: {}, types: {}", bean.getBeanClass().getSimpleName(), bean.getName(), bean.getScope().getSimpleName(), bean.isAlternative(), bean.isNullable(), bean.getInjectionPoints().size(), bean.getQualifiers().size(), bean.getQualifiers().stream().map((type)->type.getClass().getSimpleName()).collect(Collectors.joining(",", "{", "}")), bean.getStereotypes().size(), bean.getTypes().size(), bean.getTypes().stream().map((type)->((Class<?>)type).getSimpleName()).collect(Collectors.joining(",", "{", "}")) );
		}
		return defBehavMap;
    } // 
	
	@Override
	public void init(WebApp wapp) throws Exception {
		logger.debug("AbcpmonWebAppInit.init");
// ������ ��������������� ������ ��� ������-��������� � �������� ������� � ������� (������ ����������) "defBehavMap"; ������ ���������� � ����������� �������� (���������� �� FilterableGridDataComposer) �������� �������, ����� ����� �������������� ������ (��������� ����� ��� ��������������� ���� ������-��������)
		wapp.setAttribute("defBehavMap", createDefBehavMap());
	}
	
} // public class AbcpmonWebAppInit implements WebAppInit