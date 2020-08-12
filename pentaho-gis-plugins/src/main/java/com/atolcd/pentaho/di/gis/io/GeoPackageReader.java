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


import java.io.File;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.contents.Contents;
import mil.nga.geopackage.contents.ContentsDao;
import mil.nga.geopackage.db.GeoPackageDataType;
import mil.nga.geopackage.features.user.FeatureRow;
import mil.nga.geopackage.features.user.FeatureTable;
import mil.nga.geopackage.geom.GeoPackageGeometryData;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.user.UserColumn;
import mil.nga.geopackage.user.UserCoreResult;
import mil.nga.geopackage.user.UserCoreRow;
import mil.nga.geopackage.user.UserDao;
import mil.nga.geopackage.user.UserTable;

import org.pentaho.di.core.exception.KettleException;

import com.atolcd.pentaho.di.gis.io.features.Feature;
import com.atolcd.pentaho.di.gis.io.features.Field;
import com.atolcd.pentaho.di.gis.io.features.Field.FieldType;
import com.atolcd.pentaho.di.gis.utils.GeometryUtils;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;


public class GeoPackageReader extends AbstractFileReader {

	private static String GPKG_CONTENTS_TABLE_NAME = "gpkg_contents";
	private static GeometryFactory geometryFactory = new GeometryFactory();
	private static SimpleDateFormat dateFormat =  new SimpleDateFormat("YYYY-MM-DD");
	private static SimpleDateFormat dateTimeFormat =  new SimpleDateFormat(" YYYY-MM-DD HH:MM:SS.SSSZ");
	
    private String gpkgFileName;
    private boolean gpkgFileExist;
    @SuppressWarnings("rawtypes")
	private HashMap<String,UserTable> gpkgTables;
    private boolean gpkgContents;

