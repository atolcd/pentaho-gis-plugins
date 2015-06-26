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


import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.cts.CRSFactory;
import org.cts.crs.CoordinateReferenceSystem;
import org.cts.registry.EPSGRegistry;
import org.cts.registry.RegistryManager;
import org.geotools.dbffile.DbfFieldDef;
import org.geotools.dbffile.DbfFile;
import org.geotools.dbffile.DbfFileWriter;
import org.geotools.shapefile.Shapefile;
import org.pentaho.di.core.exception.KettleException;

import com.atolcd.pentaho.di.gis.io.features.Feature;
import com.atolcd.pentaho.di.gis.io.features.Field;
import com.atolcd.pentaho.di.gis.io.features.Field.FieldType;
import com.atolcd.pentaho.di.gis.utils.GeometryUtils;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jump.io.EndianDataOutputStream;

public class ShapefileWriter extends AbstractFileWriter {

    private static GeometryFactory geometryFactory = new GeometryFactory();

    private String shpFileName;
    private String shxFileName;
    private String dbfFileName;
    private String prjFileName;
    boolean multiPointShapefile;
    boolean zOnlyShapefile;
    boolean forceTo2DGeometry;
    boolean createPrjFile;
    private int srid;

    public ShapefileWriter(String fileName, String geometryFieldName, String charsetName) throws KettleException {

        super(geometryFieldName, charsetName);

        this.shpFileName = checkFilename(fileName).getFile();
        this.dbfFileName = checkFilename(replaceFileExtension(fileName, ".shp", ".dbf")).getFile();
        this.shxFileName = checkFilename(replaceFileExtension(fileName, ".shp", ".shx")).getFile();
        this.prjFileName = checkFilename(replaceFileExtension(fileName, ".shp", ".prj")).getFile();
        this.forceTo2DGeometry = false;
        this.createPrjFile = false;

        // Témoin de présence d'au moins 1 multipoint
        this.zOnlyShapefile = false;

        // Témoin de présence d'au moins 1 multipoint
        this.multiPointShapefile = false;

    }

    public boolean isForceTo2DGeometry() {
        return forceTo2DGeometry;
    }

    public void setForceTo2DGeometry(boolean forceTo2DGeometry) {
        this.forceTo2DGeometry = forceTo2DGeometry;
    }

    public boolean isCreatePrjFile() {
        return createPrjFile;
    }

