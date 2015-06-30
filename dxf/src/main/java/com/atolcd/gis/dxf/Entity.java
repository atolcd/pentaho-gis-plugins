package com.atolcd.gis.dxf;

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

    private Geometry geometry;
    private String type;
    private String text;

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

}
