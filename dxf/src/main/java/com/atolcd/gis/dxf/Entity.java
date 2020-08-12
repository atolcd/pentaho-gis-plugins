package com.atolcd.gis.dxf;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Geometry;

public class Entity {

    public static String TYPE_TEXT = "TEXT";
    public static String TYPE_MTEXT = "MTEXT";
    public static String TYPE_LINE = "LINE";
    public static String TYPE_POLYLINE = "POLYLINE";
    public static String TYPE_LWPOLYLINE = "LWPOLYLINE";
    public static String TYPE_CIRCLE = "CIRCLE";
    public static String TYPE_ELLIPSE = "ELLIPSE";
    public static String TYPE_ARC = "ARC";
    public static String TYPE_BLOCK = "BLOCK";
	public static String TYPE_POINT	= "POINT";

    private String id;
	private Geometry geometry;
	private String type;
	private String text;
	private List<ExtendedData> extendedData;

    public Entity(String id, Geometry geometry,String type, String text){
		this.id = id;
		this.geometry = geometry;
		this.type = type;
		this.text = text;
		this.extendedData = new ArrayList<ExtendedData>();
	}

    public Geometry getGeometry() {
        return geometry;
    }

    public void setGeometry(Geometry geometry) {
        this.geometry = geometry;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<ExtendedData> getExtendedData() {
		return extendedData;
	}

	public void AddExtendedData(ExtendedData extendedData){
		this.getExtendedData().add(extendedData);
	}
	
	public void AddExtendedData(String name, Class<?> type, Object value){
		this.getExtendedData().add(new ExtendedData(name, type, value));
	}

	public String getId() {
		return id;
	}

}
