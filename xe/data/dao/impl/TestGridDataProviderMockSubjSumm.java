package basos.xe.data.dao.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import basos.data.GridData;
import basos.data.dao.GridDataProviderWPk;
import basos.xe.data.entity.SubjSumm;

/** Тестовые данные для дата-модели с элементами (строками) типа GridData<SubjSumm>.
 * Список генерится в populateGridDataList() с вызовом конструктора GridData<SubjSumm>(SubjSumm.class) (бин создаётся безаргументным конструктором, который должен обеспечить тестовые данные).
 */
@javax.enterprise.context.Dependent // RequestScoped
@javax.inject.Named(value = "subjSummProvider") // !!! org.zkoss.zkplus.cdi.DelegatingVariableResolver умеет внедрять только именованные бины, игнорирует аннотированные как альнернативы; в \WebContent\WEB-INF\beans.xml исключаются из сканирования все ненужные альтернативы с таким именем, должен остаться только один аннотированный класс с совпадающим именем как выбранная реализация
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
    
	/** PK изначально не задан. Тип бина - SubjSumm.
	 * @param cnt Количество строк в дата-модели.
	 */
	public TestGridDataProviderMockSubjSumm(int cnt) {
		// FIXME: проверка корректности параметра !!!
		super(SubjSumm.class); // http://stackoverflow.com/questions/260666/can-an-abstract-class-have-a-constructor
		this.totalRowCount = cnt;
		logger.trace("instantiate TestGridDataProviderMockSubjSumm. cnt  = {}", cnt);
		//setPk("subj_id", SubjSumm.getCompareBySubjId());
		setPk(null, null);
	}
	
	/** По умолчанию модель содержит 4444 строк. */
	public TestGridDataProviderMockSubjSumm() { this(4444); }
	
	/** Список генерится с вызовом конструктора GridData<SubjSumm>(SubjSumm.class) (бин создаётся безаргументным конструктором, который должен обеспечить тестовые данные). Кол-во строк задаётся в конструкторе. */
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