    public void setCreatePrjFile(boolean createPrjFile) {
        this.createPrjFile = createPrjFile;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void writeFeatures(List<Feature> features) throws KettleException {

        try {

            // Structure DBF
            List<DbfFieldDef> dbfFields = new ArrayList<DbfFieldDef>();
            Iterator<Field> fieldIt = this.fields.iterator();
            while (fieldIt.hasNext()) {

                DbfFieldDef dbfField = null;
                Field field = fieldIt.next();

                // Geometrie
                if (field.getType().equals(FieldType.GEOMETRY)) {

                    // Pas pris en compte dans le DBF

                    // Texte
                } else if (field.getType().equals(FieldType.STRING)) {

                    // Longueur définie
                    if (field.getLength() != null && field.getLength() <= 255) {

                        dbfField = new DbfFieldDef(field.getName(), 'C', field.getLength(), 0);

                    } else {
                        dbfField = new DbfFieldDef(field.getName(), 'C', 255, 0);
                    }

                    // Date
                } else if (field.getType().equals(FieldType.DATE)) {

                    dbfField = new DbfFieldDef(field.getName(), 'D', 8, 0);

                    // Entier
                } else if (field.getType().equals(FieldType.LONG)) {

                    if (field.getLength() != null && field.getLength() <= 10) {

                        dbfField = new DbfFieldDef(field.getName(), 'N', field.getLength(), 0);

                    } else {
                        dbfField = new DbfFieldDef(field.getName(), 'N', 10, 0);
                    }

                    // Double
                } else if (field.getType().equals(FieldType.DOUBLE)) {

                    int maxLength = 20;
                    int maxDecimalCount = 15;

                    if (field.getLength() != null && field.getLength() <= maxLength) {
                        maxLength = field.getLength();
                    }

                    if (field.getDecimalCount() != null && field.getDecimalCount() <= maxDecimalCount) {
                        maxDecimalCount = field.getDecimalCount();
                    }

                    dbfField = new DbfFieldDef(field.getName(), 'N', maxLength, maxDecimalCount);

                    // Booléen
                } else if (field.getType().equals(FieldType.BOOLEAN)) {

                    dbfField = new DbfFieldDef(field.getName(), 'L', 1, 0);

                    // Autres types
                } else {
                    dbfField = new DbfFieldDef(field.getName(), 'C', 255, 0);
                }

                if (dbfField != null) {
                    dbfFields.add(dbfField);
                }

            }

            // Boucle sur chaque feature
            Geometry[] geometries = new Geometry[features.size()];
            Vector[] rows = new Vector[features.size()];
            Iterator<Feature> featureIt = features.iterator();
            int i = 0;
            while (featureIt.hasNext()) {

                Feature feature = featureIt.next();

                // Récupération de la géométrie
                Geometry geometry = (Geometry) feature.getValue(feature.getField(this.geometryFieldName));
                if (geometry instanceof LineString || geometry instanceof Polygon) {
                    geometry = GeometryUtils.getMultiGeometry(geometry);
                }
                geometries[i] = geometry;

                Vector row = new Vector();
                Iterator<Field> colIt = this.fields.iterator();

                while (colIt.hasNext()) {

                    Field field = colIt.next();
                    Object value = feature.getValue(field);

                    // Entier
                    if (field.getType().equals(FieldType.LONG)) {

                        if (value != null) {
                            row.add(Long.parseLong(String.valueOf(value)));
                        } else {
                            row.add(new Long(0));
                        }

                        // Double
                    } else if (field.getType().equals(FieldType.DOUBLE)) {

                        if (value != null) {
                            row.add((Double) value);
                        } else {
                            row.add(new Double(0.0));
                        }

                        // Date
                    } else if (field.getType().equals(FieldType.DATE)) {

                        if (value != null) {
                            row.add(DbfFile.DATE_PARSER.format((Date) value));
                        } else {
                            row.add("");
                        }

                        // Booléen
                    } else if (field.getType().equals(FieldType.BOOLEAN)) {

                        if (value != null) {
                            row.add((Boolean) value);
                        } else {
                            row.add(false);
                        }

                        // Caractère
                    } else if (field.getType().equals(FieldType.STRING)) {

                        if (value != null) {
                            row.add((String) value);
                        } else {
                            row.add("");
                        }

                        // Autre -> Caractère
                    } else {

                        // Autres colonnes de type "GEOMETRY" ignorées : 1 seule
                        // geométry pour SHP
                        if (!field.getType().equals(FieldType.GEOMETRY)) {

                            if (value != null) {
                                row.add((String) value);
                            } else {
                                row.add("");
                            }

                        } else {

                        }
                    }
                }

                rows[i] = row;
                i++;
            }

            // Création des fichiers SHP, SHX et DBF
            GeometryCollection geometryCollection = geometryFactory.createGeometryCollection(geometries);

            // Vérification homogénéité de la collection
            checkGeometryCollection(geometryCollection);

            // Cas différencié POINT / MULTIPOINT pour shapefile
            if (this.multiPointShapefile) {
                geometryCollection = toMultiPoint(geometryCollection);
            }

            // Shp
            // Shapefile shapefile = new Shapefile(new
            // File(this.shpFileName).toURI().toURL());
            Shapefile shapefile = new Shapefile(this.shpFileName);
            if (!this.forceTo2DGeometry && this.zOnlyShapefile) {
                shapefile.write(geometryCollection, 4); // x,y,z
            } else {
                shapefile.write(geometryCollection, 2); // x,y
            }

            // Shx
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(this.shxFileName));
            EndianDataOutputStream endianDataOutputStream = new EndianDataOutputStream(bufferedOutputStream);
            if (!this.forceTo2DGeometry && this.zOnlyShapefile) {
                shapefile.writeIndex(geometryCollection, endianDataOutputStream, 4);// x,y,z
            } else {
                shapefile.writeIndex(geometryCollection, endianDataOutputStream, 2);// x,y
            }
            bufferedOutputStream.close();

            // Dbf
            DbfFileWriter dbfFileWriter;
            dbfFileWriter = new DbfFileWriter(this.dbfFileName);
            dbfFileWriter.setCharset(this.charset);
            dbfFileWriter.writeHeader(dbfFields.toArray(new DbfFieldDef[dbfFields.size()]), features.size());
            dbfFileWriter.writeRecords(rows);
            dbfFileWriter.close();

            // Prj
            if (srid > 0 && createPrjFile) {

                CRSFactory cRSFactory = new CRSFactory();
                RegistryManager registryManager = cRSFactory.getRegistryManager();
                registryManager.addRegistry(new EPSGRegistry());
                CoordinateReferenceSystem crs = cRSFactory.getCRS("EPSG:" + srid);
                if (crs != null) {

                    PrintWriter printWriter = new PrintWriter(this.prjFileName, this.charset.name());
                    printWriter.print(crs.toWKT());
                    printWriter.close();

                }

            }

        } catch (IOException e) {
            throw new KettleException("Error writing features to " + this.shpFileName, e);
        } catch (Exception e) {
            throw new KettleException("Error writing features to " + this.shpFileName, e);
        }

    }

