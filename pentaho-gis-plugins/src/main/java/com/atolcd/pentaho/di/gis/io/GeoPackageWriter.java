package com.atolcd.pentaho.di.gis.io;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import com.atolcd.pentaho.di.gis.io.features.Feature;
import com.atolcd.pentaho.di.gis.io.features.Field;
import com.atolcd.pentaho.di.gis.io.features.Field.FieldType;
import com.atolcd.pentaho.di.gis.utils.GeometryUtils;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import org.cts.CRSFactory;
import org.cts.crs.CRSException;
import org.cts.crs.CoordinateReferenceSystem;
import org.cts.registry.EPSGRegistry;
import org.cts.registry.RegistryManager;
import org.pentaho.di.core.exception.KettleException;

import mil.nga.geopackage.BoundingBox;
import mil.nga.geopackage.GeoPackage;
import mil.nga.geopackage.GeoPackageManager;
import mil.nga.geopackage.attributes.AttributesColumn;
import mil.nga.geopackage.attributes.AttributesDao;
import mil.nga.geopackage.attributes.AttributesRow;
import mil.nga.geopackage.attributes.AttributesTable;
import mil.nga.geopackage.contents.Contents;
import mil.nga.geopackage.contents.ContentsDataType;
import mil.nga.geopackage.db.GeoPackageDataType;
import mil.nga.geopackage.extension.rtree.RTreeIndexExtension;
import mil.nga.geopackage.features.columns.GeometryColumns;
import mil.nga.geopackage.features.user.FeatureColumn;
import mil.nga.geopackage.features.user.FeatureDao;
import mil.nga.geopackage.features.user.FeatureRow;
import mil.nga.geopackage.features.user.FeatureTableMetadata;
import mil.nga.geopackage.geom.GeoPackageGeometryData;
import mil.nga.geopackage.user.ContentValues;

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


public class GeoPackageWriter extends AbstractFileWriter {
	
	private String gpkgFileName;
	private String tableName;
	private String pkFieldName;
    private String contentsIdentifier;
    private String contentsDescription;
    private boolean forceTo2DGeometry;
    private boolean replaceFile;
    private boolean replaceTable;
    private Long assignedSrid;
    private GpkgGeometryType assignedGeometryType;
    private long commitLimit;
    
    public enum GpkgGeometryType{
		GEOMETRY(mil.nga.sf.GeometryType.GEOMETRY),
		POINT(mil.nga.sf.GeometryType.POINT),
		LINESTRING(mil.nga.sf.GeometryType.LINESTRING),
		POLYGON(mil.nga.sf.GeometryType.POLYGON),
		MULTIPOINT(mil.nga.sf.GeometryType.MULTIPOINT),
		MULTILINESTRING(mil.nga.sf.GeometryType.MULTILINESTRING),
		MULTIPOLYGON(mil.nga.sf.GeometryType.MULTIPOLYGON),
		GEOMETRYCOLLECTION(mil.nga.sf.GeometryType.GEOMETRYCOLLECTION);
		
		private mil.nga.sf.GeometryType geometryType;
		
		GpkgGeometryType(mil.nga.sf.GeometryType geometryType){
		    this.geometryType = geometryType;
		    
		    if(!geometryType.equals(mil.nga.sf.GeometryType.POINT)
		    	|| !geometryType.equals(mil.nga.sf.GeometryType.LINESTRING)
		    	|| !geometryType.equals(mil.nga.sf.GeometryType.POLYGON)
		    	|| !geometryType.equals(mil.nga.sf.GeometryType.MULTIPOINT)
		    	|| !geometryType.equals(mil.nga.sf.GeometryType.MULTILINESTRING)
		    	|| !geometryType.equals(mil.nga.sf.GeometryType.MULTIPOLYGON)
		    	|| !geometryType.equals(mil.nga.sf.GeometryType.GEOMETRYCOLLECTION)){
			
				this.geometryType = geometryType;
				
			}else{
				 this.geometryType = mil.nga.sf.GeometryType.GEOMETRY;
			}
		    
		}
		
		public mil.nga.sf.GeometryType toGpkgValue(){
			return geometryType;
		}
		
	}
	
	public enum GpkgGeometryWithZType{
		PROHIBITED((byte)0),
		MANDATORY((byte)1),
		OPTIONAL((byte)2);
		
