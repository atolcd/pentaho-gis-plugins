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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.List;

import org.pentaho.di.core.exception.KettleException;

import com.atolcd.pentaho.di.gis.io.features.Feature;
import com.atolcd.pentaho.di.gis.io.features.Field;
import com.atolcd.pentaho.di.gis.io.features.Field.FieldType;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import de.micromata.opengis.kml.v_2_2_0.AltitudeMode;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.ExtendedData;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Schema;
import de.micromata.opengis.kml.v_2_2_0.SchemaData;
import de.micromata.opengis.kml.v_2_2_0.SimpleData;
import de.micromata.opengis.kml.v_2_2_0.SimpleField;

public class KMLWriter extends AbstractFileWriter {

    private Writer writer;
    private boolean isServletOutput;

    private String kmlFileName;

    private boolean forceTo2DGeometry;
    private boolean exportWithAttributs;
    private String documentName;
    private String documentDescription;
    private String featureNameField;
    private String featureDescriptionField;

    public KMLWriter(String fileName, String geometryFieldName, String charsetName) throws KettleException {

        super(geometryFieldName, charsetName);

        this.kmlFileName = checkFilename(fileName).getFile();
        this.forceTo2DGeometry = false;
        this.documentName = null;
        this.documentDescription = null;
        this.isServletOutput = false;
        this.writer = null;
        this.exportWithAttributs = false;
        this.featureNameField = null;
        this.featureDescriptionField = null;

    }

    public KMLWriter(Writer writer, String geometryFieldName, String charsetName) throws KettleException {

        super(geometryFieldName, charsetName);
        this.kmlFileName = null;
        this.forceTo2DGeometry = false;
        this.documentName = null;
        this.documentDescription = null;
        this.isServletOutput = true;
        this.writer = writer;
        this.exportWithAttributs = false;
        this.featureNameField = null;
        this.featureDescriptionField = null;

    }

    public boolean isForceTo2DGeometry() {
        return forceTo2DGeometry;
    }

