package com.atolcd.gis.svg.type;



public abstract class AbstractElement implements IElement{

	private String id;
	private String title;
	private String description;
	private String svgStyle;
	private String cssClass;
	private String transform;
	
	public AbstractElement() {
		this.id = null;
		this.title = null;
		this.description = null;
		this.svgStyle = null;
		this.cssClass = null;
		this.transform = null;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getSvgStyle() {
		return svgStyle;
	}

	public void setSvgStyle(String svgStyle) {
		this.svgStyle = svgStyle;
	}

	public String getCssClass() {
		return cssClass;
	}

	public void setCssClass(String cssClass) {
		this.cssClass = cssClass;
	}

	public String getTransform() {
		return transform;
	}

	public void setTransform(String transform) {
		this.transform = transform;
	}


}
