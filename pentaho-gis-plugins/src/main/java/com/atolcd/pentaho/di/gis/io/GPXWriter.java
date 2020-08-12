package com.atolcd.pentaho.di.gis.io;

/*
 * #%L
 * Pentaho Data Integrator GIS Plugin
 * %%
 * Copyright (C) 2015 Atol CD
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */


import java.io.Writer;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import org.pentaho.di.core.exception.KettleException;

import com.atolcd.gis.gpx.GpxWriter;
import com.atolcd.gis.gpx.type.AbstractSpatialElement;
import com.atolcd.gis.gpx.type.Author;
import com.atolcd.gis.gpx.type.Document;
import com.atolcd.gis.gpx.type.Metadata;
import com.atolcd.gis.gpx.type.Route;
import com.atolcd.gis.gpx.type.Track;
import com.atolcd.gis.gpx.type.WayPoint;
import com.atolcd.pentaho.di.gis.io.features.Feature;
import com.atolcd.pentaho.di.gis.io.features.Field;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

public class GPXWriter extends AbstractFileWriter {
	
	private static String GPX_CREATOR = "Pentaho Data Integration - GIS plugins";

    private String gpxFileName;
    private String version;
    private String documentName;
    private String documentDescription;
    private String authorName;
    private String authorEmail;
    private String keywords;
    private GregorianCalendar datetime;
        
    
    private String featureNameField;
    private String featureDescriptionField;

    private Writer writer;
    private boolean isServletOutput;

    public GPXWriter(String fileName, String geometryFieldName, String charsetName) throws KettleException {

        super(geometryFieldName, charsetName);
        
        this.gpxFileName = checkFilename(fileName).getFile();
        this.writer = null;
        this.isServletOutput = false;
        
        this.version = Document.GPX_VERSION_1_1;
        this.documentName = null;
        this.documentDescription = null;
        this.authorName = null;
        this.authorEmail = null;
        this.keywords = null;
        this.datetime = null;
       
        this.featureNameField = null;
        this.featureDescriptionField = null;

    }

    public GPXWriter(Writer writer, String geometryFieldName, String charsetName) throws KettleException {

        super(geometryFieldName, charsetName);
       
        this.gpxFileName = null;
        this.writer = writer;
        this.isServletOutput = true;
        
        this.version = Document.GPX_VERSION_1_1;
        this.documentName = null;
        this.documentDescription = null;
        this.authorName = null;
        this.authorEmail = null;
        this.keywords = null;
        this.datetime = null;
       
        this.featureNameField = null;
        this.featureDescriptionField = null;


    }

    public String getDocumentName() {
		return documentName;
	}

	public void setDocumentName(String documentName) {
		this.documentName = documentName;
	}

	public String getDocumentDescription() {
		return documentDescription;
	}

	public void setDocumentDescription(String documentDescription) {
		this.documentDescription = documentDescription;
	}

	public String getFeatureNameField() {
		return featureNameField;
	}

	public void setFeatureNameField(String featureNameField) {
		this.featureNameField = featureNameField;
	}

	public String getFeatureDescriptionField() {
		return featureDescriptionField;
	}

	public void setFeatureDescriptionField(String featureDescriptionField) {
		this.featureDescriptionField = featureDescriptionField;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getAuthorName() {
		return authorName;
	}

	public void setAuthorName(String authorName) {
		this.authorName = authorName;
	}

	public String getAuthorEmail() {
		return authorEmail;
	}

	public void setAuthorEmail(String authorEmail) {
		this.authorEmail = authorEmail;
	}

	public String getKeywords() {
		return keywords;
	}

	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}

	public GregorianCalendar getDatetime() {
		return datetime;
	}

	public void setDatetime(GregorianCalendar datetime) {
		this.datetime = datetime;
	}