		private byte code;

		GpkgGeometryWithZType(byte code){
		    this.code = code;
		}

		public byte toGpkgValue(){
		    return code;
		}
		
	}
	
	public enum GpkgGeometryWithMType{
		PROHIBITED((byte)0);
		
		private byte code;

		GpkgGeometryWithMType(byte code){
		    this.code = code;
		}

		public byte toGpkgValue(){
		    return code;
		}
		
	}
	
    public GeoPackageWriter(String fileName, String tableName, String pkFieldName, String charsetName) throws KettleException {
    	this(fileName, tableName, pkFieldName, null, charsetName);
    }
    

    public GeoPackageWriter(String fileName, String tableName, String pkFieldName, String geometryFieldName, String charsetName) throws KettleException {

        super(geometryFieldName, charsetName);
        if(geometryFieldName == null || geometryFieldName.isEmpty()){
    		this.geometryFieldName = "";
    	}
        this.gpkgFileName = fileName;
        this.tableName = tableName;
        this.pkFieldName = pkFieldName;
        this.contentsIdentifier = null;
        this.contentsDescription = null;
        this.forceTo2DGeometry = false;
        this.replaceFile = false;
        this.replaceTable = false;
        this.assignedSrid = null;
        this.assignedGeometryType = GpkgGeometryType.GEOMETRY;
        this.commitLimit = 1;
    }

	public void setReplaceFile(boolean replaceFile) {
		this.replaceFile = replaceFile;
	}

	public void setReplaceTable(boolean replaceTable) {
		this.replaceTable = replaceTable;
	}

	public void setContentsIdentifier(String contentsIdentifier) {
		this.contentsIdentifier = contentsIdentifier;
	}

	public void setContentsDescription(String contentsDescription) {
		this.contentsDescription = contentsDescription;
	}

	public void setForceTo2DGeometry(boolean forceTo2DGeometry) {
		this.forceTo2DGeometry = forceTo2DGeometry;
	}

	public void setAssignedSrid(long assignedSrid) {
		this.assignedSrid = assignedSrid;
	}

	public void setCommitLimit(long commitLimit) {
		this.commitLimit = commitLimit;
	}


	public void setAssignedGeometryType(String geometryType) {
		
		if(geometryType.equalsIgnoreCase("POINT")){
			this.assignedGeometryType = GpkgGeometryType.POINT;
			
		}else if(geometryType.equalsIgnoreCase("LINESTRING")){
			this.assignedGeometryType = GpkgGeometryType.LINESTRING ;
			
		}else if(geometryType.equalsIgnoreCase("POLYGON")){
			this.assignedGeometryType = GpkgGeometryType.POLYGON ;
		
		}else if(geometryType.equalsIgnoreCase("MULTIPOINT")){
			this.assignedGeometryType = GpkgGeometryType.MULTIPOINT ;
		
		}else if(geometryType.equalsIgnoreCase("MULTILINESTRING")){
			this.assignedGeometryType = GpkgGeometryType.MULTILINESTRING ;
		
		}else if(geometryType.equalsIgnoreCase("MULTIPOLYGON")){
			this.assignedGeometryType = GpkgGeometryType.MULTIPOLYGON ;
			
		}else if(geometryType.equalsIgnoreCase("GEOMETRYCOLLECTION")){
			this.assignedGeometryType = GpkgGeometryType.GEOMETRYCOLLECTION ;
			
		}else{
			this.assignedGeometryType = GpkgGeometryType.GEOMETRY ;
		}

	}

