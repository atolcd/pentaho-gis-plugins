package com.atolcd.gis.gpx.type;

import com.vividsolutions.jts.geom.GeometryFactory;

public abstract class AbstractSpatialElement extends AbstractElement{

	private static GeometryFactory geometryFactory = new GeometryFactory();
	
	private String comment;
	private String source;
	private String type;
	
	public AbstractSpatialElement(){
		super();
		this.comment = null;
		this.source = null;
		this.type = null;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public static GeometryFactory getGeometryFactory() {
		return geometryFactory;
	}

}
