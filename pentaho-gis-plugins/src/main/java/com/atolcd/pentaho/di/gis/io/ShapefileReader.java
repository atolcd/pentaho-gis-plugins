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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.geotools.dbffile.DbfFile;
import org.geotools.shapefile.Shapefile;
import org.geotools.shapefile.ShapefileHeader;
import org.pentaho.di.core.exception.KettleException;
import org.pentaho.di.trans.steps.xbaseinput.XBase;

import com.atolcd.pentaho.di.gis.io.features.Feature;
import com.atolcd.pentaho.di.gis.io.features.Field;
import com.atolcd.pentaho.di.gis.io.features.Field.FieldType;
import com.atolcd.pentaho.di.gis.utils.GeometryUtils;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jump.io.EndianDataInputStream;

public class ShapefileReader extends AbstractFileReader {

    private static GeometryFactory geometryFactory = new GeometryFactory();

    private String shpFileName;
    private String dbfFileName;
    private boolean dbfFileExist;
    private boolean shpFileExist;

    public ShapefileReader(String fileName, String geometryFieldName, String charsetName) throws KettleException {

        super(null, geometryFieldName, charsetName);

        try {
            this.shpFileExist = new File(checkFilename(fileName)).exists();
            this.dbfFileExist = new File(checkFilename(replaceFileExtension(fileName, ".shp", ".dbf"))).exists();

            if (!this.shpFileExist) {
                throw new KettleException("Missing " + fileName + " file");
            } else {
                this.shpFileName = checkFilename(fileName);
            }

            if (this.dbfFileExist) {
                this.dbfFileName = checkFilename(replaceFileExtension(fileName, ".shp", ".dbf"));
            }

            // Entête shapefile
            EndianDataInputStream endianInputStream = new EndianDataInputStream(new FileInputStream(this.shpFileName));
            ShapefileHeader shapeFileheader = new ShapefileHeader(endianInputStream);

            // vérification du type
            if (!(shapeFileheader.getShapeType() == 11 || shapeFileheader.getShapeType() == 13 || shapeFileheader.getShapeType() == 15 || shapeFileheader.getShapeType() == 18
                    || shapeFileheader.getShapeType() == 1 || shapeFileheader.getShapeType() == 3 || shapeFileheader.getShapeType() == 5 || shapeFileheader.getShapeType() == 8
                    || shapeFileheader.getShapeType() == 21 || shapeFileheader.getShapeType() == 23 || shapeFileheader.getShapeType() == 25 || shapeFileheader.getShapeType() == 28)) {

                throw new KettleException("Shapefile type " + Shapefile.getShapeTypeDescription(shapeFileheader.getShapeType()) + " not supported");
            }

            endianInputStream.close();

            this.fields.add(new Field(geometryFieldName, FieldType.GEOMETRY, null, null));

            // Si présence DBF
            if (this.dbfFileExist) {

                DbfFile dbfFile = new DbfFile(this.dbfFileName, this.charset);
                for (int i = 0; i < dbfFile.getNumFields(); i++) {

                    if (dbfFile.getFieldType(i).equalsIgnoreCase(FieldType.STRING.toString())) {
                        this.fields.add(new Field(dbfFile.getFieldName(i), FieldType.STRING, null, null));
                    } else if (dbfFile.getFieldType(i).equalsIgnoreCase("INTEGER")) {
                        this.fields.add(new Field(dbfFile.getFieldName(i), FieldType.LONG, null, null));
                    } else if (dbfFile.getFieldType(i).equalsIgnoreCase(FieldType.DOUBLE.toString())) {
                        this.fields.add(new Field(dbfFile.getFieldName(i), FieldType.DOUBLE, null, null));
                    } else if (dbfFile.getFieldType(i).equalsIgnoreCase(FieldType.DATE.toString())) {
                        this.fields.add(new Field(dbfFile.getFieldName(i), FieldType.DATE, null, null));
                    } else {
                        this.fields.add(new Field(dbfFile.getFieldName(i), FieldType.STRING, null, null));
                    }

                }

                dbfFile.close();

            }

        } catch (Exception e) {
            throw new KettleException("Error initialize reader", e);
        }
    }

    public List<Feature> getFeatures() throws KettleException {

        List<Feature> features = new ArrayList<Feature>();
        try {

            InputStream inputStream = new FileInputStream(this.shpFileName);
            Shapefile shapefile = new Shapefile(inputStream);
            GeometryCollection geometryCollection = shapefile.read(geometryFactory);

            if (this.limit == 0 || this.limit > geometryCollection.getNumGeometries() || this.limit < 0) {
                this.limit = geometryCollection.getNumGeometries();
            }

            for (int i = 0; i < this.limit; i++) {

                Feature feature = new Feature();
                Geometry geometry = geometryCollection.getGeometryN(i);

                if (this.forceTo2DGeometry) {
                    geometry = GeometryUtils.get2DGeometry(geometry);
                }

                if (this.forceToMultiGeometry) {
                    geometry = GeometryUtils.getMultiGeometry(geometry);
                }

                feature.addValue(this.fields.get(0), geometry);
                features.add(feature);

            }

            inputStream.close();
            shapefile.close();

            if (this.dbfFileExist) {

                // DbfFile dbfFile = new DbfFile(this.dbfFileName, this.charset);
                // for (int i = 0; i < features.size(); i++) {

                //     byte[] record = dbfFile.GetDbfRec(i);

                //     for (int j = 0; j < this.getFields().size() - 1; j++) {
                //         features.get(i).addValue(this.fields.get(j + 1), dbfFile.ParseRecordColumn(record, j));
                //     }

                // }

                // dbfFile.close();

                
                XBase xbase = new XBase(null, dbfFileName);

                xbase.setDbfFile(dbfFileName);
                xbase.open();
                
                xbase.getReader().setCharactersetName(this.charset.name());
                for (int i = 0; i < features.size(); i++) {

                    Object[] record = xbase.getReader().nextRecord();

                    for (int j = 0; j < this.getFields().size() - 1; j++) {
                        features.get(i).addValue(this.fields.get(j + 1), record[j]);
                    }

                }
                xbase.close();

            }

        } catch (IOException e) {
            throw new KettleException("Error reading features" + this.shpFileName + this.dbfFileName, e);
        } catch (Exception e) {
            throw new KettleException("Error reading features" + this.shpFileName + this.dbfFileName, e);
        }

        return features;

    }

}
