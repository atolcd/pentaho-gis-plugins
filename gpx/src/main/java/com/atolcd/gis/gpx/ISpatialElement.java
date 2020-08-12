package com.atolcd.gis.gpx;

import com.vividsolutions.jts.geom.Geometry;

public interface ISpatialElement extends IElement{

	public void setComment(String comment);
	public void setSource(String source);
	public void setType(String type);
	
	public String getComment();
	public String getSource();
	public String getType();
	
	public Geometry getGeometry();


}
