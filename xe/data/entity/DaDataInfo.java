package basos.xe.data.entity;

import org.apache.commons.lang3.StringUtils;

import basos.core.SafeFormatter;
import basos.util.DaDataRS;

//import java.sql.Date;


/** Данные по организациям от сервиса https://dadata.ru. Свёрнутые (no bind) и составные (add user input).
 * В полном составе поля нужны только для записи в БД и передачи в форму.
 * Сервис не оперирует комментарием (userComment, userName), saveTime; только json в suggestParty по ИНН.
 */
public final class DaDataInfo {
	
	public static enum JsonStates { BLANC // болванка, данные не загружены (suggestParty == null)
					,FROMDB_LOADED	 	// из БД
					,FIRST_FROMRS_LOADED // из сервиса, не перегружались и не сохранялись в БД
					,RELOADED_BEFORE_FORM // загружены из БД, перезагружены из сервиса до поднятия формы (по запросу на перегрузку устаревших)
					,RELOADED_DURING_FORM  // загружены из БД, перезагружены из сервиса по кнопке на форме
	};
	
	private final String inn; // (VARCHAR2(12) NOT NULL) ИНН организации
	private /*final*/ String suggestParty; // (CLOB) Данные от сервиса в формате JSON
	private /*final*/ String userComment; // (VARCHAR2(2000) DEFAULT '') Комментарий КО
	private /*final*/ String userName; // (VARCHAR2(64) DEFAULT 'basos' NOT NULL) Сохранивший пользователь
	private /*final*/ long partyActDateLong; // (NUMBER(20, 0) NOT NULL) Дата актуальности suggestParty в формате EpochMilli
	private /*final*/ java.sql.Date partyActDate; // (DATE NOT NULL) Дата актуальности suggestParty
	private final java.util.Date saveTime; // (TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL) Дата и время сохранения в БД
	
	private /*final*/ String partyActDateStr; // 'yyyyMMdd'
	private long partyActDateBeforeLong; // сохраняем ДА инфо из БД для сравнения после перегрузки из сервиса
	private JsonStates jsonState; // статус (источник) данных
	
	
	/** Конструктор рукописный, не сгенерён, преобразования (e.g. setScale) можно делать в нём.
	 * Но типы смаппировал через @ConstructorArgs + @Arg в интерфейсе.
	 */
	public DaDataInfo(String inn, String suggestParty, String userComment, String userName, Long/*long*/ partyActDateLong,
			java.sql.Date partyActDate, java.util.Date saveTime) {
		
		this.inn = inn;
		this.suggestParty = suggestParty;
		this.userComment = userComment;
		this.userName = userName;
		this.partyActDateLong = partyActDateLong == null ? 0L : partyActDateLong.longValue();
		this.partyActDate = partyActDate;
		this.saveTime = saveTime;
		this.partyActDateStr = SafeFormatter.dateASyyyymmdd(this.partyActDate);
	}
	
	/** Болванка, содержащая только ИНН, для заполнения сервисом. */
	public static DaDataInfo getBlank(String inn) {
		return new DaDataInfo(inn, null, "", null, null, null, null/*new java.util.Date()*//*, JsonStates.BLANC*/); // RULE: saveTime ВСЕГДА заполняется на стороне БД текущим временем
	}
	
	/** Ответственность web-сервиса. */
	public void setServicePart(String suggestParty, Long/*long*/ partyActDateLong) {
		this.suggestParty = suggestParty;
		this.partyActDateLong = partyActDateLong.longValue();
		this.partyActDate = new java.sql.Date(this.partyActDateLong);
		this.partyActDateStr = SafeFormatter.dateASyyyymmdd(this.partyActDate);
	}
	
	/** Ответственность web-сервиса. partyActDate необходимо установить позже. */
	public void setServicePart(String suggestParty) {
		this.suggestParty = suggestParty;
		this.partyActDateLong = 0L;
		this.partyActDate = null;
		this.partyActDateStr = null;
	}
	
	/** Когда не сразу парсим json. */
	public void setActuality(Long/*long*/ partyActDateLong) {
		this.partyActDateLong = partyActDateLong.longValue();
		this.partyActDate = new java.sql.Date(this.partyActDateLong);
		this.partyActDateStr = SafeFormatter.dateASyyyymmdd(this.partyActDate);
	}
	
	/** Ответственность web-сервиса. */
	public void setUserInput(String userComment, String userName) {
		this.userComment = userComment;
		this.userName = userName;
	}
	
	public final String getInn() {
		return inn;
	}
	public final String getSuggestParty() {
		return suggestParty;
	}
	public final String getUserComment() {
		return userComment;
	}
	public final String getUserName() {
		return userName;
	}
	public final long getPartyActDateLong() {
		return partyActDateLong;
	}
	public final java.sql.Date getPartyActDate() {
		return partyActDate;
	}
	public final java.util.Date getSaveTime() {
		return saveTime;
	}
	public JsonStates getJsonState() {
		return jsonState;
	}
	public void setJsonState(JsonStates jsonState) {
		this.jsonState = jsonState;
	}
	
