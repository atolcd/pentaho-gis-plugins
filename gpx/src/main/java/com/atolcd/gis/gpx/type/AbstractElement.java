package com.atolcd.gis.gpx.type;

public abstract class AbstractElement {

	private String name;
	private String description;
	
	public AbstractElement(){
		this.name = null;
		this.description = null;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}

}
