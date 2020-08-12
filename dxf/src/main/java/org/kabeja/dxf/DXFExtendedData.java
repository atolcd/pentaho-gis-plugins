package org.kabeja.dxf;

public class DXFExtendedData {

	private String name;
	private Class<?> type;
	private Object value;
	
	public DXFExtendedData(String name) {
		this.name = name;
		this.type = null;
		this.value = null;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Class<?> getType() {
		return type;
	}

	public void setType(Class<?> type) {
		this.type = type;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}
	
}