    @SuppressWarnings("unchecked")
    private void checkGeometryCollection(GeometryCollection geometryCollection) throws KettleException {

        HashMap<String, Object> geometryCollectionInfos = GeometryUtils.getGeometryCollectionInfos(geometryCollection);

        // Présence d'une GEOMETRYCOLLECTION
        List<String> types = (List<String>) geometryCollectionInfos.get("TYPES");
        if (types.contains("GEOMETRYCOLLECTION")) {
            throw new KettleException("Error writing features to " + this.shpFileName + " : GEOMETRYCOLLECTION geometries are not supported");
        }

        // Présence de plusieurs SRID
        List<Integer> srids = (List<Integer>) geometryCollectionInfos.get("SRIDS");
        if (srids.size() == 0) {
            this.srid = 0;
        } else if (srids.size() > 1) {
            throw new KettleException("Error writing features to " + this.shpFileName + " : Mixed SRID are not supported " + srids.toString());
        } else {

            if (!srids.get(0).equals(0)) {
                this.srid = srids.get(0);
            }
        }

        // 3D uniquement
        List<Integer> dimensions = (List<Integer>) geometryCollectionInfos.get("DIMENSIONS");
        if (dimensions.size() == 1 && dimensions.contains(3)) {
            this.zOnlyShapefile = true;
        }

        // Types généraux de geometry
        List<String> primaryTypes = (List<String>) geometryCollectionInfos.get("PRIMARY_TYPES");
        if (primaryTypes.size() > 1) {
            throw new KettleException("Error writing features to " + this.shpFileName + " : Mixed types of geometries are not supported " + types.toString());
        } else {

            // Cas différencié POINT / MULTIPOINT pour shapefile
            if (primaryTypes.contains("POINT")) {
                this.multiPointShapefile = types.contains("MULTIPOINT");
            }
        }
    }

    private GeometryCollection toMultiPoint(GeometryCollection geometryCollection) {

        MultiPoint[] geometries = new MultiPoint[geometryCollection.getNumGeometries()];
        for (int i = 0; i < geometryCollection.getNumGeometries(); i++) {
            geometries[i] = (MultiPoint) GeometryUtils.getMultiGeometry(geometryCollection.getGeometryN(i));
        }
        return geometryFactory.createGeometryCollection(geometries);

    }

}