	public void writeFeatures(List<Feature> features) throws KettleException {

		boolean createTable = true;
		
		//Fichier existe et demande remplacement
		if(new File(checkFilename(this.gpkgFileName).getFile()).exists() && this.replaceFile){
			if(!new File(checkFilename(this.gpkgFileName).getFile()).delete()){
				throw new KettleException("File " + this.gpkgFileName + " can not be deleted.");
			}
		}
		
		//Fichier n'existe pas -> tentative de création
		if(!new File(checkFilename(this.gpkgFileName).getFile()).exists()){

			try{
				GeoPackageManager.create(new File(checkFilename(this.gpkgFileName).getFile()));
			}
			catch (Exception e){
				throw new KettleException("File " + this.gpkgFileName + " can not be created.");
			}
		}

		//Ouverture du fichier
    	GeoPackage geoPackage = GeoPackageManager.open(new File(checkFilename(this.gpkgFileName).getFile()));
		if((geoPackage.getFeatureTables().contains(this.tableName)
				|| geoPackage.getAttributesTables().contains(this.tableName))){
			
			if(this.replaceTable){
	    		geoPackage.deleteTableQuietly(this.tableName);
	    		geoPackage.getContentsDao().deleteTable(this.tableName);
	    		
	    	}else{
	    		createTable = false;
	    	}

		}
		
		//Récupération des colonnes de PK et de géométrie (si existe)
	    Field pkField = null;
	    Field geometryField = null;
	    List<Field> otherFields = new ArrayList<Field>();
	    
	    //Toutes les colonnes sauf pk et geometry
	    for(Field field : this.fields){
	
	    	//Autres colonnes
			if(!field.getName().equalsIgnoreCase(this.pkFieldName) && !field.getType().equals(FieldType.GEOMETRY)){
				otherFields.add(field);
				
			}else{
				
				//La colonne de PK
				if(field.getName().equalsIgnoreCase(this.pkFieldName)){
					
					if(!field.getType().equals(FieldType.LONG)){
						throw new KettleException("Field " + this.pkFieldName + " is not of Integer type. The PK field must be of type Integer");
					}else{
						pkField = field;
					}
				}
					
				//La colonne de geometry
				if(field.getName().equalsIgnoreCase(this.geometryFieldName)){
					
					if(!field.getType().equals(FieldType.GEOMETRY)){
						throw new KettleException("Field " + this.geometryFieldName + " is not of Geometry type.");
					}else{
						geometryField = field;
					}
				}	
									
			}
	    }

    	//Contents
	    Contents contents = null;
	    
	    //Si création de table
	    if(createTable){
			contents = new Contents();
			contents.setDescription(this.contentsDescription);
			contents.setIdentifier(this.contentsIdentifier);
			contents.setTableName(this.tableName);
		
			//Feature Table
	    	if(geometryField != null){

	    		//SRS
	    		mil.nga.geopackage.srs.SpatialReferenceSystem srs = null;
	    		if(this.assignedSrid !=null){

	    			//Inconnu geography/geometry
	    			if(this.assignedSrid == 0 || this.assignedSrid == -1){
	    				try {
	    					srs = geoPackage.getSpatialReferenceSystemDao().getOrCreateFromEpsg(this.assignedSrid);
	    				} catch (SQLException e) {
	    					throw new KettleException("Error writing features to " + this.gpkgFileName, e);
	    				}
	    				
	    			//Code EPSG à priori connu
	    			}else{
	    				
	    				try {
	    					srs = toGpkgSrs(this.assignedSrid);
	    					geoPackage.getSpatialReferenceSystemDao().createIfNotExists(srs);
	    				} catch (SQLException e) {
	    					throw new KettleException("Error writing features to " + this.gpkgFileName, e);
	    				}
	   				
	    			}
	    		
	    		//On fixe à -1 défaut
	    		}else{
	    			
	    			try {
						srs = geoPackage.getSpatialReferenceSystemDao().getOrCreateFromEpsg(-1);
					} catch (SQLException e) {
						throw new KettleException("Error writing features to " + this.gpkgFileName, e);
					}
	    		}

	    		//Contents
	    		contents.setDataType(ContentsDataType.FEATURES);
	    		contents.setSrs(srs);

	    		//geometryColumns
	    		GeometryColumns geometryColumns = new GeometryColumns();
	    		geometryColumns.setGeometryType(this.assignedGeometryType.toGpkgValue());
				geometryColumns.setColumnName(geometryField.getName());
				geometryColumns.setSrs(srs);
	    		
	    		//3D autorisé ?
	    		if(this.forceTo2DGeometry){
	    			geometryColumns.setZ(GpkgGeometryWithZType.PROHIBITED.toGpkgValue());
	    		}else{
	    			geometryColumns.setZ(GpkgGeometryWithZType.OPTIONAL.toGpkgValue());	
	    		}
	    		//M jamais géré
	    		geometryColumns.setM(GpkgGeometryWithMType.PROHIBITED.toGpkgValue());

	    		//Création des métadonnées pour la FeatureTable
	    		geometryColumns.setContents(contents);
	    		geoPackage.createFeatureTable(new FeatureTableMetadata(
	    			geometryColumns,
	    			this.pkFieldName,
	    			toGpkgFeatureColumns(otherFields),
	    			new BoundingBox()
					)
	    		);
	    		
	     		//Création d'index
	    		RTreeIndexExtension extension = new RTreeIndexExtension(geoPackage);
	    		extension.create(this.tableName,this.geometryFieldName, this.pkFieldName);

	    		
	    	//Attribute Table
	    	}else{
	 
	    		//Création des métadonnées pour la AttributeTable
				contents.setDataType(ContentsDataType.ATTRIBUTES);
				geoPackage.createAttributesTable(
					new AttributesTable(
						this.tableName,
						toGpkgAttributesColumns(otherFields)
					)
				);
	    	}
			
			//Sinon, récupère les métadonnées pour la table sans créer
	    }else{
	    
	    	ContentsDataType contentsDataType = null;
	    	if(geometryField != null){
	    		contentsDataType= ContentsDataType.FEATURES;
	    	}else {
	    		contentsDataType = ContentsDataType.ATTRIBUTES;
	    	}

	    	try {

				for(Contents currentContents : geoPackage.getContentsDao().getContents(contentsDataType)){

					if(currentContents.getTableName().equalsIgnoreCase(this.tableName)){
						contents = currentContents;
						break;
					}
				}
				
			} catch (SQLException e) {
				
				throw new KettleException("Error writing features to " + this.gpkgFileName, e);
			}
	    		
	    }

	    //Alimentation des tables
		if(features != null){

			//Feature Table
	    	if(geometryField != null){
	   
	    		//On stocke les srids des features pour mettre
	    		//éventuellement le contenu des tables de métadonnées à jour si srid non forcé
				TreeSet<Long> srids = new TreeSet<Long>();
				
				//Pour stockage de l'extent
				Envelope extent = null;

	    		FeatureDao featureDao = geoPackage.getFeatureDao(this.tableName);
	    		int featIndex = 0;
	    		
	    		try {
	    			
	    			if (this.commitLimit <= 0) {
	    				this.commitLimit = (long) 1;
	    	        }

					featureDao.getConnection().setAutoCommit(false);
					
				} catch (SQLException e) {
					throw new KettleException("Error writing features to " + this.gpkgFileName, e);
				}
	    		
	    		for(Feature feature : features){
	    			
	    			//Nouvelle ligne de données
	    			FeatureRow featureRow = featureDao.newRow();
	    			
	    			//Récupération de la géométrie
					Geometry geometry = (Geometry) feature.getValue(geometryField);
			
					
					//Si forcer en 2d
					if(this.forceTo2DGeometry){
						geometry = GeometryUtils.get2DGeometry(geometry);
					}
					
					//On essaye d'hamoniser le type ou on retounr une erreur		
					geometry = checkGeometryType(geometry, (Long) feature.getValue(pkField), this.tableName);
					
					//On recupère le srid stocké dans la géométrie JTS uniquement si non forcé
					Long currentSrid = GeometryUtils.getSrid(geometry).longValue();
					if(currentSrid != null && this.assignedSrid == null){
						srids.add(currentSrid);
						this.assignedSrid = currentSrid;
					}

					//Geométrie Geopackage
					GeoPackageGeometryData geometryData = new GeoPackageGeometryData(this.assignedSrid);
					geometryData.setGeometry(toGpkgGeometry(geometry));
					featureRow.setGeometry(geometryData);
					
					//Intialisation ou modification de l'étendue
					if(extent == null){
						extent = geometry.getEnvelopeInternal();
					}else{
						extent.expandToInclude(geometry.getEnvelopeInternal());
					}

					//Autres colonnes
					for(Field field : otherFields){
						featureRow.setValue(field.getName(), feature.getValue(field));
					}
									
					ContentValues contentValues = featureRow.toContentValues();
					Object pkFieldValue = feature.getValue(pkField);
					if(pkFieldValue !=null){
						contentValues.put(this.pkFieldName, pkFieldValue);
	    			}
					featureDao.insert(contentValues);
					featIndex++;
					
					if (featIndex == commitLimit) {
		            	try {
							featureDao.getConnection().commit();
						} catch (SQLException e) {
							throw new KettleException("Error writing features to " + this.gpkgFileName, e);
						}
		            	featIndex = 0;
		            }
					
					
	    		}

	    		try {
					featureDao.getConnection().commit();
				} catch (SQLException e) {
					throw new KettleException("Error writing features to " + this.gpkgFileName, e);
				}
	    		
	    		//Maj etendue
	    		if (extent !=null){
	    			
	    			BoundingBox boundingBox  = contents.getBoundingBox();
	    			if(boundingBox != null){

	    				Envelope currentExtent = GeoPackageReader.toJtsBounds(boundingBox);
	    				currentExtent.expandToInclude(extent);
	    				contents.setBoundingBox(toGpkgBounds(currentExtent));	
	    			
	    			}else{
	    				contents.setBoundingBox(toGpkgBounds(extent));	
	    			}
	    			
	    		}
	    		
	    		//Maj du srid car non forcé et détecté depuis les géométries
	    		if(srids.size() == 1){
	    			
	    			mil.nga.geopackage.srs.SpatialReferenceSystem srs = toGpkgSrs(srids.first());
		    		if (srs != null){
		    			try {
		    				srs = toGpkgSrs(this.assignedSrid);
	    					geoPackage.getSpatialReferenceSystemDao().createIfNotExists(srs);
						
		    			} catch (SQLException e) {
							throw new KettleException("Error writing features to " + this.gpkgFileName, e);
						}			    			
		    			
		    			try {
		    				
		    				GeometryColumns currentGeometryColumns = geoPackage.getGeometryColumnsDao().queryForTableName(this.tableName);
		    				currentGeometryColumns.setSrs(srs);
		    				geoPackage.getGeometryColumnsDao().update(currentGeometryColumns);
		    				contents.setSrs(srs);
		    				
		    			} catch (SQLException e) {
							throw new KettleException("Error writing features to " + this.gpkgFileName, e);
						}
			    		
			    		
		    		}
		    			    		
	    		}
	    	
	    	//Attributes Table
	    	}else{
	    		
	    		AttributesDao attributesDao = geoPackage.getAttributesDao(this.tableName);
	    		int featIndex = 0;
	    		try {
	    			
	    			if (this.commitLimit <= 0) {
	    				this.commitLimit = (long) 1;
	    	        }

	    			attributesDao.getConnection().setAutoCommit(false);
					
				} catch (SQLException e) {
					throw new KettleException("Error writing features to " + this.gpkgFileName, e);
				}
	    		
	    		for(Feature feature : features){
	    			
					AttributesRow attributesRow = attributesDao.newRow();
					
					for(Field field : otherFields){
						attributesRow.setValue(field.getName(), feature.getValue(field));
					}
					
					ContentValues contentValues = attributesRow.toContentValues();
					Object pkFieldValue = feature.getValue(pkField);
					if(pkFieldValue !=null){
						contentValues.put(this.pkFieldName, pkFieldValue);
	    			}
					attributesDao.insert(contentValues);
					
					if (featIndex == this.commitLimit) {
		            	try {
		            		attributesDao.getConnection().commit();
						} catch (SQLException e) {
							throw new KettleException("Error writing features to " + this.gpkgFileName, e);
						}
		            	featIndex = 0;
		            }
					
				}
	    		try {
					attributesDao.getConnection().commit();
				} catch (SQLException e) {
					throw new KettleException("Error writing features to " + this.gpkgFileName, e);
				}
	    		
	    	}

			contents.setLastChange(new Date());
			try {
				geoPackage.getContentsDao().update(contents);
			} catch (SQLException e) {
				throw new KettleException("Error writing features to " + this.gpkgFileName, e);
			}
			geoPackage.close();
			
		}

		geoPackage.close();


    }
	