    @SuppressWarnings("rawtypes")
	public GeoPackageReader(String fileName, String tableName, String geometryFieldName, String charsetName) throws KettleException {

        super(tableName, null, charsetName);

        this.gpkgFileExist = new File(checkFilename(fileName).getFile()).exists();

        if (!this.gpkgFileExist) {
            throw new KettleException("Missing " + fileName + " file");
        } else {
            this.gpkgFileName = checkFilename(fileName).getFile();
        }
        
        if(tableName.equalsIgnoreCase("*")){
        	 this.layerName = GeoPackageReader.GPKG_CONTENTS_TABLE_NAME;
        }else{
        	 this.layerName = tableName;
        }
        this.gpkgTables = new HashMap<String,UserTable>();
        this.gpkgContents = false;
        this.geometryFieldName = geometryFieldName;

    	GeoPackage geoPackage = GeoPackageManager.open(new File(this.gpkgFileName));
    	
    	//Si table gpk_contents
    	if(this.layerName.equalsIgnoreCase(GeoPackageReader.GPKG_CONTENTS_TABLE_NAME)){
    		
    		//Liste les tables de type
    		//- features
    		//- attributes
    		//- tiles
    		//- autres si extensions
    		
    		this.fields.add(new Field("table_name",FieldType.STRING,null,null));
    		this.fields.add(new Field("data_type",FieldType.STRING,null,null));
    		this.fields.add(new Field("identifier",FieldType.STRING,null,null));
    		this.fields.add(new Field("description",FieldType.STRING,null,null));
    		this.fields.add(new Field("last_change",FieldType.DATE,null,null));
    		this.fields.add(new Field("min_x",FieldType.DOUBLE,null,null));
    		this.fields.add(new Field("min_y",FieldType.DOUBLE,null,null));
    		this.fields.add(new Field("max_x",FieldType.DOUBLE,null,null));
    		this.fields.add(new Field("max_y",FieldType.DOUBLE,null,null));
    		this.fields.add(new Field("srs_id",FieldType.LONG,null,null));
    		this.gpkgContents = true;
    	
    	//Sinon, table "features" ou "attributes"
    	}else{
    		
    		//Collection de toutes les tables "features" ou "attributes"
    		//avec nom en majuscule
    		       	        	
        	//Liste des tables "attributes"
        	for(String table : geoPackage.getAttributesTables()){
        		this.gpkgTables.put(table.toUpperCase(), geoPackage.getAttributesDao(table).getTable());
        	}
        	
        	//Liste des tables "features"
        	for(String table : geoPackage.getFeatureTables()){
        		this.gpkgTables.put(table.toUpperCase(), geoPackage.getFeatureDao(table).getTable());
        	}

        	//Si table non trouvée
        	if(!this.gpkgTables.containsKey(this.layerName.toUpperCase())){
        		throw new KettleException("Error initialize reader : Table " + this.layerName + " not found");
        	}
        	
        	//Récupère la table
    		UserTable table = this.gpkgTables.get(this.layerName.toUpperCase());

    		//Boucle sur les champs
    	    for(int i = 0; i < table.columnCount(); i++){

    	    	UserColumn column = table.getColumn(i);
    	    	Field field = null;

    	    	//Si type null teste si table de features
    	    	//et colonne de "geometry"
    	    	if (column.getDataType() == null) {

    	    		if(table instanceof FeatureTable && column.getIndex() == ((FeatureTable)table).getGeometryColumnIndex()){
    	    					
    	    			field = new Field(this.geometryFieldName, FieldType.GEOMETRY, null, null);	
    	    				
    	    		}else{
    	    			throw new KettleException("Error initialize reader : Unknow data type for column " + column.getName());
    	    		}  
    	    	} else if (column.getDataType().equals(GeoPackageDataType.BLOB)) {
    	    		
    	    		if(table instanceof FeatureTable && column.getIndex() == ((FeatureTable)table).getGeometryColumnIndex()){
    					
    	    			field = new Field(this.geometryFieldName, FieldType.GEOMETRY, null, null);
    	    		}

    	    	} else if (column.getDataType().equals(GeoPackageDataType.BLOB)) {
    	    		field = new Field(column.getName(), FieldType.BINARY, null, null);

    	    	} else if (column.getDataType().equals(GeoPackageDataType.BOOLEAN)) {
    	    		field = new Field(column.getName(), FieldType.BOOLEAN, null, null);

    	    	} else if (column.getDataType().equals(GeoPackageDataType.DATE)) {
    	    		field = new Field(column.getName(), FieldType.DATE, null, null);	 

    	    	} else if (column.getDataType().equals(GeoPackageDataType.DATETIME)) {
    	    		field = new Field(column.getName(), FieldType.DATE, null, null);	 

    	    	} else if (column.getDataType().equals(GeoPackageDataType.DOUBLE)) {
    	    		field = new Field(column.getName(), FieldType.DOUBLE, null, null);	  

    	    	} else if (column.getDataType().equals(GeoPackageDataType.FLOAT)) {
    	    		field = new Field(column.getName(), FieldType.DOUBLE, null, null);

    	    	} else if (column.getDataType().equals(GeoPackageDataType.INT)) {
    	    		field = new Field(column.getName(), FieldType.LONG, null, null);

    	    	} else if (column.getDataType().equals(GeoPackageDataType.INTEGER)) {
    	    		field = new Field(column.getName(), FieldType.LONG, null, null);

    	    	} else if (column.getDataType().equals(GeoPackageDataType.MEDIUMINT)) {
    	    		field = new Field(column.getName(), FieldType.LONG, null, null);

    	    	} else if (column.getDataType().equals(GeoPackageDataType.REAL)) {
    	    		field = new Field(column.getName(), FieldType.DOUBLE, null, null);

    	    	} else if (column.getDataType().equals(GeoPackageDataType.SMALLINT)) {
    	    		field = new Field(column.getName(), FieldType.LONG, null, null);

    	    	} else if (column.getDataType().equals(GeoPackageDataType.TEXT)) {
    	    		field = new Field(column.getName(), FieldType.STRING, null, null);

    	    	} else if (column.getDataType().equals(GeoPackageDataType.TINYINT)) {
    	    		field = new Field(column.getName(), FieldType.LONG, null, null);	

    	    	}else{
    	    		throw new KettleException("Error initialize reader : Type of column " + column.getType() + " not allowed for attribut table " + this.layerName);
    	    		
    	    	}

    	    	this.fields.add(field); 
    				
    		}

    	}
    	
    	geoPackage.close();

    }

