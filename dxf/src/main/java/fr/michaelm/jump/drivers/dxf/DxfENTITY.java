package fr.michaelm.jump.drivers.dxf;
import java.util.ArrayList;
import java.util.List;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class DxfENTITY {
	
	private String layerName;
	private Geometry geometry;
	private List<DxfXDATA> xData;
	
	public DxfENTITY(String layerName, Geometry geometry) {
		this.layerName = layerName;
		this.geometry = geometry;
		this.xData = new ArrayList<DxfXDATA>();
	}

    public String getLayerName() {
		return layerName;
	}

	public Geometry getGeometry() {
		return geometry;
	}

	public List<DxfXDATA> getxData() {
		return xData;
	}
	
	public void addXData(DxfXDATA xData){
		this.xData.add(xData);
	}

	public String toDXF(int precision, boolean writeXData){
		
		StringBuffer sb = new StringBuffer();
		
		if (this.geometry.getGeometryType().equals("Point")) {

			sb.append(pointToDXF(geometry, layerName, precision, this.xData));

        }
        else if (this.geometry.getGeometryType().equals("LineString")) {

			sb.append(lineStringToDXF(geometry, layerName, precision, this.xData));

        }
        else if (this.geometry.getGeometryType().equals("Polygon")) {
            sb.append(polygonToDXF(geometry, layerName, precision, this.xData));
            
        } else if (this.geometry instanceof GeometryCollection) {
           
            for (int i = 0 ; i < this.geometry.getNumGeometries() ; i++) {
                
            	Geometry geometryN = this.geometry.getGeometryN(i);
    
            	if (geometryN.getGeometryType().equals("Point")) {
            		sb.append(pointToDXF(geometryN, layerName, precision,this.xData));
            		
            	} else if (geometryN.getGeometryType().equals("LineString")) {
            		sb.append(lineStringToDXF(geometryN, layerName, precision,this.xData));
            		
            	} else if (geometryN.getGeometryType().equals("Polygon")) {
            		sb.append(polygonToDXF(geometryN, layerName, precision, this.xData));
            	
            	}

            }

        }
	
		return sb.toString();
	
    }
	
	private static StringBuffer xDataToDXF(StringBuffer sb, List<DxfXDATA> xDatas){
		
		for(DxfXDATA xData : xDatas){
			sb.append(DxfGroup.toString(DxfXDATA.GROUPCODE_XDATA_APP_NAME, xData.getName()));
			sb.append(DxfGroup.toString(xData.getCode(), xData.getValue().toString()));
		}
		return sb;
	}
	

    private static String pointToDXF(Geometry geometry, String layerName, int precision, List<DxfXDATA> xDatas) {

        StringBuffer sb = new StringBuffer(DxfGroup.toString(0, "POINT"));
        sb.append(DxfGroup.toString(8, layerName));
        Coordinate coord = ((Point)geometry).getCoordinate();
        sb.append(DxfGroup.toString(10, coord.x, precision));
        sb.append(DxfGroup.toString(20, coord.y, precision));
        if (!Double.isNaN(coord.z)){
        	sb.append(DxfGroup.toString(30, coord.z, precision));
        }
        
        sb = xDataToDXF(sb,xDatas);
        
        return sb.toString();
    }

    private static String lineStringToDXF(Geometry geometry, String layerName,int precision, List<DxfXDATA> xDatas) {
        LineString geom = (LineString)geometry;
        Coordinate[] coords = geom.getCoordinates();
        boolean isLine = (coords.length == 2);
        StringBuffer sb;
        if (!isLine) {
        	sb = new StringBuffer(DxfGroup.toString(0, "POLYLINE"));
        }
        else {
            sb = new StringBuffer(DxfGroup.toString(0, "LINE"));       	
        }
        sb.append(DxfGroup.toString(8, layerName));
        if (isLine){
            sb.append(DxfGroup.toString(10, coords[0].x, precision));
            sb.append(DxfGroup.toString(20, coords[0].y, precision));
	        if (!Double.isNaN(coords[0].z)) {
	            sb.append(DxfGroup.toString(30, coords[0].z, precision));
	        }
            sb.append(DxfGroup.toString(11, coords[1].x, precision));
            sb.append(DxfGroup.toString(21, coords[1].y, precision));
	        if (!Double.isNaN(coords[1].z)) {
	            sb.append(DxfGroup.toString(31, coords[1].z, precision));
	        }
	        
	        sb = xDataToDXF(sb,xDatas);
	        
        }
        else {
            sb.append(DxfGroup.toString(66, 1));
            sb.append(DxfGroup.toString(10, "0.0"));
            sb.append(DxfGroup.toString(20, "0.0"));
            if (!Double.isNaN(coords[0].z)) sb.append(DxfGroup.toString(30, "0.0"));
            sb.append(DxfGroup.toString(70, 8));

            sb = xDataToDXF(sb,xDatas);
            
            for (int i = 0 ; i < coords.length ; i++) {
                sb.append(DxfGroup.toString(0, "VERTEX"));
                sb.append(DxfGroup.toString(8, layerName));
                sb.append(DxfGroup.toString(10, coords[i].x, precision));
                sb.append(DxfGroup.toString(20, coords[i].y, precision));
                if (!Double.isNaN(coords[i].z)) sb.append(DxfGroup.toString(30, coords[i].z, precision));
                sb.append(DxfGroup.toString(70, 32));
            }
            sb.append(DxfGroup.toString(0, "SEQEND"));
        }
        return sb.toString();
    }

    private static String polygonToDXF(Geometry geometry, String layerName,int precision, List<DxfXDATA> xDatas) {
        Polygon geom = (Polygon)geometry;
        Coordinate[] coords = geom.getExteriorRing().getCoordinates();
        StringBuffer sb = new StringBuffer(DxfGroup.toString(0, "POLYLINE"));
        sb.append(DxfGroup.toString(8, layerName));
        sb.append(DxfGroup.toString(66, 1));
        sb.append(DxfGroup.toString(10, "0.0"));
        sb.append(DxfGroup.toString(20, "0.0"));
        if (!Double.isNaN(coords[0].z)) sb.append(DxfGroup.toString(30, "0.0"));
        sb.append(DxfGroup.toString(70, 9));
        
        sb = xDataToDXF(sb,xDatas);
        
        for (int i = 0 ; i < coords.length ; i++) {
            sb.append(DxfGroup.toString(0, "VERTEX"));
            sb.append(DxfGroup.toString(8, layerName));
            sb.append(DxfGroup.toString(10, coords[i].x, precision));
            sb.append(DxfGroup.toString(20, coords[i].y, precision));
            if (!Double.isNaN(coords[i].z)) sb.append(DxfGroup.toString(30, coords[i].z, precision));
            sb.append(DxfGroup.toString(70, 32));
        }
        sb.append(DxfGroup.toString(0, "SEQEND"));
        for (int h = 0 ; h < geom.getNumInteriorRing() ; h++) {
            sb.append(DxfGroup.toString(0, "POLYLINE"));
            sb.append(DxfGroup.toString(8, layerName));
            sb.append(DxfGroup.toString(66, 1));
            sb.append(DxfGroup.toString(10, "0.0"));
            sb.append(DxfGroup.toString(20, "0.0"));
            if (!Double.isNaN(coords[0].z)) sb.append(DxfGroup.toString(30, "0.0"));
            sb.append(DxfGroup.toString(70, 9));
            
            sb = xDataToDXF(sb,xDatas);
            
            coords = geom.getInteriorRingN(h).getCoordinates();
            for (int i = 0 ; i < coords.length ; i++) {
                sb.append(DxfGroup.toString(0, "VERTEX"));
                sb.append(DxfGroup.toString(8, layerName));
                sb.append(DxfGroup.toString(10, coords[i].x, precision));
                sb.append(DxfGroup.toString(20, coords[i].y, precision));
                if (!Double.isNaN(coords[i].z)) sb.append(DxfGroup.toString(30, coords[i].z, precision));
                sb.append(DxfGroup.toString(70, 32));
            }
            sb.append(DxfGroup.toString(0, "SEQEND"));
        }
        
        return sb.toString();
		
	}

}
