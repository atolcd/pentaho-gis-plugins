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


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.pentaho.di.core.exception.KettleException;
import org.wololo.geojson.Crs;
import org.wololo.geojson.GeoJSON;

import com.atolcd.pentaho.di.gis.io.features.Feature;
import com.atolcd.pentaho.di.gis.io.features.Field;
import com.atolcd.pentaho.di.gis.io.features.Field.FieldType;
import com.atolcd.pentaho.di.gis.utils.GeometryUtils;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

public class GeoJSONWriter extends AbstractFileWriter {

    private static GeometryFactory geometryFactory = new GeometryFactory();
    private String geoJsonFileName;

    private Writer writer;
    private boolean isServletOutput;

    private String featureIdField;

    public GeoJSONWriter(String fileName, String geometryFieldName, String charsetName) throws KettleException {

        super(geometryFieldName, charsetName);
        this.geoJsonFileName = checkFilename(fileName).getFile();
        this.isServletOutput = false;
        this.writer = null;
        this.featureIdField = null;
    }

    public GeoJSONWriter(Writer writer, String geometryFieldName, String charsetName) throws KettleException {

        super(geometryFieldName, charsetName);
        this.geoJsonFileName = null;
        this.isServletOutput = true;
        this.writer = writer;
        this.featureIdField = null;

    }

    public String getFeatureIdField() {
        return featureIdField;
    }

    public void setFeatureIdField(String featureIdField) {
        this.featureIdField = featureIdField;
    }

    @SuppressWarnings("unchecked")
    public void writeFeatures(List<Feature> features) throws KettleException {

        // Récupération des champs utilisés
        Field geometryField = null;
        Field idField = null;

        org.wololo.jts2geojson.GeoJSONWriter geoJSONWriter = new org.wololo.jts2geojson.GeoJSONWriter();
        List<org.wololo.geojson.Feature> geoJsonfeatures = new ArrayList<org.wololo.geojson.Feature>();

        // Boucle sur chaque feature
        Geometry geometries[] = new Geometry[features.size()];
        int i = 0;
        boolean first = true;
        for (Feature feature : features) {

            if (first) {

                geometryField = feature.getField(this.geometryFieldName);

                if (featureIdField != null) {
                    idField = feature.getField(this.featureIdField);
                }

                first = false;
            }

            // Récupération de la géométrie
            Geometry geometry = (Geometry) feature.getValue(geometryField);
            geometries[i] = geometry;

            // Récupération des attribust
            Map<String, Object> properties = new HashMap<String, Object>();

            // id de feature
            if (featureIdField != null) {
                String id = (String) feature.getValue(idField);
                if (id != null) {
                    properties.put("id", id);
                }

            }

            Iterator<Field> fieldIt = this.fields.iterator();
            while (fieldIt.hasNext()) {

                Field field = fieldIt.next();
                if (!field.getType().equals(FieldType.GEOMETRY)) {

                    if (!field.getName().equalsIgnoreCase(featureIdField)) {
                        properties.put(field.getName(), feature.getValue(field));
                    }
                }

            }

            // Ajout à la collection
            geoJsonfeatures.add(new org.wololo.geojson.Feature(geoJSONWriter.write(geometry), properties));

            i++;

        }

        // Vérification de la présence de" plusieurs srid
        HashMap<String, Object> geometryCollectionInfos = GeometryUtils.getGeometryCollectionInfos(geometryFactory.createGeometryCollection(geometries));
        List<Integer> srids = (List<Integer>) geometryCollectionInfos.get("SRIDS");
        Crs crs = null;

        if (srids.size() > 1) {
            throw new KettleException("Error writing features to " + this.geoJsonFileName + " : Mixed SRID are not supported " + srids.toString());
        } else {

            if (srids != null && !(srids.get(0) != null) && !srids.get(0).equals(0)) {

                HashMap<String, Object> crsProperties = new HashMap<String, Object>();
                crsProperties.put("name", "urn:ogc:def:crs:EPSG::" + srids.get(0));
                crs = new Crs("name", crsProperties);

            }
        }

        GeoJSON json = geoJSONWriter.write(geoJsonfeatures, crs);
        if (isServletOutput) {

            try {

                if (geoJsonfeatures.size() > 0) {
                    writer.write(json.toString());
                }

            } catch (IOException e) {
                throw new KettleException("Error writing features to servlet", e);
            }

        } else {

            try {

                PrintWriter printWriter = new PrintWriter(this.geoJsonFileName, this.charset.name());
                printWriter.print(json);
                printWriter.close();

            } catch (FileNotFoundException e) {
                throw new KettleException("Error writing features to " + this.geoJsonFileName, e);
            } catch (IOException e) {
                throw new KettleException("Error writing features to " + this.geoJsonFileName, e);
            }

        }

    }

}