    @SuppressWarnings("rawtypes")
	public List<Feature> getFeatures() throws KettleException {

        List<Feature> features = new ArrayList<Feature>();
        long entityNumber = 0;


    	GeoPackage geoPackage = GeoPackageManager.open(new File(this.gpkgFileName));
    	
    	//Si table gpk_contents
    	if(this.gpkgContents){
    		        		
    		//Récupère le contenu
    		ContentsDao contentsDao = geoPackage.getContentsDao();
    		long entitiesTotalNumber = 0;
			try {
				entitiesTotalNumber = contentsDao.countOf();
			} catch (SQLException e) {
				throw new KettleException("Error reading features :" + e.getMessage());
			}
    		if (this.limit == 0 || this.limit > entitiesTotalNumber || this.limit < 0) {
    			this.limit = entitiesTotalNumber;
    	    }
    		
    		//Boucle sur chaque content tant que limite non atteinte
    		for(Contents contents : contentsDao){

    			if (entityNumber < this.limit) {
        			Feature feature = new Feature();
        			feature.addValue(this.getField("table_name"), contents.getTableName());
        			feature.addValue(this.getField("data_type"), contents.getDataType().getName());
        			feature.addValue(this.getField("identifier"), contents.getIdentifier());
        			feature.addValue(this.getField("description"), contents.getDescription());
        			feature.addValue(this.getField("last_change"), contents.getLastChange());
        			feature.addValue(this.getField("min_x"), contents.getMinX());
        			feature.addValue(this.getField("min_y"), contents.getMinY());
        			feature.addValue(this.getField("max_x"), contents.getMaxX());
        			feature.addValue(this.getField("max_y"), contents.getMaxY());
        			feature.addValue(this.getField("srs_id"), contents.getSrsId());
        			
        			features.add(feature);
        			entityNumber++;
        			
    			  } else {
                      break;
                  }
    		}

    	//Sinon, table de feature ou d'attributs
    	}else{

    		//Récupère la table
        	UserTable table = this.gpkgTables.get(this.layerName.toUpperCase());
        	UserDao userDao = null;

        	if(table instanceof FeatureTable){
        		userDao = geoPackage.getFeatureDao(table.getTableName());
        	}else{
        		userDao = geoPackage.getAttributesDao(table.getTableName());
        	}

        	//Requête avec limite
        	UserCoreResult userCoreResult = null;
        	if (this.limit <= 0) {
        		userCoreResult = userDao.queryForAll();
        	}else{
        		userCoreResult  = userDao.query(null, null,null,null,null, null, String.valueOf(this.limit));
        	}

        	//Bouche sur chaque ligne du résultat
        	while(userCoreResult.moveToNext()){
        		
        		UserCoreRow userCoreRow = userCoreResult.getRow();
        		Feature feature = new Feature();

        		for(Field field : this.fields){
        			
        			Object inValue = null;
        			Object outValue = null;
        			
        			if(field.getType().equals(FieldType.GEOMETRY)){
        				inValue = ((FeatureRow)userCoreRow).getGeometry();
        			}else{
        				inValue = userCoreRow.getValue(field.getName());
        			}
        			
        			if(inValue != null){
        			
	        			//byte[]
	        			if(field.getType().equals(FieldType.BINARY)){
	        				
	        				outValue = (byte[]) inValue;

	        			//boolean
	        			}else if(field.getType().equals(FieldType.BOOLEAN)){
	
	        				outValue = (Boolean) inValue;
	
	        			//date
	        			}else if(field.getType().equals(FieldType.DATE)){
	        				
	        				try {
		        				if(table.getColumn(field.getName()).getDataType().equals(GeoPackageDataType.DATE)){
		        					outValue = dateFormat.parse(inValue.toString());
					
		        				}else{
		        					outValue = dateTimeFormat.parse(inValue.toString());
		        				}
		        				
	        				} catch (ParseException e) {
	        					throw new KettleException("Error reading features :" + e.getMessage());
							}
 	
	        			//double
	        			}else if(field.getType().equals(FieldType.DOUBLE)){
	        				
	        				outValue = Double.parseDouble(inValue.toString());
	
	        			//geometry
	        			}else if(field.getType().equals(FieldType.GEOMETRY)){
	        				
	        				GeoPackageGeometryData geometryData = (GeoPackageGeometryData) inValue;
							Geometry jtsGeometry = toJtsGeometry(geometryData.getGeometry());
							jtsGeometry.setSRID(geometryData.getSrsId());
							
							if (this.forceTo2DGeometry) {
								jtsGeometry = GeometryUtils.get2DGeometry(jtsGeometry);
							}
							
							if (this.forceToMultiGeometry) {
								jtsGeometry = GeometryUtils.getMultiGeometry(jtsGeometry);
							}

							outValue = jtsGeometry;
	
						//long
	        			}else if(field.getType().equals(FieldType.LONG)){
	
	        				outValue = Long.parseLong(inValue.toString());

	        			//String
	        			}else if(field.getType().equals(FieldType.STRING)){
	        				outValue = inValue.toString();
	
	        			}
	        			
        			}
	
        			feature.addValue(field, outValue);

        		}
     	        
     	        features.add(feature);
        		
        	}
    	
    	}
    	
    	geoPackage.close();
        	
        return features;

    }