	private List<FeatureColumn> toGpkgFeatureColumns(List<Field> fields){
		
		List<FeatureColumn> featureColumns = new ArrayList<FeatureColumn>();
		int i = 2;
		
		for(Field field : fields){
			featureColumns.add(FeatureColumn.createColumn(i, field.getName(), toGpkgDataType(field), false, null));
			i++;
		}
		
		return featureColumns;
		
	}
	
	private List<AttributesColumn> toGpkgAttributesColumns(List<Field> fields){
		
		List<AttributesColumn> attributesColumns = new ArrayList<AttributesColumn>();
		int i = 1;
		
		for(Field field : fields){
			attributesColumns.add(AttributesColumn.createColumn(i, field.getName(), toGpkgDataType(field), false, null));
			i++;
		}
		
		return attributesColumns;
		
	}
	
	private Geometry checkGeometryType(Geometry geometry, Long fid, String tableName) throws KettleException{
		
		//Pas de test : type générique
		if(this.assignedGeometryType.equals(GpkgGeometryType.GEOMETRY)){
			return geometry;
		}
				
		//Si géométrie simple et forcé à type simple : test adéquation
		if((this.assignedGeometryType.equals(GpkgGeometryType.POINT) && !(geometry instanceof Point))
			|| (this.assignedGeometryType.equals(GpkgGeometryType.LINESTRING) && !(geometry instanceof LineString))
			|| (this.assignedGeometryType.equals(GpkgGeometryType.POLYGON) && !(geometry instanceof Polygon))
		){			
			throw new KettleException("Error writing features to " + this.tableName + " in file " + this.gpkgFileName + ". The geometry of feature " + fid.toString() + " is not of type " + this.assignedGeometryType.toString());
		}
		
		//Si forcé à multi, test sur la géométrie forcé en multi-géométrie
		if((this.assignedGeometryType.equals(GpkgGeometryType.MULTIPOINT) && !(GeometryUtils.getMultiGeometry(geometry) instanceof MultiPoint))
				|| (this.assignedGeometryType.equals(GpkgGeometryType.MULTILINESTRING) && !(GeometryUtils.getMultiGeometry(geometry) instanceof MultiLineString))
				|| (this.assignedGeometryType.equals(GpkgGeometryType.MULTIPOLYGON) && !(GeometryUtils.getMultiGeometry(geometry) instanceof MultiPolygon))
			){			
				throw new KettleException("Error writing features to " + this.tableName + " in file " + this.gpkgFileName + ". The geometry of feature " + fid.toString() + " is not of type " + this.assignedGeometryType.toString());
			
			}else{
				
				//retourne la géométrie en multi
				if(this.assignedGeometryType.equals(GpkgGeometryType.MULTIPOINT)
					|| this.assignedGeometryType.equals(GpkgGeometryType.MULTILINESTRING)
					|| this.assignedGeometryType.equals(GpkgGeometryType.MULTIPOLYGON)){
					
					return GeometryUtils.getMultiGeometry(geometry);
					
				}
				
			}
		
		//Si collection
		if(this.assignedGeometryType.equals(GpkgGeometryType.GEOMETRYCOLLECTION)){
			
			return GeometryUtils.getGeometryCollection(geometry);
			
		}

		return null;

	}
	
	
	private GeoPackageDataType toGpkgDataType(Field field){

		GeoPackageDataType dataType = null;
		
		if(field.getType().equals(FieldType.BINARY)){
			dataType = GeoPackageDataType.BLOB;
			
		}else if (field.getType().equals(FieldType.BOOLEAN)){
			dataType = GeoPackageDataType.BOOLEAN;
			
		}else if (field.getType().equals(FieldType.DATE)){
			dataType = GeoPackageDataType.DATETIME;
			
		}else if (field.getType().equals(FieldType.DOUBLE)){
			dataType = GeoPackageDataType.DOUBLE;
			
		}else if (field.getType().equals(FieldType.LONG)){
			dataType = GeoPackageDataType.INTEGER;
			
		}else if (field.getType().equals(FieldType.STRING)){
			dataType = GeoPackageDataType.TEXT;
		}
		
		return dataType;
	}
	
