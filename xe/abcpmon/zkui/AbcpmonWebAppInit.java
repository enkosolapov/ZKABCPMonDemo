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


/** Размещаем свою логику уровня приложения при его инициализации (application scope initialization logic); данный листенер прописан в zk.xml.
 * Создаём поумолчательную логику для фильтр-контролов и помещаем таблицу в атрибут (уровня приложения) "defBehavMap";
 *  каждый контроллер с композитным фильтром (порождённый от FilterableGridDataComposer) копирует таблицу, далее
 *  может переопределять логику (подменять класс для соответсвующего типа фильтр-контрола).
 */
public class AbcpmonWebAppInit implements WebAppInit {
	
	private static final Logger logger = LoggerFactory.getLogger(AbcpmonWebAppInit.class);
	
	
 	/** Поведение фильтр-контролов по умолчанию заполняем проаннотированными бинами, поименованными SimpleName
 	 * класса контрола (например "BetweenFilterMacro" или "Combobox"); выбором альтернативных реализаций можно
 	 * управлять, исключая лишние (дубли по имени в @Named) в beans.xml; можно попробовать реализовать резолвер
 	 * @Alternative при помощи BeanManager.resolve()
 	 */
    private Map<String, AbstractComponentBehaveUtil> createDefBehavMap() {
// TODO: реализовать Multiton или вызывать через @Produces
	    javax.enterprise.inject.spi.BeanManager _beanMgr;
		_beanMgr = CDIUtil.getBeanManager();
		final java.util.Set<javax.enterprise.inject.spi.Bean<?>> beans = _beanMgr.getBeans(AbstractComponentBehaveUtil.class);
		logger.trace("beans.size for AbstractComponentBehaveUtil: {}", beans.size());
		Map<String, AbstractComponentBehaveUtil> defBehavMap = new HashMap<>((int)(beans.size()/0.5), 0.5f);
		for (javax.enterprise.inject.spi.Bean<?> bean : beans) {
			//behavMap.put(bean.getName(), AbstractComponentBehaveUtil.newInstance( (Class<AbstractComponentBehaveUtil>)bean.getBeanClass()) );
// просто создаём новый экземпляр рефлективно и назначаем типу контрола по имени в аннотации
			defBehavMap.put(bean.getName(), AbstractComponentBehaveUtil.newInstance( Generics.cast(bean.getBeanClass())) );
			logger.trace("beans:  beanClassSimpleName: {}, name: {}, scopeSimpleName: {}, isAlternative: {}, isNullable: {}, injectionPoints_size: {}, qualifiers_size: {}, qualifiers: {}, stereotypes_size: {}, types_size: {}, types: {}", bean.getBeanClass().getSimpleName(), bean.getName(), bean.getScope().getSimpleName(), bean.isAlternative(), bean.isNullable(), bean.getInjectionPoints().size(), bean.getQualifiers().size(), bean.getQualifiers().stream().map((type)->type.getClass().getSimpleName()).collect(Collectors.joining(",", "{", "}")), bean.getStereotypes().size(), bean.getTypes().size(), bean.getTypes().stream().map((type)->((Class<?>)type).getSimpleName()).collect(Collectors.joining(",", "{", "}")) );
		}
		return defBehavMap;
    } // 
	
	@Override
	public void init(WebApp wapp) throws Exception {
		logger.debug("AbcpmonWebAppInit.init");
// создаём поумолчательную логику для фильтр-контролов и помещаем таблицу в атрибут (уровня приложения) "defBehavMap"; каждый контроллер с композитным фильтром (порождённый от FilterableGridDataComposer) копирует таблицу, далее может переопределять логику (подменять класс для соответсвующего типа фильтр-контрола)
		wapp.setAttribute("defBehavMap", createDefBehavMap());
	}
	
} // public class AbcpmonWebAppInit implements WebAppInit