	/**
	 * JTS Envelope to Gpkg bounding box
	 * @param boundingBox
	 * @return
	 */
	public static Envelope toJtsBounds(mil.nga.geopackage.BoundingBox bounds){
		
		Envelope jtsBounds = new Envelope();
		jtsBounds.init(
			bounds.getMinLongitude(),
			bounds.getMaxLongitude(),
			bounds.getMinLatitude(),
			bounds.getMaxLatitude()
		);
		return  jtsBounds;
		
	}
	
	/**
	 * Gpkg Geometry to JTS Geometry
	 * @param geometry
	 * @return
	 * @throws KettleException 
	 * @throws GpkgGeometryConverterException 
	 */
	public static com.vividsolutions.jts.geom.Geometry toJtsGeometry(mil.nga.sf.Geometry geometry) throws KettleException{
		
		com.vividsolutions.jts.geom.Geometry jtsGeometry = null;
		
		if(geometry != null){

			//Point
			if (geometry.getGeometryType().equals(mil.nga.sf.GeometryType.POINT)){
				return toJtsPoint((mil.nga.sf.Point) geometry);
			
			//MultiPoint
			}else if (geometry.getGeometryType().equals(mil.nga.sf.GeometryType.MULTIPOINT)){
				return toJtsMultiPoint((mil.nga.sf.MultiPoint) geometry);
			
			//LineString
			}else if (geometry.getGeometryType().equals(mil.nga.sf.GeometryType.LINESTRING)){
				return toJtsLineString((mil.nga.sf.LineString) geometry);
				
			//MultiLineString	
			}else if (geometry.getGeometryType().equals(mil.nga.sf.GeometryType.MULTILINESTRING)){
				return toJtsMultiLineString((mil.nga.sf.MultiLineString) geometry);
				
			//Polygon		
			}else if (geometry.getGeometryType().equals(mil.nga.sf.GeometryType.POLYGON)){
				return toJtsPolygon((mil.nga.sf.Polygon) geometry);
			
			//MultiPolygon	
			}else if(geometry.getGeometryType().equals(mil.nga.sf.GeometryType.MULTIPOLYGON)){
				return toJtsMultiPolygon((mil.nga.sf.MultiPolygon) geometry);
	
			//GeometryCollection	
			}else if(geometry.getGeometryType().equals(mil.nga.sf.GeometryType.GEOMETRYCOLLECTION)){
				
				List<com.vividsolutions.jts.geom.Geometry> jtsGeometries = new ArrayList<com.vividsolutions.jts.geom.Geometry>();
				for(mil.nga.sf.Geometry gpkgSubgeometry : ((mil.nga.sf.GeometryCollection<?>) geometry).getGeometries()){
					jtsGeometries.add(toJtsGeometry(gpkgSubgeometry));
				}
				
				return geometryFactory.createGeometryCollection(jtsGeometries.toArray(new com.vividsolutions.jts.geom.Geometry[jtsGeometries.size()]));
				
			}else{
				
				throw new KettleException(geometry.getGeometryType().getName() + " is not supported.");
				
			}
		
		}

		return jtsGeometry;
	}
	
	/**
	 * Gpkg Point to JTS Coordinate
	 * @param point
	 * @return
	 */
	private static com.vividsolutions.jts.geom.Coordinate toJtsCoordinate(mil.nga.sf.Point point) {
		
		com.vividsolutions.jts.geom.Coordinate coordinate = new com.vividsolutions.jts.geom.Coordinate(point.getX(), point.getY());
		if(point.getZ() != null){
			coordinate.z = point.getZ();
		}
		
		return coordinate;
		
	}
		