	private static boolean hasZ(com.vividsolutions.jts.geom.Geometry geometry){
		
		if(geometry != null && !geometry.isEmpty()){
			return !Double.isNaN(geometry.getCoordinates()[0].z);
		}else{
			return false;
		}
	}
	
	
	/**
	 * srid number to GPKG Srs
	 * @param srid
	 * @return
	 * @throws KettleException 
	 * @throws CRSException 
	 */
	private mil.nga.geopackage.srs.SpatialReferenceSystem toGpkgSrs(long srid) throws KettleException{

		try {
			
			CRSFactory cRSFactory = new CRSFactory();
            RegistryManager registryManager = cRSFactory.getRegistryManager();
            registryManager.addRegistry(new EPSGRegistry());
			
        	CoordinateReferenceSystem crs = cRSFactory.getCRS("EPSG:" + srid);
		
			if (crs == null) {
            	throw new KettleException("Coordinate system  EPSG:" + srid + " not found");
            }
				
			mil.nga.geopackage.srs.SpatialReferenceSystem srs = new mil.nga.geopackage.srs.SpatialReferenceSystem();
    		srs.setSrsName(crs.getName());
    		srs.setId(Long.parseLong(crs.getAuthorityKey()));
    		srs.setOrganization(crs.getAuthorityName());
    		srs.setOrganizationCoordsysId(Long.parseLong(crs.getAuthorityKey()));
    		srs.setDefinition(crs.toWKT());
    		return srs;
		
		} catch (CRSException e) {
			throw new KettleException("Coordinate system  EPSG:" + srid + " error");
		}
        
	}
	
	
	
	
	/**
	 * JTS Geometry to GPKG Bounds
	 * @param Envelope
	 * @return
	 */
	public static mil.nga.geopackage.BoundingBox toGpkgBounds(Envelope bounds){
		
		mil.nga.geopackage.BoundingBox gpkgBounds = new mil.nga.geopackage.BoundingBox();
		gpkgBounds.setMinLongitude(bounds.getMinX());
		gpkgBounds.setMinLatitude(bounds.getMinY());
		gpkgBounds.setMaxLongitude(bounds.getMaxX());
		gpkgBounds.setMaxLatitude(bounds.getMaxY());
		return  gpkgBounds;
		
	}


