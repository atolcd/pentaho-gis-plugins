package com.atolcd.gis.svg.type;

public interface IElement {
	
	public void setId(String id);
	public void setTitle(String title);
	public void setDescription(String description);
	public void setSvgStyle(String svgStyle);
	public void setCssClass(String cssClass);
	public void setTransform(String transform);
	
	public String getId();
	public String getTitle();
	public String getDescription();
	public String getSvgStyle();
	public String getCssClass();
	public String getTransform();

}
