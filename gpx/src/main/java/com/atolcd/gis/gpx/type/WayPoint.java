package com.atolcd.gis.gpx.type;

import java.util.GregorianCalendar;

import com.atolcd.gis.gpx.ISpatialElement;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

public class WayPoint extends AbstractSpatialElement implements ISpatialElement{

	private Coordinate coordinate;
	private GregorianCalendar time;
	private String symbol;
	
	public WayPoint(Coordinate coordinate) throws WayPointException{
		this.coordinate = checkCoordinate(coordinate);
		this.time = null;
		this.symbol = null;
	}

	public Geometry getGeometry() {

		Geometry geometry = getGeometryFactory().createPoint(this.coordinate);
		geometry.setSRID(4326);
		return geometry;

	}
	
	public Double getLongitude(){
		return coordinate.x;
	}
	
	public Double getLatitude(){
		return coordinate.y;
	}
	
	public Double getElevation(){
		return coordinate.z;
	}

	public String getSymbol() {
		return symbol;
	}

	public void setSymbol(String symbol) {
		this.symbol = symbol;
	}

	public GregorianCalendar getTime() {
		return time;
	}

	public void setTime(GregorianCalendar time) {
		this.time = time;
	}

	protected Coordinate getCoordinate(){
		return this.coordinate;
	}
	
	private Coordinate checkCoordinate(Coordinate coordinate) throws WayPointException{
		
		if(coordinate == null){
			throw new WayPointException("Coordinates can not be null");
		}
		
		if(coordinate.x > 180 || coordinate.x < -180 ||coordinate.y > 90 ||coordinate.y < -90){
			throw new WayPointException("Wrong coordinates (" + coordinate.x + "|" + coordinate.y + ") for WGS84 system");
		}
		
		return coordinate;
	}

	@SuppressWarnings("serial")
	public class WayPointException extends Exception {
		
	    public WayPointException(String message) {
	        super(message);
	    }
	    
	}

}
