package com.atolcd.gis.svg.type;


public class Document extends AbstractContainer{
	
	public static String SVG_UNIT_CM = "cm";
	public static String SVG_UNIT_FT = "ft";
	public static String SVG_UNIT_IN = "in";
	public static String SVG_UNIT_M = "m";
	public static String SVG_UNIT_MM = "mm";
	public static String SVG_UNIT_PC = "pc";
	public static String SVG_UNIT_PT = "pt";
	public static String SVG_UNIT_PX = "px";
	
	public static String SVG_VERSION_1_1 = "1.1";
	public static String SVG_VERSION_1_2 = "1.2";
	
	private double height;
	private double width;
	private String units;
	private AbstractStyle style;

	public Document(){
		this.height = 0;
		this.width = 0;
		this.units = Document.SVG_UNIT_PX;
		this.style = null;
	}
	
	public double getHeight() {
		return height;
	}

	public void setHeight(double height) {
		this.height = height;
	}

	public double getWidth() {
		return width;
	}

	public void setWidth(double width) {
		this.width = width;
	}

	public String getUnits() {
		return units;
	}

	public void setUnits(String units) {
		this.units = units;
	}

	public AbstractStyle getStyle() {
		return style;
	}

	public void setStyle(AbstractStyle style) {
		this.style = style;
	}


}