    public void setForceTo2DGeometry(boolean forceTo2DGeometry) {
        this.forceTo2DGeometry = forceTo2DGeometry;
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

    public boolean isExportWithAttributs() {
        return exportWithAttributs;
    }

    public void setExportWithAttributs(boolean exportWithAttributs) {
        this.exportWithAttributs = exportWithAttributs;
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

    public void writeFeatures(List<Feature> features) throws KettleException {

        Kml kml = new Kml();
        Document document = kml.createAndSetDocument();

        if (this.documentName != null) {
            document.setName(documentName);
        }

        if (this.documentDescription != null) {
            document.setDescription(documentDescription);
        }

        // Si export des attributs
        if (exportWithAttributs) {

            Schema schema = document.createAndAddSchema();
            schema.setId("dataSchema");
            schema.setName("");

            Iterator<Field> fieldIt = this.fields.iterator();
            while (fieldIt.hasNext()) {

                Field field = fieldIt.next();

                // Pas pris en compte ici : une seule géométrie
                if (!field.getType().equals(FieldType.GEOMETRY)) {

                    SimpleField simpleField = schema.createAndAddSimpleField();
                    simpleField.setName(field.getName());

                    // Texte
                    if (field.getType().equals(FieldType.STRING)) {

                        simpleField.setType("string");

                        // Date
                    } else if (field.getType().equals(FieldType.DATE)) {

                        simpleField.setType("date");

                        // Entier
                    } else if (field.getType().equals(FieldType.LONG)) {

                        simpleField.setType("int");

                        // Double
                    } else if (field.getType().equals(FieldType.DOUBLE)) {

                        simpleField.setType("float");

                        // Booléen
                    } else if (field.getType().equals(FieldType.BOOLEAN)) {

                        simpleField.setType("bool");

                        // Autres types
                    } else {
                        simpleField.setType("string");
                    }

                }

            }

        }

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

            Geometry geometry = (Geometry) feature.getValue(geometryField);
            Envelope envelope = geometry.getEnvelopeInternal();

            // Vérification de l'emprise : doit être en WGS 84
            if (envelope.getMaxX() > 180 || envelope.getMinX() < -180 || envelope.getMaxY() > 90 || envelope.getMinY() < -90) {

                throw new KettleException("Bad coordinates for WGS84 system");

            }

            Placemark placemark = document.createAndAddPlacemark();

            // Nom de feature
            if (featureNameField != null) {
                String name = (String) feature.getValue(nameField);
                if (name != null) {
                    placemark.setName(name);
                }

            }

            // Description de feature
            if (featureDescriptionField != null) {
                String description = (String) feature.getValue(descriptionField);
                if (description != null) {
                    placemark.setDescription(description);
                }
            }

            // Attributs
            if (exportWithAttributs) {
                ExtendedData extendedData = placemark.createAndSetExtendedData();
                SchemaData schemaData = extendedData.createAndAddSchemaData();
                schemaData.setSchemaUrl("dataSchema");

                Iterator<Field> colIt = this.fields.iterator();
                while (colIt.hasNext()) {

                    Field field = colIt.next();
                    if (!field.getType().equals(FieldType.GEOMETRY)) {

                        Object value = feature.getValue(field);
                        SimpleData simpleData = schemaData.createAndAddSimpleData(field.getName());
                        simpleData.setValue(String.valueOf(value));
                    }

                }
            }

            // En fonction dy type de géométrie Jts, appel
            // aux fonctions de conversion en géométries Kml

            // POINT
            if (geometry instanceof Point) {

                placemark.setGeometry(getAsKmlPoint((Point) geometry));

                // LINESTRING
            } else if (geometry instanceof LineString) {

                placemark.setGeometry(getAsKmlLineString((LineString) geometry));

                // POLYGON
            } else if (geometry instanceof Polygon) {

                placemark.setGeometry(getAsKmlPolygon((Polygon) geometry));

                // MULTIPOINT
            } else if (geometry instanceof MultiPoint) {

                de.micromata.opengis.kml.v_2_2_0.MultiGeometry kmlMultiGeometry = placemark.createAndSetMultiGeometry();
                for (int i = 0; i < geometry.getNumGeometries(); i++) {

                    kmlMultiGeometry.addToGeometry(getAsKmlPoint((Point) ((Point) geometry).getGeometryN(i)));

                }
                // MULTILINESTRING
            } else if (geometry instanceof MultiLineString) {

                de.micromata.opengis.kml.v_2_2_0.MultiGeometry kmlMultiGeometry = placemark.createAndSetMultiGeometry();
                for (int i = 0; i < geometry.getNumGeometries(); i++) {

                    kmlMultiGeometry.addToGeometry(getAsKmlLineString((LineString) ((MultiLineString) geometry).getGeometryN(i)));

                }
                // MULTIPOLYGON
            } else if (geometry instanceof MultiPolygon) {

                de.micromata.opengis.kml.v_2_2_0.MultiGeometry kmlMultiGeometry = placemark.createAndSetMultiGeometry();
                for (int i = 0; i < geometry.getNumGeometries(); i++) {

                    kmlMultiGeometry.addToGeometry(getAsKmlPolygon((Polygon) ((MultiPolygon) geometry).getGeometryN(i)));

                }
                // GEOMETRYCOLLECTION
            } else if (geometry instanceof GeometryCollection) {

                de.micromata.opengis.kml.v_2_2_0.MultiGeometry kmlMultiGeometry = placemark.createAndSetMultiGeometry();
                for (int i = 0; i < geometry.getNumGeometries(); i++) {

                    Geometry currentGeometry = geometry.getGeometryN(i);

                    if (currentGeometry instanceof Point) {
                        kmlMultiGeometry.addToGeometry(getAsKmlPoint((Point) currentGeometry));
                    } else if (currentGeometry instanceof LineString) {
                        kmlMultiGeometry.addToGeometry(getAsKmlLineString((LineString) currentGeometry));
                    } else if (currentGeometry instanceof Polygon) {
                        kmlMultiGeometry.addToGeometry(getAsKmlPolygon((Polygon) currentGeometry));
                    } else if (currentGeometry instanceof MultiPoint) {

                        for (int j = 0; j < currentGeometry.getNumGeometries(); j++) {
                            kmlMultiGeometry.addToGeometry(getAsKmlPoint((Point) ((Point) currentGeometry).getGeometryN(j)));
                        }

                    } else if (currentGeometry instanceof MultiLineString) {

                        for (int j = 0; j < currentGeometry.getNumGeometries(); j++) {
                            kmlMultiGeometry.addToGeometry(getAsKmlLineString((LineString) ((LineString) currentGeometry).getGeometryN(j)));
                        }

                    } else if (currentGeometry instanceof MultiPolygon) {

                        for (int j = 0; j < currentGeometry.getNumGeometries(); j++) {
                            kmlMultiGeometry.addToGeometry(getAsKmlPolygon((Polygon) ((Polygon) currentGeometry).getGeometryN(j)));
                        }

                    }

                }

            }

        }

        if (isServletOutput) {

            if (features.size() > 0) {
                kml.marshal();
                kml.marshal(writer);
            }

        } else {

            try {

                FileOutputStream fileOutputStream = new FileOutputStream(this.kmlFileName);
                kml.marshal();
                kml.marshal(fileOutputStream);
                fileOutputStream.close();

            } catch (FileNotFoundException e) {
                throw new KettleException("Error writing features to " + this.kmlFileName, e);
            } catch (IOException e) {
                throw new KettleException("Error writing features to " + this.kmlFileName, e);
            }

        }
    }

    /**
     * Conversion de Jts Point en Kml Point
     * 
     * @param point
     * @return
     */
    private de.micromata.opengis.kml.v_2_2_0.Point getAsKmlPoint(Point point) {

        de.micromata.opengis.kml.v_2_2_0.Point kmlPoint = new de.micromata.opengis.kml.v_2_2_0.Point();
        Coordinate coordinate = point.getCoordinates()[0];
        if (!Double.isNaN(coordinate.z) && !this.forceTo2DGeometry) {
            kmlPoint.addToCoordinates(coordinate.x, coordinate.y, coordinate.z);
        } else {
            kmlPoint.addToCoordinates(coordinate.x, coordinate.y);
        }

        return kmlPoint;

    }

    /**
     * Creation d'un ring Kml à partir tableau de JTS coordinnates
     * 
     * @param coordinates
     * @return
     */
    private de.micromata.opengis.kml.v_2_2_0.LinearRing getAsKmlRing(Coordinate[] coordinates) {

        de.micromata.opengis.kml.v_2_2_0.LinearRing kmlRing = new de.micromata.opengis.kml.v_2_2_0.LinearRing();
        for (int i = 0; i < coordinates.length; i++) {

            Coordinate coordinate = coordinates[i];
            if (!Double.isNaN(coordinate.z) && !this.forceTo2DGeometry) {
                kmlRing.addToCoordinates(coordinate.x, coordinate.y, coordinate.z);
            } else {
                kmlRing.addToCoordinates(coordinate.x, coordinate.y);
            }

        }

        return kmlRing;

    }

    /**
     * Conversion de Jts LineString en Kml LineString
     * 
     * @param lineString
     * @return
     */
    private de.micromata.opengis.kml.v_2_2_0.LineString getAsKmlLineString(LineString lineString) {

        de.micromata.opengis.kml.v_2_2_0.LineString kmlLineString = new de.micromata.opengis.kml.v_2_2_0.LineString();
        for (int i = 0; i < lineString.getCoordinates().length; i++) {

            Coordinate coordinate = lineString.getCoordinates()[i];
            if (!Double.isNaN(coordinate.z) && !this.forceTo2DGeometry) {
                kmlLineString.addToCoordinates(coordinate.x, coordinate.y, coordinate.z);
            } else {
                kmlLineString.addToCoordinates(coordinate.x, coordinate.y);
            }

        }
        return kmlLineString.withAltitudeMode(AltitudeMode.ABSOLUTE);

    }

    /**
     * Conversion de Jts Polygon en Kml Polygon
     * 
     * @param polygon
     * @return
     */
    private de.micromata.opengis.kml.v_2_2_0.Polygon getAsKmlPolygon(Polygon polygon) {

        de.micromata.opengis.kml.v_2_2_0.Polygon kmlPolygon = new de.micromata.opengis.kml.v_2_2_0.Polygon();

        // Ring exterieur
        LineString jtsExteriorRing = polygon.getExteriorRing();
        kmlPolygon.createAndSetOuterBoundaryIs().setLinearRing(getAsKmlRing(jtsExteriorRing.getCoordinates()));

        // Rings interieurs
        for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
            kmlPolygon.createAndAddInnerBoundaryIs().setLinearRing(getAsKmlRing(polygon.getInteriorRingN(i).getCoordinates()));
        }

        return kmlPolygon;
    }

}