	/**
	 * JTS Geometry to GPKG Geometry
	 * @param jtsGeometry
	 * @return
	 */
	private static mil.nga.sf.Geometry toGpkgGeometry(com.vividsolutions.jts.geom.Geometry geometry){
		
		mil.nga.sf.Geometry gpkgGeometry = null;
				
		if(geometry != null && !geometry.isEmpty()){

			//Point
			if (geometry.getGeometryType().equals(com.vividsolutions.jts.geom.Point.class.getSimpleName())){
				return toGpkgPoint((com.vividsolutions.jts.geom.Point) geometry);
			
			//MultiPoint
			}else if (geometry.getGeometryType().equals(com.vividsolutions.jts.geom.MultiPoint.class.getSimpleName())){
				return toGpkgMultiPoint((com.vividsolutions.jts.geom.MultiPoint) geometry);
			
			//LineString
			}else if (geometry.getGeometryType().equals(com.vividsolutions.jts.geom.LineString.class.getSimpleName())){
				return toGpkgLineString((com.vividsolutions.jts.geom.LineString) geometry);
				
			//MultiLineString	
			}else if (geometry.getGeometryType().equals(com.vividsolutions.jts.geom.MultiLineString.class.getSimpleName())){
				return toGpkgMultiLineString((com.vividsolutions.jts.geom.MultiLineString) geometry);
				
			//Polygon		
			}else if (geometry.getGeometryType().equals(com.vividsolutions.jts.geom.Polygon.class.getSimpleName())){
				return toGpkgPolygon((com.vividsolutions.jts.geom.Polygon) geometry);
			
			//MultiPolygon	
			}else if(geometry.getGeometryType().equals(com.vividsolutions.jts.geom.MultiPolygon.class.getSimpleName())){
				return toGpkgMultiPolygon((com.vividsolutions.jts.geom.MultiPolygon) geometry);
	
			//GeometryCollection	
			}else if(geometry.getGeometryType().equals(com.vividsolutions.jts.geom.GeometryCollection.class.getSimpleName())){

				mil.nga.sf.GeometryCollection<mil.nga.sf.Geometry> gpkgGeometryCollection = new mil.nga.sf.GeometryCollection<mil.nga.sf.Geometry>(hasZ(geometry),false);
				for(int i = 0; i < geometry.getNumGeometries(); i++){
					gpkgGeometryCollection.addGeometry(toGpkgGeometry(geometry.getGeometryN(i)));
				}

				return gpkgGeometryCollection;
				
			}
		
		}
		
		return gpkgGeometry;
	}
		
