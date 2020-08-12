package com.atolcd.gis.svg.type.graphic;

import com.atolcd.gis.svg.type.AbstractGraphic;

public class Image extends AbstractGraphic{
	private double height;
	private double width;
	private double x;
	private double y;
	private String href;
	
	public Image(double x, double y, double width, double height, String href) throws SvgAbstractGraphicException{
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.href = this.checkHref(href);
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

	public String getHref() {
		return href;
	}

	public void setHref(String href) throws SvgAbstractGraphicException {
		this.href = this.checkHref(href);
	}

	private String checkHref(String href) throws SvgAbstractGraphicException{
		
		if(href == null){
			throw new SvgAbstractGraphicException("Href should not be null");
		}

		return href;
	}

}
