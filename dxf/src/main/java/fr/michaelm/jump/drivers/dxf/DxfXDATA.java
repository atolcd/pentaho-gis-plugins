package fr.michaelm.jump.drivers.dxf;

public class DxfXDATA {
	
	public static int GROUPCODE_XDATA_APP_NAME = 1001;
	public static int GROUPCODE_XDATA_STRING = 1000;
	public static int GROUPCODE_XDATA_BINARY = 1004;
	public static int GROUPCODE_XDATA_REAL = 1040;
	public static int GROUPCODE_XDATA_INTEGER = 1070;
	public static int GROUPCODE_XDATA_LONG = 1071;
	
	private String name;
	private int code;
	private Object value;

	public DxfXDATA(String name, int code, Object value) {
		this.name = name;
		this.code = code;
		this.value = value;
	}

	public String getName() {
		return name;
	}

	public int getCode() {
		return code;
	}

	public Object getValue() {
		return value;
	}
	

}