	public long getPartyActDateBeforeLong() {
		return partyActDateBeforeLong;
	}

	public void setPartyActDateBeforeLong(long partyActDateBeforeLong) {
		this.partyActDateBeforeLong = partyActDateBeforeLong;
	}
	
	public void fixPartyActDateBeforeLong() {
		this.partyActDateBeforeLong = this.partyActDateLong;
	}
	
	public String getPartyActDateStr() {
		return partyActDateStr;
	}
	
	@Override
	public String toString() {
		return "DaDataInfo [inn=" + inn + ", suggestParty=" + suggestParty + ", userComment=" + userComment
				+ ", userName=" + userName + ", partyActDateLong=" + partyActDateLong + ", partyActDate=" + partyActDate
				+ ", partyActDateStr=" + partyActDateStr + ", partyActDateBeforeLong=" + partyActDateBeforeLong
				+ ", saveTime=" + saveTime + ", jsonState=" + jsonState + "]";
	}
	
	/**  */
	// see ru\dadata\api\entity\Address.java
	public static enum FiasLevel {
        /** Страна */
        //@SerializedName("0")
        COUNTRY(0, "Страна"),

        /** Регион */
        //@SerializedName("1")
        REGION(1, "Регион"),

        /** Район */
        //@SerializedName("3")
        AREA(3, "Район"),

        /** Город */
        //@SerializedName("4")
        CITY(4, "Город"),

        /** Населенный пункт */
        //@SerializedName("6")
        SETTLEMENT(6, "Населенный пункт"),

        /** Улица */
        //@SerializedName("7")
        STREET(7, "Улица"),

        /** Дом */
        //@SerializedName("8")
        HOUSE(8, "Дом"),

        /** Иностранный или пустой */
        //@SerializedName("-1")
        FOREIGN_OR_EMPTY(-1, "Иностранный или пустой");
        
		private int code;
		private String desc;
        
		private FiasLevel(int code, String desc) {
        	this.code = code;
        	this.desc = desc;
        }
        
		public static String uncode(int code) {
			String ret = String.valueOf(code);
			for (FiasLevel el : FiasLevel.values()) {
				if (el.code == code) {
					ret = el.desc;
					break;
				}
			}
			return ret;
		}
		
		@Override
		public String toString() {
			return this.desc;
		}
		
    } // public static enum FiasLevel
	
	/**  */
	public static String uncodeStateStatus(String stCode) {
		switch (stCode) {
			case "ACTIVE" : return "Действующая";
			case "LIQUIDATING" : return "Ликвидируется";
			case "LIQUIDATED" : return "Ликвидирована";
			default: return stCode;
		}
	} // public static uncodeStateStatus(String stCode)
	
	/**  */
	public static String uncodeBranchType(String brtCode) {
		switch (brtCode) { 
			case "MAIN" : return "Головная организация";
			case "BRANCH" : return "Филиал";
			default: return brtCode;
		}
	} // public static uncodeBranchType(String brtCode)
	
	/**  */
	public static String uncodeQcGeo(int code) {
		String[] qcGeoArrDict = {"Точные координаты", "Ближайший дом", "Улица", "Населенный пункт", "Город", "Координаты не определены"};
		if ( code < 0 || code >= qcGeoArrDict.length ) {
			return String.valueOf(code);
		}
		return qcGeoArrDict[code];
	} // public static String uncodeQcGeo(int code)
	
	
	/** Перегрузить/загрузить json-данные suggestParty из REST-сервиса dadata.ru ("API Подсказок") по ИНН из поля inn.
	 * Коммент не трогаем, дату актуальности сбрасываем при успехе (парсить json будем в другом методе, там и
	 *  запишем в объект, которые сохраняем в БД), saveTime не трогаем.
	 * При неудачном обращении к сервису (нет содержательного ответа) поля объекта остаются нетронутыми.
	 * @return true при успешном непустом ответе сервиса (когда json обновился; но он м.б. тем же - не сравниваем).
	 */
	public boolean loadFromRS() {
		/*if ( StringUtils.isEmpty(inn) ) {
			logger.error("loadDaDataInfoFromRS.  inconsistent object (empty key field: inn): {}", this);
			throw new IllegalArgumentException("loadDaDataInfoFromRS.  inconsistent object (empty key field: inn): "+daDataInfo);
		}*/
		String newJson = DaDataRS.getSuggestionByInn(inn);
		//logger.debug("loadDaDataInfoFromRS. inn: {}, response: '{}'", inn, newJson);
		if ( StringUtils.isEmpty(newJson) ) {
        	return false;
		}
		this.setServicePart(newJson);
		return true;
	} // public boolean loadFromRS()
	
} // public final class DaDataInfo