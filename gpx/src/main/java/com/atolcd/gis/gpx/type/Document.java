package com.atolcd.gis.gpx.type;

import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class Document {
	
	public static String GPX_CREATOR_DEFAULT = "Atol Conseils et Developpements GPX Library";
	public static String GPX_VERSION_1_0 = "1.0";
	public static String GPX_VERSION_1_1 = "1.1";
	
	private String version;
	private String creator;
	private Metadata metadata;
	private List<WayPoint> wayPoints;
	private List<Track> tracks;
	private List<Route> routes;
	
	public Document(){
		this.version= GPX_VERSION_1_1;
		this.creator = GPX_CREATOR_DEFAULT;
		this.metadata = null;
		this.wayPoints = new ArrayList<WayPoint>();
		this.tracks = new ArrayList<Track>();
		this.routes = new ArrayList<Route>();
	}
		
	public String getVersion() {
		return version;
	}
	
	public void setVersion(String version) throws GpxException{
		this.version = checkVersion(version);
	}
	
	public String getCreator() {
		return creator;
	}
	
	public void setCreator(String creator) throws GpxException{
		this.creator = checkCreator(creator);
	}
	
	public Metadata getMetadata() {
		return metadata;
	}
	
	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}
	
	public List<WayPoint> getWayPoints() {
		return wayPoints;
	}
	
	public List<Track> getTracks() {
		return tracks;
	}

	public List<Route> getRoutes() {
		return routes;
	}

	private String checkVersion(String version) throws GpxException{
		
		if(!version.equals(GPX_VERSION_1_0) && !version.equals(GPX_VERSION_1_1)){

			throw new GpxException("Only 1.0 and 1.1 versions are allowed");
		
		}

		return version;
	}
	
	private String checkCreator(String creator) throws GpxException{
		
		if(creator == null){
			throw new GpxException("Creator should not be null");
		}

		return creator;
	}
	
	
	@SuppressWarnings("serial")
	public class GpxException extends Exception {
		
	    public GpxException(String message) {
	        super(message);
	    }
	    
	}
	
	public Envelope getExtent(){
		
		List<Geometry> geometries = new ArrayList<Geometry>();
		for(WayPoint wayPoint: wayPoints){
			Geometry geometry = wayPoint.getGeometry();
			if(geometry != null && !geometry.isEmpty()){
				geometries.add(wayPoint.getGeometry());
			}
		}
		
		for(Route route: routes){
			Geometry geometry = route.getGeometry();
			if(geometry != null && !geometry.isEmpty()){
				geometries.add(route.getGeometry());
			}
		}
		
		for(Track track: tracks){
			Geometry geometry = track.getGeometry();
			if(geometry != null && !geometry.isEmpty()){
				geometries.add(track.getGeometry());
			}
		}
		
		if(geometries.size() > 0){
			return new GeometryFactory().buildGeometry(geometries).getEnvelopeInternal();
		}else{
			return null;
		}
		
	}

}
