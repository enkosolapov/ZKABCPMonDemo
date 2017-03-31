package basos.xe.data.entity;

import org.apache.commons.lang3.StringUtils;

import basos.core.SafeFormatter;
import basos.util.DaDataRS;

//import java.sql.Date;


/** ������ �� ������������ �� ������� https://dadata.ru. �������� (no bind) � ��������� (add user input).
 * � ������ ������� ���� ����� ������ ��� ������ � �� � �������� � �����.
 * ������ �� ��������� ������������ (userComment, userName), saveTime; ������ json � suggestParty �� ���.
 */
public final class DaDataInfo {
	
	public static enum JsonStates { BLANC // ��������, ������ �� ��������� (suggestParty == null)
					,FROMDB_LOADED	 	// �� ��
					,FIRST_FROMRS_LOADED // �� �������, �� ������������� � �� ����������� � ��
					,RELOADED_BEFORE_FORM // ��������� �� ��, ������������� �� ������� �� �������� ����� (�� ������� �� ���������� ����������)
					,RELOADED_DURING_FORM  // ��������� �� ��, ������������� �� ������� �� ������ �� �����
	};
	
	private final String inn; // (VARCHAR2(12) NOT NULL) ��� �����������
	private /*final*/ String suggestParty; // (CLOB) ������ �� ������� � ������� JSON
	private /*final*/ String userComment; // (VARCHAR2(2000) DEFAULT '') ����������� ��
	private /*final*/ String userName; // (VARCHAR2(64) DEFAULT 'basos' NOT NULL) ����������� ������������
	private /*final*/ long partyActDateLong; // (NUMBER(20, 0) NOT NULL) ���� ������������ suggestParty � ������� EpochMilli
	private /*final*/ java.sql.Date partyActDate; // (DATE NOT NULL) ���� ������������ suggestParty
	private final java.util.Date saveTime; // (TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL) ���� � ����� ���������� � ��
	
	private /*final*/ String partyActDateStr; // 'yyyyMMdd'
	private long partyActDateBeforeLong; // ��������� �� ���� �� �� ��� ��������� ����� ���������� �� �������
	private JsonStates jsonState; // ������ (��������) ������
	
	
	/** ����������� ����������, �� �������, �������������� (e.g. setScale) ����� ������ � ��.
	 * �� ���� ����������� ����� @ConstructorArgs + @Arg � ����������.
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
	
	/** ��������, ���������� ������ ���, ��� ���������� ��������. */
	public static DaDataInfo getBlank(String inn) {
		return new DaDataInfo(inn, null, "", null, null, null, null/*new java.util.Date()*//*, JsonStates.BLANC*/); // RULE: saveTime ������ ����������� �� ������� �� ������� ��������
	}
	
	/** ��������������� web-�������. */
	public void setServicePart(String suggestParty, Long/*long*/ partyActDateLong) {
		this.suggestParty = suggestParty;
		this.partyActDateLong = partyActDateLong.longValue();
		this.partyActDate = new java.sql.Date(this.partyActDateLong);
		this.partyActDateStr = SafeFormatter.dateASyyyymmdd(this.partyActDate);
	}
	
	/** ��������������� web-�������. partyActDate ���������� ���������� �����. */
	public void setServicePart(String suggestParty) {
		this.suggestParty = suggestParty;
		this.partyActDateLong = 0L;
		this.partyActDate = null;
		this.partyActDateStr = null;
	}
	
	/** ����� �� ����� ������ json. */
	public void setActuality(Long/*long*/ partyActDateLong) {
		this.partyActDateLong = partyActDateLong.longValue();
		this.partyActDate = new java.sql.Date(this.partyActDateLong);
		this.partyActDateStr = SafeFormatter.dateASyyyymmdd(this.partyActDate);
	}
	
	/** ��������������� web-�������. */
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
        /** ������ */
        //@SerializedName("0")
        COUNTRY(0, "������"),

        /** ������ */
        //@SerializedName("1")
        REGION(1, "������"),

        /** ����� */
        //@SerializedName("3")
        AREA(3, "�����"),

        /** ����� */
        //@SerializedName("4")
        CITY(4, "�����"),

        /** ���������� ����� */
        //@SerializedName("6")
        SETTLEMENT(6, "���������� �����"),

        /** ����� */
        //@SerializedName("7")
        STREET(7, "�����"),

        /** ��� */
        //@SerializedName("8")
        HOUSE(8, "���"),

        /** ����������� ��� ������ */
        //@SerializedName("-1")
        FOREIGN_OR_EMPTY(-1, "����������� ��� ������");
        
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
			case "ACTIVE" : return "�����������";
			case "LIQUIDATING" : return "�������������";
			case "LIQUIDATED" : return "�������������";
			default: return stCode;
		}
	} // public static uncodeStateStatus(String stCode)
	
	/**  */
	public static String uncodeBranchType(String brtCode) {
		switch (brtCode) { 
			case "MAIN" : return "�������� �����������";
			case "BRANCH" : return "������";
			default: return brtCode;
		}
	} // public static uncodeBranchType(String brtCode)
	
	/**  */
	public static String uncodeQcGeo(int code) {
		String[] qcGeoArrDict = {"������ ����������", "��������� ���", "�����", "���������� �����", "�����", "���������� �� ����������"};
		if ( code < 0 || code >= qcGeoArrDict.length ) {
			return String.valueOf(code);
		}
		return qcGeoArrDict[code];
	} // public static String uncodeQcGeo(int code)
	
	
	/** �����������/��������� json-������ suggestParty �� REST-������� dadata.ru ("API ���������") �� ��� �� ���� inn.
	 * ������� �� �������, ���� ������������ ���������� ��� ������ (������� json ����� � ������ ������, ��� �
	 *  ������� � ������, ������� ��������� � ��), saveTime �� �������.
	 * ��� ��������� ��������� � ������� (��� ��������������� ������) ���� ������� �������� �����������.
	 * @return true ��� �������� �������� ������ ������� (����� json ���������; �� �� �.�. ��� �� - �� ����������).
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