	public void writeFeatures(List<Feature> features) throws KettleException {
		
		try{
			
			Document gpxDocument = new Document();
			gpxDocument.setCreator(GPX_CREATOR);
			gpxDocument.setVersion(this.version);
	
			Metadata metadata = new Metadata();
			if (this.documentName != null && !this.documentName.isEmpty()) {
				metadata.setName(this.documentName);
	        }
			
			if (this.documentDescription  != null && !this.documentDescription.isEmpty()) {
				metadata.setDescription(this.documentDescription);
	        }
			
			if (this.authorName  != null && !this.authorName.isEmpty()) {
								
				metadata.setAuthor(new Author(this.authorName,this.authorEmail));
	        }
			
			if (this.keywords  != null && !this.keywords.isEmpty()) {
				
				metadata.setKeywords(this.keywords);
	        }
			
			if (this.datetime  != null) {
				
				metadata.setTime(this.datetime);
	        }
			
		
			gpxDocument.setMetadata(metadata);

			 // Récupération des champs utilisés
	        Field geometryField = null;
	        Field nameField = null;
	        Field descriptionField = null;
	
	        Iterator<Feature> featureIt = features.iterator();
	        boolean first = true;
	        while (featureIt.hasNext()) {
	        	
	        	Feature feature = featureIt.next();
	            if (first) {
	
	                geometryField = feature.getField(this.geometryFieldName);
	
	                if (featureNameField != null) {
	                    nameField = feature.getField(this.featureNameField);
	                }
	
	                if (featureDescriptionField != null) {
	                    descriptionField = feature.getField(this.featureDescriptionField);
	                }
	
	                first = false;
	            }
	
	            //Récupération de la géométrie
	            Geometry geometry = (Geometry) feature.getValue(geometryField);
	
	            // Vérification de l'emprise : doit être en WGS 84
	            Envelope envelope = geometry.getEnvelopeInternal();
	            if (envelope.getMaxX() > 180 || envelope.getMinX() < -180 || envelope.getMaxY() > 90 || envelope.getMinY() < -90) {
	
	                throw new KettleException("Bad coordinates for WGS84 system");
	
	            }

	            //Alimention du GPX
	            AbstractSpatialElement spatialElement;
           
            	
            	//Conversion de geometrie
                //Conversion JTS en GPX
                //- Point -> Gpx Wpt ,
                //- LineString -> Gpx Route ,
                //- MultilineString -> Gpx Track
	            if(geometry instanceof Point){
	            	spatialElement = new WayPoint(geometry.getCoordinate());
	            	gpxDocument.getWayPoints().add((WayPoint) spatialElement);
	            	
	            }else if(geometry instanceof LineString){
	            	spatialElement = new Route((LineString) geometry);
	            	gpxDocument.getRoutes().add((Route) spatialElement);
	            
	            }else if(geometry instanceof MultiLineString){
	            	spatialElement = new Track((MultiLineString) geometry);
	            	gpxDocument.getTracks().add((Track) spatialElement);
	            	
	            }else{
	            	throw new KettleException("Only Point , LineString and  MultilineString are allowed.");
	           
	            }
	
	            // Nom de feature
	            if (featureNameField != null) {
	                String name = (String) feature.getValue(nameField);
	                if (name != null && !name.isEmpty()) {
	                	spatialElement.setName(name);
	                }

	            }

	            // Description de feature
	            if (featureDescriptionField != null) {
	                String description = (String) feature.getValue(descriptionField);
	                if (description != null  && !description.isEmpty()) {
	                	spatialElement.setDescription(description);
	                }
	            }


	            //Ecriture du fichier ou du flux
	        	GpxWriter gpxWriter = new GpxWriter();
	            if (isServletOutput) {

	                if (features.size() > 0) {
	                	gpxWriter.write(gpxDocument, writer, this.charset.name());
	                }

	            } else {

	            	gpxWriter.write(gpxDocument, this.gpxFileName, this.charset.name());

	            }
	        }
       
	            
        
		}catch(Exception e){
        	throw new KettleException(e.getMessage());
        }
	}
	
}
