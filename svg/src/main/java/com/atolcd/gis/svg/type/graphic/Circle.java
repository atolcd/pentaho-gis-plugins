package com.atolcd.gis.svg.type.graphic;

import com.atolcd.gis.svg.type.AbstractGraphic;

public class Circle extends AbstractGraphic{

	private double x;
	private double y;
	private double radius;
		
	public Circle(double x, double y, double radius) {
		this.x = x;
		this.y = y;
		this.radius = radius;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public double getRadius() {
		return radius;
	}

}