	/**
	 * JTS Coordinate to Gpkg Point
	 * @param coordinate
	 * @return
	 */
	private static mil.nga.sf.Point toGpkgPoint(com.vividsolutions.jts.geom.Coordinate coordinate){
		
		mil.nga.sf.Point gpkgPoint = new mil.nga.sf.Point(coordinate.x, coordinate.y);
		if(!Double.isNaN(coordinate.z)){
			gpkgPoint.setZ(coordinate.z);
		}
		
		return gpkgPoint;
		
	}
	
	/**
	 * JTS Coordinates to Gpkg Points
	 * @param coordinates
	 * @return
	 */
	private static List<mil.nga.sf.Point> toGpkgPoints(com.vividsolutions.jts.geom.Coordinate[] coordinates){
		
		List<mil.nga.sf.Point> points = new ArrayList<mil.nga.sf.Point>();
		for(Coordinate coordinate : coordinates){
			points.add(toGpkgPoint(coordinate));
		}
				
		return points;
		
	}
		
	/**
	 * JTS Point to Gpkg Point
	 * @param point
	 * @return
	 */
	private static mil.nga.sf.Point toGpkgPoint(com.vividsolutions.jts.geom.Point point){

		return toGpkgPoint(point.getCoordinate());
		
	}
	
	/**
	 * JTS MultiPoint to Gpkg MultiPoint
	 * @param multiPoint
	 * @return
	 */
	private static mil.nga.sf.MultiPoint toGpkgMultiPoint(com.vividsolutions.jts.geom.MultiPoint multiPoint){
		
		mil.nga.sf.MultiPoint gpkgMultiPoint = new mil.nga.sf.MultiPoint(hasZ(multiPoint),false);
		gpkgMultiPoint.setPoints(toGpkgPoints(multiPoint.getCoordinates()));
		return gpkgMultiPoint;
		
	}
	
