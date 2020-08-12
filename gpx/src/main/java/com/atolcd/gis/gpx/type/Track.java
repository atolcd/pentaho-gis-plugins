package com.atolcd.gis.gpx.type;

import java.util.ArrayList;
import java.util.List;

import com.atolcd.gis.gpx.ISpatialElement;
import com.atolcd.gis.gpx.type.WayPoint.WayPointException;
import com.vividsolutions.jts.geom.CoordinateList;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

public class Track extends AbstractSpatialElement implements ISpatialElement{
	
	private Integer number;
	private List<TrackSegment> segments;
	
	public Track(){
		this.number = null;
		this.segments = new ArrayList<TrackSegment>();
	}
	
	public Track(MultiLineString geometry) throws WayPointException{
		
		this.number = null;
		this.segments = new ArrayList<TrackSegment>();
		if(geometry != null && !geometry.isEmpty()){
			
			for(int iGeom = 0; iGeom < geometry.getNumGeometries(); iGeom++){
				this.segments.add(new TrackSegment((LineString) geometry.getGeometryN(iGeom)));
			}
		}
	}

	public Track(List<TrackSegment> segments){
		
		this.number = null;
		if(segments == null){
			this.segments = new ArrayList<TrackSegment>();
		}else{
			this.segments = segments;
		}
		
		this.number = null;
	}

	public List<TrackSegment> getSegments() {
		return segments;
	}

	public Integer getNumber() {
		return number;
	}

	public void setNumber(Integer number) throws TrackException {
		this.number = checkNumber(number);
	}

	@SuppressWarnings("unchecked")
	public Geometry getGeometry() {
		
		List<LineString> lineStrings = new ArrayList<LineString>();
		for(TrackSegment trackSegment : this.segments){
			
			CoordinateList coordinates = new CoordinateList();
			for(WayPoint wayPoint : trackSegment.getPoints()){
				coordinates.add(wayPoint.getCoordinate());
			}
			
			if(coordinates.size() > 1){
				lineStrings.add(getGeometryFactory().createLineString(coordinates.toCoordinateArray()));
			}

		}
		
		if(lineStrings.size() > 0){
			
			Geometry geometry = getGeometryFactory().createMultiLineString(lineStrings.toArray(new LineString[lineStrings.size()]));
			geometry.setSRID(4326);
			return geometry;
			
		}else{
			return null;
		}
	
	}
	
	private Integer checkNumber(Integer number) throws TrackException{
		
		if(number != null && number < 0){
			throw new TrackException("Track number should be positive");
		}

		return number;
	}
	
	
	@SuppressWarnings("serial")
	public class TrackException extends Exception {
		
	    public TrackException(String message) {
	        super(message);
	    }
	    
	}

}
