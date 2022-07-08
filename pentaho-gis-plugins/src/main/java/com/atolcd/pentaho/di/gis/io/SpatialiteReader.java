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
import java.util.ArrayList;
import java.util.List;

import org.pentaho.di.core.exception.KettleException;

import com.atolcd.gis.spatialite.Database;
import com.atolcd.gis.spatialite.Row;
import com.atolcd.gis.spatialite.Table;
import com.atolcd.pentaho.di.gis.io.features.Feature;
import com.atolcd.pentaho.di.gis.io.features.Field;
import com.atolcd.pentaho.di.gis.io.features.Field.FieldType;
import com.atolcd.pentaho.di.gis.utils.GeometryUtils;
import com.vividsolutions.jts.geom.Geometry;

public class SpatialiteReader extends AbstractFileReader {

    private String spatialiteFileName;
    private boolean spatialiteFileExist;
    private Database database;
    private boolean listContent;

    public SpatialiteReader(String fileName, String tableName, String charsetName) throws KettleException {

        super(tableName, null, charsetName);

        this.spatialiteFileExist = new File(checkFilename(fileName)).exists();

        if (!this.spatialiteFileExist) {
            throw new KettleException("Missing " + fileName + " file");
        } else {
            this.spatialiteFileName = checkFilename(fileName);
        }

        if(tableName.equalsIgnoreCase("*")){
            this.listContent = true;
        }else{
            this.listContent = false;
        } 

        this.database = new Database();

        try {

            database.open(this.spatialiteFileName);

            //Si liste de contenu
        	if(this.listContent){

        		this.fields.add(new Field("table_name",FieldType.STRING,null,null));
        		this.fields.add(new Field("is_spatial",FieldType.BOOLEAN,null,null));
        		
        	}else{
                Table dbTable = database.getTable(this.layerName);

                //Si table non trouvée
	        	if(dbTable == null){
	        		throw new KettleException("Error initialize reader : Table " + this.layerName + " not found");
	        	}

                for (com.atolcd.gis.spatialite.Field dbField : dbTable.getFields()) {

                    Field field = null;

                    if (dbField.getTypeAffinity().equalsIgnoreCase(com.atolcd.gis.spatialite.Field.TYPE_GEOMETRY)) {
                        field = new Field(dbField.getName(), FieldType.GEOMETRY, null, null);

                    } else if (dbField.getTypeAffinity().equalsIgnoreCase(com.atolcd.gis.spatialite.Field.TYPE_INTEGER)) {
                        field = new Field(dbField.getName(), FieldType.LONG, null, null);

                    } else if (dbField.getTypeAffinity().equalsIgnoreCase(com.atolcd.gis.spatialite.Field.TYPE_NONE)) {
                        field = new Field(dbField.getName(), FieldType.BINARY, null, null);

                    } else if (dbField.getTypeAffinity().equalsIgnoreCase(com.atolcd.gis.spatialite.Field.TYPE_NUMERIC)) {
                        field = new Field(dbField.getName(), FieldType.DOUBLE, null, null);

                    } else if (dbField.getTypeAffinity().equalsIgnoreCase(com.atolcd.gis.spatialite.Field.TYPE_REAL)) {

                        field = new Field(dbField.getName(), FieldType.DOUBLE, null, null);

                    } else if (dbField.getTypeAffinity().equalsIgnoreCase(com.atolcd.gis.spatialite.Field.TYPE_TEXT)) {

                        field = new Field(dbField.getName(), FieldType.STRING, null, null);
                    }

                    this.fields.add(field);
                }
            }

            database.close();

        } catch (ClassNotFoundException e) {
            throw new KettleException("Error initialize reader", e);
        } catch (SQLException e) {
            throw new KettleException("Error initialize reader", e);
        }

    }

    public List<Feature> getFeatures() throws KettleException {

        List<Feature> features = new ArrayList<Feature>();

        try {

            database.open(this.spatialiteFileName);

            //Si liste de contenu
            if(this.listContent){
            	
            	for(Table dbTable : database.getTables()){
            		
            		  Feature feature = new Feature();
            		  feature.addValue(this.getField("table_name"), dbTable.getName());
            		  feature.addValue(this.getField("is_spatial"), dbTable.isSpatial());
            		  features.add(feature);
            	}
            	
            	
            }else{
                Table dbTable = database.getTable(this.layerName);

                for (Row row : this.database.getRows(dbTable, limit)) {

                    Feature feature = new Feature();
                    for (Field field : this.fields) {
                        Object value = row.getValue(field.getName());
	
	                	if(field.getType().equals(FieldType.GEOMETRY)){
	                		if (this.forceTo2DGeometry) {
	                			value = GeometryUtils.get2DGeometry((Geometry) value);
	                		}
	
	                		if (this.forceToMultiGeometry) {
	                			value = GeometryUtils.getMultiGeometry((Geometry) value);
	                		}
	                	}

                        feature.addValue(field, row.getValue(field.getName()));
                    }
                    features.add(feature);

                }
            }

            database.close();

        } catch (ClassNotFoundException e) {
            throw new KettleException("Error reading features" + this.spatialiteFileName, e);
        } catch (SQLException e) {
            throw new KettleException("Error reading features" + this.spatialiteFileName, e);
        } catch (Exception e) {
            throw new KettleException("Error reading features" + this.spatialiteFileName, e);
        }
        return features;

    }

}
