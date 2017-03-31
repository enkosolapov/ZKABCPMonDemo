package basos.zkui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.zkoss.zk.ui.AbstractComponent;


/** Реализация поумолчательных поведенческих методов интерфейса AbstractComponentBehaveUtil для компонента basos.zkui.BetweenFilterMacro
 * Singleton, use getInstance().
 * @author basos
 */
@javax.enterprise.context.ApplicationScoped
@javax.inject.Named("BetweenFilterMacro")
public class BetweenFilterMacroDefBehaveUtil extends AbstractComponentBehaveUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(BetweenFilterMacroDefBehaveUtil.class);
	
	private static BetweenFilterMacroDefBehaveUtil instance;
	
	protected BetweenFilterMacroDefBehaveUtil() {
        super();
        logger.trace("instantiate BetweenFilterMacroDefBehaveUtil");
    };
	
	/** Singleton */
	public static synchronized BetweenFilterMacroDefBehaveUtil getInstance() {
		if ( instance == null ) {
			instance = new BetweenFilterMacroDefBehaveUtil(); //(BetweenFilterMacroDefBehaveUtil)AbstractComponentBehaveUtil.newInstance(BetweenFilterMacroDefBehaveUtil.class);
		}
		return instance;
	}
	
	/** fieldName = removeEnd(compId, "Fltr") */
	@Override
	public String getCorrespondingFieldName(AbstractComponent ac) {
		return AbstractComponentBehaveUtil.getCorrespondingFieldNameAsIdWithoutSuffix(ac, "Fltr"); // *Fltr // StringUtils.removeEnd(compId, "Fltr")
	}
	
	/** {@inheritDoc} {@link BetweenFilterMacro#clear()} */
	@Override
	public void clear(AbstractComponent ac) {
		((BetweenFilterMacro<?,?>)ac).clear();
	}
	
	/** {@inheritDoc} {@link BetweenFilterMacro#isEmpty()} */
	@Override
	public boolean isEmpty(AbstractComponent ac) {
		return ((BetweenFilterMacro<?,?>)ac).isEmpty();
	}
	
	/** {@inheritDoc} {@link BetweenFilterMacro#getText()} */
	@Override
	public String getText(AbstractComponent ac) {
		return ((BetweenFilterMacro<?,?>)ac).getText();
	}
	
	/** @see BetweenFilterMacro#setDisabled(boolean) BetweenFilterMacro.setDisabled() */
	@Override
	public void disable(AbstractComponent ac, boolean disable) {
		((BetweenFilterMacro<?,?>)ac).setDisabled(disable);
	}
	
	/** @see BetweenFilterMacro#getValue() */
	@SuppressWarnings("unchecked")
	@Override
	public Object getValue(AbstractComponent ac) {
		return ((BetweenFilterMacro<?,Comparable<? super Object>>)ac).getValue();
	}
	
	/** Проверка величины на соответствие диапазону значений компонента.
	 * @return Истина для пустого (поумолчательного) значения компонента, иначе ложь для пустого значения (null) поля, иначе истина для непустого значения поля принадлежащего непустому диапазону, иначе ложь.
	 * @see BetweenFilterMacro#isEmpty()
	 * @see BetweenFilterMacro#isValBetween(Comparable) BetweenFilterMacro.isValBetween()
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean theFilter(Object componentValue, Object otherValue) {
		BetweenFilterMacro<?,Comparable<? super Object>>.PairedValue compVal = (BetweenFilterMacro<?,Comparable<? super Object>>.PairedValue)componentValue;
		return compVal.isEmpty() ? true : compVal.isValBetween((Comparable<? super Object>)otherValue);
	}
	
	/** <b>Заглушка, возвращающая всегда false.</b> {@inheritDoc}
	 * @return Всегда ложь.
	 */
	@Override
	public boolean exactMatch(Object componentValue, Object otherValue) {
		return false;
	}
	
} // public class BetweenFilterMacroDefBehaveUtil extends AbstractComponentBehaveUtil