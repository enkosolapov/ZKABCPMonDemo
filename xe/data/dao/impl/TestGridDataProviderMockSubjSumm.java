package basos.xe.data.dao.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import basos.data.GridData;
import basos.data.dao.GridDataProviderWPk;
import basos.xe.data.entity.SubjSumm;

/** “естовые данные дл€ дата-модели с элементами (строками) типа GridData<SubjSumm>.
 * —писок генеритс€ в populateGridDataList() с вызовом конструктора GridData<SubjSumm>(SubjSumm.class) (бин создаЄтс€ безаргументным конструктором, который должен обеспечить тестовые данные).
 */
@javax.enterprise.context.Dependent // RequestScoped
@javax.inject.Named(value = "subjSummProvider") // !!! org.zkoss.zkplus.cdi.DelegatingVariableResolver умеет внедр€ть только именованные бины, игнорирует аннотированные как альнернативы; в \WebContent\WEB-INF\beans.xml исключаютс€ из сканировани€ все ненужные альтернативы с таким именем, должен остатьс€ только один аннотированный класс с совпадающим именем как выбранна€ реализаци€
//@javax.enterprise.inject.Alternative
public final class TestGridDataProviderMockSubjSumm extends GridDataProviderWPk<SubjSumm> implements Serializable {

	private static final long serialVersionUID = 8836187735929260905L;
	
	private static final Logger logger = LoggerFactory.getLogger(TestGridDataProviderMockSubjSumm.class);
	
    /*// не работает...
    public void processBeans(@javax.enterprise.event.Observes javax.enterprise.inject.spi.ProcessBean<?> event) {
    	javax.enterprise.inject.spi.AnnotatedMethod<?> info;
        if (event instanceof javax.enterprise.inject.spi.ProcessProducerMethod) {
            info = ((javax.enterprise.inject.spi.ProcessProducerMethod<?, ?>) event).getAnnotatedProducerMethod();
            //.getJavaMember()
            logger.trace("processBeans. producer_method_name = {}", info.getJavaMember().getName());
        } else logger.trace("processBeans. other_event: {}", event.getClass().getSimpleName());
    }*/
    
	/** PK изначально не задан. “ип бина - SubjSumm.
	 * @param cnt  оличество строк в дата-модели.
	 */
	public TestGridDataProviderMockSubjSumm(int cnt) {
		// FIXME: проверка корректности параметра !!!
		super(SubjSumm.class); // http://stackoverflow.com/questions/260666/can-an-abstract-class-have-a-constructor
		this.totalRowCount = cnt;
		logger.trace("instantiate TestGridDataProviderMockSubjSumm. cnt  = {}", cnt);
		//setPk("subj_id", SubjSumm.getCompareBySubjId());
		setPk(null, null);
	}
	
	/** ѕо умолчанию модель содержит 4444 строк. */
	public TestGridDataProviderMockSubjSumm() { this(4444); }
	
	/** —писок генеритс€ с вызовом конструктора GridData<SubjSumm>(SubjSumm.class) (бин создаЄтс€ безаргументным конструктором, который должен обеспечить тестовые данные).  ол-во строк задаЄтс€ в конструкторе. */
	protected final List<GridData<SubjSumm>> populateGridDataList() {
		List<GridData<SubjSumm>> locGridDataArrayList = new ArrayList<>(this.totalRowCount);
		for (int i = 0; i < this.totalRowCount; i++) locGridDataArrayList.add(new GridData<SubjSumm>(this.beanClass));
		return locGridDataArrayList;
	};
	
	@Override
	protected List<GridData<SubjSumm>> selectRange(String fieldName, Object key) {
		throw new UnsupportedOperationException("selectRange unsupported by TestGridDataProviderMockSubjSumm at all.");
	}
	
} // public class TestGridDataProviderMockSubjSumm implements GridDataProvider