	/**
	 * JTS LineString to Gpkg LineString
	 * @param lineString
	 * @return
	 */
	private static mil.nga.sf.LineString toGpkgLineString(com.vividsolutions.jts.geom.LineString lineString){
		
		mil.nga.sf.LineString gpkgLineString = new mil.nga.sf.LineString(hasZ(lineString),false);
		gpkgLineString.setPoints(toGpkgPoints(lineString.getCoordinates()));
		return gpkgLineString;
		
	}
	
	/**
	 * JTS MultiLineString to Gpkg MultiLineString
	 * @param multiLineString
	 * @return
	 */
	private static mil.nga.sf.MultiLineString toGpkgMultiLineString(com.vividsolutions.jts.geom.MultiLineString multiLineString){
		
		mil.nga.sf.MultiLineString gpkgMultiLineString = new mil.nga.sf.MultiLineString(hasZ(multiLineString),false);
		for(int i = 0; i < multiLineString.getNumGeometries(); i++){
			gpkgMultiLineString.addLineString(toGpkgLineString((LineString) multiLineString.getGeometryN(i)));
		}

		return gpkgMultiLineString;
		
	}
		
	/**
	 * JTS Polygon to Gpkg Polygon
	 * @param polygon
	 * @return
	 */
	private static mil.nga.sf.Polygon toGpkgPolygon(com.vividsolutions.jts.geom.Polygon polygon){
		
		mil.nga.sf.Polygon gpkgPolygon = new mil.nga.sf.Polygon(hasZ(polygon),false);
		gpkgPolygon.addRing(toGpkgLineString(polygon.getExteriorRing()));
		for(int i = 0; i < polygon.getNumInteriorRing(); i++){
			gpkgPolygon.addRing(toGpkgLineString(polygon.getInteriorRingN(i)));
		}

		return gpkgPolygon;
		
	}
	
	/**
	 * JTS MultiPolygon to Gpkg MultiPolygon
	 * @param multiPolygon
	 * @return
	 */
	private static mil.nga.sf.MultiPolygon toGpkgMultiPolygon(com.vividsolutions.jts.geom.MultiPolygon multiPolygon){
		
		mil.nga.sf.MultiPolygon gpkgMultiPolygon = new mil.nga.sf.MultiPolygon(hasZ(multiPolygon),false);
		for(int i = 0; i < multiPolygon.getNumGeometries(); i++){
			gpkgMultiPolygon.addPolygon(toGpkgPolygon((Polygon) multiPolygon.getGeometryN(i)));
		}

		return gpkgMultiPolygon;
		
	}

}
