package com.atolcd.gis.gpx.type;

import java.util.ArrayList;
import java.util.List;

import com.atolcd.gis.gpx.ISpatialElement;
import com.atolcd.gis.gpx.type.WayPoint.WayPointException;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;

public class Route extends AbstractSpatialElement implements ISpatialElement{
	
	private Integer number;
	private List<WayPoint> points;

	public Route(){
		this.number = null;
		this.points = new ArrayList<WayPoint>();
	}
	
	public Route(LineString geometry) throws WayPointException{
		
		this.number = null;
		this.points = new ArrayList<WayPoint>();
		if(geometry != null && !geometry.isEmpty()){
			
			for(Coordinate coordinate : geometry.getCoordinates()){
				this.points.add(new WayPoint(coordinate));
			}

		}
		
	}

	public Route(List<WayPoint> points){
		
		this.number = null;
		if(points == null){
			this.points = new ArrayList<WayPoint>();
		}else{
			this.points = points;
		}
	}

	public Integer getNumber() {
		return number;
	}

	public void setNumber(Integer number) throws RouteException {
		this.number = checkNumber(number);
	}

	@SuppressWarnings("unchecked")
	public Geometry getGeometry() {
		
		CoordinateList coordinates = new CoordinateList();
		for(WayPoint wayPoint : this.points){
			coordinates.add(wayPoint.getCoordinate());
		}
		
		if(coordinates.size() > 1){
			Geometry geometry = getGeometryFactory().createLineString(coordinates.toCoordinateArray());
			geometry.setSRID(4326);
			return geometry;
		}else{
			return null;
		}
	}

	public List<WayPoint> getPoints() {
		return points;
	}
		
	private Integer checkNumber(Integer number) throws RouteException{
		
		if(number != null && number < 0){
			throw new RouteException("Route number should be positive");
		}

		return number;
	}
	
	@SuppressWarnings("serial")
	public class RouteException extends Exception {
		
	    public RouteException(String message) {
	        super(message);
	    }
	    
	}


}
