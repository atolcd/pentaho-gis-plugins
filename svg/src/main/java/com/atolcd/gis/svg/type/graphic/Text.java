package com.atolcd.gis.svg.type.graphic;

import com.atolcd.gis.svg.type.AbstractGraphic;

public class Text extends AbstractGraphic{
	
	
	private double x;
	private double y;
	private String text;
	
	public Text(double x, double y, String text) throws SvgAbstractGraphicException{
		this.x = x;
		this.y = y;
		this.text = text;
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}


}