	/**
	 * Gpkg Points to JTS Coordinates
	 * @param linestring
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private static com.vividsolutions.jts.geom.Coordinate[] toJtsCoordinates(List<mil.nga.sf.Point> points) {
		
		com.vividsolutions.jts.geom.CoordinateList coordinateList = new com.vividsolutions.jts.geom.CoordinateList();
		for(mil.nga.sf.Point point : points){
			coordinateList.add(toJtsCoordinate(point));
		}
		
		return coordinateList.toCoordinateArray();
		
	}
	
	/**
	 * Gpkg Point to JTS Point
	 * @param point
	 * @return
	 */
	private static com.vividsolutions.jts.geom.Point toJtsPoint(mil.nga.sf.Point point) {

		return geometryFactory.createPoint(toJtsCoordinate((mil.nga.sf.Point) point));

	}

	/**
	 * Gpkg MultiPoint to JTS MultiPoint
	 * @param multiPoint
	 * @return
	 */
	private static com.vividsolutions.jts.geom.MultiPoint toJtsMultiPoint(mil.nga.sf.MultiPoint multiPoint) {
		
		return geometryFactory.createMultiPoint(toJtsCoordinates(multiPoint.getPoints()));

	}

	/**
	 * Gpkg LineString to JTS LineString
	 * @param lineString
	 * @return
	 */
	private static com.vividsolutions.jts.geom.LineString toJtsLineString(mil.nga.sf.LineString lineString) {

		return geometryFactory.createLineString(toJtsCoordinates(lineString.getPoints()));

	}
	
	/**
	 * Gpkg MultiLineString to JTS MultiLineString
	 * @param multiLineString
	 * @return
	 */
	private static com.vividsolutions.jts.geom.MultiLineString toJtsMultiLineString(mil.nga.sf.MultiLineString multiLineString) {

		List<com.vividsolutions.jts.geom.LineString> lineStrings = new ArrayList<com.vividsolutions.jts.geom.LineString>() ;
		for(mil.nga.sf.LineString linestring : ((mil.nga.sf.MultiLineString) multiLineString).getLineStrings()){
			lineStrings.add(toJtsLineString(linestring));
		}
		
		return geometryFactory.createMultiLineString(lineStrings.toArray(new com.vividsolutions.jts.geom.LineString[lineStrings.size()]));

	}
	
	/**
	 * Gpkg Polygon to JTS Polygon
	 * @param polygon
	 * @return
	 */
	private static com.vividsolutions.jts.geom.Polygon toJtsPolygon(mil.nga.sf.Polygon polygon) {
		
		mil.nga.sf.GeometryEnvelope envelope = mil.nga.sf.util.GeometryEnvelopeBuilder.buildEnvelope(polygon);
		
		com.vividsolutions.jts.geom.LinearRing exteriorRing = null;
		List<com.vividsolutions.jts.geom.LinearRing> interiorRings = new ArrayList<com.vividsolutions.jts.geom.LinearRing>() ;
		
		for(mil.nga.sf.LineString ring : ((mil.nga.sf.Polygon) polygon).getRings()){
			
			com.vividsolutions.jts.geom.LinearRing linearRing = geometryFactory.createLinearRing(toJtsCoordinates(ring.getPoints()));
			com.vividsolutions.jts.geom.Envelope jtsEnvelope = linearRing.getEnvelopeInternal();
			
			if(jtsEnvelope.getMinX() == envelope.getMinX()
					&& jtsEnvelope.getMinY() == envelope.getMinY()
					&& jtsEnvelope.getMaxX() == envelope.getMaxX()
					&& jtsEnvelope.getMaxY() == envelope.getMaxY()
			){
				exteriorRing = linearRing;
			
			}else{
				interiorRings.add(linearRing);
			}
			
		}

		return  geometryFactory.createPolygon(exteriorRing, interiorRings.toArray(new com.vividsolutions.jts.geom.LinearRing[interiorRings.size()]));
		
	}
	

	/**
	 * Gpkg MultiPolygon to JTS MultiPolygon
	 * @param multiPolygon
	 * @return
	 */
	private static com.vividsolutions.jts.geom.MultiPolygon toJtsMultiPolygon(mil.nga.sf.MultiPolygon multiPolygon) {

		List<com.vividsolutions.jts.geom.Polygon> polygons = new ArrayList<com.vividsolutions.jts.geom.Polygon>();
		for(mil.nga.sf.Polygon polygon : ((mil.nga.sf.MultiPolygon) multiPolygon).getPolygons()){
			polygons.add(toJtsPolygon(polygon));
		};
		
		return geometryFactory.createMultiPolygon(polygons.toArray(new com.vividsolutions.jts.geom.Polygon[polygons.size()]));

	}

}
