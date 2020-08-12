package com.atolcd.pentaho.di.gis.io;

import java.io.IOException;
import java.text.Normalizer;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import com.atolcd.gis.dxf.Entity;
import com.atolcd.gis.dxf.Layer;
import com.atolcd.pentaho.di.gis.io.features.Feature;
import com.atolcd.pentaho.di.gis.io.features.Field.FieldType;
import com.atolcd.pentaho.di.gis.io.features.Value;
import com.atolcd.pentaho.di.gis.utils.GeometryUtils;
import com.vividsolutions.jts.geom.Geometry;

import org.pentaho.di.core.exception.KettleException;

import fr.michaelm.jump.drivers.dxf.DxfFile;

public class DXFWriter extends AbstractFileWriter {

    private String dxfFileName;
    private String layerName;
    private String layerNameFieldName;
    private boolean forceTo2DGeometry;
    private int precision;
    private boolean exportWithAttributs;

    public DXFWriter(String fileName, String layerName, String geometryFieldName, String charsetName)
            throws KettleException {

        super(geometryFieldName, charsetName);
        this.dxfFileName = checkFilename(fileName).getFile();
        if (layerName == null || layerName.isEmpty()) {
            this.layerName = "0";
        } else {
            this.layerName = checkLayerName(layerName);
        }
        this.layerNameFieldName = null;
        new DxfFile();
        this.forceTo2DGeometry = false;
        this.precision = 0;
        this.exportWithAttributs = true;
    }

    public int getPrecision() {
        return precision;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    public boolean isForceTo2DGeometry() {
        return forceTo2DGeometry;
    }

    public void setForceTo2DGeometry(boolean forceTo2DGeometry) {
        this.forceTo2DGeometry = forceTo2DGeometry;
    }

    public void setLayerNameFieldName(String layerNameFieldName) {
        this.layerNameFieldName = layerNameFieldName;
    }

    public void setExportWithAttributs(boolean exportWithAttributs) {
		this.exportWithAttributs = exportWithAttributs;
	}

    public void writeFeatures(List<Feature> features) throws KettleException {

        try {

            com.atolcd.gis.dxf.DXFWriter dxfWriter= new com.atolcd.gis.dxf.DXFWriter(this.dxfFileName);
            
        	//Plusieurs layers potentiels
            if (this.layerNameFieldName != null) {
            	
            
            }else{
            	
            	//Un seul Layer
            	Layer layer = new Layer(this.layerName);
            	
            	long fid = 0;
            	for (Feature feature : features) {
            		
            		 Geometry geometry = (Geometry) feature.getValue(feature.getField(this.geometryFieldName));
            		  if (!GeometryUtils.isNullOrEmptyGeometry(geometry)) {
                      	layer.getEntities().add(toEntity(feature,String.valueOf(fid),null));
                        fid++;
            		  }
            		
            	}
            	
            	dxfWriter.addLayer(layer);
           	
            }
        	
            dxfWriter.write(this.precision, this.exportWithAttributs);

        } catch (IOException e) {
            throw new KettleException("Error writing features to " + this.dxfFileName, e);
        } catch (Exception e) {
            throw new KettleException("Error converting to Entity " + this.dxfFileName, e);
        }
    }

    private String checkLayerName(String layerName) {

        String normalizedString = Normalizer.normalize(layerName, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(normalizedString).replaceAll("").replaceAll("[^a-zA-Z]+", "_").toUpperCase();

    }

    private Entity toEntity(Feature feature, String id, String text) throws Exception {

    	Geometry geometry = (Geometry) feature.getValue(feature.getField(this.geometryFieldName));
    	
    	if(this.forceTo2DGeometry){
    		geometry = GeometryUtils.get2DGeometry(geometry);
    	}
    	
    	Entity entity = new Entity(
    			id,
    			geometry,
    			"",
    			text);
    	
    	if(this.exportWithAttributs){

    		for(Value value : feature.getValues()){
    		
    			if(!value.getField().getType().equals(FieldType.GEOMETRY)
    					&& !value.getField().getType().equals(FieldType.BINARY)){

    				Class<?> classz = null;
    				
    		        if (value.getField().getType().equals(FieldType.STRING)) {
    		        	classz = String.class;
    		        
    		        } else if (value.getField().getType().equals(FieldType.LONG)) {
    		        	classz = Long.class;
    		        
    		        } else if (value.getField().getType().equals(FieldType.DOUBLE)) {
    		        	classz = Double.class;
    		       
    		        } else if (value.getField().getType().equals(FieldType.BOOLEAN)) {
    		        	classz = Boolean.class;
    		       
    		        } else if (value.getField().getType().equals(FieldType.DATE)) {
    		        	classz = Date.class;
    		        }
    		        
    		        if(classz !=null){
    		        
	    		        entity.AddExtendedData(
	    					value.getField().getName(),
	    					String.class,
	    					String.valueOf(value.getValue())
	    				);
    		        
    		        }
    			}
    				
    		}
    		
    	}
    			
    	return entity;
    }

}
